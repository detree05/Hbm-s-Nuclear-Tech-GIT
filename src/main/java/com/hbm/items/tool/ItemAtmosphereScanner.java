package com.hbm.items.tool;

import com.hbm.dim.CelestialBody;
import com.hbm.dim.SolarSystem;
import com.hbm.dim.trait.CBT_Atmosphere;
import com.hbm.dim.trait.CBT_Atmosphere.FluidEntry;
import com.hbm.handler.atmosphere.ChunkAtmosphereManager;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toclient.PlayerInformPacket;
import com.hbm.util.BobMathUtil;
import com.hbm.util.ChatBuilder;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

public class ItemAtmosphereScanner extends Item {

	private static final int TOOLTIP_ID_BASE = 700; // keep above generic IDs and below ore density scanner IDs (777+)

	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean inHand) {

		if(!(entity instanceof EntityPlayerMP) || world.getTotalWorldTime() % 5 != 0) return;

		EntityPlayerMP player = (EntityPlayerMP) entity;

		CBT_Atmosphere atmosphere = ChunkAtmosphereManager.proxy.getAtmosphere(entity);
		EnumChatFormatting fluidColor = CelestialBody.getStar(world) == SolarSystem.dmitriy ? EnumChatFormatting.RED : EnumChatFormatting.AQUA;

		boolean hasAtmosphere = false;
		if(atmosphere != null) {
			for(int i = 0; i < atmosphere.fluids.size(); i++) {
				FluidEntry entry = atmosphere.fluids.get(i);
				if(entry.pressure > 0.0001) {
					String pressure = String.format("%.4f", BobMathUtil.roundDecimal(entry.pressure, 4));
					PacketDispatcher.wrapper.sendTo(new PlayerInformPacket(ChatBuilder.startTranslation(entry.fluid.getUnlocalizedName()).color(fluidColor).next(": ").next(pressure + "atm").color(EnumChatFormatting.RESET).flush(), TOOLTIP_ID_BASE + i, 4000), player);
					hasAtmosphere = true;
				}
			}
		}

		if(!hasAtmosphere) {
			PacketDispatcher.wrapper.sendTo(new PlayerInformPacket(ChatBuilder.start("NEAR VACUUM").color(EnumChatFormatting.YELLOW).flush(), TOOLTIP_ID_BASE, 4000), player);
		}
	}

}
