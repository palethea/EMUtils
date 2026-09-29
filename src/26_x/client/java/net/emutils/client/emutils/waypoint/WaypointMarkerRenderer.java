package net.emutils.client.emutils.waypoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.compat.MinecraftClientCompat;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.hub.HubIcons;
import net.emutils.client.emutils.gui.ui.UiIcons;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/**
 * Draws waypoints on the HUD: a colored marker with the waypoint's initial (a cross for deaths) that
 * shrinks and fades with distance, a name and distance label when you aim at it or are close, and, for
 * waypoints off-screen or behind you, the same marker pinned to the screen edge with an arrow.
 */
public final class WaypointMarkerRenderer {
	/** The marker's side in GUI pixels at the default size and close range. */
	private static final int MARKER_SIZE = 16;
	private static final int MIN_MARKER_SIZE = 6;
	private static final int MIN_INITIAL_SIZE = 9;
	private static final int ARROW_SIZE = 10;
	/** A waypoint this close to the screen edge counts as off-screen, so its marker is never half cut off. */
	private static final int EDGE_MARGIN = 10;
	/** How far in from the edge a pinned marker sits, leaving room for its arrow and distance. */
	private static final int PIN_MARGIN = 28;
	/** The bottom margin is larger, so a pinned marker stays above the hotbar. */
	private static final int PIN_MARGIN_BOTTOM = 66;
	/** Within this many blocks the name and distance always show; farther, only while you aim at the marker. */
	private static final int LABEL_NEAR_BLOCKS = 8;
	private static final int AIM_RADIUS = 14;
	private static final int NEAR_SCALE_BLOCKS = 16;
	private static final int FAR_SCALE_BLOCKS = 400;
	private static final float FAR_SCALE = 0.65F;
	private static final int FADE_NEAR_BLOCKS = 5;
	private static final float MIN_NEAR_FADE = 0.25F;
	private static final double LIGHT_LUMINANCE = 150.0D;

	private WaypointMarkerRenderer() {
	}

	static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft client = Minecraft.getInstance();
		WaypointManager manager = EMUtilsClient.waypoint();
		if (!manager.shouldRender(client)) {
			return;
		}

		EMUtilsConfig config = EMUtilsClient.config();
		int opacity = config.waypointOpacity();
		if (opacity <= 0) {
			return;
		}

		Camera camera = MinecraftClientCompat.mainCamera(client);
		Vec3 cameraForward = Vec3.directionFromRotation(camera.xRot(), camera.yaw());
		int width = graphics.guiWidth();
		int height = graphics.guiHeight();
		int maxDistance = config.waypointMaxDistance();
		boolean pin = config.waypointEdgePin();

		List<Marker> markers = new ArrayList<>();
		for (Waypoint waypoint : manager.waypointsForCurrentWorld(client)) {
			if (waypoint.hidden()) {
				continue;
			}
			int distance = manager.distanceBlocks(client, waypoint);
			if (maxDistance > 0 && distance > maxDistance) {
				continue;
			}

			Vec3 position = new Vec3(
				WaypointManager.renderX(waypoint),
				WaypointManager.renderY(waypoint),
				WaypointManager.renderZ(waypoint)
			);
			boolean inFront = position.subtract(camera.position()).dot(cameraForward) > 0.0D;
			Vec3 projected = client.gameRenderer.projectPointToScreen(position);
			// Behind the camera the projection is mirrored through the screen center, so the way to the point is the opposite.
			double directionX = inFront ? projected.x : -projected.x;
			double directionY = inFront ? projected.y : -projected.y;
			double screenX = (projected.x + 1.0D) * 0.5D * width;
			double screenY = (1.0D - projected.y) * 0.5D * height;
			boolean onScreen = inFront
				&& projected.z >= -1.0D && projected.z <= 1.0D
				&& screenX >= EDGE_MARGIN && screenX <= width - EDGE_MARGIN
				&& screenY >= EDGE_MARGIN && screenY <= height - EDGE_MARGIN;

			if (onScreen) {
				markers.add(new Marker(waypoint, distance, screenX, screenY, 0.0D, 0.0D, false));
			} else if (pin) {
				double[] edge = pinToEdge(directionX, directionY, width, height);
				markers.add(new Marker(waypoint, distance, edge[0], edge[1], edge[2], edge[3], true));
			}
		}
		// The far ones first, so a near marker is drawn over the far one it overlaps.
		markers.sort(Comparator.comparingInt(Marker::distance).reversed());

		int crosshairX = width / 2;
		int crosshairY = height / 2;
		Font font = client.font;
		float userScale = config.waypointMarkerScale();
		for (Marker marker : markers) {
			float alpha = opacity / 100.0F * fade(marker.distance(), maxDistance);
			if (alpha <= 0.0F) {
				continue;
			}
			float scale = userScale * distanceScale(marker.distance());
			int size = Math.max(MIN_MARKER_SIZE, Math.round(MARKER_SIZE * scale));
			int centerX = (int) Math.round(marker.screenX());
			int centerY = (int) Math.round(marker.screenY());
			drawMarker(graphics, font, marker.waypoint(), centerX, centerY, size, alpha, scale);

			int below = centerY + size / 2 + 3;
			if (marker.pinned()) {
				drawArrow(graphics, marker, centerX, centerY, size, alpha);
				drawDistance(graphics, font, marker.distance(), centerX, below, alpha);
			} else if (marker.distance() <= LABEL_NEAR_BLOCKS
				|| (Math.abs(centerX - crosshairX) <= size / 2 + AIM_RADIUS && Math.abs(centerY - crosshairY) <= size / 2 + AIM_RADIUS)) {
				drawLabel(graphics, font, marker.waypoint(), marker.distance(), centerX, below, alpha);
			}
		}
	}

	/** Where a ray from the screen center toward {@code (directionX, directionY)}, in clip space, meets the pin margin: {x, y, unit direction x, unit direction y} in screen space. */
	private static double[] pinToEdge(double directionX, double directionY, int width, int height) {
		double dx = directionX;
		// Screen y grows downward, clip space y upward.
		double dy = -directionY;
		if (!Double.isFinite(dx) || !Double.isFinite(dy) || dx * dx + dy * dy < 1.0E-9D) {
			// Straight behind: no side to prefer, so point down.
			dx = 0.0D;
			dy = 1.0D;
		}
		double length = Math.sqrt(dx * dx + dy * dy);
		dx /= length;
		dy /= length;
		double halfWidth = width / 2.0D - PIN_MARGIN;
		double halfHeight = height / 2.0D - (dy > 0.0D ? PIN_MARGIN_BOTTOM : PIN_MARGIN);
		double reachX = Math.abs(dx) < 1.0E-6D ? Double.MAX_VALUE : halfWidth / Math.abs(dx);
		double reachY = Math.abs(dy) < 1.0E-6D ? Double.MAX_VALUE : halfHeight / Math.abs(dy);
		double reach = Math.min(reachX, reachY);
		return new double[] {width / 2.0D + dx * reach, height / 2.0D + dy * reach, dx, dy};
	}

	/** 1 up to {@code NEAR_SCALE_BLOCKS} away, shrinking to {@code FAR_SCALE} at {@code FAR_SCALE_BLOCKS}. */
	private static float distanceScale(int distance) {
		float t = Math.clamp((distance - NEAR_SCALE_BLOCKS) / (float) (FAR_SCALE_BLOCKS - NEAR_SCALE_BLOCKS), 0.0F, 1.0F);
		return 1.0F + (FAR_SCALE - 1.0F) * t;
	}

	/** Fades out right next to you, so a marker never covers what you're doing, and over the last fifth of the max distance. */
	private static float fade(int distance, int maxDistance) {
		float fade = 1.0F;
		if (distance < FADE_NEAR_BLOCKS) {
			fade = Math.max(MIN_NEAR_FADE, distance / (float) FADE_NEAR_BLOCKS);
		}
		if (maxDistance > 0) {
			float start = maxDistance * 0.8F;
			if (distance > start) {
				fade *= Math.clamp((maxDistance - distance) / (maxDistance - start), 0.0F, 1.0F);
			}
		}
		return fade;
	}

	private static void drawMarker(GuiGraphicsExtractor graphics, Font font, Waypoint waypoint, int centerX, int centerY, int size, float alpha, float scale) {
		int color = waypoint.color();
		int x = centerX - size / 2;
		int y = centerY - size / 2;
		int fill = withAlpha(color, alpha * 0.92F);
		int border = withAlpha(0x000000, alpha * 0.55F);
		UiShapes.borderedRect(graphics, x, y, size, size, Math.max(3, size / 4), fill, border);

		int glyph = withAlpha(isLight(color) ? 0x101010 : 0xFFFFFF, alpha);
		if (waypoint.isDeath()) {
			int pad = Math.max(2, size / 5);
			UiIcons.draw(graphics, HubIcons.X, x + pad, y + pad, size - pad * 2, glyph);
			return;
		}
		if (size < MIN_INITIAL_SIZE) {
			return;
		}
		String initial = initial(waypoint.label());
		graphics.pose().pushMatrix();
		graphics.pose().translate(centerX, centerY);
		graphics.pose().scale(scale, scale);
		graphics.text(font, initial, -font.width(initial) / 2, -font.lineHeight / 2 + 1, glyph, false);
		graphics.pose().popMatrix();
	}

	private static void drawArrow(GuiGraphicsExtractor graphics, Marker marker, int centerX, int centerY, int size, float alpha) {
		double offset = size / 2.0D + ARROW_SIZE / 2.0D + 2.0D;
		graphics.pose().pushMatrix();
		graphics.pose().translate((float) (centerX + marker.directionX() * offset), (float) (centerY + marker.directionY() * offset));
		graphics.pose().rotate((float) Math.atan2(marker.directionX(), -marker.directionY()));
		UiIcons.draw(graphics, HubIcons.CHEVRON_UP, -ARROW_SIZE / 2, -ARROW_SIZE / 2, ARROW_SIZE, withAlpha(marker.waypoint().color(), alpha));
		graphics.pose().popMatrix();
	}

	private static void drawDistance(GuiGraphicsExtractor graphics, Font font, int distance, int centerX, int top, float alpha) {
		Component text = Component.translatable(EMUtilsTexts.WAYPOINT_DISTANCE_SHORT, distance);
		graphics.text(font, text, centerX - font.width(text) / 2, top, withAlpha(0xFFFFFF, alpha), true);
	}

	private static void drawLabel(GuiGraphicsExtractor graphics, Font font, Waypoint waypoint, int distance, int centerX, int top, float alpha) {
		Component title = Component.literal(waypoint.label());
		Component lore = Component.translatable(EMUtilsTexts.WAYPOINT_DISTANCE, distance);
		int titleWidth = font.width(title);
		int loreWidth = font.width(lore);
		int panelWidth = Math.max(titleWidth, loreWidth) + 10;
		int panelHeight = 24;
		UiShapes.roundedRect(graphics, centerX - panelWidth / 2, top, panelWidth, panelHeight, 4, withAlpha(0x000000, alpha * 0.6F));
		graphics.text(font, title, centerX - titleWidth / 2, top + 3, withAlpha(0xFFFFFF, alpha), true);
		graphics.text(font, lore, centerX - loreWidth / 2, top + 13, withAlpha(0xBBBBBB, alpha), true);
	}

	private static String initial(String label) {
		if (label != null) {
			String trimmed = label.strip();
			if (!trimmed.isEmpty()) {
				return new String(Character.toChars(Character.toUpperCase(trimmed.codePointAt(0))));
			}
		}
		return "?";
	}

	/** Whether dark text reads better than white on {@code color}. */
	private static boolean isLight(int color) {
		int red = (color >> 16) & 0xFF;
		int green = (color >> 8) & 0xFF;
		int blue = color & 0xFF;
		return red * 0.299D + green * 0.587D + blue * 0.114D > LIGHT_LUMINANCE;
	}

	private static int withAlpha(int color, float alpha) {
		int value = Math.clamp(Math.round(alpha * 255.0F), 0, 255);
		return (value << 24) | (color & 0x00FFFFFF);
	}

	private record Marker(Waypoint waypoint, int distance, double screenX, double screenY, double directionX, double directionY, boolean pinned) {
	}
}
