package com.hbm.entity.mob;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.hbm.main.MainRegistry;
import com.hbm.sound.AudioWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class EntityVoidStaresBack extends EntityLiving {

	private static final int DW_RECT_WIDTH = 20;
	private static final int DW_RECT_HEIGHT = 21;
	private static final int DW_CHASING = 22;
	private static final int DW_COLLAPSING = 23;
	private static final int DW_COLLAPSE_TICKS = 24;
	private static final float LOOK_DOT_THRESHOLD = 0.98F;
	private static final int LOOK_TICKS_REQUIRED = 20;
	private static final float CHASE_SPEED_MIN = 0.05F;
	private static final float CHASE_SPEED_MAX = 3.0F;
	private static final int CHASE_RAMP_TICKS = 200;
	private static final float CHASE_SOUND_RANGE = 64.0F;
	private static final float CHASE_SOUND_VOLUME = 2.0F;
	private static final float RECT_WIGGLE_STEP = 0.12F;
	private static final float RECT_WIGGLE_MIN = 0.75F;
	private static final float RECT_WIGGLE_MAX = 3.5F;
	private static final int NOT_LOOKED_TICKS_LIMIT = 1200;
	private static final int COLLAPSE_TICKS_TOTAL = 20;

	private int lookedTicks = 0;
	private int notLookedTicks = 0;
	private boolean chasing = false;
	private int chaseTicks = 0;
	private int targetEntityId = -1;
	private boolean collapsing = false;
	private int collapseTicks = 0;
	private AudioWrapper audioChase;

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
        this.dataWatcher.addObject(DW_CHASING, Byte.valueOf((byte) 0));
        this.dataWatcher.addObject(DW_COLLAPSING, Byte.valueOf((byte) 0));
        this.dataWatcher.addObject(DW_COLLAPSE_TICKS, Integer.valueOf(0));
    }

    private void setRandomRectSize() {
        float width = 0.75F + this.rand.nextFloat() * 2.25F;
        float height = 0.75F + this.rand.nextFloat() * 2.25F;
        this.dataWatcher.updateObject(DW_RECT_WIDTH, Float.valueOf(width));
        this.dataWatcher.updateObject(DW_RECT_HEIGHT, Float.valueOf(height));
    }

	private void wiggleRectSize() {
		float width = getRectWidth() + (this.rand.nextFloat() - 0.5F) * RECT_WIGGLE_STEP;
		float height = getRectHeight() + (this.rand.nextFloat() - 0.5F) * RECT_WIGGLE_STEP;
		width = MathHelper.clamp_float(width, RECT_WIGGLE_MIN, RECT_WIGGLE_MAX);
		height = MathHelper.clamp_float(height, RECT_WIGGLE_MIN, RECT_WIGGLE_MAX);
		this.dataWatcher.updateObject(DW_RECT_WIDTH, Float.valueOf(width));
		this.dataWatcher.updateObject(DW_RECT_HEIGHT, Float.valueOf(height));
	}

	private void returnToMainMenu(String message) {
		Minecraft mc = Minecraft.getMinecraft();
		if(mc.theWorld != null) {
			mc.theWorld.sendQuittingDisconnectingPacket();
		}
		mc.loadWorld((WorldClient) null);
		mc.displayGuiScreen(new GuiDisconnected(new GuiMainMenu(), "disconnect.disconnected", new ChatComponentText(message)));
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
			if(this.isDead) {
				if(this.audioChase != null) {
					this.audioChase.stopSound();
					this.audioChase = null;
				}
			} else if(isChasing()) {
				if(this.audioChase == null) {
					this.audioChase = MainRegistry.proxy.getLoopedSound("hbm:misc.itlives_itstaresback", this, CHASE_SOUND_VOLUME, CHASE_SOUND_RANGE, 1.0F, 10);
				}

				EntityPlayer player = MainRegistry.proxy.me();
				if(player != null) {
					float dist = player.getDistanceToEntity(this);
					if(dist < 1.0F) {
						String host = "p l  a y  e r";
						try {
							host = InetAddress.getLocalHost().getHostName();
						} catch (UnknownHostException ignored) {
						}
						returnToMainMenu(
							EnumChatFormatting.RED + "[Server thread/ERROR]: Encountered an unexpected exception at com.hbm.entity." +
							EnumChatFormatting.DARK_RED + EnumChatFormatting.OBFUSCATED + "VOIDSTARESBACK" +
							EnumChatFormatting.RED + ".func_70636_d:160 FATAL -- [" +
							EnumChatFormatting.DARK_RED + host +
							EnumChatFormatting.RED + "]"
						);
						return;
					}
					float scale = 1.0F - (dist / CHASE_SOUND_RANGE);
					if(scale < 0.0F) {
						scale = 0.0F;
					}
					// Much steeper falloff so it's quiet unless you're close
					scale = scale * scale * scale;
					this.audioChase.updateVolume(CHASE_SOUND_VOLUME * scale);
				} else {
					this.audioChase.updateVolume(CHASE_SOUND_VOLUME);
				}

				if(this.audioChase.isPlaying()) {
					this.audioChase.keepAlive();
				} else {
					this.audioChase.startSound();
				}
			} else {
				if(this.audioChase != null) {
					this.audioChase.stopSound();
					this.audioChase = null;
				}
			}

			this.motionX = 0.0D;
			this.motionY = 0.0D;
			this.motionZ = 0.0D;
			this.fallDistance = 0.0F;
			return;
		}

		if(isCollapsing()) {
			updateCollapse();
			return;
		}

		wiggleRectSize();

		if(isChasing()) {
			EntityPlayer target = getTargetPlayer();
			if(target != null && target.isEntityAlive()) {
				Vec3 toTarget = Vec3.createVectorHelper(
					target.posX - this.posX,
					(target.posY + target.height * 0.5D) - this.posY,
					target.posZ - this.posZ
				);
				double dist = toTarget.lengthVector();
				if(dist < 1.0D) {
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
				setChasing(false);
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
				notLookedTicks = 0;
				if(lookedTicks >= LOOK_TICKS_REQUIRED) {
					if(this.rand.nextFloat() < 0.05F) {
						setChasing(true);
						chaseTicks = 0;
						targetEntityId = watcher.getEntityId();
					} else {
						beginCollapse();
						return;
					}
				}
			} else {
				lookedTicks = 0;
				notLookedTicks++;
				if(notLookedTicks >= NOT_LOOKED_TICKS_LIMIT) {
					beginCollapse();
				}
			}
		}
	}

	private void beginCollapse() {
		if(this.collapsing || this.isDead) {
			return;
		}

		this.collapsing = true;
		this.collapseTicks = 0;
		setChasing(false);
		this.motionX = 0.0D;
		this.motionY = 0.0D;
		this.motionZ = 0.0D;
		this.fallDistance = 0.0F;

		if(!worldObj.isRemote) {
			this.dataWatcher.updateObject(DW_COLLAPSING, Byte.valueOf((byte) 1));
			this.dataWatcher.updateObject(DW_COLLAPSE_TICKS, Integer.valueOf(0));
		}
	}

	private void updateCollapse() {
		this.motionX = 0.0D;
		this.motionY = 0.0D;
		this.motionZ = 0.0D;
		this.fallDistance = 0.0F;

		if(worldObj.isRemote) {
			return;
		}

		this.collapseTicks++;
		this.dataWatcher.updateObject(DW_COLLAPSE_TICKS, Integer.valueOf(this.collapseTicks));

		if(this.collapseTicks >= COLLAPSE_TICKS_TOTAL) {
			this.setDead();
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

	@Override
	public void moveEntity(double x, double y, double z) {
		if(!isChasing()) {
			return;
		}
		super.moveEntity(x, y, z);
	}

	public void startChasing(EntityPlayer target) {
		if(target == null || isCollapsing()) {
			return;
		}
		setChasing(true);
		this.chaseTicks = 0;
		this.targetEntityId = target.getEntityId();
		this.lookedTicks = 0;
	}

	private void setChasing(boolean chasing) {
		this.chasing = chasing;
		if(!worldObj.isRemote) {
			this.dataWatcher.updateObject(DW_CHASING, Byte.valueOf((byte) (chasing ? 1 : 0)));
		}
	}

	public boolean isChasing() {
		if(worldObj.isRemote) {
			return this.dataWatcher.getWatchableObjectByte(DW_CHASING) != 0;
		}
		return this.chasing;
	}

	public boolean isCollapsing() {
		if(worldObj.isRemote) {
			return this.dataWatcher.getWatchableObjectByte(DW_COLLAPSING) != 0;
		}
		return this.collapsing;
	}

	public float getCollapseScale(float partialTicks) {
		if(!isCollapsing()) {
			return 1.0F;
		}

		float ticks = worldObj.isRemote
			? this.dataWatcher.getWatchableObjectInt(DW_COLLAPSE_TICKS) + partialTicks
			: this.collapseTicks + partialTicks;
		float progress = MathHelper.clamp_float(ticks / (float) COLLAPSE_TICKS_TOTAL, 0.0F, 1.0F);
		return 1.0F - progress;
	}

    @Override
	public void writeEntityToNBT(NBTTagCompound nbt) {
		super.writeEntityToNBT(nbt);
		nbt.setFloat("rectWidth", getRectWidth());
		nbt.setFloat("rectHeight", getRectHeight());
		nbt.setBoolean("chasing", chasing);
		nbt.setInteger("chaseTicks", chaseTicks);
		nbt.setInteger("targetEntityId", targetEntityId);
		nbt.setInteger("notLookedTicks", notLookedTicks);
		nbt.setBoolean("collapsing", collapsing);
		nbt.setInteger("collapseTicks", collapseTicks);
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
			if(!worldObj.isRemote) {
				this.dataWatcher.updateObject(DW_CHASING, Byte.valueOf((byte) (this.chasing ? 1 : 0)));
			}
		}
		if(nbt.hasKey("chaseTicks")) {
			this.chaseTicks = nbt.getInteger("chaseTicks");
		}
		if(nbt.hasKey("targetEntityId")) {
			this.targetEntityId = nbt.getInteger("targetEntityId");
		}
		if(nbt.hasKey("notLookedTicks")) {
			this.notLookedTicks = nbt.getInteger("notLookedTicks");
		}
		if(nbt.hasKey("collapsing")) {
			this.collapsing = nbt.getBoolean("collapsing");
			if(!worldObj.isRemote) {
				this.dataWatcher.updateObject(DW_COLLAPSING, Byte.valueOf((byte) (this.collapsing ? 1 : 0)));
			}
		}
		if(nbt.hasKey("collapseTicks")) {
			this.collapseTicks = nbt.getInteger("collapseTicks");
			if(!worldObj.isRemote) {
				this.dataWatcher.updateObject(DW_COLLAPSE_TICKS, Integer.valueOf(this.collapseTicks));
			}
		}
	}
}
