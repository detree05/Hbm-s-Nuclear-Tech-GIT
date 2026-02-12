package com.hbm.entity.mob;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class EntityVoidStaresBack extends EntityLiving {

    private static final int DW_RECT_WIDTH = 20;
    private static final int DW_RECT_HEIGHT = 21;

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
        this.motionX = 0.0D;
        this.motionY = 0.0D;
        this.motionZ = 0.0D;
        this.fallDistance = 0.0F;
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
    public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setFloat("rectWidth", getRectWidth());
        nbt.setFloat("rectHeight", getRectHeight());
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
    }
}
