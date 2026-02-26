package com.hbm.dim;

import com.hbm.config.SpaceConfig;
import com.hbm.dim.trait.CBT_SkyState;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toclient.BlackholeCollapseSkyPacket;
import com.hbm.packet.toclient.StarcoreDecaySkyPacket;
import com.hbm.packet.toclient.StarcoreIgnitionSkyPacket;

import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

public class StarcoreSkyEffects {

	public static final int BLACKHOLE_COLLAPSE_DURATION_TICKS = 260;

	public static void sendIgnition(World world) {
		if(world == null) return;
		long worldTime = world.getTotalWorldTime();
		for(WorldServer targetWorld : DimensionManager.getWorlds()) {
			if(targetWorld == null || targetWorld.provider == null) continue;
			int dim = targetWorld.provider.dimensionId;
			if(dim == -1 || dim == 1 || dim == SpaceConfig.dmitriyDimension) {
				continue;
			}
			PacketDispatcher.wrapper.sendToDimension(
				new StarcoreIgnitionSkyPacket(worldTime, dim),
				dim
			);
		}
	}

	public static void sendDecay(World world) {
		if(world == null) return;
		long worldTime = world.getTotalWorldTime();
		for(WorldServer targetWorld : DimensionManager.getWorlds()) {
			if(targetWorld == null || targetWorld.provider == null) continue;
			int dim = targetWorld.provider.dimensionId;
			if(dim == -1 || dim == 1 || dim == SpaceConfig.dmitriyDimension) {
				continue;
			}
			PacketDispatcher.wrapper.sendToDimension(
				new StarcoreDecaySkyPacket(worldTime, dim),
				dim
			);
		}
	}

	public static void sendBlackholeCollapse(World world) {
		if(world == null) return;
		long worldTime = world.getTotalWorldTime();
		for(WorldServer targetWorld : DimensionManager.getWorlds()) {
			if(targetWorld == null || targetWorld.provider == null) continue;
			int dim = targetWorld.provider.dimensionId;
			if(dim == -1 || dim == 1 || dim == SpaceConfig.dmitriyDimension) {
				continue;
			}
			PacketDispatcher.wrapper.sendToDimension(
				new BlackholeCollapseSkyPacket(worldTime, dim),
				dim
			);
		}
	}

	public static boolean startBlackholeCollapse(World world, CBT_SkyState skyState) {
		if(world == null || skyState == null) return false;
		if(skyState.getState() != CBT_SkyState.SkyState.BLACKHOLE) return false;
		if(skyState.getBlackholeCollapseEndTick() > 0L) {
			return false;
		}

		long now = world.getTotalWorldTime();
		skyState.setBlackholeCollapseEndTick(now + BLACKHOLE_COLLAPSE_DURATION_TICKS);
		CelestialBody.getStar(world).modifyTraits(skyState);
		sendBlackholeCollapse(world);
		return true;
	}
}

