package com.hbm.tileentity.machine;

import com.hbm.dim.CelestialBody;
import com.hbm.dim.StarcoreThroughputTracker;
import com.hbm.dim.trait.CBT_SkyState;
import com.hbm.dim.kerbol.WorldProviderKerbol;
import com.hbm.items.ISatChip;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.StarcoreRelayUtil;
import com.hbm.tileentity.TileEntityMachineBase;

import api.hbm.energymk2.IEnergyReceiverMK2;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.WorldProviderEnd;
import net.minecraft.world.WorldProviderHell;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityStarCoreEnergyInjector extends TileEntityMachineBase implements IEnergyReceiverMK2 {

	public static final long MAX_POWER = 1_000_000_000_000_000L;

	private long power;
	private long receivedThisTick;
	private long throughputThisSecond;
	private long throughputLastSecond;
	private long throughputLastTick;
	private long throughputThisFiveTicks;
	private long throughputLastFiveTicks;
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

			long sentThisTick = receivedThisTick;
			throughputThisSecond += receivedThisTick;
			receivedThisTick = 0;
			throughputLastTick = sentThisTick;
			if(sentThisTick > 0) {
				StarcoreThroughputTracker.registerInjectorTick(worldObj, xCoord, yCoord, zCoord);
			}
			throughputThisFiveTicks += sentThisTick;
			chipFreq = ISatChip.getFreqS(slots[0]);

			boolean tickSecond = worldObj.getTotalWorldTime() % 20 == 0;
			if(tickSecond) {
				throughputLastSecond = throughputThisSecond;
				throughputThisSecond = 0;
			}
			if(worldObj.getTotalWorldTime() % 5 == 0) {
				throughputLastFiveTicks = throughputThisFiveTicks;
				throughputThisFiveTicks = 0;
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
		if(state != CBT_SkyState.SkyState.STARCORE && state != CBT_SkyState.SkyState.SUN) {
			return power;
		}
		if(!canOperate()) {
			return power;
		}
		long perTickCap = getPerTickCap(state, skyState);
		if(state == CBT_SkyState.SkyState.SUN && isSunChargeNearFull(skyState)) {
			int injectors = Math.max(1, StarcoreThroughputTracker.getLastSecondInjectorCount(worldObj));
			perTickCap = Math.max(1L, perTickCap / injectors);
		}
		long allowance = Math.min(
			CBT_SkyState.STARCORE_THRESHOLD_HE_PER_TICK - receivedThisTick,
			StarcoreThroughputTracker.getRemainingCapacity(worldObj, perTickCap)
		);
		if(allowance <= 0) {
			return power;
		}

		long capped = Math.min(power, allowance);
		long overshoot = IEnergyReceiverMK2.super.transferPower(capped);
		long accepted = capped - overshoot;
		if(accepted > 0) {
			receivedThisTick += accepted;
			StarcoreThroughputTracker.add(worldObj, accepted);
		}
		return (power - capped) + overshoot;
	}

	public long getThroughputPerSecond() {
		return throughputLastSecond;
	}

	public long getThroughputPerTick() {
		return throughputLastTick;
	}

	public long getThroughputPerFiveTicks() {
		return throughputLastFiveTicks;
	}

	public int getChipFreq() {
		return chipFreq;
	}

	private boolean canOperate() {
		if(worldObj == null) return false;
		if(worldObj.provider instanceof WorldProviderHell
			|| worldObj.provider instanceof WorldProviderEnd
			|| worldObj.provider instanceof WorldProviderKerbol) {
			return false;
		}
		int freq = chipFreq > 0 ? chipFreq : ISatChip.getFreqS(slots[0]);
		if(freq <= 0) return false;
		if(isWorldDaytime()) return true;
		SatelliteSavedData data = SatelliteSavedData.getData(worldObj, xCoord, zCoord);
		if(data == null) return false;
		Satellite sat = data.getSatFromFreq(freq);
		return StarcoreRelayUtil.isStarcoreRelay(sat);
	}

	private boolean isWorldDaytime() {
		if(worldObj == null) return false;
		long time = worldObj.getWorldTime() % 24000L;
		return time < 12000L;
	}

	private static long getPerTickCap(CBT_SkyState.SkyState state, CBT_SkyState skyState) {
		long threshold = CBT_SkyState.STARCORE_THRESHOLD_HE_PER_TICK;
		if(state != CBT_SkyState.SkyState.SUN) {
			return threshold;
		}
		long current = skyState.getSunCharge();
		if(current >= CBT_SkyState.SUN_MAX_HE) {
			return threshold;
		}
		long remaining = CBT_SkyState.SUN_MAX_HE - current;
		if(remaining > Long.MAX_VALUE - threshold) {
			return Long.MAX_VALUE;
		}
		return threshold + remaining;
	}

	private static boolean isSunChargeNearFull(CBT_SkyState skyState) {
		long current = skyState.getSunCharge();
		long ninetyNinePct = (CBT_SkyState.SUN_MAX_HE / 100L) * 99L;
		return current >= ninetyNinePct;
	}

	@Override
	public void invalidate() {
		if(!worldObj.isRemote) {
			resetStarcoreThroughputOnRemoval();
		}
		super.invalidate();
	}

	@Override
	public void onChunkUnload() {
		if(!worldObj.isRemote) {
			resetStarcoreThroughputOnRemoval();
		}
		super.onChunkUnload();
	}

	private void resetStarcoreThroughputOnRemoval() {
		if(worldObj == null) return;
		CBT_SkyState skyState = CBT_SkyState.get(worldObj);
		if(skyState.getStarcoreThroughput() != 0) {
			skyState.setStarcoreThroughput(0);
			CelestialBody.getStar(worldObj).modifyTraits(skyState);
		}
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeLong(throughputLastSecond);
		buf.writeLong(throughputLastFiveTicks);
		buf.writeInt(chipFreq);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		throughputLastSecond = Math.max(0, buf.readLong());
		throughputLastFiveTicks = Math.max(0, buf.readLong());
		chipFreq = buf.readInt();
	}

	@Override public long getPower() { return power; }
	@Override public void setPower(long power) { this.power = power; }
	@Override public long getMaxPower() { return MAX_POWER; }
}
