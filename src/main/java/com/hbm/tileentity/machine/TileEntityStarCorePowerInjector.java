package com.hbm.tileentity.machine;

import com.hbm.dim.CelestialBody;
import com.hbm.dim.trait.CBT_SkyState;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toclient.DfcIgnitionSkyPacket;
import com.hbm.tileentity.TileEntityMachineBase;

import api.hbm.energymk2.IEnergyReceiverMK2;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityStarCorePowerInjector extends TileEntityMachineBase implements IEnergyReceiverMK2 {

	public static final long MAX_POWER = 1_000_000_000_000_000L;

	private long power;
	private long receivedThisTick;
	private long throughputThisSecond;
	private long throughputLastSecond;

	public TileEntityStarCorePowerInjector() {
		super(0);
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

			boolean tickSecond = worldObj.getTotalWorldTime() % 20 == 0;
			if(tickSecond) {
				throughputLastSecond = throughputThisSecond;
				throughputThisSecond = 0;

				CBT_SkyState skyState = CBT_SkyState.get(worldObj);
				if(skyState.getState() == CBT_SkyState.SkyState.DFC) {
					if(throughputLastSecond >= CBT_SkyState.DFC_THRESHOLD_HE_PER_SEC) {
						skyState.setState(CBT_SkyState.SkyState.SUN);
						skyState.setDfcThroughput(0);
						PacketDispatcher.wrapper.sendToDimension(
							new DfcIgnitionSkyPacket(worldObj.getTotalWorldTime(), worldObj.provider.dimensionId),
							worldObj.provider.dimensionId
						);
					} else {
						skyState.setDfcThroughput(throughputLastSecond);
					}
					CelestialBody.getStar(worldObj).modifyTraits(skyState);
				} else {
					if(skyState.getDfcThroughput() != 0) {
						skyState.setDfcThroughput(0);
						CelestialBody.getStar(worldObj).modifyTraits(skyState);
					}
					throughputLastSecond = 0;
					throughputThisSecond = 0;
				}
			}

			power = 0;
			networkPackNT(20);
		}
	}

	@Override
	public long transferPower(long power) {
		if(worldObj == null) return power;
		CBT_SkyState skyState = CBT_SkyState.get(worldObj);
		if(skyState.getState() != CBT_SkyState.SkyState.DFC) {
			return power;
		}
		long overshoot = IEnergyReceiverMK2.super.transferPower(power);
		long accepted = power - overshoot;
		if(accepted > 0) {
			receivedThisTick += accepted;
		}
		return overshoot;
	}

	public long getThroughputPerSecond() {
		return throughputLastSecond;
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
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		throughputLastSecond = Math.max(0, buf.readLong());
	}

	@Override public long getPower() { return power; }
	@Override public void setPower(long power) { this.power = power; }
	@Override public long getMaxPower() { return MAX_POWER; }
}
