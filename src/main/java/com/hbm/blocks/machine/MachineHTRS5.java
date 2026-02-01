package com.hbm.blocks.machine;

import java.util.ArrayList;
import java.util.List;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.dim.CelestialBody;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.machine.TileEntityMachineHTRS5;
import com.hbm.util.BobMathUtil;
import com.hbm.util.i18n.I18nUtil;

import api.hbm.fluidmk2.IFluidConnectorBlockMK2;
import api.hbm.fluidmk2.IFluidConnectorMK2;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;
import net.minecraftforge.common.util.ForgeDirection;

public class MachineHTRS5 extends BlockDummyable implements ILookOverlay, IFluidConnectorBlockMK2 {

	private static final int EXTRA_OFFSET = 4;

	public MachineHTRS5() {
		super(Material.iron);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		if(meta >= 12) return new TileEntityMachineHTRS5();
		if(meta >= 6) return new TileEntityProxyCombo(false, true, true);
		return null;
	}

	@Override
	public int[] getDimensions() {
		return new int[] {1, 1, 1, 1, 4, 4};
	}

	@Override
	public int getOffset() {
		return 3;
	}

	@Override
	public int getHeightOffset() {
		return 1;
	}

	@Override
	public ForgeDirection getDirModified(ForgeDirection dir) {
		return dir.getRotation(ForgeDirection.DOWN);
	}

	@Override
	public void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int o) {
		super.fillSpace(world, x, y, z, dir, o);

		ForgeDirection startDir = dir.getRotation(ForgeDirection.UP).getOpposite();
		ForgeDirection rot = startDir.getRotation(ForgeDirection.UP);
		ForgeDirection backDir = startDir.getOpposite();

		x += dir.offsetX * o;
		z += dir.offsetZ * o;

		int baseX = x + startDir.offsetX * EXTRA_OFFSET;
		int baseZ = z + startDir.offsetZ * EXTRA_OFFSET;
		int nearX = baseX + backDir.offsetX;
		int nearZ = baseZ + backDir.offsetZ;

		// Connector layout aligned to the thruster start face.
		this.makeExtra(world, baseX, y, baseZ);
		this.makeExtra(world, baseX, y + 1, baseZ);
		this.makeExtra(world, baseX + rot.offsetX, y, baseZ + rot.offsetZ);
		this.makeExtra(world, baseX - rot.offsetX, y, baseZ - rot.offsetZ);
		this.makeExtra(world, baseX - rot.offsetX, y + 1, baseZ - rot.offsetZ);

		this.makeExtra(world, nearX + rot.offsetX, y, nearZ + rot.offsetZ);
		this.makeExtra(world, nearX, y + 1, nearZ);
		this.makeExtra(world, nearX - rot.offsetX, y, nearZ - rot.offsetZ);
		this.makeExtra(world, nearX - rot.offsetX, y + 1, nearZ - rot.offsetZ);
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if(world.isRemote) return true;

		int[] pos = this.findCore(world, x, y, z);
		if(pos == null) return false;

		TileEntity te = world.getTileEntity(pos[0], pos[1], pos[2]);
		if(!(te instanceof TileEntityMachineHTRS5)) return false;

		TileEntityMachineHTRS5 thruster = (TileEntityMachineHTRS5) te;

		ItemStack held = player.getHeldItem();
		ItemStack stored = thruster.getStackInSlot(0);

		if(held == null) {
			if(stored == null) return false;

			ItemStack extracted = stored.copy();
			thruster.setInventorySlotContents(0, null);
			thruster.markDirty();
			thruster.networkPackNT(250);
			if(!player.inventory.addItemStackToInventory(extracted)) {
				thruster.setInventorySlotContents(0, stored);
				thruster.markDirty();
				thruster.networkPackNT(250);
				return false;
			}
			return true;
		}

		if(!thruster.isItemValidForSlot(0, held)) return false;
		if(stored != null) return false;

		ItemStack stack = held.copy();
		stack.stackSize = 1;
		thruster.setInventorySlotContents(0, stack);
		thruster.markDirty();
		thruster.networkPackNT(250);

		if(!player.capabilities.isCreativeMode) {
			held.stackSize--;
			if(held.stackSize <= 0) {
				player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
			}
		}

		return true;
	}

	@Override
	public void printHook(Pre event, World world, int x, int y, int z) {
		if(!CelestialBody.inOrbit(world)) return;

		int[] pos = this.findCore(world, x, y, z);

		if(pos == null) return;

		TileEntity te = world.getTileEntity(pos[0], pos[1], pos[2]);

		if(!(te instanceof TileEntityMachineHTRS5))
			return;

		TileEntityMachineHTRS5 thruster = (TileEntityMachineHTRS5) te;

		List<String> text = new ArrayList<String>();

		if(!thruster.isFacingPrograde()) {
			text.add("&[" + (BobMathUtil.getBlink() ? 0xff0000 : 0xffff00) + "&]! ! ! " + I18nUtil.resolveKey("atmosphere.engineFacing") + " ! ! !");
		} else {
			if(thruster.hasCatalyst()) {
				text.add("Interact to remove Black Hole");
			} else {
				text.add("Interact to insert Black Hole");
			}
			if(!thruster.hasCatalyst()) {
				text.add(EnumChatFormatting.RED + "Missing Miniature Black Hole");
			}
			if(thruster.tanks[0].getFill() <= 0) {
				text.add(EnumChatFormatting.RED + "Missing Dark Matter");
			}
			for(int i = 0; i < thruster.tanks.length; i++) {
				FluidTank tank = thruster.tanks[i];
				text.add(EnumChatFormatting.GREEN + "-> " + EnumChatFormatting.RESET + tank.getTankType().getLocalizedName() + ": " + tank.getFill() + "/" + tank.getMaxFill() + "mB");
			}
		}

		ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getUnlocalizedName() + ".name"), 0xffff00, 0x404000, text);
	}

	@Override
	public boolean canConnect(FluidType type, net.minecraft.world.IBlockAccess world, int x, int y, int z, ForgeDirection dir) {
		TileEntity te = world.getTileEntity(x, y, z);
		if(te instanceof IFluidConnectorMK2) {
			return ((IFluidConnectorMK2) te).canConnect(type, dir);
		}
		return false;
	}
}
