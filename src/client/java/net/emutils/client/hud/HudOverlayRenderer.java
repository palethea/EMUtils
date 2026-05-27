package net.emutils.client.hud;

import java.util.ArrayList;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.util.EMUtilsTexts;
import net.emutils.client.hud.layout.HudElementId;
import net.emutils.client.hud.layout.HudLayoutManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;

public final class HudOverlayRenderer {
	private static final Identifier ID = Identifier.of(EMUtilsClient.MOD_ID, "hud_overlay");
	private static final int PADDING_X = 8;
	private static final int PADDING_Y = 7;
	private static final int ROW_HEIGHT = 13;
	private static final int ICON_SIZE = 8;
	private static final int ICON_GAP = 6;
	private static final int LABEL_VALUE_GAP = 6;
	private static final int FIXED_CONTENT_WIDTH = 156;
	private static final int BACKGROUND_COLOR = 0xB5222B3D;
	private static final int SHADOW_COLOR = 0x66000000;
	private static final int BORDER_COLOR = 0xCC101725;
	private static final int VALUE_COLOR = 0xFF20F050;
	private static final int ACCENT_COLOR = 0xFF20F050;
	private static final int MEMORY_TRACK_COLOR = 0xFF101725;
	private static final int MEMORY_BAR_HEIGHT = 3;
	private static final int MEMORY_BAR_GAP = 3;

	private static HudOverlayData data = HudOverlayData.empty();

	private HudOverlayRenderer() {
	}

	public static void register() {
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ID, (context, tickCounter) -> render(context));
	}

	public static void tick(MinecraftClient client) {
		data = HudOverlayData.collect(client);
	}

	public static HudOverlayData hudOverlayData() {
		return data;
	}

	public static int unscaledPanelWidth() {
		return FIXED_CONTENT_WIDTH + PADDING_X * 2;
	}

	public static int unscaledPanelHeight(EMUtilsConfig config) {
		List<HudOverlayLine> mainLines = mainLines(config);
		boolean showMemory = config.hudShowMemory();
		if (mainLines.isEmpty() && !showMemory) {
			return PADDING_Y * 2 + ROW_HEIGHT;
		}

		int mainLineCount = mainLines.size();
		int panelHeight = PADDING_Y + mainLineCount * ROW_HEIGHT;
		if (showMemory) {
			panelHeight += ROW_HEIGHT + MEMORY_BAR_GAP + MEMORY_BAR_HEIGHT + PADDING_Y;
		} else {
			panelHeight += PADDING_Y;
		}

		return panelHeight;
	}

	public static void renderPanel(
		DrawContext context,
		MinecraftClient client,
		EMUtilsConfig config,
		int x,
		int y,
		int panelWidth,
		int panelHeight
	) {
		renderPanel(context, client, config, x, y, panelWidth, panelHeight, config.hudBackgroundOpacity());
	}

	public static void renderPanel(
		DrawContext context,
		MinecraftClient client,
		EMUtilsConfig config,
		int x,
		int y,
		int panelWidth,
		int panelHeight,
		int opacityPercent
	) {
		List<HudOverlayLine> mainLines = mainLines(config);
		boolean showMemory = config.hudShowMemory();
		TextRenderer textRenderer = client.textRenderer;
		boolean showIcons = config.hudShowIcons();
		drawPanelBackground(context, x, y, panelWidth, panelHeight, opacityPercent);
		drawLines(context, textRenderer, mainLines, showIcons, x + PADDING_X, y + PADDING_Y);

		if (showMemory) {
			int memoryRowY = y + PADDING_Y + mainLines.size() * ROW_HEIGHT;
			drawLine(
				context,
				textRenderer,
				new HudOverlayLine(EMUtilsTexts.HUD_MEMORY, data.memory(), HudOverlayLine.icon("memory")),
				showIcons,
				x + PADDING_X,
				memoryRowY
			);
			drawMemoryBar(context, x + PADDING_X, memoryRowY + ROW_HEIGHT + MEMORY_BAR_GAP, data.memoryPercent());
		}
	}

	private static void render(DrawContext context) {
		EMUtilsConfig config = EMUtilsClient.config();
		MinecraftClient client = MinecraftClient.getInstance();
		if (config == null || client == null || client.player == null || client.world == null) {
			return;
		}
		if (!config.hudOverlay() && !HudLayoutManager.isEditing()) {
			return;
		}
		if (EMUtilsClient.zoom() != null && EMUtilsClient.zoom().shouldHideHud()) {
			return;
		}
		if (config.hudHideWithDebug() && client.getDebugHud().shouldShowDebugHud()) {
			return;
		}
		if (HudLayoutManager.isEditing()) {
			return;
		}

		List<HudOverlayLine> mainLines = mainLines(config);
		boolean showMemory = config.hudShowMemory();
		if (mainLines.isEmpty() && !showMemory && !HudLayoutManager.isEditing()) {
			return;
		}

		int panelWidth = unscaledPanelWidth();
		int panelHeight = unscaledPanelHeight(config);
		HudLayoutManager.ResolvedLayout layout = HudLayoutManager.resolveLayout(
			HudElementId.INFO_OVERLAY,
			config,
			context.getScaledWindowWidth(),
			context.getScaledWindowHeight(),
			client
		);
		if (layout.dimensions().height() <= 0 && !HudLayoutManager.isEditing()) {
			return;
		}

		context.getMatrices().pushMatrix();
		try {
			context.getMatrices().translate(layout.position().x(), layout.position().y());
			context.getMatrices().scale(layout.scaleFactor(), layout.scaleFactor());
			renderPanel(context, client, config, 0, 0, panelWidth, panelHeight, layout.opacityPercent());
		} finally {
			context.getMatrices().popMatrix();
		}
	}

	private static List<HudOverlayLine> mainLines(EMUtilsConfig config) {
		List<HudOverlayLine> lines = new ArrayList<>();
		if (config.hudShowCoordinates()) {
			lines.add(new HudOverlayLine(EMUtilsTexts.HUD_COORDS, data.coordinates(), HudOverlayLine.icon("coords")));
		}
		if (config.hudShowChunkRegion()) {
			lines.add(new HudOverlayLine(EMUtilsTexts.HUD_CHUNK_REGION, data.chunkRegion(), HudOverlayLine.icon("chunk")));
		}
		if (config.hudShowBiome()) {
			lines.add(new HudOverlayLine(EMUtilsTexts.HUD_BIOME, data.biome(), HudOverlayLine.icon("biome")));
		}
		if (config.hudShowPing()) {
			lines.add(new HudOverlayLine(EMUtilsTexts.HUD_PING, data.ping(), HudOverlayLine.icon("ping")));
		}
		if (config.hudShowFps()) {
			lines.add(new HudOverlayLine(EMUtilsTexts.HUD_FPS, data.fps(), HudOverlayLine.icon("fps")));
		}
		if (config.hudShowFacing()) {
			lines.add(new HudOverlayLine(EMUtilsTexts.HUD_FACING, data.facing(), HudOverlayLine.icon("direction")));
		}
		if (config.hudShowServerTime()) {
			lines.add(new HudOverlayLine(EMUtilsTexts.HUD_SERVER_TIME, data.serverTime(), HudOverlayLine.icon("server_time")));
		}
		if (config.hudShowRealTime()) {
			lines.add(new HudOverlayLine(EMUtilsTexts.HUD_REAL_TIME, data.realTime(), HudOverlayLine.icon("real_time")));
		}
		return lines;
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

	private static void drawMemoryBar(DrawContext context, int x, int y, int memoryPercent) {
		int fillWidth = Math.max(1, (int) Math.round(FIXED_CONTENT_WIDTH * Math.min(100, Math.max(0, memoryPercent)) / 100.0));
		context.fill(x, y, x + FIXED_CONTENT_WIDTH, y + MEMORY_BAR_HEIGHT, MEMORY_TRACK_COLOR);
		context.fill(x, y, x + fillWidth, y + MEMORY_BAR_HEIGHT, ACCENT_COLOR);
	}

	private static void drawLines(
		DrawContext context,
		TextRenderer textRenderer,
		List<HudOverlayLine> lines,
		boolean showIcons,
		int x,
		int y
	) {
		for (int index = 0; index < lines.size(); index++) {
			drawLine(context, textRenderer, lines.get(index), showIcons, x, y + index * ROW_HEIGHT);
		}
	}

	private static void drawLine(
		DrawContext context,
		TextRenderer textRenderer,
		HudOverlayLine line,
		boolean showIcons,
		int x,
		int rowY
	) {
		int textX = x;
		if (showIcons) {
			context.drawTexture(RenderPipelines.GUI_TEXTURED, line.icon(), x, rowY + 1, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
			textX += ICON_SIZE + ICON_GAP;
		}

		Text label = Text.translatable(line.labelKey());
		Text value = Text.literal(line.value());
		context.drawTextWithShadow(textRenderer, label, textX, rowY, Colors.WHITE);
		context.drawTextWithShadow(
			textRenderer,
			value,
			textX + textRenderer.getWidth(label) + LABEL_VALUE_GAP,
			rowY,
			VALUE_COLOR
		);
	}
}
