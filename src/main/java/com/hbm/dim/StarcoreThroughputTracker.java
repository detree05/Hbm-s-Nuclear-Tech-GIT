package com.hbm.dim;

import java.util.HashMap;
import java.util.Map;

import com.hbm.dim.trait.CBT_SkyState;
import com.hbm.dim.StarcoreSkyEffects;
import com.hbm.dim.trait.CBT_Dyson;
import com.hbm.dim.CelestialBody;
import com.hbm.config.SpaceConfig;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toclient.StarcoreDecaySkyPacket;

import net.minecraft.world.World;
import net.minecraft.world.WorldProviderEnd;
import net.minecraft.world.WorldProviderHell;

public class StarcoreThroughputTracker {

	private static class Accumulator {
		private long totalThisTick;
		private long lastTickTotal;
		private long totalThisFiveTicks;
		private long lastFiveTickTotal;
		private java.util.Set<Long> injectorsThisFiveTicks;
		private int injectorsLastFiveTicks;
		private java.util.Set<Long> injectorsAll;
		private int injectorsAllCount;
		private java.util.Set<Long> injectorsThisSecond;
		private int injectorsLastSecond;
		private long totalThisSecond;
		private long lastSecondTotal;
		private long lastProcessedTick;
		private Map<Integer, java.util.Set<Long>> injectorsThisSecondByDim;
		private Map<Integer, Integer> injectorsByDimension;
		private boolean injectorsByDimensionDirty;
		private long lastInjectorSyncTick;
	}

	private static final Map<String, Accumulator> perStar = new HashMap<>();
	private static final Map<String, Integer> loadedWorldRefs = new HashMap<>();

	private static String getStarName(World world) {
		return CelestialBody.getStar(world).name;
	}

	private static Accumulator get(World world) {
		String starName = getStarName(world);
		Accumulator acc = perStar.get(starName);
		if(acc == null) {
			acc = new Accumulator();
			acc.injectorsThisFiveTicks = new java.util.HashSet<>();
			acc.injectorsThisSecond = new java.util.HashSet<>();
			acc.injectorsAll = new java.util.HashSet<>();
			acc.injectorsThisSecondByDim = new HashMap<>();
			acc.injectorsByDimension = new HashMap<>();
			perStar.put(starName, acc);
		}
		return acc;
	}

	public static void onWorldLoad(World world) {
		if(world == null || world.isRemote) return;
		String starName = getStarName(world);
		Integer refs = loadedWorldRefs.get(starName);
		loadedWorldRefs.put(starName, refs == null ? 1 : refs.intValue() + 1);
	}

	public static void onWorldUnload(World world) {
		if(world == null || world.isRemote) return;
		String starName = getStarName(world);
		Integer refs = loadedWorldRefs.get(starName);
		if(refs == null || refs.intValue() <= 1) {
			loadedWorldRefs.remove(starName);
			perStar.remove(starName);
		} else {
			loadedWorldRefs.put(starName, refs.intValue() - 1);
		}
	}

	private static void resetAccumulator(Accumulator acc) {
		acc.totalThisTick = 0;
		acc.lastTickTotal = 0;
		acc.totalThisFiveTicks = 0;
		acc.lastFiveTickTotal = 0;
		acc.injectorsThisFiveTicks.clear();
		acc.injectorsLastFiveTicks = 0;
		acc.injectorsAll.clear();
		acc.injectorsAllCount = 0;
		acc.injectorsThisSecond.clear();
		acc.injectorsLastSecond = 0;
		acc.totalThisSecond = 0;
		acc.lastSecondTotal = 0;
		acc.lastProcessedTick = -1L;
		acc.injectorsThisSecondByDim.clear();
		acc.injectorsByDimension = new HashMap<>();
		acc.injectorsByDimensionDirty = true;
		acc.lastInjectorSyncTick = -20L;
	}

	public static void add(World world, long amount) {
		if(world == null || world.isRemote || amount <= 0) return;
		Accumulator acc = get(world);
		acc.totalThisTick += amount;
		acc.totalThisSecond += amount;
		acc.totalThisFiveTicks += amount;
	}

	public static long getRemainingCapacity(World world, long perTickCap) {
		if(world == null || world.isRemote || perTickCap <= 0) return 0;
		Accumulator acc = get(world);
		long remaining = perTickCap - acc.totalThisTick;
		return Math.max(0L, remaining);
	}

	public static void registerInjectorTick(World world, int x, int y, int z) {
		if(world == null || world.isRemote) return;
		Accumulator acc = get(world);
		int dim = world.provider != null ? world.provider.dimensionId : 0;
		long packed = packInjectorPos(dim, x, y, z);
		acc.injectorsThisFiveTicks.add(packed);
		acc.injectorsThisSecond.add(packed);
		if(shouldCountForSkyLines(world)) {
			long packedNoDim = packInjectorPosNoDim(x, y, z);
			java.util.Set<Long> set = acc.injectorsThisSecondByDim.get(dim);
			if(set == null) {
				set = new java.util.HashSet<>();
				acc.injectorsThisSecondByDim.put(dim, set);
			}
			set.add(packedNoDim);
		}
	}

	public static void registerInjector(World world, int x, int y, int z) {
		if(world == null || world.isRemote) return;
		Accumulator acc = get(world);
		int dim = world.provider != null ? world.provider.dimensionId : 0;
		long packed = packInjectorPos(dim, x, y, z);
		if(acc.injectorsAll.add(packed)) {
			acc.injectorsAllCount = acc.injectorsAll.size();
		}
	}

	public static void unregisterInjector(World world, int x, int y, int z) {
		if(world == null || world.isRemote) return;
		Accumulator acc = get(world);
		int dim = world.provider != null ? world.provider.dimensionId : 0;
		long packed = packInjectorPos(dim, x, y, z);
		if(acc.injectorsAll.remove(packed)) {
			acc.injectorsAllCount = acc.injectorsAll.size();
		}
	}

	public static void tick(World world) {
		if(world == null || world.isRemote) return;
		long now = world.getTotalWorldTime();

		Accumulator acc = get(world);
		if(acc.lastProcessedTick > now) {
			resetAccumulator(acc);
		}
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
			acc.injectorsLastSecond = acc.injectorsThisSecond.size();
			acc.injectorsThisSecond.clear();
			if(acc.injectorsThisSecondByDim.isEmpty()) {
				if(!acc.injectorsByDimension.isEmpty()) {
					acc.injectorsByDimension = new HashMap<>();
					acc.injectorsByDimensionDirty = true;
				}
			} else {
				HashMap<Integer, Integer> next = new HashMap<>();
				for(Map.Entry<Integer, java.util.Set<Long>> entry : acc.injectorsThisSecondByDim.entrySet()) {
					int count = entry.getValue() != null ? entry.getValue().size() : 0;
					if(count > 0) {
						next.put(entry.getKey(), count);
					}
				}
				acc.injectorsByDimension = next;
				acc.injectorsByDimensionDirty = true;
				acc.injectorsThisSecondByDim.clear();
			}
		}
		long threshold = CBT_SkyState.STARCORE_THRESHOLD_HE_PER_TICK;
		long avgTotal = acc.lastSecondTotal > 0 ? acc.lastSecondTotal / 20L : total;
		long effective = Math.min(avgTotal, threshold);
		long excess = Math.max(0L, total - threshold);

		CBT_SkyState skyState = CBT_SkyState.get(world);
		CBT_SkyState.SkyState state = skyState.getState();
		if(state == CBT_SkyState.SkyState.STARCORE) {
			if(avgTotal >= threshold) {
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
			if(avgTotal >= threshold) {
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
		syncInjectorCounts(world, acc, now);
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

	public static int getLastSecondInjectorCount(World world) {
		if(world == null) return 0;
		return get(world).injectorsLastSecond;
	}

	public static int getRegisteredInjectorCount(World world) {
		if(world == null) return 0;
		return get(world).injectorsAllCount;
	}

	private static long packInjectorPos(int dim, int x, int y, int z) {
		long lx = ((long) x & 0x3FFFFFFL) << 38;
		long lz = ((long) z & 0x3FFFFFFL) << 12;
		long ly = (long) y & 0xFFFL;
		long pos = lx | lz | ly;
		long d = ((long) dim & 0xFFFFFL) << 44;
		return pos ^ d;
	}

	private static long packInjectorPosNoDim(int x, int y, int z) {
		long lx = ((long) x & 0x3FFFFFFL) << 38;
		long lz = ((long) z & 0x3FFFFFFL) << 12;
		long ly = (long) y & 0xFFFL;
		return lx | lz | ly;
	}

	private static boolean shouldCountForSkyLines(World world) {
		if(world == null || world.provider == null) return false;
		if(world.provider instanceof WorldProviderHell || world.provider instanceof WorldProviderEnd) return false;
		if(world.provider.dimensionId == SpaceConfig.kerbolDimension) return false;
		return world.provider.dimensionId != SpaceConfig.orbitDimension;
	}

	private static void syncInjectorCounts(World world, Accumulator acc, long now) {
		if(!acc.injectorsByDimensionDirty) return;
		if(now - acc.lastInjectorSyncTick < 20L) return;

		CBT_SkyState skyState = CBT_SkyState.get(world);
		if(skyState.setInjectorCounts(acc.injectorsByDimension)) {
			CelestialBody.getStar(world).modifyTraits(skyState);
		}
		acc.injectorsByDimensionDirty = false;
		acc.lastInjectorSyncTick = now;
	}
}
