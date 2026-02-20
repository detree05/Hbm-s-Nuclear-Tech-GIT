package com.hbm.dim.trait;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.hbm.util.BufferUtil;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

public class CBT_Core extends CelestialBodyTrait {

	private static final HashMap<String, Double> DENSITY_BY_TOKEN_KG_M3 = new HashMap<String, Double>();
	private static final Set<String> VALID_CATEGORY_KEYS = new HashSet<String>(Arrays.asList(
		"light", "heavy", "rare", "actinide", "nonmetal", "crystal", "schrabidic"
	));

	public static final String CAT_LIGHT = "light";
	public static final String CAT_HEAVY = "heavy";
	public static final String CAT_RARE = "rare";
	public static final String CAT_ACTINIDE = "actinide";
	public static final String CAT_NONMETAL = "nonmetal";
	public static final String CAT_CRYSTAL = "crystal";
	public static final String CAT_SCHRABIDIC = "schrabidic";

	static {
		registerDensity("Iron", 7_870D);
		registerDensity("Copper", 8_960D);
		registerDensity("Aluminum", 2_700D);
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
		registerDensity("Niobium", 8_570D);
		registerDensity("Uranium", 19_100D);
		registerDensity("Uraninite", 19_100D);
		registerDensity("Technetium99", 11_500D);
		registerDensity("Uranium238", 19_050D);
		registerDensity("Borax", 1_730D);
		registerDensity("Iodine", 4_930D);
		registerDensity("Neptunium237", 20_450D);
		registerDensity("Lead209", 11_340D);
		registerDensity("Gold198", 19_320D);
		registerDensity("Schrabidium", 25_000D);
		registerDensity("Solinium", 23_000D);
		registerDensity("Ghiorsium336", 27_000D);
		registerDensity("Bauxite", 2_500D);
		registerDensity("Plutonium", 19_800D);
		registerDensity("Uranium235", 19_050D);
		registerDensity("Hafnium", 13_310D);
		registerDensity("Polymer", 950D);
		registerDensity("Rubber", 1_100D);
		registerDensity("Semtex", 1_500D);
		registerDensity("PVC", 1_380D);
	}

	public static class CoreEntry {
		public String oreDict;
		public float percentage;

		public CoreEntry() { }

		public CoreEntry(String oreDict, float percentage) {
			this.oreDict = oreDict;
			this.percentage = percentage;
			validate();
		}

		private void validate() {
			if(percentage < 0.0F || percentage > 100.0F) {
				throw new InvalidParameterException("Core entry percentage must be within [0, 100], got: " + percentage);
			}
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
		public ArrayList<CoreEntry> entries = new ArrayList<CoreEntry>();

		public CoreCategory() { }

		public CoreCategory(String name, CoreEntry... entries) {
			this.name = normalizeCategoryKey(name);
			for(CoreEntry entry : entries) {
				this.entries.add(entry);
			}
			validateTotal();
		}

		public float getTotalPercentage() {
			float total = 0.0F;
			for(CoreEntry entry : entries) {
				total += entry.percentage;
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
			float total = getTotalPercentage();
			if(total > 100.0F) {
				throw new InvalidParameterException("Core category '" + name + "' exceeds 100% (" + total + "%)");
			}
		}
	}

	public ArrayList<CoreCategory> categories = new ArrayList<CoreCategory>();
	public ArrayList<MaterialMass> materialMasses = new ArrayList<MaterialMass>();
	public double computedRadiusKm = -1.0D;
	public double computedMassKg = 0.0D;
	public double computedBulkDensityKgPerM3 = 0.0D;

	public CBT_Core() { }

	public CBT_Core(CoreCategory... categories) {
		for(CoreCategory category : categories) {
			this.categories.add(category);
		}
	}

	public CBT_Core addCategory(CoreCategory category) {
		categories.add(category);
		invalidateComputed();
		return this;
	}

	public CBT_Core copy() {
		CBT_Core copy = new CBT_Core();
		for(CoreCategory category : categories) {
			CoreCategory categoryCopy = new CoreCategory();
			categoryCopy.name = category.name;
			categoryCopy.weight = category.weight;
			for(CoreEntry entry : category.entries) {
				categoryCopy.entries.add(new CoreEntry(entry.oreDict, entry.percentage));
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

	public CBT_Core addOrUpdateEntry(String categoryName, String oreDict, float percentage) {
		CoreCategory category = getCategory(categoryName);
		if(category == null) {
			category = new CoreCategory();
			category.name = normalizeCategoryKey(categoryName);
			categories.add(category);
		}

		CoreEntry existing = getEntry(categoryName, oreDict);
		if(existing != null) {
			float oldPercentage = existing.percentage;
			existing.percentage = percentage;
			existing.validate();
			try {
				category.validateTotal();
			} catch(Exception ex) {
				existing.percentage = oldPercentage;
				throw ex;
			}
		} else {
			category.entries.add(new CoreEntry(oreDict, percentage));
			category.validateTotal();
		}

		invalidateComputed();
		return this;
	}

	@Deprecated
	public CBT_Core addOrUpdateEntry(String categoryName, String ignoredDisplayName, String oreDict, float percentage) {
		return addOrUpdateEntry(categoryName, oreDict, percentage);
	}

	public CBT_Core setEntryPercentage(String categoryName, String entryId, float percentage) {
		CoreEntry entry = getEntry(categoryName, entryId);
		if(entry == null) {
			throw new InvalidParameterException("Core entry not found for category '" + categoryName + "' and id '" + entryId + "'");
		}

		CoreCategory category = getCategory(categoryName);
		float oldPercentage = entry.percentage;
		entry.percentage = percentage;
		entry.validate();
		try {
			category.validateTotal();
		} catch(Exception ex) {
			entry.percentage = oldPercentage;
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

	private static void registerDensity(String token, double densityKgPerM3) {
		DENSITY_BY_TOKEN_KG_M3.put(token, densityKgPerM3);
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

	public static double getDensityForOreDict(String oreDict) {
		String token = getMaterialToken(oreDict);
		Double density = DENSITY_BY_TOKEN_KG_M3.get(token);
		if(density == null) {
			throw new InvalidParameterException("No core density configured for ore dictionary key: '" + oreDict + "' (token: '" + token + "')");
		}
		return density;
	}

	private double getTotalWeightedPercentage() {
		double total = 0.0D;
		for(CoreCategory category : categories) {
			for(CoreEntry entry : category.entries) {
				total += (double) category.weight * (double) entry.percentage;
			}
		}
		if(total <= 0.0D) {
			throw new InvalidParameterException("CBT_Core has no weighted composition entries");
		}
		return total;
	}

	public double getAverageDensityKgPerM3() {
		double totalWeightedPercentage = getTotalWeightedPercentage();
		double weightedDensity = 0.0D;

		for(CoreCategory category : categories) {
			for(CoreEntry entry : category.entries) {
				double weightedPercentage = (double) category.weight * (double) entry.percentage;
				double volumeShare = weightedPercentage / totalWeightedPercentage;
				weightedDensity += getDensityForOreDict(entry.oreDict) * volumeShare;
			}
		}

		return weightedDensity;
	}

	public double calculateMassKg(double radiusKm) {
		double radiusM = radiusKm * 1_000D;
		double volumeM3 = (4D / 3D) * Math.PI * radiusM * radiusM * radiusM;
		return getAverageDensityKgPerM3() * volumeM3;
	}

	public CBT_Core recalculateForRadius(double radiusKm) {
		materialMasses = calculateMaterialMasses(radiusKm);
		computedRadiusKm = radiusKm;
		computedMassKg = 0.0D;
		for(MaterialMass materialMass : materialMasses) {
			computedMassKg += materialMass.massKg;
		}
		computedBulkDensityKgPerM3 = getAverageDensityKgPerM3();
		return this;
	}

	private void invalidateComputed() {
		materialMasses = new ArrayList<MaterialMass>();
		computedRadiusKm = -1.0D;
		computedMassKg = 0.0D;
		computedBulkDensityKgPerM3 = 0.0D;
	}

	public ArrayList<MaterialMass> calculateMaterialMasses(double radiusKm) {
		double totalWeightedPercentage = getTotalWeightedPercentage();
		double radiusM = radiusKm * 1_000D;
		double totalVolumeM3 = (4D / 3D) * Math.PI * radiusM * radiusM * radiusM;

		ArrayList<MaterialMass> masses = new ArrayList<MaterialMass>();
		double totalMassKg = 0.0D;

		for(CoreCategory category : categories) {
			for(CoreEntry entry : category.entries) {
				double weightedPercentage = (double) category.weight * (double) entry.percentage;
				double volumeShare = weightedPercentage / totalWeightedPercentage;
				double volumeM3 = totalVolumeM3 * volumeShare;
				double densityKgPerM3 = getDensityForOreDict(entry.oreDict);
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

	public static CoreEntry comp(String oreDict, float percentage) {
		return new CoreEntry(oreDict, percentage);
	}

	@Deprecated
	public static CoreEntry comp(String ignoredDisplayName, String oreDict, float percentage) {
		return new CoreEntry(oreDict, percentage);
	}

	public static CoreCategory cat(String name, CoreEntry... entries) {
		return new CoreCategory(name, entries);
	}

	@Override
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
				entryTag.setFloat("percentage", entry.percentage);
				entryList.appendTag(entryTag);
			}

			categoryTag.setTag("entries", entryList);
			categoryList.appendTag(categoryTag);
		}

		nbt.setTag("coreCategories", categoryList);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		invalidateComputed();
		categories = new ArrayList<CoreCategory>();

		NBTTagList categoryList = nbt.getTagList("coreCategories", Constants.NBT.TAG_COMPOUND);
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
				entry.percentage = entryTag.getFloat("percentage");
				entry.validate();
				category.entries.add(entry);
			}

			category.validateTotal();
			categories.add(category);
		}
	}

	@Override
	public void writeToBytes(ByteBuf buf) {
		buf.writeInt(categories.size());

		for(CoreCategory category : categories) {
			BufferUtil.writeString(buf, category.name);
			buf.writeFloat(category.weight);
			buf.writeInt(category.entries.size());

			for(CoreEntry entry : category.entries) {
				BufferUtil.writeString(buf, entry.oreDict);
				buf.writeFloat(entry.percentage);
			}
		}
	}

	@Override
	public void readFromBytes(ByteBuf buf) {
		invalidateComputed();
		categories = new ArrayList<CoreCategory>();

		int categoryCount = buf.readInt();
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
				entry.percentage = buf.readFloat();
				entry.validate();
				category.entries.add(entry);
			}

			category.validateTotal();
			categories.add(category);
		}
	}

}
