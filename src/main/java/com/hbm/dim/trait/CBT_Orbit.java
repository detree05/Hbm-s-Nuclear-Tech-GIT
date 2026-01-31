package com.hbm.dim.trait;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;

public class CBT_Orbit extends CelestialBodyTrait {

	public float axialTilt;
	public float semiMajorAxisKm;

	public CBT_Orbit() { }

	public CBT_Orbit(float axialTilt, float semiMajorAxisKm) {
		this.axialTilt = axialTilt;
		this.semiMajorAxisKm = semiMajorAxisKm;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		axialTilt = nbt.getFloat("axialTilt");
		semiMajorAxisKm = nbt.getFloat("semiMajorAxisKm");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		nbt.setFloat("axialTilt", axialTilt);
		nbt.setFloat("semiMajorAxisKm", semiMajorAxisKm);
	}

	@Override
	public void readFromBytes(ByteBuf buf) {
		axialTilt = buf.readFloat();
		semiMajorAxisKm = buf.readFloat();
	}

	@Override
	public void writeToBytes(ByteBuf buf) {
		buf.writeFloat(axialTilt);
		buf.writeFloat(semiMajorAxisKm);
	}
}
