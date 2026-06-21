package net.emutils.client.emutils.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.render.BeaconRadiusRenderer;
import net.emutils.client.emutils.render.BeaconRadiusRenderer.BeaconMapPoint;
import net.minecraft.client.Minecraft;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.hud.minimap.element.render.MinimapElementGraphics;
import xaero.hud.minimap.element.render.MinimapElementReader;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import xaero.hud.minimap.element.render.MinimapElementRenderProvider;
import xaero.hud.minimap.element.render.MinimapElementRenderer;
import xaero.lib.client.graphics.XaeroBufferProvider;

final class BeaconXaeroElementRenderer extends MinimapElementRenderer<BeaconMapPoint, Object> {
	private static final Object CONTEXT = new Object();

	BeaconXaeroElementRenderer() {
		super(new Reader(), new Provider(), CONTEXT);
	}

	@Override
	public int getOrder() {
		return -100;
	}

	@Override
	public boolean renderElement(
		BeaconMapPoint point,
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
		Minecraft client = Minecraft.getInstance();
		if (outOfBounds || client.level == null || renderInfo.mapDimension != client.level.dimension()) {
			return false;
		}
		PoseStack pose = graphics.pose();
		pose.pushPose();
		try {
			pose.translate(-1.0D, -1.0D, depth);
			graphics.fill(0, 0, 2, 2, point.color());
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
	}

	@Override
	public void postRender(
		MinimapElementRenderInfo renderInfo,
		XaeroBufferProvider buffers,
		MultiTextureRenderTypeRendererProvider renderTypes
	) {
	}

	@Override
	public boolean shouldRender(MinimapElementRenderLocation location) {
		return location == MinimapElementRenderLocation.OVER_MINIMAP
			|| location == MinimapElementRenderLocation.WORLD_MAP;
	}

	private static final class Provider extends MinimapElementRenderProvider<BeaconMapPoint, Object> {
		private List<BeaconMapPoint> points = List.of();
		private int index;

		@Override
		public void begin(MinimapElementRenderLocation location, Object context) {
			points = EMUtilsClient.config().beaconRadiusOutline()
				? BeaconRadiusRenderer.mapPoints()
				: List.of();
			index = 0;
		}

		@Override
		public boolean hasNext(MinimapElementRenderLocation location, Object context) {
			return index < points.size();
		}

		@Override
		public BeaconMapPoint getNext(MinimapElementRenderLocation location, Object context) {
			return points.get(index++);
		}

		@Override
		public void end(MinimapElementRenderLocation location, Object context) {
			points = List.of();
		}
	}

	private static final class Reader extends MinimapElementReader<BeaconMapPoint, Object> {
		@Override
		public boolean isHidden(BeaconMapPoint point, Object context) {
			return !EMUtilsClient.config().beaconRadiusOutline();
		}

		@Override
		public double getRenderX(BeaconMapPoint point, Object context, float partialTicks) {
			return point.x();
		}

		@Override
		public double getRenderY(BeaconMapPoint point, Object context, float partialTicks) {
			return point.y();
		}

		@Override
		public double getRenderZ(BeaconMapPoint point, Object context, float partialTicks) {
			return point.z();
		}

		@Override
		public int getInteractionBoxLeft(BeaconMapPoint point, Object context, float optionalScale) {
			return -1;
		}

		@Override
		public int getInteractionBoxRight(BeaconMapPoint point, Object context, float optionalScale) {
			return 1;
		}

		@Override
		public int getInteractionBoxTop(BeaconMapPoint point, Object context, float optionalScale) {
			return -1;
		}

		@Override
		public int getInteractionBoxBottom(BeaconMapPoint point, Object context, float optionalScale) {
			return 1;
		}

		@Override
		public int getRenderBoxLeft(BeaconMapPoint point, Object context, float optionalScale) {
			return -2;
		}

		@Override
		public int getRenderBoxRight(BeaconMapPoint point, Object context, float optionalScale) {
			return 2;
		}

		@Override
		public int getRenderBoxTop(BeaconMapPoint point, Object context, float optionalScale) {
			return -2;
		}

		@Override
		public int getRenderBoxBottom(BeaconMapPoint point, Object context, float optionalScale) {
			return 2;
		}

		@Override
		public int getLeftSideLength(BeaconMapPoint point, Minecraft client) {
			return 0;
		}

		@Override
		public String getMenuName(BeaconMapPoint point) {
			return "Beacon boundary";
		}

		@Override
		public String getFilterName(BeaconMapPoint point) {
			return "Beacon boundary";
		}

		@Override
		public int getMenuTextFillLeftPadding(BeaconMapPoint point) {
			return 0;
		}

		@Override
		public int getRightClickTitleBackgroundColor(BeaconMapPoint point) {
			return point.color();
		}

		@Override
		public boolean shouldScaleBoxWithOptionalScale() {
			return false;
		}

		@Override
		public boolean isInteractable(MinimapElementRenderLocation location, BeaconMapPoint point) {
			return false;
		}
	}
}
