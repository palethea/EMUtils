package xaero.map.mods.gui;

import xaero.lib.client.graphics.XaeroBufferProvider;
import xaero.map.element.MapElementGraphics;
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.element.render.ElementRenderer;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;

public final class WaypointRenderer extends ElementRenderer<Waypoint, WaypointRenderContext, WaypointRenderer> {
	private WaypointRenderer() {
		super(null, null, null);
	}

	@Override
	public void preRender(ElementRenderInfo renderInfo, XaeroBufferProvider buffers, MultiTextureRenderTypeRendererProvider renderTypes, boolean shadow) {
	}

	@Override
	public void postRender(ElementRenderInfo renderInfo, XaeroBufferProvider buffers, MultiTextureRenderTypeRendererProvider renderTypes, boolean shadow) {
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
		return false;
	}

	@Override
	public boolean shouldRender(ElementRenderLocation location, boolean shadow) {
		return false;
	}
}
