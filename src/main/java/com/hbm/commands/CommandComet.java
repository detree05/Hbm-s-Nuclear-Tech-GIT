package com.hbm.commands;

import com.hbm.main.ModEventHandler;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class CommandComet extends CommandBase {

	@Override
	public String getCommandName() {
		return "ntmcomet";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/ntmcomet [durationSeconds]";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if(!(sender instanceof EntityPlayer)) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "This command must be run by a player."));
			return;
		}

		EntityPlayer player = getCommandSenderAsPlayer(sender);
		if(!(player.worldObj.provider instanceof com.hbm.dim.WorldProviderEarth)) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Comets only appear in the Overworld (Kerbin)."));
			return;
		}
		int durationTicks = 20 * 40;
		if(args.length >= 1) {
			try {
				int seconds = Math.max(1, Integer.parseInt(args[0]));
				durationTicks = seconds * 20;
			} catch (NumberFormatException ignored) { }
		}

		ModEventHandler.triggerComet(player.worldObj, durationTicks);
		sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Summoned comet for " + (durationTicks / 20) + "s."));
	}
}
