package net.emutils.client.emutils.gui.settings;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.commandshortcuts.CommandShortcut;
import net.emutils.client.emutils.commandshortcuts.gui.CommandShortcutsScreen;
import net.emutils.client.emutils.compat.MinescriptCompat;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.hub.HubFeature;
import net.emutils.client.emutils.gui.hub.HubFeatureCatalog;
import net.emutils.client.emutils.gui.hub.HubIcons;
import net.emutils.client.emutils.gui.ui.UiConfirmDialog;
import net.emutils.client.emutils.gui.ui.UiIcons;
import net.emutils.client.emutils.gui.ui.UiPanelScreen;
import net.emutils.client.emutils.gui.ui.UiScrollArea;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.emutils.gui.ui.UiTextField;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.gui.ui.UiWidgets;
import net.emutils.client.emutils.minescript.MinescriptKeyBinding;
import net.emutils.client.emutils.minescript.MinescriptKeybindStore;
import net.emutils.client.emutils.minescript.gui.ScriptsScreen;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Every EMUtils key on one page (#155), grouped like the settings (Render, HUD, Utility, Management, QoL)
 * plus General for keys no feature lists, such as Open Settings. Each row links to its feature's sheet
 * and edits the same {@link KeyMapping} as that sheet's Keybinds section, so the two always agree. Script
 * keybinds and command shortcuts use their own key combos; they are listed read-only, with a link to
 * their screen.
 */
public final class KeybindsScreen extends UiPanelScreen {
	private static final String KEY_PREFIX = "key.emutils.";
	private static final int PADDING = 16;
	private static final int HEADER_BUTTON_HEIGHT = 20;
	private static final int TOOLBAR_HEIGHT = 22;
	private static final int ROW_HEIGHT = 36;
	private static final int ROW_GAP = 6;
	private static final int ROW_RADIUS = 9;
	private static final int ROW_PADDING = 12;
	private static final int HEADING_HEIGHT = 18;
	private static final int SECTION_GAP = 10;
	private static final int FADE_HEIGHT = 12;

	private final UiTextField search = new UiTextField(this, 64);
	private final UiScrollArea scroll = new UiScrollArea();
	private final KeybindCapture capture = new KeybindCapture();
	private final List<RowBox> rows = new ArrayList<>();
	private List<Section> sections = List.of();
	/** Built for {@link #sectionsConfig}; switching profiles swaps the config, and the features' sheets with it. */
	private @Nullable EMUtilsConfig sectionsConfig;
	/**
	 * Set when the page is shown again, such as after coming back from the Script Manager or Command
	 * Shortcuts, whose key combos may have changed there.
	 */
	private boolean sectionsStale = true;
	private boolean conflictsOnly;
	private @Nullable SettingsSheet sheet;
	private @Nullable UiConfirmDialog dialog;
	private int headerButtonsY;
	private int resetX;
	private int resetWidth;
	private int toolbarY;
	private int searchWidth;
	private int chipX;
	private int chipWidth;
	private @Nullable Component tooltip;
	private int tooltipX;
	private int tooltipY;
	/** A key whose × UI snapshots show hovered, since they can't move the mouse. */
	private @Nullable KeyMapping snapshotHover;

	public KeybindsScreen(@Nullable Screen parent) {
		super(Component.translatable(EMUtilsTexts.SCREEN_KEYBINDS), parent);
	}

	@Override
	protected int maxPanelWidth() {
		return 560;
	}

	@Override
	protected void layout() {
		int headerHeight = UiText.lineHeight(font, UiText.Size.HEADING) + 6 + UiText.lineHeight(font, UiText.Size.BODY);
		headerButtonsY = panelY + PADDING + (headerHeight - HEADER_BUTTON_HEIGHT) / 2;
		toolbarY = panelY + PADDING + headerHeight + 12;
		int bodyY = toolbarY + TOOLBAR_HEIGHT + 8;
		scroll.setBounds(panelX + PADDING, bodyY, panelWidth - PADDING * 2 + UiScrollArea.GUTTER, panelY + panelHeight - PADDING / 2 - bodyY);
		search.restoreFocus();
		sectionsStale = true;
	}

	@Override
	protected void beforeFrame() {
		capture.frame();
		if (EMUtilsClient.config() != sectionsConfig) {
			sheet = null;
			sectionsStale = true;
		}
		if (sectionsStale) {
			sections = buildSections();
			sectionsConfig = EMUtilsClient.config();
			sectionsStale = false;
		}
	}

	// ---- the list -------------------------------------------------------------------------------

	/** Every EMUtils key, by the feature that lists it, then General, then the read-only key combos. */
	private List<Section> buildSections() {
		List<HubFeature> features = HubFeatureCatalog.all();
		Map<HubFeature.Group, List<Entry>> byGroup = new LinkedHashMap<>();
		for (HubFeature.Group group : HubFeature.Group.values()) {
			byGroup.put(group, new ArrayList<>());
		}
		List<String> listed = new ArrayList<>();
		for (HubFeature feature : features) {
			for (String name : feature.keyNames()) {
				KeyMapping key = KeybindCapture.mapping(name);
				if (key != null && !listed.contains(name)) {
					listed.add(name);
					byGroup.get(feature.group()).add(Entry.key(key, feature));
				}
			}
		}
		List<Entry> general = new ArrayList<>();
		for (KeyMapping key : minecraft.options.keyMappings) {
			if (key.getName().startsWith(KEY_PREFIX) && !listed.contains(key.getName())) {
				general.add(Entry.key(key, null));
			}
		}

		List<Section> built = new ArrayList<>();
		for (Map.Entry<HubFeature.Group, List<Entry>> group : byGroup.entrySet()) {
			if (!group.getValue().isEmpty()) {
				built.add(new Section(Component.translatable(group.getKey().labelKey()), group.getKey(), group.getValue()));
			}
		}
		if (!general.isEmpty()) {
			built.add(new Section(Component.translatable(EMUtilsTexts.UI_KEYBINDS_GENERAL), null, general));
		}

		List<Entry> scripts = new ArrayList<>();
		Component scriptManager = Component.translatable(EMUtilsTexts.SCREEN_SCRIPT_MANAGER);
		for (MinescriptKeyBinding binding : MinescriptKeybindStore.load().bindings()) {
			Runnable open = MinescriptCompat.isLoaded() ? () -> minecraft.gui.setScreen(new ScriptsScreen(this)) : null;
			scripts.add(Entry.combo(Component.literal(binding.command()), Component.literal(binding.displayName()), scriptManager, open));
		}
		if (!scripts.isEmpty()) {
			built.add(new Section(Component.translatable(EMUtilsTexts.UI_KEYBINDS_SCRIPTS), null, scripts));
		}

		List<Entry> shortcuts = new ArrayList<>();
		Component shortcutsScreen = Component.translatable(EMUtilsTexts.OPTION_COMMAND_SHORTCUTS);
		for (CommandShortcut shortcut : EMUtilsClient.commandShortcuts().store().shortcuts()) {
			Runnable open = () -> minecraft.gui.setScreen(new CommandShortcutsScreen(this));
			shortcuts.add(Entry.combo(Component.literal(shortcut.displayName()), Component.literal(shortcut.keyCombo().displayName()), shortcutsScreen, open));
		}
		if (!shortcuts.isEmpty()) {
			built.add(new Section(shortcutsScreen, null, shortcuts));
		}
		return built;
	}

	/** The EMUtils keys this page edits, for Reset all. */
	private List<KeyMapping> editableKeys() {
		List<KeyMapping> keys = new ArrayList<>();
		for (Section section : sections) {
			for (Entry entry : section.entries()) {
				if (entry.key() != null) {
					keys.add(entry.key());
				}
			}
		}
		return keys;
	}

	private int conflictCount() {
		int count = 0;
		for (KeyMapping key : editableKeys()) {
			if (KeybindCapture.clashes(key)) {
				count++;
			}
		}
		return count;
	}

	/** The sections with only the rows that match the search and the Conflicts only filter. */
	private List<Section> visibleSections() {
		String query = search.text().toLowerCase(Locale.ROOT).strip();
		List<Section> visible = new ArrayList<>();
		for (Section section : sections) {
			List<Entry> matches = new ArrayList<>();
			for (Entry entry : section.entries()) {
				if (conflictsOnly && (entry.key() == null || !KeybindCapture.clashes(entry.key()))) {
					continue;
				}
				if (query.isEmpty() || matches(entry, query)) {
					matches.add(entry);
				}
			}
			if (!matches.isEmpty()) {
				visible.add(new Section(section.label(), section.group(), matches));
			}
		}
		return visible;
	}

	private boolean matches(Entry entry, String query) {
		return contains(name(entry), query) || contains(keyLabel(entry), query) || (entry.linkLabel() != null && contains(entry.linkLabel(), query));
	}

	private static boolean contains(Component text, String query) {
		return text.getString().toLowerCase(Locale.ROOT).contains(query);
	}

	private static Component name(Entry entry) {
		return entry.key() != null ? Component.translatable(entry.key().getName()) : entry.name();
	}

	private Component keyLabel(Entry entry) {
		return entry.key() != null ? capture.label(entry.key()) : entry.keyLabel();
	}

	// ---- drawing --------------------------------------------------------------------------------

	@Override
	protected void drawPanel(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		tooltip = null;
		boolean interactive = sheet == null && dialog == null && !closing();
		int hoverX = interactive ? mouseX : Integer.MIN_VALUE / 2;
		int hoverY = interactive ? mouseY : Integer.MIN_VALUE / 2;
		drawHeader(context, theme, hoverX, hoverY);
		drawToolbar(context, theme, hoverX, hoverY);
		drawRows(context, theme, hoverX, hoverY);
	}

	private void drawHeader(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		int left = panelX + PADDING;
		int top = panelY + PADDING;
		UiText.draw(context, font, title, UiText.Size.HEADING, left, top, theme.text());

		Component reset = Component.translatable(EMUtilsTexts.UI_KEYBINDS_RESET_ALL);
		int iconSize = 9;
		resetWidth = UiText.width(font, reset, UiText.Size.LABEL) + iconSize + 4 + 20;
		resetX = panelX + panelWidth - PADDING - resetWidth;
		boolean resetHovered = contains(mouseX, mouseY, resetX, headerButtonsY, resetWidth, HEADER_BUTTON_HEIGHT);
		UiShapes.roundedRect(context, resetX, headerButtonsY, resetWidth, HEADER_BUTTON_HEIGHT, 8, UiTheme.fade(theme.hover(), resetHovered ? 1.0F : 0.0F));
		UiIcons.draw(context, HubIcons.REFRESH_CW, resetX + 10, headerButtonsY + (HEADER_BUTTON_HEIGHT - iconSize) / 2, iconSize, theme.textSecondary());
		UiText.drawCentered(context, font, reset, UiText.Size.LABEL, resetX + 10 + iconSize + 4, headerButtonsY + HEADER_BUTTON_HEIGHT / 2, theme.textSecondary());

		int keys = editableKeys().size();
		int conflicts = conflictCount();
		Component subtitle = Component.translatable(EMUtilsTexts.UI_KEYBINDS_COUNT, keys);
		if (conflicts > 0) {
			subtitle = Component.translatable(EMUtilsTexts.UI_KEYBINDS_COUNT_WITH_CONFLICTS, keys, conflictsLabel(conflicts));
		}
		int subtitleY = top + UiText.lineHeight(font, UiText.Size.HEADING) + 6;
		UiText.draw(context, font, UiText.ellipsize(font, subtitle, UiText.Size.BODY, resetX - 12 - left), UiText.Size.BODY, left, subtitleY, theme.muted());
	}

	private static Component conflictsLabel(int count) {
		return Component.translatable(count == 1 ? EMUtilsTexts.UI_KEYBINDS_CONFLICT_ONE : EMUtilsTexts.UI_KEYBINDS_CONFLICTS, count);
	}

	/** The search box, and the Conflicts only switch with how many there are. */
	private void drawToolbar(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		int left = panelX + PADDING;
		int right = panelX + panelWidth - PADDING;
		int conflicts = conflictCount();
		Component chip = Component.translatable(EMUtilsTexts.UI_KEYBINDS_CONFLICTS_ONLY);
		Component count = Component.literal(Integer.toString(conflicts));
		int countWidth = UiText.width(font, count, UiText.Size.SMALL) + 8;
		chipWidth = 10 + UiText.width(font, chip, UiText.Size.LABEL) + 6 + countWidth + 6;
		chipX = right - chipWidth;
		float on = anim.transition("keybinds-conflicts", conflictsOnly, 0.18F);
		float chipHover = anim.towards("keybinds-conflicts-hover", contains(mouseX, mouseY, chipX, toolbarY, chipWidth, TOOLBAR_HEIGHT), 16.0F);
		int chipBackground = UiTheme.mix(UiTheme.mix(theme.surface(), theme.surfaceHover(), chipHover), UiTheme.mix(theme.surface(), theme.accent(), 0.22F), on);
		UiShapes.borderedRect(context, chipX, toolbarY, chipWidth, TOOLBAR_HEIGHT, 10, chipBackground, UiTheme.mix(theme.border(), theme.accent(), on));
		int center = toolbarY + TOOLBAR_HEIGHT / 2;
		UiText.drawCentered(context, font, chip, UiText.Size.LABEL, chipX + 10, center, UiTheme.mix(theme.textSecondary(), theme.text(), on));
		int countX = chipX + chipWidth - 6 - countWidth;
		int countHeight = UiText.lineHeight(font, UiText.Size.SMALL) + 5;
		int countBackground = conflicts > 0 ? UiTheme.fade(theme.warning(), 0.18F) : theme.segmentBackground();
		UiWidgets.badge(context, font, countX, center - countHeight / 2, count, countBackground, conflicts > 0 ? theme.warning() : theme.muted());

		searchWidth = chipX - 8 - left;
		UiShapes.borderedRect(context, left, toolbarY, searchWidth, TOOLBAR_HEIGHT, 10, theme.surface(), theme.border());
		UiIcons.draw(context, HubIcons.SEARCH, left + 10, center - 5, 10, theme.textSecondary());
		search.draw(context, font, theme, left + 26, center, searchWidth - 34, Component.translatable(EMUtilsTexts.UI_KEYBINDS_SEARCH));
	}

	private void drawRows(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		List<Section> visible = visibleSections();
		int contentHeight = 0;
		for (Section section : visible) {
			contentHeight += HEADING_HEIGHT + section.entries().size() * (ROW_HEIGHT + ROW_GAP) - ROW_GAP + SECTION_GAP;
		}
		scroll.setContentHeight(Math.max(0, contentHeight - SECTION_GAP + FADE_HEIGHT));
		scroll.animate(anim, "keybinds-scroll", mouseX, mouseY);
		rows.clear();
		boolean mouseInList = scroll.contains(mouseX, mouseY) && !scroll.dragging();
		int rowMouseX = mouseInList ? mouseX : Integer.MIN_VALUE / 2;
		scroll.begin(context);
		context.pose().pushMatrix();
		context.pose().translate(0.0F, scroll.offset() - scroll.exactOffset());
		int width = scroll.contentWidth();
		int y = scroll.y() + FADE_HEIGHT / 2 - scroll.offset();
		for (Section section : visible) {
			int headingCenter = y + HEADING_HEIGHT / 2 - 2;
			int dot = section.group() != null ? theme.groupColor(section.group()) : theme.muted();
			UiShapes.circle(context, scroll.x() + 2, headingCenter - 2, 5, dot);
			UiText.drawCentered(context, font, section.label(), UiText.Size.BOLD, scroll.x() + 11, headingCenter, theme.text());
			int labelWidth = UiText.width(font, section.label(), UiText.Size.BOLD);
			UiText.drawCentered(context, font, Component.literal(Integer.toString(section.entries().size())), UiText.Size.BODY, scroll.x() + 16 + labelWidth, headingCenter, theme.muted());
			y += HEADING_HEIGHT;
			for (Entry entry : section.entries()) {
				if (y + ROW_HEIGHT >= scroll.y() && y <= scroll.y() + scroll.height()) {
					rows.add(drawRow(context, theme, entry, scroll.x(), y, width, rowMouseX, mouseY));
				}
				y += ROW_HEIGHT + ROW_GAP;
			}
			y += SECTION_GAP - ROW_GAP;
		}
		context.pose().popMatrix();
		if (visible.isEmpty()) {
			Component empty = conflictsOnly && search.text().isBlank()
				? Component.translatable(EMUtilsTexts.UI_KEYBINDS_NO_CONFLICTS)
				: Component.translatable(EMUtilsTexts.UI_NO_RESULTS, search.text());
			int emptyWidth = UiText.width(font, empty, UiText.Size.BODY);
			UiText.draw(context, font, empty, UiText.Size.BODY, scroll.x() + (width - emptyWidth) / 2, scroll.y() + 40, theme.muted());
		}
		scroll.end(context, theme.panel(), FADE_HEIGHT, UiTheme.fade(theme.text(), 0.25F), UiTheme.fade(theme.text(), 0.45F));
	}

	private RowBox drawRow(GuiGraphicsExtractor context, UiTheme theme, Entry entry, int x, int y, int width, int mouseX, int mouseY) {
		KeyMapping key = entry.key();
		if (key != null && key == snapshotHover) {
			// Point the mouse at this row's ×, which sits just left of the keycap.
			mouseX = x + width - ROW_PADDING - UiWidgets.keycapWidth(font, capture.label(key)) - 3 - KeybindControl.CLEAR / 2;
			mouseY = y + ROW_HEIGHT / 2;
		}
		boolean hovered = contains(mouseX, mouseY, x, y, width, ROW_HEIGHT);
		String rowId = key != null ? key.getName() : name(entry).getString() + ":" + entry.linkLabel().getString();
		float hover = anim.towards("keybind-row:" + rowId, hovered, 16.0F);
		UiShapes.borderedRect(context, x, y, width, ROW_HEIGHT, ROW_RADIUS, UiTheme.mix(theme.surface(), theme.surfaceHover(), hover), theme.border());
		int left = x + ROW_PADDING;
		int right = x + width - ROW_PADDING;
		int center = y + ROW_HEIGHT / 2;

		// Right side: the key, with its × for EMUtils keys, or a read-only keycap for key combos.
		KeybindControl.Layout layout = null;
		int textRight;
		if (key != null) {
			layout = KeybindControl.draw(context, font, theme, anim, capture, key, right, center, hovered, mouseX, mouseY);
			if (layout.hitsClear(capture, key, mouseX, mouseY)) {
				showTooltip(Component.translatable(EMUtilsTexts.UI_KEYBIND_CLEAR), mouseX, mouseY);
			}
			textRight = right - KeybindControl.width(font, capture, key) - 10;
		} else {
			int capWidth = UiWidgets.keycapWidth(font, entry.keyLabel());
			UiWidgets.keycap(context, font, theme, right - capWidth, center - UiWidgets.KEYCAP_HEIGHT / 2, entry.keyLabel(), false, false, 0.0F);
			textRight = right - capWidth - 10;
		}

		// Left side: the name, and below it the link to the feature and what the key clashes with.
		Component linkLabel = entry.linkLabel();
		List<KeyMapping> clashes = key == null ? List.of() : KeybindCapture.clashesWith(key);
		boolean secondLine = linkLabel != null || !clashes.isEmpty();
		int nameCenter = secondLine ? y + ROW_PADDING : center;
		if (key != null) {
			KeybindControl.drawName(context, font, theme, anim, capture, key, name(entry), left, nameCenter, textRight - left);
		} else {
			UiText.drawCentered(context, font, UiText.ellipsize(font, entry.name(), UiText.Size.BOLD, textRight - left), UiText.Size.BOLD, left, nameCenter, theme.text());
		}
		int linkX = 0;
		int linkWidth = 0;
		if (secondLine) {
			int lineCenter = y + ROW_HEIGHT - ROW_PADDING;
			int lineX = left;
			if (linkLabel != null) {
				Component link = UiText.ellipsize(font, linkLabel, UiText.Size.BODY, textRight - left - 10);
				int textWidth = UiText.width(font, link, UiText.Size.BODY);
				boolean canOpen = entry.feature() != null || entry.open() != null;
				linkWidth = canOpen ? textWidth + 3 + 7 : 0;
				linkX = left;
				boolean linkHovered = canOpen && contains(mouseX, mouseY, linkX - 2, lineCenter - 6, linkWidth + 4, 12);
				int linkColor = linkHovered ? theme.accent() : theme.textSecondary();
				UiText.drawCentered(context, font, link, UiText.Size.BODY, linkX, lineCenter, linkColor);
				if (canOpen) {
					UiIcons.draw(context, HubIcons.CHEVRON_RIGHT, linkX + textWidth + 3, lineCenter - 3, 7, linkColor);
				}
				lineX += textWidth + (canOpen ? 10 : 0) + 8;
			}
			if (!clashes.isEmpty()) {
				Component clash = Component.translatable(EMUtilsTexts.UI_KEYBINDS_SAME_KEY_AS, Component.translatable(clashes.getFirst().getName()));
				if (clashes.size() > 1) {
					clash = Component.translatable(EMUtilsTexts.UI_KEYBINDS_SAME_KEY_AS_MORE, Component.translatable(clashes.getFirst().getName()), clashes.size() - 1);
				}
				UiText.drawCentered(context, font, UiText.ellipsize(font, clash, UiText.Size.BODY, Math.max(0, textRight - lineX)), UiText.Size.BODY, lineX, lineCenter, theme.warning());
			}
		}
		return new RowBox(entry, y, layout, linkX, linkWidth);
	}

	private void showTooltip(Component text, int mouseX, int mouseY) {
		tooltip = text;
		tooltipX = mouseX;
		tooltipY = mouseY;
	}

	@Override
	protected void drawOverlay(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		if (tooltip != null && sheet == null && dialog == null) {
			UiWidgets.tooltip(context, font, theme, tooltip, tooltipX, tooltipY, width, height);
		}
		if (sheet != null) {
			sheet.render(context, theme, mouseX, mouseY, panelX, panelY, panelWidth, panelHeight, width, height);
			if (sheet.isClosed()) {
				sheet = null;
			}
		}
		if (dialog != null) {
			dialog.render(context, theme, mouseX, mouseY, width, height);
			if (dialog.isClosed()) {
				dialog = null;
			}
		}
	}

	// ---- input ----------------------------------------------------------------------------------

	private void confirmResetAll() {
		search.setFocused(false);
		dialog = new UiConfirmDialog(
			font,
			anim,
			Component.translatable(EMUtilsTexts.UI_KEYBINDS_RESET_TITLE),
			Component.translatable(EMUtilsTexts.UI_KEYBINDS_RESET_MESSAGE),
			Component.translatable(EMUtilsTexts.UI_KEYBINDS_RESET_ALL),
			() -> KeybindCapture.resetAll(editableKeys())
		);
	}

	private void openSheet(HubFeature feature) {
		search.setFocused(false);
		capture.cancel();
		sheet = new SettingsSheet(font, anim, feature, capture);
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
		boolean left = click.button() == InputConstants.MOUSE_BUTTON_LEFT;
		if (dialog != null) {
			if (left) {
				dialog.mouseClicked(mouseX, mouseY);
			}
			return true;
		}
		if (sheet != null) {
			return sheet.mouseClicked(mouseX, mouseY, click.button());
		}
		RowBox row = rowAt(mouseX, mouseY);
		// Right-clicking an EMUtils key resets it, like in the settings sheets.
		if (click.button() == InputConstants.MOUSE_BUTTON_RIGHT && row != null && row.entry().key() != null) {
			KeybindCapture.resetToDefault(row.entry().key());
			return true;
		}
		if (!left) {
			return super.mouseClicked(click, doubled);
		}
		boolean onSearch = contains(mouseX, mouseY, panelX + PADDING, toolbarY, searchWidth, TOOLBAR_HEIGHT);
		search.setFocused(onSearch);
		if (onSearch) {
			search.click(font, mouseX, click.hasShiftDown());
			return true;
		}
		if (contains(mouseX, mouseY, chipX, toolbarY, chipWidth, TOOLBAR_HEIGHT)) {
			conflictsOnly = !conflictsOnly;
			scroll.reset();
			return true;
		}
		if (contains(mouseX, mouseY, resetX, headerButtonsY, resetWidth, HEADER_BUTTON_HEIGHT)) {
			confirmResetAll();
			return true;
		}
		if (scroll.mouseClicked(mouseX, mouseY)) {
			return true;
		}
		if (row != null) {
			clickRow(row, mouseX, mouseY);
			return true;
		}
		return super.mouseClicked(click, doubled);
	}

	/** The link opens the feature's sheet or the key combo's screen; the rest of an EMUtils key's row rebinds it. */
	private void clickRow(RowBox row, double mouseX, double mouseY) {
		Entry entry = row.entry();
		int lineCenter = row.y() + ROW_HEIGHT - ROW_PADDING;
		if (row.linkWidth() > 0 && contains(mouseX, mouseY, row.linkX() - 2, lineCenter - 6, row.linkWidth() + 4, 12)) {
			if (entry.feature() != null) {
				openSheet(entry.feature());
			} else if (entry.open() != null) {
				entry.open().run();
			}
			return;
		}
		KeyMapping key = entry.key();
		if (key == null) {
			if (entry.open() != null) {
				entry.open().run();
			}
			return;
		}
		if (row.layout() != null && row.layout().hitsClear(capture, key, mouseX, mouseY)) {
			KeybindCapture.clear(key);
		} else {
			search.setFocused(false);
			capture.start(key);
		}
	}

	private @Nullable RowBox rowAt(double mouseX, double mouseY) {
		if (!scroll.contains(mouseX, mouseY)) {
			return null;
		}
		for (RowBox row : rows) {
			if (mouseY >= row.y() && mouseY < row.y() + ROW_HEIGHT) {
				return row;
			}
		}
		return null;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
		if (closing()) {
			return true;
		}
		if (sheet != null) {
			return sheet.mouseDragged(click.x(), click.y());
		}
		if (dialog == null && scroll.mouseDragged(click.y())) {
			return true;
		}
		return super.mouseDragged(click, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
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
		if (closing() || dialog != null) {
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
		if (dialog != null) {
			dialog.keyPressed(input);
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

	@Override
	public void onClose() {
		search.setFocused(false);
		capture.cancel();
		if (sheet != null) {
			sheet.close();
		}
		super.onClose();
	}

	// ---- UI snapshots ---------------------------------------------------------------------------

	/** Shows the × of the key named {@code name} hovered, or nothing hovered for null; used by UI snapshots. */
	public void hoverClearForSnapshot(@Nullable String name) {
		snapshotHover = name == null ? null : KeybindCapture.mapping(name);
	}

	/** Waits for a new key for the key named {@code name}; used by UI snapshots. */
	public void listenForSnapshot(String name) {
		KeyMapping key = KeybindCapture.mapping(name);
		if (key != null) {
			capture.start(key);
		}
	}

	/** Left-clicks the × of the key named {@code name} through {@link #clickRow}; used by UI snapshots. */
	public void clickClearForSnapshot(String name) {
		for (RowBox row : rows) {
			if (row.entry().key() != null && row.entry().key().getName().equals(name) && row.layout() != null) {
				clickRow(row, row.layout().clearX() + KeybindControl.CLEAR / 2.0, row.layout().capY() + KeybindControl.CLEAR / 2.0);
			}
		}
	}

	public void setConflictsOnlyForSnapshot(boolean on) {
		conflictsOnly = on;
		scroll.reset();
	}

	public void searchForSnapshot(String text) {
		search.setText(text);
		scroll.reset();
	}

	public int conflictCountForSnapshot() {
		return conflictCount();
	}

	/** How many rows are drawn, for checking the filters; used by UI snapshots. */
	public int visibleRowsForSnapshot() {
		return rows.size();
	}

	/** The keys under General; used by UI snapshots. */
	public List<String> generalKeysForSnapshot() {
		List<String> names = new ArrayList<>();
		for (Section section : sections) {
			if (section.group() == null && section.label().getString().equals(Component.translatable(EMUtilsTexts.UI_KEYBINDS_GENERAL).getString())) {
				for (Entry entry : section.entries()) {
					names.add(entry.key().getName());
				}
			}
		}
		return names;
	}

	/** Opens the sheet of the feature the key named {@code name} belongs to, as its link does; used by UI snapshots. */
	public boolean openFeatureSheetForSnapshot(String name) {
		for (Section section : sections) {
			for (Entry entry : section.entries()) {
				if (entry.key() != null && entry.key().getName().equals(name) && entry.feature() != null) {
					openSheet(entry.feature());
					return true;
				}
			}
		}
		return false;
	}

	public boolean sheetOpenForSnapshot() {
		return sheet != null;
	}

	public void closeSheetForSnapshot() {
		sheet = null;
	}

	/**
	 * One row: an EMUtils key with the feature that lists it (null under General), or a read-only key
	 * combo with the screen that edits it.
	 */
	private record Entry(@Nullable KeyMapping key, @Nullable HubFeature feature, Component name, Component keyLabel, @Nullable Component linkLabel, @Nullable Runnable open) {
		static Entry key(KeyMapping key, @Nullable HubFeature feature) {
			return new Entry(key, feature, Component.empty(), Component.empty(), feature == null ? null : SettingsScreen.title(feature), null);
		}

		static Entry combo(Component name, Component keyLabel, Component screen, @Nullable Runnable open) {
			return new Entry(null, null, name, keyLabel, screen, open);
		}
	}

	private record Section(Component label, HubFeature.@Nullable Group group, List<Entry> entries) {
	}

	private record RowBox(Entry entry, int y, KeybindControl.@Nullable Layout layout, int linkX, int linkWidth) {
	}
}
