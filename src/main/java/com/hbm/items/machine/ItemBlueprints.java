package com.hbm.items.machine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.hbm.dim.CelestialCore;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModItems;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.oredict.OreDictionary;

public class ItemBlueprints extends Item {

	public static final String CORE_BLUEPRINT_POOL_PREFIX = "coremanip.";
	private static final String NBT_KEY_POOL = "pool";
	private static final String NBT_KEY_CORE_CATEGORY = "coreCategory";
	private static final String NBT_KEY_CORE_MATERIALS = "coreMaterials";

	@SideOnly(Side.CLIENT) protected IIcon iconDiscover;
	@SideOnly(Side.CLIENT) protected IIcon iconSecret;
	@SideOnly(Side.CLIENT) protected IIcon icon528;

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister reg) {
		super.registerIcons(reg);
		this.iconDiscover = reg.registerIcon(this.getIconString() + "_discover");
		this.iconSecret = reg.registerIcon(this.getIconString() + "_secret");
		this.icon528 = reg.registerIcon(this.getIconString() + "_528");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIconIndex(ItemStack stack) {
		return this.getIcon(stack, 0);
	}

	@Override
	public IIcon getIcon(ItemStack stack, int pass) {
		
		if(stack.hasTagCompound()) {
			String poolName = stack.stackTagCompound.getString("pool");
			if(poolName == null) return this.itemIcon;
			if(poolName.startsWith(GenericRecipes.POOL_PREFIX_DISCOVER)) return this.iconDiscover; // beige
			if(poolName.startsWith(GenericRecipes.POOL_PREFIX_SECRET)) return this.iconSecret; // black
			if(poolName.startsWith(GenericRecipes.POOL_PREFIX_528)) return this.icon528; // grey
		}
		
		return this.itemIcon; // blue
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item item, CreativeTabs tab, List list) {
		for(Entry<String, List<String>> pool : GenericRecipes.blueprintPools.entrySet()) {
			String poolName = pool.getKey();
			if(!poolName.startsWith(GenericRecipes.POOL_PREFIX_SECRET)) list.add(make(poolName));
		}
	}
	
	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		if(world.isRemote) return stack;
		if(!stack.hasTagCompound()) return stack;
		
		String poolName = stack.stackTagCompound.getString("pool");
		
		if(poolName.startsWith(GenericRecipes.POOL_PREFIX_SECRET)) return stack;
		if(!player.inventory.hasItem(Items.paper)) return stack;
		
		player.inventory.consumeInventoryItem(Items.paper);
		player.swingItem();
		
		ItemStack copy = stack.copy();
		copy.stackSize = 1;
		
		if(!player.capabilities.isCreativeMode) {
			if(stack.stackSize < stack.getMaxStackSize()) {
				stack.stackSize++;
				return stack;
			}
			
			if(!player.inventory.addItemStackToInventory(copy)) {
				copy = stack.copy();
				copy.stackSize = 1;
				player.dropPlayerItemWithRandomChoice(copy, false);
			}
			
			player.inventoryContainer.detectAndSendChanges();
		} else {
			player.dropPlayerItemWithRandomChoice(copy, false);
		}
		
		return stack;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean ext) {

		if(isCoreManipulatorBlueprint(stack)) {
			String category = getCoreManipulatorCategory(stack);
			if(category != null) {
				list.add(EnumChatFormatting.AQUA + "Core Category: " + EnumChatFormatting.WHITE + category);
			}
			List<String> materials = getCoreManipulatorMaterials(stack);
			if(materials.isEmpty()) {
				list.add(EnumChatFormatting.GRAY + "No unlocked materials");
			} else {
				list.add(EnumChatFormatting.YELLOW + "Unlocked materials:");
				int maxLines = Math.min(6, materials.size());
				for(int i = 0; i < maxLines; i++) {
					String material = materials.get(i);
					list.add(EnumChatFormatting.GRAY + "- " + formatCoreMaterialLabel(material));
				}
				if(materials.size() > maxLines) {
					list.add(EnumChatFormatting.GRAY + "...and " + (materials.size() - maxLines) + " more");
				}
			}
			return;
		}
		
		if(!stack.hasTagCompound()) {
			return;
		}
		
		String poolName = stack.stackTagCompound.getString(NBT_KEY_POOL);
		List<String> pool = GenericRecipes.blueprintPools.get(poolName);
		
		if(pool == null || pool.isEmpty()) {
			return;
		}
		if(poolName.startsWith(GenericRecipes.POOL_PREFIX_SECRET)) {
			list.add(EnumChatFormatting.RED + "Cannot be copied!");
		} else {
			list.add(EnumChatFormatting.YELLOW + "Right-click to copy (requires paper)");
		}
		
		for(String name : pool) {
			GenericRecipe recipe = GenericRecipes.pooledBlueprints.get(name);
			if(recipe != null) {
				list.add(recipe.getLocalizedName());
			}
		}
	}
	
	public static String grabPool(ItemStack stack) {
		if(stack == null) return null;
		if(stack.getItem() != ModItems.blueprints) return null;
		if(!stack.hasTagCompound()) return null;
		if(!stack.stackTagCompound.hasKey(NBT_KEY_POOL)) return null;
		return stack.stackTagCompound.getString(NBT_KEY_POOL);
	}
	
	public static ItemStack make(String pool) {
		ItemStack stack = new ItemStack(ModItems.blueprints);
		stack.stackTagCompound = new NBTTagCompound();
		stack.stackTagCompound.setString(NBT_KEY_POOL, pool);
		return stack;
	}

	public static ItemStack makeCoreManipulatorBlueprint(String category, List<String> materials) {
		ItemStack stack = make(CORE_BLUEPRINT_POOL_PREFIX + (category == null ? "" : category));
		if(!stack.hasTagCompound()) {
			stack.stackTagCompound = new NBTTagCompound();
		}

		if(category != null) {
			stack.stackTagCompound.setString(NBT_KEY_CORE_CATEGORY, category);
		}

		NBTTagList list = new NBTTagList();
		Set<String> unique = new LinkedHashSet<String>();
		if(materials != null) {
			for(String material : materials) {
				if(material != null && !material.isEmpty()) {
					unique.add(material);
				}
			}
		}
		for(String material : unique) {
			list.appendTag(new NBTTagString(material));
		}
		stack.stackTagCompound.setTag(NBT_KEY_CORE_MATERIALS, list);
		return stack;
	}

	public static boolean isCoreManipulatorBlueprint(ItemStack stack) {
		if(stack == null || stack.getItem() != ModItems.blueprints || !stack.hasTagCompound()) {
			return false;
		}

		if(stack.stackTagCompound.hasKey(NBT_KEY_CORE_CATEGORY, Constants.NBT.TAG_STRING)) {
			return true;
		}

		String pool = stack.stackTagCompound.getString(NBT_KEY_POOL);
		return pool != null && pool.startsWith(CORE_BLUEPRINT_POOL_PREFIX);
	}

	public static String getCoreManipulatorCategory(ItemStack stack) {
		if(!isCoreManipulatorBlueprint(stack) || !stack.hasTagCompound()) {
			return null;
		}

		String category = stack.stackTagCompound.getString(NBT_KEY_CORE_CATEGORY);
		if(category != null && !category.isEmpty()) {
			return category;
		}

		String pool = stack.stackTagCompound.getString(NBT_KEY_POOL);
		if(pool != null && pool.startsWith(CORE_BLUEPRINT_POOL_PREFIX)) {
			return pool.substring(CORE_BLUEPRINT_POOL_PREFIX.length());
		}
		return null;
	}

	public static List<String> getCoreManipulatorMaterials(ItemStack stack) {
		ArrayList<String> out = new ArrayList<String>();
		if(!isCoreManipulatorBlueprint(stack) || !stack.hasTagCompound()) {
			return out;
		}

		NBTTagList list = stack.stackTagCompound.getTagList(NBT_KEY_CORE_MATERIALS, Constants.NBT.TAG_STRING);
		Set<String> unique = new LinkedHashSet<String>();
		for(int i = 0; i < list.tagCount(); i++) {
			String material = list.getStringTagAt(i);
			if(material != null && !material.isEmpty()) {
				unique.add(material);
			}
		}
		out.addAll(unique);
		String category = getCoreManipulatorCategory(stack);
		if(category != null && !category.isEmpty()) {
			List<String> categoryMaterials = getAllCoreCategoryMaterials(category);
			if(!categoryMaterials.isEmpty()) {
				return categoryMaterials;
			}
		}

		out = dedupeMaterialsByLabel(out);
		sortMaterialsByDisplayName(out);
		return out;
	}

	public static boolean blueprintContainsCoreMaterial(ItemStack stack, String oreDict) {
		if(oreDict == null || oreDict.isEmpty()) {
			return false;
		}
		List<String> materials = getCoreManipulatorMaterials(stack);
		for(String material : materials) {
			if(oreDict.equals(material)) {
				return true;
			}
		}
		return false;
	}

	public static ItemStack getPreferredCoreMaterialDisplayStack(String oreDict) {
		if(oreDict == null || oreDict.isEmpty()) {
			return null;
		}

		ArrayList<String> candidates = new ArrayList<String>();
		candidates.add(oreDict);
		String token = extractMaterialToken(oreDict);

		if(oreDict.startsWith("ingot")) {
			candidates.add("dust" + token);
			candidates.add("powder" + token);
		} else if(oreDict.startsWith("dust")) {
			candidates.add("ingot" + token);
			candidates.add("powder" + token);
		} else if(oreDict.startsWith("powder")) {
			candidates.add("ingot" + token);
			candidates.add("dust" + token);
		}

		if(token != null && !token.isEmpty()) {
			candidates.add("ingot" + token);
			candidates.add("dust" + token);
			candidates.add("powder" + token);
			candidates.add("gem" + token);
			candidates.add("crystal" + token);
			candidates.add("billet" + token);
			candidates.add("nugget" + token);
		}

		for(String name : candidates) {
			List<ItemStack> ores = OreDictionary.getOres(name, false);
			if(ores == null || ores.isEmpty()) continue;
			for(ItemStack stack : ores) {
				if(stack == null) continue;
				ItemStack copy = stack.copy();
				copy.stackSize = 1;
				return copy;
			}
		}

		return null;
	}

	public static String formatCoreMaterialLabel(String oreDict) {
		if(oreDict == null || oreDict.isEmpty()) {
			return "Unknown";
		}

		String baseName = null;
		String category = CelestialCore.getCategoryForOreDict(oreDict);
		if(CelestialCore.CAT_ACTINIDE.equals(category) || CelestialCore.CAT_SCHRABIDIC.equals(category)) {
			ItemStack localized = getPreferredCoreMaterialDisplayStack(oreDict);
			if(localized != null && localized.getDisplayName() != null && !localized.getDisplayName().isEmpty()) {
				baseName = localized.getDisplayName();
			}
		}

		if(baseName == null) {
			String token = extractMaterialToken(oreDict);
			if(token == null || token.isEmpty()) {
				baseName = oreDict;
			} else {
				baseName = splitMaterialToken(token);
			}
		}

		String cleaned = baseName
			.replaceAll("(?i)\\bingot\\b", "")
			.replaceAll("(?i)\\bchunk\\b", "")
			.replaceAll("\\s+", " ")
			.trim();
		String normalized = cleaned.isEmpty() ? baseName : cleaned;

		if("Nickel Pure".equalsIgnoreCase(normalized)) {
			return "Nickel";
		}
		if("Mingrade".equalsIgnoreCase(normalized)) {
			return "Mingrade Copper";
		}
		return normalized;
	}

	public static List<String> getAllCoreCategoryMaterials(String category) {
		ArrayList<String> out = new ArrayList<String>();
		if(category == null || category.isEmpty()) {
			return out;
		}

		HashMap<String, String> bestByToken = new HashMap<String, String>();
		String[] oreNames = OreDictionary.getOreNames();
		for(String oreDict : oreNames) {
			if(oreDict == null || oreDict.isEmpty()) continue;
			if(!category.equals(CelestialCore.getCategoryForOreDict(oreDict))) continue;

			String prefix = getOreDictPrefix(oreDict);
			if(getCoreMaterialPrefixPriority(prefix) == Integer.MAX_VALUE) continue;

			String token = extractMaterialToken(oreDict);
			String tokenKey = normalizeToken(token);
			if(tokenKey == null || tokenKey.isEmpty()) continue;

			String existing = bestByToken.get(tokenKey);
			if(existing == null || compareMaterialCandidates(oreDict, existing) < 0) {
				bestByToken.put(tokenKey, oreDict);
			}
		}

		out.addAll(bestByToken.values());
		out = dedupeMaterialsByLabel(out);
		sortMaterialsByDisplayName(out);
		return out;
	}

	private static int compareMaterialCandidates(String a, String b) {
		String aPrefix = getOreDictPrefix(a);
		String bPrefix = getOreDictPrefix(b);
		int aPriority = getCoreMaterialPrefixPriority(aPrefix);
		int bPriority = getCoreMaterialPrefixPriority(bPrefix);
		if(aPriority != bPriority) return aPriority - bPriority;
		return a.compareToIgnoreCase(b);
	}

	private static int getCoreMaterialPrefixPriority(String prefix) {
		if("ingot".equals(prefix)) return 0;
		if("dust".equals(prefix)) return 1;
		if("powder".equals(prefix)) return 2;
		if("gem".equals(prefix)) return 3;
		if("crystal".equals(prefix)) return 4;
		if("billet".equals(prefix)) return 5;
		if("nugget".equals(prefix)) return 6;
		return Integer.MAX_VALUE;
	}

	private static String getOreDictPrefix(String oreDict) {
		if(oreDict == null) return "";
		for(int i = 0; i < oreDict.length(); i++) {
			if(Character.isUpperCase(oreDict.charAt(i))) {
				return oreDict.substring(0, i).toLowerCase();
			}
		}
		return oreDict.toLowerCase();
	}

	private static String extractMaterialToken(String oreDict) {
		if(oreDict == null || oreDict.isEmpty()) return oreDict;
		for(int i = 0; i < oreDict.length(); i++) {
			if(Character.isUpperCase(oreDict.charAt(i))) {
				return oreDict.substring(i);
			}
		}
		return oreDict;
	}

	private static String splitMaterialToken(String token) {
		if(token == null || token.isEmpty()) return "";
		StringBuilder out = new StringBuilder(token.length() + 8);
		for(int i = 0; i < token.length(); i++) {
			char c = token.charAt(i);
			if(i > 0) {
				char prev = token.charAt(i - 1);
				char next = i + 1 < token.length() ? token.charAt(i + 1) : 0;

				if(Character.isUpperCase(c) && Character.isLowerCase(prev)) {
					out.append(' ');
				} else if(Character.isUpperCase(c) && Character.isUpperCase(prev) && next != 0 && Character.isLowerCase(next)) {
					out.append(' ');
				} else if(Character.isDigit(c) && !Character.isDigit(prev)) {
					out.append(' ');
				}
			}
			out.append(c);
		}
		return out.toString();
	}

	private static String normalizeToken(String token) {
		if(token == null) return null;
		String lower = token.toLowerCase();
		StringBuilder out = new StringBuilder(lower.length());
		for(int i = 0; i < lower.length(); i++) {
			char c = lower.charAt(i);
			if((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
				out.append(c);
			}
		}
		return out.toString();
	}

	private static void sortMaterialsByDisplayName(List<String> materials) {
		Collections.sort(materials, new Comparator<String>() {
			@Override
			public int compare(String a, String b) {
				ItemStack sa = getPreferredCoreMaterialDisplayStack(a);
				ItemStack sb = getPreferredCoreMaterialDisplayStack(b);
				String da = sa != null ? sa.getDisplayName() : a;
				String db = sb != null ? sb.getDisplayName() : b;
				int cmp = da.compareToIgnoreCase(db);
				if(cmp != 0) return cmp;
				return a.compareToIgnoreCase(b);
			}
		});
	}

	private static ArrayList<String> dedupeMaterialsByLabel(List<String> materials) {
		ArrayList<String> out = new ArrayList<String>();
		if(materials == null || materials.isEmpty()) {
			return out;
		}

		HashMap<String, String> bestByLabel = new HashMap<String, String>();
		for(String oreDict : materials) {
			if(oreDict == null || oreDict.isEmpty()) continue;

			String labelKey = normalizeToken(formatCoreMaterialLabel(oreDict));
			if(labelKey == null || labelKey.isEmpty()) {
				labelKey = normalizeToken(oreDict);
			}

			String existing = bestByLabel.get(labelKey);
			if(existing == null || compareMaterialCandidates(oreDict, existing) < 0) {
				bestByLabel.put(labelKey, oreDict);
			}
		}

		out.addAll(bestByLabel.values());
		return out;
	}
}
