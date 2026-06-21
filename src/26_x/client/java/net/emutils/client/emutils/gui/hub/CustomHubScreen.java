package net.emutils.client.emutils.gui.hub;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.commandshortcuts.gui.CommandShortcutListScreen;
import net.emutils.client.emutils.compat.MinescriptCompat;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.minescript.gui.ScriptManagerScreen;
import net.emutils.client.emutils.packs.gui.PackManagerScreen;
import net.emutils.client.emutils.screenshot.gui.ScreenshotGalleryScreen;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.emutils.client.emutils.waypoint.gui.WaypointListScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

public final class CustomHubScreen extends Screen {
	private static final int PANEL_MARGIN = 28;
	private static final int PANEL_MAX_WIDTH = 660;
	private static final int PANEL_MAX_HEIGHT = 390;
	private static final int SIDEBAR_WIDTH = 146;
	private static final int HEADER_HEIGHT = 48;
	private static final int SEARCH_HEIGHT = 26;
	private static final int SEARCH_MAX_WIDTH = 300;
	private static final int CONTENT_PADDING = 12;
	private static final int FEATURE_HEIGHT = 48;
	private static final int FEATURE_GAP = 10;
	private static final int EXPANDED_MENU_GAP = 0;
	private static final int LIST_BOTTOM_FADE_HEIGHT = 30;
	private static final int SETTINGS_BOTTOM_FADE_HEIGHT = 24;
	private static final int COLLAPSE_DRAW_THRESHOLD = 14;
	private static final int MENU_PADDING = 14;
	private static final int MENU_MAX_HEIGHT = 198;
	private static final int MENU_ROUNDED_MIN_HEIGHT = 28;
	private static final int MENU_SURFACE = 0xFF101726;
	private static final int SETTING_ROW_HEIGHT = 34;
	private static final int TOGGLE_WIDTH = 32;
	private static final int TOGGLE_HEIGHT = 14;
	private static final int TOGGLE_THUMB = 10;
	private static final int ICON_BOX = 26;
	private static final int ICON_SIZE = 16;
	private static final int ACTION_BUTTON_WIDTH = 76;
	private static final int VALUE_COLUMN_WIDTH = 54;
	private static final int SCROLLBAR_LANE_WIDTH = 18;
	private static final int SETTINGS_SCROLLBAR_LANE_WIDTH = 10;
	private static final float BRAND_SCALE = 1.25F;
	private static final int EXPAND_ANIMATION_FRAMES = 40;
	private static final int COLLAPSE_ANIMATION_FRAMES = 40;
	private static final float EXPAND_ANIMATION_STEP = 1.0F / EXPAND_ANIMATION_FRAMES;
	private static final float COLLAPSE_ANIMATION_STEP = 1.0F / COLLAPSE_ANIMATION_FRAMES;

	private final Screen parent;
	private final List<FeatureSpec> features;
	private final Component versionText;
	private Font textRenderer;
	private FeatureGroup selectedGroup = FeatureGroup.RENDER;
	@Nullable
	private FeatureSpec expandedFeature;
	@Nullable
	private FeatureSpec collapsingFeature;
	private float expandProgress = 1.0F;
	private float collapseProgress;
	private HubSettingRow.Slider draggingSlider;
	private int draggingSliderX;
	private int draggingSliderWidth;
	@Nullable
	private HubColorPickerSession colorPicker;
	private String search = "";
	private boolean searchFocused;
	private boolean suppressListBottomFade;
	private boolean suppressSettingsBottomFade;
	private double scrollOffsetTarget;
	private double settingsScrollTarget;
	private double scrollOffset;
	private int maxScroll;
	private double settingsScroll;
	private int settingsMaxScroll;
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
	private int searchX;
	private int searchY;
	private int searchWidth;
	private int doneX;
	private int doneY;
	private int doneWidth;
	private int doneHeight;

	public CustomHubScreen(Screen parent) {
		super(Component.translatable(EMUtilsTexts.HUB_MODERN_TITLE));
		this.parent = parent;
		this.features = createFeatures();
		String version = FabricLoader.getInstance()
			.getModContainer(EMUtilsClient.MOD_ID)
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse("unknown");
		this.versionText = Component.literal("EMUtils v" + version);
	}

	@Override
	protected void init() {
		textRenderer = font;
		HubRoundedGraphics.prewarm();
		layout();
		updateScrollBounds();
	}

	private void layout() {
		panelWidth = Math.min(PANEL_MAX_WIDTH, width - PANEL_MARGIN * 2);
		panelHeight = Math.min(PANEL_MAX_HEIGHT, height - PANEL_MARGIN * 2);
		panelX = (width - panelWidth) / 2;
		panelY = (height - panelHeight) / 2;
		doneHeight = 22;
		doneWidth = textRenderer.width(CommonComponents.GUI_DONE) + 18;
		doneX = panelX + panelWidth - CONTENT_PADDING - doneWidth;
		doneY = panelY + (HEADER_HEIGHT - doneHeight) / 2;
		sidebarX = panelX + CONTENT_PADDING;
		sidebarY = panelY + HEADER_HEIGHT + CONTENT_PADDING;
		sidebarWidth = SIDEBAR_WIDTH - CONTENT_PADDING * 2;
		contentX = panelX + SIDEBAR_WIDTH + CONTENT_PADDING;
		searchY = panelY + (HEADER_HEIGHT - SEARCH_HEIGHT) / 2;
		int availableSearchWidth = Math.max(160, doneX - CONTENT_PADDING - contentX);
		searchWidth = Math.min(SEARCH_MAX_WIDTH, availableSearchWidth);
		searchX = contentX + Math.max(0, (availableSearchWidth - searchWidth) / 2);
		contentY = panelY + HEADER_HEIGHT + CONTENT_PADDING;
		contentWidth = panelX + panelWidth - CONTENT_PADDING - contentX;
		contentHeight = panelY + panelHeight - CONTENT_PADDING - contentY;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		super.extractBackground(context, mouseX, mouseY, delta);
		context.fill(0, 0, width, height, HubPanelTheme.DIM_OVERLAY);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		updateScrollAnimation();
		updateExpandAnimation();
		renderPanel(context, mouseX, mouseY);
		renderSidebar(context, mouseX, mouseY);
		renderSearch(context);

		context.enableScissor(contentX, contentY, contentX + contentWidth, contentY + contentHeight);
		renderFeatureList(context, mouseX, mouseY);
		context.disableScissor();

		if (maxScroll > 0 && scrollOffset < maxScroll && !suppressListBottomFade) {
			renderBottomFade(context);
		}
		if (maxScroll > 0) {
			renderScrollbar(context);
		}
		if (colorPicker != null) {
			colorPicker.render(context, mouseX, mouseY);
		}
	}

	private void renderPanel(GuiGraphicsExtractor context, int mouseX, int mouseY) {
		HubPanelTheme.drawPanel(context, panelX, panelY, panelWidth, panelHeight);
		HubPanelTheme.drawSidebarSurface(context, panelX + 1, panelY + HEADER_HEIGHT, SIDEBAR_WIDTH, panelHeight - HEADER_HEIGHT - 1);
		HubPanelTheme.drawContentSurface(
			context,
			panelX + SIDEBAR_WIDTH,
			panelY + HEADER_HEIGHT,
			panelWidth - SIDEBAR_WIDTH - 1,
			panelHeight - HEADER_HEIGHT - 1
		);
		Component brand = Component.translatable(EMUtilsTexts.NAME);
		drawCenteredScaledText(
			context,
			brand,
			panelX + SIDEBAR_WIDTH / 2,
			panelY + Math.round((HEADER_HEIGHT - textRenderer.lineHeight * BRAND_SCALE) / 2.0F),
			BRAND_SCALE,
			HubPanelTheme.TEXT_ACCENT
		);
		renderPillButton(context, doneX, doneY, doneWidth, doneHeight, CommonComponents.GUI_DONE, contains(mouseX, mouseY, doneX, doneY, doneWidth, doneHeight));
		context.text(
			textRenderer,
			versionText,
			panelX + Math.max(0, (SIDEBAR_WIDTH - textRenderer.width(versionText)) / 2),
			panelY + panelHeight - 18,
			HubPanelTheme.TEXT_DIM,
			false
		);
	}

	private void updateExpandAnimation() {
		if (expandedFeature == null && collapsingFeature == null) {
			expandProgress = 1.0F;
			collapseProgress = 0.0F;
			return;
		}
		if (expandedFeature != null) {
			expandProgress = Math.min(1.0F, expandProgress + EXPAND_ANIMATION_STEP);
		}
		if (collapsingFeature != null) {
			collapseProgress = Math.max(0.0F, collapseProgress - COLLAPSE_ANIMATION_STEP);
			if (collapseProgress <= 0.0F) {
				collapsingFeature = null;
				updateScrollBounds();
			}
		}
	}

	private void updateScrollAnimation() {
		double speed = 0.20;
		if (Math.abs(scrollOffsetTarget - scrollOffset) > 0.05) {
			scrollOffset += (scrollOffsetTarget - scrollOffset) * speed;
		} else {
			scrollOffset = scrollOffsetTarget;
		}

		if (Math.abs(settingsScrollTarget - settingsScroll) > 0.05) {
			settingsScroll += (settingsScrollTarget - settingsScroll) * speed;
		} else {
			settingsScroll = settingsScrollTarget;
		}
	}

	private void renderSidebar(GuiGraphicsExtractor context, int mouseX, int mouseY) {
		int y = sidebarY;
		for (FeatureGroup group : FeatureGroup.values()) {
			boolean selected = group == selectedGroup;
			boolean hovered = contains(mouseX, mouseY, sidebarX, y, sidebarWidth, HubPanelTheme.ROW_HEIGHT);
			if (selected) {
				HubPanelTheme.drawSelectedCategory(context, sidebarX, y, sidebarWidth, HubPanelTheme.ROW_HEIGHT);
			} else if (hovered) {
				HubPanelTheme.drawRowBackground(context, sidebarX, y, sidebarWidth, HubPanelTheme.ROW_HEIGHT, true);
			}
			int color = selected ? HubPanelTheme.TEXT_PRIMARY : HubPanelTheme.TEXT_MUTED;
			drawIcon(context, group.icon(), sidebarX + 8, y + 5, ICON_SIZE);
			drawClippedText(
				context,
				Component.translatable(group.labelKey()),
				sidebarX + 32,
				y + (HubPanelTheme.ROW_HEIGHT - textRenderer.lineHeight) / 2,
				sidebarWidth - 38,
				color
			);
			y += HubPanelTheme.ROW_HEIGHT + 8;
		}
	}

	private void renderSearch(GuiGraphicsExtractor context) {
		int color = searchFocused ? 0xFF182234 : 0xFF121A28;
		HubRoundedGraphics.drawRoundedRect(
			context,
			searchX,
			searchY,
			searchX + searchWidth,
			searchY + SEARCH_HEIGHT,
			searchFocused ? HubPanelTheme.ACCENT_DIM : HubPanelTheme.BORDER,
			HubRoundedGraphics.RADIUS_MD
		);
		HubRoundedGraphics.drawRoundedRect(
			context,
			searchX + 1,
			searchY + 1,
			searchX + searchWidth - 1,
			searchY + SEARCH_HEIGHT - 1,
			color,
			HubRoundedGraphics.RADIUS_MD
		);
		drawIcon(context, HubIcons.SEARCH, searchX + 12, searchY + 5, ICON_SIZE);
		Component display = search.isEmpty() && searchFocused
			? Component.empty()
			: search.isEmpty()
			? Component.translatable(EMUtilsTexts.HUB_SEARCH_PLACEHOLDER)
			: Component.literal(textRenderer.plainSubstrByWidth(search, searchWidth - 48));
		context.text(
			textRenderer,
			display,
			searchX + 34,
			searchY + (SEARCH_HEIGHT - textRenderer.lineHeight) / 2 + 1,
			search.isEmpty() ? HubPanelTheme.TEXT_MUTED : HubPanelTheme.TEXT_PRIMARY,
			false
		);
		if (search.isEmpty() && !searchFocused) {
			String keybind = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac") ? "(⌘F)" : "(Ctrl+F)";
			Component keybindText = Component.literal(keybind);
			int keybindWidth = textRenderer.width(keybindText);
			context.text(
				textRenderer,
				keybindText,
				searchX + searchWidth - 12 - keybindWidth,
				searchY + (SEARCH_HEIGHT - textRenderer.lineHeight) / 2 + 1,
				HubPanelTheme.TEXT_DIM,
				false
			);
		}
		if (searchFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
			String visibleSearch = textRenderer.plainSubstrByWidth(search, searchWidth - 48);
			int cursorX = Math.min(searchX + searchWidth - 10, searchX + 34 + textRenderer.width(visibleSearch));
			context.fill(cursorX, searchY + 7, cursorX + 1, searchY + SEARCH_HEIGHT - 7, HubPanelTheme.TEXT_ACCENT);
		}
	}

	private void renderFeatureList(GuiGraphicsExtractor context, int mouseX, int mouseY) {
		List<FeatureSpec> visible = visibleFeatures();
		if (visible.isEmpty()) {
			Component empty = Component.translatable(EMUtilsTexts.HUB_EMPTY_SEARCH);
			context.text(
				textRenderer,
				empty,
				contentX + (contentWidth - textRenderer.width(empty)) / 2,
				contentY + 24,
				HubPanelTheme.TEXT_MUTED,
				false
			);
			return;
		}

		int y = contentY - (int) Math.round(scrollOffset);
		for (FeatureSpec feature : visible) {
			int rowY = y;
			if (feature == expandedFeature || feature == collapsingFeature) {
				int menuY = rowY + FEATURE_HEIGHT + EXPANDED_MENU_GAP;
				int menuHeight = animatedMenuHeight(feature);
				if (menuHeight > 0 && intersectsContent(menuY, menuHeight)) {
					renderSettingsMenu(context, feature, menuY, menuHeight, mouseX, mouseY);
				}
				if (intersectsContent(rowY, FEATURE_HEIGHT)) {
					renderFeatureRow(context, feature, rowY, mouseX, mouseY);
				}
				if (menuHeight > 0) {
					y = menuY + menuHeight + FEATURE_GAP;
				} else {
					y = menuY + FEATURE_GAP;
				}
			} else {
				if (intersectsContent(rowY, FEATURE_HEIGHT)) {
					renderFeatureRow(context, feature, rowY, mouseX, mouseY);
				}
				y += FEATURE_HEIGHT + FEATURE_GAP;
			}
		}
	}

	private void renderFeatureRow(GuiGraphicsExtractor context, FeatureSpec feature, int y, int mouseX, int mouseY) {
		boolean expanded = isFeatureVisuallyExpanded(feature);
		boolean hovered = contains(mouseX, mouseY, contentX, y, contentWidth - scrollbarReserve(), FEATURE_HEIGHT);
		int right = contentX + contentWidth - scrollbarReserve();
		int background = expanded ? 0xFF31405F : hovered ? 0xFF26334A : 0xFF202A3D;
		HubRoundedGraphics.drawRoundedRect(context, contentX, y, right, y + FEATURE_HEIGHT, background, HubRoundedGraphics.RADIUS_MD);

		int iconX = contentX + 12;
		int iconY = y + (FEATURE_HEIGHT - ICON_BOX) / 2;
		HubRoundedGraphics.drawRoundedRect(context, iconX, iconY, iconX + ICON_BOX, iconY + ICON_BOX, 0xFF121A28, HubRoundedGraphics.RADIUS_MD);
		drawIcon(context, feature.icon().texture(), iconX + 5, iconY + 5, ICON_SIZE);

		String titleText = cleanTitle(feature.title());
		boolean expandable = canExpand(feature);
		int controlWidth = feature.primaryAction() != null
			? ACTION_BUTTON_WIDTH + 22 + (feature.toggle() == null ? 0 : TOGGLE_WIDTH + 10) + (expandable ? 22 : 0)
			: feature.toggle() == null && expandable ? 96 : expandable ? TOGGLE_WIDTH + 54 : TOGGLE_WIDTH + 34;
		int textWidth = Math.max(80, right - contentX - 52 - controlWidth);
		drawClippedText(context, Component.literal(titleText), contentX + 50, y + 9, textWidth, HubPanelTheme.TEXT_PRIMARY);
		String description = textRenderer.plainSubstrByWidth(
			Component.translatable(feature.descriptionKey()).getString(),
			textWidth
		);
		context.text(textRenderer, Component.literal(description), contentX + 50, y + 27, HubPanelTheme.TEXT_MUTED, false);

		if (expandable) {
			int chevronX;
			if (feature.primaryAction() != null) {
				int actionRight = feature.toggle() == null ? right - 10 : right - TOGGLE_WIDTH - 22;
				chevronX = actionRight - ACTION_BUTTON_WIDTH - 14;
			} else {
				chevronX = right - (feature.toggle() == null ? 20 : TOGGLE_WIDTH + 28);
			}
			drawIcon(
				context,
				expanded ? HubIcons.CHEVRON_UP : HubIcons.CHEVRON_DOWN,
				chevronX - ICON_SIZE / 2,
				y + (FEATURE_HEIGHT - ICON_SIZE) / 2,
				ICON_SIZE
			);
		}

		if (feature.primaryAction() != null) {
			int actionRight = right - 10;
			if (feature.toggle() != null) {
				int toggleX = right - TOGGLE_WIDTH - 12;
				renderToggle(context, feature.toggle().getter().getAsBoolean(), toggleX, y + (FEATURE_HEIGHT - TOGGLE_HEIGHT) / 2);
				actionRight = toggleX - 10;
			}
			int openX = actionRight - ACTION_BUTTON_WIDTH;
			boolean actionEnabled = feature.primaryActionEnabled();
			Component actionLabel = Component.translatable(actionEnabled ? EMUtilsTexts.HUB_ACTION_OPEN : EMUtilsTexts.HUB_ACTION_UNAVAILABLE);
			renderPillButton(context, openX, y + (FEATURE_HEIGHT - 19) / 2, ACTION_BUTTON_WIDTH, 19, actionLabel, hovered && actionEnabled, actionEnabled);
		} else if (feature.toggle() == null && canExpand(feature)) {
			Component label = Component.literal(expanded ? "Hide" : "Settings");
			int width = textRenderer.width(label) + 16;
			renderPillButton(context, right - width - 34, y + (FEATURE_HEIGHT - 18) / 2, width, 18, label, hovered);
		} else {
			renderToggle(context, feature.toggle().getter().getAsBoolean(), right - TOGGLE_WIDTH - 12, y + (FEATURE_HEIGHT - TOGGLE_HEIGHT) / 2);
		}
	}

	private void renderSettingsMenu(GuiGraphicsExtractor context, FeatureSpec feature, int y, int height, int mouseX, int mouseY) {
		if (feature == collapsingFeature && height < COLLAPSE_DRAW_THRESHOLD) {
			return;
		}
		int right = contentX + contentWidth - scrollbarReserve();
		int menuLeft = contentX + 18;
		int menuRight = right - 18;
		renderSettingsMenuSurface(context, menuLeft, y, menuRight, y + height);
		int innerX = menuLeft + MENU_PADDING + SETTINGS_SCROLLBAR_LANE_WIDTH;
		int innerY = y + MENU_PADDING;
		int innerW = menuRight - menuLeft - MENU_PADDING * 2 - SETTINGS_SCROLLBAR_LANE_WIDTH;
		int innerH = height - MENU_PADDING * 2;
		if (innerW <= 0 || innerH <= 0) {
			return;
		}
		int renderMaxScroll = Math.max(0, settingsContentHeight(feature) - innerH);
		int renderScroll = Mth.clamp(feature == expandedFeature ? (int) Math.round(settingsScroll) : 0, 0, renderMaxScroll);
		context.enableScissor(innerX, innerY, innerX + innerW, innerY + innerH);
		renderSettingRows(context, feature, innerX, innerY - renderScroll, innerW, mouseX, mouseY);
		context.disableScissor();
		boolean needsScrollbar = settingsContentHeight(feature) + MENU_PADDING * 2 > MENU_MAX_HEIGHT;
		if (needsScrollbar && renderMaxScroll > 0 && renderScroll < renderMaxScroll && !suppressSettingsBottomFade) {
			renderSettingsBottomFade(context, innerX, innerY, innerW, innerH);
		}
		if (needsScrollbar && renderMaxScroll > 0) {
			renderMiniScrollbar(context, menuLeft + 8, innerY, innerH, renderScroll, renderMaxScroll);
		}
	}

	private void renderSettingRows(GuiGraphicsExtractor context, FeatureSpec feature, int x, int y, int width, int mouseX, int mouseY) {
		for (HubSettingRow row : rows(feature)) {
			if (row instanceof HubSettingRow.Spacer spacer) {
				y += spacer.height();
				continue;
			}
			if (row instanceof HubSettingRow.Divider divider) {
				int lineY = y + divider.height() / 2;
				context.fill(x, lineY, x + width, lineY + 1, HubPanelTheme.DIVIDER);
				y += divider.height();
				continue;
			}
			int labelWidth = settingLabelWidth(width);
			int controlX = x + labelWidth + 18;
			int controlRight = x + width;
			int controlWidth = Math.max(80, controlRight - controlX);
			if (row instanceof HubSettingRow.Action action) {
				drawClippedText(context, action.label(), x, y + (SETTING_ROW_HEIGHT - textRenderer.lineHeight) / 2, width - ACTION_BUTTON_WIDTH - 12, HubPanelTheme.TEXT_PRIMARY);
				Component actionLabel = Component.translatable(action.enabled() ? EMUtilsTexts.HUB_ACTION_OPEN : EMUtilsTexts.HUB_ACTION_UNAVAILABLE);
				renderPillButton(
					context,
					controlRight - ACTION_BUTTON_WIDTH,
					y + 4,
					ACTION_BUTTON_WIDTH,
					19,
					actionLabel,
					action.enabled() && contains(mouseX, mouseY, x, y, width, SETTING_ROW_HEIGHT),
					action.enabled()
				);
				y += SETTING_ROW_HEIGHT;
				continue;
			}

			Component label = settingLabel(row);
			drawClippedText(context, label, x, y + (SETTING_ROW_HEIGHT - textRenderer.lineHeight) / 2, labelWidth - 8, HubPanelTheme.TEXT_PRIMARY);
			if (row instanceof HubSettingRow.Toggle toggle) {
				renderToggle(context, toggle.getter().getAsBoolean(), controlRight - TOGGLE_WIDTH, y + (SETTING_ROW_HEIGHT - TOGGLE_HEIGHT) / 2);
			} else if (row instanceof HubSettingRow.Slider slider) {
				Component value = sliderValue(slider);
				int valueWidth = Math.min(VALUE_COLUMN_WIDTH, textRenderer.width(value));
				int trackW = Math.max(120, controlWidth - VALUE_COLUMN_WIDTH - 18);
				int trackX = controlX;
				renderSlider(context, slider, trackX, y, trackW);
				drawClippedText(context, value, controlRight - valueWidth, y + (SETTING_ROW_HEIGHT - textRenderer.lineHeight) / 2, valueWidth, HubPanelTheme.TEXT_MUTED);
			} else if (row instanceof HubSettingRow.Cycle<?> cycle) {
				Component value = cycle.valueLabel().get();
				int pillW = Math.min(controlWidth, textRenderer.width(value) + 16);
				int pillX = controlRight - pillW;
				HubRoundedGraphics.drawPill(context, pillX, y + 3, pillX + pillW, y + 21, HubPanelTheme.TRACK);
				drawClippedText(context, value, pillX + 8, y + 8, pillW - 16, HubPanelTheme.TEXT_ACCENT);
			} else if (row instanceof HubSettingRow.Rgb rgb) {
				int swatchX = controlRight - HubLayout.COLOR_SWATCH_SIZE;
				int swatchY = y + (SETTING_ROW_HEIGHT - HubLayout.COLOR_SWATCH_SIZE) / 2;
				HubRoundedGraphics.drawRoundedRect(context, swatchX, swatchY, swatchX + HubLayout.COLOR_SWATCH_SIZE, swatchY + HubLayout.COLOR_SWATCH_SIZE, rgb.getter().getAsInt(), HubRoundedGraphics.RADIUS_SM);
			}
			y += SETTING_ROW_HEIGHT;
		}
	}

	private void renderSettingsMenuSurface(GuiGraphicsExtractor context, int left, int top, int right, int bottom) {
		int height = bottom - top;
		if (height <= 0) {
			return;
		}
		context.enableScissor(left, top, right, bottom);
		if (height < MENU_ROUNDED_MIN_HEIGHT) {
			context.fill(left, top, right, bottom, HubPanelTheme.DIVIDER);
			context.fill(left + 1, top + 1, right - 1, bottom - 1, MENU_SURFACE);
		} else {
			HubRoundedGraphics.drawRoundedRect(context, left, top, right, bottom, HubPanelTheme.DIVIDER, HubRoundedGraphics.RADIUS_MD);
			HubRoundedGraphics.drawRoundedRect(context, left + 1, top + 1, right - 1, bottom - 1, MENU_SURFACE, HubRoundedGraphics.RADIUS_MD);
		}
		context.disableScissor();
	}

	private void renderToggle(GuiGraphicsExtractor context, boolean enabled, int x, int y) {
		HubRoundedGraphics.drawPill(context, x, y, x + TOGGLE_WIDTH, y + TOGGLE_HEIGHT, enabled ? HubPanelTheme.ACCENT : 0xFF343A45);
		int travel = TOGGLE_WIDTH - TOGGLE_THUMB - 4;
		int thumbCenterX = x + 2 + TOGGLE_THUMB / 2 + (enabled ? travel : 0);
		HubRoundedGraphics.drawCircle(context, thumbCenterX, y + TOGGLE_HEIGHT / 2, TOGGLE_THUMB, HubPanelTheme.TEXT_PRIMARY);
	}

	private void renderSlider(GuiGraphicsExtractor context, HubSettingRow.Slider slider, int x, int y, int width) {
		int trackHeight = 8;
		int trackY = y + (SETTING_ROW_HEIGHT - trackHeight) / 2;
		drawCapsule(context, x, trackY, x + width, trackY + trackHeight, 0xFF343A45);
		double ratio = (slider.getter().getAsInt() - slider.min()) / (double) (slider.max() - slider.min());
		int fillRight = x + (int) Math.round(width * Mth.clamp(ratio, 0.0, 1.0));
		if (fillRight > x) {
			drawCapsule(context, x, trackY, Math.max(x + trackHeight, fillRight), trackY + trackHeight, HubPanelTheme.ACCENT);
		}
		int thumbX = Mth.clamp(fillRight, x + trackHeight / 2, x + width - trackHeight / 2);
		HubRoundedGraphics.drawCircle(context, thumbX, trackY + trackHeight / 2, 14, HubPanelTheme.TEXT_PRIMARY);
	}

	private void renderPillButton(GuiGraphicsExtractor context, int x, int y, int width, int height, Component label, boolean hovered) {
		renderPillButton(context, x, y, width, height, label, hovered, true);
	}

	private void renderPillButton(GuiGraphicsExtractor context, int x, int y, int width, int height, Component label, boolean hovered, boolean enabled) {
		int background = enabled
			? hovered ? HubPanelTheme.SURFACE_HOVER : HubPanelTheme.SURFACE
			: 0xFF1B2434;
		String visible = textRenderer.plainSubstrByWidth(label.getString(), Math.max(0, width - 8));
		int labelWidth = textRenderer.width(visible);
		HubRoundedGraphics.drawRoundedRect(context, x, y, x + width, y + height, background, HubRoundedGraphics.RADIUS_MD);
		context.text(
			textRenderer,
			Component.literal(visible),
			x + Math.max(0, (width - labelWidth) / 2),
			y + (height - textRenderer.lineHeight + 1) / 2,
			enabled ? HubPanelTheme.TEXT_ACCENT : HubPanelTheme.TEXT_MUTED,
			false
		);
	}

	private void renderBottomFade(GuiGraphicsExtractor context) {
		int fadeHeight = Math.min(LIST_BOTTOM_FADE_HEIGHT, contentHeight);
		int right = contentX + contentWidth - scrollbarReserve();
		context.fillGradient(
			contentX,
			contentY + contentHeight - fadeHeight,
			right,
			contentY + contentHeight,
			0x00222D42,
			HubPanelTheme.CONTENT
		);
	}

	private void renderSettingsBottomFade(GuiGraphicsExtractor context, int x, int y, int width, int height) {
		int fadeHeight = Math.min(SETTINGS_BOTTOM_FADE_HEIGHT, height);
		context.fillGradient(
			x,
			y + height - fadeHeight,
			x + width,
			y + height,
			0x00101726,
			0xFF101726
		);
	}

	private void drawCenteredScaledText(GuiGraphicsExtractor context, Component text, int centerX, int y, float scale, int color) {
		int textWidth = textRenderer.width(text);
		context.pose().pushMatrix();
		try {
			context.pose().translate(centerX - textWidth * scale / 2.0F, y);
			context.pose().scale(scale, scale);
			context.text(textRenderer, text, 0, 0, color, false);
		} finally {
			context.pose().popMatrix();
		}
	}

	private void drawClippedText(GuiGraphicsExtractor context, Component text, int x, int y, int maxWidth, int color) {
		if (maxWidth <= 0) {
			return;
		}
		context.text(textRenderer, Component.literal(textRenderer.plainSubstrByWidth(text.getString(), maxWidth)), x, y, color, false);
	}

	private void renderScrollbar(GuiGraphicsExtractor context) {
		renderMiniScrollbar(context, contentX + contentWidth - 7, contentY + 4, contentHeight - 8, (int) Math.round(scrollOffset), maxScroll);
	}

	private void renderMiniScrollbar(GuiGraphicsExtractor context, int x, int y, int height, int scroll, int max) {
		if (max <= 0) {
			return;
		}
		double visibleRatio = height / (double) (height + max);
		int thumbHeight = Math.max(22, (int) Math.round(height * visibleRatio));
		int range = Math.max(0, height - thumbHeight);
		int thumbY = y + (int) Math.round(range * (scroll / (double) max));
		drawVerticalCapsule(context, x, thumbY, x + 4, thumbY + thumbHeight, HubPanelTheme.ACCENT);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		if (colorPicker != null) {
			if (colorPicker.contains(click.x(), click.y(), width, height)) {
				colorPicker.handleMouseButtonEvent(click.x(), click.y(), width, height);
				return true;
			}
			closeColorPicker();
			return true;
		}

		if (contains(click.x(), click.y(), doneX, doneY, doneWidth, doneHeight)) {
			onClose();
			return true;
		}

		searchFocused = contains(click.x(), click.y(), searchX, searchY, searchWidth, SEARCH_HEIGHT);
		if (searchFocused) {
			return true;
		}

		if (handleSidebarMouseButtonEvent(click.x(), click.y())) {
			return true;
		}

		if (handleFeatureMouseButtonEvent(click.x(), click.y())) {
			return true;
		}

		if (handleSettingMouseButtonEvent(click.x(), click.y())) {
			return true;
		}

		return super.mouseClicked(click, doubled);
	}

	private boolean handleSidebarMouseButtonEvent(double mouseX, double mouseY) {
		int y = sidebarY;
		for (FeatureGroup group : FeatureGroup.values()) {
			if (contains(mouseX, mouseY, sidebarX, y, sidebarWidth, HubPanelTheme.ROW_HEIGHT)) {
				selectedGroup = group;
				expandedFeature = null;
				collapsingFeature = null;
				suppressListBottomFade = false;
				suppressSettingsBottomFade = false;
				scrollOffset = 0.0;
				scrollOffsetTarget = 0.0;
				settingsScroll = 0.0;
				settingsScrollTarget = 0.0;
				updateScrollBounds();
				return true;
			}
			y += HubPanelTheme.ROW_HEIGHT + 8;
		}
		return false;
	}

	private boolean handleFeatureMouseButtonEvent(double mouseX, double mouseY) {
		if (!contains(mouseX, mouseY, contentX, contentY, contentWidth, contentHeight)) {
			return false;
		}
		int y = contentY - (int) Math.round(scrollOffset);
		for (FeatureSpec feature : visibleFeatures()) {
			int rowRight = contentX + contentWidth - scrollbarReserve();
			if (contains(mouseX, mouseY, contentX, y, rowRight - contentX, FEATURE_HEIGHT)) {
				boolean opened = false;
				if (feature.toggle() != null && contains(mouseX, mouseY, rowRight - TOGGLE_WIDTH - 16, y, TOGGLE_WIDTH + 16, FEATURE_HEIGHT)) {
					feature.toggle().setter().accept(!feature.toggle().getter().getAsBoolean());
				} else if (feature.primaryAction() != null) {
					int actionRight = rowRight - 10;
					if (feature.toggle() != null) {
						actionRight = rowRight - TOGGLE_WIDTH - 22;
					}
					int openX = actionRight - ACTION_BUTTON_WIDTH;
					if (contains(mouseX, mouseY, openX, y, ACTION_BUTTON_WIDTH, FEATURE_HEIGHT) && feature.primaryActionEnabled()) {
						feature.primaryAction().run();
						expandedFeature = null;
						collapsingFeature = null;
					} else if (canExpand(feature)) {
						opened = toggleExpanded(feature);
					} else if (feature.primaryActionEnabled()) {
						feature.primaryAction().run();
						expandedFeature = null;
						collapsingFeature = null;
					}
				} else if (!canExpand(feature)) {
					if (feature.toggle() != null) {
						feature.toggle().setter().accept(!feature.toggle().getter().getAsBoolean());
					}
					expandedFeature = null;
					collapsingFeature = null;
				} else {
					opened = toggleExpanded(feature);
				}
				updateScrollBounds();
				if (opened) {
					ensureExpandedFeatureVisible();
				}
				return true;
			}
			if (feature == expandedFeature || feature == collapsingFeature) {
				y += FEATURE_HEIGHT + EXPANDED_MENU_GAP;
				int menuHeight = feature == expandedFeature ? expandedMenuHeight(feature) : collapsingMenuHeight(feature);
				if (menuHeight > 0) {
					y += menuHeight + FEATURE_GAP;
				} else {
					y += FEATURE_GAP;
				}
			} else {
				y += FEATURE_HEIGHT + FEATURE_GAP;
			}
		}
		return false;
	}

	private boolean toggleExpanded(FeatureSpec feature) {
		boolean opened = expandedFeature != feature;
		if (opened) {
			collapsingFeature = null;
			expandedFeature = feature;
			expandProgress = 0.0F;
			suppressSettingsBottomFade = true;
		} else {
			collapsingFeature = feature;
			collapseProgress = Math.max(0.12F, expandProgress);
			expandedFeature = null;
			suppressSettingsBottomFade = false;
		}
		suppressListBottomFade = false;
		settingsScroll = 0.0;
		settingsScrollTarget = 0.0;
		return opened;
	}

	private boolean handleSettingMouseButtonEvent(double mouseX, double mouseY) {
		if (expandedFeature == null || !contains(mouseX, mouseY, contentX, contentY, contentWidth, contentHeight)) {
			return false;
		}

		MenuBounds bounds = expandedMenuBounds();
		if (bounds == null || !contains(mouseX, mouseY, bounds.x(), bounds.y(), bounds.width(), bounds.height())) {
			return false;
		}

		int innerX = bounds.x() + MENU_PADDING + SETTINGS_SCROLLBAR_LANE_WIDTH;
		int innerY = bounds.y() + MENU_PADDING;
		int innerW = bounds.width() - MENU_PADDING * 2 - SETTINGS_SCROLLBAR_LANE_WIDTH;
		int labelWidth = settingLabelWidth(innerW);
		int controlX = innerX + labelWidth + 18;
		int controlRight = innerX + innerW;
		int controlWidth = Math.max(80, controlRight - controlX);
		int y = innerY - (int) Math.round(settingsScroll);
		for (HubSettingRow row : rows(expandedFeature)) {
			int rowHeight = settingRowHeight(row);
			if (mouseY >= y && mouseY < y + rowHeight) {
				if (row instanceof HubSettingRow.Toggle toggle) {
					toggle.setter().accept(!toggle.getter().getAsBoolean());
					return true;
				}
				if (row instanceof HubSettingRow.Slider slider) {
					int trackX = controlX;
					int trackW = Math.max(120, controlWidth - VALUE_COLUMN_WIDTH - 18);
					applySlider(slider, trackX, trackW, mouseX);
					draggingSlider = slider;
					draggingSliderX = trackX;
					draggingSliderWidth = trackW;
					return true;
				}
				if (row instanceof HubSettingRow.Cycle<?> cycle) {
					applyCycle(cycle);
					return true;
				}
				if (row instanceof HubSettingRow.Rgb rgb) {
					openColorPicker(rgb, controlRight - HubLayout.COLOR_SWATCH_SIZE, y + SETTING_ROW_HEIGHT / 2);
					return true;
				}
				if (row instanceof HubSettingRow.Action action && action.enabled()) {
					action.action().run();
					return true;
				}
			}
			y += rowHeight;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
		if (colorPicker != null && colorPicker.handleDrag(click.x(), click.y(), width, height)) {
			return true;
		}
		if (draggingSlider != null) {
			applySlider(draggingSlider, draggingSliderX, draggingSliderWidth, click.x());
			return true;
		}
		return super.mouseDragged(click, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		draggingSlider = null;
		if (colorPicker != null) {
			colorPicker.release();
			return true;
		}
		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (colorPicker != null) {
			return true;
		}
		MenuBounds menu = expandedMenuBounds();
		if (menu != null && settingsMaxScroll > 0 && contains(mouseX, mouseY, menu.x(), menu.y(), menu.width(), menu.height())) {
			suppressSettingsBottomFade = false;
			settingsScrollTarget = Mth.clamp(settingsScrollTarget - verticalAmount * 18.0, 0.0, settingsMaxScroll);
			return true;
		}
		if (maxScroll > 0 && contains(mouseX, mouseY, contentX, contentY, contentWidth, contentHeight)) {
			suppressListBottomFade = false;
			suppressSettingsBottomFade = false;
			scrollOffsetTarget = Mth.clamp(scrollOffsetTarget - verticalAmount * 22.0, 0.0, maxScroll);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean keyPressed(KeyEvent input) {
		if (!searchFocused) {
			if ((input.hasControlDown() || hasSuperDown(input)) && input.key() == InputConstants.KEY_F) {
				searchFocused = true;
				return true;
			}
			return super.keyPressed(input);
		}
		if (input.isEscape()) {
			searchFocused = false;
			search = "";
			scrollOffset = 0.0;
			scrollOffsetTarget = 0.0;
			suppressListBottomFade = false;
			suppressSettingsBottomFade = false;
			updateScrollBounds();
			return true;
		}
		if (input.isPaste()) {
			search = clampSearch(search + minecraft.keyboardHandler.getClipboard());
			scrollOffset = 0.0;
			scrollOffsetTarget = 0.0;
			suppressListBottomFade = false;
			suppressSettingsBottomFade = false;
			updateScrollBounds();
			return true;
		}
		if (input.key() == InputConstants.KEY_BACKSPACE) {
			if (!search.isEmpty()) {
				search = search.substring(0, search.length() - 1);
				scrollOffset = 0.0;
				scrollOffsetTarget = 0.0;
				suppressListBottomFade = false;
				suppressSettingsBottomFade = false;
				updateScrollBounds();
			}
			return true;
		}
		if (input.key() == InputConstants.KEY_DELETE) {
			search = "";
			scrollOffset = 0.0;
			scrollOffsetTarget = 0.0;
			suppressListBottomFade = false;
			suppressSettingsBottomFade = false;
			updateScrollBounds();
			return true;
		}
		return super.keyPressed(input);
	}

	@Override
	public boolean charTyped(CharacterEvent input) {
		if (!searchFocused || !input.isAllowedChatCharacter() || input.codepoint() == '\t') {
			return super.charTyped(input);
		}
		search = clampSearch(search + input.codepointAsString());
		scrollOffset = 0.0;
		scrollOffsetTarget = 0.0;
		suppressListBottomFade = false;
		suppressSettingsBottomFade = false;
		updateScrollBounds();
		return true;
	}

	private static boolean hasSuperDown(KeyEvent input) {
		return (input.modifiers() & InputConstants.MOD_SUPER) != 0;
	}

	@Override
	public void onClose() {
		closeColorPicker();
		HubRoundedGraphics.clearCache();
		HubColorPickerGraphics.clearHueWheel(minecraft);
		minecraft.setScreenAndShow(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void updateScrollBounds() {
		if (expandedFeature != null && (!canExpand(expandedFeature) || !visibleFeatures().contains(expandedFeature))) {
			expandedFeature = null;
		}
		if (collapsingFeature != null && (!canExpand(collapsingFeature) || !visibleFeatures().contains(collapsingFeature))) {
			collapsingFeature = null;
		}
		maxScroll = Math.max(0, listContentHeight() - contentHeight);
		scrollOffsetTarget = Mth.clamp(scrollOffsetTarget, 0.0, maxScroll);
		scrollOffset = Mth.clamp(scrollOffset, 0.0, maxScroll);
		settingsMaxScroll = expandedFeature == null
			? 0
			: Math.max(0, settingsContentHeight(expandedFeature) - (expandedMenuHeight(expandedFeature) - MENU_PADDING * 2));
		settingsScrollTarget = Mth.clamp(settingsScrollTarget, 0.0, settingsMaxScroll);
		settingsScroll = Mth.clamp(settingsScroll, 0.0, settingsMaxScroll);
	}

	private int listContentHeight() {
		int height = 0;
		for (FeatureSpec feature : visibleFeatures()) {
			if (feature == expandedFeature) {
				height += FEATURE_HEIGHT + EXPANDED_MENU_GAP + expandedMenuHeight(feature) + FEATURE_GAP;
			} else if (feature == collapsingFeature) {
				height += FEATURE_HEIGHT + EXPANDED_MENU_GAP + collapsingMenuHeight(feature) + FEATURE_GAP;
			} else {
				height += FEATURE_HEIGHT + FEATURE_GAP;
			}
		}
		return Math.max(0, height - FEATURE_GAP);
	}

	private int expandedMenuHeight(FeatureSpec feature) {
		if (!canExpand(feature)) {
			return 0;
		}
		return Math.min(MENU_MAX_HEIGHT, settingsContentHeight(feature) + MENU_PADDING * 2);
	}

	private int animatedExpandedMenuHeight(FeatureSpec feature) {
		int height = expandedMenuHeight(feature);
		if (feature != expandedFeature) {
			return height;
		}
		return menuHeightForProgress(height, expandProgress);
	}

	private int collapsingMenuHeight(FeatureSpec feature) {
		return menuHeightForProgress(expandedMenuHeight(feature), collapseProgress);
	}

	private int animatedMenuHeight(FeatureSpec feature) {
		if (feature == expandedFeature) {
			return animatedExpandedMenuHeight(feature);
		}
		if (feature == collapsingFeature) {
			return collapsingMenuHeight(feature);
		}
		return 0;
	}

	private boolean isFeatureVisuallyExpanded(FeatureSpec feature) {
		return feature == expandedFeature || feature == collapsingFeature;
	}

	private static int menuHeightForProgress(int height, float progressValue) {
		if (height <= 0 || progressValue <= 0.0F) {
			return 0;
		}
		float clamped = Mth.clamp(progressValue, 0.0F, 1.0F);
		float progress = clamped * clamped * (3.0F - 2.0F * clamped);
		return Math.max(1, Math.round(height * progress));
	}

	private int settingsContentHeight(FeatureSpec feature) {
		int height = 0;
		for (HubSettingRow row : rows(feature)) {
			height += settingRowHeight(row);
		}
		return height;
	}

	private int settingRowHeight(HubSettingRow row) {
		if (row instanceof HubSettingRow.Spacer spacer) {
			return spacer.height();
		}
		if (row instanceof HubSettingRow.Divider divider) {
			return divider.height();
		}
		return SETTING_ROW_HEIGHT;
	}

	private MenuBounds expandedMenuBounds() {
		if (expandedFeature == null) {
			return null;
		}
		int y = contentY - (int) Math.round(scrollOffset);
		for (FeatureSpec feature : visibleFeatures()) {
			y += FEATURE_HEIGHT;
			if (feature == expandedFeature) {
				y += EXPANDED_MENU_GAP;
				int height = expandedMenuHeight(feature);
				int right = contentX + contentWidth - scrollbarReserve();
				return new MenuBounds(contentX + 18, y, right - contentX - 36, height);
			}
			if (feature == collapsingFeature) {
				y += EXPANDED_MENU_GAP + collapsingMenuHeight(feature) + FEATURE_GAP;
			} else {
				y += FEATURE_GAP;
			}
		}
		return null;
	}

	private List<FeatureSpec> visibleFeatures() {
		String query = normalized(search);
		List<FeatureSpec> visible = new ArrayList<>();
		for (FeatureSpec feature : features) {
			if (query.isEmpty() && feature.group() != selectedGroup) {
				continue;
			}
			if (!query.isEmpty() && !feature.matches(query)) {
				continue;
			}
			visible.add(feature);
		}
		return visible;
	}

	private void ensureExpandedFeatureVisible() {
		if (expandedFeature == null) {
			return;
		}
		int y = contentY - (int) Math.round(scrollOffsetTarget);
		for (FeatureSpec feature : visibleFeatures()) {
			int rowTop = y;
			int menuTop = rowTop + FEATURE_HEIGHT + EXPANDED_MENU_GAP;
			if (feature == expandedFeature) {
				int menuBottom = menuTop + expandedMenuHeight(feature);
				int viewBottom = contentY + contentHeight;
				boolean movedToShowMenu = false;
				if (menuBottom > viewBottom) {
					scrollOffsetTarget += menuBottom - viewBottom;
					movedToShowMenu = true;
				}
				if (rowTop < contentY) {
					scrollOffsetTarget -= contentY - rowTop;
					movedToShowMenu = true;
				}
				scrollOffsetTarget = Mth.clamp(scrollOffsetTarget, 0.0, maxScroll);
				suppressListBottomFade = movedToShowMenu;
				return;
			}
			if (feature == expandedFeature) {
				y += FEATURE_HEIGHT + EXPANDED_MENU_GAP + expandedMenuHeight(feature) + FEATURE_GAP;
			} else if (feature == collapsingFeature) {
				y += FEATURE_HEIGHT + EXPANDED_MENU_GAP + collapsingMenuHeight(feature) + FEATURE_GAP;
			} else {
				y += FEATURE_HEIGHT + FEATURE_GAP;
			}
		}
	}

	private List<HubSettingRow> rows(FeatureSpec feature) {
		if (feature.rows() != null) {
			return feature.rows();
		}
		if (feature.category() == null) {
			return List.of();
		}
		return HubSettingsRegistry.rows(feature.category(), this::updateScrollBounds);
	}

	private boolean canExpand(FeatureSpec feature) {
		return feature.category() != null && !rows(feature).isEmpty();
	}

	private static int settingLabelWidth(int width) {
		return Mth.clamp(width / 2 - 12, 150, 220);
	}

	private int scrollbarReserve() {
		return SCROLLBAR_LANE_WIDTH;
	}

	private boolean intersectsContent(int y, int height) {
		return y + height >= contentY && y <= contentY + contentHeight;
	}

	private void applySlider(HubSettingRow.Slider slider, int x, int width, double mouseX) {
		double ratio = Mth.clamp((mouseX - x) / width, 0.0, 1.0);
		slider.setter().accept(slider.min() + (int) Math.round(ratio * (slider.max() - slider.min())));
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void applyCycle(HubSettingRow.Cycle<?> cycle) {
		HubSettingRow.Cycle raw = cycle;
		raw.setter().accept(raw.next().get());
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

	private Component settingLabel(HubSettingRow row) {
		if (row instanceof HubSettingRow.Toggle toggle) {
			return Component.translatable(toggle.labelKey());
		}
		if (row instanceof HubSettingRow.Slider slider) {
			return Component.translatable(slider.labelKey());
		}
		if (row instanceof HubSettingRow.Cycle<?> cycle) {
			return Component.translatable(cycle.labelKey());
		}
		if (row instanceof HubSettingRow.Rgb rgb) {
			return Component.translatable(rgb.labelKey());
		}
		return Component.empty();
	}

	private Component sliderValue(HubSettingRow.Slider slider) {
		Component suffix = slider.suffixKey() == null || slider.suffixKey().isEmpty()
			? Component.empty()
			: Component.translatable(slider.suffixKey());
		return Component.literal(String.valueOf(slider.getter().getAsInt())).append(suffix);
	}

	private List<FeatureSpec> createFeatures() {
		EMUtilsConfig config = EMUtilsClient.config();
		return List.of(
			feature(HubCategory.FULLBRIGHT, FeatureGroup.RENDER, EMUtilsTexts.OPTION_TWEAK_FULLBRIGHT, EMUtilsTexts.HUB_FEATURE_FULLBRIGHT_DESC, IconKind.SUN, toggle(config::tweakFullbright, config::setTweakFullbright)),
			feature(HubCategory.CLEAR_WEATHER, FeatureGroup.RENDER, EMUtilsTexts.OPTION_TWEAK_CLEAR_WEATHER, EMUtilsTexts.HUB_FEATURE_CLEAR_WEATHER_DESC, IconKind.CLOUD_SUN, toggle(config::tweakClearWeather, config::setTweakClearWeather)),
			leaf(FeatureGroup.RENDER, EMUtilsTexts.OPTION_TWEAK_NO_FOG, EMUtilsTexts.HUB_FEATURE_NO_FOG_DESC, IconKind.CLOUD_OFF, toggle(config::tweakNoFog, config::setTweakNoFog)),
			leaf(FeatureGroup.RENDER, EMUtilsTexts.OPTION_TWEAK_CLEAR_UNDERWATER, EMUtilsTexts.HUB_FEATURE_CLEAR_UNDERWATER_DESC, IconKind.DROPLETS, toggle(config::tweakClearUnderwater, config::setTweakClearUnderwater)),
			leaf(FeatureGroup.RENDER, EMUtilsTexts.OPTION_TWEAK_CLEAR_LAVA, EMUtilsTexts.HUB_FEATURE_CLEAR_LAVA_DESC, IconKind.FLAME, toggle(config::tweakClearLava, config::setTweakClearLava)),
			leaf(FeatureGroup.RENDER, EMUtilsTexts.OPTION_TWEAK_NO_FIRE_OVERLAY, EMUtilsTexts.HUB_FEATURE_NO_FIRE_OVERLAY_DESC, IconKind.FLAME, toggle(config::tweakNoFireOverlay, config::setTweakNoFireOverlay)),
			leaf(FeatureGroup.RENDER, EMUtilsTexts.OPTION_TWEAK_LOW_FIRE_OVERLAY, EMUtilsTexts.HUB_FEATURE_LOW_FIRE_OVERLAY_DESC, IconKind.FLAME, toggle(config::tweakLowFireOverlay, config::setTweakLowFireOverlay)),
			leaf(FeatureGroup.RENDER, EMUtilsTexts.OPTION_TWEAK_NO_NAUSEA, EMUtilsTexts.HUB_FEATURE_NO_NAUSEA_DESC, IconKind.EYE, toggle(config::tweakNoNausea, config::setTweakNoNausea)),
			leaf(FeatureGroup.RENDER, EMUtilsTexts.OPTION_TWEAK_NO_SPYGLASS_OVERLAY, EMUtilsTexts.HUB_FEATURE_NO_SPYGLASS_OVERLAY_DESC, IconKind.ZOOM, toggle(config::tweakNoSpyglassOverlay, config::setTweakNoSpyglassOverlay)),
			leaf(FeatureGroup.RENDER, EMUtilsTexts.OPTION_TWEAK_NO_ENVIRONMENT_FOG, EMUtilsTexts.HUB_FEATURE_NO_ENVIRONMENT_FOG_DESC, IconKind.CLOUD_OFF, toggle(config::tweakNoEnvironmentFog, config::setTweakNoEnvironmentFog)),
			leaf(FeatureGroup.RENDER, EMUtilsTexts.OPTION_TWEAK_NO_NETHER_PARTICLES, EMUtilsTexts.HUB_FEATURE_NO_NETHER_PARTICLES_DESC, IconKind.SPARKLES, toggle(config::tweakNoNetherParticles, config::setTweakNoNetherParticles)),
			leaf(FeatureGroup.RENDER, EMUtilsTexts.OPTION_TWEAK_NO_HURT_CAM, EMUtilsTexts.HUB_FEATURE_NO_HURT_CAM_DESC, IconKind.SHIELD, toggle(config::tweakNoHurtCam, config::setTweakNoHurtCam)),
			leaf(FeatureGroup.RENDER, EMUtilsTexts.OPTION_TWEAK_FREELOOK, EMUtilsTexts.HUB_FEATURE_FREELOOK_DESC, IconKind.EYE, toggle(config::tweakFreelook, config::setTweakFreelook)),
			leaf(FeatureGroup.RENDER, EMUtilsTexts.OPTION_TWEAK_OWN_NAMETAG, EMUtilsTexts.HUB_FEATURE_OWN_NAMETAG_DESC, IconKind.TAG, toggle(config::tweakOwnNametag, config::setTweakOwnNametag)),
			leaf(FeatureGroup.RENDER, EMUtilsTexts.OPTION_TWEAK_SHULKER_TOOLTIP_PREVIEW, EMUtilsTexts.HUB_FEATURE_SHULKER_PREVIEW_DESC, IconKind.BOX, toggle(config::tweakShulkerTooltipPreview, config::setTweakShulkerTooltipPreview)),
			leaf(FeatureGroup.RENDER, EMUtilsTexts.OPTION_TWEAK_BUNDLE_TOOLTIP_PREVIEW, EMUtilsTexts.HUB_FEATURE_BUNDLE_PREVIEW_DESC, IconKind.PACKAGE_OPEN, toggle(config::tweakBundleTooltipPreview, config::setTweakBundleTooltipPreview)),
			feature(HubCategory.ZOOM, FeatureGroup.RENDER, EMUtilsTexts.HUB_ZOOM, EMUtilsTexts.HUB_FEATURE_ZOOM_DESC, IconKind.ZOOM, toggle(config::zoomEnabled, config::setZoomEnabled)),
			feature(HubCategory.CAPES, FeatureGroup.RENDER, EMUtilsTexts.HUB_CAPES, EMUtilsTexts.HUB_FEATURE_CAPES_DESC, IconKind.CAPE, toggle(config::customCapes, config::setCustomCapes)),
			feature(HubCategory.HUD_OVERLAY, FeatureGroup.HUD, EMUtilsTexts.HUB_HUD_OVERLAY, EMUtilsTexts.HUB_FEATURE_HUD_DESC, IconKind.HUD, toggle(config::hudOverlay, config::setHudOverlay)),
			feature(HubCategory.FOOD_HUD, FeatureGroup.HUD, EMUtilsTexts.HUB_FOOD_HUD, EMUtilsTexts.HUB_FEATURE_FOOD_HUD_DESC, IconKind.APPLE, toggle(config::foodHud, config::setFoodHud)),
			feature(HubCategory.SPOTIFY, FeatureGroup.HUD, EMUtilsTexts.HUB_SPOTIFY_PLAYER, EMUtilsTexts.HUB_FEATURE_SPOTIFY_DESC, IconKind.MUSIC, toggle(config::spotifyPlayerEnabled, config::setSpotifyPlayerEnabled)),
			feature(HubCategory.AUTO_RECONNECT, FeatureGroup.UTILITY, EMUtilsTexts.HUB_AUTO_RECONNECT, EMUtilsTexts.HUB_FEATURE_AUTO_RECONNECT_DESC, IconKind.RECONNECT, toggle(config::autoReconnect, config::setAutoReconnect)),
			feature(HubCategory.SCREENSHOT, FeatureGroup.UTILITY, EMUtilsTexts.HUB_SCREENSHOT_HELPER, EMUtilsTexts.HUB_FEATURE_SCREENSHOT_DESC, IconKind.IMAGE, toggle(config::screenshotHelper, config::setScreenshotHelper)),
			feature(HubCategory.DEATH_WAYPOINTS, FeatureGroup.UTILITY, EMUtilsTexts.HUB_WAYPOINTS, EMUtilsTexts.HUB_FEATURE_WAYPOINTS_DESC, IconKind.PIN, toggle(config::waypointEnabled, config::setWaypointEnabled)),
			actionFeature(
				HubCategory.SCREENSHOT_GALLERY,
				FeatureGroup.MANAGEMENT,
				EMUtilsTexts.SCREEN_SCREENSHOT_GALLERY,
				EMUtilsTexts.HUB_FEATURE_SCREENSHOT_GALLERY_DESC,
				IconKind.IMAGE,
				openScreenAction(ScreenshotGalleryScreen::new),
				true
			),
			actionFeature(
				FeatureGroup.MANAGEMENT,
				EMUtilsTexts.SCREEN_CURRENT_WAYPOINTS,
				EMUtilsTexts.HUB_FEATURE_CURRENT_WAYPOINTS_DESC,
				IconKind.PIN,
				openScreenAction(WaypointListScreen::new),
				true
			),
			actionFeature(
				FeatureGroup.MANAGEMENT,
				EMUtilsTexts.OPTION_PACK_MANAGER,
				EMUtilsTexts.HUB_FEATURE_PACK_MANAGER_DESC,
				IconKind.PACKAGE,
				toggle(config::packManagerEnabled, config::setPackManagerEnabled),
				openScreenAction(PackManagerScreen::new),
				true
			),
			actionFeature(
				FeatureGroup.MANAGEMENT,
				EMUtilsTexts.SCREEN_SCRIPT_MANAGER,
				EMUtilsTexts.HUB_FEATURE_SCRIPT_MANAGER_DESC,
				IconKind.SCRIPT,
				openScreenAction(ScriptManagerScreen::new),
				MinescriptCompat.isLoaded()
			),
			actionFeature(
				FeatureGroup.MANAGEMENT,
				EMUtilsTexts.OPTION_COMMAND_SHORTCUTS,
				EMUtilsTexts.HUB_FEATURE_COMMAND_SHORTCUTS_DESC,
				IconKind.TOOL,
				toggle(config::commandShortcutsEnabled, config::setCommandShortcutsEnabled),
				openScreenAction(CommandShortcutListScreen::new),
				true
			),
			feature(HubCategory.CHAT, FeatureGroup.QOL, EMUtilsTexts.HUB_CHAT_FEATURES, EMUtilsTexts.HUB_FEATURE_CHAT_DESC, IconKind.CHAT, toggle(config::copyChat, config::setCopyChat)),
			feature(HubCategory.INVENTORY, FeatureGroup.QOL, EMUtilsTexts.HUB_INVENTORY_TOOLS, EMUtilsTexts.HUB_FEATURE_INVENTORY_DESC, IconKind.BAG, toggle(config::inventoryToolsEnabled, config::setInventoryToolsEnabled)),
			feature(HubCategory.AUTO_TOOL, FeatureGroup.QOL, EMUtilsTexts.OPTION_AUTO_TOOL, EMUtilsTexts.HUB_FEATURE_AUTO_TOOL_DESC, IconKind.TOOL, toggle(config::autoToolEnabled, config::setAutoToolEnabled)),
			feature(HubCategory.AUTO_FLIGHT, FeatureGroup.QOL, EMUtilsTexts.OPTION_AUTO_FLIGHT_GEAR, EMUtilsTexts.HUB_FEATURE_AUTO_FLIGHT_DESC, IconKind.CAPE, toggle(config::autoFlightGearEnabled, config::setAutoFlightGearEnabled)),
			leaf(FeatureGroup.QOL, EMUtilsTexts.OPTION_TWEAK_FAST_PLACE, EMUtilsTexts.HUB_FEATURE_FAST_PLACE_DESC, IconKind.MOUSE_CLICK, toggle(config::tweakFastPlace, config::setTweakFastPlace)),
			leaf(FeatureGroup.QOL, EMUtilsTexts.OPTION_TWEAK_ANTI_DURABILITY_BREAK, EMUtilsTexts.HUB_FEATURE_ANTI_DURABILITY_BREAK_DESC, IconKind.SHIELD, toggle(config::tweakAntiDurabilityBreak, config::setTweakAntiDurabilityBreak)),
			leaf(FeatureGroup.QOL, EMUtilsTexts.OPTION_TWEAK_SAFE_WALK, EMUtilsTexts.HUB_FEATURE_SAFE_WALK_DESC, IconKind.SHIELD, toggle(config::tweakSafeWalk, config::setTweakSafeWalk))
		);
	}

	private static void drawIcon(GuiGraphicsExtractor context, Identifier texture, int x, int y, int size) {
		context.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, size, size, 32, 32, 32, 32);
	}

	private static void drawCapsule(GuiGraphicsExtractor context, int left, int top, int right, int bottom, int color) {
		if (right <= left || bottom <= top) {
			return;
		}
		int height = bottom - top;
		if (right - left <= height) {
			HubRoundedGraphics.drawCircle(context, (left + right) / 2, (top + bottom) / 2, Math.min(height, right - left), color);
			return;
		}
		int radius = height / 2;
		context.fill(left + radius, top, right - radius, bottom, color);
		HubRoundedGraphics.drawCircle(context, left + radius, top + radius, height, color);
		HubRoundedGraphics.drawCircle(context, right - radius, top + radius, height, color);
	}

	private static void drawVerticalCapsule(GuiGraphicsExtractor context, int left, int top, int right, int bottom, int color) {
		if (right <= left || bottom <= top) {
			return;
		}
		int width = right - left;
		if (bottom - top <= width) {
			HubRoundedGraphics.drawCircle(context, (left + right) / 2, (top + bottom) / 2, Math.min(width, bottom - top), color);
			return;
		}
		int radius = width / 2;
		context.fill(left, top + radius, right, bottom - radius, color);
		HubRoundedGraphics.drawCircle(context, left + radius, top + radius, width, color);
		HubRoundedGraphics.drawCircle(context, left + radius, bottom - radius, width, color);
	}

	private static void drawThinBorder(GuiGraphicsExtractor context, int left, int top, int right, int bottom, int color) {
		context.fill(left, top, right, top + 1, color);
		context.fill(left, bottom - 1, right, bottom, color);
		context.fill(left, top, left + 1, bottom, color);
		context.fill(right - 1, top, right, bottom, color);
	}

	private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private static String cleanTitle(Component title) {
		return title.getString().replace("...", "").trim();
	}

	private static String normalized(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
	}

	private static String clampSearch(String value) {
		String safe = value == null ? "" : value.replace('\n', ' ').replace('\r', ' ');
		return safe.length() > 48 ? safe.substring(0, 48) : safe;
	}

	private static FeatureSpec feature(
		@Nullable HubCategory category,
		FeatureGroup group,
		String titleKey,
		String descriptionKey,
		IconKind icon,
		@Nullable ToggleBinding toggle
	) {
		return new FeatureSpec(category, group, titleKey, descriptionKey, icon, toggle, null, null, true);
	}

	private static FeatureSpec feature(
		FeatureGroup group,
		String titleKey,
		String descriptionKey,
		IconKind icon,
		@Nullable ToggleBinding toggle,
		List<HubSettingRow> rows
	) {
		return new FeatureSpec(null, group, titleKey, descriptionKey, icon, toggle, rows, null, true);
	}

	private static FeatureSpec actionFeature(
		@Nullable HubCategory category,
		FeatureGroup group,
		String titleKey,
		String descriptionKey,
		IconKind icon,
		Runnable action,
		boolean actionEnabled
	) {
		return new FeatureSpec(category, group, titleKey, descriptionKey, icon, null, null, action, actionEnabled);
	}

	private static FeatureSpec actionFeature(
		FeatureGroup group,
		String titleKey,
		String descriptionKey,
		IconKind icon,
		Runnable action,
		boolean actionEnabled
	) {
		return new FeatureSpec(null, group, titleKey, descriptionKey, icon, null, List.of(), action, actionEnabled);
	}

	private static FeatureSpec actionFeature(
		FeatureGroup group,
		String titleKey,
		String descriptionKey,
		IconKind icon,
		ToggleBinding toggle,
		Runnable action,
		boolean actionEnabled
	) {
		return new FeatureSpec(null, group, titleKey, descriptionKey, icon, toggle, List.of(), action, actionEnabled);
	}

	private static FeatureSpec leaf(
		FeatureGroup group,
		String titleKey,
		String descriptionKey,
		IconKind icon,
		ToggleBinding toggle
	) {
		return feature(null, group, titleKey, descriptionKey, icon, toggle);
	}

	private HubSettingRow.Action openAction(String labelKey, Function<Screen, Screen> screenFactory, boolean enabled) {
		return new HubSettingRow.Action(Component.translatable(labelKey), openScreenAction(screenFactory), enabled);
	}

	private Runnable openScreenAction(Function<Screen, Screen> screenFactory) {
		return () -> {
			minecraft.setScreenAndShow(screenFactory.apply(this));
		};
	}

	private static ToggleBinding toggle(BooleanSupplier getter, Consumer<Boolean> setter) {
		return new ToggleBinding(getter, setter);
	}

	private enum FeatureGroup {
		RENDER(EMUtilsTexts.HUB_GROUP_RENDER, HubIcons.SUN),
		HUD(EMUtilsTexts.HUB_GROUP_HUD, HubIcons.MONITOR),
		UTILITY(EMUtilsTexts.HUB_GROUP_UTILITY, HubIcons.WRENCH),
		MANAGEMENT(EMUtilsTexts.HUB_GROUP_MANAGEMENT, HubIcons.FOLDER_COG),
		QOL(EMUtilsTexts.HUB_GROUP_QOL, HubIcons.SPARKLES);

		private final String labelKey;
		private final Identifier icon;

		FeatureGroup(String labelKey, Identifier icon) {
			this.labelKey = labelKey;
			this.icon = icon;
		}

		private String labelKey() {
			return labelKey;
		}

		private Identifier icon() {
			return icon;
		}
	}

	private enum IconKind {
		CHAT(HubIcons.MESSAGE_SQUARE),
		PIN(HubIcons.MAP_PIN),
		RECONNECT(HubIcons.REFRESH_CW),
		IMAGE(HubIcons.IMAGE),
		TOOL(HubIcons.FOLDER_COG),
		HUD(HubIcons.MONITOR),
		MOUSE_CLICK(HubIcons.MOUSE_POINTER_CLICK),
		APPLE(HubIcons.APPLE),
		ZOOM(HubIcons.ZOOM_IN),
		CAPE(HubIcons.SHIRT),
		BAG(HubIcons.BACKPACK),
		MUSIC(HubIcons.MUSIC),
		SUN(HubIcons.SUN),
		CLOUD_SUN(HubIcons.CLOUD_SUN),
		CLOUD_OFF(HubIcons.CLOUD_OFF),
		DROPLETS(HubIcons.DROPLETS),
		FLAME(HubIcons.FLAME),
		SHIELD(HubIcons.SHIELD),
		EYE(HubIcons.EYE),
		TAG(HubIcons.TAG),
		BOX(HubIcons.BOX),
		PACKAGE(HubIcons.PACKAGE),
		PACKAGE_OPEN(HubIcons.PACKAGE_OPEN),
		SPARKLES(HubIcons.SPARKLES),
		SCRIPT(HubIcons.PACKAGE_OPEN);

		private final Identifier texture;

		IconKind(Identifier texture) {
			this.texture = texture;
		}

		private Identifier texture() {
			return texture;
		}
	}

	private record ToggleBinding(BooleanSupplier getter, Consumer<Boolean> setter) {
	}

	private record MenuBounds(int x, int y, int width, int height) {
	}

	private record FeatureSpec(
		@Nullable HubCategory category,
		FeatureGroup group,
		String titleKey,
		String descriptionKey,
		IconKind icon,
		@Nullable ToggleBinding toggle,
		@Nullable List<HubSettingRow> rows,
		@Nullable Runnable primaryAction,
		boolean primaryActionEnabled
	) {
		private Component title() {
			return Component.translatable(titleKey);
		}

		private boolean matches(String query) {
			return normalized(Component.translatable(titleKey).getString()).contains(query)
				|| normalized(Component.translatable(descriptionKey).getString()).contains(query);
		}
	}
}
