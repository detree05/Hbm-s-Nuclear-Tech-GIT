package com.hbm.blocks.machine;

import com.hbm.tileentity.machine.TileEntityCoreManipulator;

import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class MachineCoreManipulator extends BlockMachineBase {

	public MachineCoreManipulator(Material mat) {
		super(mat, 0);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityCoreManipulator();
	}

	@Override
	public int getRenderType() {
		return -1;
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}
}
