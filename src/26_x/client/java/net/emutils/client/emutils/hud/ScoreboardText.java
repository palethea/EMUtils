package net.emutils.client.emutils.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.emutils.client.emutils.gui.ui.UiText;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;

/**
 * Server text for the custom scoreboard (#169), measured and drawn in the font picked for it.
 * <p>
 * Minecraft's font draws a component exactly as vanilla does, every color and style included. The EMUtils
 * UI font draws it run by run in that run's color, in a heavier weight where it is bold, with underline and
 * strikethrough as lines. Italics and obfuscation can't be drawn in it, and a run set in another font, such
 * as a server's icon glyphs, is drawn in Minecraft's font because the UI font doesn't have those glyphs.
 */
final class ScoreboardText {
	private static final String ELLIPSIS = "...";
	/** Height of Minecraft's text. */
	private static final int MINECRAFT_LINE_HEIGHT = 9;
	private static final int NO_LIMIT = Integer.MAX_VALUE;

	private final Font font;
	private final boolean uiFont;

	ScoreboardText(Font font, ScoreboardFont mode) {
		this.font = font;
		this.uiFont = mode == ScoreboardFont.EMUTILS;
	}

	/** How tall one line is: vanilla's own height for the vanilla style in Minecraft's font. */
	int rowHeight(boolean vanillaStyle) {
		if (uiFont) {
			return 11;
		}
		return vanillaStyle ? MINECRAFT_LINE_HEIGHT : MINECRAFT_LINE_HEIGHT + 1;
	}

	/** Vanilla puts its title one pixel below the top of the title bar; the UI font is centered in it instead. */
	int titleNudge() {
		return uiFont ? 0 : 1;
	}

	int width(String text) {
		return prepare(Component.literal(text), false, NO_LIMIT).width();
	}

	/** The text ready to draw, cut short with "..." if it is wider than {@code maxWidth}. */
	Prepared prepare(Component text, boolean bold, int maxWidth) {
		return uiFont ? prepareUi(text, bold, maxWidth) : prepareMinecraft(text, bold, maxWidth);
	}

	private Prepared prepareMinecraft(Component text, boolean bold, int maxWidth) {
		Component styled = bold ? text.copy().withStyle(style -> style.withBold(true)) : text;
		int width = font.width(styled);
		if (width <= maxWidth) {
			return new Prepared(styled.getVisualOrderText(), width, List.of());
		}
		FormattedText cut = font.substrByWidth(styled, Math.max(0, maxWidth - font.width(ELLIPSIS)));
		FormattedCharSequence sequence = Language.getInstance().getVisualOrder(FormattedText.composite(cut, FormattedText.of(ELLIPSIS)));
		return new Prepared(sequence, font.width(sequence), List.of());
	}

	private Prepared prepareUi(Component text, boolean bold, int maxWidth) {
		List<Run> collected = new ArrayList<>();
		text.visit((style, string) -> {
			if (!string.isEmpty()) {
				collected.add(new Run(string, style, bold || style.isBold()));
			}
			return Optional.empty();
		}, Style.EMPTY);
		List<Run> runs = collected;
		int width = 0;
		for (Run run : runs) {
			width += runWidth(run);
		}
		if (width > maxWidth) {
			runs = truncate(runs, maxWidth);
			width = 0;
			for (Run run : runs) {
				width += runWidth(run);
			}
		}
		return new Prepared(null, width, runs);
	}

	/** Keeps whole runs while they fit, cuts the one that doesn't, and ends with "...". */
	private List<Run> truncate(List<Run> runs, int maxWidth) {
		int budget = maxWidth - runWidth(new Run(ELLIPSIS, Style.EMPTY, false));
		List<Run> kept = new ArrayList<>();
		Run last = new Run("", Style.EMPTY, false);
		int used = 0;
		for (Run run : runs) {
			int width = runWidth(run);
			if (used + width <= budget) {
				kept.add(run);
				used += width;
				last = run;
				continue;
			}
			String cut = run.text();
			while (!cut.isEmpty() && used + runWidth(run.withText(cut)) > budget) {
				cut = cut.substring(0, cut.offsetByCodePoints(cut.length(), -1));
			}
			if (!cut.isEmpty()) {
				kept.add(run.withText(cut));
				last = run;
			}
			break;
		}
		kept.add(last.withText(ELLIPSIS));
		return kept;
	}

	private UiText.Size size(Run run) {
		return run.bold() ? UiText.Size.BOLD : UiText.Size.BODY;
	}

	private boolean minecraftFont(Run run) {
		return !run.style().getFont().equals(FontDescription.DEFAULT);
	}

	private int runWidth(Run run) {
		if (minecraftFont(run)) {
			return font.width(Component.literal(run.text()).withStyle(run.style()));
		}
		return UiText.width(font, Component.literal(run.text()), size(run));
	}

	/** A piece of text with one style. */
	private record Run(String text, Style style, boolean bold) {
		Run withText(String other) {
			return new Run(other, style, bold);
		}
	}

	/** Text measured and ready to draw. */
	final class Prepared {
		private final FormattedCharSequence sequence;
		private final int width;
		private final List<Run> runs;

		private Prepared(FormattedCharSequence sequence, int width, List<Run> runs) {
			this.sequence = sequence;
			this.width = width;
			this.runs = runs;
		}

		int width() {
			return width;
		}

		/**
		 * Draws it at {@code x}, centered on the row that starts at {@code rowTop}. Parts without a color of
		 * their own get {@code color}.
		 */
		void draw(GuiGraphicsExtractor context, int x, int rowTop, int rowHeight, int color, boolean shadow) {
			if (!uiFont) {
				int y = rowTop + (rowHeight - MINECRAFT_LINE_HEIGHT + 1) / 2;
				context.text(font, sequence, x, y, color, shadow);
				return;
			}
			int capHeight = UiText.lineHeight(font, UiText.Size.BODY);
			int baseline = rowTop + (rowHeight + capHeight) / 2;
			int penX = x;
			for (Run run : runs) {
				int runWidth = runWidth(run);
				drawRun(context, run, penX, baseline, capHeight, color, shadow);
				penX += runWidth;
			}
		}

		private void drawRun(GuiGraphicsExtractor context, Run run, int x, int baseline, int capHeight, int defaultColor, boolean shadow) {
			TextColor textColor = run.style().getColor();
			int color = textColor == null ? defaultColor : 0xFF000000 | textColor.getValue();
			int width = runWidth(run);
			if (minecraftFont(run)) {
				// Drawn as vanilla would, so glyphs the UI font lacks still show. Its baseline is 7 below the top.
				context.text(font, Component.literal(run.text()).withStyle(run.style()), x, baseline - 7, color, shadow);
			} else {
				UiText.Size size = size(run);
				int top = baseline - UiText.lineHeight(font, size);
				Component text = Component.literal(run.text());
				if (shadow) {
					float offset = HudOverlayRenderer.shadowOffset();
					UiText.drawExact(context, font, text, size, x + offset, top + offset, HudOverlayRenderer.shadowColor());
				}
				UiText.drawExact(context, font, text, size, x, top, color);
			}
			if (run.style().isUnderlined()) {
				context.fill(x, baseline + 1, x + width, baseline + 2, color);
			}
			if (run.style().isStrikethrough()) {
				int y = baseline - capHeight / 2;
				context.fill(x, y, x + width, y + 1, color);
			}
		}
	}
}
