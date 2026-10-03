package net.emutils.client.emutils.hud.editor;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.emutils.client.emutils.gui.ui.UiAnim;
import net.emutils.client.emutils.gui.ui.UiOpacity;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.gui.ui.UiWidgets;
import net.emutils.client.emutils.hud.HudOverlayPlacement;
import net.emutils.client.emutils.hud.layout.HudElementId;
import net.emutils.client.emutils.hud.layout.HudLayoutConfig;
import net.emutils.client.emutils.hud.layout.HudLayoutDraft;
import net.emutils.client.emutils.hud.layout.HudLayoutElement;
import net.emutils.client.emutils.hud.layout.HudLayoutManager;
import net.emutils.client.emutils.hud.layout.HudLayoutRegistry;
import net.emutils.client.emutils.hud.layout.HudLayoutSnapping;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.emutils.client.versioned.VersionedInput;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * The HUD Layout Editor (#136). The game stays visible behind the HUD elements, each
 * with a light outline; hovering or selecting one shows its name and a corner handle to resize it.
 * A selected element gets a card with Size and Opacity sliders. A toolbar at the top saves, cancels or
 * resets everything. Changes go to a draft layout in {@link HudLayoutManager} until they are saved.
 *
 * <p>A click picks the smallest element under the mouse, so a small element on top of or inside a big one (the
 * Tab List) can be reached, and clicking the selected element again moves on to the one underneath (#204). The
 * Show menu hides elements from the editor only, so what is under them can be reached; the HUD in the game and
 * the saved layout don't change.
 */
public final class HudEditorScreen extends Screen {
	private static final int TOOLBAR_HEIGHT = 32;
	private static final int TOOLBAR_MARGIN = 8;
	private static final int TOOLBAR_BUTTON = 20;
	private static final int HANDLE = 8;
	private static final int CARD_WIDTH = 196;
	private static final int CARD_PADDING = 12;
	private static final int RESET_HEIGHT = 18;
	private static final int SCALE_SLIDER_MIN = 25;
	private static final int SCALE_SLIDER_MAX = 300;
	private static final int MENU_ROW = 20;
	private static final int MENU_PADDING = 6;
	/** How far the mouse has to move for a press to count as a drag rather than a click. */
	private static final int CLICK_SLOP = 3;

	/** Elements hidden in the editor, kept while the game runs; the HUD itself never reads this. */
	private static final Set<HudElementId> HIDDEN = new HashSet<>();

	private enum Drag {
		NONE,
		MOVE,
		RESIZE,
		SCALE_SLIDER,
		OPACITY_SLIDER
	}

	private final @Nullable Screen parent;
	private final UiAnim anim = new UiAnim();
	private final Map<HudElementId, HudOverlayPlacement.PanelDimensions> dimensions = new LinkedHashMap<>();
	private final List<HudLayoutSnapping.GuideLine> guides = new ArrayList<>();
	private @Nullable HudElementId selected;
	private @Nullable HudElementId dragging;
	private Drag drag = Drag.NONE;
	/** Where in the element the mouse grabbed it, so it doesn't jump to the pointer's center. */
	private int grabX;
	private int grabY;
	private int toolbarX;
	private int toolbarY = TOOLBAR_MARGIN;
	private int toolbarWidth;
	private int showX;
	private int showWidth;
	private boolean menuOpen;
	private int menuX;
	private int menuY;
	private int menuWidth;
	private int menuHeight;
	/** Set when a press lands on the element that was already selected, so releasing without dragging moves on to the next one under it. */
	private boolean cycleOnRelease;
	private int pressX;
	private int pressY;
	private boolean pressMoved;
	private int resetX;
	private int resetWidth;
	private int cancelX;
	private int cancelWidth;
	private int saveX;
	private int saveWidth;
	private int cardX;
	private int cardY;
	private int cardHeight;
	private int scaleSliderY;
	private int opacitySliderY;
	private int sliderX;
	private int sliderWidth;
	private int cardResetX;
	private int cardResetY;
	private int cardResetWidth;

	public HudEditorScreen(@Nullable Screen parent) {
		super(Component.translatable(EMUtilsTexts.UI_HUD_EDITOR_TITLE));
		this.parent = parent;
	}

	@Override
	protected void init() {
		refreshDimensions();
		seedMissingDrafts();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// ---- layout data ----------------------------------------------------------------------------

	private void refreshDimensions() {
		dimensions.clear();
		HudLayoutConfig config = HudLayoutManager.editorConfig();
		if (config == null) {
			return;
		}
		for (HudLayoutElement element : HudLayoutManager.editorElements()) {
			dimensions.put(element.id(), HudLayoutManager.dimensions(element.id(), config, minecraft));
		}
	}

	/** Elements without a draft yet start where they are drawn now. */
	private void seedMissingDrafts() {
		HudLayoutConfig config = HudLayoutManager.editorConfig();
		if (config == null) {
			return;
		}
		for (HudLayoutElement element : HudLayoutManager.editorElements()) {
			HudElementId id = element.id();
			HudOverlayPlacement.PanelDimensions panel = dimensions.get(id);
			if (HudLayoutManager.draftLayouts().containsKey(id) || panel == null || panel.width() <= 0 || panel.height() <= 0) {
				continue;
			}
			HudOverlayPlacement.Position position = HudLayoutManager.resolve(id, config, width, height, panel, minecraft);
			HudLayoutManager.setDraftLayout(id, position.x(), position.y(), HudLayoutManager.layoutScale(id, config), HudLayoutManager.layoutOpacity(id, config));
		}
	}

	private @Nullable HudLayoutDraft draft(@Nullable HudElementId id) {
		return id == null ? null : HudLayoutManager.draftLayouts().get(id);
	}

	/** The elements under the mouse, smallest first (the one drawn on top first among equals); hidden ones don't count. */
	private List<HudElementId> elementsAt(double mouseX, double mouseY) {
		List<HudElementId> hits = new ArrayList<>();
		for (HudLayoutElement element : HudLayoutManager.editorElements()) {
			if (isHidden(element.id())) {
				continue;
			}
			HudLayoutDraft draft = draft(element.id());
			HudOverlayPlacement.PanelDimensions panel = dimensions.get(element.id());
			if (draft != null && panel != null && contains(mouseX, mouseY, draft.x() - 2, draft.y() - 2, panel.width() + 4, panel.height() + 4)) {
				hits.add(element.id());
			}
		}
		// The sort is stable, so reversing first leaves the element drawn last first among ones of the same size.
		Collections.reverse(hits);
		hits.sort(Comparator.comparingLong(id -> {
			HudOverlayPlacement.PanelDimensions panel = dimensions.get(id);
			return panel == null ? 0L : (long) panel.width() * panel.height();
		}));
		return hits;
	}

	/** The element a click at this spot picks: the smallest one there. */
	private @Nullable HudElementId elementAt(double mouseX, double mouseY) {
		List<HudElementId> hits = elementsAt(mouseX, mouseY);
		return hits.isEmpty() ? null : hits.getFirst();
	}

	private static boolean isHidden(HudElementId id) {
		return HIDDEN.contains(id);
	}

	/** How much of the elements the rectangle covers, in square GUI pixels. */
	private int coveredArea(int x, int y, int width, int height) {
		int area = 0;
		for (HudLayoutElement element : HudLayoutManager.editorElements()) {
			HudLayoutDraft draft = draft(element.id());
			HudOverlayPlacement.PanelDimensions panel = dimensions.get(element.id());
			if (draft == null || panel == null || isHidden(element.id())) {
				continue;
			}
			int overlapWidth = Math.min(x + width, draft.x() + panel.width()) - Math.max(x, draft.x());
			int overlapHeight = Math.min(y + height, draft.y() + panel.height()) - Math.max(y, draft.y());
			if (overlapWidth > 0 && overlapHeight > 0) {
				area += overlapWidth * overlapHeight;
			}
		}
		return area;
	}

	private boolean onResizeHandle(HudElementId id, double mouseX, double mouseY) {
		HudLayoutDraft draft = draft(id);
		HudOverlayPlacement.PanelDimensions panel = dimensions.get(id);
		if (draft == null || panel == null) {
			return false;
		}
		int handleX = draft.x() + panel.width() - HANDLE / 2;
		int handleY = draft.y() + panel.height() - HANDLE / 2;
		return contains(mouseX, mouseY, handleX - 3, handleY - 3, HANDLE + 6, HANDLE + 6);
	}

	private static String label(HudElementId id) {
		return Component.translatable(id.labelKey()).getString();
	}

	// ---- drawing --------------------------------------------------------------------------------

	@Override
	public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		// The game stays visible, lightly dimmed, so elements are placed against the real view.
		context.fill(0, 0, width, height, 0x38000000);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		anim.frame();
		UiTheme theme = UiTheme.current();
		extractBackground(context, mouseX, mouseY, delta);
		HudLayoutConfig config = HudLayoutManager.editorConfig();
		if (config == null) {
			return;
		}

		for (HudLayoutSnapping.GuideLine guide : guides) {
			int color = UiTheme.fade(theme.accent(), 0.7F);
			if (guide.axis() == HudLayoutSnapping.GuideAxis.VERTICAL) {
				context.fill(guide.coordinate(), 0, guide.coordinate() + 1, height, color);
			} else {
				context.fill(0, guide.coordinate(), width, guide.coordinate() + 1, color);
			}
		}

		HudElementId hovered = drag == Drag.NONE ? elementAt(mouseX, mouseY) : dragging;
		for (HudLayoutElement element : HudLayoutManager.editorElements()) {
			HudElementId id = element.id();
			HudLayoutDraft draft = draft(id);
			HudOverlayPlacement.PanelDimensions panel = dimensions.get(id);
			if (draft == null || panel == null || panel.width() <= 0 || panel.height() <= 0 || isHidden(id)) {
				continue;
			}
			element.renderPreview(context, draft.x(), draft.y(), config, minecraft, draft.scale());
			drawChrome(context, theme, id, draft, panel, hovered);
		}

		if (selected != null && drag != Drag.MOVE && drag != Drag.RESIZE) {
			drawCard(context, theme, mouseX, mouseY);
		}
		drawToolbar(context, theme, mouseX, mouseY);
		if (menuOpen) {
			drawMenu(context, theme, mouseX, mouseY);
		}
	}

	/** The outline around an element, its name while hovered or selected, and the resize handle. */
	private void drawChrome(GuiGraphicsExtractor context, UiTheme theme, HudElementId id, HudLayoutDraft draft, HudOverlayPlacement.PanelDimensions panel, @Nullable HudElementId hovered) {
		boolean isSelected = id.equals(selected);
		float focus = anim.towards("hud-editor:" + id.configKey(), isSelected || id.equals(hovered), 16.0F);
		int x = draft.x() - 2;
		int y = draft.y() - 2;
		int w = panel.width() + 4;
		int h = panel.height() + 4;
		int outline = isSelected ? theme.accent() : UiTheme.mix(0x66FFFFFF, 0xD0FFFFFF, focus);
		context.fill(x, y, x + w, y + 1, outline);
		context.fill(x, y + h - 1, x + w, y + h, outline);
		context.fill(x, y + 1, x + 1, y + h - 1, outline);
		context.fill(x + w - 1, y + 1, x + w, y + h - 1, outline);
		if (focus <= 0.01F) {
			return;
		}
		UiOpacity.set(focus);
		// The name sits above the element, or below it at the top of the screen.
		Component name = Component.literal(label(id));
		int chipWidth = UiText.width(font, name, UiText.Size.SMALL) + 10;
		int chipHeight = UiText.lineHeight(font, UiText.Size.SMALL) + 6;
		int chipY = y - chipHeight - 3 < 0 ? y + h + 3 : y - chipHeight - 3;
		UiShapes.roundedRect(context, x, chipY, chipWidth, chipHeight, 5, isSelected ? theme.accent() : theme.surface());
		UiText.drawCentered(context, font, name, UiText.Size.SMALL, x + 5, chipY + chipHeight / 2, isSelected ? theme.onAccent() : theme.text());
		int handleX = draft.x() + panel.width() - HANDLE / 2;
		int handleY = draft.y() + panel.height() - HANDLE / 2;
		UiShapes.circle(context, handleX - 1, handleY - 1, HANDLE + 2, 0xFFFFFFFF);
		UiShapes.circle(context, handleX, handleY, HANDLE, theme.accent());
		UiOpacity.reset();
	}

	private void drawToolbar(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		Component titleText = title;
		Component hint = Component.translatable(EMUtilsTexts.UI_HUD_EDITOR_HINT);
		Component resetLabel = Component.translatable(EMUtilsTexts.UI_HUD_EDITOR_RESET_ALL);
		Component showLabel = hiddenCount() == 0
			? Component.translatable(EMUtilsTexts.UI_HUD_EDITOR_SHOW)
			: Component.translatable(EMUtilsTexts.UI_HUD_EDITOR_SHOW_HIDDEN, hiddenCount());
		Component saveLabel = Component.translatable(EMUtilsTexts.UI_HUD_EDITOR_SAVE);
		// As wide as its longest label (every element hidden), so the toolbar doesn't change size, and jump, when something is hidden.
		Component longestShowLabel = Component.translatable(EMUtilsTexts.UI_HUD_EDITOR_SHOW_HIDDEN, HudLayoutManager.editorElements().size());
		showWidth = Math.max(UiWidgets.buttonWidth(font, showLabel), UiWidgets.buttonWidth(font, longestShowLabel)) + 4;
		resetWidth = UiWidgets.buttonWidth(font, resetLabel) + 4;
		cancelWidth = UiWidgets.buttonWidth(font, CommonComponents.GUI_CANCEL) + 4;
		saveWidth = Math.max(56, UiWidgets.buttonWidth(font, saveLabel) + 12);
		int titleWidth = UiText.width(font, titleText, UiText.Size.BOLD);
		int hintWidth = UiText.width(font, hint, UiText.Size.BODY);
		toolbarWidth = 12 + titleWidth + 12 + hintWidth + 16 + showWidth + 4 + resetWidth + 4 + cancelWidth + 6 + saveWidth + 6;
		toolbarX = (width - toolbarWidth) / 2;
		// At the top, unless it would cover more of the elements there, such as Look-At Info (#45), than at
		// the bottom. Only decided while nothing is dragged, so it doesn't jump away from under the mouse.
		if (drag == Drag.NONE) {
			int top = TOOLBAR_MARGIN;
			int bottom = height - TOOLBAR_MARGIN - TOOLBAR_HEIGHT;
			toolbarY = coveredArea(toolbarX, bottom, toolbarWidth, TOOLBAR_HEIGHT) < coveredArea(toolbarX, top, toolbarWidth, TOOLBAR_HEIGHT) ? bottom : top;
		}
		// Fades back while dragging, so it never hides what's being placed under it.
		float shown = anim.towards("hud-editor-toolbar", drag == Drag.NONE, 10.0F);
		UiOpacity.set(0.35F + 0.65F * shown);
		UiShapes.shadow(context, toolbarX, toolbarY, toolbarWidth, TOOLBAR_HEIGHT, 10, 10, theme.shadow());
		UiShapes.borderedRect(context, toolbarX, toolbarY, toolbarWidth, TOOLBAR_HEIGHT, 10, theme.surface(), theme.line());
		int center = toolbarY + TOOLBAR_HEIGHT / 2;
		int x = toolbarX + 12;
		UiText.drawCentered(context, font, titleText, UiText.Size.BOLD, x, center, theme.text());
		x += titleWidth + 12;
		UiText.drawCentered(context, font, hint, UiText.Size.BODY, x, center, theme.muted());
		int buttonY = center - TOOLBAR_BUTTON / 2;
		boolean interactive = drag == Drag.NONE;
		resetX = toolbarX + toolbarWidth - 6 - saveWidth - 6 - cancelWidth - 4 - resetWidth;
		showX = resetX - 4 - showWidth;
		cancelX = resetX + resetWidth + 4;
		saveX = cancelX + cancelWidth + 6;
		UiWidgets.button(context, font, theme, showX, buttonY, showWidth, TOOLBAR_BUTTON, showLabel, menuOpen ? UiWidgets.ButtonStyle.OUTLINE : UiWidgets.ButtonStyle.GHOST, interactive && (menuOpen || contains(mouseX, mouseY, showX, buttonY, showWidth, TOOLBAR_BUTTON)) ? 1.0F : 0.0F);
		UiWidgets.button(context, font, theme, resetX, buttonY, resetWidth, TOOLBAR_BUTTON, resetLabel, UiWidgets.ButtonStyle.GHOST, interactive && contains(mouseX, mouseY, resetX, buttonY, resetWidth, TOOLBAR_BUTTON) ? 1.0F : 0.0F);
		UiWidgets.button(context, font, theme, cancelX, buttonY, cancelWidth, TOOLBAR_BUTTON, CommonComponents.GUI_CANCEL, UiWidgets.ButtonStyle.GHOST, interactive && contains(mouseX, mouseY, cancelX, buttonY, cancelWidth, TOOLBAR_BUTTON) ? 1.0F : 0.0F);
		UiWidgets.button(context, font, theme, saveX, buttonY, saveWidth, TOOLBAR_BUTTON, saveLabel, UiWidgets.ButtonStyle.PRIMARY, interactive && contains(mouseX, mouseY, saveX, buttonY, saveWidth, TOOLBAR_BUTTON) ? 1.0F : 0.0F);
		UiOpacity.reset();
	}

	private int hiddenCount() {
		int count = 0;
		for (HudLayoutElement element : HudLayoutManager.editorElements()) {
			if (isHidden(element.id())) {
				count++;
			}
		}
		return count;
	}

	/** Where the Show menu goes: under the Show button, or above it when the toolbar is at the bottom. */
	private void layoutMenu() {
		int rows = HudLayoutManager.editorElements().size() + 1;
		int widest = 0;
		for (HudLayoutElement element : HudLayoutManager.editorElements()) {
			widest = Math.max(widest, UiText.width(font, Component.literal(label(element.id())), UiText.Size.BODY));
		}
		int allButtons = UiWidgets.buttonWidth(font, Component.translatable(EMUtilsTexts.UI_HUD_EDITOR_SHOW_ALL)) + 4 + UiWidgets.buttonWidth(font, Component.translatable(EMUtilsTexts.UI_HUD_EDITOR_HIDE_ALL));
		widest = Math.max(widest, allButtons - 12 - UiWidgets.SWITCH_WIDTH);
		menuWidth = MENU_PADDING * 2 + 6 + widest + 12 + UiWidgets.SWITCH_WIDTH + 6;
		menuHeight = MENU_PADDING * 2 + rows * MENU_ROW;
		menuX = Math.clamp(showX + showWidth - menuWidth, 6, Math.max(6, width - menuWidth - 6));
		boolean toolbarAtTop = toolbarY == TOOLBAR_MARGIN;
		menuY = toolbarAtTop ? toolbarY + TOOLBAR_HEIGHT + 6 : Math.max(6, toolbarY - 6 - menuHeight);
	}

	/** The Show menu: Show all and Hide all, then a switch for each element. */
	private void drawMenu(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		layoutMenu();
		UiShapes.shadow(context, menuX, menuY, menuWidth, menuHeight, 10, 10, theme.shadow());
		UiShapes.borderedRect(context, menuX, menuY, menuWidth, menuHeight, 10, theme.surface(), theme.line());
		int rowX = menuX + MENU_PADDING;
		int rowWidth = menuWidth - MENU_PADDING * 2;
		int y = menuY + MENU_PADDING;
		Component showAll = Component.translatable(EMUtilsTexts.UI_HUD_EDITOR_SHOW_ALL);
		Component hideAll = Component.translatable(EMUtilsTexts.UI_HUD_EDITOR_HIDE_ALL);
		int hideWidth = UiWidgets.buttonWidth(font, hideAll);
		int showAllWidth = UiWidgets.buttonWidth(font, showAll);
		int hideX = rowX + rowWidth - hideWidth;
		int showAllX = hideX - 4 - showAllWidth;
		UiWidgets.button(context, font, theme, showAllX, y + 1, showAllWidth, MENU_ROW - 2, showAll, UiWidgets.ButtonStyle.GHOST, contains(mouseX, mouseY, showAllX, y + 1, showAllWidth, MENU_ROW - 2) ? 1.0F : 0.0F);
		UiWidgets.button(context, font, theme, hideX, y + 1, hideWidth, MENU_ROW - 2, hideAll, UiWidgets.ButtonStyle.GHOST, contains(mouseX, mouseY, hideX, y + 1, hideWidth, MENU_ROW - 2) ? 1.0F : 0.0F);
		y += MENU_ROW;
		for (HudLayoutElement element : HudLayoutManager.editorElements()) {
			HudElementId id = element.id();
			boolean shown = !isHidden(id);
			boolean hover = contains(mouseX, mouseY, rowX, y, rowWidth, MENU_ROW);
			if (hover) {
				UiShapes.roundedRect(context, rowX, y, rowWidth, MENU_ROW, 5, UiTheme.fade(theme.text(), 0.08F));
			}
			Component name = UiText.ellipsize(font, Component.literal(label(id)), UiText.Size.BODY, rowWidth - 12 - UiWidgets.SWITCH_WIDTH - 8);
			UiText.drawCentered(context, font, name, UiText.Size.BODY, rowX + 6, y + MENU_ROW / 2, shown ? theme.text() : theme.muted());
			float progress = anim.towards("hud-editor-show:" + id.configKey(), shown, 14.0F);
			UiWidgets.toggle(context, theme, rowX + rowWidth - 6 - UiWidgets.SWITCH_WIDTH, y + (MENU_ROW - UiWidgets.SWITCH_HEIGHT) / 2, progress, hover ? 1.0F : 0.0F);
			y += MENU_ROW;
		}
	}

	/** A click in the Show menu: Show all, Hide all or an element's switch. */
	private void clickMenu(double mouseX, double mouseY) {
		int rowX = menuX + MENU_PADDING;
		int rowWidth = menuWidth - MENU_PADDING * 2;
		int y = menuY + MENU_PADDING;
		int hideWidth = UiWidgets.buttonWidth(font, Component.translatable(EMUtilsTexts.UI_HUD_EDITOR_HIDE_ALL));
		int showAllWidth = UiWidgets.buttonWidth(font, Component.translatable(EMUtilsTexts.UI_HUD_EDITOR_SHOW_ALL));
		int hideX = rowX + rowWidth - hideWidth;
		int showAllX = hideX - 4 - showAllWidth;
		if (contains(mouseX, mouseY, showAllX, y + 1, showAllWidth, MENU_ROW - 2)) {
			HIDDEN.clear();
			return;
		}
		if (contains(mouseX, mouseY, hideX, y + 1, hideWidth, MENU_ROW - 2)) {
			for (HudLayoutElement element : HudLayoutManager.editorElements()) {
				HIDDEN.add(element.id());
			}
			selected = null;
			return;
		}
		y += MENU_ROW;
		for (HudLayoutElement element : HudLayoutManager.editorElements()) {
			if (contains(mouseX, mouseY, rowX, y, rowWidth, MENU_ROW)) {
				HudElementId id = element.id();
				if (!HIDDEN.remove(id)) {
					HIDDEN.add(id);
					if (id.equals(selected)) {
						selected = null;
					}
				}
				return;
			}
			y += MENU_ROW;
		}
	}

	/** The selected element's card: its name, Size and Opacity sliders, and a reset for both. */
	private void drawCard(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		HudLayoutDraft draft = draft(selected);
		HudOverlayPlacement.PanelDimensions panel = dimensions.get(selected);
		if (draft == null || panel == null) {
			return;
		}
		int lineHeight = UiText.lineHeight(font, UiText.Size.BODY);
		int headerHeight = RESET_HEIGHT;
		cardHeight = CARD_PADDING + headerHeight + 10 + (lineHeight + 6 + UiWidgets.SLIDER_HEIGHT) * 2 + 10 + CARD_PADDING;
		// Next to the element: right of it if there's room, otherwise left; kept on screen and clear of the
		// toolbar. It stays put while a slider is dragged: resizing the element would otherwise move the
		// card, and with it the slider under the mouse, so the value would jump back and forth.
		if (drag != Drag.SCALE_SLIDER && drag != Drag.OPACITY_SLIDER) {
			int right = draft.x() + panel.width() + 12;
			cardX = right + CARD_WIDTH <= width - 6 ? right : Math.max(6, draft.x() - 12 - CARD_WIDTH);
			boolean toolbarAtTop = toolbarY == TOOLBAR_MARGIN;
			int minY = toolbarAtTop ? toolbarY + TOOLBAR_HEIGHT + 8 : 6;
			int maxY = (toolbarAtTop ? height - 6 : toolbarY - 8) - cardHeight;
			cardY = Math.clamp(draft.y(), minY, Math.max(minY, maxY));
		}
		UiShapes.shadow(context, cardX, cardY, CARD_WIDTH, cardHeight, 10, 10, theme.shadow());
		UiShapes.borderedRect(context, cardX, cardY, CARD_WIDTH, cardHeight, 10, theme.surface(), theme.line());
		int left = cardX + CARD_PADDING;
		sliderX = left;
		sliderWidth = CARD_WIDTH - CARD_PADDING * 2;

		// Header: the element's name, and Reset at the top right.
		Component reset = Component.translatable(EMUtilsTexts.UI_HUD_EDITOR_RESET);
		cardResetWidth = UiWidgets.buttonWidth(font, reset);
		cardResetX = cardX + CARD_WIDTH - CARD_PADDING - cardResetWidth;
		cardResetY = cardY + CARD_PADDING;
		UiWidgets.button(context, font, theme, cardResetX, cardResetY, cardResetWidth, RESET_HEIGHT, reset, UiWidgets.ButtonStyle.OUTLINE, contains(mouseX, mouseY, cardResetX, cardResetY, cardResetWidth, RESET_HEIGHT) ? 1.0F : 0.0F);
		Component name = UiText.ellipsize(font, Component.literal(label(selected)), UiText.Size.BOLD, cardResetX - 8 - left);
		UiText.drawCentered(context, font, name, UiText.Size.BOLD, left, cardResetY + RESET_HEIGHT / 2, theme.text());

		int rowY = cardY + CARD_PADDING + headerHeight + 10;
		scaleSliderY = rowY + lineHeight + 6;
		sliderRow(context, theme, Component.translatable(EMUtilsTexts.UI_HUD_EDITOR_SIZE), draft.scale() + "%", rowY, scaleSliderY, scaleFraction(draft.scale()), drag == Drag.SCALE_SLIDER, mouseX, mouseY);
		rowY = scaleSliderY + UiWidgets.SLIDER_HEIGHT + 10;
		opacitySliderY = rowY + lineHeight + 6;
		sliderRow(context, theme, Component.translatable(EMUtilsTexts.UI_HUD_EDITOR_OPACITY), draft.opacity() + "%", rowY, opacitySliderY, draft.opacity() / 100.0F, drag == Drag.OPACITY_SLIDER, mouseX, mouseY);
	}

	private void sliderRow(GuiGraphicsExtractor context, UiTheme theme, Component label, String value, int labelY, int sliderY, float fraction, boolean dragged, int mouseX, int mouseY) {
		UiText.draw(context, font, label, UiText.Size.BODY, sliderX, labelY, theme.textSecondary());
		Component valueText = Component.literal(value);
		UiText.draw(context, font, valueText, UiText.Size.BODY, sliderX + sliderWidth - UiText.width(font, valueText, UiText.Size.BODY), labelY, theme.text());
		float hover = dragged || contains(mouseX, mouseY, sliderX, sliderY - 4, sliderWidth, UiWidgets.SLIDER_HEIGHT + 8) ? 1.0F : 0.0F;
		UiWidgets.slider(context, theme, sliderX, sliderY, sliderWidth, fraction, hover);
	}

	private static float scaleFraction(int scale) {
		return Math.clamp((scale - SCALE_SLIDER_MIN) / (float) (SCALE_SLIDER_MAX - SCALE_SLIDER_MIN), 0.0F, 1.0F);
	}

	private float sliderFraction(double mouseX) {
		return Math.clamp((float) ((mouseX - sliderX) / sliderWidth), 0.0F, 1.0F);
	}

	// ---- input ----------------------------------------------------------------------------------

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		double mouseX = click.x();
		double mouseY = click.y();
		boolean left = click.button() == InputConstants.MOUSE_BUTTON_LEFT;
		int buttonY = toolbarY + TOOLBAR_HEIGHT / 2 - TOOLBAR_BUTTON / 2;
		if (menuOpen) {
			layoutMenu();
			if (left && contains(mouseX, mouseY, menuX, menuY, menuWidth, menuHeight)) {
				clickMenu(mouseX, mouseY);
				return true;
			}
			// Any other click closes the menu; one on the Show button itself only closes it.
			menuOpen = false;
			if (contains(mouseX, mouseY, showX, buttonY, showWidth, TOOLBAR_BUTTON)) {
				return true;
			}
		} else if (left && contains(mouseX, mouseY, showX, buttonY, showWidth, TOOLBAR_BUTTON)) {
			menuOpen = true;
			return true;
		}
		if (left && contains(mouseX, mouseY, toolbarX, toolbarY, toolbarWidth, TOOLBAR_HEIGHT)) {
			if (contains(mouseX, mouseY, saveX, buttonY, saveWidth, TOOLBAR_BUTTON)) {
				save();
			} else if (contains(mouseX, mouseY, cancelX, buttonY, cancelWidth, TOOLBAR_BUTTON)) {
				cancel();
			} else if (contains(mouseX, mouseY, resetX, buttonY, resetWidth, TOOLBAR_BUTTON)) {
				resetAll();
			}
			return true;
		}
		if (left && selected != null && contains(mouseX, mouseY, cardX, cardY, CARD_WIDTH, cardHeight)) {
			if (contains(mouseX, mouseY, sliderX, scaleSliderY - 4, sliderWidth, UiWidgets.SLIDER_HEIGHT + 8)) {
				drag = Drag.SCALE_SLIDER;
				applySlider(mouseX);
			} else if (contains(mouseX, mouseY, sliderX, opacitySliderY - 4, sliderWidth, UiWidgets.SLIDER_HEIGHT + 8)) {
				drag = Drag.OPACITY_SLIDER;
				applySlider(mouseX);
			} else if (contains(mouseX, mouseY, cardResetX, cardResetY, cardResetWidth, RESET_HEIGHT)) {
				resetSelected();
			}
			return true;
		}

		HudElementId hit = selected != null && onResizeHandle(selected, mouseX, mouseY) ? selected : elementAt(mouseX, mouseY);
		if (hit == null) {
			selected = null;
			return true;
		}
		if (click.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
			// Right-click puts an element back to its normal size.
			HudLayoutManager.setDraftScale(hit, 100);
			refreshDimensions();
			return true;
		}
		if (!left) {
			return true;
		}
		cycleOnRelease = hit.equals(selected) && !onResizeHandle(hit, mouseX, mouseY);
		pressX = (int) mouseX;
		pressY = (int) mouseY;
		pressMoved = false;
		selected = hit;
		dragging = hit;
		HudLayoutDraft draft = draft(hit);
		if (onResizeHandle(hit, mouseX, mouseY)) {
			drag = Drag.RESIZE;
		} else {
			drag = Drag.MOVE;
			grabX = (int) mouseX - (draft == null ? 0 : draft.x());
			grabY = (int) mouseY - (draft == null ? 0 : draft.y());
		}
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
		HudLayoutConfig config = HudLayoutManager.editorConfig();
		if (config == null) {
			return true;
		}
		switch (drag) {
			case SCALE_SLIDER, OPACITY_SLIDER -> applySlider(click.x());
			case RESIZE -> {
				HudLayoutDraft draft = draft(dragging);
				if (draft != null) {
					int scale = HudLayoutRegistry.require(dragging).scaleFromResize(config, minecraft, draft.x(), draft.y(), (int) click.x(), (int) click.y());
					HudLayoutManager.setDraftScale(dragging, scale);
					refreshDimensions();
				}
			}
			case MOVE -> {
				if (Math.abs((int) click.x() - pressX) > CLICK_SLOP || Math.abs((int) click.y() - pressY) > CLICK_SLOP) {
					pressMoved = true;
				}
				move((int) click.x() - grabX, (int) click.y() - grabY);
			}
			case NONE -> {
			}
		}
		return true;
	}

	/** Moves the dragged element to {@code targetX, targetY}, snapping to edges and centers while Ctrl is held. */
	private void move(int targetX, int targetY) {
		HudOverlayPlacement.PanelDimensions panel = dimensions.get(dragging);
		if (panel == null) {
			return;
		}
		guides.clear();
		HudOverlayPlacement.Position result;
		if (snapHeld()) {
			List<HudLayoutSnapping.Bounds> others = new ArrayList<>();
			for (Map.Entry<HudElementId, HudLayoutDraft> entry : HudLayoutManager.draftLayouts().entrySet()) {
				HudOverlayPlacement.PanelDimensions other = dimensions.get(entry.getKey());
				if (!entry.getKey().equals(dragging) && other != null && !isHidden(entry.getKey())) {
					others.add(new HudLayoutSnapping.Bounds(entry.getValue().x(), entry.getValue().y(), other.width(), other.height()));
				}
			}
			HudLayoutSnapping.SnapResult snapped = HudLayoutSnapping.snap(
				new HudOverlayPlacement.Position(targetX, targetY),
				new HudLayoutSnapping.Bounds(targetX, targetY, panel.width(), panel.height()),
				others,
				width,
				height
			);
			result = snapped.position();
			guides.addAll(snapped.guides());
		} else {
			result = new HudOverlayPlacement.Position(
				Math.clamp(targetX, 0, Math.max(0, width - panel.width())),
				Math.clamp(targetY, 0, Math.max(0, height - panel.height()))
			);
		}
		HudLayoutManager.setDraftPosition(dragging, result.x(), result.y());
	}

	private void applySlider(double mouseX) {
		if (selected == null) {
			return;
		}
		float fraction = sliderFraction(mouseX);
		if (drag == Drag.SCALE_SLIDER) {
			HudLayoutManager.setDraftScale(selected, Math.round(SCALE_SLIDER_MIN + fraction * (SCALE_SLIDER_MAX - SCALE_SLIDER_MIN)));
			refreshDimensions();
		} else {
			HudLayoutManager.setDraftOpacity(selected, Math.round(fraction * 100.0F));
		}
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		if (cycleOnRelease && !pressMoved && drag == Drag.MOVE) {
			selectNextUnder(pressX, pressY);
		}
		cycleOnRelease = false;
		drag = Drag.NONE;
		dragging = null;
		guides.clear();
		return true;
	}

	/** Clicking the selected element again, without dragging it, selects the next one under that spot, going round. */
	private void selectNextUnder(int mouseX, int mouseY) {
		List<HudElementId> hits = elementsAt(mouseX, mouseY);
		int index = hits.indexOf(selected);
		if (hits.size() > 1 && index >= 0) {
			selected = hits.get((index + 1) % hits.size());
		}
	}

	/** Arrows nudge the selected element (Shift for 10 px), Enter saves, Esc deselects and then cancels. */
	@Override
	public boolean keyPressed(KeyEvent input) {
		if (input.isEscape()) {
			if (menuOpen) {
				menuOpen = false;
			} else if (selected != null) {
				selected = null;
			} else {
				cancel();
			}
			return true;
		}
		if (input.isConfirmation()) {
			save();
			return true;
		}
		HudLayoutDraft draft = draft(selected);
		if (draft != null) {
			int step = input.hasShiftDown() ? 10 : 1;
			int dx = input.key() == InputConstants.KEY_LEFT ? -step : input.key() == InputConstants.KEY_RIGHT ? step : 0;
			int dy = input.key() == InputConstants.KEY_UP ? -step : input.key() == InputConstants.KEY_DOWN ? step : 0;
			if (dx != 0 || dy != 0) {
				HudOverlayPlacement.PanelDimensions panel = dimensions.get(selected);
				int maxX = panel == null ? width : Math.max(0, width - panel.width());
				int maxY = panel == null ? height : Math.max(0, height - panel.height());
				HudLayoutManager.setDraftPosition(selected, Math.clamp(draft.x() + dx, 0, maxX), Math.clamp(draft.y() + dy, 0, maxY));
				return true;
			}
		}
		return super.keyPressed(input);
	}

	private boolean snapHeld() {
		long window = minecraft.getWindow().handle();
		return VersionedInput.isKeyCodeDown(window, InputConstants.KEY_LCONTROL) || VersionedInput.isKeyCodeDown(window, InputConstants.KEY_RCONTROL);
	}

	// ---- actions --------------------------------------------------------------------------------

	private void resetSelected() {
		HudLayoutConfig config = HudLayoutManager.editorConfig();
		if (selected == null || config == null) {
			return;
		}
		HudLayoutManager.setDraftScale(selected, 100);
		HudLayoutManager.setDraftOpacity(selected, HudLayoutRegistry.require(selected).defaultOpacityPercent(config));
		refreshDimensions();
	}

	private void resetAll() {
		HudLayoutConfig config = HudLayoutManager.editorConfig();
		if (config == null) {
			return;
		}
		HudLayoutManager.resetAllDraftsToDefaults(minecraft, config, width, height);
		refreshDimensions();
		guides.clear();
	}

	private void save() {
		HudLayoutConfig config = HudLayoutManager.editorConfig();
		if (config != null) {
			HudLayoutManager.saveDraft(config);
		}
		minecraft.gui.setScreen(parent);
	}

	private void cancel() {
		HudLayoutConfig config = HudLayoutManager.editorConfig();
		if (config != null) {
			HudLayoutManager.cancelEditor(config);
		} else {
			HudLayoutManager.clearDraft();
		}
		minecraft.gui.setScreen(parent);
	}

	/** Closing any other way (such as the window losing the screen) cancels, like the Cancel button. */
	@Override
	public void onClose() {
		cancel();
	}

	/** Selects the first element; used by UI snapshots. */
	public void selectFirstForSnapshot() {
		if (!HudLayoutManager.editorElements().isEmpty()) {
			selected = HudLayoutManager.editorElements().getFirst().id();
		}
	}

	/** Drags the Size slider to {@code fraction} of its width, as the mouse would (null ends the drag); used by UI snapshots. */
	public void dragSizeSliderForSnapshot(@Nullable Float fraction) {
		if (fraction == null) {
			drag = Drag.NONE;
			return;
		}
		drag = Drag.SCALE_SLIDER;
		applySlider(sliderX + fraction * sliderWidth);
	}

	/** Where the selected element's card is drawn; used by UI snapshots. */
	public int cardXForSnapshot() {
		return cardX;
	}

	/** The selected element's draft layout; used by UI snapshots. */
	public @Nullable HudLayoutDraft selectedDraftForSnapshot() {
		return draft(selected);
	}

	/**
	 * Checks that, where two elements overlap, a click picks the smaller one, and that clicking the selected one
	 * again goes on to the other; returns what went wrong, or null. Used by UI snapshots.
	 */
	public @Nullable String hitOrderProblemForSnapshot() {
		List<HudLayoutElement> elements = HudLayoutManager.editorElements();
		int pairs = 0;
		for (int i = 0; i < elements.size(); i++) {
			for (int j = i + 1; j < elements.size(); j++) {
				HudElementId a = elements.get(i).id();
				HudElementId b = elements.get(j).id();
				HudLayoutDraft draftA = draft(a);
				HudLayoutDraft draftB = draft(b);
				HudOverlayPlacement.PanelDimensions panelA = dimensions.get(a);
				HudOverlayPlacement.PanelDimensions panelB = dimensions.get(b);
				if (draftA == null || draftB == null || panelA == null || panelB == null) {
					continue;
				}
				int left = Math.max(draftA.x(), draftB.x());
				int right = Math.min(draftA.x() + panelA.width(), draftB.x() + panelB.width());
				int top = Math.max(draftA.y(), draftB.y());
				int bottom = Math.min(draftA.y() + panelA.height(), draftB.y() + panelB.height());
				if (right - left < 2 || bottom - top < 2) {
					continue;
				}
				pairs++;
				List<HudElementId> hits = elementsAt((left + right) / 2.0D, (top + bottom) / 2.0D);
				if (hits.size() < 2) {
					return label(a) + " and " + label(b) + " overlap but only " + hits.size() + " can be hit there";
				}
				for (int k = 1; k < hits.size(); k++) {
					HudOverlayPlacement.PanelDimensions first = dimensions.get(hits.get(k - 1));
					HudOverlayPlacement.PanelDimensions next = dimensions.get(hits.get(k));
					if ((long) first.width() * first.height() > (long) next.width() * next.height()) {
						return label(hits.get(k - 1)) + " is picked before the smaller " + label(hits.get(k));
					}
				}
			}
		}
		return pairs == 0 ? "no two elements overlap in the editor" : null;
	}

	/** Checks that a hidden element can't be picked and can again once shown; used by UI snapshots. */
	public boolean hidingWorksForSnapshot() {
		for (HudLayoutElement element : HudLayoutManager.editorElements()) {
			HudElementId id = element.id();
			HudLayoutDraft draft = draft(id);
			HudOverlayPlacement.PanelDimensions panel = dimensions.get(id);
			if (draft == null || panel == null) {
				continue;
			}
			double x = draft.x() + panel.width() / 2.0D;
			double y = draft.y() + panel.height() / 2.0D;
			HIDDEN.add(id);
			boolean gone = !elementsAt(x, y).contains(id);
			HIDDEN.remove(id);
			return gone && elementsAt(x, y).contains(id);
		}
		return false;
	}

	/** Hides or shows an element in the editor, as its switch in the Show menu does; used by UI snapshots. */
	public void setHiddenForSnapshot(HudElementId id, boolean hidden) {
		if (hidden) {
			HIDDEN.add(id);
		} else {
			HIDDEN.remove(id);
		}
	}

	/** The toolbar's width as last drawn; used by UI snapshots. */
	public int toolbarWidthForSnapshot() {
		return toolbarWidth;
	}

	/** Opens or closes the Show menu; used by UI snapshots. */
	public void setMenuOpenForSnapshot(boolean open) {
		menuOpen = open;
	}

	private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
}
