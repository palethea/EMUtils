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
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.settings.SettingsScreen;
import net.emutils.client.emutils.gui.ui.UiAnim;
import net.emutils.client.emutils.gui.hub.HubIcons;
import net.emutils.client.emutils.gui.ui.UiContextMenu;
import net.emutils.client.emutils.gui.ui.UiIcons;
import net.emutils.client.emutils.gui.ui.UiOpacity;
import net.emutils.client.emutils.gui.ui.UiPromptDialog;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.emutils.client.emutils.waypoint.SharedWaypoint;
import net.emutils.client.emutils.waypoint.Waypoint;
import net.emutils.client.emutils.waypoint.WaypointEntry;
import net.emutils.client.emutils.waypoint.WaypointManager;
import net.emutils.client.emutils.waypoint.WaypointMarkerRenderer;
import net.emutils.client.emutils.waypoint.gui.WaypointSheet;
import net.emutils.client.emutils.waypoint.gui.WaypointsScreen;
import net.emutils.client.versioned.VersionedGuiTriangles;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * The full-screen world map (#215): everything you explored in a dimension, drawn like the minimap. Drag to
 * move, scroll to zoom around the cursor. Right-click for a menu: add a waypoint there, or edit, share or
 * delete the one under the cursor, teleport there when the server lets you, or copy the coordinates. Adding
 * and editing open the waypoint sheet over the map. The other explored dimensions can be looked at too.
 *
 * <p>Under the title, a chip shows which of the server's worlds the map is (#219), with a menu to look at the
 * others, rename them, say which one you're in, start a new one or delete one; and a chip switches between
 * the surface and underground (#222), which is the cave layer at your height. A dimension with a ceiling,
 * like the Nether, has no surface to show, so it's underground only.
 *
 * <p>Opened while the minimap shows, the minimap grows out of its corner into the world map, turning
 * north-up and losing its round shape on the way, and shrinks back into it when closed. The map's panels
 * fade and slide in with it.
 */
public final class WorldMapScreen extends Screen {
	private static final float MIN_ZOOM = 1.0F / 64.0F;
	private static final float MAX_ZOOM = 16.0F;
	private static final float ZOOM_STEP = 1.25F;
	private static final float DEFAULT_ZOOM = 2.0F;
	private static final long ANIMATION_NANOS = 280_000_000L;
	private static final int BACKGROUND = 0xFF101216;
	private static final int DIM = 0xA0000000;
	private static final int MARGIN = 8;
	private static final int PANEL_HEIGHT = 26;
	private static final int PANEL_RADIUS = 8;
	private static final int CHIP_HEIGHT = 18;
	private static final int MARKER_SIZE = 12;
	private static final float HIDDEN_ALPHA = 0.4F;
	/** How quickly zooming glides to where the wheel asked, per second; higher is snappier. */
	private static final float ZOOM_SPEED = 16.0F;
	private static final int SPINNER_SIZE = 10;
	private static final int CHEVRON_SIZE = 8;
	/** A player's face on the radar (#224), and how much bigger mob dots are than on the minimap. */
	private static final int RADAR_FACE = 10;
	private static final float RADAR_DOT_SCALE = 1.2F;
	/** Mobs and items show from this zoom in, in GUI pixels per block. */
	private static final float RADAR_MOBS_ZOOM = 0.5F;
	/** How close the cursor has to be to an entity, in GUI pixels, to name it. */
	private static final float RADAR_HOVER = 6.0F;
	private static final long LOADING_SHOW_AFTER_MILLIS = 250L;
	private static final long LOADING_HIDE_AFTER_MILLIS = 600L;
	private static final int ARROW = 0xFFFFFFFF;
	private static final int ARROW_OUTLINE = 0xE0101010;
	/** The panels start appearing once the map is this far open, so they arrive with it rather than after it. */
	private static final float PANELS_FROM = 0.35F;
	private static final float PANEL_SLIDE = 6.0F;
	/** Remembered while the game runs, so the map opens where you last left it zoomed. */
	private static float lastZoom = DEFAULT_ZOOM;

	private final UiAnim anim = new UiAnim();
	private final @Nullable KeyMapping openKey;
	/** The minimap's place on screen when the map opened, which the map grows from; null when it wasn't showing. */
	private final MinimapRenderer.@Nullable Frame from;
	private final List<String> dimensions = new ArrayList<>();
	/** Each dimension chip's x and width, by the index of its dimension. */
	private final List<int[]> dimensionChips = new ArrayList<>();
	/** The world chip's and the layer chip's place, x, y, width, height each; empty while not shown. */
	private int[] worldChip = new int[0];
	private int[] layerChip = new int[0];
	/** The panels' places on screen, x, y, width, height each, so clicks on them don't reach the map. */
	private final List<int[]> panels = new ArrayList<>();
	private String dimension;
	/** Which of the server's worlds is shown (#219), or null when that isn't known. */
	private @Nullable String worldId;
	/** The cave layer shown (#222), or {@link MapSampler#SURFACE}. */
	private int cave;
	private MapWorld world;
	private MapTiles tiles;
	/** Tiles of a map that isn't one of the two kept for where you are, freed when the screen closes. */
	private @Nullable MapTiles otherTiles;
	private @Nullable UiPromptDialog prompt;
	/**
	 * What the screen showed before switching to another map, drawn under the new one until it has drawn,
	 * so switching layers or worlds doesn't flash the map empty; closed when it goes if the screen opened it.
	 */
	private @Nullable MapTiles fading;
	private @Nullable MapWorld fadingWorld;
	private long fadingIdleSince = -1L;
	private long fadingSince;
	private double centerX;
	private double centerZ;
	private float zoom = lastZoom;
	/** The zoom the wheel asked for; {@link #zoom} glides to it. */
	private float targetZoom = lastZoom;
	/** The world spot that stays under the cursor while zooming glides, and where on screen it stays. */
	private double anchorWorldX;
	private double anchorWorldZ;
	private float anchorScreenX;
	private float anchorScreenY;
	private long lastFrame;
	/** When the map started or stopped loading, so the loading sign doesn't flash for a moment's work. */
	private long busySince = -1L;
	private long idleSince = -1L;
	private long openedAt = -1L;
	private long closingAt = -1L;
	private boolean dragging;
	private @Nullable WaypointEntry hovered;
	/** How many waypoints were drawn last frame, for UI snapshot checks. */
	private int waypointsDrawn;
	private @Nullable UiContextMenu menu;
	/** The entity on the radar under the cursor (#224), named in a tooltip. */
	private MapRadar.@Nullable Blip hoveredBlip;
	private @Nullable WaypointSheet sheet;

	private WorldMapScreen(@Nullable KeyMapping openKey, MinimapRenderer.@Nullable Frame from, MapWorld world, MapTiles tiles) {
		super(Component.translatable(EMUtilsTexts.SCREEN_WORLD_MAP));
		this.openKey = openKey;
		this.from = from;
		this.world = world;
		this.tiles = tiles;
		this.dimension = world.dimension();
		this.worldId = world.worldId;
		this.cave = world.cave;
	}

	/** Opens the world map where you are, as the minimap shows it: the surface or your cave layer. */
	public static void open(Minecraft client, @Nullable KeyMapping openKey) {
		MapWorld world = MapManager.shownWorld();
		if (world == null || client.player == null || client.level == null) {
			return;
		}
		WorldMapScreen screen = new WorldMapScreen(openKey, MinimapRenderer.frame(client), world, MapManager.shownTiles());
		screen.centerX = client.player.getX();
		screen.centerZ = client.player.getZ();
		screen.anchorWorldX = screen.centerX;
		screen.anchorWorldZ = screen.centerZ;
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
		// Back from the settings or the waypoint list, which closed a map of its own when they opened.
		if (otherTiles == null && !showingLive()) {
			show(dimension, worldId, cave);
		}
		dimensions.clear();
		// Yours first, also when another one is shown.
		dimensions.add(minecraft.level == null ? dimension : WaypointManager.dimensionId(minecraft.level));
		if (!dimensions.contains(dimension)) {
			dimensions.add(dimension);
		}
		Path folder = MapManager.serverFolder();
		if (folder != null && Files.isDirectory(folder)) {
			try (Stream<Path> children = Files.list(folder)) {
				children.filter(Files::isDirectory).sorted().forEach(path -> {
					String id = dimensionOf(path);
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
	private static String dimensionOf(Path folder) {
		String id = MapWorld.dimensionOf(folder);
		if (id != null) {
			return id;
		}
		// Without the hash a changed name ends in.
		String name = folder.getFileName().toString().replaceFirst("-[0-9a-f]{1,8}$", "");
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
		// Esc closes what's over the map first.
		if (sheet != null) {
			sheet.close();
			return;
		}
		if (prompt != null) {
			return;
		}
		if (menu != null) {
			menu = null;
			return;
		}
		if (closingAt < 0L) {
			closingAt = System.nanoTime();
		}
	}

	@Override
	public void tick() {
		super.tick();
		followLive();
		if (otherTiles != null) {
			if (world.importer != null) {
				world.importer.center(centerX, centerZ);
			}
			// Another dimension's regions load like yours, but nobody else prepares them.
			MapManager.prepare(world, otherTiles);
			MapManager.importSaved(world, otherTiles, System.nanoTime() + 2_000_000L);
		}
		if (closingAt >= 0L && progress() <= 0.0F) {
			minecraft.gui.setScreen(null);
		}
	}

	@Override
	public void removed() {
		lastZoom = targetZoom;
		releaseFading();
		closeOther();
		super.removed();
	}

	/** Lets go of a map the screen opened itself: stops its importer and frees its tiles. */
	private void closeOther() {
		if (otherTiles != null) {
			if (world != MapManager.world() && world != MapManager.cave()) {
				world.close();
			}
			otherTiles.clear();
			otherTiles = null;
		}
	}

	/** Whether the map shown is one of the two kept for where you are, the surface's or your cave layer's. */
	private boolean showingLive() {
		return world == MapManager.world() || world == MapManager.cave();
	}

	/**
	 * Keeps showing what's live when it changes under the screen: a new cave layer as you climb or dig, or
	 * the map started for the world you said you're in.
	 */
	private void followLive() {
		if (otherTiles != null || showingLive()) {
			return;
		}
		MapWorld live = cave == MapSampler.SURFACE ? MapManager.world() : MapManager.cave();
		if (live != null && live.dimension().equals(dimension)) {
			world = live;
			tiles = cave == MapSampler.SURFACE ? MapManager.tiles() : MapManager.caveTiles();
			worldId = live.worldId;
			cave = live.cave;
		} else {
			// What was shown isn't live any more, like the cave layer you just left: it stays on screen, read from disk.
			show(dimension, worldId, cave);
		}
	}

	/**
	 * Shows the map of a dimension, one of its worlds and a layer: one of the two kept for where you are when
	 * it's one of them, else read from disk with tiles of its own. Returns false when there's no such map.
	 */
	private boolean show(String shownDimension, @Nullable String shownWorld, int shownCave) {
		if (minecraft.level == null) {
			return false;
		}
		keepFading();
		MapWorld surface = MapManager.world();
		MapWorld liveCave = MapManager.cave();
		if (surface != null && surface.dimension().equals(shownDimension) && java.util.Objects.equals(surface.worldId, shownWorld)) {
			if (shownCave == MapSampler.SURFACE) {
				use(surface, MapManager.tiles(), null);
				return true;
			}
			if (liveCave != null && liveCave.cave == shownCave) {
				use(liveCave, MapManager.caveTiles(), null);
				return true;
			}
		}
		MapWorlds catalog = MapManager.worlds(shownDimension);
		MapWorlds.Entry entry = catalog == null ? null : catalog.get(shownWorld);
		if (entry == null && catalog != null) {
			entry = catalog.latest();
		}
		if (entry == null && catalog != null && minecraft.getSingleplayerServer() != null) {
			// A save is one world, so a dimension not mapped yet gets it, and what's imported for it is kept.
			entry = catalog.create();
		}
		// Without a world, a dimension nobody mapped yet: an empty map, kept nowhere.
		Path folder = entry == null ? null : shownCave == MapSampler.SURFACE ? catalog.folder(entry.id()) : catalog.caveFolder(entry.id(), shownCave);
		boolean own = shownDimension.equals(WaypointManager.dimensionId(minecraft.level));
		Identifier dimensionId = Identifier.tryParse(shownDimension);
		ResourceKey<Level> key = dimensionId == null ? null : ResourceKey.create(Registries.DIMENSION, dimensionId);
		// In singleplayer the dimension's bottom is known, so what's imported for it can be saved; elsewhere only yours is.
		ServerLevel server = key == null ? null : MapImporter.serverLevel(minecraft, key);
		int minY = own ? minecraft.level.getMinY() : server != null ? server.getMinY() : Integer.MIN_VALUE;
		boolean ceiling = own ? minecraft.level.dimensionType().hasCeiling() : server != null ? server.dimensionType().hasCeiling() : shownDimension.equals("minecraft:the_nether");
		MapWorld opened = new MapWorld(minecraft.level, folder, shownDimension, minY, ceiling, entry == null ? null : entry.id(), shownCave);
		if (key != null && !own && shownCave == MapSampler.SURFACE) {
			opened.importer = MapImporter.start(minecraft, opened, key);
		}
		MapTiles opening = new MapTiles();
		use(opened, opening, opening);
		return true;
	}

	/** Keeps what's shown on screen under the next map, instead of closing it right away. */
	private void keepFading() {
		releaseFading();
		fading = tiles;
		fadingWorld = otherTiles != null ? world : null;
		fadingIdleSince = -1L;
		fadingSince = System.currentTimeMillis();
		// No longer this screen's own map, so switching doesn't close it; it's closed when it stops fading.
		otherTiles = null;
	}

	/** Lets go of what faded under the map, closing it when the screen had opened it. */
	private void releaseFading() {
		if (fading != null && fadingWorld != null && fadingWorld != MapManager.world() && fadingWorld != MapManager.cave()) {
			fadingWorld.close();
			fading.clear();
		}
		fading = null;
		fadingWorld = null;
	}

	private void use(MapWorld shown, MapTiles shownTiles, @Nullable MapTiles own) {
		world = shown;
		tiles = shownTiles;
		otherTiles = own;
		dimension = shown.dimension();
		worldId = shown.worldId;
		cave = shown.cave;
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
		anim.frame();
		glideZoom();
		float progress = progress();
		LocalPlayer player = minecraft.player;
		boolean ownDimension = isOwnDimension();
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
		int level = MapDraw.level(screenPixelsPerBlock, MapTileBaker.LEVELS - 1);
		// The map shown before stays under the new one while it draws: the layer you just left, or the one the
		// screen switched away from.
		MapTiles backdrop = fading != null ? fading : showingLive() ? MapManager.backdropTiles() : null;
		if (backdrop != null && backdrop != tiles) {
			MapDraw.backdrop(context, backdrop, view, outline, level, tint);
		}
		if (fading != null) {
			// Like the minimap's (#222): gone once the new map has had nothing to draw for a moment, or after a while.
			long now = System.currentTimeMillis();
			if (tiles.busy()) {
				fadingIdleSince = -1L;
			} else if (fadingIdleSince < 0L) {
				fadingIdleSince = now;
			}
			if (fadingIdleSince >= 0L && now - fadingIdleSince >= MapManager.BACKDROP_IDLE_MILLIS || now - fadingSince >= MapManager.BACKDROP_MAX_MILLIS) {
				releaseFading();
			}
		}
		MapDraw.tiles(context, world, tiles, view, outline, level, tint);

		boolean interactive = menu == null && sheet == null && prompt == null && progress >= 1.0F;
		hovered = null;
		hoveredBlip = null;
		MapWorld surface = MapManager.world();
		boolean here = ownDimension && player != null && (surface == null || surface.worldId == null || java.util.Objects.equals(surface.worldId, worldId));
		// The players and mobs around you (#224) where you are; the waypoints of the dimension shown, also
		// when it isn't yours, over them; your arrow only in your own.
		if (here) {
			drawRadar(context, view, x, y, w, h, mouseX, mouseY, progress, delta, interactive && !overPanel(mouseX, mouseY));
		}
		drawWaypoints(context, view, mouseX, mouseY, progress, interactive && !overPanel(mouseX, mouseY));
		if (here) {
			double px = player.xo + (player.getX() - player.xo) * delta;
			double pz = player.zo + (player.getZ() - player.zo) * delta;
			drawArrow(context, view.screenX(px, pz), view.screenY(px, pz), (float) Math.toRadians(yaw + 180.0F) + angle);
		}

		// The panels arrive with the map instead of popping in after it, and leave with it.
		float panels = Math.clamp((progress - PANELS_FROM) / (1.0F - PANELS_FROM), 0.0F, 1.0F);
		UiTheme theme = UiTheme.current();
		if (panels > 0.0F) {
			UiOpacity.set(panels);
			drawPanels(context, theme, view, mouseX, mouseY, ownDimension, (1.0F - panels) * PANEL_SLIDE, interactive);
			UiOpacity.reset();
		}
		drawLoading(context, theme, panels);
		if (interactive && hovered != null && hovered.waypoint().label() != null) {
			context.setTooltipForNextFrame(Component.literal(hovered.waypoint().label()), mouseX, mouseY);
		} else if (interactive && hoveredBlip != null) {
			context.setTooltipForNextFrame(hoveredBlip.name(), mouseX, mouseY);
		}
		if (menu != null) {
			menu.render(context, theme, mouseX, mouseY, width, height);
			if (menu.isClosed()) {
				menu = null;
			}
		}
		if (sheet != null) {
			sheet.render(context, theme, mouseX, mouseY, width, height);
			if (sheet.isClosed()) {
				sheet = null;
			}
		}
		if (prompt != null) {
			prompt.render(context, theme, mouseX, mouseY, width, height);
			if (prompt.isClosed()) {
				prompt = null;
			}
		}
	}

	/** Moves the zoom a step closer to where the wheel asked, keeping the anchored spot under the cursor. */
	private void glideZoom() {
		long now = System.nanoTime();
		float seconds = lastFrame == 0L ? 0.0F : Math.min(0.1F, (now - lastFrame) / 1.0E9F);
		lastFrame = now;
		if (zoom == targetZoom) {
			return;
		}
		double logZoom = Math.log(zoom);
		double logTarget = Math.log(targetZoom);
		double step = 1.0D - Math.exp(-ZOOM_SPEED * seconds);
		zoom = Math.abs(logTarget - logZoom) < 0.002D ? targetZoom : (float) Math.exp(logZoom + (logTarget - logZoom) * step);
		centerX = anchorWorldX - (anchorScreenX - width / 2.0D) / zoom;
		centerZ = anchorWorldZ - (anchorScreenY - height / 2.0D) / zoom;
	}

	/**
	 * A small sign at the bottom while tiles on screen are being drawn or the regions under them read, with
	 * how much is done, so a blank or blurry part of the map reads as on its way rather than missing. It
	 * shows only after a moment of loading and stays until loading has stopped for a moment, so short bursts
	 * don't make it flash. Fades in and out.
	 */
	private void drawLoading(GuiGraphicsExtractor context, UiTheme theme, float panels) {
		long now = System.currentTimeMillis();
		// Saving a picture of the map (#225) shows here too, with how far it got.
		boolean exporting = MapExport.running();
		boolean loading = tiles.busy() || exporting;
		if (loading) {
			idleSince = -1L;
			if (busySince < 0L) {
				busySince = now;
			}
		} else if (idleSince < 0L) {
			idleSince = now;
		}
		boolean show = busySince >= 0L && now - busySince >= LOADING_SHOW_AFTER_MILLIS;
		if (!loading && idleSince >= 0L && now - idleSince >= LOADING_HIDE_AFTER_MILLIS) {
			busySince = -1L;
			show = false;
		}
		float progress = exporting ? MapExport.progress() : loading ? tiles.progress() : 1.0F;
		// Counts up smoothly while shown; while hidden it keeps up at once, so it never shows from zero.
		float shownProgress = anim.towards("world-map-loading-progress", progress, show ? 8.0F : 1000.0F);
		float shown = anim.towards("world-map-loading", show && panels > 0.0F ? 1.0F : 0.0F, show ? 6.0F : 3.0F) * panels;
		if (shown <= 0.01F) {
			return;
		}
		String sign = exporting ? EMUtilsTexts.WORLD_MAP_EXPORTING : EMUtilsTexts.WORLD_MAP_LOADING;
		Component text = Component.translatable(sign, Math.round(Math.clamp(shownProgress, 0.0F, 1.0F) * 100.0F) + "%");
		// Sized for the widest percentage, so the sign doesn't change width as it counts.
		int textWidth = UiText.width(font, Component.translatable(sign, "100%"), UiText.Size.SMALL);
		int pillWidth = 10 + SPINNER_SIZE + 6 + textWidth + 12;
		int pillHeight = 20;
		int x = (width - pillWidth) / 2;
		// Just above the bottom panels, which can reach the middle on a narrow screen.
		int y = height - MARGIN - PANEL_HEIGHT - 8 - pillHeight;
		UiOpacity.set(shown);
		UiShapes.shadow(context, x, y, pillWidth, pillHeight, pillHeight / 2, 8, theme.shadow());
		UiShapes.borderedRect(context, x, y, pillWidth, pillHeight, pillHeight / 2, UiTheme.fade(theme.panel(), 0.94F), theme.line());
		context.pose().pushMatrix();
		context.pose().translate(x + 10 + SPINNER_SIZE / 2.0F, y + pillHeight / 2.0F);
		context.pose().rotate((float) ((System.nanoTime() / 1.0E9D * Math.PI * 2.0D) % (Math.PI * 2.0D)));
		UiIcons.draw(context, HubIcons.REFRESH_CW, -SPINNER_SIZE / 2, -SPINNER_SIZE / 2, SPINNER_SIZE, theme.textSecondary());
		context.pose().popMatrix();
		UiText.drawCentered(context, font, text, UiText.Size.SMALL, x + 10 + SPINNER_SIZE + 6, y + pillHeight / 2, theme.textSecondary());
		UiOpacity.reset();
	}

	private void drawWaypoints(GuiGraphicsExtractor context, MapView view, int mouseX, int mouseY, float progress, boolean hover) {
		waypointsDrawn = 0;
		WaypointManager manager = EMUtilsClient.waypoint();
		if (manager == null || !manager.enabled()) {
			return;
		}
		for (WaypointEntry entry : manager.renderEntries(minecraft, dimension, worldId)) {
			Waypoint waypoint = entry.waypoint();
			if (!entry.placeable()) {
				continue;
			}
			float sx = view.screenX(entry.renderX(), entry.renderZ());
			float sy = view.screenY(entry.renderX(), entry.renderZ());
			if (sx < -MARKER_SIZE || sy < -MARKER_SIZE || sx > width + MARKER_SIZE || sy > height + MARKER_SIZE) {
				continue;
			}
			context.pose().pushMatrix();
			context.pose().translate(sx, sy);
			// Hidden waypoints stay on the world map, faded, so they can be shown again from its menu.
			WaypointMarkerRenderer.drawMapMarker(context, waypoint, MARKER_SIZE, waypoint.hidden() ? progress * HIDDEN_ALPHA : progress);
			context.pose().popMatrix();
			waypointsDrawn++;
			if (hover && Math.abs(mouseX - sx) <= MARKER_SIZE / 2.0F && Math.abs(mouseY - sy) <= MARKER_SIZE / 2.0F) {
				hovered = entry;
			}
		}
	}

	/**
	 * The players, mobs and items around you (#224) inside the map's frame; mobs and items only while zoomed in
	 * far enough for them not to crowd around you.
	 */
	private void drawRadar(GuiGraphicsExtractor context, MapView view, float x, float y, float w, float h, int mouseX, int mouseY, float progress, float delta, boolean hover) {
		LocalPlayer player = minecraft.player;
		EMUtilsConfig config = EMUtilsClient.config();
		if (player == null || !MapRadar.enabled(config)) {
			return;
		}
		double reach = Math.max(w, h) / view.zoom();
		double playerY = player.yo + (player.getY() - player.yo) * delta;
		boolean mobs = view.zoom() >= RADAR_MOBS_ZOOM;
		float nearest = RADAR_HOVER;
		for (MapRadar.Blip blip : MapRadar.blips(minecraft, view.centerX(), view.centerZ(), reach, delta)) {
			boolean face = blip.kind() == MapRadar.Kind.PLAYER;
			if (!face && !mobs) {
				continue;
			}
			float sx = view.screenX(blip.x(), blip.z());
			float sy = view.screenY(blip.x(), blip.z());
			if (sx < x || sy < y || sx > x + w || sy > y + h) {
				continue;
			}
			context.pose().pushMatrix();
			context.pose().translate(sx, sy);
			MapRadar.draw(context, font, blip, playerY, RADAR_FACE, RADAR_DOT_SCALE, config.mapRadarNames(), progress);
			context.pose().popMatrix();
			float distance = Math.max(Math.abs(mouseX - sx), Math.abs(mouseY - sy));
			if (hover && distance <= nearest) {
				nearest = distance;
				hoveredBlip = blip;
			}
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

	/**
	 * Floating panels in the menus' look: the title with the dimension chips at the top left, the coordinates
	 * under the cursor at the bottom left, and what the mouse does at the bottom right. {@code slide} moves
	 * them towards the screen's edge while they come in.
	 */
	private void drawPanels(GuiGraphicsExtractor context, UiTheme theme, MapView view, int mouseX, int mouseY, boolean ownDimension, float slide, boolean interactive) {
		panels.clear();
		dimensionChips.clear();
		int panelColor = UiTheme.fade(theme.panel(), 0.94F);

		// Top left: the title and a chip per explored dimension.
		int titleWidth = UiText.width(font, title, UiText.Size.LABEL);
		int chipsWidth = 0;
		List<Component> labels = new ArrayList<>();
		for (String id : dimensions) {
			Component label = Component.literal(dimensionName(id));
			labels.add(label);
			chipsWidth += UiText.width(font, label, UiText.Size.SMALL) + 16;
		}
		chipsWidth += Math.max(0, labels.size() - 1) * 2;
		int topWidth = 10 + titleWidth + 10 + chipsWidth + (PANEL_HEIGHT - CHIP_HEIGHT) / 2;
		context.pose().pushMatrix();
		context.pose().translate(0.0F, -slide);
		drawPanel(context, theme, MARGIN, MARGIN, topWidth, panelColor);
		UiText.drawCentered(context, font, title, UiText.Size.LABEL, MARGIN + 10, MARGIN + PANEL_HEIGHT / 2, theme.text());
		int chipX = MARGIN + 10 + titleWidth + 10;
		int chipY = MARGIN + (PANEL_HEIGHT - CHIP_HEIGHT) / 2;
		for (int i = 0; i < labels.size(); i++) {
			Component label = labels.get(i);
			int labelWidth = UiText.width(font, label, UiText.Size.SMALL);
			int chipWidth = labelWidth + 16;
			boolean selected = dimensions.get(i).equals(dimension);
			boolean hover = interactive && contains(mouseX, mouseY, chipX, chipY, chipWidth, CHIP_HEIGHT);
			if (selected || hover) {
				UiShapes.roundedRect(context, chipX, chipY, chipWidth, CHIP_HEIGHT, CHIP_HEIGHT / 2, selected ? theme.accent() : theme.hover());
			}
			int textColor = selected ? 0xFFFFFFFF : hover ? theme.text() : theme.textSecondary();
			UiText.drawCentered(context, font, label, UiText.Size.SMALL, chipX + (chipWidth - labelWidth) / 2, chipY + CHIP_HEIGHT / 2, textColor);
			dimensionChips.add(new int[] {chipX, chipWidth});
			chipX += chipWidth + 2;
		}
		context.pose().popMatrix();
		panels.add(new int[] {MARGIN, MARGIN, topWidth, PANEL_HEIGHT});
		drawWorldPanel(context, theme, mouseX, mouseY, panelColor, slide, interactive);

		// Bottom left: where the cursor points, with the ground's height when the map knows it.
		int blockX = (int) Math.floor(view.worldX(mouseX, mouseY));
		int blockZ = (int) Math.floor(view.worldZ(mouseX, mouseY));
		MapChunk chunk = world.chunk(blockX >> 4, blockZ >> 4);
		Component position = Component.literal(chunk == null || chunk.top(MapChunk.index(blockX & 15, blockZ & 15)) == MapChunk.NONE
			? blockX + ", " + blockZ
			: blockX + ", " + chunk.topY(MapChunk.index(blockX & 15, blockZ & 15)) + ", " + blockZ);
		int positionWidth = UiText.width(font, position, UiText.Size.LABEL) + 20;
		int bottomY = height - MARGIN - PANEL_HEIGHT;
		context.pose().pushMatrix();
		context.pose().translate(0.0F, slide);
		drawPanel(context, theme, MARGIN, bottomY, positionWidth, panelColor);
		UiText.drawCentered(context, font, position, UiText.Size.LABEL, MARGIN + 10, bottomY + PANEL_HEIGHT / 2, theme.text());

		// Bottom right: what the mouse does.
		Component hint = Component.translatable(ownDimension ? EMUtilsTexts.WORLD_MAP_HINT : EMUtilsTexts.WORLD_MAP_HINT_OTHER);
		int hintWidth = UiText.width(font, hint, UiText.Size.SMALL) + 20;
		int hintX = width - MARGIN - hintWidth;
		drawPanel(context, theme, hintX, bottomY, hintWidth, panelColor);
		UiText.drawCentered(context, font, hint, UiText.Size.SMALL, hintX + 10, bottomY + PANEL_HEIGHT / 2, theme.textSecondary());
		context.pose().popMatrix();
		panels.add(new int[] {MARGIN, bottomY, positionWidth, PANEL_HEIGHT});
		panels.add(new int[] {hintX, bottomY, hintWidth, PANEL_HEIGHT});
	}

	/**
	 * Under the title: which of the server's worlds is shown (#219), when there's a choice, and which layer
	 * (#222), each a chip that opens a menu.
	 */
	private void drawWorldPanel(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY, int panelColor, float slide, boolean interactive) {
		worldChip = new int[0];
		layerChip = new int[0];
		MapWorlds catalog = MapManager.worlds(dimension);
		MapWorlds.Entry entry = catalog == null ? null : catalog.get(worldId);
		boolean worlds = entry != null && (minecraft.getSingleplayerServer() == null || catalog.worlds().size() > 1);
		Component worldLabel = worlds ? Component.literal(entry.name()) : null;
		Component layerLabel = Component.literal(layerName(cave));
		int y = MARGIN + PANEL_HEIGHT + 4;
		int chipY = y + (PANEL_HEIGHT - CHIP_HEIGHT) / 2;
		int pad = (PANEL_HEIGHT - CHIP_HEIGHT) / 2;
		int x = MARGIN + pad;
		int panelWidth = pad;
		if (worldLabel != null) {
			panelWidth += UiText.width(font, worldLabel, UiText.Size.SMALL) + 16 + CHEVRON_SIZE + 4 + 2;
		}
		panelWidth += UiText.width(font, layerLabel, UiText.Size.SMALL) + 16 + CHEVRON_SIZE + 4 + pad;
		context.pose().pushMatrix();
		context.pose().translate(0.0F, -slide);
		drawPanel(context, theme, MARGIN, y, panelWidth, panelColor);
		if (worldLabel != null) {
			worldChip = drawChip(context, theme, worldLabel, x, chipY, mouseX, mouseY, interactive);
			x += worldChip[2] + 2;
		}
		layerChip = drawChip(context, theme, layerLabel, x, chipY, mouseX, mouseY, interactive);
		context.pose().popMatrix();
		panels.add(new int[] {MARGIN, y, panelWidth, PANEL_HEIGHT});
	}

	private int[] drawChip(GuiGraphicsExtractor context, UiTheme theme, Component label, int x, int y, int mouseX, int mouseY, boolean interactive) {
		int labelWidth = UiText.width(font, label, UiText.Size.SMALL);
		int chipWidth = labelWidth + 16 + CHEVRON_SIZE + 4;
		boolean hover = interactive && contains(mouseX, mouseY, x, y, chipWidth, CHIP_HEIGHT);
		if (hover) {
			UiShapes.roundedRect(context, x, y, chipWidth, CHIP_HEIGHT, CHIP_HEIGHT / 2, theme.hover());
		}
		int color = hover ? theme.text() : theme.textSecondary();
		UiText.drawCentered(context, font, label, UiText.Size.SMALL, x + 8, y + CHIP_HEIGHT / 2, color);
		UiIcons.draw(context, HubIcons.CHEVRON_DOWN, x + 8 + labelWidth + 4, y + (CHIP_HEIGHT - CHEVRON_SIZE) / 2, CHEVRON_SIZE, color);
		return new int[] {x, y, chipWidth, CHIP_HEIGHT};
	}

	/**
	 * "Surface" or "Underground"; in a dimension with a ceiling, which has no surface, "Full" (every floor under
	 * the roof, like Xaero's full cave mode) or "Your Height".
	 */
	private String layerName(int layer) {
		if (hasCeiling(dimension)) {
			return Component.translatable(layer == MapSampler.SURFACE ? EMUtilsTexts.WORLD_MAP_FULL : EMUtilsTexts.WORLD_MAP_YOUR_HEIGHT).getString();
		}
		return Component.translatable(layer == MapSampler.SURFACE ? EMUtilsTexts.WORLD_MAP_SURFACE : EMUtilsTexts.WORLD_MAP_UNDERGROUND).getString();
	}

	/**
	 * The cave layer Underground shows: the one you're in when you're underground in the dimension shown, else
	 * the one you were last in there, else the one at your height.
	 */
	private int undergroundLayer(String shownDimension) {
		MapWorld liveCave = MapManager.cave();
		if (liveCave != null && liveCave.dimension().equals(shownDimension)) {
			return liveCave.cave;
		}
		return MapManager.lastLayer(shownDimension, playerLayer());
	}

	/** Whether a dimension has a ceiling, like the Nether, so it has no surface to map (#221). */
	private boolean hasCeiling(String shownDimension) {
		if (minecraft.level != null && shownDimension.equals(WaypointManager.dimensionId(minecraft.level))) {
			return minecraft.level.dimensionType().hasCeiling();
		}
		Identifier id = Identifier.tryParse(shownDimension);
		ServerLevel server = id == null ? null : MapImporter.serverLevel(minecraft, ResourceKey.create(Registries.DIMENSION, id));
		return server != null ? server.dimensionType().hasCeiling() : shownDimension.equals("minecraft:the_nether");
	}

	/** The cave layer at your height. */
	private int playerLayer() {
		return minecraft.player == null ? 4 : Math.floorDiv(minecraft.player.getBlockY() + 2, MapWorld.LAYER_BLOCKS);
	}

	/** The menu of the world chip: the dimension's worlds, and what can be done with the one shown (#219). */
	private void openWorldMenu() {
		MapWorlds catalog = MapManager.worlds(dimension);
		if (catalog == null || worldChip.length == 0) {
			return;
		}
		MapWorld surface = MapManager.world();
		String live = surface != null && surface.dimension().equals(dimension) ? surface.worldId : null;
		List<UiContextMenu.Item> items = new ArrayList<>();
		for (MapWorlds.Entry entry : catalog.worlds()) {
			String name = entry.name() + (entry.id().equals(live) ? "  " + Component.translatable(EMUtilsTexts.WORLD_MAP_HERE).getString() : "");
			items.add(UiContextMenu.Item.choice(Component.literal(name), entry.id().equals(worldId), () -> show(dimension, entry.id(), cave)));
		}
		String shown = worldId;
		MapWorlds.Entry shownEntry = catalog.get(shown);
		if (shownEntry != null) {
			items.add(UiContextMenu.Item.of(Component.translatable(EMUtilsTexts.WORLD_MAP_RENAME_WORLD), () -> prompt = new UiPromptDialog(
				font, anim,
				Component.translatable(EMUtilsTexts.WORLD_MAP_RENAME_WORLD),
				Component.translatable(EMUtilsTexts.WORLD_MAP_RENAME_MESSAGE),
				shownEntry.name(),
				Component.translatable(EMUtilsTexts.WORLD_MAP_RENAME_PLACEHOLDER),
				Component.translatable(EMUtilsTexts.WORLD_MAP_RENAME_CONFIRM),
				name -> {
					if (name.isBlank()) {
						return Component.translatable(EMUtilsTexts.WORLD_MAP_RENAME_EMPTY);
					}
					catalog.rename(shownEntry.id(), name);
					return null;
				}
			)));
		}
		if (isOwnDimension() && surface != null && surface.worldId != null) {
			if (shown != null && !shown.equals(live)) {
				items.add(UiContextMenu.Item.of(Component.translatable(EMUtilsTexts.WORLD_MAP_THIS_WORLD), () -> {
					MapManager.useWorld(shown);
					showLive();
				}));
			} else {
				items.add(UiContextMenu.Item.of(Component.translatable(EMUtilsTexts.WORLD_MAP_NEW_WORLD), () -> {
					MapManager.useWorld(null);
					showLive();
				}));
			}
		}
		if (shownEntry != null && !shownEntry.id().equals(live)) {
			int x = worldChip[0];
			int y = worldChip[1] + worldChip[3] + 2;
			items.add(new UiContextMenu.Item(Component.translatable(EMUtilsTexts.WORLD_MAP_DELETE_WORLD), true, true, () -> menu = new UiContextMenu(font, anim, x, y, List.of(
				new UiContextMenu.Item(Component.translatable(EMUtilsTexts.WORLD_MAP_CONFIRM_DELETE_WORLD, shownEntry.name()), true, true, () -> {
					closeOther();
					MapManager.deleteWorld(catalog, shownEntry.id());
					showLive();
				})
			))));
		}
		menu = new UiContextMenu(font, anim, worldChip[0], worldChip[1] + worldChip[3] + 2, items);
	}

	/** The menu of the layer chip: the surface or underground (#222). */
	private void openLayerMenu() {
		if (layerChip.length == 0) {
			return;
		}
		List<UiContextMenu.Item> items = new ArrayList<>();
		items.add(UiContextMenu.Item.choice(Component.literal(layerName(MapSampler.SURFACE)), cave == MapSampler.SURFACE, () -> show(dimension, worldId, MapSampler.SURFACE)));
		items.add(UiContextMenu.Item.choice(Component.literal(layerName(0)), cave != MapSampler.SURFACE, () -> show(dimension, worldId, undergroundLayer(dimension))));
		menu = new UiContextMenu(font, anim, layerChip[0], layerChip[1] + layerChip[3] + 2, items);
	}

	/** Back to the map of where you are. */
	private void showLive() {
		MapWorld live = MapManager.shownWorld();
		if (live != null) {
			keepFading();
			use(live, MapManager.shownTiles(), null);
		}
	}

	private static void drawPanel(GuiGraphicsExtractor context, UiTheme theme, int x, int y, int width, int color) {
		UiShapes.shadow(context, x, y, width, PANEL_HEIGHT, PANEL_RADIUS, 10, theme.shadow());
		UiShapes.borderedRect(context, x, y, width, PANEL_HEIGHT, PANEL_RADIUS, color, theme.line());
	}

	private static String dimensionName(String id) {
		return switch (id) {
			case "minecraft:overworld" -> Component.translatable(EMUtilsTexts.WORLD_MAP_OVERWORLD).getString();
			case "minecraft:the_nether" -> Component.translatable(EMUtilsTexts.WORLD_MAP_NETHER).getString();
			case "minecraft:the_end" -> Component.translatable(EMUtilsTexts.WORLD_MAP_END).getString();
			default -> id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
		};
	}

	private boolean overPanel(double mouseX, double mouseY) {
		for (int[] panel : panels) {
			if (contains(mouseX, mouseY, panel[0], panel[1], panel[2], panel[3])) {
				return true;
			}
		}
		return false;
	}

	private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	// ---- input ----------------------------------------------------------------------------------

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		if (closingAt >= 0L || progress() < 1.0F) {
			return true;
		}
		double mouseX = click.x();
		double mouseY = click.y();
		boolean left = click.button() == InputConstants.MOUSE_BUTTON_LEFT;
		if (sheet != null) {
			if (left) {
				sheet.mouseClicked(mouseX, mouseY);
			}
			return true;
		}
		if (prompt != null) {
			if (left && !prompt.closing()) {
				prompt.mouseClicked(mouseX, mouseY, click.hasShiftDown());
			}
			return true;
		}
		if (menu != null) {
			// Cleared before the item runs, since an item may open a menu of its own (deleting asks first).
			UiContextMenu clicked = menu;
			menu = null;
			boolean onMenu = clicked.contains(mouseX, mouseY);
			clicked.mouseClicked(mouseX, mouseY);
			// A click beside the menu only closes it.
			if (onMenu || !left) {
				return true;
			}
		}
		if (overPanel(mouseX, mouseY)) {
			if (left && worldChip.length > 0 && contains(mouseX, mouseY, worldChip[0], worldChip[1], worldChip[2], worldChip[3])) {
				openWorldMenu();
				return true;
			}
			if (left && layerChip.length > 0 && contains(mouseX, mouseY, layerChip[0], layerChip[1], layerChip[2], layerChip[3])) {
				openLayerMenu();
				return true;
			}
			if (left) {
				int chipY = MARGIN + (PANEL_HEIGHT - CHIP_HEIGHT) / 2;
				for (int i = 0; i < dimensionChips.size(); i++) {
					int[] chip = dimensionChips.get(i);
					if (contains(mouseX, mouseY, chip[0], chipY, chip[1], CHIP_HEIGHT)) {
						switchDimension(dimensions.get(i));
						break;
					}
				}
			}
			return true;
		}
		if (left) {
			if (hovered != null) {
				openSheet(hovered.waypoint(), null);
				return true;
			}
			dragging = true;
			return true;
		}
		if (click.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
			openMenu((int) mouseX, (int) mouseY);
			return true;
		}
		return super.mouseClicked(click, doubled);
	}

	/** The right-click menu, for the waypoint under the cursor or the spot on the map. */
	private void openMenu(int mouseX, int mouseY) {
		List<UiContextMenu.Item> items = new ArrayList<>();
		boolean teleport = canTeleport();
		WaypointEntry entry = hovered;
		if (entry != null) {
			String id = entry.waypoint().id();
			boolean hidden = entry.waypoint().hidden();
			items.add(UiContextMenu.Item.of(Component.translatable(EMUtilsTexts.WORLD_MAP_EDIT_WAYPOINT), () -> openSheet(entry.waypoint(), null)));
			items.add(new UiContextMenu.Item(Component.translatable(EMUtilsTexts.WORLD_MAP_TELEPORT_WAYPOINT), teleport, false, () -> teleport(entry.x(), entry.y(), entry.z())));
			items.add(UiContextMenu.Item.of(Component.translatable(EMUtilsTexts.WORLD_MAP_SHARE), () -> EMUtilsClient.waypoint().shareInChat(minecraft, id)));
			items.add(UiContextMenu.Item.of(Component.translatable(EMUtilsTexts.WORLD_MAP_COPY_COORDINATES), () -> EMUtilsClient.waypoint().copyCoordinates(minecraft, entry.x(), entry.y(), entry.z())));
			items.add(UiContextMenu.Item.of(Component.translatable(hidden ? EMUtilsTexts.WORLD_MAP_SHOW_WAYPOINT : EMUtilsTexts.WORLD_MAP_HIDE_WAYPOINT), () -> EMUtilsClient.waypoint().toggleHidden(id)));
			// Deleting asks once more in a menu of its own at the same spot, as it can't be undone.
			items.add(new UiContextMenu.Item(Component.translatable(EMUtilsTexts.UI_DELETE), true, true, () -> menu = new UiContextMenu(font, anim, mouseX, mouseY, List.of(
				new UiContextMenu.Item(Component.translatable(EMUtilsTexts.WORLD_MAP_CONFIRM_DELETE), true, true, () -> EMUtilsClient.waypoint().clear(minecraft, id))
			))));
		} else {
			MapView view = fullView();
			int blockX = (int) Math.floor(view.worldX(mouseX, mouseY));
			int blockZ = (int) Math.floor(view.worldZ(mouseX, mouseY));
			int blockY = groundY(blockX, blockZ);
			String other = isOwnDimension() ? null : dimension;
			items.add(UiContextMenu.Item.of(Component.translatable(EMUtilsTexts.WORLD_MAP_ADD_WAYPOINT), () -> openSheet(null, new SharedWaypoint(null, blockX, blockY, blockZ, other, null))));
			items.add(new UiContextMenu.Item(Component.translatable(EMUtilsTexts.WORLD_MAP_TELEPORT), teleport, false, () -> teleport(blockX, blockY, blockZ)));
			items.add(UiContextMenu.Item.of(Component.translatable(EMUtilsTexts.WORLD_MAP_SHARE_LOCATION), () -> EMUtilsClient.waypoint().shareLocation(minecraft, Component.translatable(EMUtilsTexts.WORLD_MAP_LOCATION).getString(), blockX, blockY, blockZ, dimension)));
			items.add(UiContextMenu.Item.of(Component.translatable(EMUtilsTexts.WORLD_MAP_COPY_COORDINATES), () -> EMUtilsClient.waypoint().copyCoordinates(minecraft, blockX, blockY, blockZ)));
			items.add(UiContextMenu.Item.of(Component.translatable(EMUtilsTexts.WORLD_MAP_OPEN_WAYPOINTS), () -> minecraft.gui.setScreen(new WaypointsScreen(this))));
			items.add(new UiContextMenu.Item(Component.translatable(EMUtilsTexts.WORLD_MAP_EXPORT), !MapExport.running(), false, () -> MapExport.start(minecraft, world)));
			items.add(UiContextMenu.Item.of(Component.translatable(EMUtilsTexts.WORLD_MAP_OPEN_SETTINGS), () -> {
				SettingsScreen settings = new SettingsScreen(this);
				minecraft.gui.setScreen(settings);
				settings.openSheet("minimap");
			}));
		}
		menu = new UiContextMenu(font, anim, mouseX, mouseY, items);
	}

	/** The waypoint sheet over the map: editing {@code editing}, or adding one at {@code prefill}. */
	private void openSheet(@Nullable Waypoint editing, @Nullable SharedWaypoint prefill) {
		menu = null;
		dragging = false;
		sheet = new WaypointSheet(font, anim, editing, prefill, saved -> { });
	}

	/** Standing height at a spot: one above the ground the map knows, or your own height when it doesn't. */
	private int groundY(int blockX, int blockZ) {
		MapChunk chunk = world.chunk(blockX >> 4, blockZ >> 4);
		if (chunk != null && chunk.top(MapChunk.index(blockX & 15, blockZ & 15)) != MapChunk.NONE) {
			return chunk.topY(MapChunk.index(blockX & 15, blockZ & 15)) + 1;
		}
		return minecraft.player == null ? 64 : minecraft.player.getBlockY();
	}

	/** The server sends only the commands you may use, so /tp being among them means you can teleport. */
	private boolean canTeleport() {
		ClientPacketListener connection = minecraft.getConnection();
		return connection != null && connection.getCommands().getRoot().getChild("tp") != null;
	}

	private void teleport(int x, int y, int z) {
		ClientPacketListener connection = minecraft.getConnection();
		if (connection == null) {
			return;
		}
		String tp = "tp @s " + x + " " + y + " " + z;
		connection.sendCommand(isOwnDimension() ? tp : "execute in " + dimension + " run " + tp);
		closingAt = System.nanoTime();
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		dragging = false;
		if (sheet != null) {
			sheet.mouseReleased();
			return true;
		}
		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
		if (sheet != null) {
			sheet.mouseDragged(click.x(), click.y());
			return true;
		}
		if (dragging) {
			centerX -= dx / zoom;
			centerZ -= dy / zoom;
			anchorWorldX -= dx / zoom;
			anchorWorldZ -= dy / zoom;
			return true;
		}
		return super.mouseDragged(click, dx, dy);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY == 0.0D || closingAt >= 0L || sheet != null) {
			return true;
		}
		menu = null;
		// The block under the cursor stays under it while the zoom glides there.
		MapView before = fullView();
		anchorWorldX = before.worldX((float) mouseX, (float) mouseY);
		anchorWorldZ = before.worldZ((float) mouseX, (float) mouseY);
		anchorScreenX = (float) mouseX;
		anchorScreenY = (float) mouseY;
		targetZoom = Math.clamp(targetZoom * (float) Math.pow(ZOOM_STEP, scrollY), MIN_ZOOM, MAX_ZOOM);
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (sheet != null) {
			sheet.keyPressed(event);
			return true;
		}
		if (prompt != null) {
			if (!prompt.closing()) {
				prompt.keyPressed(event);
			}
			return true;
		}
		if (openKey != null && openKey.matches(event)) {
			menu = null;
			onClose();
			return true;
		}
		if (event.key() == InputConstants.KEY_SPACE && minecraft.player != null && isOwnDimension()) {
			lookAt(minecraft.player.getX(), minecraft.player.getZ());
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (sheet != null) {
			sheet.charTyped(event);
			return true;
		}
		if (prompt != null) {
			if (!prompt.closing()) {
				prompt.charTyped(event);
			}
			return true;
		}
		return super.charTyped(event);
	}

	/** Puts a spot in the middle of the map, also for a zoom still gliding. */
	private void lookAt(double x, double z) {
		centerX = x;
		centerZ = z;
		anchorWorldX = x;
		anchorWorldZ = z;
		anchorScreenX = width / 2.0F;
		anchorScreenY = height / 2.0F;
	}

	/** For UI snapshot checks: looks at a spot. */
	public void centerForSnapshot(double x, double z) {
		lookAt(x, z);
	}

	/** For UI snapshot checks: zooms by scroll steps around the middle of the screen, at once instead of gliding. */
	public void scrollForSnapshot(double steps) {
		mouseScrolled(width / 2.0D, height / 2.0D, 0.0D, steps);
		zoom = targetZoom;
		lookAt(anchorWorldX, anchorWorldZ);
	}

	/** For UI snapshot checks: shows another dimension's map, as clicking its chip does. */
	public void switchDimensionForSnapshot(String id) {
		switchDimension(id);
	}

	/** For UI snapshot checks: starts saving the map shown as an image, as its menu item does. */
	public boolean exportForSnapshot() {
		return MapExport.start(minecraft, world);
	}

	/** For UI snapshot checks: opens the world chip's menu, or the layer chip's. */
	public void openChipMenuForSnapshot(boolean worlds) {
		if (worlds) {
			openWorldMenu();
		} else {
			openLayerMenu();
		}
	}

	/** For UI snapshot checks: how many waypoints the map drew last frame. */
	public int waypointsDrawnForSnapshot() {
		return waypointsDrawn;
	}

	/** For UI snapshot checks: whether the map shown is still reading regions or drawing tiles on screen. */
	public boolean loadingForSnapshot() {
		return world.busy() || tiles.busy();
	}

	/** For UI snapshot checks: the map's zoom in GUI pixels per block. */
	public float zoomForSnapshot() {
		return zoom;
	}

	/** For UI snapshot checks: opens the right-click menu at a point, as a right-click there would. */
	public void openMenuForSnapshot(int x, int y) {
		openMenu(x, y);
	}

	/** For UI snapshot checks: opens the add sheet at the middle of the map, as the menu's first item does. */
	public void addWaypointForSnapshot() {
		MapView view = fullView();
		int blockX = (int) Math.floor(view.worldX(width / 2.0F, height / 2.0F));
		int blockZ = (int) Math.floor(view.worldZ(width / 2.0F, height / 2.0F));
		openSheet(null, new SharedWaypoint(null, blockX, groundY(blockX, blockZ), blockZ, null, null));
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
		String previous = dimension;
		boolean own = id.equals(WaypointManager.dimensionId(minecraft.level));
		if (own) {
			// Back to where you are: the map the minimap shows.
			showLive();
			if (minecraft.player != null) {
				lookAt(minecraft.player.getX(), minecraft.player.getZ());
			}
			return;
		}
		MapWorlds catalog = MapManager.worlds(id);
		MapWorlds.Entry latest = catalog == null ? null : catalog.latest();
		// The Nether opens at the height you were last at there, unless the full map is chosen for it.
		boolean underground = hasCeiling(id) && EMUtilsClient.config().mapCaves() && !EMUtilsClient.config().mapCeilingFull();
		if (!show(id, latest == null ? null : latest.id(), underground ? MapManager.lastLayer(id, 4) : MapSampler.SURFACE)) {
			return;
		}
		// The Nether is an eighth the size of the Overworld, so the view moves with the scale between them.
		boolean toNether = id.equals("minecraft:the_nether");
		boolean fromNether = previous.equals("minecraft:the_nether");
		if (toNether && !fromNether) {
			lookAt(centerX / 8.0D, centerZ / 8.0D);
		} else if (fromNether && !toNether) {
			lookAt(centerX * 8.0D, centerZ * 8.0D);
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
