package com.hbm.packet.toclient;

import com.hbm.dim.SkyProviderCelestial;
import com.hbm.main.ModEventHandlerClient;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;

public class SupernovaeSkyPacket implements IMessage {

	private long worldTime;
	private int dimension;
	private float yaw;
	private float pitch;
	private float roll;
	private boolean playEffects;

	public SupernovaeSkyPacket() { }

	public SupernovaeSkyPacket(long worldTime, int dimension, float yaw, float pitch, float roll, boolean playEffects) {
		this.worldTime = worldTime;
		this.dimension = dimension;
		this.yaw = yaw;
		this.pitch = pitch;
		this.roll = roll;
		this.playEffects = playEffects;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		worldTime = buf.readLong();
		dimension = buf.readInt();
		yaw = buf.readFloat();
		pitch = buf.readFloat();
		roll = buf.readFloat();
		playEffects = buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeLong(worldTime);
		buf.writeInt(dimension);
		buf.writeFloat(yaw);
		buf.writeFloat(pitch);
		buf.writeFloat(roll);
		buf.writeBoolean(playEffects);
	}

	public static class Handler implements IMessageHandler<SupernovaeSkyPacket, IMessage> {

		@Override
		@SideOnly(Side.CLIENT)
		public IMessage onMessage(SupernovaeSkyPacket m, MessageContext ctx) {
			Minecraft mc = Minecraft.getMinecraft();
			if(mc != null) {
				SkyProviderCelestial.startNovaeEffect(m.worldTime, m.dimension, m.yaw, m.pitch, m.roll);
				if(m.playEffects) {
					ModEventHandlerClient.flashTimestamp = System.currentTimeMillis();
					ModEventHandlerClient.shakeTimestamp = System.currentTimeMillis();
					mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("hbm:misc.a7"), 1.0F));
				}
			}
			return null;
		}
	}
}
