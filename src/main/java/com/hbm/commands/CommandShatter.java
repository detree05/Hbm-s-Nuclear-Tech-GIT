package com.hbm.commands;

import com.hbm.dim.CelestialBody;
import com.hbm.dim.SolarSystem;
import com.hbm.dim.trait.CBT_Destroyed;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class CommandShatter extends CommandBase {

	@Override
	public String getCommandName() {
		return "ntmshatter";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/ntmshatter";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		CelestialBody minmus = CelestialBody.getBody("minmus");
		if(minmus == null || !"minmus".equals(minmus.name)) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Minmus body not found."));
			return;
		}

		boolean alreadyDestroyed = minmus.hasTrait(CBT_Destroyed.class);
		minmus.modifyTraits(new CBT_Destroyed());
		SolarSystem.applyMinmusShatterState();

		if(alreadyDestroyed) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "Minmus was already shattered. Post-shatter state ensured."));
		} else {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Minmus shattered."));
		}
	}
}
