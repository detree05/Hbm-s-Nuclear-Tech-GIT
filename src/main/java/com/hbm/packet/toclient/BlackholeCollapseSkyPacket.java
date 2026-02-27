package com.hbm.packet.toclient;

import com.hbm.dim.SkyProviderCelestial;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;

public class BlackholeCollapseSkyPacket implements IMessage {

	private long worldTime;
	private int dimension;

	public BlackholeCollapseSkyPacket() { }

	public BlackholeCollapseSkyPacket(long worldTime, int dimension) {
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

	public static class Handler implements IMessageHandler<BlackholeCollapseSkyPacket, IMessage> {

		@Override
		@SideOnly(Side.CLIENT)
		public IMessage onMessage(BlackholeCollapseSkyPacket m, MessageContext ctx) {
			Minecraft mc = Minecraft.getMinecraft();
			if(mc != null) {
				SkyProviderCelestial.startBlackholeCollapseEffect(m.worldTime, m.dimension);
				mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("hbm:misc.fartholeinit"), 1.0F));
			}
			return null;
		}
	}
}
