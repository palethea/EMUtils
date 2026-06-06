package net.emutils.client.emutils.waypoint;

import java.lang.reflect.InvocationTargetException;
import net.emutils.client.EMUtilsClient;

public final class WaypointRenderer {
	private static final String WORLD_RENDER_EVENTS_CLASS = "net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents";
	private static final String BRIDGE_CLASS = "net.emutils.client.emutils.waypoint.WaypointWorldRenderBridge";

	private WaypointRenderer() {
	}

	public static void register() {
		ClassLoader loader = WaypointRenderer.class.getClassLoader();
		try {
			Class.forName(WORLD_RENDER_EVENTS_CLASS, false, loader);
			Class.forName(BRIDGE_CLASS, true, loader).getMethod("register").invoke(null);
		} catch (ClassNotFoundException exception) {
			EMUtilsClient.LOGGER.warn("Waypoint world rendering is unavailable because Fabric world render events are missing.");
		} catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | LinkageError exception) {
			EMUtilsClient.LOGGER.warn("Failed to register waypoint world renderer.", exception);
		}
	}
}
