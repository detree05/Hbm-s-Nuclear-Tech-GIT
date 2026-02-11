package com.hbm.commands;

import com.hbm.dim.WorldProviderCelestial;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;

public class CommandNtmDaytime extends CommandBase {

	@Override
	public String getCommandName() {
		return "ntmdaytime";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/ntmdaytime";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if(args.length != 0) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + getCommandUsage(sender)));
			return;
		}

		World world = sender.getEntityWorld();
		WorldProvider provider = world != null ? world.provider : null;

		boolean eclipse = false;
		if(provider instanceof WorldProviderCelestial) {
			eclipse = ((WorldProviderCelestial) provider).isEclipse();
		}

		long worldTime = world != null ? world.getWorldTime() : 0L;
		long timeOfDay = worldTime % 24000L;
		boolean timeDay = timeOfDay < 12000L;

		float angle = world != null ? world.getCelestialAngleRadians(0.0F) : 0.0F;
		boolean orbitDay = MathHelper.cos(angle) > 0.0F;
		boolean isDaytime = !eclipse && orbitDay;

		String providerName = provider != null ? provider.getClass().getSimpleName() : "null";
		boolean doDaylight = world != null && world.getGameRules().getGameRuleBooleanValue("doDaylightCycle");
		boolean hasNoSky = provider != null && provider.hasNoSky;

		sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "ntmdaytime: "
			+ EnumChatFormatting.WHITE + (isDaytime ? "DAY" : "NIGHT")));
		sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "provider="
			+ EnumChatFormatting.WHITE + providerName
			+ EnumChatFormatting.GRAY + " eclipse="
			+ EnumChatFormatting.WHITE + eclipse));
		sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "worldTime="
			+ EnumChatFormatting.WHITE + worldTime
			+ EnumChatFormatting.GRAY + " timeOfDay="
			+ EnumChatFormatting.WHITE + timeOfDay
			+ EnumChatFormatting.GRAY + " timeDay="
			+ EnumChatFormatting.WHITE + timeDay));
		sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "celestialAngle="
			+ EnumChatFormatting.WHITE + angle
			+ EnumChatFormatting.GRAY + " orbitDay="
			+ EnumChatFormatting.WHITE + orbitDay));
		sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "doDaylightCycle="
			+ EnumChatFormatting.WHITE + doDaylight
			+ EnumChatFormatting.GRAY + " hasNoSky="
			+ EnumChatFormatting.WHITE + hasNoSky));
	}
}
