package net.emutils.client.emutils.hud;

import java.util.ArrayList;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.ui.UiIcons;
import net.emutils.client.emutils.gui.ui.UiRasterScale;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.emutils.client.emutils.hud.layout.HudLayoutManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * The HUD Overlay (#93), in the look of the new UI: a rounded card in the settings UI's theme with
 * smooth icons, labels in one column and values lined up in the next. Values change every tick, so
 * they are drawn from cached glyphs ({@link UiText#drawGlyphs}) rather than a texture per string.
 */
public final class HudOverlayRenderer {
	private static final Identifier ID = Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "hud_overlay");
	private static final int PADDING_X = 8;
	private static final int PADDING_Y = 7;
	private static final int ROW_HEIGHT = 12;
	private static final int RADIUS = 9;
	private static final int SHADOW_BLUR = 8;
	private static final int ICON_SIZE = 9;
	private static final int ICON_GAP = 6;
	private static final int LABEL_VALUE_GAP = 8;
	private static final int MIN_CONTENT_WIDTH = 130;
	private static final int MEMORY_BAR_HEIGHT = 3;
	private static final int MEMORY_BAR_GAP = 3;
	/** Below this background opacity the world shows through, so text gets a shadow to stay readable. */
	private static final int TEXT_SHADOW_BELOW_OPACITY = 50;
	private static final UiText.Size LABEL_SIZE = UiText.Size.BODY;
	private static final UiText.Size VALUE_SIZE = UiText.Size.LABEL;

	private static HudOverlayData data = HudOverlayData.empty();

	private HudOverlayRenderer() {
	}

	public static void register() {
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ID, (context, tickCounter) -> render(context));
	}

	public static void tick(Minecraft client) {
		data = HudOverlayData.collect(client);
	}

	public static HudOverlayData hudOverlayData() {
		return data;
	}

	// The panel keeps its usual width and only grows while a line (a long biome name, for example) needs more room.
	public static int unscaledPanelWidth(EMUtilsConfig config) {
		Font font = Minecraft.getInstance().font;
		List<HudOverlayLine> lines = allLines(config);
		int labelWidth = labelColumnWidth(font, lines);
		int valueWidth = 0;
		for (HudOverlayLine line : lines) {
			valueWidth = Math.max(valueWidth, UiText.glyphsWidth(font, line.value(), VALUE_SIZE));
		}
		int iconWidth = config.hudShowIcons() ? ICON_SIZE + ICON_GAP : 0;
		return Math.max(MIN_CONTENT_WIDTH, iconWidth + labelWidth + LABEL_VALUE_GAP + valueWidth) + PADDING_X * 2;
	}

	public static int unscaledPanelHeight(EMUtilsConfig config) {
		List<HudOverlayLine> mainLines = mainLines(config);
		boolean showMemory = config.hudShowMemory();
		if (mainLines.isEmpty() && !showMemory) {
			return PADDING_Y * 2 + ROW_HEIGHT;
		}
		int height = PADDING_Y * 2 + mainLines.size() * ROW_HEIGHT;
		if (showMemory) {
			height += ROW_HEIGHT + MEMORY_BAR_GAP + MEMORY_BAR_HEIGHT;
		}
		return height;
	}

	public static void renderPanel(
		GuiGraphicsExtractor context,
		Minecraft client,
		EMUtilsConfig config,
		int x,
		int y,
		int panelWidth,
		int panelHeight
	) {
		renderPanel(context, client, config, x, y, panelWidth, panelHeight, config.hudBackgroundOpacity());
	}

	public static void renderPanel(
		GuiGraphicsExtractor context,
		Minecraft client,
		EMUtilsConfig config,
		int x,
		int y,
		int panelWidth,
		int panelHeight,
		int opacityPercent
	) {
		UiTheme theme = UiTheme.current();
		drawCard(context, theme, x, y, panelWidth, panelHeight, opacityPercent);

		List<HudOverlayLine> mainLines = mainLines(config);
		List<HudOverlayLine> lines = allLines(config);
		Font font = client.font;
		boolean showIcons = config.hudShowIcons();
		boolean shadow = opacityPercent < TEXT_SHADOW_BELOW_OPACITY;
		int textX = x + PADDING_X + (showIcons ? ICON_SIZE + ICON_GAP : 0);
		int valueX = textX + labelColumnWidth(font, lines) + LABEL_VALUE_GAP;
		int rowY = y + PADDING_Y;
		for (HudOverlayLine line : lines) {
			drawLine(context, font, theme, line, showIcons, shadow, x + PADDING_X, textX, valueX, rowY);
			rowY += ROW_HEIGHT;
		}
		if (config.hudShowMemory()) {
			int barY = y + PADDING_Y + (mainLines.size() + 1) * ROW_HEIGHT + MEMORY_BAR_GAP;
			drawMemoryBar(context, theme, x + PADDING_X, barY, panelWidth - PADDING_X * 2, data.memoryPercent());
		}
	}

	private static void render(GuiGraphicsExtractor context) {
		EMUtilsConfig config = EMUtilsClient.config();
		Minecraft client = Minecraft.getInstance();
		if (config == null || client == null || client.player == null || client.level == null) {
			return;
		}
		if (!config.hudOverlay() && !HudLayoutManager.isEditing()) {
			return;
		}
		if (EMUtilsClient.zoom() != null && EMUtilsClient.zoom().shouldHideHud()) {
			return;
		}
		if (config.hudHideWithDebug() && client.getDebugOverlay().showDebugScreen()) {
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

		int panelWidth = unscaledPanelWidth(config);
		int panelHeight = unscaledPanelHeight(config);
		HudLayoutManager.ResolvedLayout layout = HudLayoutManager.resolveLayout(
			net.emutils.client.EMUtilsHudElements.INFO_OVERLAY,
			config,
			context.guiWidth(),
			context.guiHeight(),
			client
		);
		if (layout.dimensions().height() <= 0 && !HudLayoutManager.isEditing()) {
			return;
		}

		context.pose().pushMatrix();
		UiRasterScale.set(layout.scaleFactor());
		try {
			context.pose().translate(layout.position().x(), layout.position().y());
			context.pose().scale(layout.scaleFactor(), layout.scaleFactor());
			renderPanel(context, client, config, 0, 0, panelWidth, panelHeight, layout.opacityPercent());
		} finally {
			UiRasterScale.reset();
			context.pose().popMatrix();
		}
	}

	private static List<HudOverlayLine> allLines(EMUtilsConfig config) {
		List<HudOverlayLine> lines = mainLines(config);
		if (config.hudShowMemory()) {
			lines.add(new HudOverlayLine(EMUtilsTexts.HUD_MEMORY, data.memory(), HudOverlayLine.icon("memory")));
		}
		return lines;
	}

	private static List<HudOverlayLine> mainLines(EMUtilsConfig config) {
		List<HudOverlayLine> lines = new ArrayList<>();
		if (config.hudShowCoordinates()) {
			lines.add(new HudOverlayLine(EMUtilsTexts.HUD_COORDS, data.coordinates(), HudOverlayLine.icon("coords")));
			if (EMUtilsClient.tweaks() != null && EMUtilsClient.tweaks().freeCamera().isActive()) {
				lines.add(new HudOverlayLine(EMUtilsTexts.HUD_FREE_CAMERA_COORDS, data.freeCameraCoordinates(), HudOverlayLine.icon("free_camera")));
			}
		}
		Minecraft client = Minecraft.getInstance();
		if (config.hudShowNetherCoordinates()
			&& client.level != null
			&& client.level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
			lines.add(new HudOverlayLine("emutils.hud.nether_coords", data.portalCoordinates(), HudOverlayLine.icon("nether")));
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
		if (config.hudShowSpeed()) {
			lines.add(new HudOverlayLine(EMUtilsTexts.HUD_SPEED, data.speed(), HudOverlayLine.icon("speed")));
		}
		if (config.hudShowServerTime()) {
			lines.add(new HudOverlayLine(EMUtilsTexts.HUD_SERVER_TIME, data.serverTime(), HudOverlayLine.icon("server_time")));
		}
		if (config.hudShowRealTime()) {
			lines.add(new HudOverlayLine(EMUtilsTexts.HUD_REAL_TIME, data.realTime(), HudOverlayLine.icon("real_time")));
		}
		if (EMUtilsClient.tweaks() != null && EMUtilsClient.tweaks().lockedYPlacement().active()) {
			lines.add(new HudOverlayLine(EMUtilsTexts.HUD_LOCKED_Y, data.lockedYPlacement(), HudOverlayLine.icon("locked_y")));
		}
		return lines;
	}

	private static int labelColumnWidth(Font font, List<HudOverlayLine> lines) {
		int width = 0;
		for (HudOverlayLine line : lines) {
			width = Math.max(width, UiText.width(font, Component.translatable(line.labelKey()), LABEL_SIZE));
		}
		return width;
	}

	private static void drawCard(GuiGraphicsExtractor context, UiTheme theme, int x, int y, int width, int height, int opacityPercent) {
		float opacity = Math.clamp(opacityPercent / 100.0F, 0.0F, 1.0F);
		if (opacity <= 0.0F) {
			return;
		}
		UiShapes.shadow(context, x, y, width, height, RADIUS, SHADOW_BLUR, UiTheme.fade(theme.shadow(), opacity));
		UiShapes.borderedRect(context, x, y, width, height, RADIUS, UiTheme.fade(theme.panel(), opacity), UiTheme.fade(theme.border(), opacity));
	}

	private static void drawLine(
		GuiGraphicsExtractor context,
		Font font,
		UiTheme theme,
		HudOverlayLine line,
		boolean showIcons,
		boolean shadow,
		int iconX,
		int textX,
		int valueX,
		int rowY
	) {
		int centerY = rowY + ROW_HEIGHT / 2;
		if (showIcons) {
			if (shadow) {
				float offset = shadowOffset();
				context.pose().pushMatrix();
				context.pose().translate(offset, offset);
				UiIcons.draw(context, line.icon(), iconX, centerY - ICON_SIZE / 2, ICON_SIZE, shadowColor());
				context.pose().popMatrix();
			}
			UiIcons.draw(context, line.icon(), iconX, centerY - ICON_SIZE / 2, ICON_SIZE, theme.muted());
		}

		Component label = Component.translatable(line.labelKey());
		int labelTop = centerY - UiText.lineHeight(font, LABEL_SIZE) / 2;
		int valueTop = centerY - UiText.lineHeight(font, VALUE_SIZE) / 2;
		if (shadow) {
			float offset = shadowOffset();
			int color = shadowColor();
			UiText.drawExact(context, font, label, LABEL_SIZE, textX + offset, labelTop + offset, color);
			UiText.drawGlyphs(context, font, line.value(), VALUE_SIZE, valueX + offset, valueTop + offset, color);
		}
		UiText.draw(context, font, label, LABEL_SIZE, textX, labelTop, theme.textSecondary());
		UiText.drawGlyphs(context, font, line.value(), VALUE_SIZE, valueX, valueTop, theme.text());
	}

	/** A dark shadow under the dark theme's light text, and a light one under the light theme's dark text. */
	private static int shadowColor() {
		return UiTheme.current() == UiTheme.LIGHT ? 0x99FFFFFF : 0x99000000;
	}

	/** One physical pixel, however the GUI and the element are scaled. */
	private static float shadowOffset() {
		return 1.0F / (float) (Minecraft.getInstance().getWindow().getGuiScale() * UiRasterScale.get());
	}

	private static void drawMemoryBar(GuiGraphicsExtractor context, UiTheme theme, int x, int y, int width, int memoryPercent) {
		UiShapes.pill(context, x, y, width, MEMORY_BAR_HEIGHT, theme.line());
		int fill = Math.round(width * Math.clamp(memoryPercent, 0, 100) / 100.0F);
		if (fill > 0) {
			UiShapes.pill(context, x, y, Math.max(MEMORY_BAR_HEIGHT, fill), MEMORY_BAR_HEIGHT, theme.accent());
		}
	}
}
