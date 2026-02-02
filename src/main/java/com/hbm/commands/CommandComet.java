package com.hbm.commands;

import com.hbm.dim.SkyProviderCelestial;

import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.ClientCommandHandler;

public class CommandComet extends CommandBase {

	public static void register() {
		if(FMLLaunchHandler.side() != Side.CLIENT) return;
		ClientCommandHandler.instance.registerCommand(new CommandComet());
	}

	@Override
	public String getCommandName() {
		return "ntmcomet";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/ntmcomet [seconds]";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		Minecraft mc = Minecraft.getMinecraft();
		if(mc == null || mc.theWorld == null) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "No world loaded."));
			return;
		}

		long durationTicks = 600L;
		if(args.length >= 1) {
			if("help".equalsIgnoreCase(args[0])) {
				throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
			}
			int seconds = parseIntWithMin(sender, args[0], 1);
			durationTicks = seconds * 20L;
		}

		SkyProviderCelestial.forceComet(mc.theWorld, durationTicks);
		sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Comet summoned for " + (durationTicks / 20L) + "s."));
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 0;
	}
}
