package com.hbm.blocks.machine;

import java.util.ArrayList;
import java.util.List;

import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.dim.trait.CBT_SkyState;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.machine.TileEntityStarCorePowerInjector;
import com.hbm.util.BobMathUtil;
import com.hbm.util.i18n.I18nUtil;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;

public class MachineStarCorePowerInjector extends BlockContainer implements ILookOverlay, ITooltipProvider {

	public MachineStarCorePowerInjector(Material mat) {
		super(mat);
		this.setLightOpacity(0);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityStarCorePowerInjector();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister reg) {
		this.blockIcon = reg.registerIcon(RefStrings.MODID + ":air");
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
	public int getRenderType() {
		return -1;
	}

	@Override
	public void printHook(Pre event, World world, int x, int y, int z) {
		TileEntity te = world.getTileEntity(x, y, z);
		if(!(te instanceof TileEntityStarCorePowerInjector)) return;

		TileEntityStarCorePowerInjector injector = (TileEntityStarCorePowerInjector) te;
		List<String> text = new ArrayList<>();

		CBT_SkyState skyState = CBT_SkyState.get(world);
		CBT_SkyState.SkyState state = skyState.getState();
		if(state == CBT_SkyState.SkyState.BLACKHOLE || state == CBT_SkyState.SkyState.NOTHING) {
			EnumChatFormatting color = BobMathUtil.getBlink() ? EnumChatFormatting.YELLOW : EnumChatFormatting.RED;
			text.add(color + "Nothing to shoot beam at... yet.");
		} else if(state == CBT_SkyState.SkyState.SUN) {
			text.add(EnumChatFormatting.YELLOW + "Sun is here! :3");
		} else {
			text.add("Power throughput: " + BobMathUtil.getShortNumber(injector.getThroughputPerSecond()) + "HE/s");
		}

		ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getUnlocalizedName() + ".name"), 0xffff00, 0x404000, text);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean ext) {
		addStandardInfo(stack, player, list, ext);
	}
}
