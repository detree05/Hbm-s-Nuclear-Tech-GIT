package com.hbm.render.entity;

import org.lwjgl.opengl.GL11;

import com.hbm.entity.mob.EntityVoidStaresBack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

public class RenderVoidStaresBack extends Render {

	private static final float SHAKE_RANGE = 64.0F;
	private static final float SHAKE_MAX_OFFSET = 0.40F;
	private static final float RENDER_SIZE_SCALE = 1.5F;

    public RenderVoidStaresBack() {
        this.shadowSize = 0.0F;
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        EntityVoidStaresBack voidEntity = (EntityVoidStaresBack) entity;
        float collapseScale = voidEntity.getCollapseScale(partialTicks);
        if(collapseScale <= 0.001F) {
            return;
        }

        float baseWidth = voidEntity.getRectWidth();
        float baseHeight = voidEntity.getRectHeight();
        float width = baseWidth * collapseScale * RENDER_SIZE_SCALE;
        float height = baseHeight * collapseScale * RENDER_SIZE_SCALE;

        GL11.glPushMatrix();
		GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glTranslated(x, y + baseHeight * 0.5F, z);
        applyShake(voidEntity, partialTicks);
		GL11.glRotatef(180.0F - this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(-this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);

		GL11.glDisable(GL11.GL_FOG);
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
		GL11.glEnable(GL11.GL_FOG);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glPopAttrib();
		GL11.glPopMatrix();
	}

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return null;
    }

	private void applyShake(EntityVoidStaresBack voidEntity, float partialTicks) {
		float intensity = getShakeIntensity(voidEntity);
		if(intensity <= 0.001F) {
			return;
		}

		float amp = SHAKE_MAX_OFFSET * intensity * intensity;
		float t = (voidEntity.worldObj.getTotalWorldTime() + partialTicks) * (2.0F + 8.0F * intensity);
		float seed = voidEntity.getEntityId() * 0.37F;

		float dx = MathHelper.sin(t * 1.7F + seed) * amp;
		float dy = MathHelper.sin(t * 2.3F + seed * 1.3F) * amp;
		float dz = MathHelper.sin(t * 2.9F + seed * 2.1F) * amp;
		GL11.glTranslatef(dx, dy, dz);
	}

	private float getShakeIntensity(EntityVoidStaresBack voidEntity) {
		if(voidEntity == null) {
			return 0.0F;
		}

		Entity view = this.renderManager != null ? this.renderManager.livingPlayer : null;
		if(!(view instanceof EntityPlayer)) {
			view = Minecraft.getMinecraft().thePlayer;
		}
		if(!(view instanceof EntityPlayer)) {
			return 0.0F;
		}

		float dist = view.getDistanceToEntity(voidEntity);
		float intensity = 1.0F - (dist / SHAKE_RANGE);
		return MathHelper.clamp_float(intensity, 0.0F, 1.0F);
	}
}
