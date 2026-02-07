package com.hbm.commands;

import com.hbm.dim.trait.CBT_SkyState;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

public class CommandDfc extends CommandBase {

	@Override
	public String getCommandName() {
		return "ntmdfc";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/ntmdfc";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		World world = sender.getEntityWorld();
		if(world == null) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "No world available."));
			return;
		}

		CBT_SkyState skyState = CBT_SkyState.get(world);
		if(skyState.getState() != CBT_SkyState.SkyState.DFC) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "Sky state is not DFC."));
			return;
		}

		CBT_SkyState.setState(world, CBT_SkyState.SkyState.SUN);
		sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Sky state set to SUN."));
	}
}
