package com.hbm.dim;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import org.lwjgl.opengl.GL11;

import com.hbm.dim.SolarSystem.AstroMetric;
import com.hbm.config.SpaceConfig;
import com.hbm.dim.orbit.OrbitalStation;
import com.hbm.dim.trait.CBT_Atmosphere;
import com.hbm.dim.trait.CBT_Atmosphere.FluidEntry;
import com.hbm.dim.trait.CBT_Dyson;
import com.hbm.dim.trait.CBT_SkyState;
import com.hbm.dim.trait.CelestialBodyTrait.CBT_COMPROMISED;
import com.hbm.dim.trait.CBT_War;
import com.hbm.dim.trait.CBT_Destroyed;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.RefStrings;
import com.hbm.main.ResourceManager;
import com.hbm.render.shader.Shader;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.util.BobMathUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.IRenderHandler;

import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GLContext;

import com.hbm.dim.trait.CBT_Impact;
import com.hbm.dim.trait.CBT_Lights;
import com.hbm.main.ModEventHandlerClient;
import com.hbm.main.ModEventHandlerRenderer;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.ReflectionHelper;

public class SkyProviderCelestial extends IRenderHandler {

	private static final ResourceLocation planetTexture = new ResourceLocation(RefStrings.MODID, "textures/misc/space/planet.png");
	private static final ResourceLocation cloudsTexture = new ResourceLocation(RefStrings.MODID, "textures/misc/space/clouds.png");
	private static final ResourceLocation flareTexture = new ResourceLocation(RefStrings.MODID, "textures/misc/space/starcore_spike.png");
	private static final ResourceLocation supernovaeTexture = new ResourceLocation(RefStrings.MODID, "textures/misc/supernovae.png");
	private static final ResourceLocation nightTexture = new ResourceLocation(RefStrings.MODID, "textures/misc/space/night.png");
	private static final ResourceLocation digammaStar = new ResourceLocation(RefStrings.MODID, "textures/misc/space/star_digamma.png");
	private static final ResourceLocation lodeStar = new ResourceLocation(RefStrings.MODID, "textures/misc/star_lode.png");
	private static final ResourceLocation stationTexture = new ResourceLocation(RefStrings.MODID, "textures/misc/space/station.png");
	private static final ResourceLocation defaultSunTexture = new ResourceLocation(RefStrings.MODID, "textures/misc/space/sun.png");
	private static final ResourceLocation starcoreSpikeTexture = new ResourceLocation(RefStrings.MODID, "textures/misc/space/starcore_spike.png");
	private static final ResourceLocation starcoreCoreTexture = new ResourceLocation(RefStrings.MODID, "textures/misc/space/starcore.png");

	private static final ResourceLocation impactTexture = new ResourceLocation(RefStrings.MODID, "textures/misc/space/impact.png");
	private static final ResourceLocation shockwaveTexture = new ResourceLocation(RefStrings.MODID, "textures/particle/shockwave.png");
	protected static final ResourceLocation shockFlareTexture = new ResourceLocation(RefStrings.MODID, "textures/particle/flare.png");
	private static final ResourceLocation particleBaseTexture = new ResourceLocation(RefStrings.MODID, "textures/particle/particle_base.png");

	private static final ResourceLocation ringTexture = new ResourceLocation(RefStrings.MODID, "textures/misc/space/rings.png");
	private static final ResourceLocation destroyedBody = new ResourceLocation(RefStrings.MODID, "textures/misc/space/destroyed.png");

	private static final ResourceLocation thatmoShield = new ResourceLocation(RefStrings.MODID, "textures/particle/cens.png");

	private static final Shader fleshShader = new Shader(new ResourceLocation(RefStrings.MODID, "shaders/fle.frag"));
	private static final ResourceLocation noise = new ResourceLocation(RefStrings.MODID, "shaders/iChannel1.png");

	protected static final Shader planetShader = new Shader(new ResourceLocation(RefStrings.MODID, "shaders/crescent.frag"));
	protected static final Shader swarmShader = new Shader(new ResourceLocation(RefStrings.MODID, "shaders/swarm.vert"), new ResourceLocation(RefStrings.MODID, "shaders/swarm.frag"));
	
	private static final List<NovaeEffect> novaeEffects = new CopyOnWriteArrayList<>();
	private static final List<SkyfallMeteor> skyfallMeteors = new CopyOnWriteArrayList<>();
	private static final List<SkyfallMeteor> skyfallSmoke = new CopyOnWriteArrayList<>();
	private static boolean starcoreIgnitionActive = false;
	private static long starcoreIgnitionStartWorldTime = 0L;
	private static int starcoreIgnitionDimension = Integer.MIN_VALUE;
	private static boolean starcoreDecayFlareActive = false;
	private static long starcoreDecayFlareStartWorldTime = 0L;
	private static int starcoreDecayFlareDimension = Integer.MIN_VALUE;
	private static boolean skyfallActive = false;
	private static long skyfallStartWorldTime = 0L;
	private static int skyfallDurationTicks = 0;
	private static int skyfallDimension = Integer.MIN_VALUE;
	private static long skyfallLastUpdateTick = Long.MIN_VALUE;
	private static final Random skyfallRandom = new Random();
	
	private static final Map<String, Float> ringFade = new HashMap<>();
	private static final Map<String, Long> ringFadeTick = new HashMap<>();

	private static final class NovaeEffect {
		private final long startWorldTime;
		private final int dimension;
		private final float yaw;
		private final float pitch;
		private final float roll;
		private final float r;
		private final float g;
		private final float b;
		private final float sizeScale;

		private NovaeEffect(long startWorldTime, int dimension, float yaw, float pitch, float roll, float r, float g, float b, float sizeScale) {
			this.startWorldTime = startWorldTime;
			this.dimension = dimension;
			this.yaw = yaw;
			this.pitch = pitch;
			this.roll = roll;
			this.r = r;
			this.g = g;
			this.b = b;
			this.sizeScale = sizeScale;
		}
	}

	private static final ResourceLocation[] citylights = new ResourceLocation[] {
		new ResourceLocation(RefStrings.MODID, "textures/misc/space/citylights_0.png"),
		new ResourceLocation(RefStrings.MODID, "textures/misc/space/citylights_1.png"),
		new ResourceLocation(RefStrings.MODID, "textures/misc/space/citylights_2.png"),
		new ResourceLocation(RefStrings.MODID, "textures/misc/space/citylights_3.png"),
	};

	private static final ResourceLocation defaultMask = new ResourceLocation(RefStrings.MODID, "textures/misc/space/default_mask.png");

	private static final String[] GL_SKY_LIST = new String[] { "glSkyList", "field_72771_w", "G" };
	private static final String[] GL_SKY_LIST2 = new String[] { "glSkyList2", "field_72781_x", "H" };

	public static boolean displayListsInitialized = false;
	public static int glSkyList;
	public static int glSkyList2;

	private static boolean gl13;


	public SkyProviderCelestial() {
		if(!displayListsInitialized) {
			initializeDisplayLists();
		}
	}

	private void initializeDisplayLists() {
		ContextCapabilities contextcapabilities = GLContext.getCapabilities();

		Minecraft mc = Minecraft.getMinecraft();
		glSkyList = ReflectionHelper.getPrivateValue(RenderGlobal.class, mc.renderGlobal, GL_SKY_LIST);
		glSkyList2 = ReflectionHelper.getPrivateValue(RenderGlobal.class, mc.renderGlobal, GL_SKY_LIST2);

		gl13 = contextcapabilities.OpenGL13;

		displayListsInitialized = true;
	}

	private static int lastBrightestPixel = 0;
	private static CBT_SkyState.SkyState lastSkyState = CBT_SkyState.SkyState.SUN;
	private static long lastStarcoreThroughput = 0L;

	@Override
	public void render(float partialTicks, WorldClient world, Minecraft mc) {
		// We can now guarantee that this only runs with celestial, but it doesn't hurt to be safe
		if(!(world.provider instanceof WorldProviderCelestial)) return;

		WorldProviderCelestial celestialProvider = (WorldProviderCelestial) world.provider;

		// Without mixins, we have to resort to some very wacky ways of checking that the lightmap needs to be updated
		// fortunately, thanks to torch flickering, we can just check to see if the brightest pixel has been modified
		if(lastBrightestPixel != mc.entityRenderer.lightmapColors[255] + mc.entityRenderer.lightmapColors[250]) {
			if(celestialProvider.updateLightmap(mc.entityRenderer.lightmapColors)) {
				mc.entityRenderer.lightmapTexture.updateDynamicTexture();
			}

			lastBrightestPixel = mc.entityRenderer.lightmapColors[255] + mc.entityRenderer.lightmapColors[250];
		}

		float fogIntensity = ModEventHandlerRenderer.lastFogDensity * 30;

		CelestialBody body = CelestialBody.getBody(world);
		CelestialBody sun = body.getStar();
		CBT_SkyState skyState = sun.getTrait(CBT_SkyState.class);
		CBT_SkyState.SkyState sky = skyState != null ? skyState.getState() : lastSkyState;
		if(skyState != null) {
			lastSkyState = sky;
			lastStarcoreThroughput = skyState.getStarcoreThroughput();
		}
		CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);

		boolean hasAtmosphere = atmosphere != null;

		float pressure = hasAtmosphere ? (float)atmosphere.getPressure() : 0.0F;
		float visibility = hasAtmosphere ? MathHelper.clamp_float(2.0F - pressure, 0.1F, 1.0F) : 1.0F;
		float skyDim = 1.0F;
		if(sky == CBT_SkyState.SkyState.NOTHING) {
			skyDim = 0.45F;
		} else if(sky == CBT_SkyState.SkyState.STARCORE) {
			long starcoreThroughput = skyState != null ? skyState.getStarcoreThroughput() : lastStarcoreThroughput;
			float ratio = MathHelper.clamp_float(
				(float)((double)starcoreThroughput / (double)CBT_SkyState.STARCORE_THRESHOLD_HE_PER_TICK),
				0.0F,
				1.0F
			);
			skyDim = 0.45F + (1.0F - 0.45F) * ratio;
		} else if(sky == CBT_SkyState.SkyState.SUN && skyState != null) {
			float ratio = MathHelper.clamp_float(
				(float)((double)skyState.getSunCharge() / (double)CBT_SkyState.SUN_MAX_HE),
				0.0F,
				1.0F
			);
			skyDim = 0.45F + (1.0F - 0.45F) * ratio;
		}
		visibility *= skyDim;

		GL11.glDisable(GL11.GL_TEXTURE_2D);
		Vec3 skyColor = world.getSkyColor(mc.renderViewEntity, partialTicks);

		float skyR = (float) skyColor.xCoord;
		float skyG = (float) skyColor.yCoord;
		float skyB = (float) skyColor.zCoord;

		// Diminish sky colour when leaving the atmosphere
		if(mc.renderViewEntity.posY > 300) {
			double curvature = MathHelper.clamp_float((800.0F - (float)mc.renderViewEntity.posY) / 500.0F, 0.0F, 1.0F);
			skyR *= curvature;
			skyG *= curvature;
			skyB *= curvature;
		}

		if(mc.gameSettings.anaglyph) {
			float[] anaglyphColor = applyAnaglyph(skyR, skyG, skyB);
			skyR = anaglyphColor[0];
			skyG = anaglyphColor[1];
			skyB = anaglyphColor[2];
		}

		float planetR = skyR;
		float planetG = skyG;
		float planetB = skyB;

		if(fogIntensity > 0.01F) {
			Vec3 fogColor = world.getFogColor(partialTicks);
			planetR = (float)BobMathUtil.clampedLerp(skyR, fogColor.xCoord, fogIntensity);
			planetG = (float)BobMathUtil.clampedLerp(skyG, fogColor.yCoord, fogIntensity);
			planetB = (float)BobMathUtil.clampedLerp(skyB, fogColor.zCoord, fogIntensity);
		}

		if(skyDim < 1.0F) {
			planetR *= skyDim;
			planetG *= skyDim;
			planetB *= skyDim;
		}

		Vec3 planetTint = Vec3.createVectorHelper(planetR, planetG, planetB);

		Tessellator tessellator = Tessellator.instance;

		GL11.glDepthMask(false);
		GL11.glEnable(GL11.GL_FOG);
		GL11.glColor3f(skyR, skyG, skyB);

		// Set maximum sky fog distance to 12 chunks, works nicely with Celeritas/Distant Horizons
		// and makes for a more consistent sky in vanilla too
		GL11.glPushAttrib(GL11.GL_FOG_BIT);
		{

			GL11.glFogf(GL11.GL_FOG_START, 0.0F);
			GL11.glFogf(GL11.GL_FOG_END, Math.min(12.0F, mc.gameSettings.renderDistanceChunks) * 16.0F);

			GL11.glCallList(glSkyList);

		}
		GL11.glPopAttrib();

		GL11.glDisable(GL11.GL_FOG);
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);

		GL11.glEnable(GL11.GL_BLEND);
		RenderHelper.disableStandardItemLighting();

		OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

		float starBrightness = world.getStarBrightness(partialTicks) * visibility;
		float solarAngle = world.getCelestialAngle(partialTicks);
		float siderealAngle = (float)SolarSystem.calculateSiderealAngle(world, partialTicks, body);
		if(world.provider != null && world.provider.dimensionId == SpaceConfig.kerbolDimension) {
			double t = world.getTotalWorldTime() + partialTicks;
			siderealAngle += (float)(Math.sin(t / 6000.0D * Math.PI * 2.0D) * 6.0D);
		}

		// Handle any special per-body sunset rendering
		renderSunset(partialTicks, world, mc, solarAngle, pressure, body.surfaceTexture);

		renderStars(partialTicks, world, mc, starBrightness, solarAngle + siderealAngle, body.axialTilt);

		// Render novae before sun/celestials so it appears behind them.
		renderSpecialEffects(partialTicks, world, mc);

		GL11.glPushMatrix();
		{

			GL11.glRotatef(body.axialTilt, 1.0F, 0.0F, 0.0F);
			GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
			GL11.glRotatef(solarAngle * 360.0F, 1.0F, 0.0F, 0.0F);

			// Draw DIGAMMA STAR
			renderDigamma(partialTicks, world, mc, solarAngle);

			OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);

			// Scale sun size for rendering (texture is 4 times larger than actual, for glow)
			double sunSize = SolarSystem.calculateSunSize(body) * SolarSystem.SUN_RENDER_SCALE;
			double coronaSize = sunSize * (3 - MathHelper.clamp_float(pressure, 0.0F, 1.0F));

			renderSun(partialTicks, world, mc, sun, sunSize, coronaSize, visibility, pressure);

			float blendAmount = hasAtmosphere ? MathHelper.clamp_float(1 - world.getSunBrightnessFactor(partialTicks), 0.25F, 1F) : 1F;

			renderCelestials(partialTicks, world, mc, celestialProvider.metrics, solarAngle, null, planetTint, visibility, blendAmount, null, SolarSystem.MAX_APPARENT_SIZE_SURFACE, skyState);

			GL11.glEnable(GL11.GL_BLEND);

			if(visibility > 0.2F) {
				// JEFF BOZOS WOULD LIKE TO KNOW YOUR LOCATION
				// ... to send you a pakedge :)))
				if(world.provider.dimensionId == 0) {
					Satellite.renderDefault(partialTicks, world, mc, solarAngle, 1916169, new float[] { 1.0F, 0.534F, 0.385F, 1.0F });
				}

				// Light up the sky
				for(Map.Entry<Integer, Satellite> satelliteEntry : SatelliteSavedData.getClientSats().entrySet()) {
					satelliteEntry.getValue().render(partialTicks, world, mc, solarAngle, satelliteEntry.getKey());
				}

				// Stations, too
				for(OrbitalStation station : OrbitalStation.orbitingStations) {
					renderStation(partialTicks, world, mc, station, solarAngle);
				}
			}

		}
		GL11.glPopMatrix();

		render3DModel(partialTicks, world, mc);

		CBT_War war = body.getTrait(CBT_War.class);
		if(war != null) {
			for(int i = 0; i < war.getProjectiles().size(); i++) {
				CBT_War.Projectile projectile = war.getProjectiles().get(i);
				float thing = projectile.getFlashtime() + partialTicks;

				if(projectile.getTravel() <= 0) {
					float alpd = 1.0F - Math.min(1.0F, thing / 100);

					GL11.glPushMatrix();
					{

						render3DModel(partialTicks, world, mc);

						GL11.glTranslated(projectile.getTranslateX() + 70, projectile.getTranslateY(), projectile.getTranslateZ() + 50);
						GL11.glScaled(thing, thing, thing);
						GL11.glRotated(90.0, -10.0, -1.0, 50.0);
						GL11.glRotated(20.0, -0.0, -1.0, 1.0);

						GL11.glColor4d(1, 1, 1, alpd);

						mc.renderEngine.bindTexture(shockwaveTexture);
						ResourceManager.plane.renderAll();

					}
					GL11.glPopMatrix();

					GL11.glPushMatrix();
					{

						GL11.glTranslated(projectile.getTranslateX() + 70, projectile.getTranslateY(), projectile.getTranslateZ() + 50);
						GL11.glScaled(thing * 0.4f, thing * 0.4f, thing * 0.4f);
						GL11.glRotated(90.0, -10.0, -1.0, 50.0);
						GL11.glRotated(20.0, -0.0, -1.0, 1.0);
						GL11.glColor4d(1, 1, 1, alpd);

						mc.renderEngine.bindTexture(thatmoShield);
						ResourceManager.plane.renderAll();

					}
					GL11.glPopMatrix();
				}
			}
		}

		if(body.hasRings) {
			GL11.glPushMatrix();
			{

				GL11.glRotatef(body.axialTilt - body.ringTilt, 1.0F, 0.0F, 0.0F);
				GL11.glTranslatef(0, -100, 0);
				GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);

				float ringVisibility = visibility * getRingFade(body, world);
				renderRings(partialTicks, world, mc, body.ringTilt, body.ringColor, 200, ringVisibility);

			}
			GL11.glPopMatrix();
		}

		CBT_COMPROMISED compromised = body.getTrait(CBT_COMPROMISED.class);
		if(compromised != null) {
			GL11.glPushMatrix();
			{

				float time = ((float)world.getWorldTime() + partialTicks) * 0.2F;

				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
				GL11.glDisable(GL11.GL_CULL_FACE);

				fleshShader.use();
				GL11.glScaled(194.5, 70.5, 94.5);
				GL11.glRotated(90, 0, 0, 1);

				mc.renderEngine.bindTexture(noise);
				ResourceManager.sphere_v2.renderAll();

				// Fix orbital plane
				GL11.glRotatef(-90.0F, 0, 1, 0);

				fleshShader.setUniform1f("iTime", time * 0.05F);
				fleshShader.setUniform1i("iChannel1", 0);

				fleshShader.stop();

				OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);

			}
			GL11.glPopMatrix();
		}

		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glEnable(GL11.GL_FOG);

		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glColor3f(0.0F, 0.0F, 0.0F);

		Vec3 pos = mc.thePlayer.getPosition(partialTicks);
		double heightAboveHorizon = pos.yCoord - world.getHorizon();

		if(heightAboveHorizon < 0.0D) {
			GL11.glPushMatrix();
			{

				GL11.glTranslatef(0.0F, 12.0F, 0.0F);
				GL11.glCallList(glSkyList2);

			}
			GL11.glPopMatrix();

			float f8 = 1.0F;
			float f9 = -((float) (heightAboveHorizon + 65.0D));
			float opposite = -f8;

			tessellator.startDrawingQuads();
			tessellator.setColorRGBA_I(0, 255);
			tessellator.addVertex(-f8, f9, f8);
			tessellator.addVertex(f8, f9, f8);
			tessellator.addVertex(f8, opposite, f8);
			tessellator.addVertex(-f8, opposite, f8);
			tessellator.addVertex(-f8, opposite, -f8);
			tessellator.addVertex(f8, opposite, -f8);
			tessellator.addVertex(f8, f9, -f8);
			tessellator.addVertex(-f8, f9, -f8);
			tessellator.addVertex(f8, opposite, -f8);
			tessellator.addVertex(f8, opposite, f8);
			tessellator.addVertex(f8, f9, f8);
			tessellator.addVertex(f8, f9, -f8);
			tessellator.addVertex(-f8, f9, -f8);
			tessellator.addVertex(-f8, f9, f8);
			tessellator.addVertex(-f8, opposite, f8);
			tessellator.addVertex(-f8, opposite, -f8);
			tessellator.addVertex(-f8, opposite, -f8);
			tessellator.addVertex(-f8, opposite, f8);
			tessellator.addVertex(f8, opposite, f8);
			tessellator.addVertex(f8, opposite, -f8);
			tessellator.draw();
		}

		if(world.provider.isSkyColored()) {
			GL11.glColor3f(skyR * 0.2F + 0.04F, skyG * 0.2F + 0.04F, skyB * 0.6F + 0.1F);
		} else {
			GL11.glColor3f(skyR, skyG, skyB);
		}

		GL11.glPushMatrix();
		{

			GL11.glTranslatef(0.0F, -((float) (heightAboveHorizon - 16.0D)), 0.0F);
			GL11.glCallList(glSkyList2);

		}
		GL11.glPopMatrix();

		double sc = 1 / (pos.yCoord / 1000);
		double uvOffset = (pos.xCoord / 1024) % 1;
		GL11.glPushMatrix();
		{

			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glDisable(GL11.GL_ALPHA_TEST);
			GL11.glDisable(GL11.GL_FOG);
			GL11.glEnable(GL11.GL_BLEND);

			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

			float sunBrightness = (sky == CBT_SkyState.SkyState.BLACKHOLE || sky == CBT_SkyState.SkyState.STARCORE)
				? 0.0F
				: world.getSunBrightness(partialTicks);

			GL11.glColor4f(sunBrightness, sunBrightness, sunBrightness, ((float)pos.yCoord - 200.0F) / 300.0F);
			mc.renderEngine.bindTexture(body.texture);
			GL11.glRotated(180, 1, 0, 0);

			tessellator.startDrawingQuads();
			tessellator.addVertexWithUV(-115 * sc, 100.0D, -115 * sc, 0.0D + uvOffset, 0.0D);
			tessellator.addVertexWithUV(115 * sc, 100.0D, -115 * sc, 1.0D + uvOffset, 0.0D);
			tessellator.addVertexWithUV(115 * sc, 100.0D, 115 * sc, 1.0D + uvOffset, 1.0D);
			tessellator.addVertexWithUV(-115 * sc, 100.0D, 115 * sc, 0.0D + uvOffset, 1.0D);
			tessellator.draw();

			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glEnable(GL11.GL_ALPHA_TEST);
			GL11.glEnable(GL11.GL_FOG);
			GL11.glDisable(GL11.GL_BLEND);

			OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);

		}

		GL11.glPopMatrix();
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDepthMask(true);

	}

	protected void renderSunset(float partialTicks, WorldClient world, Minecraft mc, float solarAngle, float pressure, ResourceLocation surfaceTexture) {
		Tessellator tessellator = Tessellator.instance;

		float[] sunsetColor = calcSunriseSunsetColors(partialTicks, world, mc, solarAngle, pressure);

		if(sunsetColor != null) {
			float[] anaglyphColor = mc.gameSettings.anaglyph ? applyAnaglyph(sunsetColor) : sunsetColor;
			float sunsetDirection = MathHelper.sin(world.getCelestialAngleRadians(partialTicks)) < 0.0F ? 180.0F : 0.0F;

			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glShadeModel(GL11.GL_SMOOTH);

			GL11.glPushMatrix();
			{

				GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
				GL11.glRotatef(sunsetDirection, 0.0F, 0.0F, 1.0F);
				GL11.glRotatef(90.0F, 0.0F, 0.0F, 1.0F);

				tessellator.startDrawing(6);
				tessellator.setColorRGBA_F(anaglyphColor[0], anaglyphColor[1], anaglyphColor[2], sunsetColor[3]);
				tessellator.addVertex(0.0, 100.0, 0.0);
				tessellator.setColorRGBA_F(sunsetColor[0], sunsetColor[1], sunsetColor[2], 0.0F);
				byte segments = 16;

				for(int j = 0; j <= segments; ++j) {
					float angle = (float)j * 3.1415927F * 2.0F / (float)segments;
					float sinAngle = MathHelper.sin(angle);
					float cosAngle = MathHelper.cos(angle);
					tessellator.addVertex((double)(sinAngle * 120.0F), (double)(cosAngle * 120.0F), (double)(-cosAngle * 40.0F * sunsetColor[3]));
				}

				tessellator.draw();

			}
			GL11.glPopMatrix();
			GL11.glShadeModel(GL11.GL_FLAT);
			GL11.glEnable(GL11.GL_TEXTURE_2D);

			// charged dust
			if(pressure < 0.05F) {
				Random rand = new Random(0);

				GL11.glPushMatrix();
				{

					double time = ((double)world.provider.getWorldTime() + partialTicks) * 0.002;

					GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
					GL11.glRotatef(sunsetDirection, 0.0F, 0.0F, 1.0F);
					GL11.glRotatef(90.0F, 0.0F, 0.0F, 1.0F);

					mc.renderEngine.bindTexture(surfaceTexture);
					GL11.glColor4f(0.5F + rand.nextFloat() * 0.5F, 0.5F + rand.nextFloat() * 0.5F, 0.5F + rand.nextFloat() * 0.5F, rand.nextFloat() * sunsetColor[3] * 4.0F);

					OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_CONSTANT_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

					tessellator.startDrawing(GL11.GL_POINTS);
					for(int i = 0; i < 1024; i++) {
						tessellator.addVertex(rand.nextGaussian() * 50, 100, -((Math.abs(rand.nextGaussian() * 20) + time) % Math.abs(rand.nextGaussian()) * 20));
					}
					tessellator.draw();

				}
				GL11.glPopMatrix();
			}
		}
	}

	// We don't want certain sunrise/sunset effects to change the fog colour, so we do them here
	protected float[] calcSunriseSunsetColors(float partialTicks, WorldClient world, Minecraft mc, float solarAngle, float pressure) {
		if(pressure < 0.05F) {
			float cutoff = 0.4F;
			float angle = MathHelper.cos(solarAngle * (float)Math.PI * 2.0F) - 0.0F;

			if(angle < -cutoff || angle > cutoff) return null;

			float colorIntensity = angle / cutoff * 0.5F + 0.5F;
			float alpha = 1.0F - (1.0F - MathHelper.sin(colorIntensity * (float)Math.PI)) * 0.99F;
			alpha *= alpha;
			return new float[] { 0.9F, 1.0F, 1.0F, alpha * 0.2F };
		}

		return world.provider.calcSunriseSunsetColors(world.getCelestialAngle(partialTicks), partialTicks);
	}

	protected void renderStars(float partialTicks, WorldClient world, Minecraft mc, float starBrightness, float siderealAngle, float axialTilt) {
		Tessellator tessellator = Tessellator.instance;

		if(starBrightness > 0.0F) {
			GL11.glPushMatrix();
			{
				GL11.glRotatef(axialTilt, 1.0F, 0.0F, 0.0F);

				mc.renderEngine.bindTexture(nightTexture);

				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

				float starBrightnessAlpha = starBrightness * 0.6f;
				GL11.glColor4f(1.0F, 1.0F, 1.0F, starBrightnessAlpha);

				GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);

				GL11.glRotatef(siderealAngle * 360.0F, 1.0F, 0.0F, 0.0F);
				GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
				GL11.glColor4f(1.0F, 1.0F, 1.0F, starBrightnessAlpha);

				GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
				GL11.glRotatef(-90.0F, 0.0F, 0.0F, 1.0F);
				renderSkyboxSide(tessellator, 4);

				GL11.glPushMatrix();
				GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
				renderSkyboxSide(tessellator, 1);
				GL11.glPopMatrix();

				GL11.glPushMatrix();
				GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
				renderSkyboxSide(tessellator, 0);
				GL11.glPopMatrix();

				GL11.glRotatef(90.0F, 0.0F, 0.0F, 1.0F);
				renderSkyboxSide(tessellator, 5);

				GL11.glRotatef(90.0F, 0.0F, 0.0F, 1.0F);
				renderSkyboxSide(tessellator, 2);

				GL11.glRotatef(90.0F, 0.0F, 0.0F, 1.0F);
				renderSkyboxSide(tessellator, 3);

				OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

			}
			GL11.glPopMatrix();
		}
	}

	protected void renderSun(float partialTicks, WorldClient world, Minecraft mc, CelestialBody sun, double sunSize, double coronaSize, float visibility, float pressure) {
		Tessellator tessellator = Tessellator.instance;

		CBT_Dyson dyson = sun.getTrait(CBT_Dyson.class);
		int swarmCount = dyson != null ? dyson.size() : 0;
		CBT_SkyState skyState = sun.getTrait(CBT_SkyState.class);
		CBT_SkyState.SkyState sky = skyState != null ? skyState.getState() : lastSkyState;
		float sunChargeRatio = (sky == CBT_SkyState.SkyState.SUN && skyState != null)
			? MathHelper.clamp_float((float)((double)skyState.getSunCharge() / (double)CBT_SkyState.SUN_MAX_HE), 0.0F, 1.0F)
			: 1.0F;
		float sunDecayProgress = 1.0F - sunChargeRatio;
		float sunScale = 0.35F + 0.65F * sunChargeRatio;
		double renderSunSize = sunSize * sunScale;
		double renderCoronaSize = coronaSize * sunScale;

		if(sky == CBT_SkyState.SkyState.STARCORE) {
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			long starcoreThroughput = skyState != null ? skyState.getStarcoreThroughput() : lastStarcoreThroughput;
			float ratio = MathHelper.clamp_float((float)((double) starcoreThroughput / (double) CBT_SkyState.STARCORE_THRESHOLD_HE_PER_TICK), 0.0F, 1.0F);
			float hueSpeed = 0.01F + 0.04F * ratio;
			float hue = ((world.getWorldTime() + partialTicks) * hueSpeed + ratio * 0.25F) % 1.0F;
			int rgb = Color.HSBtoRGB(hue, 1.0F, 1.0F);
			float r = ((rgb >> 16) & 0xFF) / 255.0F;
			float g = ((rgb >> 8) & 0xFF) / 255.0F;
			float b = (rgb & 0xFF) / 255.0F;

			GL11.glColor4f(r, g, b, 1.0F);
			mc.renderEngine.bindTexture(starcoreCoreTexture);

			double starcoreCoreSize = 0.1D + 1.25D * ratio;
			tessellator.startDrawingQuads();
			tessellator.addVertexWithUV(-starcoreCoreSize, 100.0D, -starcoreCoreSize, 0.0D, 0.0D);
			tessellator.addVertexWithUV(starcoreCoreSize, 100.0D, -starcoreCoreSize, 1.0D, 0.0D);
			tessellator.addVertexWithUV(starcoreCoreSize, 100.0D, starcoreCoreSize, 1.0D, 1.0D);
			tessellator.addVertexWithUV(-starcoreCoreSize, 100.0D, starcoreCoreSize, 0.0D, 1.0D);
			tessellator.draw();

			if(ratio > 0.0F) {
				double starcoreSpikeBase = 0.2D + 2.5D * ratio;
				double spikeSize = starcoreSpikeBase * (1.5D + 3.0D * ratio);
				float spikeAlpha = MathHelper.clamp_float(0.15F + 0.85F * ratio, 0.0F, 1.0F);
				GL11.glColor4f(1.0F, 1.0F, 1.0F, spikeAlpha);
				mc.renderEngine.bindTexture(starcoreSpikeTexture);

				GL11.glDisable(GL11.GL_DEPTH_TEST);
				tessellator.startDrawingQuads();
				tessellator.addVertexWithUV(-spikeSize, 100.0D, -spikeSize, 0.0D, 0.0D);
				tessellator.addVertexWithUV(spikeSize, 100.0D, -spikeSize, 1.0D, 0.0D);
				tessellator.addVertexWithUV(spikeSize, 100.0D, spikeSize, 1.0D, 1.0D);
				tessellator.addVertexWithUV(-spikeSize, 100.0D, spikeSize, 0.0D, 1.0D);
				tessellator.draw();
				GL11.glEnable(GL11.GL_DEPTH_TEST);
			}
			renderStarcoreDecayFlareEffect(partialTicks, world, mc, sunSize);
			return;
		}

		renderStarcoreDecayFlareEffect(partialTicks, world, mc, sunSize);

		if(sky == CBT_SkyState.SkyState.NOTHING || sky == CBT_SkyState.SkyState.STARCORE) {
			return;
		}

		boolean isBlackhole = sky == CBT_SkyState.SkyState.BLACKHOLE;

		if(isBlackhole && sun.shader != null) {
			// BLACK HOLE SUN
			// WON'T YOU COME
			// AND WASH AWAY THE RAIN

			Shader shader = sun.shader;
			double shaderSize = sunSize * sun.shaderScale;

			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

			shader.use();

			// Keep time in a tight range to avoid float precision jitter after large /time adds.
			long timeTicks = world.getWorldTime() % 24000L;
			float time = ((float)timeTicks + partialTicks) / 20.0F;

			mc.renderEngine.bindTexture(noise);
			GL11.glPushMatrix();

			// Fix orbital plane
			GL11.glRotatef(-90.0F, 0, 1, 0);
			shader.setUniform1f("iTime", time);
			shader.setUniform1i("iChannel1", 0);

			tessellator.startDrawingQuads();
			tessellator.addVertexWithUV(-shaderSize, 100.0D, -shaderSize, 0.0D, 0.0D);
			tessellator.addVertexWithUV(shaderSize, 100.0D, -shaderSize, 1.0D, 0.0D);
			tessellator.addVertexWithUV(shaderSize, 100.0D, shaderSize, 1.0D, 1.0D);
			tessellator.addVertexWithUV(-shaderSize, 100.0D, shaderSize, 0.0D, 1.0D);
			tessellator.draw();

			shader.stop();

			GL11.glPopMatrix();

			OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);
		} else if(!isBlackhole) {
			// Depth-only blanking to conceal stars behind the sun.
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glColorMask(false, false, false, false);
			GL11.glDepthMask(true);

			// Depth-only mask. Keep it behind the sun (99.9D) so it doesn't
			// occlude the actual sun quad at 100.0D.
			tessellator.startDrawingQuads();
			tessellator.addVertexWithUV(-renderSunSize * 0.25D, 99.9D, -renderSunSize * 0.25D, 0.0D, 0.0D);
			tessellator.addVertexWithUV( renderSunSize * 0.25D, 99.9D, -renderSunSize * 0.25D, 1.0D, 0.0D);
			tessellator.addVertexWithUV( renderSunSize * 0.25D, 99.9D,  renderSunSize * 0.25D, 1.0D, 1.0D);
			tessellator.addVertexWithUV(-renderSunSize * 0.25D, 99.9D,  renderSunSize * 0.25D, 0.0D, 1.0D);
			tessellator.draw();

			GL11.glColorMask(true, true, true, true);
			GL11.glDepthMask(false);

			// Draw the sun without depth testing so the depth-only mask doesn't hide it.
			GL11.glDisable(GL11.GL_DEPTH_TEST);
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glColor4f(1.0F, 1.0F, 1.0F, visibility);

			ResourceLocation sunTexture = SolarSystem.kerbol.texture;
			if(sun != null) {
				boolean isKerbol = sun == SolarSystem.kerbol || "kerbol".equals(sun.name);
				if(isKerbol) {
					sunTexture = defaultSunTexture;
				} else if(sun.texture != null) {
					sunTexture = sun.texture;
				}
			}
			mc.renderEngine.bindTexture(sunTexture);

			float[] sunColor = world.provider instanceof WorldProviderCelestial
				? ((WorldProviderCelestial) world.provider).getSunColor()
				: new float[] { 1.0F, 1.0F, 1.0F };

			if(sunDecayProgress > 0.0F) {
				float t = MathHelper.clamp_float(sunDecayProgress, 0.0F, 1.0F);
				float targetR = 1.0F;
				float targetG = 0.2F;
				float targetB = 0.2F;
				sunColor = new float[] {
					sunColor[0] + (targetR - sunColor[0]) * t,
					sunColor[1] + (targetG - sunColor[1]) * t,
					sunColor[2] + (targetB - sunColor[2]) * t
				};
			}

			GL11.glColor4f(sunColor[0], sunColor[1], sunColor[2], visibility);
			tessellator.startDrawingQuads();
			tessellator.addVertexWithUV(-renderSunSize, 100.0D, -renderSunSize, 0.0D, 0.0D);
			tessellator.addVertexWithUV(renderSunSize, 100.0D, -renderSunSize, 1.0D, 0.0D);
			tessellator.addVertexWithUV(renderSunSize, 100.0D, renderSunSize, 1.0D, 1.0D);
			tessellator.addVertexWithUV(-renderSunSize, 100.0D, renderSunSize, 0.0D, 1.0D);
			tessellator.draw();

			GL11.glEnable(GL11.GL_DEPTH_TEST);

			// Draw a big ol' spiky flare! Less so when there is an atmosphere
			// Render on top of the sun regardless of depth test mode.
			GL11.glDisable(GL11.GL_DEPTH_TEST);
			GL11.glColor4f(sunColor[0], sunColor[1], sunColor[2], 1 - MathHelper.clamp_float(pressure, 0.0F, 1.0F) * 0.75F);
			mc.renderEngine.bindTexture(flareTexture);

			tessellator.startDrawingQuads();
			tessellator.addVertexWithUV(-renderCoronaSize, 99.9D, -renderCoronaSize, 0.0D, 0.0D);
			tessellator.addVertexWithUV(renderCoronaSize, 99.9D, -renderCoronaSize, 1.0D, 0.0D);
			tessellator.addVertexWithUV(renderCoronaSize, 99.9D, renderCoronaSize, 1.0D, 1.0D);
			tessellator.addVertexWithUV(-renderCoronaSize, 99.9D, renderCoronaSize, 0.0D, 1.0D);
			tessellator.draw();
			GL11.glEnable(GL11.GL_DEPTH_TEST);

			renderStarcoreIgnitionEffect(partialTicks, world, mc, renderSunSize);
			renderStarcoreDecayFlareEffect(partialTicks, world, mc, renderSunSize);

			// Draw the swarm members with depth occlusion
			// We do this last so we can render transparency against the sun
			if(swarmCount > 0) {
				renderSwarm(partialTicks, world, mc, renderSunSize * 0.5, swarmCount);
			}

			// Clear and disable the depth buffer once again, buffer has to be writable to clear it
			GL11.glDepthMask(true);
			GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
			GL11.glDepthMask(false);
		}
	}

	private void renderSwarm(float partialTicks, WorldClient world, Minecraft mc, double swarmRadius, int swarmCount) {
		Tessellator tessellator = Tessellator.instance;

		// bloodseeking, parasitic, ecstatically tracing decay
		// thriving in the glow that death emits, the warm perfume it radiates

		swarmShader.use();

		// swarm members render as pixels, which can vary based on screen resolution
		// because of this, we make the pixels more transparent based on their apparent size, which varies by a fair few factors
		// this isn't a foolproof solution, analyzing the projection matrices would be best, but it works for now.
		float swarmScreenSize = (float)((mc.displayHeight / mc.gameSettings.fovSetting) * swarmRadius * 0.002);
		float time = ((float)world.getWorldTime() + partialTicks) / 800.0F;

		swarmShader.setUniform1f("iTime", time);

		int offsetLocation = swarmShader.getUniformLocation("iOffset");

		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glColor4f(0.0F, 0.0F, 0.0F, MathHelper.clamp_float(swarmScreenSize, 0, 1));

		GL11.glPushMatrix();
		{

			GL11.glTranslatef(0.0F, 100.0F, 0.0F);
			GL11.glScaled(swarmRadius, swarmRadius, swarmRadius);

			GL11.glPushMatrix();
			{

				GL11.glRotatef(80.0F, 1, 0, 0);

				tessellator.startDrawing(GL11.GL_POINTS);
				for(int i = 0; i < swarmCount; i += 3) {
					swarmShader.setUniform1f(offsetLocation, i);

					float t = i + time;
					double x = Math.cos(t);
					double z = Math.sin(t);

					tessellator.addVertex(x, 0, z);
				}
				tessellator.draw();

			}
			GL11.glPopMatrix();

			GL11.glPushMatrix();
			{

				GL11.glRotatef(60.0F, 0, 1, 0);
				GL11.glRotatef(80.0F, 1, 0, 0);

				tessellator.startDrawing(GL11.GL_POINTS);
				for(int i = 1; i < swarmCount; i += 3) {
					swarmShader.setUniform1f(offsetLocation, i);

					float t = i + time;
					double x = Math.cos(t);
					double z = Math.sin(t);

					tessellator.addVertex(x, 0, z);
				}
				tessellator.draw();

			}
			GL11.glPopMatrix();

			GL11.glPushMatrix();
			{

				GL11.glRotatef(-60.0F, 0, 1, 0);
				GL11.glRotatef(80.0F, 1, 0, 0);

				tessellator.startDrawing(GL11.GL_POINTS);
				for(int i = 2; i < swarmCount; i += 3) {
					swarmShader.setUniform1f(offsetLocation, i);

					float t = i + time;
					double x = Math.cos(t);
					double z = Math.sin(t);

					tessellator.addVertex(x, 0, z);
				}
				tessellator.draw();

			}
			GL11.glPopMatrix();

		}
		GL11.glPopMatrix();

		swarmShader.stop();

		OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);
	}

	protected void renderCelestials(float partialTicks, WorldClient world, Minecraft mc, List<AstroMetric> metrics, float solarAngle, CelestialBody tidalLockedBody, Vec3 planetTint, float visibility, float blendAmount, CelestialBody orbiting, float maxSize, CBT_SkyState skyState) {
		Tessellator tessellator = Tessellator.instance;
		final float bodyVisibility = visibility;
		float blendDarken = 0.1F;
		final float redOverlayAlpha = 0.0F;

		double transitionMinSize = 0.1D;
		double transitionMaxSize = 0.5D;

		double maxDistance = 0.0D;
		for(AstroMetric metric : metrics) {
			if(metric.distance > maxDistance) {
				maxDistance = metric.distance;
			}
		}
		if(maxDistance <= 0.0D) {
			maxDistance = 1.0D;
		}

		for(AstroMetric metric : metrics) {

			// Ignore self
			if(metric.distance == 0)
				continue;

			boolean orbitingThis = metric.body == orbiting;

			double uvOffset = orbitingThis ? 1 - ((((double)world.getWorldTime() + partialTicks) / 1024) % 1) : 0;
			float axialTilt = orbitingThis ? 0 : metric.body.axialTilt;
			Vec3 bodyTextureTint = getBodyTextureTint(metric.body);

			int injectorCount = 0;
			if(skyState != null && metric.body != null && metric.body.canLand) {
				injectorCount = skyState.getInjectorCount(metric.body.dimensionId);
			}
			if(injectorCount > 0 && bodyVisibility > 0.05F) {
				float distanceFactor = (float)MathHelper.clamp_double(metric.distance / maxDistance, 0.0D, 1.0D);
				float lineWidth = MathHelper.clamp_float((2.5F - 1.75F * distanceFactor) * 1.05F, 1.8F, 9.0F);
				float time = (float)(world.getTotalWorldTime() + partialTicks);
				float spinAngle = (float)(((world.getTotalWorldTime() + partialTicks) / 1024.0D) * Math.PI * 2.0D);
				renderInjectorLines(tessellator, metric, axialTilt, injectorCount, bodyVisibility, lineWidth, time, spinAngle);
			}

			GL11.glPushMatrix();
			{

				double size = MathHelper.clamp_double(metric.apparentSize, 0, maxSize);
				boolean renderPoint = size < transitionMaxSize;
				boolean renderBody = size > transitionMinSize;

				GL11.glRotated(metric.angle, 1.0, 0.0, 0.0);
				GL11.glRotated(metric.inclination, 0.0, 0.0, 1.0);
				GL11.glRotatef(axialTilt + 90.0F, 0.0F, 1.0F, 0.0F);

				if(renderBody) {
					// Draw the back half of the ring (obscured by body)
					if(metric.body.hasRings) {
						GL11.glPushMatrix();
						{

							float ringVisibility = visibility * getRingFade(metric.body, world);
							GL11.glColor4f(metric.body.ringColor[0], metric.body.ringColor[1], metric.body.ringColor[2], ringVisibility);
							mc.renderEngine.bindTexture(ringTexture);

							GL11.glDisable(GL11.GL_CULL_FACE);

							double ringSize = size * metric.body.ringSize;

							GL11.glTranslatef(0.0F, 100.0F, 0.0F);
							GL11.glRotated(-metric.angle, 0, 0, 1);
							GL11.glRotatef(90.0F - metric.body.ringTilt, 1, 0, 0);
							GL11.glRotated(metric.angle, 0, 1, 0);

							tessellator.startDrawingQuads();
							tessellator.addVertexWithUV(-ringSize, 0, -ringSize, 0.0D, 0.0D);
							tessellator.addVertexWithUV(ringSize, 0, -ringSize, 1.0D, 0.0D);
							tessellator.addVertexWithUV(ringSize, 0, 0, 1.0D, 0.5D);
							tessellator.addVertexWithUV(-ringSize, 0, 0, 0.0D, 0.5D);
							tessellator.draw();

							GL11.glEnable(GL11.GL_CULL_FACE);

						}
						GL11.glPopMatrix();
					}

					CBT_Destroyed d = metric.body.getTrait(CBT_Destroyed.class);

					if(d != null) {
						// Stop calling things "interp", that's a verb not a noun
						double interpr = d.interp + size * 0.5;

						float alpd = (float) (1.0F - Math.min(1.0F, interpr / 100));
						Random random = new Random(12);

						int numQuads = 30;
						for (int i = 0; i < numQuads; i++) {
							double radius = (random.nextDouble() * size) * d.interp;

							double randomTheta = random.nextDouble() * Math.PI * 2;
							double randomPhi = random.nextDouble() * Math.PI;

							double randomX = radius * Math.sin(randomPhi) * Math.cos(randomTheta) * 0.7;
							double randomY = radius * Math.sin(randomPhi) * Math.sin(randomTheta);
							double randomZ = radius * Math.cos(randomPhi) * 0.7;

							float randomRotation = random.nextFloat() * 360.0F;


							double uMin = random.nextDouble();
							double vMin = random.nextDouble();
							double uMax = Math.min(uMin + (random.nextDouble() * 0.2), 1.0);
							double vMax = Math.min(vMin + (random.nextDouble() * 0.2), 1.0);

							GL11.glPushMatrix();
							{

								GL11.glTranslated(randomX * -0.05, randomY * 0.00, randomZ * -0.05);

								GL11.glRotatef(randomRotation * d.interp * 0.05F, 0.0F, 1.0F, 0.0F);

								mc.renderEngine.bindTexture(metric.body.texture);
								GL11.glColor4d(bodyTextureTint.xCoord, bodyTextureTint.yCoord, bodyTextureTint.zCoord, bodyVisibility);

								tessellator.startDrawingQuads();
								double qsize = size * random.nextDouble() * 0.1;
								tessellator.addVertexWithUV(-qsize, 100.0D, -qsize, uMin, vMin);
								tessellator.addVertexWithUV(qsize, 100.0D, -qsize, uMax, vMin);
								tessellator.addVertexWithUV(qsize, 100.0D, qsize, uMax, vMax);
								tessellator.addVertexWithUV(-qsize, 100.0D, qsize, uMin, vMax);

								tessellator.draw();
							}

							GL11.glPopMatrix();
							GL11.glPushMatrix();
							{

								GL11.glTranslated(randomX * 0.04, randomY * 0.00, randomZ * 0.04);

								GL11.glRotatef(randomRotation * d.interp * 0.05F, 0.0F, 1.0F, 0.0F);
								mc.renderEngine.bindTexture(destroyedBody);
								GL11.glColor4d(1, 1, 1, bodyVisibility);
								tessellator.startDrawingQuads();
								double qsize = size * random.nextDouble() * 0.07;
								tessellator.addVertexWithUV(-qsize, 100.0D, -qsize, uMin, vMin);
								tessellator.addVertexWithUV(qsize, 100.0D, -qsize, uMax, vMin);
								tessellator.addVertexWithUV(qsize, 100.0D, qsize, uMax, vMax);
								tessellator.addVertexWithUV(-qsize, 100.0D, qsize, uMin, vMax);

								tessellator.draw();

							}
							GL11.glPopMatrix();

						}


						GL11.glColor4f(1.0F, 1.0F, 1.0F, alpd * bodyVisibility);
						mc.renderEngine.bindTexture(shockwaveTexture);
						double interpe = (d.interp * 0.5) * size * 0.1;
						tessellator.startDrawingQuads();
						tessellator.addVertexWithUV(-interpe, 100.0D, -interpe, 0.0D + uvOffset, 0.0D);
						tessellator.addVertexWithUV(interpe, 100.0D, -interpe, 1.0D + uvOffset, 0.0D);
						tessellator.addVertexWithUV(interpe, 100.0D, interpe, 1.0D + uvOffset, 1.0D);
						tessellator.addVertexWithUV(-interpe, 100.0D, interpe, 0.0D + uvOffset, 1.0D);
						tessellator.draw();


						if(!"minmus".equals(metric.body.name)) {
							GL11.glColor4f(1.0F, 1.0F, 1.0F, alpd * 2 * bodyVisibility);
							mc.renderEngine.bindTexture(shockFlareTexture);

							interpr = size * 3;
							tessellator.startDrawingQuads();
							tessellator.addVertexWithUV(-interpr, 100.0D, -interpr, 0.0D + uvOffset, 0.0D);
							tessellator.addVertexWithUV(interpr, 100.0D, -interpr, 1.0D + uvOffset, 0.0D);
							tessellator.addVertexWithUV(interpr, 100.0D, interpr, 1.0D + uvOffset, 1.0D);
							tessellator.addVertexWithUV(-interpr, 100.0D, interpr, 0.0D + uvOffset, 1.0D);
							tessellator.draw();
						}

					} else {

						GL11.glDisable(GL11.GL_BLEND);
						GL11.glColor4f((float)bodyTextureTint.xCoord * bodyVisibility, (float)bodyTextureTint.yCoord * bodyVisibility, (float)bodyTextureTint.zCoord * bodyVisibility, bodyVisibility);
						mc.renderEngine.bindTexture(metric.body.texture);

						tessellator.startDrawingQuads();
						tessellator.addVertexWithUV(-size, 100.0D, -size, 0.0D + uvOffset, 0.0D);
						tessellator.addVertexWithUV(size, 100.0D, -size, 1.0D + uvOffset, 0.0D);
						tessellator.addVertexWithUV(size, 100.0D, size, 1.0D + uvOffset, 1.0D);
						tessellator.addVertexWithUV(-size, 100.0D, size, 0.0D + uvOffset, 1.0D);
						tessellator.draw();

						float cloudAlpha = getCloudAlpha(metric.body) * bodyVisibility;
						if(cloudAlpha > 0.001F) {
							double cloudUvOffset = uvOffset + getCloudUvOffset(world, partialTicks, metric.body);

							GL11.glEnable(GL11.GL_BLEND);
							OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

							GL11.glColor4f(1.0F, 1.0F, 1.0F, cloudAlpha);
							mc.renderEngine.bindTexture(cloudsTexture);

							tessellator.startDrawingQuads();
							tessellator.addVertexWithUV(-size, 100.0D, -size, 0.0D + cloudUvOffset, 0.0D);
							tessellator.addVertexWithUV(size, 100.0D, -size, 1.0D + cloudUvOffset, 0.0D);
							tessellator.addVertexWithUV(size, 100.0D, size, 1.0D + cloudUvOffset, 1.0D);
							tessellator.addVertexWithUV(-size, 100.0D, size, 0.0D + cloudUvOffset, 1.0D);
							tessellator.draw();

							GL11.glDisable(GL11.GL_BLEND);
						}


						CBT_Impact impact = metric.body.getTrait(CBT_Impact.class);
						CBT_Lights light = metric.body.getTrait(CBT_Lights.class);

						double impactTime = impact != null ? (world.getTotalWorldTime() - impact.time) + partialTicks : 0;
						int lightIntensity = light != null && impactTime < 40 ? light.getIntensity() : 0;

						int blackoutInterval = 8;
						int maxBlackouts = 5;

						int activeBlackouts = Math.min((int)(impactTime / blackoutInterval), maxBlackouts);

						GL11.glEnable(GL11.GL_BLEND);
						// Draw a shader on top to render celestial phase
						OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

						GL11.glColor4f(1.0F, 1.0F, 1.0F, bodyVisibility);

						planetShader.use();
						planetShader.setUniform1f("phase", (float)-metric.phase);
						planetShader.setUniform1f("offset", (float)uvOffset);
						planetShader.setUniform1i("lights", 0);
						planetShader.setUniform1i("cityMask", 1);
						planetShader.setUniform1i("blackouts", activeBlackouts);

						mc.renderEngine.bindTexture(citylights[lightIntensity]);
						if(gl13) {
							GL13.glActiveTexture(GL13.GL_TEXTURE1);
							mc.renderEngine.bindTexture(metric.body.cityMask != null ? metric.body.cityMask : defaultMask);
							GL13.glActiveTexture(GL13.GL_TEXTURE0);
						}

						tessellator.startDrawingQuads();
						tessellator.addVertexWithUV(-size, 100.0D, -size, 0.0D, 0.0D);
						tessellator.addVertexWithUV(size, 100.0D, -size, 1.0D, 0.0D);
						tessellator.addVertexWithUV(size, 100.0D, size, 1.0D, 1.0D);
						tessellator.addVertexWithUV(-size, 100.0D, size, 0.0D, 1.0D);
						tessellator.draw();

						GL11.glEnable(GL11.GL_TEXTURE_2D);

						planetShader.stop();

						OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);

						if(impact != null) {
							double lavaAlpha = Math.min(impactTime * 0.1, 1.0);

							double impactSize = (impactTime * 0.1) * size * 0.035;
							double impactAlpha = 1.0 - Math.min(1.0, impactTime * 0.0015);
							double flareSize = size * 1.5;
							double flareAlpha = 1.0 - Math.min(1.0, impactTime * 0.002);

							if(lavaAlpha > 0) {
								GL11.glColor4d(1.0, 1.0, 1.0, lavaAlpha * bodyVisibility);
								mc.renderEngine.bindTexture(impactTexture);

								tessellator.startDrawingQuads();
								tessellator.addVertexWithUV(-size, 100.0D, -size, 0.0D + uvOffset, 0.0D);
								tessellator.addVertexWithUV(size, 100.0D, -size, 1.0D + uvOffset, 0.0D);
								tessellator.addVertexWithUV(size, 100.0D, size, 1.0D + uvOffset, 1.0D);
								tessellator.addVertexWithUV(-size, 100.0D, size, 0.0D + uvOffset, 1.0D);
								tessellator.draw();
							}

							GL11.glPushMatrix();
							{

								GL11.glTranslated(-size * 0.5, 0, size * 0.4);

								// impact shockwave, increases in size and fades out
								if(impactAlpha > 0) {
									GL11.glColor4d(1.0, 1.0, 1.0F, impactAlpha * bodyVisibility);
									mc.renderEngine.bindTexture(shockwaveTexture);

									tessellator.startDrawingQuads();
									tessellator.addVertexWithUV(-impactSize, 100.0D, -impactSize, 0.0D, 0.0D);
									tessellator.addVertexWithUV(impactSize, 100.0D, -impactSize, 1.0D, 0.0D);
									tessellator.addVertexWithUV(impactSize, 100.0D, impactSize, 1.0D, 1.0D);
									tessellator.addVertexWithUV(-impactSize, 100.0D, impactSize, 0.0D, 1.0D);
									tessellator.draw();
								}

								// impact flare, remains static in size and fades out
								if(flareAlpha > 0) {
									GL11.glColor4d(1.0F, 1.0F, 1.0F, flareAlpha * bodyVisibility);
									mc.renderEngine.bindTexture(shockFlareTexture);

									tessellator.startDrawingQuads();
									tessellator.addVertexWithUV(-flareSize, 100.0D, -flareSize, 0.0D, 0.0D);
									tessellator.addVertexWithUV(flareSize, 100.0D, -flareSize, 1.0D, 0.0D);
									tessellator.addVertexWithUV(flareSize, 100.0D, flareSize, 1.0D, 1.0D);
									tessellator.addVertexWithUV(-flareSize, 100.0D, flareSize, 0.0D, 1.0D);
									tessellator.draw();
								}

							}
							GL11.glPopMatrix();
						}


						GL11.glDisable(GL11.GL_TEXTURE_2D);

						// Extra red overlay to ensure the tint is visible in all skies.
						GL11.glColor4f(1.0F, 0.5F, 0.5F, redOverlayAlpha * bodyVisibility);
						tessellator.startDrawingQuads();
						tessellator.addVertexWithUV(-size, 100.0D, -size, 0.0D, 0.0D);
						tessellator.addVertexWithUV(size, 100.0D, -size, 1.0D, 0.0D);
						tessellator.addVertexWithUV(size, 100.0D, size, 1.0D, 1.0D);
						tessellator.addVertexWithUV(-size, 100.0D, size, 0.0D, 1.0D);
						tessellator.draw();

						// Draw another layer on top to blend with the atmosphere
						GL11.glColor4d(planetTint.xCoord - blendDarken, planetTint.yCoord - blendDarken, planetTint.zCoord - blendDarken, (1 - blendAmount * visibility));

						tessellator.startDrawingQuads();
						tessellator.addVertexWithUV(-size, 100.0D, -size, 0.0D, 0.0D);
						tessellator.addVertexWithUV(size, 100.0D, -size, 1.0D, 0.0D);
						tessellator.addVertexWithUV(size, 100.0D, size, 1.0D, 1.0D);
						tessellator.addVertexWithUV(-size, 100.0D, size, 0.0D, 1.0D);
						tessellator.draw();

						GL11.glEnable(GL11.GL_TEXTURE_2D);
					}




					// Draw the front half of the ring (unobscured)
					if(metric.body.hasRings) {
						float ringVisibility = visibility * getRingFade(metric.body, world);
						GL11.glColor4f(metric.body.ringColor[0], metric.body.ringColor[1], metric.body.ringColor[2], ringVisibility);
						mc.renderEngine.bindTexture(ringTexture);

						double ringSize = size * metric.body.ringSize;

						GL11.glDisable(GL11.GL_CULL_FACE);

						GL11.glTranslatef(0.0F, 100.0F, 0.0F);
						GL11.glRotated(-metric.angle, 0, 0, 1);
						GL11.glRotatef(90.0F - metric.body.ringTilt, 1, 0, 0);
						GL11.glRotated(metric.angle, 0, 1, 0);

						tessellator.startDrawingQuads();
						tessellator.addVertexWithUV(-ringSize, 0, 0, 0.0D, 0.5D);
						tessellator.addVertexWithUV(ringSize, 0, 0, 1.0D, 0.5D);
						tessellator.addVertexWithUV(ringSize, 0, ringSize, 1.0D, 1.0D);
						tessellator.addVertexWithUV(-ringSize, 0, ringSize, 0.0D, 1.0D);
						tessellator.draw();

						GL11.glEnable(GL11.GL_CULL_FACE);
					}
				}

				if(renderPoint) {
					float alpha = MathHelper.clamp_float((float)size * 100.0F, 0.0F, 1.0F);
					alpha *= 1 - BobMathUtil.remap01_clamp((float)size, (float)transitionMinSize, (float)transitionMaxSize);
					float r = MathHelper.clamp_float(metric.body.color[0] * (float)bodyTextureTint.xCoord, 0.0F, 1.0F);
					float g = MathHelper.clamp_float(metric.body.color[1] * (float)bodyTextureTint.yCoord, 0.0F, 1.0F);
					float b = MathHelper.clamp_float(metric.body.color[2] * (float)bodyTextureTint.zCoord, 0.0F, 1.0F);
					GL11.glColor4f(r, g, b, alpha * visibility);
					mc.renderEngine.bindTexture(planetTexture);

					tessellator.startDrawingQuads();
					tessellator.addVertexWithUV(-1.0D, 100.0D, -1.0D, 0.0D, 0.0D);
					tessellator.addVertexWithUV(1.0D, 100.0D, -1.0D, 1.0D, 0.0D);
					tessellator.addVertexWithUV(1.0D, 100.0D, 1.0D, 1.0D, 1.0D);
					tessellator.addVertexWithUV(-1.0D, 100.0D, 1.0D, 0.0D, 1.0D);
					tessellator.draw();
				}

			}
			GL11.glPopMatrix();
		}
	}

	private Vec3 getBodyTextureTint(CelestialBody body) {
		if(body == null) {
			return Vec3.createVectorHelper(1.0D, 1.0D, 1.0D);
		}

		Vec3 atmosphereColor = getBodyAtmosphereColor(body);

		float tintStrength = 0.0F;
		CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);
		if(atmosphere != null && !atmosphere.fluids.isEmpty()) {
			float pressure = MathHelper.clamp_float((float)atmosphere.getPressure(), 0.0F, 2.0F);
			tintStrength = MathHelper.clamp_float(0.25F + pressure * 0.2F, 0.25F, 0.65F);
		} else if(body.gas != null) {
			tintStrength = 0.65F;
		}

		float r = MathHelper.clamp_float(1.0F + ((float)atmosphereColor.xCoord - 1.0F) * tintStrength, 0.0F, 1.0F);
		float g = MathHelper.clamp_float(1.0F + ((float)atmosphereColor.yCoord - 1.0F) * tintStrength, 0.0F, 1.0F);
		float b = MathHelper.clamp_float(1.0F + ((float)atmosphereColor.zCoord - 1.0F) * tintStrength, 0.0F, 1.0F);
		return Vec3.createVectorHelper(r, g, b);
	}

	private Vec3 getBodyAtmosphereColor(CelestialBody body) {
		CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);
		if(atmosphere != null && !atmosphere.fluids.isEmpty()) {
			double totalPressure = atmosphere.getPressure();

			if(totalPressure > 0.0D) {
				double r = 0.0D;
				double g = 0.0D;
				double b = 0.0D;

				for(FluidEntry entry : atmosphere.fluids) {
					if(entry == null || entry.fluid == null || entry.pressure <= 0.0D) {
						continue;
					}

					Vec3 fluidColor = getAtmosphereFluidColor(entry.fluid);
					double percentage = entry.pressure / totalPressure;
					r += fluidColor.xCoord * percentage;
					g += fluidColor.yCoord * percentage;
					b += fluidColor.zCoord * percentage;
				}

				return Vec3.createVectorHelper(
					MathHelper.clamp_double(r, 0.0D, 1.0D),
					MathHelper.clamp_double(g, 0.0D, 1.0D),
					MathHelper.clamp_double(b, 0.0D, 1.0D)
				);
			}
		}

		if(body.gas != null) {
			return getAtmosphereFluidColor(body.gas);
		}

		return Vec3.createVectorHelper(1.0D, 1.0D, 1.0D);
	}

	private Vec3 getAtmosphereFluidColor(FluidType fluid) {
		if(fluid == null) {
			return Vec3.createVectorHelper(1.0D, 1.0D, 1.0D);
		}

		if(fluid == Fluids.EVEAIR) {
			return Vec3.createVectorHelper(53F / 255F, 32F / 255F, 74F / 255F);
		}
		if(fluid == Fluids.DUNAAIR || fluid == Fluids.CARBONDIOXIDE) {
			return Vec3.createVectorHelper(212F / 255F, 112F / 255F, 78F / 255F);
		}
		if(fluid == Fluids.EARTHAIR || fluid == Fluids.OXYGEN || fluid == Fluids.NITROGEN) {
			return Vec3.createVectorHelper(0.7529412F, 0.84705883F, 1.0F);
		}

		return getColorFromHex(fluid.getColor());
	}

	private Vec3 getColorFromHex(int hexColor) {
		float red = ((hexColor >> 16) & 0xFF) / 255.0F;
		float green = ((hexColor >> 8) & 0xFF) / 255.0F;
		float blue = (hexColor & 0xFF) / 255.0F;
		return Vec3.createVectorHelper(red, green, blue);
	}

	private float getCloudAlpha(CelestialBody body) {
		if(body == null) {
			return 0.0F;
		}

		CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);
		if(atmosphere == null) {
			return 0.0F;
		}

		float pressure = (float)atmosphere.getPressure();
		if(pressure <= 0.5F) {
			return 0.0F;
		}

		return MathHelper.clamp_float((pressure - 0.5F) * 0.5F, 0.05F, 0.35F);
	}

	private double getCloudUvOffset(WorldClient world, float partialTicks, CelestialBody body) {
		if(world == null || body == null) {
			return 0.0D;
		}

		double time = world.getWorldTime() + partialTicks;
		double bodyPeriod = body.getRotationalPeriod();
		double driftPeriod = bodyPeriod > 1.0D ? MathHelper.clamp_double(bodyPeriod * 0.35D, 4000.0D, 360000.0D) : 24000.0D;
		double phaseOffset = (Math.abs(body.name.hashCode()) % 2048) / 2048.0D;
		return (time / driftPeriod + phaseOffset) % 1.0D;
	}

	private void renderInjectorLines(Tessellator tessellator, AstroMetric metric, float axialTilt, int count, float visibility, float lineWidth, float time, float spinAngle) {
		if(count <= 0) return;

		final double skyHeight = 100.0D;
		final double angleRad = Math.toRadians(metric.angle);
		final double inclinationRad = Math.toRadians(metric.inclination);
		final double tiltRad = Math.toRadians(axialTilt + 90.0F);

		double x = 0.0D;
		double y = skyHeight;
		double z = 0.0D;

		double cosY = Math.cos(tiltRad);
		double sinY = Math.sin(tiltRad);
		double x1 = x * cosY + z * sinY;
		double y1 = y;
		double z1 = -x * sinY + z * cosY;

		double cosZ = Math.cos(inclinationRad);
		double sinZ = Math.sin(inclinationRad);
		double x2 = x1 * cosZ - y1 * sinZ;
		double y2 = x1 * sinZ + y1 * cosZ;
		double z2 = z1;

		double cosX = Math.cos(angleRad);
		double sinX = Math.sin(angleRad);
		double x3 = x2;
		double y3 = y2 * cosX - z2 * sinX;
		double z3 = y2 * sinX + z2 * cosX;

		double bodyX = x3;
		double bodyY = y3;
		double bodyZ = z3;

		double sunX = 0.0D;
		double sunY = skyHeight;
		double sunZ = 0.0D;

		double dirX = sunX - bodyX;
		double dirY = sunY - bodyY;
		double dirZ = sunZ - bodyZ;
		double dirLen = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
		if(dirLen < 1.0E-4D) return;

		dirX /= dirLen;
		dirY /= dirLen;
		dirZ /= dirLen;

		double rightX = 1.0D;
		double rightZ = 0.0D;

		double forwardX = 0.0D;
		double forwardZ = 1.0D;

		double spinCos = Math.cos(spinAngle);
		double spinSin = Math.sin(spinAngle);
		double spinRightX = rightX * spinCos + forwardX * spinSin;
		double spinRightZ = -rightX * spinSin + forwardX * spinCos;
		double spinForwardX = forwardX * spinCos - rightX * spinSin;
		double spinForwardZ = forwardZ * spinCos + rightZ * spinSin;

		double litX = dirX;
		double litZ = dirZ;
		double litLen = Math.sqrt(litX * litX + litZ * litZ);
		if(litLen < 1.0E-4D) return;
		litX /= litLen;
		litZ /= litLen;

		GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LINE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_TEXTURE_BIT);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glLineWidth(lineWidth);
		OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);

		float alpha = MathHelper.clamp_float(visibility * 0.1F, 0.0F, 1.0F);
		float basePhase = time * 0.02F;
		tessellator.startDrawing(GL11.GL_LINES);
		for(int i = 0; i < count; i++) {
			int seedBase = metric.body != null ? metric.body.dimensionId : 0;
			int hash = seedBase * 73471 + i * 912367;
			hash ^= (hash << 13);
			hash ^= (hash >>> 17);
			hash ^= (hash << 5);

			float phase = basePhase + ((hash & 0xFF) / 255.0F) * 6.2831855F;
			float r = 0.5F + 0.5F * MathHelper.sin(phase);
			float g = 0.5F + 0.5F * MathHelper.sin(phase + 2.0943952F);
			float b = 0.5F + 0.5F * MathHelper.sin(phase + 4.1887903F);

			double theta = ((hash & 0xFFFF) / 65535.0D) * Math.PI * 2.0D;
			double radius = 0.15D + (((hash >> 16) & 0xFF) / 255.0D) * 0.35D;

			double offsetX = spinRightX * Math.cos(theta) * radius + spinForwardX * Math.sin(theta) * radius;
			double offsetY = 0.0D;
			double offsetZ = spinRightZ * Math.cos(theta) * radius + spinForwardZ * Math.sin(theta) * radius;

			double litDot = offsetX * litX + offsetZ * litZ;
			if(litDot < 0.0D) {
				offsetX = -offsetX;
				offsetZ = -offsetZ;
			}

			tessellator.setColorRGBA_F(r, g, b, alpha);
			tessellator.addVertex(bodyX + offsetX, bodyY + offsetY, bodyZ + offsetZ);
			tessellator.addVertex(sunX, sunY, sunZ);
		}
		tessellator.draw();

		GL11.glPopAttrib();
	}

	protected void renderRings(float partialTicks, WorldClient world, Minecraft mc, float ringTilt, float[] ringColor, float ringSize, float visibility) {
		Tessellator tessellator = Tessellator.instance;

		GL11.glColor4f(ringColor[0], ringColor[1], ringColor[2], visibility);
		mc.renderEngine.bindTexture(ringTexture);

		double offset = -20.0D;

		tessellator.startDrawingQuads();
		tessellator.addVertexWithUV(offset, -ringSize, -ringSize, 0.0D, 0.0D);
		tessellator.addVertexWithUV(offset, ringSize, -ringSize, 1.0D, 0.0D);
		tessellator.addVertexWithUV(offset, ringSize, ringSize, 1.0D, 1.0D);
		tessellator.addVertexWithUV(offset, -ringSize, ringSize, 0.0D, 1.0D);
		tessellator.draw();
	}

	private float getRingFade(CelestialBody body, WorldClient world) {
		if(body == null || world == null) return 1.0F;

		// Only apply ring fade to moons (bodies with a planet parent).
		// Planets keep rings fully visible.
		boolean isMoon = body.parent != null && body.parent.parent != null;
		if(!isMoon) {
			return body.hasRings ? 1.0F : 0.0F;
		}

		String key = body.name;
		long tick = world.getTotalWorldTime();
		long lastTick = ringFadeTick.containsKey(key) ? ringFadeTick.get(key) : tick;
		long dt = Math.max(0L, tick - lastTick);

		// First observation should reflect current ring state immediately.
		float fade = ringFade.containsKey(key) ? ringFade.get(key) : (body.hasRings ? 1.0F : 0.0F);
		if(body.hasRings) {
			fade = Math.min(1.0F, fade + dt * (1.0F / (5.0F * 24000.0F)));
		} else {
			fade = Math.max(0.0F, fade - dt * (1.0F / (5.0F * 24000.0F)));
		}

		ringFade.put(key, fade);
		ringFadeTick.put(key, tick);

		return fade;
	}

	protected void renderDigamma(float partialTicks, WorldClient world, Minecraft mc, float solarAngle) {
		Tessellator tessellator = Tessellator.instance;

		GL11.glPushMatrix();
		{
			float var12 = 1F + world.rand.nextFloat() * 0.5F;
			double dist = 100D;
			double t = world.getTotalWorldTime() + partialTicks;
			boolean isKerbol = world.provider != null && world.provider.dimensionId == SpaceConfig.kerbolDimension;
			double orbitYaw = isKerbol ? Math.sin(t / 6000.0D * Math.PI * 2.0D) * 18.0D : 0.0D;
			double orbitPitch = isKerbol ? Math.sin(t / 4200.0D * Math.PI * 2.0D + 1.2D) * 10.0D : 0.0D;
			double distOffset = isKerbol ? Math.sin(t / 3600.0D * Math.PI * 2.0D) * 10.0D : 0.0D;

			if(ModEventHandlerClient.renderLodeStar) {
				GL11.glPushMatrix();
				GL11.glRotatef(-60.0F + (float)orbitPitch, 1.0F, 0.0F, 0.0F);
				GL11.glRotatef(45.0F + (float)orbitYaw, 0.0F, 1.0F, 0.0F);
				FMLClientHandler.instance().getClient().renderEngine.bindTexture(lodeStar); // genu-ine bona-fide ass whooping

				double lodeDist = dist + distOffset;
				tessellator.startDrawingQuads();
				tessellator.addVertexWithUV(-var12, lodeDist, -var12, 0.0D, 0.0D);
				tessellator.addVertexWithUV(var12, lodeDist, -var12, 0.0D, 1.0D);
				tessellator.addVertexWithUV(var12, lodeDist, var12, 1.0D, 1.0D);
				tessellator.addVertexWithUV(-var12, lodeDist, var12, 1.0D, 0.0D);
				tessellator.draw();

				GL11.glPopMatrix();
			}

			OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);

			float brightness = (float) Math.sin(solarAngle * Math.PI);
			brightness *= brightness;
			GL11.glColor4f(brightness, brightness, brightness, brightness);
			GL11.glRotatef(-90.0F + (float)orbitYaw, 0.0F, 1.0F, 0.0F);
			GL11.glRotatef(solarAngle * 360.0F, 1.0F, 0.0F, 0.0F);
			GL11.glRotatef(140.0F + (float)orbitPitch, 1.0F, 0.0F, 0.0F);
			GL11.glRotatef(-90.0F, 0.0F, 0.0F, 1.0F);

			mc.renderEngine.bindTexture(digammaStar);

			float digamma = HbmLivingProps.getDigamma(Minecraft.getMinecraft().thePlayer);
			var12 = 1F * (1 + digamma * 0.25F);
			dist = 100D - digamma * 2.5 - distOffset;

			tessellator.startDrawingQuads();
			tessellator.addVertexWithUV(-var12, dist, -var12, 0.0D, 0.0D);
			tessellator.addVertexWithUV(var12, dist, -var12, 0.0D, 1.0D);
			tessellator.addVertexWithUV(var12, dist, var12, 1.0D, 1.0D);
			tessellator.addVertexWithUV(-var12, dist, var12, 1.0D, 0.0D);
			tessellator.draw();

		}
		GL11.glPopMatrix();
	}

	// Does anyone even play with 3D glasses anymore?
	protected float[] applyAnaglyph(float... colors) {
		float r = (colors[0] * 30.0F + colors[1] * 59.0F + colors[2] * 11.0F) / 100.0F;
		float g = (colors[0] * 30.0F + colors[1] * 70.0F) / 100.0F;
		float b = (colors[0] * 30.0F + colors[2] * 70.0F) / 100.0F;

		return new float[] { r, g, b };
	}

	// is just drawing a big cube with UVs prepared to draw a gradient
	private void renderSkyboxSide(Tessellator tessellator, int side) {
		double u = side % 3 / 3.0D;
		double v = side / 3 / 2.0D;
		tessellator.startDrawingQuads();
		tessellator.addVertexWithUV(-100.0D, -100.0D, -100.0D, u, v);
		tessellator.addVertexWithUV(-100.0D, -100.0D, 100.0D, u, v + 0.5D);
		tessellator.addVertexWithUV(100.0D, -100.0D, 100.0D, u + 0.3333333333333333D, v + 0.5D);
		tessellator.addVertexWithUV(100.0D, -100.0D, -100.0D, u + 0.3333333333333333D, v);
		tessellator.draw();
	}

	protected void renderSpecialEffects(float partialTicks, WorldClient world, Minecraft mc) {
		if(world.provider == null) return;
		renderSkyfallEffect(partialTicks, world, mc);
		if(novaeEffects.isEmpty()) return;
		if(world.provider.dimensionId == SpaceConfig.kerbolDimension) return;

		CelestialBody body = CelestialBody.getBody(world);
		float axialTilt = body != null ? body.axialTilt : 0.0F;
		float siderealAngle = body != null ? (float)SolarSystem.calculateSiderealAngle(world, partialTicks, body) : 0.0F;
		if(world.provider.dimensionId == SpaceConfig.kerbolDimension) {
			double t = world.getTotalWorldTime() + partialTicks;
			siderealAngle += (float)(Math.sin(t / 6000.0D * Math.PI * 2.0D) * 6.0D);
		}

		Tessellator tessellator = Tessellator.instance;
		for(int i = 0; i < novaeEffects.size(); i++) {
			NovaeEffect effect = novaeEffects.get(i);
			if(effect.dimension != world.provider.dimensionId) {
				continue;
			}

			float age = (world.getTotalWorldTime() - effect.startWorldTime) + partialTicks;
			if(age < 0.0F) {
				continue;
			}

			float spikeGrowTicks = 120.0F;
			float growProgress = MathHelper.clamp_float(age / spikeGrowTicks, 0.0F, 1.0F);
			float grow = smoothstep(0.0F, 1.0F, growProgress);
			float spikeAlpha = 1.0F - grow * 0.25F;

			float minSpikeSize = 0.5F;
			float maxSpikeSize = 20.0F;
			double spikeSize = (minSpikeSize + (maxSpikeSize - minSpikeSize) * grow) * effect.sizeScale;
			double skyHeight = 120.0D;

			GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_ENABLE_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_TEXTURE_BIT);
			GL11.glPushMatrix();
			{
				GL11.glDisable(GL11.GL_CULL_FACE);
				GL11.glDisable(GL11.GL_ALPHA_TEST);
				GL11.glEnable(GL11.GL_BLEND);
				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

				// Match starfield rotation so the event moves with the night sky.
				GL11.glRotatef(axialTilt, 1.0F, 0.0F, 0.0F);
				GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
				GL11.glRotatef(siderealAngle * 360.0F, 1.0F, 0.0F, 0.0F);
				GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);

				// Orient the spike to a random sky direction.
				GL11.glRotatef(effect.pitch, 1.0F, 0.0F, 0.0F);
				GL11.glRotatef(effect.yaw, 0.0F, 1.0F, 0.0F);
				GL11.glRotatef(effect.roll, 0.0F, 0.0F, 1.0F);

				GL11.glColor4f(effect.r, effect.g, effect.b, spikeAlpha);
				GL11.glEnable(GL11.GL_TEXTURE_2D);
				mc.renderEngine.bindTexture(supernovaeTexture);

				tessellator.startDrawingQuads();
				tessellator.addVertexWithUV(-spikeSize, skyHeight, -spikeSize, 0.0D, 0.0D);
				tessellator.addVertexWithUV(spikeSize, skyHeight, -spikeSize, 1.0D, 0.0D);
				tessellator.addVertexWithUV(spikeSize, skyHeight, spikeSize, 1.0D, 1.0D);
				tessellator.addVertexWithUV(-spikeSize, skyHeight, spikeSize, 0.0D, 1.0D);
				tessellator.draw();

				// Fast, oversized shockwave when the spike appears.
				float waveDurationTicks = 360.0F;
				float waveProgress = MathHelper.clamp_float(age / waveDurationTicks, 0.0F, 1.0F);
				if(waveProgress < 1.0F) {
					float waveAlpha = 1.0F - smoothstep(0.0F, 1.0F, waveProgress);
					double waveSize = maxSpikeSize * effect.sizeScale * (1.0D + waveProgress * 6.0D);

					GL11.glColor4f(effect.r, effect.g, effect.b, waveAlpha);
					mc.renderEngine.bindTexture(shockwaveTexture);
					tessellator.startDrawingQuads();
					tessellator.addVertexWithUV(-waveSize, skyHeight, -waveSize, 0.0D, 0.0D);
					tessellator.addVertexWithUV(waveSize, skyHeight, -waveSize, 1.0D, 0.0D);
					tessellator.addVertexWithUV(waveSize, skyHeight, waveSize, 1.0D, 1.0D);
					tessellator.addVertexWithUV(-waveSize, skyHeight, waveSize, 0.0D, 1.0D);
					tessellator.draw();

					float flareProgress = MathHelper.clamp_float(age / (waveDurationTicks * 0.5F), 0.0F, 1.0F);
					float flareAlpha = 1.0F - smoothstep(0.0F, 1.0F, flareProgress);
					GL11.glColor4f(effect.r, effect.g, effect.b, flareAlpha * 1.25F);
					mc.renderEngine.bindTexture(shockFlareTexture);
					double flareSize = maxSpikeSize * effect.sizeScale * (1.25D + waveProgress * 2.5D);
					tessellator.startDrawingQuads();
					tessellator.addVertexWithUV(-flareSize, skyHeight, -flareSize, 0.0D, 0.0D);
					tessellator.addVertexWithUV(flareSize, skyHeight, -flareSize, 1.0D, 0.0D);
					tessellator.addVertexWithUV(flareSize, skyHeight, flareSize, 1.0D, 1.0D);
					tessellator.addVertexWithUV(-flareSize, skyHeight, flareSize, 0.0D, 1.0D);
					tessellator.draw();
				}

				// Tom-style blast wave cylinder emerging from the center.
				float tomWaveDuration = 600.0F;
				if(age <= tomWaveDuration) {
					float tomProgress = MathHelper.clamp_float(age / tomWaveDuration, 0.0F, 1.0F);
					double tomScale = 4.0D + (tomProgress * tomProgress) * 220.0D * effect.sizeScale;

					int segments = 16;
					float angle = (float) Math.toRadians(360D / segments);
					int height = 40;
					int depth = 40;

					mc.renderEngine.bindTexture(ResourceManager.tomblast);
					GL11.glMatrixMode(GL11.GL_TEXTURE);
					GL11.glLoadIdentity();
					GL11.glTranslatef(0.0F, -(world.getTotalWorldTime() + partialTicks) * 0.01F, 0.0F);
					GL11.glMatrixMode(GL11.GL_MODELVIEW);

					tessellator.startDrawingQuads();
					for(int j = 0; j < segments; j++) {
						for(int k = 0; k < 5; k++) {
							double mod = 1 - k * 0.025;
							double h = height + k * 10;
							double off = 1D / k;

							Vec3 vec = Vec3.createVectorHelper(tomScale, 0, 0);
							vec.rotateAroundY(angle * j);
							double x0 = vec.xCoord * mod;
							double z0 = vec.zCoord * mod;

							tessellator.setColorRGBA_F(effect.r, effect.g, effect.b, 0.0F);
							tessellator.addVertexWithUV(x0, h, z0, 0, 1 + off);
							tessellator.setColorRGBA_F(effect.r, effect.g, effect.b, 1.0F);
							tessellator.addVertexWithUV(x0, -depth, z0, 0, 0 + off);

							vec.rotateAroundY(angle);
							x0 = vec.xCoord * mod;
							z0 = vec.zCoord * mod;

							tessellator.setColorRGBA_F(effect.r, effect.g, effect.b, 1.0F);
							tessellator.addVertexWithUV(x0, -depth, z0, 1, 0 + off);
							tessellator.setColorRGBA_F(effect.r, effect.g, effect.b, 0.0F);
							tessellator.addVertexWithUV(x0, h, z0, 1, 1 + off);
						}
					}
					tessellator.draw();

					GL11.glMatrixMode(GL11.GL_TEXTURE);
					GL11.glLoadIdentity();
					GL11.glMatrixMode(GL11.GL_MODELVIEW);
				}

				GL11.glEnable(GL11.GL_ALPHA_TEST);
				GL11.glEnable(GL11.GL_CULL_FACE);
			}
			GL11.glPopMatrix();
			GL11.glPopAttrib();
		}
	}

	private void renderSkyfallEffect(float partialTicks, WorldClient world, Minecraft mc) {
		if(!skyfallActive) return;
		if(world.provider == null || world.provider.dimensionId != skyfallDimension) return;
		if(mc.thePlayer == null) return;

		long ageTicks = world.getTotalWorldTime() - skyfallStartWorldTime;
		if(ageTicks < 0) return;
		if(ageTicks > skyfallDurationTicks) {
			skyfallActive = false;
			skyfallMeteors.clear();
			skyfallSmoke.clear();
			return;
		}

		long worldTick = world.getTotalWorldTime();
		if(worldTick != skyfallLastUpdateTick) {
			skyfallLastUpdateTick = worldTick;

			for(SkyfallMeteor meteor : skyfallMeteors) {
				updateSkyfallMeteor(meteor);
			}
			for(SkyfallMeteor smoke : skyfallSmoke) {
				updateSkyfallMeteor(smoke);
			}

			SkyfallMeteor meteor = new SkyfallMeteor(
				(mc.thePlayer.posX + skyfallRandom.nextInt(16000)) - 8000,
				2017.0D,
				(mc.thePlayer.posZ + skyfallRandom.nextInt(16000)) - 8000,
				-31.2D,
				-20.8D,
				20.0D,
				false
			);
			skyfallMeteors.add(meteor);

			skyfallMeteors.removeIf(m -> m.dead);
			skyfallSmoke.removeIf(m -> m.dead);
		}

		double playerX = mc.thePlayer.prevPosX + (mc.thePlayer.posX - mc.thePlayer.prevPosX) * partialTicks;
		double playerY = mc.thePlayer.prevPosY + (mc.thePlayer.posY - mc.thePlayer.prevPosY) * partialTicks;
		double playerZ = mc.thePlayer.prevPosZ + (mc.thePlayer.posZ - mc.thePlayer.prevPosZ) * partialTicks;

		GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
		GL11.glDisable(GL11.GL_FOG);
		GL11.glEnable(GL11.GL_TEXTURE_2D);

		for(SkyfallMeteor meteor : skyfallMeteors) {
			GL11.glPushMatrix();

			Vec3 offset = Vec3.createVectorHelper(meteor.posX - playerX, meteor.posY - playerY, meteor.posZ - playerZ);
			double offsetLength = offset.lengthVector();
			double distance = Math.min(Minecraft.getMinecraft().gameSettings.renderDistanceChunks * 16, offsetLength);
			Vec3 offsetNormal = offsetLength >= 1.0E-4D
				? Vec3.createVectorHelper(offset.xCoord / offsetLength, offset.yCoord / offsetLength, offset.zCoord / offsetLength)
				: offset;
			Vec3 renderOffset = Vec3.createVectorHelper(offsetNormal.xCoord * distance, offsetNormal.yCoord * distance, offsetNormal.zCoord * distance);

			GL11.glTranslated(renderOffset.xCoord, renderOffset.yCoord, renderOffset.zCoord);

			double descent = 2017.0D - meteor.posY;
			double quadratic = (-(descent * descent) + (1517.0D * descent)) / 41.0D;
			double safeLength = Math.max(1.0D, offsetLength);
			float scalar = (float)(quadratic / safeLength);
			GL11.glScaled(scalar, scalar, scalar);

			GL11.glColor4d(1, 1, 1, 1);
			renderSkyfallGlow(shockFlareTexture);
			GL11.glPopMatrix();
		}

		for(SkyfallMeteor smoke : skyfallSmoke) {
			GL11.glPushMatrix();

			Vec3 offset = Vec3.createVectorHelper(smoke.posX - playerX, smoke.posY - playerY, smoke.posZ - playerZ);
			double offsetLength = offset.lengthVector();
			double distance = Math.min(Minecraft.getMinecraft().gameSettings.renderDistanceChunks * 16, offsetLength);
			Vec3 offsetNormal = offsetLength >= 1.0E-4D
				? Vec3.createVectorHelper(offset.xCoord / offsetLength, offset.yCoord / offsetLength, offset.zCoord / offsetLength)
				: offset;
			Vec3 renderOffset = Vec3.createVectorHelper(offsetNormal.xCoord * distance, offsetNormal.yCoord * distance, offsetNormal.zCoord * distance);

			GL11.glTranslated(renderOffset.xCoord, renderOffset.yCoord, renderOffset.zCoord);

			double descent = 2017.0D - smoke.posY;
			double quadratic = (-(descent * descent) + (1517.0D * descent)) / 41.0D;
			double safeLength = Math.max(1.0D, offsetLength);
			float scalar = (float)(quadratic / safeLength);
			GL11.glScaled(scalar, scalar, scalar);

			renderSkyfallSmoke(particleBaseTexture, smoke.age);
			GL11.glPopMatrix();
		}

		GL11.glEnable(GL11.GL_FOG);
		GL11.glPopAttrib();
	}

	private static void updateSkyfallMeteor(SkyfallMeteor meteor) {
		if(!meteor.smoke) {
			SkyfallMeteor smoke = new SkyfallMeteor(
				(meteor.posX + skyfallRandom.nextInt(16)) - 8,
				(meteor.posY + skyfallRandom.nextInt(16)),
				(meteor.posZ + skyfallRandom.nextInt(16)) - 8,
				0.0D,
				0.0D,
				0.0D,
				true
			);
			skyfallSmoke.add(smoke);
		}

		if(meteor.posY <= 500.0D && !meteor.smoke) {
			meteor.dead = true;
		}

		if(meteor.smoke) {
			meteor.age++;
			if(meteor.age >= 60) {
				meteor.dead = true;
			}
		}

		meteor.prevPosX = meteor.posX;
		meteor.prevPosY = meteor.posY;
		meteor.prevPosZ = meteor.posZ;
		meteor.posX += meteor.motionX;
		meteor.posY += meteor.motionY;
		meteor.posZ += meteor.motionZ;
	}

	private void renderSkyfallSmoke(ResourceLocation texture, long age) {
		GL11.glPushMatrix();
		GL11.glEnable(GL11.GL_BLEND);
		float width = 1.0F;
		float xOffset = 0.5F;
		float yOffset = 0.25F;
		float dark = 1.0F - Math.min(((float)age / (100.0F * 0.35F)), 1.0F);
		float tint = Math.min(1.0F, (float)age / 12.0F);
		double r = (1.0 - tint) + (0.55 * tint);
		double g = (1.0 - tint) + (1.0 * tint);
		double b = (1.0 - tint) + (0.75 * tint);
		GL11.glRotatef(180.0F - RenderManager.instance.playerViewY, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(-RenderManager.instance.playerViewX, 1.0F, 0.0F, 0.0F);
		GL11.glColor4d(r * dark, g * dark, b * dark, 1.0);
		Tessellator tess = Tessellator.instance;
		tess.startDrawingQuads();
		tess.setNormal(0.0F, 1.0F, 0.0F);
		tess.addVertexWithUV(0.0F - xOffset, 0.0F - yOffset, 0.0D, 1, 0);
		tess.addVertexWithUV(width - xOffset, 0.0F - yOffset, 0.0D, 0, 0);
		tess.addVertexWithUV(width - xOffset, width - yOffset, 0.0D, 0, 1);
		tess.addVertexWithUV(0.0F - xOffset, width - yOffset, 0.0D, 1, 1);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		tess.draw();
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glPopMatrix();
	}

	private void renderSkyfallGlow(ResourceLocation texture) {
		GL11.glPushMatrix();
		GL11.glEnable(GL11.GL_BLEND);
		float width = 1.0F;
		float xOffset = 0.5F;
		float yOffset = 0.25F;
		GL11.glRotatef(180.0F - RenderManager.instance.playerViewY, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(-RenderManager.instance.playerViewX, 1.0F, 0.0F, 0.0F);
		GL11.glColor4d(1.0D, 1.0D, 1.0D, 1.0D);
		Tessellator tess = Tessellator.instance;
		tess.startDrawingQuads();
		tess.setNormal(0.0F, 1.0F, 0.0F);
		tess.addVertexWithUV(0.0F - xOffset, 0.0F - yOffset, 0.0D, 1, 0);
		tess.addVertexWithUV(width - xOffset, 0.0F - yOffset, 0.0D, 0, 0);
		tess.addVertexWithUV(width - xOffset, width - yOffset, 0.0D, 0, 1);
		tess.addVertexWithUV(0.0F - xOffset, width - yOffset, 0.0D, 1, 1);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		tess.draw();
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glPopMatrix();
	}

	protected void renderStarcoreIgnitionEffect(float partialTicks, WorldClient world, Minecraft mc, double sunSize) {
		if(!starcoreIgnitionActive) return;
		if(world.provider == null || world.provider.dimensionId != starcoreIgnitionDimension) return;

		float age = (world.getTotalWorldTime() - starcoreIgnitionStartWorldTime) + partialTicks;
		if(age < 0.0F) return;
		if(age > 600.0F) {
			starcoreIgnitionActive = false;
			return;
		}

		Tessellator tessellator = Tessellator.instance;

		GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_ENABLE_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_TEXTURE_BIT);
		GL11.glPushMatrix();
		{
			GL11.glDisable(GL11.GL_CULL_FACE);
			GL11.glDisable(GL11.GL_ALPHA_TEST);
			GL11.glDisable(GL11.GL_DEPTH_TEST);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

			// Fast, oversized shockwave when the ignition appears.
			float waveDurationTicks = 360.0F;
			float waveProgress = MathHelper.clamp_float(age / waveDurationTicks, 0.0F, 1.0F);
			if(waveProgress < 1.0F) {
				float waveAlpha = 1.0F - smoothstep(0.0F, 1.0F, waveProgress);
				double baseWaveSize = sunSize * 2.0D;
				double waveSize = baseWaveSize * (1.0D + waveProgress * 6.0D);

				GL11.glColor4f(1.0F, 1.0F, 1.0F, waveAlpha);
				mc.renderEngine.bindTexture(shockwaveTexture);
				tessellator.startDrawingQuads();
				tessellator.addVertexWithUV(-waveSize, 100.0D, -waveSize, 0.0D, 0.0D);
				tessellator.addVertexWithUV(waveSize, 100.0D, -waveSize, 1.0D, 0.0D);
				tessellator.addVertexWithUV(waveSize, 100.0D, waveSize, 1.0D, 1.0D);
				tessellator.addVertexWithUV(-waveSize, 100.0D, waveSize, 0.0D, 1.0D);
				tessellator.draw();
			}

			// Tom-style blast wave cylinder emerging from the center.
			float tomWaveDuration = 600.0F;
			if(age <= tomWaveDuration) {
				float tomProgress = MathHelper.clamp_float(age / tomWaveDuration, 0.0F, 1.0F);
				double tomScale = 4.0D + (tomProgress * tomProgress) * (sunSize * 12.0D);

				int segments = 16;
				float angle = (float) Math.toRadians(360D / segments);
				int height = 40;
				int depth = 40;

				mc.renderEngine.bindTexture(ResourceManager.tomblast);
				GL11.glMatrixMode(GL11.GL_TEXTURE);
				GL11.glLoadIdentity();
				GL11.glTranslatef(0.0F, -(world.getTotalWorldTime() + partialTicks) * 0.01F, 0.0F);
				GL11.glMatrixMode(GL11.GL_MODELVIEW);

				tessellator.startDrawingQuads();
				for(int i = 0; i < segments; i++) {
					for(int j = 0; j < 5; j++) {
						double mod = 1 - j * 0.025;
						double h = height + j * 10;
						double off = 1D / j;

						Vec3 vec = Vec3.createVectorHelper(tomScale, 0, 0);
						vec.rotateAroundY(angle * i);
						double x0 = vec.xCoord * mod;
						double z0 = vec.zCoord * mod;

						tessellator.setColorRGBA_F(1.0F, 1.0F, 1.0F, 0.0F);
						tessellator.addVertexWithUV(x0, h, z0, 0, 1 + off);
						tessellator.setColorRGBA_F(1.0F, 1.0F, 1.0F, 1.0F);
						tessellator.addVertexWithUV(x0, -depth, z0, 0, 0 + off);

						vec.rotateAroundY(angle);
						x0 = vec.xCoord * mod;
						z0 = vec.zCoord * mod;

						tessellator.setColorRGBA_F(1.0F, 1.0F, 1.0F, 1.0F);
						tessellator.addVertexWithUV(x0, -depth, z0, 1, 0 + off);
						tessellator.setColorRGBA_F(1.0F, 1.0F, 1.0F, 0.0F);
						tessellator.addVertexWithUV(x0, h, z0, 1, 1 + off);
					}
				}
				tessellator.draw();

				GL11.glMatrixMode(GL11.GL_TEXTURE);
				GL11.glLoadIdentity();
				GL11.glMatrixMode(GL11.GL_MODELVIEW);
			}

			GL11.glEnable(GL11.GL_ALPHA_TEST);
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glEnable(GL11.GL_CULL_FACE);
		}
		GL11.glPopMatrix();
		GL11.glPopAttrib();
	}

	protected void renderStarcoreDecayFlareEffect(float partialTicks, WorldClient world, Minecraft mc, double sunSize) {
		if(!starcoreDecayFlareActive) return;
		if(world.provider == null || world.provider.dimensionId != starcoreDecayFlareDimension) return;

		float age = (world.getTotalWorldTime() - starcoreDecayFlareStartWorldTime) + partialTicks;
		if(age < 0.0F) return;
		if(age > 200.0F) {
			starcoreDecayFlareActive = false;
			return;
		}

		float progress = MathHelper.clamp_float(age / 200.0F, 0.0F, 1.0F);
		float alpha = 1.0F - smoothstep(0.0F, 1.0F, progress);
		double size = sunSize * (0.15D + 0.35D * progress);

		Tessellator tessellator = Tessellator.instance;

		GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_ENABLE_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_TEXTURE_BIT);
		GL11.glPushMatrix();
		{
			GL11.glDisable(GL11.GL_CULL_FACE);
			GL11.glDisable(GL11.GL_ALPHA_TEST);
			GL11.glDisable(GL11.GL_DEPTH_TEST);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

			GL11.glColor4f(1.0F, 0.5F, 0.5F, alpha);
			mc.renderEngine.bindTexture(shockFlareTexture);

			tessellator.startDrawingQuads();
			tessellator.addVertexWithUV(-size, 100.0D, -size, 0.0D, 0.0D);
			tessellator.addVertexWithUV(size, 100.0D, -size, 1.0D, 0.0D);
			tessellator.addVertexWithUV(size, 100.0D, size, 1.0D, 1.0D);
			tessellator.addVertexWithUV(-size, 100.0D, size, 0.0D, 1.0D);
			tessellator.draw();

			GL11.glEnable(GL11.GL_ALPHA_TEST);
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glEnable(GL11.GL_CULL_FACE);
		}
		GL11.glPopMatrix();
		GL11.glPopAttrib();
	}



	private static float smoothstep(float edge0, float edge1, float x) {
		float t = MathHelper.clamp_float((x - edge0) / (edge1 - edge0), 0.0F, 1.0F);
		return t * t * (3.0F - 2.0F * t);
	}

	public static void startNovaeEffect(long worldTime, int dimension, float yaw, float pitch, float roll, float r, float g, float b, float sizeScale) {
		novaeEffects.add(new NovaeEffect(worldTime, dimension, yaw, pitch, roll, r, g, b, sizeScale));
	}

	public static void startStarcoreIgnitionEffect(long worldTime, int dimension) {
		starcoreIgnitionActive = true;
		starcoreIgnitionStartWorldTime = worldTime;
		starcoreIgnitionDimension = dimension;
	}

	public static void startStarcoreDecayFlareEffect(long worldTime, int dimension) {
		starcoreDecayFlareActive = true;
		starcoreDecayFlareStartWorldTime = worldTime;
		starcoreDecayFlareDimension = dimension;
	}

	public static void startSkyfallEffect(long worldTime, int dimension, int durationTicks) {
		skyfallActive = true;
		skyfallStartWorldTime = worldTime;
		skyfallDimension = dimension;
		skyfallDurationTicks = Math.max(1, durationTicks);
		skyfallLastUpdateTick = Long.MIN_VALUE;
		skyfallMeteors.clear();
		skyfallSmoke.clear();
	}

	protected void render3DModel(float partialTicks, WorldClient world, Minecraft mc) {

	}

protected void renderStation(float partialTicks, WorldClient world, Minecraft mc, OrbitalStation station, float solarAngle) {
		Tessellator tessellator = Tessellator.instance;

		long seed = station.dX * 1024 + station.dZ;

		double ticks = (double)(System.currentTimeMillis() % (1600 * 50)) / 50;

		GL11.glPushMatrix();
		{

			GL11.glRotatef(solarAngle * -360.0F, 1.0F, 0.0F, 0.0F);
			GL11.glRotatef(-40.0F + (float)(seed % 800) * 0.1F - 5.0F, 1.0F, 0.0F, 0.0F);
			GL11.glRotatef((float)(seed % 50) * 0.1F - 20.0F, 0.0F, 1.0F, 0.0F);
			GL11.glRotatef((float)(seed % 80) * 0.1F - 2.5F, 0.0F, 0.0F, 1.0F);
			GL11.glRotated((ticks / 1600.0D) * -360.0D, 1.0F, 0.0F, 0.0F);

			GL11.glColor4f(0.8F, 1, 1, 1);

			mc.renderEngine.bindTexture(stationTexture);

			float size = 0.8F;

			tessellator.startDrawingQuads();
			tessellator.addVertexWithUV(-size, 100.0, -size, 0.0D, 0.0D);
			tessellator.addVertexWithUV(size, 100.0, -size, 0.0D, 1.0D);
			tessellator.addVertexWithUV(size, 100.0, size, 1.0D, 1.0D);
			tessellator.addVertexWithUV(-size, 100.0, size, 1.0D, 0.0D);
			tessellator.draw();

		}
		GL11.glPopMatrix();
	}

	private static final class SkyfallMeteor {
		private double posX;
		private double posY;
		private double posZ;
		private double prevPosX;
		private double prevPosY;
		private double prevPosZ;
		private final double motionX;
		private final double motionY;
		private final double motionZ;
		private long age;
		private final boolean smoke;
		private boolean dead;

		private SkyfallMeteor(double posX, double posY, double posZ, double motionX, double motionY, double motionZ, boolean smoke) {
			this.posX = posX;
			this.posY = posY;
			this.posZ = posZ;
			this.motionX = motionX;
			this.motionY = motionY;
			this.motionZ = motionZ;
			this.smoke = smoke;
		}
	}

}
