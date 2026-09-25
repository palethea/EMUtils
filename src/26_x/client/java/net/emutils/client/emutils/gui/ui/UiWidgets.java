package net.emutils.client.emutils.gui.ui;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Drawing for the settings UI's controls. Screens own the state and hit testing. */
public final class UiWidgets {
	public static final int SWITCH_WIDTH = 24;
	public static final int SWITCH_HEIGHT = 14;
	private static final int SWITCH_KNOB = 10;

	private UiWidgets() {
	}

	public enum ButtonStyle {
		PRIMARY,
		SURFACE,
		GHOST,
		/** For actions that delete something. */
		DANGER
	}

	/**
	 * A switch; {@code progress} runs from 0 (off) to 1 (on) so the knob can slide, and {@code hover}
	 * from 0 to 1 brightens the track.
	 */
	public static void toggle(GuiGraphicsExtractor context, UiTheme theme, int x, int y, float progress, float hover) {
		float eased = Math.clamp(progress, 0.0F, 1.0F);
		int track = UiTheme.mix(theme.switchOff(), theme.accent(), eased);
		track = UiTheme.mix(track, theme.text(), hover * 0.08F);
		UiShapes.pill(context, x, y, SWITCH_WIDTH, SWITCH_HEIGHT, track);
		// The knob moves by fractions of a pixel, so it glides instead of stepping one GUI pixel at a time.
		float knobX = x + 2 + (SWITCH_WIDTH - SWITCH_KNOB - 4) * eased;
		int wholeX = (int) Math.floor(knobX);
		context.pose().pushMatrix();
		context.pose().translate(knobX - wholeX, 0.0F);
		UiShapes.circle(context, wholeX, y + (SWITCH_HEIGHT - SWITCH_KNOB) / 2, SWITCH_KNOB, 0xFFFFFFFF);
		context.pose().popMatrix();
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
			case DANGER -> UiTheme.fade(theme.warning(), 0.16F + 0.12F * hover);
		};
		int text = switch (style) {
			case PRIMARY -> 0xFFFFFFFF;
			case GHOST -> theme.textSecondary();
			case SURFACE -> theme.text();
			case DANGER -> theme.warning();
		};
		UiShapes.roundedRect(context, x, y, width, height, Math.min(8, height / 2), background);
		int labelWidth = UiText.width(font, label, UiText.Size.LABEL);
		UiText.drawCentered(context, font, label, UiText.Size.LABEL, x + (width - labelWidth) / 2, y + height / 2, text);
	}

	public static int buttonWidth(Font font, Component label) {
		return UiText.width(font, label, UiText.Size.LABEL) + 18;
	}

	/** A square button showing a white icon texture tinted to the theme. */
	public static void iconButton(GuiGraphicsExtractor context, UiTheme theme, int x, int y, int size, Identifier icon, float hover) {
		iconButton(context, theme, x, y, size, icon, icon, 0.0F, hover);
	}

	/** An icon button whose icon crossfades from {@code from} to {@code to} as {@code blend} goes from 0 to 1. */
	public static void iconButton(GuiGraphicsExtractor context, UiTheme theme, int x, int y, int size, Identifier from, Identifier to, float blend, float hover) {
		UiShapes.roundedRect(context, x, y, size, size, size / 2, UiTheme.mix(theme.surface(), theme.surfaceHover(), hover));
		int iconSize = Math.round(size * 0.5F);
		int offset = (size - iconSize) / 2;
		if (blend < 1.0F) {
			UiIcons.draw(context, from, x + offset, y + offset, iconSize, UiTheme.fade(theme.textSecondary(), 1.0F - blend));
		}
		if (blend > 0.0F) {
			UiIcons.draw(context, to, x + offset, y + offset, iconSize, UiTheme.fade(theme.textSecondary(), blend));
		}
	}

	/** A borderless icon button that only shows its background while hovered, for row actions. */
	public static void ghostIconButton(GuiGraphicsExtractor context, UiTheme theme, int x, int y, int size, Identifier icon, int iconColor, float hover) {
		UiShapes.roundedRect(context, x, y, size, size, 6, UiTheme.fade(theme.hover(), hover * 1.6F));
		int iconSize = Math.round(size * 0.6F);
		int offset = (size - iconSize) / 2;
		UiIcons.draw(context, icon, x + offset, y + offset, iconSize, iconColor);
	}

	/** A small label that explains the control under the mouse, kept inside the screen. */
	public static void tooltip(GuiGraphicsExtractor context, Font font, UiTheme theme, Component text, int mouseX, int mouseY, int screenWidth, int screenHeight) {
		int width = UiText.width(font, text, UiText.Size.LABEL) + 12;
		int height = UiText.lineHeight(font, UiText.Size.LABEL) + 10;
		int x = Math.clamp(mouseX - width / 2, 4, Math.max(4, screenWidth - width - 4));
		int y = mouseY - height - 8 < 4 ? mouseY + 14 : mouseY - height - 8;
		UiShapes.shadow(context, x, y, width, height, 6, 6, theme.shadow());
		UiShapes.borderedRect(context, x, y, width, height, 6, theme.surface(), theme.line());
		UiText.drawCentered(context, font, text, UiText.Size.LABEL, x + 6, y + height / 2, theme.text());
	}

	public static final int SLIDER_HEIGHT = 12;
	private static final int SLIDER_TRACK = 4;
	private static final int SLIDER_KNOB = 10;

	/** A slider; {@code fraction} runs from 0 to 1 and may be fractional so dragging glides. */
	public static void slider(GuiGraphicsExtractor context, UiTheme theme, int x, int y, int width, float fraction, float hover) {
		float clamped = Math.clamp(fraction, 0.0F, 1.0F);
		int trackY = y + (SLIDER_HEIGHT - SLIDER_TRACK) / 2;
		UiShapes.pill(context, x, trackY, width, SLIDER_TRACK, theme.switchOff());
		int filled = Math.round((width - SLIDER_KNOB) * clamped) + SLIDER_KNOB / 2;
		UiShapes.pill(context, x, trackY, filled, SLIDER_TRACK, theme.accent());
		float knobX = x + (width - SLIDER_KNOB) * clamped;
		int wholeX = (int) Math.floor(knobX);
		context.pose().pushMatrix();
		context.pose().translate(knobX - wholeX, 0.0F);
		int knobY = y + (SLIDER_HEIGHT - SLIDER_KNOB) / 2;
		UiShapes.shadow(context, wholeX, knobY, SLIDER_KNOB, SLIDER_KNOB, SLIDER_KNOB / 2, 3, UiTheme.fade(theme.shadow(), 0.8F));
		UiShapes.circle(context, wholeX, knobY, SLIDER_KNOB, UiTheme.mix(0xFFFFFFFF, 0xFFF0F2F0, 1.0F - hover));
		context.pose().popMatrix();
	}

	public static final int SEGMENT_HEIGHT = 16;

	/** Left edge of each segment of a segmented control at {@code x}, plus its right edge. */
	public static int[] segmentEdges(Font font, int x, List<Component> labels) {
		int[] edges = new int[labels.size() + 1];
		int cursor = x + 2;
		for (int i = 0; i < labels.size(); i++) {
			edges[i] = cursor;
			cursor += UiText.width(font, labels.get(i), UiText.Size.LABEL) + 16;
		}
		edges[labels.size()] = cursor;
		return edges;
	}

	/** Width of a segmented control with these labels. */
	public static int segmentedWidth(Font font, List<Component> labels) {
		int width = 4;
		for (Component label : labels) {
			width += UiText.width(font, label, UiText.Size.LABEL) + 16;
		}
		return width;
	}

	/**
	 * A segmented control. {@code selection} is the selected index and may be fractional while the
	 * highlight slides between segments. Returns the x of each segment's left edge plus the right edge.
	 */
	public static int[] segmented(GuiGraphicsExtractor context, Font font, UiTheme theme, int x, int y, List<Component> labels, float selection, int hovered) {
		int[] edges = segmentEdges(font, x, labels);
		UiShapes.roundedRect(context, x, y, edges[labels.size()] + 2 - x, SEGMENT_HEIGHT, 8, theme.segmentBackground());

		int from = (int) Math.floor(Math.clamp(selection, 0.0F, labels.size() - 1));
		int to = Math.min(labels.size() - 1, from + 1);
		float blend = selection - from;
		float left = edges[from] + (edges[to] - edges[from]) * blend;
		float right = edges[from + 1] + (edges[to + 1] - edges[from + 1]) * blend;
		int wholeLeft = (int) Math.floor(left);
		context.pose().pushMatrix();
		context.pose().translate(left - wholeLeft, 0.0F);
		UiShapes.roundedRect(context, wholeLeft, y + 2, Math.round(right - left), SEGMENT_HEIGHT - 4, 6, theme.segmentSelected());
		context.pose().popMatrix();

		for (int i = 0; i < labels.size(); i++) {
			boolean selected = Math.round(selection) == i;
			int color = selected ? theme.text() : hovered == i ? theme.textSecondary() : theme.muted();
			int labelWidth = UiText.width(font, labels.get(i), UiText.Size.LABEL);
			UiText.drawCentered(context, font, labels.get(i), UiText.Size.LABEL, edges[i] + (edges[i + 1] - edges[i] - labelWidth) / 2, y + SEGMENT_HEIGHT / 2, color);
		}
		return edges;
	}

	public static final int KEYCAP_HEIGHT = 13;

	public static int keycapWidth(Font font, Component label) {
		return Math.max(KEYCAP_HEIGHT, UiText.width(font, label, UiText.Size.SMALL) + 10);
	}

	/**
	 * A keycap showing a key, like the Ctrl F hint. {@code listening} highlights it while it waits for a
	 * key, and {@code clash} shows the key in the warning color when something else uses it too.
	 */
	public static void keycap(GuiGraphicsExtractor context, Font font, UiTheme theme, int x, int y, Component label, boolean listening, boolean clash, float hover) {
		int width = keycapWidth(font, label);
		int background = listening ? UiTheme.fade(theme.accent(), 0.25F) : UiTheme.mix(theme.segmentBackground(), theme.segmentSelected(), hover);
		if (listening) {
			UiShapes.borderedRect(context, x, y, width, KEYCAP_HEIGHT, 4, background, theme.accent());
		} else {
			UiShapes.roundedRect(context, x, y, width, KEYCAP_HEIGHT, 4, background);
		}
		int color = listening ? theme.text() : clash ? theme.warning() : theme.textSecondary();
		UiText.drawCentered(context, font, label, UiText.Size.SMALL, x + (width - UiText.width(font, label, UiText.Size.SMALL)) / 2, y + KEYCAP_HEIGHT / 2, color);
	}

	/** A small rounded label such as the DEV badge; returns its width. */
	public static int badge(GuiGraphicsExtractor context, Font font, int x, int y, Component label, int background, int text) {
		int width = UiText.width(font, label, UiText.Size.SMALL) + 8;
		int height = UiText.lineHeight(font, UiText.Size.SMALL) + 5;
		UiShapes.roundedRect(context, x, y, width, height, 3, background);
		UiText.drawCentered(context, font, label, UiText.Size.SMALL, x + 4, y + height / 2, text);
		return width;
	}
}
