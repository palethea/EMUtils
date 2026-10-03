package net.emutils.client.emutils.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.EMUtilsHudElements;
import net.emutils.client.emutils.compat.MinecraftClientCompat;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.ui.UiRasterScale;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.emutils.hud.layout.HudLayoutManager;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.emutils.client.emutils.waypoint.Waypoint;
import net.emutils.client.emutils.waypoint.WaypointEntry;
import net.emutils.client.emutils.waypoint.WaypointManager;
import net.emutils.client.emutils.waypoint.WaypointMarkerRenderer;
import net.emutils.client.versioned.VersionedGuiTriangles;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * The minimap HUD element (#212): the map around you, square or round, turning with you or north-up, with
 * your arrow in the middle, a north marker, your waypoints (pinned to the edge when past it) and your
 * coordinates under it.
 *
 * <p>The map is drawn from tiles as triangles cut to the map's outline, so a turning or round map needs no
 * stencil or render target. Which tiles it uses depends on how many screen pixels a block covers: close up
 * each block shows its full texture, farther out the tiles with fewer pixels per block take over.
 */
public final class MinimapRenderer {
	private static final Identifier ID = Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "minimap");
	/** The map's side in GUI pixels at 100% scale; the HUD Layout Editor scales it. */
	static final int MAP_SIZE = 112;
	private static final int TEXT_GAP = 3;
	private static final int ROUND_SEGMENTS = 72;
	private static final int BACKGROUND = 0xFF15171C;
	private static final int FRAME = 0xFF0B0C0F;
	private static final int FRAME_LIGHT = 0x40FFFFFF;
	private static final int ARROW = 0xFFFFFFFF;
	private static final int ARROW_OUTLINE = 0xE0101010;
	private static final int MARKER_SIZE = 10;
	private static final int NORTH_SIZE = 10;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int TEXT_SHADOW = 0x99000000;
	/** Below this many screen pixels per block, the next coarser tiles are used. */
	private static final float[] DETAIL_BELOW = {6.0F, 1.5F};

	private static @Nullable KeyMapping zoomInKey;
	private static @Nullable KeyMapping zoomOutKey;

	private MinimapRenderer() {
	}

	public static void register() {
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ID, MinimapRenderer::render);
	}

	public static void setKeyMappings(KeyMapping zoomIn, KeyMapping zoomOut) {
		zoomInKey = zoomIn;
		zoomOutKey = zoomOut;
	}

	public static void tick() {
		EMUtilsConfig config = EMUtilsClient.config();
		while (zoomInKey != null && zoomInKey.consumeClick()) {
			config.setMinimapZoom(config.minimapZoom().closer());
		}
		while (zoomOutKey != null && zoomOutKey.consumeClick()) {
			config.setMinimapZoom(config.minimapZoom().farther());
		}
	}

	public static int width() {
		return MAP_SIZE;
	}

	public static int height(EMUtilsConfig config, Font font) {
		return config.minimapCoordinates() ? MAP_SIZE + TEXT_GAP + UiText.lineHeight(font, UiText.Size.SMALL) : MAP_SIZE;
	}

	static int defaultX(int screenWidth) {
		return screenWidth - MAP_SIZE - 6;
	}

	static int defaultY() {
		return 6;
	}

	private static void render(GuiGraphicsExtractor context, DeltaTracker deltaTracker) {
		EMUtilsConfig config = EMUtilsClient.config();
		Minecraft client = Minecraft.getInstance();
		if (config == null || client.player == null || client.level == null || !config.minimap()) {
			return;
		}
		if (HudLayoutManager.isEditing() || MinecraftClientCompat.isHudHidden(client)) {
			return;
		}
		if (EMUtilsClient.zoom() != null && EMUtilsClient.zoom().shouldHideHud()) {
			return;
		}
		HudLayoutManager.ResolvedLayout layout = HudLayoutManager.resolveLayout(EMUtilsHudElements.MINIMAP, config, context.guiWidth(), context.guiHeight(), client);
		context.pose().pushMatrix();
		UiRasterScale.set(layout.scaleFactor());
		try {
			context.pose().translate(layout.position().x(), layout.position().y());
			context.pose().scale(layout.scaleFactor(), layout.scaleFactor());
			float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
			drawMap(context, client, config, layout.scaleFactor(), layout.opacityPercent(), partialTick);
		} finally {
			UiRasterScale.reset();
			context.pose().popMatrix();
		}
	}

	/** Draws the minimap with its top-left corner at the current origin, {@link #MAP_SIZE} wide. */
	static void drawMap(GuiGraphicsExtractor context, Minecraft client, EMUtilsConfig config, float layoutScale, int opacityPercent, float partialTick) {
		LocalPlayer player = client.player;
		if (player == null) {
			return;
		}
		MinimapShape shape = config.minimapShape();
		boolean rotate = config.minimapRotate();
		float zoom = config.minimapZoom().pixelsPerBlock();
		float half = MAP_SIZE / 2.0F;
		double centerX = player.xo + (player.getX() - player.xo) * partialTick;
		double centerZ = player.zo + (player.getZ() - player.zo) * partialTick;
		float yaw = player.getViewYRot(partialTick);
		// Turns world offsets so the way you face points up; north-up when the map doesn't turn.
		float angle = rotate ? (float) Math.toRadians(180.0F - yaw) : 0.0F;
		View view = new View(centerX, centerZ, zoom, angle, half);
		float opacity = Math.clamp(opacityPercent / 100.0F, 0.0F, 1.0F);
		float[] outline = outline(shape, half);

		if (shape == MinimapShape.ROUND) {
			UiShapes.circle(context, -1, -1, MAP_SIZE + 2, fade(FRAME, opacity));
		} else {
			context.fill(-1, -1, MAP_SIZE + 1, MAP_SIZE + 1, fade(FRAME, opacity));
		}
		fillOutline(context, outline, fade(BACKGROUND, opacity));
		drawTiles(context, client, view, outline, layoutScale, opacity);
		drawFrame(context, shape, opacity);
		// Markers are placed exactly where the map puts them, not rounded to pixels, so they move with it.
		drawNorth(context, client.font, view, shape, opacity);
		if (config.minimapWaypoints()) {
			drawWaypoints(context, client, view, shape, opacity, config.minimapWaypointsPinned());
		}
		drawArrow(context, half, rotate ? 0.0F : (float) Math.toRadians(yaw + 180.0F), opacity);
		if (config.minimapCoordinates()) {
			drawCoordinates(context, client.font, player, opacity);
		}
	}

	/** Maps world positions to the map: offsets from the center, scaled, turned, around the map's middle. */
	private record View(double centerX, double centerZ, float zoom, float angle, float half) {
		float screenX(double worldX, double worldZ) {
			double dx = (worldX - centerX) * zoom;
			double dz = (worldZ - centerZ) * zoom;
			return (float) (dx * Math.cos(angle) - dz * Math.sin(angle)) + half;
		}

		float screenY(double worldX, double worldZ) {
			double dx = (worldX - centerX) * zoom;
			double dz = (worldZ - centerZ) * zoom;
			return (float) (dx * Math.sin(angle) + dz * Math.cos(angle)) + half;
		}

		double worldX(float screenX, float screenY) {
			double sx = screenX - half;
			double sy = screenY - half;
			return centerX + (sx * Math.cos(angle) + sy * Math.sin(angle)) / zoom;
		}

		double worldZ(float screenX, float screenY) {
			double sx = screenX - half;
			double sy = screenY - half;
			return centerZ + (-sx * Math.sin(angle) + sy * Math.cos(angle)) / zoom;
		}

		/** How far from the center, in blocks, the map can show anything, corners included. */
		double reach() {
			return half * Math.sqrt(2.0D) / zoom;
		}
	}

	/** The map's outline as x, y corners going clockwise: a square, or a circle of many sides. */
	private static float[] outline(MinimapShape shape, float half) {
		if (shape == MinimapShape.SQUARE) {
			float size = half * 2.0F;
			return new float[] {0.0F, 0.0F, size, 0.0F, size, size, 0.0F, size};
		}
		float[] points = new float[ROUND_SEGMENTS * 2];
		for (int i = 0; i < ROUND_SEGMENTS; i++) {
			double a = i * Math.PI * 2.0D / ROUND_SEGMENTS;
			points[i * 2] = half + (float) Math.cos(a) * half;
			points[i * 2 + 1] = half + (float) Math.sin(a) * half;
		}
		return points;
	}

	private static void fillOutline(GuiGraphicsExtractor context, float[] outline, int color) {
		int corners = outline.length / 2;
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

	private static void drawTiles(GuiGraphicsExtractor context, Minecraft client, View view, float[] outline, float layoutScale, float opacity) {
		MapWorld world = MapManager.world();
		if (world == null) {
			return;
		}
		MapTiles tiles = MapManager.tiles();
		tiles.beginFrame();
		float screenPixelsPerBlock = view.zoom() * layoutScale * (float) client.getWindow().getGuiScale();
		int level = 0;
		while (level < DETAIL_BELOW.length && screenPixelsPerBlock < DETAIL_BELOW[level]) {
			level++;
		}
		int blocks = MapTileBaker.blocksPerTile(level);
		double reach = view.reach();
		int minTileX = (int) Math.floor((view.centerX() - reach) / blocks);
		int maxTileX = (int) Math.floor((view.centerX() + reach) / blocks);
		int minTileZ = (int) Math.floor((view.centerZ() - reach) / blocks);
		int maxTileZ = (int) Math.floor((view.centerZ() + reach) / blocks);
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
		int color = fade(0xFFFFFFFF, opacity);
		for (int[] tile : order) {
			DynamicTexture texture = tiles.texture(world, level, tile[0], tile[1]);
			if (texture == null && level < MapTileBaker.LEVELS - 1) {
				// Until the detailed tile is ready, a coarser one fills in, so the map is never empty.
				drawCoarserFallback(context, tiles, world, view, outline, level, tile[0], tile[1], color);
				continue;
			}
			if (texture != null) {
				drawTile(context, texture, view, outline, tile[0] * (double) blocks, tile[1] * (double) blocks, blocks, color);
			}
		}
	}

	/** Draws the part of a coarser tile that covers a missing detailed one. */
	private static void drawCoarserFallback(GuiGraphicsExtractor context, MapTiles tiles, MapWorld world, View view, float[] outline, int level, int tileX, int tileZ, int color) {
		int blocks = MapTileBaker.blocksPerTile(level);
		int coarseBlocks = MapTileBaker.blocksPerTile(level + 1);
		int coarseX = Math.floorDiv(tileX * blocks, coarseBlocks);
		int coarseZ = Math.floorDiv(tileZ * blocks, coarseBlocks);
		DynamicTexture coarse = tiles.texture(world, level + 1, coarseX, coarseZ);
		if (coarse == null) {
			return;
		}
		double x0 = tileX * (double) blocks;
		double z0 = tileZ * (double) blocks;
		float[] clip = clipToRect(outline, view, x0, z0, blocks);
		drawClipped(context, coarse, view, clip, coarseX * (double) coarseBlocks, coarseZ * (double) coarseBlocks, coarseBlocks, color);
	}

	private static void drawTile(GuiGraphicsExtractor context, DynamicTexture texture, View view, float[] outline, double x0, double z0, int blocks, int color) {
		float[] clip = clipToRect(outline, view, x0, z0, blocks);
		drawClipped(context, texture, view, clip, x0, z0, blocks, color);
	}

	/** The map's outline cut down to the part where a tile from (x0, z0), {@code blocks} wide, lies. */
	private static float[] clipToRect(float[] outline, View view, double x0, double z0, int blocks) {
		float[] tile = {
			view.screenX(x0, z0), view.screenY(x0, z0),
			view.screenX(x0 + blocks, z0), view.screenY(x0 + blocks, z0),
			view.screenX(x0 + blocks, z0 + blocks), view.screenY(x0 + blocks, z0 + blocks),
			view.screenX(x0, z0 + blocks), view.screenY(x0, z0 + blocks)
		};
		return clip(outline, tile);
	}

	/** Draws a convex polygon in map space with the tile's texture, its texture coordinates worked out from the world position of each corner. */
	private static void drawClipped(GuiGraphicsExtractor context, DynamicTexture texture, View view, float[] polygon, double x0, double z0, int blocks, int color) {
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

	private static void drawFrame(GuiGraphicsExtractor context, MinimapShape shape, float opacity) {
		if (shape == MinimapShape.SQUARE) {
			int light = fade(FRAME_LIGHT, opacity);
			context.fill(0, 0, MAP_SIZE, 1, light);
			context.fill(0, MAP_SIZE - 1, MAP_SIZE, MAP_SIZE, light);
			context.fill(0, 1, 1, MAP_SIZE - 1, light);
			context.fill(MAP_SIZE - 1, 1, MAP_SIZE, MAP_SIZE - 1, light);
			return;
		}
		// A thin ring over the map's edge hides the steps of its many-sided outline.
		float[] ring = new float[ROUND_SEGMENTS * 12];
		float half = MAP_SIZE / 2.0F;
		float outer = half + 0.6F;
		float inner = half - 1.0F;
		for (int i = 0; i < ROUND_SEGMENTS; i++) {
			double a0 = i * Math.PI * 2.0D / ROUND_SEGMENTS;
			double a1 = (i + 1) * Math.PI * 2.0D / ROUND_SEGMENTS;
			float ox0 = half + (float) Math.cos(a0) * outer;
			float oy0 = half + (float) Math.sin(a0) * outer;
			float ox1 = half + (float) Math.cos(a1) * outer;
			float oy1 = half + (float) Math.sin(a1) * outer;
			float ix0 = half + (float) Math.cos(a0) * inner;
			float iy0 = half + (float) Math.sin(a0) * inner;
			float ix1 = half + (float) Math.cos(a1) * inner;
			float iy1 = half + (float) Math.sin(a1) * inner;
			int t = i * 12;
			float[] quad = {ox0, oy0, ox1, oy1, ix1, iy1, ox0, oy0, ix1, iy1, ix0, iy0};
			System.arraycopy(quad, 0, ring, t, 12);
		}
		VersionedGuiTriangles.colored(context, ring, ROUND_SEGMENTS * 6, fade(FRAME_LIGHT, opacity));
	}

	/** An "N" on the map's edge where north is. */
	private static void drawNorth(GuiGraphicsExtractor context, Font font, View view, MinimapShape shape, float opacity) {
		float half = view.half();
		// North is the world's -z, turned like the rest of the map.
		float dirX = (float) Math.sin(view.angle());
		float dirY = (float) -Math.cos(view.angle());
		float[] at = edgePoint(shape, half, dirX, dirY, NORTH_SIZE / 2.0F + 1.0F);
		Component north = Component.translatable(EMUtilsTexts.HUD_MINIMAP_NORTH);
		context.pose().pushMatrix();
		// The circle and the letter share one center.
		context.pose().translate(at[0], at[1]);
		UiShapes.circle(context, -NORTH_SIZE / 2, -NORTH_SIZE / 2, NORTH_SIZE, fade(0xE0101010, opacity));
		UiText.drawInkCentered(context, font, north, UiText.Size.SMALL, 0.0F, 0.0F, fade(TEXT_COLOR, opacity));
		context.pose().popMatrix();
	}

	/**
	 * Where a ray from the map's middle in direction ({@code dirX}, {@code dirY}) leaves the map, pulled in by
	 * {@code inset} so something drawn there stays inside.
	 */
	private static float[] edgePoint(MinimapShape shape, float half, float dirX, float dirY, float inset) {
		float length = (float) Math.sqrt(dirX * dirX + dirY * dirY);
		if (length < 1.0E-6F) {
			return new float[] {half, half};
		}
		dirX /= length;
		dirY /= length;
		float reach = half - inset;
		if (shape == MinimapShape.SQUARE) {
			float scale = reach / Math.max(Math.abs(dirX), Math.abs(dirY));
			return new float[] {half + dirX * scale, half + dirY * scale};
		}
		return new float[] {half + dirX * reach, half + dirY * reach};
	}

	private static boolean inside(MinimapShape shape, float half, float x, float y, float inset) {
		float dx = x - half;
		float dy = y - half;
		float reach = half - inset;
		if (shape == MinimapShape.SQUARE) {
			return Math.abs(dx) <= reach && Math.abs(dy) <= reach;
		}
		return dx * dx + dy * dy <= reach * reach;
	}

	private static void drawWaypoints(GuiGraphicsExtractor context, Minecraft client, View view, MinimapShape shape, float opacity, boolean pinned) {
		WaypointManager manager = EMUtilsClient.waypoint();
		if (manager == null || !manager.enabled()) {
			return;
		}
		float half = view.half();
		float inset = MARKER_SIZE / 2.0F + 1.0F;
		for (WaypointEntry entry : manager.renderEntries(client)) {
			Waypoint waypoint = entry.waypoint();
			if (waypoint.hidden() || !entry.placeable()) {
				continue;
			}
			float x = view.screenX(entry.renderX(), entry.renderZ());
			float y = view.screenY(entry.renderX(), entry.renderZ());
			if (!inside(shape, half, x, y, inset)) {
				if (!pinned) {
					continue;
				}
				float[] edge = edgePoint(shape, half, x - half, y - half, inset);
				x = edge[0];
				y = edge[1];
			}
			context.pose().pushMatrix();
			context.pose().translate(x, y);
			WaypointMarkerRenderer.drawMapMarker(context, waypoint, MARKER_SIZE, opacity);
			context.pose().popMatrix();
		}
	}

	/** Your arrow in the middle of the map, pointing the way you face. */
	private static void drawArrow(GuiGraphicsExtractor context, float half, float angle, float opacity) {
		context.pose().pushMatrix();
		context.pose().translate(half, half);
		context.pose().rotate(angle);
		float[] outline = {0.0F, -6.2F, 4.6F, 4.8F, 0.0F, 2.4F, 0.0F, -6.2F, 0.0F, 2.4F, -4.6F, 4.8F};
		float[] arrow = {0.0F, -4.6F, 3.3F, 3.4F, 0.0F, 1.6F, 0.0F, -4.6F, 0.0F, 1.6F, -3.3F, 3.4F};
		VersionedGuiTriangles.colored(context, outline, 6, fade(ARROW_OUTLINE, opacity));
		VersionedGuiTriangles.colored(context, arrow, 6, fade(ARROW, opacity));
		context.pose().popMatrix();
	}

	private static void drawCoordinates(GuiGraphicsExtractor context, Font font, LocalPlayer player, float opacity) {
		Component text = Component.literal(player.getBlockX() + ", " + player.getBlockY() + ", " + player.getBlockZ());
		int width = UiText.width(font, text, UiText.Size.SMALL);
		int x = (MAP_SIZE - width) / 2;
		int top = MAP_SIZE + TEXT_GAP;
		// A one physical pixel shadow keeps the white text readable over any ground.
		float offset = 1.0F / (float) (Minecraft.getInstance().getWindow().getGuiScale() * UiRasterScale.get());
		UiText.drawExact(context, font, text, UiText.Size.SMALL, x + offset, top + offset, fade(TEXT_SHADOW, opacity));
		UiText.draw(context, font, text, UiText.Size.SMALL, x, top, fade(TEXT_COLOR, opacity));
	}

	private static int fade(int color, float opacity) {
		int alpha = Math.round((color >>> 24) * opacity);
		return alpha << 24 | (color & 0x00FFFFFF);
	}
}
