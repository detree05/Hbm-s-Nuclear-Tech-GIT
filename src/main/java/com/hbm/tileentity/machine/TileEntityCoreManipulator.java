package com.hbm.tileentity.machine;

import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerCoreManipulator;
import com.hbm.inventory.gui.GUICoreManipulator;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
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
	private CoreChangeMode coreChangeMode = CoreChangeMode.PUSH;
	private boolean machineEnabled = false;

	public TileEntityCoreManipulator() {
		super(0);
	}

	@Override
	public void updateEntity() {
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

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.coreChangeMode = CoreChangeMode.fromId(nbt.getString(NBT_KEY_CORE_CHANGE_MODE));
		this.machineEnabled = nbt.getBoolean(NBT_KEY_MACHINE_ENABLED);
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setString(NBT_KEY_CORE_CHANGE_MODE, this.coreChangeMode.getId());
		nbt.setBoolean(NBT_KEY_MACHINE_ENABLED, this.machineEnabled);
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
	}
}
