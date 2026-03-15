package com.hbm.world;

import com.hbm.dim.duna.biome.BiomeGenBaseDuna;
import com.hbm.dim.dmitriy.biome.BiomeGenDmitriy;
import com.hbm.dim.minmus.biome.BiomeGenBaseMinmus;

import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;

public class ModBiomes
{
    public static void init()
    {
        BiomeDictionary.registerBiomeType(BiomeGenBaseDuna.dunaPlains, Type.COLD, Type.DRY, Type.DEAD);
        BiomeDictionary.registerBiomeType(BiomeGenBaseDuna.dunaLowlands, Type.COLD, Type.DRY, Type.DEAD);
        BiomeDictionary.registerBiomeType(BiomeGenBaseDuna.dunaPolar, Type.COLD, Type.DRY, Type.DEAD, Type.SNOWY);
        BiomeDictionary.registerBiomeType(BiomeGenBaseDuna.dunaHills, Type.COLD, Type.DRY, Type.DEAD, Type.HILLS);
        BiomeDictionary.registerBiomeType(BiomeGenBaseDuna.dunaHills, Type.COLD, Type.DRY, Type.DEAD, Type.SNOWY, Type.MOUNTAIN);
        BiomeDictionary.registerBiomeType(BiomeGenBaseMinmus.minmusCanyon, Type.COLD, Type.DRY, Type.DEAD, Type.SNOWY, Type.MOUNTAIN);
        BiomeDictionary.registerBiomeType(BiomeGenBaseMinmus.minmusPlains, Type.COLD, Type.DRY, Type.DEAD, Type.SNOWY, Type.MOUNTAIN);
        BiomeDictionary.registerBiomeType(BiomeGenDmitriy.digammaWastelands, Type.DEAD, Type.DRY, Type.HOT);
        BiomeDictionary.registerBiomeType(BiomeGenDmitriy.digammaOcean, Type.OCEAN, Type.DEAD, Type.HOT);

    }
}

