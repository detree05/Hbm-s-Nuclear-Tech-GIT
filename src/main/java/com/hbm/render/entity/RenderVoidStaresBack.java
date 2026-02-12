package com.hbm.render.entity;

import org.lwjgl.opengl.GL11;

import com.hbm.entity.mob.EntityVoidStaresBack;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

public class RenderVoidStaresBack extends Render {

    public RenderVoidStaresBack() {
        this.shadowSize = 0.0F;
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        EntityVoidStaresBack voidEntity = (EntityVoidStaresBack) entity;
        float width = voidEntity.getRectWidth();
        float height = voidEntity.getRectHeight();

        GL11.glPushMatrix();
        GL11.glTranslated(x, y + height * 0.5F, z);
		GL11.glRotatef(180.0F - this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(-this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);

		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDepthMask(false);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glColor4f(0.0F, 0.0F, 0.0F, 1.0F);

        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertex(-width * 0.5F, -height * 0.5F, 0.0D);
        tess.addVertex(width * 0.5F, -height * 0.5F, 0.0D);
        tess.addVertex(width * 0.5F, height * 0.5F, 0.0D);
        tess.addVertex(-width * 0.5F, height * 0.5F, 0.0D);
        tess.draw();

		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDepthMask(true);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glPopMatrix();
	}

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return null;
    }
}
