package net.emutils.client.emhelpers.gui.widget;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.emutils.client.emhelpers.util.EMUtilsTexts;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public final class IntConfigSlider extends SliderWidget {
	private final Text label;
	private final Text suffix;
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
		this(x, y, width, height, Text.translatable(labelKey), Text.translatable(suffixKey), min, max, getter, setter);
	}

	public IntConfigSlider(
		int x,
		int y,
		int width,
		int height,
		Text label,
		Text suffix,
		int min,
		int max,
		IntSupplier getter,
		IntConsumer setter
	) {
		super(x, y, width, height, Text.empty(), valueFrom(getter.getAsInt(), min, max));
		this.label = label;
		this.suffix = suffix;
		this.min = min;
		this.max = max;
		this.setter = setter;
		updateMessage();
	}

	@Override
	protected void updateMessage() {
		Text value = Text.literal(String.valueOf(currentValue())).append(suffix);
		setMessage(Text.translatable(EMUtilsTexts.OPTION_VALUE, label, value));
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
