package net.emutils.client.hud.layout;

public final class HudCustomLayoutEntry {
	private Integer x;
	private Integer y;

	public int x() {
		return x == null ? 0 : x;
	}

	public int y() {
		return y == null ? 0 : y;
	}

	public void setX(int x) {
		this.x = x;
	}

	public void setY(int y) {
		this.y = y;
	}
}
