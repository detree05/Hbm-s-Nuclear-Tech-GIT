package com.hbm.dim.kerbol;

import java.util.Random;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockSkeletonHolder.TileEntitySkeletonHolder;
import com.hbm.config.SpaceConfig;
import com.hbm.util.Compat;
import com.hbm.world.WorldUtil;
import com.hbm.lib.RefStrings;
import com.hbm.world.gen.terrain.MapGenCrater;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.gen.NoiseGeneratorPerlin;
import net.minecraft.world.gen.feature.WorldGenLakes;
import java.util.List;
import net.minecraftforge.oredict.OreDictionary;

public class ChunkProviderKerbol implements IChunkProvider {
	private final World worldObj;
	private final NoiseGeneratorPerlin terrainNoise;
	private final MapGenCrater blackenedCraters;

	private static final int BASE_HEIGHT = 64;
	private static final int[] FIGURE_BLOCK_IDS = { 1095, 1096, 1098, 1099 };
	private static final int FIGURE_SPAWN_CHANCE = 30;
	private static final int FIGURE_SKY_CHANCE = 4;
	private static final int OCTAHEDRON_RADIUS_MIN = 4;
	private static final int OCTAHEDRON_RADIUS_MAX = 9;
	private static final int TORUS_MAJOR_MIN = 4;
	private static final int TORUS_MAJOR_MAX = 8;
	private static final int TORUS_MINOR_MIN = 2;
	private static final int TORUS_MINOR_MAX = 4;
	private static final int[] MENGER_SIZES = { 6, 9, 12 };
	private static final int TESSERACT_SIZE_MIN = 6;
	private static final int TESSERACT_SIZE_MAX = 10;
	private static final int[] SIERPINSKI_SIZES = { 8, 12, 16 };
	private static final int CHARRED_LOG_BLOCK_ID = 1197;
	private static final int CHARRED_TREE_CHANCE = 6;
	private static final int CHARRED_TREE_ATTEMPTS = 2;
	private static final int CHARRED_TREE_HEIGHT_MIN = 4;
	private static final int CHARRED_TREE_HEIGHT_MAX = 9;
	private static final int BLACKENED_RBMK_DEBRIS_ID = 1495;
	private static final int LINGERING_DIGAMMA_ID = 1744;
	private static final int CRATER_FREQUENCY = 1000;
	private static final int CRATER_SIZE_MIN = 6;
	private static final int CRATER_SIZE_MAX = 12;
	private static final int SELLAFITE_SPIKE_CHANCE = 180;
	private static final int SELLAFITE_SPIKE_HEIGHT_MIN = 8;
	private static final int SELLAFITE_SPIKE_HEIGHT_MAX = 20;
	private static final int SELLAFITE_SPIKE_RADIUS_MIN = 1;
	private static final int SELLAFITE_SPIKE_RADIUS_MAX = 3;
	private static final int SKELETON_HOLDER_CHANCE = 1000;
	private static final int[] SKELETON_HOLDER_METAS = { 2, 3, 4, 5 };
	private static final int BLOOD_POOL_CHANCE = 120;
	private static final int BLOOD_POOL_ATTEMPTS = 2;
	private static List<ItemStack> cachedHbmIngots = null;

	public ChunkProviderKerbol(World world) {
		this.worldObj = world;
		this.terrainNoise = new NoiseGeneratorPerlin(new Random(world.getSeed() ^ 0x5deece66dL), 4);
		this.blackenedCraters = new MapGenCrater(CRATER_FREQUENCY);
		this.blackenedCraters.setSize(CRATER_SIZE_MIN, CRATER_SIZE_MAX);
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

		Block debris = Block.getBlockById(BLACKENED_RBMK_DEBRIS_ID);
		Block digamma = Block.getBlockById(LINGERING_DIGAMMA_ID);
		if(debris != null && digamma != null) {
			blackenedCraters.rock = debris;
			blackenedCraters.regolith = debris;
			blackenedCraters.func_151539_a(this, worldObj, x, z, blocks);
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

		// No forced spawn block at world origin

		generateGeometricFigures(rand, x, z);
		generateSellafiteSpikes(rand, x, z);
		generateSkeletonHolder(rand, x, z);
		generateCraterLingeringDigamma(rand, x, z);
		generateBloodPools(rand, x, z);
		generateCharredTrees(rand, x, z);
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

			int figureType = rand.nextInt(5);
			switch(figureType) {
				case 0:
					placeOctahedron(wx, baseY, wz, randRange(rand, OCTAHEDRON_RADIUS_MIN, OCTAHEDRON_RADIUS_MAX), orientation);
					break;
				case 1:
					placeMengerSponge(wx, baseY, wz, pickMengerSize(rand), orientation);
					break;
				case 2:
					placeTorus(wx, baseY + 2, wz, randRange(rand, TORUS_MAJOR_MIN, TORUS_MAJOR_MAX), randRange(rand, TORUS_MINOR_MIN, TORUS_MINOR_MAX), orientation);
					break;
				case 3:
					placeTesseract(wx, baseY + 4, wz, randRange(rand, TESSERACT_SIZE_MIN, TESSERACT_SIZE_MAX), orientation);
					break;
				default:
					placeSierpinskiTetrahedron(wx, baseY, wz, pickSierpinskiSize(rand), orientation);
					break;
			}
		}
	}

	private void generateSellafiteSpikes(Random rand, int chunkX, int chunkZ) {
		if(rand.nextInt(SELLAFITE_SPIKE_CHANCE) != 0) {
			return;
		}

		int lx = rand.nextInt(16);
		int lz = rand.nextInt(16);
		int wx = (chunkX << 4) + lx;
		int wz = (chunkZ << 4) + lz;
		if(wx == 0 && wz == 0) return;

		int baseY = getHeight(wx, wz) + 1;
		int height = randRange(rand, SELLAFITE_SPIKE_HEIGHT_MIN, SELLAFITE_SPIKE_HEIGHT_MAX);
		int baseRadius = randRange(rand, SELLAFITE_SPIKE_RADIUS_MIN, SELLAFITE_SPIKE_RADIUS_MAX);

		if(isNearGeometricFigure(wx, baseY, wz, baseRadius + 2, height + 2)) {
			return;
		}

		for(int dy = 0; dy < height; dy++) {
			int radius = baseRadius - (int)Math.floor((double)baseRadius * dy / height);
			int meta = MathHelper.clamp_int(5 - (int)Math.floor(6.0 * dy / Math.max(1, height - 1)), 0, 5);
			int y = baseY + dy;

			for(int dx = -radius; dx <= radius; dx++) {
				for(int dz = -radius; dz <= radius; dz++) {
					if(dx * dx + dz * dz > radius * radius) {
						continue;
					}
					int x = wx + dx;
					int z = wz + dz;
					if(!worldObj.isAirBlock(x, y, z)) {
						continue;
					}
					worldObj.setBlock(x, y, z, ModBlocks.sellafield, meta, 3);
				}
			}
		}
	}

	private void generateCraterLingeringDigamma(Random rand, int chunkX, int chunkZ) {
		Block debris = Block.getBlockById(BLACKENED_RBMK_DEBRIS_ID);
		Block digamma = Block.getBlockById(LINGERING_DIGAMMA_ID);
		if(debris == null || digamma == null) {
			return;
		}

		int attempts = 10;
		for(int i = 0; i < attempts; i++) {
			int lx = rand.nextInt(16);
			int lz = rand.nextInt(16);
			int wx = (chunkX << 4) + lx;
			int wz = (chunkZ << 4) + lz;
			if(wx == 0 && wz == 0) continue;

			int y = getHeight(wx, wz) + 1;
			Block below = worldObj.getBlock(wx, y - 1, wz);
			if(below == debris && worldObj.isAirBlock(wx, y, wz)) {
				worldObj.setBlock(wx, y, wz, digamma);
			}
		}
	}

	private void generateBloodPools(Random rand, int chunkX, int chunkZ) {
		if(ModBlocks.blood_block == null) {
			return;
		}
		if(rand.nextInt(BLOOD_POOL_CHANCE) != 0) {
			return;
		}
		for(int i = 0; i < BLOOD_POOL_ATTEMPTS; i++) {
			int lx = rand.nextInt(16);
			int lz = rand.nextInt(16);
			int wx = (chunkX << 4) + lx;
			int wz = (chunkZ << 4) + lz;
			if(wx == 0 && wz == 0) continue;

			int surface = getHeight(wx, wz);
			int y = MathHelper.clamp_int(surface - 1 - rand.nextInt(3), 1, 250);
			if(isNearGeometricFigure(wx, surface + 1, wz, 6, 6)) {
				continue;
			}
			new WorldGenLakes(ModBlocks.blood_block).generate(worldObj, rand, wx, y, wz);
		}
	}

	private void generateSkeletonHolder(Random rand, int chunkX, int chunkZ) {
		if(rand.nextInt(SKELETON_HOLDER_CHANCE) != 0) {
			return;
		}

		int lx = rand.nextInt(16);
		int lz = rand.nextInt(16);
		int wx = (chunkX << 4) + lx;
		int wz = (chunkZ << 4) + lz;
		if(wx == 0 && wz == 0) return;

		int baseY = getHeight(wx, wz) + 1;
		if(!worldObj.isAirBlock(wx, baseY, wz)) {
			return;
		}

		ItemStack ingot = getRandomHbmIngot(rand);
		if(ingot == null) {
			return;
		}

		int meta = SKELETON_HOLDER_METAS[rand.nextInt(SKELETON_HOLDER_METAS.length)];
		worldObj.setBlock(wx, baseY, wz, ModBlocks.skeleton_holder, meta, 3);

		TileEntitySkeletonHolder te = (TileEntitySkeletonHolder) worldObj.getTileEntity(wx, baseY, wz);
		if(te != null) {
			te.item = ingot.copy();
			te.markDirty();
			worldObj.markBlockForUpdate(wx, baseY, wz);
		}
	}

	private static ItemStack getRandomHbmIngot(Random rand) {
		if(cachedHbmIngots == null) {
			cachedHbmIngots = new java.util.ArrayList<>();
			for(String name : OreDictionary.getOreNames()) {
				if(!name.startsWith("ingot")) {
					continue;
				}
				for(Object obj : OreDictionary.getOres(name)) {
					if(!(obj instanceof ItemStack)) {
						continue;
					}
					ItemStack stack = (ItemStack) obj;
					if(stack == null || stack.getItem() == null) {
						continue;
					}
					Item item = stack.getItem();
					UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(item);
					if(id != null && RefStrings.MODID.equals(id.modId)) {
						ItemStack copy = stack.copy();
						copy.stackSize = 1;
						cachedHbmIngots.add(copy);
					}
				}
			}
		}

		if(cachedHbmIngots.isEmpty()) {
			return null;
		}
		return cachedHbmIngots.get(rand.nextInt(cachedHbmIngots.size()));
	}

	private void generateCharredTrees(Random rand, int chunkX, int chunkZ) {
		Block log = Block.getBlockById(CHARRED_LOG_BLOCK_ID);
		if(log == null) {
			return;
		}
		if(rand.nextInt(CHARRED_TREE_CHANCE) != 0) {
			return;
		}

		int attempts = 1 + rand.nextInt(CHARRED_TREE_ATTEMPTS);
		for(int i = 0; i < attempts; i++) {
			int lx = rand.nextInt(16);
			int lz = rand.nextInt(16);
			int wx = (chunkX << 4) + lx;
			int wz = (chunkZ << 4) + lz;
			if(wx == 0 && wz == 0) continue;

			int baseY = getHeight(wx, wz) + 1;
			int height = randRange(rand, CHARRED_TREE_HEIGHT_MIN, CHARRED_TREE_HEIGHT_MAX);

			if(!worldObj.isAirBlock(wx, baseY, wz)) {
				continue;
			}
			if(isNearGeometricFigure(wx, baseY, wz, 2, height + 2)) {
				continue;
			}

			for(int y = 0; y < height; y++) {
				if(!worldObj.isAirBlock(wx, baseY + y, wz)) {
					break;
				}
				worldObj.setBlock(wx, baseY + y, wz, log);
			}

			int branches = 1 + rand.nextInt(2);
			for(int b = 0; b < branches; b++) {
				int branchY = baseY + height - 2 - rand.nextInt(3);
				int dir = rand.nextInt(4);
				int dx = (dir == 0 ? 1 : dir == 1 ? -1 : 0);
				int dz = (dir == 2 ? 1 : dir == 3 ? -1 : 0);
				int length = 1 + rand.nextInt(2);
				int bx = wx;
				int bz = wz;
				for(int step = 0; step < length; step++) {
					bx += dx;
					bz += dz;
					if(!worldObj.isAirBlock(bx, branchY, bz)) {
						break;
					}
					worldObj.setBlock(bx, branchY, bz, log);
				}
				if(worldObj.isAirBlock(bx, branchY + 1, bz)) {
					worldObj.setBlock(bx, branchY + 1, bz, log);
				}
			}
		}
	}

	private boolean isNearGeometricFigure(int centerX, int centerY, int centerZ, int radius, int height) {
		for(int dx = -radius; dx <= radius; dx++) {
			for(int dz = -radius; dz <= radius; dz++) {
				for(int dy = 0; dy <= height; dy++) {
					Block block = worldObj.getBlock(centerX + dx, centerY + dy, centerZ + dz);
					if(block == null) {
						continue;
					}
					int id = Block.getIdFromBlock(block);
					for(int figId : FIGURE_BLOCK_IDS) {
						if(id == figId) {
							return true;
						}
					}
				}
			}
		}
		return false;
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

	private void placeTesseract(int centerX, int centerY, int centerZ, int outerHalfSize, Orientation orientation) {
		int innerHalfSize = Math.max(2, MathHelper.floor_double(outerHalfSize * 0.6D));
		int[][] outer = cubeVertices(outerHalfSize);
		int[][] inner = cubeVertices(innerHalfSize);

		for(int i = 0; i < outer.length; i++) {
			int[] o = transform(outer[i][0], outer[i][1], outer[i][2], orientation);
			int[] in = transform(inner[i][0], inner[i][1], inner[i][2], orientation);
			drawEdge(centerX, centerY, centerZ, o, in);
		}

		drawCubeEdges(centerX, centerY, centerZ, outer, orientation);
		drawCubeEdges(centerX, centerY, centerZ, inner, orientation);
	}

	private void placeSierpinskiTetrahedron(int centerX, int centerY, int centerZ, int size, Orientation orientation) {
		int half = size / 2;
		int startX = centerX - half;
		int startZ = centerZ - half;
		int depth = 0;
		int temp = size;
		while(temp >= 2 && depth < 4) {
			if(temp % 2 != 0) {
				break;
			}
			depth++;
			temp /= 2;
		}
		buildSierpinski(startX, centerY, startZ, size, Math.max(1, depth), orientation);
	}

	private void buildSierpinski(int startX, int startY, int startZ, int size, int depth, Orientation orientation) {
		if(depth <= 1 || size <= 2) {
			fillTetrahedron(startX, startY, startZ, size, orientation);
			return;
		}

		int half = size / 2;
		buildSierpinski(startX, startY, startZ, half, depth - 1, orientation);
		buildSierpinski(startX + half, startY, startZ, half, depth - 1, orientation);
		buildSierpinski(startX, startY, startZ + half, half, depth - 1, orientation);
		buildSierpinski(startX, startY + half, startZ, half, depth - 1, orientation);
	}

	private void fillTetrahedron(int startX, int startY, int startZ, int size, Orientation orientation) {
		for(int x = 0; x < size; x++) {
			for(int y = 0; y < size; y++) {
				for(int z = 0; z < size; z++) {
					if(x + y + z <= size - 1) {
						int localX = x;
						int localY = y;
						int localZ = z;
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

	private Orientation pickOrientation(Random rand) {
		int perm = rand.nextInt(6);
		int sx = rand.nextBoolean() ? 1 : -1;
		int sy = rand.nextBoolean() ? 1 : -1;
		int sz = rand.nextBoolean() ? 1 : -1;
		return new Orientation(perm, sx, sy, sz);
	}

	private int pickMengerSize(Random rand) {
		return MENGER_SIZES[rand.nextInt(MENGER_SIZES.length)];
	}

	private int pickSierpinskiSize(Random rand) {
		return SIERPINSKI_SIZES[rand.nextInt(SIERPINSKI_SIZES.length)];
	}

	private int randRange(Random rand, int minInclusive, int maxInclusive) {
		if(maxInclusive <= minInclusive) {
			return minInclusive;
		}
		return minInclusive + rand.nextInt(maxInclusive - minInclusive + 1);
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

	private int[][] cubeVertices(int halfSize) {
		return new int[][] {
			{ -halfSize, -halfSize, -halfSize },
			{ halfSize, -halfSize, -halfSize },
			{ halfSize, halfSize, -halfSize },
			{ -halfSize, halfSize, -halfSize },
			{ -halfSize, -halfSize, halfSize },
			{ halfSize, -halfSize, halfSize },
			{ halfSize, halfSize, halfSize },
			{ -halfSize, halfSize, halfSize }
		};
	}

	private void drawCubeEdges(int centerX, int centerY, int centerZ, int[][] vertices, Orientation orientation) {
		int[][] edges = new int[][] {
			{ 0, 1 }, { 1, 2 }, { 2, 3 }, { 3, 0 },
			{ 4, 5 }, { 5, 6 }, { 6, 7 }, { 7, 4 },
			{ 0, 4 }, { 1, 5 }, { 2, 6 }, { 3, 7 }
		};
		for(int i = 0; i < edges.length; i++) {
			int[] a = vertices[edges[i][0]];
			int[] b = vertices[edges[i][1]];
			int[] ta = transform(a[0], a[1], a[2], orientation);
			int[] tb = transform(b[0], b[1], b[2], orientation);
			drawEdge(centerX, centerY, centerZ, ta, tb);
		}
	}

	private void drawEdge(int centerX, int centerY, int centerZ, int[] a, int[] b) {
		int x0 = centerX + a[0];
		int y0 = centerY + a[1];
		int z0 = centerZ + a[2];
		int x1 = centerX + b[0];
		int y1 = centerY + b[1];
		int z1 = centerZ + b[2];
		drawLine(x0, y0, z0, x1, y1, z1);
	}

	private void drawLine(int x0, int y0, int z0, int x1, int y1, int z1) {
		int dx = x1 - x0;
		int dy = y1 - y0;
		int dz = z1 - z0;
		int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
		if(steps == 0) {
			setFigureBlock(x0, y0, z0, pickFigureBlock(0, 0, 0));
			return;
		}
		for(int i = 0; i <= steps; i++) {
			double t = i / (double) steps;
			int x = (int) Math.round(x0 + dx * t);
			int y = (int) Math.round(y0 + dy * t);
			int z = (int) Math.round(z0 + dz * t);
			setFigureBlock(x, y, z, pickFigureBlock(x, y, z));
		}
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
