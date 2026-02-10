package com.hbm.dim.trait;

import com.hbm.dim.CelestialBody;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class CBT_SkyState extends CelestialBodyTrait {

	public static final long STARCORE_THRESHOLD_HE_PER_TICK = 500_000_000_000_000L / 20L;
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

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		nbt.setInteger("state", state.ordinal());
		nbt.setInteger("clusters", blackholeClustersSent);
		nbt.setLong("starcoreThroughput", starcoreThroughput);
		nbt.setLong("sunCharge", sunCharge);
		nbt.setLong("sunLastSustain", sunLastSustainTick);
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
	}

	@Override
	public void writeToBytes(ByteBuf buf) {
		buf.writeByte(state.ordinal());
		buf.writeShort(blackholeClustersSent);
		buf.writeLong(starcoreThroughput);
		buf.writeLong(sunCharge);
		buf.writeLong(sunLastSustainTick);
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
	}
}
