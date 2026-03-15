package com.hbm.handler.nei;

import java.awt.Rectangle;
import java.util.Iterator;
import java.util.Map.Entry;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.gui.GUIMachineCentrifuge;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.special.ItemBedrockOreNew;
import com.hbm.items.special.ItemBedrockOreNew.BedrockOreGrade;
import com.hbm.items.special.ItemBedrockOreNew.CelestialBedrockOre;
import com.hbm.items.special.ItemBedrockOreNew.CelestialBedrockOreType;

import net.minecraft.item.ItemStack;

public class CentrifugeRecipeHandler extends NEIUniversalHandler {

	public CentrifugeRecipeHandler() {
		super("Centrifuge", ModBlocks.machine_centrifuge, CentrifugeRecipes.getRecipes());
	}

	@Override
	public String getKey() {
		return "ntmCentrifuge";
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
				ItemStack[] output = CentrifugeRecipes.getOutput(input);
				if(output != null && output.length > 0) {
					this.recipes.put(new ComparableStack(input).makeSingular(), output);
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
	
	@Override
	public void loadTransferRects() {
		super.loadTransferRects();
		transferRectsGui.add(new RecipeTransferRect(new Rectangle(56, 0, 80, 38), "ntmCentrifuge"));
		guiGui.add(GUIMachineCentrifuge.class);
		RecipeTransferRectHandler.registerRectsToGuis(guiGui, transferRectsGui);
	}
}
