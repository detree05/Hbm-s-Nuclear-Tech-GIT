package com.hbm.dim.kerbol.biome;

import java.util.Random;

import com.hbm.dim.BiomeGenBaseCelestial;

import net.minecraft.block.Block;
import net.minecraft.world.World;

public class BiomeGenKerbolOcean extends BiomeGenBaseCelestial {

	public BiomeGenKerbolOcean(int id) {
		super(id);
		this.setBiomeName("Digma Ocean");
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
