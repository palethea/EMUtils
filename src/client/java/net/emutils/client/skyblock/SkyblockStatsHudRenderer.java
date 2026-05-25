package net.emutils.client.skyblock;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.hud.HudOverlayPlacement;
import net.emutils.client.hud.layout.HudElementId;
import net.emutils.client.hud.layout.HudLayoutManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class SkyblockStatsHudRenderer {
	private static final Identifier ID = Identifier.of(EMUtilsClient.MOD_ID, "skyblock_stats_hud");
	private static final int PADDING_X = 10;
	private static final int PADDING_Y = 6;
	private static final int ROW_HEIGHT = 11;
	private static final int SEGMENT_GAP = 10;
	private static final int BACKGROUND_COLOR = 0xB5222B3D;
	private static final int SHADOW_COLOR = 0x66000000;
	private static final int BORDER_COLOR = 0xCC101725;
	private static final int HEALTH_COLOR = 0xFFFF5555;
	private static final int DEFENSE_COLOR = 0xFF55FF55;
	private static final int MANA_COLOR = 0xFF55FFFF;
	private static final int SOULFLOW_COLOR = 0xFF00AAAA;
	private static final SkyblockActionBarStats PREVIEW_STATS = new SkyblockActionBarStats(4612, 4237, 1032, 1212, 1212, 400);

	private SkyblockStatsHudRenderer() {
	}

	public static void register() {
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ID, (context, tickCounter) -> render(context));
	}

	public static int unscaledPanelWidth(EMUtilsConfig config, SkyblockActionBarStats stats) {
		return measureWidth(MinecraftClient.getInstance().textRenderer, config, stats) + PADDING_X * 2;
	}

	public static int unscaledPanelHeight(EMUtilsConfig config, SkyblockActionBarStats stats) {
		if (!hasVisibleStats(config, stats) && !HudLayoutManager.isEditing()) {
			return 0;
		}

		return PADDING_Y * 2 + ROW_HEIGHT;
	}

	public static void renderPanel(
		DrawContext context,
		EMUtilsConfig config,
		SkyblockActionBarStats stats,
		int x,
		int y,
		int panelWidth,
		int panelHeight
	) {
		TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
		drawPanelBackground(context, x, y, panelWidth, panelHeight, config.skyblockStatsHudBackgroundOpacity());
		drawStats(context, textRenderer, config, stats, x + PADDING_X, y + PADDING_Y);
	}

	private static void render(DrawContext context) {
		EMUtilsConfig config = EMUtilsClient.config();
		MinecraftClient client = MinecraftClient.getInstance();
		SkyblockActionBarManager manager = EMUtilsClient.skyblockActionBar();
		if (config == null || client == null || client.player == null || client.world == null || manager == null) {
			return;
		}
		if (!manager.active(client) && !HudLayoutManager.isEditing()) {
			return;
		}
		if (EMUtilsClient.zoom() != null && EMUtilsClient.zoom().shouldHideHud()) {
			return;
		}
		if (HudLayoutManager.isEditing()) {
			return;
		}

		SkyblockActionBarStats stats = previewStats(manager);
		if (!hasVisibleStats(config, stats) && !HudLayoutManager.isEditing()) {
			return;
		}

		int panelWidth = unscaledPanelWidth(config, stats);
		int panelHeight = unscaledPanelHeight(config, stats);
		float scale = config.skyblockStatsHudScale() / 100.0F;
		HudOverlayPlacement.PanelDimensions dimensions = new HudOverlayPlacement.PanelDimensions(
			Math.round(panelWidth * scale),
			Math.round(panelHeight * scale)
		);
		HudOverlayPlacement.Position position = HudLayoutManager.resolve(
			HudElementId.SKYBLOCK_STATS,
			config,
			context.getScaledWindowWidth(),
			context.getScaledWindowHeight(),
			dimensions,
			client
		);

		context.getMatrices().pushMatrix();
		try {
			context.getMatrices().translate(position.x(), position.y());
			context.getMatrices().scale(scale, scale);
			renderPanel(context, config, stats, 0, 0, panelWidth, panelHeight);
		} finally {
			context.getMatrices().popMatrix();
		}
	}

	public static SkyblockActionBarStats previewStats(@org.jspecify.annotations.Nullable SkyblockActionBarManager manager) {
		if (manager != null) {
			SkyblockActionBarStats stats = manager.stats();
			if (stats.hasAny()) {
				return stats;
			}
		}

		return PREVIEW_STATS;
	}

	private static void drawStats(
		DrawContext context,
		TextRenderer textRenderer,
		EMUtilsConfig config,
		SkyblockActionBarStats stats,
		int x,
		int y
	) {
		List<StatSegment> segments = visibleSegments(config, stats);
		int cursorX = x;
		for (StatSegment segment : segments) {
			context.drawTextWithShadow(textRenderer, segment.icon(), cursorX, y, segment.color());
			cursorX += textRenderer.getWidth(segment.icon()) + 3;
			context.drawTextWithShadow(textRenderer, segment.value(), cursorX, y, segment.color());
			cursorX += textRenderer.getWidth(segment.value()) + SEGMENT_GAP;
		}
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

	private static int measureWidth(TextRenderer textRenderer, EMUtilsConfig config, SkyblockActionBarStats stats) {
		int width = 0;
		List<StatSegment> segments = visibleSegments(config, stats);
		for (int index = 0; index < segments.size(); index++) {
			StatSegment segment = segments.get(index);
			width += textRenderer.getWidth(segment.icon()) + 3 + textRenderer.getWidth(segment.value());
			if (index + 1 < segments.size()) {
				width += SEGMENT_GAP;
			}
		}

		return width;
	}

	private static List<StatSegment> visibleSegments(EMUtilsConfig config, SkyblockActionBarStats stats) {
		List<StatSegment> segments = new ArrayList<>();
		if (config.skyblockStatsShowHealth() && stats.healthCurrent() != null && stats.healthMax() != null) {
			segments.add(new StatSegment(
				Text.literal("❤ "),
				Text.literal(formatNumber(stats.healthCurrent()) + "/" + formatNumber(stats.healthMax())),
				HEALTH_COLOR
			));
		}
		if (config.skyblockStatsShowDefense() && stats.defense() != null) {
			segments.add(new StatSegment(
				Text.literal("❈ "),
				Text.literal(formatNumber(stats.defense())),
				DEFENSE_COLOR
			));
		}
		if (config.skyblockStatsShowMana() && stats.manaCurrent() != null && stats.manaMax() != null) {
			segments.add(new StatSegment(
				Text.literal("✎ "),
				Text.literal(formatNumber(stats.manaCurrent()) + "/" + formatNumber(stats.manaMax())),
				MANA_COLOR
			));
		}
		if (config.skyblockStatsShowSoulflow() && stats.soulflow() != null) {
			segments.add(new StatSegment(
				Text.literal("ʬ "),
				Text.literal(formatNumber(stats.soulflow())),
				SOULFLOW_COLOR
			));
		}

		return segments;
	}

	private static boolean hasVisibleStats(EMUtilsConfig config, SkyblockActionBarStats stats) {
		return !visibleSegments(config, stats).isEmpty();
	}

	private static String formatNumber(int value) {
		return String.format(Locale.US, "%,d", value);
	}

	private record StatSegment(Text icon, Text value, int color) {
	}
}
