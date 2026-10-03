package net.emutils.client.emutils.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.emutils.client.versioned.VersionedGuiTriangles;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;

/**
 * Draws the map's tiles into a shape on screen (#212, #215), for the minimap and the world map alike. Tiles
 * are drawn as triangles cut to the shape's outline, so a turning or round map needs no stencil or render
 * target. Which tiles it uses depends on how many screen pixels a block covers: close up each block shows its
 * full texture, farther out tiles with fewer pixels per block take over, down to the regions' overviews.
 */
public final class MapDraw {
	/** Below this many screen pixels per block, the next coarser level is used. */
	private static final float[] DETAIL_BELOW = {6.0F, 1.5F, 0.375F, 0.094F, 0.0235F};

	private MapDraw() {
	}

	/** The detail level for a scale, no coarser than {@code coarsest}. */
	static int level(float screenPixelsPerBlock, int coarsest) {
		int level = 0;
		while (level < DETAIL_BELOW.length && level < coarsest && screenPixelsPerBlock < DETAIL_BELOW[level]) {
			level++;
		}
		return level;
	}

	/** Fills a convex outline with one color. */
	public static void fill(GuiGraphicsExtractor context, float[] outline, int color) {
		int corners = outline.length / 2;
		if (corners < 3) {
			return;
		}
		float[] triangles = new float[(corners - 2) * 6];
		for (int i = 1; i < corners - 1; i++) {
			int t = (i - 1) * 6;
			triangles[t] = outline[0];
			triangles[t + 1] = outline[1];
			triangles[t + 2] = outline[i * 2];
			triangles[t + 3] = outline[i * 2 + 1];
			triangles[t + 4] = outline[i * 2 + 2];
			triangles[t + 5] = outline[i * 2 + 3];
		}
		VersionedGuiTriangles.colored(context, triangles, (corners - 2) * 3, color);
	}

	/**
	 * Draws the tiles that fall inside a convex outline. Missing tiles are asked for, and a coarser tile fills
	 * in for one until it's ready, so the map is never empty while it loads.
	 */
	public static void tiles(GuiGraphicsExtractor context, MapWorld world, MapTiles tiles, MapView view, float[] outline, int level, int color) {
		int blocks = MapTileBaker.blocksPerTile(level);
		double minX = Double.MAX_VALUE;
		double maxX = -Double.MAX_VALUE;
		double minZ = Double.MAX_VALUE;
		double maxZ = -Double.MAX_VALUE;
		for (int i = 0; i < outline.length; i += 2) {
			double x = view.worldX(outline[i], outline[i + 1]);
			double z = view.worldZ(outline[i], outline[i + 1]);
			minX = Math.min(minX, x);
			maxX = Math.max(maxX, x);
			minZ = Math.min(minZ, z);
			maxZ = Math.max(maxZ, z);
		}
		int minTileX = (int) Math.floor(minX / blocks);
		int maxTileX = (int) Math.floor(maxX / blocks);
		int minTileZ = (int) Math.floor(minZ / blocks);
		int maxTileZ = (int) Math.floor(maxZ / blocks);
		int centerTileX = (int) Math.floor(view.centerX() / blocks);
		int centerTileZ = (int) Math.floor(view.centerZ() / blocks);

		// Nearest tiles first, so they are the first to be drawn when many are missing.
		List<int[]> order = new ArrayList<>();
		for (int tileZ = minTileZ; tileZ <= maxTileZ; tileZ++) {
			for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
				order.add(new int[] {tileX, tileZ});
			}
		}
		order.sort((a, b) -> Integer.compare(
			Math.max(Math.abs(a[0] - centerTileX), Math.abs(a[1] - centerTileZ)),
			Math.max(Math.abs(b[0] - centerTileX), Math.abs(b[1] - centerTileZ))
		));
		for (int[] tile : order) {
			DynamicTexture texture = tiles.texture(world, level, tile[0], tile[1]);
			if (texture == null && level < MapTileBaker.LEVELS - 1) {
				drawCoarserFallback(context, tiles, world, view, outline, level, tile[0], tile[1], color);
				continue;
			}
			if (texture != null) {
				double x0 = tile[0] * (double) blocks;
				double z0 = tile[1] * (double) blocks;
				drawClipped(context, texture, view, clipToRect(outline, view, x0, z0, blocks), x0, z0, blocks, color);
			}
		}
	}

	/** Draws the part of a coarser tile that covers a missing detailed one. */
	private static void drawCoarserFallback(GuiGraphicsExtractor context, MapTiles tiles, MapWorld world, MapView view, float[] outline, int level, int tileX, int tileZ, int color) {
		int blocks = MapTileBaker.blocksPerTile(level);
		int coarseBlocks = MapTileBaker.blocksPerTile(level + 1);
		int coarseX = Math.floorDiv(tileX * blocks, coarseBlocks);
		int coarseZ = Math.floorDiv(tileZ * blocks, coarseBlocks);
		DynamicTexture coarse = tiles.texture(world, level + 1, coarseX, coarseZ);
		if (coarse == null) {
			return;
		}
		float[] clip = clipToRect(outline, view, tileX * (double) blocks, tileZ * (double) blocks, blocks);
		drawClipped(context, coarse, view, clip, coarseX * (double) coarseBlocks, coarseZ * (double) coarseBlocks, coarseBlocks, color);
	}

	/** The outline cut down to the part where a tile from (x0, z0), {@code blocks} wide, lies. */
	private static float[] clipToRect(float[] outline, MapView view, double x0, double z0, int blocks) {
		float[] tile = {
			view.screenX(x0, z0), view.screenY(x0, z0),
			view.screenX(x0 + blocks, z0), view.screenY(x0 + blocks, z0),
			view.screenX(x0 + blocks, z0 + blocks), view.screenY(x0 + blocks, z0 + blocks),
			view.screenX(x0, z0 + blocks), view.screenY(x0, z0 + blocks)
		};
		return clip(outline, tile);
	}

	/** Draws a convex polygon with a tile's texture, its texture coordinates worked out from the world position of each corner. */
	private static void drawClipped(GuiGraphicsExtractor context, DynamicTexture texture, MapView view, float[] polygon, double x0, double z0, int blocks, int color) {
		int corners = polygon.length / 2;
		if (corners < 3) {
			return;
		}
		float[] xy = new float[(corners - 2) * 6];
		float[] uv = new float[(corners - 2) * 6];
		for (int i = 1; i < corners - 1; i++) {
			int t = (i - 1) * 6;
			int[] picks = {0, i, i + 1};
			for (int k = 0; k < 3; k++) {
				float x = polygon[picks[k] * 2];
				float y = polygon[picks[k] * 2 + 1];
				xy[t + k * 2] = x;
				xy[t + k * 2 + 1] = y;
				uv[t + k * 2] = (float) ((view.worldX(x, y) - x0) / blocks);
				uv[t + k * 2 + 1] = (float) ((view.worldZ(x, y) - z0) / blocks);
			}
		}
		VersionedGuiTriangles.textured(context, texture, xy, uv, (corners - 2) * 3, color);
	}

	/**
	 * Cuts a convex polygon down to another convex polygon (Sutherland-Hodgman). Both are x, y corners; the
	 * clip polygon may go either way round.
	 */
	public static float[] clip(float[] subject, float[] clipper) {
		float[] output = subject;
		int clipCorners = clipper.length / 2;
		float orientation = signedArea(clipper);
		for (int e = 0; e < clipCorners && output.length >= 6; e++) {
			float ax = clipper[e * 2];
			float ay = clipper[e * 2 + 1];
			float bx = clipper[(e + 1) % clipCorners * 2];
			float by = clipper[(e + 1) % clipCorners * 2 + 1];
			float[] input = output;
			float[] next = new float[input.length + 4];
			int count = 0;
			int corners = input.length / 2;
			for (int i = 0; i < corners; i++) {
				float px = input[i * 2];
				float py = input[i * 2 + 1];
				float qx = input[(i + 1) % corners * 2];
				float qy = input[(i + 1) % corners * 2 + 1];
				float pSide = side(ax, ay, bx, by, px, py) * orientation;
				float qSide = side(ax, ay, bx, by, qx, qy) * orientation;
				if (pSide >= 0.0F) {
					if (count + 2 > next.length) {
						next = Arrays.copyOf(next, next.length * 2);
					}
					next[count++] = px;
					next[count++] = py;
				}
				if ((pSide >= 0.0F) != (qSide >= 0.0F)) {
					float t = pSide / (pSide - qSide);
					if (count + 2 > next.length) {
						next = Arrays.copyOf(next, next.length * 2);
					}
					next[count++] = px + (qx - px) * t;
					next[count++] = py + (qy - py) * t;
				}
			}
			output = Arrays.copyOf(next, count);
		}
		return output;
	}

	private static float side(float ax, float ay, float bx, float by, float px, float py) {
		return (bx - ax) * (py - ay) - (by - ay) * (px - ax);
	}

	private static float signedArea(float[] polygon) {
		float area = 0.0F;
		int corners = polygon.length / 2;
		for (int i = 0; i < corners; i++) {
			int j = (i + 1) % corners;
			area += polygon[i * 2] * polygon[j * 2 + 1] - polygon[j * 2] * polygon[i * 2 + 1];
		}
		return Math.signum(area);
	}

	/**
	 * A rounded rectangle's outline, {@code segments} corners per rounded corner. With the radius at half the
	 * smaller side it's a circle or a pill, at 0 a plain rectangle, so a shape can turn into another smoothly.
	 */
	public static float[] roundedRect(float x, float y, float width, float height, float radius, int segments) {
		float r = Math.clamp(radius, 0.0F, Math.min(width, height) / 2.0F);
		if (r < 0.01F) {
			return new float[] {x, y, x + width, y, x + width, y + height, x, y + height};
		}
		float[] points = new float[segments * 4 * 2];
		float[][] centers = {
			{x + width - r, y + r},
			{x + width - r, y + height - r},
			{x + r, y + height - r},
			{x + r, y + r}
		};
		int i = 0;
		for (int corner = 0; corner < 4; corner++) {
			double start = -Math.PI / 2.0D + corner * Math.PI / 2.0D;
			for (int s = 0; s < segments; s++) {
				double a = start + s * (Math.PI / 2.0D) / (segments - 1);
				points[i++] = centers[corner][0] + (float) Math.cos(a) * r;
				points[i++] = centers[corner][1] + (float) Math.sin(a) * r;
			}
		}
		return points;
	}
}
