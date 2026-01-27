package com.hbm.entity.projectile;

import com.hbm.config.WorldConfig;
import com.hbm.main.MainRegistry;
import com.hbm.sound.AudioWrapper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class EntityKerbolMeteor extends Entity {

	private AudioWrapper audioFly;

	public EntityKerbolMeteor(World world) {
		super(world);
		this.ignoreFrustumCheck = true;
		this.isImmuneToFire = true;
		this.setSize(4F, 4F);
	}

	@Override
	public void onUpdate() {
		this.prevPosX = this.posX;
		this.prevPosY = this.posY;
		this.prevPosZ = this.posZ;

		this.motionY -= 0.03;
		if(this.motionY < -2.5) {
			this.motionY = -2.5;
		}

		this.moveEntity(this.motionX, this.motionY, this.motionZ);

		if(!this.worldObj.isRemote && this.onGround) {
			this.worldObj.playSoundEffect(this.posX, this.posY, this.posZ, "hbm:entity.oldExplosion", 6.0F, 0.8F + this.rand.nextFloat() * 0.1F);
			this.setDead();
		}

		if(this.worldObj.isRemote) {
			if(this.isDead) {
				if(this.audioFly != null) {
					this.audioFly.stopSound();
				}
			} else {
				if(this.audioFly == null) {
					this.audioFly = MainRegistry.proxy.getLoopedSound("hbm:entity.meteoriteFallingLoop", 0, 0, 0, 1F, 200F, 0.9F + this.rand.nextFloat() * 0.2F, 10);
				}

				if(this.audioFly.isPlaying()) {
					this.audioFly.keepAlive();
					this.audioFly.updateVolume(1F);
					this.audioFly.updatePosition((float) this.posX, (float) (this.posY + this.height / 2), (float) this.posZ);
				} else {
					EntityPlayer player = MainRegistry.proxy.me();
					if(player != null) {
						double distance = player.getDistanceSq(this.posX, this.posY, this.posZ);
						if(distance < 210 * 210) {
							this.audioFly.startSound();
						}
					}
				}
			}

			if(WorldConfig.enableMeteorTails) {
				NBTTagCompound data = new NBTTagCompound();
				data.setString("type", "exhaust");
				data.setString("mode", "meteor");
				data.setInteger("count", 10);
				data.setDouble("width", 1);
				data.setDouble("posX", this.posX - this.motionX);
				data.setDouble("posY", this.posY - this.motionY);
				data.setDouble("posZ", this.posZ - this.motionZ);
				MainRegistry.proxy.effectNT(data);
			}
		}
	}

	@Override
	protected void entityInit() {
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbt) {
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbt) {
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isInRangeToRenderDist(double distance) {
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getBrightnessForRender(float f) {
		return 15728880;
	}

	@Override
	public float getBrightness(float f) {
		return 1.0F;
	}
}
