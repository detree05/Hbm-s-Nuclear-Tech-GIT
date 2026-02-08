package com.hbm.dim.trait;

import com.hbm.dim.CelestialBody;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class CBT_SkyState extends CelestialBodyTrait {

	public static final long DFC_THRESHOLD_HE_PER_SEC = 10_000_000_000_000L;

	public enum SkyState {
		SUN,
		BLACKHOLE,
		NOTHING,
		DFC
	}

	private SkyState state = SkyState.SUN;
	private int blackholeClustersSent;
	private long dfcThroughput;

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

	public long getDfcThroughput() {
		return dfcThroughput;
	}

	public void setDfcThroughput(long throughput) {
		dfcThroughput = Math.max(0, throughput);
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
		nbt.setLong("dfcThroughput", dfcThroughput);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		int ordinal = nbt.getInteger("state");
		SkyState[] values = SkyState.values();
		state = ordinal >= 0 && ordinal < values.length ? values[ordinal] : SkyState.SUN;
		blackholeClustersSent = nbt.getInteger("clusters");
		dfcThroughput = nbt.getLong("dfcThroughput");
	}

	@Override
	public void writeToBytes(ByteBuf buf) {
		buf.writeByte(state.ordinal());
		buf.writeShort(blackholeClustersSent);
		buf.writeLong(dfcThroughput);
	}

	@Override
	public void readFromBytes(ByteBuf buf) {
		int ordinal = buf.readByte();
		SkyState[] values = SkyState.values();
		state = ordinal >= 0 && ordinal < values.length ? values[ordinal] : SkyState.SUN;
		blackholeClustersSent = buf.readShort();
		dfcThroughput = buf.readLong();
	}
}
