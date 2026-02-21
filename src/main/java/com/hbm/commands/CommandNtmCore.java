package com.hbm.commands;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.hbm.dim.CelestialBody;
import com.hbm.dim.CelestialCore;
import com.hbm.dim.CelestialCore.MaterialMass;
import com.hbm.dim.orbit.OrbitalStation;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

public class CommandNtmCore extends CommandBase {

	@Override
	public String getCommandName() {
		return "ntmcore";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/ntmcore";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 0;
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if(args.length != 0) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + getCommandUsage(sender)));
			return;
		}

		World world = sender.getEntityWorld();
		if(world == null) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "No world available."));
			return;
		}

		CelestialBody body = resolveBody(sender, world);
		if(body == null) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Could not resolve celestial body."));
			return;
		}

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
			+ EnumChatFormatting.YELLOW + "Density scale: " + EnumChatFormatting.WHITE + formatNumber(core.densityScale)
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

		Map<String, List<MaterialMass>> massesByCategory = groupByCategory(core.materialMasses);
		if(massesByCategory.isEmpty()) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "No core material entries."));
			return;
		}

		for(CelestialCore.CoreCategory category : core.categories) {
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
				+ EnumChatFormatting.GRAY + " (vol " + formatPercent(totalVolumeShare)
				+ ", mass " + formatPercent(totalMassShare) + ")"
			));

			for(MaterialMass mass : masses) {
				String token = getMaterialToken(mass.oreDict);
				sender.addChatMessage(new ChatComponentText(
					EnumChatFormatting.GRAY + " - "
					+ EnumChatFormatting.AQUA + token
					+ EnumChatFormatting.GRAY + ": "
					+ EnumChatFormatting.WHITE + formatPercent(mass.volumeShare) + " vol, "
					+ formatPercent(mass.massShare) + " mass, "
					+ formatNumber(mass.densityKgPerM3) + " kg/m^3"
				));
			}
		}
	}

	private CelestialBody resolveBody(ICommandSender sender, World world) {
		if(CelestialBody.inOrbit(world)) {
			ChunkCoordinates coords = sender.getPlayerCoordinates();
			OrbitalStation station = OrbitalStation.getStationFromPosition(coords.posX, coords.posZ);
			if(station != null && station.orbiting != null) {
				return station.orbiting;
			}
		}
		return CelestialBody.getBody(world);
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
		return oreDict;
	}

	private String formatPercent(double share) {
		return String.format(Locale.US, "%.2f%%", share * 100.0D);
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
}
