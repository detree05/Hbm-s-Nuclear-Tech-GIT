package com.hbm.particle;

import com.hbm.main.ModEventHandlerClient;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.World;

@SideOnly(Side.CLIENT)
public class ParticleDust extends EntityFX {
	private final boolean smoothAlpha;
	private final float baseAlpha;

	public ParticleDust(World world, double x, double y, double z, double mX, double mY, double mZ, float scale) {
		this(world, x, y, z, mX, mY, mZ, scale, 0.4f, 0.2f, 0.1f, 0.5f, false);
	}

	public ParticleDust(World world, double x, double y, double z, double mX, double mY, double mZ, float scale, float r, float g, float b, float a) {
		this(world, x, y, z, mX, mY, mZ, scale, r, g, b, a, false);
	}

	public ParticleDust(World world, double x, double y, double z, double mX, double mY, double mZ, float scale, float r, float g, float b, float a, boolean smoothAlpha) {
		super(world, x, y, z, mX, mY, mZ);
		particleIcon = ModEventHandlerClient.particleBase;
		this.particleRed = r;
		this.particleGreen = g;
		this.particleBlue = b;
		this.particleScale = scale;
		this.motionX = mX;
		this.motionY = mY;
		this.motionZ = mZ;
		this.particleAge = 1;
		this.particleMaxAge = 50 + world.rand.nextInt(50);
		this.baseAlpha = a;
		this.smoothAlpha = smoothAlpha;
		this.particleAlpha = a;
		this.noClip = true;
	}

	@Override
	public int getFXLayer() {
		return 1;
	}

	@Override
	public void onUpdate() {
		super.onUpdate();

		if(smoothAlpha) {
			float t = (float) this.particleAge / (float) this.particleMaxAge;
			float fade = (float) Math.sin(t * Math.PI);
			this.particleAlpha = baseAlpha * Math.max(0F, Math.min(1F, fade));
		}
	}

	@Override
	public void renderParticle(Tessellator tess, float interp, float fX, float fY, float fZ, float sX, float sZ) {
		tess.setNormal(0.0F, 1.0F, 0.0F);
		
		tess.setColorRGBA_F(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha);

		float scale = this.particleScale;
		float pX = (float) (this.prevPosX + (this.posX - this.prevPosX) * (double) interp - interpPosX);
		float pY = (float) (this.prevPosY + (this.posY - this.prevPosY) * (double) interp - interpPosY);
		float pZ = (float) (this.prevPosZ + (this.posZ - this.prevPosZ) * (double) interp - interpPosZ);

		tess.addVertexWithUV((double) (pX - fX * scale - sX * scale), (double) (pY - fY * scale), (double) (pZ - fZ * scale - sZ * scale), particleIcon.getMaxU(), particleIcon.getMaxV());
		tess.addVertexWithUV((double) (pX - fX * scale + sX * scale), (double) (pY + fY * scale), (double) (pZ - fZ * scale + sZ * scale), particleIcon.getMaxU(), particleIcon.getMinV());
		tess.addVertexWithUV((double) (pX + fX * scale + sX * scale), (double) (pY + fY * scale), (double) (pZ + fZ * scale + sZ * scale), particleIcon.getMinU(), particleIcon.getMinV());
		tess.addVertexWithUV((double) (pX + fX * scale - sX * scale), (double) (pY - fY * scale), (double) (pZ + fZ * scale - sZ * scale), particleIcon.getMinU(), particleIcon.getMaxV());
	}
	
}
