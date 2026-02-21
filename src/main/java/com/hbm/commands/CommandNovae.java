package com.hbm.commands;

import java.util.Random;

import com.hbm.config.SpaceConfig;
import com.hbm.dim.CelestialBody;
import com.hbm.dim.SolarSystem;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toclient.SupernovaeSkyPacket;
import com.hbm.saveddata.NovaeSavedData;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

public class CommandNovae extends CommandBase {
	private static final int MAX_VISIBLE_ATTEMPTS = 64;
	private static final float MIN_HORIZON_DOT = 0.08F;

	private static final class NovaOrientation {
		private final float yaw;
		private final float pitch;
		private final float roll;

		private NovaOrientation(float yaw, float pitch, float roll) {
			this.yaw = yaw;
			this.pitch = pitch;
			this.roll = roll;
		}
	}

	@Override
	public String getCommandName() {
		return "ntmnovae";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/ntmnovae";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if(sender.getEntityWorld() == null) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "No world available."));
			return;
		}

		triggerNovaeAcrossDimensions();
		sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "kaboom."));
	}

	public static void triggerNovaeAcrossDimensions() {

		Integer[] dimensionIds = DimensionManager.getStaticDimensionIDs();
		for(int dimensionId : dimensionIds) {
			if(dimensionId == SpaceConfig.dmitriyDimension) {
				continue;
			}

			WorldServer world = DimensionManager.getWorld(dimensionId);
			if(world == null) {
				DimensionManager.initDimension(dimensionId);
				world = DimensionManager.getWorld(dimensionId);
			}
			if(world == null) {
				continue;
			}

			Random rand = world.rand != null ? world.rand : new Random();
			NovaOrientation orientation = pickVisibleOrientation(world, rand);
			float yaw = orientation.yaw;
			float pitch = orientation.pitch;
			float roll = orientation.roll;
			float r = 0.6F + rand.nextFloat() * 0.4F;
			float g = 0.6F + rand.nextFloat() * 0.4F;
			float b = 0.6F + rand.nextFloat() * 0.4F;
			float sizeScale = 0.3F + rand.nextFloat() * 0.4F;

			long worldTime = world.getTotalWorldTime();

			NovaeSavedData data = NovaeSavedData.forWorld(world);
			data.addEntry(worldTime, yaw, pitch, roll, r, g, b, sizeScale);

			PacketDispatcher.wrapper.sendToDimension(
				new SupernovaeSkyPacket(worldTime, dimensionId, yaw, pitch, roll, r, g, b, sizeScale, true),
				dimensionId
			);
		}
	}

	private static NovaOrientation pickVisibleOrientation(WorldServer world, Random rand) {
		CelestialBody body = CelestialBody.getBody(world);
		float axialTilt = body != null ? body.axialTilt : 0.0F;
		float siderealAngle = body != null ? (float)SolarSystem.calculateSiderealAngle(world, 0.0F, body) : 0.0F;

		for(int i = 0; i < MAX_VISIBLE_ATTEMPTS; i++) {
			float yaw = rand.nextFloat() * 360.0F;
			float pitch = -75.0F + rand.nextFloat() * 150.0F;
			float roll = rand.nextFloat() * 360.0F;
			if(isAboveHorizon(yaw, pitch, roll, axialTilt, siderealAngle)) {
				return new NovaOrientation(yaw, pitch, roll);
			}
		}

		float yaw = rand.nextFloat() * 360.0F;
		float pitch = -75.0F + rand.nextFloat() * 150.0F;
		float roll = rand.nextFloat() * 360.0F;
		return new NovaOrientation(yaw, pitch, roll);
	}

	private static boolean isAboveHorizon(float yaw, float pitch, float roll, float axialTilt, float siderealAngle) {
		Vec3 center = Vec3.createVectorHelper(0.0D, 1.0D, 0.0D);

		// Matches the render transform chain for novae center direction.
		center.rotateAroundZ((float)Math.toRadians(roll));
		center.rotateAroundY((float)Math.toRadians(yaw));
		center.rotateAroundX((float)Math.toRadians(pitch));
		center.rotateAroundX((float)Math.toRadians(-90.0F));
		center.rotateAroundX((float)Math.toRadians(siderealAngle * 360.0F));
		center.rotateAroundY((float)Math.toRadians(-90.0F));
		center.rotateAroundX((float)Math.toRadians(axialTilt));

		return center.yCoord > MIN_HORIZON_DOT;
	}
}

