package com.hbm.world.gen.terrain;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockDeadPlant.EnumDeadPlantType;
import com.hbm.blocks.generic.BlockNTMFlower.EnumFlowerType;
import com.hbm.world.gen.MapGenBaseMeta;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public class MapGenBedrockSubsurfaceWater extends MapGenBaseMeta {

	/**
	 * Same logic as bedrock oil, but uses subsurface water deposit.
	 */

	private final int frequency;

	public Block block = ModBlocks.ore_bedrock_subsurface_water;
	public Block replace = Blocks.stone;
	public byte meta = 0;

	public int spotWidth = 5;
	public int spotCount = 50;

	public MapGenBedrockSubsurfaceWater(int frequency) {
		this.frequency = frequency;
		this.range = 4;
	}

	@Override
	protected void func_151538_a(World world, int offsetX, int offsetZ, int chunkX, int chunkZ, Block[] blocks) {
		if(rand.nextInt(frequency) == frequency - 1) {
			int xCoord = (chunkX - offsetX) * 16 + rand.nextInt(16);
			int zCoord = (chunkZ - offsetZ) * 16 + rand.nextInt(16);

			// Add the bedrock water spot
			for(int bx = 15; bx >= 0; bx--)
			for(int bz = 15; bz >= 0; bz--)
			for(int y = 0; y < 5; y++) {
				int index = (bx * 16 + bz) * 256 + y;

				if(blocks[index] == replace || blocks[index] == Blocks.bedrock) {
					// x, z are the coordinates relative to the target virtual chunk origin
					int x = xCoord + bx;
					int z = zCoord + bz;

					if(Math.abs(x) < 5 && Math.abs(z) < 5 && Math.abs(x) + Math.abs(y) + Math.abs(z) <= 6) {
						blocks[index] = block;
						metas[index] = meta;
					}
				}
			}

			// Add water spot surface (clay + water)
			for(int i = 0; i < spotCount; i++) {
				int rx = (int)(rand.nextGaussian() * spotWidth) - xCoord;
				int rz = (int)(rand.nextGaussian() * spotWidth) - zCoord;

				if(rx >= 0 && rx < 16 && rz >= 0 && rz < 16) {
					// find ground level
					for(int y = 127; y >= 0; y--) {
						int index = (rx * 16 + rz) * 256 + y;

						if(blocks[index] != null && blocks[index].isOpaqueCube()) {
							for(int oy = 1; oy > -3; oy--) {
								int subIndex = index + oy;

								if(blocks[subIndex] == Blocks.grass || blocks[subIndex] == Blocks.dirt || blocks[subIndex] == Blocks.sand || blocks[subIndex] == Blocks.stone) {
									blocks[subIndex] = rand.nextInt(3) == 0 ? Blocks.water : Blocks.clay;
									break;
								}
							}

							break;
						}
					}
				}
			}
		}
	}
}
