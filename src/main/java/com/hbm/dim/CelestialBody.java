package com.hbm.dim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.hbm.config.SpaceConfig;
import com.hbm.dim.orbit.OrbitalStation;
import com.hbm.dim.dmitriy.WorldProviderDmitriy;
import com.hbm.dim.trait.CBT_Atmosphere;
import com.hbm.dim.trait.CBT_War;
import com.hbm.dim.trait.CBT_Dyson;
import com.hbm.dim.trait.CBT_SkyState;
import com.hbm.dim.trait.CBT_Atmosphere.FluidEntry;
import com.hbm.dim.trait.CBT_Water;
import com.hbm.dim.trait.CelestialBodyTrait;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.inventory.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FT_Gaseous;
import com.hbm.inventory.recipes.AtmosphereRecipes;
import com.hbm.inventory.recipes.AtmosphereRecipes.AtmosphereRecipe;
import com.hbm.items.ItemVOTVdrive.Target;
import com.hbm.lib.RefStrings;
import com.hbm.render.shader.Shader;
import com.hbm.util.AstronomyUtil;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DimensionManager;

public class CelestialBody {

	/**
	 * Stores planet data in a tree structure, allowing for bodies orbiting bodies
	 * Unit suffixes added when they differ from SI units, for clarity
	 */

	public String name;

	public int dimensionId = 0;

	public boolean canLand = false; // does this body have an associated dimension and a solid surface?

	// Orbital elements
	public float massKg = 0;
	public float radiusKm = 0;
	public float semiMajorAxisKm = 0; // Distance to the parent body
	public float semiMinorAxisFactor = 0; // has a sqrt so done ahead of time
	public float eccentricity = 0;
	public float inclination = 0;
	public float ascendingNode = 0;
	public float argumentPeriapsis = 0;

	private int rotationalPeriod = 6 * 60 * 60; // Day length in seconds
	private float baselineMassKg = -1.0F;
	private float baselineSemiMajorAxisKm = -1.0F;
	private float baselineEccentricity = 0.0F;
	private float baselineInclination = 0.0F;
	private float baselineAscendingNode = 0.0F;
	private float baselineArgumentPeriapsis = 0.0F;
	private float baselineParentMassKg = -1.0F;
	private int baselineRotationalPeriod = -1;
	private double baselineCoreDensityKgPerM3 = -1.0D;
	private boolean dynamicsBaselineCaptured = false;

	public float axialTilt = 0;

	private int minProcessingLevel = 0; // What level of technology can locate this body? This defines the minimum level, automatically adjusted based on stardar location

	public ResourceLocation texture = null;
	public ResourceLocation biomeMask = null;
	public ResourceLocation cityMask = null;
	public float[] color = new float[] {0.4F, 0.4F, 0.4F}; // When too small to render the texture

	public String tidallyLockedTo = null;

	public boolean hasRings = false; // put a ring on it
	public float ringTilt = 0;
	public float[] ringColor = new float[] {0.5F, 0.5F, 0.5F};
	public float ringSize = 2;

	public boolean hasIce = false; // has bedrock ice?

	public FluidType gas;
	private CelestialCore core;

	private static final double ATM_OVERPRESSURE_DISSIPATION_FACTOR = 0.02D;
	private static final double ATM_MIN_DISSIPATION_PER_UPDATE_ATM = 0.0001D;
	private static final double ATM_MIN_PRESENT_PRESSURE_ATM = 0.0001D;

	public List<CelestialBody> satellites = new ArrayList<CelestialBody>(); // moon boyes
	public CelestialBody parent = null;

	private HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits = new HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>();

	public ResourceLocation stoneTexture;
	public ResourceLocation surfaceTexture;
	public SolarSystem.Body type;

	@SideOnly(Side.CLIENT)
	public Shader shader;

	public float shaderScale = 1; // If the shader renders the item within the quad (not filling it entirely), scale it up from the true size

	public CelestialBody(String name) {
		this.name = name;
		this.texture = new ResourceLocation(RefStrings.MODID, "textures/misc/space/" + name + ".png");

		nameToBodyMap.put(name, this);
	}

	public CelestialBody(String name, int id, SolarSystem.Body type) {
		this(name);
		this.dimensionId = id;
		this.canLand = true;
		this.type = type;

		dimToBodyMap.put(id, this);
	}


	// Chainables for construction

	public CelestialBody withMassRadius(float kg, float km) {
		this.massKg = kg;
		this.radiusKm = km;
		if(this.core != null) {
			applyMassFromCore(this, this.core);
		}
		return this;
	}

	public CelestialBody withOrbitalParameters(float semiMajorAxisKm, float eccentricity, float argumentPeriapsisDegrees, float inclinationDegrees, float ascendingNodeDegrees) {
		this.semiMajorAxisKm = semiMajorAxisKm;
		this.semiMinorAxisFactor = (float)Math.sqrt(1 - eccentricity * eccentricity);
		this.eccentricity = eccentricity;
		this.argumentPeriapsis = (float)Math.toRadians(argumentPeriapsisDegrees);
		this.inclination = (float)Math.toRadians(inclinationDegrees);
		this.ascendingNode = (float)Math.toRadians(ascendingNodeDegrees);
		return this;
	}

	public CelestialBody withRotationalPeriod(int seconds) {
		this.rotationalPeriod = seconds;
		return this;
	}

	public CelestialBody withAxialTilt(float degrees) {
		this.axialTilt = degrees;
		return this;
	}

	public CelestialBody withMinProcessingLevel(int level) {
		this.minProcessingLevel = level;
		return this;
	}

	public CelestialBody withTexture(ResourceLocation location) {
		this.texture = location;
		return this;
	}

	public CelestialBody withCityMask(ResourceLocation location) {
		this.cityMask = location;
		return this;
	}

	public CelestialBody withBiomeMask(ResourceLocation location) {
		this.biomeMask = location;
		return this;
	}

	public CelestialBody withBlockTextures(String stone, String surface) {
		this.stoneTexture = new ResourceLocation(stone);
		this.surfaceTexture = new ResourceLocation(surface);
		return this;
	}

	public CelestialBody withColor(float... color) {
		this.color = color;
		return this;
	}

	public CelestialBody withTidalLockingTo(String name) {
		tidallyLockedTo = name;
		return this;
	}

	public CelestialBody withRings(float tilt, float size, float... color) {
		this.hasRings = true;
		this.ringTilt = tilt;
		this.ringSize = size;
		this.ringColor = color;
		return this;
	}

	public CelestialBody clearRings() {
		this.hasRings = false;
		this.ringTilt = 0.0F;
		this.ringSize = 2.0F;
		this.ringColor = new float[] { 0.5F, 0.5F, 0.5F };
		return this;
	}

	public CelestialBody withGas(FluidType gas) {
		this.gas = gas;
		return this;
	}

	public CelestialBody withCore(CelestialCore core) {
		this.core = core != null ? core.copy() : null;
		if(this.core != null && this.radiusKm > 0.0F) {
			applyMassFromCore(this, this.core);
		}
		return this;
	}

	public CelestialBody withSatellites(CelestialBody... bodies) {
		Collections.addAll(satellites, bodies);
		for(CelestialBody body : bodies) {
			body.parent = this;
		}
		return this;
	}

	public CelestialBody withTraits(CelestialBodyTrait... traits) {
		for(CelestialBodyTrait trait : traits) this.traits.put(trait.getClass(), trait);
		return this;
	}

	public CelestialBody removeTrait(Class<? extends CelestialBodyTrait> trait) {
		this.traits.remove(trait);
		return this;
	}

	public CelestialBody withShader(ResourceLocation fragmentShader) {
		return withShader(fragmentShader, 1);
	}

	public CelestialBody withShader(ResourceLocation fragmentShader, float scale) {
		if(FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER) return this;

		shader = new Shader(fragmentShader);
		shaderScale = scale;
		return this;
	}

	public CelestialBody withIce(boolean hasIce) {
		this.hasIce = hasIce;
		return this;
	}


	// /Chainables



	// Terraforming - trait overrides
	// If trait overrides exist, delete existing traits from the world, and replace them with the saved ones

	// Prefer statics over instance methods, for performance
	// but if you need to update a _different_ body (like, blowing up the sun)
	// instance members are the go

	public static void setTraits(World world, CelestialBodyTrait... traits) {
		SolarSystemWorldSavedData traitsData = SolarSystemWorldSavedData.get(world);
		CelestialBody body = getBody(world);
		traitsData.setTraits(body.name, traits);
		applyMassFromCurrentCore(body);
	}

	public static void setTraits(World world, Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits) {
		setTraits(world, traits.values().toArray(new CelestialBodyTrait[traits.size()]));
	}

	public void setTraits(CelestialBodyTrait... traits) {
		SolarSystemWorldSavedData traitsData = SolarSystemWorldSavedData.get();
		traitsData.setTraits(name, traits);
		applyMassFromCurrentCore(this);
	}

	public void setTraits(Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits) {
		setTraits(traits.values().toArray(new CelestialBodyTrait[traits.size()]));
	}

	// Gets a clone of the body traits that are SAFE for modifying
	public static HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> getTraits(World world) {
		SolarSystemWorldSavedData traitsData = SolarSystemWorldSavedData.get(world);
		CelestialBody body = getBody(world);
		HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = traitsData.getTraits(body.name);

		if(currentTraits == null) {
			currentTraits = new HashMap<>();
			for(CelestialBodyTrait trait : body.traits.values()) {
				currentTraits.put(trait.getClass(), cloneTrait(trait));
			}
		}

		return currentTraits;
	}

	public HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> getTraits() {
		SolarSystemWorldSavedData traitsData = SolarSystemWorldSavedData.get();
		HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = traitsData.getTraits(name);

		if(currentTraits == null) {
			currentTraits = new HashMap<>();
			for(CelestialBodyTrait trait : traits.values()) {
				currentTraits.put(trait.getClass(), cloneTrait(trait));
			}
		}

		return currentTraits;
	}

	private static CelestialBodyTrait cloneTrait(CelestialBodyTrait trait) {
		try {
			CelestialBodyTrait clone = trait.getClass().newInstance();
			NBTTagCompound nbt = new NBTTagCompound();
			trait.writeToNBT(nbt);
			clone.readFromNBT(nbt);
			return clone;
		} catch(Exception ignored) {
			return trait;
		}
	}

	public static void modifyTraits(World world, CelestialBodyTrait... traits) {
		HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = getTraits(world);

		for(CelestialBodyTrait trait : traits) {
			currentTraits.put(trait.getClass(), trait);
		}

		setTraits(world, currentTraits);
	}

	public void modifyTraits(CelestialBodyTrait... traits) {
		HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = getTraits();

		for(CelestialBodyTrait trait : traits) {
			currentTraits.put(trait.getClass(), trait);
		}

		setTraits(currentTraits);
	}

	public static void clearTraits(World world) {
		SolarSystemWorldSavedData traitsData = SolarSystemWorldSavedData.get(world);
		CelestialBody body = getBody(world);

		traitsData.clearTraits(body.name);
		applyMassFromCurrentCore(body);
	}

	public void clearTraits() {
		SolarSystemWorldSavedData traitsData = SolarSystemWorldSavedData.get();

		traitsData.clearTraits(name);
		applyMassFromCurrentCore(this);
	}

	public static void applyMassFromCore(CelestialBody body, CelestialCore core) {
		core.recalculateForRadius(body.radiusKm);
		body.massKg = (float) core.computedMassKg;
		body.applyOrbitalDynamicsFromCore(core);
	}

	private static void applyMassFromCurrentCore(CelestialBody body) {
		CelestialCore core = body.getCore();
		if(core != null) {
			applyMassFromCore(body, core);
		}
	}

	public void captureDynamicBaselinesRecursive() {
		CelestialCore core = getCore();
		captureDynamicsBaseline(core);

		for(CelestialBody satellite : satellites) {
			satellite.captureDynamicBaselinesRecursive();
		}
	}

	private void captureDynamicsBaseline(CelestialCore core) {
		if(dynamicsBaselineCaptured) return;

		baselineMassKg = massKg;
		baselineSemiMajorAxisKm = semiMajorAxisKm;
		baselineEccentricity = eccentricity;
		baselineInclination = inclination;
		baselineAscendingNode = ascendingNode;
		baselineArgumentPeriapsis = argumentPeriapsis;
		baselineRotationalPeriod = rotationalPeriod;
		baselineParentMassKg = parent != null ? parent.massKg : -1.0F;
		if(core != null) {
			if(core.computedRadiusKm != radiusKm) {
				core.recalculateForRadius(radiusKm);
			}
			baselineCoreDensityKgPerM3 = Math.max(core.computedBulkDensityKgPerM3, 1.0D);
		}

		dynamicsBaselineCaptured = true;
	}

	private void applyOrbitalDynamicsFromCore(CelestialCore core) {
		if(core == null) return;

		captureDynamicsBaseline(core);

		double massRatio = baselineMassKg > 0.0F ? (double)massKg / (double)baselineMassKg : 1.0D;
		massRatio = clampDouble(massRatio, 0.25D, 8.0D);

		double densityRatio = 1.0D;
		if(baselineCoreDensityKgPerM3 > 0.0D) {
			densityRatio = core.computedBulkDensityKgPerM3 / baselineCoreDensityKgPerM3;
		}
		densityRatio = clampDouble(densityRatio, 0.25D, 8.0D);

		double rotationScale = clampDouble(Math.pow(massRatio, 0.6D) * Math.pow(densityRatio, 0.2D), 0.5D, 4.0D);
		if(baselineRotationalPeriod > 0) {
			rotationalPeriod = Math.max(60, (int)Math.round((double)baselineRotationalPeriod * rotationScale));
		}

		if(parent == null || baselineSemiMajorAxisKm <= 0.0F) return;

		double parentMassRatio = 1.0D;
		if(baselineParentMassKg > 0.0F && parent.massKg > 0.0F) {
			parentMassRatio = (double)parent.massKg / (double)baselineParentMassKg;
		}
		parentMassRatio = clampDouble(parentMassRatio, 0.25D, 8.0D);

		double orbitalScale = clampDouble(Math.pow(parentMassRatio, -1.0D / 3.0D) * Math.pow(densityRatio, -0.05D), 0.5D, 2.0D);
		semiMajorAxisKm = baselineSemiMajorAxisKm * (float)orbitalScale;

		double eccentricityScale = clampDouble(Math.pow(densityRatio, 0.15D), 0.7D, 1.3D);
		eccentricity = (float)clampDouble((double)baselineEccentricity * eccentricityScale, 0.0D, 0.95D);
		semiMinorAxisFactor = (float)Math.sqrt(Math.max(0.0D, 1.0D - eccentricity * eccentricity));

		double inclinationScale = clampDouble(Math.pow(densityRatio, 0.08D), 0.8D, 1.2D);
		inclination = baselineInclination * (float)inclinationScale;
		ascendingNode = baselineAscendingNode;
		argumentPeriapsis = baselineArgumentPeriapsis;
	}

	private static double clampDouble(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	// he has ceased to be
	public static void degas(World world) {
		HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = getTraits(world);

		currentTraits.remove(CBT_Atmosphere.class);

		setTraits(world, currentTraits);
	}

	public static boolean consumeGas(World world, FluidType fluid, double amount) {
		HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = getTraits(world);

		CBT_Atmosphere atmosphere = (CBT_Atmosphere) currentTraits.get(CBT_Atmosphere.class);
		if(atmosphere == null) return false;

		boolean didConsume = false;
		int emptyIndex = -1;
		for(int i = 0; i < atmosphere.fluids.size(); i++) {
			FluidEntry entry = atmosphere.fluids.get(i);
			if(entry.fluid == fluid) {
				entry.pressure -= amount / AstronomyUtil.MB_PER_ATM;
				didConsume = true;
				emptyIndex = entry.pressure <= 0 ? i : -1;
				break;
			}
		}

		if(emptyIndex >= 0) {
			atmosphere.fluids.remove(emptyIndex);

			if(atmosphere.fluids.size() == 0) {
				currentTraits.remove(CBT_Atmosphere.class);
			}
		}

		setTraits(world, currentTraits);

		return didConsume;
	}

	public static void emitGas(World world, FluidType fluid, double amount) {
		if(amount <= 0.0D) return;

		HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = getTraits(world);

		CBT_Atmosphere atmosphere = (CBT_Atmosphere) currentTraits.get(CBT_Atmosphere.class);
		if(atmosphere == null) {
			atmosphere = new CBT_Atmosphere();
			currentTraits.put(CBT_Atmosphere.class, atmosphere);
		}

		double pressureDelta = amount / AstronomyUtil.MB_PER_ATM;
		FluidEntry existingEntry = null;
		int existingIndex = -1;

		for(int i = 0; i < atmosphere.fluids.size(); i++) {
			FluidEntry entry = atmosphere.fluids.get(i);
			if(entry.fluid == fluid) {
				existingEntry = entry;
				existingIndex = i;
				break;
			}
		}

		if(existingEntry != null) {
			existingEntry.pressure += pressureDelta;

			// Keep "most recently added/touched" gases at the end so overpressure loss targets them first.
			if(existingIndex >= 0 && existingIndex < atmosphere.fluids.size() - 1) {
				atmosphere.fluids.remove(existingIndex);
				atmosphere.fluids.add(existingEntry);
			}
		} else {
			// Sort existing fluids and remove the lowest fraction
			if(atmosphere.fluids.size() >= 8) {
				atmosphere.sortDescending();
				atmosphere.fluids.remove(atmosphere.fluids.size() - 1);
			}

			atmosphere.fluids.add(new FluidEntry(fluid, pressureDelta));
		}

		setTraits(world, currentTraits);
	}

	public static boolean reactAtmosphere(World world, Entry<FluidStack, AtmosphereRecipe> recipe) {
		HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = getTraits(world);
		CBT_Atmosphere atmosphere = (CBT_Atmosphere) currentTraits.get(CBT_Atmosphere.class);

		int scale = 64;

		// because atmochem runs infrequently, we will automatically scale this to react all it can immediately
		for(FluidStack recipeFluid : recipe.getValue().inputFluids) {
			boolean hasInput = false;
			
			for(CBT_Atmosphere.FluidEntry entry : atmosphere.fluids) {
				if(entry.fluid == recipeFluid.type && entry.pressure * AstronomyUtil.MB_PER_ATM >= recipeFluid.fill * scale) hasInput = true;
			}

			if(!hasInput) return false;
		}

		for(FluidStack recipeFluid : recipe.getValue().inputFluids) {
			FT_Gaseous.capture(world, recipeFluid.type, recipeFluid.fill * scale);
		}

		FT_Gaseous.release(world, recipe.getKey().type, recipe.getKey().fill * scale);

		return true;
	}

	public static void updateChemistry(World world) {
		boolean hasUpdated = false;
		CelestialBody body = getBody(world);
		HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = getTraits(world);
		CBT_Atmosphere atmosphere = (CBT_Atmosphere) currentTraits.get(CBT_Atmosphere.class);

		CBT_Water water = (CBT_Water) currentTraits.get(CBT_Water.class);
		if(water == null) {

			if(atmosphere != null) {
				double pressure = 0;
				for(FluidEntry entry : atmosphere.fluids) {
					if(entry.fluid == Fluids.STEAM
					|| entry.fluid == Fluids.HOTSTEAM
					|| entry.fluid == Fluids.SUPERHOTSTEAM
					|| entry.fluid == Fluids.ULTRAHOTSTEAM
					|| entry.fluid == Fluids.SPENTSTEAM) {
						pressure += entry.pressure;
					}
				}

				if(pressure > 0.2D) {
					currentTraits.put(CBT_Water.class, new CBT_Water());
					hasUpdated = true;
				}
			}
		}

		if(atmosphere != null) {
			for(Entry<FluidStack, AtmosphereRecipe> recipe : AtmosphereRecipes.getRecipesMap().entrySet()) {
				if(reactAtmosphere(world, recipe)) {
					hasUpdated = true;
				}
			}

			if(dissipateOverpressureAtmosphere(body, atmosphere)) {
				hasUpdated = true;
				if(atmosphere.fluids.isEmpty()) {
					currentTraits.remove(CBT_Atmosphere.class);
				}
			}
		}

		if(hasUpdated)
			setTraits(world, currentTraits);
	}

	private static boolean dissipateOverpressureAtmosphere(CelestialBody body, CBT_Atmosphere atmosphere) {
		if(body == null || atmosphere == null || atmosphere.fluids.isEmpty()) return false;

		double maxPressure = getAtmosphereRetentionLimitAtm(body);
		if(Double.isInfinite(maxPressure) || Double.isNaN(maxPressure)) return false;

		double totalPressure = atmosphere.getPressure();
		double excessPressure = totalPressure - maxPressure;
		if(excessPressure <= 0.0D) return false;

		int lastIndex = atmosphere.fluids.size() - 1;
		FluidEntry lastAdded = atmosphere.fluids.get(lastIndex);
		if(lastAdded == null || lastAdded.pressure <= 0.0D) {
			atmosphere.fluids.remove(lastIndex);
			return true;
		}

		double dissipatedPressure = Math.max(ATM_MIN_DISSIPATION_PER_UPDATE_ATM, excessPressure * ATM_OVERPRESSURE_DISSIPATION_FACTOR);
		dissipatedPressure = Math.min(dissipatedPressure, lastAdded.pressure);

		if(dissipatedPressure <= 0.0D) return false;

		lastAdded.pressure -= dissipatedPressure;
		if(lastAdded.pressure <= ATM_MIN_PRESENT_PRESSURE_ATM) {
			atmosphere.fluids.remove(lastIndex);
		}

		return true;
	}

	public static double getAtmosphereRetentionLimitAtm(World world) {
		return getAtmosphereRetentionLimitAtm(getBody(world));
	}

	public static double getAtmosphereRetentionLimitAtm(CelestialBody body) {
		if(body == null) return Double.POSITIVE_INFINITY;

		double bodyCoreMassKg = getBodyCoreMassKg(body);
		if(bodyCoreMassKg <= 0.0D) return Double.POSITIVE_INFINITY;

		double kerbinCoreMassKg = getReferenceCoreMassKg("kerbin");
		double eveCoreMassKg = getReferenceCoreMassKg("eve");
		if(kerbinCoreMassKg <= 0.0D || eveCoreMassKg <= 0.0D || Math.abs(eveCoreMassKg - kerbinCoreMassKg) < 1.0D) {
			return 1.5D;
		}

		double interpolation = (bodyCoreMassKg - kerbinCoreMassKg) / (eveCoreMassKg - kerbinCoreMassKg);
		double maxPressure = 1.5D + interpolation * (6.0D - 1.5D);
		return Math.max(0.0D, maxPressure);
	}

	private static double getReferenceCoreMassKg(String bodyName) {
		CelestialBody reference = nameToBodyMap.get(bodyName);
		if(reference == null || !bodyName.equals(reference.name)) return -1.0D;
		return getBodyCoreMassKg(reference);
	}

	private static double getBodyCoreMassKg(CelestialBody body) {
		CelestialCore core = body.getCore();
		if(core == null) return -1.0D;

		if(core.computedRadiusKm != body.radiusKm) {
			core.recalculateForRadius(body.radiusKm);
		}

		return core.computedCoreMassKg;
	}

	// Called once per tick to attenuate swarm counts based on a swarm half-life
	public static void updateSwarms() {
		// We currently only have the one solar body, so we just update that directly
		HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = SolarSystem.kerbol.getTraits();

		CBT_SkyState skyState = (CBT_SkyState) currentTraits.get(CBT_SkyState.class);
		if(skyState == null) {
			skyState = SolarSystem.kerbol.getDefaultTrait(CBT_SkyState.class);
		}
		if(skyState != null && skyState.isBlackhole()) return;

		CBT_Dyson dyson = (CBT_Dyson) currentTraits.get(CBT_Dyson.class);
		if(dyson == null) return;

		dyson.attenuate();
	}

	// /Terraforming



	public static void damage(int dmg, World world) {
		HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = getTraits(world);

		CBT_War war = (CBT_War) currentTraits.get(CBT_War.class);
		if(war == null) {
			war = new CBT_War();
			currentTraits.put(CBT_War.class, war);
		}

		if(war.shield > 0) {
			war.shield -= dmg;
		} else {
			war.health -= dmg;
		}

		setTraits(world, currentTraits);
	}


	// Static getters
	// A lot of these are member getters but without having to check the celestial body exists
	// If it doesn't exist, return the overworld as the default, may cause issues with terraforming the overworld

	private static HashMap<Integer, CelestialBody> dimToBodyMap = new HashMap<Integer, CelestialBody>();
	private static HashMap<String, CelestialBody> nameToBodyMap = new HashMap<String, CelestialBody>();

	public static Collection<CelestialBody> getAllBodies() {
		return nameToBodyMap.values();
	}

	public static Collection<CelestialBody> getLandableBodies() {
		return dimToBodyMap.values();
	}

	public static CelestialBody getBody(String name) {
		CelestialBody body = nameToBodyMap.get(name);
		return body != null ? body : dimToBodyMap.get(0);
	}

	public static CelestialBody getBody(int id) {
		CelestialBody body = dimToBodyMap.get(id);
		return body != null ? body : dimToBodyMap.get(0);
	}

	// bit of a dumb one but the other function is already used widely
	public static CelestialBody getBodyOrNull(int id) {
		return dimToBodyMap.get(id);
	}

	public static CelestialBody getBody(World world) {
		return getBody(world.provider.dimensionId);
	}

	public static Target getTarget(World world, int x, int z) {
		if(inOrbit(world)) {
			OrbitalStation station = !world.isRemote ? OrbitalStation.getStationFromPosition(x, z) : OrbitalStation.clientStation;
			return new Target(station.orbiting, true, station.hasStation);
		}

		return new Target(getBody(world), false, true);
	}

	public static CelestialBody getStar(World world) {
		return getBody(world).getStar();
	}

	public static CelestialBody getPlanet(World world) {
		return getBody(world).getPlanet();
	}


	public static float getGravity(EntityLivingBase entity) {
		if(entity instanceof EntityWaterMob) return AstronomyUtil.STANDARD_GRAVITY;

		if(inOrbit(entity.worldObj)) {
			if(HbmLivingProps.hasGravity(entity)) {
				OrbitalStation station = entity.worldObj.isRemote
					? OrbitalStation.clientStation
					: OrbitalStation.getStationFromPosition((int)entity.posX, (int)entity.posZ);

				float gravity = AstronomyUtil.STANDARD_GRAVITY * station.gravityMultiplier;
				if(gravity < 0.2) return 0;
				return gravity;
			}

			return 0;
		}

		CelestialBody body = CelestialBody.getBody(entity.worldObj);
		float gravity = body.getSurfaceGravity() * AstronomyUtil.PLAYER_GRAVITY_MODIFIER;
		if(entity.worldObj.provider instanceof WorldProviderCelestial) {
			WorldProviderCelestial provider = (WorldProviderCelestial) entity.worldObj.provider;
			gravity *= provider.getGravityMultiplier();
		} else if(entity.worldObj.provider.dimensionId == 0) {
			gravity *= SolarSystemWorldSavedData.get(entity.worldObj).getKerbinGravityMultiplier();
		}
		return gravity;
	}

	public static boolean inOrbit(World world) {
		return world.provider.dimensionId == SpaceConfig.orbitDimension;
	}

	public static SolarSystem.Body getEnum(World world) {
		return getBody(world).getEnum();
	}

	public static int getMeta(World world) {
		return getBody(world).getEnum().ordinal();
	}

	public static double getRotationalPeriod(World world) {
		return getBody(world).getRotationalPeriod();
	}

	public static float getSemiMajorAxis(World world) {
		return getBody(world).semiMajorAxisKm;
	}

	public static boolean hasTrait(World world, Class<? extends CelestialBodyTrait> trait) {
		return getBody(world).hasTrait(trait);
	}

	public static <T extends CelestialBodyTrait> T getTrait(World world, Class<? extends T> trait) {
		return getBody(world).getTrait(trait);
	}

	public static CelestialCore getCore(World world) {
		return getBody(world).getCore();
	}

	public static void setCore(World world, CelestialCore core) {
		CelestialBody body = getBody(world);
		body.withCore(core);
		applyMassFromCurrentCore(body);
	}

	public static boolean hasDefaultTrait(World world, Class<? extends CelestialBodyTrait> trait) {
		return getBody(world).hasDefaultTrait(trait);
	}

	public static <T extends CelestialBodyTrait> T getDefaultTrait(World world, Class<? extends T> trait) {
		return getBody(world).getDefaultTrait(trait);
	}

	// /Statics



	public String getUnlocalizedName() {
		return name;
	}

	public SolarSystem.Body getEnum() {
		return type;
	}

	public CelestialBody getStar() {
		CelestialBody body = this;
		while(body.parent != null)
			body = body.parent;

		return body;
	}

	public CelestialBody getPlanet() {
		if(this.parent == null) return this;
		CelestialBody body = this;
		while(body.parent.parent != null)
			body = body.parent;

		return body;
	}

	// Returns the day length in ticks, adjusted for the 20 minute minecraft day
	public double getRotationalPeriod() {
		return (double)rotationalPeriod * (AstronomyUtil.DAY_FACTOR / (double)AstronomyUtil.TIME_MULTIPLIER) * 20;
	}

	// Returns the year length in days, derived from semi-major axis
	public double getOrbitalPeriod() {
		if(parent == null) return 0;
		double semiMajorAxis = semiMajorAxisKm * 1_000;
		double orbitalPeriod = 2 * Math.PI * Math.sqrt((semiMajorAxis * semiMajorAxis * semiMajorAxis) / (AstronomyUtil.GRAVITATIONAL_CONSTANT * parent.massKg));
		return orbitalPeriod / (double)AstronomyUtil.SECONDS_IN_KSP_DAY;
	}

	// Get the gravitational force at the surface, derived from mass and radius
	public float getSurfaceGravity() {
		float radius = radiusKm * 1000;
		return AstronomyUtil.GRAVITATIONAL_CONSTANT * massKg / (radius * radius);
	}

	// Get the power multiplier for sun based machines
	public float getSunPower() {
		float distanceAU = getPlanet().semiMajorAxisKm / AstronomyUtil.KM_IN_AU;
		return 1 / (distanceAU * distanceAU);
	}

	// Processing level is based off of where you are processing from, so if you're on Duna, Ike will be tier 0
	public int getProcessingLevel(CelestialBody from) {
		int level = 3;

		if(this == from) {
			// If self, tier 0
			level = 0;
		} else if(this == from.parent || this.parent == from) {
			// If going to/from a moon, tier 0
			level = 0;
		} else {
			// Otherwise, tier 1
			level = 1;
		}

		// Unless a minimum processing level is set
		return Math.max(level, minProcessingLevel);
	}


	public boolean hasTrait(Class<? extends CelestialBodyTrait> trait) {
		return getTraitsUnsafe().containsKey(trait);
	}

	@SuppressWarnings("unchecked")
	public <T extends CelestialBodyTrait> T getTrait(Class<? extends T> trait) {
		return (T) getTraitsUnsafe().get(trait);
	}

	// Don't modify traits returned from this!
	private HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> getTraitsUnsafe() {
		Side side = FMLCommonHandler.instance().getEffectiveSide();
		HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits;
		if(side == Side.CLIENT) {
			traits = SolarSystemWorldSavedData.getClientTraits(name);
		} else {
			traits = SolarSystemWorldSavedData.get().getTraits(name);
		}

		if(traits != null)
			return traits;

		return this.traits;
	}

	public boolean hasDefaultTrait(Class<? extends CelestialBodyTrait> trait) {
		return traits.containsKey(trait);
	}

	@SuppressWarnings("unchecked")
	public <T extends CelestialBodyTrait> T getDefaultTrait(Class<? extends T> trait) {
		return (T) traits.get(trait);
	}

	public CelestialCore getCore() {
		return core;
	}

	// Loads in the heightmap data for a given chunk
	public int[] getHeightmap(int chunkX, int chunkZ) {
		WorldServer world = DimensionManager.getWorld(dimensionId);

		// Dimension isn't already loaded, try loading it now
		if(world == null) {
			DimensionManager.initDimension(dimensionId);
			world = DimensionManager.getWorld(dimensionId);

			if(world == null) return null;
		}

		// Load OR generate the desired chunk
		Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
		return chunk.heightMap;
	}

}

