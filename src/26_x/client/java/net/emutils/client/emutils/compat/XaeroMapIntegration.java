package net.emutils.client.emutils.compat;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Supplier;
import net.emutils.client.EMUtilsClient;
import net.fabricmc.loader.api.FabricLoader;

public final class XaeroMapIntegration {
	private static final Set<Object> MINIMAP_HANDLERS = Collections.newSetFromMap(new IdentityHashMap<>());
	private static final Set<Object> WORLD_MAP_HANDLERS = Collections.newSetFromMap(new IdentityHashMap<>());
	private static final boolean WORLD_MAP_LOADED = FabricLoader.getInstance().isModLoaded("xaeroworldmap");
	private static boolean minimapFailureLogged;
	private static boolean worldMapFailureLogged;

	private XaeroMapIntegration() {
	}

	public static void tick() {
		attachMinimapHandlers();
		if (WORLD_MAP_LOADED) {
			attachWorldMapHandler();
		}
	}

	private static void attachMinimapHandlers() {
		try {
			Class<?> hudModClass = Class.forName("xaero.common.HudMod");
			Object hudMod = hudModClass.getField("INSTANCE").get(null);
			if (hudMod == null) {
				return;
			}
			Object minimap = hudModClass.getMethod("getMinimap").invoke(hudMod);
			if (minimap == null) {
				return;
			}

			attachMinimapHandler(minimap.getClass().getMethod("getOverMapRendererHandler").invoke(minimap));
		} catch (ReflectiveOperationException | LinkageError exception) {
			if (!minimapFailureLogged) {
				minimapFailureLogged = true;
				EMUtilsClient.LOGGER.warn("Could not attach beacon boundaries to Xaero's Minimap 26.1.3.", exception);
			}
		}
	}

	private static void attachMinimapHandler(Object handler) throws ReflectiveOperationException {
		if (handler == null || MINIMAP_HANDLERS.contains(handler)) {
			return;
		}
		Class<?> rendererClass = Class.forName("xaero.hud.minimap.element.render.MinimapElementRenderer");
		Method add = handler.getClass().getMethod("add", rendererClass);
		add.invoke(handler, new BeaconXaeroElementRenderer());
		MINIMAP_HANDLERS.add(handler);
	}

	private static void attachWorldMapHandler() {
		try {
			Class<?> worldMapClass = Class.forName("xaero.map.WorldMap");
			Object handler = worldMapClass.getField("mapElementRenderHandler").get(null);
			if (handler == null || WORLD_MAP_HANDLERS.contains(handler)) {
				return;
			}

			BeaconXaeroElementRenderer renderer = new BeaconXaeroElementRenderer();
			Class<?> minimapRendererClass = Class.forName("xaero.hud.minimap.element.render.MinimapElementRenderer");
			Class<?> builderClass = Class.forName("xaero.map.mods.minimap.element.MinimapElementRendererWrapper$Builder");
			Object builder = builderClass.getMethod("begin", minimapRendererClass).invoke(null, renderer);

			Class<?> hudModClass = Class.forName("xaero.common.HudMod");
			Object hudMod = hudModClass.getField("INSTANCE").get(null);
			if (hudMod == null) {
				return;
			}
			Class<?> minimapApiClass = Class.forName("xaero.common.IXaeroMinimap");
			builder = builderClass.getMethod("setModMain", minimapApiClass).invoke(builder, hudMod);
			Supplier<Boolean> enabled = () -> EMUtilsClient.config().beaconRadiusOutline();
			builder = builderClass.getMethod("setShouldRenderSupplier", Supplier.class).invoke(builder, enabled);
			builder = builderClass.getMethod("setOrder", int.class).invoke(builder, -100);
			Object wrapper = builderClass.getMethod("build").invoke(builder);

			Class<?> worldRendererClass = Class.forName("xaero.map.element.render.ElementRenderer");
			handler.getClass().getMethod("add", worldRendererClass).invoke(handler, wrapper);
			WORLD_MAP_HANDLERS.add(handler);
		} catch (ReflectiveOperationException | LinkageError exception) {
			if (!worldMapFailureLogged) {
				worldMapFailureLogged = true;
				EMUtilsClient.LOGGER.warn("Could not attach beacon boundaries to Xaero's World Map 1.41.1.", exception);
			}
		}
	}
}
