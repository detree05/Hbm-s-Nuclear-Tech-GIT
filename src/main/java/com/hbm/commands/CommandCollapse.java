package com.hbm.commands;

import com.hbm.dim.CelestialBody;
import com.hbm.dim.StarcoreSkyEffects;
import com.hbm.dim.trait.CBT_SkyState;
import com.hbm.tileentity.machine.TileEntityDysonLauncher;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

public class CommandCollapse extends CommandBase {

	private static final int OVERWORLD_DIMENSION = 0;

	@Override
	public String getCommandName() {
		return "ntmcollapse";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/ntmcollapse";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if(args.length != 0) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + getCommandUsage(sender)));
			return;
		}

		WorldServer world = resolveServerWorld(sender.getEntityWorld());
		if(world == null || world.provider == null) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "No valid server world available."));
			return;
		}
		if(!StarcoreSkyEffects.isBlackholeCollapseStartAllowedDimension(world.provider.dimensionId)) {
			sender.addChatMessage(new ChatComponentText(
				EnumChatFormatting.RED + "Blackhole collapse cannot be started from Nether, The End, or Dmitriy dimensions."
			));
			return;
		}

		CBT_SkyState skyState = CBT_SkyState.get(world);
		if(skyState == null) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Sky state is unavailable."));
			return;
		}
		if(!skyState.isBlackhole()) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Sky state is not BLACKHOLE. Collapse requires blackhole state."));
			return;
		}

		long now = world.getTotalWorldTime();
		long collapseEndTick = skyState.getBlackholeCollapseEndTick();
		if(collapseEndTick > now) {
			long remainingTicks = collapseEndTick - now;
			sender.addChatMessage(new ChatComponentText(
				EnumChatFormatting.YELLOW + "Blackhole collapse is already active (" + remainingTicks + " ticks remaining)."
			));
			return;
		}

		int clusterLimit = TileEntityDysonLauncher.BLACKHOLE_CLUSTER_LIMIT;
		int sent = skyState.getBlackholeClustersSent();
		if(sent < clusterLimit) {
			skyState.addBlackholeClusters(clusterLimit - sent);
			CelestialBody.getStar(world).modifyTraits(skyState);
		}

		boolean started = StarcoreSkyEffects.startBlackholeCollapse(world, skyState);
		if(started) {
			sender.addChatMessage(new ChatComponentText(
				EnumChatFormatting.DARK_RED + "Blackhole collapse triggered via antimatter-cluster path."
			));
		} else {
			sender.addChatMessage(new ChatComponentText(
				EnumChatFormatting.RED + "Collapse could not be triggered (state may already be transitioning)."
			));
		}
	}

	private static WorldServer resolveServerWorld(World senderWorld) {
		if(senderWorld instanceof WorldServer) {
			return (WorldServer) senderWorld;
		}
		WorldServer world = DimensionManager.getWorld(OVERWORLD_DIMENSION);
		if(world == null) {
			DimensionManager.initDimension(OVERWORLD_DIMENSION);
			world = DimensionManager.getWorld(OVERWORLD_DIMENSION);
		}
		return world;
	}
}
