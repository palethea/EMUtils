package net.emutils.client.skyblock.tracker;

import java.util.List;
import net.emutils.client.hud.layout.HudElementId;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Colors;
import org.jspecify.annotations.Nullable;

public final class TrackerPanelRenderer {
	public static final int ROW_HEIGHT = 10;
	public static final int PADDING_X = 6;
	public static final int PADDING_Y = 4;
	private static final int BACKGROUND_COLOR = 0xB5222B3D;
	private static final int SHADOW_COLOR = 0x66000000;
	private static final int BORDER_COLOR = 0xCC101725;
	private static final int MODE_UNDERLINE = 0xFFAAAAAA;

	private TrackerPanelRenderer() {
	}

	public static int measureWidth(TextRenderer textRenderer, List<TrackerPanelLine> lines) {
		int width = 0;
		for (TrackerPanelLine line : lines) {
			if (line.isHeader() && line.headerParts() != null) {
				TrackerPanelLine.TrackerHeaderParts parts = line.headerParts();
				width = Math.max(
					width,
					textRenderer.getWidth(parts.prefixLegacy())
						+ textRenderer.getWidth(parts.modeLegacy())
						+ textRenderer.getWidth(parts.clickLegacy())
				);
			} else {
				width = Math.max(width, textRenderer.getWidth(line.text()));
			}
		}

		return width;
	}

	public static int panelHeight(List<TrackerPanelLine> lines) {
		if (lines.isEmpty()) {
			return 0;
		}

		return PADDING_Y * 2 + lines.size() * ROW_HEIGHT;
	}

	public static int panelWidth(TextRenderer textRenderer, List<TrackerPanelLine> lines) {
		return measureWidth(textRenderer, lines) + PADDING_X * 2;
	}

	public static void render(
		DrawContext context,
		TextRenderer textRenderer,
		List<TrackerPanelLine> lines,
		int x,
		int y,
		int opacityPercent
	) {
		if (lines.isEmpty()) {
			return;
		}

		int panelWidth = panelWidth(textRenderer, lines);
		int panelHeight = panelHeight(lines);
		drawBackground(context, x, y, panelWidth, panelHeight, opacityPercent);

		int cursorY = y + PADDING_Y;
		for (TrackerPanelLine line : lines) {
			if (line.isHeader() && line.headerParts() != null) {
				cursorY = renderHeader(context, textRenderer, line.headerParts(), x + PADDING_X, cursorY, opacityPercent);
			} else {
				context.drawTextWithShadow(textRenderer, line.text().asOrderedText(), x + PADDING_X, cursorY, Colors.WHITE);
				cursorY += ROW_HEIGHT;
			}
		}
	}

	@Nullable
	public static TrackerHudHitbox registerModeClickHitbox(
		HudElementId elementId,
		int screenPanelX,
		int screenPanelY,
		float scaleFactor,
		TextRenderer textRenderer,
		List<TrackerPanelLine> lines
	) {
		if (lines.isEmpty() || !(lines.getFirst().isHeader()) || lines.getFirst().headerParts() == null) {
			return null;
		}

		TrackerPanelLine.TrackerHeaderParts parts = lines.getFirst().headerParts();
		int prefixWidth = textRenderer.getWidth(parts.prefixLegacy());
		int modeClickWidth = textRenderer.getWidth(parts.modeLegacy() + parts.clickLegacy());
		int localModeX = PADDING_X + prefixWidth;
		int localModeY = PADDING_Y;
		int scaledModeX = screenPanelX + Math.round(localModeX * scaleFactor);
		int scaledModeY = screenPanelY + Math.round(localModeY * scaleFactor);
		int scaledModeW = Math.round(modeClickWidth * scaleFactor);
		int scaledModeH = Math.round(ROW_HEIGHT * scaleFactor);
		TrackerHudHitbox hitbox = new TrackerHudHitbox(
			elementId,
			scaledModeX,
			scaledModeY,
			Math.max(1, scaledModeW),
			Math.max(1, scaledModeH)
		);
		TrackerHudHitbox.register(hitbox);
		return hitbox;
	}

	private static int renderHeader(
		DrawContext context,
		TextRenderer textRenderer,
		TrackerPanelLine.TrackerHeaderParts parts,
		int x,
		int y,
		int opacityPercent
	) {
		int cursorX = x;
		context.drawTextWithShadow(textRenderer, parts.prefixLegacy(), cursorX, y, Colors.WHITE);
		cursorX += textRenderer.getWidth(parts.prefixLegacy());

		int modeStartX = cursorX;
		context.drawTextWithShadow(textRenderer, parts.modeLegacy(), cursorX, y, Colors.WHITE);
		cursorX += textRenderer.getWidth(parts.modeLegacy());
		context.drawTextWithShadow(textRenderer, parts.clickLegacy(), cursorX, y, Colors.WHITE);
		int modeEndX = cursorX + textRenderer.getWidth(parts.clickLegacy());

		context.fill(
			modeStartX,
			y + ROW_HEIGHT - 2,
			modeEndX,
			y + ROW_HEIGHT - 1,
			withOpacity(MODE_UNDERLINE, opacityPercent)
		);

		return y + ROW_HEIGHT;
	}

	public static void drawBackground(DrawContext context, int x, int y, int width, int height, int opacityPercent) {
		context.fill(x + 2, y + 2, x + width + 2, y + height + 2, withOpacity(SHADOW_COLOR, opacityPercent));
		context.fill(x, y, x + width, y + height, withOpacity(BORDER_COLOR, opacityPercent));
		context.fill(x + 1, y + 1, x + width - 1, y + height - 1, withOpacity(BACKGROUND_COLOR, opacityPercent));
	}

	public static int withOpacity(int color, int opacityPercent) {
		int alpha = color >>> 24;
		int scaledAlpha = Math.round(alpha * Math.min(100, Math.max(0, opacityPercent)) / 100.0F);
		return (scaledAlpha << 24) | (color & 0x00FFFFFF);
	}
}
