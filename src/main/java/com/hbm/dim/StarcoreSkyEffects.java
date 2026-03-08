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

	public static final int BLACKHOLE_PRE_COLLAPSE_PULSE_TICKS = 138 * 20;
	public static final int BLACKHOLE_FINAL_COLLAPSE_TICKS = 5 * 20;
	public static final int BLACKHOLE_COLLAPSE_DURATION_TICKS = BLACKHOLE_PRE_COLLAPSE_PULSE_TICKS + BLACKHOLE_FINAL_COLLAPSE_TICKS;
	public static final int BLACKHOLE_GRAVITY_MALFUNCTION_DELAY_TICKS = 28 * 20;
	public static final int BLACKHOLE_GRAVITY_MALFUNCTION_RAMP_TICKS = 15 * 20;

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

	public static float getGravityMalfunctionProgress(long now, long collapseEndTick) {
		if(collapseEndTick <= 0L) {
			return 0.0F;
		}

		long collapseStartTick = collapseEndTick - BLACKHOLE_COLLAPSE_DURATION_TICKS;
		long gravityStartTick = collapseStartTick + BLACKHOLE_GRAVITY_MALFUNCTION_DELAY_TICKS;
		if(now < gravityStartTick || now >= collapseEndTick) {
			return 0.0F;
		}

		long gravityRampEndTick = gravityStartTick + BLACKHOLE_GRAVITY_MALFUNCTION_RAMP_TICKS;
		if(now >= gravityRampEndTick || BLACKHOLE_GRAVITY_MALFUNCTION_RAMP_TICKS <= 0) {
			return 1.0F;
		}

		float progress = (float)(now - gravityStartTick) / (float)BLACKHOLE_GRAVITY_MALFUNCTION_RAMP_TICKS;
		return Math.max(0.0F, Math.min(progress, 1.0F));
	}

	public static boolean isGravityMalfunctionActive(long now, long collapseEndTick) {
		if(collapseEndTick <= 0L) {
			return false;
		}
		long collapseStartTick = collapseEndTick - BLACKHOLE_COLLAPSE_DURATION_TICKS;
		long gravityStartTick = collapseStartTick + BLACKHOLE_GRAVITY_MALFUNCTION_DELAY_TICKS;
		return now >= gravityStartTick && now < collapseEndTick;
	}

	public static boolean startBlackholeCollapse(World world, CBT_SkyState skyState) {
		if(world == null || skyState == null) return false;
		if(world.provider != null && world.provider.dimensionId == SpaceConfig.dmitriyDimension) return false;
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

