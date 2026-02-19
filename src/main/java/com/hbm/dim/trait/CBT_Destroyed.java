package com.hbm.dim.trait;

import com.hbm.dim.SolarSystemWorldSavedData;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;

public class CBT_Destroyed extends CelestialBodyTrait {

	private static final int MUN_RING_TRANSITION_TICKS = 10 * 20;

	public float interp;
	private boolean completed;
	private long munRingTransitionStartTick = -1L;
	private boolean munRingsEnabled;

	public CBT_Destroyed() {}

	public CBT_Destroyed(float interp) {
		this.interp = interp;
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		nbt.setFloat("interp", interp);
		nbt.setBoolean("completed", completed);
		nbt.setLong("munRingTransitionStartTick", munRingTransitionStartTick);
		nbt.setBoolean("munRingsEnabled", munRingsEnabled);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		interp = nbt.getFloat("interp");
		completed = nbt.getBoolean("completed");
		munRingTransitionStartTick = nbt.hasKey("munRingTransitionStartTick") ? nbt.getLong("munRingTransitionStartTick") : -1L;
		munRingsEnabled = nbt.hasKey("munRingsEnabled") ? nbt.getBoolean("munRingsEnabled") : completed;
		if(completed) {
			interp = 200.0f;
		}
	}

	@Override
	public void writeToBytes(ByteBuf buf) {
		buf.writeFloat(interp);
		buf.writeBoolean(completed);
		buf.writeLong(munRingTransitionStartTick);
		buf.writeBoolean(munRingsEnabled);
	}

	@Override
	public void readFromBytes(ByteBuf buf) {
		interp = buf.readFloat();
		completed = buf.readBoolean();
		munRingTransitionStartTick = buf.readLong();
		munRingsEnabled = buf.readBoolean();
		if(completed) {
			interp = 200.0f;
		}
	}

	public void beginMunRingTransition(long worldTime) {
		if(munRingTransitionStartTick < 0L) {
			munRingTransitionStartTick = worldTime;
		}
	}

	public boolean hasMunRingTransitionStarted() {
		return munRingTransitionStartTick >= 0L;
	}

	public float getMunRingTransitionProgress(long worldTime) {
		if(munRingsEnabled) {
			return 1.0F;
		}
		if(munRingTransitionStartTick < 0L) {
			return 0.0F;
		}
		float elapsed = worldTime - munRingTransitionStartTick;
		return Math.max(0.0F, Math.min(1.0F, elapsed / MUN_RING_TRANSITION_TICKS));
	}

	public boolean tryFinalizeMunRings(long worldTime) {
		if(munRingsEnabled) {
			return false;
		}
		if(getMunRingTransitionProgress(worldTime) >= 1.0F) {
			munRingsEnabled = true;
			return true;
		}
		return false;
	}

	public boolean areMunRingsEnabled() {
		return munRingsEnabled;
	}

	@Override
	public void update(boolean isRemote) {
		if(completed) return;

		interp = Math.min(201.0f, interp + 0.0025f * (201.0f - interp) * 0.15f);
		// Clamp at end so the shatter animation plays once and stays finished.
		if(interp >= 200) {
			interp = 200;
			completed = true;
			if(!isRemote) {
				SolarSystemWorldSavedData.get().markDirty();
			}
		}
	}

}
