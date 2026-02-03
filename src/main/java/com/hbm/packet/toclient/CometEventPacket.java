package com.hbm.packet.toclient;

import com.hbm.dim.SkyProviderCelestial;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public class CometEventPacket implements IMessage {

	private long worldTime;
	private int durationTicks;
	private int dimension;
	private float yaw;
	private float pitch;
	private float roll;

	public CometEventPacket() { }

	public CometEventPacket(long worldTime, int durationTicks, int dimension, float yaw, float pitch, float roll) {
		this.worldTime = worldTime;
		this.durationTicks = durationTicks;
		this.dimension = dimension;
		this.yaw = yaw;
		this.pitch = pitch;
		this.roll = roll;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		worldTime = buf.readLong();
		durationTicks = buf.readInt();
		dimension = buf.readInt();
		yaw = buf.readFloat();
		pitch = buf.readFloat();
		roll = buf.readFloat();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeLong(worldTime);
		buf.writeInt(durationTicks);
		buf.writeInt(dimension);
		buf.writeFloat(yaw);
		buf.writeFloat(pitch);
		buf.writeFloat(roll);
	}

	public static class Handler implements IMessageHandler<CometEventPacket, IMessage> {

		@Override
		@SideOnly(Side.CLIENT)
		public IMessage onMessage(CometEventPacket m, MessageContext ctx) {
			SkyProviderCelestial.startComet(m.worldTime, m.durationTicks, m.dimension, m.yaw, m.pitch, m.roll);
			return null;
		}
	}
}
