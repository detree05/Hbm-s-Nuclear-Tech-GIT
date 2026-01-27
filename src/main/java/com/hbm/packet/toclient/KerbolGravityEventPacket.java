package com.hbm.packet.toclient;

import com.hbm.main.ModEventHandlerClient;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;

public class KerbolGravityEventPacket implements IMessage {

	private float nextGravity;

	public KerbolGravityEventPacket() { }

	public KerbolGravityEventPacket(float nextGravity) {
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

	public static class Handler implements IMessageHandler<KerbolGravityEventPacket, IMessage> {

		@Override
		@SideOnly(Side.CLIENT)
		public IMessage onMessage(KerbolGravityEventPacket m, MessageContext ctx) {
			Minecraft mc = Minecraft.getMinecraft();
			ModEventHandlerClient.handleKerbolGravityEvent(mc, m.nextGravity);
			return null;
		}
	}
}
