package net.emutils.client.emhelpers.hud.layout;

public final class HudCustomLayoutEntry {
	private Integer x;
	private Integer y;
	private Integer scale;
	private Integer opacity;

	public int x() {
		return x == null ? 0 : x;
	}

	public int y() {
		return y == null ? 0 : y;
	}

	public int scale() {
		return scale == null ? 100 : scale;
	}

	public int opacity() {
		return opacity == null ? 100 : opacity;
	}

	public boolean hasStoredScale() {
		return scale != null;
	}

	public boolean hasStoredOpacity() {
		return opacity != null;
	}

	public boolean hasStoredPosition() {
		return x != null && y != null;
	}

	public void setX(int x) {
		this.x = x;
	}

	public void setY(int y) {
		this.y = y;
	}

	public void setScale(int scale) {
		this.scale = scale;
	}

	public void setOpacity(int opacity) {
		this.opacity = opacity;
	}

	public HudCustomLayoutEntry copy() {
		HudCustomLayoutEntry copy = new HudCustomLayoutEntry();
		copy.x = x;
		copy.y = y;
		copy.scale = scale;
		copy.opacity = opacity;
		return copy;
	}
}
