package net.emutils.client.skyblock.tracker;

import java.util.EnumMap;
import java.util.Map;
import net.emutils.client.hud.layout.HudElementId;
import org.jspecify.annotations.Nullable;

public record TrackerHudHitbox(
	HudElementId elementId,
	int modeClickX,
	int modeClickY,
	int modeClickWidth,
	int modeClickHeight
) {
	private static final Map<HudElementId, TrackerHudHitbox> HITBOXES = new EnumMap<>(HudElementId.class);

	public boolean containsModeClick(double mouseX, double mouseY) {
		return mouseX >= modeClickX
			&& mouseX < modeClickX + modeClickWidth
			&& mouseY >= modeClickY
			&& mouseY < modeClickY + modeClickHeight;
	}

	public static void register(TrackerHudHitbox hitbox) {
		HITBOXES.put(hitbox.elementId(), hitbox);
	}

	public static void clear(HudElementId id) {
		HITBOXES.remove(id);
	}

	public static void clearAll() {
		HITBOXES.clear();
	}

	@Nullable
	public static TrackerHudHitbox atModeClick(double mouseX, double mouseY) {
		for (TrackerHudHitbox hitbox : HITBOXES.values()) {
			if (hitbox.containsModeClick(mouseX, mouseY)) {
				return hitbox;
			}
		}

		return null;
	}
}
