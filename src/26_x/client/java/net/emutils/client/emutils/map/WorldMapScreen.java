package net.emutils.client.emutils.map;

import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.compat.MinecraftClientCompat;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.emutils.client.emutils.waypoint.SharedWaypoint;
import net.emutils.client.emutils.waypoint.Waypoint;
import net.emutils.client.emutils.waypoint.WaypointEntry;
import net.emutils.client.emutils.waypoint.WaypointManager;
import net.emutils.client.emutils.waypoint.WaypointMarkerRenderer;
import net.emutils.client.emutils.waypoint.gui.WaypointsScreen;
import net.emutils.client.versioned.VersionedGuiTriangles;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * The full-screen world map (#215): everything you explored in a dimension, drawn like the minimap. Drag to
 * move, scroll to zoom around the cursor, right-click to add a waypoint there, click a waypoint to edit it.
 * The other explored dimensions of the world can be looked at too.
 *
 * <p>Opened while the minimap shows, the minimap grows out of its corner into the world map, turning
 * north-up and losing its round shape on the way, and shrinks back into it when closed.
 */
public final class WorldMapScreen extends Screen {
	private static final float MIN_ZOOM = 1.0F / 64.0F;
	private static final float MAX_ZOOM = 16.0F;
	private static final float ZOOM_STEP = 1.25F;
	private static final float DEFAULT_ZOOM = 2.0F;
	private static final long ANIMATION_NANOS = 280_000_000L;
	private static final int BACKGROUND = 0xFF101216;
	private static final int DIM = 0xA0000000;
	private static final int BAR_HEIGHT = 22;
	private static final int MARKER_SIZE = 12;
	private static final int ARROW = 0xFFFFFFFF;
	private static final int ARROW_OUTLINE = 0xE0101010;
	private static final int CHIP_HEIGHT = 16;
	/** Remembered while the game runs, so the map opens where you last left it zoomed. */
	private static float lastZoom = DEFAULT_ZOOM;

	private final @Nullable KeyMapping openKey;
	/** The minimap's place on screen when the map opened, which the map grows from; null when it wasn't showing. */
	private final MinimapRenderer.@Nullable Frame from;
	private final List<String> dimensions = new ArrayList<>();
	private final List<int[]> dimensionChips = new ArrayList<>();
	private String dimension;
	private MapWorld world;
	private MapTiles tiles;
	/** Tiles of another dimension than yours, freed when the screen closes. */
	private @Nullable MapTiles otherTiles;
	private double centerX;
	private double centerZ;
	private float zoom = lastZoom;
	private long openedAt = -1L;
	private long closingAt = -1L;
	private boolean dragging;
	private @Nullable Waypoint hovered;

	private WorldMapScreen(@Nullable KeyMapping openKey, MinimapRenderer.@Nullable Frame from, MapWorld world, String dimension) {
		super(Component.translatable(EMUtilsTexts.SCREEN_WORLD_MAP));
		this.openKey = openKey;
		this.from = from;
		this.world = world;
		this.tiles = MapManager.tiles();
		this.dimension = dimension;
	}

	/** Opens the world map of the dimension you are in, or nothing when no map is being kept. */
	public static void open(Minecraft client, @Nullable KeyMapping openKey) {
		MapWorld world = MapManager.world();
		if (world == null || client.player == null || client.level == null) {
			return;
		}
		WorldMapScreen screen = new WorldMapScreen(openKey, MinimapRenderer.frame(client), world, WaypointManager.dimensionId(client.level));
		screen.centerX = client.player.getX();
		screen.centerZ = client.player.getZ();
		client.gui.setScreen(screen);
	}

	/** The world map is open; the minimap hides meanwhile, since the map grows out of it. */
	public static boolean isOpen(Minecraft client) {
		return MinecraftClientCompat.screen(client) instanceof WorldMapScreen;
	}

	@Override
	protected void init() {
		if (openedAt < 0L) {
			openedAt = System.nanoTime();
		}
		dimensions.clear();
		dimensions.add(dimension);
		Path folder = MapManager.worldFolder(minecraft);
		if (folder != null && Files.isDirectory(folder)) {
			try (Stream<Path> children = Files.list(folder)) {
				children.filter(Files::isDirectory).sorted().forEach(path -> {
					String id = unsafeName(path);
					if (!dimensions.contains(id)) {
						dimensions.add(id);
					}
				});
			} catch (IOException exception) {
				EMUtilsClient.LOGGER.warn("EMUtils world map couldn't list the explored dimensions", exception);
			}
		}
	}

	/** The dimension id a folder was made for, as the map wrote it there, or a guess from the folder's name. */
	private static String unsafeName(Path folder) {
		String id = MapWorld.dimensionOf(folder);
		if (id != null) {
			return id;
		}
		String name = folder.getFileName().toString();
		int colon = name.indexOf('_');
		return colon > 0 ? name.substring(0, colon) + ":" + name.substring(colon + 1) : name;
	}

	/** How far the opening animation is, 0 to 1, eased; it runs backwards while closing. */
	private float progress() {
		long now = System.nanoTime();
		float t = closingAt >= 0L
			? 1.0F - Math.min(1.0F, (now - closingAt) / (float) ANIMATION_NANOS)
			: Math.min(1.0F, (now - openedAt) / (float) ANIMATION_NANOS);
		float inverse = 1.0F - t;
		return 1.0F - inverse * inverse * inverse;
	}

	@Override
	public void onClose() {
		if (closingAt < 0L) {
			closingAt = System.nanoTime();
		}
	}

	@Override
	public void removed() {
		lastZoom = zoom;
		if (otherTiles != null) {
			otherTiles.clear();
			otherTiles = null;
		}
		super.removed();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		// The map covers the screen; while it grows, the world dims behind it instead of blurring.
		context.fill(0, 0, width, height, UiTheme.fade(DIM, progress()));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		float progress = progress();
		LocalPlayer player = minecraft.player;
		boolean ownDimension = minecraft.level != null && dimension.equals(WaypointManager.dimensionId(minecraft.level));
		float yaw = player == null ? 180.0F : player.getViewYRot(delta);

		// The map's frame, view and shape, from the minimap's to the full screen's.
		float x = 0.0F;
		float y = 0.0F;
		float w = width;
		float h = height;
		float radius = 0.0F;
		float angle = 0.0F;
		float viewZoom = zoom;
		double viewX = centerX;
		double viewZ = centerZ;
		if (from != null && player != null && progress < 1.0F) {
			x = lerp(from.x(), 0.0F, progress);
			y = lerp(from.y(), 0.0F, progress);
			w = lerp(from.size(), width, progress);
			h = lerp(from.size(), height, progress);
			radius = lerp(from.round() ? from.size() / 2.0F : 0.0F, 0.0F, progress);
			angle = lerpAngle(from.angle(), 0.0F, progress);
			viewZoom = (float) Math.exp(lerp((float) Math.log(from.zoom() * from.scale()), (float) Math.log(zoom), progress));
			double px = player.xo + (player.getX() - player.xo) * delta;
			double pz = player.zo + (player.getZ() - player.zo) * delta;
			viewX = px + (centerX - px) * progress;
			viewZ = pz + (centerZ - pz) * progress;
		} else if (from == null && progress < 1.0F) {
			float scale = 0.9F + 0.1F * progress;
			w = width * scale;
			h = height * scale;
			x = (width - w) / 2.0F;
			y = (height - h) / 2.0F;
			radius = 8.0F * (1.0F - progress);
		}
		float[] outline = MapDraw.roundedRect(x, y, w, h, radius, 18);
		MapView view = new MapView(viewX, viewZ, viewZoom, angle, x + w / 2.0F, y + h / 2.0F);
		int tint = from == null ? UiTheme.fade(0xFFFFFFFF, progress) : 0xFFFFFFFF;

		MapDraw.fill(context, outline, from == null ? UiTheme.fade(BACKGROUND, progress) : BACKGROUND);
		tiles.beginFrame();
		float screenPixelsPerBlock = viewZoom * (float) minecraft.getWindow().getGuiScale();
		MapDraw.tiles(context, world, tiles, view, outline, MapDraw.level(screenPixelsPerBlock, MapTileBaker.LEVELS - 1), tint);

		hovered = null;
		if (ownDimension && player != null) {
			drawWaypoints(context, view, mouseX, mouseY, progress);
			double px = player.xo + (player.getX() - player.xo) * delta;
			double pz = player.zo + (player.getZ() - player.zo) * delta;
			drawArrow(context, view.screenX(px, pz), view.screenY(px, pz), (float) Math.toRadians(yaw + 180.0F) + angle);
		}
		if (progress >= 1.0F) {
			drawBars(context, view, mouseX, mouseY, ownDimension);
		}
	}

	private void drawWaypoints(GuiGraphicsExtractor context, MapView view, int mouseX, int mouseY, float progress) {
		WaypointManager manager = EMUtilsClient.waypoint();
		if (manager == null || !manager.enabled()) {
			return;
		}
		for (WaypointEntry entry : manager.renderEntries(minecraft)) {
			Waypoint waypoint = entry.waypoint();
			if (waypoint.hidden() || !entry.placeable()) {
				continue;
			}
			float sx = view.screenX(entry.renderX(), entry.renderZ());
			float sy = view.screenY(entry.renderX(), entry.renderZ());
			if (sx < -MARKER_SIZE || sy < -MARKER_SIZE || sx > width + MARKER_SIZE || sy > height + MARKER_SIZE) {
				continue;
			}
			context.pose().pushMatrix();
			context.pose().translate(sx, sy);
			WaypointMarkerRenderer.drawMapMarker(context, waypoint, MARKER_SIZE, progress);
			context.pose().popMatrix();
			if (Math.abs(mouseX - sx) <= MARKER_SIZE / 2.0F && Math.abs(mouseY - sy) <= MARKER_SIZE / 2.0F) {
				hovered = waypoint;
			}
		}
		if (hovered != null && progress >= 1.0F && hovered.label() != null) {
			context.setTooltipForNextFrame(Component.literal(hovered.label()), mouseX, mouseY);
		}
	}

	private static void drawArrow(GuiGraphicsExtractor context, float x, float y, float angle) {
		context.pose().pushMatrix();
		context.pose().translate(x, y);
		context.pose().rotate(angle);
		float[] outline = {0.0F, -7.5F, 5.6F, 5.8F, 0.0F, 2.9F, 0.0F, -7.5F, 0.0F, 2.9F, -5.6F, 5.8F};
		float[] arrow = {0.0F, -5.6F, 4.0F, 4.2F, 0.0F, 2.0F, 0.0F, -5.6F, 0.0F, 2.0F, -4.0F, 4.2F};
		VersionedGuiTriangles.colored(context, outline, 6, ARROW_OUTLINE);
		VersionedGuiTriangles.colored(context, arrow, 6, ARROW);
		context.pose().popMatrix();
	}

	/** The title and dimension chips along the top, and the coordinates under the cursor with the controls along the bottom. */
	private void drawBars(GuiGraphicsExtractor context, MapView view, int mouseX, int mouseY, boolean ownDimension) {
		UiTheme theme = UiTheme.current();
		context.fill(0, 0, width, BAR_HEIGHT, 0xB0000000);
		context.fill(0, height - BAR_HEIGHT, width, height, 0xB0000000);
		int textTop = (BAR_HEIGHT - UiText.lineHeight(font, UiText.Size.LABEL)) / 2;
		UiText.draw(context, font, title, UiText.Size.LABEL, 8, textTop, 0xFFFFFFFF);

		dimensionChips.clear();
		int chipX = 8 + UiText.width(font, title, UiText.Size.LABEL) + 12;
		int chipY = (BAR_HEIGHT - CHIP_HEIGHT) / 2;
		for (int i = 0; i < dimensions.size(); i++) {
			Component label = Component.literal(dimensionName(dimensions.get(i)));
			int chipWidth = UiText.width(font, label, UiText.Size.SMALL) + 12;
			boolean selected = dimensions.get(i).equals(dimension);
			boolean hover = mouseX >= chipX && mouseX < chipX + chipWidth && mouseY >= chipY && mouseY < chipY + CHIP_HEIGHT;
			int fill = selected ? theme.accent() : hover ? 0x60FFFFFF : 0x30FFFFFF;
			UiShapes.roundedRect(context, chipX, chipY, chipWidth, CHIP_HEIGHT, CHIP_HEIGHT / 2, fill);
			UiText.drawCentered(context, font, label, UiText.Size.SMALL, chipX + chipWidth / 2, chipY + CHIP_HEIGHT / 2, 0xFFFFFFFF);
			dimensionChips.add(new int[] {chipX, chipWidth, i});
			chipX += chipWidth + 4;
		}

		int blockX = (int) Math.floor(view.worldX(mouseX, mouseY));
		int blockZ = (int) Math.floor(view.worldZ(mouseX, mouseY));
		MapChunk chunk = mouseY > BAR_HEIGHT && mouseY < height - BAR_HEIGHT ? world.chunk(blockX >> 4, blockZ >> 4) : null;
		String position = chunk == null
			? blockX + ", " + blockZ
			: blockX + ", " + chunk.topY(MapChunk.index(blockX & 15, blockZ & 15)) + ", " + blockZ;
		int bottomTop = height - BAR_HEIGHT + textTop;
		UiText.draw(context, font, Component.literal(position), UiText.Size.LABEL, 8, bottomTop, 0xFFFFFFFF);
		Component hint = Component.translatable(ownDimension ? EMUtilsTexts.WORLD_MAP_HINT : EMUtilsTexts.WORLD_MAP_HINT_OTHER);
		UiText.draw(context, font, hint, UiText.Size.SMALL, width - 8 - UiText.width(font, hint, UiText.Size.SMALL), bottomTop + 1, 0xB0FFFFFF);
	}

	private static String dimensionName(String id) {
		return switch (id) {
			case "minecraft:overworld" -> Component.translatable(EMUtilsTexts.WORLD_MAP_OVERWORLD).getString();
			case "minecraft:the_nether" -> Component.translatable(EMUtilsTexts.WORLD_MAP_NETHER).getString();
			case "minecraft:the_end" -> Component.translatable(EMUtilsTexts.WORLD_MAP_END).getString();
			default -> id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
		};
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		if (closingAt >= 0L || progress() < 1.0F) {
			return true;
		}
		double mouseX = click.x();
		double mouseY = click.y();
		if (click.button() == InputConstants.MOUSE_BUTTON_LEFT && mouseY < BAR_HEIGHT) {
			int chipY = (BAR_HEIGHT - CHIP_HEIGHT) / 2;
			for (int[] chip : dimensionChips) {
				if (mouseX >= chip[0] && mouseX < chip[0] + chip[1] && mouseY >= chipY && mouseY < chipY + CHIP_HEIGHT) {
					switchDimension(dimensions.get(chip[2]));
					return true;
				}
			}
			return true;
		}
		if (mouseY < BAR_HEIGHT || mouseY >= height - BAR_HEIGHT) {
			return true;
		}
		if (click.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			if (hovered != null) {
				minecraft.gui.setScreen(WaypointsScreen.editWaypoint(this, hovered));
				return true;
			}
			dragging = true;
			return true;
		}
		if (click.button() == InputConstants.MOUSE_BUTTON_RIGHT && isOwnDimension()) {
			MapView view = fullView();
			int blockX = (int) Math.floor(view.worldX((float) mouseX, (float) mouseY));
			int blockZ = (int) Math.floor(view.worldZ((float) mouseX, (float) mouseY));
			MapChunk chunk = world.chunk(blockX >> 4, blockZ >> 4);
			int blockY = chunk != null
				? chunk.topY(MapChunk.index(blockX & 15, blockZ & 15)) + 1
				: minecraft.player == null ? 64 : minecraft.player.getBlockY();
			minecraft.gui.setScreen(WaypointsScreen.addShared(this, SharedWaypoint.at(blockX, blockY, blockZ)));
			return true;
		}
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		dragging = false;
		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
		if (dragging) {
			centerX -= dx / zoom;
			centerZ -= dy / zoom;
			return true;
		}
		return super.mouseDragged(click, dx, dy);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY == 0.0D || closingAt >= 0L) {
			return true;
		}
		// The block under the cursor stays under it while zooming.
		MapView before = fullView();
		double worldX = before.worldX((float) mouseX, (float) mouseY);
		double worldZ = before.worldZ((float) mouseX, (float) mouseY);
		zoom = Math.clamp(zoom * (float) Math.pow(ZOOM_STEP, scrollY), MIN_ZOOM, MAX_ZOOM);
		centerX = worldX - (mouseX - width / 2.0D) / zoom;
		centerZ = worldZ - (mouseY - height / 2.0D) / zoom;
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (openKey != null && openKey.matches(event)) {
			onClose();
			return true;
		}
		if (event.key() == InputConstants.KEY_SPACE && minecraft.player != null && isOwnDimension()) {
			centerX = minecraft.player.getX();
			centerZ = minecraft.player.getZ();
			return true;
		}
		return super.keyPressed(event);
	}

	/** For UI snapshot checks: zooms by scroll steps around the middle of the screen. */
	public void scrollForSnapshot(double steps) {
		mouseScrolled(width / 2.0D, height / 2.0D, 0.0D, steps);
	}

	/** For UI snapshot checks: the map's zoom in GUI pixels per block. */
	public float zoomForSnapshot() {
		return zoom;
	}

	private MapView fullView() {
		return new MapView(centerX, centerZ, zoom, 0.0F, width / 2.0F, height / 2.0F);
	}

	private boolean isOwnDimension() {
		return minecraft.level != null && dimension.equals(WaypointManager.dimensionId(minecraft.level));
	}

	/** Shows another dimension's map: read from its files, with tiles of its own. Back to yours, the shared ones. */
	private void switchDimension(String id) {
		if (id.equals(dimension) || minecraft.level == null) {
			return;
		}
		if (otherTiles != null) {
			otherTiles.clear();
			otherTiles = null;
		}
		dimension = id;
		if (isOwnDimension() && MapManager.world() != null) {
			world = MapManager.world();
			tiles = MapManager.tiles();
			if (minecraft.player != null) {
				centerX = minecraft.player.getX();
				centerZ = minecraft.player.getZ();
			}
			return;
		}
		Path folder = MapManager.worldFolder(minecraft);
		if (folder == null) {
			return;
		}
		world = new MapWorld(minecraft.level, folder.resolve(MapManager.safeName(id)));
		otherTiles = new MapTiles();
		tiles = otherTiles;
		// The Nether is an eighth the size of the Overworld, so the view moves with the scale between them.
		boolean toNether = id.equals("minecraft:the_nether");
		boolean fromNether = WaypointManager.dimensionId(minecraft.level).equals("minecraft:the_nether");
		if (toNether && !fromNether) {
			centerX /= 8.0D;
			centerZ /= 8.0D;
		} else if (fromNether && !toNether) {
			centerX *= 8.0D;
			centerZ *= 8.0D;
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (otherTiles != null) {
			// Another dimension's regions load like yours, but nobody else prepares them.
			MapManager.prepare(world, otherTiles);
		}
		if (closingAt >= 0L && progress() <= 0.0F) {
			minecraft.gui.setScreen(null);
		}
	}

	private static float lerp(float from, float to, float t) {
		return from + (to - from) * t;
	}

	/** Turns the shorter way round. */
	private static float lerpAngle(float from, float to, float t) {
		float difference = (float) Math.IEEEremainder(to - from, Math.PI * 2.0D);
		return from + difference * t;
	}
}
