package com.hbm.entity.mob;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class EntityVoidStaresBack extends EntityLiving {

	private static final int DW_RECT_WIDTH = 20;
	private static final int DW_RECT_HEIGHT = 21;
	private static final float LOOK_DOT_THRESHOLD = 0.98F;
	private static final int LOOK_TICKS_REQUIRED = 20;
	private static final float CHASE_SPEED_MIN = 0.05F;
	private static final float CHASE_SPEED_MAX = 0.6F;
	private static final int CHASE_RAMP_TICKS = 200;

	private int lookedTicks = 0;
	private boolean chasing = false;
	private int chaseTicks = 0;
	private int targetEntityId = -1;

    public EntityVoidStaresBack(World world) {
        super(world);
        this.noClip = true;
        this.preventEntitySpawning = true;
        this.setSize(0.1F, 0.1F);

        if(!world.isRemote) {
            setRandomRectSize();
        }
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataWatcher.addObject(DW_RECT_WIDTH, Float.valueOf(1.0F));
        this.dataWatcher.addObject(DW_RECT_HEIGHT, Float.valueOf(1.0F));
    }

    private void setRandomRectSize() {
        float width = 0.75F + this.rand.nextFloat() * 2.25F;
        float height = 0.75F + this.rand.nextFloat() * 2.25F;
        this.dataWatcher.updateObject(DW_RECT_WIDTH, Float.valueOf(width));
        this.dataWatcher.updateObject(DW_RECT_HEIGHT, Float.valueOf(height));
    }

    public float getRectWidth() {
        return this.dataWatcher.getWatchableObjectFloat(DW_RECT_WIDTH);
    }

    public float getRectHeight() {
        return this.dataWatcher.getWatchableObjectFloat(DW_RECT_HEIGHT);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(1.0D);
        this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.0D);
    }

    @Override
    protected boolean isAIEnabled() {
        return false;
    }

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();

		if(worldObj.isRemote) {
			this.motionX = 0.0D;
			this.motionY = 0.0D;
			this.motionZ = 0.0D;
			this.fallDistance = 0.0F;
			return;
		}

		if(chasing) {
			EntityPlayer target = getTargetPlayer();
			if(target != null && target.isEntityAlive()) {
				Vec3 toTarget = Vec3.createVectorHelper(
					target.posX - this.posX,
					(target.posY + target.getEyeHeight() * 0.5D) - this.posY,
					target.posZ - this.posZ
				);
				double dist = toTarget.lengthVector();
				if(dist < 2.0D) {
					this.setDead();
					return;
				}
				if(dist > 0.0001D) {
					float ramp = MathHelper.clamp_float((float) chaseTicks / (float) CHASE_RAMP_TICKS, 0.0F, 1.0F);
					float speed = CHASE_SPEED_MIN + (CHASE_SPEED_MAX - CHASE_SPEED_MIN) * ramp;
					double nx = toTarget.xCoord / dist;
					double ny = toTarget.yCoord / dist;
					double nz = toTarget.zCoord / dist;
					this.motionX = nx * speed;
					this.motionY = ny * speed;
					this.motionZ = nz * speed;
					this.posX += this.motionX;
					this.posY += this.motionY;
					this.posZ += this.motionZ;
					this.rotationYaw = (float)(Math.atan2(nz, nx) * 180.0D / Math.PI) - 90.0F;
				}
				chaseTicks++;
			} else {
				chasing = false;
				chaseTicks = 0;
				targetEntityId = -1;
			}
		} else {
			this.motionX = 0.0D;
			this.motionY = 0.0D;
			this.motionZ = 0.0D;
			this.fallDistance = 0.0F;

			EntityPlayer watcher = getWatchingPlayer();
			if(watcher != null) {
				lookedTicks++;
				if(lookedTicks >= LOOK_TICKS_REQUIRED) {
					if(this.rand.nextFloat() < 0.05F) {
						chasing = true;
						chaseTicks = 0;
						targetEntityId = watcher.getEntityId();
					} else {
						this.setDead();
					}
				}
			} else {
				lookedTicks = 0;
			}
		}
	}

	private EntityPlayer getTargetPlayer() {
		if(targetEntityId >= 0) {
			EntityPlayer player = (EntityPlayer) worldObj.getEntityByID(targetEntityId);
			if(player != null) {
				return player;
			}
		}
		return worldObj.getClosestPlayerToEntity(this, 128.0D);
	}

	private EntityPlayer getWatchingPlayer() {
		EntityPlayer player = worldObj.getClosestPlayerToEntity(this, 96.0D);
		if(player == null) {
			return null;
		}

		Vec3 look = player.getLookVec();
		Vec3 toEntity = Vec3.createVectorHelper(
			this.posX - player.posX,
			(this.posY + this.height * 0.5D) - (player.posY + player.getEyeHeight()),
			this.posZ - player.posZ
		);
		double dist = toEntity.lengthVector();
		if(dist < 0.0001D) {
			return null;
		}

		toEntity = Vec3.createVectorHelper(toEntity.xCoord / dist, toEntity.yCoord / dist, toEntity.zCoord / dist);
		double dot = look.dotProduct(toEntity);
		if(dot < LOOK_DOT_THRESHOLD) {
			return null;
		}

		if(!player.canEntityBeSeen(this)) {
			return null;
		}

		return player;
	}

    @Override
    public void setHealth(float health) {
        super.setHealth(this.getMaxHealth());
    }

    @Override
    public boolean isEntityInvulnerable() {
        return true;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

	@Override
	public boolean canBePushed() {
		return false;
	}

	public void startChasing(EntityPlayer target) {
		if(target == null) {
			return;
		}
		this.chasing = true;
		this.chaseTicks = 0;
		this.targetEntityId = target.getEntityId();
		this.lookedTicks = 0;
	}

    @Override
	public void writeEntityToNBT(NBTTagCompound nbt) {
		super.writeEntityToNBT(nbt);
		nbt.setFloat("rectWidth", getRectWidth());
		nbt.setFloat("rectHeight", getRectHeight());
		nbt.setBoolean("chasing", chasing);
		nbt.setInteger("chaseTicks", chaseTicks);
		nbt.setInteger("targetEntityId", targetEntityId);
	}

    @Override
	public void readEntityFromNBT(NBTTagCompound nbt) {
		super.readEntityFromNBT(nbt);
		if(nbt.hasKey("rectWidth")) {
			this.dataWatcher.updateObject(DW_RECT_WIDTH, Float.valueOf(nbt.getFloat("rectWidth")));
		}
		if(nbt.hasKey("rectHeight")) {
			this.dataWatcher.updateObject(DW_RECT_HEIGHT, Float.valueOf(nbt.getFloat("rectHeight")));
		}
		if(nbt.hasKey("chasing")) {
			this.chasing = nbt.getBoolean("chasing");
		}
		if(nbt.hasKey("chaseTicks")) {
			this.chaseTicks = nbt.getInteger("chaseTicks");
		}
		if(nbt.hasKey("targetEntityId")) {
			this.targetEntityId = nbt.getInteger("targetEntityId");
		}
	}
}
