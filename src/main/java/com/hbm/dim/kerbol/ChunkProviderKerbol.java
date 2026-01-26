package com.hbm.dim.kerbol;

import java.util.Random;

import com.hbm.blocks.ModBlocks;
import com.hbm.config.SpaceConfig;
import com.hbm.util.Compat;
import com.hbm.world.WorldUtil;

import cpw.mods.fml.common.Loader;
import net.minecraft.block.Block;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.gen.NoiseGeneratorPerlin;
import java.util.List;

public class ChunkProviderKerbol implements IChunkProvider {
	private final World worldObj;
	private final NoiseGeneratorPerlin terrainNoise;

	private static final int BASE_HEIGHT = 64;

	public ChunkProviderKerbol(World world) {
		this.worldObj = world;
		this.terrainNoise = new NoiseGeneratorPerlin(new Random(world.getSeed() ^ 0x5deece66dL), 4);
	}

	@Override public boolean chunkExists(int x, int z) { return true; }

	@Override
	public Chunk provideChunk(int x, int z) {
		Block[] blocks = new Block[65536];
		byte[] meta = new byte[65536];

		for(int lx = 0; lx < 16; lx++) {
			for(int lz = 0; lz < 16; lz++) {
				int wx = (x << 4) + lx;
				int wz = (z << 4) + lz;
				int height = getHeight(wx, wz);

				for(int y = 0; y <= height; y++) {
					int index = (lx << 12) | (lz << 8) | y;
					blocks[index] = ModBlocks.ash_digamma;
					meta[index] = 0;
				}
			}
		}

		Chunk chunk = new Chunk(worldObj, blocks, meta, x, z);

		if(Loader.isModLoaded(Compat.MOD_EIDS)) {
			short[] biomes = WorldUtil.getBiomeShortArray(chunk);
			for(int i = 0; i < biomes.length; i++) {
				biomes[i] = (short) SpaceConfig.kerbolBiome;
			}
		} else {
			byte[] biomes = chunk.getBiomeArray();
			for(int i = 0; i < biomes.length; i++) {
				biomes[i] = (byte) SpaceConfig.kerbolBiome;
			}
		}

		chunk.generateSkylightMap();
		return chunk;
	}

	@Override public Chunk loadChunk(int x, int z) { return provideChunk(x, z); }

	@Override
	public void populate(IChunkProvider provider, int x, int z) {
		Random rand = new Random(worldObj.getSeed() + (x * 341873128712L) + (z * 132897987541L));

		// Rare lingering digamma on the surface
		for(int i = 0; i < 3; i++) {
			int lx = rand.nextInt(16);
			int lz = rand.nextInt(16);
			int wx = (x << 4) + lx;
			int wz = (z << 4) + lz;
			if(wx == 0 && wz == 0) continue;

			if(rand.nextInt(100) < 3) {
				int height = getHeight(wx, wz);
				worldObj.setBlock(wx, height + 1, wz, ModBlocks.fire_digamma);
			}
		}

		// Single spawn block at the world origin
		if(x == 0 && z == 0) {
			worldObj.setBlock(0, BASE_HEIGHT, 0, ModBlocks.basalt_smooth);
		}
	}

	@Override public boolean saveChunks(boolean combined, IProgressUpdate progress) { return true; }
	@Override public boolean unloadQueuedChunks() { return false; }
	@Override public boolean canSave() { return true; }
	@Override public String makeString() { return "KerbolLevelSource"; }
	@Override @SuppressWarnings("rawtypes")
	public List getPossibleCreatures(EnumCreatureType type, int x, int y, int z) { return null; }
	@Override public ChunkPosition func_147416_a(World w, String s, int x, int y, int z) { return null; }
	@Override public int getLoadedChunkCount() { return 0; }
	@Override public void recreateStructures(int x, int z) {}
	@Override public void saveExtraData() {}

	private int getHeight(int worldX, int worldZ) {
		// Very gentle, smooth plains-like variation (no dips below base).
		double scale = 0.01D;
		double n = terrainNoise.func_151601_a(worldX * scale, worldZ * scale);
		double height = BASE_HEIGHT + Math.max(0D, n) * 2.0D; // 0..~2 blocks
		return MathHelper.floor_double(height);
	}
}
