package com.hbm.items.machine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

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
					list.add(EnumChatFormatting.GRAY + "- " + materials.get(i));
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
		Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
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

		if(oreDict.startsWith("ingot")) {
			String token = oreDict.substring("ingot".length());
			candidates.add("dust" + token);
			candidates.add("powder" + token);
		} else if(oreDict.startsWith("dust")) {
			String token = oreDict.substring("dust".length());
			candidates.add("ingot" + token);
			candidates.add("powder" + token);
		} else if(oreDict.startsWith("powder")) {
			String token = oreDict.substring("powder".length());
			candidates.add("ingot" + token);
			candidates.add("dust" + token);
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
}
