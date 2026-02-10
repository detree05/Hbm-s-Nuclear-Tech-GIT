package com.hbm.blocks.machine;

import java.util.ArrayList;
import java.util.List;

import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.dim.StarcoreThroughputTracker;
import com.hbm.dim.trait.CBT_SkyState;
import com.hbm.items.ModItems;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.machine.TileEntityStarCoreEnergyInjector;
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

public class MachineStarCoreEnergyInjector extends BlockContainer implements ILookOverlay, ITooltipProvider {

	public MachineStarCoreEnergyInjector(Material mat) {
		super(mat);
		this.setLightOpacity(0);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityStarCoreEnergyInjector();
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
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if(world.isRemote) {
			return true;
		}

		TileEntity te = world.getTileEntity(x, y, z);
		if(!(te instanceof TileEntityStarCoreEnergyInjector)) return false;
		TileEntityStarCoreEnergyInjector injector = (TileEntityStarCoreEnergyInjector) te;

		ItemStack heldStack = player.getHeldItem();

		if(heldStack != null && heldStack.getItem() == ModItems.sat_chip) {
			if(injector.slots[0] != null) return false;

			injector.slots[0] = heldStack.copy();
			injector.markChanged();
			heldStack.stackSize = 0;
			world.playSoundEffect(x, y, z, "hbm:item.upgradePlug", 1.0F, 1.0F);
		} else if(heldStack == null && injector.slots[0] != null) {
			if(player.inventory.addItemStackToInventory(injector.slots[0].copy())) {
				injector.slots[0] = null;
				injector.markChanged();
				world.playSoundEffect(x, y, z, "hbm:item.upgradePlug", 1.0F, 1.0F);
			}
		}

		return true;
	}

	@Override
	public void printHook(Pre event, World world, int x, int y, int z) {
		TileEntity te = world.getTileEntity(x, y, z);
		if(!(te instanceof TileEntityStarCoreEnergyInjector)) return;

		TileEntityStarCoreEnergyInjector injector = (TileEntityStarCoreEnergyInjector) te;
		List<String> text = new ArrayList<>();

		if(injector.getChipFreq() <= 0) {
			text.add("No Satellite ID-Chip installed!");
			ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getUnlocalizedName() + ".name"), 0xffff00, 0x404000, text);
			return;
		}

		CBT_SkyState skyState = CBT_SkyState.get(world);
		CBT_SkyState.SkyState state = skyState.getState();
		if(state == CBT_SkyState.SkyState.BLACKHOLE || state == CBT_SkyState.SkyState.NOTHING) {
			EnumChatFormatting color = BobMathUtil.getBlink() ? EnumChatFormatting.YELLOW : EnumChatFormatting.RED;
			text.add(color + "Nothing to shoot beam at... yet.");
		} else {
			long injectorPerSecond = injector.getThroughputPerFiveTicks() * 4L;
			long totalPerSecond = StarcoreThroughputTracker.getLastFiveTickTotal(world) * 4L;
			text.add("Current power: " + BobMathUtil.getShortNumber(injectorPerSecond) + "HE/s");
			text.add("Injectors: " + StarcoreThroughputTracker.getLastFiveTickInjectorCount(world) + " injectors");
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
