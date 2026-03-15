package com.hbm.tileentity.machine;

import com.hbm.config.SpaceConfig;
import com.hbm.dim.CelestialBody;
import com.hbm.dim.CelestialCore;
import com.hbm.dim.SolarSystemWorldSavedData;
import com.hbm.dim.WorldProviderCelestial;
import com.hbm.dim.trait.CBT_Dyson;
import com.hbm.dim.trait.CBT_SkyState;
import com.hbm.dim.orbit.OrbitalStation;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerCoreManipulator;
import com.hbm.inventory.gui.GUICoreManipulator;
import com.hbm.items.ISatChip;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.main.MainRegistry;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteDysonRelay;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.ParticleUtil;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderEnd;
import net.minecraft.world.WorldProviderHell;

public class TileEntityCoreManipulator extends TileEntityMachineBase implements IGUIProvider, IControlReceiver {

	public static enum CoreChangeMode {
		PUSH("push"),
		PULL("pull");

		private final String id;

		private CoreChangeMode(String id) {
			this.id = id;
		}

		public String getId() {
			return this.id;
		}

		public static CoreChangeMode fromId(String id) {
			if("pull".equalsIgnoreCase(id)) {
				return PULL;
			}
			return PUSH;
		}
	}

	private static final String NBT_KEY_CORE_CHANGE_MODE = "coreChangeMode";
	private static final String NBT_KEY_MACHINE_ENABLED = "machineEnabled";
	private static final String NBT_KEY_SELECTED_MATERIAL = "selectedCoreMaterial";
	private static final int SLOT_BLUEPRINT = 0;
	private static final int SLOT_DYSON_CHIP = 1;
	private static final int CORE_OPERATION_INTERVAL = 20;
	private static final int NETWORK_SYNC_INTERVAL = 20;
	private static final int DYSON_ENVIRONMENT_CHECK_INTERVAL = 20;
	private static final float CORE_CHANGE_PER_REAL_DAY = 0.1F;
	private static final float REAL_DAY_SECONDS = 24.0F * 60.0F * 60.0F;
	private static final float CORE_OPERATION_VALUE_STEP =
		CORE_CHANGE_PER_REAL_DAY * CORE_OPERATION_INTERVAL / (20.0F * REAL_DAY_SECONDS);
	private static final float WORKING_SOUND_VOLUME = 0.375F;
	private static final double WORKING_SOUND_Y_OFFSET = 4.0D;
	private static final double TOP_PARTICLE_ZONE_MIN_OFFSET = -1.5D;
	private static final double TOP_PARTICLE_ZONE_SIZE = 4.0D;
	private static final double TOP_PARTICLE_Y_OFFSET = 8.05D;
	public static final int MIN_DYSON_SWARM_MEMBERS = 4096;
	private static final String NULL_SELECTION = "null";
	private CoreChangeMode coreChangeMode = CoreChangeMode.PUSH;
	private boolean machineEnabled = false;
	private String selectedMaterialOreDict = null;
	private int dysonSwarmId = 0;
	private int dysonSwarmCount = 0;
	private int dysonSwarmConsumers = 0;
	private boolean dysonPowered = false;
	private boolean working = false;
	private boolean selectedMaterialValidityDirty = true;
	private boolean selectedMaterialValid = false;
	private String selectedMaterialCategoryKey = null;
	private String selectedMaterialCategory = null;
	private ItemStack cachedBlueprintSnapshot;
	private long nextDysonEnvironmentCheckTick = 0L;
	private int cachedDysonEnvironmentSwarmId = Integer.MIN_VALUE;
	private boolean cachedDysonEnvironmentPowered = false;
	private AudioWrapper audio;
	private AxisAlignedBB renderBounds;

	public TileEntityCoreManipulator() {
		super(2);
	}

	@Override
	public void updateEntity() {
		if(this.worldObj == null) {
			return;
		}

		if(this.worldObj.isRemote) {
			this.updateClientEffects();
			return;
		}

		this.refreshBlueprintSnapshot();
		long worldTime = this.worldObj.getTotalWorldTime();
		boolean changed = false;
		boolean syncNeeded = false;
		int prevDysonSwarmId = this.dysonSwarmId;
		int prevDysonSwarmCount = this.dysonSwarmCount;
		int prevDysonSwarmConsumers = this.dysonSwarmConsumers;
		boolean prevDysonPowered = this.dysonPowered;
		if(this.selectedMaterialOreDict != null && !this.hasValidSelectedMaterial()) {
			this.selectedMaterialOreDict = null;
			this.invalidateSelectionCaches();
			changed = true;
			syncNeeded = true;
		}

		this.updateDysonPowerState();
		if(prevDysonSwarmId != this.dysonSwarmId
			|| prevDysonSwarmCount != this.dysonSwarmCount
			|| prevDysonSwarmConsumers != this.dysonSwarmConsumers
			|| prevDysonPowered != this.dysonPowered) {
			syncNeeded = true;
		}

		boolean nextWorking = this.computeWorkingState();
		if(this.working != nextWorking) {
			this.working = nextWorking;
			syncNeeded = true;
		}

		if(this.working && worldTime % CORE_OPERATION_INTERVAL == 0) {
			if(this.processCoreChange()) {
				changed = true;
				syncNeeded = true;
			}
		}

		if(changed) {
			this.markChanged();
		}
		if(syncNeeded || worldTime % NETWORK_SYNC_INTERVAL == 0) {
			this.networkPackNT(64);
		}
	}

	@Override
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new ContainerCoreManipulator(player.inventory, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new GUICoreManipulator(player.inventory, this);
	}

	@Override
	public String getName() {
		return "container.coreManipulator";
	}

	public CoreChangeMode getCoreChangeMode() {
		return this.coreChangeMode;
	}

	public String getCoreChangeModeId() {
		return this.coreChangeMode.getId();
	}

	public boolean isCoreChangeModePull() {
		return this.coreChangeMode == CoreChangeMode.PULL;
	}

	public void setCoreChangeMode(CoreChangeMode mode) {
		CoreChangeMode nextMode = mode != null ? mode : CoreChangeMode.PUSH;
		if(this.coreChangeMode == nextMode) {
			return;
		}

		this.coreChangeMode = nextMode;
		if(this.worldObj != null && !this.worldObj.isRemote) {
			this.markChanged();
		}
	}

	public void setCoreChangeModeById(String modeId) {
		this.setCoreChangeMode(CoreChangeMode.fromId(modeId));
	}

	public void toggleCoreChangeMode() {
		this.setCoreChangeMode(this.coreChangeMode == CoreChangeMode.PULL ? CoreChangeMode.PUSH : CoreChangeMode.PULL);
	}

	public boolean isMachineEnabled() {
		return this.machineEnabled;
	}

	public void setMachineEnabled(boolean enabled) {
		if(this.machineEnabled == enabled) {
			return;
		}

		this.machineEnabled = enabled;
		this.invalidateDysonEnvironmentCache();
		if(this.worldObj != null && !this.worldObj.isRemote) {
			this.markChanged();
		}
	}

	public void toggleMachineEnabled() {
		this.setMachineEnabled(!this.machineEnabled);
	}

	public ItemStack getBlueprintStack() {
		return this.slots[SLOT_BLUEPRINT];
	}

	public String getSelectedMaterialOreDict() {
		return this.selectedMaterialOreDict;
	}

	public int getDysonSwarmId() {
		return this.dysonSwarmId;
	}

	public int getDysonSwarmCount() {
		return this.dysonSwarmCount;
	}

	public int getDysonSwarmConsumers() {
		return this.dysonSwarmConsumers;
	}

	public boolean hasDysonPower() {
		return this.dysonPowered;
	}

	public boolean isWorking() {
		return this.working;
	}

	public long getDysonEnergyOutputPerSecond() {
		if(!this.dysonPowered || this.dysonSwarmCount < MIN_DYSON_SWARM_MEMBERS || this.dysonSwarmConsumers <= 0) {
			return 0;
		}
		return TileEntityDysonReceiver.getEnergyOutput(this.dysonSwarmCount) / this.dysonSwarmConsumers * 20;
	}

	public ItemStack getSelectedMaterialDisplayStack() {
		return ItemBlueprints.getPreferredCoreMaterialDisplayStack(this.selectedMaterialOreDict);
	}

	public void setSelectedMaterialOreDict(String oreDict) {
		String normalizedSelection = normalizeSelection(oreDict);
		if(normalizedSelection != null && !this.isSelectionValidForBlueprint(normalizedSelection)) {
			normalizedSelection = null;
		}

		if(this.selectedMaterialOreDict == null ? normalizedSelection == null : this.selectedMaterialOreDict.equals(normalizedSelection)) {
			return;
		}

		this.selectedMaterialOreDict = normalizedSelection;
		this.invalidateSelectionCaches();
		if(this.worldObj != null && !this.worldObj.isRemote) {
			this.markChanged();
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.coreChangeMode = CoreChangeMode.fromId(nbt.getString(NBT_KEY_CORE_CHANGE_MODE));
		this.machineEnabled = nbt.getBoolean(NBT_KEY_MACHINE_ENABLED);
		this.selectedMaterialOreDict = normalizeSelection(nbt.getString(NBT_KEY_SELECTED_MATERIAL));
		this.cachedBlueprintSnapshot = null;
		this.invalidateSelectionCaches();
		this.invalidateDysonEnvironmentCache();
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setString(NBT_KEY_CORE_CHANGE_MODE, this.coreChangeMode.getId());
		nbt.setBoolean(NBT_KEY_MACHINE_ENABLED, this.machineEnabled);
		if(this.selectedMaterialOreDict != null) {
			nbt.setString(NBT_KEY_SELECTED_MATERIAL, this.selectedMaterialOreDict);
		}
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		ByteBufUtils.writeUTF8String(buf, this.coreChangeMode.getId());
		buf.writeBoolean(this.machineEnabled);
		ByteBufUtils.writeUTF8String(buf, this.selectedMaterialOreDict == null ? "" : this.selectedMaterialOreDict);
		buf.writeInt(this.dysonSwarmId);
		buf.writeInt(this.dysonSwarmCount);
		buf.writeInt(this.dysonSwarmConsumers);
		buf.writeBoolean(this.dysonPowered);
		buf.writeBoolean(this.working);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		this.coreChangeMode = CoreChangeMode.fromId(ByteBufUtils.readUTF8String(buf));
		this.machineEnabled = buf.readBoolean();
		this.selectedMaterialOreDict = normalizeSelection(ByteBufUtils.readUTF8String(buf));
		this.dysonSwarmId = buf.readInt();
		this.dysonSwarmCount = buf.readInt();
		this.dysonSwarmConsumers = buf.readInt();
		this.dysonPowered = buf.readBoolean();
		this.working = buf.readBoolean();
		this.cachedBlueprintSnapshot = null;
		this.invalidateSelectionCaches();
		this.invalidateDysonEnvironmentCache();
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		if(slot == SLOT_BLUEPRINT) {
			return ItemBlueprints.isCoreManipulatorBlueprint(stack);
		}
		return slot == SLOT_DYSON_CHIP && stack != null && stack.getItem() == ModItems.sat_chip;
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack itemStack, int side) {
		return slot == SLOT_BLUEPRINT || slot == SLOT_DYSON_CHIP;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		return new int[] { SLOT_BLUEPRINT, SLOT_DYSON_CHIP };
	}

	@Override
	public boolean hasPermission(EntityPlayer player) {
		return this.isUseableByPlayer(player);
	}

	@Override
	public void receiveControl(NBTTagCompound data) {
		if(data != null && data.hasKey(NBT_KEY_CORE_CHANGE_MODE)) {
			this.setCoreChangeModeById(data.getString(NBT_KEY_CORE_CHANGE_MODE));
		}
		if(data != null && data.hasKey(NBT_KEY_MACHINE_ENABLED)) {
			this.setMachineEnabled(data.getBoolean(NBT_KEY_MACHINE_ENABLED));
		}
		if(data != null && data.hasKey("index") && data.hasKey("selection")) {
			int index = data.getInteger("index");
			if(index == 0) {
				this.setSelectedMaterialOreDict(data.getString("selection"));
			}
		}
	}

	private String normalizeSelection(String selection) {
		if(selection == null || selection.isEmpty() || NULL_SELECTION.equalsIgnoreCase(selection)) {
			return null;
		}
		return selection;
	}

	private boolean isSelectionValidForBlueprint(String oreDict) {
		return ItemBlueprints.blueprintContainsCoreMaterial(this.getBlueprintStack(), oreDict);
	}

	private boolean computeWorkingState() {
		if(this.isDisabledDimension()) {
			return false;
		}
		if(!this.machineEnabled || !this.dysonPowered) {
			return false;
		}
		if(!this.hasValidSelectedMaterial()) {
			return false;
		}

		String category = this.getSelectedMaterialCategory();
		if(category == null || category.isEmpty()) {
			return false;
		}

		CelestialBody body = resolveTargetBody();
		if(body == null) {
			return false;
		}

		CelestialCore core = body.getCore();
		if(core == null) {
			return false;
		}

		if(this.coreChangeMode == CoreChangeMode.PULL) {
			return core.getEntry(category, this.selectedMaterialOreDict) != null;
		}

		return true;
	}

	private boolean processCoreChange() {
		if(this.isDisabledDimension()) {
			return false;
		}

		if(this.selectedMaterialOreDict == null) {
			return false;
		}
		if(!this.hasValidSelectedMaterial()) {
			return false;
		}

		String category = this.getSelectedMaterialCategory();
		if(category == null || category.isEmpty()) {
			return false;
		}

		CelestialBody body = resolveTargetBody();
		if(body == null) {
			return false;
		}

		CelestialCore core = body.getCore();
		if(core == null) {
			return false;
		}

		try {
			if(this.coreChangeMode == CoreChangeMode.PULL) {
				CelestialCore.CoreEntry entry = core.getEntry(category, this.selectedMaterialOreDict);
				if(entry == null) {
					return false;
				}

				float nextValue = entry.value - CORE_OPERATION_VALUE_STEP;
				if(nextValue <= CelestialCore.MATERIAL_ENTRY_VALUE_MIN) {
					if(!core.removeEntry(category, this.selectedMaterialOreDict)) {
						return false;
					}
				} else {
					core.setEntryValue(category, this.selectedMaterialOreDict, nextValue);
				}
			} else {
				CelestialCore.CoreEntry entry = core.getEntry(category, this.selectedMaterialOreDict);
				float nextValue = entry != null ? entry.value + CORE_OPERATION_VALUE_STEP : CORE_OPERATION_VALUE_STEP;
				core.addOrUpdateEntryValue(category, this.selectedMaterialOreDict, nextValue);
			}
		} catch(Exception ex) {
			return false;
		}

		applyAndPersistCore(body, core);
		return true;
	}

	private void updateDysonPowerState() {
		this.dysonSwarmId = ISatChip.getFreqS(this.slots[SLOT_DYSON_CHIP]);
		this.dysonSwarmCount = 0;
		this.dysonSwarmConsumers = 0;
		this.dysonPowered = false;
		boolean hasValidMaterialSelection = this.hasValidSelectedMaterial();
		boolean shouldConsumeDysonPower = this.machineEnabled && hasValidMaterialSelection;

		if(this.worldObj == null || this.worldObj.provider == null || this.dysonSwarmId <= 0) {
			return;
		}

		if(this.isDisabledDimension()) {
			return;
		}

		this.dysonSwarmCount = CBT_Dyson.count(this.worldObj, this.dysonSwarmId);
		if(this.dysonSwarmCount < MIN_DYSON_SWARM_MEMBERS || !shouldConsumeDysonPower) {
			return;
		}

		this.dysonSwarmConsumers = CBT_Dyson.consumers(this.worldObj, this.dysonSwarmId);
		if(this.dysonSwarmConsumers <= 0) {
			return;
		}

		this.dysonPowered = this.getDysonEnvironmentPowered();
	}

	private boolean isSkyOccludedForDyson() {
		for(int x = -3; x <= 3; x++) {
			for(int z = -3; z <= 3; z++) {
				if(this.worldObj.getHeightValue(this.xCoord + x, this.zCoord + z) > this.yCoord + 10) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean hasNaturalSunlight(CBT_SkyState skyState) {
		if(this.worldObj == null || this.worldObj.provider == null || skyState == null || skyState.getState() != CBT_SkyState.SkyState.SUN) {
			return false;
		}
		if(this.worldObj.provider instanceof WorldProviderCelestial && ((WorldProviderCelestial) this.worldObj.provider).isEclipse()) {
			return false;
		}
		float angle = this.worldObj.getCelestialAngleRadians(0.0F);
		return MathHelper.cos(angle) > 0.0F;
	}

	private CelestialBody resolveTargetBody() {
		if(this.worldObj == null || this.worldObj.provider == null) {
			return null;
		}

		if(CelestialBody.inOrbit(this.worldObj)) {
			OrbitalStation station = OrbitalStation.getStationFromPosition(this.xCoord, this.zCoord);
			if(station != null) {
				return station.orbiting;
			}
			return null;
		}

		return CelestialBody.getBodyOrNull(this.worldObj.provider.dimensionId);
	}

	private boolean isDisabledDimension() {
		if(this.worldObj == null || this.worldObj.provider == null) {
			return false;
		}

		int dimensionId = this.worldObj.provider.dimensionId;
		return dimensionId == SpaceConfig.dmitriyDimension
			|| dimensionId == SpaceConfig.orbitDimension
			|| dimensionId == -1
			|| dimensionId == 1
			|| this.worldObj.provider instanceof WorldProviderHell
			|| this.worldObj.provider instanceof WorldProviderEnd;
	}

	private void applyAndPersistCore(CelestialBody body, CelestialCore core) {
		if(body == null || core == null || this.worldObj == null || this.worldObj.isRemote) {
			return;
		}

		CelestialBody.applyMassFromCore(body, core);
		SolarSystemWorldSavedData.get(this.worldObj).setCore(body.name, core);
	}

	private void refreshBlueprintSnapshot() {
		ItemStack currentBlueprint = this.getBlueprintStack();
		if(this.areBlueprintStacksEqual(this.cachedBlueprintSnapshot, currentBlueprint)) {
			return;
		}

		this.cachedBlueprintSnapshot = currentBlueprint != null ? currentBlueprint.copy() : null;
		this.invalidateSelectionCaches();
		this.invalidateDysonEnvironmentCache();
	}

	private boolean areBlueprintStacksEqual(ItemStack a, ItemStack b) {
		if(a == b) {
			return true;
		}
		if(a == null || b == null) {
			return false;
		}
		if(a.getItem() != b.getItem()) {
			return false;
		}
		if(a.getItemDamage() != b.getItemDamage()) {
			return false;
		}
		if(a.stackSize != b.stackSize) {
			return false;
		}

		return ItemStack.areItemStackTagsEqual(a, b);
	}

	private void invalidateSelectionCaches() {
		this.selectedMaterialValidityDirty = true;
		this.selectedMaterialValid = false;
		this.selectedMaterialCategoryKey = null;
		this.selectedMaterialCategory = null;
	}

	private boolean hasValidSelectedMaterial() {
		if(this.selectedMaterialOreDict == null) {
			return false;
		}

		if(this.selectedMaterialValidityDirty) {
			this.selectedMaterialValid = this.isSelectionValidForBlueprint(this.selectedMaterialOreDict);
			this.selectedMaterialValidityDirty = false;
		}

		return this.selectedMaterialValid;
	}

	private String getSelectedMaterialCategory() {
		if(this.selectedMaterialOreDict == null) {
			return null;
		}

		if(!this.selectedMaterialOreDict.equals(this.selectedMaterialCategoryKey)) {
			this.selectedMaterialCategoryKey = this.selectedMaterialOreDict;
			this.selectedMaterialCategory = CelestialCore.getCategoryForOreDict(this.selectedMaterialOreDict);
		}

		return this.selectedMaterialCategory;
	}

	private void invalidateDysonEnvironmentCache() {
		this.nextDysonEnvironmentCheckTick = 0L;
		this.cachedDysonEnvironmentSwarmId = Integer.MIN_VALUE;
		this.cachedDysonEnvironmentPowered = false;
	}

	private boolean getDysonEnvironmentPowered() {
		if(this.worldObj == null) {
			return false;
		}

		long time = this.worldObj.getTotalWorldTime();
		if(time >= this.nextDysonEnvironmentCheckTick || this.cachedDysonEnvironmentSwarmId != this.dysonSwarmId) {
			this.cachedDysonEnvironmentSwarmId = this.dysonSwarmId;
			this.cachedDysonEnvironmentPowered = this.computeDysonEnvironmentPowered();
			this.nextDysonEnvironmentCheckTick = time + DYSON_ENVIRONMENT_CHECK_INTERVAL;
		}

		return this.cachedDysonEnvironmentPowered;
	}

	private boolean computeDysonEnvironmentPowered() {
		SatelliteSavedData data = SatelliteSavedData.getData(this.worldObj, this.xCoord, this.zCoord);
		Satellite sat = data != null ? data.getSatFromFreq(this.dysonSwarmId) : null;
		CBT_SkyState skyState = CBT_SkyState.get(this.worldObj);
		boolean hasDaylight = hasNaturalSunlight(skyState);
		boolean occluded = isSkyOccludedForDyson();
		return (sat instanceof SatelliteDysonRelay || hasDaylight) && !occluded;
	}

	@SideOnly(Side.CLIENT)
	private void updateClientEffects() {
		if(this.working) {
			if(this.audio == null) {
				this.audio = MainRegistry.proxy.getLoopedSound(
					"hbm:block.dysonBeam",
					this.xCoord + 0.5F,
					(float)(this.yCoord + WORKING_SOUND_Y_OFFSET),
					this.zCoord + 0.5F,
					WORKING_SOUND_VOLUME,
					20F,
					1.0F,
					20
				);
				this.audio.startSound();
			}

			this.audio.keepAlive();
			this.audio.updatePitch(0.85F);

			if(this.worldObj.rand.nextInt(10) == 0) {
				ParticleUtil.spawnFlare(
					this.worldObj,
					this.xCoord + TOP_PARTICLE_ZONE_MIN_OFFSET + this.worldObj.rand.nextDouble() * TOP_PARTICLE_ZONE_SIZE,
					this.yCoord + TOP_PARTICLE_Y_OFFSET,
					this.zCoord + TOP_PARTICLE_ZONE_MIN_OFFSET + this.worldObj.rand.nextDouble() * TOP_PARTICLE_ZONE_SIZE,
					0,
					0.1D + this.worldObj.rand.nextFloat() * 0.1D,
					0,
					4F + this.worldObj.rand.nextFloat() * 2F
				);
			}
		} else {
			this.stopAudio();
		}
	}

	private void stopAudio() {
		if(this.audio != null) {
			this.audio.stopSound();
			this.audio = null;
		}
	}

	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
		this.stopAudio();
	}

	@Override
	public void invalidate() {
		super.invalidate();
		this.stopAudio();
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		if(this.renderBounds == null) {
			this.renderBounds = AxisAlignedBB.getBoundingBox(
				this.xCoord - 6,
				0,
				this.zCoord - 6,
				this.xCoord + 7,
				this.yCoord + 11,
				this.zCoord + 7
			);
		}
		return this.renderBounds;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}
}
