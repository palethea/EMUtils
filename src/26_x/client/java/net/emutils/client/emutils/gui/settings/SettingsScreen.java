package net.emutils.client.emutils.gui.settings;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.hub.HubFeature;
import net.emutils.client.emutils.gui.hub.HubFeatureCatalog;
import net.emutils.client.emutils.gui.hub.HubIcons;
import net.emutils.client.emutils.gui.ui.UiAnim;
import net.emutils.client.emutils.gui.ui.UiIcons;
import net.emutils.client.emutils.gui.ui.UiOpacity;
import net.emutils.client.emutils.gui.ui.UiPanelScreen;
import net.emutils.client.emutils.gui.ui.UiScrollArea;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.emutils.gui.ui.UiTextField;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.gui.ui.UiWidgets;
import net.emutils.client.emutils.profile.gui.ProfileBadge;
import net.emutils.client.emutils.profile.gui.ProfileMenu;
import net.emutils.client.emutils.profile.gui.ProfilesScreen;
import net.emutils.client.emutils.util.EMUtilsBuild;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

/** The EMUtils settings screen (#88). */
public final class SettingsScreen extends UiPanelScreen {
	private static final int PADDING = 16;
	private static final int CONTROL_RADIUS = 10;
	private static final int SEARCH_ROW = 22;
	private static final int CATEGORY_ROW = 20;
	private static final int CATEGORY_BUTTON_HEIGHT = 15;
	private static final int ROUND_BUTTON = 20;
	private static final int PROFILE_BUTTON_WIDTH = 34;
	private static final int PROFILE_BADGE = 14;
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
	private static final float CATEGORY_SECONDS = 0.2F;
	private static final float LIST_SECONDS = 0.2F;

	/** Built for {@link #featuresConfig}; switching profiles swaps the config, and the features with it. */
	private List<HubFeature> features;
	private EMUtilsConfig featuresConfig;
	private @Nullable ProfileMenu profileMenu;
	private int profileButtonX;
	private final UiTextField search = new UiTextField(this, 64);
	private final UiScrollArea scroll = new UiScrollArea();
	private final KeybindCapture capture = new KeybindCapture();
	private final List<CategoryButton> categoryButtons = new ArrayList<>();
	private final List<CardBox> cards = new ArrayList<>();
	private HubFeature.@Nullable Group selectedGroup;
	private @Nullable SettingsSheet sheet;
	private int controlX;
	private int controlY;
	private int controlWidth;
	private int themeButtonX;
	private int rightButtonsY;
	private int titleY;
	private boolean stackedHeader;
	private boolean showTagline;

	public SettingsScreen(Screen parent) {
		super(Component.translatable(EMUtilsTexts.HUB_MODERN_TITLE), parent);
		this.features = HubFeatureCatalog.all();
		this.featuresConfig = EMUtilsClient.config();
	}

	@Override
	protected void layout() {
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

		int rightButtonsWidth = ROUND_BUTTON + 6 + PROFILE_BUTTON_WIDTH;
		int titleBlockWidth = titleBlockWidth();
		int centeredX = panelX + (panelWidth - controlWidth) / 2;
		stackedHeader = centeredX < panelX + PADDING + titleBlockWidth + 12
			|| centeredX + controlWidth > panelX + panelWidth - PADDING - rightButtonsWidth - 12;
		// The tagline is the first thing to go when the title column gets narrow.
		int taglineWidth = UiText.width(font, Component.translatable(EMUtilsTexts.UI_TAGLINE), UiText.Size.BODY);
		// When the header stacks, the tagline goes too, to leave room for the cards.
		showTagline = !stackedHeader && centeredX >= panelX + PADDING + taglineWidth + 14;

		themeButtonX = panelX + panelWidth - PADDING - ROUND_BUTTON;
		profileButtonX = themeButtonX - 6 - PROFILE_BUTTON_WIDTH;
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
		search.restoreFocus();
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
	protected void beforeFrame() {
		capture.frame();
		// The features' switches and sheets point at the settings of the profile they were built for.
		if (EMUtilsClient.config() != featuresConfig) {
			features = HubFeatureCatalog.all();
			featuresConfig = EMUtilsClient.config();
			sheet = null;
		}
	}

	@Override
	protected void drawPanel(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		// While a sheet or the profile list is open, nothing underneath reacts to the mouse.
		boolean interactive = sheet == null && profileMenu == null;
		int backX = interactive ? mouseX : Integer.MIN_VALUE / 2;
		int backY = interactive ? mouseY : Integer.MIN_VALUE / 2;
		drawTitle(context, theme);
		drawControl(context, theme, backX, backY);
		drawRightButtons(context, theme, backX, backY);
		drawCards(context, theme, backX, backY);
	}

	@Override
	protected void drawOverlay(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		if (profileMenu != null) {
			profileMenu.render(context, theme, mouseX, mouseY, width, height);
			if (profileMenu.isClosed()) {
				profileMenu = null;
			}
		} else if (sheet == null && !closing() && contains(mouseX, mouseY, profileButtonX, rightButtonsY, PROFILE_BUTTON_WIDTH, ROUND_BUTTON)) {
			Component tip = Component.translatable(EMUtilsTexts.UI_PROFILE_BUTTON, EMUtilsClient.profiles().active().name());
			UiWidgets.tooltip(context, font, theme, tip, mouseX, mouseY, width, height);
		}
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
		context.fill(controlX + 1, controlY + SEARCH_ROW, controlX + controlWidth - 1, controlY + SEARCH_ROW + 1, UiOpacity.apply(theme.line()));

		int buttonsWidth = categoryButtons.stream().mapToInt(button -> button.width).sum() + (categoryButtons.size() - 1) * 2;
		int x = controlX + (controlWidth - buttonsWidth) / 2;
		int y = controlY + SEARCH_ROW + 1 + (CATEGORY_ROW - CATEGORY_BUTTON_HEIGHT) / 2;
		int selectedIndex = 0;
		for (int i = 0; i < categoryButtons.size(); i++) {
			CategoryButton button = categoryButtons.get(i);
			button.x = x;
			button.y = y;
			if (button.group == selectedGroup) {
				selectedIndex = i;
			}
			x += button.width + 2;
		}

		// The highlight slides between buttons; its position may fall between two of them.
		float slide = anim.transition("category-slide", selectedIndex, CATEGORY_SECONDS);
		for (int i = 0; i < categoryButtons.size(); i++) {
			CategoryButton button = categoryButtons.get(i);
			float hover = anim.towards("category:" + i, contains(mouseX, mouseY, button.x, button.y, button.width, CATEGORY_BUTTON_HEIGHT), 16.0F);
			float covered = Math.clamp(1.0F - Math.abs(slide - i), 0.0F, 1.0F);
			UiShapes.roundedRect(context, button.x, button.y, button.width, CATEGORY_BUTTON_HEIGHT, 7, UiTheme.fade(theme.hover(), hover * (1.0F - covered)));
		}
		int from = (int) Math.floor(slide);
		int to = Math.min(categoryButtons.size() - 1, from + 1);
		float blend = slide - from;
		CategoryButton fromButton = categoryButtons.get(from);
		CategoryButton toButton = categoryButtons.get(to);
		float left = fromButton.x + (toButton.x - fromButton.x) * blend;
		float right = fromButton.x + fromButton.width + (toButton.x + toButton.width - fromButton.x - fromButton.width) * blend;
		int wholeLeft = (int) Math.floor(left);
		context.pose().pushMatrix();
		context.pose().translate(left - wholeLeft, 0.0F);
		UiShapes.roundedRect(context, wholeLeft, y, Math.round(right - left), CATEGORY_BUTTON_HEIGHT, 7, theme.selectedBackground());
		context.pose().popMatrix();

		for (int i = 0; i < categoryButtons.size(); i++) {
			CategoryButton button = categoryButtons.get(i);
			// Labels switch color as the highlight passes under them.
			float covered = Math.clamp(1.0F - Math.abs(slide - i), 0.0F, 1.0F);
			int color = UiTheme.mix(theme.textSecondary(), theme.selectedText(), covered);
			int textX = button.x + 6;
			if (button.icon != null) {
				UiIcons.draw(context, button.icon, textX, y + (CATEGORY_BUTTON_HEIGHT - 9) / 2, 9, color);
				textX += 13;
			}
			UiText.drawCentered(context, font, button.label, UiText.Size.LABEL, textX, y + CATEGORY_BUTTON_HEIGHT / 2, color);
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
		// The profile switcher: the active profile's icon and a chevron, like the mockup.
		boolean menuOpen = profileMenu != null;
		float profileHover = anim.towards("profile-button", menuOpen || contains(mouseX, mouseY, profileButtonX, rightButtonsY, PROFILE_BUTTON_WIDTH, ROUND_BUTTON), 16.0F);
		UiShapes.roundedRect(context, profileButtonX, rightButtonsY, PROFILE_BUTTON_WIDTH, ROUND_BUTTON, ROUND_BUTTON / 2, UiTheme.mix(theme.surface(), theme.surfaceHover(), profileHover));
		int badgeOffset = (ROUND_BUTTON - PROFILE_BADGE) / 2;
		ProfileBadge.draw(context, EMUtilsClient.profiles().active(), profileButtonX + badgeOffset, rightButtonsY + badgeOffset, PROFILE_BADGE);
		UiIcons.draw(context, HubIcons.CHEVRON_DOWN, profileButtonX + PROFILE_BUTTON_WIDTH - 13, rightButtonsY + (ROUND_BUTTON - 8) / 2, 8, theme.muted());

		// The sun (switch to light) crossfades into the moon (switch to dark) along with the theme.
		float themeHover = anim.towards("theme-button", contains(mouseX, mouseY, themeButtonX, rightButtonsY, ROUND_BUTTON, ROUND_BUTTON), 16.0F);
		UiWidgets.iconButton(context, theme, themeButtonX, rightButtonsY, ROUND_BUTTON, HubIcons.SUN, HubIcons.MOON, lightness(), themeHover);
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
		scroll.animate(anim, "scroll", mouseX, mouseY);

		cards.clear();
		boolean mouseInList = scroll.contains(mouseX, mouseY) && !scroll.dragging();
		scroll.begin(context);
		// After picking another category, the cards fade and rise in instead of popping.
		float listIn = anim.transition("list", 1.0F, LIST_SECONDS, true);
		UiOpacity.set(openProgress() * listIn);
		// Cards are laid out at whole pixels and the leftover fraction of the scroll offset is applied
		// as a translation, so scrolling glides smoothly while clicks still use whole positions.
		context.pose().pushMatrix();
		context.pose().translate(0.0F, scroll.offset() - scroll.exactOffset() + (1.0F - listIn) * 6.0F);
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
		UiOpacity.set(openProgress());
		scroll.end(context, theme.panel(), FADE_HEIGHT, UiTheme.fade(theme.text(), 0.25F), UiTheme.fade(theme.text(), 0.45F));
	}

	/** Draws one feature card and returns where it and its control are, for clicks. */
	private CardBox drawCard(GuiGraphicsExtractor context, UiTheme theme, HubFeature feature, int x, int y, int width, boolean mouseInList, int mouseX, int mouseY) {
		boolean hovered = mouseInList && contains(mouseX, mouseY, x, y, width, CARD_HEIGHT);
		float hover = anim.towards("card:" + feature.id(), hovered, 16.0F);
		// Hovered cards rise a little onto a soft shadow, like the mockup. Clicks keep using the resting position.
		context.pose().pushMatrix();
		context.pose().translate(0.0F, -hover);
		UiShapes.shadow(context, x, y + 2, width, CARD_HEIGHT, CARD_RADIUS, 8, UiTheme.fade(theme.shadow(), hover * 0.9F));
		UiShapes.borderedRect(context, x, y, width, CARD_HEIGHT, CARD_RADIUS, UiTheme.mix(theme.surface(), theme.surfaceHover(), hover), theme.border());

		int rowCenter = y + CARD_PADDING + 6;
		UiIcons.draw(context, feature.icon().texture(), x + CARD_PADDING, rowCenter - CARD_ICON / 2, CARD_ICON, theme.text());

		// Top row: the name, then the Open button and the switch on the right.
		int controlRight = x + width - CARD_PADDING;
		Box switchBox = null;
		Box openBox = null;
		if (feature.toggle() != null) {
			int switchX = controlRight - UiWidgets.SWITCH_WIDTH;
			int switchY = rowCenter - UiWidgets.SWITCH_HEIGHT / 2;
			boolean on = feature.toggle().getter().getAsBoolean();
			float progress = anim.transition("switch:" + feature.id(), on, 0.18F);
			float switchHover = hovered && contains(mouseX, mouseY, switchX, switchY, UiWidgets.SWITCH_WIDTH, UiWidgets.SWITCH_HEIGHT) ? 1.0F : 0.0F;
			UiWidgets.toggle(context, theme, switchX, switchY, progress, switchHover);
			switchBox = new Box(switchX, switchY, UiWidgets.SWITCH_WIDTH, UiWidgets.SWITCH_HEIGHT);
			controlRight = switchX - 6;
		}
		if (feature.primaryAction() != null) {
			Component open = Component.translatable(EMUtilsTexts.UI_OPEN);
			int buttonWidth = UiWidgets.buttonWidth(font, open);
			int buttonX = controlRight - buttonWidth;
			int buttonY = rowCenter - OPEN_BUTTON_HEIGHT / 2;
			boolean enabled = feature.primaryActionEnabled();
			float buttonHover = enabled && hovered && contains(mouseX, mouseY, buttonX, buttonY, buttonWidth, OPEN_BUTTON_HEIGHT) ? 1.0F : 0.0F;
			UiShapes.roundedRect(context, buttonX, buttonY, buttonWidth, OPEN_BUTTON_HEIGHT, 7, UiTheme.mix(theme.surfaceAlt(), theme.segmentSelected(), buttonHover));
			UiText.drawCentered(context, font, open, UiText.Size.LABEL, buttonX + 9, rowCenter, enabled ? theme.text() : theme.muted());
			openBox = new Box(buttonX, buttonY, buttonWidth, OPEN_BUTTON_HEIGHT);
			controlRight = buttonX - 6;
		}

		int nameX = x + CARD_PADDING + CARD_ICON + 6;
		Component name = UiText.ellipsize(font, title(feature), UiText.Size.BOLD, controlRight - nameX);
		UiText.drawCentered(context, font, name, UiText.Size.BOLD, nameX, rowCenter, theme.text());

		// Bottom row: the description, with a needs-another-mod badge on the right.
		int bottomCenter = y + CARD_HEIGHT - CARD_PADDING - 3;
		int descriptionRight = x + width - CARD_PADDING;
		if (feature.missingMod() != null) {
			Component badge = Component.translatable(EMUtilsTexts.UI_NEEDS_MOD, feature.missingMod());
			int badgeWidth = UiText.width(font, badge, UiText.Size.SMALL) + 8;
			int badgeHeight = UiText.lineHeight(font, UiText.Size.SMALL) + 5;
			UiWidgets.badge(context, font, descriptionRight - badgeWidth, bottomCenter - badgeHeight / 2, badge, theme.devBackground(), theme.devText());
			descriptionRight -= badgeWidth + 6;
		}
		Component description = UiText.ellipsize(font, Component.translatable(feature.descriptionKey()), UiText.Size.BODY, descriptionRight - x - CARD_PADDING);
		UiText.drawCentered(context, font, description, UiText.Size.BODY, x + CARD_PADDING, bottomCenter, theme.muted());
		context.pose().popMatrix();
		return new CardBox(feature, x, y, width, CARD_HEIGHT, switchBox, openBox);
	}

	/** Whether the feature's sheet has anything to show: settings or keybinds. */
	private static boolean hasSheetContent(HubFeature feature) {
		return feature.category() != null || (feature.rows() != null && !feature.rows().isEmpty()) || !feature.keyNames().isEmpty();
	}

	/**
	 * The feature's name without a trailing "...", which some names carry to mark a submenu; here every
	 * card opens its settings the same way.
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
		if (closing()) {
			return true;
		}
		if (capture.mouseClicked(click.button())) {
			return true;
		}
		double mouseX = click.x();
		double mouseY = click.y();
		if (profileMenu != null) {
			if (!profileMenu.mouseClicked(mouseX, mouseY)) {
				profileMenu = null;
			}
			return true;
		}
		if (sheet != null) {
			return sheet.mouseClicked(mouseX, mouseY, click.button());
		}
		// Mouse button numbers differ between versions (26.3 uses SDL's, where left is 1), so compare
		// against the constants rather than raw numbers.
		if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) {
			return super.mouseClicked(click, doubled);
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
					if (selectedGroup != button.group) {
						anim.snap("list", 0.0F);
					}
					selectedGroup = button.group;
					scroll.reset();
					return true;
				}
			}
			return true;
		}
		if (contains(mouseX, mouseY, profileButtonX, rightButtonsY, PROFILE_BUTTON_WIDTH, ROUND_BUTTON)) {
			openProfileMenu();
			return true;
		}
		if (contains(mouseX, mouseY, themeButtonX, rightButtonsY, ROUND_BUTTON, ROUND_BUTTON)) {
			EMUtilsClient.config().setSettingsUiDark(UiTheme.current() != UiTheme.DARK);
			return true;
		}
		if (scroll.mouseClicked(mouseX, mouseY)) {
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

	private void openProfileMenu() {
		search.setFocused(false);
		profileMenu = new ProfileMenu(
			font,
			anim,
			profileButtonX + PROFILE_BUTTON_WIDTH,
			rightButtonsY + ROUND_BUTTON + 4,
			profile -> EMUtilsClient.profiles().pick(profile),
			() -> minecraft.gui.setScreen(new ProfilesScreen(this))
		);
	}

	/**
	 * The switch toggles the feature and Open opens its screen. The rest of the card opens the settings
	 * sheet when there's something to set up there (settings or keybinds), and the feature's screen
	 * otherwise.
	 */
	private void clickCard(CardBox card, double mouseX, double mouseY) {
		HubFeature feature = card.feature();
		if (card.switchBox() != null && card.switchBox().contains(mouseX, mouseY)) {
			feature.toggle().setter().accept(!feature.toggle().getter().getAsBoolean());
		} else if (card.openBox() != null && card.openBox().contains(mouseX, mouseY)) {
			if (feature.primaryActionEnabled()) {
				feature.primaryAction().run();
			}
		} else if (hasSheetContent(feature) || feature.primaryAction() == null) {
			search.setFocused(false);
			sheet = new SettingsSheet(font, anim, feature, capture);
		} else if (feature.primaryActionEnabled()) {
			feature.primaryAction().run();
		}
	}

	/** Closes any sheet and searches for {@code text}; used by UI snapshots. */
	public void searchFor(String text) {
		sheet = null;
		search.setText(text);
		scroll.reset();
	}

	/**
	 * Left-clicks the dark/light button through {@link #mouseClicked}, with this version's left button
	 * number; used by UI snapshots to catch clicks being dropped. Returns whether the theme switched.
	 */
	public boolean clickThemeButton() {
		boolean dark = EMUtilsClient.config().settingsUiDark();
		MouseButtonInfo left = new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0);
		mouseClicked(new MouseButtonEvent(themeButtonX + ROUND_BUTTON / 2.0, rightButtonsY + ROUND_BUTTON / 2.0, left), false);
		mouseReleased(new MouseButtonEvent(themeButtonX + ROUND_BUTTON / 2.0, rightButtonsY + ROUND_BUTTON / 2.0, left));
		boolean switched = EMUtilsClient.config().settingsUiDark() != dark;
		EMUtilsClient.config().setSettingsUiDark(dark);
		return switched;
	}

	/** Opens the profile switcher's list; used by UI snapshots. */
	public void openProfileMenuForSnapshot() {
		openProfileMenu();
	}

	/** Scrolls the profile switcher's list to its end; used by UI snapshots. Returns whether it scrolls. */
	public boolean scrollProfileMenuForSnapshot() {
		return profileMenu != null && profileMenu.scrollToEndForSnapshot();
	}

	/** Whether a settings sheet is open; used by UI snapshots. */
	public boolean sheetOpen() {
		return sheet != null;
	}

	/** Waits for a new key for the open sheet's first keybind; used by UI snapshots. */
	public void listenForKeyInSheet() {
		if (sheet != null) {
			sheet.listenForFirstKey();
		}
	}

	/** Opens the settings sheet of the feature with this id, if there is one; used by UI snapshots. */
	public void openSheet(String featureId) {
		for (HubFeature feature : features) {
			if (feature.id().equals(featureId)) {
				sheet = new SettingsSheet(font, anim, feature, capture);
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
		if (closing()) {
			return true;
		}
		if (profileMenu != null) {
			profileMenu.mouseDragged(click.y());
			return true;
		}
		if (sheet != null) {
			return sheet.mouseDragged(click.x(), click.y());
		}
		if (scroll.mouseDragged(click.y())) {
			return true;
		}
		return super.mouseDragged(click, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		if (profileMenu != null) {
			profileMenu.mouseReleased();
			return true;
		}
		if (sheet != null) {
			return sheet.mouseReleased();
		}
		if (scroll.mouseReleased()) {
			return true;
		}
		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (closing()) {
			return true;
		}
		if (profileMenu != null) {
			if (!profileMenu.mouseScrolled(mouseX, mouseY, verticalAmount)) {
				profileMenu = null;
			}
			return true;
		}
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
		if (closing()) {
			return true;
		}
		if (capture.keyPressed(input)) {
			return true;
		}
		if (profileMenu != null) {
			if (input.isEscape()) {
				profileMenu = null;
			}
			return true;
		}
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
		// The character of a key that was just bound is not typed anywhere.
		if (closing() || capture.swallowChar()) {
			return true;
		}
		if (sheet != null) {
			return sheet.charTyped(input);
		}
		if (search.charTyped(input, scroll::reset)) {
			return true;
		}
		return super.charTyped(input);
	}

	/** Fades and scales the panel out, then returns to the previous screen. */
	@Override
	public void onClose() {
		search.setFocused(false);
		if (sheet != null) {
			sheet.close();
		}
		super.onClose();
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

	private record CardBox(HubFeature feature, int x, int y, int width, int height, @Nullable Box switchBox, @Nullable Box openBox) {
	}

	/** Where a control on a card is; clicks within 2 pixels of it count, so small controls are easy to hit. */
	private record Box(int x, int y, int width, int height) {
		private boolean contains(double mouseX, double mouseY) {
			return SettingsScreen.contains(mouseX, mouseY, x - 2, y - 2, width + 4, height + 4);
		}
	}
}
