package net.emutils.client.emutils.gui.settings;

import java.util.ArrayList;
import java.util.List;
import net.emutils.client.emutils.gui.hub.HubFeature;
import net.emutils.client.emutils.gui.hub.HubIcons;
import net.emutils.client.emutils.gui.hub.HubSettingRow;
import net.emutils.client.emutils.gui.hub.HubSettingsRegistry;
import net.emutils.client.emutils.gui.ui.UiAnim;
import net.emutils.client.emutils.gui.ui.UiColorPicker;
import net.emutils.client.emutils.gui.ui.UiIcons;
import net.emutils.client.emutils.gui.ui.UiOpacity;
import net.emutils.client.emutils.gui.ui.UiScrollArea;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.gui.ui.UiWidgets;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * A feature's settings in a centered sheet over the settings screen (#92). It lists the same rows as
 * the classic hub, so every setting works here, and shows an optional description under each one from
 * the language key {@code <label key>.desc}.
 */
final class SettingsSheet {
	private static final int MAX_WIDTH = 380;
	private static final int PADDING = 16;
	private static final int RADIUS = 16;
	private static final int ROW_GAP = 6;
	private static final int ROW_PADDING = 10;
	private static final int ROW_RADIUS = 10;
	private static final int LINE_HEIGHT = 11;
	private static final int BUTTON_HEIGHT = 20;
	private static final int SWATCH = 16;
	private static final int DROPDOWN_ROW = 16;
	private static final int FADE_HEIGHT = 10;
	/** Choices use a segmented control when they have at most this many options and fit on one line. */
	private static final int MAX_SEGMENTS = 4;
	private static final float OPEN_SECONDS = 0.2F;

	private final Font font;
	private final UiAnim anim;
	private final HubFeature feature;
	private final UiScrollArea scroll = new UiScrollArea();
	private final List<RowBox> boxes = new ArrayList<>();
	private List<HubSettingRow> rows;
	private boolean rowsDirty;
	private boolean closing;
	private float progress;
	private boolean prepared;
	private @Nullable Dropdown dropdown;
	private HubSettingRow.@Nullable Slider draggingSlider;
	private int draggingX;
	private int draggingWidth;
	private float draggingFraction;
	private @Nullable UiColorPicker colorPicker;
	private int x;
	private int y;
	private int width;
	private int height;
	private int doneX;
	private int resetX;
	private int resetWidth;
	private int doneWidth;
	private int footerY;
	private int switchX;
	private int switchY;
	/** Width available inside a row, for deciding whether a segmented control fits. */
	private int rowContentWidth = Integer.MAX_VALUE;

	SettingsSheet(Font font, UiAnim anim, HubFeature feature) {
		this.font = font;
		this.anim = anim;
		this.feature = feature;
		this.rows = loadRows();
		// Starts the open animation from nothing.
		anim.transition(animKey(), 0.0F, OPEN_SECONDS, true);
	}

	private String animKey() {
		return "sheet:" + feature.id();
	}

	private List<HubSettingRow> loadRows() {
		if (feature.rows() != null) {
			return feature.rows();
		}
		if (feature.category() == null) {
			return List.of();
		}
		return HubSettingsRegistry.rows(feature.category(), () -> rowsDirty = true);
	}

	boolean isClosed() {
		return closing && progress <= 0.0F;
	}

	void close() {
		closing = true;
		dropdown = null;
		draggingSlider = null;
		closeColorPicker();
	}

	void render(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY, int panelX, int panelY, int panelWidth, int panelHeight, int screenWidth, int screenHeight) {
		if (rowsDirty) {
			rowsDirty = false;
			rows = loadRows();
		}
		if (!prepared) {
			// Rendering every label up front means the first frames of the animation have nothing new
			// to create, so it runs without a hitch; the animation's clock starts after this.
			prepared = true;
			layout(panelX, panelY, panelWidth, panelHeight);
			prepareText();
		}
		progress = anim.transition(animKey(), closing ? 0.0F : 1.0F, OPEN_SECONDS, true);
		context.fill(0, 0, screenWidth, screenHeight, UiTheme.fade(theme.overlay(), progress));
		if (progress <= 0.0F) {
			return;
		}

		layout(panelX, panelY, panelWidth, panelHeight);
		// The whole sheet fades with the animation instead of popping away on its last frame.
		UiOpacity.set(progress);
		float scale = 0.96F + 0.04F * progress;
		context.pose().pushMatrix();
		context.pose().translate(x + width / 2.0F, y + height / 2.0F);
		context.pose().scale(scale, scale);
		context.pose().translate(-(x + width / 2.0F), -(y + height / 2.0F));

		UiShapes.shadow(context, x, y, width, height, RADIUS, 22, UiTheme.fade(theme.shadow(), progress * 1.6F));
		UiShapes.borderedRect(context, x, y, width, height, RADIUS, theme.surface(), theme.border());
		drawHeader(context, theme, mouseX, mouseY);
		drawRows(context, theme, mouseX, mouseY);
		drawFooter(context, theme, mouseX, mouseY);
		context.pose().popMatrix();

		if (dropdown != null) {
			drawDropdown(context, theme, dropdown, mouseX, mouseY);
		}
		if (colorPicker != null) {
			colorPicker.render(context, font, theme);
		}
		UiOpacity.reset();
	}

	private void layout(int panelX, int panelY, int panelWidth, int panelHeight) {
		width = Math.min(MAX_WIDTH, panelWidth - 32);
		int contentWidth = width - PADDING * 2;
		measureRows(contentWidth);
		int contentHeight = 0;
		for (RowBox box : boxes) {
			contentHeight = Math.max(contentHeight, box.top + box.height);
		}
		int headerHeight = headerHeight(contentWidth);
		int footerHeight = BUTTON_HEIGHT + PADDING;
		int maxHeight = panelHeight - 24;
		height = Math.min(maxHeight, PADDING + headerHeight + 10 + Math.max(contentHeight, 18) + FADE_HEIGHT + 8 + footerHeight);
		x = panelX + (panelWidth - width) / 2;
		y = panelY + (panelHeight - height) / 2;
		footerY = y + height - PADDING - BUTTON_HEIGHT;
		int listTop = y + PADDING + headerHeight + 10 - FADE_HEIGHT / 2;
		scroll.setBounds(x + PADDING, listTop, contentWidth + UiScrollArea.GUTTER, footerY - 8 - listTop);
		scroll.setContentHeight(contentHeight + FADE_HEIGHT);
	}

	private void prepareText() {
		UiText.prepare(SettingsScreen.title(feature), UiText.Size.HEADING);
		UiText.prepare(Component.translatable(feature.group().labelKey()), UiText.Size.BODY);
		for (Component line : UiText.wrap(font, Component.translatable(feature.descriptionKey()), UiText.Size.BODY, width - PADDING * 2)) {
			UiText.prepare(line, UiText.Size.BODY);
		}
		for (RowBox box : boxes) {
			String key = labelKey(box.row);
			if (key != null) {
				UiText.prepare(Component.translatable(key), UiText.Size.BOLD);
			}
			for (Component line : box.description) {
				UiText.prepare(line, UiText.Size.BODY);
			}
		}
		UiText.prepare(CommonComponents.GUI_DONE, UiText.Size.LABEL);
		UiText.prepare(Component.translatable(EMUtilsTexts.UI_RESET), UiText.Size.LABEL);
	}

	private int headerHeight(int contentWidth) {
		int titleBlock = UiText.lineHeight(font, UiText.Size.HEADING) + 5 + UiText.lineHeight(font, UiText.Size.BODY);
		List<Component> description = UiText.wrap(font, Component.translatable(feature.descriptionKey()), UiText.Size.BODY, contentWidth);
		return titleBlock + 12 + description.size() * LINE_HEIGHT;
	}

	private void drawHeader(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		int left = x + PADDING;
		int top = y + PADDING;
		int headingHeight = UiText.lineHeight(font, UiText.Size.HEADING);
		UiIcons.draw(context, feature.icon().texture(), left, top + headingHeight / 2 - 8 + 3, 16, theme.text());
		int textX = left + 24;
		UiText.draw(context, font, SettingsScreen.title(feature), UiText.Size.HEADING, textX, top, theme.text());
		UiText.draw(context, font, Component.translatable(feature.group().labelKey()), UiText.Size.BODY, textX, top + headingHeight + 5, theme.muted());

		if (feature.toggle() != null) {
			switchX = x + width - PADDING - UiWidgets.SWITCH_WIDTH;
			switchY = top + headingHeight / 2 + 3 - UiWidgets.SWITCH_HEIGHT / 2;
			float on = anim.transition("switch:" + feature.id(), feature.toggle().getter().getAsBoolean(), 0.18F);
			UiWidgets.toggle(context, theme, switchX, switchY, on, contains(mouseX, mouseY, switchX, switchY, UiWidgets.SWITCH_WIDTH, UiWidgets.SWITCH_HEIGHT) ? 1.0F : 0.0F);
		}

		int descriptionTop = top + headingHeight + 5 + UiText.lineHeight(font, UiText.Size.BODY) + 12;
		List<Component> description = UiText.wrap(font, Component.translatable(feature.descriptionKey()), UiText.Size.BODY, width - PADDING * 2);
		for (int i = 0; i < description.size(); i++) {
			UiText.draw(context, font, description.get(i), UiText.Size.BODY, left, descriptionTop + i * LINE_HEIGHT, theme.textSecondary());
		}
	}

	// ---- rows -----------------------------------------------------------------------------------

	private void measureRows(int contentWidth) {
		boxes.clear();
		rowContentWidth = contentWidth - ROW_PADDING * 2;
		int top = FADE_HEIGHT / 2;
		for (HubSettingRow row : rows) {
			if (row instanceof HubSettingRow.Divider) {
				top += 8;
				continue;
			}
			if (row instanceof HubSettingRow.Spacer spacer) {
				top += spacer.gap();
				continue;
			}
			if (repeatsFeatureSwitch(row)) {
				continue;
			}
			RowBox box = new RowBox(row, top);
			int textWidth = contentWidth - ROW_PADDING * 2 - inlineControlWidth(row);
			box.description = description(row) == null ? List.of() : UiText.wrap(font, description(row), UiText.Size.BODY, textWidth);
			int content;
			if (row instanceof HubSettingRow.Action) {
				content = BUTTON_HEIGHT;
			} else {
				content = 12 + (box.description.isEmpty() ? 0 : 3 + box.description.size() * LINE_HEIGHT - 2);
				if (row instanceof HubSettingRow.Slider) {
					content += 7 + UiWidgets.SLIDER_HEIGHT;
				} else if (row instanceof HubSettingRow.Cycle<?> cycle && usesSegments(cycle)) {
					content += 7 + UiWidgets.SEGMENT_HEIGHT;
				}
			}
			box.height = content + ROW_PADDING * 2;
			boxes.add(box);
			top += box.height + ROW_GAP;
		}
	}

	/** The classic hub lists a feature's own on/off switch as its first row; the sheet header has it already. */
	private boolean repeatsFeatureSwitch(HubSettingRow row) {
		return feature.toggle() != null
			&& row instanceof HubSettingRow.Toggle toggle
			&& Component.translatable(toggle.labelKey()).getString().equalsIgnoreCase(SettingsScreen.title(feature).getString());
	}

	/** Width reserved on the right of the label for controls that sit beside it. */
	private int inlineControlWidth(HubSettingRow row) {
		if (row instanceof HubSettingRow.Toggle) {
			return UiWidgets.SWITCH_WIDTH + 10;
		}
		if (row instanceof HubSettingRow.Rgb) {
			return SWATCH + 10;
		}
		if (row instanceof HubSettingRow.Slider) {
			return 40;
		}
		if (row instanceof HubSettingRow.Cycle<?> cycle && !usesSegments(cycle)) {
			return 110;
		}
		return 0;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private boolean usesSegments(HubSettingRow.Cycle cycle) {
		if (cycle.options() == null || cycle.options().size() > MAX_SEGMENTS) {
			return false;
		}
		List<Component> labels = new ArrayList<>();
		for (Object option : cycle.options()) {
			labels.add((Component) cycle.optionLabel().apply(option));
		}
		return UiWidgets.segmentedWidth(font, labels) <= rowContentWidth;
	}

	private static @Nullable Component description(HubSettingRow row) {
		String key = labelKey(row);
		if (key == null) {
			return null;
		}
		String descriptionKey = key + ".desc";
		return Language.getInstance().has(descriptionKey) ? Component.translatable(descriptionKey) : null;
	}

	private static @Nullable String labelKey(HubSettingRow row) {
		if (row instanceof HubSettingRow.Toggle toggle) {
			return toggle.labelKey();
		}
		if (row instanceof HubSettingRow.Slider slider) {
			return slider.labelKey();
		}
		if (row instanceof HubSettingRow.Cycle<?> cycle) {
			return cycle.labelKey();
		}
		if (row instanceof HubSettingRow.Rgb rgb) {
			return rgb.labelKey();
		}
		return null;
	}

	private void drawRows(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		boolean interactive = dropdown == null && colorPicker == null;
		scroll.animate(anim, "sheet-scroll:" + feature.id(), interactive ? mouseX : Integer.MIN_VALUE / 2, mouseY);
		boolean mouseInList = interactive && scroll.contains(mouseX, mouseY);
		scroll.begin(context);
		context.pose().pushMatrix();
		context.pose().translate(0.0F, scroll.offset() - scroll.exactOffset());
		int rowWidth = scroll.contentWidth();
		for (RowBox box : boxes) {
			box.x = scroll.x();
			box.y = scroll.y() + box.top - scroll.offset();
			box.width = rowWidth;
			if (box.y + box.height < scroll.y() || box.y > scroll.y() + scroll.height()) {
				continue;
			}
			drawRow(context, theme, box, mouseInList ? mouseX : -1, mouseInList ? mouseY : -1);
		}
		if (boxes.isEmpty()) {
			UiShapes.roundedRect(context, scroll.x(), scroll.y() + FADE_HEIGHT / 2, rowWidth, 30, ROW_RADIUS, theme.surfaceAlt());
			UiText.drawCentered(context, font, Component.translatable(EMUtilsTexts.UI_NOTHING_TO_SET_UP), UiText.Size.BODY, scroll.x() + ROW_PADDING, scroll.y() + FADE_HEIGHT / 2 + 15, theme.muted());
		}
		context.pose().popMatrix();
		scroll.end(context, theme.surface(), FADE_HEIGHT, UiTheme.fade(theme.text(), 0.25F), UiTheme.fade(theme.text(), 0.45F));
	}

	private void drawRow(GuiGraphicsExtractor context, UiTheme theme, RowBox box, int mouseX, int mouseY) {
		HubSettingRow row = box.row;
		boolean hovered = contains(mouseX, mouseY, box.x, box.y, box.width, box.height);
		float hover = anim.towards("row:" + feature.id() + ":" + box.top, hovered, 16.0F);
		UiShapes.roundedRect(context, box.x, box.y, box.width, box.height, ROW_RADIUS, UiTheme.mix(theme.surfaceAlt(), theme.surfaceHover(), hover * 0.6F));
		int left = box.x + ROW_PADDING;
		int right = box.x + box.width - ROW_PADDING;
		int labelCenter = box.y + ROW_PADDING + 6;

		if (row instanceof HubSettingRow.Action action) {
			float buttonHover = contains(mouseX, mouseY, left, box.y + ROW_PADDING, right - left, BUTTON_HEIGHT) ? 1.0F : 0.0F;
			box.setControl(left, box.y + ROW_PADDING, right - left, BUTTON_HEIGHT);
			UiWidgets.button(context, font, theme, left, box.y + ROW_PADDING, right - left, BUTTON_HEIGHT, action.label(), action.enabled() ? UiWidgets.ButtonStyle.SURFACE : UiWidgets.ButtonStyle.GHOST, buttonHover);
			return;
		}

		Component label = Component.translatable(labelKey(row));
		UiText.drawCentered(context, font, UiText.ellipsize(font, label, UiText.Size.BOLD, right - left - inlineControlWidth(row)), UiText.Size.BOLD, left, labelCenter, theme.text());
		int textTop = labelCenter + 6 + 3;
		for (int i = 0; i < box.description.size(); i++) {
			UiText.draw(context, font, box.description.get(i), UiText.Size.BODY, left, textTop + i * LINE_HEIGHT, theme.muted());
		}
		int below = box.description.isEmpty() ? labelCenter + 6 + 7 : textTop + box.description.size() * LINE_HEIGHT + 3;

		if (row instanceof HubSettingRow.Toggle toggle) {
			int toggleX = right - UiWidgets.SWITCH_WIDTH;
			int toggleY = labelCenter - UiWidgets.SWITCH_HEIGHT / 2;
			box.setControl(box.x, box.y, box.width, box.height);
			float on = anim.transition("row-switch:" + toggle.labelKey(), toggle.getter().getAsBoolean(), 0.18F);
			UiWidgets.toggle(context, theme, toggleX, toggleY, on, contains(mouseX, mouseY, toggleX, toggleY, UiWidgets.SWITCH_WIDTH, UiWidgets.SWITCH_HEIGHT) ? 1.0F : 0.0F);
		} else if (row instanceof HubSettingRow.Slider slider) {
			Component value = sliderValue(slider);
			int valueWidth = UiText.width(font, value, UiText.Size.LABEL) + 10;
			UiShapes.roundedRect(context, right - valueWidth, labelCenter - 7, valueWidth, 14, 5, UiTheme.fade(theme.accent(), 0.22F));
			UiText.drawCentered(context, font, value, UiText.Size.LABEL, right - valueWidth + 5, labelCenter, theme.text());
			int range = Math.max(1, slider.max() - slider.min());
			float target = (slider.getter().getAsInt() - slider.min()) / (float) range;
			float fraction = draggingSlider == slider ? draggingFraction : anim.towards("slider:" + slider.labelKey(), target, 22.0F);
			box.setControl(left, below, right - left, UiWidgets.SLIDER_HEIGHT);
			boolean sliderHover = draggingSlider == slider || contains(mouseX, mouseY, left, below - 3, right - left, UiWidgets.SLIDER_HEIGHT + 6);
			UiWidgets.slider(context, theme, left, below, right - left, fraction, sliderHover ? 1.0F : 0.0F);
		} else if (row instanceof HubSettingRow.Cycle<?> cycle) {
			drawCycle(context, theme, box, cycle, left, right, labelCenter, below, mouseX, mouseY);
		} else if (row instanceof HubSettingRow.Rgb rgb) {
			int swatchX = right - SWATCH;
			int swatchY = labelCenter - SWATCH / 2;
			box.setControl(box.x, box.y, box.width, box.height);
			box.anchorX = swatchX;
			box.anchorY = swatchY + SWATCH / 2;
			UiShapes.roundedRect(context, swatchX - 1, swatchY - 1, SWATCH + 2, SWATCH + 2, 5, theme.line());
			UiShapes.roundedRect(context, swatchX, swatchY, SWATCH, SWATCH, 4, 0xFF000000 | rgb.getter().getAsInt());
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void drawCycle(GuiGraphicsExtractor context, UiTheme theme, RowBox box, HubSettingRow.Cycle cycle, int left, int right, int labelCenter, int below, int mouseX, int mouseY) {
		if (usesSegments(cycle)) {
			List<Object> options = cycle.options();
			List<Component> labels = new ArrayList<>();
			for (Object option : options) {
				labels.add((Component) cycle.optionLabel().apply(option));
			}
			int selected = Math.max(0, options.indexOf(cycle.getter().get()));
			float selection = anim.transition("segment:" + cycle.labelKey(), selected, 0.18F);
			int[] edges = UiWidgets.segmentEdges(font, left, labels);
			int hovered = -1;
			for (int i = 0; i < labels.size(); i++) {
				if (contains(mouseX, mouseY, edges[i], below, edges[i + 1] - edges[i], UiWidgets.SEGMENT_HEIGHT)) {
					hovered = i;
				}
			}
			box.segmentEdges = UiWidgets.segmented(context, font, theme, left, below, labels, selection, hovered);
			box.setControl(left, below, UiWidgets.segmentedWidth(font, labels), UiWidgets.SEGMENT_HEIGHT);
			return;
		}

		Component value = (Component) cycle.valueLabel().get();
		int buttonWidth = Math.min(110, UiText.width(font, value, UiText.Size.LABEL) + 26);
		int buttonX = right - buttonWidth;
		int buttonY = labelCenter - 8;
		boolean hovered = contains(mouseX, mouseY, buttonX, buttonY, buttonWidth, 16) || (dropdown != null && dropdown.box == box);
		UiShapes.roundedRect(context, buttonX, buttonY, buttonWidth, 16, 6, UiTheme.mix(theme.segmentBackground(), theme.segmentSelected(), hovered ? 1.0F : 0.0F));
		UiText.drawCentered(context, font, UiText.ellipsize(font, value, UiText.Size.LABEL, buttonWidth - 24), UiText.Size.LABEL, buttonX + 8, labelCenter, theme.text());
		UiIcons.draw(context, HubIcons.CHEVRON_DOWN, buttonX + buttonWidth - 14, labelCenter - 4, 8, theme.muted());
		box.setControl(buttonX, buttonY, buttonWidth, 16);
	}

	private Component sliderValue(HubSettingRow.Slider slider) {
		Component suffix = slider.suffixKey() == null || slider.suffixKey().isEmpty() ? Component.empty() : Component.translatable(slider.suffixKey());
		return Component.literal(String.valueOf(slider.getter().getAsInt())).append(suffix);
	}

	// ---- footer and dropdown --------------------------------------------------------------------

	private void drawFooter(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		Component done = CommonComponents.GUI_DONE;
		doneWidth = Math.max(60, UiWidgets.buttonWidth(font, done) + 12);
		doneX = x + width - PADDING - doneWidth;
		UiWidgets.button(context, font, theme, doneX, footerY, doneWidth, BUTTON_HEIGHT, done, UiWidgets.ButtonStyle.PRIMARY, contains(mouseX, mouseY, doneX, footerY, doneWidth, BUTTON_HEIGHT) ? 1.0F : 0.0F);
		if (feature.resetAction() != null) {
			Component reset = Component.translatable(EMUtilsTexts.UI_RESET);
			resetWidth = UiWidgets.buttonWidth(font, reset) + 4;
			resetX = doneX - 6 - resetWidth;
			UiWidgets.button(context, font, theme, resetX, footerY, resetWidth, BUTTON_HEIGHT, reset, UiWidgets.ButtonStyle.GHOST, contains(mouseX, mouseY, resetX, footerY, resetWidth, BUTTON_HEIGHT) ? 1.0F : 0.0F);
		} else {
			resetWidth = 0;
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void drawDropdown(GuiGraphicsExtractor context, UiTheme theme, Dropdown dropdown, int mouseX, int mouseY) {
		HubSettingRow.Cycle cycle = dropdown.cycle;
		List<Object> options = cycle.options();
		Object current = cycle.getter().get();
		int listWidth = dropdown.width;
		for (Object option : options) {
			listWidth = Math.max(listWidth, UiText.width(font, (Component) cycle.optionLabel().apply(option), UiText.Size.LABEL) + 30);
		}
		int listHeight = options.size() * DROPDOWN_ROW + 6;
		int listX = dropdown.x + dropdown.width - listWidth;
		int listY = dropdown.y + 19;
		dropdown.listX = listX;
		dropdown.listY = listY;
		dropdown.listWidth = listWidth;
		UiShapes.shadow(context, listX, listY, listWidth, listHeight, 8, 10, theme.shadow());
		UiShapes.borderedRect(context, listX, listY, listWidth, listHeight, 8, theme.surface(), theme.line());
		for (int i = 0; i < options.size(); i++) {
			int rowY = listY + 3 + i * DROPDOWN_ROW;
			boolean hovered = contains(mouseX, mouseY, listX + 3, rowY, listWidth - 6, DROPDOWN_ROW);
			boolean selected = options.get(i).equals(current);
			if (hovered || selected) {
				UiShapes.roundedRect(context, listX + 3, rowY, listWidth - 6, DROPDOWN_ROW, 5, selected ? UiTheme.fade(theme.accent(), 0.22F) : theme.hover());
			}
			UiText.drawCentered(context, font, (Component) cycle.optionLabel().apply(options.get(i)), UiText.Size.LABEL, listX + 10, rowY + DROPDOWN_ROW / 2, selected ? theme.text() : theme.textSecondary());
		}
	}

	// ---- input ----------------------------------------------------------------------------------

	boolean mouseClicked(double mouseX, double mouseY) {
		if (closing) {
			return true;
		}
		if (colorPicker != null) {
			if (colorPicker.contains(mouseX, mouseY)) {
				colorPicker.mouseClicked(mouseX, mouseY);
			} else {
				closeColorPicker();
			}
			return true;
		}
		if (dropdown != null) {
			clickDropdown(dropdown, mouseX, mouseY);
			dropdown = null;
			return true;
		}
		if (!contains(mouseX, mouseY, x, y, width, height)) {
			close();
			return true;
		}
		if (contains(mouseX, mouseY, doneX, footerY, doneWidth, BUTTON_HEIGHT)) {
			close();
			return true;
		}
		if (resetWidth > 0 && contains(mouseX, mouseY, resetX, footerY, resetWidth, BUTTON_HEIGHT)) {
			feature.resetAction().run();
			rowsDirty = true;
			return true;
		}
		if (scroll.mouseClicked(mouseX, mouseY)) {
			return true;
		}
		if (feature.toggle() != null && contains(mouseX, mouseY, switchX - 2, switchY - 2, UiWidgets.SWITCH_WIDTH + 4, UiWidgets.SWITCH_HEIGHT + 4)) {
			feature.toggle().setter().accept(!feature.toggle().getter().getAsBoolean());
			return true;
		}
		if (scroll.contains(mouseX, mouseY)) {
			for (RowBox box : boxes) {
				if (contains(mouseX, mouseY, box.x, box.y, box.width, box.height)) {
					clickRow(box, mouseX, mouseY);
					return true;
				}
			}
		}
		return true;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void clickRow(RowBox box, double mouseX, double mouseY) {
		HubSettingRow row = box.row;
		if (row instanceof HubSettingRow.Toggle toggle) {
			toggle.setter().accept(!toggle.getter().getAsBoolean());
		} else if (row instanceof HubSettingRow.Slider slider) {
			if (contains(mouseX, mouseY, box.controlX, box.controlY - 4, box.controlWidth, box.controlHeight + 8)) {
				draggingSlider = slider;
				draggingX = box.controlX;
				draggingWidth = box.controlWidth;
				drag(mouseX);
			}
		} else if (row instanceof HubSettingRow.Cycle cycle) {
			if (usesSegments(cycle) && box.segmentEdges != null) {
				for (int i = 0; i < cycle.options().size(); i++) {
					if (mouseX >= box.segmentEdges[i] && mouseX < box.segmentEdges[i + 1] && contains(mouseX, mouseY, box.controlX, box.controlY, box.controlWidth, box.controlHeight)) {
						cycle.setter().accept(cycle.options().get(i));
					}
				}
			} else if (cycle.options() != null && contains(mouseX, mouseY, box.controlX, box.controlY, box.controlWidth, box.controlHeight)) {
				dropdown = new Dropdown(box, cycle, box.controlX, box.controlY, box.controlWidth);
			} else if (cycle.options() == null && contains(mouseX, mouseY, box.controlX, box.controlY, box.controlWidth, box.controlHeight)) {
				cycle.setter().accept(cycle.next().get());
			}
		} else if (row instanceof HubSettingRow.Rgb rgb) {
			colorPicker = new UiColorPicker(rgb.getter(), rgb.setter(), box.anchorX, box.anchorY, screenWidth(), screenHeight());
		} else if (row instanceof HubSettingRow.Action action) {
			if (action.enabled() && contains(mouseX, mouseY, box.controlX, box.controlY, box.controlWidth, box.controlHeight)) {
				action.action().run();
			}
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void clickDropdown(Dropdown dropdown, double mouseX, double mouseY) {
		List<Object> options = dropdown.cycle.options();
		for (int i = 0; i < options.size(); i++) {
			int rowY = dropdown.listY + 3 + i * DROPDOWN_ROW;
			if (contains(mouseX, mouseY, dropdown.listX + 3, rowY, dropdown.listWidth - 6, DROPDOWN_ROW)) {
				dropdown.cycle.setter().accept(options.get(i));
			}
		}
	}

	boolean mouseDragged(double mouseX, double mouseY) {
		if (colorPicker != null) {
			colorPicker.drag(mouseX, mouseY);
			return true;
		}
		if (draggingSlider != null) {
			drag(mouseX);
			return true;
		}
		return scroll.mouseDragged(mouseY);
	}

	private void drag(double mouseX) {
		HubSettingRow.Slider slider = draggingSlider;
		draggingFraction = (float) Math.clamp((mouseX - draggingX) / Math.max(1, draggingWidth), 0.0, 1.0);
		slider.setter().accept(slider.min() + Math.round(draggingFraction * (slider.max() - slider.min())));
	}

	boolean mouseReleased() {
		if (colorPicker != null) {
			colorPicker.release();
		}
		if (draggingSlider != null) {
			// Lets the knob settle from the mouse position onto the chosen value.
			anim.set("slider:" + draggingSlider.labelKey(), draggingFraction);
			draggingSlider = null;
		}
		scroll.mouseReleased();
		return true;
	}

	boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if (dropdown == null && colorPicker == null) {
			scroll.scroll(mouseX, mouseY, amount);
		}
		return true;
	}

	/** Esc closes the innermost thing that is open: the dropdown, the color picker, then the sheet. */
	boolean keyPressed(KeyEvent input) {
		if (colorPicker != null && colorPicker.keyPressed(input)) {
			return true;
		}
		if (input.isEscape()) {
			if (dropdown != null) {
				dropdown = null;
			} else if (colorPicker != null) {
				closeColorPicker();
			} else {
				close();
			}
		}
		return true;
	}

	/** Opens the color picker of the first color setting; used by UI snapshots. */
	void openFirstColorPicker() {
		for (RowBox box : boxes) {
			if (box.row instanceof HubSettingRow.Rgb rgb) {
				colorPicker = new UiColorPicker(rgb.getter(), rgb.setter(), box.anchorX, box.anchorY, screenWidth(), screenHeight());
				return;
			}
		}
	}

	boolean charTyped(CharacterEvent input) {
		if (colorPicker != null) {
			colorPicker.charTyped(input);
		}
		return true;
	}

	private void closeColorPicker() {
		if (colorPicker != null) {
			colorPicker.blur();
			colorPicker.release();
			colorPicker = null;
		}
	}

	private static int screenWidth() {
		return Minecraft.getInstance().getWindow().getGuiScaledWidth();
	}

	private static int screenHeight() {
		return Minecraft.getInstance().getWindow().getGuiScaledHeight();
	}

	private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private static final class RowBox {
		private final HubSettingRow row;
		private final int top;
		private int height;
		private List<Component> description = List.of();
		private int x;
		private int y;
		private int width;
		private int controlX;
		private int controlY;
		private int controlWidth;
		private int controlHeight;
		private int anchorX;
		private int anchorY;
		private int @Nullable [] segmentEdges;

		private RowBox(HubSettingRow row, int top) {
			this.row = row;
			this.top = top;
		}

		private void setControl(int x, int y, int width, int height) {
			controlX = x;
			controlY = y;
			controlWidth = width;
			controlHeight = height;
		}
	}

	@SuppressWarnings("rawtypes")
	private static final class Dropdown {
		private final RowBox box;
		private final HubSettingRow.Cycle cycle;
		private final int x;
		private final int y;
		private final int width;
		private int listX;
		private int listY;
		private int listWidth;

		private Dropdown(RowBox box, HubSettingRow.Cycle cycle, int x, int y, int width) {
			this.box = box;
			this.cycle = cycle;
			this.x = x;
			this.y = y;
			this.width = width;
		}
	}
}
