package com.hbm.main;

import java.util.HashMap;

import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockAshes;
import com.hbm.blocks.generic.BlockOre;
import com.hbm.blocks.generic.BlockRebar;
import com.hbm.config.ClientConfig;
import com.hbm.config.GeneralConfig;
import com.hbm.config.SpaceConfig;
import com.hbm.dim.CelestialBody;
import com.hbm.dim.CelestialCore;
import com.hbm.dim.SkyProviderCelestial;
import com.hbm.dim.SolarSystem;
import com.hbm.dim.SolarSystemWorldSavedData;
import com.hbm.dim.StarcoreSkyEffects;
import com.hbm.dim.WorldProviderCelestial;
import com.hbm.dim.dmitriy.WorldProviderDmitriy;
import com.hbm.dim.trait.CBT_SkyState;
import com.hbm.dim.trait.CBT_War;
import com.hbm.dim.trait.CelestialBodyTrait;
import com.hbm.dim.orbit.OrbitalStation;
import com.hbm.dim.orbit.OrbitalStation.StationState;
import com.hbm.dim.orbit.WorldProviderOrbit;
import com.hbm.entity.mob.EntityHunterChopper;
import com.hbm.entity.mob.EntityVoidFightsBack;
import com.hbm.entity.mob.EntityVoidStaresBack;
import com.hbm.entity.projectile.EntityChopperMine;
import com.hbm.entity.train.EntityRailCarRidable;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.extprop.HbmPlayerProps;
import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.HTTPHandler;
import com.hbm.handler.HazmatRegistry;
import com.hbm.handler.HbmKeybinds;
import com.hbm.handler.ImpactWorldHandler;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.hazard.type.HazardTypeNeutron;
import com.hbm.interfaces.IHoldableWeapon;
import com.hbm.interfaces.IItemHUD;
import com.hbm.interfaces.Spaghetti;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.gui.GUIArmorTable;
import com.hbm.inventory.gui.GUIScreenPreview;
import com.hbm.inventory.gui.GUIScreenWikiRender;
import com.hbm.inventory.gui.LoadingScreenRendererNT;
import com.hbm.items.ItemCustomLore;
import com.hbm.items.ModItems;
import com.hbm.items.armor.*;
import com.hbm.items.machine.ItemDepletedFuel;
import com.hbm.items.machine.ItemFluidDuct;
import com.hbm.items.machine.ItemRBMKPellet;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.lib.Library;
import com.hbm.lib.RefStrings;
import com.hbm.packet.PacketDispatcher;
import com.hbm.potion.HbmPotion;
import com.hbm.packet.toserver.AuxButtonPacket;
import com.hbm.qmaw.GuiQMAW;
import com.hbm.qmaw.QMAWLoader;
import com.hbm.qmaw.QuickManualAndWiki;
import com.hbm.render.anim.HbmAnimations;
import com.hbm.render.anim.HbmAnimations.Animation;
import com.hbm.render.block.ct.CTStitchReceiver;
import com.hbm.render.item.weapon.sedna.ItemRenderWeaponBase;
import com.hbm.render.util.RenderAccessoryUtility;
import com.hbm.render.util.RenderOverhead;
import com.hbm.render.util.RenderScreenOverlay;
import com.hbm.render.util.SoyuzPronter;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.MovingSoundChopper;
import com.hbm.sound.MovingSoundChopperMine;
import com.hbm.sound.MovingSoundCrashing;
import com.hbm.sound.MovingSoundPlayerLoop;
import com.hbm.sound.MovingSoundPlayerLoop.EnumHbmSound;
import com.hbm.tileentity.bomb.TileEntityNukeCustom;
import com.hbm.tileentity.bomb.TileEntityNukeCustom.CustomNukeEntry;
import com.hbm.tileentity.bomb.TileEntityNukeCustom.EnumEntryType;
import com.hbm.util.*;
import com.hbm.util.Tuple;
import com.hbm.util.ArmorRegistry.HazardClass;
import com.hbm.util.i18n.I18nUtil;
import com.hbm.wiaj.GuiWorldInAJar;
import com.hbm.wiaj.WorldInAJar;
import com.hbm.wiaj.cannery.CanneryBase;
import com.hbm.wiaj.cannery.Jars;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.WorldTickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
	import net.minecraft.block.Block;
	import net.minecraft.block.BlockRedstoneOre;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0CPacketInput;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.stats.Achievement;
import net.minecraft.event.HoverEvent;
	import net.minecraft.util.*;
	import net.minecraft.block.material.Material;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.sound.PlaySoundEvent17;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.oredict.OreDictionary;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class ModEventHandlerClient {

	public static long flashTimestamp;
	public static long starcoreFlashTimestamp;
	public static long shakeTimestamp;
	private static Float lastDmitriyGravity;
	private static float dmitriyStarStareIntensity = 0.0F;
	private static Integer lastClientDimensionId = null;
	private static final List<DmitriyStareTextEntry> dmitriyStarStareTextEntries = new ArrayList<DmitriyStareTextEntry>();
	private static int dmitriyStarStareTextSpawnTimer = 0;
	private static ResourceLocation voidStareBlurTexture;
	private static int dmitriyDialogueCooldownTicks = -1;
	private static Method setupCameraTransformMethod;
	private static Method dmitriyGhostAchievementToastMethod;
	private static DmitriyGhostAchievement dmitriyGhostAchievement;
	private static final Set<String> dmitriyGhostAchievementSeenCache = new HashSet<String>();
	private static boolean dmitriyGhostAchievementSeenLoadedFromConfig = false;
	private static int voidFightsBackShakeTicks = 0;
	private static int voidFightsBackLoopTicks = 0;
	private static AudioWrapper voidFightsBackLoopSound;
	private static final String[] BLACKHOLE_OBFUSCATED_WARNING_TEXTS = new String[] {
		"IT WATCHES",
		"DO NOT BLINK",
		"IT'S HUNGRY",
		"LOOK DEEPER",
		"I KNOW YOU",
		"I SEE YOU"
	};
	private static int blackholeItsHereFxDimension = Integer.MIN_VALUE;
	private static long blackholeItsHereFxCollapseEndTick = Long.MIN_VALUE;
	private static int blackholeGravityLiftFxDimension = Integer.MIN_VALUE;
	private static long blackholeGravityLiftFxCollapseEndTick = Long.MIN_VALUE;
	private static long blackholeGravityLiftFxNextSpawnTick = Long.MIN_VALUE;
	private static long blackholeGravityLiftFxNextChunkSpawnTick = Long.MIN_VALUE;
	private static final List<BlackholeGravityLiftBlock> blackholeGravityLiftBlocks = new ArrayList<BlackholeGravityLiftBlock>();
	private static final List<BlackholeGravityLiftChunk> blackholeGravityLiftChunks = new ArrayList<BlackholeGravityLiftChunk>();

	private static class BlackholeGravityLiftBlock {
		private final WorldInAJar jar;
		private final double x;
		private final double y;
		private final double z;
		private final double riseSpeed;
		private final double rotYaw;
		private final double rotPitch;
		private final double rotYawSpeed;
		private final double rotPitchSpeed;
		private final int maxAgeTicks;
		private int ageTicks;

		private BlackholeGravityLiftBlock(Block block, int meta, double x, double y, double z, double riseSpeed, int maxAgeTicks, double rotYaw, double rotPitch, double rotYawSpeed, double rotPitchSpeed) {
			this.jar = new WorldInAJar(1, 1, 1);
			this.jar.setBlock(0, 0, 0, block, meta);
			this.x = x;
			this.y = y;
			this.z = z;
			this.riseSpeed = riseSpeed;
			this.rotYaw = rotYaw;
			this.rotPitch = rotPitch;
			this.rotYawSpeed = rotYawSpeed;
			this.rotPitchSpeed = rotPitchSpeed;
			this.maxAgeTicks = maxAgeTicks;
			this.ageTicks = 0;
		}
	}

	private static class BlackholeGravityLiftChunk {
		private final WorldInAJar jar;
		private final double x;
		private final double y;
		private final double z;
		private final double riseSpeed;
		private final double rotYaw;
		private final double rotPitch;
		private final double rotYawSpeed;
		private final double rotPitchSpeed;
		private final int maxAgeTicks;
		private int ageTicks;

		private BlackholeGravityLiftChunk(WorldInAJar jar, double x, double y, double z, double riseSpeed, int maxAgeTicks, double rotYaw, double rotPitch, double rotYawSpeed, double rotPitchSpeed) {
			this.jar = jar;
			this.x = x;
			this.y = y;
			this.z = z;
			this.riseSpeed = riseSpeed;
			this.rotYaw = rotYaw;
			this.rotPitch = rotPitch;
			this.rotYawSpeed = rotYawSpeed;
			this.rotPitchSpeed = rotPitchSpeed;
			this.maxAgeTicks = maxAgeTicks;
			this.ageTicks = 0;
		}
	}

	private static String resolveHostnameSafe() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch(UnknownHostException ignored) {
			return "P L A Y E R";
		}
	}

	private static String getDmitriyStarStareHostName() {
		String envHost = System.getenv("COMPUTERNAME");
		if(envHost != null && !envHost.isEmpty()) {
			return envHost;
		}
		return resolveHostnameSafe();
	}

	public static void triggerVoidFightsBackClient(boolean playIntro, int shakeTicks, int loopTicks, boolean stopLoop) {
		Minecraft mc = Minecraft.getMinecraft();
		if(mc == null || mc.thePlayer == null || mc.theWorld == null) {
			return;
		}

		if(stopLoop) {
			voidFightsBackLoopTicks = 0;
			if(voidFightsBackLoopSound != null) {
				voidFightsBackLoopSound.stopSound();
				voidFightsBackLoopSound = null;
			}
		}

		if(playIntro) {
			mc.getSoundHandler().playSound(new PositionedSoundRecord(
				new ResourceLocation("hbm:misc.itlives_itfightsback"),
				1.0F,
				1.0F,
				(float) mc.thePlayer.posX,
				(float) mc.thePlayer.posY,
				(float) mc.thePlayer.posZ
			));
		}

		voidFightsBackShakeTicks = Math.max(voidFightsBackShakeTicks, shakeTicks);
		if(loopTicks < 0) {
			voidFightsBackLoopTicks = -1;
		} else if(voidFightsBackLoopTicks >= 0) {
			voidFightsBackLoopTicks = Math.max(voidFightsBackLoopTicks, loopTicks);
		}
	}

	private static Random getDmitriyStarStareTextRandom() {
		Minecraft mc = Minecraft.getMinecraft();
		if(mc != null && mc.theWorld != null && mc.theWorld.rand != null) {
			return mc.theWorld.rand;
		}
		return new Random();
	}

	private static String getDmitriyStarStareText(Random random) {
		switch(random.nextInt(8)) {
		case 0:
			return "I T   W A T C H E S";
		case 1:
			return "D O   N O T   B L I N K";
		case 2:
			return "I T ' S   H U N G R Y";
		case 3:
			return "L O O K   D E E P E R";
		case 4:
			return "I   K N O W   Y O U,   " + getDmitriyStarStareHostName();
		case 5:
			return "I   S E E   Y O U";
		case 6:
			return "S T A Y   I N   T H E   L I G H T";
		default:
			return "H A V E   Y O U   S E E N   T H E   " + EnumChatFormatting.DARK_RED + EnumChatFormatting.OBFUSCATED + "VOIDSTARESBACK" + EnumChatFormatting.RESET + "   ?";
		}
	}

	private static boolean setupCameraTransform(Minecraft mc, float partialTicks) {
		if(mc == null || mc.entityRenderer == null) {
			return false;
		}
		try {
			if(setupCameraTransformMethod == null) {
				setupCameraTransformMethod = ReflectionHelper.findMethod(
					EntityRenderer.class,
					mc.entityRenderer,
					new String[] { "setupCameraTransform", "func_78479_a" },
					float.class, int.class
				);
				setupCameraTransformMethod.setAccessible(true);
			}
			setupCameraTransformMethod.invoke(mc.entityRenderer, partialTicks, 0);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private static class DmitriyStareTextEntry {
		private final String text;
		private final float anchorX;
		private final float anchorY;
		private int ageTicks;

		private DmitriyStareTextEntry(String text, float anchorX, float anchorY) {
			this.text = text;
			this.anchorX = anchorX;
			this.anchorY = anchorY;
			this.ageTicks = 0;
		}
	}



	@SubscribeEvent
	public void onOverlayRender(RenderGameOverlayEvent.Pre event) {

		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		final int flashDuration = 5_000;
		final int starcoreFlashDuration = 9_000;

		// nuke/starcore flash

		/// NUKE FLASH ///
		long now = Clock.get_ms();
		long remainingBase = flashTimestamp + flashDuration - now;
		long remainingStarcore = starcoreFlashTimestamp + starcoreFlashDuration - now;
		if(event.type == ElementType.CROSSHAIRS && (remainingBase > 0 || remainingStarcore > 0) && ClientConfig.NUKE_HUD_FLASH.get()) {
			int width = event.resolution.getScaledWidth();
			int height = event.resolution.getScaledHeight();
			Tessellator tess = Tessellator.instance;
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
			GL11.glAlphaFunc(GL11.GL_GEQUAL, 0.0F);
			GL11.glDepthMask(false);
			tess.startDrawingQuads();
			float baseBrightness = remainingBase > 0 ? (remainingBase / (float) flashDuration) : 0.0F;
			float starcoreBrightness = remainingStarcore > 0 ? (remainingStarcore / (float) starcoreFlashDuration) : 0.0F;
			float brightness = Math.max(baseBrightness, starcoreBrightness);
			tess.setColorRGBA_F(1F, 1F, 1F, brightness * 1F);
			tess.addVertex(width, 0, 0);
			tess.addVertex(0, 0, 0);
			tess.addVertex(0, height, 0);
			tess.addVertex(width, height, 0);
			tess.draw();
			OpenGlHelper.glBlendFunc(770, 771, 1, 0);
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
			GL11.glDepthMask(true);
			return;
		}

		if(event.type == ElementType.CROSSHAIRS) {
			float intensity = getVoidStareIntensity(player);
			float overlayIntensity = Math.max(intensity, dmitriyStarStareIntensity);
			if(overlayIntensity > 0.001F) {
				renderVoidStareOverlay(event.resolution, overlayIntensity);
			}
			if(dmitriyStarStareIntensity > 0.001F) {
				renderDmitriyStarStareText(event.resolution, dmitriyStarStareIntensity);
			}
		}

		/*if(event.type == ElementType.CROSSHAIRS && player.getHeldItem() != null && player.getHeldItem().getItem() == ModItems.gun_aberrator) {
			int width = event.resolution.getScaledWidth();
			int height = event.resolution.getScaledHeight();
			Tessellator tess = Tessellator.instance;
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glEnable(GL11.GL_BLEND);
			OpenGlHelper.glBlendFunc(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ONE_MINUS_SRC_COLOR, 1, 0);
			GL11.glAlphaFunc(GL11.GL_GEQUAL, 0.0F);
			GL11.glDepthMask(false);
			tess.startDrawingQuads();
			float intensity = 0.2F;
			tess.setColorRGBA_F(intensity, intensity, intensity, 1F);
			tess.addVertex(width, 0, 0);
			tess.addVertex(0, 0, 0);
			tess.addVertex(0, height, 0);
			tess.addVertex(width, height, 0);
			tess.draw();
			OpenGlHelper.glBlendFunc(770, 771, 1, 0);
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
			GL11.glDepthMask(true);
		}*/

		/// HANDLE GUN OVERLAYS ///
		if(player.getHeldItem() != null && player.getHeldItem().getItem() instanceof IItemHUD) {
			((IItemHUD)player.getHeldItem().getItem()).renderHUD(event, event.type, player, player.getHeldItem());
		}

		/// HANDLE GEIGER COUNTER HUD ///
		if(event.type == ElementType.HOTBAR) {

			if(!(ArmorFSB.hasFSBArmor(player) && ((ArmorFSB)player.inventory.armorInventory[2].getItem()).customGeiger)) {

				if(player.inventory.hasItem(ModItems.geiger_counter)) {

					float rads = HbmLivingProps.getRadiation(player);

					RenderScreenOverlay.renderRadCounter(event.resolution, rads, Minecraft.getMinecraft().ingameGUI);
				}
			}
		}


		/// DODD DIAG HOOK FOR RBMK
		if(event.type == ElementType.CROSSHAIRS && ClientConfig.DODD_RBMK_DIAGNOSTIC.get()) {
			Minecraft mc = Minecraft.getMinecraft();
			World world = mc.theWorld;
			MovingObjectPosition mop = mc.objectMouseOver;

			if(mop != null) {

				if(mop.typeOfHit == MovingObjectType.BLOCK) {

					if(player.getHeldItem() != null && player.getHeldItem().getItem() instanceof ILookOverlay) {
						((ILookOverlay) player.getHeldItem().getItem()).printHook(event, world, mop.blockX, mop.blockY, mop.blockZ);

					} else if(world.getBlock(mop.blockX, mop.blockY, mop.blockZ) instanceof ILookOverlay) {
						((ILookOverlay) world.getBlock(mop.blockX, mop.blockY, mop.blockZ)).printHook(event, world, mop.blockX, mop.blockY, mop.blockZ);
					}

					/*List<String> text = new ArrayList();
					text.add("Meta: " + world.getBlockMetadata(mop.blockX, mop.blockY, mop.blockZ));
					ILookOverlay.printGeneric(event, "DEBUG", 0xffff00, 0x4040000, text);*/
					
					if(ClientConfig.SHOW_BLOCK_META_OVERLAY.get()) {
						Block b = world.getBlock(mop.blockX, mop.blockY, mop.blockZ);
						int i = world.getBlockMetadata(mop.blockX, mop.blockY, mop.blockZ);
						List<String> text = new ArrayList();
						text.add(b.getUnlocalizedName());
						text.add("Meta: " + i);
						ILookOverlay.printGeneric(event, "DEBUG", 0xffff00, 0x4040000, text);
					}

				} else if(mop.typeOfHit == MovingObjectType.ENTITY) {
					Entity entity = mop.entityHit;

					if(entity instanceof ILookOverlay) {
						((ILookOverlay) entity).printHook(event, world, 0, 0, 0);
					}
				}

				GL11.glColor4f(1F, 1F, 1F, 1F);
			}

			/*List<String> text = new ArrayList();
			text.add("IMPACT: " + ImpactWorldHandler.getImpactForClient(world));
			text.add("DUST: " + ImpactWorldHandler.getDustForClient(world));
			text.add("FIRE: " + ImpactWorldHandler.getFireForClient(world));
			ILookOverlay.printGeneric(event, "DEBUG", 0xffff00, 0x4040000, text);*/

			/*if(mop != null && mop.typeOfHit == mop.typeOfHit.BLOCK) {
				ScaledResolution resolution = event.resolution;
				GL11.glPushMatrix();
				int pX = resolution.getScaledWidth() / 2 + 8;
				int pZ = resolution.getScaledHeight() / 2;
				mc.fontRenderer.drawString("META: " + world.getBlockMetadata(mop.blockX, mop.blockY, mop.blockZ), pX, pZ - 3, 0xffff00);
				GL11.glDisable(GL11.GL_BLEND);
				GL11.glColor3f(1F, 1F, 1F);
				GL11.glPopMatrix();
				Minecraft.getMinecraft().renderEngine.bindTexture(Gui.icons);
			}*/

			/*List<String> text = new ArrayList();
			MovingObjectPosition pos = Library.rayTrace(player, 500, 1, false, true, false);

			for(int i = 0; i < 2; i++) if(pos != null && pos.typeOfHit == pos.typeOfHit.BLOCK) {

				float yaw = player.rotationYaw;

				Vec3 next = Vec3.createVectorHelper(pos.hitVec.xCoord, pos.hitVec.yCoord, pos.hitVec.zCoord);
				int it = 0;

				BlockPos anchor = new BlockPos(pos.blockX, pos.blockY, pos.blockZ);

				double distanceToCover = 4D * (i == 0 ? 1 : -1);

				if(distanceToCover < 0) {
					distanceToCover *= -1;
					yaw += 180;
				}

				do {

					it++;

					if(it > 30) {
						world.createExplosion(player, pos.hitVec.xCoord, pos.hitVec.yCoord, pos.hitVec.zCoord, 5F, false);
						break;
					}

					int x = anchor.getX();
					int y = anchor.getY();
					int z = anchor.getZ();
					Block block = world.getBlock(x, y, z);

					Vec3 rot = Vec3.createVectorHelper(0, 0, 1);
					rot.rotateAroundY((float) (-yaw * Math.PI / 180D));

					if(block instanceof IRailNTM) {
						IRailNTM rail = (IRailNTM) block;
						RailContext info = new RailContext();

						boolean flip = distanceToCover < 0;

						if(it == 1) {
							Vec3 snap = next = rail.getTravelLocation(world, x, y, z, next.xCoord, next.yCoord, next.zCoord, rot.xCoord, rot.yCoord, rot.zCoord, 0, info, new MoveContext(RailCheckType.CORE, 0));
							if(i == 0) world.spawnParticle("reddust", snap.xCoord, snap.yCoord + 0.25, snap.zCoord, 0.1, 1, 0.1);
						}

						Vec3 prev = next;
						next = rail.getTravelLocation(world, x, y, z, prev.xCoord, prev.yCoord, prev.zCoord, rot.xCoord, rot.yCoord, rot.zCoord, distanceToCover, info, new MoveContext(i == 0 ? RailCheckType.FRONT : RailCheckType.BACK, 0));
						distanceToCover = info.overshoot;
						anchor = info.pos;
						if(i == 0) world.spawnParticle("reddust", next.xCoord, next.yCoord + 0.25, next.zCoord, 0, distanceToCover != 0 ? 0.5 : 0, 0);
						else world.spawnParticle("reddust", next.xCoord, next.yCoord + 0.25, next.zCoord, 0, distanceToCover != 0 ? 0.5 : 0, 1);

						double deltaX = next.xCoord - prev.xCoord;
						double deltaZ = next.zCoord - prev.zCoord;
						double radians = -Math.atan2(deltaX, deltaZ);
						yaw = (float) MathHelper.wrapAngleTo180_double(radians * 180D / Math.PI + (flip ? 180 : 0));

						text.add(it + ": " + yaw);

					} else {
						break;
					}

				} while(distanceToCover != 0);

				ILookOverlay.printGeneric(event, "DEBUG", 0xffff00, 0x4040000, text);
			}*/
		}

		/// HANLDE ANIMATION BUSES ///

		for(int i = 0; i < HbmAnimations.hotbar.length; i++) {
			for(int j = 0; j < HbmAnimations.hotbar[i].length; j++) {

				Animation animation = HbmAnimations.hotbar[i][j];

				if(animation == null)
					continue;

				if(animation.holdLastFrame)
					continue;

				long time = Clock.get_ms() - animation.startMillis;

				if(time > animation.animation.getDuration())
					HbmAnimations.hotbar[i][j] = null;
			}
		}

		if(!ducked && Keyboard.isKeyDown(Keyboard.KEY_O) && Minecraft.getMinecraft().currentScreen == null) {
			ducked = true;
			PacketDispatcher.wrapper.sendToServer(new AuxButtonPacket(0, 0, 0, 999, 0));
		}

		/// HANDLE SCOPE OVERLAY ///
		ItemStack held = player.getHeldItem();

		if(held != null && held.getItem() instanceof ItemGunBaseNT && ItemGunBaseNT.aimingProgress == ItemGunBaseNT.prevAimingProgress && ItemGunBaseNT.aimingProgress == 1F && event.type == ElementType.HOTBAR)  {
			ItemGunBaseNT gun = (ItemGunBaseNT) held.getItem();
			GunConfig cfg = gun.getConfig(held, 0);
			if(cfg.getScopeTexture(held) != null) {
				ScaledResolution resolution = event.resolution;
				RenderScreenOverlay.renderScope(resolution, cfg.getScopeTexture(held));
			}
		}

		//prevents NBT changes (read: every fucking tick) on guns from bringing up the item's name over the hotbar
		if(held != null && held.getItem() instanceof ItemGunBaseNT && Minecraft.getMinecraft().ingameGUI.highlightingItemStack != null && Minecraft.getMinecraft().ingameGUI.highlightingItemStack.getItem() == held.getItem()) {
			Minecraft.getMinecraft().ingameGUI.highlightingItemStack = held;
		}

		/// HANDLE FLASHBANG OVERLAY///
		if(player.isPotionActive(HbmPotion.flashbang)) {
			RenderScreenOverlay.renderFlashbangOverlay(event.resolution);
		}
		/// HANDLE FSB HUD ///
		ItemStack helmet = player.inventory.armorInventory[3];

		if(helmet != null && helmet.getItem() instanceof ArmorFSB) {
			((ArmorFSB)helmet.getItem()).handleOverlay(event, player);
		}
		if(!event.isCanceled() && event.type == ElementType.HOTBAR) {

			HbmPlayerProps props = HbmPlayerProps.getData(player);
			if(props.getDashCount() > 0) {
				RenderScreenOverlay.renderDashBar(event.resolution, Minecraft.getMinecraft().ingameGUI, props);

			}
		}
	}

	@SubscribeEvent(receiveCanceled = true)
	public void onHUDRenderShield(RenderGameOverlayEvent.Pre event) {

		EntityPlayer player = Minecraft.getMinecraft().thePlayer;

		if(event.type == ElementType.ARMOR) {

			HbmPlayerProps props = HbmPlayerProps.getData(player);
			if(props.getEffectiveMaxShield() > 0) {
				RenderScreenOverlay.renderShieldBar(event.resolution, Minecraft.getMinecraft().ingameGUI);
			}
		}
	}

	private List<Tuple.Pair<Float, Integer>> getBars(ItemStack stack, EntityPlayer player) {

		List<Tuple.Pair<Float, Integer>> bars = new ArrayList<>();

		if(stack.getItem() instanceof ArmorFSBPowered && ArmorFSBPowered.hasFSBArmorIgnoreCharge(player)) {
			float charge = 1F - (float) ((ArmorFSBPowered) stack.getItem()).getDurabilityForDisplay(stack);

			bars.add(new Tuple.Pair<Float, Integer>(charge, 0x00FF00));
		}

		if(stack.getItem() instanceof JetpackFueledBase) {
			JetpackFueledBase jetpack = (JetpackFueledBase) stack.getItem();
			float fuel = (float) JetpackFueledBase.getFuel(stack) / jetpack.maxFuel;

			bars.add(new Tuple.Pair<Float, Integer>(fuel, jetpack.fuel.getColor()));
		}

		return bars;
	}

	@SubscribeEvent(receiveCanceled = true, priority = EventPriority.LOW)
	public void onHUDRenderBar(RenderGameOverlayEvent.Post event) {

		/// HANDLE ELECTRIC FSB HUD ///

		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		Tessellator tess = Tessellator.instance;

		if(!event.isCanceled() && event.type == ElementType.HEALTH) {
			if(player.isPotionActive(HbmPotion.nitan)) {
				RenderScreenOverlay.renderTaintBar(event.resolution, Minecraft.getMinecraft().ingameGUI);
			}
		}

		if (!event.isCanceled() && event.type == ElementType.ALL) {
			long time = ImpactWorldHandler.getTimeForClient(player.worldObj);
			if(time > 0) {
				RenderScreenOverlay.renderCountdown(event.resolution, Minecraft.getMinecraft().ingameGUI, Minecraft.getMinecraft().theWorld);
			}
		}

		if(event.type == ElementType.ARMOR) {

			List<List<Tuple.Pair<Float, Integer>>> barsList = new ArrayList<>();

			for (int i = 0; i < 4; i++) {

				barsList.add(new ArrayList<>());

				ItemStack stack = player.inventory.armorInventory[i];

				if(stack == null)
					continue;

				barsList.get(i).addAll(getBars(stack, player));

				if (!(ArmorModHandler.hasMods(stack)))
					continue;

				for (ItemStack mod : ArmorModHandler.pryMods(stack)) {
					if (mod == null) continue;

					barsList.get(i).addAll(getBars(mod, player));
				}
			}

			GL11.glDisable(GL11.GL_TEXTURE_2D);
			tess.startDrawingQuads();

			if(ForgeHooks.getTotalArmorValue(player) == 0) {
				GuiIngameForge.left_height -= 10;
			}

			int width = event.resolution.getScaledWidth();
			int height = event.resolution.getScaledHeight();
			int left = width / 2 - 91;

			for (List<Tuple.Pair<Float, Integer>> bars : barsList) {

				if (bars.isEmpty())
					continue;

				int top = height - GuiIngameForge.left_height + 7;

				for (int i = 0; i < bars.size(); i++) {

					float val = bars.get(i).key;
					int hstart, hend;

					if (i == 0) {
						hstart = left;
						hend = hstart + (bars.size() == 1 ? 81 : 40);
					} else {
						int bl = (int) Math.ceil(40F / (bars.size() - 1));
						// :(
						hstart = left + 41 + bl * (i - 1);
						hend = i == bars.size() - 1 ? left + 81 : hstart + bl;

						if (i != 1) hstart += 1;
					}

					tess.setColorOpaque_F(0.25F, 0.25F, 0.25F);
					tess.addVertex(hstart, top - 1, 0);
					tess.addVertex(hstart, top + 2, 0);
					tess.addVertex(hend, top + 2, 0);
					tess.addVertex(hend, top - 1, 0);

					float valx = hstart + (hend - hstart - 1) * val;

					int color = bars.get(i).value;
					float r = ((color >> 16) & 0xFF) / 255F;
					float g = ((color >> 8) & 0xFF) / 255F;
					float b = (color & 0xFF) / 255F;

					tess.setColorOpaque_F(r, g, b);
					tess.addVertex(hstart+1, top, 0);
					tess.addVertex(hstart+1, top + 1, 0);
					tess.addVertex(valx, top + 1, 0);
					tess.addVertex(valx, top, 0);
				}

				GuiIngameForge.left_height += 4;
			}

			tess.draw();
			GL11.glEnable(GL11.GL_TEXTURE_2D);
		}
	}

	@SubscribeEvent
	public void setupFOV(FOVUpdateEvent event) {

		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		ItemStack held = player.getHeldItem();
		float fov = event.fov;

		if(held != null) {
			IItemRenderer customRenderer = MinecraftForgeClient.getItemRenderer(held, IItemRenderer.ItemRenderType.EQUIPPED);
			if(customRenderer instanceof ItemRenderWeaponBase) {
				ItemRenderWeaponBase renderGun = (ItemRenderWeaponBase) customRenderer;
				fov = renderGun.getViewFOV(held, fov);
			}
		}

		// Simple "pull" effect when transfer animation approaches Dmitriy.
		if(player.worldObj.provider instanceof WorldProviderOrbit) {
			OrbitalStation station = OrbitalStation.clientStation;
			if(station != null && station.state != StationState.ORBIT && station.getAnimationTarget() == SolarSystem.dmitriy) {
				float progress = MathHelper.clamp_float((float)station.getTransferProgress(0), 0F, 1F);
				fov *= (1.0F - 0.15F * progress);
			}
		}

		float voidStareIntensity = getVoidStareIntensity(player);
		if(voidStareIntensity > 0.001F) {
			final float voidStareMaxFovShift = 0.25F;
			float curve = voidStareIntensity * voidStareIntensity;
			fov *= (1.0F + voidStareMaxFovShift * curve);
		}

		if(dmitriyStarStareIntensity > 0.001F) {
			final float dmitriyStarStareMaxFovZoom = 0.6F;
			float zoomScale = 1.0F - dmitriyStarStareMaxFovZoom * dmitriyStarStareIntensity;
			fov *= MathHelper.clamp_float(zoomScale, 0.1F, 1.0F);
		}

		event.newfov = fov;
	}

	private static float getVoidStareIntensity(EntityPlayer player) {
		final float voidStareEffectRange = 64.0F;
		if(player == null || player.worldObj == null) {
			return 0.0F;
		}
		AxisAlignedBB bounds = player.boundingBox.expand(voidStareEffectRange, voidStareEffectRange, voidStareEffectRange);
		List<EntityVoidStaresBack> nearby = player.worldObj.getEntitiesWithinAABB(EntityVoidStaresBack.class, bounds);
		if(nearby == null || nearby.isEmpty()) {
			return 0.0F;
		}

		float max = 0.0F;
		for(EntityVoidStaresBack entity : nearby) {
			if(entity == null || entity.isDead || !entity.isChasing()) {
				continue;
			}
			float dist = player.getDistanceToEntity(entity);
			float intensity = 1.0F - (dist / voidStareEffectRange);
			if(intensity > max) {
				max = intensity;
			}
		}
		return MathHelper.clamp_float(max, 0.0F, 1.0F);
	}

	private static void updateDmitriyStarStare(Minecraft mc, float partialTicks) {
		final float dmitriyStarStareAngleDeg = 35.0F;
		final float dmitriyStarStareRaytraceDistance = 512.0F;
		final float dmitriyStarStareMaxTurnDeg = 2.5F;
		dmitriyStarStareIntensity = 0.0F;
		if(mc == null || mc.theWorld == null || mc.thePlayer == null) {
			updateDmitriyStarStareTextState(false);
			return;
		}
		if(!(mc.theWorld.provider instanceof WorldProviderDmitriy)) {
			updateDmitriyStarStareTextState(false);
			return;
		}

		Vec3 starDir = getDmitriyStarADirection(mc.theWorld, partialTicks);
		if(starDir == null) {
			updateDmitriyStarStareTextState(false);
			return;
		}

		EntityPlayer player = mc.thePlayer;
		Vec3 look = player.getLookVec();
		if(look == null) {
			updateDmitriyStarStareTextState(false);
			return;
		}

		double dot = look.xCoord * starDir.xCoord + look.yCoord * starDir.yCoord + look.zCoord * starDir.zCoord;
		dot = MathHelper.clamp_double(dot, -1.0D, 1.0D);
		float angleDeg = (float) (Math.acos(dot) * 180.0D / Math.PI);
		if(angleDeg > dmitriyStarStareAngleDeg) {
			updateDmitriyStarStareTextState(false);
			return;
		}

		if(!isStarVisible(mc.theWorld, player, starDir, dmitriyStarStareRaytraceDistance)) {
			updateDmitriyStarStareTextState(false);
			return;
		}

		float intensity = 1.0F - (angleDeg / dmitriyStarStareAngleDeg);
		intensity = intensity * intensity;
		dmitriyStarStareIntensity = intensity;

		float targetYaw = (float) (Math.atan2(starDir.zCoord, starDir.xCoord) * 180.0D / Math.PI) - 90.0F;
		float targetPitch = (float) (-Math.asin(starDir.yCoord) * 180.0D / Math.PI);

		float maxStep = dmitriyStarStareMaxTurnDeg * intensity;
		float yaw = player.rotationYaw;
		float pitch = player.rotationPitch;

		float yawDiff = MathHelper.wrapAngleTo180_float(targetYaw - yaw);
		float pitchDiff = MathHelper.wrapAngleTo180_float(targetPitch - pitch);

		yaw += MathHelper.clamp_float(yawDiff, -maxStep, maxStep);
		pitch += MathHelper.clamp_float(pitchDiff, -maxStep, maxStep);

		player.rotationYaw = yaw;
		player.rotationPitch = MathHelper.clamp_float(pitch, -90.0F, 90.0F);
		player.rotationYawHead = yaw;
		updateDmitriyStarStareTextState(true);
	}

	private static void updateDmitriyStarStareTextState(boolean stareActive) {
		final int dmitriyStarStareTextSpawnIntervalTicks = 20;
		final int dmitriyStarStareTextMaxActive = 4;
		if(!stareActive) {
			dmitriyStarStareTextEntries.clear();
			dmitriyStarStareTextSpawnTimer = 0;
			return;
		}

		for(Iterator<DmitriyStareTextEntry> iterator = dmitriyStarStareTextEntries.iterator(); iterator.hasNext();) {
			DmitriyStareTextEntry entry = iterator.next();
			entry.ageTicks++;
			if(entry.ageTicks >= getDmitriyStarStareTextTotalTicks()) {
				iterator.remove();
			}
		}

		if(dmitriyStarStareTextSpawnTimer > 0) {
			dmitriyStarStareTextSpawnTimer--;
		}

		if(dmitriyStarStareTextSpawnTimer <= 0 && dmitriyStarStareTextEntries.size() < dmitriyStarStareTextMaxActive) {
			dmitriyStarStareTextEntries.add(createDmitriyStarStareTextEntry());
			dmitriyStarStareTextSpawnTimer = dmitriyStarStareTextSpawnIntervalTicks;
		}
	}

	private static DmitriyStareTextEntry createDmitriyStarStareTextEntry() {
		Random random = getDmitriyStarStareTextRandom();
		String text = getDmitriyStarStareText(random);
		float anchorX = 0.15F + random.nextFloat() * 0.70F;
		float anchorY = 0.25F + random.nextFloat() * 0.55F;
		return new DmitriyStareTextEntry(text, anchorX, anchorY);
	}

	private static int getDmitriyStarStareTextTotalTicks() {
		return 15;
	}

	private static Vec3 getDmitriyStarADirection(World world, float partialTicks) {
		if(world == null) {
			return null;
		}
		float solarAngle = (world.getCelestialAngle(partialTicks) * 128.0F) % 1.0F;
		if(solarAngle < 0.0F) {
			solarAngle += 1.0F;
		}

		Vec3 dir = Vec3.createVectorHelper(0.0D, 1.0D, 0.0D);
		dir = rotateZ(dir, 45.0F);
		dir = rotateY(dir, 25.0F);
		dir = rotateX(dir, solarAngle * 360.0F);
		dir = rotateY(dir, -90.0F);

		if(dir.lengthVector() <= 0.0D) {
			return null;
		}
		return dir.normalize();
	}

	private static Vec3 rotateX(Vec3 v, float degrees) {
		double rad = Math.toRadians(degrees);
		double cos = Math.cos(rad);
		double sin = Math.sin(rad);
		double y = v.yCoord * cos - v.zCoord * sin;
		double z = v.yCoord * sin + v.zCoord * cos;
		return Vec3.createVectorHelper(v.xCoord, y, z);
	}

	private static Vec3 rotateY(Vec3 v, float degrees) {
		double rad = Math.toRadians(degrees);
		double cos = Math.cos(rad);
		double sin = Math.sin(rad);
		double x = v.xCoord * cos + v.zCoord * sin;
		double z = -v.xCoord * sin + v.zCoord * cos;
		return Vec3.createVectorHelper(x, v.yCoord, z);
	}

	private static Vec3 rotateZ(Vec3 v, float degrees) {
		double rad = Math.toRadians(degrees);
		double cos = Math.cos(rad);
		double sin = Math.sin(rad);
		double x = v.xCoord * cos - v.yCoord * sin;
		double y = v.xCoord * sin + v.yCoord * cos;
		return Vec3.createVectorHelper(x, y, v.zCoord);
	}

	private static boolean isStarVisible(World world, EntityPlayer player, Vec3 dir, double maxDistance) {
		Vec3 start = Vec3.createVectorHelper(
			player.posX,
			player.posY + player.getEyeHeight(),
			player.posZ
		);
		Vec3 end = start.addVector(dir.xCoord * maxDistance, dir.yCoord * maxDistance, dir.zCoord * maxDistance);
		Vec3 cursor = start;

		for(int i = 0; i < 16; i++) {
			MovingObjectPosition hit = world.rayTraceBlocks(cursor, end, false);
			if(hit == null || hit.typeOfHit != MovingObjectType.BLOCK) {
				return true;
			}

			Block block = world.getBlock(hit.blockX, hit.blockY, hit.blockZ);
			if(block != null && block.isOpaqueCube()) {
				return false;
			}

			if(hit.hitVec == null) {
				return true;
			}

			cursor = Vec3.createVectorHelper(
				hit.hitVec.xCoord + dir.xCoord * 0.01D,
				hit.hitVec.yCoord + dir.yCoord * 0.01D,
				hit.hitVec.zCoord + dir.zCoord * 0.01D
			);

			if(cursor.distanceTo(start) >= maxDistance) {
				return true;
			}
		}

		return true;
	}

	private static void renderVoidStareOverlay(ScaledResolution resolution, float intensity) {
		final float voidStareMaxBlurAlpha = 1.0F;
		if(voidStareBlurTexture == null) {
			voidStareBlurTexture = new ResourceLocation(RefStrings.MODID + ":textures/misc/overlay_goggles.png");
		}

		float curve = MathHelper.clamp_float(intensity * intensity, 0.0F, 1.0F);
		float alpha = voidStareMaxBlurAlpha * curve;

		Minecraft.getMinecraft().getTextureManager().bindTexture(voidStareBlurTexture);

		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDepthMask(false);
		OpenGlHelper.glBlendFunc(770, 771, 1, 0);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, alpha);
		GL11.glDisable(GL11.GL_ALPHA_TEST);

		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		tessellator.addVertexWithUV(0.0D, (double) resolution.getScaledHeight(), -90.0D, 0.0D, 1.0D);
		tessellator.addVertexWithUV((double) resolution.getScaledWidth(), (double) resolution.getScaledHeight(), -90.0D, 1.0D, 1.0D);
		tessellator.addVertexWithUV((double) resolution.getScaledWidth(), 0.0D, -90.0D, 1.0D, 0.0D);
		tessellator.addVertexWithUV(0.0D, 0.0D, -90.0D, 0.0D, 0.0D);
		tessellator.draw();

		GL11.glDepthMask(true);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private static void renderDmitriyStarStareText(ScaledResolution resolution, float intensity) {
		final float dmitriyStarStareTextMaxAlpha = 1.0F;
		final float dmitriyStarStareTextShakePx = 3.0F;
		Random random = getDmitriyStarStareTextRandom();
		Minecraft mc = Minecraft.getMinecraft();
		if(mc == null || mc.fontRenderer == null || resolution == null) {
			return;
		}
		if(dmitriyStarStareTextEntries.isEmpty()) {
			return;
		}

		GL11.glPushMatrix();
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

		for(DmitriyStareTextEntry entry : dmitriyStarStareTextEntries) {
			float alphaFactor = 1.0F;
			int alpha = MathHelper.clamp_int((int)(alphaFactor * 255.0F * dmitriyStarStareTextMaxAlpha), 0, 127);
			if(alpha <= 0) {
				continue;
			}

			String display = EnumChatFormatting.ITALIC + entry.text;
			float shake = dmitriyStarStareTextShakePx * alphaFactor;
			int jitterRange = MathHelper.ceiling_float_int(shake);
			int jitterX = jitterRange > 0 ? random.nextInt(jitterRange * 2 + 1) - jitterRange : 0;
			int jitterY = jitterRange > 0 ? random.nextInt(jitterRange * 2 + 1) - jitterRange : 0;
			int textWidth = mc.fontRenderer.getStringWidth(display);
			int x = (int)(resolution.getScaledWidth() * entry.anchorX) - textWidth / 2 + jitterX;
			int y = (int)(resolution.getScaledHeight() * entry.anchorY) + jitterY;
			x = MathHelper.clamp_int(x, 4, Math.max(4, resolution.getScaledWidth() - textWidth - 4));
			y = MathHelper.clamp_int(y, 4, Math.max(4, resolution.getScaledHeight() - 12));
			int color = (alpha << 24) | 0xFFFFFF;

			mc.fontRenderer.drawStringWithShadow(display, x, y, color);
		}

		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glPopMatrix();
	}

	private static class DmitriyGhostAchievement extends Achievement {
		private IChatComponent ghostTitle = new ChatComponentText("");
		private String ghostDescription = ": )";

		private DmitriyGhostAchievement() {
			super("achievement.dmitriyGhostFake", "dmitriyGhostFake", -128, -128, getDmitriyGhostAchievementIconItem(), null);
			this.initIndependentStat();
		}

		private void setGhostText(String title, String description) {
			this.ghostTitle = new ChatComponentText(title);
			this.ghostDescription = description;
			try {
				ReflectionHelper.setPrivateValue(net.minecraft.stats.StatBase.class, this, this.ghostTitle, new String[] { "statName", "field_75975_e" });
			} catch(Exception ignored) { }
			try {
				ReflectionHelper.setPrivateValue(Achievement.class, this, this.ghostDescription, new String[] { "achievementDescription", "field_75992_c" });
			} catch(Exception ignored) { }
		}

		public IChatComponent func_150951_e() {
			return this.ghostTitle;
		}

		public String getDescription() {
			return this.ghostDescription;
		}
	}

	private static Item getDmitriyGhostAchievementIconItem() {
		Item icon = Item.getItemById(5270);
		return icon != null ? icon : Items.nether_star;
	}

	private static void resetDmitriyCameraEffects(Minecraft mc) {
		dmitriyStarStareIntensity = 0.0F;
		updateDmitriyStarStareTextState(false);
		stopDmitriyStarStareSound();
		if(mc != null && mc.entityRenderer != null) {
			String[] camRollFields = new String[] { "camRoll", "field_78495_O", "O" };
			ReflectionHelper.setPrivateValue(net.minecraft.client.renderer.EntityRenderer.class, mc.entityRenderer, 0.0F, camRollFields);
		}
	}

	private static float getDmitriyStarStareCloseness() {
		return MathHelper.sqrt_float(MathHelper.clamp_float(dmitriyStarStareIntensity, 0.0F, 1.0F));
	}

	@SubscribeEvent
	public void onRenderTick(TickEvent.RenderTickEvent event) {
		if(event.phase != Phase.END) {
			return;
		}
		Minecraft mc = Minecraft.getMinecraft();
		WorldClient world = mc != null ? mc.theWorld : null;
		if(mc == null || mc.entityRenderer == null) {
			return;
		}
		float roll = 0.0F;
		boolean blackholeTiltActive = false;
		if(world != null && world.provider != null && world.provider.dimensionId == SpaceConfig.dmitriyDimension) {
			float t = (float)(world.getTotalWorldTime() + event.renderTickTime);
			roll = MathHelper.sin(t * 0.001F) * 2.0F;

			if(dmitriyStarStareIntensity > 0.001F) {
				final float dmitriyStarStareShakeMinDeg = 0.25F;
				final float dmitriyStarStareShakeMaxDeg = 2.25F;
				float closeness = MathHelper.sqrt_float(MathHelper.clamp_float(dmitriyStarStareIntensity, 0.0F, 1.0F));
				float shakeAmp = dmitriyStarStareShakeMinDeg
						+ (dmitriyStarStareShakeMaxDeg - dmitriyStarStareShakeMinDeg) * closeness;

				float tremor = 0.0F;
				tremor += MathHelper.sin(t * 0.90F);
				tremor += MathHelper.sin(t * 2.60F + 1.30F) * 0.55F;
				tremor += MathHelper.cos(t * 4.10F + 0.20F) * 0.30F;
				tremor /= 1.85F;

				roll += tremor * shakeAmp;
			}
		} else {
			blackholeTiltActive = isBlackholeCollapseCameraTiltActive(world);
		}
		if(blackholeTiltActive) {
			voidFightsBackShakeTicks = 0;
		}

		if(voidFightsBackShakeTicks > 0 && !blackholeTiltActive) {
			float t = (float)(world != null ? (world.getTotalWorldTime() + event.renderTickTime) : event.renderTickTime);
			float intensity = MathHelper.clamp_float(voidFightsBackShakeTicks / (float)(30 * 20), 0.0F, 1.0F);
			float amp = 0.08F + intensity * 0.55F;
			float tremor = 0.0F;
			tremor += MathHelper.sin(t * 1.35F);
			tremor += MathHelper.cos(t * 2.75F + 0.3F) * 0.65F;
			tremor += MathHelper.sin(t * 4.80F + 1.2F) * 0.25F;
			tremor /= 1.9F;
			roll += tremor * amp;
		}

		String[] camRollFields = new String[] { "camRoll", "field_78495_O", "O" };
		ReflectionHelper.setPrivateValue(net.minecraft.client.renderer.EntityRenderer.class, mc.entityRenderer, roll, camRollFields);
	}

	public static boolean ducked = false;

	@SubscribeEvent
	public void preRenderEvent(RenderPlayerEvent.Pre event) {

		RenderPlayer renderer = event.renderer;
		AbstractClientPlayer player = (AbstractClientPlayer)event.entityPlayer;

		PotionEffect invis = player.getActivePotionEffect(Potion.invisibility);

		if(invis != null && invis.getAmplifier() > 0)
			event.setCanceled(true);

		if(player.getDisplayName().toLowerCase(Locale.US).equals("martmn")) {

			event.setCanceled(true);

			float pX = (float) (player.prevPosX + (player.posX - player.prevPosX) * (double)event.partialRenderTick);
			float pY = (float) (player.prevPosY + (player.posY - player.prevPosY) * (double)event.partialRenderTick);
			float pZ = (float) (player.prevPosZ + (player.posZ - player.prevPosZ) * (double)event.partialRenderTick);
			EntityPlayer me = Minecraft.getMinecraft().thePlayer;
			float mX = (float) (me.prevPosX + (me.posX - me.prevPosX) * (double)event.partialRenderTick);
			float mY = (float) (me.prevPosY + (me.posY - me.prevPosY) * (double)event.partialRenderTick);
			float mZ = (float) (me.prevPosZ + (me.posZ - me.prevPosZ) * (double)event.partialRenderTick);

			Minecraft.getMinecraft().renderEngine.bindTexture(new ResourceLocation(RefStrings.MODID + ":textures/particle/fart.png"));
			GL11.glPushMatrix();
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glDisable(GL11.GL_CULL_FACE);
			GL11.glTranslatef(pX - mX, pY - mY + 0.75F - (float)player.getYOffset(), pZ - mZ);
			GL11.glRotatef(-me.rotationYaw, 0.0F, 1.0F, 0.0F);
			GL11.glRotatef(me.rotationPitch, 1.0F, 0.0F, 0.0F);
			Tessellator t = Tessellator.instance;
			t.startDrawingQuads();
			t.setBrightness(240);
			t.addVertexWithUV(-1, 1, 0, 0, 0);
			t.addVertexWithUV(1, 1, 0, 1, 0);
			t.addVertexWithUV(1, -1, 0, 1, 1);
			t.addVertexWithUV(-1, -1, 0, 0, 1);
			t.draw();

			GL11.glEnable(GL11.GL_LIGHTING);

			GL11.glPopMatrix();
		}

		ResourceLocation cloak = RenderAccessoryUtility.getCloakFromPlayer(player);

		if(cloak != null)
			player.func_152121_a(Type.CAPE, cloak);

		if(player.getHeldItem() != null && player.getHeldItem().getItem() instanceof IHoldableWeapon) {
			renderer.modelBipedMain.aimedBow = true;
			renderer.modelArmor.aimedBow = true;
			renderer.modelArmorChestplate.aimedBow = true;
		}
	}

	@SubscribeEvent
	public void onRenderArmorEvent(RenderPlayerEvent.SetArmorModel event) {

		EntityPlayer player = event.entityPlayer;

		for(int i = 0; i < 4; i++) {

			ItemStack armor = player.getCurrentArmor(i);

			if(armor != null && ArmorModHandler.hasMods(armor)) {

				for(ItemStack mod : ArmorModHandler.pryMods(armor)) {

					if(mod != null && mod.getItem() instanceof ItemArmorMod) {
						((ItemArmorMod)mod.getItem()).modRender(event, armor);
					}
				}
			}

			//because armor that isn't ItemArmor doesn't render at all
			if(armor != null && armor.getItem() instanceof JetpackBase) {
				((ItemArmorMod)armor.getItem()).modRender(event, armor);
			}

			if(armor != null && armor.getItem() instanceof ItemModHeavyBoots) {
				((ItemModHeavyBoots)armor.getItem()).armorRender(event, armor);
			}
		}

		if(player.getCurrentArmor(2) == null && !player.isPotionActive(Potion.invisibility)) {
			if(player.getUniqueID().toString().equals(ShadyUtil.HbMinecraft) ||		player.getDisplayName().equals("HbMinecraft"))		RenderAccessoryUtility.renderWings(event, 2);
			if(player.getUniqueID().toString().equals(ShadyUtil.the_NCR) ||			player.getDisplayName().equals("the_NCR"))			RenderAccessoryUtility.renderWings(event, 3);
			if(player.getUniqueID().toString().equals(ShadyUtil.Barnaby99_x) ||		player.getDisplayName().equals("pheo7"))			RenderAccessoryUtility.renderAxePack(event);
			if(player.getUniqueID().toString().equals(ShadyUtil.LePeeperSauvage) ||	player.getDisplayName().equals("LePeeperSauvage"))	RenderAccessoryUtility.renderFaggot(event);
		}
	}

	private static ISound currentSong;

	private static final ResourceLocation BLACKHOLE_COLLAPSE_EVENT_MUSIC = new ResourceLocation("hbm:music.collapseImminent");

	public static void stopCurrentMusic() {
		Minecraft mc = Minecraft.getMinecraft();
		if(mc == null || mc.getSoundHandler() == null) return;

		if(currentSong != null && mc.getSoundHandler().isSoundPlaying(currentSong)) {
			mc.getSoundHandler().stopSound(currentSong);
		}
		currentSong = null;

		net.minecraft.client.audio.MusicTicker musicTicker = ReflectionHelper.getPrivateValue(
			Minecraft.class,
			mc,
			new String[] { "mcMusicTicker", "field_147126_aw" }
		);
		if(musicTicker != null) {
			ISound tickerSong = ReflectionHelper.getPrivateValue(
				net.minecraft.client.audio.MusicTicker.class,
				musicTicker,
				new String[] { "field_147678_c", "currentMusic" }
			);
			if(tickerSong != null && mc.getSoundHandler().isSoundPlaying(tickerSong)) {
				mc.getSoundHandler().stopSound(tickerSong);
			}
			ReflectionHelper.setPrivateValue(
				net.minecraft.client.audio.MusicTicker.class,
				musicTicker,
				null,
				new String[] { "field_147678_c", "currentMusic" }
			);
		}
	}

	private static int getBlackholeCollapseWarningLevel(World world) {
		if(!isBlackholeCollapseFxAllowedInDimension(world)) {
			return 0;
		}

		CBT_SkyState skyState = CBT_SkyState.get(world);
		if(skyState == null || skyState.getState() != CBT_SkyState.SkyState.BLACKHOLE) {
			return 0;
		}

		long collapseEndTick = skyState.getBlackholeCollapseEndTick();
		if(collapseEndTick <= 0L) {
			return 0;
		}

		long now = world.getTotalWorldTime();
		long collapseStartTick = collapseEndTick - StarcoreSkyEffects.BLACKHOLE_COLLAPSE_DURATION_TICKS;
		long visualCortexStartTick = collapseStartTick + 15 * 20;
		long gravityMalfunctionStartTick = collapseStartTick + StarcoreSkyEffects.BLACKHOLE_GRAVITY_MALFUNCTION_DELAY_TICKS;
		long digammaWarningStartTick = collapseStartTick + 40 * 20;
		if(now >= digammaWarningStartTick && now < collapseEndTick) {
			return 3;
		}
		if(now >= gravityMalfunctionStartTick && now < collapseEndTick) {
			return 2;
		}
		if(now >= visualCortexStartTick && now < gravityMalfunctionStartTick && now < collapseEndTick) {
			return 1;
		}
		return 0;
	}

	private static boolean isBlackholeCollapseCameraTiltActive(World world) {
		if(world instanceof WorldClient && SkyProviderCelestial.isBlackholeCollapseMeteorPhaseActive((WorldClient) world)) {
			return true;
		}
		if(!isBlackholeCollapseFxAllowedInDimension(world)) {
			return false;
		}

		CBT_SkyState skyState = CBT_SkyState.get(world);
		if(skyState == null || skyState.getState() != CBT_SkyState.SkyState.BLACKHOLE) {
			return false;
		}

		long collapseEndTick = skyState.getBlackholeCollapseEndTick();
		if(collapseEndTick <= 0L) {
			return false;
		}

		long collapseStartTick = collapseEndTick - StarcoreSkyEffects.BLACKHOLE_COLLAPSE_DURATION_TICKS;
		long cameraTiltStartTick = collapseStartTick + 54 * 20;
		long now = world.getTotalWorldTime();
		return now >= cameraTiltStartTick && now < collapseEndTick;
	}

	static boolean isBlackholeCollapseCameraTiltWindowActive(World world) {
		return isBlackholeCollapseCameraTiltActive(world);
	}

	private static boolean isBlackholeGravityRampWarningActive(World world) {
		if(!isBlackholeCollapseFxAllowedInDimension(world)) {
			return false;
		}

		CBT_SkyState skyState = CBT_SkyState.get(world);
		if(skyState == null || skyState.getState() != CBT_SkyState.SkyState.BLACKHOLE) {
			return false;
		}

		long collapseEndTick = skyState.getBlackholeCollapseEndTick();
		if(collapseEndTick <= 0L) {
			return false;
		}

		long now = world.getTotalWorldTime();
		long collapseStartTick = collapseEndTick - StarcoreSkyEffects.BLACKHOLE_COLLAPSE_DURATION_TICKS;
		long gravityStartTick = collapseStartTick + StarcoreSkyEffects.BLACKHOLE_GRAVITY_MALFUNCTION_DELAY_TICKS;
		long gravityRampEndTick = gravityStartTick + StarcoreSkyEffects.BLACKHOLE_GRAVITY_MALFUNCTION_RAMP_TICKS;
		long warningEndTick = Math.min(gravityRampEndTick, collapseEndTick);
		return now >= gravityStartTick && now < warningEndTick;
	}

	private static boolean isBlackholeObfuscatedTooltipWindowActive(World world) {
		if(!isBlackholeCollapseFxAllowedInDimension(world)) {
			return false;
		}

		CBT_SkyState skyState = CBT_SkyState.get(world);
		if(skyState == null || skyState.getState() != CBT_SkyState.SkyState.BLACKHOLE) {
			return false;
		}

		long collapseEndTick = skyState.getBlackholeCollapseEndTick();
		if(collapseEndTick <= 0L) {
			return false;
		}

		long collapseStartTick = collapseEndTick - StarcoreSkyEffects.BLACKHOLE_COLLAPSE_DURATION_TICKS;
		long tooltipStartTick = collapseStartTick + 79 * 20;
		long tooltipEndTick = collapseStartTick + 91 * 20 + 10;
		long now = world.getTotalWorldTime();
		return now >= tooltipStartTick && now <= tooltipEndTick && now < collapseEndTick;
	}

	private static int getBlackholeWarningObfuscationStage(World world) {
		if(!isBlackholeObfuscatedTooltipWindowActive(world)) {
			return 0;
		}

		CBT_SkyState skyState = CBT_SkyState.get(world);
		if(skyState == null) {
			return 0;
		}

		long collapseEndTick = skyState.getBlackholeCollapseEndTick();
		if(collapseEndTick <= 0L) {
			return 0;
		}

		long collapseStartTick = collapseEndTick - StarcoreSkyEffects.BLACKHOLE_COLLAPSE_DURATION_TICKS;
		long tooltipStartTick = collapseStartTick + 79 * 20;
		long elapsedTicks = world.getTotalWorldTime() - tooltipStartTick;
		if(elapsedTicks < 60) {
			return 1;
		}

		return 2;
	}

	private static int getBlackholeObfuscatedTooltipCount(World world) {
		if(!isBlackholeCollapseFxAllowedInDimension(world)) {
			return 0;
		}

		CBT_SkyState skyState = CBT_SkyState.get(world);
		if(skyState == null || skyState.getState() != CBT_SkyState.SkyState.BLACKHOLE) {
			return 0;
		}

		long collapseEndTick = skyState.getBlackholeCollapseEndTick();
		if(collapseEndTick <= 0L) {
			return 0;
		}

		long collapseStartTick = collapseEndTick - StarcoreSkyEffects.BLACKHOLE_COLLAPSE_DURATION_TICKS;
		long tooltipStartTick = collapseStartTick + 79 * 20;
		long tooltipEndTick = collapseStartTick + 91 * 20 + 10;
		long now = world.getTotalWorldTime();
		if(now < tooltipStartTick || now > tooltipEndTick || now >= collapseEndTick) {
			return 0;
		}

		long elapsedTicks = now - tooltipStartTick;
		if(elapsedTicks < 60) {
			return 0;
		}

		long totalWindowTicks = tooltipEndTick - tooltipStartTick;
		if(totalWindowTicks < 60) {
			return 0;
		}

		long elapsedAfterInitialDelay = elapsedTicks - 60;
		int count = (int)(elapsedAfterInitialDelay / 60) + 1;
		int maxCount = (int)((totalWindowTicks - 60) / 60) + 1;
		return Math.min(maxCount, count);
	}

	private static boolean isBlackholeItsHereTooltipActive(World world) {
		if(!isBlackholeCollapseFxAllowedInDimension(world)) {
			return false;
		}

		CBT_SkyState skyState = CBT_SkyState.get(world);
		if(skyState == null || skyState.getState() != CBT_SkyState.SkyState.BLACKHOLE) {
			return false;
		}

		long collapseEndTick = skyState.getBlackholeCollapseEndTick();
		if(collapseEndTick <= 0L) {
			return false;
		}

		long collapseStartTick = collapseEndTick - StarcoreSkyEffects.BLACKHOLE_COLLAPSE_DURATION_TICKS;
		long tooltipEndTick = collapseStartTick + 91 * 20 + 10;
		long now = world.getTotalWorldTime();
		return now > tooltipEndTick && now < collapseEndTick;
	}

	private static long getBlackholeCollapseEndTick(World world) {
		if(!isBlackholeCollapseFxAllowedInDimension(world)) {
			return -1L;
		}
		CBT_SkyState skyState = CBT_SkyState.get(world);
		if(skyState == null || skyState.getState() != CBT_SkyState.SkyState.BLACKHOLE) {
			return -1L;
		}
		return skyState.getBlackholeCollapseEndTick();
	}

	private static boolean isBlackholeCollapseFxAllowedInDimension(World world) {
		if(world == null || world.provider == null) {
			return false;
		}
		return StarcoreSkyEffects.isBlackholeCollapseStartAllowedDimension(world.provider.dimensionId);
	}

	private static void resetBlackholeItsHereFxState() {
		blackholeItsHereFxDimension = Integer.MIN_VALUE;
		blackholeItsHereFxCollapseEndTick = Long.MIN_VALUE;
	}

	public static float getBlackholeItsHereWorldTintStrength(World world) {
		if(world == null || world.provider == null) {
			return 0.0F;
		}
		if(blackholeItsHereFxCollapseEndTick <= 0L) {
			return 0.0F;
		}
		if(world.provider.dimensionId != blackholeItsHereFxDimension) {
			return 0.0F;
		}

		long now = world.getTotalWorldTime();
		if(now >= blackholeItsHereFxCollapseEndTick) {
			return 0.0F;
		}
		return 0.32F;
	}

	private static void resetBlackholeGravityLiftFxState() {
		blackholeGravityLiftFxDimension = Integer.MIN_VALUE;
		blackholeGravityLiftFxCollapseEndTick = Long.MIN_VALUE;
		blackholeGravityLiftFxNextSpawnTick = Long.MIN_VALUE;
		blackholeGravityLiftFxNextChunkSpawnTick = Long.MIN_VALUE;
		blackholeGravityLiftBlocks.clear();
		blackholeGravityLiftChunks.clear();
	}

	private static void updateBlackholeGravityLiftFx(Minecraft mc) {
		if(mc == null || mc.theWorld == null || mc.thePlayer == null || mc.theWorld.provider == null) {
			return;
		}

		World world = mc.theWorld;
		int dimension = world.provider.dimensionId;
		if(!StarcoreSkyEffects.isBlackholeGravityLiftFxAllowedDimension(dimension)) {
			resetBlackholeGravityLiftFxState();
			return;
		}

		CBT_SkyState skyState = CBT_SkyState.get(world);
		if(skyState == null || skyState.getState() != CBT_SkyState.SkyState.BLACKHOLE) {
			resetBlackholeGravityLiftFxState();
			return;
		}

		long collapseEndTick = skyState.getBlackholeCollapseEndTick();
		if(collapseEndTick <= 0L) {
			resetBlackholeGravityLiftFxState();
			return;
		}

		long now = world.getTotalWorldTime();
		long collapseStartTick = collapseEndTick - StarcoreSkyEffects.BLACKHOLE_COLLAPSE_DURATION_TICKS;
		long gravityEndTick = collapseStartTick
			+ StarcoreSkyEffects.BLACKHOLE_GRAVITY_MALFUNCTION_DELAY_TICKS
			+ StarcoreSkyEffects.BLACKHOLE_GRAVITY_MALFUNCTION_RAMP_TICKS;

		if(now < gravityEndTick || now >= collapseEndTick) {
			resetBlackholeGravityLiftFxState();
			return;
		}

		boolean newCollapseWindow = blackholeGravityLiftFxDimension != dimension || blackholeGravityLiftFxCollapseEndTick != collapseEndTick;
		if(newCollapseWindow) {
			blackholeGravityLiftFxDimension = dimension;
			blackholeGravityLiftFxCollapseEndTick = collapseEndTick;
			blackholeGravityLiftFxNextSpawnTick = now + 1L;
			blackholeGravityLiftFxNextChunkSpawnTick = now + 8L;
			blackholeGravityLiftBlocks.clear();
			blackholeGravityLiftChunks.clear();
		}

		int baseX = MathHelper.floor_double(mc.thePlayer.posX);
		int baseZ = MathHelper.floor_double(mc.thePlayer.posZ);
		double playerX = mc.thePlayer.posX;
		double playerZ = mc.thePlayer.posZ;
		tickBlackholeGravityLiftFx(playerX, playerZ);

		Random rand = world.rand;
		if(blackholeGravityLiftBlocks.size() < 100 && now >= blackholeGravityLiftFxNextSpawnTick) {
			int delayRange = 8 - 2 + 1;
			boolean spawned = false;
			for(int i = 0; i < 20; i++) {
				double angle = rand.nextDouble() * Math.PI * 2.0D;
				double radius = Math.sqrt(rand.nextDouble()) * 100.0D;
				int x = baseX + MathHelper.floor_double(Math.cos(angle) * radius);
				int z = baseZ + MathHelper.floor_double(Math.sin(angle) * radius);

				int topY = world.getTopSolidOrLiquidBlock(x, z);
				if(topY <= 0) {
					continue;
				}

				int y = topY - 1;
				Block block = world.getBlock(x, y, z);
				if(!isLiftBlockEligible(world, x, y, z, block)) {
					continue;
				}
				int meta = world.getBlockMetadata(x, y, z);

				double px = x + 0.5D;
				double pz = z + 0.5D;
				if(isLiftBlockTooClose(px, pz)) {
					continue;
				}

				double riseSpeed = 0.018D + rand.nextDouble() * (0.042D - 0.018D);
				int maxAgeTicks = Math.max(1, MathHelper.ceiling_double_int(150.0D / riseSpeed));
				double rotYaw = 0.0D;
				double rotPitch = 0.0D;
				double rotYawSpeed = randomSignedSpeed(rand, 0.04D, 0.16D);
				double rotPitchSpeed = randomSignedSpeed(rand, 0.04D, 0.16D);
				if(!world.setBlockToAir(x, y, z)) {
					continue;
				}
				blackholeGravityLiftBlocks.add(new BlackholeGravityLiftBlock(block, meta, px, y + 1.02D, pz, riseSpeed, maxAgeTicks, rotYaw, rotPitch, rotYawSpeed, rotPitchSpeed));
				spawned = true;
				break;
			}

			int nextDelay = 2 + rand.nextInt(delayRange);
			if(!spawned) {
				nextDelay += 6;
			}
			blackholeGravityLiftFxNextSpawnTick = now + nextDelay;
		}

		if(blackholeGravityLiftChunks.size() < 10 && now >= blackholeGravityLiftFxNextChunkSpawnTick) {
			int chunkDelayRange = 28 - 10 + 1;
			boolean chunkSpawned = trySpawnLiftChunk(world, baseX, baseZ, rand);
			int nextChunkDelay = 10 + rand.nextInt(chunkDelayRange);
			if(!chunkSpawned) {
				nextChunkDelay += 35;
			}
			blackholeGravityLiftFxNextChunkSpawnTick = now + nextChunkDelay;
		}
	}

	private static boolean isLiftBlockTooClose(double x, double z) {
		for(BlackholeGravityLiftBlock block : blackholeGravityLiftBlocks) {
			double dx = block.x - x;
			double dz = block.z - z;
			if(dx * dx + dz * dz < 0.65D) {
				return true;
			}
		}
		for(BlackholeGravityLiftChunk chunk : blackholeGravityLiftChunks) {
			double dx = chunk.x - x;
			double dz = chunk.z - z;
			if(dx * dx + dz * dz < 2.0D) {
				return true;
			}
		}
		return false;
	}

	private static boolean isLiftChunkTooClose(double x, double z) {
		for(BlackholeGravityLiftChunk chunk : blackholeGravityLiftChunks) {
			double dx = chunk.x - x;
			double dz = chunk.z - z;
			if(dx * dx + dz * dz < 36.0D) {
				return true;
			}
		}
		return false;
	}

	private static boolean trySpawnLiftChunk(World world, int baseX, int baseZ, Random rand) {
		for(int attempt = 0; attempt < 28; attempt++) {
			double angle = rand.nextDouble() * Math.PI * 2.0D;
			double radius = Math.sqrt(rand.nextDouble()) * 100.0D;
			int x = baseX + MathHelper.floor_double(Math.cos(angle) * radius);
			int z = baseZ + MathHelper.floor_double(Math.sin(angle) * radius);
			int topY = world.getTopSolidOrLiquidBlock(x, z);
			if(topY <= 0) {
				continue;
			}

			int y = topY - 1;
			Block centerBlock = world.getBlock(x, y, z);
			if(!isLiftBlockEligible(world, x, y, z, centerBlock)) {
				continue;
			}

			double px = x + 0.5D;
			double pz = z + 0.5D;
			if(isLiftChunkTooClose(px, pz)) {
				continue;
			}

			int size = 32 + rand.nextInt(64 - 32 + 1);
			WorldInAJar jar = new WorldInAJar(size, size, size);
			int middle = size / 2 - 1;
			int copied = 0;
			Set<Long> sourcePositions = new HashSet<Long>();

			for(int ix = 0; ix < 2; ix++) {
				for(int iy = 0; iy < 2; iy++) {
					for(int iz = 0; iz < 2; iz++) {
						copied += copyLiftBlockIntoJar(world, jar, middle + ix, middle + iy, middle + iz, x + ix, y + iy, z + iz, sourcePositions);
					}
				}
			}

			for(int layer = 2; layer <= (size / 2); layer++) {
				for(int i = 0; i < 34; i++) {
					int jx = -layer + rand.nextInt(layer * 2 + 1);
					int jy = -layer + rand.nextInt(layer * 2 + 1);
					int jz = -layer + rand.nextInt(layer * 2 + 1);
					int jarX = middle + jx;
					int jarY = middle + jy;
					int jarZ = middle + jz;
					if(jar.getBlock(jarX + 1, jarY, jarZ) != Blocks.air
						|| jar.getBlock(jarX - 1, jarY, jarZ) != Blocks.air
						|| jar.getBlock(jarX, jarY + 1, jarZ) != Blocks.air
						|| jar.getBlock(jarX, jarY - 1, jarZ) != Blocks.air
						|| jar.getBlock(jarX, jarY, jarZ + 1) != Blocks.air
						|| jar.getBlock(jarX, jarY, jarZ - 1) != Blocks.air) {
						copied += copyLiftBlockIntoJar(world, jar, jarX, jarY, jarZ, x + jx, y + jy, z + jz, sourcePositions);
					}
				}
			}

			if(copied < 6) {
				continue;
			}
			if(!removeLiftSourceBlocks(world, sourcePositions)) {
				continue;
			}

			double riseSpeed = 0.010D + rand.nextDouble() * (0.022D - 0.010D);
			int maxAgeTicks = Math.max(1, MathHelper.ceiling_double_int(150.0D / riseSpeed));
			double rotYaw = 0.0D;
			double rotPitch = 0.0D;
			double rotYawSpeed = randomSignedSpeed(rand, 0.02D, 0.08D);
			double rotPitchSpeed = randomSignedSpeed(rand, 0.02D, 0.08D);
			blackholeGravityLiftChunks.add(new BlackholeGravityLiftChunk(jar, px, y + 1.0D, pz, riseSpeed, maxAgeTicks, rotYaw, rotPitch, rotYawSpeed, rotPitchSpeed));
			return true;
		}
		return false;
	}

	private static double randomSignedSpeed(Random rand, double min, double max) {
		double mag = min + rand.nextDouble() * (max - min);
		return rand.nextBoolean() ? mag : -mag;
	}

	private static int copyLiftBlockIntoJar(World world, WorldInAJar jar, int jarX, int jarY, int jarZ, int worldX, int worldY, int worldZ, Set<Long> sourcePositions) {
		Block block = world.getBlock(worldX, worldY, worldZ);
		if(!isLiftBlockEligible(world, worldX, worldY, worldZ, block)) {
			return 0;
		}
		int meta = world.getBlockMetadata(worldX, worldY, worldZ);
		jar.setBlock(jarX, jarY, jarZ, block, meta);
		sourcePositions.add(packBlockPos(worldX, worldY, worldZ));
		return 1;
	}

	private static boolean removeLiftSourceBlocks(World world, Set<Long> sourcePositions) {
		if(sourcePositions.isEmpty()) {
			return false;
		}
		int removedCount = 0;
		for(Long packed : sourcePositions) {
			int x = unpackBlockX(packed.longValue());
			int y = unpackBlockY(packed.longValue());
			int z = unpackBlockZ(packed.longValue());
			if(world.setBlockToAir(x, y, z)) {
				removedCount++;
			}
		}
		return removedCount == sourcePositions.size();
	}

	private static boolean isLiftBlockEligible(World world, int x, int y, int z, Block block) {
		if(block == null || block == Blocks.air || block.isAir(world, x, y, z)) {
			return false;
		}
		Material material = block.getMaterial();
		if(material == Material.air || material.isLiquid()) {
			return false;
		}
		if(!isVanillaBlock(block)) {
			return false;
		}
		int meta = world.getBlockMetadata(x, y, z);
		return !block.hasTileEntity(meta);
	}

	private static boolean isVanillaBlock(Block block) {
		Object nameObj = Block.blockRegistry.getNameForObject(block);
		if(!(nameObj instanceof String)) {
			return false;
		}
		String registryName = (String)nameObj;
		return registryName.startsWith("minecraft:");
	}

	private static long packBlockPos(int x, int y, int z) {
		return ((long)(x & 67108863) << 38) | ((long)(z & 67108863) << 12) | (long)(y & 4095);
	}

	private static int unpackBlockX(long packed) {
		int x = (int)(packed >> 38);
		return x >= 33554432 ? x - 67108864 : x;
	}

	private static int unpackBlockY(long packed) {
		int y = (int)(packed & 4095L);
		return y >= 2048 ? y - 4096 : y;
	}

	private static int unpackBlockZ(long packed) {
		int z = (int)((packed >> 12) & 67108863L);
		return z >= 33554432 ? z - 67108864 : z;
	}

	private static void tickBlackholeGravityLiftFx(double playerX, double playerZ) {
		Iterator<BlackholeGravityLiftBlock> iterator = blackholeGravityLiftBlocks.iterator();
		while(iterator.hasNext()) {
			BlackholeGravityLiftBlock block = iterator.next();
			block.ageTicks++;
			if(block.ageTicks >= block.maxAgeTicks || isLiftOutsideFollowRadius(block.x, block.z, playerX, playerZ)) {
				iterator.remove();
			}
		}

		Iterator<BlackholeGravityLiftChunk> chunkIterator = blackholeGravityLiftChunks.iterator();
		while(chunkIterator.hasNext()) {
			BlackholeGravityLiftChunk chunk = chunkIterator.next();
			chunk.ageTicks++;
			if(chunk.ageTicks >= chunk.maxAgeTicks || isLiftOutsideFollowRadius(chunk.x, chunk.z, playerX, playerZ)) {
				chunkIterator.remove();
			}
		}
	}

	private static boolean isLiftOutsideFollowRadius(double x, double z, double playerX, double playerZ) {
		double dx = x - playerX;
		double dz = z - playerZ;
		double maxDist = 100.0D;
		return dx * dx + dz * dz > maxDist * maxDist;
	}

	private static boolean isBlackholeCollapseMusicLockActive(World world) {
		if(!isBlackholeCollapseFxAllowedInDimension(world)) {
			return false;
		}
		long collapseEndTick = getBlackholeCollapseEndTick(world);
		if(collapseEndTick <= 0L) {
			return false;
		}
		long collapseStartTick = collapseEndTick - StarcoreSkyEffects.BLACKHOLE_COLLAPSE_DURATION_TICKS;
		long now = world.getTotalWorldTime();
		return now >= collapseStartTick && now < collapseEndTick;
	}

	private static boolean shouldBlockMusicDuringBlackholeCollapse(World world, ResourceLocation soundLocation) {
		if(!isBlackholeCollapseMusicLockActive(world) || soundLocation == null) {
			return false;
		}
		if(BLACKHOLE_COLLAPSE_EVENT_MUSIC.equals(soundLocation)) {
			return false;
		}
		return soundLocation.getResourcePath().startsWith("music.");
	}

	@SideOnly(Side.CLIENT)
	private static void renderBlackholeGravityLiftFx(RenderWorldLastEvent event) {
		if(blackholeGravityLiftBlocks.isEmpty() && blackholeGravityLiftChunks.isEmpty()) {
			return;
		}

		Minecraft mc = Minecraft.getMinecraft();
		if(mc == null || mc.thePlayer == null) {
			return;
		}

		EntityPlayer player = mc.thePlayer;
		World world = player.worldObj;
		if(world == null) {
			return;
		}
		if(world.provider == null || !StarcoreSkyEffects.isBlackholeGravityLiftFxAllowedDimension(world.provider.dimensionId)) {
			resetBlackholeGravityLiftFxState();
			return;
		}
		double cameraX = player.prevPosX + (player.posX - player.prevPosX) * event.partialTicks;
		double cameraY = player.prevPosY + (player.posY - player.prevPosY) * event.partialTicks;
		double cameraZ = player.prevPosZ + (player.posZ - player.prevPosZ) * event.partialTicks;
		mc.entityRenderer.enableLightmap((double)event.partialTicks);

		RenderHelper.disableStandardItemLighting();
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glPushMatrix();
		{
			mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
			GL11.glShadeModel(GL11.GL_SMOOTH);
			for(BlackholeGravityLiftBlock block : blackholeGravityLiftBlocks) {
				double y = block.y + (block.ageTicks + event.partialTicks) * block.riseSpeed;
				double rotTicks = block.ageTicks + event.partialTicks;
				float yaw = (float)(block.rotYaw + rotTicks * block.rotYawSpeed);
				float pitch = (float)(block.rotPitch + rotTicks * block.rotPitchSpeed);
				int bx = MathHelper.floor_double(block.x);
				int by = MathHelper.floor_double(block.y);
				int bz = MathHelper.floor_double(block.z);
				int brightness = world.getLightBrightnessForSkyBlocks(bx, by, bz, 0);
				block.jar.lightlevel = brightness;
				GL11.glPushMatrix();
				{
					GL11.glTranslated(block.x - cameraX, y - cameraY, block.z - cameraZ);
					GL11.glTranslated(0.5D, 0.5D, 0.5D);
					GL11.glRotatef(yaw, 0.0F, 1.0F, 0.0F);
					GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
					GL11.glTranslated(-0.5D, -0.5D, -0.5D);
					GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
					Tessellator.instance.startDrawingQuads();
					Tessellator.instance.setBrightness(brightness);
					try {
						RenderBlocks renderer = new RenderBlocks(block.jar);
						renderer.enableAO = true;
						renderer.renderBlockByRenderType(block.jar.getBlock(0, 0, 0), 0, 0, 0);
					} catch(Exception ignored) { }
					Tessellator.instance.draw();
				}
				GL11.glPopMatrix();
			}
			for(BlackholeGravityLiftChunk chunk : blackholeGravityLiftChunks) {
				double y = chunk.y + (chunk.ageTicks + event.partialTicks) * chunk.riseSpeed;
				double rotTicks = chunk.ageTicks + event.partialTicks;
				float yaw = (float)(chunk.rotYaw + rotTicks * chunk.rotYawSpeed);
				float pitch = (float)(chunk.rotPitch + rotTicks * chunk.rotPitchSpeed);
				int bx = MathHelper.floor_double(chunk.x);
				int by = MathHelper.floor_double(chunk.y + chunk.jar.sizeY * 0.5D);
				int bz = MathHelper.floor_double(chunk.z);
				int brightness = world.getLightBrightnessForSkyBlocks(bx, by, bz, 0);
				chunk.jar.lightlevel = brightness;
				GL11.glPushMatrix();
				{
					GL11.glTranslated(chunk.x - cameraX, y - cameraY, chunk.z - cameraZ);
					GL11.glRotatef(yaw, 0.0F, 1.0F, 0.0F);
					GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
					GL11.glTranslated(-chunk.jar.sizeX / 2.0D, -chunk.jar.sizeY / 2.0D, -chunk.jar.sizeZ / 2.0D);
					GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
					Tessellator.instance.startDrawingQuads();
					Tessellator.instance.setBrightness(brightness);
					try {
						RenderBlocks renderer = new RenderBlocks(chunk.jar);
						renderer.enableAO = true;
						for(int ix = 0; ix < chunk.jar.sizeX; ix++) {
							for(int iy = 0; iy < chunk.jar.sizeY; iy++) {
								for(int iz = 0; iz < chunk.jar.sizeZ; iz++) {
									renderer.renderBlockByRenderType(chunk.jar.getBlock(ix, iy, iz), ix, iy, iz);
								}
							}
						}
					} catch(Exception ignored) { }
					Tessellator.instance.draw();
				}
				GL11.glPopMatrix();
			}
			GL11.glShadeModel(GL11.GL_FLAT);
		}
		GL11.glPopMatrix();
		mc.entityRenderer.disableLightmap((double)event.partialTicks);
		RenderHelper.enableStandardItemLighting();
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private static void triggerBlackholeItsHereFx(World world, long collapseEndTick) {
		if(!isBlackholeCollapseFxAllowedInDimension(world) || collapseEndTick <= 0L) {
			return;
		}

		int dimension = world.provider.dimensionId;
		if(blackholeItsHereFxDimension == dimension && blackholeItsHereFxCollapseEndTick == collapseEndTick) {
			return;
		}

		blackholeItsHereFxDimension = dimension;
		blackholeItsHereFxCollapseEndTick = collapseEndTick;
		starcoreFlashTimestamp = System.currentTimeMillis();
	}

	private static void updateBlackholeGravityWarningTooltip(Minecraft mc) {
		if(mc == null || mc.theWorld == null || mc.thePlayer == null) {
			return;
		}
		int warningLevel = getBlackholeCollapseWarningLevel(mc.theWorld);
		if(warningLevel <= 0) {
			return;
		}

		boolean blinkRed = ((mc.theWorld.getTotalWorldTime() / 5L) & 1L) == 0L;
		String color = blinkRed ? EnumChatFormatting.RED.toString() : EnumChatFormatting.YELLOW.toString();
		int warningObfuscationStage = getBlackholeWarningObfuscationStage(mc.theWorld);
		if(warningLevel >= 1) {
			String visualCortexText = warningObfuscationStage >= 1 && warningLevel >= 3
				? color + EnumChatFormatting.OBFUSCATED + "VISUAL CORTEX MALFUNCTION" + EnumChatFormatting.RESET
				: color + "VISUAL CORTEX MALFUNCTION";
			MainRegistry.proxy.displayTooltip(
				visualCortexText,
				250,
				ServerProxy.ID_VISUAL_CORTEX_MALFUNCTION
			);
		}
		if(warningLevel >= 2 && isBlackholeGravityRampWarningActive(mc.theWorld)) {
			MainRegistry.proxy.displayTooltip(
				color + "GRAVITATIONAL FORCES CHANGE DETECTED",
				250,
				ServerProxy.ID_GRAVITY_MALFUNCTION
			);
		}
		if(warningLevel >= 3) {
			String digammaText = warningObfuscationStage >= 2
				? color + EnumChatFormatting.OBFUSCATED + "DIGAMMA RADIATION LEVELS INCREASING" + EnumChatFormatting.RESET
				: color + "DIGAMMA RADIATION LEVELS INCREASING";
			MainRegistry.proxy.displayTooltip(
				digammaText,
				250,
				ServerProxy.ID_DIGAMMA_RADIATION_WARNING
			);

			int obfuscatedTooltipCount = getBlackholeObfuscatedTooltipCount(mc.theWorld);
			for(int i = 0; i < obfuscatedTooltipCount; i++) {
				String text = BLACKHOLE_OBFUSCATED_WARNING_TEXTS[i % BLACKHOLE_OBFUSCATED_WARNING_TEXTS.length];
				MainRegistry.proxy.displayTooltip(
					color + EnumChatFormatting.OBFUSCATED + text + EnumChatFormatting.RESET,
					250,
					ServerProxy.ID_BLACKHOLE_OBFUSCATED_WARNING_BASE + i
				);
			}
			if(obfuscatedTooltipCount <= 0 && isBlackholeItsHereTooltipActive(mc.theWorld)) {
				triggerBlackholeItsHereFx(mc.theWorld, getBlackholeCollapseEndTick(mc.theWorld));
				MainRegistry.proxy.displayTooltip(
					color + "=)",
					250,
					ServerProxy.ID_BLACKHOLE_ITS_HERE_WARNING
				);
			}
		}
	}

	@SubscribeEvent
	public void onPlayMusic(PlaySoundEvent17 event) {
		final ResourceLocation musicLocation = new ResourceLocation("hbm:music.game.space");
		final ResourceLocation musicLocationDmitriy = new ResourceLocation("hbm:music.game.dmitriy");
		if(event == null || event.sound == null) return;
		ResourceLocation r = event.sound.getPositionedSoundLocation();
		Minecraft mc = Minecraft.getMinecraft();
		if(mc.theWorld == null || r == null) return;
		if(shouldBlockMusicDuringBlackholeCollapse(mc.theWorld, r)) {
			stopCurrentMusic();
			event.setResult(Result.DENY);
			event.result = null;
			return;
		}
		if(!r.toString().equals("minecraft:music.game.creative") && !r.toString().equals("minecraft:music.game")) return;

		// Prevent songs playing over the top of each other
		if(mc.getSoundHandler().isSoundPlaying(currentSong)) {
			event.setResult(Result.DENY);
			event.result = null;
			return;
		}

		// Dmitriy: match space music cadence
		WorldProvider provider = mc.theWorld.provider;
		if(provider != null && provider.dimensionId == SpaceConfig.dmitriyDimension) {
			event.result = currentSong = PositionedSoundRecord.func_147673_a(musicLocationDmitriy);
			return;
		}

		// Replace the sound if we're not on Earth
		if((provider instanceof WorldProviderCelestial || provider instanceof WorldProviderOrbit) && provider.dimensionId != 0) {
			event.result = currentSong = PositionedSoundRecord.func_147673_a(musicLocation);
		}
	}

	@Spaghetti("please get this shit out of my face")
	@SubscribeEvent
	public void onPlaySound(PlaySoundEvent17 e) {

		EntityPlayer player = MainRegistry.proxy.me();
		Minecraft mc = Minecraft.getMinecraft();

		if(player != null && mc.theWorld != null) {
			int i = MathHelper.floor_double(player.posX);
			int j = MathHelper.floor_double(player.posY);
			int k = MathHelper.floor_double(player.posZ);
			Block block = mc.theWorld.getBlock(i, j, k);

			if(block == ModBlocks.vacuum) {
				e.result = null;
				return;
			}


		}

		ResourceLocation r = e.sound.getPositionedSoundLocation();

		WorldClient wc = mc.theWorld;

		//Alright, alright, I give the fuck up, you've wasted my time enough with this bullshit. You win.
		//A winner is you.
		//Conglaturations.
		//Fuck you.

		if(r.toString().equals("hbm:misc.nullChopper") && Library.getClosestChopperForSound(wc, e.sound.getXPosF(), e.sound.getYPosF(), e.sound.getZPosF(), 2) != null)
		{
			EntityHunterChopper ent = Library.getClosestChopperForSound(wc, e.sound.getXPosF(), e.sound.getYPosF(), e.sound.getZPosF(), 2);

			if(MovingSoundPlayerLoop.getSoundByPlayer(ent, EnumHbmSound.soundChopperLoop) == null) {
				MovingSoundPlayerLoop.globalSoundList.add(new MovingSoundChopper(new ResourceLocation("hbm:entity.chopperFlyingLoop"), ent, EnumHbmSound.soundChopperLoop));
				MovingSoundPlayerLoop.getSoundByPlayer(ent, EnumHbmSound.soundChopperLoop).setVolume(10.0F);
			}
		}

		if(r.toString().equals("hbm:misc.nullCrashing") && Library.getClosestChopperForSound(wc, e.sound.getXPosF(), e.sound.getYPosF(), e.sound.getZPosF(), 2) != null)
		{
			EntityHunterChopper ent = Library.getClosestChopperForSound(wc, e.sound.getXPosF(), e.sound.getYPosF(), e.sound.getZPosF(), 2);

			if(MovingSoundPlayerLoop.getSoundByPlayer(ent, EnumHbmSound.soundCrashingLoop) == null) {
				MovingSoundPlayerLoop.globalSoundList.add(new MovingSoundCrashing(new ResourceLocation("hbm:entity.chopperCrashingLoop"), ent, EnumHbmSound.soundCrashingLoop));
				MovingSoundPlayerLoop.getSoundByPlayer(ent, EnumHbmSound.soundCrashingLoop).setVolume(10.0F);
			}
		}

		if(r.toString().equals("hbm:misc.nullMine") && Library.getClosestMineForSound(wc, e.sound.getXPosF(), e.sound.getYPosF(), e.sound.getZPosF(), 2) != null)
		{
			EntityChopperMine ent = Library.getClosestMineForSound(wc, e.sound.getXPosF(), e.sound.getYPosF(), e.sound.getZPosF(), 2);

			if(MovingSoundPlayerLoop.getSoundByPlayer(ent, EnumHbmSound.soundMineLoop) == null) {
				MovingSoundPlayerLoop.globalSoundList.add(new MovingSoundChopperMine(new ResourceLocation("hbm:entity.chopperMineLoop"), ent, EnumHbmSound.soundMineLoop));
				MovingSoundPlayerLoop.getSoundByPlayer(ent, EnumHbmSound.soundMineLoop).setVolume(10.0F);
			}
		}

		for(MovingSoundPlayerLoop sounds : MovingSoundPlayerLoop.globalSoundList)
		{
			if(!sounds.init || sounds.isDonePlaying()) {
				sounds.init = true;
				sounds.setDone(false);
				mc.getSoundHandler().playSound(sounds);
			}
		}
	}

	@SubscribeEvent
	public void drawTooltip(ItemTooltipEvent event) {

		ItemStack stack = event.itemStack;
		List<String> list = event.toolTip;

		/// DAMAGE RESISTANCE ///
		DamageResistanceHandler.addInfo(stack, list);

		/// HAZMAT INFO ///
		List<HazardClass> hazInfo = ArmorRegistry.hazardClasses.get(stack.getItem());

		if(hazInfo != null) {

			if(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
				list.add(EnumChatFormatting.GOLD + I18nUtil.resolveKey("hazard.prot"));
				for(HazardClass clazz : hazInfo) {
					list.add(EnumChatFormatting.YELLOW + "  " + I18nUtil.resolveKey(clazz.lang));
				}
			} else {

				list.add(EnumChatFormatting.DARK_GRAY + "" + EnumChatFormatting.ITALIC +"Hold <" +
						EnumChatFormatting.YELLOW + "" + EnumChatFormatting.ITALIC + "LSHIFT" +
						EnumChatFormatting.DARK_GRAY + "" + EnumChatFormatting.ITALIC + "> to display protection info");
			}
		}

		/// CLADDING (LEGACY) ///
		double rad = HazmatRegistry.getResistance(stack);
		rad = ((int)(rad * 1000)) / 1000D;
		if(rad > 0) list.add(EnumChatFormatting.YELLOW + I18nUtil.resolveKey("trait.radResistance", rad));

		/// ARMOR MODS ///
		if(stack.getItem() instanceof ItemArmor && ArmorModHandler.hasMods(stack)) {

			if(!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && !(Minecraft.getMinecraft().currentScreen instanceof GUIArmorTable)) {

				list.add(EnumChatFormatting.DARK_GRAY + "" + EnumChatFormatting.ITALIC +"Hold <" +
						EnumChatFormatting.YELLOW + "" + EnumChatFormatting.ITALIC + "LSHIFT" +
						EnumChatFormatting.DARK_GRAY + "" + EnumChatFormatting.ITALIC + "> to display installed armor mods");

			} else {

				list.add(EnumChatFormatting.YELLOW + "Mods:");

				ItemStack[] mods = ArmorModHandler.pryMods(stack);

				for(int i = 0; i < 8; i++) {

					if(mods[i] != null && mods[i].getItem() instanceof ItemArmorMod) {

						((ItemArmorMod)mods[i].getItem()).addDesc(list, mods[i], stack);
					}
				}
			}
		}

		/// HAZARDS ///
		HazardSystem.addFullTooltip(stack, event.entityPlayer, list);

		if(event.showAdvancedItemTooltips && ClientConfig.ITEM_TOOLTIP_SHOW_OREDICT.get()) {
			List<String> names = ItemStackUtil.getOreDictNames(stack);

			if(names.size() > 0) {
				list.add(EnumChatFormatting.BLUE + "Ore Dict:");
				for(String s : names) {
					list.add(EnumChatFormatting.AQUA + " -" + s);
				}
			}
		}

		appendCoreCompositionTooltip(stack, list);

		///NEUTRON ACTIVATION
		float level = 0;
		float rads = HazardSystem.getHazardLevelFromStack(stack, HazardRegistry.RADIATION);
		if(rads == 0) {
			if(stack.hasTagCompound() && stack.stackTagCompound.hasKey(HazardTypeNeutron.NEUTRON_KEY)) {
				level += stack.stackTagCompound.getFloat(HazardTypeNeutron.NEUTRON_KEY);
			}

			if(level >= 1e-5) {
				list.add(EnumChatFormatting.GREEN + "[" + I18nUtil.resolveKey("trait.radioactive") + "]");
				String rads2 = "" + (Math.floor(level* 1000) / 1000);
				list.add(EnumChatFormatting.YELLOW + (rads2 + "RAD/s"));

				if(stack.stackSize > 1) {
					list.add(EnumChatFormatting.YELLOW + "Stack: " + ((Math.floor(level * 1000 * stack.stackSize) / 1000) + "RAD/s"));
				}
			}
		}

		/// CUSTOM NUKE ///
		ComparableStack comp = new ComparableStack(stack).makeSingular();

		if(ClientConfig.ITEM_TOOLTIP_SHOW_CUSTOM_NUKE.get()) {
			CustomNukeEntry entry = TileEntityNukeCustom.entries.get(comp);

			if(entry != null) {

				if(!list.isEmpty())
					list.add("");

				if(entry.entry == EnumEntryType.ADD)
					list.add(EnumChatFormatting.GOLD + "Adds " + entry.value + " to the custom nuke stage " + entry.type);

				if(entry.entry == EnumEntryType.MULT)
					list.add(EnumChatFormatting.GOLD + "Adds multiplier " + entry.value + " to the custom nuke stage " + entry.type);
			}
		}

		try {
			QuickManualAndWiki qmaw = QMAWLoader.triggers.get(comp);
			if(qmaw == null) {
				qmaw = QMAWLoader.triggers.get(new ComparableStack(comp.item, 1, OreDictionary.WILDCARD_VALUE));
			}
			if(qmaw != null) {
				list.add(EnumChatFormatting.YELLOW + I18nUtil.resolveKey("qmaw.tab", Keyboard.getKeyName(HbmKeybinds.qmaw.getKeyCode())));
				lastQMAW = qmaw;
				qmawTimestamp = Clock.get_ms();
			}
		} catch(Exception ex) {
			list.add(EnumChatFormatting.RED + "Error loading cannery: " + ex.getLocalizedMessage());
		}

		try {
			CanneryBase cannery = Jars.canneries.get(comp);
			if(cannery != null) {
				list.add(EnumChatFormatting.GREEN + I18nUtil.resolveKey("cannery.f1", Keyboard.getKeyName(Keyboard.KEY_LSHIFT) + " + " + Keyboard.getKeyName(HbmKeybinds.qmaw.getKeyCode())));
				lastCannery = comp;
				canneryTimestamp = Clock.get_ms();
			}
		} catch(Exception ex) {
			list.add(EnumChatFormatting.RED + "Error loading cannery: " + ex.getLocalizedMessage());
		}

		/*ItemStack copy = stack.copy();
		List<MaterialStack> materials = Mats.getMaterialsFromItem(copy);

		if(!materials.isEmpty()) {
			for(MaterialStack mat : materials) {
				list.add(EnumChatFormatting.DARK_PURPLE + mat.material.names[0] + ": " + Mats.formatAmount(mat.amount * stack.stackSize));
			}
		}*/

		/// ORES ///
		if(SpaceConfig.showOreLocations) {
			Block block = stack != null ? Block.getBlockFromItem(stack.getItem()) : null;
			if(block instanceof net.minecraft.block.BlockOre || block instanceof BlockRedstoneOre) {
				BlockOre ore = BlockOre.vanillaMap.get(block);
				if(ore != null) {
					ore.addInformation(stack, event.entityPlayer, list, event.showAdvancedItemTooltips);
				} else if(block == Blocks.coal_ore) {
					// we don't have any celestial coal, special case
					list.add(EnumChatFormatting.GOLD + "Can be found on:");
					list.add(EnumChatFormatting.AQUA + " - " + I18nUtil.resolveKey("body.kerbin"));
				}
			}
		}
	}

	private void appendCoreCompositionTooltip(ItemStack stack, List<String> list) {
		if(stack == null || stack.getItem() == null) return;

		List<String> oreDictNames = ItemStackUtil.getOreDictNames(stack);
		if(oreDictNames == null || oreDictNames.isEmpty()) return;

		List<String> coreTooltipLines = new ArrayList<String>();
		Set<String> seenEntries = new LinkedHashSet<String>();
		for(String oreDict : oreDictNames) {
			String categoryKey = CelestialCore.getCategoryForOreDict(oreDict);
			if(categoryKey == null || categoryKey.isEmpty()) continue;

			String categoryName = formatCoreCategoryName(categoryKey);
			double density;
			try {
				density = CelestialCore.getDensityForOreDict(oreDict);
			} catch(Exception ignored) {
				continue;
			}

			String formattedDensity = formatCoreDensity(density);
			String dedupeKey = categoryName + "|" + formattedDensity;
			if(!seenEntries.add(dedupeKey)) continue;

			coreTooltipLines.add(EnumChatFormatting.YELLOW + "Category: " + EnumChatFormatting.AQUA + categoryName);
			coreTooltipLines.add(EnumChatFormatting.YELLOW + "Density: " + EnumChatFormatting.AQUA + formattedDensity);
		}

		if(coreTooltipLines.isEmpty()) return;
		if(!list.isEmpty()) {
			list.add("");
		}
		list.add(EnumChatFormatting.GOLD + "Planet Core Composition Material");
		list.addAll(coreTooltipLines);
	}

	private String formatCoreCategoryName(String categoryName) {
		if(categoryName == null || categoryName.isEmpty()) return "Unknown";
		if(CelestialCore.CAT_NONMETAL.equals(categoryName)) return "Non-Metal";
		if(CelestialCore.CAT_SCHRABIDIC.equals(categoryName)) return "Schrabidic";
		if(CelestialCore.CAT_LIVING.equals(categoryName)) return "Living";
		if(CelestialCore.CAT_ACTINIDE.equals(categoryName)) return "Actinide";
		if(CelestialCore.CAT_CRYSTAL.equals(categoryName)) return "Crystalline";
		if(CelestialCore.CAT_LIGHT.equals(categoryName)) return "Light Metal";
		if(CelestialCore.CAT_HEAVY.equals(categoryName)) return "Heavy Metal";
		if(CelestialCore.CAT_RARE.equals(categoryName)) return "Rare Earth";
		return Character.toUpperCase(categoryName.charAt(0)) + categoryName.substring(1);
	}

	private String formatCoreDensity(double densityKgPerM3) {
		return String.format(Locale.US, "%,.0f kg/m^3", densityKgPerM3);
	}

	private static long canneryTimestamp;
	private static ComparableStack lastCannery = null;
	private static long qmawTimestamp;
	private static QuickManualAndWiki lastQMAW = null;

	private ResourceLocation ashes = new ResourceLocation(RefStrings.MODID + ":textures/misc/overlay_ash.png");

	@SideOnly(Side.CLIENT)
	//@SubscribeEvent
	public void onRenderStorm(RenderHandEvent event) {

		if(BlockAshes.ashes == 0)
			return;

		GL11.glPushMatrix();

		Minecraft mc = Minecraft.getMinecraft();

		GL11.glRotatef((float)-mc.thePlayer.rotationYaw, 0, 1, 0);
		GL11.glRotatef((float)(mc.thePlayer.rotationPitch), 1, 0, 0);

		ScaledResolution resolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);

		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDepthMask(false);
		GL11.glEnable(GL11.GL_BLEND);
		OpenGlHelper.glBlendFunc(770, 771, 1, 0);
		GL11.glEnable(GL11.GL_ALPHA_TEST);

		int w = resolution.getScaledWidth();
		int h = resolution.getScaledHeight();
		double off = Clock.get_ms() / -10000D % 10000D;
		double aw = 25;

		Tessellator tessellator = Tessellator.instance;

		//int d = mc.theWorld.getLightBrightnessForSkyBlocks(MathHelper.floor_double(mc.thePlayer.posX), MathHelper.floor_double(mc.thePlayer.posY), MathHelper.floor_double(mc.thePlayer.posZ), 0);
		int cX = currentBrightness % 65536;
		int cY = currentBrightness / 65536;
		int lX = lastBrightness % 65536;
		int lY = lastBrightness / 65536;
		float interp = (mc.theWorld.getTotalWorldTime() % 20) * 0.05F;

		if(mc.theWorld.getTotalWorldTime() == 1)
			lastBrightness = currentBrightness;

		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float)(lX + (cX - lX) * interp) / 1.0F, (float)(lY + (cY - lY) * interp) / 1.0F);

		mc.entityRenderer.enableLightmap((double)event.partialTicks);

		mc.getTextureManager().bindTexture(ashes);

		for(int i = 1; i < 3; i++) {

			GL11.glRotatef(-15, 0, 0, 1);
			GL11.glColor4f(1.0F, 1.0F, 1.0F, BlockAshes.ashes / 256F * 0.98F / i);

			tessellator.startDrawingQuads();
			tessellator.addVertexWithUV(-w * 0.25, 	h * 0.25, 	aw, 0.0D + off * i, 1.0D);
			tessellator.addVertexWithUV(w * 0.25, 	h * 0.25, 	aw, 1.0D + off * i, 1.0D);
			tessellator.addVertexWithUV(w * 0.25, 	-h * 0.25, 	aw, 1.0D + off * i, 0.0D);
			tessellator.addVertexWithUV(-w * 0.25, 	-h * 0.25, 	aw, 0.0D + off * i, 0.0D);
			tessellator.draw();
		}

		mc.entityRenderer.disableLightmap((double)event.partialTicks);

		GL11.glDepthMask(true);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

		GL11.glPopMatrix();
	}

	public static int currentBrightness = 0;
	public static int lastBrightness = 0;

	static boolean isRenderingItems = false;

	@SubscribeEvent
	public void clientTick(ClientTickEvent event) {

		Minecraft mc = Minecraft.getMinecraft();
		ArmorNo9.updateWorldHook(mc.theWorld);

		boolean supportsHighRenderDistance = FMLClientHandler.instance().hasOptifine() || Loader.isModLoaded("angelica");

		if(mc.gameSettings.renderDistanceChunks > 16 && GeneralConfig.enableRenderDistCheck && !supportsHighRenderDistance) {
			mc.gameSettings.renderDistanceChunks = 16;
			LoggingUtil.errorWithHighlight("========================== WARNING ==========================");
			LoggingUtil.errorWithHighlight("Dangerous render distance detected: Values over 16 only work on 1.8+ or with Optifine/Angelica installed!!");
			LoggingUtil.errorWithHighlight("Set '1.25_enableRenderDistCheck' in hbm.cfg to 'false' to disable this check.");
			LoggingUtil.errorWithHighlight("========================== WARNING ==========================");
			LoggingUtil.errorWithHighlight("If you got this error after removing Optifine/Angelica: Consider deleting your option files after removing mods.");
			LoggingUtil.errorWithHighlight("If you got this error after downgrading your Minecraft version: Consider using a launcher that doesn't reuse the same folders for every game instance. MultiMC for example, it's really good and it comes with a dedicated cat button. You like cats, right? Are you using the Microsoft launcher? The one launcher that turns every version switch into a tightrope act because all the old config and options files are still here because different instances all use the same folder structure instead of different folders like a competent launcher would, because some MO-RON thought that this was an acceptable way of doing things? Really? The launcher that circumcises every crashlog into indecipherable garbage, tricking oblivious people into posting that as a \"crash report\", effectively wasting everyone's time? The launcher made by the company that thought it would be HI-LA-RI-OUS to force everyone to use Microsoft accounts, effectively breaking every other launcher until they implement their terrible auth system?");
			LoggingUtil.errorWithHighlight("========================== WARNING ==========================");
		}

		if(mc.theWorld == null || mc.thePlayer == null) {
			resetDmitriyCameraEffects(mc);
			resetBlackholeItsHereFxState();
			resetBlackholeGravityLiftFxState();
			lastClientDimensionId = null;
			return;
		}

		int currentDimension = mc.theWorld.provider != null ? mc.theWorld.provider.dimensionId : 0;
		if(lastClientDimensionId == null || lastClientDimensionId.intValue() != currentDimension) {
			if(lastClientDimensionId != null) {
				resetDmitriyCameraEffects(mc);
			}
			resetBlackholeItsHereFxState();
			resetBlackholeGravityLiftFxState();
			lastClientDimensionId = currentDimension;
		} else if(currentDimension != SpaceConfig.dmitriyDimension) {
			resetDmitriyCameraEffects(mc);
		}

		SolarSystem.applyMinmusShatterState(mc.theWorld);

		if(event.phase == Phase.START && event.side == Side.CLIENT) {

			if(BlockAshes.ashes > 256) BlockAshes.ashes = 256;
			if(BlockAshes.ashes > 0) BlockAshes.ashes -= 2;
			if(BlockAshes.ashes < 0) BlockAshes.ashes = 0;

			maybeSpawnDmitriyRedRain(mc);
			maybePlayDmitriyDialogue(mc);
			maybeSpawnDmitriyGhostChat(mc);
			maybeSpawnDmitriyGhostAchievement(mc);
			updateDmitriyStarStare(mc, 0.0F);

			if(mc.theWorld.getTotalWorldTime() % 20 == 0) {
				lastBrightness = currentBrightness;
				currentBrightness = mc.theWorld.getLightBrightnessForSkyBlocks(MathHelper.floor_double(mc.thePlayer.posX), MathHelper.floor_double(mc.thePlayer.posY), MathHelper.floor_double(mc.thePlayer.posZ), 0);
			}

			if(ArmorUtil.isWearingEmptyMask(mc.thePlayer)) {
				MainRegistry.proxy.displayTooltip(EnumChatFormatting.RED + "Your mask has no filter!", ServerProxy.ID_FILTER);
			}
			updateBlackholeGravityWarningTooltip(mc);
			updateBlackholeGravityLiftFx(mc);
			
			//prune other entities' muzzle flashes
			if(mc.theWorld.getTotalWorldTime() % 30 == 0) {
				long millis = System.currentTimeMillis();
				//dead entities may have later insertion order than actively firing ones, so we be safe
				ItemRenderWeaponBase.flashMap.values().removeIf(entry -> millis - entry.longValue() >= 150);
			}
		}

		if(Keyboard.isKeyDown(HbmKeybinds.qmaw.getKeyCode()) && Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && Minecraft.getMinecraft().currentScreen != null) {

			ComparableStack comp = canneryTimestamp > Clock.get_ms() - 100 ? lastCannery : null;

			if(comp == null) {
				ItemStack stack = getMouseOverStack();
				if(stack != null) comp = new ComparableStack(stack).makeSingular();
			}

			if(comp != null) {
				CanneryBase cannery = Jars.canneries.get(comp);
				if(cannery != null) {
					Minecraft.getMinecraft().thePlayer.closeScreen();
					FMLCommonHandler.instance().showGuiScreen(new GuiWorldInAJar(cannery.createScript(), cannery.getName(), cannery.getIcon(), cannery.seeAlso()));
				}
			}
		}

		if(Keyboard.isKeyDown(HbmKeybinds.qmaw.getKeyCode()) && !Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && Minecraft.getMinecraft().currentScreen != null) {

			QuickManualAndWiki qmaw = qmawTimestamp > Clock.get_ms() - 100 ? lastQMAW : null;

			if(qmaw != null) {
				Minecraft.getMinecraft().thePlayer.closeScreen();
				FMLCommonHandler.instance().showGuiScreen(new GuiQMAW(qmaw));
			}
		}

		if(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_LMENU)) {

			ItemStack stack = getMouseOverStack();
			if(stack != null) {
				stack = stack.copy();
				stack.stackSize = 1;
				Minecraft.getMinecraft().thePlayer.closeScreen();
				FMLCommonHandler.instance().showGuiScreen(new GUIScreenPreview(stack));
			}
		}

		if(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_0) && Keyboard.isKeyDown(Keyboard.KEY_1)) {
			if (!isRenderingItems) {
				isRenderingItems = true;

				MainRegistry.logger.info("Taking a screenshot of ALL items, if you did this by mistake: fucking lmao get rekt nerd");

				List<Item> ignoredItems = Arrays.asList(
					ModItems.assembly_template,
					ModItems.crucible_template,
					ModItems.chemistry_template,
					ModItems.chemistry_icon,
					ModItems.achievement_icon,
					Items.spawn_egg,
					Item.getItemFromBlock(Blocks.mob_spawner)
				);

				List<Class<? extends Item>> collapsedClasses = Arrays.asList(
					ItemRBMKPellet.class,
					ItemDepletedFuel.class,
					ItemFluidDuct.class
				);

				String prefix = "Gun ";
				//int gunScale = 16;
				//int defaultScale = 1;
				int slotScale = 16;
				boolean ignoreNonNTM = true;
				boolean onlyGuns = true;

				List<ItemStack> stacks = new ArrayList<ItemStack>();
				for (Object reg : Item.itemRegistry) {
					Item item = (Item) reg;
					if(ignoreNonNTM && !Item.itemRegistry.getNameForObject(item).startsWith("hbm:")) continue;
					if(ignoredItems.contains(item)) continue;
					if(onlyGuns && !(item instanceof ItemGunBaseNT)) continue;
					if(collapsedClasses.contains(item.getClass())) {
						stacks.add(new ItemStack(item));
					} else {
						item.getSubItems(item, null, stacks);
					}
				}

				Minecraft.getMinecraft().thePlayer.closeScreen();
				FMLCommonHandler.instance().showGuiScreen(new GUIScreenWikiRender(stacks.toArray(new ItemStack[0]), prefix, "wiki-block-renders-256", slotScale));
			}
		} else {
			isRenderingItems = false;
		}

		EntityPlayer player = mc.thePlayer;

		if(event.phase == Phase.START) {

			float discriminator = 0.003F;
			float defaultStepSize = 0.5F;
			int newStepSize = 0;

			if(player.inventory.armorInventory[2] != null && player.inventory.armorInventory[2].getItem() instanceof ArmorFSB) {
				ArmorFSB plate = (ArmorFSB) player.inventory.armorInventory[2].getItem();
				if(ArmorFSB.hasFSBArmor(player)) newStepSize = plate.stepSize;
			}

			if(newStepSize > 0) {
				player.stepHeight = newStepSize + discriminator;
			} else {
				for(int i = 1; i < 4; i++) if(player.stepHeight == i + discriminator) player.stepHeight = defaultStepSize;
			}
		}

		if(!mc.isGamePaused() && event.phase == Phase.END) {
			for(CelestialBody body : CelestialBody.getAllBodies()) {
				HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> bodyTraits = SolarSystemWorldSavedData.getClientTraits(body.name);
				if(bodyTraits != null) {
					for(CelestialBodyTrait trait : bodyTraits.values()) {
						trait.update(true);
					}
				}
				if(body.getCore() != null) {
					CelestialBody.applyMassFromCore(body, body.getCore());
				}
			}

			CBT_War war = CelestialBody.getTrait(mc.theWorld, CBT_War.class);

			if(war != null) {
				for(int i = 0; i < war.getProjectiles().size(); i++) {
					CBT_War.Projectile projectile = war.getProjectiles().get(i);
					if(projectile != null && projectile.getTravel() >= 18 && projectile.getTravel() <= 18) {
						Minecraft.getMinecraft().thePlayer.playSound("hbm:misc.impact", 10F, 1F);
					}
				}
			}
		}

		if(event.phase == Phase.END) {

			if(ClientConfig.GUN_VISUAL_RECOIL.get()) {
				ItemGunBaseNT.offsetVertical += ItemGunBaseNT.recoilVertical;
				ItemGunBaseNT.offsetHorizontal += ItemGunBaseNT.recoilHorizontal;
				player.rotationPitch -= ItemGunBaseNT.recoilVertical;
				player.rotationYaw -= ItemGunBaseNT.recoilHorizontal;

				ItemGunBaseNT.recoilVertical *= ItemGunBaseNT.recoilDecay;
				ItemGunBaseNT.recoilHorizontal *= ItemGunBaseNT.recoilDecay;
				float dV = ItemGunBaseNT.offsetVertical * ItemGunBaseNT.recoilRebound;
				float dH = ItemGunBaseNT.offsetHorizontal * ItemGunBaseNT.recoilRebound;

				ItemGunBaseNT.offsetVertical -= dV;
				ItemGunBaseNT.offsetHorizontal -= dH;
				player.rotationPitch += dV;
				player.rotationYaw += dH;
			} else {
				ItemGunBaseNT.offsetVertical = 0;
				ItemGunBaseNT.offsetHorizontal = 0;
				ItemGunBaseNT.recoilVertical = 0;
				ItemGunBaseNT.recoilHorizontal = 0;
			}
		}

		if(event.phase == Phase.START && mc.theWorld.provider.dimensionId == SpaceConfig.orbitDimension) {
			for(Object o : mc.theWorld.loadedEntityList) {
				if(o instanceof EntityItem) {
					EntityItem item = (EntityItem) o;
					item.motionX *= 0.81D; // applies twice on server it seems? 0.9 * 0.9
					item.motionY = 0.03999999910593033D;
					item.motionZ *= 0.81D;
				}
			}
		}
	}

	public static void handleGravityEvent(Minecraft mc, float nextGravity) {
		if(mc == null || mc.theWorld == null || mc.thePlayer == null) {
			return;
		}

		if(!(mc.theWorld.provider instanceof WorldProviderCelestial)) {
			return;
		}

		WorldProviderCelestial provider = (WorldProviderCelestial) mc.theWorld.provider;
		provider.setGravityMultiplier(nextGravity);

		if(!(provider instanceof WorldProviderDmitriy)) {
			return;
		}

		WorldProviderDmitriy dmitriy = (WorldProviderDmitriy) provider;
		if(lastDmitriyGravity == null) {
			lastDmitriyGravity = dmitriy.getGravityMultiplier();
		}
		shakeTimestamp = System.currentTimeMillis();
		MainRegistry.proxy.displayTooltip(EnumChatFormatting.DARK_RED.toString() + EnumChatFormatting.BOLD + "IT LIVES...", ServerProxy.ID_GAS_HAZARD);

		float previous = lastDmitriyGravity != null ? lastDmitriyGravity : 1.0F;
		lastDmitriyGravity = nextGravity;
		boolean gravityHeavier = nextGravity > previous;
		double speed = gravityHeavier ? -0.7D : 0.7D;
		int baseX = MathHelper.floor_double(mc.thePlayer.posX);
		int baseZ = MathHelper.floor_double(mc.thePlayer.posZ);
		for(int i = 0; i < 90; i++) {
			double angle = mc.theWorld.rand.nextDouble() * Math.PI * 2.0D;
			double radius = 2.0D + mc.theWorld.rand.nextDouble() * 6.0D;
			int x = baseX + MathHelper.floor_double(Math.cos(angle) * radius);
			int z = baseZ + MathHelper.floor_double(Math.sin(angle) * radius);
			int topY = mc.theWorld.getTopSolidOrLiquidBlock(x, z);
			if(topY <= 0) {
				continue;
			}
			Block block = mc.theWorld.getBlock(x, topY - 1, z);
			if(block == null || block.isAir(mc.theWorld, x, topY - 1, z)) {
				continue;
			}
			double px = x + 0.5D + (mc.theWorld.rand.nextDouble() - 0.5D) * 0.6D;
			double pz = z + 0.5D + (mc.theWorld.rand.nextDouble() - 0.5D) * 0.6D;
			double py = gravityHeavier ? topY + 1.2D : topY + 0.1D;
			double motionX = (mc.theWorld.rand.nextDouble() - 0.5D) * 0.08D;
			double motionZ = (mc.theWorld.rand.nextDouble() - 0.5D) * 0.08D;

			if(gravityHeavier) {
				ParticleUtil.spawnDmitriyDot(mc.theWorld, px, py, pz, motionX, speed, motionZ, 0.9F, 0.1F, 0.1F);
			} else {
				int color = block.colorMultiplier(mc.theWorld, x, topY - 1, z);
				float r = ((color >> 16) & 0xFF) / 255.0F;
				float g = ((color >> 8) & 0xFF) / 255.0F;
				float b = (color & 0xFF) / 255.0F;
				ParticleUtil.spawnDmitriyDot(mc.theWorld, px, py, pz, motionX, speed, motionZ, r, g, b);
			}
		}
	}

	private void maybePlayDmitriyDialogue(Minecraft mc) {
		if(mc.theWorld == null || mc.thePlayer == null) {
			return;
		}
		if(mc.theWorld.provider.dimensionId != SpaceConfig.dmitriyDimension) {
			dmitriyDialogueCooldownTicks = -1;
			return;
		}

		if(dmitriyDialogueCooldownTicks < 0) {
			dmitriyDialogueCooldownTicks = 6000 + mc.theWorld.rand.nextInt(6000);
			return;
		}

		if(dmitriyDialogueCooldownTicks > 0) {
			dmitriyDialogueCooldownTicks--;
			return;
		}

		dmitriyDialogueCooldownTicks = 6000 + mc.theWorld.rand.nextInt(6000);
		mc.theWorld.playSoundAtEntity(mc.thePlayer, "hbm:misc.itlives_dialogue1", 1.0F, 1.0F);
	}

	private void maybeSpawnDmitriyGhostChat(Minecraft mc) {
		final int dmitriyGhostChatIntervalTicks = 60 * 20;
		final float dmitriyGhostChatChance = 0.10F;
		if(mc.theWorld == null || mc.thePlayer == null || mc.ingameGUI == null) {
			return;
		}
		if(mc.theWorld.provider.dimensionId != SpaceConfig.dmitriyDimension) {
			return;
		}

		long tick = mc.theWorld.getTotalWorldTime();
		if(tick % dmitriyGhostChatIntervalTicks != 0L) {
			return;
		}
		if(mc.theWorld.rand.nextFloat() >= dmitriyGhostChatChance) {
			return;
		}

		String sender = pickRandomDmitriyGhostSender(mc);
		if(sender == null || sender.isEmpty()) {
			return;
		}

		String message = createDmitriyGhostMessage(mc.theWorld.rand);
		if(message.isEmpty()) {
			return;
		}

		mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText("<" + sender + "> " + message));
	}

	private void maybeSpawnDmitriyGhostAchievement(Minecraft mc) {
		final int dmitriyGhostChatIntervalTicks = 60 * 20;
		final float dmitriyGhostAchievementChance = 0.30F;
		if(mc.theWorld == null || mc.thePlayer == null || mc.guiAchievement == null) {
			return;
		}
		if(mc.theWorld.provider.dimensionId != SpaceConfig.dmitriyDimension) {
			return;
		}
		String playerName = mc.thePlayer.getCommandSenderName();
		if(playerName == null || playerName.isEmpty() || hasSeenDmitriyGhostAchievement(mc.thePlayer, playerName)) {
			return;
		}

		long tick = mc.theWorld.getTotalWorldTime();
		if(tick % dmitriyGhostChatIntervalTicks != 0L) {
			return;
		}
		if(mc.theWorld.rand.nextFloat() >= dmitriyGhostAchievementChance) {
			return;
		}

		DmitriyGhostAchievement achievement = getOrCreateDmitriyGhostAchievement();
		String achievementName = createDmitriyGhostAchievementName(mc.theWorld.rand);
		achievement.setGhostText(achievementName, ": )");
		showDmitriyGhostAchievement(mc, achievement);
		if(mc.ingameGUI != null) {
			IChatComponent chatDisplayName = createDmitriyGhostAchievementChatTitle(achievementName);
			mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentTranslation("chat.type.achievement", playerName, chatDisplayName));
		}
		markDmitriyGhostAchievementSeen(mc.thePlayer, playerName);
	}

	private boolean hasSeenDmitriyGhostAchievement(EntityPlayer player, String playerName) {
		final String dmitriyGhostAchievementNbtRoot = "hbmDmitriyGhostAchievement";
		if(player == null || playerName == null || playerName.isEmpty()) {
			return true;
		}
		String seenKey = getDmitriyGhostAchievementSeenKey(player, playerName);
		ensureDmitriyGhostAchievementSeenCacheLoaded();
		if(dmitriyGhostAchievementSeenCache.contains(seenKey)) {
			return true;
		}
		NBTTagCompound persisted = player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
		NBTTagCompound ghostData = persisted.getCompoundTag(dmitriyGhostAchievementNbtRoot);
		boolean seen = ghostData.getBoolean(seenKey) || ghostData.getBoolean(getDmitriyGhostAchievementLegacySeenKey(playerName));
		if(seen && dmitriyGhostAchievementSeenCache.add(seenKey)) {
			persistDmitriyGhostAchievementSeenCache();
		}
		return seen;
	}

	private void markDmitriyGhostAchievementSeen(EntityPlayer player, String playerName) {
		final String dmitriyGhostAchievementNbtRoot = "hbmDmitriyGhostAchievement";
		if(player == null || playerName == null || playerName.isEmpty()) {
			return;
		}
		String seenKey = getDmitriyGhostAchievementSeenKey(player, playerName);
		ensureDmitriyGhostAchievementSeenCacheLoaded();
		boolean cacheChanged = dmitriyGhostAchievementSeenCache.add(seenKey);
		NBTTagCompound entityData = player.getEntityData();
		NBTTagCompound persisted = entityData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
		NBTTagCompound ghostData = persisted.getCompoundTag(dmitriyGhostAchievementNbtRoot);
		ghostData.setBoolean(seenKey, true);
		ghostData.setBoolean(getDmitriyGhostAchievementLegacySeenKey(playerName), true);
		persisted.setTag(dmitriyGhostAchievementNbtRoot, ghostData);
		entityData.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
		if(cacheChanged) {
			persistDmitriyGhostAchievementSeenCache();
		}
	}

	private String getDmitriyGhostAchievementSeenKey(EntityPlayer player, String playerName) {
		if(player != null && player.getUniqueID() != null) {
			return "seen_uuid_" + player.getUniqueID().toString().toLowerCase(Locale.ROOT);
		}
		return getDmitriyGhostAchievementLegacySeenKey(playerName);
	}

	private String getDmitriyGhostAchievementLegacySeenKey(String playerName) {
		final String dmitriyGhostAchievementNbtSeenPrefix = "seen_";
		return dmitriyGhostAchievementNbtSeenPrefix + playerName.toLowerCase(Locale.ROOT);
	}

	private void ensureDmitriyGhostAchievementSeenCacheLoaded() {
		if(dmitriyGhostAchievementSeenLoadedFromConfig) {
			return;
		}
		dmitriyGhostAchievementSeenLoadedFromConfig = true;
		String serialized = ClientConfig.DMITRIY_GHOST_ACHIEVEMENT_SEEN.get();
		if(serialized == null || serialized.isEmpty()) {
			return;
		}
		String[] keys = serialized.split(";");
		for(String key : keys) {
			if(key == null) {
				continue;
			}
			String normalized = key.trim().toLowerCase(Locale.ROOT);
			if(!normalized.isEmpty()) {
				dmitriyGhostAchievementSeenCache.add(normalized);
			}
		}
	}

	private void persistDmitriyGhostAchievementSeenCache() {
		if(!dmitriyGhostAchievementSeenLoadedFromConfig) {
			return;
		}
		List<String> sortedKeys = new ArrayList<String>(dmitriyGhostAchievementSeenCache);
		Collections.sort(sortedKeys);
		StringBuilder serialized = new StringBuilder();
		for(String key : sortedKeys) {
			if(serialized.length() > 0) {
				serialized.append(';');
			}
			serialized.append(key);
		}
		ClientConfig.DMITRIY_GHOST_ACHIEVEMENT_SEEN.set(serialized.toString());
		ClientConfig.refresh();
	}

	private DmitriyGhostAchievement getOrCreateDmitriyGhostAchievement() {
		if(dmitriyGhostAchievement == null) {
			dmitriyGhostAchievement = new DmitriyGhostAchievement();
		}
		return dmitriyGhostAchievement;
	}

	private void showDmitriyGhostAchievement(Minecraft mc, Achievement achievement) {
		if(mc == null || mc.guiAchievement == null || achievement == null) {
			return;
		}
		try {
			if(dmitriyGhostAchievementToastMethod == null) {
				dmitriyGhostAchievementToastMethod = ReflectionHelper.findMethod(
					net.minecraft.client.gui.achievement.GuiAchievement.class,
					mc.guiAchievement,
					new String[] { "displayAchievement", "func_146256_a" },
					Achievement.class
				);
				dmitriyGhostAchievementToastMethod.setAccessible(true);
			}
			dmitriyGhostAchievementToastMethod.invoke(mc.guiAchievement, achievement);
		} catch(Exception ignored) { }
	}

	private String createDmitriyGhostAchievementName(Random rand) {
		final int dmitriyGhostAchievementMinSymbols = 1;
		final int dmitriyGhostAchievementMaxSymbols = 12;
		final String dmitriyGhostChatChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()-_=+{};:'\",.?/\\\\|";
		int symbolCount = dmitriyGhostAchievementMinSymbols
			+ rand.nextInt(dmitriyGhostAchievementMaxSymbols - dmitriyGhostAchievementMinSymbols + 1);
		StringBuilder raw = new StringBuilder(symbolCount + 1);
		for(int i = 0; i < symbolCount; i++) {
			raw.append(dmitriyGhostChatChars.charAt(rand.nextInt(dmitriyGhostChatChars.length())));
		}
		if(symbolCount > 1 && rand.nextBoolean()) {
			int spacePos = 1 + rand.nextInt(symbolCount - 1);
			raw.insert(spacePos, ' ');
		}
		return raw.toString();
	}

	private IChatComponent createDmitriyGhostAchievementChatTitle(String rawTitle) {
		ChatStyle bracketStyle = new ChatStyle().setColor(EnumChatFormatting.GREEN);
		ChatStyle titleStyle = new ChatStyle().setColor(EnumChatFormatting.GREEN).setObfuscated(true)
			.setChatHoverEvent(new HoverEvent(
				HoverEvent.Action.SHOW_TEXT,
				new ChatComponentText(EnumChatFormatting.OBFUSCATED + "Achievement\n" + EnumChatFormatting.RESET + " : )")
			));
		ChatComponentText chatTitle = new ChatComponentText("");
		chatTitle.appendSibling(new ChatComponentText("[").setChatStyle(bracketStyle));
		chatTitle.appendSibling(new ChatComponentText(rawTitle).setChatStyle(titleStyle));
		chatTitle.appendSibling(new ChatComponentText("]").setChatStyle(bracketStyle));
		return chatTitle;
	}

	private String pickRandomDmitriyGhostSender(Minecraft mc) {
		if(mc.theWorld == null || mc.theWorld.playerEntities == null || mc.theWorld.playerEntities.isEmpty()) {
			return null;
		}

		List<String> names = new ArrayList<String>();
		for(Object entry : mc.theWorld.playerEntities) {
			if(!(entry instanceof EntityPlayer)) {
				continue;
			}
			String name = ((EntityPlayer)entry).getCommandSenderName();
			if(name != null && !name.isEmpty()) {
				names.add(name);
			}
		}

		if(names.isEmpty()) {
			return null;
		}

		return names.get(mc.theWorld.rand.nextInt(names.size()));
	}

	private String createDmitriyGhostMessage(Random rand) {
		final int dmitriyGhostChatMinLength = 8;
		final int dmitriyGhostChatMaxLength = 26;
		final String dmitriyGhostChatChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()-_=+[]{};:'\",.?/\\\\|";
		int length = dmitriyGhostChatMinLength + rand.nextInt(dmitriyGhostChatMaxLength - dmitriyGhostChatMinLength + 1);
		StringBuilder builder = new StringBuilder(length + 4);
		for(int i = 0; i < length; i++) {
			builder.append(dmitriyGhostChatChars.charAt(rand.nextInt(dmitriyGhostChatChars.length())));
			if(i > 2 && i < length - 2 && rand.nextInt(12) == 0) {
				builder.append(' ');
			}
		}
		return builder.toString().trim();
	}

	private void maybeSpawnDmitriyRedRain(Minecraft mc) {
		if(mc.theWorld == null || mc.thePlayer == null) {
			return;
		}
		if(mc.theWorld.provider.dimensionId != SpaceConfig.dmitriyDimension) {
			return;
		}
		float rain = mc.theWorld.getRainStrength(1.0F);
		if(rain <= 0.0F) {
			return;
		}
		int count = 6 + mc.theWorld.rand.nextInt(6);
		double radius = 6.0D;
		for(int i = 0; i < count; i++) {
			double ox = (mc.theWorld.rand.nextDouble() - 0.5D) * radius * 2.0D;
			double oz = (mc.theWorld.rand.nextDouble() - 0.5D) * radius * 2.0D;
			double oy = 6.0D + mc.theWorld.rand.nextDouble() * 4.0D;
			double x = mc.thePlayer.posX + ox;
			double y = mc.thePlayer.posY + oy;
			double z = mc.thePlayer.posZ + oz;
			double mX = (mc.theWorld.rand.nextDouble() - 0.5D) * 0.02D;
			double mZ = (mc.theWorld.rand.nextDouble() - 0.5D) * 0.02D;
			double mY = -0.6D - mc.theWorld.rand.nextDouble() * 0.3D;
			ParticleUtil.spawnDmitriyDot(mc.theWorld, x, y, z, mX, mY, mZ, 0.9F, 0.1F, 0.1F);
		}
	}

	public static float getDmitriyHeartbeatPulse(World world, float partialTicks) {
		final double dmitriyHeartbeatPeriodTicks = 200.0D;
		if(world == null || world.provider == null || world.provider.dimensionId != SpaceConfig.dmitriyDimension) {
			return 0.0F;
		}
		double ticks = world.getTotalWorldTime() + partialTicks;
		double phase = (ticks / dmitriyHeartbeatPeriodTicks) % 1.0D;
		double p1 = 1.0D - Math.abs(phase - 0.0D) / 0.03D;
		double p2 = 1.0D - Math.abs(phase - 0.02D) / 0.035D;
		double pulse = Math.max(0.0D, p1);
		pulse = Math.max(pulse, Math.max(0.0D, p2) * 1.2D);
		return (float) (pulse * pulse);
	}

	public static ItemStack getMouseOverStack() {

		Minecraft mc = Minecraft.getMinecraft();
		if(mc.currentScreen instanceof GuiContainer) {

			ScaledResolution scaledresolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
			int width = scaledresolution.getScaledWidth();
			int height = scaledresolution.getScaledHeight();
			int mouseX = Mouse.getX() * width / mc.displayWidth;
			int mouseY = height - Mouse.getY() * height / mc.displayHeight - 1;

			GuiContainer container = (GuiContainer) mc.currentScreen;

			for(Object o : container.inventorySlots.inventorySlots) {
				Slot slot = (Slot) o;

				if(slot.getHasStack()) {
					try {
						Method isMouseOverSlot = ReflectionHelper.findMethod(GuiContainer.class, container, new String[] {"func_146981_a", "isMouseOverSlot"}, Slot.class, int.class, int.class);

						if((boolean) isMouseOverSlot.invoke(container, slot, mouseX, mouseY)) {
							return slot.getStack();
						}

					} catch(Exception ex) { }
				}
			}
		}

		return null;
	}

	public static boolean renderLodeStar = false;
	public static long lastStarCheck = 0L;
	public static long lastLoadScreenReplacement = 0L;
	public static int loadingScreenReplacementRetry = 0;

	private static AudioWrapper shipHum;
	private static AudioWrapper dmitriyWind;
	private static AudioWrapper dmitriyStarStareSound;
	private static AudioWrapper dmitriyStarStareBackSound;
	private static AudioWrapper sunDecayHum;

	private static void stopDmitriyStarStareSound() {
		if(dmitriyStarStareSound != null) {
			dmitriyStarStareSound.stopSound();
			dmitriyStarStareSound = null;
		}
		if(dmitriyStarStareBackSound != null) {
			dmitriyStarStareBackSound.stopSound();
			dmitriyStarStareBackSound = null;
		}
	}

	private static void updateVoidFightsBackClientEffects(Minecraft mc) {
		EntityPlayer player = mc != null ? mc.thePlayer : null;
		World world = mc != null ? mc.theWorld : null;

		if(voidFightsBackShakeTicks > 0) {
			voidFightsBackShakeTicks--;
		}

		if(voidFightsBackLoopTicks != 0 && world != null && player != null) {
			float loopVolume = getVoidFightsBackLoopVolume(world, player);
			if(voidFightsBackLoopSound == null || !voidFightsBackLoopSound.isPlaying()) {
				voidFightsBackLoopSound = MainRegistry.proxy.getLoopedSound("hbm:misc.itlives_itstaresback", player, loopVolume, 8.0F, 1.0F, 10);
				voidFightsBackLoopSound.startSound();
			}
			voidFightsBackLoopSound.updateVolume(loopVolume);
			voidFightsBackLoopSound.keepAlive();
			if(voidFightsBackLoopTicks > 0) {
				voidFightsBackLoopTicks--;
			}
		} else if(voidFightsBackLoopSound != null) {
			if(world == null || player == null) {
				voidFightsBackLoopTicks = 0;
			}
			voidFightsBackLoopSound.stopSound();
			voidFightsBackLoopSound = null;
		}
	}

	private static float getVoidFightsBackLoopVolume(World world, EntityPlayer player) {
		final float minVolume = 0.1F;
		final float maxVolume = 0.9F;
		final double maxDistance = 64.0D;
		double minDistanceSq = Double.MAX_VALUE;

		if(world == null || player == null || world.loadedEntityList == null) {
			return minVolume;
		}

		for(Object obj : world.loadedEntityList) {
			if(!(obj instanceof EntityVoidFightsBack)) {
				continue;
			}
			EntityVoidFightsBack entity = (EntityVoidFightsBack) obj;
			if(entity == null || entity.isDead) {
				continue;
			}
			double distanceSq = entity.getDistanceSqToEntity(player);
			if(distanceSq < minDistanceSq) {
				minDistanceSq = distanceSq;
			}
		}

		if(minDistanceSq == Double.MAX_VALUE) {
			return minVolume;
		}

		double distance = Math.sqrt(minDistanceSq);
		float proximity = 1.0F - MathHelper.clamp_float((float)(distance / maxDistance), 0.0F, 1.0F);
		proximity = proximity * proximity * proximity;
		return minVolume + (maxVolume - minVolume) * proximity;
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onClientTickLast(ClientTickEvent event) {

		Minecraft mc = Minecraft.getMinecraft();
		long millis = Clock.get_ms();
		if(millis == 0) millis = System.currentTimeMillis();

		if(GeneralConfig.enableLoadScreenReplacement && loadingScreenReplacementRetry < 25 && !(mc.loadingScreen instanceof LoadingScreenRendererNT) && millis > lastLoadScreenReplacement + 5_000) {
			mc.loadingScreen = new LoadingScreenRendererNT(mc);
			lastLoadScreenReplacement = millis;
			loadingScreenReplacementRetry++; // this might not do anything, but at least it should prevent a metric fuckton of framebuffers from being created
		}

		if(event.phase == Phase.START) {
			updateVoidFightsBackClientEffects(mc);

			// I didn't see anything boss, I swears it
			World world = mc.theWorld;
			if(world == null) return;

			EntityPlayer player = mc.thePlayer;

			if(lastStarCheck + 200 < millis) {
				renderLodeStar = false;
				lastStarCheck = millis;

				if(player != null) {
					Vec3NT pos = new Vec3NT(player.posX, player.posY, player.posZ);
					Vec3NT lodestarHeading = new Vec3NT(0, 0, -1D).rotateAroundXDeg(-15).multiply(25);
					Vec3NT nextPos = new Vec3NT(pos).add(lodestarHeading.xCoord,lodestarHeading.yCoord, lodestarHeading.zCoord);
					MovingObjectPosition mop = world.func_147447_a(pos, nextPos, false, true, false);
					if(mop != null && mop.typeOfHit == mop.typeOfHit.BLOCK && world.getBlock(mop.blockX, mop.blockY, mop.blockZ) == ModBlocks.glass_polarized) {
						renderLodeStar = true;
					}
				}
			}

			if(player != null && world.provider instanceof WorldProviderOrbit && HbmLivingProps.hasGravity(player)) {
				if(shipHum == null || !shipHum.isPlaying()) {
					shipHum = MainRegistry.proxy.getLoopedSound("hbm:misc.stationhum", player, ClientConfig.AUDIO_SHIP_HUM_VOLUME.get(), 5.0F, 1.0F, 10);
					shipHum.startSound();
				}

				shipHum.updateVolume(ClientConfig.AUDIO_SHIP_HUM_VOLUME.get());
				shipHum.keepAlive();
			} else if(shipHum != null) {
				shipHum.stopSound();
				shipHum = null;
			}

			if(player != null && world.provider.dimensionId == SpaceConfig.dmitriyDimension) {
				if(dmitriyWind == null || !dmitriyWind.isPlaying()) {
					dmitriyWind = MainRegistry.proxy.getLoopedSound("hbm:ambient.dmitriy_wind", player, 0.7F, 6.0F, 1.0F, 10);
					dmitriyWind.startSound();
				}

				dmitriyWind.updateVolume(0.7F);
				dmitriyWind.keepAlive();
			} else if(dmitriyWind != null) {
				dmitriyWind.stopSound();
				dmitriyWind = null;
			}

			float stareCloseness = 0.0F;
			if(player != null && world.provider != null && world.provider.dimensionId == SpaceConfig.dmitriyDimension) {
				stareCloseness = getDmitriyStarStareCloseness();
			}
			final float dmitriyStarStareSoundMaxVolume = 0.2F;
			final float dmitriyStarStareBackSoundMaxVolume = 0.1F;
			final float dmitriyStarStareSoundMinTrigger = 0.001F;
			final float dmitriyStarStareSoundRange = 1.0F;
			final String dmitriyStarStareSoundEvent = "hbm:misc.stare";
			final String dmitriyStarStareBackSoundEvent = "hbm:misc.itlives_itstaresback";
			float stareVolume = stareCloseness * dmitriyStarStareSoundMaxVolume;
			float stareBackVolume = stareCloseness * dmitriyStarStareBackSoundMaxVolume;
			if(stareVolume > dmitriyStarStareSoundMinTrigger) {
				if(dmitriyStarStareSound == null || !dmitriyStarStareSound.isPlaying()) {
					dmitriyStarStareSound = MainRegistry.proxy.getLoopedSound(dmitriyStarStareSoundEvent, player, stareVolume, dmitriyStarStareSoundRange, 1.0F, 10);
					dmitriyStarStareSound.startSound();
				}
				if(dmitriyStarStareBackSound == null || !dmitriyStarStareBackSound.isPlaying()) {
					dmitriyStarStareBackSound = MainRegistry.proxy.getLoopedSound(dmitriyStarStareBackSoundEvent, player, stareBackVolume, dmitriyStarStareSoundRange, 1.0F, 10);
					dmitriyStarStareBackSound.startSound();
				}

				dmitriyStarStareSound.updateVolume(stareVolume);
				dmitriyStarStareSound.keepAlive();
				dmitriyStarStareBackSound.updateVolume(stareBackVolume);
				dmitriyStarStareBackSound.keepAlive();
			} else {
				stopDmitriyStarStareSound();
			}

			if(sunDecayHum != null) {
				sunDecayHum.stopSound();
				sunDecayHum = null;
			}

		}

		// ???
		/*if(event.phase == Phase.START) {

			Minecraft mc = Minecraft.getMinecraft();

			if(mc.currentScreen != null && mc.currentScreen.allowUserInput) {
				HbmPlayerProps props = HbmPlayerProps.getData(MainRegistry.proxy.me());

				for(EnumKeybind key : EnumKeybind.values()) {
					boolean last = props.getKeyPressed(key);

					if(last) {
						PacketDispatcher.wrapper.sendToServer(new KeybindPacket(key, !last));
						props.setKeyPressed(key, !last);
					}
				}
			}
		}*/
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onPlayerTick(TickEvent.PlayerTickEvent event) {
		EntityPlayer player = event.player;

		if(player.worldObj != null && player.worldObj.provider instanceof WorldProviderDmitriy && !player.capabilities.isCreativeMode) {
			if(player.capabilities.isFlying || player.capabilities.allowFlying) {
				player.capabilities.isFlying = false;
				player.capabilities.allowFlying = false;
			}
		}

		int x = MathHelper.floor_double(player.posX);
		int y = MathHelper.floor_double(player.posY);
		int z = MathHelper.floor_double(player.posZ);
		Block b = player.worldObj.getBlock(x, y, z);

		// Support climbing freestanding vines and chains using spacebar
		if (
			b.isLadder(player.worldObj, x, y, z, player) &&
			b.getCollisionBoundingBoxFromPool(player.worldObj, x, y, z) == null &&
			!player.capabilities.isFlying &&
			GameSettings.isKeyDown(Minecraft.getMinecraft().gameSettings.keyBindJump) &&
			player.motionY < 0.15
		) {
			player.motionY = 0.15;
		}
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onRenderWorldLastEvent(RenderWorldLastEvent event) {

		Clock.update();

		BlockRebar.renderRebar(Minecraft.getMinecraft().theWorld.loadedTileEntityList, event.partialTicks);
		renderBlackholeGravityLiftFx(event);

		GL11.glPushMatrix();

		EntityPlayer player = Minecraft.getMinecraft().thePlayer;

		double dx = player.prevPosX + (player.posX - player.prevPosX) * event.partialTicks;
		double dy = player.prevPosY + (player.posY - player.prevPosY) * event.partialTicks;
		double dz = player.prevPosZ + (player.posZ - player.prevPosZ) * event.partialTicks;

		int dist = 300;
		int x = 0;
		int y = 500;
		int z = 0;

		Vec3 vec = Vec3.createVectorHelper(x - dx, y - dy, z - dz);

		if(player.worldObj.provider.dimensionId == 0 && vec.lengthVector() < dist && !HTTPHandler.capsule.isEmpty()) {

			GL11.glTranslated(vec.xCoord, vec.yCoord, vec.zCoord);

			GL11.glPushMatrix();

			RenderHelper.enableStandardItemLighting();

			GL11.glRotated(80, 0, 0, 1);
			GL11.glRotated(30, 0, 1, 0);

			double sine = Math.sin(Clock.get_ms() * 0.0005) * 5;
			double sin3 = Math.sin(Clock.get_ms() * 0.0005 + Math.PI * 0.5) * 5;
			GL11.glRotated(sine, 0, 0, 1);
			GL11.glRotated(sin3, 1, 0, 0);

			GL11.glTranslated(0, -3, 0);
			OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 6500F, 30F);
			SoyuzPronter.prontCapsule();

			GL11.glRotated(Clock.get_ms() * 0.025 % 360, 0, -1, 0);

			int rand = new Random(MainRegistry.startupTime).nextInt(HTTPHandler.capsule.size());
			String msg = HTTPHandler.capsule.get(rand);

			GL11.glTranslated(0, 3.75, 0);
			GL11.glRotated(180, 1, 0, 0);

			float rot = 0F;

			// looks dumb but we'll use this technology for the cyclotron
			for(char c : msg.toCharArray()) {

				GL11.glPushMatrix();

				GL11.glRotatef(rot, 0, 1, 0);

				float width = Minecraft.getMinecraft().fontRenderer.getStringWidth(msg);
				float scale = 5 / width;

				rot -= Minecraft.getMinecraft().fontRenderer.getCharWidth(c) * scale * 50;

				GL11.glTranslated(2, 0, 0);

				GL11.glRotatef(-90, 0, 1, 0);
				GL11.glScalef(scale, scale, scale);
				GL11.glDisable(GL11.GL_CULL_FACE);
				Minecraft.getMinecraft().fontRenderer.drawString(String.valueOf(c), 0, 0, 0xff00ff);
				GL11.glEnable(GL11.GL_CULL_FACE);
				GL11.glPopMatrix();
			}

			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

			RenderHelper.disableStandardItemLighting();

			GL11.glPopMatrix();
		}

		GL11.glPopMatrix();

		boolean hudOn = HbmPlayerProps.getData(player).enableHUD;

		if(hudOn) {
			RenderOverhead.renderMarkers(event.partialTicks);
			boolean thermalSights = false;

			if(ArmorFSB.hasFSBArmor(player)) {
				ItemStack plate = player.inventory.armorInventory[2];
				ArmorFSB chestplate = (ArmorFSB) plate.getItem();

				if(chestplate.thermal) thermalSights = true;
			}

			if(player.getHeldItem() != null && player.getHeldItem().getItem() instanceof ItemGunBaseNT && ItemGunBaseNT.aimingProgress == 1) {
				ItemGunBaseNT gun = (ItemGunBaseNT) player.getHeldItem().getItem();
				for(int i = 0; i < gun.getConfigCount(); i++) if(gun.getConfig(player.getHeldItem(), i).hasThermalSights(player.getHeldItem())) thermalSights = true;
			}

			if(thermalSights) RenderOverhead.renderThermalSight(event.partialTicks);
		}

		RenderOverhead.renderActionPreview(event.partialTicks);
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onGuiScreenDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
		Minecraft mc = Minecraft.getMinecraft();
		if(mc == null || mc.theWorld == null || mc.thePlayer == null) {
			return;
		}

		float partialTicks = event.renderPartialTicks;
		if(!setupCameraTransform(mc, partialTicks)) {
			return;
		}

		RenderManager rm = RenderManager.instance;
		for(Object obj : mc.theWorld.loadedEntityList) {
			if(!(obj instanceof EntityVoidStaresBack) || obj instanceof EntityVoidFightsBack) {
				continue;
			}
			EntityVoidStaresBack voidEntity = (EntityVoidStaresBack) obj;
			rm.renderEntityWithPosYaw(
				voidEntity,
				voidEntity.posX - rm.renderPosX,
				voidEntity.posY - rm.renderPosY,
				voidEntity.posZ - rm.renderPosZ,
				voidEntity.rotationYaw,
				partialTicks
			);
		}

		mc.entityRenderer.setupOverlayRendering();
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void preRenderEventFirst(RenderLivingEvent.Pre event) {

		if(MainRegistry.proxy.isVanished(event.entity))
			event.setCanceled(true);
	}

	@SubscribeEvent
	public void preRenderEvent(RenderLivingEvent.Pre event) {
		if(event.entity instanceof EntityVoidFightsBack) {
			return;
		}

		EntityPlayer player = Minecraft.getMinecraft().thePlayer;

		if(ArmorFSB.hasFSBArmor(player) && HbmPlayerProps.getData(player).enableHUD) {
			ItemStack plate = player.inventory.armorInventory[2];
			ArmorFSB chestplate = (ArmorFSB)plate.getItem();

			if(chestplate.vats) {

				int count = (int)Math.min(event.entity.getMaxHealth(), 100);

				int bars = (int)Math.ceil(event.entity.getHealth() * count / event.entity.getMaxHealth());

				String bar = EnumChatFormatting.RED + "";

				for(int i = 0; i < count; i++) {

					if(i == bars)
						bar += EnumChatFormatting.RESET + "";

						bar += "|";
				}
				RenderOverhead.renderTag(event.entity, event.x, event.y, event.z, event.renderer, bar, chestplate.thermal);
			}
		}
	}

	public static IIcon particleBase;
	public static IIcon particleLeaf;
	public static IIcon particleSwen;
	public static IIcon particleLen;

	public static IIcon particleSplash;
	public static IIcon particleAshes;

	public static IIcon particleFlare;

	@SubscribeEvent
	public void onTextureStitch(TextureStitchEvent.Pre event) {

		if(event.map.getTextureType() == 0) {
			particleBase = event.map.registerIcon(RefStrings.MODID + ":particle/particle_base");
			particleLeaf = event.map.registerIcon(RefStrings.MODID + ":particle/dead_leaf");
			particleSwen = event.map.registerIcon(RefStrings.MODID + ":particle/particlenote2");
			particleLen = event.map.registerIcon(RefStrings.MODID + ":particle/particlenote1");

			particleSplash = event.map.registerIcon(RefStrings.MODID + ":particle/particle_splash");
			particleAshes = event.map.registerIcon(RefStrings.MODID + ":particle/particle_ashes");

			particleFlare = event.map.registerIcon(RefStrings.MODID + ":particle/yelflare");
		}
	}

	@SubscribeEvent
	public void postTextureStitch(TextureStitchEvent.Post event) {
		CTStitchReceiver.receivers.forEach(x -> x.postStitch());
	}

	@SubscribeEvent
	public void renderFrame(RenderItemInFrameEvent event) {

		if(event.item != null && event.item.getItem() == ModItems.flame_pony) {
			final ResourceLocation poster = new ResourceLocation(RefStrings.MODID + ":textures/models/misc/poster.png");
			event.setCanceled(true);

			double p = 0.0625D;
			double o = p * 2.75D;

			GL11.glDisable(GL11.GL_LIGHTING);
			Minecraft.getMinecraft().renderEngine.bindTexture(poster);
			Tessellator tess = Tessellator.instance;
			tess.startDrawingQuads();
			tess.addVertexWithUV(0.5, 0.5 + o, p * 0.5, 1, 0);
			tess.addVertexWithUV(-0.5, 0.5 + o, p * 0.5, 0, 0);
			tess.addVertexWithUV(-0.5, -0.5 + o, p * 0.5, 0, 1);
			tess.addVertexWithUV(0.5, -0.5 + o, p * 0.5, 1, 1);
			tess.draw();
			GL11.glEnable(GL11.GL_LIGHTING);
		}

		if(event.item != null && event.item.getItem() == Items.paper) {
			final ResourceLocation posterCat = new ResourceLocation(RefStrings.MODID + ":textures/models/misc/poster_cat.png");
			event.setCanceled(true);

			double p = 0.0625D;
			double o = p * 2.75D;

			GL11.glDisable(GL11.GL_LIGHTING);
			Minecraft.getMinecraft().renderEngine.bindTexture(posterCat);
			Tessellator tess = Tessellator.instance;
			tess.startDrawingQuads();
			tess.addVertexWithUV(0.5, 0.5 + o, p * 0.5, 1, 0);
			tess.addVertexWithUV(-0.5, 0.5 + o, p * 0.5, 0, 0);
			tess.addVertexWithUV(-0.5, -0.5 + o, p * 0.5, 0, 1);
			tess.addVertexWithUV(0.5, -0.5 + o, p * 0.5, 1, 1);
			tess.draw();
			GL11.glEnable(GL11.GL_LIGHTING);
		}
	}

	@SubscribeEvent
	public void worldTick(WorldTickEvent event) {

		EntityPlayer player = Minecraft.getMinecraft().thePlayer;

		if(player != null && player.ridingEntity instanceof EntityRailCarRidable && player instanceof EntityClientPlayerMP) {
			EntityRailCarRidable train = (EntityRailCarRidable) player.ridingEntity;
			EntityClientPlayerMP client = (EntityClientPlayerMP) player;

			//mojank compensation, because apparently the "this makes the render work" method also determines the fucking input
			if(!train.shouldRiderSit()) {
				client.sendQueue.addToSendQueue(new C03PacketPlayer.C05PacketPlayerLook(client.rotationYaw, client.rotationPitch, client.onGround));
				client.sendQueue.addToSendQueue(new C0CPacketInput(client.moveStrafing, client.moveForward, client.movementInput.jump, client.movementInput.sneak));
			}
		}

		if(event.phase == event.phase.END) {
			ItemCustomLore.updateSystem();
		}
	}


	@SubscribeEvent
	public void onOpenGUI(GuiOpenEvent event) {

		if(event.gui instanceof GuiMainMenu && ClientConfig.MAIN_MENU_WACKY_SPLASHES.get()) {
			GuiMainMenu main = (GuiMainMenu) event.gui;
			int rand = (int)(Math.random() * 150);

			switch(rand) {
			case 0: main.splashText = "Floppenheimer!"; break;
			case 1: main.splashText = "i should dip my balls in sulfuric acid"; break;
			case 2: main.splashText = "All answers are popbob!"; break;
			case 3: main.splashText = "None may enter The Orb!"; break;
			case 4: main.splashText = "Wacarb was here"; break;
			case 5: main.splashText = "SpongeBoy me Bob I am overdosing on ketamine agagagagaga"; break;
			case 6: main.splashText = EnumChatFormatting.RED + "I know where you live, " + System.getProperty("user.name"); break;
			case 7: main.splashText = "Nice toes, now hand them over."; break;
			case 8: main.splashText = "I smell burnt toast!"; break;
			case 9: main.splashText = "There are bugs under your skin!"; break;
			case 10: main.splashText = "Fentanyl!"; break;
			case 11: main.splashText = "Do drugs!"; break;
			case 12: main.splashText = "Imagine being scared by splash texts!"; break;
			case 13: main.splashText = "Semantic versioning? More like pedantic versioning."; break;
			}

			double d = Math.random();
			if(d < 0.1) main.splashText = "Redditors aren't people!";
		}
	}
}



