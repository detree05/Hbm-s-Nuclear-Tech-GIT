package com.hbm.inventory.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import com.hbm.dim.CelestialBody;
import com.hbm.dim.CelestialCore;
import com.hbm.dim.SolarSystem;
import com.hbm.dim.trait.CBT_Atmosphere;
import com.hbm.dim.trait.CBT_Atmosphere.FluidEntry;
import com.hbm.inventory.container.ContainerCoreManipulator;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.machine.TileEntityCoreManipulator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.IResource;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class GUICoreManipulator extends GuiInfoContainer {

	private enum CompositionRegion {
		NONE,
		CORE,
		MANTLE,
		STONE
	}

	private static class CompositionMetrics {
		final double coreTotal;
		final double mantleTotal;
		final double stoneTotal;
		final int coreSize;
		final int mantleSize;
		final int stoneSize;
		final boolean coreVisible;
		final boolean mantleVisible;
		final boolean stoneVisible;

		CompositionMetrics(double coreTotal, double mantleTotal, double stoneTotal, int coreSize, int mantleSize, int stoneSize, boolean coreVisible, boolean mantleVisible, boolean stoneVisible) {
			this.coreTotal = coreTotal;
			this.mantleTotal = mantleTotal;
			this.stoneTotal = stoneTotal;
			this.coreSize = coreSize;
			this.mantleSize = mantleSize;
			this.stoneSize = stoneSize;
			this.coreVisible = coreVisible;
			this.mantleVisible = mantleVisible;
			this.stoneVisible = stoneVisible;
		}
	}

	private static final ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/coremanipulator/coremanipulator.png");
	private static final ResourceLocation cloudsTexture = new ResourceLocation(RefStrings.MODID + ":textures/gui/coremanipulator/clouds.png");
	private static final ResourceLocation coreTexture = new ResourceLocation(RefStrings.MODID + ":textures/gui/coremanipulator/core.png");
	private static final ResourceLocation coreDmitriyTexture = new ResourceLocation(RefStrings.MODID + ":textures/gui/coremanipulator/core_dmitriy.png");

	private static final int PLANET_SIZE = 64;
	private static final int PLANET_X = 77;
	private static final int PLANET_Y = 33;
	private static final int PLANET_CENTER_X = 109;
	private static final int PLANET_CENTER_Y = 64;
	private static final float PLANET_RENDER_SCALE = 1.0F / 1.5F;
	private static final double BASE_SCROLL_PIXELS_PER_TICK = 0.4D;
	private static final double CORE_SPEED_MULTIPLIER = 1.01D;
	private static final double CORE_SPIN_RATIO = 0.5D;
	private static final float CORE_BLOCK_LAYER_SIZE_DIVISOR = 1.2F;
	private static final float CORE_TEXTURE_SHRINK = 2.0F;
	private static final float MANTLE_TEXTURE_SIZE_MULTIPLIER = 1.5F;
	private static final int CORE_TEXTURE_SIZE = 36;
	private static final int COMPOSITION_REFRESH_TICKS = 4;
	private static final long COMPOSITION_REFRESH_MS = 200L;
	private static final double REGION_VALUE_MIN = 0.5D;
	private static final double REGION_VALUE_BASE = 1.2D;
	private static final double REGION_VALUE_MAX = 3.0D;
	private static final float REGION_SCALE_MIN = 0.5F;
	private static final float REGION_SCALE_BASE = 1.0F;
	private static final float REGION_SCALE_MAX = 1.5F;
	private static final float CORE_MATERIAL_RECOLOR_SCALE = 6.0F;
	private static final Vec3 CORE_TINT_SCHRABIDIUM = Vec3.createVectorHelper(0.63D, 0.86D, 1.0D);
	private static final Vec3 CORE_TINT_EUPHEMIUM = Vec3.createVectorHelper(1.0D, 0.62D, 0.86D);
	private static final Vec3 MANTLE_RED_TINT = Vec3.createVectorHelper(1.0D, 0.42D, 0.42D);
	private static final Vec3 DMITRIY_MANTLE_TINT = Vec3.createVectorHelper(1.0D, 0.34D, 0.34D);
	private static final Vec3 DMITRIY_CORE_RED_TINT = Vec3.createVectorHelper(1.0D, 0.45D, 0.45D);
	private static final int DMITRIY_FACE_MIN_SIZE = 24;
	private static final float DMITRIY_FACE_OPACITY_MIN = 0.2F;
	private static final float DMITRIY_FACE_OPACITY_MAX = 0.4F;
	private static final long DMITRIY_FACE_OPACITY_PERIOD_TICKS = 240L;

	private final TileEntityCoreManipulator coreManipulator;
	private final Map<String, ResourceLocation> planetTextureCache = new HashMap<String, ResourceLocation>();
	private CompositionMetrics cachedCompositionMetrics = new CompositionMetrics(0.0D, 0.0D, 0.0D, 0, 0, 0, false, false, false);
	private long lastCompositionRefreshTick = Long.MIN_VALUE;
	private long lastCompositionRefreshMs = Long.MIN_VALUE;
	private int lastCompositionDimensionId = Integer.MIN_VALUE;

	public GUICoreManipulator(InventoryPlayer playerInv, TileEntityCoreManipulator coreManipulator) {
		super(new ContainerCoreManipulator(playerInv, coreManipulator));
		this.coreManipulator = coreManipulator;
		this.xSize = 256;
		this.ySize = 256;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		drawCompositionTooltip(mouseX, mouseY);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float interp, int mouseX, int mouseY) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		ResourceLocation planetTexture = getCurrentPlanetTexture();

		if(planetTexture != null) {
			int px = guiLeft + PLANET_X;
			int py = guiTop + PLANET_Y;
			float half = PLANET_SIZE / 2.0F;
			World world = coreManipulator != null ? coreManipulator.getWorldObj() : mc.theWorld;
			CelestialBody body = getCurrentBody(world);
			CompositionMetrics composition = getLiveCompositionMetrics(world, body);
			int baseScroll = getCurrentScrollOffset(world, interp);
			int coreScroll = getCurrentCoreScrollOffset(world, interp);

			GL11.glPushMatrix();
			GL11.glTranslatef(px + half, py + half, 0.0F);
			GL11.glScalef(PLANET_RENDER_SCALE, PLANET_RENDER_SCALE, 1.0F);
			GL11.glTranslatef(-half, -half, 0.0F);

			Vec3 bodyTint = getBodyTextureTint(body);
			GL11.glColor4f((float) bodyTint.xCoord, (float) bodyTint.yCoord, (float) bodyTint.zCoord, 1.0F);
			Minecraft.getMinecraft().getTextureManager().bindTexture(planetTexture);
			drawScrollingPlanet(baseScroll);

			float cloudAlpha = getCloudAlpha(body);
			if(cloudAlpha > 0.001F) {
				int cloudScroll = (baseScroll + getCloudScrollOffset(world, interp, body)) % PLANET_SIZE;
				Vec3 cloudTint = getBodyCloudTint(body);
				GL11.glEnable(GL11.GL_BLEND);
				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
				GL11.glColor4f((float) cloudTint.xCoord, (float) cloudTint.yCoord, (float) cloudTint.zCoord, cloudAlpha);
				Minecraft.getMinecraft().getTextureManager().bindTexture(cloudsTexture);
				drawScrollingPlanet(cloudScroll);
				GL11.glDisable(GL11.GL_BLEND);
			}

			drawCenteredBodyBlockLayer(body, composition, baseScroll);
			drawCoreLayers(coreScroll, body, composition);

			GL11.glPopMatrix();
			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		} else {
			String text = "?";
			int textWidth = fontRendererObj.getStringWidth(text);
			fontRendererObj.drawStringWithShadow(text, guiLeft + PLANET_CENTER_X - textWidth / 2, guiTop + PLANET_CENTER_Y - 4, 0xFFFFFF);
		}
	}

	private void drawScrollingPlanet(int scroll) {
		int uStart = (PLANET_SIZE - scroll) % PLANET_SIZE;
		int firstWidth = PLANET_SIZE - uStart;

		if(firstWidth > 0) {
			func_146110_a(0, 0, uStart, 0, firstWidth, PLANET_SIZE, PLANET_SIZE, PLANET_SIZE);
		}

		if(uStart > 0) {
			func_146110_a(firstWidth, 0, 0, 0, uStart, PLANET_SIZE, PLANET_SIZE, PLANET_SIZE);
		}
	}

	private void drawCoreLayers(int coreScroll, CelestialBody body, CompositionMetrics composition) {
		boolean dmitriy = isDmitriyBody(body);
		Vec3 mantleTint = dmitriy ? DMITRIY_MANTLE_TINT : MANTLE_RED_TINT;
		Vec3 coreTint = dmitriy ? applyTint(getCoreTextureRecolor(body), DMITRIY_CORE_RED_TINT, 0.45F) : getCoreTextureRecolor(body);

		if(composition.mantleVisible) {
			drawCenteredCoreTexture(coreScroll, composition.mantleSize, mantleTint);
		}
		if(composition.coreVisible) {
			drawCenteredCoreTexture(coreScroll, composition.coreSize, coreTint);
		}
		if(dmitriy) {
			int fallbackCoreSize = Math.max(1, Math.round(CORE_TEXTURE_SIZE / CORE_TEXTURE_SHRINK));
			int overlaySize = composition.coreVisible ? composition.coreSize : fallbackCoreSize;
			overlaySize = Math.max(DMITRIY_FACE_MIN_SIZE, overlaySize);
			drawCenteredStaticTexture(coreDmitriyTexture, overlaySize, Vec3.createVectorHelper(1.0D, 1.0D, 1.0D), getDmitriyFaceOpacity());
		}
	}

	private float getDmitriyFaceOpacity() {
		World world = coreManipulator != null ? coreManipulator.getWorldObj() : mc.theWorld;
		double ticks = world != null ? (double) world.getTotalWorldTime() : (double) Minecraft.getSystemTime() / 50.0D;
		double period = Math.max(1.0D, (double) DMITRIY_FACE_OPACITY_PERIOD_TICKS);
		double cycle = (ticks % period) / period;
		double pingPong = cycle <= 0.5D ? cycle * 2.0D : (1.0D - cycle) * 2.0D;
		return DMITRIY_FACE_OPACITY_MIN + (float) pingPong * (DMITRIY_FACE_OPACITY_MAX - DMITRIY_FACE_OPACITY_MIN);
	}

	private void drawCenteredBodyBlockLayer(CelestialBody body, CompositionMetrics composition, int scroll) {
		ResourceLocation blockTexture = getBodyPrimaryBlockTexture(body);
		if(blockTexture == null) {
			return;
		}

		int renderSize = composition.stoneSize;
		float center = PLANET_SIZE * 0.5F;
		float start = center - (renderSize * 0.5F);
		float stoneAlpha = 0.7F;

		GL11.glColor4f(1.0F, 1.0F, 1.0F, stoneAlpha);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		Minecraft.getMinecraft().getTextureManager().bindTexture(blockTexture);
		drawScrollingTexturedQuad(start, start, renderSize, renderSize, scroll);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private ResourceLocation getBodyPrimaryBlockTexture(CelestialBody body) {
		if(body == null || body.stoneTexture == null) {
			return null;
		}
		return body.stoneTexture;
	}

	private void drawTexturedQuad(float x, float y, float width, float height) {
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		tessellator.addVertexWithUV(x, y + height, this.zLevel, 0.0D, 1.0D);
		tessellator.addVertexWithUV(x + width, y + height, this.zLevel, 1.0D, 1.0D);
		tessellator.addVertexWithUV(x + width, y, this.zLevel, 1.0D, 0.0D);
		tessellator.addVertexWithUV(x, y, this.zLevel, 0.0D, 0.0D);
		tessellator.draw();
	}

	private void drawScrollingTexturedQuad(float x, float y, float width, float height, int scroll) {
		float uOffset = (float) ((PLANET_SIZE - (scroll % PLANET_SIZE)) % PLANET_SIZE) / (float) PLANET_SIZE;
		if(uOffset < 0.0F) {
			uOffset += 1.0F;
		}
		if(uOffset <= 0.0001F) {
			drawTexturedQuad(x, y, width, height);
			return;
		}

		float firstPartWidth = width * (1.0F - uOffset);
		float secondPartWidth = width - firstPartWidth;
		Tessellator tessellator = Tessellator.instance;

		if(firstPartWidth > 0.0F) {
			tessellator.startDrawingQuads();
			tessellator.addVertexWithUV(x, y + height, this.zLevel, uOffset, 1.0D);
			tessellator.addVertexWithUV(x + firstPartWidth, y + height, this.zLevel, 1.0D, 1.0D);
			tessellator.addVertexWithUV(x + firstPartWidth, y, this.zLevel, 1.0D, 0.0D);
			tessellator.addVertexWithUV(x, y, this.zLevel, uOffset, 0.0D);
			tessellator.draw();
		}

		if(secondPartWidth > 0.0F) {
			tessellator.startDrawingQuads();
			tessellator.addVertexWithUV(x + firstPartWidth, y + height, this.zLevel, 0.0D, 1.0D);
			tessellator.addVertexWithUV(x + width, y + height, this.zLevel, uOffset, 1.0D);
			tessellator.addVertexWithUV(x + width, y, this.zLevel, uOffset, 0.0D);
			tessellator.addVertexWithUV(x + firstPartWidth, y, this.zLevel, 0.0D, 0.0D);
			tessellator.draw();
		}
	}

	private void drawCenteredCoreTexture(int scroll, int renderSize, Vec3 tint) {
		float scale = renderSize / (float) CORE_TEXTURE_SIZE;
		float center = PLANET_SIZE * 0.5F;
		float start = center - (renderSize * 0.5F);
		Vec3 textureTint = tint != null ? tint : Vec3.createVectorHelper(1.0D, 1.0D, 1.0D);

		GL11.glPushMatrix();
		GL11.glTranslatef(start, start, 0.0F);
		GL11.glScalef(scale, scale, 1.0F);
		GL11.glColor4f((float) textureTint.xCoord, (float) textureTint.yCoord, (float) textureTint.zCoord, 1.0F);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		Minecraft.getMinecraft().getTextureManager().bindTexture(coreTexture);
		drawScrollingCore(scroll);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glPopMatrix();
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private void drawCenteredStaticTexture(ResourceLocation texture, int renderSize, Vec3 tint, float alpha) {
		if(texture == null || renderSize <= 0) {
			return;
		}

		float center = PLANET_SIZE * 0.5F;
		float start = center - (renderSize * 0.5F);
		Vec3 textureTint = tint != null ? tint : Vec3.createVectorHelper(1.0D, 1.0D, 1.0D);
		float clampedAlpha = MathHelper.clamp_float(alpha, 0.0F, 1.0F);

		GL11.glColor4f((float) textureTint.xCoord, (float) textureTint.yCoord, (float) textureTint.zCoord, clampedAlpha);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedQuad(start, start, renderSize, renderSize);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private Vec3 applyTint(Vec3 baseTint, Vec3 targetTint, float strength) {
		Vec3 base = baseTint != null ? baseTint : Vec3.createVectorHelper(1.0D, 1.0D, 1.0D);
		Vec3 target = targetTint != null ? targetTint : Vec3.createVectorHelper(1.0D, 1.0D, 1.0D);
		float mix = MathHelper.clamp_float(strength, 0.0F, 1.0F);
		float r = blendChannel((float) base.xCoord, (float) target.xCoord, mix);
		float g = blendChannel((float) base.yCoord, (float) target.yCoord, mix);
		float b = blendChannel((float) base.zCoord, (float) target.zCoord, mix);
		return Vec3.createVectorHelper(r, g, b);
	}

	private Vec3 getCoreTextureRecolor(CelestialBody body) {
		if(body == null) {
			return Vec3.createVectorHelper(1.0D, 1.0D, 1.0D);
		}

		CelestialCore core = body.getCore();
		if(core == null || core.categories == null || core.categories.isEmpty()) {
			return Vec3.createVectorHelper(1.0D, 1.0D, 1.0D);
		}

		double schrabidiumShare = getCoreMaterialShare(core, "schrabidium");
		double euphemiumShare = getCoreMaterialShare(core, "euphemium");

		float schrabidiumStrength = MathHelper.clamp_float((float) (schrabidiumShare * CORE_MATERIAL_RECOLOR_SCALE), 0.0F, 1.0F);
		float euphemiumStrength = MathHelper.clamp_float((float) (euphemiumShare * CORE_MATERIAL_RECOLOR_SCALE), 0.0F, 1.0F);
		float totalStrength = MathHelper.clamp_float(schrabidiumStrength + euphemiumStrength, 0.0F, 1.0F);

		if(totalStrength <= 0.0F) {
			return Vec3.createVectorHelper(1.0D, 1.0D, 1.0D);
		}

		float sum = schrabidiumStrength + euphemiumStrength;
		float schrabidiumMix = sum > 0.0F ? schrabidiumStrength / sum : 0.0F;
		float euphemiumMix = sum > 0.0F ? euphemiumStrength / sum : 0.0F;

		float targetR = (float) (CORE_TINT_SCHRABIDIUM.xCoord * schrabidiumMix + CORE_TINT_EUPHEMIUM.xCoord * euphemiumMix);
		float targetG = (float) (CORE_TINT_SCHRABIDIUM.yCoord * schrabidiumMix + CORE_TINT_EUPHEMIUM.yCoord * euphemiumMix);
		float targetB = (float) (CORE_TINT_SCHRABIDIUM.zCoord * schrabidiumMix + CORE_TINT_EUPHEMIUM.zCoord * euphemiumMix);

		float r = blendChannel(1.0F, targetR, totalStrength);
		float g = blendChannel(1.0F, targetG, totalStrength);
		float b = blendChannel(1.0F, targetB, totalStrength);

		return Vec3.createVectorHelper(r, g, b);
	}

	private double getCoreMaterialShare(CelestialCore core, String materialToken) {
		if(core == null || materialToken == null || materialToken.isEmpty()) {
			return 0.0D;
		}

		String normalizedNeedle = normalizeMaterialToken(materialToken);
		if(normalizedNeedle.isEmpty()) {
			return 0.0D;
		}

		double totalWeighted = 0.0D;
		double matchedWeighted = 0.0D;

		for(CelestialCore.CoreCategory category : core.categories) {
			if(category == null || category.entries == null) continue;
			double categoryWeight = Math.max(0.0D, category.weight);
			for(CelestialCore.CoreEntry entry : category.entries) {
				if(entry == null || entry.oreDict == null) continue;
				double weightedValue = categoryWeight * Math.max(0.0D, (double) entry.value);
				if(weightedValue <= 0.0D) continue;

				totalWeighted += weightedValue;

				String normalizedEntryToken = normalizeMaterialToken(extractMaterialToken(entry.oreDict));
				if(normalizedEntryToken.contains(normalizedNeedle)) {
					matchedWeighted += weightedValue;
				}
			}
		}

		if(totalWeighted <= 0.0D) {
			return 0.0D;
		}

		return matchedWeighted / totalWeighted;
	}

	private String extractMaterialToken(String oreDict) {
		if(oreDict == null || oreDict.isEmpty()) return "";
		for(int i = 0; i < oreDict.length(); i++) {
			if(Character.isUpperCase(oreDict.charAt(i))) {
				return oreDict.substring(i);
			}
		}
		return oreDict;
	}

	private String normalizeMaterialToken(String token) {
		if(token == null || token.isEmpty()) {
			return "";
		}
		StringBuilder normalized = new StringBuilder(token.length());
		for(int i = 0; i < token.length(); i++) {
			char c = Character.toLowerCase(token.charAt(i));
			if((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
				normalized.append(c);
			}
		}
		return normalized.toString();
	}

	private float blendChannel(float base, float target, float strength) {
		return MathHelper.clamp_float(base + (target - base) * strength, 0.0F, 1.0F);
	}

	private CompositionMetrics getCompositionMetrics(CelestialCore core) {
		double coreTotal = getCategoryGroupTotal(core, CelestialCore.CAT_HEAVY, CelestialCore.CAT_SCHRABIDIC, CelestialCore.CAT_LIGHT, CelestialCore.CAT_LIVING);
		double mantleTotal = getCategoryGroupTotal(core, CelestialCore.CAT_RARE, CelestialCore.CAT_ACTINIDE);
		double stoneTotal = getCategoryGroupTotal(core, CelestialCore.CAT_NONMETAL, CelestialCore.CAT_CRYSTAL);

		int baseCoreSize = Math.max(1, Math.round(CORE_TEXTURE_SIZE / CORE_TEXTURE_SHRINK));
		int baseMantleSize = Math.max(1, Math.round(baseCoreSize * MANTLE_TEXTURE_SIZE_MULTIPLIER));
		int baseStoneSize = Math.max(1, Math.round(PLANET_SIZE / CORE_BLOCK_LAYER_SIZE_DIVISOR));

		boolean coreVisible = coreTotal > 0.0D;
		boolean mantleVisible = mantleTotal > 0.0D;
		boolean stoneVisible = true;

		float coreScale = getRegionSizeScale(coreTotal);
		int coreSize = coreVisible ? Math.max(1, Math.round(baseCoreSize * coreScale)) : 0;
		int mantleSize = mantleVisible ? Math.max(1, Math.round(baseMantleSize * coreScale)) : 0;
		int stoneSize = baseStoneSize;

		return new CompositionMetrics(coreTotal, mantleTotal, stoneTotal, coreSize, mantleSize, stoneSize, coreVisible, mantleVisible, stoneVisible);
	}

	private float getRegionSizeScale(double totalValue) {
		if(totalValue <= REGION_VALUE_MIN) {
			return REGION_SCALE_MIN;
		}
		if(totalValue >= REGION_VALUE_MAX) {
			return REGION_SCALE_MAX;
		}
		if(totalValue <= REGION_VALUE_BASE) {
			double t = (totalValue - REGION_VALUE_MIN) / (REGION_VALUE_BASE - REGION_VALUE_MIN);
			return (float) (REGION_SCALE_MIN + t * (REGION_SCALE_BASE - REGION_SCALE_MIN));
		}

		double t = (totalValue - REGION_VALUE_BASE) / (REGION_VALUE_MAX - REGION_VALUE_BASE);
		return (float) (REGION_SCALE_BASE + t * (REGION_SCALE_MAX - REGION_SCALE_BASE));
	}

	private double getCategoryGroupTotal(CelestialCore core, String... categoryKeys) {
		if(core == null || core.categories == null || core.categories.isEmpty() || categoryKeys == null || categoryKeys.length == 0) {
			return 0.0D;
		}

		double total = 0.0D;
		for(String categoryKey : categoryKeys) {
			total += getCoreCategoryTotalValue(core, categoryKey);
		}
		return total;
	}

	private void drawCompositionTooltip(int mouseX, int mouseY) {
		World world = coreManipulator != null ? coreManipulator.getWorldObj() : mc.theWorld;
		CelestialBody body = getCurrentBody(world);
		if(body == null || body.getCore() == null) {
			return;
		}
		CompositionMetrics composition = getLiveCompositionMetrics(world, body);

		CompositionRegion hoveredRegion = getHoveredCompositionRegion(mouseX, mouseY, body, composition);
		if(hoveredRegion == CompositionRegion.NONE) {
			return;
		}

		List<String> lines = new ArrayList<String>();
		CelestialCore core = body.getCore();

		switch(hoveredRegion) {
		case CORE:
			lines.add(EnumChatFormatting.YELLOW + "CORE");
			addCategoryPercentLine(lines, core, "Heavy Metal", CelestialCore.CAT_HEAVY);
			addCategoryPercentLine(lines, core, "Schrabidic", CelestialCore.CAT_SCHRABIDIC);
			addCategoryPercentLine(lines, core, "Light Metal", CelestialCore.CAT_LIGHT);
			addCategoryPercentLine(lines, core, "Living", CelestialCore.CAT_LIVING);
			break;
		case MANTLE:
			lines.add(EnumChatFormatting.GOLD + "MANTLE");
			addCategoryPercentLine(lines, core, "Rare Earth", CelestialCore.CAT_RARE);
			addCategoryPercentLine(lines, core, "Actinide", CelestialCore.CAT_ACTINIDE);
			break;
		case STONE:
			lines.add(EnumChatFormatting.GRAY + "STONE");
			addCategoryPercentLine(lines, core, "Non-Metal", CelestialCore.CAT_NONMETAL);
			addCategoryPercentLine(lines, core, "Crystalline", CelestialCore.CAT_CRYSTAL);
			if(lines.size() <= 1) {
				lines.add("No ores found.");
			}
			break;
		default:
			return;
		}

		drawInfo(lines.toArray(new String[lines.size()]), mouseX, mouseY);
	}

	private CompositionRegion getHoveredCompositionRegion(int mouseX, int mouseY, CelestialBody body, CompositionMetrics composition) {
		if(body == null || getCurrentPlanetTexture() == null) {
			return CompositionRegion.NONE;
		}

		float half = PLANET_SIZE * 0.5F;
		float centerX = guiLeft + PLANET_X + half;
		float centerY = guiTop + PLANET_Y + half;
		float localX = (mouseX - centerX) / PLANET_RENDER_SCALE + half;
		float localY = (mouseY - centerY) / PLANET_RENDER_SCALE + half;

		if(localX < 0.0F || localY < 0.0F || localX >= PLANET_SIZE || localY >= PLANET_SIZE) {
			return CompositionRegion.NONE;
		}

		if(composition.coreVisible && isInsideCenteredSquare(localX, localY, composition.coreSize)) {
			return CompositionRegion.CORE;
		}

		if(composition.mantleVisible && isInsideCenteredSquare(localX, localY, composition.mantleSize)) {
			return CompositionRegion.MANTLE;
		}

		if(composition.stoneVisible && isInsideCenteredSquare(localX, localY, composition.stoneSize)) {
			return CompositionRegion.STONE;
		}

		return CompositionRegion.NONE;
	}

	private boolean isInsideCenteredSquare(float x, float y, int size) {
		float start = PLANET_SIZE * 0.5F - size * 0.5F;
		float end = start + size;
		return x >= start && x < end && y >= start && y < end;
	}

	private boolean isDmitriyBody(CelestialBody body) {
		return body != null && body.name != null && "dmitriy".equalsIgnoreCase(body.name);
	}

	private void addCategoryPercentLine(List<String> lines, CelestialCore core, String label, String categoryKey) {
		double share = getCoreCategoryShare(core, categoryKey);
		if(share <= 0.0005D) {
			return;
		}
		lines.add(label + ": " + String.format(Locale.ROOT, "%.1f%%", share * 100.0D));
	}

	private CompositionMetrics getLiveCompositionMetrics(World world, CelestialBody body) {
		CelestialCore core = body != null ? body.getCore() : null;
		if(core == null) {
			cachedCompositionMetrics = new CompositionMetrics(0.0D, 0.0D, 0.0D, 0, 0, 0, false, false, false);
			return cachedCompositionMetrics;
		}

		boolean shouldRefresh = false;
		int dimensionId = world != null && world.provider != null ? world.provider.dimensionId : Integer.MIN_VALUE;
		if(dimensionId != lastCompositionDimensionId) {
			shouldRefresh = true;
		}

		if(world != null) {
			long tick = world.getTotalWorldTime();
			if(lastCompositionRefreshTick == Long.MIN_VALUE || tick - lastCompositionRefreshTick >= COMPOSITION_REFRESH_TICKS) {
				shouldRefresh = true;
			}
			if(shouldRefresh) {
				lastCompositionRefreshTick = tick;
				lastCompositionRefreshMs = Minecraft.getSystemTime();
			}
		} else {
			long nowMs = Minecraft.getSystemTime();
			if(lastCompositionRefreshMs == Long.MIN_VALUE || nowMs - lastCompositionRefreshMs >= COMPOSITION_REFRESH_MS) {
				shouldRefresh = true;
				lastCompositionRefreshMs = nowMs;
			}
		}

		if(shouldRefresh) {
			cachedCompositionMetrics = getCompositionMetrics(core);
			lastCompositionDimensionId = dimensionId;
		}

		return cachedCompositionMetrics;
	}

	private double getCoreCategoryShare(CelestialCore core, String categoryKey) {
		if(core == null || core.categories == null || core.categories.isEmpty()) {
			return 0.0D;
		}

		double totalWeighted = getTotalWeightedCoreValue(core);
		double matchedWeighted = getCoreCategoryTotalValue(core, categoryKey);

		if(totalWeighted <= 0.0D) {
			return 0.0D;
		}

		return MathHelper.clamp_double(matchedWeighted / totalWeighted, 0.0D, 1.0D);
	}

	private double getTotalWeightedCoreValue(CelestialCore core) {
		if(core == null || core.categories == null || core.categories.isEmpty()) {
			return 0.0D;
		}

		double totalWeighted = 0.0D;
		for(CelestialCore.CoreCategory category : core.categories) {
			if(category == null || category.entries == null) continue;
			double categoryWeight = Math.max(0.0D, category.weight);
			for(CelestialCore.CoreEntry entry : category.entries) {
				if(entry == null) continue;
				double weightedValue = categoryWeight * Math.max(0.0D, (double) entry.value);
				if(weightedValue > 0.0D) {
					totalWeighted += weightedValue;
				}
			}
		}
		return totalWeighted;
	}

	private double getCoreCategoryTotalValue(CelestialCore core, String categoryKey) {
		if(core == null || core.categories == null || core.categories.isEmpty()) {
			return 0.0D;
		}

		String normalizedNeedle = normalizeCategoryKey(categoryKey);
		double matchedWeighted = 0.0D;

		for(CelestialCore.CoreCategory category : core.categories) {
			if(category == null || category.entries == null) continue;
			if(!normalizeCategoryKey(category.name).equals(normalizedNeedle)) continue;
			double categoryWeight = Math.max(0.0D, category.weight);
			for(CelestialCore.CoreEntry entry : category.entries) {
				if(entry == null) continue;
				double weightedValue = categoryWeight * Math.max(0.0D, (double) entry.value);
				if(weightedValue > 0.0D) {
					matchedWeighted += weightedValue;
				}
			}
		}

		return matchedWeighted;
	}

	private String normalizeCategoryKey(String key) {
		if(key == null) return "";
		String lower = key.toLowerCase(Locale.ROOT);
		StringBuilder normalized = new StringBuilder(lower.length());
		for(int i = 0; i < lower.length(); i++) {
			char c = lower.charAt(i);
			if((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
				normalized.append(c);
			}
		}
		return normalized.toString();
	}

	private void drawScrollingCore(int scroll) {
		int uStart = (CORE_TEXTURE_SIZE - scroll) % CORE_TEXTURE_SIZE;
		int firstWidth = CORE_TEXTURE_SIZE - uStart;

		if(firstWidth > 0) {
			func_146110_a(0, 0, uStart, 0, firstWidth, CORE_TEXTURE_SIZE, CORE_TEXTURE_SIZE, CORE_TEXTURE_SIZE);
		}

		if(uStart > 0) {
			func_146110_a(firstWidth, 0, 0, 0, uStart, CORE_TEXTURE_SIZE, CORE_TEXTURE_SIZE, CORE_TEXTURE_SIZE);
		}
	}

	private CelestialBody getCurrentBody(World world) {
		if(world == null || world.provider == null) return null;
		return CelestialBody.getBodyOrNull(world.provider.dimensionId);
	}

	private int getCurrentScrollOffset(World world, float partialTicks) {
		double rotationScale = getCurrentCoreRotationScale(world);
		double ticks = world != null ? (double) world.getTotalWorldTime() + (double) partialTicks : (double) Minecraft.getSystemTime() / 50.0D;
		double traveled = ticks * BASE_SCROLL_PIXELS_PER_TICK * rotationScale * CORE_SPEED_MULTIPLIER;
		int scroll = (int) Math.floor(traveled) % PLANET_SIZE;
		if(scroll < 0) scroll += PLANET_SIZE;
		return scroll;
	}

	private int getCurrentCoreScrollOffset(World world, float partialTicks) {
		double rotationScale = getCurrentCoreRotationScale(world);
		double ticks = world != null ? (double) world.getTotalWorldTime() + (double) partialTicks : (double) Minecraft.getSystemTime() / 50.0D;
		double planetCycles = (ticks * BASE_SCROLL_PIXELS_PER_TICK * rotationScale * CORE_SPEED_MULTIPLIER) / PLANET_SIZE;
		double coreCycles = planetCycles * CORE_SPIN_RATIO;
		double wrappedCoreCycle = coreCycles - Math.floor(coreCycles);

		int scroll = (int) Math.floor(wrappedCoreCycle * CORE_TEXTURE_SIZE) % CORE_TEXTURE_SIZE;
		if(scroll < 0) scroll += CORE_TEXTURE_SIZE;
		return scroll;
	}

	private int getCloudScrollOffset(World world, float partialTicks, CelestialBody body) {
		if(world == null || body == null) return 0;

		double time = world.getWorldTime() + partialTicks;
		double bodyPeriod = body.getRotationalPeriod();
		double driftPeriod = bodyPeriod > 1.0D ? MathHelper.clamp_double(bodyPeriod * 0.35D, 4000.0D, 360000.0D) : 24000.0D;
		double phaseOffset = (Math.abs(body.name.hashCode()) % 2048) / 2048.0D;
		double uvOffset = (time / driftPeriod + phaseOffset) % 1.0D;

		int scroll = (int) Math.floor(uvOffset * PLANET_SIZE) % PLANET_SIZE;
		if(scroll < 0) scroll += PLANET_SIZE;
		return scroll;
	}

	private double getCurrentCoreRotationScale(World world) {
		if(world != null && world.provider != null) {
			CelestialBody body = getCurrentBody(world);
			if(body != null) {
				CelestialCore core = body.getCore();
				if(core != null) {
					double scale = core.rotationalSpeedScale;
					if(!Double.isNaN(scale) && !Double.isInfinite(scale) && scale > 0.0D) {
						return scale;
					}
				}
			}
		}
		return 1.0D;
	}

	private Vec3 getBodyTextureTint(CelestialBody body) {
		if(body == null) {
			return Vec3.createVectorHelper(1.0D, 1.0D, 1.0D);
		}

		Vec3 atmosphereColor = getBodyAtmosphereColor(body);

		float tintStrength = 0.0F;
		CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);
		if(atmosphere != null && !atmosphere.fluids.isEmpty()) {
			float pressure = MathHelper.clamp_float((float) atmosphere.getPressure(), 0.0F, 2.0F);
			tintStrength = MathHelper.clamp_float(0.25F + pressure * 0.2F, 0.25F, 0.65F);
		} else if(body.gas != null) {
			tintStrength = 0.65F;
		}

		float r = MathHelper.clamp_float(1.0F + ((float) atmosphereColor.xCoord - 1.0F) * tintStrength, 0.0F, 1.0F);
		float g = MathHelper.clamp_float(1.0F + ((float) atmosphereColor.yCoord - 1.0F) * tintStrength, 0.0F, 1.0F);
		float b = MathHelper.clamp_float(1.0F + ((float) atmosphereColor.zCoord - 1.0F) * tintStrength, 0.0F, 1.0F);
		return Vec3.createVectorHelper(r, g, b);
	}

	private Vec3 getBodyCloudTint(CelestialBody body) {
		if(body == null) {
			return Vec3.createVectorHelper(1.0D, 1.0D, 1.0D);
		}

		if(body.type == SolarSystem.Body.EVE) {
			return Vec3.createVectorHelper(0.85D, 0.55D, 1.0D);
		}

		Vec3 atmosphereColor = getBodyAtmosphereColor(body, true);
		double r = MathHelper.clamp_double((1.0D + atmosphereColor.xCoord) * 0.5D, 0.0D, 1.0D);
		double g = MathHelper.clamp_double((1.0D + atmosphereColor.yCoord) * 0.5D, 0.0D, 1.0D);
		double b = MathHelper.clamp_double((1.0D + atmosphereColor.zCoord) * 0.5D, 0.0D, 1.0D);
		return Vec3.createVectorHelper(r, g, b);
	}

	private Vec3 getBodyAtmosphereColor(CelestialBody body) {
		return getBodyAtmosphereColor(body, false);
	}

	private Vec3 getBodyAtmosphereColor(CelestialBody body, boolean ignoreAirFluids) {
		if(body == null) {
			return Vec3.createVectorHelper(1.0D, 1.0D, 1.0D);
		}

		CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);
		if(atmosphere != null && !atmosphere.fluids.isEmpty()) {
			double totalPressure = 0.0D;
			double r = 0.0D;
			double g = 0.0D;
			double b = 0.0D;

			for(FluidEntry entry : atmosphere.fluids) {
				if(entry == null || entry.fluid == null || entry.pressure <= 0.0D) {
					continue;
				}
				if(ignoreAirFluids && isAirAtmosphereFluid(entry.fluid)) {
					continue;
				}

				Vec3 fluidColor = getAtmosphereFluidColor(entry.fluid);
				r += fluidColor.xCoord * entry.pressure;
				g += fluidColor.yCoord * entry.pressure;
				b += fluidColor.zCoord * entry.pressure;
				totalPressure += entry.pressure;
			}

			if(totalPressure > 0.0D) {
				return Vec3.createVectorHelper(
					MathHelper.clamp_double(r / totalPressure, 0.0D, 1.0D),
					MathHelper.clamp_double(g / totalPressure, 0.0D, 1.0D),
					MathHelper.clamp_double(b / totalPressure, 0.0D, 1.0D)
				);
			}
		}

		if(body.gas != null && !(ignoreAirFluids && isAirAtmosphereFluid(body.gas))) {
			return getAtmosphereFluidColor(body.gas);
		}

		return Vec3.createVectorHelper(1.0D, 1.0D, 1.0D);
	}

	private float getCloudAlpha(CelestialBody body) {
		if(body == null) {
			return 0.0F;
		}

		CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);
		if(atmosphere == null) {
			return 0.0F;
		}

		float pressure = (float) atmosphere.getPressure();
		if(pressure <= 0.5F) {
			return 0.0F;
		}

		return MathHelper.clamp_float((pressure - 0.5F) * 0.5F, 0.3F, 0.8F);
	}

	private boolean isAirAtmosphereFluid(FluidType fluid) {
		if(fluid == null) {
			return false;
		}

		String fluidName = fluid.getName();
		return fluid == Fluids.AIR || (fluidName != null && fluidName.toUpperCase().contains("AIR"));
	}

	private Vec3 getAtmosphereFluidColor(FluidType fluid) {
		if(fluid == null) {
			return Vec3.createVectorHelper(1.0D, 1.0D, 1.0D);
		}

		if(fluid == Fluids.EVEAIR) {
			return Vec3.createVectorHelper(53F / 255F, 32F / 255F, 74F / 255F);
		}
		if(fluid == Fluids.DUNAAIR) {
			return Vec3.createVectorHelper(198F / 255F, 96F / 255F, 64F / 255F);
		}
		if(fluid == Fluids.CARBONDIOXIDE) {
			return Vec3.createVectorHelper(188F / 255F, 192F / 255F, 198F / 255F);
		}
		if(fluid == Fluids.EARTHAIR || fluid == Fluids.OXYGEN || fluid == Fluids.NITROGEN) {
			return Vec3.createVectorHelper(0.7529412F, 0.84705883F, 1.0F);
		}

		return getColorFromHex(fluid.getColor());
	}

	private Vec3 getColorFromHex(int hexColor) {
		float red = ((hexColor >> 16) & 0xFF) / 255.0F;
		float green = ((hexColor >> 8) & 0xFF) / 255.0F;
		float blue = (hexColor & 0xFF) / 255.0F;
		return Vec3.createVectorHelper(red, green, blue);
	}

	private ResourceLocation getCurrentPlanetTexture() {
		String key = getCurrentDimensionTextureKey();
		if(key == null || key.isEmpty()) return null;

		if(planetTextureCache.containsKey(key)) return planetTextureCache.get(key);

		ResourceLocation texture = new ResourceLocation(RefStrings.MODID, "textures/gui/coremanipulator/" + key + ".png");
		IResource resource = null;

		try {
			resource = mc.getResourceManager().getResource(texture);
			planetTextureCache.put(key, texture);
			return texture;
		} catch(IOException ex) {
			planetTextureCache.put(key, null);
			return null;
		} finally {
			if(resource != null) {
				try {
					resource.getInputStream().close();
				} catch(IOException ignored) { }
			}
		}
	}

	private String getCurrentDimensionTextureKey() {
		World world = coreManipulator != null ? coreManipulator.getWorldObj() : null;
		if(world == null) world = mc.theWorld;
		if(world == null || world.provider == null) return null;

		CelestialBody body = CelestialBody.getBodyOrNull(world.provider.dimensionId);
		if(body != null && body.name != null && !body.name.isEmpty()) {
			return normalizeTextureKey(body.name);
		}

		String dimensionName = world.provider.getDimensionName();
		if(dimensionName != null && !dimensionName.isEmpty()) {
			return normalizeTextureKey(dimensionName);
		}

		return "dim" + world.provider.dimensionId;
	}

	private String normalizeTextureKey(String key) {
		String lower = key.toLowerCase(Locale.ROOT);
		StringBuilder normalized = new StringBuilder(lower.length());

		for(int i = 0; i < lower.length(); i++) {
			char c = lower.charAt(i);
			if((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
				normalized.append(c);
			} else {
				normalized.append('_');
			}
		}

		return normalized.toString();
	}
}
