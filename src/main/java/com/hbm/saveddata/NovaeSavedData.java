package com.hbm.saveddata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

public class NovaeSavedData extends WorldSavedData {

	public static final String key = "novaeSky";

	public static class NovaeEntry {
		public final long startWorldTime;
		public final float yaw;
		public final float pitch;
		public final float roll;
		public final float r;
		public final float g;
		public final float b;
		public final float sizeScale;

		public NovaeEntry(long startWorldTime, float yaw, float pitch, float roll, float r, float g, float b, float sizeScale) {
			this.startWorldTime = startWorldTime;
			this.yaw = yaw;
			this.pitch = pitch;
			this.roll = roll;
			this.r = r;
			this.g = g;
			this.b = b;
			this.sizeScale = sizeScale;
		}
	}

	private final List<NovaeEntry> entries = new ArrayList<>();

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

	public List<NovaeEntry> getEntries() {
		return Collections.unmodifiableList(entries);
	}

	public void addEntry(long startWorldTime, float yaw, float pitch, float roll, float r, float g, float b, float sizeScale) {
		entries.add(new NovaeEntry(startWorldTime, yaw, pitch, roll, r, g, b, sizeScale));
		markDirty();
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		entries.clear();

		if(compound.hasKey("entries", 9)) {
			NBTTagList list = compound.getTagList("entries", 10);
			for(int i = 0; i < list.tagCount(); i++) {
				NBTTagCompound entry = list.getCompoundTagAt(i);
				long startWorldTime = entry.getLong("startWorldTime");
				float yaw = entry.getFloat("yaw");
				float pitch = entry.getFloat("pitch");
				float roll = entry.getFloat("roll");
				float r = entry.hasKey("r") ? entry.getFloat("r") : 1.0F;
				float g = entry.hasKey("g") ? entry.getFloat("g") : 1.0F;
				float b = entry.hasKey("b") ? entry.getFloat("b") : 1.0F;
				float sizeScale = entry.hasKey("sizeScale") ? entry.getFloat("sizeScale") : 1.0F;
				entries.add(new NovaeEntry(startWorldTime, yaw, pitch, roll, r, g, b, sizeScale));
			}
			return;
		}

		if(compound.getBoolean("active")) {
			long startWorldTime = compound.getLong("startWorldTime");
			float yaw = compound.getFloat("yaw");
			float pitch = compound.getFloat("pitch");
			float roll = compound.getFloat("roll");
			float r = compound.hasKey("r") ? compound.getFloat("r") : 1.0F;
			float g = compound.hasKey("g") ? compound.getFloat("g") : 1.0F;
			float b = compound.hasKey("b") ? compound.getFloat("b") : 1.0F;
			float sizeScale = compound.hasKey("sizeScale") ? compound.getFloat("sizeScale") : 1.0F;
			entries.add(new NovaeEntry(startWorldTime, yaw, pitch, roll, r, g, b, sizeScale));
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		NBTTagList list = new NBTTagList();
		for(NovaeEntry entry : entries) {
			NBTTagCompound compound = new NBTTagCompound();
			compound.setLong("startWorldTime", entry.startWorldTime);
			compound.setFloat("yaw", entry.yaw);
			compound.setFloat("pitch", entry.pitch);
			compound.setFloat("roll", entry.roll);
			compound.setFloat("r", entry.r);
			compound.setFloat("g", entry.g);
			compound.setFloat("b", entry.b);
			compound.setFloat("sizeScale", entry.sizeScale);
			list.appendTag(compound);
		}
		nbt.setTag("entries", list);

		// Keep legacy fields for compatibility with existing saves/tools.
		nbt.setBoolean("active", !entries.isEmpty());
		if(!entries.isEmpty()) {
			NovaeEntry first = entries.get(0);
			nbt.setLong("startWorldTime", first.startWorldTime);
			nbt.setFloat("yaw", first.yaw);
			nbt.setFloat("pitch", first.pitch);
			nbt.setFloat("roll", first.roll);
			nbt.setFloat("r", first.r);
			nbt.setFloat("g", first.g);
			nbt.setFloat("b", first.b);
			nbt.setFloat("sizeScale", first.sizeScale);
		}
	}
}
