package com.hbm.tileentity.machine;

import com.hbm.dim.CelestialBody;
import com.hbm.dim.DfcThroughputTracker;
import com.hbm.dim.trait.CBT_SkyState;
import com.hbm.items.ISatChip;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteDfcRelay;
import com.hbm.tileentity.TileEntityMachineBase;

import api.hbm.energymk2.IEnergyReceiverMK2;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityStarCoreEnergyInjector extends TileEntityMachineBase implements IEnergyReceiverMK2 {

	public static final long MAX_POWER = 1_000_000_000_000_000L;

	private long power;
	private long receivedThisTick;
	private long throughputThisSecond;
	private long throughputLastSecond;
	private int chipFreq;

	public TileEntityStarCoreEnergyInjector() {
		super(1);
	}

	@Override
	public String getName() {
		return "container.starCorePowerInjector";
	}

	@Override
	public void updateEntity() {
		if(!worldObj.isRemote) {
			for(ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
				trySubscribe(worldObj, xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ, dir);
			}

			throughputThisSecond += receivedThisTick;
			receivedThisTick = 0;
			chipFreq = ISatChip.getFreqS(slots[0]);

			boolean tickSecond = worldObj.getTotalWorldTime() % 20 == 0;
			if(tickSecond) {
				throughputLastSecond = throughputThisSecond;
				throughputThisSecond = 0;
				DfcThroughputTracker.add(worldObj, throughputLastSecond);
			}

			power = 0;
			networkPackNT(20);
		}
	}

	@Override
	public long transferPower(long power) {
		if(worldObj == null) return power;
		CBT_SkyState skyState = CBT_SkyState.get(worldObj);
		CBT_SkyState.SkyState state = skyState.getState();
		if(state != CBT_SkyState.SkyState.DFC && state != CBT_SkyState.SkyState.SUN) {
			return power;
		}
		if(!canOperate()) {
			return power;
		}
		long allowance = CBT_SkyState.DFC_THRESHOLD_HE_PER_SEC - (throughputThisSecond + receivedThisTick);
		if(allowance <= 0) {
			return power;
		}

		long capped = Math.min(power, allowance);
		long overshoot = IEnergyReceiverMK2.super.transferPower(capped);
		long accepted = capped - overshoot;
		if(accepted > 0) {
			receivedThisTick += accepted;
		}
		return (power - capped) + overshoot;
	}

	public long getThroughputPerSecond() {
		return throughputLastSecond;
	}

	public int getChipFreq() {
		return chipFreq;
	}

	private boolean canOperate() {
		if(worldObj == null) return false;
		int freq = chipFreq > 0 ? chipFreq : ISatChip.getFreqS(slots[0]);
		if(freq <= 0) return false;
		if(isWorldDaytime()) return true;
		SatelliteSavedData data = SatelliteSavedData.getData(worldObj, xCoord, zCoord);
		if(data == null) return false;
		Satellite sat = data.getSatFromFreq(freq);
		return sat instanceof SatelliteDfcRelay;
	}

	private boolean isWorldDaytime() {
		if(worldObj == null) return false;
		long time = worldObj.getWorldTime() % 24000L;
		return time < 12000L;
	}

	@Override
	public void invalidate() {
		if(!worldObj.isRemote) {
			resetDfcThroughputOnRemoval();
		}
		super.invalidate();
	}

	@Override
	public void onChunkUnload() {
		if(!worldObj.isRemote) {
			resetDfcThroughputOnRemoval();
		}
		super.onChunkUnload();
	}

	private void resetDfcThroughputOnRemoval() {
		if(worldObj == null) return;
		CBT_SkyState skyState = CBT_SkyState.get(worldObj);
		if(skyState.getDfcThroughput() != 0) {
			skyState.setDfcThroughput(0);
			CelestialBody.getStar(worldObj).modifyTraits(skyState);
		}
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeLong(throughputLastSecond);
		buf.writeInt(chipFreq);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		throughputLastSecond = Math.max(0, buf.readLong());
		chipFreq = buf.readInt();
	}

	@Override public long getPower() { return power; }
	@Override public void setPower(long power) { this.power = power; }
	@Override public long getMaxPower() { return MAX_POWER; }
}
