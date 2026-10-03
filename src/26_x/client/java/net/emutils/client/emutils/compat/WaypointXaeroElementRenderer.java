package net.emutils.client.emutils.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.waypoint.Waypoint;
import net.emutils.client.emutils.waypoint.WaypointEntry;
import net.emutils.client.emutils.waypoint.WaypointManager;
import net.emutils.client.emutils.waypoint.WaypointMarkerRenderer;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.hud.minimap.element.render.MinimapElementGraphics;
import xaero.hud.minimap.element.render.MinimapElementReader;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import xaero.hud.minimap.element.render.MinimapElementRenderProvider;
import xaero.hud.minimap.element.render.MinimapElementRenderer;
import xaero.lib.client.graphics.XaeroBufferProvider;

/**
 * Draws EMUtils waypoints on Xaero's Minimap (#185; the World Map has {@link WaypointWorldMapElementRenderer}) as the same rounded colored marker with the
 * waypoint's initial (a cross for deaths) that Xaero draws for its own waypoints, at the same size. Xaero does
 * the placing: it puts each element where it is on the map and pins the ones past the minimap's edge to it, like
 * its own waypoints, so the renderer only draws a marker at the origin it is given.
 */
final class WaypointXaeroElementRenderer extends MinimapElementRenderer<WaypointEntry, Object> {
	private static final Object CONTEXT = new Object();
	/** Half a marker's width: the colored area is 9 pixels across, like Xaero's, with a 1 pixel dark border around it. */
	private static final int HALF = 4;
	private static final int BORDER_COLOR = 0x000000;
	/** Where waypoints sit among the map's elements: below Xaero's own (100), so theirs stay on top of ours. */
	private static final int ORDER = 90;

	private final Provider provider;

	WaypointXaeroElementRenderer() {
		this(new Provider());
	}

	private WaypointXaeroElementRenderer(Provider provider) {
		super(new Reader(), provider, CONTEXT);
		this.provider = provider;
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

	@Override
	public boolean renderElement(
		WaypointEntry entry,
		boolean highlighted,
		boolean outOfBounds,
		double depth,
		float optionalScale,
		double partialX,
		double partialZ,
		MinimapElementRenderInfo renderInfo,
		MinimapElementGraphics graphics,
		XaeroBufferProvider buffers
	) {
		EMUtilsConfig config = EMUtilsClient.config();
		int maxDistance = config.waypointMaxDistance();
		if (maxDistance > 0) {
			double offsetX = entry.renderX() - renderInfo.renderPos.x;
			double offsetZ = entry.renderZ() - renderInfo.renderPos.z;
			if (offsetX * offsetX + offsetZ * offsetZ > (double) maxDistance * maxDistance) {
				return false;
			}
		}
		int opacity = config.waypointOpacity();
		if (opacity <= 0) {
			return false;
		}

		Waypoint waypoint = entry.waypoint();
		int alpha = Math.clamp(Math.round(opacity * 255.0F / 100.0F), 0, 255) << 24;
		int color = waypoint.color();
		PoseStack pose = graphics.pose();
		pose.pushPose();
		try {
			pose.translate(-1.0D, -1.0D, depth);
			float scale = optionalScale * config.waypointMarkerScale();
			pose.scale(scale, scale, 1.0F);
			drawMarker(graphics, waypoint, alpha | (color & 0x00FFFFFF), alpha | BORDER_COLOR, alpha | (WaypointMarkerRenderer.isLight(color) ? 0x101010 : 0xFFFFFF));
		} finally {
			pose.popPose();
		}
		return true;
	}

	/** A marker around the origin: a border and a fill that each leave their corner pixels out, which rounds it. */
	private static void drawMarker(MinimapElementGraphics graphics, Waypoint waypoint, int fill, int border, int glyph) {
		graphics.fill(-HALF, -HALF - 1, HALF + 1, HALF + 2, border);
		graphics.fill(-HALF - 1, -HALF, HALF + 2, HALF + 1, border);
		graphics.fill(-HALF + 1, -HALF, HALF, HALF + 1, fill);
		graphics.fill(-HALF, -HALF + 1, HALF + 1, HALF, fill);

		if (waypoint.isDeath()) {
			for (int i = -2; i <= 3; i++) {
				graphics.fill(i, i, i + 1, i + 1, glyph);
				graphics.fill(i, 1 - i, i + 1, 2 - i, glyph);
			}
			return;
		}
		Minecraft client = Minecraft.getInstance();
		String initial = WaypointMarkerRenderer.initial(waypoint.label());
		// Placed like Xaero places its own initials: centered on the marker, with the top of the capitals at -3.
		graphics.drawString(client.font, initial, 1 - client.font.width(initial) / 2, -3, glyph, false);
	}

	@Override
	public void preRender(
		MinimapElementRenderInfo renderInfo,
		XaeroBufferProvider buffers,
		MultiTextureRenderTypeRendererProvider renderTypes
	) {
		// Xaero calls this right before it asks for the elements, so the provider knows which dimension the map shows.
		provider.renderInfo = renderInfo;
	}

	@Override
	public void postRender(
		MinimapElementRenderInfo renderInfo,
		XaeroBufferProvider buffers,
		MultiTextureRenderTypeRendererProvider renderTypes
	) {
		provider.renderInfo = null;
	}

	@Override
	public boolean shouldRender(MinimapElementRenderLocation location) {
		return XaeroMapIntegration.waypointsWanted()
			&& location == MinimapElementRenderLocation.OVER_MINIMAP;
	}

	/** The waypoints that have a place in the dimension the map shows, taken once a frame. */
	private static final class Provider extends MinimapElementRenderProvider<WaypointEntry, Object> {
		private List<WaypointEntry> entries = List.of();
		private int index;
		private @Nullable MinimapElementRenderInfo renderInfo;

		@Override
		public void begin(MinimapElementRenderLocation location, Object context) {
			index = 0;
			entries = List.of();
			Minecraft client = Minecraft.getInstance();
			WaypointManager manager = EMUtilsClient.waypoint();
			// Positions are in the dimension you are in, so they mean nothing on a map of another one.
			if (client.level == null || client.player == null || !manager.enabled()
				|| renderInfo == null || renderInfo.mapDimension != client.level.dimension()) {
				return;
			}
			List<WaypointEntry> shown = new ArrayList<>();
			for (WaypointEntry entry : manager.renderEntries(client)) {
				if (!entry.waypoint().hidden()) {
					shown.add(entry);
				}
			}
			entries = shown;
		}

		@Override
		public boolean hasNext(MinimapElementRenderLocation location, Object context) {
			return index < entries.size();
		}

		@Override
		public WaypointEntry getNext(MinimapElementRenderLocation location, Object context) {
			return entries.get(index++);
		}

		@Override
		public void end(MinimapElementRenderLocation location, Object context) {
			entries = List.of();
			index = 0;
		}
	}

	private static final class Reader extends MinimapElementReader<WaypointEntry, Object> {
		@Override
		public boolean isHidden(WaypointEntry entry, Object context) {
			return false;
		}

		@Override
		public double getRenderX(WaypointEntry entry, Object context, float partialTicks) {
			return entry.renderX();
		}

		@Override
		public double getRenderY(WaypointEntry entry, Object context, float partialTicks) {
			return entry.renderY();
		}

		@Override
		public double getRenderZ(WaypointEntry entry, Object context, float partialTicks) {
			return entry.renderZ();
		}

		@Override
		public int getInteractionBoxLeft(WaypointEntry entry, Object context, float optionalScale) {
			return -HALF;
		}

		@Override
		public int getInteractionBoxRight(WaypointEntry entry, Object context, float optionalScale) {
			return HALF + 1;
		}

		@Override
		public int getInteractionBoxTop(WaypointEntry entry, Object context, float optionalScale) {
			return -HALF;
		}

		@Override
		public int getInteractionBoxBottom(WaypointEntry entry, Object context, float optionalScale) {
			return HALF + 1;
		}

		@Override
		public int getRenderBoxLeft(WaypointEntry entry, Object context, float optionalScale) {
			return -HALF - 1;
		}

		@Override
		public int getRenderBoxRight(WaypointEntry entry, Object context, float optionalScale) {
			return HALF + 2;
		}

		@Override
		public int getRenderBoxTop(WaypointEntry entry, Object context, float optionalScale) {
			return -HALF - 1;
		}

		@Override
		public int getRenderBoxBottom(WaypointEntry entry, Object context, float optionalScale) {
			return HALF + 2;
		}

		@Override
		public int getLeftSideLength(WaypointEntry entry, Minecraft client) {
			return 0;
		}

		@Override
		public String getMenuName(WaypointEntry entry) {
			return entry.waypoint().label() == null ? "" : entry.waypoint().label();
		}

		@Override
		public String getFilterName(WaypointEntry entry) {
			return getMenuName(entry);
		}

		@Override
		public int getMenuTextFillLeftPadding(WaypointEntry entry) {
			return 0;
		}

		@Override
		public int getRightClickTitleBackgroundColor(WaypointEntry entry) {
			return 0xFF000000 | entry.waypoint().color();
		}

		@Override
		public boolean shouldScaleBoxWithOptionalScale() {
			return true;
		}

		@Override
		public boolean isInteractable(MinimapElementRenderLocation location, WaypointEntry entry) {
			return false;
		}
	}
}
