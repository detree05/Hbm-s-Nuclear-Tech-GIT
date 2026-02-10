package com.hbm.dim;

import java.util.HashMap;
import java.util.Map;

import com.hbm.dim.trait.CBT_SkyState;
import com.hbm.dim.StarcoreSkyEffects;

import net.minecraft.world.World;

public class StarcoreThroughputTracker {

	private static class Accumulator {
		private long totalThisSecond;
		private long lastSecondTotal;
		private long lastProcessedTick;
	}

	private static final Map<Integer, Accumulator> perDimension = new HashMap<>();

	private static Accumulator get(World world) {
		int dim = world.provider != null ? world.provider.dimensionId : 0;
		Accumulator acc = perDimension.get(dim);
		if(acc == null) {
			acc = new Accumulator();
			perDimension.put(dim, acc);
		}
		return acc;
	}

	public static void add(World world, long amount) {
		if(world == null || world.isRemote || amount <= 0) return;
		Accumulator acc = get(world);
		acc.totalThisSecond += amount;
	}

	public static void tick(World world) {
		if(world == null || world.isRemote) return;
		long now = world.getTotalWorldTime();
		if(now % 20 != 0) return;

		Accumulator acc = get(world);
		if(acc.lastProcessedTick == now) return;
		acc.lastProcessedTick = now;

		long total = acc.totalThisSecond;
		acc.lastSecondTotal = total;
		acc.totalThisSecond = 0;

		CBT_SkyState skyState = CBT_SkyState.get(world);
		CBT_SkyState.SkyState state = skyState.getState();
		if(state == CBT_SkyState.SkyState.STARCORE) {
			if(total >= CBT_SkyState.STARCORE_THRESHOLD_HE_PER_SEC) {
				skyState.setState(CBT_SkyState.SkyState.SUN);
				skyState.setStarcoreThroughput(0);
				skyState.setSunLastSustainTick(now);
				StarcoreSkyEffects.sendIgnition(world);
			} else {
				skyState.setStarcoreThroughput(total);
			}
			CelestialBody.getStar(world).modifyTraits(skyState);
		} else if(state == CBT_SkyState.SkyState.SUN) {
			if(total >= CBT_SkyState.STARCORE_THRESHOLD_HE_PER_SEC) {
				if(skyState.getSunLastSustainTick() != now) {
					skyState.setSunLastSustainTick(now);
					CelestialBody.getStar(world).modifyTraits(skyState);
				}
			}
			if(skyState.getStarcoreThroughput() != 0) {
				skyState.setStarcoreThroughput(0);
				CelestialBody.getStar(world).modifyTraits(skyState);
			}
		} else {
			if(skyState.getStarcoreThroughput() != 0) {
				skyState.setStarcoreThroughput(0);
				CelestialBody.getStar(world).modifyTraits(skyState);
			}
			acc.lastSecondTotal = 0;
		}
	}

	public static long getLastSecondTotal(World world) {
		if(world == null) return 0;
		return get(world).lastSecondTotal;
	}
}
