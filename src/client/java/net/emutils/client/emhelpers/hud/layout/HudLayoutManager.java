package net.emutils.client.emhelpers.hud.layout;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emhelpers.hud.editor.HudLayoutEditorScreen;
import net.emutils.client.emhelpers.hud.HudOverlayPlacement;
import net.minecraft.client.MinecraftClient;
import org.jspecify.annotations.Nullable;

/**
 * Central HUD layout: position and scale per {@link HudElementId} in {@link EMUtilsConfig#hudCustomLayout}.
 * New elements: add {@link HudElementId} + lang key, implement {@link HudLayoutElement},
 * {@code HudLayoutRegistry.register(new YourHudElement());} — no scale slider in feature settings.
 */
public final class HudLayoutManager {
	public static final int LAYOUT_SCALE_MIN = 10;
	public static final int LAYOUT_SCALE_MAX = 500;
	public static final int LAYOUT_OPACITY_MIN = 0;
	public static final int LAYOUT_OPACITY_MAX = 100;

	private static final Map<HudElementId, HudLayoutDraft> draftLayouts = new EnumMap<>(HudElementId.class);
	private static @Nullable Map<String, HudCustomLayoutEntry> layoutSnapshot;

	private HudLayoutManager() {
	}

	public record ResolvedLayout(
		HudOverlayPlacement.Position position,
		int scalePercent,
		int opacityPercent,
		HudOverlayPlacement.PanelDimensions dimensions
	) {
		public float scaleFactor() {
			return scalePercent / 100.0F;
		}
	}

	public static boolean isEditing() {
		return HudLayoutEditorContext.isActive(MinecraftClient.getInstance());
	}

	public static int clampLayoutScale(int scale) {
		return Math.max(LAYOUT_SCALE_MIN, Math.min(LAYOUT_SCALE_MAX, scale));
	}

	public static int clampLayoutOpacity(int opacity) {
		return Math.max(LAYOUT_OPACITY_MIN, Math.min(LAYOUT_OPACITY_MAX, opacity));
	}

	public static int layoutScale(HudElementId id, EMUtilsConfig config) {
		if (isEditing()) {
			HudLayoutDraft draft = draftLayouts.get(id);
			if (draft != null) {
				return draft.scale();
			}
		}

		HudCustomLayoutEntry entry = config.hudCustomLayoutEntry(id);
		if (entry != null && entry.hasStoredScale()) {
			return clampLayoutScale(entry.scale());
		}

		return 100;
	}

	public static int layoutOpacity(HudElementId id, EMUtilsConfig config) {
		if (isEditing()) {
			HudLayoutDraft draft = draftLayouts.get(id);
			if (draft != null) {
				return draft.opacity();
			}
		}

		HudCustomLayoutEntry entry = config.hudCustomLayoutEntry(id);
		if (entry != null && entry.hasStoredOpacity()) {
			return clampLayoutOpacity(entry.opacity());
		}

		return clampLayoutOpacity(HudLayoutRegistry.require(id).defaultOpacityPercent(config));
	}

	public static HudOverlayPlacement.PanelDimensions dimensions(
		HudElementId id,
		EMUtilsConfig config,
		MinecraftClient client
	) {
		int scale = layoutScale(id, config);
		return HudLayoutRegistry.require(id).scaledDimensions(config, client, scale);
	}

	public static ResolvedLayout resolveLayout(
		HudElementId id,
		EMUtilsConfig config,
		int screenWidth,
		int screenHeight,
		@Nullable MinecraftClient client
	) {
		int scale = layoutScale(id, config);
		HudLayoutElement element = HudLayoutRegistry.require(id);
		HudOverlayPlacement.PanelDimensions dimensions = element.scaledDimensions(config, client, scale);
		HudOverlayPlacement.Position position = resolve(
			id,
			config,
			screenWidth,
			screenHeight,
			dimensions,
			client
		);
		int opacity = layoutOpacity(id, config);
		return new ResolvedLayout(position, scale, opacity, dimensions);
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
			HudLayoutDraft draft = draftLayouts.get(id);
			if (draft != null) {
				return new HudOverlayPlacement.Position(draft.x(), draft.y());
			}
		}

		HudCustomLayoutEntry entry = config.hudCustomLayoutEntry(id);
		if (entry != null && entry.hasStoredPosition()) {
			return new HudOverlayPlacement.Position(entry.x(), entry.y());
		}

		return HudLayoutRegistry.require(id).defaultPosition(config, screenWidth, screenHeight, dimensions);
	}

	public static void openEditor(@Nullable MinecraftClient client) {
		if (client == null || HudLayoutEditorContext.isActive(client)) {
			return;
		}
		if (!beginEditorSession(client)) {
			return;
		}

		client.setScreen(new HudLayoutEditorScreen(client.currentScreen));
	}

	public static boolean beginEditorSession(@Nullable MinecraftClient client) {
		if (client == null) {
			return false;
		}

		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null) {
			return false;
		}

		takeLayoutSnapshot(config);
		seedDraftFromCurrent(client, config);
		return true;
	}

	public static void seedDraftFromCurrent(MinecraftClient client, EMUtilsConfig config) {
		draftLayouts.clear();
		int screenWidth = client.getWindow().getScaledWidth();
		int screenHeight = client.getWindow().getScaledHeight();
		for (HudLayoutElement element : HudLayoutRegistry.all()) {
			HudElementId id = element.id();
			int scale = layoutScale(id, config);
			HudOverlayPlacement.PanelDimensions dimensions = element.scaledDimensions(config, client, scale);
			if (dimensions.width() <= 0 || dimensions.height() <= 0) {
				continue;
			}

			HudCustomLayoutEntry saved = config.hudCustomLayoutEntry(id);
			HudOverlayPlacement.Position position = saved != null && saved.hasStoredPosition()
				? new HudOverlayPlacement.Position(saved.x(), saved.y())
				: element.defaultPosition(config, screenWidth, screenHeight, dimensions);
			int opacity = layoutOpacity(id, config);
			draftLayouts.put(id, new HudLayoutDraft(position.x(), position.y(), scale, opacity));
		}
	}

	public static Map<HudElementId, HudLayoutDraft> draftLayouts() {
		return draftLayouts;
	}

	public static void setDraftLayout(HudElementId id, int x, int y, int scale) {
		setDraftLayout(id, x, y, scale, 100);
	}

	public static void setDraftLayout(HudElementId id, int x, int y, int scale, int opacity) {
		draftLayouts.put(id, new HudLayoutDraft(x, y, clampLayoutScale(scale), clampLayoutOpacity(opacity)));
	}

	public static void setDraftPosition(HudElementId id, int x, int y) {
		HudLayoutDraft current = draftLayouts.get(id);
		int scale = current == null ? 100 : current.scale();
		int opacity = current == null ? 100 : current.opacity();
		draftLayouts.put(id, new HudLayoutDraft(x, y, scale, opacity));
	}

	public static void setDraftScale(HudElementId id, int scale) {
		HudLayoutDraft current = draftLayouts.get(id);
		if (current == null) {
			return;
		}

		draftLayouts.put(id, new HudLayoutDraft(current.x(), current.y(), clampLayoutScale(scale), current.opacity()));
	}

	public static void setDraftOpacity(HudElementId id, int opacity) {
		HudLayoutDraft current = draftLayouts.get(id);
		if (current == null) {
			return;
		}

		draftLayouts.put(id, new HudLayoutDraft(current.x(), current.y(), current.scale(), clampLayoutOpacity(opacity)));
	}

	public static void resetAllDraftsToDefaults(
		MinecraftClient client,
		EMUtilsConfig config,
		int screenWidth,
		int screenHeight
	) {
		for (HudLayoutElement element : HudLayoutRegistry.all()) {
			HudElementId id = element.id();
			int scale = 100;
			HudOverlayPlacement.PanelDimensions dimensions = element.scaledDimensions(config, client, scale);
			if (dimensions.width() <= 0 || dimensions.height() <= 0) {
				continue;
			}

			HudOverlayPlacement.Position position = element.defaultPosition(
				config,
				screenWidth,
				screenHeight,
				dimensions
			);
			draftLayouts.put(id, new HudLayoutDraft(
				position.x(),
				position.y(),
				scale,
				clampLayoutOpacity(element.defaultOpacityPercent(config))
			));
		}
	}

	public static void saveDraft(EMUtilsConfig config) {
		for (Map.Entry<HudElementId, HudLayoutDraft> entry : draftLayouts.entrySet()) {
			HudLayoutDraft draft = entry.getValue();
			config.setHudCustomLayoutEntry(entry.getKey(), draft.x(), draft.y(), draft.scale(), draft.opacity());
		}
		config.save();
		discardLayoutSnapshot();
	}

	public static void clearDraft() {
		draftLayouts.clear();
	}

	public static void cancelEditor(EMUtilsConfig config) {
		restoreLayoutSnapshot(config);
		clearDraft();
		discardLayoutSnapshot();
	}

	private static void takeLayoutSnapshot(EMUtilsConfig config) {
		layoutSnapshot = new LinkedHashMap<>();
		for (Map.Entry<String, HudCustomLayoutEntry> entry : config.hudCustomLayout().entrySet()) {
			layoutSnapshot.put(entry.getKey(), entry.getValue().copy());
		}
	}

	private static void restoreLayoutSnapshot(EMUtilsConfig config) {
		if (layoutSnapshot == null) {
			return;
		}

		config.hudCustomLayout().clear();
		config.hudCustomLayout().putAll(layoutSnapshot);
	}

	private static void discardLayoutSnapshot() {
		layoutSnapshot = null;
	}

	/** @deprecated Use {@link #draftLayouts()} and {@link HudLayoutDraft}. */
	@Deprecated
	public static Map<HudElementId, HudOverlayPlacement.Position> draftPositions() {
		Map<HudElementId, HudOverlayPlacement.Position> positions = new EnumMap<>(HudElementId.class);
		for (Map.Entry<HudElementId, HudLayoutDraft> entry : draftLayouts.entrySet()) {
			HudLayoutDraft draft = entry.getValue();
			positions.put(entry.getKey(), new HudOverlayPlacement.Position(draft.x(), draft.y()));
		}
		return positions;
	}
}
