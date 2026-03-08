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

		// Fully solid collision: exact 9x9x9 volume.
		this.bounding.add(AxisAlignedBB.getBoundingBox(-4.5D, 0D, -4.5D, 4.5D, 9D, 4.5D));
	}

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
		return new int[] {8, 0, 4, 4, 4, 4};
	}

	@Override
	public int getOffset() {
		return 0;
	}

	@Override
	protected boolean isLegacyMonoblock(World world, int x, int y, int z) {
		TileEntity te = world.getTileEntity(x, y, z);
		return te instanceof TileEntityCoreManipulator && world.getBlockMetadata(x, y, z) < 12;
	}

	@Override
	protected void fixLegacyMonoblock(World world, int x, int y, int z) {
		int oldMeta = world.getBlockMetadata(x, y, z) % 6;
		if(oldMeta < 2) oldMeta = 2;
		world.setBlockMetadataWithNotify(x, y, z, offset + oldMeta, 3);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB entityBounding, List list, Entity entity) {
		if(!this.useDetailedHitbox()) {
			super.addCollisionBoxesToList(world, x, y, z, entityBounding, list, entity);
			return;
		}

		int[] pos = this.findCore(world, x, y, z);
		if(pos == null) {
			return;
		}

		ForgeDirection rot = ForgeDirection.getOrientation(world.getBlockMetadata(pos[0], pos[1], pos[2]) - offset).getRotation(ForgeDirection.UP);
		for(AxisAlignedBB aabb : this.bounding) {
			AxisAlignedBB boxlet = getAABBRotationOffset(aabb, pos[0] + 0.5, pos[1], pos[2] + 0.5, rot);
			if(entityBounding.intersectsWith(boxlet)) {
				list.add(boxlet);
			}
		}
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
