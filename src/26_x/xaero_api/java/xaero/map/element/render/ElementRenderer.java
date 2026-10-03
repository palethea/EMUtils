package xaero.map.element.render;

import net.minecraft.world.entity.Entity;
import xaero.lib.client.graphics.XaeroBufferProvider;
import xaero.map.element.MapElementGraphics;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;

public abstract class ElementRenderer<E, C, R extends ElementRenderer<E, C, R>> implements Comparable<ElementRenderer<?, ?, ?>> {
	protected ElementRenderer(C context, ElementRenderProvider<E, C> provider, ElementReader<E, C, R> reader) {
	}

	public C getContext() {
		return null;
	}

	public int getOrder() {
		return 0;
	}

	public boolean shouldBeDimScaled() {
		return true;
	}

	@Override
	public int compareTo(ElementRenderer<?, ?, ?> other) {
		return 0;
	}

	public abstract void preRender(ElementRenderInfo renderInfo, XaeroBufferProvider buffers, MultiTextureRenderTypeRendererProvider renderTypes, boolean shadow);

	public abstract void postRender(ElementRenderInfo renderInfo, XaeroBufferProvider buffers, MultiTextureRenderTypeRendererProvider renderTypes, boolean shadow);

	public abstract void renderElementShadow(
		E element,
		boolean hovered,
		float optionalScale,
		double partialX,
		double partialY,
		ElementRenderInfo renderInfo,
		MapElementGraphics graphics,
		XaeroBufferProvider buffers,
		MultiTextureRenderTypeRendererProvider renderTypes
	);

	public abstract boolean renderElement(
		E element,
		boolean hovered,
		double optionalDepth,
		float optionalScale,
		double partialX,
		double partialY,
		ElementRenderInfo renderInfo,
		MapElementGraphics graphics,
		XaeroBufferProvider buffers,
		MultiTextureRenderTypeRendererProvider renderTypes
	);

	public abstract boolean shouldRender(ElementRenderLocation location, boolean shadow);
}
