package net.emutils.client.hud.layout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.emutils.client.hud.HudOverlayPlacement;

public final class HudLayoutSnapping {
	public static final int SNAP_DISTANCE = 8;
	public static final int STACK_GAP = 4;

	private HudLayoutSnapping() {
	}

	public record Bounds(int x, int y, int width, int height) {
		public int right() {
			return x + width;
		}

		public int bottom() {
			return y + height;
		}
	}

	public static HudOverlayPlacement.Position snap(
		HudOverlayPlacement.Position candidate,
		Bounds moving,
		Collection<Bounds> others,
		int screenWidth,
		int screenHeight
	) {
		int x = candidate.x();
		int y = candidate.y();
		List<Integer> xTargets = new ArrayList<>();
		List<Integer> yTargets = new ArrayList<>();

		xTargets.add(HudOverlayPlacement.MARGIN);
		xTargets.add((screenWidth - moving.width()) / 2);
		xTargets.add(screenWidth - HudOverlayPlacement.MARGIN - moving.width());
		yTargets.add(HudOverlayPlacement.MARGIN);
		yTargets.add((screenHeight - moving.height()) / 2);
		yTargets.add(screenHeight - HudOverlayPlacement.MARGIN - moving.height());

		for (Bounds other : others) {
			xTargets.add(other.x());
			xTargets.add(other.right() - moving.width());
			xTargets.add(other.right() + STACK_GAP);
			xTargets.add(other.x() - STACK_GAP - moving.width());
			xTargets.add(other.x() + (other.width() - moving.width()) / 2);

			yTargets.add(other.y());
			yTargets.add(other.bottom() - moving.height());
			yTargets.add(other.bottom() + STACK_GAP);
			yTargets.add(other.y() - STACK_GAP - moving.height());
			yTargets.add(other.y() + (other.height() - moving.height()) / 2);
		}

		x = snapAxis(x, xTargets);
		y = snapAxis(y, yTargets);
		return new HudOverlayPlacement.Position(
			clamp(x, 0, Math.max(0, screenWidth - moving.width())),
			clamp(y, 0, Math.max(0, screenHeight - moving.height()))
		);
	}

	private static int snapAxis(int value, List<Integer> targets) {
		int best = value;
		int bestDistance = SNAP_DISTANCE + 1;
		for (int target : targets) {
			int distance = Math.abs(value - target);
			if (distance <= SNAP_DISTANCE && distance < bestDistance) {
				bestDistance = distance;
				best = target;
			}
		}

		return best;
	}

	private static int clamp(int value, int min, int max) {
		return Math.min(max, Math.max(min, value));
	}
}
