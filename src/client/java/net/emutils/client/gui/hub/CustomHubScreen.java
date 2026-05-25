package net.emutils.client.gui.hub;

import java.util.ArrayList;
import java.util.List;
import net.emutils.client.gui.hub.widget.HubActionButtonWidget;
import net.emutils.client.gui.hub.widget.HubColorSwatchWidget;
import net.emutils.client.gui.hub.widget.HubHeaderButtonWidget;
import net.emutils.client.gui.hub.widget.HubSliderWidget;
import net.emutils.client.util.EMUtilsTexts;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

public final class CustomHubScreen extends Screen {
	private static final int PANEL_MARGIN = 24;
	private static final int SIDEBAR_WIDTH = 148;
	private static final int HEADER_HEIGHT = 40;
	private static final int CONTENT_PADDING = 12;
	private static final int BOTTOM_PADDING = 12;
	private static final int SIDEBAR_CLASSIC_PADDING = 10;
	private static final int FOOTER_BUTTON_HEIGHT = 20;
	private static final int LABEL_COLUMN_WIDTH = 168;

	private final Screen parent;
	private final List<HubCategory> categories = HubSettingsRegistry.visibleCategories();
	private final List<ClickableWidget> contentWidgets = new ArrayList<>();

	private HubCategory selectedCategory;
	@Nullable
	private HubHeaderButtonWidget resetButton;
	@Nullable
	private HubColorPickerSession colorPicker;
	private int scrollOffset;
	private int maxScroll;
	private int sidebarScrollOffset;
	private int sidebarMaxScroll;
	private int panelX;
	private int panelY;
	private int panelWidth;
	private int panelHeight;
	private int sidebarX;
	private int sidebarY;
	private int sidebarWidth;
	private int contentX;
	private int contentY;
	private int contentWidth;
	private int contentHeight;
	private int controlsWidth;
	@Nullable
	private HubActionButtonWidget classicButton;
	@Nullable
	private HubHeaderButtonWidget doneButton;
	private int classicButtonY;

	public CustomHubScreen(Screen parent) {
		super(Text.translatable(EMUtilsTexts.HUB_MODERN_TITLE));
		this.parent = parent;
		this.selectedCategory = categories.getFirst();
	}

	@Override
	protected void init() {
		HubRoundedGraphics.prewarm();
		layoutPanel();
		rebuildContent();
		rebuildResetButton();
		rebuildFooterButtons();
		updateSidebarScroll();
	}

	private void rebuildFooterButtons() {
		if (classicButton != null) {
			remove(classicButton);
		}

		if (doneButton != null) {
			remove(doneButton);
		}

		classicButtonY = panelY + panelHeight - BOTTOM_PADDING - HubPanelTheme.ROW_HEIGHT;
		classicButton = addDrawableChild(new HubActionButtonWidget(
			sidebarX,
			classicButtonY,
			sidebarWidth,
			Text.translatable(EMUtilsTexts.HUB_CLASSIC_OPEN),
			button -> client.setScreen(new net.emutils.client.gui.EMUtilsHubScreen(this))
		));

		Text doneLabel = ScreenTexts.DONE;
		int doneWidth = HubHeaderButtonWidget.widthFor(doneLabel);
		doneButton = addDrawableChild(new HubHeaderButtonWidget(
			controlsRightEdge() - doneWidth,
			panelY + panelHeight - BOTTOM_PADDING - FOOTER_BUTTON_HEIGHT,
			doneWidth,
			doneLabel,
			this::close
		));
		updateSidebarScroll();
	}

	private int sidebarListHeight() {
		return Math.max(0, classicButtonY - SIDEBAR_CLASSIC_PADDING - sidebarY);
	}

	private int sidebarContentHeight() {
		int height = 0;
		for (HubCategory ignored : categories) {
			height += HubPanelTheme.ROW_HEIGHT + HubPanelTheme.SECTION_GAP / 2;
		}

		return height;
	}

	private void updateSidebarScroll() {
		sidebarMaxScroll = Math.max(0, sidebarContentHeight() - sidebarListHeight());
		sidebarScrollOffset = MathHelper.clamp(sidebarScrollOffset, 0, sidebarMaxScroll);
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		super.renderBackground(context, mouseX, mouseY, delta);
		context.fill(0, 0, width, height, HubPanelTheme.DIM_OVERLAY);
	}

	private void layoutPanel() {
		panelWidth = Math.min(660, width - PANEL_MARGIN * 2);
		panelHeight = Math.min(440, height - PANEL_MARGIN * 2);
		panelX = (width - panelWidth) / 2;
		panelY = (height - panelHeight) / 2;
		sidebarX = panelX + CONTENT_PADDING;
		sidebarY = panelY + HEADER_HEIGHT + CONTENT_PADDING;
		sidebarWidth = SIDEBAR_WIDTH - CONTENT_PADDING * 2;
		contentX = panelX + SIDEBAR_WIDTH + CONTENT_PADDING;
		contentY = panelY + HEADER_HEIGHT + CONTENT_PADDING;
		contentWidth = panelWidth - SIDEBAR_WIDTH - CONTENT_PADDING * 3;
		int contentFooterZone = BOTTOM_PADDING + FOOTER_BUTTON_HEIGHT + BOTTOM_PADDING;
		contentHeight = panelHeight - HEADER_HEIGHT - CONTENT_PADDING - contentFooterZone;
		controlsWidth = contentWidth - HubLayout.SCROLLBAR_GUTTER - HubLayout.CONTROLS_RIGHT_PADDING;
	}

	private int controlsRightEdge() {
		return contentX + controlsWidth;
	}

	private void rebuildResetButton() {
		if (resetButton != null) {
			remove(resetButton);
			resetButton = null;
		}

		boolean canReset = selectedCategory != HubCategory.PACKS && selectedCategory != HubCategory.SCRIPTS;
		if (!canReset) {
			return;
		}

		Text resetLabel = Text.translatable(EMUtilsTexts.OPTION_RESET_DEFAULTS);
		int resetWidth = HubHeaderButtonWidget.widthFor(resetLabel);
		resetButton = addDrawableChild(new HubHeaderButtonWidget(
			controlsRightEdge() - resetWidth,
			panelY + 11,
			resetWidth,
			resetLabel,
			() -> HubSettingsRegistry.resetAction(selectedCategory, this::rebuildContent).run()
		));
	}

	private void selectCategory(HubCategory category) {
		if (selectedCategory == category) {
			return;
		}

		closeColorPicker();
		selectedCategory = category;
		scrollOffset = 0;
		rebuildContent();
		rebuildResetButton();
	}

	private void openColorPicker(HubSettingRow.Rgb row, int swatchX, int swatchY) {
		colorPicker = new HubColorPickerSession(row.getter(), row.setter(), swatchX, swatchY);
	}

	private void closeColorPicker() {
		if (colorPicker != null) {
			colorPicker.release();
			colorPicker.close();
			colorPicker = null;
		}
	}

	private void rebuildContent() {
		contentWidgets.forEach(this::remove);
		contentWidgets.clear();

		List<HubSettingRow> rows = HubSettingsRegistry.rows(selectedCategory, this::rebuildContent);
		int y = contentY - scrollOffset;
		for (HubSettingRow row : rows) {
			int rowBottom = y + row.height();
			if (rowBottom > contentY && y < contentY + contentHeight) {
				addRowWidgets(row, y);
			}

			y += row.height();
		}

		maxScroll = Math.max(0, HubSettingRow.totalHeight(rows) - contentHeight + CONTENT_PADDING);
		scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);
		updateContentWidgetVisibility();
	}

	private void updateContentWidgetVisibility() {
		int contentBottom = contentY + contentHeight;
		for (ClickableWidget widget : contentWidgets) {
			int widgetBottom = widget.getY() + widget.getHeight();
			widget.visible = widget.getY() < contentBottom && widgetBottom > contentY;
		}
	}

	private void addRowWidgets(HubSettingRow row, int y) {
		switch (row) {
			case HubSettingRow.Spacer ignored -> {}
			case HubSettingRow.Divider ignored -> {}
			case HubSettingRow.Slider slider -> {
				Text suffix = slider.suffixKey() == null || slider.suffixKey().isEmpty()
					? Text.empty()
					: Text.translatable(slider.suffixKey());
				HubSliderWidget widget = new HubSliderWidget(
					contentX + LABEL_COLUMN_WIDTH,
					y,
					HubLayout.SLIDER_TRACK_WIDTH,
					suffix,
					slider.min(),
					slider.max(),
					slider.getter(),
					slider.setter()
				);
				contentWidgets.add(widget);
				addDrawableChild(widget);
			}
			case HubSettingRow.Rgb rgb -> {
				int swatchX = controlsRightEdge() - HubLayout.COLOR_SWATCH_SIZE;
				HubColorSwatchWidget widget = new HubColorSwatchWidget(
					swatchX,
					y,
					rgb.getter(),
					() -> openColorPicker(rgb, swatchX, y + HubPanelTheme.ROW_HEIGHT / 2)
				);
				contentWidgets.add(widget);
				addDrawableChild(widget);
			}
			case HubSettingRow.Action action -> {
				int buttonWidth = Math.min(controlsWidth - 24, textRenderer.getWidth(action.label()) + 24);
				int buttonX = contentX + (controlsWidth - buttonWidth) / 2;
				HubActionButtonWidget button = new HubActionButtonWidget(buttonX, y, buttonWidth, action.label(), ignored -> action.action().run());
				button.active = action.enabled();
				contentWidgets.add(button);
				addDrawableChild(button);
			}
			default -> {
				for (ClickableWidget widget : row.createWidgets(contentX + LABEL_COLUMN_WIDTH, y, controlsWidth - LABEL_COLUMN_WIDTH)) {
					contentWidgets.add(widget);
					addDrawableChild(widget);
				}
			}
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		HubPanelTheme.drawPanel(context, panelX, panelY, panelWidth, panelHeight);
		HubPanelTheme.drawSidebarSurface(context, panelX + 1, panelY + HEADER_HEIGHT, SIDEBAR_WIDTH, panelHeight - HEADER_HEIGHT - 1);
		HubPanelTheme.drawContentSurface(
			context,
			panelX + SIDEBAR_WIDTH,
			panelY + HEADER_HEIGHT,
			panelWidth - SIDEBAR_WIDTH - 1,
			panelHeight - HEADER_HEIGHT - 1
		);
		context.fill(panelX + SIDEBAR_WIDTH, panelY + HEADER_HEIGHT, panelX + SIDEBAR_WIDTH + 1, panelY + panelHeight - 1, HubPanelTheme.DIVIDER);

		context.drawTextWithShadow(textRenderer, title, panelX + CONTENT_PADDING, panelY + 14, HubPanelTheme.TEXT_ACCENT);
		renderSidebar(context, mouseX, mouseY);
		if (sidebarMaxScroll > 0) {
			drawSidebarScrollbar(context);
		}

		int resetReserve = resetButton == null ? 0 : resetButton.getWidth() + 10;
		String trimmedTitle = textRenderer.trimToWidth(
			Text.translatable(selectedCategory.titleKey()).getString().replace("...", "").trim(),
			contentWidth - resetReserve
		);
		context.drawTextWithShadow(textRenderer, trimmedTitle, contentX, panelY + 14, HubPanelTheme.TEXT_PRIMARY);

		int scissorRight = contentX + contentWidth - HubLayout.SCROLLBAR_GUTTER;
		context.enableScissor(contentX, contentY, scissorRight, contentY + contentHeight);
		for (ClickableWidget widget : contentWidgets) {
			widget.render(context, mouseX, mouseY, delta);
		}

		renderSettingLabels(context);
		context.disableScissor();

		renderChromeWidget(context, resetButton, mouseX, mouseY, delta);
		renderChromeWidget(context, classicButton, mouseX, mouseY, delta);
		renderChromeWidget(context, doneButton, mouseX, mouseY, delta);

		if (maxScroll > 0) {
			drawScrollbar(context);
		}

		if (colorPicker != null) {
			colorPicker.render(context, mouseX, mouseY);
		}
	}

	private static void renderChromeWidget(
		DrawContext context,
		@Nullable ClickableWidget widget,
		int mouseX,
		int mouseY,
		float delta
	) {
		if (widget != null) {
			widget.render(context, mouseX, mouseY, delta);
		}
	}

	private void renderSidebar(DrawContext context, int mouseX, int mouseY) {
		int sidebarListBottom = classicButtonY - SIDEBAR_CLASSIC_PADDING;
		context.enableScissor(panelX + 1, sidebarY, panelX + SIDEBAR_WIDTH - 1, sidebarListBottom);
		int y = sidebarY - sidebarScrollOffset;
		for (HubCategory category : categories) {
			int rowBottom = y + HubPanelTheme.ROW_HEIGHT;
			if (rowBottom < sidebarY || y > sidebarListBottom) {
				y += HubPanelTheme.ROW_HEIGHT + HubPanelTheme.SECTION_GAP / 2;
				continue;
			}

			boolean selected = category == selectedCategory;
			boolean hovered = mouseX >= sidebarX
				&& mouseX < sidebarX + sidebarWidth
				&& mouseY >= y
				&& mouseY < rowBottom;

			if (selected) {
				HubPanelTheme.drawSelectedCategory(context, sidebarX, y, sidebarWidth, HubPanelTheme.ROW_HEIGHT);
			} else if (hovered) {
				HubPanelTheme.drawRowBackground(context, sidebarX, y, sidebarWidth, HubPanelTheme.ROW_HEIGHT, true);
			}

			Text label = sidebarLabel(category);
			context.drawTextWithShadow(
				textRenderer,
				label,
				sidebarX + 8,
				y + (HubPanelTheme.ROW_HEIGHT - 8) / 2,
				selected ? HubPanelTheme.TEXT_ACCENT : HubPanelTheme.TEXT_MUTED
			);
			y += HubPanelTheme.ROW_HEIGHT + HubPanelTheme.SECTION_GAP / 2;
		}

		context.disableScissor();
	}

	private static Text sidebarLabel(HubCategory category) {
		String raw = Text.translatable(category.titleKey()).getString();
		return Text.literal(raw.replace("...", "").trim());
	}

	private boolean handleSidebarClick(double mouseX, double mouseY) {
		if (mouseY >= classicButtonY - SIDEBAR_CLASSIC_PADDING) {
			return false;
		}

		if (mouseY < sidebarY || mouseY > classicButtonY - SIDEBAR_CLASSIC_PADDING) {
			return false;
		}

		int y = sidebarY - sidebarScrollOffset;
		for (HubCategory category : categories) {
			if (mouseX >= sidebarX
				&& mouseX < sidebarX + sidebarWidth
				&& mouseY >= y
				&& mouseY < y + HubPanelTheme.ROW_HEIGHT) {
				selectCategory(category);
				return true;
			}

			y += HubPanelTheme.ROW_HEIGHT + HubPanelTheme.SECTION_GAP / 2;
		}

		return false;
	}

	private void drawSidebarScrollbar(DrawContext context) {
		int trackWidth = HubLayout.SCROLLBAR_WIDTH;
		int trackX = panelX + SIDEBAR_WIDTH - trackWidth - 3;
		int trackTop = sidebarY + 2;
		int trackHeight = sidebarListHeight() - 4;
		int trackBottom = trackTop + trackHeight;

		context.fill(trackX, trackTop, trackX + trackWidth, trackBottom, HubPanelTheme.TRACK);

		double visibleRatio = sidebarListHeight() / (double) sidebarContentHeight();
		int thumbHeight = Math.max(20, (int) Math.round(trackHeight * visibleRatio));
		thumbHeight = Math.min(thumbHeight, trackHeight);
		int thumbRange = Math.max(0, trackHeight - thumbHeight);
		int thumbY = trackTop + (int) Math.round(thumbRange * (sidebarScrollOffset / (double) sidebarMaxScroll));
		context.fill(trackX, thumbY, trackX + trackWidth, thumbY + thumbHeight, HubPanelTheme.ACCENT);
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (colorPicker != null) {
			if (colorPicker.contains(click.x(), click.y(), width, height)) {
				colorPicker.handleClick(click.x(), click.y(), width, height);
				return true;
			}

			closeColorPicker();
			return true;
		}

		if (handleSidebarClick(click.x(), click.y())) {
			return true;
		}

		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseDragged(Click click, double deltaX, double deltaY) {
		if (colorPicker != null && colorPicker.handleDrag(click.x(), click.y(), width, height)) {
			return true;
		}

		return super.mouseDragged(click, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(Click click) {
		if (colorPicker != null) {
			colorPicker.release();
			return true;
		}

		return super.mouseReleased(click);
	}

	private void renderSettingLabels(DrawContext context) {
		List<HubSettingRow> rows = HubSettingsRegistry.rows(selectedCategory, this::rebuildContent);
		int y = contentY - scrollOffset;
		for (HubSettingRow row : rows) {
			if (row instanceof HubSettingRow.Spacer) {
				y += row.height();
				continue;
			}

			if (row instanceof HubSettingRow.Divider) {
				int lineY = y + row.height() / 2;
				context.fill(
					contentX,
					lineY,
					contentX + contentWidth - HubLayout.SCROLLBAR_GUTTER,
					lineY + 1,
					HubPanelTheme.DIVIDER
				);
				y += row.height();
				continue;
			}

			if (row instanceof HubSettingRow.Action) {
				y += row.height();
				continue;
			}

			int labelY = y + (HubPanelTheme.ROW_HEIGHT - 8) / 2;
			if (row instanceof HubSettingRow.Slider slider) {
				context.drawTextWithShadow(
					textRenderer,
					Text.translatable(slider.labelKey()),
					contentX,
					labelY,
					HubPanelTheme.TEXT_PRIMARY
				);
				for (ClickableWidget widget : contentWidgets) {
					if (widget instanceof HubSliderWidget sliderWidget && sliderWidget.getY() == y) {
						Text value = sliderWidget.valueText();
						context.drawTextWithShadow(
							textRenderer,
							value,
							controlsRightEdge() - textRenderer.getWidth(value),
							labelY,
							HubPanelTheme.TEXT_MUTED
						);
						break;
					}
				}
			} else if (row instanceof HubSettingRow.Rgb rgb) {
				context.drawTextWithShadow(
					textRenderer,
					Text.translatable(rgb.labelKey()),
					contentX,
					labelY,
					HubPanelTheme.TEXT_PRIMARY
				);
			} else if (row instanceof HubSettingRow.Toggle toggle) {
				context.drawTextWithShadow(
					textRenderer,
					Text.translatable(toggle.labelKey()),
					contentX,
					labelY,
					HubPanelTheme.TEXT_PRIMARY
				);
			} else if (row instanceof HubSettingRow.Cycle<?> cycle) {
				context.drawTextWithShadow(
					textRenderer,
					Text.translatable(cycle.labelKey()),
					contentX,
					labelY,
					HubPanelTheme.TEXT_PRIMARY
				);
			}

			y += row.height();
		}
	}

	private void drawScrollbar(DrawContext context) {
		int trackWidth = HubLayout.SCROLLBAR_WIDTH;
		int trackX = contentX + contentWidth - HubLayout.SCROLLBAR_GUTTER + (HubLayout.SCROLLBAR_GUTTER - trackWidth) / 2;
		int trackTop = contentY + 4;
		int trackHeight = contentHeight - 8;
		int trackBottom = trackTop + trackHeight;

		context.fill(trackX, trackTop, trackX + trackWidth, trackBottom, HubPanelTheme.TRACK);

		double visibleRatio = contentHeight / (double) (contentHeight + maxScroll);
		int thumbHeight = Math.max(24, (int) Math.round(trackHeight * visibleRatio));
		thumbHeight = Math.min(thumbHeight, trackHeight);
		int thumbRange = Math.max(0, trackHeight - thumbHeight);
		int thumbY = trackTop + (int) Math.round(thumbRange * (scrollOffset / (double) maxScroll));
		context.fill(trackX, thumbY, trackX + trackWidth, thumbY + thumbHeight, HubPanelTheme.ACCENT);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (colorPicker != null) {
			return true;
		}

		int scrollStep = (int) (verticalAmount * 16.0);
		if (sidebarMaxScroll > 0
			&& mouseX >= panelX
			&& mouseX < panelX + SIDEBAR_WIDTH
			&& mouseY >= sidebarY
			&& mouseY < classicButtonY - SIDEBAR_CLASSIC_PADDING) {
			sidebarScrollOffset = MathHelper.clamp(sidebarScrollOffset - scrollStep, 0, sidebarMaxScroll);
			return true;
		}

		if (maxScroll > 0 && mouseX >= contentX && mouseX <= contentX + contentWidth && mouseY >= contentY && mouseY <= contentY + contentHeight) {
			scrollOffset = MathHelper.clamp(scrollOffset - scrollStep, 0, maxScroll);
			rebuildContent();
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public void close() {
		closeColorPicker();
		HubRoundedGraphics.clearCache();
		if (client != null) {
			HubColorPickerGraphics.clearHueWheel(client);
			client.setScreen(parent);
		}
	}
}
