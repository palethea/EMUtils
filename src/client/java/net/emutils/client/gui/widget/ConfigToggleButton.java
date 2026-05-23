package net.emutils.client.gui.widget;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.emutils.client.util.EMUtilsTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class ConfigToggleButton {
	private ConfigToggleButton() {
	}

	public static ButtonWidget create(Text label, BooleanSupplier getter, Consumer<Boolean> setter) {
		return ButtonWidget.builder(message(label, getter), button -> {
			setter.accept(!getter.getAsBoolean());
			button.setMessage(message(label, getter));
		}).width(200).build();
	}

	public static ButtonWidget create(String labelKey, BooleanSupplier getter, Consumer<Boolean> setter) {
		return create(Text.translatable(labelKey), getter, setter);
	}

	private static Text message(Text label, BooleanSupplier getter) {
		Text state = Text.translatable(getter.getAsBoolean() ? EMUtilsTexts.OPTION_ON : EMUtilsTexts.OPTION_OFF);
		return Text.translatable(EMUtilsTexts.OPTION_TOGGLE, label, state);
	}
}
