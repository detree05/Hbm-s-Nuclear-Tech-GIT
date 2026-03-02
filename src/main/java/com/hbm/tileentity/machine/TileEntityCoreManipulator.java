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
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteDysonRelay;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

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
	private static final float CORE_OPERATION_VALUE_STEP = 0.001F;
	public static final int MIN_DYSON_SWARM_MEMBERS = 4096;
	private static final String NULL_SELECTION = "null";
	private CoreChangeMode coreChangeMode = CoreChangeMode.PUSH;
	private boolean machineEnabled = false;
	private String selectedMaterialOreDict = null;
	private int dysonSwarmId = 0;
	private int dysonSwarmCount = 0;
	private int dysonSwarmConsumers = 0;
	private boolean dysonPowered = false;

	public TileEntityCoreManipulator() {
		super(2);
	}

	@Override
	public void updateEntity() {
		if(this.worldObj == null || this.worldObj.isRemote) {
			return;
		}

		boolean changed = false;
		if(this.selectedMaterialOreDict != null && !this.isSelectionValidForBlueprint(this.selectedMaterialOreDict)) {
			this.selectedMaterialOreDict = null;
			changed = true;
		}

		this.updateDysonPowerState();

		if(this.machineEnabled && this.dysonPowered && this.worldObj.getTotalWorldTime() % CORE_OPERATION_INTERVAL == 0) {
			if(this.processCoreChange()) {
				changed = true;
			}
		}

		if(changed) {
			this.markChanged();
		}
		this.networkPackNT(64);
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

	private boolean processCoreChange() {
		if(this.worldObj != null && this.worldObj.provider != null && this.worldObj.provider.dimensionId == SpaceConfig.dmitriyDimension) {
			return false;
		}

		if(this.selectedMaterialOreDict == null) {
			return false;
		}
		if(!this.isSelectionValidForBlueprint(this.selectedMaterialOreDict)) {
			return false;
		}

		String category = CelestialCore.getCategoryForOreDict(this.selectedMaterialOreDict);
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
		boolean hasValidMaterialSelection = this.selectedMaterialOreDict != null && this.isSelectionValidForBlueprint(this.selectedMaterialOreDict);
		boolean shouldConsumeDysonPower = this.machineEnabled && hasValidMaterialSelection;

		if(this.worldObj == null || this.worldObj.provider == null || this.dysonSwarmId <= 0) {
			return;
		}

		if(this.worldObj.provider.dimensionId == SpaceConfig.dmitriyDimension) {
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

		SatelliteSavedData data = SatelliteSavedData.getData(this.worldObj, this.xCoord, this.zCoord);
		Satellite sat = data != null ? data.getSatFromFreq(this.dysonSwarmId) : null;
		CBT_SkyState skyState = CBT_SkyState.get(this.worldObj);
		boolean hasDaylight = hasNaturalSunlight(skyState);
		boolean occluded = isSkyOccludedForDyson();

		this.dysonPowered = (sat instanceof SatelliteDysonRelay || hasDaylight) && !occluded;
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

	private void applyAndPersistCore(CelestialBody body, CelestialCore core) {
		if(body == null || core == null || this.worldObj == null || this.worldObj.isRemote) {
			return;
		}

		CelestialBody.applyMassFromCore(body, core);
		SolarSystemWorldSavedData.get(this.worldObj).setCore(body.name, core);
	}
}
