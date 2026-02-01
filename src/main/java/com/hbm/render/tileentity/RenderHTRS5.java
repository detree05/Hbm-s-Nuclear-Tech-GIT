package com.hbm.render.tileentity;

import org.lwjgl.opengl.GL11;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.render.util.BeamPronter;
import com.hbm.render.util.BeamPronter.EnumBeamType;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.hbm.render.util.RenderSparks;
import com.hbm.tileentity.machine.TileEntityMachineHTRS5;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;

public class RenderHTRS5 extends TileEntitySpecialRenderer implements IItemRendererProvider {

	@Override
	public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float interp) {
		GL11.glPushMatrix();
		GL11.glTranslated(x + 0.5D, y - 1.0D, z + 0.5D);
		GL11.glEnable(GL11.GL_LIGHTING);

		switch(tile.getBlockMetadata() - BlockDummyable.offset) {
		case 3: GL11.glRotatef(270, 0F, 1F, 0F); break;
		case 5: GL11.glRotatef(0, 0F, 1F, 0F); break;
		case 2: GL11.glRotatef(90, 0F, 1F, 0F); break;
		case 4: GL11.glRotatef(180, 0F, 1F, 0F); break;
		}
		GL11.glRotatef(90, 0F, 1F, 0F);
		

		GL11.glShadeModel(GL11.GL_SMOOTH);
		bindTexture(ResourceManager.htrs5_tex);
		ResourceManager.htrs5.renderAllExcept("Magnet1", "Magnet2", "Exhaust");

		if(tile instanceof TileEntityMachineHTRS5) {
			float spin = ((TileEntityMachineHTRS5) tile).getSpinAngle(interp);

			// Rotate each magnet around its own pivot (group centroid).
			GL11.glPushMatrix();
			GL11.glTranslated(-1.410067, 1.5, 0.0);
			GL11.glRotatef(spin, 1F, 0F, 0F);
			GL11.glTranslated(1.410067, -1.5, 0.0);
			ResourceManager.htrs5.renderPart("Magnet1");
			GL11.glPopMatrix();

			GL11.glPushMatrix();
			GL11.glTranslated(1.410067, 1.5, 0.0);
			GL11.glRotatef(-spin, 1F, 0F, 0F);
			GL11.glTranslated(-1.410067, -1.5, 0.0);
			ResourceManager.htrs5.renderPart("Magnet2");
			GL11.glPopMatrix();
		} else {
			ResourceManager.htrs5.renderPart("Magnet1");
			ResourceManager.htrs5.renderPart("Magnet2");
		}

		if(tile instanceof TileEntityMachineHTRS5 && ((TileEntityMachineHTRS5) tile).isBurning()) {
			double pulseTime = tile.getWorldObj() != null ? tile.getWorldObj().getTotalWorldTime() + interp : 0D;
			float pulse = 0.5F + 0.5F * (float)Math.sin(pulseTime * 1.6D);
			float jitter = 0.008F * (float)Math.sin(pulseTime * 9.8D) + 0.006F * (float)Math.sin(pulseTime * 22.4D);
			float scaleX = 1.2F + 0.08F * pulse + jitter;
			float scaleYZ = 1.0F + 0.02F * pulse;
			float alpha = 0.28F + 0.2F * pulse + jitter * 2F;

			GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LIGHTING_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);
			GL11.glPushMatrix();
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
			OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
			GL11.glColor4f(0.7F, 0.2F, 1.0F, alpha);
			GL11.glTranslatef(-4.723555F, 1.5F, 0.0F);
			GL11.glScalef(scaleX, scaleYZ, scaleYZ);
			GL11.glTranslatef(4.723555F, -1.5F, 0.0F);
			ResourceManager.htrs5.renderPart("Exhaust");
			GL11.glPopMatrix();
			GL11.glPopAttrib();
		}

		if(tile instanceof TileEntityMachineHTRS5 && ((TileEntityMachineHTRS5) tile).hasCatalyst()) {
			TileEntityMachineHTRS5 htrs = (TileEntityMachineHTRS5) tile;
			double dmatterRatio = 0D;
			if(htrs.tanks != null && htrs.tanks.length > 0 && htrs.tanks[0].getTankType() == Fluids.DMAT) {
				double maxFill = htrs.tanks[0].getMaxFill();
				if(maxFill > 0D) {
					dmatterRatio = Math.max(0D, Math.min(1D, htrs.tanks[0].getFill() / maxFill));
				}
			}
			double floatOffset = Math.sin(((tile.getWorldObj().getTotalWorldTime() + interp) * 0.08D) % (Math.PI * 2D)) * 0.08D;
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glDisable(GL11.GL_CULL_FACE);
			GL11.glColor3f(0.02F, 0.02F, 0.02F);
			GL11.glPushMatrix();
			GL11.glTranslated(0.0D, 1.5D + floatOffset, 0.0D);
			double sphereScale = (0.25D + dmatterRatio * 0.5D) * 1.1D;
			GL11.glScaled(sphereScale, sphereScale, sphereScale);
			ResourceManager.sphere_uv.renderAll();
			if(htrs.tanks != null && htrs.tanks.length > 0 && htrs.tanks[0].getTankType() == Fluids.DMAT && htrs.tanks[0].getFill() > 0) {
				GL11.glEnable(GL11.GL_BLEND);
				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
				GL11.glDisable(GL11.GL_LIGHTING);
				GL11.glColor4f(0.35F, 0.0F, 0.5F, 0.3F);
				GL11.glPushMatrix();
				double fieldScale = 1.15D;
				GL11.glScaled(fieldScale, fieldScale, fieldScale);
				ResourceManager.sphere_uv.renderAll();
				GL11.glPopMatrix();
				GL11.glEnable(GL11.GL_LIGHTING);
				GL11.glDisable(GL11.GL_BLEND);
			}
			if((System.currentTimeMillis() / 100) % 10 == 0) {
				for(int i = 0; i < 3; i++) {
					RenderSparks.renderSpark((int) System.currentTimeMillis() / 100 + i * 10000, 0, 0, 0, 1.0F, 4, 8, 0x2b2b2b, 0x4a4a4a);
					RenderSparks.renderSpark((int) System.currentTimeMillis() / 50 + i * 10000, 0, 0, 0, 1.0F, 4, 8, 0x2b2b2b, 0x4a4a4a);
				}
			}
			GL11.glPopMatrix();
			GL11.glColor4f(1F, 1F, 1F, 1F);
			GL11.glEnable(GL11.GL_TEXTURE_2D);
		}

		if(tile instanceof TileEntityMachineHTRS5) {
			TileEntityMachineHTRS5 htrs = (TileEntityMachineHTRS5) tile;
			if(htrs.hasCatalyst() && htrs.tanks != null && htrs.tanks.length > 0 && htrs.tanks[0].getTankType() == Fluids.DMAT && htrs.tanks[0].getFill() > 0) {
				int color = 0x4b1f6f;
				double beamHalfLength = 1.9D;
				double edgeOffset = 0.25D;
				double beamY = 1.5D;
				int beamLayers = 2;
				float beamThickness = 0.08F;
				float waveSize = 0.0125F;
				float beamAlpha = 0.6F;

				GL11.glPushAttrib(GL11.GL_LIGHTING_BIT);
				GL11.glDisable(GL11.GL_LIGHTING);
				OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);

				GL11.glPushMatrix();
				GL11.glTranslated(edgeOffset, beamY, 0.0D);
				BeamPronter.prontBeamwithDepth(Vec3.createVectorHelper(beamHalfLength, 0, 0), EnumWaveType.SPIRAL, EnumBeamType.SOLID, color, color, 0, 1, 0F, beamLayers, beamThickness, beamAlpha);
				BeamPronter.prontBeamwithDepth(Vec3.createVectorHelper(beamHalfLength, 0, 0), EnumWaveType.RANDOM, EnumBeamType.SOLID, color, color, (int)(tile.getWorldObj().getTotalWorldTime() % 1000), (int)(beamHalfLength * 8), waveSize, beamLayers, beamThickness, beamAlpha);
				GL11.glPopMatrix();

				GL11.glPushMatrix();
				GL11.glTranslated(-edgeOffset - beamHalfLength, beamY, 0.0D);
				BeamPronter.prontBeamwithDepth(Vec3.createVectorHelper(beamHalfLength, 0, 0), EnumWaveType.SPIRAL, EnumBeamType.SOLID, color, color, 0, 1, 0F, beamLayers, beamThickness, beamAlpha);
				BeamPronter.prontBeamwithDepth(Vec3.createVectorHelper(beamHalfLength, 0, 0), EnumWaveType.RANDOM, EnumBeamType.SOLID, color, color, (int)(tile.getWorldObj().getTotalWorldTime() % 1000), (int)(beamHalfLength * 8), waveSize, beamLayers, beamThickness, beamAlpha);
				GL11.glPopMatrix();

				GL11.glEnable(GL11.GL_LIGHTING);
				GL11.glPopAttrib();
			}
		}

		GL11.glShadeModel(GL11.GL_FLAT);

		GL11.glPopMatrix();
	}

	@Override
	public Item getItemForRenderer() {
		return Item.getItemFromBlock(ModBlocks.machine_htrs5);
	}

	@Override
	public IItemRenderer getRenderer() {
		return new ItemRenderBase() {
			@Override
			public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
				GL11.glPushMatrix();
				GL11.glEnable(GL11.GL_CULL_FACE);
				GL11.glEnable(GL11.GL_ALPHA_TEST);

				if(type == ItemRenderType.INVENTORY) {
					net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
					GL11.glTranslated(8, 10, 0);
					GL11.glRotated(-30, 1, 0, 0);
					GL11.glRotated(45, 0, 1, 0);
					GL11.glScaled(-1, -1, -1);
					renderInventory();
				} else {
					if(type != ItemRenderType.ENTITY)
						GL11.glTranslated(0.5, 0.25, 0);
					else
						GL11.glScaled(1.5, 1.5, 1.5);

					GL11.glScaled(0.25, 0.25, 0.25);

					if(type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
						GL11.glRotated(90, 0, 1, 0);
					} else if(type != ItemRenderType.EQUIPPED) {
						GL11.glRotated(90, 0, 1, 0);
					}

					renderNonInv();
				}
				renderCommon();
				renderCommonWithStack(item);

				GL11.glPopMatrix();
			}
			public void renderInventory() {
				GL11.glTranslated(0, -1, 0);
				GL11.glRotatef(90F, 0F, 1F, 0F);
				GL11.glScaled(3.0, 3.0, 3.0);
			}
			public void renderCommon() {
				GL11.glScaled(0.5, 0.5, 0.5);
				GL11.glShadeModel(GL11.GL_SMOOTH);
				bindTexture(ResourceManager.htrs5_tex);
				ResourceManager.htrs5.renderAllExcept("Exhaust");
				GL11.glShadeModel(GL11.GL_FLAT);
			}
		};
	}
}
