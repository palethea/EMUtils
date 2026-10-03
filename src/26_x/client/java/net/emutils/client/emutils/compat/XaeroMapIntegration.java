package net.emutils.client.emutils.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * Puts the Beacon Radius Outline (#188) and EMUtils waypoints (#185) on Xaero's Minimap and World Map. Nothing is
 * attached, built or looked up for a feature while it or its Xaero setting is off; once attached, the handlers
 * are only looked up again every second, with the reflection handles kept.
 */
public final class XaeroMapIntegration {
	private static final boolean MINIMAP_LOADED = FabricLoader.getInstance().isModLoaded("xaerominimap");
	private static final boolean WORLD_MAP_LOADED = FabricLoader.getInstance().isModLoaded("xaeroworldmap");
	/** How often an attached handler is checked for still being the current one. */
	private static final int RECHECK_TICKS = 20;
	/** How long to wait before trying again after Xaero's classes weren't there or didn't match. */
	private static final int RETRY_TICKS = 200;

	/** One kind of element EMUtils puts on the maps, with the handlers it has been attached to. */
	private static final class Attachment {
		final String name;
		final BooleanSupplier wanted;
		final Supplier<Object> renderer;
		final int order;
		final Set<Object> minimapHandlers = Collections.newSetFromMap(new IdentityHashMap<>());
		final Set<Object> worldMapHandlers = Collections.newSetFromMap(new IdentityHashMap<>());
		boolean minimapFailureLogged;
		boolean worldMapFailureLogged;

		Attachment(String name, BooleanSupplier wanted, Supplier<Object> renderer, int order) {
			this.name = name;
			this.wanted = wanted;
			this.renderer = renderer;
			this.order = order;
		}
	}

	private static final Attachment BEACONS = new Attachment("beacon boundaries", XaeroMapIntegration::beaconsWanted, XaeroMapIntegration::newBeaconRenderer, -100);
	private static final Attachment WAYPOINTS = new Attachment("waypoints", XaeroMapIntegration::waypointsWanted, XaeroMapIntegration::newWaypointRenderer, 90);
	private static final Attachment[] ATTACHMENTS = {BEACONS, WAYPOINTS};
	private static int cooldown;

	// Looked up once.
	private static @Nullable Class<?> hudModClass;
	private static @Nullable Field hudModInstance;
	private static @Nullable Method getMinimap;
	private static @Nullable Method getOverMapHandler;
	private static @Nullable Class<?> minimapRendererClass;
	private static @Nullable Field worldMapHandlerField;

	/** The handler beacons were attached to, and the fields of it that say what the minimap shows. */
	private static @Nullable Object overMapHandler;
	private static @Nullable Field[] viewFields;
	private static final String[] VIEW_FIELD_NAMES = {"zoom", "ps", "pc", "halfViewW", "halfViewH", "specW", "specH", "circle"};
	private static final View VIEW = new View();

	/**
	 * What the minimap shows, in the units its elements are placed in: an element {@code (dx, dz)} blocks from the
	 * map's center is {@code zoom * (ps * dx - pc * dz)} across and {@code zoom * (pc * dx + ps * dz)} down, and is
	 * on the map within {@code spec}. Read from Xaero's handler.
	 */
	public static final class View {
		public double zoom;
		public double ps;
		public double pc;
		public int halfViewWidth;
		public int halfViewHeight;
		public int specWidth;
		public int specHeight;
		public boolean circle;
	}

	private XaeroMapIntegration() {
	}

	// The renderers extend Xaero's classes, so they are only created, and loaded, once Xaero is known to be there.
	private static Object newBeaconRenderer() {
		return new BeaconXaeroElementRenderer();
	}

	private static Object newWaypointRenderer() {
		return new WaypointXaeroElementRenderer();
	}

	/** Whether the outline should be on Xaero's maps: Xaero is installed, and the outline and its Xaero setting are on. */
	public static boolean beaconsWanted() {
		EMUtilsConfig config = EMUtilsClient.config();
		return MINIMAP_LOADED && config != null && config.beaconRadiusOutline() && config.beaconRadiusXaero();
	}

	/** Whether waypoints should be on Xaero's maps: Xaero is installed, and waypoints and their Xaero setting are on. */
	public static boolean waypointsWanted() {
		EMUtilsConfig config = EMUtilsClient.config();
		return MINIMAP_LOADED && config != null && config.waypointEnabled() && config.waypointXaero();
	}

	public static void tick() {
		if (!MINIMAP_LOADED) {
			return;
		}
		boolean any = false;
		for (Attachment attachment : ATTACHMENTS) {
			any |= attachment.wanted.getAsBoolean();
		}
		if (!any || --cooldown > 0) {
			return;
		}

		boolean attached = true;
		for (Attachment attachment : ATTACHMENTS) {
			if (!attachment.wanted.getAsBoolean()) {
				continue;
			}
			attached &= attachMinimapHandlers(attachment);
			if (WORLD_MAP_LOADED) {
				attached &= attachWorldMapHandler(attachment);
			}
		}
		cooldown = attached ? RECHECK_TICKS : RETRY_TICKS;
	}

	/** Whether waypoints have been added to Xaero's minimap and, if it is installed, its World Map; for UI snapshots. */
	public static boolean waypointsAttachedForSnapshot() {
		return !WAYPOINTS.minimapHandlers.isEmpty() && (!WORLD_MAP_LOADED || !WAYPOINTS.worldMapHandlers.isEmpty());
	}

	/** Xaero's World Map screen as its keybind opens it, or null when it isn't installed or can't be built; for UI snapshots. */
	public static @Nullable Screen worldMapScreenForSnapshot(Minecraft client, @Nullable Screen parent) {
		if (!WORLD_MAP_LOADED || client.player == null) {
			return null;
		}
		try {
			Class<?> sessionClass = Class.forName("xaero.map.WorldMapSession");
			Object session = sessionClass.getMethod("getCurrentSession").invoke(null);
			Object processor = sessionClass.getMethod("getMapProcessor").invoke(session);
			Class<?> mapClass = Class.forName("xaero.map.gui.GuiMap");
			Constructor<?> constructor = mapClass.getConstructor(Screen.class, Screen.class, Class.forName("xaero.map.MapProcessor"), Entity.class);
			return (Screen) constructor.newInstance(parent, parent, processor, client.player);
		} catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
			EMUtilsClient.LOGGER.warn("Could not open Xaero's World Map for the UI snapshots.", exception);
			return null;
		}
	}

	/** What the minimap shows right now, or null when that can't be read (which only costs the trimming). The result is reused. */
	public static @Nullable View overMapView() {
		Object handler = overMapHandler;
		Field[] fields = viewFields;
		if (handler == null || fields == null) {
			return null;
		}
		try {
			View view = VIEW;
			view.zoom = fields[0].getDouble(handler);
			view.ps = fields[1].getDouble(handler);
			view.pc = fields[2].getDouble(handler);
			view.halfViewWidth = fields[3].getInt(handler);
			view.halfViewHeight = fields[4].getInt(handler);
			view.specWidth = fields[5].getInt(handler);
			view.specHeight = fields[6].getInt(handler);
			view.circle = fields[7].getBoolean(handler);
			return view.zoom > 0.0D ? view : null;
		} catch (ReflectiveOperationException | RuntimeException exception) {
			viewFields = null;
			EMUtilsClient.LOGGER.warn("Could not read the view of Xaero's Minimap; beacon boundaries are drawn as dots.", exception);
			return null;
		}
	}

	private static boolean attachMinimapHandlers(Attachment attachment) {
		try {
			if (hudModInstance == null) {
				hudModClass = Class.forName("xaero.common.HudMod");
				hudModInstance = hudModClass.getField("INSTANCE");
				getMinimap = hudModClass.getMethod("getMinimap");
				minimapRendererClass = Class.forName("xaero.hud.minimap.element.render.MinimapElementRenderer");
			}
			Object hudMod = hudModInstance.get(null);
			if (hudMod == null) {
				return false;
			}
			Object minimap = getMinimap.invoke(hudMod);
			if (minimap == null) {
				return false;
			}
			if (getOverMapHandler == null) {
				getOverMapHandler = minimap.getClass().getMethod("getOverMapRendererHandler");
			}

			attachMinimapHandler(attachment, getOverMapHandler.invoke(minimap));
			return true;
		} catch (ReflectiveOperationException | LinkageError exception) {
			if (!attachment.minimapFailureLogged) {
				attachment.minimapFailureLogged = true;
				EMUtilsClient.LOGGER.warn("Could not attach " + attachment.name + " to Xaero's Minimap.", exception);
			}
			return false;
		}
	}

	private static void attachMinimapHandler(Attachment attachment, Object handler) throws ReflectiveOperationException {
		if (handler == null || attachment.minimapHandlers.contains(handler)) {
			return;
		}
		Method add = handler.getClass().getMethod("add", minimapRendererClass);
		add.invoke(handler, attachment.renderer.get());
		attachment.minimapHandlers.add(handler);
		if (handler == overMapHandler && viewFields != null) {
			return;
		}
		overMapHandler = handler;
		try {
			Field[] fields = new Field[VIEW_FIELD_NAMES.length];
			for (int i = 0; i < fields.length; i++) {
				fields[i] = handler.getClass().getDeclaredField(VIEW_FIELD_NAMES[i]);
				fields[i].setAccessible(true);
			}
			viewFields = fields;
		} catch (ReflectiveOperationException | RuntimeException exception) {
			// Xaero changed its handler; the outline still draws, as dots, without trimming to what the map shows.
			viewFields = null;
			EMUtilsClient.LOGGER.warn("Could not find the view fields of Xaero's Minimap; beacon boundaries are drawn as dots.", exception);
		}
	}

	private static boolean attachWorldMapHandler(Attachment attachment) {
		try {
			if (worldMapHandlerField == null) {
				worldMapHandlerField = Class.forName("xaero.map.WorldMap").getField("mapElementRenderHandler");
			}
			Object handler = worldMapHandlerField.get(null);
			if (handler == null) {
				return false;
			}
			if (attachment.worldMapHandlers.contains(handler)) {
				return true;
			}

			Object renderer = attachment.renderer.get();
			Class<?> minimapRendererClass = Class.forName("xaero.hud.minimap.element.render.MinimapElementRenderer");
			Class<?> builderClass = Class.forName("xaero.map.mods.minimap.element.MinimapElementRendererWrapper$Builder");
			Object builder = builderClass.getMethod("begin", minimapRendererClass).invoke(null, renderer);

			Class<?> hudModClass = Class.forName("xaero.common.HudMod");
			Object hudMod = hudModClass.getField("INSTANCE").get(null);
			if (hudMod == null) {
				return false;
			}
			Class<?> minimapApiClass = Class.forName("xaero.common.IXaeroMinimap");
			builder = builderClass.getMethod("setModMain", minimapApiClass).invoke(builder, hudMod);
			Supplier<Boolean> enabled = attachment.wanted::getAsBoolean;
			builder = builderClass.getMethod("setShouldRenderSupplier", Supplier.class).invoke(builder, enabled);
			builder = builderClass.getMethod("setOrder", int.class).invoke(builder, attachment.order);
			Object wrapper = builderClass.getMethod("build").invoke(builder);

			Class<?> worldRendererClass = Class.forName("xaero.map.element.render.ElementRenderer");
			handler.getClass().getMethod("add", worldRendererClass).invoke(handler, wrapper);
			attachment.worldMapHandlers.add(handler);
			return true;
		} catch (ReflectiveOperationException | LinkageError exception) {
			if (!attachment.worldMapFailureLogged) {
				attachment.worldMapFailureLogged = true;
				EMUtilsClient.LOGGER.warn("Could not attach " + attachment.name + " to Xaero's World Map.", exception);
			}
			return false;
		}
	}
}
