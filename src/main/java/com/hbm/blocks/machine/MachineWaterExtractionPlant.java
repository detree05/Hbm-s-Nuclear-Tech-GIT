package com.hbm.blocks.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.tileentity.machine.TileEntityWaterExtractionPlant;
import com.hbm.util.i18n.I18nUtil;

import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;

public class MachineWaterExtractionPlant extends BlockDummyable implements ILookOverlay {

	public MachineWaterExtractionPlant() {
		super(Material.iron);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		if(meta >= 12) {
			return new TileEntityWaterExtractionPlant();
		}

		return null;
	}

	@Override
	public int[] getDimensions() {
		return new int[] {5, 0, 1, 1, 1, 1};
	}

	@Override
	public int getOffset() {
		return 1;
	}

	@Override
	public void printHook(Pre event, World world, int x, int y, int z) {
		int[] pos = this.findCore(world, x, y, z);

		if(pos == null) {
			return;
		}

		TileEntity te = world.getTileEntity(pos[0], pos[1], pos[2]);

		if(!(te instanceof TileEntityWaterExtractionPlant)) {
			return;
		}

		TileEntityWaterExtractionPlant plant = (TileEntityWaterExtractionPlant) te;
		List<String> text = new ArrayList<>();

		text.add(EnumChatFormatting.GREEN + "-> " + EnumChatFormatting.RESET
			+ String.format(Locale.US, "%,d", plant.getPower()) + " / " + String.format(Locale.US, "%,d", plant.getMaxPower()) + "HE");

		FluidTank[] tanks = plant.getAllTanks();
		if(tanks.length > 0 && tanks[0] != null) {
			FluidTank water = tanks[0];
			text.add(EnumChatFormatting.RED + "<- " + EnumChatFormatting.RESET + water.getTankType().getLocalizedName() + ": "
				+ String.format(Locale.US, "%,d", water.getFill()) + " / " + String.format(Locale.US, "%,d", water.getMaxFill()) + "mB");
		}

		ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getUnlocalizedName() + ".name"), 0xffff00, 0x404000, text);
	}
}
