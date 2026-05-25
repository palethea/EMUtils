package net.emutils.client.hud.layout;

import java.util.EnumMap;
import java.util.Map;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.gui.hud.HudLayoutEditorScreen;
import net.emutils.client.hud.HudOverlayPlacement;
import net.minecraft.client.MinecraftClient;
import org.jspecify.annotations.Nullable;

public final class HudLayoutManager {
	private static final Map<HudElementId, HudOverlayPlacement.Position> draftPositions = new EnumMap<>(HudElementId.class);

	private HudLayoutManager() {
	}

	public static boolean isEditing() {
		return HudLayoutEditorContext.isActive(MinecraftClient.getInstance());
	}

	public static boolean isCustomMode() {
		EMUtilsConfig config = EMUtilsClient.config();
		return config != null && config.hudLayoutMode() == HudLayoutMode.CUSTOM;
	}

	public static HudOverlayPlacement.Position resolve(
		HudElementId id,
		EMUtilsConfig config,
		int screenWidth,
		int screenHeight,
		HudOverlayPlacement.PanelDimensions dimensions,
		@Nullable MinecraftClient client
	) {
		if (isEditing()) {
			HudOverlayPlacement.Position draft = draftPositions.get(id);
			if (draft != null) {
				return draft;
			}
		}

		if (isCustomMode()) {
			HudCustomLayoutEntry entry = config.hudCustomLayoutEntry(id);
			if (entry != null) {
				return new HudOverlayPlacement.Position(entry.x(), entry.y());
			}
		}

		return HudElementMetrics.anchorPosition(id, config, screenWidth, screenHeight, dimensions);
	}

	public static void openEditor(@Nullable MinecraftClient client) {
		if (client == null) {
			return;
		}

		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null) {
			return;
		}

		config.setHudLayoutMode(HudLayoutMode.CUSTOM);
		seedDraftFromCurrent(client, config);
		client.setScreen(new HudLayoutEditorScreen(client.currentScreen));
	}

	public static void seedDraftFromCurrent(MinecraftClient client, EMUtilsConfig config) {
		draftPositions.clear();
		int screenWidth = client.getWindow().getScaledWidth();
		int screenHeight = client.getWindow().getScaledHeight();
		for (HudElementId id : HudElementId.values()) {
			HudOverlayPlacement.PanelDimensions dimensions = HudElementMetrics.dimensions(id, config, client);
			if (dimensions.width() <= 0 || dimensions.height() <= 0) {
				continue;
			}

			HudCustomLayoutEntry saved = config.hudCustomLayoutEntry(id);
			HudOverlayPlacement.Position position = saved != null
				? new HudOverlayPlacement.Position(saved.x(), saved.y())
				: HudElementMetrics.anchorPosition(id, config, screenWidth, screenHeight, dimensions);
			draftPositions.put(id, position);
			config.setHudCustomLayoutEntry(id, position.x(), position.y());
		}
	}

	public static Map<HudElementId, HudOverlayPlacement.Position> draftPositions() {
		return draftPositions;
	}

	public static void setDraftPosition(HudElementId id, int x, int y) {
		draftPositions.put(id, new HudOverlayPlacement.Position(x, y));
	}

	public static void saveDraft(EMUtilsConfig config) {
		for (Map.Entry<HudElementId, HudOverlayPlacement.Position> entry : draftPositions.entrySet()) {
			config.setHudCustomLayoutEntry(entry.getKey(), entry.getValue().x(), entry.getValue().y());
		}
		config.save();
	}

	public static void clearDraft() {
		draftPositions.clear();
	}
}
