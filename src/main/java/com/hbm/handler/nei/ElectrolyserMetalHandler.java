package com.hbm.handler.nei;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.gui.GUIElectrolyserMetal;
import com.hbm.inventory.recipes.ElectrolyserMetalRecipes;
import com.hbm.inventory.recipes.ElectrolyserMetalRecipes.ElectrolysisMetalRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.items.machine.ItemScraps;
import com.hbm.items.special.ItemBedrockOreNew;
import com.hbm.items.special.ItemBedrockOreNew.BedrockOreGrade;
import com.hbm.items.special.ItemBedrockOreNew.CelestialBedrockOre;
import com.hbm.items.special.ItemBedrockOreNew.CelestialBedrockOreType;

import net.minecraft.item.ItemStack;

public class ElectrolyserMetalHandler extends NEIUniversalHandler {

	public ElectrolyserMetalHandler() {
		super(ModBlocks.machine_electrolyser.getLocalizedName(), ModBlocks.machine_electrolyser, ElectrolyserMetalRecipes.getRecipes());
	}

	@Override
	public String getKey() {
		return "ntmElectrolysisMetal";
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
				ElectrolysisMetalRecipe recipe = ElectrolyserMetalRecipes.getRecipe(input);
				if(recipe == null) {
					continue;
				}

				Object[] in = new Object[] { new ComparableStack(input).makeSingular(), ItemFluidIcon.make(Fluids.NITRIC_ACID, 100) };
				List<Object> out = new ArrayList<Object>();
				if(recipe.output1 != null) out.add(ItemScraps.create(recipe.output1, true));
				if(recipe.output2 != null) out.add(ItemScraps.create(recipe.output2, true));
				if(recipe.byproduct != null) {
					for(ItemStack byproduct : recipe.byproduct) {
						if(byproduct != null) out.add(byproduct.copy());
					}
				}
				if(!out.isEmpty()) {
					this.recipes.put(in, out.toArray());
				}
			}
		}
	}

	private boolean isBedrockInput(Object key) {
		if(!(key instanceof Object[])) return false;
		Object[] array = (Object[]) key;
		if(array.length == 0) return false;
		Object first = array[0];
		if(first instanceof ComparableStack) {
			return ((ComparableStack) first).item == ModItems.bedrock_ore;
		}
		if(first instanceof ItemStack) {
			return ((ItemStack) first).getItem() == ModItems.bedrock_ore;
		}
		return false;
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
		transferRectsGui.add(new RecipeTransferRect(new Rectangle(2, 35, 22, 25), "ntmElectrolysisMetal"));
		guiGui.add(GUIElectrolyserMetal.class);
		RecipeTransferRectHandler.registerRectsToGuis(guiGui, transferRectsGui);
	}
}
