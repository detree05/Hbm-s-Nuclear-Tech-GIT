package com.hbm.dim.kerbol;

import com.hbm.dim.WorldProviderCelestial;
import com.hbm.dim.orbit.BiomeGenOrbit;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Vec3;
import net.minecraft.world.biome.WorldChunkManagerHell;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.client.IRenderHandler;

public class WorldProviderKerbol extends WorldProviderCelestial {
	private static final IRenderHandler EMPTY_SKY = new IRenderHandler() {
		@Override
		public void render(float partialTicks, WorldClient world, Minecraft mc) {
			// Intentionally empty: render nothing in the sky.
		}
	};

	@Override
	public void registerWorldChunkManager() {
		this.worldChunkMgr = new WorldChunkManagerHell(BiomeGenOrbit.biome, 0.0F);
	}

	@Override
	public String getDimensionName() { return "Kerbol"; }

	@Override
	public IChunkProvider createChunkGenerator() {
		return new ChunkProviderKerbol(this.worldObj);
	}

	@Override
	public Block getStone() { return Blocks.obsidian; }

	@Override
	protected double getDayLength() {
		// Kerbol has no parent, so avoid orbital-period math from WorldProviderCelestial.
		return com.hbm.dim.CelestialBody.getBody(worldObj).getRotationalPeriod();
	}

	@Override
	public IRenderHandler getSkyRenderer() {
		return EMPTY_SKY;
	}

	@Override
	public Vec3 getSkyColor(Entity camera, float partialTicks) {
		// Dark red sky
		return Vec3.createVectorHelper(0.18, 0.02, 0.02);
	}

	@Override
	public Vec3 getFogColor(float x, float y) {
		// Darker red fog to match the sky
		return Vec3.createVectorHelper(0.10, 0.01, 0.01);
	}

	@Override
	public float[] calcSunriseSunsetColors(float solarAngle, float partialTicks) {
		return null;
	}

	@Override
	public ChunkCoordinates getSpawnPoint() {
		// Spawn one block above the single basalt block at origin
		return new ChunkCoordinates(0, 65, 0);
	}
}
