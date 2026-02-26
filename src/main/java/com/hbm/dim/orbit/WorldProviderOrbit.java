package com.hbm.dim.orbit;

import java.util.List;

import com.hbm.dim.CelestialBody;
import com.hbm.dim.SolarSystem;
import com.hbm.dim.SolarSystem.AstroMetric;
import com.hbm.dim.StarcoreSkyEffects;
import com.hbm.dim.WorldProviderCelestial;
import com.hbm.dim.orbit.OrbitalStation.StationState;
import com.hbm.dim.trait.CBT_Atmosphere;
import com.hbm.dim.trait.CBT_Destroyed;
import com.hbm.dim.trait.CBT_SkyState;
import com.hbm.handler.atmosphere.ChunkAtmosphereManager;
import com.hbm.config.SpaceConfig;
import com.hbm.util.AstronomyUtil;
import com.hbm.util.BobMathUtil;
import com.hbm.util.Compat;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.WorldChunkManagerHell;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.client.IRenderHandler;

public class WorldProviderOrbit extends WorldProvider {

	// Orbit at an altitude that provides an hour-long realtime orbit (game time is fast so we go slow)
	// We want a consistent orbital period to prevent orbiting too slow or fast (both for player comfort and feel)
	private static final float ORBITAL_PERIOD = 7200;
	private static final float SUN_MIN_SKY_DIM = 0.45F;
	private static final float NOTHING_DIM_MULTIPLIER = 2.5F;
	private static final float BLACKHOLE_SKY_DIM = 1.0F;
	private static final float NOTHING_SKY_DIM = BLACKHOLE_SKY_DIM / NOTHING_DIM_MULTIPLIER;

	public List<AstroMetric> metrics;

	private double eclipseAmount;
	private float celestialAngle;

	protected float getOrbitalAltitude(CelestialBody body) {
		return getAltitudeForPeriod(body.massKg, ORBITAL_PERIOD);
	}

	// r = ∛[(G x Me x T2) / (4π2)]
	private float getAltitudeForPeriod(float massKg, float period) {
		return (float)Math.cbrt((AstronomyUtil.GRAVITATIONAL_CONSTANT * massKg * (period * period)) / (4 * Math.PI * Math.PI));
	}

	public float getSunPower() {
		double progress = OrbitalStation.clientStation.getTransferProgress(0);
		float sunPower = OrbitalStation.clientStation.orbiting.getSunPower();
		if(progress > 0) {
			CelestialBody visualTarget = OrbitalStation.clientStation.getAnimationTarget();
			float targetSunPower = visualTarget != null && visualTarget.parent != null ? visualTarget.getSunPower() : sunPower;
			return (float)BobMathUtil.lerp(progress, sunPower, targetSunPower);
		}
		return sunPower;
	}

	@Override
	public void registerWorldChunkManager() {
		this.worldChunkMgr = new WorldChunkManagerHell(BiomeGenOrbit.biome, 0.0F);
	}

	@Override
	public String getDimensionName() {
		return "Orbit";
	}

	@Override
	public IChunkProvider createChunkGenerator() {
		return new ChunkProviderOrbit(this.worldObj);
	}

	@Override
	public void updateWeather() {
		isHellWorld = !worldObj.isRemote && !Loader.isModLoaded(Compat.MOD_COFH);

		worldObj.prevRainingStrength = 0.0F;
		worldObj.rainingStrength = 0.0F;
		worldObj.prevThunderingStrength = 0.0F;
		worldObj.thunderingStrength = 0.0F;
	}

	// This is called once, at the beginning of every frame
	// so we use this to memoise expensive calcs
	@SideOnly(Side.CLIENT)
	protected void updateSky(float partialTicks) {
		CelestialBody body = CelestialBody.getBody(worldObj);
		OrbitalStation station = OrbitalStation.clientStation;

		// First fetch the suns true size
		double sunSize = SolarSystem.calculateSunSize(body);

		double progress = station.getTransferProgress(partialTicks);
		CelestialBody visualTarget = station.getAnimationTarget();

		// Get our orrery of bodies, this is cached for reuse in sky rendering
		if(station.state == StationState.ORBIT) {
			double altitude = getOrbitalAltitude(station.orbiting);
			metrics = SolarSystem.calculateMetricsFromSatellite(worldObj, partialTicks, station.orbiting, altitude);
		} else {
			double fromAlt = getOrbitalAltitude(station.orbiting);
			double toAlt = getOrbitalAltitude(visualTarget);
			metrics = SolarSystem.calculateMetricsBetweenSatelliteOrbits(worldObj, partialTicks, station.orbiting, visualTarget, fromAlt, toAlt, progress);
		}

		// Get our sun angle
		float angle = (float)SolarSystem.calculateSingleAngle(worldObj, partialTicks, metrics, station.orbiting, getOrbitalAltitude(station.orbiting));
		if(progress > 0) {
			angle = (float)BobMathUtil.clerp(progress, angle, (float)SolarSystem.calculateSingleAngle(worldObj, partialTicks, metrics, visualTarget, getOrbitalAltitude(visualTarget)));
		}

		celestialAngle = 0.5F - (angle / 360.0F);

		// Get our eclipse amount
		eclipseAmount = WorldProviderCelestial.getEclipseFactor(metrics, sunSize, SolarSystem.MAX_APPARENT_SIZE_ORBIT);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Vec3 getFogColor(float x, float y) {
		return Vec3.createVectorHelper(0, 0, 0);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Vec3 getSkyColor(Entity camera, float partialTicks) {
		// getSkyColor is called first on every frame, so if you want to memoise anything, do it here
		updateSky(partialTicks);

		return Vec3.createVectorHelper(0, 0, 0);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public float[] calcSunriseSunsetColors(float solarAngle, float partialTicks) {
		return null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public float getStarBrightness(float par1) {
		CBT_SkyState.SkyState sky = CBT_SkyState.get(worldObj).getState();
		if(sky == CBT_SkyState.SkyState.BLACKHOLE
			|| sky == CBT_SkyState.SkyState.NOTHING
			|| sky == CBT_SkyState.SkyState.STARCORE) {
			return 1.0F;
		}

		// Stars look cool in orbit, but obvs at Moho we don't want the big fuckoff sun to not extinguish
		// Stars become visible during the day part of orbit just before Earth
		// And are fully visible during the day beyond the orbit of Duna
		float distanceStart = 9_000_000;
		float distanceEnd = 30_000_000;

		double progress = OrbitalStation.clientStation.getTransferProgress(par1);
		float semiMajorAxisKm = OrbitalStation.clientStation.orbiting.getPlanet().semiMajorAxisKm;
		if(progress > 0) {
			CelestialBody visualTarget = OrbitalStation.clientStation.getAnimationTarget();
			semiMajorAxisKm = (float)BobMathUtil.lerp(progress, semiMajorAxisKm, visualTarget.getPlanet().semiMajorAxisKm);
		}

		float distanceFactor = MathHelper.clamp_float((semiMajorAxisKm - distanceStart) / (distanceEnd - distanceStart), 0F, 1F);

		float starBrightness = (float)eclipseAmount;

		return MathHelper.clamp_float(starBrightness, distanceFactor, 1F);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public float getSunBrightness(float par1) {
		if(worldObj.provider.dimensionId != SpaceConfig.dmitriyDimension)
			return 0;

		return 1.0F - (float)eclipseAmount;
	}

	/**
	 * Matches celestial lightmap dimming so orbit reflects NONE/STARCORE/SUN sky-state brightness.
	 */
	@SideOnly(Side.CLIENT)
	public boolean updateLightmap(int[] lightmap) {
		boolean changed = false;
		final int redBoost = 0;
		final float greenBlueScale = 0.95F;
		final float skyDim = getSkyLightDimmer();

		for(int i = 0; i < lightmap.length; i++) {
			int[] colors = unpackColor(lightmap[i]);
			int r = colors[0];
			int g = colors[1];
			int b = colors[2];

			if(skyDim < 1.0F) {
				r = (int)(r * skyDim);
				g = (int)(g * skyDim);
				b = (int)(b * skyDim);
			}

			int nr = MathHelper.clamp_int(r + redBoost, 0, 255);
			int ng = MathHelper.clamp_int((int)(g * greenBlueScale), 0, 255);
			int nb = MathHelper.clamp_int((int)(b * greenBlueScale), 0, 255);

			if(nr != r || ng != g || nb != b) {
				lightmap[i] = packColor(nr, ng, nb);
				changed = true;
			}
		}

		return changed;
	}

	@SideOnly(Side.CLIENT)
	private float getSkyLightDimmer() {
		if(worldObj == null || worldObj.provider == null) {
			return 1.0F;
		}
		try {
			CBT_SkyState skyState = CBT_SkyState.get(worldObj);
			if(skyState.isBlackhole()) {
				long collapseEndTick = skyState.getBlackholeCollapseEndTick();
				if(collapseEndTick > 0L) {
					long now = worldObj.getTotalWorldTime();
					long collapseStartTick = collapseEndTick - StarcoreSkyEffects.BLACKHOLE_COLLAPSE_DURATION_TICKS;
					if(now >= collapseEndTick) {
						return NOTHING_SKY_DIM;
					}
					if(now > collapseStartTick) {
						float progress = MathHelper.clamp_float(
							(float)(now - collapseStartTick) / (float)StarcoreSkyEffects.BLACKHOLE_COLLAPSE_DURATION_TICKS,
							0.0F,
							1.0F
						);
						return BLACKHOLE_SKY_DIM + (NOTHING_SKY_DIM - BLACKHOLE_SKY_DIM) * progress;
					}
				}
				return BLACKHOLE_SKY_DIM;
			}
			if(skyState.isNothing()) {
				return NOTHING_SKY_DIM;
			}
			if(skyState.getState() == CBT_SkyState.SkyState.STARCORE) {
				float ratio = MathHelper.clamp_float(
					(float)((double)skyState.getStarcoreThroughput() / (double)CBT_SkyState.STARCORE_THRESHOLD_HE_PER_TICK),
					0.0F,
					1.0F
				);
				return NOTHING_SKY_DIM + (SUN_MIN_SKY_DIM - NOTHING_SKY_DIM) * ratio;
			}
			if(skyState.getState() == CBT_SkyState.SkyState.SUN) {
				float ratio = MathHelper.clamp_float(
					(float)((double)skyState.getSunCharge() / (double)CBT_SkyState.SUN_MAX_HE),
					0.0F,
					1.0F
				);
				return SUN_MIN_SKY_DIM + (1.0F - SUN_MIN_SKY_DIM) * ratio;
			}
			return 1.0F;
		} catch(Exception ignored) {
			return 1.0F;
		}
	}

	@SideOnly(Side.CLIENT)
	private int packColor(final int r, final int g, final int b) {
		return 255 << 24 | r << 16 | g << 8 | b;
	}

	@SideOnly(Side.CLIENT)
	private int[] unpackColor(final int color) {
		final int[] colors = new int[3];
		colors[0] = color >> 16 & 255;
		colors[1] = color >> 8 & 255;
		colors[2] = color & 255;
		return colors;
	}

	@Override
	public boolean canDoLightning(Chunk chunk) {
		return false;
	}

	@Override
	public boolean canDoRainSnowIce(Chunk chunk) {
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public float getCloudHeight() {
		return -99999;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IRenderHandler getSkyRenderer() {
		return new SkyProviderOrbit();
	}

	// Just fetches a memoised angle instead, for speed
	@Override
	public float calculateCelestialAngle(long worldTime, float partialTicks) {
		return celestialAngle;
	}

	// Same shit as in Celestial
	@Override
	public int getRespawnDimension(EntityPlayerMP player) {
		ChunkCoordinates coords = player.getBedLocation(dimensionId);

		// If no bed, respawn in overworld
		if(coords == null)
			return 0;

		// If the bed location has no breathable atmosphere, respawn in overworld
		CBT_Atmosphere atmosphere = ChunkAtmosphereManager.proxy.getAtmosphere(worldObj, coords.posX, coords.posY, coords.posZ);
		if(!ChunkAtmosphereManager.proxy.canBreathe(atmosphere))
			return 0;

		return dimensionId;
	}

	@Override
	public boolean canRespawnHere() {
		if(WorldProviderCelestial.attemptingSleep) {
			WorldProviderCelestial.attemptingSleep = false;
			return true;
		}

		return false;
	}

}

