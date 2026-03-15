package com.hbm.tileentity.machine;

import com.hbm.dim.CelestialBody;
import com.hbm.dim.StarcoreThroughputTracker;
import com.hbm.dim.trait.CBT_SkyState;
import com.hbm.dim.WorldProviderCelestial;
import com.hbm.dim.dmitriy.WorldProviderDmitriy;
import com.hbm.blocks.BlockDummyable;
import com.hbm.config.SpaceConfig;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.fauxpointtwelve.DirPos;

import api.hbm.energymk2.IEnergyReceiverMK2;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldProviderEnd;
import net.minecraft.world.WorldProviderHell;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityStarCoreEnergyEmitter extends TileEntityMachineBase implements IEnergyReceiverMK2 {

	public static final long MAX_POWER = 1_000_000_000_000_000L;
	private static final long STEEL_INJECTOR_MAX_HE_PER_TICK = CBT_SkyState.STARCORE_THRESHOLD_HE_PER_TICK;
	private static final float GUN_PITCH_MAX_UP = 90.0F;
	private static final float GUN_PITCH_MAX_DOWN = -35.0F;
	private static final float TABLE_YAW_SPEED_DEG_PER_TICK = 2.0F;
	private static final float GUN_PITCH_SPEED_DEG_PER_TICK = 2.0F;
	private static final float AIM_LOCK_TOLERANCE_DEG = 0.75F;
	private static final int RENDER_RADIUS = 144;
	private static final int NETWORK_SYNC_RANGE = 512;
	private static final double LASER_OBSTRUCTION_CHECK_START_OFFSET = 4.0D;
	private static final double LASER_OBSTRUCTION_CHECK_MAX_DISTANCE = 512.0D;

	private long power;
	private long receivedThisTick;
	private long throughputThisSecond;
	private long throughputLastSecond;
	private long throughputLastTick;
	private long throughputThisFiveTicks;
	private long throughputLastFiveTicks;
	private boolean registered;
	private AudioWrapper audio;
	private float clientTableYaw;
	private float clientGunPitch;
	private boolean clientAimInitialized;
	private boolean clientAimLocked;
	private long obstructionCacheTick = Long.MIN_VALUE;
	private boolean obstructionCached;

	public TileEntityStarCoreEnergyEmitter() {
		super(0);
	}

	@Override
	public String getName() {
		return "container.starCorePowerEmitter";
	}

	@Override
	public void updateEntity() {
		updateClientAimLock();

		if(!worldObj.isRemote) {
			for(DirPos pos : getPowerConPos()) {
				trySubscribe(worldObj, pos.getX(), pos.getY(), pos.getZ(), pos.getDir());
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
			networkPackNT(NETWORK_SYNC_RANGE);
		} else {
			if(getThroughputPerFiveTicks() > 0L && canOperate() && clientAimLocked) {
				if(audio == null) {
					audio = MainRegistry.proxy.getLoopedSound("hbm:block.dysonBeam", xCoord + 0.5F, yCoord + 1.0F, zCoord + 0.5F, 0.375F, 20F, 1.0F, 20);
					audio.startSound();
				}

				audio.keepAlive();
				audio.updatePitch(0.85F);
			} else if(audio != null) {
				audio.stopSound();
				audio = null;
			}
		}
	}

	private DirPos[] getPowerConPos() {
		return new DirPos[] {
			// Core bottom intake.
			new DirPos(xCoord, yCoord - 1, zCoord, Library.NEG_Y),
			// Shell intake points: subscribe to conductors OUTSIDE the side-locked shell ports.
			new DirPos(xCoord + 3, yCoord, zCoord + 1, Library.POS_X),
			new DirPos(xCoord + 3, yCoord, zCoord - 1, Library.POS_X),
			new DirPos(xCoord + 1, yCoord, zCoord - 3, Library.NEG_Z),
			new DirPos(xCoord - 1, yCoord, zCoord - 3, Library.NEG_Z),
			new DirPos(xCoord - 3, yCoord, zCoord - 1, Library.NEG_X),
			new DirPos(xCoord - 3, yCoord, zCoord + 1, Library.NEG_X),
			new DirPos(xCoord - 1, yCoord, zCoord + 3, Library.POS_Z),
			new DirPos(xCoord + 1, yCoord, zCoord + 3, Library.POS_Z)
		};
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
		updateClientAimLock();
		if(!clientAimLocked) {
			return power;
		}
		if(isLaserObstructed()) {
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

	public boolean isClientAimLocked() {
		return clientAimLocked;
	}

	public boolean isLaserObstructed() {
		if(worldObj == null) return false;
		long worldTick = worldObj.getTotalWorldTime();
		if(obstructionCacheTick != worldTick) {
			obstructionCached = computeLaserObstructed();
			obstructionCacheTick = worldTick;
		}
		return obstructionCached;
	}

	private boolean computeLaserObstructed() {
		if(worldObj == null || !canOperate()) return false;

		CBT_SkyState.SkyState state = CBT_SkyState.get(worldObj).getState();
		if(state != CBT_SkyState.SkyState.STARCORE && state != CBT_SkyState.SkyState.SUN) {
			return false;
		}

		Vec3 sunDir = getSunDirectionWorld();
		if(sunDir == null || sunDir.lengthVector() <= 1.0E-6D) return false;
		sunDir = sunDir.normalize();

		double baseX = xCoord + 0.5D;
		double baseY = yCoord + 1.5D;
		double baseZ = zCoord + 0.5D;
		Vec3 start = Vec3.createVectorHelper(
			baseX + sunDir.xCoord * LASER_OBSTRUCTION_CHECK_START_OFFSET,
			baseY + sunDir.yCoord * LASER_OBSTRUCTION_CHECK_START_OFFSET,
			baseZ + sunDir.zCoord * LASER_OBSTRUCTION_CHECK_START_OFFSET
		);
		Vec3 end = Vec3.createVectorHelper(
			baseX + sunDir.xCoord * LASER_OBSTRUCTION_CHECK_MAX_DISTANCE,
			baseY + sunDir.yCoord * LASER_OBSTRUCTION_CHECK_MAX_DISTANCE,
			baseZ + sunDir.zCoord * LASER_OBSTRUCTION_CHECK_MAX_DISTANCE
		);

		MovingObjectPosition hit = worldObj.func_147447_a(start, end, false, true, false);
		return hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;
	}

	private boolean canOperate() {
		if(worldObj == null) return false;
		if(worldObj.provider != null && worldObj.provider.dimensionId == SpaceConfig.orbitDimension) {
			return false;
		}
		if(worldObj.provider instanceof WorldProviderHell
			|| worldObj.provider instanceof WorldProviderEnd
			|| worldObj.provider instanceof WorldProviderDmitriy) {
			return false;
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

	private static class AimAngles {
		final float tableYaw;
		final float gunPitch;

		AimAngles(float tableYaw, float gunPitch) {
			this.tableYaw = tableYaw;
			this.gunPitch = gunPitch;
		}
	}

	private void updateClientAimLock() {
		AimAngles target = getClientTargetAimAngles();

		if(!clientAimInitialized) {
			clientTableYaw = target.tableYaw;
			clientGunPitch = target.gunPitch;
			clientAimInitialized = true;
		} else {
			float yawDiff = MathHelper.wrapAngleTo180_float(target.tableYaw - clientTableYaw);
			yawDiff = MathHelper.clamp_float(yawDiff, -TABLE_YAW_SPEED_DEG_PER_TICK, TABLE_YAW_SPEED_DEG_PER_TICK);
			clientTableYaw = MathHelper.wrapAngleTo180_float(clientTableYaw + yawDiff);

			float pitchDiff = target.gunPitch - clientGunPitch;
			pitchDiff = MathHelper.clamp_float(pitchDiff, -GUN_PITCH_SPEED_DEG_PER_TICK, GUN_PITCH_SPEED_DEG_PER_TICK);
			clientGunPitch = MathHelper.clamp_float(clientGunPitch + pitchDiff, GUN_PITCH_MAX_DOWN, GUN_PITCH_MAX_UP);
		}

		float yawErr = Math.abs(MathHelper.wrapAngleTo180_float(target.tableYaw - clientTableYaw));
		float pitchErr = Math.abs(target.gunPitch - clientGunPitch);
		clientAimLocked = yawErr <= AIM_LOCK_TOLERANCE_DEG && pitchErr <= AIM_LOCK_TOLERANCE_DEG;
	}

	private AimAngles getClientTargetAimAngles() {
		if(worldObj == null) {
			return new AimAngles(0.0F, GUN_PITCH_MAX_UP);
		}
		if(worldObj.provider != null && worldObj.provider.dimensionId == SpaceConfig.orbitDimension) {
			return new AimAngles(0.0F, GUN_PITCH_MAX_UP);
		}

		CBT_SkyState skyState = CBT_SkyState.get(worldObj);
		CBT_SkyState.SkyState state = skyState.getState();
		if((state != CBT_SkyState.SkyState.STARCORE && state != CBT_SkyState.SkyState.SUN) || !isWorldDaytime()) {
			return new AimAngles(0.0F, GUN_PITCH_MAX_UP);
		}

		Vec3 sunDirWorld = getSunDirectionWorld();
		if(sunDirWorld == null || sunDirWorld.lengthVector() <= 1.0E-6D) {
			return new AimAngles(0.0F, GUN_PITCH_MAX_UP);
		}

		Vec3 sunDirLocal = toEmitterLocal(sunDirWorld.normalize(), normalizeMeta(getBlockMetadata()));
		if(sunDirLocal.lengthVector() <= 1.0E-6D) {
			return new AimAngles(0.0F, GUN_PITCH_MAX_UP);
		}

		sunDirLocal = sunDirLocal.normalize();
		float tableYaw = (float) Math.toDegrees(Math.atan2(-sunDirLocal.zCoord, sunDirLocal.xCoord));
		float gunPitch = (float) Math.toDegrees(Math.asin(MathHelper.clamp_double(sunDirLocal.yCoord, -1.0D, 1.0D)));
		gunPitch = MathHelper.clamp_float(gunPitch, GUN_PITCH_MAX_DOWN, GUN_PITCH_MAX_UP);

		return new AimAngles(MathHelper.wrapAngleTo180_float(tableYaw), gunPitch);
	}

	private Vec3 getSunDirectionWorld() {
		float solarAngle = worldObj.getCelestialAngle(0.0F);
		float axialTilt = 0.0F;

		if(worldObj.provider instanceof WorldProviderCelestial) {
			axialTilt = CelestialBody.getBody(worldObj).axialTilt;
		}

		Vec3 dir = Vec3.createVectorHelper(0.0D, 1.0D, 0.0D);
		dir = rotateX(dir, solarAngle * 360.0F);
		dir = rotateY(dir, -90.0F);
		if(axialTilt != 0.0F) {
			dir = rotateX(dir, axialTilt);
		}
		return dir.normalize();
	}

	private static Vec3 toEmitterLocal(Vec3 worldDir, int meta) {
		Vec3 local = rotateY(worldDir, -90.0F);

		switch(meta) {
		case 0:
			return rotateX(local, -90.0F);
		case 1:
			return rotateX(local, 90.0F);
		case 2:
			return rotateY(local, -90.0F);
		case 4:
			return rotateY(local, -180.0F);
		case 3:
			return rotateY(local, -270.0F);
		case 5:
		default:
			return local;
		}
	}

	private static Vec3 rotateX(Vec3 vec, float degrees) {
		double rad = Math.toRadians(degrees);
		double cos = Math.cos(rad);
		double sin = Math.sin(rad);
		double y = vec.yCoord * cos - vec.zCoord * sin;
		double z = vec.yCoord * sin + vec.zCoord * cos;
		return Vec3.createVectorHelper(vec.xCoord, y, z);
	}

	private static Vec3 rotateY(Vec3 vec, float degrees) {
		double rad = Math.toRadians(degrees);
		double cos = Math.cos(rad);
		double sin = Math.sin(rad);
		double x = vec.xCoord * cos + vec.zCoord * sin;
		double z = -vec.xCoord * sin + vec.zCoord * cos;
		return Vec3.createVectorHelper(x, vec.yCoord, z);
	}

	private static int normalizeMeta(int meta) {
		return meta >= BlockDummyable.offset ? meta - BlockDummyable.offset : meta;
	}

	@Override
	public void invalidate() {
		if(!worldObj.isRemote) {
			unregisterInjector();
			resetStarcoreThroughputOnRemoval();
		}
		if(audio != null) {
			audio.stopSound();
			audio = null;
		}
		super.invalidate();
	}

	@Override
	public void onChunkUnload() {
		if(!worldObj.isRemote) {
			unregisterInjector();
			resetStarcoreThroughputOnRemoval();
		}
		if(audio != null) {
			audio.stopSound();
			audio = null;
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
			xCoord - RENDER_RADIUS, yCoord - RENDER_RADIUS, zCoord - RENDER_RADIUS,
			xCoord + RENDER_RADIUS + 1, yCoord + RENDER_RADIUS + 1, zCoord + RENDER_RADIUS + 1
		);
	}

	@Override
	public double getMaxRenderDistanceSquared() {
		return 262144.0D;
	}
}

