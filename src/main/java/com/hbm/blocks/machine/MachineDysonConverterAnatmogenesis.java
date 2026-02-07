package com.hbm.blocks.machine;

import java.util.ArrayList;
import java.util.List;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.config.SpaceConfig;
import com.hbm.dim.trait.CBT_SkyState;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.trait.FT_Gaseous;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous_ART;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.machine.TileEntityDysonConverterAnatmogenesis;
import com.hbm.util.BobMathUtil;
import com.hbm.util.i18n.I18nUtil;

import api.hbm.block.IToolable;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;

public class MachineDysonConverterAnatmogenesis extends BlockDummyable implements ILookOverlay, IToolable {

	public MachineDysonConverterAnatmogenesis(Material mat) {
		super(mat);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		if(meta >= 12) return new TileEntityDysonConverterAnatmogenesis();
		if(meta >= 6) return new TileEntityProxyCombo(false, false, false);
		return null;
	}

	@Override
	public int[] getDimensions() {
		return new int[] {2, 0, 5, 5, 1, 1};
	}

	@Override
	public int getOffset() {
		return 5;
	}

	@Override
	public void printHook(Pre event, World world, int x, int y, int z) {
		int[] pos = this.findCore(world, x, y, z);

		if(pos == null) return;

		TileEntity te = world.getTileEntity(pos[0], pos[1], pos[2]);

		if(!(te instanceof TileEntityDysonConverterAnatmogenesis)) return;

		TileEntityDysonConverterAnatmogenesis converter = (TileEntityDysonConverterAnatmogenesis) te;

		List<String> text = new ArrayList<String>();

		if(world.provider.dimensionId == SpaceConfig.kerbolDimension) {
			text.add(EnumChatFormatting.RED + MachineDysonLauncher.getKerbolWarning(world));
			ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getUnlocalizedName() + ".name"), 0xff0000, 0x400000, text);
			return;
		}

		CBT_SkyState skyState = CBT_SkyState.get(world);
		if(skyState.isBlackhole()) {
			EnumChatFormatting color = BobMathUtil.getBlink() ? EnumChatFormatting.YELLOW : EnumChatFormatting.RED;
			text.add(color + "! ! ! CAN'T CREATE ATMOSPHERE");
			text.add(color + "IN BLACK HOLE CONDITIONS ! ! !");
		} else {
			text.add("Fluid: " + converter.fluid.getLocalizedName());
			text.add("Mode: " + (converter.isEmitting ? "Emitting" : "Removing"));
		}

		ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getUnlocalizedName() + ".name"), 0xffff00, 0x404000, text);
	}

	@Override
	public boolean onScrew(World world, EntityPlayer player, int x, int y, int z, int side, float fX, float fY, float fZ, ToolType tool) {
		if(tool != ToolType.SCREWDRIVER) return false;

		if(world.isRemote) return true;

		int[] pos = this.findCore(world, x, y, z);

		if(pos == null) return false;

		TileEntity te = world.getTileEntity(pos[0], pos[1], pos[2]);

		if(!(te instanceof TileEntityDysonConverterAnatmogenesis)) return false;

		TileEntityDysonConverterAnatmogenesis converter = (TileEntityDysonConverterAnatmogenesis) te;
		converter.isEmitting = !converter.isEmitting;
		converter.markDirty();

		return true;
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if(world.isRemote)
			return true;

		if(player.getHeldItem() != null && player.getHeldItem().getItem() instanceof IItemFluidIdentifier) {
			int[] pos = this.findCore(world, x, y, z);

			if(pos == null) return false;

			TileEntity te = world.getTileEntity(pos[0], pos[1], pos[2]);

			if(!(te instanceof TileEntityDysonConverterAnatmogenesis))
				return false;

			TileEntityDysonConverterAnatmogenesis converter = (TileEntityDysonConverterAnatmogenesis) te;
			FluidType type = ((IItemFluidIdentifier) player.getHeldItem().getItem()).getType(world, x, y, z, player.getHeldItem());
			if(type.hasTrait(FT_Gaseous.class) || type.hasTrait(FT_Gaseous_ART.class)) {
				converter.fluid = type;
				converter.markDirty();
				player.addChatComponentMessage(new ChatComponentText("Changed type to ").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.YELLOW)).appendSibling(new ChatComponentTranslation(type.getConditionalName())).appendSibling(new ChatComponentText("!")));
			}

			return true;
		}

		return false;
	}

}

