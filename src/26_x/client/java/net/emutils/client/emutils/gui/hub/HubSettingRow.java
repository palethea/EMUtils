package net.emutils.client.emutils.gui.hub;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.emutils.client.emutils.gui.hub.widget.HubActionButtonWidget;
import net.emutils.client.emutils.gui.hub.widget.HubCycleWidget;
import net.emutils.client.emutils.gui.hub.widget.HubSliderWidget;
import net.emutils.client.emutils.gui.hub.widget.HubToggleWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public sealed interface HubSettingRow permits HubSettingRow.Toggle, HubSettingRow.Slider, HubSettingRow.Cycle, HubSettingRow.Rgb, HubSettingRow.Action, HubSettingRow.Spacer, HubSettingRow.Divider {
	int height();

	List<AbstractWidget> createWidgets(int x, int y, int width);

	record Toggle(String labelKey, BooleanSupplier getter, Consumer<Boolean> setter) implements HubSettingRow {
		@Override
		public int height() {
			return HubPanelTheme.ROW_HEIGHT;
		}

		@Override
		public List<AbstractWidget> createWidgets(int x, int y, int width) {
			return List.of(new HubToggleWidget(x, y, width, getter, setter));
		}
	}

	record Slider(
		String labelKey,
		String suffixKey,
		int min,
		int max,
		IntSupplier getter,
		IntConsumer setter
	) implements HubSettingRow {
		@Override
		public int height() {
			return HubPanelTheme.ROW_HEIGHT;
		}

		@Override
		public List<AbstractWidget> createWidgets(int x, int y, int width) {
			Component suffix = suffixKey == null || suffixKey.isEmpty()
				? Component.empty()
				: Component.translatable(suffixKey);
			return List.of(new HubSliderWidget(x, y, width, suffix, min, max, getter, setter));
		}
	}

	record Cycle<T>(
		String labelKey,
		Supplier<T> getter,
		Consumer<T> setter,
		Supplier<T> next,
		Supplier<Component> valueLabel
	) implements HubSettingRow {
		@Override
		public int height() {
			return HubPanelTheme.ROW_HEIGHT;
		}

		@Override
		public List<AbstractWidget> createWidgets(int x, int y, int width) {
			HubCycleWidget<T> widget = new HubCycleWidget<>(x, y, width, getter, setter, next, valueLabel);
			return List.of(widget);
		}
	}

	record Rgb(String labelKey, IntSupplier getter, IntConsumer setter) implements HubSettingRow {
		@Override
		public int height() {
			return HubPanelTheme.ROW_HEIGHT;
		}

		@Override
		public List<AbstractWidget> createWidgets(int x, int y, int width) {
			return List.of();
		}
	}

	record Action(Component label, Runnable action, boolean enabled) implements HubSettingRow {
		@Override
		public int height() {
			return HubPanelTheme.ROW_HEIGHT;
		}

		@Override
		public List<AbstractWidget> createWidgets(int x, int y, int width) {
			HubActionButtonWidget button = new HubActionButtonWidget(x, y, width, label, ignored -> action.run());
			button.active = enabled;
			return List.of(button);
		}
	}

	record Spacer(int gap) implements HubSettingRow {
		@Override
		public int height() {
			return gap;
		}

		@Override
		public List<AbstractWidget> createWidgets(int x, int y, int width) {
			return List.of();
		}
	}

	record Divider() implements HubSettingRow {
		@Override
		public int height() {
			return HubPanelTheme.SECTION_GAP;
		}

		@Override
		public List<AbstractWidget> createWidgets(int x, int y, int width) {
			return List.of();
		}
	}

	static List<AbstractWidget> buildAll(List<HubSettingRow> rows, int x, int startY, int width) {
		List<AbstractWidget> widgets = new ArrayList<>();
		int y = startY;
		for (HubSettingRow row : rows) {
			widgets.addAll(row.createWidgets(x, y, width));
			y += row.height();
		}

		return widgets;
	}

	static int totalHeight(List<HubSettingRow> rows) {
		int height = 0;
		for (HubSettingRow row : rows) {
			height += row.height();
		}

		return height;
	}
}
