package net.emutils.client.emskyblock.features.inventory.estimateditemvalue;

import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emskyblock.config.EMSkyblockSettings;
import net.emutils.client.emhelpers.hud.layout.HudElementId;
import net.emutils.client.emhelpers.hud.layout.HudLayoutManager;
import net.emutils.client.emskyblock.context.SkyblockFeatures;
import net.emutils.client.emskyblock.features.inventory.estimateditemvalue.EstimatedItemValueResult.EstimatedItemValueLine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.util.Colors;

public final class EstimatedItemValueHudRenderer {
	private static final int ROW_HEIGHT = 10;
	private static final int PADDING_X = 6;
	private static final int PADDING_Y = 4;
	private static final int BACKGROUND_COLOR = 0xB5222B3D;
	private static final int SHADOW_COLOR = 0x66000000;
	private static final int BORDER_COLOR = 0xCC101725;

	private static final List<EstimatedItemValueLine> PREVIEW_LINES = List.of(
		EstimatedItemValueLine.header("§aEstimated Item Value:"),
		EstimatedItemValueLine.of("§7Base item: §eExample Item §7(§61.2M§7)", 1_200_000.0D, true),
		EstimatedItemValueLine.of("§7Reforge: §9Fabled", 0.0D, false),
		EstimatedItemValueLine.of(" §7Stone: §eDragon Claw §7(§6492k§7)", 492_000.0D, true),
		EstimatedItemValueLine.of("§aTotal: §6§l1.7M coins", 1_700_000.0D, true)
	);

	private EstimatedItemValueHudRenderer() {
	}

	public static void register() {
	}

	public static int unscaledPanelWidth(@SuppressWarnings("unused") EMUtilsConfig config) {
		return measureWidth(MinecraftClient.getInstance().textRenderer, previewLines()) + PADDING_X * 2;
	}

	public static int unscaledPanelHeight(@SuppressWarnings("unused") EMUtilsConfig config) {
		List<EstimatedItemValueLine> lines = previewLines();
		if (lines.isEmpty()) {
			return 0;
		}

		return PADDING_Y * 2 + lines.size() * ROW_HEIGHT;
	}

	public static void renderPanel(
		DrawContext context,
		EMUtilsConfig config,
		List<EstimatedItemValueLine> lines,
		int x,
		int y
	) {
		renderPanel(context, config, lines, x, y, EMSkyblockSettings.skyblockStatsHudBackgroundOpacity());
	}

	public static void renderPanel(
		DrawContext context,
		EMUtilsConfig config,
		List<EstimatedItemValueLine> lines,
		int x,
		int y,
		int opacityPercent
	) {
		if (lines.isEmpty()) {
			return;
		}

		TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
		int panelWidth = measureWidth(textRenderer, lines) + PADDING_X * 2;
		int panelHeight = PADDING_Y * 2 + lines.size() * ROW_HEIGHT;
		drawPanelBackground(context, x, y, panelWidth, panelHeight, opacityPercent);

		int cursorY = y + PADDING_Y;
		for (EstimatedItemValueLine line : lines) {
			context.drawTextWithShadow(textRenderer, line.text().asOrderedText(), x + PADDING_X, cursorY, Colors.WHITE);
			cursorY += ROW_HEIGHT;
		}
	}

	public static void renderOverlay(DrawContext context, MinecraftClient client) {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || client.player == null || client.world == null) {
			return;
		}
		if (!(client.currentScreen instanceof HandledScreen<?>)) {
			return;
		}
		if (!EMSkyblockSettings.skyblockEnabled() || !EMSkyblockSettings.estimatedItemValueHudEnabled()) {
			return;
		}
		if (!SkyblockFeatures.inSkyBlock(client) && !HudLayoutManager.isEditing()) {
			return;
		}
		if (EMUtilsClient.zoom() != null && EMUtilsClient.zoom().shouldHideHud()) {
			return;
		}
		if (HudLayoutManager.isEditing()) {
			return;
		}

		EstimatedItemValueResult result = EstimatedItemValueManager.get().current();
		if (result.isEmpty()) {
			return;
		}

		List<EstimatedItemValueLine> lines = result.lines();
		HudLayoutManager.ResolvedLayout layout = HudLayoutManager.resolveLayout(
			HudElementId.ESTIMATED_ITEM_VALUE,
			config,
			context.getScaledWindowWidth(),
			context.getScaledWindowHeight(),
			client
		);

		context.getMatrices().pushMatrix();
		try {
			context.getMatrices().translate(layout.position().x(), layout.position().y());
			context.getMatrices().scale(layout.scaleFactor(), layout.scaleFactor());
			renderPanel(context, config, lines, 0, 0, layout.opacityPercent());
		} finally {
			context.getMatrices().popMatrix();
		}
	}

	public static List<EstimatedItemValueLine> previewLines() {
		EstimatedItemValueResult result = EstimatedItemValueManager.get().current();
		if (!result.isEmpty()) {
			return result.lines();
		}

		return PREVIEW_LINES;
	}

	private static int measureWidth(TextRenderer textRenderer, List<EstimatedItemValueLine> lines) {
		int width = 0;
		for (EstimatedItemValueLine line : lines) {
			width = Math.max(width, textRenderer.getWidth(line.text()));
		}

		return width;
	}

	private static void drawPanelBackground(DrawContext context, int x, int y, int width, int height, int opacityPercent) {
		context.fill(x + 2, y + 2, x + width + 2, y + height + 2, withOpacity(SHADOW_COLOR, opacityPercent));
		context.fill(x, y, x + width, y + height, withOpacity(BORDER_COLOR, opacityPercent));
		context.fill(x + 1, y + 1, x + width - 1, y + height - 1, withOpacity(BACKGROUND_COLOR, opacityPercent));
	}

	private static int withOpacity(int color, int opacityPercent) {
		int alpha = color >>> 24;
		int scaledAlpha = Math.round(alpha * Math.min(100, Math.max(0, opacityPercent)) / 100.0F);
		return (scaledAlpha << 24) | (color & 0x00FFFFFF);
	}
}
