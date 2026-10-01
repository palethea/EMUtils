package net.emutils.client.emutils.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Supplier;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.Nullable;

/**
 * Puts the Beacon Radius Outline on Xaero's Minimap and World Map (#188). Nothing is attached, built or looked
 * up while the outline or the Xaero Map Integration setting is off; once attached, the handlers are only
 * looked up again every second, with the reflection handles kept.
 */
public final class XaeroMapIntegration {
	private static final boolean MINIMAP_LOADED = FabricLoader.getInstance().isModLoaded("xaerominimap");
	private static final boolean WORLD_MAP_LOADED = FabricLoader.getInstance().isModLoaded("xaeroworldmap");
	/** How often an attached handler is checked for still being the current one. */
	private static final int RECHECK_TICKS = 20;
	/** How long to wait before trying again after Xaero's classes weren't there or didn't match. */
	private static final int RETRY_TICKS = 200;

	private static final Set<Object> MINIMAP_HANDLERS = Collections.newSetFromMap(new IdentityHashMap<>());
	private static final Set<Object> WORLD_MAP_HANDLERS = Collections.newSetFromMap(new IdentityHashMap<>());
	private static int cooldown;
	private static boolean minimapFailureLogged;
	private static boolean worldMapFailureLogged;

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

	/** Whether the outline should be on Xaero's maps: Xaero is installed, and the outline and its Xaero setting are on. */
	public static boolean isWanted() {
		EMUtilsConfig config = EMUtilsClient.config();
		return MINIMAP_LOADED && config != null && config.beaconRadiusOutline() && config.beaconRadiusXaero();
	}

	public static void tick() {
		if (!isWanted() || --cooldown > 0) {
			return;
		}

		boolean attached = attachMinimapHandlers();
		if (WORLD_MAP_LOADED) {
			attached &= attachWorldMapHandler();
		}
		cooldown = attached ? RECHECK_TICKS : RETRY_TICKS;
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

	private static boolean attachMinimapHandlers() {
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

			attachMinimapHandler(getOverMapHandler.invoke(minimap));
			return true;
		} catch (ReflectiveOperationException | LinkageError exception) {
			if (!minimapFailureLogged) {
				minimapFailureLogged = true;
				EMUtilsClient.LOGGER.warn("Could not attach beacon boundaries to Xaero's Minimap.", exception);
			}
			return false;
		}
	}

	private static void attachMinimapHandler(Object handler) throws ReflectiveOperationException {
		if (handler == null || MINIMAP_HANDLERS.contains(handler)) {
			return;
		}
		Method add = handler.getClass().getMethod("add", minimapRendererClass);
		add.invoke(handler, new BeaconXaeroElementRenderer());
		MINIMAP_HANDLERS.add(handler);
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

	private static boolean attachWorldMapHandler() {
		try {
			if (worldMapHandlerField == null) {
				worldMapHandlerField = Class.forName("xaero.map.WorldMap").getField("mapElementRenderHandler");
			}
			Object handler = worldMapHandlerField.get(null);
			if (handler == null) {
				return false;
			}
			if (WORLD_MAP_HANDLERS.contains(handler)) {
				return true;
			}

			BeaconXaeroElementRenderer renderer = new BeaconXaeroElementRenderer();
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
			Supplier<Boolean> enabled = XaeroMapIntegration::isWanted;
			builder = builderClass.getMethod("setShouldRenderSupplier", Supplier.class).invoke(builder, enabled);
			builder = builderClass.getMethod("setOrder", int.class).invoke(builder, -100);
			Object wrapper = builderClass.getMethod("build").invoke(builder);

			Class<?> worldRendererClass = Class.forName("xaero.map.element.render.ElementRenderer");
			handler.getClass().getMethod("add", worldRendererClass).invoke(handler, wrapper);
			WORLD_MAP_HANDLERS.add(handler);
			return true;
		} catch (ReflectiveOperationException | LinkageError exception) {
			if (!worldMapFailureLogged) {
				worldMapFailureLogged = true;
				EMUtilsClient.LOGGER.warn("Could not attach beacon boundaries to Xaero's World Map.", exception);
			}
			return false;
		}
	}
}
