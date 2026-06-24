package xaero.hud.minimap.element.render;

import net.minecraft.client.Minecraft;

public abstract class MinimapElementReader<E, RC> {
	public abstract boolean isHidden(E element, RC context);

	public abstract double getRenderX(E element, RC context, float partialTicks);

	public abstract double getRenderY(E element, RC context, float partialTicks);

	public abstract double getRenderZ(E element, RC context, float partialTicks);

	public abstract int getInteractionBoxLeft(E element, RC context, float optionalScale);

	public abstract int getInteractionBoxRight(E element, RC context, float optionalScale);

	public abstract int getInteractionBoxTop(E element, RC context, float optionalScale);

	public abstract int getInteractionBoxBottom(E element, RC context, float optionalScale);

	public abstract int getRenderBoxLeft(E element, RC context, float optionalScale);

	public abstract int getRenderBoxRight(E element, RC context, float optionalScale);

	public abstract int getRenderBoxTop(E element, RC context, float optionalScale);

	public abstract int getRenderBoxBottom(E element, RC context, float optionalScale);

	public abstract int getLeftSideLength(E element, Minecraft client);

	public abstract String getMenuName(E element);

	public abstract String getFilterName(E element);

	public abstract int getMenuTextFillLeftPadding(E element);

	public abstract int getRightClickTitleBackgroundColor(E element);

	public abstract boolean shouldScaleBoxWithOptionalScale();

	public boolean isInteractable(MinimapElementRenderLocation location, E element) {
		return true;
	}
}
