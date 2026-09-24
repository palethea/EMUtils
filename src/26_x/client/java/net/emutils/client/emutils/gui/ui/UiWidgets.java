package net.emutils.client.emutils.gui.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Drawing for the settings UI's controls. Screens own the state and hit testing. */
public final class UiWidgets {
	public static final int SWITCH_WIDTH = 24;
	public static final int SWITCH_HEIGHT = 14;
	private static final int SWITCH_KNOB = 10;
	public static final int ICON_TEXTURE_SIZE = 32;

	private UiWidgets() {
	}

	public enum ButtonStyle {
		PRIMARY,
		SURFACE,
		GHOST
	}

	/**
	 * A switch; {@code progress} runs from 0 (off) to 1 (on) so the knob can slide, and {@code hover}
	 * from 0 to 1 brightens the track.
	 */
	public static void toggle(GuiGraphicsExtractor context, UiTheme theme, int x, int y, float progress, float hover) {
		float eased = UiAnim.easeOut(progress);
		int track = UiTheme.mix(theme.switchOff(), theme.accent(), eased);
		track = UiTheme.mix(track, theme.text(), hover * 0.08F);
		UiShapes.pill(context, x, y, SWITCH_WIDTH, SWITCH_HEIGHT, track);
		int travel = SWITCH_WIDTH - SWITCH_KNOB - 4;
		int knobX = x + 2 + Math.round(travel * eased);
		UiShapes.circle(context, knobX, y + (SWITCH_HEIGHT - SWITCH_KNOB) / 2, SWITCH_KNOB, 0xFFFFFFFF);
	}

	public static void button(
		GuiGraphicsExtractor context,
		Font font,
		UiTheme theme,
		int x,
		int y,
		int width,
		int height,
		Component label,
		ButtonStyle style,
		float hover
	) {
		int background = switch (style) {
			case PRIMARY -> UiTheme.mix(theme.accent(), theme.accentHover(), hover);
			case SURFACE -> UiTheme.mix(theme.surface(), theme.surfaceHover(), hover);
			case GHOST -> UiTheme.fade(theme.hover(), hover);
		};
		int text = style == ButtonStyle.PRIMARY ? 0xFFFFFFFF : style == ButtonStyle.GHOST ? theme.textSecondary() : theme.text();
		UiShapes.roundedRect(context, x, y, width, height, Math.min(8, height / 2), background);
		int labelWidth = UiText.width(font, label, UiText.Size.BOLD);
		UiText.drawCentered(context, font, label, UiText.Size.BOLD, x + (width - labelWidth) / 2, y + height / 2, text);
	}

	public static int buttonWidth(Font font, Component label) {
		return UiText.width(font, label, UiText.Size.BOLD) + 18;
	}

	/** A square button showing a white icon texture tinted to the theme. */
	public static void iconButton(GuiGraphicsExtractor context, UiTheme theme, int x, int y, int size, Identifier icon, float hover) {
		UiShapes.roundedRect(context, x, y, size, size, size / 2, UiTheme.mix(theme.surface(), theme.surfaceHover(), hover));
		int iconSize = Math.round(size * 0.5F);
		int offset = (size - iconSize) / 2;
		UiShapes.icon(context, icon, x + offset, y + offset, iconSize, ICON_TEXTURE_SIZE, theme.textSecondary());
	}

	/** A small rounded label such as the DEV badge; returns its width. */
	public static int badge(GuiGraphicsExtractor context, Font font, int x, int y, Component label, int background, int text) {
		int width = UiText.width(font, label, UiText.Size.SMALL) + 8;
		int height = UiText.lineHeight(font, UiText.Size.SMALL) + 5;
		UiShapes.roundedRect(context, x, y, width, height, 3, background);
		UiText.drawCentered(context, font, label, UiText.Size.SMALL, x + 4, y + height / 2 + 1, text);
		return width;
	}
}
