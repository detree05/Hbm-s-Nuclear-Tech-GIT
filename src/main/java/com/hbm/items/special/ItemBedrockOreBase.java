package com.hbm.items.special;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.hbm.dim.CelestialBody;
import com.hbm.dim.CelestialCore;
import com.hbm.dim.CelestialCore.CoreCategory;
import com.hbm.dim.CelestialCore.CoreEntry;
import com.hbm.dim.SolarSystem;
import com.hbm.items.special.ItemBedrockOreNew.CelestialBedrockOre;
import com.hbm.items.special.ItemBedrockOreNew.CelestialBedrockOreType;
import com.hbm.items.tool.ItemOreDensityScanner;
import com.hbm.util.i18n.I18nUtil;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraft.world.gen.NoiseGeneratorPerlin;

public class ItemBedrockOreBase extends Item {

	private static final String[] TOOLTIP_CATEGORY_ORDER = new String[] {
		CelestialCore.CAT_LIGHT,
		CelestialCore.CAT_HEAVY,
		CelestialCore.CAT_RARE,
		CelestialCore.CAT_ACTINIDE,
		CelestialCore.CAT_NONMETAL,
		CelestialCore.CAT_CRYSTAL,
		CelestialCore.CAT_SCHRABIDIC,
		CelestialCore.CAT_LIVING
	};

	public ItemBedrockOreBase() {
		this.setHasSubtypes(true);
	}

	public static double getOreAmount(ItemStack stack, CelestialBedrockOreType type) {
		if(!stack.hasTagCompound()) return 1;
		NBTTagCompound data = stack.getTagCompound();
		return data.getDouble(type.suffix);
	}

	public static SolarSystem.Body getOreBody(ItemStack stack) {
		if(stack.getItemDamage() >= SolarSystem.Body.values().length || stack.getItemDamage() <= 0) return SolarSystem.Body.KERBIN;
		return SolarSystem.Body.values()[stack.getItemDamage()];
	}

	public static void setOreAmount(World world, ItemStack stack, int x, int z, double mult) {
		if(!stack.hasTagCompound()) stack.stackTagCompound = new NBTTagCompound();
		NBTTagCompound data = stack.getTagCompound();

		SolarSystem.Body body = CelestialBody.getEnum(world);

		stack.setItemDamage(body.ordinal());

		for(CelestialBedrockOreType type : CelestialBedrockOre.get(body).types) {
			data.setDouble(type.suffix, getOreLevel(world, x, z, type) * mult);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean bool) {
		SolarSystem.Body body = getOreBody(stack);
		list.add("Mined on: " + I18nUtil.resolveKey("body." + body.name));

		for(CelestialBedrockOreType type : getTooltipOrderedTypes(body)) {
			double amount = getOreAmount(stack, type);
			if(amount <= 0.0D) continue;
			String typeName = StatCollector.translateToLocalFormatted("item.bedrock_ore.type." + type.suffix + ".name");
			list.add(typeName + ": " + ((int) (amount * 100)) / 100D + " (" + ItemOreDensityScanner.getColor(amount) + StatCollector.translateToLocalFormatted(ItemOreDensityScanner.translateDensity(amount)) + EnumChatFormatting.GRAY + ")");
		}
	}

	private static List<CelestialBedrockOreType> getTooltipOrderedTypes(SolarSystem.Body body) {
		CelestialBedrockOre ore = CelestialBedrockOre.get(body);
		if(ore == null || ore.types == null || ore.types.length == 0) {
			return Collections.emptyList();
		}

		Map<String, CelestialBedrockOreType> bySuffix = new HashMap<String, CelestialBedrockOreType>();
		for(CelestialBedrockOreType type : ore.types) {
			if(type == null || type.suffix == null || bySuffix.containsKey(type.suffix)) continue;
			bySuffix.put(type.suffix, type);
		}

		ArrayList<CelestialBedrockOreType> ordered = new ArrayList<CelestialBedrockOreType>();
		for(String suffix : TOOLTIP_CATEGORY_ORDER) {
			CelestialBedrockOreType type = bySuffix.remove(suffix);
			if(type != null) {
				ordered.add(type);
			}
		}

		if(!bySuffix.isEmpty()) {
			ArrayList<CelestialBedrockOreType> extras = new ArrayList<CelestialBedrockOreType>(bySuffix.values());
			Collections.sort(extras, new Comparator<CelestialBedrockOreType>() {
				@Override
				public int compare(CelestialBedrockOreType left, CelestialBedrockOreType right) {
					if(left == null || left.suffix == null) return -1;
					if(right == null || right.suffix == null) return 1;
					return left.suffix.compareTo(right.suffix);
				}
			});
			ordered.addAll(extras);
		}

		return ordered;
	}

	public static double getOreLevel(World world, int x, int z, CelestialBedrockOreType type) {
		long seed = world.getSeed() + world.provider.dimensionId;

		NoiseGeneratorPerlin level = getGenerator(seed);
		NoiseGeneratorPerlin ore = getGenerator(seed - 4096 + type.index);

		double scale = 0.01D;
		double compositionMultiplier = getCoreCategoryMultiplier(world, type);
		if(compositionMultiplier <= 0.0D) {
			return 0.0D;
		}

		return MathHelper.clamp_double(Math.abs(level.func_151601_a(x * scale, z * scale) * ore.func_151601_a(x * scale, z * scale)) * 0.05 * compositionMultiplier, 0, 2);
	}

	public static boolean hasCategoryInCore(World world, CelestialBedrockOreType type) {
		return getCoreCategoryMultiplier(world, type) > 0.0D;
	}

	private static double getCoreCategoryMultiplier(World world, CelestialBedrockOreType type) {
		if(world == null || type == null || type.suffix == null) return 1.0D;

		CelestialCore core = CelestialBody.getCore(world);
		if(core == null) {
			return 1.0D;
		}

		CoreCategory category = core.getCategory(type.suffix);
		if(category == null || category.entries == null || category.entries.isEmpty()) {
			return 0.0D;
		}

		for(CoreEntry entry : category.entries) {
			if(entry != null && entry.value >= ItemBedrockOreNew.MIN_PROCESSABLE_CORE_VALUE) {
				return 1.0D;
			}
		}

		return 0.0D;
	}

	private static Map<Long, NoiseGeneratorPerlin> generators = new HashMap<>();

	private static NoiseGeneratorPerlin getGenerator(long seed) {
		return generators.computeIfAbsent(seed, key -> new NoiseGeneratorPerlin(new Random(seed), 4));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item item, CreativeTabs tab, List list) {

		for(SolarSystem.Body body : SolarSystem.Body.values()) {
			if(body == SolarSystem.Body.ORBIT) continue;
			list.add(new ItemStack(item, 1, body.ordinal()));
		}
	}

}
