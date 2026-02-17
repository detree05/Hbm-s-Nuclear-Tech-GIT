package com.hbm.render.tileentity;

import org.lwjgl.opengl.GL11;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.ItemRenderBase;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.IItemRenderer;

public class RenderStarCoreEnergyEmitter extends TileEntitySpecialRenderer implements IItemRendererProvider {

	@Override
	public void renderTileEntityAt(TileEntity te, double x, double y, double z, float interp) {

		GL11.glPushMatrix();
		GL11.glTranslated(x + 0.5, y, z + 0.5);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_CULL_FACE);

		GL11.glRotatef(90, 0F, 1F, 0F);

		int meta = te.getBlockMetadata();
		if(meta >= BlockDummyable.offset) {
			meta -= BlockDummyable.offset;
		}

		switch(meta) {
		case 0:
			GL11.glTranslated(0.0D, 0.5D, -0.5D);
			GL11.glRotatef(90, 1F, 0F, 0F); break;
		case 1:
			GL11.glTranslated(0.0D, 0.5D, 0.5D);
			GL11.glRotatef(90, -1F, 0F, 0F); break;
		case 2:
			GL11.glRotatef(90, 0F, 1F, 0F); break;
		case 4:
			GL11.glRotatef(180, 0F, 1F, 0F); break;
		case 3:
			GL11.glRotatef(270, 0F, 1F, 0F); break;
		case 5:
			GL11.glRotatef(0, 0F, 1F, 0F); break;
		}

		GL11.glTranslated(0.0D, 0.5D, 0.0D);
		GL11.glRotatef(0F, 0F, 0F, 0F);
		GL11.glTranslated(0.0D, -0.5D, 0.0D);
		bindTexture(ResourceManager.star_core_energy_emitter_tex);
		ResourceManager.star_core_energy_emitter.renderAll();

		GL11.glEnable(GL11.GL_LIGHTING);

		GL11.glPopMatrix();
	}

	@Override
	public Item getItemForRenderer() {
		return Item.getItemFromBlock(ModBlocks.star_core_energy_emitter);
	}

	@Override
	public IItemRenderer getRenderer() {
		return new ItemRenderBase() {
			public void renderInventory() {
				GL11.glTranslated(0, -2.5, 0);
				double scale = 4.5;
				GL11.glScaled(scale, scale, scale);
			}
			public void renderCommon() {
				GL11.glScaled(0.5D, 0.5D, 0.5D);
				GL11.glRotated(90, 0, 1, 0);
				GL11.glRotatef(0F, 0F, 0F, 0F);
				bindTexture(ResourceManager.star_core_energy_emitter_tex);
				ResourceManager.star_core_energy_emitter.renderAll();
			}
		};
	}
}
