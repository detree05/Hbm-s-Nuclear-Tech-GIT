package com.hbm.dim.trait;

import com.hbm.dim.SolarSystemWorldSavedData;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;

public class CBT_Destroyed extends CelestialBodyTrait {

	public float interp;
	private boolean completed;

	public CBT_Destroyed() {}

	public CBT_Destroyed(float interp) {
		this.interp = interp;
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		nbt.setFloat("interp", interp);
		nbt.setBoolean("completed", completed);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		interp = nbt.getFloat("interp");
		completed = nbt.getBoolean("completed");
		if(completed) {
			interp = 200.0f;
		}
	}

	@Override
	public void writeToBytes(ByteBuf buf) {
		buf.writeFloat(interp);
		buf.writeBoolean(completed);
	}

	@Override
	public void readFromBytes(ByteBuf buf) {
		interp = buf.readFloat();
		completed = buf.readBoolean();
		if(completed) {
			interp = 200.0f;
		}
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
