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

public class RenderCoreManipulator extends TileEntitySpecialRenderer implements IItemRendererProvider {

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
			GL11.glShadeModel(GL11.GL_FLAT);

			GL11.glDisable(GL11.GL_NORMALIZE);
		}
		GL11.glPopMatrix();
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
