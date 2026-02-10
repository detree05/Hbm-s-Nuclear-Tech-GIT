package com.hbm.dim;

import com.hbm.config.SpaceConfig;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toclient.StarcoreIgnitionSkyPacket;

import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

public class StarcoreSkyEffects {

	public static void sendIgnition(World world) {
		if(world == null) return;
		long worldTime = world.getTotalWorldTime();
		for(WorldServer targetWorld : DimensionManager.getWorlds()) {
			if(targetWorld == null || targetWorld.provider == null) continue;
			int dim = targetWorld.provider.dimensionId;
			if(dim == -1 || dim == 1 || dim == SpaceConfig.kerbolDimension) {
				continue;
			}
			PacketDispatcher.wrapper.sendToDimension(
				new StarcoreIgnitionSkyPacket(worldTime, dim),
				dim
			);
		}
	}
}
