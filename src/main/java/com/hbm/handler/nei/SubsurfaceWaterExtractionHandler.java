package com.hbm.handler.nei;

import java.util.HashMap;

import com.hbm.blocks.ModBlocks;
import com.hbm.dim.CelestialBody;
import com.hbm.dim.SolarSystem;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.ItemFluidIcon;

import net.minecraft.item.ItemStack;

public class SubsurfaceWaterExtractionHandler extends NEICelestialHandler {

	public SubsurfaceWaterExtractionHandler() {
		super("Subsurface Liquid Extraction", new ItemStack[] { new ItemStack(ModBlocks.machine_sub_liquid_extraction_plant) }, getRecipes());
	}

	@Override
	public String getKey() {
		return "ntmSubsurfaceWaterExtraction";
	}

	public static HashMap<CelestialBody, ItemStack[]> getRecipes() {
		HashMap<CelestialBody, ItemStack[]> map = new HashMap<>();

		for(SolarSystem.Body bodyEnum : SolarSystem.Body.values()) {
			CelestialBody body = bodyEnum.getBody();
			if(body == null) continue;

			switch(bodyEnum) {
				case KERBIN:
				case LAYTHE:
					map.put(body, new ItemStack[] { ItemFluidIcon.make(Fluids.SUBSURFACE_WATER, 1_000) });
					break;
				case EVE:
					map.put(body, new ItemStack[] { ItemFluidIcon.make(Fluids.AMIDO_MERCURY_COMPLEX, 1_000) });
					break;
				default: break;
			}
		}

		return map;
	}
}
