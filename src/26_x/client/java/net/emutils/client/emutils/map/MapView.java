package net.emutils.client.emutils.map;

/**
 * Where the map is looking and how it is laid on screen (#212, #215): the world position at the drawing
 * area's middle, GUI pixels per block, the turn that puts the way you face up (0 keeps north up), and where
 * that middle is on screen.
 */
public record MapView(double centerX, double centerZ, float zoom, float angle, float screenCenterX, float screenCenterY) {
	public float screenX(double worldX, double worldZ) {
		double dx = (worldX - centerX) * zoom;
		double dz = (worldZ - centerZ) * zoom;
		return (float) (dx * Math.cos(angle) - dz * Math.sin(angle)) + screenCenterX;
	}

	public float screenY(double worldX, double worldZ) {
		double dx = (worldX - centerX) * zoom;
		double dz = (worldZ - centerZ) * zoom;
		return (float) (dx * Math.sin(angle) + dz * Math.cos(angle)) + screenCenterY;
	}

	public double worldX(float screenX, float screenY) {
		double sx = screenX - screenCenterX;
		double sy = screenY - screenCenterY;
		return centerX + (sx * Math.cos(angle) + sy * Math.sin(angle)) / zoom;
	}

	public double worldZ(float screenX, float screenY) {
		double sx = screenX - screenCenterX;
		double sy = screenY - screenCenterY;
		return centerZ + (-sx * Math.sin(angle) + sy * Math.cos(angle)) / zoom;
	}
}
