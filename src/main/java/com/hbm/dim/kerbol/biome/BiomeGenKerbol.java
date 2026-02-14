package com.hbm.dim.kerbol.biome;

import java.util.Random;

import com.hbm.config.SpaceConfig;
import com.hbm.dim.BiomeGenBaseCelestial;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

public class BiomeGenKerbol extends BiomeGenBaseCelestial {

	public static final BiomeGenBase digammaWastelands = new BiomeGenKerbol(SpaceConfig.kerbolBiome);
	public static final BiomeGenBase digammaOcean = new BiomeGenKerbolOcean(SpaceConfig.kerbolOceanBiome);

	public BiomeGenKerbol(int id) {
		super(id);
		this.setBiomeName(" S  U    RF A CE :   )");
		this.setDisableRain();
	}

	@Override
	public void genTerrainBlocks(World world, Random rand, Block[] blocks, byte[] meta, int x, int z, double noise) {
		// Terrain is handled by the Kerbol chunk provider.
	}

	@Override
	public void decorate(World world, Random rand, int x, int z) {
		// Decorations are handled by the Kerbol chunk provider.
	}
}
