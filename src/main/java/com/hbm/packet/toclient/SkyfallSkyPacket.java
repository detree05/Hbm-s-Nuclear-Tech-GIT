package com.hbm.packet.toclient;

import com.hbm.dim.SkyProviderCelestial;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;

public class SkyfallSkyPacket implements IMessage {

	private long worldTime;
	private int dimension;
	private int durationTicks;

	public SkyfallSkyPacket() { }

	public SkyfallSkyPacket(long worldTime, int dimension, int durationTicks) {
		this.worldTime = worldTime;
		this.dimension = dimension;
		this.durationTicks = durationTicks;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		worldTime = buf.readLong();
		dimension = buf.readInt();
		durationTicks = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeLong(worldTime);
		buf.writeInt(dimension);
		buf.writeInt(durationTicks);
	}

	public static class Handler implements IMessageHandler<SkyfallSkyPacket, IMessage> {

		@Override
		@SideOnly(Side.CLIENT)
		public IMessage onMessage(SkyfallSkyPacket m, MessageContext ctx) {
			Minecraft mc = Minecraft.getMinecraft();
			if(mc != null) {
				SkyProviderCelestial.startSkyfallEffect(m.worldTime, m.dimension, m.durationTicks);
			}
			return null;
		}
	}
}
