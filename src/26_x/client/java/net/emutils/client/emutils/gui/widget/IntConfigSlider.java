package net.emutils.client.emutils.gui.widget;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.emutils.client.emutils.util.EMHelpersTexts;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public final class IntConfigSlider extends AbstractSliderButton {
	private final Component label;
	private final Component suffix;
	private final int min;
	private final int max;
	private final IntConsumer setter;

	public IntConfigSlider(
		int x,
		int y,
		int width,
		int height,
		String labelKey,
		String suffixKey,
		int min,
		int max,
		IntSupplier getter,
		IntConsumer setter
	) {
		this(x, y, width, height, Component.translatable(labelKey), Component.translatable(suffixKey), min, max, getter, setter);
	}

	public IntConfigSlider(
		int x,
		int y,
		int width,
		int height,
		Component label,
		Component suffix,
		int min,
		int max,
		IntSupplier getter,
		IntConsumer setter
	) {
		super(x, y, width, height, Component.empty(), valueFrom(getter.getAsInt(), min, max));
		this.label = label;
		this.suffix = suffix;
		this.min = min;
		this.max = max;
		this.setter = setter;
		updateMessage();
	}

	@Override
	protected void updateMessage() {
		Component value = Component.literal(String.valueOf(currentValue())).append(suffix);
		setMessage(Component.translatable(EMHelpersTexts.optionValue(), label, value));
	}

	@Override
	protected void applyValue() {
		setter.accept(currentValue());
	}

	private int currentValue() {
		return min + (int) Math.round(value * (max - min));
	}

	private static double valueFrom(int current, int min, int max) {
		if (max == min) {
			return 0.0;
		}

		return (current - min) / (double) (max - min);
	}
}
