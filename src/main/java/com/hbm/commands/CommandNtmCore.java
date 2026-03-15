package com.hbm.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.hbm.dim.CelestialBody;
import com.hbm.dim.CelestialCore;
import com.hbm.dim.CelestialCore.CoreCategory;
import com.hbm.dim.CelestialCore.CoreEntry;
import com.hbm.dim.CelestialCore.MaterialMass;
import com.hbm.dim.SolarSystemWorldSavedData;
import com.hbm.dim.orbit.OrbitalStation;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

public class CommandNtmCore extends CommandBase {

	private static final double MASS_CAP_EPS = 1.0D;
	private static final int MAX_ENTRIES_PER_CATEGORY = 4;

	private static class CoreEntryRef {
		final CoreCategory category;
		final CoreEntry entry;

		CoreEntryRef(CoreCategory category, CoreEntry entry) {
			this.category = category;
			this.entry = entry;
		}
	}

	@Override
	public String getCommandName() {
		return "ntmcore";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/ntmcore [help|info|set|add|remove|scale] (remove expects a value)";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 0;
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		World world = sender.getEntityWorld();
		if(world == null) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "No world available."));
			return;
		}

		if(args.length == 0) {
			CelestialBody body = resolveBody(sender, world);
			if(body == null) {
				sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Could not resolve celestial body."));
				return;
			}
			showCoreInfo(sender, body);
			return;
		}

		String sub = args[0].toLowerCase(Locale.US);
		try {
			switch(sub) {
				case "help":
					showUsage(sender);
					return;
				case "info":
					handleInfo(sender, world, args);
					return;
				case "set":
					handleSet(sender, world, args);
					return;
				case "add":
					handleAdd(sender, world, args);
					return;
				case "remove":
				case "del":
				case "delete":
					handleRemove(sender, world, args);
					return;
				case "scale":
					handleScale(sender, world, args);
					return;
				default:
					sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + getCommandUsage(sender)));
					return;
			}
		} catch(Exception ex) {
			String message = ex.getMessage();
			if(message == null || message.trim().isEmpty()) {
				message = ex.getClass().getSimpleName();
			}
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + message));
		}
	}

	private void handleInfo(ICommandSender sender, World world, String[] args) {
		if(args.length > 2) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: /ntmcore info [body]"));
			return;
		}
		CelestialBody body = resolveBody(sender, world, args.length == 2 ? args[1] : null);
		if(body == null) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Unknown celestial body."));
			return;
		}
		showCoreInfo(sender, body);
	}

	private void handleSet(ICommandSender sender, World world, String[] args) {
		if(!canEdit(sender)) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "You need OP level 2 to edit core composition."));
			return;
		}
		if(args.length < 3 || args.length > 4) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: /ntmcore set <material|oreDict> <value> [body]"));
			return;
		}
		String bodyArg = args.length == 4 ? args[3] : null;

		CelestialBody body = resolveBody(sender, world, bodyArg);
		if(body == null) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Unknown celestial body."));
			return;
		}
		CelestialCore core = body.getCore();
		if(core == null) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Body '" + body.name + "' has no core data."));
			return;
		}

		String query = args[1];
		float value = (float) parseDoubleBounded(
			sender,
			args[2],
			(double) CelestialCore.MATERIAL_ENTRY_VALUE_MIN,
			(double) CelestialCore.MATERIAL_ENTRY_VALUE_MAX
		);
		List<CoreEntryRef> matches = findEntryMatches(core, query);
		if(matches.isEmpty()) {
			sender.addChatMessage(new ChatComponentText(
				EnumChatFormatting.RED + "Material '" + query + "' is not in this core. Use /ntmcore add to insert it first."
			));
			return;
		}
		if(matches.size() > 1) {
			sendAmbiguousMatches(sender, query, matches);
			return;
		}
		CoreEntryRef ref = matches.get(0);
		float oldValue = Math.max(0.0F, ref.entry.value);
		CelestialCore candidate = core.copy();
		candidate.setEntryValue(ref.category.name, ref.entry.oreDict, value);
		boolean increasesValue = value > oldValue;
		boolean decreasesValue = value < oldValue;
		String massError = validateMassCaps(body, candidate, increasesValue, decreasesValue);
		if(massError != null) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + massError));
			return;
		}
		core.setEntryValue(ref.category.name, ref.entry.oreDict, value);

		applyAndPersistCore(world, body, core);
		double categoryShare = getCategoryShare(core, ref.category.name, ref.entry.oreDict);
		double coreShare = getCoreShare(core, ref.category.name, ref.entry.oreDict);
		double massShare = getMassShare(core, ref.category.name, ref.entry.oreDict);
		float entryValue = getEntryValue(core, ref.category.name, ref.entry.oreDict);
		sender.addChatMessage(new ChatComponentText(
			EnumChatFormatting.GREEN + "Updated "
			+ EnumChatFormatting.AQUA + getMaterialToken(ref.entry.oreDict)
			+ EnumChatFormatting.GRAY + " (" + formatCategory(ref.category.name) + ")"
			+ EnumChatFormatting.GREEN + " to value "
			+ EnumChatFormatting.WHITE + formatValue(entryValue)
			+ EnumChatFormatting.GRAY + " (category " + formatPercent(categoryShare) + ", core " + formatPercent(coreShare) + ", mass " + formatPercent(massShare) + ")"
			+ EnumChatFormatting.GREEN + " on "
			+ EnumChatFormatting.AQUA + body.name
		));
	}

	private void handleAdd(ICommandSender sender, World world, String[] args) {
		if(!canEdit(sender)) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "You need OP level 2 to edit core composition."));
			return;
		}
		if(args.length < 3 || args.length > 4) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: /ntmcore add <material|oreDict> <value> [body]"));
			return;
		}

		String bodyArg = args.length == 4 ? args[3] : null;
		CelestialBody body = resolveBody(sender, world, bodyArg);
		if(body == null) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Unknown celestial body."));
			return;
		}
		CelestialCore core = body.getCore();
		if(core == null) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Body '" + body.name + "' has no core data."));
			return;
		}

		String query = args[1];
		float value = (float) parseDoubleBounded(sender, args[2], 0.0D, (double) Float.MAX_VALUE);
		if(value <= 0.0F) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Value must be > 0."));
			return;
		}
		List<CoreEntryRef> matches = findEntryMatches(core, query);
		if(matches.size() > 1) {
			sendAmbiguousMatches(sender, query, matches);
			return;
		}

		String oreDict;
		String categoryKey;
		CelestialCore candidate = core.copy();
		if(matches.size() == 1) {
			CoreEntryRef existing = matches.get(0);
			oreDict = existing.entry.oreDict;
			categoryKey = existing.category.name;
			CoreEntry candidateEntry = candidate.getEntry(categoryKey, oreDict);
			float nextValue = (candidateEntry != null ? candidateEntry.value : 0.0F) + value;
			candidate.setEntryValue(categoryKey, oreDict, nextValue);
		} else {
			oreDict = normalizeInputOreDict(query);
			categoryKey = CelestialCore.getCategoryForOreDict(oreDict);
			if(categoryKey == null) {
				sender.addChatMessage(new ChatComponentText(
					EnumChatFormatting.RED + "Unknown material '" + query + "'. Use a valid oreDict ID."
				));
				return;
			}
			if(getCategoryEntryCount(core, categoryKey) >= MAX_ENTRIES_PER_CATEGORY) {
				sender.addChatMessage(new ChatComponentText(
					EnumChatFormatting.RED + formatCategory(categoryKey) + " already has "
					+ MAX_ENTRIES_PER_CATEGORY + " entries. Remove one before adding a new material."
				));
				return;
			}
			candidate.addOrUpdateEntryValue(categoryKey, oreDict, value);
		}

		String massError = validateMassCaps(body, candidate, true, false);
		if(massError != null) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + massError));
			return;
		}

		if(matches.size() == 1) {
			CoreEntryRef existing = matches.get(0);
			float nextValue = existing.entry.value + value;
			core.setEntryValue(categoryKey, oreDict, nextValue);
		} else {
			core.addOrUpdateEntryValue(categoryKey, oreDict, value);
		}

		applyAndPersistCore(world, body, core);

		double categoryShare = getCategoryShare(core, categoryKey, oreDict);
		double coreShare = getCoreShare(core, categoryKey, oreDict);
		double massShare = getMassShare(core, categoryKey, oreDict);
		float entryValue = getEntryValue(core, categoryKey, oreDict);
		sender.addChatMessage(new ChatComponentText(
			EnumChatFormatting.GREEN + "Added value "
			+ EnumChatFormatting.WHITE + formatValue(value)
			+ EnumChatFormatting.GREEN + " to "
			+ EnumChatFormatting.AQUA + getMaterialToken(oreDict)
			+ EnumChatFormatting.GRAY + " (" + formatCategory(categoryKey) + ")"
			+ EnumChatFormatting.GRAY + " -> value " + formatValue(entryValue)
			+ EnumChatFormatting.GRAY + " (category " + formatPercent(categoryShare) + ", core " + formatPercent(coreShare) + ", mass " + formatPercent(massShare) + ")"
			+ EnumChatFormatting.GREEN + " on "
			+ EnumChatFormatting.AQUA + body.name
		));
	}

	private void handleRemove(ICommandSender sender, World world, String[] args) {
		if(!canEdit(sender)) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "You need OP level 2 to edit core composition."));
			return;
		}
		if(args.length < 3 || args.length > 4) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: /ntmcore remove <material|oreDict> <value> [body]"));
			return;
		}

		CelestialBody body = resolveBody(sender, world, args.length == 4 ? args[3] : null);
		if(body == null) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Unknown celestial body."));
			return;
		}
		CelestialCore core = body.getCore();
		if(core == null) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Body '" + body.name + "' has no core data."));
			return;
		}

		String query = args[1];
		float value = (float) parseDoubleBounded(sender, args[2], 0.0D, (double) Float.MAX_VALUE);
		if(value <= 0.0F) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Value must be > 0."));
			return;
		}
		List<CoreEntryRef> matches = findEntryMatches(core, query);
		if(matches.isEmpty()) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "No matching core entry for '" + query + "'."));
			return;
		}
		if(matches.size() > 1) {
			sendAmbiguousMatches(sender, query, matches);
			return;
		}

		CoreEntryRef ref = matches.get(0);
		float oldValue = Math.max(0.0F, ref.entry.value);
		if(value > oldValue) {
			sender.addChatMessage(new ChatComponentText(
				EnumChatFormatting.RED + "Cannot remove " + formatValue(value) + " from "
				+ getMaterialToken(ref.entry.oreDict) + "; current value is " + formatValue(oldValue) + "."
			));
			return;
		}

		float nextValue = oldValue - value;
		CelestialCore candidate = core.copy();
		if(nextValue <= 0.0F) {
			if(!candidate.removeEntry(ref.category.name, ref.entry.oreDict)) {
				sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Failed to remove core entry '" + ref.entry.oreDict + "' from candidate state."));
				return;
			}
		} else {
			candidate.setEntryValue(ref.category.name, ref.entry.oreDict, nextValue);
		}

		String massError = validateMassCaps(body, candidate, false, true);
		if(massError != null) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + massError));
			return;
		}

		if(nextValue <= 0.0F) {
			if(!core.removeEntry(ref.category.name, ref.entry.oreDict)) {
				sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Failed to remove core entry '" + ref.entry.oreDict + "'."));
				return;
			}
		} else {
			core.setEntryValue(ref.category.name, ref.entry.oreDict, nextValue);
		}

		applyAndPersistCore(world, body, core);
		if(nextValue <= 0.0F) {
			sender.addChatMessage(new ChatComponentText(
				EnumChatFormatting.GREEN + "Removed value "
				+ EnumChatFormatting.WHITE + formatValue(value)
				+ EnumChatFormatting.GREEN + " from "
				+ EnumChatFormatting.AQUA + getMaterialToken(ref.entry.oreDict)
				+ EnumChatFormatting.GREEN + " and deleted the entry on "
				+ EnumChatFormatting.AQUA + body.name
			));
			return;
		}

		double categoryShare = getCategoryShare(core, ref.category.name, ref.entry.oreDict);
		double coreShare = getCoreShare(core, ref.category.name, ref.entry.oreDict);
		double massShare = getMassShare(core, ref.category.name, ref.entry.oreDict);
		float entryValue = getEntryValue(core, ref.category.name, ref.entry.oreDict);

		sender.addChatMessage(new ChatComponentText(
			EnumChatFormatting.GREEN + "Removed value "
			+ EnumChatFormatting.WHITE + formatValue(value)
			+ EnumChatFormatting.GREEN + " from "
			+ EnumChatFormatting.AQUA + getMaterialToken(ref.entry.oreDict)
			+ EnumChatFormatting.GRAY + " (" + formatCategory(ref.category.name) + ")"
			+ EnumChatFormatting.GRAY + " -> value " + formatValue(entryValue)
			+ EnumChatFormatting.GRAY + " (category " + formatPercent(categoryShare) + ", core " + formatPercent(coreShare) + ", mass " + formatPercent(massShare) + ")"
			+ EnumChatFormatting.GREEN + " on "
			+ EnumChatFormatting.AQUA + body.name
		));
	}

	private void handleScale(ICommandSender sender, World world, String[] args) {
		if(!canEdit(sender)) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "You need OP level 2 to edit core composition."));
			return;
		}
		if(args.length < 2 || args.length > 3) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: /ntmcore scale <densityScale> [body]"));
			return;
		}

		CelestialBody body = resolveBody(sender, world, args.length == 3 ? args[2] : null);
		if(body == null) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Unknown celestial body."));
			return;
		}
		CelestialCore core = body.getCore();
		if(core == null) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Body '" + body.name + "' has no core data."));
			return;
		}

		double scale = parseDouble(sender, args[1]);
		if(scale <= 0.0D) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Density scale must be > 0."));
			return;
		}

		core.withDensityScale(scale);
		applyAndPersistCore(world, body, core);
		sender.addChatMessage(new ChatComponentText(
			EnumChatFormatting.GREEN + "Set density scale for "
			+ EnumChatFormatting.AQUA + body.name
			+ EnumChatFormatting.GREEN + " to "
			+ EnumChatFormatting.WHITE + String.format(Locale.US, "%.3f", core.densityScale)
		));
	}

	private void showUsage(ICommandSender sender) {
		sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/ntmcore info [body]"));
		sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/ntmcore set <material|oreDict> <value> [body]"));
		sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/ntmcore add <material|oreDict> <value> [body]"));
		sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/ntmcore remove <material|oreDict> <value> [body]"));
		sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/ntmcore scale <densityScale> [body]"));
	}

	private void showCoreInfo(ICommandSender sender, CelestialBody body) {
		CelestialCore core = body.getCore();
		if(core == null) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Body '" + body.name + "' has no core data."));
			return;
		}

		core.recalculateForRadius(body.radiusKm);

		sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "Core Info: " + EnumChatFormatting.AQUA + body.name));
		sender.addChatMessage(new ChatComponentText(
			EnumChatFormatting.YELLOW + "Radius: " + EnumChatFormatting.WHITE + formatNumber(body.radiusKm) + " km"
			+ EnumChatFormatting.GRAY + " | "
			+ EnumChatFormatting.YELLOW + "Density scale: " + EnumChatFormatting.WHITE + String.format(Locale.US, "%.3f", core.densityScale)
		));
		sender.addChatMessage(new ChatComponentText(
			EnumChatFormatting.YELLOW + "Bulk density: " + EnumChatFormatting.WHITE + formatNumber(core.computedBulkDensityKgPerM3) + " kg/m^3"
		));
		sender.addChatMessage(new ChatComponentText(
			EnumChatFormatting.YELLOW + "Core mass: " + EnumChatFormatting.WHITE + formatMass(core.computedCoreMassKg) + " kg"
			+ EnumChatFormatting.GRAY + " | "
			+ EnumChatFormatting.YELLOW + "Planet shell mass: " + EnumChatFormatting.WHITE + formatMass(core.computedPlanetMassKg) + " kg"
		));
		sender.addChatMessage(new ChatComponentText(
			EnumChatFormatting.YELLOW + "Total mass: " + EnumChatFormatting.WHITE + formatMass(core.computedTotalMassKg) + " kg"
		));
		sender.addChatMessage(new ChatComponentText(
			EnumChatFormatting.YELLOW + "Core radioactivity: " + EnumChatFormatting.WHITE + formatRadiation(core.getAverageRadioactivity()) + " rad/s"
		));
		sender.addChatMessage(new ChatComponentText(
			EnumChatFormatting.YELLOW + "Magnetic shielding: " + EnumChatFormatting.WHITE + formatPercent(core.getMagneticFieldStrength())
		));
		sender.addChatMessage(new ChatComponentText(
			EnumChatFormatting.YELLOW + "Atmosphere retention: " + EnumChatFormatting.WHITE + formatAtmosphere(CelestialBody.getAtmosphereRetentionLimitAtm(body)) + " atm"
		));
		sender.addChatMessage(new ChatComponentText(
			EnumChatFormatting.DARK_GRAY + "Amounts are absolute; category/core/mass values are relative shares."
		));

		Map<String, List<MaterialMass>> massesByCategory = groupByCategory(core.materialMasses);
		if(massesByCategory.isEmpty()) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "No core material entries."));
			return;
		}

		for(CoreCategory category : core.categories) {
			List<MaterialMass> masses = massesByCategory.get(category.name);
			if(masses == null || masses.isEmpty()) continue;

			double totalVolumeShare = 0.0D;
			double totalMassShare = 0.0D;
			for(MaterialMass mass : masses) {
				totalVolumeShare += mass.volumeShare;
				totalMassShare += mass.massShare;
			}

			sender.addChatMessage(new ChatComponentText(
				EnumChatFormatting.GREEN + formatCategory(category.name)
				+ EnumChatFormatting.GRAY + " (amount " + formatValue(getCategoryTotalValue(category))
				+ ", core-share " + formatPercent(totalVolumeShare)
				+ ", mass-share " + formatPercent(totalMassShare) + ")"
			));

			for(MaterialMass mass : masses) {
				String token = getMaterialToken(mass.oreDict);
				double categoryShare = getCategoryShare(core, mass.category, mass.oreDict);
				float value = getEntryValue(core, mass.category, mass.oreDict);
				sender.addChatMessage(new ChatComponentText(
					EnumChatFormatting.GRAY + " - "
					+ EnumChatFormatting.AQUA + token
					+ EnumChatFormatting.GRAY + ": "
					+ EnumChatFormatting.WHITE + "amount " + formatValue(value) + ", "
					+ EnumChatFormatting.WHITE + formatPercent(categoryShare) + " category-share, "
					+ EnumChatFormatting.WHITE + formatPercent(mass.volumeShare) + " core-share, "
					+ EnumChatFormatting.WHITE + formatPercent(mass.massShare) + " mass-share"
				));
			}
		}
	}

	private void applyAndPersistCore(World world, CelestialBody body, CelestialCore core) {
		CelestialBody.applyMassFromCore(body, core);
		if(world != null && !world.isRemote) {
			SolarSystemWorldSavedData.get(world).setCore(body.name, core);
		}
	}

	private void sendAmbiguousMatches(ICommandSender sender, String query, List<CoreEntryRef> matches) {
		sender.addChatMessage(new ChatComponentText(
			EnumChatFormatting.RED + "Material '" + query + "' is ambiguous. Use oreDict ID from /ntmcore info."
		));
		int limit = Math.min(6, matches.size());
		for(int i = 0; i < limit; i++) {
			CoreEntryRef ref = matches.get(i);
			sender.addChatMessage(new ChatComponentText(
				EnumChatFormatting.GRAY + " - "
				+ EnumChatFormatting.WHITE + ref.entry.oreDict
				+ EnumChatFormatting.GRAY + " (" + formatCategory(ref.category.name) + ")"
			));
		}
		if(matches.size() > limit) {
			sender.addChatMessage(new ChatComponentText(
				EnumChatFormatting.GRAY + " ...and " + (matches.size() - limit) + " more"
			));
		}
	}

	private boolean canEdit(ICommandSender sender) {
		return sender.canCommandSenderUseCommand(2, getCommandName());
	}

	private CelestialBody resolveBody(ICommandSender sender, World world, String explicitBodyName) {
		if(explicitBodyName != null && !explicitBodyName.trim().isEmpty()) {
			return findBodyByName(explicitBodyName);
		}
		return resolveBody(sender, world);
	}

	private CelestialBody resolveBody(ICommandSender sender, World world) {
		if(CelestialBody.inOrbit(world)) {
			ChunkCoordinates coords = sender.getPlayerCoordinates();
			if(coords != null) {
				OrbitalStation station = OrbitalStation.getStationFromPosition(coords.posX, coords.posZ);
				if(station != null && station.orbiting != null) {
					return station.orbiting;
				}
			}
		}
		return CelestialBody.getBody(world);
	}

	private CelestialBody findBodyByName(String bodyName) {
		if(bodyName == null || bodyName.trim().isEmpty()) return null;
		for(CelestialBody body : CelestialBody.getAllBodies()) {
			if(body != null && body.name != null && body.name.equalsIgnoreCase(bodyName.trim())) {
				return body;
			}
		}
		return null;
	}

	private List<CoreEntryRef> findEntryMatches(CelestialCore core, String query) {
		List<CoreEntryRef> matches = new ArrayList<CoreEntryRef>();
		if(core == null || query == null) return matches;
		String normalizedQuery = normalizeKey(query);
		if(normalizedQuery == null || normalizedQuery.isEmpty()) return matches;

		for(CoreCategory category : core.categories) {
			if(category == null || category.entries == null) continue;
			for(CoreEntry entry : category.entries) {
				if(entry == null || entry.oreDict == null) continue;
				String entryId = normalizeKey(entry.oreDict);
				String token = normalizeKey(getMaterialToken(entry.oreDict));
				if(normalizedQuery.equals(entryId) || normalizedQuery.equals(token)) {
					matches.add(new CoreEntryRef(category, entry));
				}
			}
		}
		return matches;
	}

	private String normalizeInputOreDict(String query) {
		if(query == null) return null;
		return query.trim();
	}

	private float getCategoryTotalValue(CoreCategory category) {
		if(category == null || category.entries == null) return 0.0F;
		float total = 0.0F;
		for(CoreEntry entry : category.entries) {
			if(entry == null) continue;
			total += Math.max(0.0F, entry.value);
		}
		return total;
	}

	private double getCategoryShare(CelestialCore core, String categoryName, String oreDict) {
		if(core == null || categoryName == null || oreDict == null) return 0.0D;
		CoreCategory category = core.getCategory(categoryName);
		if(category == null || category.entries == null) return 0.0D;
		double categoryTotal = 0.0D;
		double entryTotal = 0.0D;
		for(CoreEntry entry : category.entries) {
			if(entry == null) continue;
			categoryTotal += Math.max(0.0D, (double) entry.value);
			if(oreDict.equals(entry.oreDict)) {
				entryTotal += Math.max(0.0D, (double) entry.value);
			}
		}
		return categoryTotal > 0.0D ? entryTotal / categoryTotal : 0.0D;
	}

	private double getCoreShare(CelestialCore core, String categoryName, String oreDict) {
		if(core == null || categoryName == null || oreDict == null || core.materialMasses == null) return 0.0D;
		for(MaterialMass mass : core.materialMasses) {
			if(mass == null) continue;
			if(categoryName.equals(mass.category) && oreDict.equals(mass.oreDict)) {
				return Math.max(0.0D, mass.volumeShare);
			}
		}
		return 0.0D;
	}

	private double getMassShare(CelestialCore core, String categoryName, String oreDict) {
		if(core == null || categoryName == null || oreDict == null || core.materialMasses == null) return 0.0D;
		for(MaterialMass mass : core.materialMasses) {
			if(mass == null) continue;
			if(categoryName.equals(mass.category) && oreDict.equals(mass.oreDict)) {
				return Math.max(0.0D, mass.massShare);
			}
		}
		return 0.0D;
	}

	private float getEntryValue(CelestialCore core, String categoryName, String oreDict) {
		if(core == null || categoryName == null || oreDict == null) return 0.0F;
		CoreEntry entry = core.getEntry(categoryName, oreDict);
		return entry != null ? Math.max(0.0F, entry.value) : 0.0F;
	}

	private String validateMassCaps(CelestialBody body, CelestialCore candidate, boolean enforceMax, boolean enforceMin) {
		if(body == null || candidate == null) return "Unable to validate mass caps for this operation.";
		double rawTotalMass = candidate.calculateRawTotalMassKg(body.radiusKm);
		if(Double.isNaN(rawTotalMass) || Double.isInfinite(rawTotalMass)) {
			return "Resulting core mass is invalid.";
		}

		if(enforceMax) {
			double maxMass = candidate.getMaxTotalMassKg();
			if(!Double.isInfinite(maxMass) && rawTotalMass > maxMass + MASS_CAP_EPS) {
				return "Operation would exceed maximum core mass cap (" + formatMass(maxMass) + " kg).";
			}
		}

		if(enforceMin) {
			double minMass = candidate.getMinTotalMassKg();
			if(rawTotalMass < minMass - MASS_CAP_EPS) {
				return "Operation would go below minimum core mass cap (" + formatMass(minMass) + " kg).";
			}
		}

		return null;
	}

	private Map<String, List<MaterialMass>> groupByCategory(List<MaterialMass> materialMasses) {
		Map<String, List<MaterialMass>> map = new LinkedHashMap<String, List<MaterialMass>>();
		if(materialMasses == null) return map;

		for(MaterialMass mass : materialMasses) {
			if(mass == null || mass.category == null) continue;
			List<MaterialMass> list = map.get(mass.category);
			if(list == null) {
				list = new ArrayList<MaterialMass>();
				map.put(mass.category, list);
			}
			list.add(mass);
		}
		return map;
	}

	private String formatCategory(String category) {
		if(category == null) return null;

		switch(category) {
			case CelestialCore.CAT_LIGHT: return "Light Metal";
			case CelestialCore.CAT_HEAVY: return "Heavy Metal";
			case CelestialCore.CAT_RARE: return "Rare Earth";
			case CelestialCore.CAT_ACTINIDE: return "Actinide";
			case CelestialCore.CAT_NONMETAL: return "Non-Metal";
			case CelestialCore.CAT_CRYSTAL: return "Crystalline";
			case CelestialCore.CAT_SCHRABIDIC: return "Schrabidic";
			case CelestialCore.CAT_LIVING: return "Living";
			default: return category;
		}
	}

	private String getMaterialToken(String oreDict) {
		if(oreDict == null || oreDict.isEmpty()) return oreDict;
		for(int i = 0; i < oreDict.length(); i++) {
			if(Character.isUpperCase(oreDict.charAt(i))) {
				return oreDict.substring(i);
			}
		}
		return toDisplayToken(oreDict);
	}

	private String toDisplayToken(String token) {
		if(token == null || token.isEmpty()) return token;
		for(int i = 0; i < token.length(); i++) {
			if(Character.isUpperCase(token.charAt(i))) {
				return token;
			}
		}
		char first = token.charAt(0);
		if(Character.isLetter(first) && Character.isLowerCase(first)) {
			return Character.toUpperCase(first) + token.substring(1);
		}
		return token;
	}

	private int getCategoryEntryCount(CelestialCore core, String categoryName) {
		if(core == null || categoryName == null) return 0;
		CoreCategory category = core.getCategory(categoryName);
		if(category == null || category.entries == null) return 0;
		return category.entries.size();
	}

	private String normalizeKey(String value) {
		if(value == null) return null;
		return value.trim().toLowerCase(Locale.US)
			.replace("-", "")
			.replace("_", "")
			.replace(" ", "");
	}

	@SuppressWarnings("rawtypes")
	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args) {
		if(args.length < 1) {
			return Collections.emptyList();
		}
		if(args.length == 1) {
			return getListOfStringsMatchingLastWord(args, "help", "info", "set", "add", "remove", "scale");
		}

		String sub = args[0].toLowerCase(Locale.US);
		if("info".equals(sub)) {
			if(args.length == 2) {
				return getListOfStringsFromIterableMatchingLastWord(args, getBodyNames());
			}
			return Collections.emptyList();
		}

		if("set".equals(sub)) {
			if(args.length == 2) {
				return getListOfStringsFromIterableMatchingLastWord(args, getMaterialSuggestions(sender));
			}
			if(args.length == 3) {
				return getListOfStringsMatchingLastWord(args, "0.05", "0.10", "0.25", "0.50", "1.0");
			}
			if(args.length == 4) {
				return getListOfStringsFromIterableMatchingLastWord(args, getBodyNames());
			}
			return Collections.emptyList();
		}

		if("add".equals(sub)) {
			if(args.length == 2) {
				return getListOfStringsFromIterableMatchingLastWord(args, getMaterialSuggestions(sender));
			}
			if(args.length == 3) {
				return getListOfStringsMatchingLastWord(args, "0.05", "0.10", "0.25", "0.50", "1.0");
			}
			if(args.length == 4) {
				return getListOfStringsFromIterableMatchingLastWord(args, getBodyNames());
			}
			return Collections.emptyList();
		}

		if("remove".equals(sub) || "del".equals(sub) || "delete".equals(sub)) {
			if(args.length == 2) {
				return getListOfStringsFromIterableMatchingLastWord(args, getMaterialSuggestions(sender));
			}
			if(args.length == 3) {
				return getListOfStringsMatchingLastWord(args, "0.01", "0.05", "0.10", "0.25", "0.50", "1.0");
			}
			if(args.length == 4) {
				return getListOfStringsFromIterableMatchingLastWord(args, getBodyNames());
			}
			return Collections.emptyList();
		}

		if("scale".equals(sub)) {
			if(args.length == 2) {
				return getListOfStringsMatchingLastWord(args, "0.5", "1.0", "1.5", "2.0");
			}
			if(args.length == 3) {
				return getListOfStringsFromIterableMatchingLastWord(args, getBodyNames());
			}
			return Collections.emptyList();
		}

		return Collections.emptyList();
	}

	private List<String> getBodyNames() {
		ArrayList<String> names = new ArrayList<String>();
		for(CelestialBody body : CelestialBody.getAllBodies()) {
			if(body != null && body.name != null) {
				names.add(body.name);
			}
		}
		return names;
	}

	private List<String> getMaterialSuggestions(ICommandSender sender) {
		World world = sender.getEntityWorld();
		if(world == null) return Collections.emptyList();
		CelestialBody body = resolveBody(sender, world);
		if(body == null || body.getCore() == null) return Collections.emptyList();

		Set<String> suggestions = new LinkedHashSet<String>();
		Set<String> seenTokens = new HashSet<String>();
		for(CoreCategory category : body.getCore().categories) {
			if(category == null || category.entries == null) continue;
			for(CoreEntry entry : category.entries) {
				if(entry == null || entry.oreDict == null) continue;
				String token = getMaterialToken(entry.oreDict);
				String normalizedToken = normalizeKey(token);
				if(normalizedToken != null && !seenTokens.contains(normalizedToken)) {
					suggestions.add(token);
					seenTokens.add(normalizedToken);
				}
				suggestions.add(entry.oreDict);
			}
		}
		return new ArrayList<String>(suggestions);
	}

	private String formatPercent(double share) {
		return String.format(Locale.US, "%.2f%%", share * 100.0D);
	}

	private String formatValue(double value) {
		if(Double.isNaN(value) || Double.isInfinite(value)) return "0";
		return String.format(Locale.US, "%.6f", value);
	}

	private String formatNumber(double value) {
		if(Double.isNaN(value) || Double.isInfinite(value)) return "0";
		return String.format(Locale.US, "%,.0f", value);
	}

	private String formatMass(double kg) {
		if(Double.isNaN(kg) || Double.isInfinite(kg)) return "0";
		if(Math.abs(kg) >= 1.0E12D) {
			return String.format(Locale.US, "%.3e", kg);
		}
		return String.format(Locale.US, "%,.0f", kg);
	}

	private String formatRadiation(double radiation) {
		if(Double.isNaN(radiation) || Double.isInfinite(radiation)) return "0";
		if(Math.abs(radiation) >= 10_000.0D) {
			return String.format(Locale.US, "%.3e", radiation);
		}
		return String.format(Locale.US, "%.3f", radiation);
	}

	private String formatAtmosphere(double pressureAtm) {
		if(Double.isNaN(pressureAtm) || Double.isInfinite(pressureAtm)) return "N/A";
		return String.format(Locale.US, "%.3f", pressureAtm);
	}
}
