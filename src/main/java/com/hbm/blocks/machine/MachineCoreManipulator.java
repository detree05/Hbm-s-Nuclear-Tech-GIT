package com.hbm.blocks.machine;

import java.util.ArrayList;
import java.util.List;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.machine.TileEntityCoreManipulator;
import com.hbm.util.BobMathUtil;
import com.hbm.util.i18n.I18nUtil;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;
import net.minecraftforge.common.util.ForgeDirection;

public class MachineCoreManipulator extends BlockDummyable implements ILookOverlay, ITooltipProvider {

	public MachineCoreManipulator(Material mat) {
		super(mat);
		this.setLightOpacity(0);

		// Front LEFT/RIGHT Foot
		this.bounding.add(AxisAlignedBB.getBoundingBox(-4.5, 0, -4.5, -1.5, 1, -1.5));
		this.bounding.add(AxisAlignedBB.getBoundingBox(1.5, 0, -4.5, 4.5, 1, -1.5));
		//Leg
		this.bounding.add(AxisAlignedBB.getBoundingBox(-4, 1, -4, -2, 7, -2));
		this.bounding.add(AxisAlignedBB.getBoundingBox(2, 1, -4, 4, 7, -2));
		// Top
		this.bounding.add(AxisAlignedBB.getBoundingBox(-4.5, 7, -4.5, -1.5, 8, -1.5));
		this.bounding.add(AxisAlignedBB.getBoundingBox(1.5, 7, -4.5, 4.5, 8, -1.5));


		// Back LEFT/RIGHT Foot
		this.bounding.add(AxisAlignedBB.getBoundingBox(-4.5, 0, 1.5, -1.5, 1, 4.5));
		this.bounding.add(AxisAlignedBB.getBoundingBox(1.5, 0, 1.5, 4.5, 1, 4.5));
		//Leg
		this.bounding.add(AxisAlignedBB.getBoundingBox(-4, 1, 2, -2, 7, 4));
		this.bounding.add(AxisAlignedBB.getBoundingBox(2, 1, 2, 4, 7, 4));
		// Top
		this.bounding.add(AxisAlignedBB.getBoundingBox(-4.5, 7, 1.5, -1.5, 8, 4.5));
		this.bounding.add(AxisAlignedBB.getBoundingBox(1.5, 7, 1.5, 4.5, 8, 4.5));

		// Beams Bottom Front1,2/Back1,2
		this.bounding.add(AxisAlignedBB.getBoundingBox(-1.5, 0.25, -4, 1.5, 0.75, -3.5));
		this.bounding.add(AxisAlignedBB.getBoundingBox(-1.5, 0.25, -2.5, 1.5, 0.75, -2));
		this.bounding.add(AxisAlignedBB.getBoundingBox(-1.5, 0.25, 2, 1.5, 0.75, 2.5));
		this.bounding.add(AxisAlignedBB.getBoundingBox(-1.5, 0.25, 3.5, 1.5, 0.75, 4));

		// Beams Bottom Front1,2/Back1,2 rotated tho
		this.bounding.add(AxisAlignedBB.getBoundingBox(3.5, 0.25, -1.5, 4, 0.75, 1.5));
		this.bounding.add(AxisAlignedBB.getBoundingBox(2, 0.25, -1.5, 2.5, 0.75, 1.5));
		this.bounding.add(AxisAlignedBB.getBoundingBox(-2.5, 0.25, -1.5, -2, 0.75, 1.5));
		this.bounding.add(AxisAlignedBB.getBoundingBox(-4, 0.25, -1.5, -3.5, 0.75, 1.5));

		// Beams Top Front1,2/Back1,2
		this.bounding.add(AxisAlignedBB.getBoundingBox(-1.5, 7.125, -4, 1.5, 7.625, -3.5));
		this.bounding.add(AxisAlignedBB.getBoundingBox(-1.5, 7.125, -2.5, 1.5, 7.625, -2));
		this.bounding.add(AxisAlignedBB.getBoundingBox(-1.5, 7.125, 2, 1.5, 7.625, 2.5));
		this.bounding.add(AxisAlignedBB.getBoundingBox(-1.5, 7.125, 3.5, 1.5, 7.625, 4));

		// Beams Top Front1,2/Back1,2 rotated tho
		this.bounding.add(AxisAlignedBB.getBoundingBox(3.5, 7.125, -1.5, 4, 7.625, 1.5));
		this.bounding.add(AxisAlignedBB.getBoundingBox(2, 7.125, -1.5, 2.5, 7.625, 1.5));
		this.bounding.add(AxisAlignedBB.getBoundingBox(-2.5, 7.125, -1.5, -2, 7.625, 1.5));
		this.bounding.add(AxisAlignedBB.getBoundingBox(-4, 7.125, -1.5, -3.5, 7.625, 1.5));

		// Nozzle
		this.bounding.add(AxisAlignedBB.getBoundingBox(-0.5, 0, -0.5, 0.5, 1, 0.5));
		this.bounding.add(AxisAlignedBB.getBoundingBox(-1.25, 1, -1.25, 1.25, 2, 1.25));
		this.bounding.add(AxisAlignedBB.getBoundingBox(-0.75, 2, -0.75, 0.75, 6, 0.75));
		this.bounding.add(AxisAlignedBB.getBoundingBox(-1, 6, -1, 1, 8, 1));

		//Bottom/Top Boxes
		this.bounding.add(AxisAlignedBB.getBoundingBox(-1.5, 1.4375, -3.5, 1.5, 3.4375, -2));
		this.bounding.add(AxisAlignedBB.getBoundingBox(-1.5, 1.4375, 2, 1.5, 3.4375, 3.5));

		this.bounding.add(AxisAlignedBB.getBoundingBox(-1.75, 4.5, -4.5, 1.75, 6.5, -2.5));
		this.bounding.add(AxisAlignedBB.getBoundingBox(-1.75, 4.5, 2.5, 1.75, 6.5, 4.5));
	}

	// Front LEFT/RIGHT Leg
//		this.bounding.add(AxisAlignedBB.getBoundingBox(-4.5, 0, -4.5, -1.5, 1, -1.5));
//		this.bounding.add(AxisAlignedBB.getBoundingBox(0.2, 0, -0.8, 0.8, 1, -0.2));
	// Back LEFT/RIGHT Leg
//		this.bounding.add(AxisAlignedBB.getBoundingBox(1.5, 0, -4.5, 4.5, 1, -1.5));
//		this.bounding.add(AxisAlignedBB.getBoundingBox(1.5, 0, 1.5, 4.5, 1, 4.5));

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		if(meta >= 12) return new TileEntityCoreManipulator();
		return new TileEntityProxyCombo().inventory();
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		return this.standardOpenBehavior(world, x, y, z, player, 0);
	}

	@Override
	public int[] getDimensions() {
		// Order is U,D,N,S,W,E (not XYZ), so this is a centered 9x9x9 volume.
		return new int[] {7, 0, 4, 4, 4, 4};
	}

	@Override
	public int getOffset() {
		return 4;
	}

	@Override
	public void printHook(Pre event, World world, int x, int y, int z) {
		int[] pos = this.findCore(world, x, y, z);
		if(pos == null) {
			return;
		}

		TileEntity te = world.getTileEntity(pos[0], pos[1], pos[2]);
		if(!(te instanceof TileEntityCoreManipulator)) {
			return;
		}

		TileEntityCoreManipulator manipulator = (TileEntityCoreManipulator) te;
		long energyOutput = manipulator.getDysonEnergyOutputPerSecond();

		List<String> text = new ArrayList<String>();
		if(manipulator.getDysonSwarmId() > 0) {
			text.add("ID: " + manipulator.getDysonSwarmId());
			text.add("Swarm: " + manipulator.getDysonSwarmCount() + " members");
			text.add("Consumers: " + manipulator.getDysonSwarmConsumers() + " consumers");
			text.add("Power: " + BobMathUtil.getShortNumber(energyOutput) + "HE/s");
		} else {
			text.add("No Satellite ID-Chip installed!");
		}

		ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getUnlocalizedName() + ".name"), 0xffff00, 0x404000, text);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean ext) {
		addStandardInfo(stack, player, list, ext);
	}
}


