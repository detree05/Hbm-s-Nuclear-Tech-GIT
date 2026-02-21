package com.hbm.dim.dmitriy.biome;

import java.util.Random;

import com.hbm.dim.BiomeGenBaseCelestial;

import net.minecraft.block.Block;
import net.minecraft.world.World;

public class BiomeGenDmitriyOcean extends BiomeGenBaseCelestial {

	public BiomeGenDmitriyOcean(int id) {
		super(id);
		this.setBiomeName(" o   c ea n");
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

