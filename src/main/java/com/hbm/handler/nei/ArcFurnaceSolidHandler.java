package com.hbm.handler.nei;

import java.util.Iterator;
import java.util.Map.Entry;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.ArcFurnaceRecipes;
import com.hbm.inventory.recipes.ArcFurnaceRecipes.ArcFurnaceRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.special.ItemBedrockOreNew;
import com.hbm.items.special.ItemBedrockOreNew.BedrockOreGrade;
import com.hbm.items.special.ItemBedrockOreNew.CelestialBedrockOre;
import com.hbm.items.special.ItemBedrockOreNew.CelestialBedrockOreType;

import net.minecraft.item.ItemStack;

public class ArcFurnaceSolidHandler extends NEIUniversalHandler {

	public ArcFurnaceSolidHandler() {
		super(ModBlocks.machine_arc_furnace.getLocalizedName(), ModBlocks.machine_arc_furnace, ArcFurnaceRecipes.getSolidRecipes());
	}

	@Override
	public String getKey() {
		return "ntmArcFurnaceSolid";
	}

	private void refreshDynamicBedrockRecipes() {
		Iterator<Entry<Object, Object>> iterator = this.recipes.entrySet().iterator();
		while(iterator.hasNext()) {
			Entry<Object, Object> entry = iterator.next();
			if(isBedrockInput(entry.getKey())) {
				iterator.remove();
			}
		}

		for(CelestialBedrockOreType type : CelestialBedrockOre.getAllTypes()) {
			for(BedrockOreGrade grade : BedrockOreGrade.values()) {
				ItemStack input = ItemBedrockOreNew.make(grade, type);
				ArcFurnaceRecipe recipe = ArcFurnaceRecipes.getOutput(input, false);
				if(recipe != null && recipe.solidOutput != null) {
					this.recipes.put(new ComparableStack(input).makeSingular(), recipe.solidOutput.copy());
				}
			}
		}
	}

	private boolean isBedrockInput(Object key) {
		if(!(key instanceof ComparableStack)) return false;
		ComparableStack comp = (ComparableStack) key;
		return comp.item == ModItems.bedrock_ore;
	}

	@Override
	public void loadCraftingRecipes(String outputId, Object... results) {
		refreshDynamicBedrockRecipes();
		super.loadCraftingRecipes(outputId, results);
	}

	@Override
	public void loadCraftingRecipes(ItemStack result) {
		refreshDynamicBedrockRecipes();
		super.loadCraftingRecipes(result);
	}

	@Override
	public void loadUsageRecipes(String inputId, Object... ingredients) {
		refreshDynamicBedrockRecipes();
		super.loadUsageRecipes(inputId, ingredients);
	}

	@Override
	public void loadUsageRecipes(ItemStack ingredient) {
		refreshDynamicBedrockRecipes();
		super.loadUsageRecipes(ingredient);
	}
}
