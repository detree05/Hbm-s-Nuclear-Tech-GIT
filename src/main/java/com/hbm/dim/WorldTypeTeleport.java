package com.hbm.dim;

import com.hbm.config.SpaceConfig;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.WorldType;

public class WorldTypeTeleport extends WorldType {

	public static WorldType martian;
	public static WorldType blackHoleSun;

	public WorldTypeTeleport(String name) {
		super(name);
	}

	public static void init() {
		martian = new WorldTypeTeleport("martian");
		blackHoleSun = new WorldTypeTeleport("black_hole_sun");
	}

	public boolean onPlayerJoin(EntityPlayer player) {
		if(this == martian) {
			CelestialTeleporter.teleport(player, SpaceConfig.dunaDimension, 0, 0, 0, true);
			return true;
		}

		return false;
	}

}
