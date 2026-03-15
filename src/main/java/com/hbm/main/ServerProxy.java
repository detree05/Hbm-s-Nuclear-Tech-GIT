package com.hbm.main;

import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.render.util.MissilePart;
import com.hbm.saveddata.TomSaveData;
import com.hbm.sound.AudioWrapper;
import com.hbm.util.i18n.I18nServer;
import com.hbm.util.i18n.ITranslate;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class ServerProxy {

	private static final I18nServer I18N = new I18nServer();

	//sort by estimated time of display. longer lasting ones should be sorted at the top.
	public static final int ID_DUCK = 0;
	public static final int ID_FILTER = 1;
	public static final int ID_COMPASS = 2;
	public static final int ID_CABLE = 3;
	public static final int ID_DRONE = 4;
	public static final int ID_JETPACK = 5;
	public static final int ID_MAGNET = 6;
	public static final int ID_HUD = 7;
	public static final int ID_DETONATOR = 8;
	public static final int ID_FLUID_ID = 9;
	public static final int ID_FAN_MODE = 10;
	public static final int ID_TOOLABILITY = 11;
	public static final int ID_GAS_HAZARD = 12;
	public static final int ID_WRENCH = 13;
	public static final int ID_VISUAL_CORTEX_MALFUNCTION = 14;
	public static final int ID_GRAVITY_MALFUNCTION = 15;
	public static final int ID_DIGAMMA_RADIATION_WARNING = 16;
	public static final int ID_BLACKHOLE_OBFUSCATED_WARNING_BASE = 17;
	public static final int ID_BLACKHOLE_ITS_HERE_WARNING = 18;

	public ITranslate getI18n() { return I18N; }

	public void registerPreRenderInfo() { }
	public void registerRenderInfo() { }
	public void registerTileEntitySpecialRenderer() { }
	public void registerItemRenderer() { }
	public void registerEntityRenderer() { }
	public void registerBlockRenderer() { }
	public void registerGunCfg() { }
	public void handleNHNEICompat() { }

	public void spawnParticle(double x, double y, double z, String type, float[] args) { }

	public void effectNT(NBTTagCompound data) { }

	public void registerMissileItems() {
		MissilePart.registerAllPartsServer();
	}

	/** Retired in favor of the version that uses keepAlive */
	@Deprecated public AudioWrapper getLoopedSound(String sound, float x, float y, float z, float volume, float range, float pitch) { return null; }
	public AudioWrapper getLoopedSound(String sound, float x, float y, float z, float volume, float range, float pitch, int keepAlive) { return null; }
	public AudioWrapper getLoopedSound(String sound, Entity entity, float volume, float range, float pitch, int keepAlive) { return null; }

	public void playSound(String sound, Object data) { }

	public void showDisconnectedScreen(String message) { }

	public void displayTooltip(String msg, int id) {
		displayTooltip(msg, 1000, id);
	}

	public void displayTooltip(String msg, int time, int id) { }

	public boolean getIsKeyPressed(EnumKeybind key) {
		return false;
	}

	public EntityPlayer me() {
		return null;
	}

	public boolean isVanished(Entity e) {
		return false;
	}

	public void openLink(String url) { }

	public List<ItemStack> getSubItems(ItemStack stack) {

		List<ItemStack> list = new ArrayList();
		list.add(stack);
		return list;
	}

	public float getImpactDust(World world) {
		return TomSaveData.forWorld(world).dust;
	}

	public float getImpactFire(World world) {
		return TomSaveData.forWorld(world).fire;
	}

	public boolean getImpact(World world) {
		return TomSaveData.forWorld(world).impact;
	}

	public void playSoundClient(double x, double y, double z, String sound, float volume, float pitch) { }

	public String getLanguageCode() { return "en_US"; }

	public int getStackColor(ItemStack stack, boolean amplify) { return 0x000000; }
}
