package com.hbm.saveddata.satellites;

import com.hbm.commands.CommandNovae;
import com.hbm.entity.missile.EntitySoyuzCapsule;
import com.hbm.items.ModItems;
import com.hbm.saveddata.SatelliteSavedData;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

import java.util.Iterator;
import java.util.Map;

public class SatelliteDerald extends Satellite {

	private static final int MIN_DROP_RANGE = 15;
	private static final int MAX_DROP_RANGE = 20;

	@Override
	public void onOrbit(World world, double x, double y, double z) {
		if(world.isRemote) return;

		CommandNovae.triggerNovaeAcrossDimensions();
		dropCapsule(world, (int)Math.floor(x), (int)Math.floor(z));
		removeFromSatelliteMap(world, (int) x, (int) z);
	}

	private void removeFromSatelliteMap(World world, int x, int z) {
		SatelliteSavedData data = SatelliteSavedData.getData(world, x, z);
		Iterator<Map.Entry<Integer, Satellite>> iterator = data.sats.entrySet().iterator();

		while(iterator.hasNext()) {
			Map.Entry<Integer, Satellite> entry = iterator.next();
			if(entry.getValue() == this) {
				iterator.remove();
				data.markDirty();
				return;
			}
		}
	}

	private static void dropCapsule(World world, int sourceX, int sourceZ) {
		int targetX = sourceX;
		int targetZ = sourceZ;
		boolean found = false;

		for(int i = 0; i < 64; i++) {
			int offsetX = world.rand.nextInt(MAX_DROP_RANGE * 2 + 1) - MAX_DROP_RANGE;
			int offsetZ = world.rand.nextInt(MAX_DROP_RANGE * 2 + 1) - MAX_DROP_RANGE;
			double distance = Math.sqrt(offsetX * offsetX + offsetZ * offsetZ);

			if(distance < MIN_DROP_RANGE || distance > MAX_DROP_RANGE) continue;

			targetX = sourceX + offsetX;
			targetZ = sourceZ + offsetZ;
			found = true;
			break;
		}

		if(!found) {
			double angle = world.rand.nextDouble() * Math.PI * 2.0D;
			int radius = MIN_DROP_RANGE + world.rand.nextInt(MAX_DROP_RANGE - MIN_DROP_RANGE + 1);
			targetX = sourceX + (int)Math.round(Math.cos(angle) * radius);
			targetZ = sourceZ + (int)Math.round(Math.sin(angle) * radius);
		}

		EntitySoyuzCapsule capsule = new EntitySoyuzCapsule(world);
		capsule.payload = createDeraldLoot(world);
		capsule.soyuz = 0;
		capsule.includeReturnRocket = false;
		capsule.setPosition(targetX + 0.5, 600, targetZ + 0.5);

		IChunkProvider provider = world.getChunkProvider();
		provider.loadChunk(targetX >> 4, targetZ >> 4);

		world.spawnEntityInWorld(capsule);
	}

	private static ItemStack[] createDeraldLoot(World world) {
		ItemStack[] payload = new ItemStack[18];

		payload[0] = new ItemStack(ModItems.ingot_osmiridium, 16 + world.rand.nextInt(33));
		payload[1] = new ItemStack(ModItems.crystal_osmiridium, 16 + world.rand.nextInt(33));
		payload[2] = new ItemStack(ModItems.nugget_osmiridium, 16 + world.rand.nextInt(33));

		return payload;
	}

	@Override
	protected float[] getColor() {
		return new float[] { 0.0F, 0.0F, 0.0F, 0.0F };
	}
}
