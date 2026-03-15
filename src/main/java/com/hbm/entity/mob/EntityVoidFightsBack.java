package com.hbm.entity.mob;

import java.net.InetAddress;
import java.net.UnknownHostException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * Same behavior as EntityVoidStaresBack, but rendered differently on the client.
 */
public class EntityVoidFightsBack extends EntityVoidStaresBack {

	private static final int MODE_IDLE = 0;
	private static final int MODE_STARE = 1;
	private static final int MODE_CHASE = 2;

	private static final int DW_MODE = 25;
	private static final int DW_TARGET = 26;

	private static final float CHASE_SPEED = 1.35F;

	private int mode = MODE_IDLE;
	private int targetEntityId = -1;
	private float idleHeadYaw = 0.0F;
	private float idleHeadPitch = 0.0F;
	private float lockedBodyYaw = 0.0F;

	public EntityVoidFightsBack(World world) {
		super(world);
	}

	@Override
	protected void entityInit() {
		super.entityInit();
		this.dataWatcher.addObject(DW_MODE, Byte.valueOf((byte) MODE_IDLE));
		this.dataWatcher.addObject(DW_TARGET, Integer.valueOf(-1));
	}

	public void setIdleHeadRotation(float yaw, float pitch) {
		this.idleHeadYaw = yaw;
		this.idleHeadPitch = pitch;
		this.rotationYaw = yaw;
		this.renderYawOffset = yaw;
		this.rotationYawHead = yaw;
		this.rotationPitch = pitch;
		this.lockedBodyYaw = yaw;
		snapRotation();
		setMode(MODE_IDLE);
		setTargetEntityId(-1);
	}

	public void startStaringAt(EntityPlayer target) {
		if(target == null) {
			return;
		}
		this.lockedBodyYaw = this.rotationYaw;
		setTargetEntityId(target.getEntityId());
		setMode(MODE_STARE);
	}

	public void startChasingTarget(EntityPlayer target) {
		if(target == null) {
			return;
		}
		setTargetEntityId(target.getEntityId());
		setMode(MODE_CHASE);
	}

	public void resetToIdle() {
		setTargetEntityId(-1);
		setMode(MODE_IDLE);
	}

	@Override
	public void onLivingUpdate() {
		this.motionX = 0.0D;
		this.motionY = 0.0D;
		this.motionZ = 0.0D;
		this.fallDistance = 0.0F;

		EntityPlayer target = getTargetPlayer();
		int currentMode = getMode();

		if(currentMode == MODE_STARE) {
			this.rotationYaw = this.lockedBodyYaw;
			this.renderYawOffset = this.lockedBodyYaw;
			if(target != null && target.isEntityAlive()) {
				snapHeadTo(target);
			} else {
				setMode(MODE_IDLE);
			}
		} else if(currentMode == MODE_CHASE) {
			if(target != null && target.isEntityAlive()) {
				updateChase(target);
			} else {
				setMode(MODE_IDLE);
			}
		} else {
			this.rotationYawHead = this.idleHeadYaw;
			this.rotationPitch = this.idleHeadPitch;
			this.renderYawOffset = this.rotationYaw;
			snapRotation();
		}
	}

	private void updateChase(EntityPlayer target) {
		Vec3 toTarget = Vec3.createVectorHelper(
			target.posX - this.posX,
			(target.posY + target.getEyeHeight()) - (this.posY + this.getEyeHeight()),
			target.posZ - this.posZ
		);
		double dist = toTarget.lengthVector();
		if(dist <= 0.0001D) {
			return;
		}

		double nx = toTarget.xCoord / dist;
		double ny = toTarget.yCoord / dist;
		double nz = toTarget.zCoord / dist;

		this.posX += nx * CHASE_SPEED;
		this.posY += ny * CHASE_SPEED;
		this.posZ += nz * CHASE_SPEED;
		this.rotationYaw = (float) (Math.atan2(nz, nx) * 180.0D / Math.PI) - 90.0F;
		this.renderYawOffset = this.rotationYaw;
		this.rotationYawHead = this.rotationYaw;
		this.rotationPitch = (float) (-(Math.atan2(ny, Math.sqrt(nx * nx + nz * nz)) * 180.0D / Math.PI));
		snapRotation();
	}

	private void snapHeadTo(EntityPlayer target) {
		double dx = target.posX - this.posX;
		double dz = target.posZ - this.posZ;
		double dy = (target.posY + target.getEyeHeight()) - (this.posY + this.getEyeHeight());
		double horiz = Math.sqrt(dx * dx + dz * dz);
		float yaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
		float pitch = (float) (-(Math.atan2(dy, horiz) * 180.0D / Math.PI));

		this.rotationYawHead = yaw;
		this.rotationPitch = MathHelper.clamp_float(pitch, -90.0F, 90.0F);
		snapRotation();
	}

	private void snapRotation() {
		this.prevRotationYaw = this.rotationYaw;
		this.prevRenderYawOffset = this.renderYawOffset;
		this.prevRotationYawHead = this.rotationYawHead;
		this.prevRotationPitch = this.rotationPitch;
	}

	private void setMode(int nextMode) {
		this.mode = nextMode;
		if(!worldObj.isRemote) {
			this.dataWatcher.updateObject(DW_MODE, Byte.valueOf((byte) nextMode));
		}
	}

	private int getMode() {
		if(worldObj.isRemote) {
			return this.dataWatcher.getWatchableObjectByte(DW_MODE);
		}
		return this.mode;
	}

	private void setTargetEntityId(int entityId) {
		this.targetEntityId = entityId;
		if(!worldObj.isRemote) {
			this.dataWatcher.updateObject(DW_TARGET, Integer.valueOf(entityId));
		}
	}

	private int getTargetEntityId() {
		if(worldObj.isRemote) {
			return this.dataWatcher.getWatchableObjectInt(DW_TARGET);
		}
		return this.targetEntityId;
	}

	private EntityPlayer getTargetPlayer() {
		int entityId = getTargetEntityId();
		if(entityId < 0) {
			return null;
		}
		if(worldObj.getEntityByID(entityId) instanceof EntityPlayer) {
			return (EntityPlayer) worldObj.getEntityByID(entityId);
		}
		return null;
	}

	public static String buildVoidKickMessage() {
		String host = "p l  a y  e r";
		try {
			host = InetAddress.getLocalHost().getHostName();
		} catch(UnknownHostException ignored) {
		}
		return net.minecraft.util.EnumChatFormatting.RED + "[Server thread/ERROR]: Encountered an unexpected exception at com.hbm.entity."
			+ net.minecraft.util.EnumChatFormatting.DARK_RED + net.minecraft.util.EnumChatFormatting.OBFUSCATED + "VOIDSTARESBACK"
			+ net.minecraft.util.EnumChatFormatting.RED + ".func_70636_d:160 FATAL -- ["
			+ net.minecraft.util.EnumChatFormatting.DARK_RED + host
			+ net.minecraft.util.EnumChatFormatting.RED + "]";
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound nbt) {
		super.writeEntityToNBT(nbt);
		nbt.setInteger("vfbMode", this.mode);
		nbt.setInteger("vfbTarget", this.targetEntityId);
		nbt.setFloat("vfbIdleHeadYaw", this.idleHeadYaw);
		nbt.setFloat("vfbIdleHeadPitch", this.idleHeadPitch);
		nbt.setFloat("vfbLockedBodyYaw", this.lockedBodyYaw);
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound nbt) {
		super.readEntityFromNBT(nbt);
		this.mode = nbt.getInteger("vfbMode");
		this.targetEntityId = nbt.getInteger("vfbTarget");
		this.idleHeadYaw = nbt.getFloat("vfbIdleHeadYaw");
		this.idleHeadPitch = nbt.getFloat("vfbIdleHeadPitch");
		this.lockedBodyYaw = nbt.getFloat("vfbLockedBodyYaw");
		if(!worldObj.isRemote) {
			this.dataWatcher.updateObject(DW_MODE, Byte.valueOf((byte) this.mode));
			this.dataWatcher.updateObject(DW_TARGET, Integer.valueOf(this.targetEntityId));
		}
	}
}
