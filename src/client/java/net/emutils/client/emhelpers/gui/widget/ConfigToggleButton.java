package net.emutils.client.emhelpers.gui.widget;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.emutils.client.emutils.gui.EMUtilsScreen;
import net.emutils.client.emhelpers.util.EMUtilsTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class ConfigToggleButton {
	private ConfigToggleButton() {
	}

	public static ButtonWidget create(Text label, BooleanSupplier getter, Consumer<Boolean> setter) {
		return ButtonWidget.builder(message(label, getter), button -> {
			setter.accept(!getter.getAsBoolean());
			button.setMessage(message(label, getter));
		}).width(EMUtilsScreen.SETTINGS_BUTTON_WIDTH).build();
	}

	public static ButtonWidget create(String labelKey, BooleanSupplier getter, Consumer<Boolean> setter) {
		return create(Text.translatable(labelKey), getter, setter);
	}

	private static Text message(Text label, BooleanSupplier getter) {
		Text state = Text.translatable(getter.getAsBoolean() ? EMUtilsTexts.OPTION_ON : EMUtilsTexts.OPTION_OFF);
		return Text.translatable(EMUtilsTexts.OPTION_TOGGLE, label, state);
	}
}
