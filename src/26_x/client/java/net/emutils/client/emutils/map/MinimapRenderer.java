package net.emutils.client.emutils.map;

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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * The minimap HUD element (#212): the map around you, square or round, turning with you or north-up, with
 * your arrow in the middle, a north marker, your waypoints (pinned to the edge when past it) and your
 * coordinates under it.
 *
 * <p>The tiles are drawn by {@link MapDraw}, which the world map shares.
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
		if (HudLayoutManager.isEditing() || MinecraftClientCompat.isHudHidden(client) || WorldMapScreen.isOpen(client)) {
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

	/** Where the minimap is on screen and what it shows, for the world map to grow out of (#215). */
	record Frame(float x, float y, float size, float scale, float zoom, float angle, boolean round) {
	}

	/** The minimap's frame on screen right now, or null when it isn't showing. */
	static @Nullable Frame frame(Minecraft client) {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || client.player == null || !config.minimap() || MinecraftClientCompat.isHudHidden(client)) {
			return null;
		}
		int guiWidth = client.getWindow().getGuiScaledWidth();
		int guiHeight = client.getWindow().getGuiScaledHeight();
		HudLayoutManager.ResolvedLayout layout = HudLayoutManager.resolveLayout(EMUtilsHudElements.MINIMAP, config, guiWidth, guiHeight, client);
		float yaw = client.player.getViewYRot(1.0F);
		float angle = config.minimapRotate() ? (float) Math.toRadians(180.0F - yaw) : 0.0F;
		return new Frame(
			layout.position().x(),
			layout.position().y(),
			MAP_SIZE * layout.scaleFactor(),
			layout.scaleFactor(),
			config.minimapZoom().pixelsPerBlock(),
			angle,
			config.minimapShape() == MinimapShape.ROUND
		);
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
		MapWorld mapWorld = MapManager.world();
		// In the tilted view (#217) you're drawn where your height lifts you, which stays in the middle.
		if (mapWorld != null) {
			centerZ -= MapManager.lift(mapWorld, player.getY());
		}
		float yaw = player.getViewYRot(partialTick);
		// Turns world offsets so the way you face points up; north-up when the map doesn't turn.
		float angle = rotate ? (float) Math.toRadians(180.0F - yaw) : 0.0F;
		MapView view = new MapView(centerX, centerZ, zoom, angle, half, half);
		float opacity = Math.clamp(opacityPercent / 100.0F, 0.0F, 1.0F);
		float[] outline = outline(shape);

		if (shape == MinimapShape.ROUND) {
			UiShapes.circle(context, -1, -1, MAP_SIZE + 2, fade(FRAME, opacity));
		} else {
			context.fill(-1, -1, MAP_SIZE + 1, MAP_SIZE + 1, fade(FRAME, opacity));
		}
		MapDraw.fill(context, outline, fade(BACKGROUND, opacity));
		MapWorld world = MapManager.world();
		if (world != null) {
			MapManager.tiles().beginFrame();
			float screenPixelsPerBlock = zoom * layoutScale * (float) client.getWindow().getGuiScale();
			MapDraw.tiles(context, world, MapManager.tiles(), view, outline, MapDraw.level(screenPixelsPerBlock, MapTileBaker.COLUMN_LEVELS - 1), fade(0xFFFFFFFF, opacity));
		}
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

	/** The map's outline: a square, or a circle of many sides. */
	static float[] outline(MinimapShape shape) {
		return MapDraw.roundedRect(0.0F, 0.0F, MAP_SIZE, MAP_SIZE, shape == MinimapShape.ROUND ? MAP_SIZE / 2.0F : 0.0F, ROUND_SEGMENTS / 4);
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
	private static void drawNorth(GuiGraphicsExtractor context, Font font, MapView view, MinimapShape shape, float opacity) {
		float half = view.screenCenterX();
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

	private static void drawWaypoints(GuiGraphicsExtractor context, Minecraft client, MapView view, MinimapShape shape, float opacity, boolean pinned) {
		WaypointManager manager = EMUtilsClient.waypoint();
		if (manager == null || !manager.enabled()) {
			return;
		}
		float half = view.screenCenterX();
		float inset = MARKER_SIZE / 2.0F + 1.0F;
		for (WaypointEntry entry : manager.renderEntries(client)) {
			Waypoint waypoint = entry.waypoint();
			if (waypoint.hidden() || !entry.placeable()) {
				continue;
			}
			double z = entry.renderZ() - (MapManager.world() == null ? 0.0D : MapManager.lift(MapManager.world(), entry.y()));
			float x = view.screenX(entry.renderX(), z);
			float y = view.screenY(entry.renderX(), z);
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
