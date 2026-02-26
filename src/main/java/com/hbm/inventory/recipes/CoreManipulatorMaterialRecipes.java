package com.hbm.inventory.recipes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonElement;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBlueprints;

import net.minecraft.item.ItemStack;

public class CoreManipulatorMaterialRecipes extends GenericRecipes<GenericRecipe> {

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

			GenericRecipe recipe = new GenericRecipe(oreDict);
			ItemStack icon = ItemBlueprints.getPreferredCoreMaterialDisplayStack(oreDict);
			recipe.setIcon(icon != null ? icon : new ItemStack(ModItems.nothing));
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
	public GenericRecipe instantiateRecipe(String name) {
		return new GenericRecipe(name);
	}

	@Override
	public void readRecipe(JsonElement element) { }
}
