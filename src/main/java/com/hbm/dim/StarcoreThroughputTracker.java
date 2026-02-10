package com.hbm.dim;

import java.util.HashMap;
import java.util.Map;

import com.hbm.dim.trait.CBT_SkyState;
import com.hbm.dim.StarcoreSkyEffects;
import com.hbm.dim.trait.CBT_Dyson;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toclient.StarcoreDecaySkyPacket;

import net.minecraft.world.World;

public class StarcoreThroughputTracker {

	private static class Accumulator {
		private long totalThisTick;
		private long lastTickTotal;
		private long totalThisFiveTicks;
		private long lastFiveTickTotal;
		private java.util.Set<Long> injectorsThisFiveTicks;
		private int injectorsLastFiveTicks;
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
			acc.injectorsThisFiveTicks = new java.util.HashSet<>();
			perDimension.put(dim, acc);
		}
		return acc;
	}

	public static void add(World world, long amount) {
		if(world == null || world.isRemote || amount <= 0) return;
		Accumulator acc = get(world);
		acc.totalThisTick += amount;
		acc.totalThisSecond += amount;
		acc.totalThisFiveTicks += amount;
	}

	public static void registerInjectorTick(World world, int x, int y, int z) {
		if(world == null || world.isRemote) return;
		Accumulator acc = get(world);
		acc.injectorsThisFiveTicks.add(packInjectorPos(x, y, z));
	}

	public static void tick(World world) {
		if(world == null || world.isRemote) return;
		long now = world.getTotalWorldTime();

		Accumulator acc = get(world);
		if(acc.lastProcessedTick == now) return;
		acc.lastProcessedTick = now;

		long total = acc.totalThisTick;
		acc.lastTickTotal = total;
		acc.totalThisTick = 0;
		if(now % 5 == 0) {
			acc.lastFiveTickTotal = acc.totalThisFiveTicks;
			acc.totalThisFiveTicks = 0;
			acc.injectorsLastFiveTicks = acc.injectorsThisFiveTicks.size();
			acc.injectorsThisFiveTicks.clear();
		}
		if(now % 20 == 0) {
			acc.lastSecondTotal = acc.totalThisSecond;
			acc.totalThisSecond = 0;
		}
		long effective = Math.min(total, CBT_SkyState.STARCORE_THRESHOLD_HE_PER_TICK);
		long excess = Math.max(0L, total - CBT_SkyState.STARCORE_THRESHOLD_HE_PER_TICK);

		CBT_SkyState skyState = CBT_SkyState.get(world);
		CBT_SkyState.SkyState state = skyState.getState();
		if(state == CBT_SkyState.SkyState.STARCORE) {
			if(effective >= CBT_SkyState.STARCORE_THRESHOLD_HE_PER_TICK) {
				skyState.setState(CBT_SkyState.SkyState.SUN);
				skyState.setStarcoreThroughput(0);
				skyState.setSunCharge(CBT_SkyState.STARCORE_THRESHOLD_HE_PER_TICK * 20L * 10L);
				skyState.setSunLastSustainTick(now);
				StarcoreSkyEffects.sendIgnition(world);
			} else {
				if(skyState.getStarcoreThroughput() != effective) {
					skyState.setStarcoreThroughput(effective);
				}
			}
			CelestialBody.getStar(world).modifyTraits(skyState);
		} else if(state == CBT_SkyState.SkyState.SUN) {
			long lastSustain = skyState.getSunLastSustainTick();
			if(lastSustain <= 0) {
				skyState.setSunLastSustainTick(now);
				CelestialBody.getStar(world).modifyTraits(skyState);
				lastSustain = now;
			}
			if(effective >= CBT_SkyState.STARCORE_THRESHOLD_HE_PER_TICK) {
				if(skyState.getSunLastSustainTick() != now) {
					skyState.setSunLastSustainTick(now);
					CelestialBody.getStar(world).modifyTraits(skyState);
				}
				if(excess > 0) {
					long current = skyState.getSunCharge();
					long next = Math.min(CBT_SkyState.SUN_MAX_HE, current + excess);
					if(next != current) {
						skyState.setSunCharge(next);
						CelestialBody.getStar(world).modifyTraits(skyState);
					}
				}
			}
			long current = skyState.getSunCharge();
			long decay = CBT_SkyState.STARCORE_THRESHOLD_HE_PER_TICK - effective;
			if(decay > 0) {
				long next = Math.max(0L, current - decay);
				if(next != current) {
					skyState.setSunCharge(next);
					CelestialBody.getStar(world).modifyTraits(skyState);
				}
			}
			if(effective < CBT_SkyState.STARCORE_THRESHOLD_HE_PER_TICK && skyState.getSunCharge() <= 0
				&& (now - lastSustain) >= CBT_SkyState.SUN_GRACE_TICKS) {
				skyState.setState(CBT_SkyState.SkyState.NOTHING);
				skyState.setStarcoreThroughput(0);
				skyState.setSunLastSustainTick(0);
				CelestialBody.getStar(world).modifyTraits(skyState);
				CBT_Dyson.clearAll(world);
				if(world.provider != null) {
					PacketDispatcher.wrapper.sendToDimension(
						new StarcoreDecaySkyPacket(world.getTotalWorldTime(), world.provider.dimensionId),
						world.provider.dimensionId
					);
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
			if(skyState.getSunCharge() != 0) {
				skyState.setSunCharge(0);
				CelestialBody.getStar(world).modifyTraits(skyState);
			}
			acc.lastSecondTotal = 0;
		}
	}

	public static long getLastSecondTotal(World world) {
		if(world == null) return 0;
		return get(world).lastSecondTotal;
	}

	public static long getLastTickTotal(World world) {
		if(world == null) return 0;
		return get(world).lastTickTotal;
	}

	public static long getLastFiveTickTotal(World world) {
		if(world == null) return 0;
		return get(world).lastFiveTickTotal;
	}

	public static int getLastFiveTickInjectorCount(World world) {
		if(world == null) return 0;
		return get(world).injectorsLastFiveTicks;
	}

	private static long packInjectorPos(int x, int y, int z) {
		long lx = ((long) x & 0x3FFFFFFL) << 38;
		long lz = ((long) z & 0x3FFFFFFL) << 12;
		long ly = (long) y & 0xFFFL;
		return lx | lz | ly;
	}
}
