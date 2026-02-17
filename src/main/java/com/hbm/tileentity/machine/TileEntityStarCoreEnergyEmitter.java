package com.hbm.tileentity.machine;

import com.hbm.dim.CelestialBody;
import com.hbm.dim.StarcoreThroughputTracker;
import com.hbm.dim.trait.CBT_SkyState;
import com.hbm.dim.WorldProviderCelestial;
import com.hbm.dim.kerbol.WorldProviderKerbol;
import com.hbm.config.SpaceConfig;
import com.hbm.tileentity.TileEntityMachineBase;

import api.hbm.energymk2.IEnergyReceiverMK2;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldProviderEnd;
import net.minecraft.world.WorldProviderHell;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityStarCoreEnergyEmitter extends TileEntityMachineBase implements IEnergyReceiverMK2 {

	public static final long MAX_POWER = 1_000_000_000_000_000L;
	private static final long STEEL_INJECTOR_MAX_HE_PER_TICK = CBT_SkyState.STARCORE_THRESHOLD_HE_PER_TICK;

	private long power;
	private long receivedThisTick;
	private long throughputThisSecond;
	private long throughputLastSecond;
	private long throughputLastTick;
	private long throughputThisFiveTicks;
	private long throughputLastFiveTicks;
	private boolean registered;

	public TileEntityStarCoreEnergyEmitter() {
		super(0);
	}

	@Override
	public String getName() {
		return "container.starCorePowerEmitter";
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
		long supportRequirement = getSupportRequirementPerTick(state, skyState);
		long globalPerTickCap = getPerTickCap(state, skyState, supportRequirement);
		long perInjectorCap = Math.min(globalPerTickCap, STEEL_INJECTOR_MAX_HE_PER_TICK);
		if(state == CBT_SkyState.SkyState.SUN && isSunChargeNearFull(skyState)) {
			int injectors = Math.max(1, StarcoreThroughputTracker.getRegisteredInjectorCount(worldObj));
			perInjectorCap = Math.min(perInjectorCap, Math.max(1L, globalPerTickCap / injectors));
		}
		long allowance = Math.min(
			Math.min(supportRequirement - receivedThisTick, perInjectorCap),
			StarcoreThroughputTracker.getRemainingCapacity(worldObj, globalPerTickCap)
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

	private boolean canOperate() {
		if(worldObj == null) return false;
		if(worldObj.provider != null && worldObj.provider.dimensionId == SpaceConfig.orbitDimension) {
			return false;
		}
		if(worldObj.provider instanceof WorldProviderHell
			|| worldObj.provider instanceof WorldProviderEnd
			|| worldObj.provider instanceof WorldProviderKerbol) {
			return false;
		}
		CBT_SkyState skyState = CBT_SkyState.get(worldObj);
		if(skyState != null && skyState.getState() == CBT_SkyState.SkyState.STARCORE) {
			return true;
		}
		return isWorldDaytime();
	}

	private boolean isWorldDaytime() {
		if(worldObj == null) return false;
		if(worldObj.provider instanceof WorldProviderCelestial) {
			if(((WorldProviderCelestial) worldObj.provider).isEclipse()) return false;
		}
		float angle = worldObj.getCelestialAngleRadians(0.0F);
		return MathHelper.cos(angle) > 0.0F;
	}

	private static long getSupportRequirementPerTick(CBT_SkyState.SkyState state, CBT_SkyState skyState) {
		if(state != CBT_SkyState.SkyState.SUN) {
			return CBT_SkyState.STARCORE_THRESHOLD_HE_PER_TICK;
		}
		return CBT_SkyState.getSunSupportRequirementPerTick(skyState.getSunCharge());
	}

	private static long getPerTickCap(CBT_SkyState.SkyState state, CBT_SkyState skyState, long supportRequirement) {
		if(state != CBT_SkyState.SkyState.SUN) {
			return supportRequirement;
		}
		long current = skyState.getSunCharge();
		if(current >= CBT_SkyState.SUN_MAX_HE) {
			return supportRequirement;
		}
		long remaining = CBT_SkyState.SUN_MAX_HE - current;
		if(remaining > Long.MAX_VALUE - supportRequirement) {
			return Long.MAX_VALUE;
		}
		return supportRequirement + remaining;
	}

	private static boolean isSunChargeNearFull(CBT_SkyState skyState) {
		long current = skyState.getSunCharge();
		long ninetyNinePct = (CBT_SkyState.SUN_MAX_HE / 100L) * 99L;
		return current >= ninetyNinePct;
	}

	@Override
	public void invalidate() {
		if(!worldObj.isRemote) {
			unregisterInjector();
			resetStarcoreThroughputOnRemoval();
		}
		super.invalidate();
	}

	@Override
	public void onChunkUnload() {
		if(!worldObj.isRemote) {
			unregisterInjector();
			resetStarcoreThroughputOnRemoval();
		}
		super.onChunkUnload();
	}

	@Override
	public void validate() {
		super.validate();
		if(!worldObj.isRemote) {
			registerInjector();
		}
	}

	private void resetStarcoreThroughputOnRemoval() {
		if(worldObj == null) return;
		CBT_SkyState skyState = CBT_SkyState.get(worldObj);
		if(skyState.getStarcoreThroughput() != 0) {
			skyState.setStarcoreThroughput(0);
			CelestialBody.getStar(worldObj).modifyTraits(skyState);
		}
	}

	private void registerInjector() {
		if(worldObj == null || registered) return;
		if(worldObj.provider != null && worldObj.provider.dimensionId == SpaceConfig.orbitDimension) return;
		StarcoreThroughputTracker.registerInjector(worldObj, xCoord, yCoord, zCoord);
		registered = true;
	}

	private void unregisterInjector() {
		if(worldObj == null || !registered) return;
		StarcoreThroughputTracker.unregisterInjector(worldObj, xCoord, yCoord, zCoord);
		registered = false;
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeLong(throughputLastSecond);
		buf.writeLong(throughputLastFiveTicks);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		throughputLastSecond = Math.max(0, buf.readLong());
		throughputLastFiveTicks = Math.max(0, buf.readLong());
	}

	@Override
	public boolean canConnect(ForgeDirection side) {
		return canConnectFrom(this.xCoord, this.yCoord, this.zCoord, side);
	}

	public boolean canConnectFrom(int blockX, int blockY, int blockZ, ForgeDirection side) {
		if(side == ForgeDirection.UNKNOWN) return false;

		int relX = blockX - this.xCoord;
		int relY = blockY - this.yCoord;
		int relZ = blockZ - this.zCoord;

		// Core intake.
		if(relX == 0 && relY == 0 && relZ == 0) return side == ForgeDirection.DOWN;
		if(relY != 0) return false;

		// Shell intake points.
		if(relX == 2 && (relZ == 1 || relZ == -1)) return side == ForgeDirection.EAST;
		if(relZ == -2 && (relX == 1 || relX == -1)) return side == ForgeDirection.NORTH;
		if(relX == -2 && (relZ == -1 || relZ == 1)) return side == ForgeDirection.WEST;
		if(relZ == 2 && (relX == -1 || relX == 1)) return side == ForgeDirection.SOUTH;

		return false;
	}

	@Override public long getPower() { return power; }
	@Override public void setPower(long power) { this.power = power; }
	@Override public long getMaxPower() { return MAX_POWER; }

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return AxisAlignedBB.getBoundingBox(
			xCoord - 3, yCoord, zCoord - 3,
			xCoord + 4, yCoord + 4, zCoord + 4
		);
	}

	@Override
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}
}
