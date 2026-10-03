package xaero.map.element.render;

public abstract class ElementReader<E, C, R extends ElementRenderer<E, ?, R>> {
	public boolean isHidden(E element, C context) {
		return false;
	}

	public boolean isRightClickValid(E element) {
		return false;
	}
}
