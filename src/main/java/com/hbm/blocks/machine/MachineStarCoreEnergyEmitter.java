package com.hbm.blocks.machine;

import java.util.ArrayList;
import java.util.List;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.config.SpaceConfig;
import com.hbm.dim.StarcoreThroughputTracker;
import com.hbm.dim.kerbol.WorldProviderKerbol;
import com.hbm.dim.trait.CBT_SkyState;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.machine.TileEntityStarCoreEnergyEmitter;
import com.hbm.util.BobMathUtil;
import com.hbm.util.i18n.I18nUtil;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderEnd;
import net.minecraft.world.WorldProviderHell;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;
import net.minecraftforge.common.util.ForgeDirection;

public class MachineStarCoreEnergyEmitter extends BlockDummyable implements ILookOverlay, ITooltipProvider {

	public MachineStarCoreEnergyEmitter(Material mat) {
		super(mat);
		this.setLightOpacity(0);

		// Relative to core center (x + 0.5, z + 0.5), 3 stacked layers.
		// Layer 1: full 5x5 square footprint.
		this.bounding.add(AxisAlignedBB.getBoundingBox(-2.5D, 0D, -2.5D, 2.5D, 1D, 2.5D));

		// Layer 2: 3.5x3.5 circle approximation.
		this.bounding.add(AxisAlignedBB.getBoundingBox(-1.25D, 1D, -1.25D, 1.25D, 2D, 1.25D));
		this.bounding.add(AxisAlignedBB.getBoundingBox(-1.25D, 1D, 1.25D, 1.25D, 2D, 1.75D));
		this.bounding.add(AxisAlignedBB.getBoundingBox(-1.25D, 1D, -1.75D, 1.25D, 2D, -1.25D));
		this.bounding.add(AxisAlignedBB.getBoundingBox(1.25D, 1D, -1.25D, 1.75D, 2D, 1.25D));
		this.bounding.add(AxisAlignedBB.getBoundingBox(-1.75D, 1D, -1.25D, -1.25D, 2D, 1.25D));

		// Layer 3: 2.5x2.5 circle approximation.
		this.bounding.add(AxisAlignedBB.getBoundingBox(-0.875D, 2D, -0.875D, 0.875D, 3D, 0.875D));
		this.bounding.add(AxisAlignedBB.getBoundingBox(-0.875D, 2D, 0.875D, 0.875D, 3D, 1.25D));
		this.bounding.add(AxisAlignedBB.getBoundingBox(-0.875D, 2D, -1.25D, 0.875D, 3D, -0.875D));
		this.bounding.add(AxisAlignedBB.getBoundingBox(0.875D, 2D, -0.875D, 1.25D, 3D, 0.875D));
		this.bounding.add(AxisAlignedBB.getBoundingBox(-1.25D, 2D, -0.875D, -0.875D, 3D, 0.875D));
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		if(meta >= 12) return new TileEntityStarCoreEnergyEmitter();
		if(meta >= 6) return new TileEntityProxyCombo().power();
		return null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister reg) {
		this.blockIcon = reg.registerIcon(RefStrings.MODID + ":air");
	}

	@Override
	public int[] getDimensions() {
		return new int[] {2, 0, 2, 2, 2, 2};
	}

	@Override
	public int getOffset() {
		return 0;
	}

	@Override
	protected void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int o) {
		super.fillSpace(world, x, y, z, dir, o);

		x += dir.offsetX * o;
		z += dir.offsetZ * o;

		// Keep power IO accessible from the shell, with side-locked intake points.
		this.makeExtra(world, x + 2, y, z + 1);
		this.makeExtra(world, x + 2, y, z - 1);
		this.makeExtra(world, x + 1, y, z - 2);
		this.makeExtra(world, x - 1, y, z - 2);
		this.makeExtra(world, x - 2, y, z - 1);
		this.makeExtra(world, x - 2, y, z + 1);
		this.makeExtra(world, x - 1, y, z + 2);
		this.makeExtra(world, x + 1, y, z + 2);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB entityBounding, List list, Entity entity) {
		if(!this.useDetailedHitbox()) {
			super.addCollisionBoxesToList(world, x, y, z, entityBounding, list, entity);
			return;
		}

		int[] pos = this.findCore(world, x, y, z);
		if(pos == null) return;

		ForgeDirection rot = ForgeDirection.getOrientation(world.getBlockMetadata(pos[0], pos[1], pos[2]) - offset).getRotation(ForgeDirection.UP);

		for(AxisAlignedBB aabb : this.bounding) {
			AxisAlignedBB boxlet = getAABBRotationOffset(aabb, pos[0] + 0.5, pos[1], pos[2] + 0.5, rot);
			if(entityBounding.intersectsWith(boxlet)) {
				list.add(boxlet);
			}
		}
	}

	@Override
	protected boolean isLegacyMonoblock(World world, int x, int y, int z) {
		TileEntity te = world.getTileEntity(x, y, z);
		return te instanceof TileEntityStarCoreEnergyEmitter && world.getBlockMetadata(x, y, z) < 12;
	}

	@Override
	protected void fixLegacyMonoblock(World world, int x, int y, int z) {
		int oldMeta = world.getBlockMetadata(x, y, z) % 6;
		// Core metadata must stay in the dummy-core range; vertical legacy metas get normalized.
		if(oldMeta < 2) oldMeta = 2;
		world.setBlockMetadataWithNotify(x, y, z, offset + oldMeta, 3);
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		return true;
	}

	@Override
	public void printHook(Pre event, World world, int x, int y, int z) {
		int[] pos = this.findCore(world, x, y, z);
		if(pos == null) return;

		TileEntity te = world.getTileEntity(pos[0], pos[1], pos[2]);
		if(!(te instanceof TileEntityStarCoreEnergyEmitter)) return;

		TileEntityStarCoreEnergyEmitter emitter = (TileEntityStarCoreEnergyEmitter) te;
		List<String> text = new ArrayList<>();

		if(world.provider != null && world.provider.dimensionId == SpaceConfig.orbitDimension) {
			EnumChatFormatting color = BobMathUtil.getBlink() ? EnumChatFormatting.RED : EnumChatFormatting.YELLOW;
			text.add(color + "! ! ! Unstable angle ! ! !");
			ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getUnlocalizedName() + ".name"), 0xff0000, 0x404000, text);
			return;
		}

		if(world.provider instanceof WorldProviderHell || world.provider instanceof WorldProviderEnd) {
			text.add(EnumChatFormatting.RED + "did you really consider this would work.");
			ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getUnlocalizedName() + ".name"), 0xff0000, 0x400000, text);
			return;
		}

		if(world.provider instanceof WorldProviderKerbol) {
			text.add(EnumChatFormatting.RED + "never.");
			ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getUnlocalizedName() + ".name"), 0xff0000, 0x400000, text);
			return;
		}

		CBT_SkyState skyState = CBT_SkyState.get(world);
		CBT_SkyState.SkyState state = skyState.getState();
		if(state == CBT_SkyState.SkyState.BLACKHOLE || state == CBT_SkyState.SkyState.NOTHING) {
			EnumChatFormatting color = BobMathUtil.getBlink() ? EnumChatFormatting.YELLOW : EnumChatFormatting.RED;
			text.add(color + "Nothing to emit beam at.");
		} else {
			long emitterPerSecond = emitter.getThroughputPerFiveTicks() * 4L;
			long totalPerSecond = StarcoreThroughputTracker.getLastFiveTickTotal(world) * 4L;
			text.add("Current power: " + BobMathUtil.getShortNumber(emitterPerSecond) + "HE/s");
			text.add("Emitters: " + StarcoreThroughputTracker.getLastFiveTickInjectorCount(world) + " emitters");
			text.add("Total power: " + BobMathUtil.getShortNumber(totalPerSecond) + "HE/s");
			text.add("Star charge: " + BobMathUtil.getShortNumber(skyState.getSunCharge()) + "HE");
		}

		ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getUnlocalizedName() + ".name"), 0xffff00, 0x404000, text);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean ext) {
		addStandardInfo(stack, player, list, ext);
	}
}
