package com.hbm.saveddata.satellites;

import java.util.Iterator;
import java.util.Map;

import com.hbm.dim.CelestialBody;
import com.hbm.dim.StarcoreSkyEffects;
import com.hbm.dim.trait.CBT_SkyState;
import com.hbm.main.MainRegistry;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.tileentity.machine.TileEntityDysonLauncher;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class SatelliteSadald extends Satellite {

	@Override
	public void onOrbit(World world, double x, double y, double z) {
		if(world.isRemote) return;

		CBT_SkyState skyState = CBT_SkyState.get(world);
		boolean ready = skyState != null
			&& skyState.isBlackhole()
			&& skyState.getBlackholeClustersSent() >= TileEntityDysonLauncher.BLACKHOLE_CLUSTER_LIMIT;

		// Sadald is consumed regardless, but only does anything once the cluster threshold is met.
		if(ready) {
			if(!skyState.isBlackholeSadaldLaunched()) {
				skyState.setBlackholeSadaldLaunched(true);
				CelestialBody.getStar(world).modifyTraits(skyState);
			}
			StarcoreSkyEffects.startBlackholeCollapse(world, skyState);
		} else {
			for(Object p : world.playerEntities) {
				if(p instanceof EntityPlayer) {
					((EntityPlayer) p).triggerAchievement(MainRegistry.achWastedPotential);
				}
			}
		}

		removeFromSatelliteMap(world, (int)x, (int)z);
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

	@Override
	protected float[] getColor() {
		return new float[] { 0.0F, 0.0F, 0.0F, 0.0F };
	}
}
