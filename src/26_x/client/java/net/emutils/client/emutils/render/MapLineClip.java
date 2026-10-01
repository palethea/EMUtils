package net.emutils.client.emutils.render;

import org.jspecify.annotations.Nullable;

/**
 * Trims a line segment to what a map shows (#188): the rectangle {@code |x| <= w, |y| <= h} of a square
 * minimap, or the circle of a round one, both around the origin. Results are written into one reused array,
 * so a frame's worth of lines allocates nothing.
 */
public final class MapLineClip {
	private static final double[] RESULT = new double[4];

	private MapLineClip() {
	}

	/** The part of the segment inside the rectangle, as {x1, y1, x2, y2}, or null when none is. The array is reused. */
	public static double @Nullable [] toRectangle(double x1, double y1, double x2, double y2, double w, double h) {
		double t0 = 0.0D;
		double t1 = 1.0D;
		double dx = x2 - x1;
		double dy = y2 - y1;
		double[] p = {-dx, dx, -dy, dy};
		double[] q = {x1 + w, w - x1, y1 + h, h - y1};
		for (int i = 0; i < 4; i++) {
			if (p[i] == 0.0D) {
				if (q[i] < 0.0D) {
					return null;
				}
			} else {
				double t = q[i] / p[i];
				if (p[i] < 0.0D) {
					t0 = Math.max(t0, t);
				} else {
					t1 = Math.min(t1, t);
				}
			}
		}
		return t0 > t1 ? null : result(x1, y1, dx, dy, t0, t1);
	}

	/** The part of the segment inside the circle of radius {@code r}, as {x1, y1, x2, y2}, or null when none is. The array is reused. */
	public static double @Nullable [] toCircle(double x1, double y1, double x2, double y2, double r) {
		double dx = x2 - x1;
		double dy = y2 - y1;
		double a = dx * dx + dy * dy;
		double b = 2.0D * (x1 * dx + y1 * dy);
		double c = x1 * x1 + y1 * y1 - r * r;
		if (a < 1.0E-12D) {
			return c <= 0.0D ? result(x1, y1, 0.0D, 0.0D, 0.0D, 1.0D) : null;
		}
		double discriminant = b * b - 4.0D * a * c;
		if (discriminant < 0.0D) {
			return null;
		}
		double root = Math.sqrt(discriminant);
		double t0 = Math.max(0.0D, (-b - root) / (2.0D * a));
		double t1 = Math.min(1.0D, (-b + root) / (2.0D * a));
		return t0 > t1 ? null : result(x1, y1, dx, dy, t0, t1);
	}

	private static double[] result(double x1, double y1, double dx, double dy, double t0, double t1) {
		RESULT[0] = x1 + t0 * dx;
		RESULT[1] = y1 + t0 * dy;
		RESULT[2] = x1 + t1 * dx;
		RESULT[3] = y1 + t1 * dy;
		return RESULT;
	}
}
