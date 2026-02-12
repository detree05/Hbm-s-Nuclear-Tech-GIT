package com.hbm.commands;

import com.hbm.entity.mob.EntityVoidStaresBack;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class CommandVoidStaresBack extends CommandBase {

	@Override
	public String getCommandName() {
		return "ntmvoid";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/ntmvoid";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if(!(sender instanceof EntityPlayer)) {
			throw new PlayerNotFoundException();
		}

		EntityPlayer player = (EntityPlayer) sender;
		EntityVoidStaresBack entity = new EntityVoidStaresBack(player.worldObj);
		entity.setPositionAndRotation(player.posX, player.posY + 1.0D, player.posZ, player.rotationYaw, 0.0F);
		player.worldObj.spawnEntityInWorld(entity);
		sender.addChatMessage(new ChatComponentText(EnumChatFormatting.DARK_PURPLE + "The void stares back."));
	}
}
