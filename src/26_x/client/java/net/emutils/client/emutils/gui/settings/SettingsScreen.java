package net.emutils.client.emutils.gui.settings;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.gui.hub.CustomHubScreen;
import net.emutils.client.emutils.gui.hub.HubFeature;
import net.emutils.client.emutils.gui.hub.HubFeatureCatalog;
import net.emutils.client.emutils.gui.hub.HubIcons;
import net.emutils.client.emutils.gui.ui.UiAnim;
import net.emutils.client.emutils.gui.ui.UiIcons;
import net.emutils.client.emutils.gui.ui.UiScrollArea;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.emutils.gui.ui.UiTextField;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.gui.ui.UiWidgets;
import net.emutils.client.emutils.util.EMUtilsBuild;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

/**
 * The new EMUtils settings screen (#88). It is still being built, so it is only reachable in dev builds
 * through the "New Settings UI (Preview)" option; everyone else gets {@link CustomHubScreen}.
 */
public final class SettingsScreen extends Screen {
	private static final int MARGIN = 14;
	private static final int MAX_WIDTH = 720;
	private static final int MAX_HEIGHT = 430;
	private static final int PADDING = 16;
	private static final int PANEL_RADIUS = 14;
	private static final int CONTROL_RADIUS = 10;
	private static final int SEARCH_ROW = 22;
	private static final int CATEGORY_ROW = 20;
	private static final int CATEGORY_BUTTON_HEIGHT = 15;
	private static final int ROUND_BUTTON = 20;
	private static final int MAX_COLUMNS = 3;
	/** Cards narrower than this drop to fewer columns, so names are not cut off. */
	private static final int MIN_CARD_WIDTH = 160;
	private static final int CARD_HEIGHT = 44;
	private static final int CARD_GAP = 8;
	private static final int CARD_RADIUS = 9;
	private static final int CARD_PADDING = 10;
	private static final int CARD_ICON = 11;
	private static final int HEADING_HEIGHT = 16;
	private static final int GROUP_GAP = 12;
	private static final int FADE_HEIGHT = 12;
	private static final int OPEN_BUTTON_HEIGHT = 14;

	private final Screen parent;
	private final List<HubFeature> features;
	private final UiAnim anim = new UiAnim();
	private final UiTextField search = new UiTextField(this, 64);
	private final UiScrollArea scroll = new UiScrollArea();
	private final List<CategoryButton> categoryButtons = new ArrayList<>();
	private final List<CardBox> cards = new ArrayList<>();
	private HubFeature.@Nullable Group selectedGroup;
	private @Nullable SettingsSheet sheet;
	private int panelX;
	private int panelY;
	private int panelWidth;
	private int panelHeight;
	private int controlX;
	private int controlY;
	private int controlWidth;
	private int themeButtonX;
	private int classicButtonX;
	private int classicButtonWidth;
	private int rightButtonsY;
	private int titleY;
	private boolean stackedHeader;
	private boolean showTagline;

	public SettingsScreen(Screen parent) {
		super(Component.translatable(EMUtilsTexts.HUB_MODERN_TITLE));
		this.parent = parent;
		this.features = HubFeatureCatalog.all();
	}

	@Override
	protected void init() {
		UiText.refreshFonts();
		layout();
		search.restoreFocus();
	}

	private void layout() {
		panelWidth = Math.min(MAX_WIDTH, width - MARGIN * 2);
		panelHeight = Math.min(MAX_HEIGHT, height - MARGIN * 2);
		panelX = (width - panelWidth) / 2;
		panelY = (height - panelHeight) / 2;

		// Category buttons decide how wide the search and category control is.
		categoryButtons.clear();
		int buttonsWidth = 0;
		categoryButtons.add(new CategoryButton(null, Component.translatable(EMUtilsTexts.UI_ALL), null));
		for (HubFeature.Group group : HubFeature.Group.values()) {
			categoryButtons.add(new CategoryButton(group, Component.translatable(group.labelKey()), group.icon()));
		}
		for (CategoryButton button : categoryButtons) {
			button.width = categoryButtonWidth(button);
			buttonsWidth += button.width;
		}
		buttonsWidth += (categoryButtons.size() - 1) * 2;
		controlWidth = Math.min(panelWidth - PADDING * 2, Math.max(300, buttonsWidth + 8));

		Component classic = Component.translatable(EMUtilsTexts.UI_CLASSIC);
		classicButtonWidth = UiWidgets.buttonWidth(font, classic);
		int rightButtonsWidth = ROUND_BUTTON + 6 + classicButtonWidth;
		int titleBlockWidth = titleBlockWidth();
		int centeredX = panelX + (panelWidth - controlWidth) / 2;
		stackedHeader = centeredX < panelX + PADDING + titleBlockWidth + 12
			|| centeredX + controlWidth > panelX + panelWidth - PADDING - rightButtonsWidth - 12;
		// The tagline is the first thing to go when the title column gets narrow.
		int taglineWidth = UiText.width(font, Component.translatable(EMUtilsTexts.UI_TAGLINE), UiText.Size.BODY);
		// When the header stacks, the tagline goes too, to leave room for the cards.
		showTagline = !stackedHeader && centeredX >= panelX + PADDING + taglineWidth + 14;

		themeButtonX = panelX + panelWidth - PADDING - ROUND_BUTTON;
		classicButtonX = themeButtonX - 6 - classicButtonWidth;
		controlX = centeredX;
		controlY = stackedHeader ? panelY + PADDING + 28 : panelY + PADDING;

		// Like the mockup, the title block, the search control and the buttons share one center line.
		int controlHeight = SEARCH_ROW + 1 + CATEGORY_ROW;
		int titleHeight = UiText.lineHeight(font, UiText.Size.TITLE);
		int titleBlockHeight = titleHeight + (showTagline ? 5 + UiText.lineHeight(font, UiText.Size.BODY) : 0);
		if (stackedHeader) {
			titleY = panelY + PADDING + 6;
			rightButtonsY = titleY + titleHeight / 2 - ROUND_BUTTON / 2;
		} else {
			int headerCenter = controlY + controlHeight / 2;
			titleY = headerCenter - titleBlockHeight / 2;
			rightButtonsY = headerCenter - ROUND_BUTTON / 2;
		}
		int bodyY = controlY + controlHeight + 10;
		scroll.setBounds(panelX + PADDING, bodyY, panelWidth - PADDING * 2 + UiScrollArea.GUTTER, panelY + panelHeight - PADDING / 2 - bodyY);
	}

	private int categoryButtonWidth(CategoryButton button) {
		int iconSpace = button.icon == null ? 0 : 9 + 4;
		return UiText.width(font, button.label, UiText.Size.LABEL) + iconSpace + 12;
	}

	private int titleBlockWidth() {
		return UiText.width(font, Component.translatable(EMUtilsTexts.NAME), UiText.Size.TITLE)
			+ 6
			+ UiText.width(font, Component.literal(EMUtilsBuild.modVersion()), UiText.Size.BODY);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		super.extractBackground(context, mouseX, mouseY, delta);
		context.fill(0, 0, width, height, UiTheme.current().dim());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		anim.frame();
		UiTheme theme = UiTheme.current();
		UiShapes.shadow(context, panelX, panelY, panelWidth, panelHeight, PANEL_RADIUS, 18, theme.shadow());
		UiShapes.roundedRect(context, panelX, panelY, panelWidth, panelHeight, PANEL_RADIUS, theme.panel());

		// While a sheet is open, nothing underneath reacts to the mouse.
		int backX = sheet == null ? mouseX : Integer.MIN_VALUE / 2;
		int backY = sheet == null ? mouseY : Integer.MIN_VALUE / 2;
		drawTitle(context, theme);
		drawControl(context, theme, backX, backY);
		drawRightButtons(context, theme, backX, backY);
		drawCards(context, theme, backX, backY);
		if (sheet != null) {
			sheet.render(context, theme, mouseX, mouseY, panelX, panelY, panelWidth, panelHeight, width, height);
			if (sheet.isClosed()) {
				sheet = null;
			}
		}
	}

	private void drawTitle(GuiGraphicsExtractor context, UiTheme theme) {
		int x = panelX + PADDING + 2;
		Component name = Component.translatable(EMUtilsTexts.NAME);
		int titleHeight = UiText.lineHeight(font, UiText.Size.TITLE);
		UiText.draw(context, font, name, UiText.Size.TITLE, x, titleY, theme.text());
		int nameWidth = UiText.width(font, name, UiText.Size.TITLE);
		int versionHeight = UiText.lineHeight(font, UiText.Size.BODY);
		UiText.draw(context, font, Component.literal(EMUtilsBuild.modVersion()), UiText.Size.BODY, x + nameWidth + 6, titleY + titleHeight - versionHeight, theme.muted());
		if (showTagline) {
			UiText.draw(context, font, Component.translatable(EMUtilsTexts.UI_TAGLINE), UiText.Size.BODY, x, titleY + titleHeight + 5, theme.muted());
		}

		// Dev builds get a badge in the space above the name, so the title never moves.
		String commit = EMUtilsBuild.devCommit();
		if (commit != null) {
			int badgeHeight = UiText.lineHeight(font, UiText.Size.SMALL) + 5;
			UiWidgets.badge(context, font, x - 1, titleY - badgeHeight - 2, Component.literal("DEV " + commit), theme.devBackground(), theme.devText());
		}
	}

	private void drawControl(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		int height = SEARCH_ROW + 1 + CATEGORY_ROW;
		UiShapes.shadow(context, controlX, controlY, controlWidth, height, CONTROL_RADIUS, 6, UiTheme.fade(theme.shadow(), 0.6F));
		UiShapes.borderedRect(context, controlX, controlY, controlWidth, height, CONTROL_RADIUS, theme.surface(), theme.border());

		int searchCenter = controlY + SEARCH_ROW / 2 + 1;
		UiIcons.draw(context, HubIcons.SEARCH, controlX + 10, searchCenter - 5, 10, theme.textSecondary());
		int hintWidth = 0;
		if (!search.focused()) {
			hintWidth = drawShortcutHint(context, theme, controlX + controlWidth - 8, searchCenter) + 8;
		}
		search.draw(context, font, theme, controlX + 26, searchCenter, controlWidth - 34 - hintWidth, Component.translatable(EMUtilsTexts.UI_SEARCH));
		context.fill(controlX + 1, controlY + SEARCH_ROW, controlX + controlWidth - 1, controlY + SEARCH_ROW + 1, theme.line());

		int buttonsWidth = categoryButtons.stream().mapToInt(button -> button.width).sum() + (categoryButtons.size() - 1) * 2;
		int x = controlX + (controlWidth - buttonsWidth) / 2;
		int y = controlY + SEARCH_ROW + 1 + (CATEGORY_ROW - CATEGORY_BUTTON_HEIGHT) / 2;
		for (int i = 0; i < categoryButtons.size(); i++) {
			CategoryButton button = categoryButtons.get(i);
			button.x = x;
			button.y = y;
			boolean selected = button.group == selectedGroup;
			float hover = anim.towards("category:" + i, contains(mouseX, mouseY, x, y, button.width, CATEGORY_BUTTON_HEIGHT), 16.0F);
			float select = anim.towards("category-selected:" + i, selected, 16.0F);
			int background = UiTheme.mix(UiTheme.fade(theme.hover(), hover), theme.selectedBackground(), select);
			UiShapes.roundedRect(context, x, y, button.width, CATEGORY_BUTTON_HEIGHT, 7, background);
			int color = UiTheme.mix(theme.textSecondary(), theme.selectedText(), select);
			int textX = x + 6;
			if (button.icon != null) {
				UiIcons.draw(context, button.icon, textX, y + (CATEGORY_BUTTON_HEIGHT - 9) / 2, 9, color);
				textX += 13;
			}
			UiText.drawCentered(context, font, button.label, UiText.Size.LABEL, textX, y + CATEGORY_BUTTON_HEIGHT / 2, color);
			x += button.width + 2;
		}
	}

	/** Draws the "Ctrl F" keycaps ending at {@code right}; returns their total width. */
	private int drawShortcutHint(GuiGraphicsExtractor context, UiTheme theme, int right, int centerY) {
		String[] keys = Util.getPlatform() == Util.OS.OSX ? new String[] {"Cmd", "F"} : new String[] {"Ctrl", "F"};
		int height = UiText.lineHeight(font, UiText.Size.SMALL) + 6;
		int x = right;
		for (int i = keys.length - 1; i >= 0; i--) {
			Component key = Component.literal(keys[i]);
			int keyWidth = Math.max(height, UiText.width(font, key, UiText.Size.SMALL) + 7);
			x -= keyWidth;
			UiShapes.roundedRect(context, x, centerY - height / 2, keyWidth, height, 3, theme.segmentBackground());
			int textWidth = UiText.width(font, key, UiText.Size.SMALL);
			UiText.drawCentered(context, font, key, UiText.Size.SMALL, x + (keyWidth - textWidth) / 2, centerY, theme.muted());
			if (i > 0) {
				x -= 2;
			}
		}
		return right - x;
	}

	private void drawRightButtons(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		boolean dark = theme == UiTheme.DARK;
		float themeHover = anim.towards("theme", contains(mouseX, mouseY, themeButtonX, rightButtonsY, ROUND_BUTTON, ROUND_BUTTON), 16.0F);
		UiWidgets.iconButton(context, theme, themeButtonX, rightButtonsY, ROUND_BUTTON, dark ? HubIcons.SUN : HubIcons.MOON, themeHover);

		float classicHover = anim.towards("classic", contains(mouseX, mouseY, classicButtonX, rightButtonsY, classicButtonWidth, ROUND_BUTTON), 16.0F);
		UiWidgets.button(context, font, theme, classicButtonX, rightButtonsY, classicButtonWidth, ROUND_BUTTON, Component.translatable(EMUtilsTexts.UI_CLASSIC), UiWidgets.ButtonStyle.GHOST, classicHover);
	}

	private void drawCards(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		List<Group> groups = visibleGroups();
		boolean headings = selectedGroup == null;
		int innerWidth = scroll.contentWidth();
		int columns = MAX_COLUMNS;
		while (columns > 1 && (innerWidth - CARD_GAP * (columns - 1)) / columns < MIN_CARD_WIDTH) {
			columns--;
		}
		int cardWidth = (innerWidth - CARD_GAP * (columns - 1)) / columns;

		int contentHeight = 0;
		for (Group group : groups) {
			int rows = (group.features().size() + columns - 1) / columns;
			contentHeight += (headings ? HEADING_HEIGHT : 0) + rows * CARD_HEIGHT + (rows - 1) * CARD_GAP + GROUP_GAP;
		}
		scroll.setContentHeight(Math.max(0, contentHeight - GROUP_GAP + FADE_HEIGHT));
		scroll.animate(anim, "scroll");

		cards.clear();
		boolean mouseInList = scroll.contains(mouseX, mouseY);
		scroll.begin(context);
		// Cards are laid out at whole pixels and the leftover fraction of the scroll offset is applied
		// as a translation, so scrolling glides smoothly while clicks still use whole positions.
		context.pose().pushMatrix();
		context.pose().translate(0.0F, scroll.offset() - scroll.exactOffset());
		int y = scroll.y() + FADE_HEIGHT / 2 - scroll.offset();
		for (Group group : groups) {
			if (headings) {
				int headingCenter = y + HEADING_HEIGHT / 2 - 2;
				UiShapes.circle(context, scroll.x() + 2, headingCenter - 2, 5, theme.groupColor(group.group()));
				Component label = Component.translatable(group.group().labelKey());
				UiText.drawCentered(context, font, label, UiText.Size.BOLD, scroll.x() + 11, headingCenter, theme.text());
				int labelWidth = UiText.width(font, label, UiText.Size.BOLD);
				UiText.drawCentered(context, font, Component.literal(Integer.toString(group.features().size())), UiText.Size.BODY, scroll.x() + 16 + labelWidth, headingCenter, theme.muted());
				y += HEADING_HEIGHT;
			}
			for (int i = 0; i < group.features().size(); i++) {
				int column = i % columns;
				int row = i / columns;
				int cardX = scroll.x() + column * (cardWidth + CARD_GAP);
				int cardY = y + row * (CARD_HEIGHT + CARD_GAP);
				cards.add(drawCard(context, theme, group.features().get(i), cardX, cardY, cardWidth, mouseInList, mouseX, mouseY));
			}
			int rows = (group.features().size() + columns - 1) / columns;
			y += rows * CARD_HEIGHT + (rows - 1) * CARD_GAP + GROUP_GAP;
		}
		context.pose().popMatrix();
		if (groups.isEmpty()) {
			Component empty = Component.translatable(EMUtilsTexts.UI_NO_RESULTS, search.text());
			int emptyWidth = UiText.width(font, empty, UiText.Size.BODY);
			UiText.draw(context, font, empty, UiText.Size.BODY, scroll.x() + (innerWidth - emptyWidth) / 2, scroll.y() + 40, theme.muted());
		}
		scroll.end(context, theme.panel(), FADE_HEIGHT, UiTheme.fade(theme.text(), 0.25F));
	}

	/** Draws one feature card and returns where it and its control are, for clicks. */
	private CardBox drawCard(GuiGraphicsExtractor context, UiTheme theme, HubFeature feature, int x, int y, int width, boolean mouseInList, int mouseX, int mouseY) {
		boolean hovered = mouseInList && contains(mouseX, mouseY, x, y, width, CARD_HEIGHT);
		float hover = anim.towards("card:" + feature.id(), hovered, 16.0F);
		UiShapes.borderedRect(context, x, y, width, CARD_HEIGHT, CARD_RADIUS, UiTheme.mix(theme.surface(), theme.surfaceHover(), hover), theme.border());

		int rowCenter = y + CARD_PADDING + 6;
		UiIcons.draw(context, feature.icon().texture(), x + CARD_PADDING, rowCenter - CARD_ICON / 2, CARD_ICON, theme.text());

		int controlRight = x + width - CARD_PADDING;
		int nameRight = controlRight;
		int controlX = 0;
		int controlY = 0;
		int controlWidth = 0;
		int controlHeight = 0;
		if (feature.toggle() != null) {
			int switchX = controlRight - UiWidgets.SWITCH_WIDTH;
			int switchY = rowCenter - UiWidgets.SWITCH_HEIGHT / 2;
			boolean on = feature.toggle().getter().getAsBoolean();
			float progress = anim.transition("switch:" + feature.id(), on, 0.18F);
			float switchHover = hovered && contains(mouseX, mouseY, switchX, switchY, UiWidgets.SWITCH_WIDTH, UiWidgets.SWITCH_HEIGHT) ? 1.0F : 0.0F;
			UiWidgets.toggle(context, theme, switchX, switchY, progress, switchHover);
			nameRight = switchX - 6;
			controlX = switchX;
			controlY = switchY;
			controlWidth = UiWidgets.SWITCH_WIDTH;
			controlHeight = UiWidgets.SWITCH_HEIGHT;
		} else if (feature.primaryAction() != null) {
			Component open = Component.translatable(EMUtilsTexts.UI_OPEN);
			int buttonWidth = UiWidgets.buttonWidth(font, open);
			int buttonX = controlRight - buttonWidth;
			int buttonY = rowCenter - OPEN_BUTTON_HEIGHT / 2;
			float buttonHover = hovered && contains(mouseX, mouseY, buttonX, buttonY, buttonWidth, OPEN_BUTTON_HEIGHT) ? 1.0F : 0.0F;
			UiShapes.roundedRect(context, buttonX, buttonY, buttonWidth, OPEN_BUTTON_HEIGHT, 7, UiTheme.mix(theme.surfaceAlt(), theme.segmentSelected(), buttonHover));
			UiText.drawCentered(context, font, open, UiText.Size.LABEL, buttonX + 9, rowCenter, feature.primaryActionEnabled() ? theme.text() : theme.muted());
			nameRight = buttonX - 6;
			controlX = buttonX;
			controlY = buttonY;
			controlWidth = buttonWidth;
			controlHeight = OPEN_BUTTON_HEIGHT;
		}

		int nameX = x + CARD_PADDING + CARD_ICON + 6;
		Component name = UiText.ellipsize(font, title(feature), UiText.Size.BOLD, nameRight - nameX);
		UiText.drawCentered(context, font, name, UiText.Size.BOLD, nameX, rowCenter, theme.text());

		Component description = UiText.ellipsize(font, Component.translatable(feature.descriptionKey()), UiText.Size.BODY, width - CARD_PADDING * 2);
		UiText.drawCentered(context, font, description, UiText.Size.BODY, x + CARD_PADDING, y + CARD_HEIGHT - CARD_PADDING - 3, theme.muted());
		return new CardBox(feature, x, y, width, CARD_HEIGHT, controlX, controlY, controlWidth, controlHeight);
	}

	/**
	 * The feature's name without the trailing "..." the classic hub uses to mark features that open a
	 * submenu; here every card opens its settings the same way.
	 */
	static Component title(HubFeature feature) {
		String name = feature.title().getString().strip();
		while (name.endsWith(".") || name.endsWith("…")) {
			name = name.substring(0, name.length() - 1).stripTrailing();
		}
		return Component.literal(name);
	}

	private List<Group> visibleGroups() {
		String query = HubFeatureCatalog.normalize(search.text());
		List<Group> groups = new ArrayList<>();
		for (HubFeature.Group group : HubFeature.Group.values()) {
			if (selectedGroup != null && selectedGroup != group) {
				continue;
			}
			List<HubFeature> matches = new ArrayList<>();
			for (HubFeature feature : features) {
				if (feature.group() == group && (query.isEmpty() || feature.matches(query))) {
					matches.add(feature);
				}
			}
			if (!matches.isEmpty()) {
				groups.add(new Group(group, matches));
			}
		}
		return groups;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		double mouseX = click.x();
		double mouseY = click.y();
		if (sheet != null) {
			return sheet.mouseClicked(mouseX, mouseY);
		}
		int controlHeight = SEARCH_ROW + 1 + CATEGORY_ROW;
		boolean onSearch = contains(mouseX, mouseY, controlX, controlY, controlWidth, SEARCH_ROW);
		search.setFocused(onSearch);
		if (onSearch) {
			search.click(font, mouseX, click.hasShiftDown());
			return true;
		}
		if (contains(mouseX, mouseY, controlX, controlY, controlWidth, controlHeight)) {
			for (CategoryButton button : categoryButtons) {
				if (contains(mouseX, mouseY, button.x, button.y, button.width, CATEGORY_BUTTON_HEIGHT)) {
					selectedGroup = button.group;
					scroll.reset();
					return true;
				}
			}
			return true;
		}
		if (contains(mouseX, mouseY, themeButtonX, rightButtonsY, ROUND_BUTTON, ROUND_BUTTON)) {
			EMUtilsClient.config().setSettingsUiDark(UiTheme.current() != UiTheme.DARK);
			return true;
		}
		if (contains(mouseX, mouseY, classicButtonX, rightButtonsY, classicButtonWidth, ROUND_BUTTON)) {
			EMUtilsClient.config().setSettingsUiPreview(false);
			minecraft.setScreenAndShow(new CustomHubScreen(parent));
			return true;
		}
		if (scroll.contains(mouseX, mouseY)) {
			for (CardBox card : cards) {
				if (contains(mouseX, mouseY, card.x(), card.y(), card.width(), card.height())) {
					clickCard(card, mouseX, mouseY);
					return true;
				}
			}
		}
		return super.mouseClicked(click, doubled);
	}

	/**
	 * The switch toggles the feature. The rest of the card opens the feature's own screen if it has one,
	 * and its settings sheet otherwise.
	 */
	private void clickCard(CardBox card, double mouseX, double mouseY) {
		HubFeature feature = card.feature();
		boolean onControl = contains(mouseX, mouseY, card.controlX() - 2, card.controlY() - 2, card.controlWidth() + 4, card.controlHeight() + 4);
		if (onControl && feature.toggle() != null) {
			feature.toggle().setter().accept(!feature.toggle().getter().getAsBoolean());
		} else if (feature.primaryAction() != null) {
			if (feature.primaryActionEnabled()) {
				feature.primaryAction().run();
			}
		} else {
			search.setFocused(false);
			sheet = new SettingsSheet(font, anim, feature);
		}
	}

	/** Opens the settings sheet of the feature with this id, if there is one; used by UI snapshots. */
	public void openSheet(String featureId) {
		for (HubFeature feature : features) {
			if (feature.id().equals(featureId)) {
				sheet = new SettingsSheet(font, anim, feature);
			}
		}
	}

	/** Opens the color picker in the open sheet; used by UI snapshots. */
	public void openColorPickerInSheet() {
		if (sheet != null) {
			sheet.openFirstColorPicker();
		}
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
		if (sheet != null && sheet.mouseDragged(click.x(), click.y())) {
			return true;
		}
		return super.mouseDragged(click, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		if (sheet != null) {
			return sheet.mouseReleased();
		}
		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (sheet != null) {
			return sheet.mouseScrolled(mouseX, mouseY, verticalAmount);
		}
		if (scroll.scroll(mouseX, mouseY, verticalAmount)) {
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean keyPressed(KeyEvent input) {
		if (sheet != null) {
			return sheet.keyPressed(input);
		}
		if (!search.focused() && (input.hasControlDown() || (input.modifiers() & InputConstants.MOD_SUPER) != 0) && input.key() == InputConstants.KEY_F) {
			search.setFocused(true);
			return true;
		}
		if (search.keyPressed(input, scroll::reset)) {
			return true;
		}
		return super.keyPressed(input);
	}

	@Override
	public boolean charTyped(CharacterEvent input) {
		if (sheet != null) {
			return true;
		}
		if (search.charTyped(input, scroll::reset)) {
			return true;
		}
		return super.charTyped(input);
	}

	@Override
	public void onClose() {
		search.setFocused(false);
		minecraft.setScreenAndShow(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private static final class CategoryButton {
		private final HubFeature.@Nullable Group group;
		private final Component label;
		private final @Nullable Identifier icon;
		private int x;
		private int y;
		private int width;

		private CategoryButton(HubFeature.@Nullable Group group, Component label, @Nullable Identifier icon) {
			this.group = group;
			this.label = label;
			this.icon = icon;
		}
	}

	private record Group(HubFeature.Group group, List<HubFeature> features) {
	}

	private record CardBox(HubFeature feature, int x, int y, int width, int height, int controlX, int controlY, int controlWidth, int controlHeight) {
	}
}
