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
	private static final int[] FIGURE_BLOCK_IDS = { 1095, 1096, 1098, 1099 };
	private static final int FIGURE_SPAWN_CHANCE = 30;
	private static final int FIGURE_SKY_CHANCE = 4;

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

		generateGeometricFigures(rand, x, z);
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

	private void generateGeometricFigures(Random rand, int chunkX, int chunkZ) {
		if(rand.nextInt(FIGURE_SPAWN_CHANCE) != 0) {
			return;
		}

		int attempts = 1;
		for(int i = 0; i < attempts; i++) {
			int lx = rand.nextInt(16);
			int lz = rand.nextInt(16);
			int wx = (chunkX << 4) + lx;
			int wz = (chunkZ << 4) + lz;
			boolean inSky = rand.nextInt(FIGURE_SKY_CHANCE) == 0;
			int baseY = inSky ? (BASE_HEIGHT + 40 + rand.nextInt(80)) : (getHeight(wx, wz) + 1);
			Orientation orientation = pickOrientation(rand);

			int figureType = rand.nextInt(3);
			switch(figureType) {
				case 0:
					placeOctahedron(wx, baseY, wz, 5 + rand.nextInt(3), orientation);
					break;
				case 1:
					placeMengerSponge(wx, baseY, wz, 9, orientation);
					break;
				default:
					placeTorus(wx, baseY + 2, wz, 5 + rand.nextInt(2), 2 + rand.nextInt(2), orientation);
					break;
			}
		}
	}

	private void placeOctahedron(int centerX, int centerY, int centerZ, int radius, Orientation orientation) {
		for(int dx = -radius; dx <= radius; dx++) {
			for(int dy = -radius; dy <= radius; dy++) {
				for(int dz = -radius; dz <= radius; dz++) {
					if(Math.abs(dx) + Math.abs(dy) + Math.abs(dz) <= radius) {
						int[] t = transform(dx, dy, dz, orientation);
						int wx = centerX + t[0];
						int wy = centerY + t[1];
						int wz = centerZ + t[2];
						setFigureBlock(wx, wy, wz, pickFigureBlock(dx, dy, dz));
					}
				}
			}
		}
	}

	private void placeMengerSponge(int startX, int startY, int startZ, int size, Orientation orientation) {
		for(int x = 0; x < size; x++) {
			for(int y = 0; y < size; y++) {
				for(int z = 0; z < size; z++) {
					if(isMengerSolid(x, y, z)) {
						int localX = x - size / 2;
						int localY = y;
						int localZ = z - size / 2;
						int[] t = transform(localX, localY, localZ, orientation);
						int wx = startX + t[0];
						int wy = startY + t[1];
						int wz = startZ + t[2];
						setFigureBlock(wx, wy, wz, pickFigureBlock(localX, localY, localZ));
					}
				}
			}
		}
	}

	private boolean isMengerSolid(int x, int y, int z) {
		while(x > 0 || y > 0 || z > 0) {
			if((x % 3 == 1 && y % 3 == 1) || (x % 3 == 1 && z % 3 == 1) || (y % 3 == 1 && z % 3 == 1)) {
				return false;
			}
			x /= 3;
			y /= 3;
			z /= 3;
		}
		return true;
	}

	private void placeTorus(int centerX, int centerY, int centerZ, int majorRadius, int minorRadius, Orientation orientation) {
		int bound = majorRadius + minorRadius + 1;
		double minorRadiusSq = minorRadius * minorRadius;

		for(int dx = -bound; dx <= bound; dx++) {
			for(int dy = -minorRadius; dy <= minorRadius; dy++) {
				for(int dz = -bound; dz <= bound; dz++) {
					double radial = dx * dx + dz * dz;
					double tube = (Math.sqrt(radial) - majorRadius);
					double eq = tube * tube + dy * dy;
					if(eq <= minorRadiusSq) {
						int[] t = transform(dx, dy, dz, orientation);
						int wx = centerX + t[0];
						int wy = centerY + t[1];
						int wz = centerZ + t[2];
						setFigureBlock(wx, wy, wz, pickFigureBlock(dx, dy, dz));
					}
				}
			}
		}
	}

	private Orientation pickOrientation(Random rand) {
		int perm = rand.nextInt(6);
		int sx = rand.nextBoolean() ? 1 : -1;
		int sy = rand.nextBoolean() ? 1 : -1;
		int sz = rand.nextBoolean() ? 1 : -1;
		return new Orientation(perm, sx, sy, sz);
	}

	private int[] transform(int x, int y, int z, Orientation orientation) {
		int a = x;
		int b = y;
		int c = z;
		int tx;
		int ty;
		int tz;
		switch(orientation.permutation) {
			default:
			case 0: tx = a; ty = b; tz = c; break; // xyz
			case 1: tx = a; ty = c; tz = b; break; // xzy
			case 2: tx = b; ty = a; tz = c; break; // yxz
			case 3: tx = b; ty = c; tz = a; break; // yzx
			case 4: tx = c; ty = a; tz = b; break; // zxy
			case 5: tx = c; ty = b; tz = a; break; // zyx
		}
		tx *= orientation.sx;
		ty *= orientation.sy;
		tz *= orientation.sz;
		return new int[] { tx, ty, tz };
	}

	private static class Orientation {
		final int permutation;
		final int sx;
		final int sy;
		final int sz;

		Orientation(int permutation, int sx, int sy, int sz) {
			this.permutation = permutation;
			this.sx = sx;
			this.sy = sy;
			this.sz = sz;
		}
	}

	private Block pickFigureBlock(int x, int y, int z) {
		int idx = Math.abs(x + y * 3 + z * 5) & 3;
		Block block = Block.getBlockById(FIGURE_BLOCK_IDS[idx]);
		return block == null ? ModBlocks.ash_digamma : block;
	}

	private void setFigureBlock(int x, int y, int z, Block block) {
		if(y < 1 || y > 254) {
			return;
		}
		worldObj.setBlock(x, y, z, block);
	}
}
