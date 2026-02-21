package com.hbm.dim.orbit;

import java.util.List;

import org.lwjgl.opengl.GL11;

import com.hbm.dim.CelestialBody;
import com.hbm.dim.SkyProviderCelestial;
import com.hbm.dim.SolarSystem;
import com.hbm.dim.SolarSystem.AstroMetric;
import com.hbm.dim.orbit.OrbitalStation.StationState;
import com.hbm.dim.trait.CBT_SkyState;
import com.hbm.lib.Library;
import com.hbm.util.BobMathUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class SkyProviderOrbit extends SkyProviderCelestial {

	private static CelestialBody lastBody;
	private static int lastBrightestPixel = 0;

	@Override
	public void render(float partialTicks, WorldClient world, Minecraft mc) {
		WorldProviderOrbit provider = (WorldProviderOrbit) world.provider;
		OrbitalStation station = OrbitalStation.clientStation;
		double progress = station.getTransferProgress(partialTicks);
		CelestialBody animationTarget = station.getAnimationTarget();
		float orbitalTilt = 80;

		// Keep orbit lightmap dimming in sync with sky-state transitions.
		if(lastBrightestPixel != mc.entityRenderer.lightmapColors[255] + mc.entityRenderer.lightmapColors[250]) {
			if(provider.updateLightmap(mc.entityRenderer.lightmapColors)) {
				mc.entityRenderer.lightmapTexture.updateDynamicTexture();
			}
			lastBrightestPixel = mc.entityRenderer.lightmapColors[255] + mc.entityRenderer.lightmapColors[250];
		}

		GL11.glDepthMask(false);
		GL11.glDisable(GL11.GL_FOG);
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);

		GL11.glEnable(GL11.GL_BLEND);
		RenderHelper.disableStandardItemLighting();

		OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

		float solarAngle = getCelestialAngle(world, provider.metrics, partialTicks, station);
		float siderealAngle = (float)SolarSystem.calculateSiderealAngle(world, partialTicks, station.orbiting);
		float celestialPhase = (1 - (solarAngle + 0.5F) % 1) * 2 - 1;

		float starBrightness = world.getStarBrightness(partialTicks);

		renderStars(partialTicks, world, mc, starBrightness, solarAngle + siderealAngle, orbitalTilt);

		GL11.glPushMatrix();
		{

			GL11.glRotatef(orbitalTilt, 1.0F, 0.0F, 0.0F);
			GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
			GL11.glRotatef(solarAngle * 360.0F, 1.0F, 0.0F, 0.0F);

			// digma balls
			renderDigamma(partialTicks, world, mc, solarAngle);

			OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);

			double sunSize = SolarSystem.calculateSunSize(station.orbiting) * SolarSystem.SUN_RENDER_SCALE;
			if(station.state != StationState.ORBIT) {
				double sunTargetSize;
				if(station.target == SolarSystem.dmitriy && animationTarget != null && animationTarget.parent == null) {
					// Approaching Kerbol: linear distance ramp for a steady growth.
					double baseDist = Math.max(1.0, station.orbiting.semiMajorAxisKm);
					double dist = Math.max(1.0, BobMathUtil.lerp(progress, baseDist, 50_000D));
					sunTargetSize = 2D * Math.atan((2D * animationTarget.radiusKm) / (2D * dist)) * SolarSystem.RENDER_SCALE;
					sunTargetSize *= SolarSystem.SUN_RENDER_SCALE;
					sunTargetSize = Math.min(sunTargetSize, SolarSystem.MAX_APPARENT_SIZE_ORBIT);
				} else {
					sunTargetSize = SolarSystem.calculateSunSize(animationTarget) * SolarSystem.SUN_RENDER_SCALE;
				}
				sunSize = BobMathUtil.lerp(progress, sunSize, sunTargetSize);
			}
			double coronaSize = sunSize * (3 - Library.smoothstep(Math.abs(celestialPhase), 0.7, 0.8));

			CelestialBody orbiting = station.orbiting;
			if(station.state != StationState.ORBIT && progress > 0.5) orbiting = animationTarget;

			CBT_SkyState skyState = station.orbiting != null ? station.orbiting.getStar().getTrait(CBT_SkyState.class) : null;
			renderInjectorLinesPass(partialTicks, world, provider.metrics, orbiting, 1.0F, skyState);
			renderSun(partialTicks, world, mc, station.orbiting.getStar(), sunSize, coronaSize, 1, 0);

			renderCelestials(partialTicks, world, mc, provider.metrics, solarAngle, null, Vec3.createVectorHelper(0, 0, 0), 1, 1, orbiting, SolarSystem.MAX_APPARENT_SIZE_ORBIT, skyState);

		}
		GL11.glPopMatrix();


		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glEnable(GL11.GL_FOG);

		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDepthMask(true);
	}

	// All angles within are normalized to -180/180
	private float getCelestialAngle(WorldClient world, List<AstroMetric> metrics, float partialTicks, OrbitalStation station) {
		float solarAngle = world.getCelestialAngle(partialTicks);
		if(station.state == StationState.ORBIT) return solarAngle;

		solarAngle = solarAngle * 360.0F - 180.0F;

		if(station.state != StationState.ARRIVING) lastBody = station.orbiting;

		double progress = station.getUnscaledProgress(partialTicks);
		float travelAngle = -(float)SolarSystem.calculateSingleAngle(metrics, lastBody, station.getAnimationTarget());
		travelAngle = MathHelper.wrapAngleTo180_float(travelAngle + 90.0F);

		if(station.state == StationState.TRANSFER) {
			return (travelAngle + 180.0F) / 360.0F;
		} else if(station.state == StationState.LEAVING) {
			return ((float)BobMathUtil.clerp(progress, solarAngle, travelAngle) + 180.0F) / 360.0F;
		} else {
			return ((float)BobMathUtil.clerp(progress, travelAngle, solarAngle) + 180.0F) / 360.0F;
		}
	}

}

