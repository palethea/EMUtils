package xaero.hud.minimap.element.render;

import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.lib.client.graphics.XaeroBufferProvider;

public abstract class MinimapElementRenderer<E, RC> {
	protected MinimapElementRenderer(
		MinimapElementReader<E, RC> reader,
		MinimapElementRenderProvider<E, RC> provider,
		RC context
	) {
	}

	public int getOrder() {
		return 0;
	}

	public abstract boolean renderElement(
		E element,
		boolean highlighted,
		boolean outOfBounds,
		double depth,
		float optionalScale,
		double partialX,
		double partialZ,
		MinimapElementRenderInfo renderInfo,
		MinimapElementGraphics graphics,
		XaeroBufferProvider buffers
	);

	public abstract void preRender(
		MinimapElementRenderInfo renderInfo,
		XaeroBufferProvider buffers,
		MultiTextureRenderTypeRendererProvider renderTypes
	);

	public abstract void postRender(
		MinimapElementRenderInfo renderInfo,
		XaeroBufferProvider buffers,
		MultiTextureRenderTypeRendererProvider renderTypes
	);

	public abstract boolean shouldRender(MinimapElementRenderLocation location);
}
