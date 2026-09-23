package net.emutils.client.emutils.gui.widget;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.emutils.client.emutils.util.EMHelpersTexts;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class ConfigToggleButton {
	public static final int DEFAULT_WIDTH = 200;

	private ConfigToggleButton() {
	}

	public static Button create(Component label, BooleanSupplier getter, Consumer<Boolean> setter) {
		return Button.builder(message(label, getter), button -> {
			setter.accept(!getter.getAsBoolean());
			button.setMessage(message(label, getter));
		}).width(DEFAULT_WIDTH).build();
	}

	public static Button create(String labelKey, BooleanSupplier getter, Consumer<Boolean> setter) {
		return create(Component.translatable(labelKey), getter, setter);
	}

	private static Component message(Component label, BooleanSupplier getter) {
		Component state = Component.translatable(getter.getAsBoolean() ? EMHelpersTexts.optionOn() : EMHelpersTexts.optionOff());
		return Component.translatable(EMHelpersTexts.optionToggle(), label, state);
	}
}
