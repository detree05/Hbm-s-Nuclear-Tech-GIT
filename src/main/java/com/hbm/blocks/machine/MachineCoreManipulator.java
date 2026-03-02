package com.hbm.blocks.machine;

import java.util.ArrayList;
import java.util.List;

import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.tileentity.machine.TileEntityCoreManipulator;
import com.hbm.util.BobMathUtil;
import com.hbm.util.i18n.I18nUtil;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;

public class MachineCoreManipulator extends BlockMachineBase implements ILookOverlay, ITooltipProvider {

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

	@Override
	public void printHook(Pre event, World world, int x, int y, int z) {
		TileEntity te = world.getTileEntity(x, y, z);
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
