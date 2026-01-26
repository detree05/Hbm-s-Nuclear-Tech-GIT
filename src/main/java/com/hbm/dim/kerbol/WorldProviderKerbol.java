package com.hbm.dim.kerbol;

import com.hbm.dim.SolarSystem;
import com.hbm.lib.RefStrings;
import com.hbm.dim.WorldProviderCelestial;
import com.hbm.dim.orbit.BiomeGenOrbit;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Vec3;
import net.minecraft.world.biome.WorldChunkManagerHell;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.client.IRenderHandler;
import org.lwjgl.opengl.GL11;
import net.minecraft.util.ResourceLocation;

public class WorldProviderKerbol extends WorldProviderCelestial {
	private static final ResourceLocation SUNSPIKE_TEXTURE = new ResourceLocation(RefStrings.MODID, "textures/misc/space/sunspike.png");
	private static final IRenderHandler KERBOL_SKY = new IRenderHandler() {
		@Override
		public void render(float partialTicks, WorldClient world, Minecraft mc) {
			// Render a single bright "star" using the sun texture.
			Tessellator tessellator = Tessellator.instance;

			GL11.glDisable(GL11.GL_FOG);
			GL11.glDisable(GL11.GL_ALPHA_TEST);
			GL11.glEnable(GL11.GL_BLEND);
			OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);
			GL11.glDepthMask(false);

			GL11.glPushMatrix();
			{
				float solarAngle = world.getCelestialAngle(partialTicks);

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

					mc.renderEngine.bindTexture(SolarSystem.kerbol.texture);
					GL11.glColor4f(3.0F, 0.4F, 0.4F, 1.0F);
					double size = 6.0D;
					tessellator.startDrawingQuads();
					tessellator.addVertexWithUV(-size, 100.0D, -size, 0.0D, 0.0D);
					tessellator.addVertexWithUV(size, 100.0D, -size, 1.0D, 0.0D);
					tessellator.addVertexWithUV(size, 100.0D, size, 1.0D, 1.0D);
					tessellator.addVertexWithUV(-size, 100.0D, size, 0.0D, 1.0D);
					tessellator.draw();
				}
				GL11.glPopMatrix();

				// Star B (orange, smaller)
				GL11.glPushMatrix();
				{
					GL11.glRotatef(-75.0F, 0.0F, 1.0F, 0.0F);
					GL11.glRotatef(20.0F, 0.0F, 0.0F, 1.0F);

					double spikeSize = 10.0D;
					mc.renderEngine.bindTexture(SUNSPIKE_TEXTURE);
					GL11.glColor4f(1.0F, 0.45F, 0.15F, 0.85F);
					tessellator.startDrawingQuads();
					tessellator.addVertexWithUV(-spikeSize, 99.9D, -spikeSize, 0.0D, 0.0D);
					tessellator.addVertexWithUV(spikeSize, 99.9D, -spikeSize, 1.0D, 0.0D);
					tessellator.addVertexWithUV(spikeSize, 99.9D, spikeSize, 1.0D, 1.0D);
					tessellator.addVertexWithUV(-spikeSize, 99.9D, spikeSize, 0.0D, 1.0D);
					tessellator.draw();

					mc.renderEngine.bindTexture(SolarSystem.kerbol.texture);
					GL11.glColor4f(3.0F, 0.9F, 0.25F, 1.0F);
					double size = 2.5D;
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

	@Override
	public void registerWorldChunkManager() {
		this.worldChunkMgr = new WorldChunkManagerHell(BiomeGenOrbit.biome, 0.0F);
	}

	@Override
	public String getDimensionName() { return "Kerbol"; }

	@Override
	public IChunkProvider createChunkGenerator() {
		return new ChunkProviderKerbol(this.worldObj);
	}

	@Override
	public Block getStone() { return Blocks.obsidian; }

	@Override
	protected double getDayLength() {
		// Kerbol has no parent, so avoid orbital-period math from WorldProviderCelestial.
		return com.hbm.dim.CelestialBody.getBody(worldObj).getRotationalPeriod();
	}

	@Override
	public IRenderHandler getSkyRenderer() {
		return KERBOL_SKY;
	}

	@Override
	public Vec3 getSkyColor(Entity camera, float partialTicks) {
		// Dark red sky
		return Vec3.createVectorHelper(0.18, 0.02, 0.02);
	}

	@Override
	public Vec3 getFogColor(float x, float y) {
		// Darker red fog to match the sky
		return Vec3.createVectorHelper(0.10, 0.01, 0.01);
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

	@Override
	public ChunkCoordinates getSpawnPoint() {
		// Spawn one block above the single basalt block at origin
		return new ChunkCoordinates(0, 65, 0);
	}
}
