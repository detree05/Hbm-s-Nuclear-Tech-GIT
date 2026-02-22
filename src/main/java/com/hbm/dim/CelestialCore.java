package com.hbm.dim;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.util.BufferUtil;

import net.minecraft.item.ItemStack;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.oredict.OreDictionary;

public class CelestialCore {

	private static final HashMap<String, Double> DENSITY_BY_TOKEN_KG_M3 = new HashMap<String, Double>();
	private static final HashMap<String, String> CATEGORY_BY_TOKEN = new HashMap<String, String>();
	private static final HashMap<String, Float> BASE_RAD_BY_TOKEN = new HashMap<String, Float>();
	private static final HashMap<String, Float> RAD_BY_OREDICT_CACHE = new HashMap<String, Float>();
	private static final Set<String> VALID_CATEGORY_KEYS = new HashSet<String>(Arrays.asList(
		"light", "heavy", "rare", "actinide", "nonmetal", "crystal", "schrabidic", "living"
	));

	public static final String CAT_LIGHT = "light";
	public static final String CAT_HEAVY = "heavy";
	public static final String CAT_RARE = "rare";
	public static final String CAT_ACTINIDE = "actinide";
	public static final String CAT_NONMETAL = "nonmetal";
	public static final String CAT_CRYSTAL = "crystal";
	public static final String CAT_SCHRABIDIC = "schrabidic";
	public static final String CAT_LIVING = "living";

	static {
		// Base material densities (kg/m^3)
		registerDensity("Iron", 7_870D);
		registerDensity("Copper", 8_960D);
		registerDensity("Aluminum", 2_700D);
		registerDensity("Aluminium", 2_700D);
		registerDensity("Sodium", 968D);
		registerDensity("Tungsten", 19_300D);
		registerDensity("Zinc", 7_140D);
		registerDensity("Coal", 1_350D);
		registerDensity("Lignite", 1_200D);
		registerDensity("Sulfur", 2_070D);
		registerDensity("Saltpeter", 2_110D);
		registerDensity("Redstone", 2_650D);
		registerDensity("Asbestos", 2_500D);
		registerDensity("Diamond", 3_510D);
		registerDensity("Emerald", 2_760D);
		registerDensity("Lithium", 534D);
		registerDensity("Chlorocalcite", 2_150D);
		registerDensity("Lead", 11_340D);
		registerDensity("Gold", 19_320D);
		registerDensity("Bismuth", 9_780D);
		registerDensity("Cobalt", 8_900D);
		registerDensity("RareEarth", 7_200D);
		registerDensity("Neodymium", 7_000D);
		registerDensity("Strontium", 2_640D);
		registerDensity("Fluorite", 3_180D);
		registerDensity("Silicon", 2_330D);
		registerDensity("Quartz", 2_650D);
		registerDensity("Sodalite", 2_290D);
		registerDensity("Cinnabar", 8_100D);
		registerDensity("Titanium", 4_500D);
		registerDensity("Zirconium", 6_520D);
		registerDensity("Boron", 2_460D);
		registerDensity("NickelPure", 8_908D);
		registerDensity("Beryllium", 1_850D);
		registerDensity("Thorium232", 11_700D);
		registerDensity("Radium226", 5_500D);
		registerDensity("Polonium210", 9_200D);
		registerDensity("Uranium233", 19_050D);
		registerDensity("RedPhosphorus", 2_300D);
		registerDensity("WhitePhosphorus", 1_820D);
		registerDensity("Australium", 19_500D);
		registerDensity("Tasmanite", 12_000D);
		registerDensity("Ayerite", 15_000D);
		registerDensity("Bromine", 3_120D);
		registerDensity("Glowstone", 2_500D);
		registerDensity("Molysite", 4_500D);
		registerDensity("Cadmium", 8_650D);
		registerDensity("Gallium", 5_910D);
		registerDensity("Arsenic", 5_720D);
		registerDensity("Tantalum", 16_650D);
		registerDensity("Lanthanum", 6_150D);
		registerDensity("Lanthanium", 6_150D);
		registerDensity("Niobium", 8_570D);
		registerDensity("Uranium", 19_100D);
		registerDensity("Technetium99", 11_500D);
		registerDensity("Technetium", 11_500D);
		registerDensity("Uranium238", 19_050D);
		registerDensity("Borax", 1_730D);
		registerDensity("Iodine", 4_930D);
		registerDensity("Neptunium237", 20_450D);
		registerDensity("Neptunium", 20_450D);
		registerDensity("Lead209", 11_340D);
		registerDensity("Gold198", 19_320D);
		registerDensity("Schrabidium", 25_000D);
		registerDensity("Schrabidate", 20_000D);
		registerDensity("FerricSchrabidate", 20_000D);
		registerDensity("Solinium", 23_000D);
		registerDensity("Ghiorsium336", 27_000D);
		registerDensity("Ghiorisium", 27_000D);
		registerDensity("Bauxite", 2_500D);
		registerDensity("Plutonium", 19_800D);
		registerDensity("Plutonium238", 19_800D);
		registerDensity("Plutonium239", 19_800D);
		registerDensity("Plutonium240", 19_800D);
		registerDensity("Plutonium241", 19_800D);
		registerDensity("Uranium235", 19_050D);
		registerDensity("Hafnium", 13_310D);
		registerDensity("Polymer", 950D);
		registerDensity("Rubber", 1_100D);
		registerDensity("Semtex", 1_500D);
		registerDensity("PVC", 1_380D);
		registerDensity("Mingrade", 8_960D);
		registerDensity("IndustrialGradeCopper", 8_960D);
		registerDensity("MinecraftGradeCopper", 8_960D);
		registerDensity("AdvancedAlloy", 7_900D);
		registerDensity("Starmetal", 14_000D);
		registerDensity("Desh", 7_850D);
		registerDensity("Yharonite", 64_050D);
		registerDensity("Ethyroite", 64_050D);
		registerDensity("MagnetizedTungsten", 19_300D);
		registerDensity("StainlessSteel", 8_000D);
		registerDensity("Saturnite", 12_500D);
		registerDensity("BSCCO", 6_400D);
		registerDensity("Steel", 7_850D);
		registerDensity("HighSpeedSteel", 8_100D);
		registerDensity("GunMetal", 8_800D);
		registerDensity("WeaponSteel", 7_900D);
		registerDensity("Ferrouranium", 12_000D);
		registerDensity("CMBSteel", 8_200D);
		registerDensity("RareEarthOreChunk", 7_200D);
		registerDensity("Cryolite", 2_950D);
		registerDensity("U233", 19_050D);
		registerDensity("U235", 19_050D);
		registerDensity("U238", 19_050D);
		registerDensity("Thorium", 11_700D);
		registerDensity("Th232", 11_700D);
		registerDensity("Radium", 5_500D);
		registerDensity("Ra226", 5_500D);
		registerDensity("Polonium", 9_200D);
		registerDensity("Po210", 9_200D);
		registerDensity("Pu238", 19_800D);
		registerDensity("Pu239", 19_800D);
		registerDensity("Pu240", 19_800D);
		registerDensity("Pu241", 19_800D);
		registerDensity("Np237", 20_450D);
		registerDensity("Tc99", 11_500D);
		registerDensity("Cobalt60", 8_900D);
		registerDensity("Co60", 8_900D);
		registerDensity("Pb209", 11_340D);
		registerDensity("Au198", 19_320D);
		registerDensity("Strontium90", 2_640D);
		registerDensity("Sr90", 2_640D);
		registerDensity("Actinium227", 10_070D);
		registerDensity("Ac227", 10_070D);
		registerDensity("Niter", 2_110D);
		registerDensity("Phosphorus", 2_300D);
		registerDensity("Graphite", 2_260D);
		registerDensity("Bakelite", 1_300D);
		registerDensity("Latex", 920D);
		registerDensity("Polycarbonate", 1_200D);
		registerDensity("Euphemium", 24_000D);
		registerDensity("CrystallineFullerite", 1_650D);
		registerDensity("Rift", 191_619D);
		registerDensity("Abyss", 191_619D);

		// Base radioactivity levels mirror the values used in ore dict hazard registration.
		registerRadioactivity("Uraninite", HazardRegistry.u);
		registerRadioactivity("Uranium", HazardRegistry.u);
		registerRadioactivity("Uranium233", HazardRegistry.u233);
		registerRadioactivity("U233", HazardRegistry.u233);
		registerRadioactivity("Uranium235", HazardRegistry.u235);
		registerRadioactivity("U235", HazardRegistry.u235);
		registerRadioactivity("Uranium238", HazardRegistry.u238);
		registerRadioactivity("U238", HazardRegistry.u238);
		registerRadioactivity("Thorium", HazardRegistry.th232);
		registerRadioactivity("Thorium232", HazardRegistry.th232);
		registerRadioactivity("Th232", HazardRegistry.th232);
		registerRadioactivity("Radium", HazardRegistry.ra226);
		registerRadioactivity("Radium226", HazardRegistry.ra226);
		registerRadioactivity("Ra226", HazardRegistry.ra226);
		registerRadioactivity("Polonium", HazardRegistry.po210);
		registerRadioactivity("Polonium210", HazardRegistry.po210);
		registerRadioactivity("Po210", HazardRegistry.po210);
		registerRadioactivity("Plutonium", HazardRegistry.pu);
		registerRadioactivity("Plutonium238", HazardRegistry.pu238);
		registerRadioactivity("Pu238", HazardRegistry.pu238);
		registerRadioactivity("Plutonium239", HazardRegistry.pu239);
		registerRadioactivity("Pu239", HazardRegistry.pu239);
		registerRadioactivity("Plutonium240", HazardRegistry.pu240);
		registerRadioactivity("Pu240", HazardRegistry.pu240);
		registerRadioactivity("Plutonium241", HazardRegistry.pu241);
		registerRadioactivity("Pu241", HazardRegistry.pu241);
		registerRadioactivity("Neptunium", HazardRegistry.np237);
		registerRadioactivity("Neptunium237", HazardRegistry.np237);
		registerRadioactivity("Np237", HazardRegistry.np237);
		registerRadioactivity("Technetium", HazardRegistry.tc99);
		registerRadioactivity("Technetium99", HazardRegistry.tc99);
		registerRadioactivity("Tc99", HazardRegistry.tc99);
		registerRadioactivity("Gold198", HazardRegistry.au198);
		registerRadioactivity("Au198", HazardRegistry.au198);
		registerRadioactivity("Cobalt60", HazardRegistry.co60);
		registerRadioactivity("Co60", HazardRegistry.co60);
		registerRadioactivity("Lead209", HazardRegistry.pb209);
		registerRadioactivity("Pb209", HazardRegistry.pb209);
		registerRadioactivity("Strontium90", HazardRegistry.sr90);
		registerRadioactivity("Sr90", HazardRegistry.sr90);
		registerRadioactivity("Actinium227", HazardRegistry.ac227);
		registerRadioactivity("Ac227", HazardRegistry.ac227);
		registerRadioactivity("Schrabidium", HazardRegistry.sa326);
		registerRadioactivity("Solinium", HazardRegistry.sa327);
		registerRadioactivity("Schrabidate", HazardRegistry.sb);
		registerRadioactivity("Schraranium", HazardRegistry.sr);
		registerRadioactivity("Ghiorsium336", HazardRegistry.gh336);
		registerRadioactivity("Gh336", HazardRegistry.gh336);
		registerRadioactivity("WatzMud", HazardRegistry.mud);
		registerRadioactivity("Moscovium45", HazardRegistry.mc45);
		registerRadioactivity("Mc45", HazardRegistry.mc45);
		registerRadioactivity("Chinesium989", HazardRegistry.cn989);
		registerRadioactivity("Cn989", HazardRegistry.cn989);

		// Canonical material-category assignments for all core composition materials.
		// If a token appears in multiple category lists, first registration wins.
		registerCategory("Iron", CAT_LIGHT);
		registerCategory("Copper", CAT_LIGHT);
		registerCategory("Aluminum", CAT_LIGHT);
		registerCategory("Aluminium", CAT_LIGHT);
		registerCategory("Cryolite", CAT_LIGHT);
		registerCategory("Sodium", CAT_LIGHT);
		registerCategory("Lithium", CAT_LIGHT);
		registerCategory("Chlorocalcite", CAT_LIGHT);
		registerCategory("Titanium", CAT_LIGHT);
		registerCategory("NickelPure", CAT_LIGHT);
		registerCategory("Nickel", CAT_LIGHT);
		registerCategory("Cadmium", CAT_LIGHT);
		registerCategory("Gallium", CAT_LIGHT);
		registerCategory("Cobalt", CAT_LIGHT);
		registerCategory("Bauxite", CAT_LIGHT);
		registerCategory("Hafnium", CAT_LIGHT);
		registerCategory("IndustrialGradeCopper", CAT_LIGHT);
		registerCategory("MinecraftGradeCopper", CAT_LIGHT);
		registerCategory("Mingrade", CAT_LIGHT);
		registerCategory("AdvancedAlloy", CAT_LIGHT);
		registerCategory("Starmetal", CAT_LIGHT);
		registerCategory("Desh", CAT_LIGHT);
		registerCategory("MagnetizedTungsten", CAT_LIGHT);
		registerCategory("StainlessSteel", CAT_LIGHT);

		registerCategory("Tungsten", CAT_HEAVY);
		registerCategory("Zinc", CAT_HEAVY);
		registerCategory("Lead", CAT_HEAVY);
		registerCategory("Gold", CAT_HEAVY);
		registerCategory("Bismuth", CAT_HEAVY);
		registerCategory("Beryllium", CAT_HEAVY);
		registerCategory("Arsenic", CAT_HEAVY);
		registerCategory("Saturnite", CAT_HEAVY);
		registerCategory("Yharonite", CAT_HEAVY);
		registerCategory("Ethyroite", CAT_HEAVY);
		registerCategory("BSCCO", CAT_HEAVY);
		registerCategory("Steel", CAT_HEAVY);
		registerCategory("HighSpeedSteel", CAT_HEAVY);
		registerCategory("GunMetal", CAT_HEAVY);
		registerCategory("WeaponSteel", CAT_HEAVY);
		registerCategory("Ferrouranium", CAT_HEAVY);
		registerCategory("CMBSteel", CAT_HEAVY);

		registerCategory("RareEarth", CAT_RARE);
		registerCategory("RareEarthOreChunk", CAT_RARE);
		registerCategory("Neodymium", CAT_RARE);
		registerCategory("Strontium", CAT_RARE);
		registerCategory("Zirconium", CAT_RARE);
		registerCategory("Boron", CAT_RARE);
		registerCategory("Bromine", CAT_RARE);
		registerCategory("Tantalum", CAT_RARE);
		registerCategory("Tantalium", CAT_RARE);
		registerCategory("Lanthanum", CAT_RARE);
		registerCategory("Lanthanium", CAT_RARE);
		registerCategory("Niobium", CAT_RARE);
		registerCategory("Iodine", CAT_RARE);

		registerCategory("Uranium", CAT_ACTINIDE);
		registerCategory("Uraninite", CAT_ACTINIDE);
		registerCategory("Thorium", CAT_ACTINIDE);
		registerCategory("Thorium232", CAT_ACTINIDE);
		registerCategory("Th232", CAT_ACTINIDE);
		registerCategory("Radium", CAT_ACTINIDE);
		registerCategory("Radium226", CAT_ACTINIDE);
		registerCategory("Ra226", CAT_ACTINIDE);
		registerCategory("Polonium", CAT_ACTINIDE);
		registerCategory("Polonium210", CAT_ACTINIDE);
		registerCategory("Po210", CAT_ACTINIDE);
		registerCategory("Uranium233", CAT_ACTINIDE);
		registerCategory("U233", CAT_ACTINIDE);
		registerCategory("Uranium235", CAT_ACTINIDE);
		registerCategory("U235", CAT_ACTINIDE);
		registerCategory("Uranium238", CAT_ACTINIDE);
		registerCategory("U238", CAT_ACTINIDE);
		registerCategory("Plutonium", CAT_ACTINIDE);
		registerCategory("Plutonium238", CAT_ACTINIDE);
		registerCategory("Plutonium239", CAT_ACTINIDE);
		registerCategory("Plutonium240", CAT_ACTINIDE);
		registerCategory("Plutonium241", CAT_ACTINIDE);
		registerCategory("Pu238", CAT_ACTINIDE);
		registerCategory("Pu239", CAT_ACTINIDE);
		registerCategory("Pu240", CAT_ACTINIDE);
		registerCategory("Pu241", CAT_ACTINIDE);
		registerCategory("Neptunium", CAT_ACTINIDE);
		registerCategory("Neptunium237", CAT_ACTINIDE);
		registerCategory("Np237", CAT_ACTINIDE);
		registerCategory("Technetium", CAT_ACTINIDE);
		registerCategory("Technetium99", CAT_ACTINIDE);
		registerCategory("Tc99", CAT_ACTINIDE);
		registerCategory("Australium", CAT_ACTINIDE);
		registerCategory("Tasmanite", CAT_ACTINIDE);
		registerCategory("Ayerite", CAT_ACTINIDE);
		registerCategory("Gold198", CAT_ACTINIDE);
		registerCategory("Au198", CAT_ACTINIDE);
		registerCategory("Cobalt60", CAT_ACTINIDE);
		registerCategory("Co60", CAT_ACTINIDE);
		registerCategory("Lead209", CAT_ACTINIDE);
		registerCategory("Pb209", CAT_ACTINIDE);
		registerCategory("Strontium90", CAT_ACTINIDE);
		registerCategory("Sr90", CAT_ACTINIDE);
		registerCategory("Actinium227", CAT_ACTINIDE);
		registerCategory("Ac227", CAT_ACTINIDE);
		registerCategory("FerricSchrabidate", CAT_ACTINIDE);

		registerCategory("Coal", CAT_NONMETAL);
		registerCategory("Lignite", CAT_NONMETAL);
		registerCategory("Sulfur", CAT_NONMETAL);
		registerCategory("Saltpeter", CAT_NONMETAL);
		registerCategory("Niter", CAT_NONMETAL);
		registerCategory("Fluorite", CAT_NONMETAL);
		registerCategory("Silicon", CAT_NONMETAL);
		registerCategory("Phosphorus", CAT_NONMETAL);
		registerCategory("RedPhosphorus", CAT_NONMETAL);
		registerCategory("WhitePhosphorus", CAT_NONMETAL);
		registerCategory("Glowstone", CAT_NONMETAL);
		registerCategory("Polymer", CAT_NONMETAL);
		registerCategory("Rubber", CAT_NONMETAL);
		registerCategory("Semtex", CAT_NONMETAL);
		registerCategory("PVC", CAT_NONMETAL);
		registerCategory("Graphite", CAT_NONMETAL);
		registerCategory("Bakelite", CAT_NONMETAL);
		registerCategory("Latex", CAT_NONMETAL);
		registerCategory("Polycarbonate", CAT_NONMETAL);

		registerCategory("Schrabidium", CAT_SCHRABIDIC);
		registerCategory("Solinium", CAT_SCHRABIDIC);
		registerCategory("Ghiorsium336", CAT_SCHRABIDIC);
		registerCategory("Gh336", CAT_SCHRABIDIC);
		registerCategory("Ghiorisium", CAT_SCHRABIDIC);
		registerCategory("Euphemium", CAT_SCHRABIDIC);

		registerCategory("Rift", CAT_LIVING);
		registerCategory("Abyss", CAT_LIVING);

		registerCategory("Redstone", CAT_CRYSTAL);
		registerCategory("Asbestos", CAT_CRYSTAL);
		registerCategory("Diamond", CAT_CRYSTAL);
		registerCategory("Emerald", CAT_CRYSTAL);
		registerCategory("Quartz", CAT_CRYSTAL);
		registerCategory("Sodalite", CAT_CRYSTAL);
		registerCategory("Cinnabar", CAT_CRYSTAL);
		registerCategory("Molysite", CAT_CRYSTAL);
		registerCategory("Borax", CAT_CRYSTAL);
		registerCategory("CrystallineFullerite", CAT_CRYSTAL);
	}

	public static class CoreEntry {
		public String oreDict;
		public float value;

		public CoreEntry() { }

		public CoreEntry(String oreDict, float value) {
			this.oreDict = oreDict;
			this.value = value;
			validate();
		}

		private void validate() {
			if(Float.isNaN(value) || Float.isInfinite(value) || value < 0.0F) {
				throw new InvalidParameterException("Core entry value must be finite and >= 0, got: " + value);
			}
		}

		public float getValue() {
			return value;
		}

		public void setValue(float value) {
			this.value = value;
			validate();
		}
	}

	public static class MaterialMass {
		public String category;
		public String oreDict;
		public double densityKgPerM3;
		public double volumeShare;
		public double massShare;
		public double volumeM3;
		public double massKg;
	}

	public static class CoreCategory {
		public String name;
		public float weight = 1.0F;
		// Packet sync can update cores while gameplay code reads them in integrated worlds.
		public List<CoreEntry> entries = new CopyOnWriteArrayList<CoreEntry>();

		public CoreCategory() { }

		public CoreCategory(String name, CoreEntry... entries) {
			this.name = normalizeCategoryKey(name);
			for(CoreEntry entry : entries) {
				this.entries.add(entry);
			}
			validateTotal();
		}

		public float getTotalValue() {
			float total = 0.0F;
			for(CoreEntry entry : entries) {
				total += entry.value;
			}
			return total;
		}

		public CoreCategory and(CoreEntry entry) {
			entries.add(entry);
			validateTotal();
			return this;
		}

		public CoreCategory withWeight(float weight) {
			if(weight <= 0.0F) {
				throw new InvalidParameterException("Core category weight must be > 0, got: " + weight);
			}
			this.weight = weight;
			return this;
		}

		public CoreCategory setName(String categoryKey) {
			this.name = normalizeCategoryKey(categoryKey);
			return this;
		}

		private void validateTotal() {
			if(name == null || !VALID_CATEGORY_KEYS.contains(name)) {
				throw new InvalidParameterException("Unsupported core category key '" + name + "'");
			}
		}
	}

	// Copy-on-write avoids ConcurrentModificationException when core sync and tick logic overlap.
	public List<CoreCategory> categories = new CopyOnWriteArrayList<CoreCategory>();
	public ArrayList<MaterialMass> materialMasses = new ArrayList<MaterialMass>();
	private static final double TOTAL_MASS_MIN_KG = 1.0E18D;
	private static final double TOTAL_MASS_MAX_KG = 1.0E24D;
	private static final double PLANET_MASS_SHARE_MIN = 0.65D;
	private static final double PLANET_MASS_SHARE_MAX = 0.75D;
	private static final double PLANET_MASS_SHARE_RADIUS_MIN_KM = 60.0D;
	private static final double PLANET_MASS_SHARE_RADIUS_MAX_KM = 700.0D;
	// Calibrated so Kerbin/Earth-like light+heavy composition yields full magnetic shielding (1.0).
	private static final double MAGNETIC_FIELD_EARTH_REFERENCE_COUPLING = 2.788131528E3D;
	public double computedRadiusKm = -1.0D;
	public double computedCoreMassKg = 0.0D;
	public double computedPlanetMassKg = 0.0D;
	public double computedTotalMassKg = 0.0D;
	public double computedBulkDensityKgPerM3 = 0.0D;
	public double computedCoreRadioactivity = Double.NaN;
	public double computedMagneticFieldStrength = Double.NaN;
	public double densityScale = 1.0D;
	public double rotationalSpeedScale = 1.0D;
	private boolean bypassMaxMassLimit = false;

	public CelestialCore() { }

	public CelestialCore(CoreCategory... categories) {
		for(CoreCategory category : categories) {
			this.categories.add(category);
		}
	}

	public CelestialCore(CoreEntry... entries) {
		for(CoreEntry entry : entries) {
			if(entry != null) {
				addAutoCategorizedEntry(entry);
			}
		}
	}

	public CelestialCore addCategory(CoreCategory category) {
		categories.add(category);
		invalidateComputed();
		return this;
	}

	public CelestialCore withDensityScale(double scale) {
		if(scale <= 0.0D) {
			throw new InvalidParameterException("Core density scale must be > 0, got: " + scale);
		}
		this.densityScale = scale;
		invalidateComputed();
		return this;
	}

	public CelestialCore withRotationalSpeedScale(double scale) {
		if(Double.isNaN(scale) || Double.isInfinite(scale) || scale <= 0.0D) {
			throw new InvalidParameterException("Core rotational speed scale must be finite and > 0, got: " + scale);
		}
		this.rotationalSpeedScale = scale;
		return this;
	}

	public CelestialCore withMaxMassCapBypass(boolean bypass) {
		if(this.bypassMaxMassLimit == bypass) {
			return this;
		}
		this.bypassMaxMassLimit = bypass;
		invalidateComputed();
		return this;
	}

	public CelestialCore copy() {
		CelestialCore copy = new CelestialCore();
		copy.densityScale = densityScale;
		copy.rotationalSpeedScale = rotationalSpeedScale;
		copy.bypassMaxMassLimit = bypassMaxMassLimit;
		for(CoreCategory category : categories) {
			CoreCategory categoryCopy = new CoreCategory();
			categoryCopy.name = category.name;
			categoryCopy.weight = category.weight;
			for(CoreEntry entry : category.entries) {
				categoryCopy.entries.add(new CoreEntry(entry.oreDict, entry.value));
			}
			categoryCopy.validateTotal();
			copy.categories.add(categoryCopy);
		}
		if(computedRadiusKm > 0.0D) {
			copy.recalculateForRadius(computedRadiusKm);
		}
		return copy;
	}

	public CoreCategory getCategory(String categoryName) {
		if(categoryName == null) return null;
		String key = normalizeCategoryKey(categoryName);
		for(CoreCategory category : categories) {
			if(key.equals(category.name)) {
				return category;
			}
		}
		return null;
	}

	public CoreEntry getEntry(String categoryName, String entryId) {
		CoreCategory category = getCategory(categoryName);
		if(category == null || entryId == null) return null;

		for(CoreEntry entry : category.entries) {
			if(entryId.equals(entry.oreDict)) {
				return entry;
			}
		}
		return null;
	}

	public CelestialCore addOrUpdateEntryValue(String categoryName, String oreDict, float value) {
		CoreCategory category = getCategory(categoryName);
		if(category == null) {
			category = new CoreCategory();
			category.name = normalizeCategoryKey(categoryName);
			categories.add(category);
		}

		CoreEntry existing = getEntry(categoryName, oreDict);
		if(existing != null) {
			float oldValue = existing.value;
			existing.value = value;
			existing.validate();
			try {
				category.validateTotal();
			} catch(Exception ex) {
				existing.value = oldValue;
				throw ex;
			}
		} else {
			category.entries.add(new CoreEntry(oreDict, value));
			category.validateTotal();
		}

		invalidateComputed();
		return this;
	}

	public CelestialCore setEntryValue(String categoryName, String entryId, float value) {
		CoreEntry entry = getEntry(categoryName, entryId);
		if(entry == null) {
			throw new InvalidParameterException("Core entry not found for category '" + categoryName + "' and id '" + entryId + "'");
		}

		CoreCategory category = getCategory(categoryName);
		float oldValue = entry.value;
		entry.value = value;
		entry.validate();
		try {
			category.validateTotal();
		} catch(Exception ex) {
			entry.value = oldValue;
			throw ex;
		}

		invalidateComputed();
		return this;
	}

	public boolean removeEntry(String categoryName, String entryId) {
		CoreCategory category = getCategory(categoryName);
		if(category == null) return false;

		for(int i = 0; i < category.entries.size(); i++) {
			CoreEntry entry = category.entries.get(i);
			if(entryId.equals(entry.oreDict)) {
				category.entries.remove(i);
				invalidateComputed();
				return true;
			}
		}

		return false;
	}

	private void addAutoCategorizedEntry(CoreEntry entry) {
		String categoryKey = getCategoryForOreDict(entry.oreDict);
		if(categoryKey == null) {
			throw new InvalidParameterException("No core category configured for ore dictionary key: '" + entry.oreDict + "' (token: '" + getMaterialToken(entry.oreDict) + "')");
		}

		CoreCategory category = getCategory(categoryKey);
		if(category == null) {
			category = new CoreCategory();
			category.name = categoryKey;
			categories.add(category);
		}

		category.entries.add(entry);
		category.validateTotal();
	}

	private static void registerDensity(String token, double densityKgPerM3) {
		DENSITY_BY_TOKEN_KG_M3.put(normalizeMaterialToken(token), densityKgPerM3);
	}

	private static void registerRadioactivity(String token, float radiation) {
		BASE_RAD_BY_TOKEN.put(normalizeMaterialToken(token), radiation);
	}

	private static void registerCategory(String token, String categoryKey) {
		if(token == null || token.isEmpty()) return;
		String normalizedToken = normalizeMaterialToken(token);
		String normalizedCategory = normalizeCategoryKey(categoryKey);
		if(normalizedToken == null || normalizedToken.isEmpty()) return;
		if(normalizedCategory == null || !VALID_CATEGORY_KEYS.contains(normalizedCategory)) {
			throw new InvalidParameterException("Unsupported core category key '" + categoryKey + "'");
		}
		if(!CATEGORY_BY_TOKEN.containsKey(normalizedToken)) {
			CATEGORY_BY_TOKEN.put(normalizedToken, normalizedCategory);
		}
	}

	private static String normalizeCategoryKey(String categoryName) {
		if(categoryName == null) return null;
		String key = categoryName.trim().toLowerCase(Locale.US)
			.replace("-", "")
			.replace("_", "")
			.replace(" ", "");

		if("light".equals(key)) return CAT_LIGHT;
		if("lightmetal".equals(key)) return CAT_LIGHT;
		if("heavy".equals(key)) return CAT_HEAVY;
		if("heavymetal".equals(key)) return CAT_HEAVY;
		if("rare".equals(key)) return CAT_RARE;
		if("rareearth".equals(key)) return CAT_RARE;
		if("crystal".equals(key)) return CAT_CRYSTAL;
		if("crystalline".equals(key)) return CAT_CRYSTAL;
		if("nonmetal".equals(key)) return CAT_NONMETAL;
		if("schrabidic".equals(key)) return CAT_SCHRABIDIC;
		if("living".equals(key)) return CAT_LIVING;
		if("plastic".equals(key)) return CAT_NONMETAL;
		if("actinide".equals(key)) return CAT_ACTINIDE;
		if("hazard".equals(key)) return CAT_ACTINIDE;
		if("hazardous".equals(key)) return CAT_ACTINIDE;
		if("hazardouswaste".equals(key)) return CAT_ACTINIDE;

		return key;
	}

	private static String getMaterialToken(String oreDict) {
		if(oreDict == null || oreDict.isEmpty()) return oreDict;
		for(int i = 0; i < oreDict.length(); i++) {
			if(Character.isUpperCase(oreDict.charAt(i))) {
				return oreDict.substring(i);
			}
		}
		return oreDict;
	}

	private static String normalizeMaterialToken(String token) {
		if(token == null) return null;
		return token.trim().toLowerCase(Locale.US)
			.replace("-", "")
			.replace("_", "")
			.replace(" ", "");
	}

	public static String getCategoryForOreDict(String oreDict) {
		String token = getMaterialToken(oreDict);
		if(token == null || token.isEmpty()) return null;
		return CATEGORY_BY_TOKEN.get(normalizeMaterialToken(token));
	}

	public static double getDensityForOreDict(String oreDict) {
		String token = getMaterialToken(oreDict);
		Double density = DENSITY_BY_TOKEN_KG_M3.get(normalizeMaterialToken(token));
		if(density == null) {
			throw new InvalidParameterException("No core density configured for ore dictionary key: '" + oreDict + "' (token: '" + token + "')");
		}
		return density;
	}

	public static float getRadioactivityForOreDict(String oreDict) {
		if(oreDict == null || oreDict.isEmpty()) return 0.0F;

		Float cached = RAD_BY_OREDICT_CACHE.get(oreDict);
		if(cached != null) {
			return cached.floatValue();
		}

		float radiation = resolveRadioactivityFromHazards(oreDict);
		if(radiation <= 0.0F) {
			radiation = resolveRadioactivityFallback(oreDict);
		}

		RAD_BY_OREDICT_CACHE.put(oreDict, radiation);
		return radiation;
	}

	private static float resolveRadioactivityFromHazards(String oreDict) {
		float maxRadiation = 0.0F;
		try {
			List<ItemStack> stacks = OreDictionary.getOres(oreDict);
			if(stacks != null) {
				for(ItemStack registered : stacks) {
					if(registered == null || registered.getItem() == null) continue;
					ItemStack stack = registered.copy();
					stack.stackSize = 1;
					float stackRadiation = HazardSystem.getHazardLevelFromStack(stack, HazardRegistry.RADIATION);
					if(stackRadiation > maxRadiation) {
						maxRadiation = stackRadiation;
					}
				}
			}
		} catch(Exception ignored) { }

		return maxRadiation;
	}

	private static float resolveRadioactivityFallback(String oreDict) {
		String token = getMaterialToken(oreDict);
		if(token == null || token.isEmpty()) return 0.0F;

		Float base = BASE_RAD_BY_TOKEN.get(normalizeMaterialToken(token));
		if(base == null || base.floatValue() <= 0.0F) return 0.0F;

		String prefix = oreDict.substring(0, Math.max(0, oreDict.length() - token.length()));
		return base.floatValue() * getRadiationShapeMultiplier(prefix);
	}

	private static float getRadiationShapeMultiplier(String prefix) {
		if(prefix == null || prefix.isEmpty()) return HazardRegistry.ingot;

		String normalized = prefix.trim().toLowerCase(Locale.US);
		if("ingot".equals(normalized)) return HazardRegistry.ingot;
		if("nugget".equals(normalized) || "tiny".equals(normalized)) return HazardRegistry.nugget;
		if("dusttiny".equals(normalized)) return HazardRegistry.powder_tiny;
		if("dust".equals(normalized)) return HazardRegistry.powder;
		if("gem".equals(normalized) || "crystal".equals(normalized)) return HazardRegistry.gem;
		if("plate".equals(normalized)) return HazardRegistry.plate;
		if("platetriple".equals(normalized)) return HazardRegistry.plateCast;
		if("platesextuple".equals(normalized)) return HazardRegistry.plate * 6.0F;
		if("billet".equals(normalized)) return HazardRegistry.billet;
		if("block".equals(normalized)) return HazardRegistry.block;
		if("ore".equals(normalized) || "orenether".equals(normalized)) return HazardRegistry.ore;
		return HazardRegistry.ingot;
	}

	private double getTotalWeightedValue() {
		double total = 0.0D;
		for(CoreCategory category : categories) {
			for(CoreEntry entry : category.entries) {
				total += (double) category.weight * (double) entry.value;
			}
		}
		return total;
	}

	public double getAverageDensityKgPerM3() {
		double totalWeightedValue = getTotalWeightedValue();
		if(totalWeightedValue <= 0.0D) {
			return 0.0D;
		}
		double weightedDensity = 0.0D;

		for(CoreCategory category : categories) {
			for(CoreEntry entry : category.entries) {
				double weightedValue = (double) category.weight * (double) entry.value;
				double volumeShare = weightedValue / totalWeightedValue;
				weightedDensity += getDensityForOreDict(entry.oreDict) * volumeShare;
			}
		}

		return weightedDensity;
	}

	public double calculateMassKg(double radiusKm) {
		// `radiusKm` is the full celestial body radius, not a separate inner-core radius.
		double totalMassKg = calculateRawTotalMassKg(radiusKm);
		return clampTotalMassKg(totalMassKg);
	}

	public double calculateRawTotalMassKg(double radiusKm) {
		// `radiusKm` is the full celestial body radius, not a separate inner-core radius.
		double radiusM = radiusKm * 1_000D;
		double volumeM3 = (4D / 3D) * Math.PI * radiusM * radiusM * radiusM;
		double coreMassKg = getAverageDensityKgPerM3() * densityScale * volumeM3;
		double planetMassShare = getPlanetMassShareForRadius(radiusKm);
		return coreMassKg / (1.0D - planetMassShare);
	}

	public double getMinTotalMassKg() {
		return TOTAL_MASS_MIN_KG;
	}

	public double getMaxTotalMassKg() {
		return bypassMaxMassLimit ? Double.POSITIVE_INFINITY : TOTAL_MASS_MAX_KG;
	}

	public double getAverageRadioactivity() {
		if(!Double.isNaN(computedCoreRadioactivity)) {
			return computedCoreRadioactivity;
		}

		double totalWeightedValue = getTotalWeightedValue();
		if(totalWeightedValue <= 0.0D) {
			computedCoreRadioactivity = 0.0D;
			return computedCoreRadioactivity;
		}
		double weightedRadioactivity = 0.0D;

		for(CoreCategory category : categories) {
			for(CoreEntry entry : category.entries) {
				double weightedValue = (double) category.weight * (double) entry.value;
				double volumeShare = weightedValue / totalWeightedValue;
				weightedRadioactivity += (double) getRadioactivityForOreDict(entry.oreDict) * volumeShare;
			}
		}

		computedCoreRadioactivity = weightedRadioactivity;
		return computedCoreRadioactivity;
	}

	// Returns a normalized shielding strength in [0, 1] from conductive core composition.
	// Only light/heavy categories contribute, per design.
	public double getMagneticFieldStrength() {
		if(!Double.isNaN(computedMagneticFieldStrength)) {
			return computedMagneticFieldStrength;
		}

		double totalWeightedValue = getTotalWeightedValue();
		if(totalWeightedValue <= 0.0D) {
			computedMagneticFieldStrength = 0.0D;
			return computedMagneticFieldStrength;
		}
		double lightCoupling = getCategoryConductiveCoupling(CAT_LIGHT, totalWeightedValue);
		double heavyCoupling = getCategoryConductiveCoupling(CAT_HEAVY, totalWeightedValue);
		if(lightCoupling <= 0.0D || heavyCoupling <= 0.0D) {
			computedMagneticFieldStrength = 0.0D;
			return computedMagneticFieldStrength;
		}

		double coupling = Math.sqrt(lightCoupling * heavyCoupling);
		computedMagneticFieldStrength = clamp01(coupling / MAGNETIC_FIELD_EARTH_REFERENCE_COUPLING);
		return computedMagneticFieldStrength;
	}

	private double getCategoryConductiveCoupling(String categoryKey, double totalWeightedValue) {
		CoreCategory category = getCategory(categoryKey);
		if(category == null) return 0.0D;

		double coupling = 0.0D;
		for(CoreEntry entry : category.entries) {
			double weightedShare = ((double) category.weight * (double) entry.value) / totalWeightedValue;
			coupling += weightedShare * getDensityForOreDict(entry.oreDict);
		}
		return coupling;
	}

	private static double clamp01(double value) {
		return Math.max(0.0D, Math.min(1.0D, value));
	}

	public CelestialCore recalculateForRadius(double radiusKm) {
		// `radiusKm` is the full planetary radius used by gravity/orbit calcs.
		materialMasses = calculateMaterialMasses(radiusKm);
		computedRadiusKm = radiusKm;
		double rawCoreMassKg = 0.0D;
		for(MaterialMass materialMass : materialMasses) {
			rawCoreMassKg += materialMass.massKg;
		}

		double planetMassShare = getPlanetMassShareForRadius(radiusKm);
		double rawTotalMassKg = rawCoreMassKg / (1.0D - planetMassShare);
		computedTotalMassKg = clampTotalMassKg(rawTotalMassKg);
		double massScale = rawTotalMassKg > 0.0D ? computedTotalMassKg / rawTotalMassKg : 1.0D;
		if(Math.abs(massScale - 1.0D) > 1.0E-12D) {
			for(MaterialMass materialMass : materialMasses) {
				materialMass.massKg *= massScale;
				materialMass.densityKgPerM3 *= massScale;
			}
		}
		computedCoreMassKg = rawCoreMassKg * massScale;
		computedPlanetMassKg = computedTotalMassKg - computedCoreMassKg;
		computedCoreRadioactivity = getAverageRadioactivity();
		computedMagneticFieldStrength = getMagneticFieldStrength();

		double radiusM = radiusKm * 1_000D;
		double volumeM3 = (4D / 3D) * Math.PI * radiusM * radiusM * radiusM;
		computedBulkDensityKgPerM3 = volumeM3 > 0.0D ? computedTotalMassKg / volumeM3 : 0.0D;
		return this;
	}

	private double clampTotalMassKg(double totalMassKg) {
		if(Double.isNaN(totalMassKg) || Double.isInfinite(totalMassKg)) {
			return TOTAL_MASS_MIN_KG;
		}
		double clamped = Math.max(TOTAL_MASS_MIN_KG, totalMassKg);
		if(!bypassMaxMassLimit) {
			clamped = Math.min(TOTAL_MASS_MAX_KG, clamped);
		}
		return clamped;
	}

	private void invalidateComputed() {
		materialMasses = new ArrayList<MaterialMass>();
		computedRadiusKm = -1.0D;
		computedCoreMassKg = 0.0D;
		computedPlanetMassKg = 0.0D;
		computedTotalMassKg = 0.0D;
		computedBulkDensityKgPerM3 = 0.0D;
		computedCoreRadioactivity = Double.NaN;
		computedMagneticFieldStrength = Double.NaN;
	}

	public static double getPlanetMassShareForRadius(double radiusKm) {
		double t = (radiusKm - PLANET_MASS_SHARE_RADIUS_MIN_KM) / (PLANET_MASS_SHARE_RADIUS_MAX_KM - PLANET_MASS_SHARE_RADIUS_MIN_KM);
		t = Math.max(0.0D, Math.min(1.0D, t));
		return PLANET_MASS_SHARE_MIN + (PLANET_MASS_SHARE_MAX - PLANET_MASS_SHARE_MIN) * t;
	}

	public ArrayList<MaterialMass> calculateMaterialMasses(double radiusKm) {
		// `radiusKm` is the full celestial body radius.
		double totalWeightedValue = getTotalWeightedValue();
		double radiusM = radiusKm * 1_000D;
		double totalVolumeM3 = (4D / 3D) * Math.PI * radiusM * radiusM * radiusM;

		ArrayList<MaterialMass> masses = new ArrayList<MaterialMass>();
		if(totalWeightedValue <= 0.0D) {
			return masses;
		}
		double totalMassKg = 0.0D;

		for(CoreCategory category : categories) {
			for(CoreEntry entry : category.entries) {
				double weightedValue = (double) category.weight * (double) entry.value;
				double volumeShare = weightedValue / totalWeightedValue;
				double volumeM3 = totalVolumeM3 * volumeShare;
				double densityKgPerM3 = getDensityForOreDict(entry.oreDict) * densityScale;
				double massKg = densityKgPerM3 * volumeM3;

				MaterialMass materialMass = new MaterialMass();
				materialMass.category = category.name;
				materialMass.oreDict = entry.oreDict;
				materialMass.densityKgPerM3 = densityKgPerM3;
				materialMass.volumeShare = volumeShare;
				materialMass.volumeM3 = volumeM3;
				materialMass.massKg = massKg;

				totalMassKg += massKg;
				masses.add(materialMass);
			}
		}

		if(totalMassKg > 0.0D) {
			for(MaterialMass materialMass : masses) {
				materialMass.massShare = materialMass.massKg / totalMassKg;
			}
		}

		return masses;
	}

	public static CoreEntry value(String oreDict, float value) {
		return new CoreEntry(oreDict, value);
	}

	public static CoreEntry weight(String oreDict, float relativeWeight) {
		return new CoreEntry(oreDict, relativeWeight);
	}

	public static CoreCategory cat(String name, CoreEntry... entries) {
		return new CoreCategory(name, entries);
	}
	public void writeToNBT(NBTTagCompound nbt) {
		NBTTagList categoryList = new NBTTagList();

		for(CoreCategory category : categories) {
			NBTTagCompound categoryTag = new NBTTagCompound();
			categoryTag.setString("name", category.name);
			categoryTag.setFloat("weight", category.weight);

			NBTTagList entryList = new NBTTagList();
			for(CoreEntry entry : category.entries) {
				NBTTagCompound entryTag = new NBTTagCompound();
				entryTag.setString("oreDict", entry.oreDict);
				entryTag.setFloat("value", entry.value);
				entryList.appendTag(entryTag);
			}

			categoryTag.setTag("entries", entryList);
			categoryList.appendTag(categoryTag);
		}

		nbt.setTag("coreCategories", categoryList);
		nbt.setDouble("densityScale", densityScale);
		nbt.setDouble("rotationalSpeedScale", rotationalSpeedScale);
		nbt.setBoolean("bypassMaxMassLimit", bypassMaxMassLimit);
	}
	public void readFromNBT(NBTTagCompound nbt) {
		NBTTagList categoryList = nbt.getTagList("coreCategories", Constants.NBT.TAG_COMPOUND);
		double parsedDensityScale = nbt.hasKey("densityScale") ? nbt.getDouble("densityScale") : 1.0D;
		double parsedRotationalSpeedScale = nbt.hasKey("rotationalSpeedScale") ? nbt.getDouble("rotationalSpeedScale") : 1.0D;
		boolean parsedBypassMaxMassLimit = nbt.hasKey("bypassMaxMassLimit") && nbt.getBoolean("bypassMaxMassLimit");
		if(parsedDensityScale <= 0.0D) {
			parsedDensityScale = 1.0D;
		}
		if(Double.isNaN(parsedRotationalSpeedScale) || Double.isInfinite(parsedRotationalSpeedScale) || parsedRotationalSpeedScale <= 0.0D) {
			parsedRotationalSpeedScale = 1.0D;
		}
		List<CoreCategory> parsedCategories = new CopyOnWriteArrayList<CoreCategory>();
		for(int i = 0; i < categoryList.tagCount(); i++) {
			NBTTagCompound categoryTag = categoryList.getCompoundTagAt(i);
			CoreCategory category = new CoreCategory();
			category.name = normalizeCategoryKey(categoryTag.getString("name"));
			category.weight = categoryTag.hasKey("weight") ? categoryTag.getFloat("weight") : 1.0F;
			if(category.weight <= 0.0F) {
				throw new InvalidParameterException("Core category weight must be > 0, got: " + category.weight);
			}

			NBTTagList entryList = categoryTag.getTagList("entries", Constants.NBT.TAG_COMPOUND);
			for(int j = 0; j < entryList.tagCount(); j++) {
				NBTTagCompound entryTag = entryList.getCompoundTagAt(j);
				CoreEntry entry = new CoreEntry();
				entry.oreDict = entryTag.getString("oreDict");
				entry.value = entryTag.hasKey("value") ? entryTag.getFloat("value") : entryTag.getFloat("percentage");
				entry.validate();
				category.entries.add(entry);
			}

			category.validateTotal();
			parsedCategories.add(category);
		}

		densityScale = parsedDensityScale;
		rotationalSpeedScale = parsedRotationalSpeedScale;
		bypassMaxMassLimit = parsedBypassMaxMassLimit;
		categories = parsedCategories;
		invalidateComputed();
	}
	public void writeToBytes(ByteBuf buf) {
		buf.writeInt(categories.size());
		buf.writeDouble(densityScale);
		buf.writeDouble(rotationalSpeedScale);
		buf.writeBoolean(bypassMaxMassLimit);

		for(CoreCategory category : categories) {
			BufferUtil.writeString(buf, category.name);
			buf.writeFloat(category.weight);
			buf.writeInt(category.entries.size());

			for(CoreEntry entry : category.entries) {
				BufferUtil.writeString(buf, entry.oreDict);
				buf.writeFloat(entry.value);
			}
		}
	}
	public void readFromBytes(ByteBuf buf) {
		int categoryCount = buf.readInt();
		double parsedDensityScale = buf.readDouble();
		double parsedRotationalSpeedScale = buf.readDouble();
		boolean parsedBypassMaxMassLimit = buf.readBoolean();
		if(parsedDensityScale <= 0.0D) {
			parsedDensityScale = 1.0D;
		}
		if(Double.isNaN(parsedRotationalSpeedScale) || Double.isInfinite(parsedRotationalSpeedScale) || parsedRotationalSpeedScale <= 0.0D) {
			parsedRotationalSpeedScale = 1.0D;
		}
		List<CoreCategory> parsedCategories = new CopyOnWriteArrayList<CoreCategory>();
		for(int i = 0; i < categoryCount; i++) {
			CoreCategory category = new CoreCategory();
			category.name = normalizeCategoryKey(BufferUtil.readString(buf));
			category.weight = buf.readFloat();
			if(category.weight <= 0.0F) {
				throw new InvalidParameterException("Core category weight must be > 0, got: " + category.weight);
			}

			int entryCount = buf.readInt();
			for(int j = 0; j < entryCount; j++) {
				CoreEntry entry = new CoreEntry();
				entry.oreDict = BufferUtil.readString(buf);
				entry.value = buf.readFloat();
				entry.validate();
				category.entries.add(entry);
			}

			category.validateTotal();
			parsedCategories.add(category);
		}

		densityScale = parsedDensityScale;
		rotationalSpeedScale = parsedRotationalSpeedScale;
		bypassMaxMassLimit = parsedBypassMaxMassLimit;
		categories = parsedCategories;
		invalidateComputed();
	}

}
