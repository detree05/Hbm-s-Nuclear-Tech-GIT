package com.hbm.render.util;

import java.util.Random;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class BeamPronter {
	
	public static Random rand = new Random();

	public static enum EnumWaveType {
		RANDOM, SPIRAL
	}

	public static enum EnumBeamType {
		SOLID, LINE, CYLINDER
	}
	
	private static boolean depthMask = false;

	public static void prontBeamwithDepth(Vec3 skeleton, EnumWaveType wave, EnumBeamType beam, int outerColor, int innerColor, int start, int segments, float size, int layers, float thickness) {
		prontBeamwithDepth(skeleton, wave, beam, outerColor, innerColor, start, segments, size, layers, thickness, 1F);
	}

	public static void prontBeamwithDepth(Vec3 skeleton, EnumWaveType wave, EnumBeamType beam, int outerColor, int innerColor, int start, int segments, float size, int layers, float thickness, float alpha) {
		depthMask = true;
		prontBeam(skeleton, wave, beam, outerColor, innerColor, start, segments, size, layers, thickness, alpha, -1.0F);
		depthMask = false;
	}

	public static void prontBeamwithDepth(Vec3 skeleton, EnumWaveType wave, EnumBeamType beam, int outerColor, int innerColor, int start, int segments, float size, int layers, float thickness, float alpha, float fadeStartRatio) {
		depthMask = true;
		prontBeam(skeleton, wave, beam, outerColor, innerColor, start, segments, size, layers, thickness, alpha, fadeStartRatio);
		depthMask = false;
	}

	public static void prontBeam(Vec3 skeleton, EnumWaveType wave, EnumBeamType beam, int outerColor, int innerColor, int start, int segments, float size, int layers, float thickness) {
		prontBeam(skeleton, wave, beam, outerColor, innerColor, start, segments, size, layers, thickness, 1F);
	}

	public static void prontBeam(Vec3 skeleton, EnumWaveType wave, EnumBeamType beam, int outerColor, int innerColor, int start, int segments, float size, int layers, float thickness, float alpha) {
		prontBeam(skeleton, wave, beam, outerColor, innerColor, start, segments, size, layers, thickness, alpha, -1.0F);
	}

	public static void prontBeam(Vec3 skeleton, EnumWaveType wave, EnumBeamType beam, int outerColor, int innerColor, int start, int segments, float size, int layers, float thickness, float alpha, float fadeStartRatio) {

		GL11.glPushMatrix();
		GL11.glDepthMask(depthMask);

		float sYaw = (float) (Math.atan2(skeleton.xCoord, skeleton.zCoord) * 180F / Math.PI);
		float sqrt = MathHelper.sqrt_double(skeleton.xCoord * skeleton.xCoord + skeleton.zCoord * skeleton.zCoord);
		float sPitch = (float) (Math.atan2(skeleton.yCoord, (double) sqrt) * 180F / Math.PI);

		GL11.glRotatef(180, 0, 1F, 0);
		GL11.glRotatef(sYaw, 0, 1F, 0);
		GL11.glRotatef(sPitch - 90, 1F, 0, 0);

		GL11.glPushMatrix();
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_LIGHTING);

		if(beam == EnumBeamType.SOLID || beam == EnumBeamType.CYLINDER) {
			GL11.glDisable(GL11.GL_CULL_FACE);

			GL11.glEnable(GL11.GL_BLEND);

			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
		}

		Tessellator tessellator = Tessellator.instance;

		Vec3 unit = Vec3.createVectorHelper(0, 1, 0);
		rand.setSeed(start);
		double length = skeleton.lengthVector();
		double segLength = length / segments;
		double lastX = 0;
		double lastY = 0;
		double lastZ = 0;

		for(int i = 0; i <= segments; i++) {

			Vec3 spinner = Vec3.createVectorHelper(size, 0, 0);

			if(wave == EnumWaveType.SPIRAL) {
				spinner.rotateAroundY((float) Math.PI * (float) start / 180F);
				spinner.rotateAroundY((float) Math.PI * 45F / 180F * i);
			} else if(wave == EnumWaveType.RANDOM) {
				spinner.rotateAroundY((float) Math.PI * 2 * rand.nextFloat());
				spinner.rotateAroundY((float) Math.PI * 2 * rand.nextFloat());
			}
			double pX = unit.xCoord * segLength * i + spinner.xCoord;
			double pY = unit.yCoord * segLength * i + spinner.yCoord;
			double pZ = unit.zCoord * segLength * i + spinner.zCoord;

			if(beam == EnumBeamType.LINE && i > 0) {

				tessellator.startDrawing(3);
				tessellator.setColorOpaque_I(outerColor);
				tessellator.addVertex(pX, pY, pZ);
				tessellator.addVertex(lastX, lastY, lastZ);
				tessellator.draw();
			}

			if(beam == EnumBeamType.SOLID && i > 0) {
				float segmentAlpha = alpha * getDistanceFadeAlpha(i, segments, fadeStartRatio);
				if(segmentAlpha <= 0.0F) {
					lastX = pX;
					lastY = pY;
					lastZ = pZ;
					continue;
				}

				float radius = thickness / layers;

				for(int j = 1; j <= layers; j++) {

					float inter = (float) (j - 1) / (float) (layers - 1);

					int r1 = ((outerColor & 0xFF0000) >> 16);
					int g1 = ((outerColor & 0x00FF00) >> 8);
					int b1 = ((outerColor & 0x0000FF) >> 0);
					
					int r2 = ((innerColor & 0xFF0000) >> 16);
					int g2 = ((innerColor & 0x00FF00) >> 8);
					int b2 = ((innerColor & 0x0000FF) >> 0);

					int r = ((int)(r1 + (r2 - r1) * inter)) << 16;
					int g = ((int)(g1 + (g2 - g1) * inter)) << 8;
					int b = ((int)(b1 + (b2 - b1) * inter)) << 0;
					
					int color = r | g | b;

					tessellator.startDrawingQuads();
					setColorWithAlpha(tessellator, color, segmentAlpha);
					tessellator.addVertex(lastX + (radius * j), lastY, lastZ + (radius * j));
					tessellator.addVertex(lastX + (radius * j), lastY, lastZ - (radius * j));
					tessellator.addVertex(pX + (radius * j), pY, pZ - (radius * j));
					tessellator.addVertex(pX + (radius * j), pY, pZ + (radius * j));
					tessellator.draw();
					tessellator.startDrawingQuads();
					setColorWithAlpha(tessellator, color, segmentAlpha);
					tessellator.addVertex(lastX - (radius * j), lastY, lastZ + (radius * j));
					tessellator.addVertex(lastX - (radius * j), lastY, lastZ - (radius * j));
					tessellator.addVertex(pX - (radius * j), pY, pZ - (radius * j));
					tessellator.addVertex(pX - (radius * j), pY, pZ + (radius * j));
					tessellator.draw();
					tessellator.startDrawingQuads();
					setColorWithAlpha(tessellator, color, segmentAlpha);
					tessellator.addVertex(lastX + (radius * j), lastY, lastZ + (radius * j));
					tessellator.addVertex(lastX - (radius * j), lastY, lastZ + (radius * j));
					tessellator.addVertex(pX - (radius * j), pY, pZ + (radius * j));
					tessellator.addVertex(pX + (radius * j), pY, pZ + (radius * j));
					tessellator.draw();
					tessellator.startDrawingQuads();
					setColorWithAlpha(tessellator, color, segmentAlpha);
					tessellator.addVertex(lastX + (radius * j), lastY, lastZ - (radius * j));
					tessellator.addVertex(lastX - (radius * j), lastY, lastZ - (radius * j));
					tessellator.addVertex(pX - (radius * j), pY, pZ - (radius * j));
					tessellator.addVertex(pX + (radius * j), pY, pZ - (radius * j));
					tessellator.draw();
					
					
				}
			}

			if(beam == EnumBeamType.CYLINDER && i > 0) {
				float segmentAlpha = alpha * getDistanceFadeAlpha(i, segments, fadeStartRatio);
				if(segmentAlpha <= 0.0F) {
					lastX = pX;
					lastY = pY;
					lastZ = pZ;
					continue;
				}

				int radialSegments = 12;
				int safeLayers = Math.max(1, layers);
				float radiusStep = thickness / safeLayers;

				for(int j = 1; j <= safeLayers; j++) {
					float inter = safeLayers > 1 ? (float) (j - 1) / (float) (safeLayers - 1) : 1.0F;

					int r1 = ((outerColor & 0xFF0000) >> 16);
					int g1 = ((outerColor & 0x00FF00) >> 8);
					int b1 = ((outerColor & 0x0000FF) >> 0);

					int r2 = ((innerColor & 0xFF0000) >> 16);
					int g2 = ((innerColor & 0x00FF00) >> 8);
					int b2 = ((innerColor & 0x0000FF) >> 0);

					int r = ((int)(r1 + (r2 - r1) * inter)) << 16;
					int g = ((int)(g1 + (g2 - g1) * inter)) << 8;
					int b = ((int)(b1 + (b2 - b1) * inter)) << 0;

					int color = r | g | b;
					double radius = radiusStep * j;

					for(int s = 0; s < radialSegments; s++) {
						double a0 = (Math.PI * 2.0D * s) / radialSegments;
						double a1 = (Math.PI * 2.0D * (s + 1)) / radialSegments;
						double c0 = Math.cos(a0) * radius;
						double s0 = Math.sin(a0) * radius;
						double c1 = Math.cos(a1) * radius;
						double s1 = Math.sin(a1) * radius;

						tessellator.startDrawingQuads();
						setColorWithAlpha(tessellator, color, segmentAlpha);
						tessellator.addVertex(lastX + c0, lastY, lastZ + s0);
						tessellator.addVertex(lastX + c1, lastY, lastZ + s1);
						tessellator.addVertex(pX + c1, pY, pZ + s1);
						tessellator.addVertex(pX + c0, pY, pZ + s0);
						tessellator.draw();
					}
				}
			}

			lastX = pX;
			lastY = pY;
			lastZ = pZ;
		}

		if(beam == EnumBeamType.LINE) {

			tessellator.startDrawing(3);
			tessellator.setColorOpaque_I(innerColor);
			tessellator.addVertex(0, 0, 0);
			tessellator.addVertex(0, skeleton.lengthVector(), 0);
			tessellator.draw();
		}

		if(beam == EnumBeamType.SOLID || beam == EnumBeamType.CYLINDER) {
			GL11.glDisable(GL11.GL_BLEND);
			GL11.glEnable(GL11.GL_TEXTURE_2D);

		}

		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		
		GL11.glPopMatrix();
		GL11.glDepthMask(true);

		GL11.glPopMatrix();
	}

	private static float getDistanceFadeAlpha(int segmentIndex, int segments, float fadeStartRatio) {
		if(fadeStartRatio < 0.0F || segments <= 0) {
			return 1.0F;
		}
		float segmentEnd = (float) segmentIndex / (float) segments;
		if(segmentEnd <= fadeStartRatio) {
			return 1.0F;
		}
		float denom = Math.max(1.0E-4F, 1.0F - fadeStartRatio);
		float t = MathHelper.clamp_float((segmentEnd - fadeStartRatio) / denom, 0.0F, 1.0F);
		float smooth = t * t * (3.0F - 2.0F * t);
		return 1.0F - smooth;
	}
    private static void setColorWithAlpha(Tessellator tessellator, int color, float alpha) {
        float red = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;
        
        GL11.glColor4f(red, green, blue, alpha);
    }
}
