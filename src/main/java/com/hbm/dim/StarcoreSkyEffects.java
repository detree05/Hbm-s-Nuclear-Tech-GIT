package com.hbm.dim;

import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toclient.StarcoreIgnitionSkyPacket;

import net.minecraft.world.World;

public class StarcoreSkyEffects {

	public static void sendIgnition(World world) {
		if(world == null || world.provider == null) return;
		PacketDispatcher.wrapper.sendToDimension(
			new StarcoreIgnitionSkyPacket(world.getTotalWorldTime(), world.provider.dimensionId),
			world.provider.dimensionId
		);
	}
}
