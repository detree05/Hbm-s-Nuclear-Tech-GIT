package com.hbm.dim.trait;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.hbm.dim.CelestialBody;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class CBT_SkyState extends CelestialBodyTrait {

	public static final long STARCORE_THRESHOLD_HE_PER_TICK = 500_000_000_000_000L / 20L;
	public static final long SUN_SUPPORT_MIN_HE_PER_TICK = STARCORE_THRESHOLD_HE_PER_TICK;
	public static final long SUN_SUPPORT_MAX_HE_PER_TICK = 3_000_000_000_000_000L / 20L;
	public static final int SUN_BUILDUP_TICKS = 5 * 24000;
	public static final long SUN_BUILDUP_SECONDS = SUN_BUILDUP_TICKS / 20L;
	public static final long SUN_MAX_HE = STARCORE_THRESHOLD_HE_PER_TICK * 20L * SUN_BUILDUP_SECONDS;
	public static final int SUN_GRACE_TICKS = 10 * 20;

	public enum SkyState {
		SUN,
		BLACKHOLE,
		NOTHING,
		STARCORE
	}

	private SkyState state = SkyState.SUN;
	private int blackholeClustersSent;
	private long starcoreThroughput;
	private long sunCharge;
	private long sunLastSustainTick;
	private Map<Integer, Integer> injectorCounts = new HashMap<>();

	public CBT_SkyState() { }

	public CBT_SkyState(SkyState state) {
		this.state = state;
	}

	public SkyState getState() {
		return state;
	}

	public void setState(SkyState state) {
		this.state = state;
	}

	public boolean isBlackhole() {
		return state == SkyState.BLACKHOLE;
	}

	public boolean isNothing() {
		return state == SkyState.NOTHING;
	}

	public boolean isSun() {
		return state == SkyState.SUN;
	}

	public int getBlackholeClustersSent() {
		return blackholeClustersSent;
	}

	public void addBlackholeClusters(int amount) {
		if(amount <= 0) return;
		blackholeClustersSent += amount;
	}

	public void setBlackholeClustersSent(int sent) {
		blackholeClustersSent = Math.max(0, sent);
	}

	public long getStarcoreThroughput() {
		return starcoreThroughput;
	}

	public void setStarcoreThroughput(long throughput) {
		starcoreThroughput = Math.max(0, throughput);
	}

	public long getSunLastSustainTick() {
		return sunLastSustainTick;
	}

	public void setSunLastSustainTick(long tick) {
		sunLastSustainTick = Math.max(0, tick);
	}

	public long getSunCharge() {
		return sunCharge;
	}

	public void setSunCharge(long charge) {
		sunCharge = Math.max(0, charge);
	}

	public int getInjectorCount(int dimensionId) {
		Integer count = injectorCounts.get(dimensionId);
		return count != null ? count.intValue() : 0;
	}

	public boolean setInjectorCounts(Map<Integer, Integer> counts) {
		HashMap<Integer, Integer> next = new HashMap<>();
		if(counts != null) {
			for(Map.Entry<Integer, Integer> entry : counts.entrySet()) {
				if(entry.getValue() != null && entry.getValue().intValue() > 0) {
					next.put(entry.getKey(), entry.getValue());
				}
			}
		}
		if(next.equals(injectorCounts)) {
			return false;
		}
		injectorCounts = next;
		return true;
	}

	public static CBT_SkyState get(World world) {
		CelestialBody star = CelestialBody.getStar(world);
		CBT_SkyState sky = star.getTrait(CBT_SkyState.class);
		if(sky == null) {
			sky = new CBT_SkyState(SkyState.BLACKHOLE);
		}
		return sky;
	}

	public static void setState(World world, SkyState state) {
		CelestialBody star = CelestialBody.getStar(world);
		CBT_SkyState sky = star.getTrait(CBT_SkyState.class);
		if(sky == null) {
			sky = new CBT_SkyState(state);
		} else {
			sky.setState(state);
		}
		star.modifyTraits(sky);
	}

	public static boolean isBlackhole(World world) {
		return get(world).isBlackhole();
	}

	public static boolean isNothing(World world) {
		return get(world).isNothing();
	}

	public static long getSunSupportRequirementPerTick(long sunCharge) {
		if(SUN_MAX_HE <= 0) {
			return SUN_SUPPORT_MIN_HE_PER_TICK;
		}

		long clampedCharge = Math.max(0L, Math.min(sunCharge, SUN_MAX_HE));
		if(clampedCharge <= 0L) {
			return SUN_SUPPORT_MIN_HE_PER_TICK;
		}
		if(clampedCharge >= SUN_MAX_HE) {
			return SUN_SUPPORT_MAX_HE_PER_TICK;
		}

		double ratio = (double) clampedCharge / (double) SUN_MAX_HE;
		double scaled = SUN_SUPPORT_MIN_HE_PER_TICK
			+ (SUN_SUPPORT_MAX_HE_PER_TICK - SUN_SUPPORT_MIN_HE_PER_TICK) * ratio;
		long requirement = Math.round(scaled);
		return Math.max(SUN_SUPPORT_MIN_HE_PER_TICK, Math.min(requirement, SUN_SUPPORT_MAX_HE_PER_TICK));
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		nbt.setInteger("state", state.ordinal());
		nbt.setInteger("clusters", blackholeClustersSent);
		nbt.setLong("starcoreThroughput", starcoreThroughput);
		nbt.setLong("sunCharge", sunCharge);
		nbt.setLong("sunLastSustain", sunLastSustainTick);
		if(!injectorCounts.isEmpty()) {
			NBTTagCompound injectors = new NBTTagCompound();
			for(Map.Entry<Integer, Integer> entry : injectorCounts.entrySet()) {
				if(entry.getValue() != null && entry.getValue().intValue() > 0) {
					injectors.setInteger(String.valueOf(entry.getKey()), entry.getValue().intValue());
				}
			}
			nbt.setTag("injectorCounts", injectors);
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		int ordinal = nbt.getInteger("state");
		SkyState[] values = SkyState.values();
		state = ordinal >= 0 && ordinal < values.length ? values[ordinal] : SkyState.SUN;
		blackholeClustersSent = nbt.getInteger("clusters");
		starcoreThroughput = nbt.getLong("starcoreThroughput");
		sunCharge = nbt.getLong("sunCharge");
		sunLastSustainTick = nbt.getLong("sunLastSustain");
		injectorCounts.clear();
		if(nbt.hasKey("injectorCounts")) {
			NBTTagCompound injectors = nbt.getCompoundTag("injectorCounts");
			@SuppressWarnings("unchecked")
			Set<String> keys = injectors.func_150296_c();
			for(String key : keys) {
				try {
					int dim = Integer.parseInt(key);
					int count = injectors.getInteger(key);
					if(count > 0) {
						injectorCounts.put(dim, count);
					}
				} catch(NumberFormatException ex) {
					// ignore malformed keys
				}
			}
		}
	}

	@Override
	public void writeToBytes(ByteBuf buf) {
		buf.writeByte(state.ordinal());
		buf.writeShort(blackholeClustersSent);
		buf.writeLong(starcoreThroughput);
		buf.writeLong(sunCharge);
		buf.writeLong(sunLastSustainTick);
		buf.writeInt(injectorCounts.size());
		for(Map.Entry<Integer, Integer> entry : injectorCounts.entrySet()) {
			buf.writeInt(entry.getKey().intValue());
			buf.writeInt(entry.getValue().intValue());
		}
	}

	@Override
	public void readFromBytes(ByteBuf buf) {
		int ordinal = buf.readByte();
		SkyState[] values = SkyState.values();
		state = ordinal >= 0 && ordinal < values.length ? values[ordinal] : SkyState.SUN;
		blackholeClustersSent = buf.readShort();
		starcoreThroughput = buf.readLong();
		sunCharge = buf.readLong();
		sunLastSustainTick = buf.readLong();
		injectorCounts.clear();
		int size = buf.readInt();
		for(int i = 0; i < size; i++) {
			int dim = buf.readInt();
			int count = buf.readInt();
			if(count > 0) {
				injectorCounts.put(dim, count);
			}
		}
	}
}
