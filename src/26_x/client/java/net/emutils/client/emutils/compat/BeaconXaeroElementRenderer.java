package net.emutils.client.emutils.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.emutils.client.emutils.render.BeaconRadiusRenderer;
import net.emutils.client.emutils.render.BeaconRadiusRenderer.MapRect;
import net.emutils.client.emutils.render.MapLineClip;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
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
 * Draws the beacon cages on Xaero's maps (#188). Xaero runs every element through its whole pipeline each frame
 * and the minimap draws each with its own fill, so a cage made of hundreds of dots cost milliseconds. On the
 * minimap the whole outline is one element that draws the cage edges as a few lines, placed with the map's own
 * zoom and rotation and trimmed to what it shows. Where that can't be done (the World Map, or if Xaero changes)
 * the cages are dots along their edges.
 */
final class BeaconXaeroElementRenderer extends MinimapElementRenderer<BeaconXaeroElementRenderer.Dot, Object> {
	private static final Object CONTEXT = new Object();
	/** Turns a line to its direction; PoseStack's rotation methods differ between versions, multiplying by a matrix doesn't. */
	private static final Matrix4f ROTATION = new Matrix4f();
	/** The side of a dot, in the map's pixels. */
	private static final int DOT_PIXELS = 2;
	/** Where the zoom is unknown (the World Map, which doesn't tell elements its scale): a dot every half block, as close as its zoom needs. */
	private static final double DEFAULT_STEP_BLOCKS = 0.5D;
	/** A safety limit on the dots in a frame, for a map zoomed far out over many beacons. */
	private static final int MAX_DOTS = 8000;

	private final DotProvider dots;

	BeaconXaeroElementRenderer() {
		this(new DotProvider());
	}

	private BeaconXaeroElementRenderer(DotProvider dots) {
		super(new Reader(), dots, CONTEXT);
		this.dots = dots;
	}

	@Override
	public int getOrder() {
		return -100;
	}

	@Override
	public boolean renderElement(
		Dot dot,
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
		if (outOfBounds) {
			return false;
		}
		PoseStack pose = graphics.pose();
		if (dot.overlay) {
			dots.drawLines(graphics, pose, depth);
			return true;
		}
		pose.pushPose();
		try {
			pose.translate(-1.0D, -1.0D, depth);
			graphics.fill(0, 0, DOT_PIXELS, DOT_PIXELS, dot.color);
		} finally {
			pose.popPose();
		}
		return true;
	}

	@Override
	public void preRender(
		MinimapElementRenderInfo renderInfo,
		XaeroBufferProvider buffers,
		MultiTextureRenderTypeRendererProvider renderTypes
	) {
		// Xaero calls this right before it asks for the elements, so the provider knows where the map is centered.
		dots.renderInfo = renderInfo;
	}

	@Override
	public void postRender(
		MinimapElementRenderInfo renderInfo,
		XaeroBufferProvider buffers,
		MultiTextureRenderTypeRendererProvider renderTypes
	) {
		dots.renderInfo = null;
	}

	@Override
	public boolean shouldRender(MinimapElementRenderLocation location) {
		return XaeroMapIntegration.beaconsWanted()
			&& (location == MinimapElementRenderLocation.OVER_MINIMAP || location == MinimapElementRenderLocation.WORLD_MAP);
	}

	/** One dot on a cage's edge. Reused, so Xaero is never given a reference it may keep. */
	static final class Dot {
		double x;
		double y;
		double z;
		int color;
		boolean overlay;
	}

	private static final class DotProvider extends MinimapElementRenderProvider<Dot, Object> {
		private final List<Dot> pool = new ArrayList<>();
		private int size;
		private int index;
		private @Nullable MinimapElementRenderInfo renderInfo;
		/** The lines of the minimap's outline: x1, y1, x2, y2 and color of each, in the map's pixels from its center. */
		private double[] lines = new double[5 * 64];
		private int segments;

		@Override
		public void begin(MinimapElementRenderLocation location, Object context) {
			size = 0;
			index = 0;
			segments = 0;
			Minecraft client = Minecraft.getInstance();
			if (renderInfo == null || client.level == null || renderInfo.mapDimension != client.level.dimension()) {
				return;
			}
			List<MapRect> rects = BeaconRadiusRenderer.mapRects();
			if (rects.isEmpty()) {
				return;
			}

			Vec3 center = renderInfo.renderPos;
			XaeroMapIntegration.View view = location == MinimapElementRenderLocation.OVER_MINIMAP ? XaeroMapIntegration.overMapView() : null;
			if (view != null) {
				// The whole outline as lines, drawn by one element at the center of the map.
				for (MapRect rect : rects) {
					addCage(view, rect, center.x, center.z);
				}
				if (segments > 0) {
					Dot dot = size < pool.size() ? pool.get(size) : newDot();
					size++;
					dot.x = center.x;
					dot.y = center.y;
					dot.z = center.z;
					dot.overlay = true;
				}
				return;
			}

			for (MapRect rect : rects) {
				if (size >= MAX_DOTS) {
					break;
				}
				addEdges(rect, center.x, center.z, Double.POSITIVE_INFINITY, DEFAULT_STEP_BLOCKS);
			}
		}

		/** The four edges of one cage as lines on the map, trimmed to the part of it the map shows. */
		private void addCage(XaeroMapIntegration.View view, MapRect rect, double centerX, double centerZ) {
			double x0 = rect.minX() - centerX;
			double x1 = rect.maxX() - centerX;
			double z0 = rect.minZ() - centerZ;
			double z1 = rect.maxZ() - centerZ;
			addLine(view, x0, z0, x1, z0, rect.color());
			addLine(view, x1, z0, x1, z1, rect.color());
			addLine(view, x1, z1, x0, z1, rect.color());
			addLine(view, x0, z1, x0, z0, rect.color());
		}

		/** A line between two points given in blocks from the map's center, placed with the map's zoom and rotation, then trimmed to what it shows. */
		private void addLine(XaeroMapIntegration.View view, double dx1, double dz1, double dx2, double dz2, int color) {
			double ax = view.zoom * (view.ps * dx1 - view.pc * dz1);
			double ay = view.zoom * (view.pc * dx1 + view.ps * dz1);
			double bx = view.zoom * (view.ps * dx2 - view.pc * dz2);
			double by = view.zoom * (view.pc * dx2 + view.ps * dz2);
			double[] clipped = view.circle
				? MapLineClip.toCircle(ax, ay, bx, by, Math.min(view.specWidth, view.specHeight))
				: MapLineClip.toRectangle(ax, ay, bx, by, view.specWidth, view.specHeight);
			if (clipped == null) {
				return;
			}
			if ((segments + 1) * 5 > lines.length) {
				lines = Arrays.copyOf(lines, lines.length * 2);
			}
			int at = segments * 5;
			lines[at] = clipped[0];
			lines[at + 1] = clipped[1];
			lines[at + 2] = clipped[2];
			lines[at + 3] = clipped[3];
			lines[at + 4] = color;
			segments++;
		}

		/** Draws the lines, from where the map is centered, which is where Xaero has put this element. */
		void drawLines(MinimapElementGraphics graphics, PoseStack pose, double depth) {
			for (int i = 0; i < segments; i++) {
				int at = i * 5;
				double dx = lines[at + 2] - lines[at];
				double dy = lines[at + 3] - lines[at + 1];
				int length = (int) Math.ceil(Math.hypot(dx, dy));
				if (length < 1) {
					continue;
				}
				pose.pushPose();
				try {
					pose.translate(lines[at], lines[at + 1], depth);
					pose.mulPose(ROTATION.rotationZ((float) Math.atan2(dy, dx)));
					graphics.fill(0, -1, length, 1, (int) lines[at + 4]);
				} finally {
					pose.popPose();
				}
			}
		}

		/** The dots of one cage's outline that lie within {@code radius} of the map's center. */
		private void addEdges(MapRect rect, double centerX, double centerZ, double radius, double step) {
			double loX = Math.max(rect.minX(), centerX - radius);
			double hiX = Math.min(rect.maxX(), centerX + radius);
			double loZ = Math.max(rect.minZ(), centerZ - radius);
			double hiZ = Math.min(rect.maxZ(), centerZ + radius);
			if (loX > hiX || loZ > hiZ) {
				return;
			}
			// Dots sit at fixed steps along each edge from its corner, so they don't swim as the map moves.
			if (Math.abs(rect.minZ() - centerZ) <= radius) {
				addRun(rect, rect.minX(), loX, hiX, step, rect.minZ(), true);
			}
			if (Math.abs(rect.maxZ() - centerZ) <= radius) {
				addRun(rect, rect.minX(), loX, hiX, step, rect.maxZ(), true);
			}
			// The sides leave out the corners, which the runs above already have.
			if (Math.abs(rect.minX() - centerX) <= radius) {
				addRun(rect, rect.minZ(), Math.max(loZ, rect.minZ() + step), Math.min(hiZ, rect.maxZ() - step * 0.5D), step, rect.minX(), false);
			}
			if (Math.abs(rect.maxX() - centerX) <= radius) {
				addRun(rect, rect.minZ(), Math.max(loZ, rect.minZ() + step), Math.min(hiZ, rect.maxZ() - step * 0.5D), step, rect.maxX(), false);
			}
		}

		/** Dots every {@code step} from {@code origin} along one axis, between {@code lo} and {@code hi}, at {@code fixed} on the other. */
		private void addRun(MapRect rect, double origin, double lo, double hi, double step, double fixed, boolean alongX) {
			if (lo > hi) {
				return;
			}
			double first = origin + Math.ceil((lo - origin) / step - 1.0E-9D) * step;
			for (double along = first; along <= hi + 1.0E-9D && size < MAX_DOTS; along += step) {
				Dot dot = size < pool.size() ? pool.get(size) : newDot();
				size++;
				dot.x = alongX ? along : fixed;
				dot.z = alongX ? fixed : along;
				dot.y = rect.y();
				dot.color = rect.color();
				dot.overlay = false;
			}
		}

		private Dot newDot() {
			Dot dot = new Dot();
			pool.add(dot);
			return dot;
		}

		@Override
		public boolean hasNext(MinimapElementRenderLocation location, Object context) {
			return index < size;
		}

		@Override
		public Dot getNext(MinimapElementRenderLocation location, Object context) {
			return pool.get(index++);
		}

		@Override
		public void end(MinimapElementRenderLocation location, Object context) {
			size = 0;
			index = 0;
			segments = 0;
		}
	}

	private static final class Reader extends MinimapElementReader<Dot, Object> {
		@Override
		public boolean isHidden(Dot dot, Object context) {
			return false;
		}

		@Override
		public double getRenderX(Dot dot, Object context, float partialTicks) {
			return dot.x;
		}

		@Override
		public double getRenderY(Dot dot, Object context, float partialTicks) {
			return dot.y;
		}

		@Override
		public double getRenderZ(Dot dot, Object context, float partialTicks) {
			return dot.z;
		}

		@Override
		public int getInteractionBoxLeft(Dot dot, Object context, float optionalScale) {
			return -1;
		}

		@Override
		public int getInteractionBoxRight(Dot dot, Object context, float optionalScale) {
			return 1;
		}

		@Override
		public int getInteractionBoxTop(Dot dot, Object context, float optionalScale) {
			return -1;
		}

		@Override
		public int getInteractionBoxBottom(Dot dot, Object context, float optionalScale) {
			return 1;
		}

		@Override
		public int getRenderBoxLeft(Dot dot, Object context, float optionalScale) {
			return -2;
		}

		@Override
		public int getRenderBoxRight(Dot dot, Object context, float optionalScale) {
			return 2;
		}

		@Override
		public int getRenderBoxTop(Dot dot, Object context, float optionalScale) {
			return -2;
		}

		@Override
		public int getRenderBoxBottom(Dot dot, Object context, float optionalScale) {
			return 2;
		}

		@Override
		public int getLeftSideLength(Dot dot, Minecraft client) {
			return 0;
		}

		@Override
		public String getMenuName(Dot dot) {
			return "Beacon boundary";
		}

		@Override
		public String getFilterName(Dot dot) {
			return "Beacon boundary";
		}

		@Override
		public int getMenuTextFillLeftPadding(Dot dot) {
			return 0;
		}

		@Override
		public int getRightClickTitleBackgroundColor(Dot dot) {
			return dot.color;
		}

		@Override
		public boolean shouldScaleBoxWithOptionalScale() {
			return false;
		}

		@Override
		public boolean isInteractable(MinimapElementRenderLocation location, Dot dot) {
			return false;
		}
	}
}
