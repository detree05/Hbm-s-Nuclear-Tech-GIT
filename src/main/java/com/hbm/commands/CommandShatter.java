package com.hbm.commands;

import com.hbm.dim.CelestialBody;
import com.hbm.dim.SolarSystem;
import com.hbm.dim.SolarSystemWorldSavedData;
import com.hbm.config.SpaceConfig;
import com.hbm.dim.trait.CBT_Destroyed;
import com.hbm.dim.trait.CelestialBodyTrait;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toclient.SkyfallSkyPacket;
import java.util.HashMap;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

public class CommandShatter extends CommandBase {
	private static final int OVERWORLD_DIMENSION = 0;
	private static final int SKYFALL_DURATION_TICKS = 20 * 60;
	private static final int SKYFALL_DELAY_TICKS = 10 * 20;

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
		WorldServer overworld = getOrLoadWorld(OVERWORLD_DIMENSION);
		long shatterWorldTime = overworld != null ? overworld.getTotalWorldTime() : -1L;

		SolarSystemWorldSavedData data = SolarSystemWorldSavedData.get();
		HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits = data.getTraits(minmus.name);
		if(traits == null) {
			traits = new HashMap<>();
			traits.putAll(minmus.getTraits());
		}
		CBT_Destroyed destroyedTrait = (CBT_Destroyed) traits.get(CBT_Destroyed.class);
		if(destroyedTrait == null) {
			destroyedTrait = new CBT_Destroyed();
		}
		if(shatterWorldTime >= 0L && !destroyedTrait.hasMunRingTransitionStarted() && !destroyedTrait.areMunRingsEnabled()) {
			destroyedTrait.beginMunRingTransition(shatterWorldTime);
		}
		traits.put(CBT_Destroyed.class, destroyedTrait);
		data.setTraits(minmus.name, traits.values().toArray(new CelestialBodyTrait[traits.size()]));
		SolarSystem.applyMinmusShatterState(overworld);
		scheduleOverworldSkyfall();

		if(alreadyDestroyed) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "Minmus was already shattered. Post-shatter state ensured."));
		} else {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Minmus shattered."));
		}
		sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "Skyfall will begin in 10 seconds (Overworld + Mun)."));
	}

	private static void scheduleOverworldSkyfall() {
		scheduleSkyfallForDimension(OVERWORLD_DIMENSION);
		scheduleSkyfallForDimension(SpaceConfig.moonDimension);
	}

	private static void scheduleSkyfallForDimension(int dimensionId) {
		WorldServer world = getOrLoadWorld(dimensionId);
		if(world == null) {
			return;
		}

		long startTime = world.getTotalWorldTime() + SKYFALL_DELAY_TICKS;
		PacketDispatcher.wrapper.sendToDimension(
			new SkyfallSkyPacket(startTime, dimensionId, SKYFALL_DURATION_TICKS),
			dimensionId
		);
	}

	private static WorldServer getOrLoadWorld(int dimensionId) {
		WorldServer world = DimensionManager.getWorld(dimensionId);
		if(world == null) {
			DimensionManager.initDimension(dimensionId);
			world = DimensionManager.getWorld(dimensionId);
		}
		return world;
	}
}
