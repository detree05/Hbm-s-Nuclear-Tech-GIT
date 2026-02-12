package com.hbm.commands;

import com.hbm.entity.mob.EntityVoidStaresBack;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class CommandVoidStaresBackChase extends CommandBase {

	@Override
	public String getCommandName() {
		return "ntmstaresback";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/ntmstaresback";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if(!(sender instanceof EntityPlayer)) {
			throw new PlayerNotFoundException();
		}

		EntityPlayer player = (EntityPlayer) sender;
		EntityVoidStaresBack entity = new EntityVoidStaresBack(player.worldObj);
		double[] pos = CommandVoidStaresBack.getSpawnPosition(player);
		entity.setPositionAndRotation(pos[0], pos[1], pos[2], player.rotationYaw, 0.0F);
		entity.startChasing(player);
		player.worldObj.spawnEntityInWorld(entity);
		sender.addChatMessage(new ChatComponentText(EnumChatFormatting.DARK_PURPLE + "It begins to approach."));
	}
}
