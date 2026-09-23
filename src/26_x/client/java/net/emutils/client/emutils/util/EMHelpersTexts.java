package net.emutils.client.emutils.util;

import java.util.Objects;
import net.minecraft.network.chat.Component;

public final class EMHelpersTexts {
	private static String prefix = "emhelpers";

	private EMHelpersTexts() {
	}

	public static void setPrefix(String newPrefix) {
		prefix = Objects.requireNonNull(newPrefix, "prefix");
	}

	public static String prefix() {
		return prefix;
	}

	public static String optionOn() {
		return prefix + ".option.on";
	}

	public static String optionOff() {
		return prefix + ".option.off";
	}

	public static String optionToggle() {
		return prefix + ".option.toggle";
	}

	public static String optionValue() {
		return prefix + ".option.value";
	}

	public static String screenHudLayoutEditor() {
		return prefix + ".screen.hud_layout_editor";
	}

	public static String hudLayoutEditorHint() {
		return prefix + ".hud.layout_editor.hint";
	}

	public static String hudLayoutEditorSave() {
		return prefix + ".hud.layout_editor.save";
	}

	public static String hudLayoutEditorCancel() {
		return prefix + ".hud.layout_editor.cancel";
	}

	public static String hudLayoutEditorResetAll() {
		return prefix + ".hud.layout_editor.reset_all";
	}

	public static String hudLayoutModeAnchor() {
		return prefix + ".hud.layout_mode.anchor";
	}

	public static String hudLayoutModeCustom() {
		return prefix + ".hud.layout_mode.custom";
	}

	public static String hudAnchorTopLeft() {
		return prefix + ".hud.anchor.top_left";
	}

	public static String hudAnchorTopCenter() {
		return prefix + ".hud.anchor.top_center";
	}

	public static String hudAnchorTopRight() {
		return prefix + ".hud.anchor.top_right";
	}

	public static String hudAnchorBottomLeft() {
		return prefix + ".hud.anchor.bottom_left";
	}

	public static String hudAnchorBottomCenter() {
		return prefix + ".hud.anchor.bottom_center";
	}

	public static String hudAnchorBottomRight() {
		return prefix + ".hud.anchor.bottom_right";
	}

	public static Component toggleLabel(String optionKey) {
		return Component.translatable(optionKey);
	}
}
