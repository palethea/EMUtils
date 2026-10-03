package xaero.map.element.render;

public abstract class ElementRenderProvider<E, C> {
	public abstract void begin(ElementRenderLocation location, C context);

	public abstract boolean hasNext(ElementRenderLocation location, C context);

	public abstract E getNext(ElementRenderLocation location, C context);

	public abstract void end(ElementRenderLocation location, C context);
}
