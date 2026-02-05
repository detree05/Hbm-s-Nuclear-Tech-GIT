package com.hbm.commands;

import com.hbm.dim.CelestialBody;
import com.hbm.dim.SolarSystem;
import com.hbm.dim.SolarSystemWorldSavedData;
import com.hbm.dim.trait.CBT_Destroyed;
import com.hbm.dim.trait.CelestialBodyTrait;
import java.util.HashMap;

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

		SolarSystemWorldSavedData data = SolarSystemWorldSavedData.get();
		HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits = data.getTraits(minmus.name);
		if(traits == null) {
			traits = new HashMap<>();
			traits.putAll(minmus.getTraits());
		}
		traits.put(CBT_Destroyed.class, new CBT_Destroyed());
		data.setTraits(minmus.name, traits.values().toArray(new CelestialBodyTrait[traits.size()]));
		SolarSystem.applyMinmusShatterState();

		if(alreadyDestroyed) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "Minmus was already shattered. Post-shatter state ensured."));
		} else {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Minmus shattered."));
		}
	}
}
