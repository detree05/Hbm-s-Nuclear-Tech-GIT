package com.hbm.commands;

import java.util.Random;

import com.hbm.config.SpaceConfig;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toclient.SupernovaeSkyPacket;
import com.hbm.saveddata.NovaeSavedData;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

public class CommandNovae extends CommandBase {

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

		Integer[] dimensionIds = DimensionManager.getStaticDimensionIDs();
		for(int dimensionId : dimensionIds) {
			if(dimensionId == SpaceConfig.kerbolDimension) {
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
			float yaw = rand.nextFloat() * 360.0F;
			float pitch = -75.0F + rand.nextFloat() * 150.0F;
			float roll = rand.nextFloat() * 360.0F;

			long worldTime = world.getTotalWorldTime();

			NovaeSavedData data = NovaeSavedData.forWorld(world);
			data.active = true;
			data.startWorldTime = worldTime;
			data.yaw = yaw;
			data.pitch = pitch;
			data.roll = roll;
			data.markDirty();

			PacketDispatcher.wrapper.sendToDimension(
				new SupernovaeSkyPacket(worldTime, dimensionId, yaw, pitch, roll, true),
				dimensionId
			);
		}

		sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "kaboom."));
	}
}
