package com.hbm.render.entity.mob;

import org.lwjgl.opengl.GL11;

import com.hbm.entity.mob.EntityVoidFightsBack;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

public class RenderVoidFightsBack extends RenderBiped {

	private static final ResourceLocation STEVE_TEXTURE = new ResourceLocation("textures/entity/steve.png");

	public RenderVoidFightsBack() {
		super(new ModelBiped(0.0F), 0.5F, 1.0F);
	}

	@Override
	protected void preRenderCallback(EntityLivingBase living, float partialTicks) {
		EntityVoidFightsBack entity = (EntityVoidFightsBack) living;
		float collapseScale = entity.getCollapseScale(partialTicks);
		GL11.glScalef(collapseScale, collapseScale, collapseScale);
		GL11.glColor4f(0.0F, 0.0F, 0.0F, 1.0F);
	}

	@Override
	public void doRender(EntityLiving entity, double x, double y, double z, float yaw, float partialTicks) {
		GL11.glPushAttrib(GL11.GL_CURRENT_BIT);
		super.doRender(entity, x, y, z, yaw, partialTicks);
		GL11.glPopAttrib();
	}

	@Override
	protected ResourceLocation getEntityTexture(EntityLiving entity) {
		return STEVE_TEXTURE;
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		return STEVE_TEXTURE;
	}
}
