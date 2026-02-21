package com.hbm.dim.dmitriy.biome;

import java.util.Random;

import com.hbm.config.SpaceConfig;
import com.hbm.dim.BiomeGenBaseCelestial;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

public class BiomeGenDmitriy extends BiomeGenBaseCelestial {

	public static final BiomeGenBase digammaWastelands = new BiomeGenDmitriy(SpaceConfig.dmitriyBiome);
	public static final BiomeGenBase digammaOcean = new BiomeGenDmitriyOcean(SpaceConfig.dmitriyOceanBiome);

	public BiomeGenDmitriy(int id) {
		super(id);
		this.setBiomeName(" S  U    RF A CE :   )");
		this.setDisableRain();
	}

	@Override
	public void genTerrainBlocks(World world, Random rand, Block[] blocks, byte[] meta, int x, int z, double noise) {
		// Terrain is handled by the Dmitriy chunk provider.
	}

	@Override
	public void decorate(World world, Random rand, int x, int z) {
		// Decorations are handled by the Dmitriy chunk provider.
	}
}


