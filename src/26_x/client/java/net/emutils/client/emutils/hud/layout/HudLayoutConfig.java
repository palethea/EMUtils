package net.emutils.client.emutils.hud.layout;

import java.util.Map;
import org.jspecify.annotations.Nullable;

public interface HudLayoutConfig {
	HudLayoutMode hudLayoutMode();

	void setHudLayoutMode(HudLayoutMode mode);

	@Nullable
	HudCustomLayoutEntry hudCustomLayoutEntry(HudElementId id);

	void setHudCustomLayoutEntry(HudElementId id, int x, int y, int scale, int opacity);

	Map<String, HudCustomLayoutEntry> hudCustomLayout();

	void save();
}
