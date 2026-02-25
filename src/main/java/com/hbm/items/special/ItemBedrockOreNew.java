package com.hbm.items.special;

import static com.hbm.inventory.material.Mats.*;
import static com.hbm.items.special.ItemBedrockOreNew.ProcessingTrait.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.hbm.items.ModItems;
import com.hbm.util.EnumUtil;
import com.hbm.util.i18n.I18nUtil;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.dim.CelestialBody;
import com.hbm.dim.CelestialCore;
import com.hbm.dim.CelestialCore.CoreCategory;
import com.hbm.dim.CelestialCore.CoreEntry;
import com.hbm.dim.SolarSystem;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.material.NTMMaterial.SmeltingBehavior;
import com.hbm.lib.RefStrings;
import com.hbm.render.icon.RGBMutatorInterpolatedComponentRemap;
import com.hbm.render.icon.TextureAtlasSpriteMutatable;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraftforge.oredict.OreDictionary;

public class ItemBedrockOreNew extends Item {

	public IIcon[] icons = new IIcon[CelestialBedrockOre.oreTypes.size() * BedrockOreGrade.values().length];
	public IIcon[] overlays = new IIcon[ProcessingTrait.values().length];
	public static final float MIN_PROCESSABLE_CORE_VALUE = 0.1F;
	private static final Comparator<ResolvedCoreEntry> CORE_VALUE_DESC = new Comparator<ResolvedCoreEntry>() {
		@Override
		public int compare(ResolvedCoreEntry left, ResolvedCoreEntry right) {
			return Float.compare(right.value, left.value);
		}
	};

	public ItemBedrockOreNew() {
		this.setHasSubtypes(true);
		this.setMaxDamage(0);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister reg) {


		if(reg instanceof TextureMap) {
			TextureMap map = (TextureMap) reg;

			for(int i = 0; i < BedrockOreGrade.values().length; i++) { BedrockOreGrade grade = BedrockOreGrade.values()[i];
				for(int j = 0; j < CelestialBedrockOre.oreTypes.size(); j++) { CelestialBedrockOreType type = CelestialBedrockOre.oreTypes.get(j);
					String placeholderName = RefStrings.MODID + ":bedrock_ore_" + grade.prefix + "_" + type.textureSuffix + "-" + (i * CelestialBedrockOre.oreTypes.size() + j);
					TextureAtlasSpriteMutatable mutableIcon = new TextureAtlasSpriteMutatable(placeholderName, new RGBMutatorInterpolatedComponentRemap(0xFFFFFF, 0x505050, type.light, type.dark));
					map.setTextureEntry(placeholderName, mutableIcon);
					this.icons[i * CelestialBedrockOre.oreTypes.size() + j] = mutableIcon;
				}
			}
		}

		for(int i = 0; i < overlays.length; i++) {
			ProcessingTrait trait = ProcessingTrait.values()[i];
			overlays[i] = reg.registerIcon(RefStrings.MODID + ":bedrock_ore_overlay." + trait.name().toLowerCase(Locale.US));
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item item, CreativeTabs tab, List list) {
		for(int i = 0; i < BedrockOreGrade.values().length; i++) { BedrockOreGrade grade = BedrockOreGrade.values()[i];
			if(tab != null && grade != BedrockOreGrade.BASE) continue;
			for(int j = 0; j < CelestialBedrockOre.oreTypes.size(); j++) { CelestialBedrockOreType type = CelestialBedrockOre.oreTypes.get(j);
				list.add(this.make(grade, type));
			}
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean requiresMultipleRenderPasses() {
		return true;
	}

	@Override
	public int getRenderPasses(int metadata) {
		return 1 + this.getGrade(metadata).traits.length;
	}

	@SideOnly(Side.CLIENT)
	public IIcon getIconFromDamageForRenderPass(int meta, int pass) {
		if(pass == 0) return this.getIconFromDamage(meta);
		return this.overlays[this.getGrade(meta).traits[pass - 1].ordinal()];
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIconFromDamage(int meta) {
		int icon = this.getGrade(meta).ordinal() * CelestialBedrockOre.oreTypes.size() + this.getType(meta).index;
		return icons[Math.abs(icon % icons.length)];
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		int meta = stack.getItemDamage();
		String type = StatCollector.translateToLocalFormatted(this.getUnlocalizedNameInefficiently(stack) + ".type." + this.getType(meta).suffix + ".name");
		CelestialBedrockOreType oreType = this.getType(meta);
		String body = oreType.body == SolarSystem.Body.DMITRIY ? "\u00A7kDmitriy\u00A7r" : I18nUtil.resolveKey("body." + oreType.body.name);
		return StatCollector.translateToLocalFormatted(this.getUnlocalizedNameInefficiently(stack) + ".grade." + this.getGrade(meta).name().toLowerCase(Locale.US) + ".name", type) + " (" + body + ")";
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean bool) {
		int meta = stack.getItemDamage();
		BedrockOreGrade grade = this.getGrade(meta);
		CelestialBedrockOreType type = this.getType(meta);

		for(ProcessingTrait trait : grade.traits) {
			list.add(I18nUtil.resolveKey(this.getUnlocalizedNameInefficiently(stack) + ".trait." + trait.name().toLowerCase(Locale.US)));
		}

		List<ResolvedCoreEntry> entries = getRankedCoreEntries(type);
		if(entries.isEmpty()) {
			return;
		}

		list.add(EnumChatFormatting.DARK_GRAY + "Core composition:");
		for(ResolvedCoreEntry entry : entries) {
			list.add(EnumChatFormatting.GRAY + "- " + getMaterialDisplayName(entry.material, entry.oreDict) + ": " + formatCoreValue(entry.value));
		}

		BedrockOreOutput primary = getPrimaryOutput(type);
		BedrockOreOutput sulfuric = getSulfuricOutput(type);
		BedrockOreOutput solvent = getSolventOutput(type);
		BedrockOreOutput rad = getRadOutput(type);
		list.add(EnumChatFormatting.DARK_GRAY + "Processing levels:");
		list.add(EnumChatFormatting.GRAY + "Main: " + describeOutput(primary));
		list.add(EnumChatFormatting.GRAY + "Sulfuric: " + describeOutput(sulfuric));
		list.add(EnumChatFormatting.GRAY + "Solvent: " + describeOutput(solvent));
		list.add(EnumChatFormatting.GRAY + "HPS: " + describeOutput(rad));
	}

	public static class BedrockOreOutput {
		public NTMMaterial mat;
		public int amount;
		public BedrockOreOutput(NTMMaterial mat, int amount) {
			this.mat = mat;
			this.amount = amount;
		}
	}

	public static BedrockOreOutput o(NTMMaterial mat, int amount) {
		return new BedrockOreOutput(mat, amount);
	}

	private static class ResolvedCoreEntry {
		public final String oreDict;
		public final float value;
		public final NTMMaterial material;

		private ResolvedCoreEntry(String oreDict, float value, NTMMaterial material) {
			this.oreDict = oreDict;
			this.value = value;
			this.material = material;
		}
	}

	private static CelestialCore getLiveCore(CelestialBedrockOreType type) {
		if(type == null || type.body == null) {
			return null;
		}

		CelestialBody body = type.body.getBody();
		if(body == null) {
			return null;
		}

		return body.getCore();
	}

	private static boolean isCoreDriven(CelestialBedrockOreType type) {
		if(type == null || type.suffix == null) {
			return false;
		}

		CelestialCore core = getLiveCore(type);
		if(core == null) {
			return false;
		}

		return core.getCategory(type.suffix) != null;
	}

	public static boolean isCoreDrivenType(CelestialBedrockOreType type) {
		return isCoreDriven(type);
	}

	public static boolean hasCategoryInCore(CelestialBedrockOreType type) {
		if(type == null || type.suffix == null) {
			return false;
		}

		CelestialCore core = getLiveCore(type);
		if(core == null) {
			// Non-core planets keep legacy bedrock ore visibility.
			return true;
		}

		CoreCategory category = core.getCategory(type.suffix);
		if(category == null || category.entries == null || category.entries.isEmpty()) {
			return false;
		}

		for(CoreEntry entry : category.entries) {
			if(entry != null && entry.value >= MIN_PROCESSABLE_CORE_VALUE) {
				return true;
			}
		}

		return false;
	}

	private static List<ResolvedCoreEntry> getRankedCoreEntries(CelestialBedrockOreType type) {
		if(!isCoreDriven(type)) {
			return Collections.emptyList();
		}

		CelestialCore core = getLiveCore(type);
		CoreCategory category = core.getCategory(type.suffix);
		if(category == null || category.entries == null || category.entries.isEmpty()) {
			return Collections.emptyList();
		}

		ArrayList<ResolvedCoreEntry> entries = new ArrayList<ResolvedCoreEntry>();
		for(CoreEntry entry : category.entries) {
			if(entry == null || entry.oreDict == null || entry.oreDict.isEmpty() || entry.value < MIN_PROCESSABLE_CORE_VALUE) {
				continue;
			}

			entries.add(new ResolvedCoreEntry(entry.oreDict, entry.value, getMaterialFromOreDict(entry.oreDict)));
		}

		if(entries.isEmpty()) {
			return Collections.emptyList();
		}

		Collections.sort(entries, CORE_VALUE_DESC);
		return entries;
	}

	private static String getMaterialToken(String oreDict) {
		if(oreDict == null || oreDict.isEmpty()) return oreDict;
		for(int i = 0; i < oreDict.length(); i++) {
			if(Character.isUpperCase(oreDict.charAt(i))) {
				return oreDict.substring(i);
			}
		}
		return oreDict;
	}

	private static NTMMaterial getMaterialFromOreDict(String oreDict) {
		String token = getMaterialToken(oreDict);
		if(token == null || token.isEmpty()) {
			return null;
		}

		NTMMaterial direct = Mats.matByName.get(token);
		if(direct != null) {
			return direct;
		}

		if("Aluminum".equals(token)) {
			direct = Mats.matByName.get("Aluminium");
			if(direct != null) return direct;
		}
		if("Lanthanum".equals(token)) {
			direct = Mats.matByName.get("Lanthanium");
			if(direct != null) return direct;
		}

		for(Map.Entry<String, NTMMaterial> entry : Mats.matByName.entrySet()) {
			if(entry.getKey() != null && entry.getKey().equalsIgnoreCase(token)) {
				return entry.getValue();
			}
		}

		try {
			List<ItemStack> dictEntries = OreDictionary.getOres(oreDict);
			if(dictEntries != null) {
				for(ItemStack dictStack : dictEntries) {
					if(dictStack == null) continue;
					List<MaterialStack> mats = Mats.getMaterialsFromItem(dictStack);
					if(mats == null) continue;
					for(MaterialStack matStack : mats) {
						if(matStack != null && matStack.material != null) {
							return matStack.material;
						}
					}
				}
			}
		} catch(Exception ignored) { }

		return null;
	}

	private static BedrockOreOutput resolveDynamicOutput(CelestialBedrockOreType type, int stage) {
		BedrockOreOutput fallback = getFallbackOutput(type, stage);
		List<ResolvedCoreEntry> entries = getRankedCoreEntries(type);
		if(entries.isEmpty()) {
			if(isCoreDriven(type)) {
				return null;
			}
			return fallback;
		}

		ResolvedCoreEntry first = entries.get(0);
		ResolvedCoreEntry second = entries.size() > 1 ? entries.get(1) : first;
		ResolvedCoreEntry third = entries.size() > 2 ? entries.get(2) : second;
		ResolvedCoreEntry last = entries.get(entries.size() - 1);
		ResolvedCoreEntry selected;

		if(entries.size() == 1) {
			selected = first;
		} else if(entries.size() == 2) {
			selected = stage <= 1 ? first : last;
		} else if(entries.size() == 3) {
			if(stage <= 1) {
				selected = first;
			} else if(stage == 2) {
				selected = second;
			} else {
				selected = last;
			}
		} else {
			if(stage == 0) {
				selected = first;
			} else if(stage == 1) {
				selected = second;
			} else if(stage == 2) {
				selected = third;
			} else {
				selected = last;
			}
		}

		if(selected == null || selected.material == null) {
			if(isCoreDriven(type)) {
				return null;
			}
			return fallback;
		}
		int amount = getStageAmount(selected.value, stage);

		return new BedrockOreOutput(selected.material, amount);
	}

	private static int getStageAmount(float value, int stage) {
		int amount = value <= MIN_PROCESSABLE_CORE_VALUE ? 1 : Math.max(1, Math.round(value * 36.0F));
		for(int i = 0; i < stage; i++) {
			amount = Math.max(1, (int) Math.floor(amount / 1.4D));
		}
		return amount;
	}

	private static BedrockOreOutput getFallbackOutput(CelestialBedrockOreType type, int stage) {
		if(type == null) return null;
		if(stage == 0) return type.primary;
		if(stage == 1) return type.byproductAcid;
		if(stage == 2) return type.byproductSolvent;
		return type.byproductRad;
	}

	public static BedrockOreOutput getPrimaryOutput(CelestialBedrockOreType type) {
		return resolveDynamicOutput(type, 0);
	}

	public static BedrockOreOutput getSulfuricOutput(CelestialBedrockOreType type) {
		return resolveDynamicOutput(type, 1);
	}

	public static BedrockOreOutput getSolventOutput(CelestialBedrockOreType type) {
		return resolveDynamicOutput(type, 2);
	}

	public static BedrockOreOutput getRadOutput(CelestialBedrockOreType type) {
		return resolveDynamicOutput(type, 3);
	}

	private static String formatCoreValue(float value) {
		return String.format(Locale.US, "%.4f", value);
	}

	private static String getMaterialDisplayName(NTMMaterial material, String oreDict) {
		if(material != null) {
			String translated = StatCollector.translateToLocal(material.getUnlocalizedName());
			if(translated != null && !translated.equals(material.getUnlocalizedName())) {
				return translated;
			}

			if(material.names != null && material.names.length > 0 && material.names[0] != null) {
				return material.names[0];
			}
		}

		String token = getMaterialToken(oreDict);
		return token == null || token.isEmpty() ? "Unknown" : token;
	}

	private static String describeOutput(BedrockOreOutput output) {
		if(output == null || output.mat == null) {
			return "Unknown";
		}
		return getMaterialDisplayName(output.mat, null) + " (" + output.amount + ")";
	}

	// public static enum BedrockOreType {
	// 	//																	primary													sulfuric																		solvent																		radsolvent
	// 	LIGHT_METAL(	0xFFFFFF, 0x353535, "light",		o(MAT_IRON, 9),		o(MAT_COPPER, 9),	o(MAT_TITANIUM, 9),	o(MAT_ALUMINIUM, 5),	o(MAT_NICKEL, 4),		o(MAT_CHLOROCALCITE, 5),	o(MAT_LITHIUM, 5),		o(MAT_SODIUM, 3),		o(MAT_CHLOROCALCITE, 6),	o(MAT_GALLIUM, 6),		o(MAT_SODIUM, 6)),
	// 	HEAVY_METAL(	0x868686, 0x000000, "heavy",		o(MAT_TUNGSTEN, 9),	o(MAT_LEAD, 9),		o(MAT_GOLD, 4),		o(MAT_ZINC, 4),		o(MAT_BERYLLIUM, 3),	o(MAT_TUNGSTEN, 9),			o(MAT_LEAD, 9),			o(MAT_GOLD, 5),			o(MAT_BISMUTH, 1),			o(MAT_BISMUTH, 1),		o(MAT_ARSENIC, 2)),
	// 	RARE_EARTH(		0xE6E6B6, 0x1C1C00, "rare",		o(MAT_COBALT, 5),	o(MAT_RAREEARTH, 5),	o(MAT_BORON, 5),		o(MAT_LANTHANIUM, 3),	o(MAT_NIOBIUM, 4),		o(MAT_NEODYMIUM, 3),		o(MAT_STRONTIUM, 3),	o(MAT_ZIRCONIUM, 3),	o(MAT_NIOBIUM, 5),			o(MAT_NEODYMIUM, 5),	o(MAT_STRONTIUM, 3)),
	// 	ACTINIDE(		0xC1C7BD, 0x2B3227, "actinide",	o(MAT_URANIUM, 4),	o(MAT_THORIUM, 4),	o(MAT_RADIUM, 2),	o(MAT_RADIUM, 2),		o(MAT_POLONIUM, 2),		o(MAT_RADIUM, 2),			o(MAT_RADIUM, 2),		o(MAT_POLONIUM, 2),		o(MAT_TECHNETIUM, 1),		o(MAT_TECHNETIUM, 1),	o(MAT_U238, 1)),
	// 	NON_METAL(		0xAFAFAF, 0x0F0F0F, "nonmetal",	o(MAT_COAL, 9),		o(MAT_SULFUR, 9),	o(MAT_LIGNITE, 9),	o(MAT_KNO, 6),			o(MAT_FLUORITE, 6),		o(MAT_PHOSPHORUS, 5),		o(MAT_FLUORITE, 6),		o(MAT_SULFUR, 6),		o(MAT_CHLOROCALCITE, 6),	o(MAT_SILICON, 2),		o(MAT_SILICON, 2)),
	// 	CRYSTALLINE(	0xE2FFFA, 0x1E8A77, "crystal",	o(MAT_REDSTONE, 9),	o(MAT_CINNABAR, 4),	o(MAT_SODALITE, 9),	o(MAT_ASBESTOS, 6),	o(MAT_DIAMOND, 3),		o(MAT_CINNABAR, 3),			o(MAT_ASBESTOS, 5),		o(MAT_EMERALD, 3),		o(MAT_BORAX, 3),			o(MAT_MOLYSITE, 3),		o(MAT_SODALITE, 9));
	// 	//sediment

	// 	public int light;
	// 	public int dark;
	// 	public String suffix;
	// 	public BedrockOreOutput primary1, primary2;
	// 	public BedrockOreOutput byproductAcid1, byproductAcid2, byproductAcid3;
	// 	public BedrockOreOutput byproductSolvent1, byproductSolvent2, byproductSolvent3;
	// 	public BedrockOreOutput byproductRad1, byproductRad2, byproductRad3;

	// 	private BedrockOreType(int light, int dark, String suffix, BedrockOreOutput p1, BedrockOreOutput p2, BedrockOreOutput bA1, BedrockOreOutput bA2, BedrockOreOutput bA3, BedrockOreOutput bS1, BedrockOreOutput bS2, BedrockOreOutput bS3, BedrockOreOutput bR1, BedrockOreOutput bR2, BedrockOreOutput bR3) {
	// 		this.light = light;
	// 		this.dark = dark;
	// 		this.suffix = suffix;
	// 		this.primary1 = p1; this.primary2 = p2;
	// 		this.byproductAcid1 = bA1; this.byproductAcid2 = bA2; this.byproductAcid3 = bA3;
	// 		this.byproductSolvent1 = bS1; this.byproductSolvent2 = bS2; this.byproductSolvent3 = bS3;
	// 		this.byproductRad1 = bR1; this.byproductRad2 = bR2; this.byproductRad3 = bR3;
	// 	}
	// }

	public static class CelestialBedrockOreType {
		public SolarSystem.Body body;
		public int index;

		public int light;
		public int dark;
		public String suffix;
		public String textureSuffix;
		public BedrockOreOutput primary;
		public BedrockOreOutput byproductAcid;
		public BedrockOreOutput byproductSolvent;
		public BedrockOreOutput byproductRad;
	}

	public static class CelestialBedrockOre {
		// TODO: rebalance cryolite, Laythe gets regular aluminium for now
		static {
			//					primary						sulfuric					solvent						dcm
			register(
				SolarSystem.Body.KERBIN,
				T("light",		o(MAT_IRON, 18),			o(MAT_COPPER, 9),			o(MAT_CRYOLITE, 6),			o(MAT_SODIUM, 3)),
				T("heavy",		o(MAT_TUNGSTEN, 18),		o(MAT_TUNGSTEN, 9),			o(MAT_TUNGSTEN, 6),			o(MAT_ZINC, 3)),
				T("nonmetal",	o(MAT_COAL, 18),			o(MAT_LIGNITE, 9),			o(MAT_SULFUR, 6),			o(MAT_KNO, 3)),
				T("crystal",	o(MAT_REDSTONE, 18),		o(MAT_ASBESTOS, 9),			o(MAT_DIAMOND, 6),			o(MAT_EMERALD, 3))
			);

			register(
				SolarSystem.Body.MUN,
				T("light",		o(MAT_LITHIUM, 18),			o(MAT_IRON, 9),				o(MAT_SODIUM, 6),			o(MAT_CHLOROCALCITE, 3)),
				T("heavy",		o(MAT_LEAD, 18),			o(MAT_ZINC, 9),				o(MAT_GOLD, 6),				o(MAT_BISMUTH, 3)),
				T("rare",		o(MAT_COBALT, 18),			o(MAT_RAREEARTH, 9),		o(MAT_NEODYMIUM, 6),		o(MAT_STRONTIUM, 3)),
				T("nonmetal",	o(MAT_SULFUR, 18),			o(MAT_FLUORITE, 9),			o(MAT_KNO, 6),				o(MAT_SILICON, 3)),
				T("crystal",	o(MAT_QUARTZ, 18),			o(MAT_SODALITE, 9),			o(MAT_EMERALD, 6),			o(MAT_CINNABAR, 3))
			);

			register(
				SolarSystem.Body.MINMUS,
				T("light",		o(MAT_COPPER, 18),			o(MAT_TITANIUM, 9),			o(MAT_CHLOROCALCITE, 6),	o(MAT_COPPER, 3)),
				T("heavy",		o(MAT_LEAD, 18),			o(MAT_GOLD, 9),				o(MAT_TUNGSTEN, 6),			o(MAT_BISMUTH, 3)),
				T("rare",		o(MAT_ZIRCONIUM, 18),		o(MAT_BORON, 9),			o(MAT_COBALT, 6),			o(MAT_STRONTIUM, 3)),
				T("nonmetal",	o(MAT_SULFUR, 18),			o(MAT_KNO, 9),				o(MAT_FLUORITE, 6),			o(MAT_SILICON, 3)),
				T("crystal",	o(MAT_EMERALD, 18),			o(MAT_SODALITE, 9),			o(MAT_DIAMOND, 6),			o(MAT_EMERALD, 3))
			);

			register(
				SolarSystem.Body.DUNA,
				T("light",		o(MAT_IRON, 18),			o(MAT_NICKEL, 9),			o(MAT_TITANIUM, 6),			o(MAT_CHLOROCALCITE, 3)),
				T("heavy",		o(MAT_BERYLLIUM, 18),		o(MAT_TUNGSTEN, 9),			o(MAT_ZINC, 6),				o(MAT_BISMUTH, 3)),
				T("rare",		o(MAT_RAREEARTH, 18),		o(MAT_BORON, 9),			o(MAT_ZIRCONIUM, 6),		o(MAT_STRONTIUM, 3)),
				T("actinide",	o(MAT_THORIUM, 18),			o(MAT_RADIUM, 9),			o(MAT_POLONIUM, 6),			o(MAT_U233, 3)),
				T("nonmetal",	o(MAT_FLUORITE, 18),		o(MAT_SULFUR, 9),			o(MAT_SILICON, 6),			o(MAT_PHOSPHORUS, 3)),
				T("crystal",	o(MAT_REDSTONE, 18),		o(MAT_CINNABAR, 9),			o(MAT_DIAMOND, 6),			o(MAT_MOLYSITE, 3))
			);

			register(
				SolarSystem.Body.MOHO,
				T("light",		o(MAT_TITANIUM, 18),		o(MAT_CHLOROCALCITE, 9),	o(MAT_NICKEL, 6),			o(MAT_LITHIUM, 3)),
				T("heavy",		o(MAT_GOLD, 18),			o(MAT_ZINC, 9),				o(MAT_LEAD, 6),				o(MAT_BISMUTH, 3)),
				T("rare",		o(MAT_NEODYMIUM, 18),		o(MAT_ZIRCONIUM, 9),		o(MAT_BROMINE, 6),			o(MAT_STRONTIUM, 3)),
				T("actinide",	o(MAT_AUSTRALIUM, 18),		o(MAT_AUSTRALIUM, 9),		o(MAT_TASMANITE, 6),		o(MAT_AYERITE, 3)),
				T("nonmetal",	o(MAT_GLOWSTONE, 18),		o(MAT_PHOSPHORUS, 9),		o(MAT_SULFUR, 6),			o(MAT_PHOSPHORUS_W, 3)),
				T("crystal",	o(MAT_CINNABAR, 18),		o(MAT_REDSTONE, 9),			o(MAT_QUARTZ, 6),			o(MAT_MOLYSITE, 3))
			);

			register(
				SolarSystem.Body.DRES,
				T("light",		o(MAT_NICKEL, 18),			o(MAT_TITANIUM, 9),			o(MAT_CADMIUM, 6),			o(MAT_GALLIUM, 3)),
				T("heavy",		o(MAT_ZINC, 18),			o(MAT_GOLD, 9),				o(MAT_BISMUTH, 6),			o(MAT_ARSENIC, 3)),
				T("rare",		o(MAT_TANTALIUM, 18),		o(MAT_LANTHANIUM, 9),		o(MAT_NIOBIUM, 6),			o(MAT_STRONTIUM, 3)),
				T("actinide",	o(MAT_URANIUM, 18),			o(MAT_RADIUM, 9),			o(MAT_TECHNETIUM, 6),		o(MAT_U238, 3)),
				T("nonmetal",	o(MAT_SILICON, 18),			o(MAT_SILICON, 9),			o(MAT_FLUORITE, 6),			o(MAT_FLUORITE, 3)),
				T("crystal",	o(MAT_DIAMOND, 18),			o(MAT_BORAX, 9),			o(MAT_MOLYSITE, 6),			o(MAT_MOLYSITE, 3))
			);

			register(
				SolarSystem.Body.EVE,
				T("light",		o(MAT_SODIUM, 18),			o(MAT_CHLOROCALCITE, 9),	o(MAT_IRON, 6),				o(MAT_CO60, 3)),
				T("heavy",		o(MAT_TUNGSTEN, 18),		o(MAT_LEAD, 9),				o(MAT_ARSENIC, 6),			o(MAT_PB209, 3)),
				T("rare",		o(MAT_NIOBIUM, 18),			o(MAT_STRONTIUM, 9),		o(MAT_IODINE, 6),			o(MAT_AU198, 3)),
				T("actinide",	o(MAT_PLUTONIUM, 18),		o(MAT_POLONIUM, 9),			o(MAT_NEPTUNIUM, 6),		o(MAT_PU239, 3)),
				T("schrabidic",	o(MAT_SCHRABIDIUM, 18),		o(MAT_SOLINIUM, 9),			o(MAT_GHIORSIUM, 6),		o(MAT_SCHRABIDIUM, 3)),
				T("crystal",	o(MAT_SODALITE, 18),		o(MAT_MOLYSITE, 9),			o(MAT_DIAMOND, 6),			o(MAT_BORAX, 3))
			);

			register(
				SolarSystem.Body.IKE,
				T("light",		o(MAT_COPPER, 18),			o(MAT_BAUXITE, 9),			o(MAT_NICKEL, 6),			o(MAT_SODIUM, 3)),
				T("heavy",		o(MAT_LEAD, 18),			o(MAT_ZINC, 9),				o(MAT_GOLD, 6),				o(MAT_ARSENIC, 3)),
				T("rare",		o(MAT_BORON, 18),			o(MAT_NEODYMIUM, 9),		o(MAT_STRONTIUM, 6),		o(MAT_LANTHANIUM, 3)),
				T("actinide", "hazard",	o(MAT_URANIUM, 18),			o(MAT_U238, 9),				o(MAT_PLUTONIUM, 6),		o(MAT_TECHNETIUM, 3))
			);

			register(
				SolarSystem.Body.LAYTHE,
				T("light",		o(MAT_ALUMINIUM, 18),		o(MAT_TITANIUM, 9),			o(MAT_GALLIUM, 6),			o(MAT_HAFNIUM, 3)),
				T("heavy",		o(MAT_BERYLLIUM, 18),		o(MAT_TUNGSTEN, 9),			o(MAT_LEAD, 6),				o(MAT_ARSENIC, 3)),
				T("rare",		o(MAT_RAREEARTH, 18),		o(MAT_NEODYMIUM, 9),		o(MAT_STRONTIUM, 6),		o(MAT_NIOBIUM, 3)),
				T("actinide",	o(MAT_URANIUM, 18),			o(MAT_THORIUM, 9),			o(MAT_POLONIUM, 6),			o(MAT_U235, 3)),
				T("nonmetal",	o(MAT_CHLOROCALCITE, 18),	o(MAT_COAL, 9),				o(MAT_FLUORITE, 6),			o(MAT_SILICON, 3)),
				T("crystal",	o(MAT_ASBESTOS, 18),		o(MAT_SODALITE, 9),			o(MAT_DIAMOND, 6),			o(MAT_SODALITE, 3))
			);
			register(
				SolarSystem.Body.TEKTO,
				T("light",		o(MAT_TITANIUM, 18),		o(MAT_COPPER, 9),			o(MAT_NICKEL, 6),			o(MAT_LITHIUM, 3)),
				T("heavy",		o(MAT_BERYLLIUM, 18),		o(MAT_LEAD, 9),				o(MAT_ZINC, 6),				o(MAT_ZINC, 3)),
				T("rare",		o(MAT_BORON, 18),			o(MAT_RAREEARTH, 9),		o(MAT_TANTALIUM, 6),		o(MAT_BISMUTH, 3)),
				T("actinide",	o(MAT_URANIUM, 18),			o(MAT_NEPTUNIUM, 9),		o(MAT_RADIUM, 6),			o(MAT_TECHNETIUM, 3)),
				T("crystal",	o(MAT_EMERALD, 18),			o(MAT_SILICON, 9),			o(MAT_MOLYSITE, 6),			o(MAT_BORAX, 3)),
				T("nonmetal", "plastic",	o(MAT_POLYMER, 18),			o(MAT_RUBBER, 9),			o(MAT_SEMTEX, 6),			o(MAT_PVC, 3))
			);
			register(
				SolarSystem.Body.DMITRIY,
				T("light",		o(MAT_DESH, 18),			o(MAT_DESH, 9),				o(MAT_DESH, 6),				o(MAT_DESH, 3)),
				T("heavy",		o(MAT_SATURN, 18),			o(MAT_SATURN, 9),			o(MAT_YHARONITE, 6),		o(MAT_ETHYROITE, 3)),
				T("schrabidic",	o(MAT_SCHRABIDIUM, 18),		o(MAT_SCHRABIDIUM, 9),		o(MAT_EUPHEMIUM, 6),		o(MAT_EUPHEMIUM, 3)),
				T("living", "schrabidic", 0x2A2A2A, 0x0A0A0A,	o(MAT_RIFT, 18),			o(MAT_RIFT, 9),				o(MAT_ABYSS, 6),			o(MAT_ABYSS, 3))
			);
			/*
			register(
				SolarSystem.Body.THATMO,
				T("light",		o(MAT_CRYOLITE, 24),		o(MAT_TITANIUM, 12),			o(MAT_GALLIUM, 6),		o(MAT_HAFNIUM, 3)),
				T("heavy",		o(MAT_BERYLLIUM, 24),	o(MAT_TUNGSTEN, 12),			o(MAT_LEAD, 6),			o(MAT_ARSENIC, 3)),
				T("rare",		o(MAT_RAREEARTH, 24),	o(MAT_NEODYMIUM, 12),		o(MAT_STRONTIUM, 6),		o(MAT_NIOBIUM, 3)),
				T("actinide",	o(MAT_URANIUM, 24),		o(MAT_THORIUM, 12),			o(MAT_POLONIUM, 6),		o(MAT_U235, 3)),
				T("nonmetal",	o(MAT_CHLOROCALCITE, 24),o(MAT_COAL, 12),				o(MAT_FLUORITE, 6),		o(MAT_SILICON, 3)),
				T("crystal",		o(MAT_ASBESTOS, 24),		o(MAT_SODALITE, 12),			o(MAT_DIAMOND, 6),		o(MAT_SODALITE, 3))
				);
				*/

			ensureCoreCategoryCoverage();
		}

		public CelestialBedrockOreType[] types;

		private CelestialBedrockOre(CelestialBedrockOreType... types) {
			this.types = types;
		}

		private static HashMap<SolarSystem.Body, CelestialBedrockOre> oreMap;
		private static List<CelestialBedrockOreType> oreTypes;

		private static void register(SolarSystem.Body body, CelestialBedrockOreType... types) {
			if(oreMap == null) oreMap = new HashMap<>();
			if(oreTypes == null) oreTypes = new ArrayList<>();
			oreMap.put(body, new CelestialBedrockOre(types));
			for(CelestialBedrockOreType type : types) {
				type.body = body;
				oreTypes.add(type);
			}
		}

		private static String[] getCoreCategorySuffixes() {
			return new String[] {
				CelestialCore.CAT_LIGHT,
				CelestialCore.CAT_HEAVY,
				CelestialCore.CAT_RARE,
				CelestialCore.CAT_ACTINIDE,
				CelestialCore.CAT_NONMETAL,
				CelestialCore.CAT_CRYSTAL,
				CelestialCore.CAT_SCHRABIDIC,
				CelestialCore.CAT_LIVING
			};
		}

		private static void ensureCoreCategoryCoverage() {
			if(oreMap == null || oreTypes == null) return;

			for(SolarSystem.Body body : SolarSystem.Body.values()) {
				if(body == SolarSystem.Body.ORBIT) continue;
				CelestialBedrockOre ore = oreMap.get(body);
				if(ore == null || ore.types == null) continue;

				List<CelestialBedrockOreType> additions = new ArrayList<CelestialBedrockOreType>();
				for(String categorySuffix : getCoreCategorySuffixes()) {
					if(hasTypeWithSuffix(ore.types, categorySuffix)) {
						continue;
					}

					CelestialBedrockOreType added = makeCoreFallbackType(categorySuffix);
					added.body = body;
					additions.add(added);
					oreTypes.add(added);
				}

				if(!additions.isEmpty()) {
					CelestialBedrockOreType[] merged = new CelestialBedrockOreType[ore.types.length + additions.size()];
					System.arraycopy(ore.types, 0, merged, 0, ore.types.length);
					for(int i = 0; i < additions.size(); i++) {
						merged[ore.types.length + i] = additions.get(i);
					}
					ore.types = merged;
				}
			}
		}

		private static boolean hasTypeWithSuffix(CelestialBedrockOreType[] types, String suffix) {
			if(types == null || suffix == null) return false;
			for(CelestialBedrockOreType type : types) {
				if(type != null && suffix.equals(type.suffix)) {
					return true;
				}
			}
			return false;
		}

		private static String getTextureSuffixForCategory(String categorySuffix) {
			if(CelestialCore.CAT_LIVING.equals(categorySuffix)) return CelestialCore.CAT_SCHRABIDIC;
			return categorySuffix;
		}

		private static CelestialBedrockOreType makeCoreFallbackType(String categorySuffix) {
			if(CelestialCore.CAT_HEAVY.equals(categorySuffix)) {
				return T(categorySuffix, getTextureSuffixForCategory(categorySuffix), o(MAT_TUNGSTEN, 18), o(MAT_LEAD, 9), o(MAT_GOLD, 6), o(MAT_ZINC, 3));
			}
			if(CelestialCore.CAT_RARE.equals(categorySuffix)) {
				return T(categorySuffix, getTextureSuffixForCategory(categorySuffix), o(MAT_COBALT, 18), o(MAT_RAREEARTH, 9), o(MAT_NEODYMIUM, 6), o(MAT_STRONTIUM, 3));
			}
			if(CelestialCore.CAT_ACTINIDE.equals(categorySuffix)) {
				return T(categorySuffix, getTextureSuffixForCategory(categorySuffix), o(MAT_URANIUM, 18), o(MAT_THORIUM, 9), o(MAT_POLONIUM, 6), o(MAT_U238, 3));
			}
			if(CelestialCore.CAT_NONMETAL.equals(categorySuffix)) {
				return T(categorySuffix, getTextureSuffixForCategory(categorySuffix), o(MAT_COAL, 18), o(MAT_SULFUR, 9), o(MAT_FLUORITE, 6), o(MAT_SILICON, 3));
			}
			if(CelestialCore.CAT_CRYSTAL.equals(categorySuffix)) {
				return T(categorySuffix, getTextureSuffixForCategory(categorySuffix), o(MAT_REDSTONE, 18), o(MAT_ASBESTOS, 9), o(MAT_DIAMOND, 6), o(MAT_EMERALD, 3));
			}
			if(CelestialCore.CAT_SCHRABIDIC.equals(categorySuffix)) {
				return T(categorySuffix, getTextureSuffixForCategory(categorySuffix), o(MAT_SCHRABIDIUM, 18), o(MAT_SOLINIUM, 9), o(MAT_GHIORSIUM, 6), o(MAT_SCHRABIDIUM, 3));
			}
			if(CelestialCore.CAT_LIVING.equals(categorySuffix)) {
				return T(categorySuffix, getTextureSuffixForCategory(categorySuffix), o(MAT_RIFT, 18), o(MAT_RIFT, 9), o(MAT_ABYSS, 6), o(MAT_ABYSS, 3));
			}
			return T(CelestialCore.CAT_LIGHT, getTextureSuffixForCategory(CelestialCore.CAT_LIGHT), o(MAT_IRON, 18), o(MAT_COPPER, 9), o(MAT_TITANIUM, 6), o(MAT_SODIUM, 3));
		}

		public static CelestialBedrockOre get(SolarSystem.Body body) {
			return oreMap.get(body);
		}

		public static List<CelestialBedrockOreType> getAllTypes() {
			return oreTypes;
		}

		private static int index;

		private static CelestialBedrockOreType T(String suffix, BedrockOreOutput primary, BedrockOreOutput byproductAcid, BedrockOreOutput byproductSolvent, BedrockOreOutput byproductRad) {
			return T(suffix, suffix, primary, byproductAcid, byproductSolvent, byproductRad);
		}

		private static CelestialBedrockOreType T(String suffix, String textureSuffix, BedrockOreOutput primary, BedrockOreOutput byproductAcid, BedrockOreOutput byproductSolvent, BedrockOreOutput byproductRad) {
			CelestialBedrockOreType type = new CelestialBedrockOreType();
			type.index = index++;

			type.suffix = suffix;
			type.textureSuffix = textureSuffix;
			type.primary = primary;
			type.byproductAcid = byproductAcid;
			type.byproductSolvent = byproductSolvent;
			type.byproductRad = byproductRad;

			// Colours are autogenerated because otherwise I'd have to manually specify like 80 different colours
			type.light = getAverageColor(false, primary, byproductAcid, byproductSolvent, byproductRad);
			type.dark = getAverageColor(true, primary, byproductAcid, byproductSolvent, byproductRad);

			return type;
		}

		private static CelestialBedrockOreType T(String suffix, String textureSuffix, int light, int dark, BedrockOreOutput primary, BedrockOreOutput byproductAcid, BedrockOreOutput byproductSolvent, BedrockOreOutput byproductRad) {
			CelestialBedrockOreType type = T(suffix, textureSuffix, primary, byproductAcid, byproductSolvent, byproductRad);
			type.light = light;
			type.dark = dark;
			return type;
		}

		private static int getAverageColor(boolean dark, BedrockOreOutput... outputs) {
			int r = 0, g = 0, b = 0;

			for(BedrockOreOutput output : outputs) {
				int color = dark ? output.mat.solidColorDark : output.mat.solidColorLight;
				r += color >> 16 & 255;
				g += color >> 8 & 255;
				b += color & 255;
			}

			r /= outputs.length;
			g /= outputs.length;
			b /= outputs.length;

			return r << 16 | g << 8 | b;
		}
	}

	public static MaterialStack toFluid(BedrockOreOutput o, double amount) {
		if(o == null || o.mat == null) {
			return null;
		}
		if(o.mat != null && o.mat.smeltable == SmeltingBehavior.SMELTABLE) {
			return new MaterialStack(o.mat, (int) Math.ceil(MaterialShapes.FRAGMENT.q(o.amount) * amount));
		}
		return null;
	}

	public static ItemStack extract(BedrockOreOutput o, double amount) {
		if(o == null || o.mat == null) {
			return new ItemStack(ModItems.dust);
		}
		return new ItemStack(ModItems.bedrock_ore_fragment, Math.min((int) Math.ceil(o.amount * amount), 64), o.mat.id);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getColorFromItemStack(ItemStack stack, int pass) {
		if(pass != 0) return 0xFFFFFF;
		BedrockOreGrade grade = this.getGrade(stack.getItemDamage());
		return grade.tint;
	}

	public static final int none = 0xFFFFFF;
	public static final int roasted = 0xCFCFCF;
	public static final int arc = 0xC3A2A2;
	public static final int washed = 0xDBE2CB;

	public static enum ProcessingTrait {
		ROASTED,
		ARC,
		WASHED,
		CENTRIFUGED,
		SULFURIC,
		SOLVENT,
		RAD
	}

	public static enum BedrockOreGrade {
		BASE(none, "base"),												//from the slopper
		BASE_ROASTED(roasted, "base", ROASTED),							//optional combination oven step, yields vitriol
		BASE_WASHED(washed, "base", WASHED),							//primitive-ass acidizer with water
		PRIMARY(none, "primary", CENTRIFUGED),							//centrifuging for more primary
		PRIMARY_ROASTED(roasted, "primary", ROASTED),					//optional comboven
		PRIMARY_SULFURIC(0xFFFFD3, "primary", SULFURIC),				//sulfuric acid
		PRIMARY_NOSULFURIC(0xD3D4FF, "primary", CENTRIFUGED, SULFURIC),	//from centrifuging, sulfuric byproduct removed
		PRIMARY_SOLVENT(0xD3F0FF, "primary", SOLVENT),					//solvent
		PRIMARY_NOSOLVENT(0xFFDED3, "primary", CENTRIFUGED, SOLVENT),	//solvent byproduct removed
		PRIMARY_RAD(0xECFFD3, "primary", RAD),							//radsolvent
		PRIMARY_NORAD(0xEBD3FF, "primary", CENTRIFUGED, RAD),			//radsolvent byproduct removed
		PRIMARY_FIRST(0xFFD3D4, "primary", CENTRIFUGED),				//higher first material yield
		PRIMARY_SECOND(0xD3FFEB, "primary", CENTRIFUGED),				//higher second material yield
		CRUMBS(none, "crumbs", CENTRIFUGED),							//endpoint for primary, recycling

		SULFURIC_BYPRODUCT(none, "sulfuric", CENTRIFUGED, SULFURIC),	//from centrifuging
		SULFURIC_ROASTED(roasted, "sulfuric", ROASTED, SULFURIC),		//comboven again
		SULFURIC_ARC(arc, "sulfuric", ARC, SULFURIC),					//alternate step
		SULFURIC_WASHED(washed, "sulfuric", WASHED, SULFURIC),			//sulfuric endpoint

		SOLVENT_BYPRODUCT(none, "solvent", CENTRIFUGED, SOLVENT),		//from centrifuging
		SOLVENT_ROASTED(roasted, "solvent", ROASTED, SOLVENT),			//comboven again
		SOLVENT_ARC(arc, "solvent", ARC, SOLVENT),						//alternate step
		SOLVENT_WASHED(washed, "solvent", WASHED, SOLVENT),				//solvent endpoint

		RAD_BYPRODUCT(none, "rad", CENTRIFUGED, RAD),					//from centrifuging
		RAD_ROASTED(roasted, "rad", ROASTED, RAD),						//comboven again
		RAD_ARC(arc, "rad", ARC, RAD),									//alternate step
		RAD_WASHED(washed, "rad", WASHED, RAD);							//rad endpoint

		public int tint;
		public String prefix;
		public ProcessingTrait[] traits;

		private BedrockOreGrade(int tint, String prefix, ProcessingTrait... traits) {
			this.tint = tint;
			this.prefix = prefix;
			this.traits = traits;
		}
	}

	public static ItemStack make(BedrockOreGrade grade, CelestialBedrockOreType type) {
		return make(grade, type, 1);
	}

	public static ItemStack make(BedrockOreGrade grade, CelestialBedrockOreType type, int amount) {
		return new ItemStack(ModItems.bedrock_ore, amount, grade.ordinal() << 8 | type.index);
	}

	public BedrockOreGrade getGrade(int meta) {
		return EnumUtil.grabEnumSafely(BedrockOreGrade.class, meta >> 8);
	}

	public CelestialBedrockOreType getType(int meta) {
		return CelestialBedrockOre.oreTypes.get((meta & 255) % CelestialBedrockOre.oreTypes.size());
	}
}

