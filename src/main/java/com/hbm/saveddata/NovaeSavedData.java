package com.hbm.saveddata;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

public class NovaeSavedData extends WorldSavedData {

	public static final String key = "novaeSky";

	public boolean active;
	public long startWorldTime;
	public float yaw;
	public float pitch;
	public float roll;

	public static NovaeSavedData forWorld(World world) {
		NovaeSavedData result = (NovaeSavedData) world.perWorldStorage.loadData(NovaeSavedData.class, key);
		if(result == null) {
			world.perWorldStorage.setData(key, new NovaeSavedData(key));
			result = (NovaeSavedData) world.perWorldStorage.loadData(NovaeSavedData.class, key);
		}
		return result;
	}

	public NovaeSavedData(String tagName) {
		super(tagName);
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		this.active = compound.getBoolean("active");
		this.startWorldTime = compound.getLong("startWorldTime");
		this.yaw = compound.getFloat("yaw");
		this.pitch = compound.getFloat("pitch");
		this.roll = compound.getFloat("roll");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		nbt.setBoolean("active", active);
		nbt.setLong("startWorldTime", startWorldTime);
		nbt.setFloat("yaw", yaw);
		nbt.setFloat("pitch", pitch);
		nbt.setFloat("roll", roll);
	}
}
