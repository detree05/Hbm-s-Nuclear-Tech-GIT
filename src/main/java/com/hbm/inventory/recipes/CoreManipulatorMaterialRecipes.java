package com.hbm.inventory.recipes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.google.gson.JsonElement;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBlueprints;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

public class CoreManipulatorMaterialRecipes extends GenericRecipes<CoreManipulatorMaterialRecipes.CoreMaterialRecipe> {

	public static class CoreMaterialRecipe extends GenericRecipe {

		public CoreMaterialRecipe(String oreDict, ItemStack icon) {
			super(oreDict);
			this.setIcon(icon != null ? icon : new ItemStack(ModItems.nothing));
		}

		@Override
		public List<String> print() {
			List<String> list = new ArrayList<String>();
			list.add(EnumChatFormatting.YELLOW + this.getLocalizedName());
			return list;
		}

		@Override
		public boolean matchesSearch(String substring) {
			String needle = substring == null ? "" : substring.toLowerCase(Locale.US);
			return this.getLocalizedName().toLowerCase(Locale.US).contains(needle)
				|| this.getInternalName().toLowerCase(Locale.US).contains(needle);
		}
	}

	public CoreManipulatorMaterialRecipes() { }

	public static CoreManipulatorMaterialRecipes fromBlueprint(ItemStack blueprint) {
		CoreManipulatorMaterialRecipes recipes = new CoreManipulatorMaterialRecipes();
		recipes.rebuildFromBlueprint(blueprint);
		return recipes;
	}

	public void rebuildFromBlueprint(ItemStack blueprint) {
		this.deleteRecipes();

		List<String> materials = new ArrayList<String>(ItemBlueprints.getCoreManipulatorMaterials(blueprint));
		Collections.sort(materials, String.CASE_INSENSITIVE_ORDER);

		for(String oreDict : materials) {
			if(oreDict == null || oreDict.isEmpty()) continue;

			ItemStack icon = ItemBlueprints.getPreferredCoreMaterialDisplayStack(oreDict);
			if(icon == null) continue;
			CoreMaterialRecipe recipe = new CoreMaterialRecipe(oreDict, icon);
			this.register(recipe);
		}
	}

	@Override
	public int inputItemLimit() {
		return 0;
	}

	@Override
	public int inputFluidLimit() {
		return 0;
	}

	@Override
	public int outputItemLimit() {
		return 0;
	}

	@Override
	public int outputFluidLimit() {
		return 0;
	}

	@Override
	public String getFileName() {
		return "hbmCoreManipulatorVirtual.json";
	}

	@Override
	public void registerDefaults() { }

	@Override
	public CoreMaterialRecipe instantiateRecipe(String name) {
		return new CoreMaterialRecipe(name, new ItemStack(ModItems.nothing));
	}

	@Override
	public void readRecipe(JsonElement element) { }
}
