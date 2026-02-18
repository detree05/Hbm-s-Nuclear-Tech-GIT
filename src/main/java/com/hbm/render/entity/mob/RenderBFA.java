package com.hbm.render.entity.mob;

import org.lwjgl.opengl.GL11;

import com.hbm.entity.mob.EntityBFAngel;
import com.hbm.entity.mob.EntityUFO;
import com.hbm.main.ResourceManager;
import com.hbm.render.util.BeamPronter;
import com.hbm.render.util.BeamPronter.EnumBeamType;
import com.hbm.render.util.BeamPronter.EnumWaveType;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.BossStatus;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;

public class RenderBFA extends Render {

	@Override
	public void doRender(Entity entity, double x, double y, double z, float f0, float f1) {

		BossStatus.setBossStatus((IBossDisplayData)entity, false);


		EntityBFAngel ufo = (EntityBFAngel)entity;

		float yaw = ufo.prevRotationYaw + (ufo.rotationYaw - ufo.prevRotationYaw) * f1;
		float pitch = ufo.prevRotationPitch + (ufo.rotationPitch - ufo.prevRotationPitch) * f1;
		
		GL11.glPushMatrix();
		GL11.glTranslated(x, y + 1, z);
		GL11.glRotatef(-yaw, 0F, 1F, 0F);
		GL11.glRotatef(pitch, 1F, 0F, 0F);
		

		double scale = 2D;
		
		this.bindTexture(getEntityTexture(entity));
		



		GL11.glPushMatrix();

		GL11.glScalef(2, 2, 2);
		GL11.glTranslated(0.5, 0.2, 0.5);
		GL11.glDisable(GL11.GL_CULL_FACE);

		double time = entity.ticksExisted + f1;
		double cy0 = Math.sin(time * 0.08 % (Math.PI * 2));
		double cy1 = Math.sin(time * 0.07 % (Math.PI * 2) - Math.PI * 0.2);
		double cy2 = Math.sin(time * 0.06 % (Math.PI * 2) - Math.PI * 0.4);

		GL11.glTranslatef(0, 0.5F, 0);
		bindTexture(ResourceManager.bfangel_tex);

		// Core
		ResourceManager.bfangel.renderPart("eye");
		ResourceManager.bfangel.renderPart("body");

		// === Wings ===
		GL11.glPushMatrix();
		GL11.glRotated(cy0 * 2, 0, 0, 1);
		GL11.glRotated(cy0 * 20, 0, 1, 0);
		ResourceManager.bfangel.renderPart("wingL1");
		GL11.glPopMatrix();

		GL11.glPushMatrix();
		GL11.glRotated(cy0 * -2, 0, 0, 1);
		GL11.glRotated(cy0 * -20, 0, 1, 0);
		ResourceManager.bfangel.renderPart("wingR1");
		GL11.glPopMatrix();

		GL11.glPushMatrix();
		GL11.glRotated(cy1 * 5, 0, 0, 1);
		GL11.glRotated(cy1 * 20, 0, 1, 0);
		ResourceManager.bfangel.renderPart("wingL2");
		GL11.glPopMatrix();

		GL11.glPushMatrix();
		GL11.glRotated(cy1 * -5, 0, 0, 1);
		GL11.glRotated(cy1 * -20, 0, 1, 0);
		ResourceManager.bfangel.renderPart("wingR2");
		GL11.glPopMatrix();

		GL11.glPushMatrix();
		GL11.glRotated(cy1 * 5, 1, 0, 0);
		GL11.glRotated(cy2 * 20, 0, 1, 0);
		ResourceManager.bfangel.renderPart("wingL3");
		GL11.glPopMatrix();

		GL11.glPushMatrix();
		GL11.glRotated(cy1 * -5, 1, 0, 0);
		GL11.glRotated(cy2 * -20, 0, 1, 0);
		ResourceManager.bfangel.renderPart("wingR3");
		GL11.glPopMatrix();

		GL11.glPopMatrix();

		if(ufo.getBeam()) {
			int ix = (int)Math.floor(entity.posX);
			int iz = (int)Math.floor(entity.posZ);
			int iy = 0;
			
			for(int i = (int)Math.ceil(entity.posY); i >= 0; i--) {
				
				if(entity.worldObj.getBlock(ix, i, iz) != Blocks.air) {
					iy = i;
					break;
				}
			}
			
			double length = entity.posY - iy;
			
			if(length > 0) {
				BeamPronter.prontBeam(Vec3.createVectorHelper(0, -length, 0), EnumWaveType.SPIRAL, EnumBeamType.SOLID, 0x101020, 0x101020, 0, (int)(length + 1), 0F, 6, (float)scale * 0.75F, 256);
				BeamPronter.prontBeam(Vec3.createVectorHelper(0, -length, 0), EnumWaveType.RANDOM, EnumBeamType.SOLID, 0x202060, 0x202060, entity.ticksExisted / 2, (int)(length / 2 + 1), (float)scale * 1.5F, 2, 0.0625F, 256);
				BeamPronter.prontBeam(Vec3.createVectorHelper(0, -length, 0), EnumWaveType.RANDOM, EnumBeamType.SOLID, 0x202060, 0x202060, entity.ticksExisted / 4, (int)(length / 2 + 1), (float)scale * 1.5F, 2, 0.0625F, 256);
			}
		}
		
		GL11.glPopMatrix();
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		return ResourceManager.bfangel_tex;
	}
}
