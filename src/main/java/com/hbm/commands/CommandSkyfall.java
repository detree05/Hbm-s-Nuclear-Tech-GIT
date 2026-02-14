package com.hbm.commands;

import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toclient.SkyfallSkyPacket;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

public class CommandSkyfall extends CommandBase {

	private static final int OVERWORLD_DIMENSION = 0;
	private static final int DURATION_TICKS = 20 * 60;

	@Override
	public String getCommandName() {
		return "ntmskyfall";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/ntmskyfall";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if(args.length != 0) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + getCommandUsage(sender)));
			return;
		}

		WorldServer world = DimensionManager.getWorld(OVERWORLD_DIMENSION);
		if(world == null) {
			DimensionManager.initDimension(OVERWORLD_DIMENSION);
			world = DimensionManager.getWorld(OVERWORLD_DIMENSION);
		}

		if(world == null) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Overworld is not available."));
			return;
		}

		PacketDispatcher.wrapper.sendToDimension(
			new SkyfallSkyPacket(world.getTotalWorldTime(), OVERWORLD_DIMENSION, DURATION_TICKS),
			OVERWORLD_DIMENSION
		);

		sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "Skyfall started in the Overworld for 60 seconds."));
	}
}
