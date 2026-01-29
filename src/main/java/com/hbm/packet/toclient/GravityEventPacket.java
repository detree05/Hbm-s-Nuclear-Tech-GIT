package com.hbm.packet.toclient;

import com.hbm.main.ModEventHandlerClient;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;

public class GravityEventPacket implements IMessage {

	private float nextGravity;

	public GravityEventPacket() { }

	public GravityEventPacket(float nextGravity) {
		this.nextGravity = nextGravity;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		nextGravity = buf.readFloat();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeFloat(nextGravity);
	}

	public static class Handler implements IMessageHandler<GravityEventPacket, IMessage> {

		@Override
		@SideOnly(Side.CLIENT)
		public IMessage onMessage(GravityEventPacket m, MessageContext ctx) {
			Minecraft mc = Minecraft.getMinecraft();
			ModEventHandlerClient.handleGravityEvent(mc, m.nextGravity);
			return null;
		}
	}
}
