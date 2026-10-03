package net.emutils.client.emutils.waypoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.compat.MinecraftClientCompat;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.hub.HubIcons;
import net.emutils.client.emutils.gui.ui.UiIcons;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.emutils.hud.HudFont;
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
 *
 * <p>Everything is measured from the camera on every frame, and drawn at whole screen pixels, so markers
 * glide instead of jumping as you walk: distances from the player's position only change 20 times a
 * second, and rounding to whole GUI pixels moves a marker two screen pixels at a time at GUI scale 2.
 */
public final class WaypointMarkerRenderer {
	/** The marker's side in GUI pixels at the default size and close range. */
	private static final int MARKER_SIZE = 16;
	private static final int MIN_MARKER_SIZE = 6;
	private static final int MIN_INITIAL_SIZE = 9;
	private static final int ARROW_SIZE = 10;
	/** How far in from the screen's sides and top a waypoint stays before it is pinned, leaving room for its arrow and distance. */
	private static final int PIN_MARGIN = 28;
	/** The bottom margin is larger, so a pinned marker stays above the hotbar. */
	private static final int PIN_MARGIN_BOTTOM = 66;
	/** How far a waypoint has to be past the pin margin for its arrow to show in full, so the arrow fades in instead of popping. */
	private static final float ARROW_FADE_PIXELS = 12.0F;
	/** Without pinning, a waypoint this close to the screen edge is left out, so its marker is never half cut off. */
	private static final int EDGE_MARGIN = 10;
	/** Within this many blocks the name and distance always show; farther, only while you aim at the marker. */
	private static final int LABEL_NEAR_BLOCKS = 8;
	private static final int AIM_RADIUS = 14;
	/** How long a label takes to fade in or out, in game ticks. */
	private static final float LABEL_FADE_TICKS = 3.0F;
	private static final int NEAR_SCALE_BLOCKS = 16;
	private static final int FAR_SCALE_BLOCKS = 400;
	private static final float FAR_SCALE = 0.65F;
	private static final int FADE_NEAR_BLOCKS = 5;
	private static final float MIN_NEAR_FADE = 0.25F;
	private static final double LIGHT_LUMINANCE = 150.0D;
	private static final UiText.Size TEXT_SIZE = UiText.Size.LABEL;
	private static final UiText.Size INITIAL_SIZE = UiText.Size.BOLD;

	/** How far each waypoint's label has faded in, by waypoint id. */
	private static final Map<String, Float> LABEL_FADES = new HashMap<>();

	private WaypointMarkerRenderer() {
	}

	static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft client = Minecraft.getInstance();
		WaypointManager manager = EMUtilsClient.waypoint();
		if (!manager.shouldRender(client)) {
			LABEL_FADES.clear();
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
		double guiScale = client.getWindow().getGuiScale();
		int maxDistance = config.waypointMaxDistance();
		boolean pin = config.waypointEdgePin();
		HudFont fontMode = config.waypointFont();
		int background = config.waypointLabelBackground();

		List<Marker> markers = new ArrayList<>();
		for (WaypointEntry entry : manager.renderEntries(client)) {
			Waypoint waypoint = entry.waypoint();
			if (waypoint.hidden()) {
				continue;
			}
			Vec3 position = new Vec3(entry.renderX(), entry.renderY(), entry.renderZ());
			Vec3 toWaypoint = position.subtract(camera.position());
			double distance = toWaypoint.length();
			if (maxDistance > 0 && distance > maxDistance) {
				continue;
			}

			boolean inFront = toWaypoint.dot(cameraForward) > 0.0D;
			Vec3 projected = client.gameRenderer.projectPointToScreen(position);
			// Behind the camera the projection is mirrored through the screen center, so the way to the point is the opposite.
			double directionX = inFront ? projected.x : -projected.x;
			double directionY = inFront ? projected.y : -projected.y;
			double screenX = (projected.x + 1.0D) * 0.5D * width;
			double screenY = (1.0D - projected.y) * 0.5D * height;
			int margin = pin ? PIN_MARGIN : EDGE_MARGIN;
			int marginBottom = pin ? PIN_MARGIN_BOTTOM : EDGE_MARGIN;
			boolean inside = inFront
				&& projected.z >= -1.0D && projected.z <= 1.0D
				&& screenX >= margin && screenX <= width - margin
				&& screenY >= margin && screenY <= height - marginBottom;

			if (inside) {
				markers.add(new Marker(waypoint, distance, screenX, screenY, 0.0D, 0.0D, 0.0F));
			} else if (pin) {
				double[] edge = pinToEdge(directionX, directionY, width, height);
				// How far the waypoint really is past the margin, so the arrow fades in as it leaves the screen.
				float past = inFront ? (float) Math.hypot(screenX - edge[0], screenY - edge[1]) : ARROW_FADE_PIXELS;
				markers.add(new Marker(waypoint, distance, edge[0], edge[1], edge[2], edge[3], Math.clamp(past / ARROW_FADE_PIXELS, 0.0F, 1.0F)));
			}
		}
		// The far ones first, so a near marker is drawn over the far one it overlaps.
		markers.sort(Comparator.comparingDouble(Marker::distance).reversed());

		float fadeStep = deltaTracker.getRealtimeDeltaTicks() / LABEL_FADE_TICKS;
		int crosshairX = width / 2;
		int crosshairY = height / 2;
		Font font = client.font;
		float userScale = config.waypointMarkerScale();
		Map<String, Float> fades = new HashMap<>();
		for (Marker marker : markers) {
			Waypoint waypoint = marker.waypoint();
			float alpha = opacity / 100.0F * fade(marker.distance(), maxDistance);
			// Far away a marker shrinks, but never below the size its letter stays readable at, unless the Size setting makes it smaller up close.
			int nearSize = Math.max(MIN_MARKER_SIZE, Math.round(MARKER_SIZE * userScale));
			int size = Math.max(Math.min(nearSize, MIN_INITIAL_SIZE), Math.round(MARKER_SIZE * userScale * distanceScale(marker.distance())));
			float scale = size / (float) MARKER_SIZE;
			double half = size / 2.0D;
			int shownDistance = (int) Math.round(marker.distance());

			boolean aimed = marker.arrowFade() <= 0.0F
				&& (marker.distance() <= LABEL_NEAR_BLOCKS
					|| (Math.abs(marker.screenX() - crosshairX) <= half + AIM_RADIUS && Math.abs(marker.screenY() - crosshairY) <= half + AIM_RADIUS));
			float labelFade = LABEL_FADES.getOrDefault(waypoint.id(), 0.0F);
			labelFade = aimed ? Math.min(1.0F, labelFade + fadeStep) : Math.max(0.0F, labelFade - fadeStep);
			if (labelFade > 0.0F) {
				fades.put(waypoint.id(), labelFade);
			}

			if (alpha > 0.0F) {
				graphics.pose().pushMatrix();
				graphics.pose().translate(snap(marker.screenX() - half, guiScale), snap(marker.screenY() - half, guiScale));
				drawMarker(graphics, font, fontMode, waypoint, size, alpha, scale);
				graphics.pose().popMatrix();

				double below = marker.screenY() + half + 3.0D;
				if (marker.arrowFade() > 0.0F) {
					drawArrow(graphics, marker, size, alpha * marker.arrowFade(), guiScale);
					drawDistance(graphics, font, fontMode, shownDistance, marker.screenX(), below, alpha * marker.arrowFade(), guiScale);
				}
				if (labelFade > 0.0F) {
					drawLabel(graphics, font, fontMode, waypoint, shownDistance, marker.screenX(), below, alpha * labelFade, background, guiScale);
				}
			}
		}
		// Only the waypoints still shown keep their fade, so it doesn't pile up for ones that are gone.
		LABEL_FADES.clear();
		LABEL_FADES.putAll(fades);
	}

	/** {@code value} moved to the nearest whole screen pixel, in GUI pixels. */
	private static float snap(double value, double guiScale) {
		return (float) (Math.round(value * guiScale) / guiScale);
	}

	/**
	 * Where a ray from the screen center toward {@code (directionX, directionY)}, in clip space, meets the
	 * pin margin: {x, y, unit direction x, unit direction y} in screen space. Clip space runs -1 to 1 across
	 * both sides whatever their length, so the direction is scaled to pixels before it is used as one.
	 */
	private static double[] pinToEdge(double directionX, double directionY, int width, int height) {
		double dx = directionX * width / 2.0D;
		// Screen y grows downward, clip space y upward.
		double dy = -directionY * height / 2.0D;
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

	/** {@link #pinToEdge}, for UI snapshot checks. */
	public static double[] pinToEdgeForSnapshot(double directionX, double directionY, int width, int height) {
		return pinToEdge(directionX, directionY, width, height);
	}

	/** 1 up to {@code NEAR_SCALE_BLOCKS} away, shrinking to {@code FAR_SCALE} at {@code FAR_SCALE_BLOCKS}. */
	private static float distanceScale(double distance) {
		float t = Math.clamp((float) (distance - NEAR_SCALE_BLOCKS) / (FAR_SCALE_BLOCKS - NEAR_SCALE_BLOCKS), 0.0F, 1.0F);
		return 1.0F + (FAR_SCALE - 1.0F) * t;
	}

	/** Fades out right next to you, so a marker never covers what you're doing, and over the last fifth of the max distance. */
	private static float fade(double distance, int maxDistance) {
		float fade = 1.0F;
		if (distance < FADE_NEAR_BLOCKS) {
			fade = Math.max(MIN_NEAR_FADE, (float) distance / FADE_NEAR_BLOCKS);
		}
		if (maxDistance > 0) {
			double start = maxDistance * 0.8D;
			if (distance > start) {
				fade *= Math.clamp((float) ((maxDistance - distance) / (maxDistance - start)), 0.0F, 1.0F);
			}
		}
		return fade;
	}

	/** Draws a waypoint's marker centered on the current origin, the same marker as in the world, for the EMUtils minimap (#212). */
	public static void drawMapMarker(GuiGraphicsExtractor graphics, Waypoint waypoint, int size, float alpha) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(-size / 2.0F, -size / 2.0F);
		drawMarker(graphics, Minecraft.getInstance().font, EMUtilsClient.config().waypointFont(), waypoint, size, alpha, size / (float) MARKER_SIZE);
		graphics.pose().popMatrix();
	}

	/** Draws the marker with its top-left corner at the current origin. */
	private static void drawMarker(GuiGraphicsExtractor graphics, Font font, HudFont fontMode, Waypoint waypoint, int size, float alpha, float scale) {
		int color = waypoint.color();
		int fill = withAlpha(color, alpha * 0.92F);
		int border = withAlpha(0x000000, alpha * 0.55F);
		UiShapes.borderedRect(graphics, 0, 0, size, size, Math.max(3, size / 4), fill, border);

		int glyph = withAlpha(isLight(color) ? 0x101010 : 0xFFFFFF, alpha);
		if (waypoint.isDeath()) {
			int pad = Math.max(2, size / 5);
			UiIcons.draw(graphics, HubIcons.X, pad, pad, size - pad * 2, glyph);
			return;
		}
		if (size < MIN_INITIAL_SIZE) {
			return;
		}
		String initial = initial(waypoint.label());
		graphics.pose().pushMatrix();
		graphics.pose().translate(size / 2.0F, size / 2.0F);
		graphics.pose().scale(scale, scale);
		if (fontMode == HudFont.EMUTILS) {
			UiText.drawInkCentered(graphics, font, Component.literal(initial), INITIAL_SIZE, 0.0F, 0.0F, glyph);
		} else {
			// Minecraft's capitals stand 7 pixels tall from the text's y, and each character advances one pixel past its ink.
			graphics.pose().translate(-(font.width(initial) - 1) / 2.0F, -3.5F);
			graphics.text(font, initial, 0, 0, glyph, false);
		}
		graphics.pose().popMatrix();
	}

	private static void drawArrow(GuiGraphicsExtractor graphics, Marker marker, int size, float alpha, double guiScale) {
		double offset = size / 2.0D + ARROW_SIZE / 2.0D + 2.0D;
		graphics.pose().pushMatrix();
		graphics.pose().translate(
			snap(marker.screenX() + marker.directionX() * offset, guiScale),
			snap(marker.screenY() + marker.directionY() * offset, guiScale)
		);
		graphics.pose().rotate((float) Math.atan2(marker.directionX(), -marker.directionY()));
		UiIcons.draw(graphics, HubIcons.CHEVRON_UP, -ARROW_SIZE / 2, -ARROW_SIZE / 2, ARROW_SIZE, withAlpha(marker.waypoint().color(), alpha));
		graphics.pose().popMatrix();
	}

	private static void drawDistance(GuiGraphicsExtractor graphics, Font font, HudFont fontMode, int distance, double centerX, double top, float alpha, double guiScale) {
		Component text = Component.translatable(EMUtilsTexts.WAYPOINT_DISTANCE_SHORT, distance);
		graphics.pose().pushMatrix();
		graphics.pose().translate(snap(centerX - inkCenter(font, fontMode, text), guiScale), snap(top, guiScale));
		drawText(graphics, font, fontMode, text, 0, 0, withAlpha(0xFFFFFF, alpha));
		graphics.pose().popMatrix();
	}

	private static void drawLabel(GuiGraphicsExtractor graphics, Font font, HudFont fontMode, Waypoint waypoint, int distance, double centerX, double top, float alpha, int background, double guiScale) {
		Component title = Component.literal(waypoint.label() == null ? "" : waypoint.label());
		Component lore = Component.translatable(EMUtilsTexts.WAYPOINT_DISTANCE, distance);
		int titleWidth = textWidth(font, fontMode, title);
		int loreWidth = textWidth(font, fontMode, lore);
		int panelWidth = Math.max(titleWidth, loreWidth) + 10;
		int panelHeight = 24;
		graphics.pose().pushMatrix();
		// The panel and both lines share one origin, so they move together.
		graphics.pose().translate(snap(centerX - panelWidth / 2.0D, guiScale), snap(top, guiScale));
		if (background > 0) {
			UiShapes.roundedRect(graphics, 0, 0, panelWidth, panelHeight, 4, withAlpha(0x000000, alpha * background / 100.0F));
		}
		drawLine(graphics, font, fontMode, title, panelWidth / 2.0F, 3, withAlpha(0xFFFFFF, alpha), guiScale);
		drawLine(graphics, font, fontMode, lore, panelWidth / 2.0F, 13, withAlpha(0xBBBBBB, alpha), guiScale);
		graphics.pose().popMatrix();
	}

	/** Where the middle of the text's ink is, in GUI pixels from the x it is drawn at. */
	private static double inkCenter(Font font, HudFont fontMode, Component text) {
		if (fontMode == HudFont.EMUTILS) {
			float[] ink = UiText.inkExtent(font, text, TEXT_SIZE);
			return (ink[0] + ink[1]) / 2.0D;
		}
		// Minecraft's width counts the shadow's pixel, which is part of what you see.
		return font.width(text) / 2.0D;
	}

	/** Draws a line of text centered on {@code centerX}, at the current origin's x. */
	private static void drawLine(GuiGraphicsExtractor graphics, Font font, HudFont fontMode, Component text, float centerX, int capTop, int color, double guiScale) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(snap(centerX - inkCenter(font, fontMode, text), guiScale), 0.0F);
		drawText(graphics, font, fontMode, text, 0, capTop, color);
		graphics.pose().popMatrix();
	}

	private static int textWidth(Font font, HudFont fontMode, Component text) {
		return fontMode == HudFont.EMUTILS ? UiText.width(font, text, TEXT_SIZE) : font.width(text);
	}

	/** Draws shadowed text with the top of its capital letters at {@code capTop}, in either font. */
	private static void drawText(GuiGraphicsExtractor graphics, Font font, HudFont fontMode, Component text, int x, int capTop, int color) {
		if (fontMode == HudFont.EMUTILS) {
			int shadow = Math.round((color >>> 24) * 0.7F) << 24;
			UiText.draw(graphics, font, text, TEXT_SIZE, x + 1, capTop + 1, shadow);
			UiText.draw(graphics, font, text, TEXT_SIZE, x, capTop, color);
		} else {
			graphics.text(font, text, x, capTop, color, true);
		}
	}

	public static String initial(String label) {
		if (label != null) {
			String trimmed = label.strip();
			if (!trimmed.isEmpty()) {
				return new String(Character.toChars(Character.toUpperCase(trimmed.codePointAt(0))));
			}
		}
		return "?";
	}

	/** Whether dark text reads better than white on {@code color}. */
	public static boolean isLight(int color) {
		int red = (color >> 16) & 0xFF;
		int green = (color >> 8) & 0xFF;
		int blue = color & 0xFF;
		return red * 0.299D + green * 0.587D + blue * 0.114D > LIGHT_LUMINANCE;
	}

	private static int withAlpha(int color, float alpha) {
		int value = Math.clamp(Math.round(alpha * 255.0F), 0, 255);
		return (value << 24) | (color & 0x00FFFFFF);
	}

	/** {@code arrowFade} is 0 for a marker on its waypoint, up to 1 for one pinned to the edge with its arrow in full. */
	private record Marker(Waypoint waypoint, double distance, double screenX, double screenY, double directionX, double directionY, float arrowFade) {
	}
}
