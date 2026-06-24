package xaero.hud.minimap.element.render;

public abstract class MinimapElementRenderProvider<E, RC> {
	public abstract void begin(MinimapElementRenderLocation location, RC context);

	public abstract boolean hasNext(MinimapElementRenderLocation location, RC context);

	public abstract E getNext(MinimapElementRenderLocation location, RC context);

	public abstract void end(MinimapElementRenderLocation location, RC context);
}
