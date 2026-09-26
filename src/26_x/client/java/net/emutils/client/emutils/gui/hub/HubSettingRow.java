package net.emutils.client.emutils.gui.hub;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public sealed interface HubSettingRow permits HubSettingRow.Toggle, HubSettingRow.Slider, HubSettingRow.Cycle, HubSettingRow.Rgb, HubSettingRow.Action, HubSettingRow.Spacer, HubSettingRow.Divider {
	record Toggle(String labelKey, BooleanSupplier getter, Consumer<Boolean> setter) implements HubSettingRow {
	}

	record Slider(
		String labelKey,
		String suffixKey,
		int min,
		int max,
		IntSupplier getter,
		IntConsumer setter
	) implements HubSettingRow {
	}

	/**
	 * A choice between values. {@code options} and {@code optionLabel} list every value so the settings
	 * UI can show them all at once; without them it can only step to the next value.
	 */
	record Cycle<T>(
		String labelKey,
		Supplier<T> getter,
		Consumer<T> setter,
		Supplier<T> next,
		Supplier<Component> valueLabel,
		@Nullable List<T> options,
		@Nullable Function<T, Component> optionLabel
	) implements HubSettingRow {
		public Cycle(String labelKey, Supplier<T> getter, Consumer<T> setter, Supplier<T> next, Supplier<Component> valueLabel) {
			this(labelKey, getter, setter, next, valueLabel, null, null);
		}

		/** A choice between all values of an enum, labeled by {@code label}. */
		public static <E extends Enum<E>> Cycle<E> ofEnum(String labelKey, Supplier<E> getter, Consumer<E> setter, Class<E> type, Function<E, Component> label) {
			List<E> values = List.of(type.getEnumConstants());
			return new Cycle<>(
				labelKey,
				getter,
				setter,
				() -> values.get((values.indexOf(getter.get()) + 1) % values.size()),
				() -> label.apply(getter.get()),
				values,
				label
			);
		}

		/** A choice between the indexes of {@code names}. */
		public static Cycle<Integer> ofNames(String labelKey, Supplier<Integer> getter, Consumer<Integer> setter, String[] names) {
			List<Integer> values = IntStream.range(0, names.length).boxed().toList();
			return new Cycle<>(
				labelKey,
				getter,
				setter,
				() -> (getter.get() + 1) % names.length,
				() -> Component.literal(names[Math.clamp(getter.get(), 0, names.length - 1)]),
				values,
				index -> Component.literal(names[index])
			);
		}
	}

	record Rgb(String labelKey, IntSupplier getter, IntConsumer setter) implements HubSettingRow {
	}

	record Action(Component label, Runnable action, boolean enabled) implements HubSettingRow {
	}

	record Spacer(int gap) implements HubSettingRow {
	}

	record Divider() implements HubSettingRow {
	}
}
