package net.emutils.client.emutils.compat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.waypoint.WaypointEntry;
import net.emutils.client.emutils.waypoint.WaypointManager;
import net.emutils.client.emutils.waypoint.WaypointMarkerRenderer;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;
import xaero.lib.client.graphics.XaeroBufferProvider;
import xaero.map.element.MapElementGraphics;
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.element.render.ElementRenderProvider;
import xaero.map.element.render.ElementRenderer;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.map.mods.SupportMods;
import xaero.map.mods.gui.Waypoint;
import xaero.map.mods.gui.WaypointReader;
import xaero.map.mods.gui.WaypointRenderContext;
import xaero.map.mods.gui.WaypointRenderer;

/**
 * Draws EMUtils waypoints on Xaero's World Map (#185) with Xaero's own waypoint renderer, so they are the same
 * flags with the initial, the name that fades in under the cursor, the same scale, backgrounds, minimum zoom and
 * hide setting, as its own waypoints. Each waypoint is handed to that renderer as a Xaero World Map waypoint that
 * isn't in any of Xaero's lists; the only thing replaced is the color, which Xaero keeps to 16 and EMUtils doesn't.
 * Right-clicking one offers nothing, because Xaero's edit, teleport and delete options act on its own waypoints.
 */
final class WaypointWorldMapElementRenderer extends ElementRenderer<Waypoint, WaypointRenderContext, WaypointRenderer> {
	/** Where waypoints sit among the map's elements: just below Xaero's own (200). */
	private static final int ORDER = 190;

	private final WaypointRenderer real;
	private final Provider provider;

	/** The renderer, or null while Xaero's own isn't set up yet. */
	static @Nullable WaypointWorldMapElementRenderer create() {
		if (SupportMods.xaeroMinimap == null) {
			return null;
		}
		WaypointRenderer real = SupportMods.xaeroMinimap.getWaypointRenderer();
		return real == null ? null : new WaypointWorldMapElementRenderer(real, new Provider());
	}

	private WaypointWorldMapElementRenderer(WaypointRenderer real, Provider provider) {
		super(real.getContext(), provider, new Reader());
		this.real = real;
		this.provider = provider;
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

	@Override
	public boolean shouldBeDimScaled() {
		// Like Xaero's waypoints, which place themselves with the dimension scale.
		return false;
	}

	@Override
	public boolean shouldRender(ElementRenderLocation location, boolean shadow) {
		return XaeroMapIntegration.waypointsWanted() && real.shouldRender(location, shadow);
	}

	@Override
	public void preRender(ElementRenderInfo renderInfo, XaeroBufferProvider buffers, MultiTextureRenderTypeRendererProvider renderTypes, boolean shadow) {
		real.preRender(renderInfo, buffers, renderTypes, shadow);
		// Xaero calls this right before it asks for the elements, so the provider knows which dimension the map shows.
		provider.renderInfo = renderInfo;
	}

	@Override
	public void postRender(ElementRenderInfo renderInfo, XaeroBufferProvider buffers, MultiTextureRenderTypeRendererProvider renderTypes, boolean shadow) {
		real.postRender(renderInfo, buffers, renderTypes, shadow);
		provider.renderInfo = null;
	}

	@Override
	public void renderElementShadow(
		Waypoint element,
		boolean hovered,
		float optionalScale,
		double partialX,
		double partialY,
		ElementRenderInfo renderInfo,
		MapElementGraphics graphics,
		XaeroBufferProvider buffers,
		MultiTextureRenderTypeRendererProvider renderTypes
	) {
		real.renderElementShadow(element, hovered, optionalScale, partialX, partialY, renderInfo, graphics, buffers, renderTypes);
	}

	@Override
	public boolean renderElement(
		Waypoint element,
		boolean hovered,
		double optionalDepth,
		float optionalScale,
		double partialX,
		double partialY,
		ElementRenderInfo renderInfo,
		MapElementGraphics graphics,
		XaeroBufferProvider buffers,
		MultiTextureRenderTypeRendererProvider renderTypes
	) {
		return real.renderElement(element, hovered, optionalDepth, optionalScale, partialX, partialY, renderInfo, graphics, buffers, renderTypes);
	}

	/** A waypoint of EMUtils as Xaero's World Map sees one. */
	private static final class EmWaypoint extends Waypoint {
		private final Key key;

		EmWaypoint(Key key) {
			// Xaero's waypoint type 1 is a death point.
			super(new xaero.common.minimap.waypoints.Waypoint(key.x(), key.y(), key.z(), key.label(), key.initial(), 0, key.death() ? 1 : 0), false, "", 1.0D);
			this.key = key;
		}

		@Override
		public int getColor() {
			return key.color() & 0x00FFFFFF;
		}

		@Override
		public boolean isDisabled() {
			// Xaero's hide key flips this on its own waypoint objects; hiding is done in EMUtils, so it must not stick here.
			return false;
		}
	}

	/** What a waypoint looks like on the map; the same waypoint keeps its Xaero object, and with it its hover animation, while this stays the same. */
	private record Key(String id, int x, int y, int z, String label, String initial, int color, boolean death) {
	}

	private static final class Provider extends ElementRenderProvider<Waypoint, WaypointRenderContext> {
		private final Map<String, EmWaypoint> known = new HashMap<>();
		private List<Waypoint> shown = List.of();
		private int index;
		private @Nullable ElementRenderInfo renderInfo;

		@Override
		public void begin(ElementRenderLocation location, WaypointRenderContext context) {
			index = 0;
			shown = List.of();
			Minecraft client = Minecraft.getInstance();
			WaypointManager manager = EMUtilsClient.waypoint();
			// Positions are in the dimension you are in, so they mean nothing on the map of another one.
			if (client.level == null || client.player == null || !manager.enabled()
				|| renderInfo == null || renderInfo.mapDimension != client.level.dimension()) {
				known.clear();
				return;
			}
			if (context.worldmapWaypointsScale <= 0.0F) {
				context.worldmapWaypointsScale = 1.0F;
			}

			Map<String, EmWaypoint> current = new HashMap<>();
			List<Waypoint> list = new ArrayList<>();
			for (WaypointEntry entry : manager.renderEntries(client)) {
				if (entry.waypoint().hidden()) {
					continue;
				}
				String label = entry.waypoint().label() == null ? "" : entry.waypoint().label();
				Key key = new Key(entry.waypoint().id(), entry.x(), entry.y(), entry.z(), label, WaypointMarkerRenderer.initial(label), entry.waypoint().color(), entry.waypoint().isDeath());
				EmWaypoint waypoint = known.get(key.id());
				if (waypoint == null || !waypoint.key.equals(key)) {
					waypoint = new EmWaypoint(key);
				}
				current.put(key.id(), waypoint);
				list.add(waypoint);
			}
			known.clear();
			known.putAll(current);
			shown = list;
		}

		@Override
		public boolean hasNext(ElementRenderLocation location, WaypointRenderContext context) {
			return index < shown.size();
		}

		@Override
		public Waypoint getNext(ElementRenderLocation location, WaypointRenderContext context) {
			return shown.get(index++);
		}

		@Override
		public void end(ElementRenderLocation location, WaypointRenderContext context) {
			shown = List.of();
			index = 0;
		}
	}

	private static final class Reader extends WaypointReader {
		@Override
		public boolean isRightClickValid(Waypoint element) {
			return false;
		}
	}
}
