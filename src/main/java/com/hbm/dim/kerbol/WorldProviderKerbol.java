package com.hbm.dim.kerbol;

import com.hbm.dim.SolarSystem;
import com.hbm.lib.RefStrings;
import com.hbm.dim.WorldProviderCelestial;
import com.hbm.dim.kerbol.biome.BiomeGenKerbol;
import com.hbm.util.ParticleUtil;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.biome.WorldChunkManagerHell;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.client.event.EntityViewRenderEvent.FogDensity;
import org.lwjgl.opengl.GL11;
import net.minecraft.util.ResourceLocation;
import java.util.Random;

public class WorldProviderKerbol extends WorldProviderCelestial {
	private static final float GRAVITY_MIN = 0.1F;
	private static final float GRAVITY_MAX = 6.0F;
	private static final long GRAVITY_EVENT_INTERVAL_MILLIS = 10L * 60L * 1000L;

	private long lastGravityEventMillis = -1L;
	private static final ResourceLocation SUNSPIKE_TEXTURE = new ResourceLocation(RefStrings.MODID, "textures/misc/space/sunspike.png");
	private static final ResourceLocation CLOUD_TEXTURE = new ResourceLocation("textures/environment/clouds.png");
	private static final ResourceLocation NEIDON_TEXTURE = new ResourceLocation(RefStrings.MODID, "textures/misc/space/neidon.png");
	private static final float[] CLOUD_LAYER_OFFSETS = new float[] { 0F, 25F, 50F };
	private static final float[] CLOUD_LAYER_SPEEDS = new float[] { 0.02F, 0.035F, 0.05F };
	private static final float[] CLOUD_LAYER_ALPHA = new float[] { 0.6F, 0.45F, 0.3F };
	private static final IRenderHandler KERBOL_SKY = new IRenderHandler() {
		@Override
		public void render(float partialTicks, WorldClient world, Minecraft mc) {
			Tessellator tessellator = Tessellator.instance;

			GL11.glDisable(GL11.GL_FOG);
			GL11.glDisable(GL11.GL_ALPHA_TEST);
			GL11.glEnable(GL11.GL_BLEND);
			OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);
			GL11.glDepthMask(false);

			GL11.glPushMatrix();
			{
				// Speed up the dual-star orbit in the Kerbol sky.
				float solarAngle = (world.getCelestialAngle(partialTicks) * 128.0F) % 1.0F;
				if(solarAngle < 0.0F) {
					solarAngle += 1.0F;
				}

				GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
				GL11.glRotatef(solarAngle * 360.0F, 1.0F, 0.0F, 0.0F);

				// Star A (red, larger)
				GL11.glPushMatrix();
				{
					GL11.glRotatef(25.0F, 0.0F, 1.0F, 0.0F);
					GL11.glRotatef(45.0F, 0.0F, 0.0F, 1.0F);

					double spikeSize = 16.0D;
					mc.renderEngine.bindTexture(SUNSPIKE_TEXTURE);
					GL11.glColor4f(1.0F, 0.2F, 0.2F, 0.9F);
					tessellator.startDrawingQuads();
					tessellator.addVertexWithUV(-spikeSize, 99.9D, -spikeSize, 0.0D, 0.0D);
					tessellator.addVertexWithUV(spikeSize, 99.9D, -spikeSize, 1.0D, 0.0D);
					tessellator.addVertexWithUV(spikeSize, 99.9D, spikeSize, 1.0D, 1.0D);
					tessellator.addVertexWithUV(-spikeSize, 99.9D, spikeSize, 0.0D, 1.0D);
					tessellator.draw();

					OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
					GL11.glDisable(GL11.GL_TEXTURE_2D);
					GL11.glColor4f(0.0F, 0.0F, 0.0F, 1.0F);
					double size = 2.4D;
					tessellator.startDrawingQuads();
					tessellator.addVertex(-size, 100.0D, -size);
					tessellator.addVertex(size, 100.0D, -size);
					tessellator.addVertex(size, 100.0D, size);
					tessellator.addVertex(-size, 100.0D, size);
					tessellator.draw();
					GL11.glEnable(GL11.GL_TEXTURE_2D);
					OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);
				}
				GL11.glPopMatrix();

				// Star B (orange, smaller)
				GL11.glPushMatrix();
				{
					GL11.glRotatef(-75.0F, 0.0F, 1.0F, 0.0F);
					GL11.glRotatef(20.0F, 0.0F, 0.0F, 1.0F);

					double spikeSize = 10.0D;
					mc.renderEngine.bindTexture(SUNSPIKE_TEXTURE);
					GL11.glColor4f(0.55F, 0.12F, 1.1F, 0.85F);
					tessellator.startDrawingQuads();
					tessellator.addVertexWithUV(-spikeSize, 99.9D, -spikeSize, 0.0D, 0.0D);
					tessellator.addVertexWithUV(spikeSize, 99.9D, -spikeSize, 1.0D, 0.0D);
					tessellator.addVertexWithUV(spikeSize, 99.9D, spikeSize, 1.0D, 1.0D);
					tessellator.addVertexWithUV(-spikeSize, 99.9D, spikeSize, 0.0D, 1.0D);
					tessellator.draw();

					mc.renderEngine.bindTexture(NEIDON_TEXTURE);
					GL11.glColor4f(1.9F, 0.3F, 3.8F, 1.0F);
					double size = 0.6666667D;
					tessellator.startDrawingQuads();
					tessellator.addVertexWithUV(-size, 100.0D, -size, 0.0D, 0.0D);
					tessellator.addVertexWithUV(size, 100.0D, -size, 1.0D, 0.0D);
					tessellator.addVertexWithUV(size, 100.0D, size, 1.0D, 1.0D);
					tessellator.addVertexWithUV(-size, 100.0D, size, 0.0D, 1.0D);
					tessellator.draw();
				}
				GL11.glPopMatrix();
			}
			GL11.glPopMatrix();

			GL11.glDepthMask(true);
			GL11.glDisable(GL11.GL_BLEND);
			GL11.glEnable(GL11.GL_ALPHA_TEST);
			GL11.glEnable(GL11.GL_FOG);
			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		}
	};

	private static final IRenderHandler KERBOL_CLOUDS = new IRenderHandler() {
		@Override
		@SideOnly(Side.CLIENT)
		public void render(float partialTicks, WorldClient world, Minecraft mc) {
			Entity viewer = mc.renderViewEntity;
			if(viewer == null) return;

			Vec3 cloudColor = world.provider.drawClouds(partialTicks);
			float r = (float) cloudColor.xCoord;
			float g = (float) cloudColor.yCoord;
			float b = (float) cloudColor.zCoord;

			double px = viewer.prevPosX + (viewer.posX - viewer.prevPosX) * partialTicks;
			double py = viewer.prevPosY + (viewer.posY - viewer.prevPosY) * partialTicks;
			double pz = viewer.prevPosZ + (viewer.posZ - viewer.prevPosZ) * partialTicks;

			double baseHeight = world.provider.getCloudHeight();
			double cloudSize = 512.0D;
			double texScale = 1.0D / 256.0D;

			GL11.glDisable(GL11.GL_CULL_FACE);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glDepthMask(false);

			mc.renderEngine.bindTexture(CLOUD_TEXTURE);

			Tessellator tess = Tessellator.instance;

			GL11.glPushMatrix();
			GL11.glTranslated(-px, -py, -pz);

			double time = world.getTotalWorldTime() + partialTicks;

			for(int i = 0; i < CLOUD_LAYER_OFFSETS.length; i++) {
				double height = baseHeight + CLOUD_LAYER_OFFSETS[i];
				double speed = CLOUD_LAYER_SPEEDS[i];
				float alpha = CLOUD_LAYER_ALPHA[i];

				double scroll = time * speed * 8.0D;
				double x0 = px - cloudSize;
				double z0 = pz - cloudSize;
				double x1 = px + cloudSize;
				double z1 = pz + cloudSize;

				double u0 = (x0 + scroll) * texScale;
				double v0 = (z0 + scroll * 0.7D) * texScale;
				double u1 = (x1 + scroll) * texScale;
				double v1 = (z1 + scroll * 0.7D) * texScale;

				GL11.glColor4f(r, g, b, alpha);
				tess.startDrawingQuads();
				tess.addVertexWithUV(x0, height, z1, u0, v1);
				tess.addVertexWithUV(x1, height, z1, u1, v1);
				tess.addVertexWithUV(x1, height, z0, u1, v0);
				tess.addVertexWithUV(x0, height, z0, u0, v0);
				tess.draw();
			}

			GL11.glPopMatrix();

			GL11.glDepthMask(true);
			GL11.glDisable(GL11.GL_BLEND);
			GL11.glEnable(GL11.GL_CULL_FACE);
			GL11.glColor4f(1F, 1F, 1F, 1F);
		}
	};

	@Override
	public void registerWorldChunkManager() {
		this.worldChunkMgr = new WorldChunkManagerHell(BiomeGenKerbol.digammaWastelands, 0.0F);
	}

	@Override
	public String getDimensionName() { return "Kerbol"; }

	@Override
	public IChunkProvider createChunkGenerator() {
		return new ChunkProviderKerbol(this.worldObj);
	}

	@Override
	public void updateWeather() {
		super.updateWeather();

		if(!worldObj.isRemote) {
			return;
		}

		EntityLivingBase viewEntity = Minecraft.getMinecraft().renderViewEntity;
		if(viewEntity == null) {
			return;
		}

		if(worldObj.rand.nextFloat() > 0.25F) {
			return;
		}

		double time = (worldObj.getWorldTime() % 24000L) / 24000.0D * Math.PI * 2.0D;
		double windSpeed = 2.0D;
		double windX = (Math.cos(time) * 0.08D + worldObj.rand.nextGaussian() * 0.015D) * windSpeed;
		double windZ = (Math.sin(time) * 0.08D + worldObj.rand.nextGaussian() * 0.015D) * windSpeed;
		double windY = (worldObj.rand.nextDouble() - 0.5D) * 0.004D * windSpeed;

		Vec3 vec = Vec3.createVectorHelper(16 + worldObj.rand.nextDouble() * 24, worldObj.rand.nextDouble() * 6 - 3, 0);
		vec.rotateAroundY((float)(worldObj.rand.nextDouble() * Math.PI * 2));

		float scale = 0.6F + worldObj.rand.nextFloat() * 0.6F;
		ParticleUtil.spawnKerbolWind(worldObj,
				viewEntity.posX + vec.xCoord,
				viewEntity.posY + 1 + vec.yCoord,
				viewEntity.posZ + vec.zCoord,
				windX, windY, windZ,
				scale,
				0.95F, 0.2F, 0.2F, 0.6F);

		for(int i = 0; i < 2; i++) {
			if(worldObj.rand.nextFloat() > 0.85F) {
				continue;
			}
			Vec3 dotVec = Vec3.createVectorHelper(worldObj.rand.nextGaussian() * 10, worldObj.rand.nextDouble() * 6, worldObj.rand.nextGaussian() * 10);
			double dotX = worldObj.rand.nextGaussian() * 0.004D * windSpeed;
			double dotZ = worldObj.rand.nextGaussian() * 0.004D * windSpeed;
			double dotY = (0.025D + worldObj.rand.nextDouble() * 0.02D) * windSpeed;
			ParticleUtil.spawnKerbolDot(worldObj,
					viewEntity.posX + dotVec.xCoord,
					viewEntity.posY + 1 + dotVec.yCoord,
					viewEntity.posZ + dotVec.zCoord,
					dotX, dotY, dotZ,
					0.95F, 0.2F, 0.2F);
		}
	}

	@Override
	public Block getStone() { return Blocks.obsidian; }

	@Override
	protected double getDayLength() {
		// Kerbol has no parent, so avoid orbital-period math from WorldProviderCelestial.
		return com.hbm.dim.CelestialBody.getBody(worldObj).getRotationalPeriod();
	}

	@Override
	public void setGravityMultiplier(float multiplier) {
		super.setGravityMultiplier(MathHelper.clamp_float(multiplier, GRAVITY_MIN, GRAVITY_MAX));
	}

	public static Float rollGravityEvent(World world) {
		if (!(world.provider instanceof WorldProviderKerbol)) {
			return null;
		}

		WorldProviderKerbol kerbol = (WorldProviderKerbol) world.provider;
		long now = System.currentTimeMillis();

		if (kerbol.lastGravityEventMillis >= 0L &&
			(now - kerbol.lastGravityEventMillis) < GRAVITY_EVENT_INTERVAL_MILLIS) {
			return null;
		}

		kerbol.lastGravityEventMillis = now;

		float current = kerbol.getGravityMultiplier();
		float next = Math.abs(current - GRAVITY_MAX) < 0.01F ? GRAVITY_MIN : GRAVITY_MAX;

		return next;
	}

	@Override
	public IRenderHandler getSkyRenderer() {
		return KERBOL_SKY;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IRenderHandler getCloudRenderer() {
		return KERBOL_CLOUDS;
	}

	@Override
	public Vec3 getSkyColor(Entity camera, float partialTicks) {
		// Dark red sky with heartbeat pulse
		float pulse = getHeartbeatPulse(partialTicks);
		double r = 0.18 + pulse * 0.05;
		double g = 0.02 + pulse * 0.006;
		double b = 0.02 + pulse * 0.006;
		return Vec3.createVectorHelper(r, g, b);
	}

	@Override
	public Vec3 getFogColor(float x, float y) {
		// Darker red fog to match the sky, with heartbeat pulse
		float pulse = getHeartbeatPulse(0.0F);
		double r = 0.10 + pulse * 0.03;
		double g = 0.01 + pulse * 0.003;
		double b = 0.01 + pulse * 0.003;
		return Vec3.createVectorHelper(r, g, b);
	}

	@Override
	public float fogDensity(FogDensity event) {
		return 0.045F;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Vec3 drawClouds(float partialTicks) {
		Vec3 clouds = super.drawClouds(partialTicks);
		return Vec3.createVectorHelper(clouds.xCoord, clouds.yCoord * 0.2D, clouds.zCoord * 0.2D);
	}

	@Override
	public float[] calcSunriseSunsetColors(float solarAngle, float partialTicks) {
		return null;
	}

	private float getHeartbeatPulse(float partialTicks) {
		if(worldObj == null) {
			return 0.0F;
		}
		double periodTicks = 200.0D;
		double ticks = worldObj.getTotalWorldTime() + partialTicks;
		double phase = (ticks / periodTicks) % 1.0D;
		double p1 = 1.0D - Math.abs(phase - 0.0D) / 0.03D;
		double p2 = 1.0D - Math.abs(phase - 0.02D) / 0.035D;
		double pulse = Math.max(0.0D, p1);
		pulse = Math.max(pulse, Math.max(0.0D, p2) * 1.2D);
		return (float) (pulse * pulse);
	}
}
