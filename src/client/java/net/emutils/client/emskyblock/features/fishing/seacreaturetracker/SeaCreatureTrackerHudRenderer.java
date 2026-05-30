package net.emutils.client.emskyblock.features.fishing.seacreaturetracker;

import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emhelpers.hud.layout.HudElementId;
import net.emutils.client.emhelpers.hud.layout.HudLayoutManager;
import net.emutils.client.emskyblock.config.EMSkyblockSettings;
import net.emutils.client.emskyblock.features.fishing.trackercommon.TrackerDisplayMode;
import net.emutils.client.emskyblock.features.fishing.trackercommon.TrackerHudHitbox;
import net.emutils.client.emskyblock.features.fishing.trackercommon.TrackerPanelLine;
import net.emutils.client.emskyblock.features.fishing.trackercommon.TrackerPanelRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public final class SeaCreatureTrackerHudRenderer {
	private static final Identifier ID = Identifier.of(EMUtilsClient.MOD_ID, "sea_creature_tracker");

	private SeaCreatureTrackerHudRenderer() {
	}

	public static void register() {
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ID, (context, tickCounter) -> render(context));
	}

	public static int unscaledPanelWidth(EMUtilsConfig config) {
		TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
		return TrackerPanelRenderer.panelWidth(textRenderer, previewLines());
	}

	public static int unscaledPanelHeight(EMUtilsConfig config) {
		return TrackerPanelRenderer.panelHeight(previewLines());
	}

	public static List<TrackerPanelLine> previewLines() {
		return SeaCreatureTrackerManager.lines(MinecraftClient.getInstance(), true);
	}

	public static void renderPanel(
		DrawContext context,
		EMUtilsConfig config,
		List<TrackerPanelLine> lines,
		int x,
		int y,
		int opacityPercent
	) {
		TrackerPanelRenderer.render(
			context,
			MinecraftClient.getInstance().textRenderer,
			lines,
			x,
			y,
			opacityPercent
		);
	}

	private static void render(DrawContext context) {
		EMUtilsConfig config = EMUtilsClient.config();
		MinecraftClient client = MinecraftClient.getInstance();
		TrackerHudHitbox.clear(HudElementId.SEA_CREATURE_TRACKER);
		if (config == null || client.player == null || client.world == null) {
			return;
		}

		boolean editing = HudLayoutManager.isEditing();
		if (!SeaCreatureTrackerManager.shouldShow(client) && !editing) {
			return;
		}

		if (EMUtilsClient.zoom() != null && EMUtilsClient.zoom().shouldHideHud() && !editing) {
			return;
		}

		if (editing) {
			return;
		}

		List<TrackerPanelLine> lines = SeaCreatureTrackerManager.lines(client, false);
		if (lines.isEmpty()) {
			return;
		}

		HudLayoutManager.ResolvedLayout layout = HudLayoutManager.resolveLayout(
			HudElementId.SEA_CREATURE_TRACKER,
			config,
			context.getScaledWindowWidth(),
			context.getScaledWindowHeight(),
			client
		);

		TrackerDisplayMode mode = SeaCreatureTrackerManager.displayMode();
		if (client.mouse != null && client.getWindow() != null) {
			double scale = client.getWindow().getScaleFactor();
			double mouseX = client.mouse.getX() / scale;
			double mouseY = client.mouse.getY() / scale;
			if (TrackerPanelRenderer.isModeClickHovered(
				layout.position().x(), layout.position().y(), layout.scaleFactor(),
				client.textRenderer, lines, mouseX, mouseY
			)) {
				lines.add(TrackerPanelLine.of(TrackerPanelLine.TrackerHeaderParts.tooltipLine(mode)));
			}
		}

		context.getMatrices().pushMatrix();
		try {
			context.getMatrices().translate(layout.position().x(), layout.position().y());
			context.getMatrices().scale(layout.scaleFactor(), layout.scaleFactor());
			renderPanel(context, config, lines, 0, 0, layout.opacityPercent());
			TrackerPanelRenderer.registerModeClickHitbox(
				HudElementId.SEA_CREATURE_TRACKER,
				layout.position().x(),
				layout.position().y(),
				layout.scaleFactor(),
				client.textRenderer,
				lines
			);
		} finally {
			context.getMatrices().popMatrix();
		}
	}
}
