package com.hbm.render.tileentity;

import org.lwjgl.opengl.GL11;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.render.util.BeamPronter;
import com.hbm.render.util.BeamPronter.EnumBeamType;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.hbm.tileentity.machine.TileEntityCoreManipulator;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.IItemRenderer;

public class RenderCoreManipulator extends TileEntitySpecialRenderer implements IItemRendererProvider {

	private static final float BEAM_ORIGIN_Y = 4.0F;
	private static final int DYSON_BEAM_COLOR = 0xFF8800;

	@Override
	public void renderTileEntityAt(TileEntity te, double x, double y, double z, float interp) {
		GL11.glPushMatrix();
		{
			GL11.glTranslated(x + 0.5D, y, z + 0.5D);
			GL11.glEnable(GL11.GL_LIGHTING);
			GL11.glEnable(GL11.GL_CULL_FACE);
			GL11.glEnable(GL11.GL_NORMALIZE);

			int meta = te.getBlockMetadata();
			if(meta >= BlockDummyable.offset) {
				meta -= BlockDummyable.offset;
			}

			switch(meta) {
			case 2: GL11.glRotatef(180, 0F, 1F, 0F); break;
			case 4: GL11.glRotatef(90, 0F, 1F, 0F); break;
			case 5: GL11.glRotatef(270, 0F, 1F, 0F); break;
			case 3:
			default: break;
			}

			GL11.glShadeModel(GL11.GL_SMOOTH);
			bindTexture(ResourceManager.giant_core_fucker_tex);
			ResourceManager.giant_core_fucker.renderAll();
			this.renderWorkingBeam(te);
			GL11.glShadeModel(GL11.GL_FLAT);

			GL11.glDisable(GL11.GL_NORMALIZE);
		}
		GL11.glPopMatrix();
	}

	private void renderWorkingBeam(TileEntity te) {
		if(!(te instanceof TileEntityCoreManipulator) || te.getWorldObj() == null || !((TileEntityCoreManipulator) te).isWorking()) {
			return;
		}

		double beamLength = te.yCoord + BEAM_ORIGIN_Y;
		if(beamLength <= 0.0D) {
			return;
		}

		int randomSegments = Math.max(1, (int)(beamLength / 2.0D));
		int time = (int)(te.getWorldObj().getTotalWorldTime() % 1000L);

		GL11.glPushAttrib(GL11.GL_LIGHTING_BIT);
		GL11.glDisable(GL11.GL_LIGHTING);
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);

		GL11.glPushMatrix();
		GL11.glTranslated(0.0D, BEAM_ORIGIN_Y, 0.0D);
		Vec3 beamVector = Vec3.createVectorHelper(0.0D, -beamLength, 0.0D);
		BeamPronter.prontBeamwithDepth(beamVector, EnumWaveType.SPIRAL, EnumBeamType.SOLID, DYSON_BEAM_COLOR, DYSON_BEAM_COLOR, 0, 1, 0F, 2, 0.4F, 0.5F);
		BeamPronter.prontBeamwithDepth(beamVector, EnumWaveType.RANDOM, EnumBeamType.SOLID, DYSON_BEAM_COLOR, DYSON_BEAM_COLOR, time, randomSegments, 0.0625F, 2, 0.4F, 0.5F);
		GL11.glPopMatrix();

		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glPopAttrib();
	}

	@Override
	public Item getItemForRenderer() {
		return Item.getItemFromBlock(ModBlocks.machine_core_manipulator);
	}

	@Override
	public IItemRenderer getRenderer() {
		return new ItemRenderBase() {
			public void renderInventory() {
				GL11.glTranslated(0, -2.5D, 0);
				GL11.glScaled(5.5D, 5.5D, 5.5D);
			}
			public void renderCommon() {
				GL11.glScaled(0.2D, 0.2D, 0.2D);
				GL11.glShadeModel(GL11.GL_SMOOTH);
				bindTexture(ResourceManager.giant_core_fucker_tex);
				ResourceManager.giant_core_fucker.renderAll();
				GL11.glShadeModel(GL11.GL_FLAT);
			}
		};
	}
}
