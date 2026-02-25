package com.hbm.tileentity.machine;

import com.hbm.inventory.container.ContainerCoreManipulator;
import com.hbm.inventory.gui.GUICoreManipulator;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.world.World;

public class TileEntityCoreManipulator extends TileEntityMachineBase implements IGUIProvider {

	public TileEntityCoreManipulator() {
		super(0);
	}

	@Override
	public void updateEntity() {
	}

	@Override
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new ContainerCoreManipulator(player.inventory, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new GUICoreManipulator(player.inventory, this);
	}

	@Override
	public String getName() {
		return "container.coreManipulator";
	}
}
