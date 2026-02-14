package com.hbm.dim.kerbol;

import java.util.Random;

import com.hbm.config.SpaceConfig;
import com.hbm.dim.kerbol.biome.BiomeGenKerbol;
import com.hbm.main.StructureManager;
import com.hbm.world.gen.nbt.JigsawPiece;
import com.hbm.world.gen.nbt.NBTStructure;
import com.hbm.world.gen.nbt.SpawnCondition;

import cpw.mods.fml.common.IWorldGenerator;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;

public class WorldGeneratorKerbol implements IWorldGenerator {

	private static final int FOOTPRINT_SIZE = 5;
	private static final int HALF_FOOTPRINT = FOOTPRINT_SIZE / 2;
	private static final int SPAWN_GRID = 6;
	private static final int SPAWN_CHANCE = 2;
	private static final int BLOOD_SEA_LEVEL = 62;

	public WorldGeneratorKerbol() {
		NBTStructure.registerStructure(SpaceConfig.kerbolDimension, new SpawnCondition("kerbol_tree") {{
			structure = new JigsawPiece("kerbol_tree", StructureManager.tree, -1) {{
				keepExistingOnAir = true;
			}};
			checkCoordinates = coords -> {
				int chunkX = coords.coords.chunkXPos;
				int chunkZ = coords.coords.chunkZPos;

				if(Math.floorMod(chunkX, SPAWN_GRID) != 0 || Math.floorMod(chunkZ, SPAWN_GRID) != 0) {
					return false;
				}

				if(coords.rand.nextInt(SPAWN_CHANCE) != 0) {
					return false;
				}

				int centerX = (chunkX << 4) + 8;
				int centerZ = (chunkZ << 4) + 8;
				BiomeGenBase centerBiome = coords.world.getBiomeGenForCoords(centerX, centerZ);
				if(centerBiome != BiomeGenKerbol.digammaWastelands) {
					return false;
				}

				int minX = centerX - HALF_FOOTPRINT;
				int minZ = centerZ - HALF_FOOTPRINT;
				int maxX = minX + FOOTPRINT_SIZE - 1;
				int maxZ = minZ + FOOTPRINT_SIZE - 1;

				int requiredTopY = coords.world.getTopSolidOrLiquidBlock(minX, minZ);
				if(requiredTopY <= BLOOD_SEA_LEVEL) {
					return false;
				}

				for(int x = minX; x <= maxX; x++) {
					for(int z = minZ; z <= maxZ; z++) {
						if(coords.world.getBiomeGenForCoords(x, z) != BiomeGenKerbol.digammaWastelands) {
							return false;
						}

						int topY = coords.world.getTopSolidOrLiquidBlock(x, z);
						if(topY != requiredTopY) {
							return false;
						}

						Material topMaterial = coords.world.getBlock(x, topY - 1, z).getMaterial();
						if(topMaterial.isLiquid()) {
							return false;
						}
					}
				}

				return true;
			};
		}});
	}

	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
	}
}
