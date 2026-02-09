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

public class DfcDecaySkyPacket implements IMessage {

	private long worldTime;
	private int dimension;

	public DfcDecaySkyPacket() { }

	public DfcDecaySkyPacket(long worldTime, int dimension) {
		this.worldTime = worldTime;
		this.dimension = dimension;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		worldTime = buf.readLong();
		dimension = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeLong(worldTime);
		buf.writeInt(dimension);
	}

	public static class Handler implements IMessageHandler<DfcDecaySkyPacket, IMessage> {

		@Override
		@SideOnly(Side.CLIENT)
		public IMessage onMessage(DfcDecaySkyPacket m, MessageContext ctx) {
			Minecraft mc = Minecraft.getMinecraft();
			if(mc != null) {
				SkyProviderCelestial.startDfcDecayFlareEffect(m.worldTime, m.dimension);
				ModEventHandlerClient.dfcFlashTimestamp = System.currentTimeMillis();
				ModEventHandlerClient.shakeTimestamp = System.currentTimeMillis();
				mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("hbm:misc.dfcignited"), 0.4F));
			}
			return null;
		}
	}
}
