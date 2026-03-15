package com.hbm.render.tileentity;

import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.config.SpaceConfig;
import com.hbm.dim.CelestialBody;
import com.hbm.dim.WorldProviderCelestial;
import com.hbm.dim.trait.CBT_SkyState;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.render.util.BeamPronter;
import com.hbm.render.util.BeamPronter.EnumBeamType;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.hbm.tileentity.machine.TileEntityStarCoreEnergyEmitter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer;

public class RenderStarCoreEnergyEmitter extends TileEntitySpecialRenderer implements IItemRendererProvider {

	private static final float GUN_PITCH_MAX_UP = 90.0F;
	private static final float GUN_PITCH_MAX_DOWN = -35.0F;
	private static final float TABLE_YAW_SPEED_DEG_PER_TICK = 2.0F;
	private static final float GUN_PITCH_SPEED_DEG_PER_TICK = 2.0F;
	private static final float MAX_STEP_TICKS = 1.0F;
	private static final long STATE_CLEANUP_INTERVAL_TICKS = 200L;
	private static final long STATE_STALE_TICKS = 200L;
	private static final double GUN_PIVOT_X = 0.0D;
	private static final double GUN_PIVOT_Y = 5.0D;
	private static final double GUN_PIVOT_Z = 0.0D;
	private static final double GUN_BEAM_ORIGIN_X = 3.0D;
	private static final double GUN_BEAM_ORIGIN_Y = 4.97D;
	private static final double GUN_BEAM_ORIGIN_Z = 0.0D;
	private static final float EMITTER_BEAM_MAX_LENGTH = 256.0F;
	private static final float EMITTER_BEAM_BASE_THICKNESS = 0.35F;
	private static final float EMITTER_BEAM_BASE_ALPHA = 0.55F;
	private static final float EMITTER_BEAM_VISIBILITY_DISTANCE_SQ = 512.0F * 512.0F;
	private static final float EMITTER_BEAM_HIGH_DETAIL_DISTANCE_SQ = 96.0F * 96.0F;
	private static final float GUN_SHAKE_OFFSET_X = 0.015F;
	private static final float GUN_SHAKE_OFFSET_Y = 0.006F;
	private static final float GUN_SHAKE_ROT_X = 0.05F;
	private static final float GUN_SHAKE_ROT_Z = 0.10F;
	private static final Map<String, SmoothAimState> SMOOTH_AIM_STATES = new HashMap<String, SmoothAimState>();
	private static long lastCleanupTick = Long.MIN_VALUE;

	private static class AimAngles {
		final float tableYaw;
		final float gunPitch;

		AimAngles(float tableYaw, float gunPitch) {
			this.tableYaw = tableYaw;
			this.gunPitch = gunPitch;
		}
	}

	private static class SmoothAimState {
		float tableYaw;
		float gunPitch;
		double lastRenderTime;
		long lastSeenWorldTick;
		boolean initialized;
	}

	@Override
	public void renderTileEntityAt(TileEntity te, double x, double y, double z, float interp) {

		GL11.glPushMatrix();
		GL11.glTranslated(x + 0.5, y, z + 0.5);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_NORMALIZE);
		GL11.glShadeModel(GL11.GL_SMOOTH);

		GL11.glRotatef(90, 0F, 1F, 0F);

		int meta = te.getBlockMetadata();
		if(meta >= BlockDummyable.offset) {
			meta -= BlockDummyable.offset;
		}

		switch(meta) {
		case 0:
			GL11.glTranslated(0.0D, 0.5D, -0.5D);
			GL11.glRotatef(90, 1F, 0F, 0F); break;
		case 1:
			GL11.glTranslated(0.0D, 0.5D, 0.5D);
			GL11.glRotatef(90, -1F, 0F, 0F); break;
		case 2:
			GL11.glRotatef(90, 0F, 1F, 0F); break;
		case 4:
			GL11.glRotatef(180, 0F, 1F, 0F); break;
		case 3:
			GL11.glRotatef(270, 0F, 1F, 0F); break;
		case 5:
			GL11.glRotatef(0, 0F, 1F, 0F); break;
		}

		AimAngles aim = getSmoothedAimAngles(te, interp, meta);
		boolean beamActive = isEmitterWorking(te) && isEmitterAimLocked(te);

		bindTexture(ResourceManager.star_core_energy_emitter_tex);
		ResourceManager.star_core_energy_emitter.renderPart("Body");

		GL11.glPushMatrix();
		GL11.glRotatef(aim.tableYaw, 0.0F, 1.0F, 0.0F);
		ResourceManager.star_core_energy_emitter.renderPart("Table");

		GL11.glPushMatrix();
		GL11.glTranslated(GUN_PIVOT_X, GUN_PIVOT_Y, GUN_PIVOT_Z);
		GL11.glRotatef(aim.gunPitch, 0.0F, 0.0F, 1.0F);
		GL11.glTranslated(-GUN_PIVOT_X, -GUN_PIVOT_Y, -GUN_PIVOT_Z);

		GL11.glPushMatrix();
		applyWorkingGunShake(te, interp, beamActive);
		ResourceManager.star_core_energy_emitter.renderPart("Gun");
		GL11.glPopMatrix();

		renderWorkingBeam(te, interp, beamActive);
		GL11.glPopMatrix();
		GL11.glPopMatrix();

		GL11.glShadeModel(GL11.GL_FLAT);
		GL11.glDisable(GL11.GL_NORMALIZE);

		GL11.glEnable(GL11.GL_LIGHTING);

		GL11.glPopMatrix();
	}

	private static AimAngles getSmoothedAimAngles(TileEntity te, float partialTicks, int meta) {
		AimAngles target = getTargetAimAngles(te, partialTicks, meta);
		if(te == null || te.getWorldObj() == null) {
			return target;
		}

		String key = getStateKey(te);
		SmoothAimState smooth = SMOOTH_AIM_STATES.get(key);
		if(smooth == null) {
			smooth = new SmoothAimState();
			SMOOTH_AIM_STATES.put(key, smooth);
		}

		long worldTick = te.getWorldObj().getTotalWorldTime();
		double renderTime = worldTick + partialTicks;
		float dt = smooth.initialized ? (float) Math.max(0.0D, renderTime - smooth.lastRenderTime) : 0.0F;
		dt = Math.min(dt, MAX_STEP_TICKS);

		if(!smooth.initialized) {
			smooth.tableYaw = target.tableYaw;
			smooth.gunPitch = target.gunPitch;
			smooth.initialized = true;
		} else if(dt > 0.0F) {
			float yawDiff = MathHelper.wrapAngleTo180_float(target.tableYaw - smooth.tableYaw);
			float yawStep = TABLE_YAW_SPEED_DEG_PER_TICK * dt;
			yawDiff = MathHelper.clamp_float(yawDiff, -yawStep, yawStep);
			smooth.tableYaw = MathHelper.wrapAngleTo180_float(smooth.tableYaw + yawDiff);

			float pitchDiff = target.gunPitch - smooth.gunPitch;
			float pitchStep = GUN_PITCH_SPEED_DEG_PER_TICK * dt;
			pitchDiff = MathHelper.clamp_float(pitchDiff, -pitchStep, pitchStep);
			smooth.gunPitch = MathHelper.clamp_float(smooth.gunPitch + pitchDiff, GUN_PITCH_MAX_DOWN, GUN_PITCH_MAX_UP);
		}

		smooth.lastRenderTime = renderTime;
		smooth.lastSeenWorldTick = worldTick;

		if(lastCleanupTick == Long.MIN_VALUE || worldTick - lastCleanupTick >= STATE_CLEANUP_INTERVAL_TICKS) {
			cleanupSmoothStates(worldTick);
		}

		return new AimAngles(smooth.tableYaw, smooth.gunPitch);
	}

	private static void cleanupSmoothStates(long nowTick) {
		lastCleanupTick = nowTick;
		Iterator<Map.Entry<String, SmoothAimState>> it = SMOOTH_AIM_STATES.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<String, SmoothAimState> entry = it.next();
			SmoothAimState state = entry.getValue();
			if(state == null || nowTick - state.lastSeenWorldTick > STATE_STALE_TICKS) {
				it.remove();
			}
		}
	}

	private static String getStateKey(TileEntity te) {
		return te.getWorldObj().provider.dimensionId + ":" + te.xCoord + ":" + te.yCoord + ":" + te.zCoord;
	}

	private static AimAngles getTargetAimAngles(TileEntity te, float partialTicks, int meta) {
		if(te == null || te.getWorldObj() == null) {
			return new AimAngles(0.0F, GUN_PITCH_MAX_UP);
		}
		if(te.getWorldObj().provider != null && te.getWorldObj().provider.dimensionId == SpaceConfig.orbitDimension) {
			return new AimAngles(0.0F, GUN_PITCH_MAX_UP);
		}

		CBT_SkyState skyState = CBT_SkyState.get(te.getWorldObj());
		CBT_SkyState.SkyState state = skyState.getState();
		if((state != CBT_SkyState.SkyState.STARCORE && state != CBT_SkyState.SkyState.SUN) || !isWorldDaytime(te.getWorldObj())) {
			return new AimAngles(0.0F, GUN_PITCH_MAX_UP);
		}

		Vec3 sunDirWorld = getSunDirectionWorld(te, partialTicks);
		if(sunDirWorld == null || sunDirWorld.lengthVector() <= 1.0E-6D) {
			return new AimAngles(0.0F, GUN_PITCH_MAX_UP);
		}

		Vec3 sunDirLocal = toEmitterLocal(sunDirWorld.normalize(), normalizeMeta(meta));
		if(sunDirLocal.lengthVector() <= 1.0E-6D) {
			return new AimAngles(0.0F, GUN_PITCH_MAX_UP);
		}

		sunDirLocal = sunDirLocal.normalize();
		float tableYaw = (float) Math.toDegrees(Math.atan2(-sunDirLocal.zCoord, sunDirLocal.xCoord));
		float gunPitch = (float) Math.toDegrees(Math.asin(MathHelper.clamp_double(sunDirLocal.yCoord, -1.0D, 1.0D)));
		gunPitch = MathHelper.clamp_float(gunPitch, GUN_PITCH_MAX_DOWN, GUN_PITCH_MAX_UP);

		return new AimAngles(MathHelper.wrapAngleTo180_float(tableYaw), gunPitch);
	}

	private static void renderWorkingBeam(TileEntity te, float partialTicks, boolean beamActive) {
		if(!beamActive) {
			return;
		}

		Minecraft mc = Minecraft.getMinecraft();
		if(mc == null || mc.thePlayer == null) {
			return;
		}

		double dx = mc.thePlayer.posX - (te.xCoord + 0.5D);
		double dy = mc.thePlayer.posY - (te.yCoord + 1.5D);
		double dz = mc.thePlayer.posZ - (te.zCoord + 0.5D);
		double distanceSq = dx * dx + dy * dy + dz * dz;
		if(distanceSq > EMITTER_BEAM_VISIBILITY_DISTANCE_SQ) {
			return;
		}
		boolean highDetail = distanceSq <= EMITTER_BEAM_HIGH_DETAIL_DISTANCE_SQ;
		int segmentCount = 1;
		float beamThickness = highDetail ? EMITTER_BEAM_BASE_THICKNESS : EMITTER_BEAM_BASE_THICKNESS * 0.75F;
		float beamAlpha = highDetail ? EMITTER_BEAM_BASE_ALPHA : EMITTER_BEAM_BASE_ALPHA * 0.7F;

		float t = te.getWorldObj().getTotalWorldTime() + partialTicks;
		float hue = (t * 0.005F) % 1.0F;
		int outerColor = Color.HSBtoRGB(hue, 1.0F, 1.0F) & 0xFFFFFF;
		int innerColor = Color.HSBtoRGB((hue + 0.08F) % 1.0F, 1.0F, 1.0F) & 0xFFFFFF;

		GL11.glPushAttrib(GL11.GL_LIGHTING_BIT);
		GL11.glDisable(GL11.GL_LIGHTING);
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);

		GL11.glPushMatrix();
		GL11.glTranslated(GUN_BEAM_ORIGIN_X, GUN_BEAM_ORIGIN_Y, GUN_BEAM_ORIGIN_Z);
		Vec3 skeleton = Vec3.createVectorHelper(EMITTER_BEAM_MAX_LENGTH, 0.0D, 0.0D);
		BeamPronter.prontBeamwithDepth(
			skeleton,
			EnumWaveType.SPIRAL,
			EnumBeamType.CYLINDER,
			outerColor,
			innerColor,
			0,
			segmentCount,
			0.0F,
			2,
			beamThickness,
			beamAlpha
		);
		GL11.glPopMatrix();

		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glPopAttrib();
	}

	private static void applyWorkingGunShake(TileEntity te, float partialTicks, boolean beamActive) {
		if(!beamActive) {
			return;
		}

		float t = te.getWorldObj().getTotalWorldTime() + partialTicks;
		float phase = te.xCoord * 0.37F + te.yCoord * 0.19F + te.zCoord * 0.29F;

		float jitterX = (float) Math.sin((t + phase) * 2.8F) * GUN_SHAKE_OFFSET_X;
		float jitterY = (float) Math.sin((t + phase * 1.3F) * 3.6F) * GUN_SHAKE_OFFSET_Y;
		float jitterRotX = (float) Math.cos((t + phase * 1.1F) * 3.1F) * GUN_SHAKE_ROT_X;
		float jitterRotZ = (float) Math.sin((t + phase * 0.7F) * 4.2F) * GUN_SHAKE_ROT_Z;

		GL11.glTranslatef(jitterX, jitterY, 0.0F);
		GL11.glRotatef(jitterRotX, 1.0F, 0.0F, 0.0F);
		GL11.glRotatef(jitterRotZ, 0.0F, 0.0F, 1.0F);
	}

	private static boolean isEmitterWorking(TileEntity te) {
		if(!(te instanceof TileEntityStarCoreEnergyEmitter) || te.getWorldObj() == null) {
			return false;
		}

		TileEntityStarCoreEnergyEmitter emitter = (TileEntityStarCoreEnergyEmitter) te;
		if(emitter.getThroughputPerFiveTicks() <= 0L) {
			return false;
		}

		CBT_SkyState.SkyState state = CBT_SkyState.get(te.getWorldObj()).getState();
		if(state != CBT_SkyState.SkyState.STARCORE && state != CBT_SkyState.SkyState.SUN) {
			return false;
		}

		return isWorldDaytime(te.getWorldObj());
	}

	private static boolean isEmitterAimLocked(TileEntity te) {
		return te instanceof TileEntityStarCoreEnergyEmitter && ((TileEntityStarCoreEnergyEmitter) te).isClientAimLocked();
	}

	private static boolean isWorldDaytime(World world) {
		if(world == null) return false;
		if(world.provider instanceof WorldProviderCelestial && ((WorldProviderCelestial) world.provider).isEclipse()) {
			return false;
		}
		float angle = world.getCelestialAngleRadians(0.0F);
		return MathHelper.cos(angle) > 0.0F;
	}

	private static int normalizeMeta(int meta) {
		return meta >= BlockDummyable.offset ? meta - BlockDummyable.offset : meta;
	}

	private static Vec3 getSunDirectionWorld(TileEntity te, float partialTicks) {
		float solarAngle = te.getWorldObj().getCelestialAngle(partialTicks);
		float axialTilt = 0.0F;

		if(te.getWorldObj().provider instanceof WorldProviderCelestial) {
			axialTilt = CelestialBody.getBody(te.getWorldObj()).axialTilt;
		}

		Vec3 dir = Vec3.createVectorHelper(0.0D, 1.0D, 0.0D);
		dir = rotateX(dir, solarAngle * 360.0F);
		dir = rotateY(dir, -90.0F);
		if(axialTilt != 0.0F) {
			dir = rotateX(dir, axialTilt);
		}
		return dir.normalize();
	}

	private static Vec3 toEmitterLocal(Vec3 worldDir, int meta) {
		Vec3 local = rotateY(worldDir, -90.0F);

		switch(meta) {
		case 0:
			return rotateX(local, -90.0F);
		case 1:
			return rotateX(local, 90.0F);
		case 2:
			return rotateY(local, -90.0F);
		case 4:
			return rotateY(local, -180.0F);
		case 3:
			return rotateY(local, -270.0F);
		case 5:
		default:
			return local;
		}
	}

	private static Vec3 rotateX(Vec3 v, float degrees) {
		double rad = Math.toRadians(degrees);
		double cos = Math.cos(rad);
		double sin = Math.sin(rad);
		double y = v.yCoord * cos - v.zCoord * sin;
		double z = v.yCoord * sin + v.zCoord * cos;
		return Vec3.createVectorHelper(v.xCoord, y, z);
	}

	private static Vec3 rotateY(Vec3 v, float degrees) {
		double rad = Math.toRadians(degrees);
		double cos = Math.cos(rad);
		double sin = Math.sin(rad);
		double x = v.xCoord * cos + v.zCoord * sin;
		double z = -v.xCoord * sin + v.zCoord * cos;
		return Vec3.createVectorHelper(x, v.yCoord, z);
	}

	@Override
	public Item getItemForRenderer() {
		return Item.getItemFromBlock(ModBlocks.star_core_energy_emitter);
	}

	@Override
	public IItemRenderer getRenderer() {
		return new ItemRenderBase() {
			public void renderInventory() {
				GL11.glTranslated(0, -2.5, 0);
				double scale = 4.2;
				GL11.glScaled(scale, scale, scale);
			}
			public void renderCommon() {
				GL11.glScaled(0.5D, 0.5D, 0.5D);
				GL11.glRotated(90, 0, 1, 0);
				GL11.glRotatef(0F, 0F, 0F, 0F);
				GL11.glEnable(GL11.GL_NORMALIZE);
				GL11.glShadeModel(GL11.GL_SMOOTH);
				bindTexture(ResourceManager.star_core_energy_emitter_tex);
				ResourceManager.star_core_energy_emitter.renderAll();
				GL11.glShadeModel(GL11.GL_FLAT);
				GL11.glDisable(GL11.GL_NORMALIZE);
			}
		};
	}
}
