package net.emutils.client.emutils.waypoint.gui;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.emutils.client.EMUtilsClient;
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
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.emutils.client.emutils.waypoint.Waypoint;
import net.emutils.client.emutils.waypoint.WaypointEntry;
import net.emutils.client.emutils.waypoint.WaypointSort;
import net.emutils.client.emutils.waypoint.WaypointType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * The waypoints of the current world (#103, #105): each one with its color, name, type, coordinates and
 * distance, and buttons to edit it, copy the coordinates, hide it, toggle its beacon or delete it. A search
 * narrows the list, it can be sorted by distance, name or age, and waypoints in sets are grouped under
 * headers that collapse. It lists this dimension's waypoints, and with Other dimensions on, the rest of the
 * world's too. Clicking a row edits it; waypoints are added and edited in a sheet over the list.
 */
public final class WaypointsScreen extends UiPanelScreen {
	private static final int PADDING = 16;
	private static final int HEADER_BUTTON_HEIGHT = 20;
	private static final int TOOLBAR_HEIGHT = 20;
	private static final int ROW_HEIGHT = 44;
	private static final int ROW_GAP = 8;
	private static final int GROUP_HEIGHT = 24;
	private static final int GROUP_GAP = 6;
	private static final int ROW_RADIUS = 9;
	private static final int ROW_PADDING = 12;
	private static final int ACTION = 20;
	private static final int ACTION_GAP = 2;
	private static final int ACTION_COUNT = 5;
	private static final int FADE_HEIGHT = 12;
	/** The sets that are folded up, by lowercase name; "" is the waypoints in no set. Kept while the game runs. */
	private static final Set<String> COLLAPSED = new HashSet<>();

	private final UiScrollArea scroll = new UiScrollArea();
	private final UiTextField search = new UiTextField(this, 64);
	private final List<RowBox> rows = new ArrayList<>();
	private final List<GroupBox> groups = new ArrayList<>();
	/** Opened by the Add Waypoint keybind: the screen closes together with the add sheet. */
	private final boolean addOnly;
	private @Nullable WaypointSheet sheet;
	private @Nullable UiConfirmDialog dialog;
	private int addX;
	private int addWidth;
	private int clearX;
	private int clearWidth;
	private int headerButtonsY;
	private int toolbarY;
	private int searchWidth;
	private int sortX;
	private int sortWidth;
	private int dimensionsX;
	private int dimensionsWidth;
	private @Nullable Component tooltip;
	private int tooltipX;
	private int tooltipY;

	public WaypointsScreen(@Nullable Screen parent) {
		this(parent, false);
	}

	private WaypointsScreen(@Nullable Screen parent, boolean addOnly) {
		super(Component.translatable(EMUtilsTexts.SCREEN_CURRENT_WAYPOINTS), parent);
		this.addOnly = addOnly;
		// Typing goes to the search, unless the screen only opens the add sheet or there is no world to search in.
		if (!addOnly && Minecraft.getInstance().level != null) {
			search.setFocused(true);
		}
	}

	/** Opens straight into the add sheet, for the Add Waypoint keybind; closing it closes the screen too. */
	public static WaypointsScreen addWaypoint(@Nullable Screen parent) {
		return new WaypointsScreen(parent, true);
	}

	@Override
	protected int maxPanelWidth() {
		return 560;
	}

	@Override
	protected void layout() {
		int titleHeight = UiText.lineHeight(font, UiText.Size.HEADING);
		int subtitleHeight = UiText.lineHeight(font, UiText.Size.BODY);
		int headerHeight = titleHeight + 6 + subtitleHeight;
		headerButtonsY = panelY + PADDING + (headerHeight - HEADER_BUTTON_HEIGHT) / 2;
		toolbarY = panelY + PADDING + headerHeight + 12;
		int listY = toolbarY + TOOLBAR_HEIGHT + 10;
		scroll.setBounds(panelX + PADDING, listY, panelWidth - PADDING * 2 + UiScrollArea.GUTTER, panelY + panelHeight - PADDING / 2 - listY);
		if (addOnly && sheet == null && !closing()) {
			openAddSheet();
		}
		if (sheet == null) {
			search.restoreFocus();
		}
	}

	/** The waypoints made in this dimension, which is what Clear all deletes. */
	private List<Waypoint> waypoints() {
		return EMUtilsClient.waypoint().waypointsForCurrentWorld(minecraft);
	}

	/** What the list shows, before the search: this dimension's waypoints and, with Other dimensions on, the rest of the world's. */
	private List<WaypointEntry> allEntries() {
		return EMUtilsClient.waypoint().entriesForCurrentWorld(minecraft, EMUtilsClient.config().waypointShowOtherDimensions());
	}

	/** The entries that match the search, in the chosen order. */
	private List<WaypointEntry> shownEntries() {
		String query = search.text().trim().toLowerCase(Locale.ROOT);
		List<WaypointEntry> entries = new ArrayList<>();
		for (WaypointEntry entry : allEntries()) {
			if (query.isEmpty() || matches(entry.waypoint(), query)) {
				entries.add(entry);
			}
		}
		entries.sort(EMUtilsClient.config().waypointSort().comparator(entry -> minecraft.player == null ? 0.0D : EMUtilsClient.waypoint().distance(minecraft, entry)));
		return entries;
	}

	private static boolean matches(Waypoint waypoint, String query) {
		String label = waypoint.label() == null ? "" : waypoint.label();
		return label.toLowerCase(Locale.ROOT).contains(query) || waypoint.set().toLowerCase(Locale.ROOT).contains(query);
	}

	/** Waypoints belong to a world; the screen can also be opened from the title screen, through the settings. */
	private boolean inWorld() {
		return minecraft.level != null;
	}

	private void openAddSheet() {
		openSheet(null);
	}

	/** Opens the sheet to add a waypoint, or to edit {@code editing}. */
	private void openSheet(@Nullable Waypoint editing) {
		if (!inWorld()) {
			return;
		}
		// The sheet's fields take the typing, so the search lets go of it until the sheet closes.
		search.setFocused(false);
		sheet = new WaypointSheet(font, anim, editing, added -> {
			if (addOnly) {
				onClose();
			}
		});
	}

	private void openEditSheet(String id) {
		for (WaypointEntry entry : allEntries()) {
			if (entry.waypoint().id().equals(id)) {
				openSheet(entry.waypoint());
				return;
			}
		}
	}

	// ---- drawing --------------------------------------------------------------------------------

	@Override
	protected void drawPanel(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		tooltip = null;
		boolean interactive = sheet == null && dialog == null && !closing();
		int hoverX = interactive ? mouseX : Integer.MIN_VALUE / 2;
		int hoverY = interactive ? mouseY : Integer.MIN_VALUE / 2;
		List<WaypointEntry> entries = shownEntries();
		drawHeader(context, theme, entries.size(), waypoints().size(), hoverX, hoverY);
		if (inWorld()) {
			drawToolbar(context, theme, hoverX, hoverY);
		}
		drawRows(context, theme, entries, hoverX, hoverY);
	}

	private void drawHeader(GuiGraphicsExtractor context, UiTheme theme, int count, int clearable, int mouseX, int mouseY) {
		int left = panelX + PADDING;
		int top = panelY + PADDING;
		UiText.draw(context, font, title, UiText.Size.HEADING, left, top, theme.text());
		Component subtitle;
		if (inWorld()) {
			Component countText = Component.translatable(count == 1 ? EMUtilsTexts.UI_WAYPOINT_COUNT_ONE : EMUtilsTexts.UI_WAYPOINT_COUNT, count);
			subtitle = dimensionName(minecraft.level.dimension().identifier().toString()).copy().append(" · ").append(countText);
		} else {
			subtitle = Component.translatable(EMUtilsTexts.UI_WAYPOINT_NO_WORLD_TITLE);
		}
		UiText.draw(context, font, subtitle, UiText.Size.BODY, left, top + UiText.lineHeight(font, UiText.Size.HEADING) + 6, theme.muted());

		Component add = Component.translatable(EMUtilsTexts.UI_ADD_WAYPOINT);
		int iconSize = 9;
		addWidth = UiText.width(font, add, UiText.Size.LABEL) + iconSize + 4 + 20;
		addX = panelX + panelWidth - PADDING - addWidth;
		boolean canAdd = inWorld();
		boolean addHovered = contains(mouseX, mouseY, addX, headerButtonsY, addWidth, HEADER_BUTTON_HEIGHT);
		float addHover = canAdd && addHovered ? 1.0F : 0.0F;
		// Outside a world the button is greyed out, like Clear all with nothing to clear.
		int addFill = canAdd ? UiTheme.mix(theme.accent(), theme.accentHover(), addHover) : theme.segmentBackground();
		int addText = canAdd ? theme.onAccent() : UiTheme.fade(theme.muted(), 0.6F);
		UiShapes.roundedRect(context, addX, headerButtonsY, addWidth, HEADER_BUTTON_HEIGHT, 8, addFill);
		UiIcons.draw(context, HubIcons.PLUS, addX + 10, headerButtonsY + (HEADER_BUTTON_HEIGHT - iconSize) / 2, iconSize, addText);
		UiText.drawCentered(context, font, add, UiText.Size.LABEL, addX + 10 + iconSize + 4, headerButtonsY + HEADER_BUTTON_HEIGHT / 2, addText);
		if (!canAdd && addHovered) {
			tooltip = Component.translatable(EMUtilsTexts.UI_WAYPOINT_NEEDS_WORLD);
			tooltipX = mouseX;
			tooltipY = mouseY;
		}

		Component clear = Component.translatable(EMUtilsTexts.UI_CLEAR_ALL);
		clearWidth = UiWidgets.buttonWidth(font, clear) + 4;
		clearX = addX - 6 - clearWidth;
		boolean canClear = clearable > 0;
		float clearHover = canClear && contains(mouseX, mouseY, clearX, headerButtonsY, clearWidth, HEADER_BUTTON_HEIGHT) ? 1.0F : 0.0F;
		UiShapes.roundedRect(context, clearX, headerButtonsY, clearWidth, HEADER_BUTTON_HEIGHT, 8, UiTheme.fade(theme.hover(), clearHover));
		UiText.drawCentered(context, font, clear, UiText.Size.LABEL, clearX + (clearWidth - UiText.width(font, clear, UiText.Size.LABEL)) / 2, headerButtonsY + HEADER_BUTTON_HEIGHT / 2, canClear ? theme.textSecondary() : UiTheme.fade(theme.muted(), 0.5F));
	}

	/** The search, the sort and the Other dimensions switch, in one row under the header. */
	private void drawToolbar(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		int left = panelX + PADDING;
		int right = panelX + panelWidth - PADDING;
		int center = toolbarY + TOOLBAR_HEIGHT / 2;

		// The switch is at the right, the sort beside it, and the search takes what is left.
		Component dimensions = Component.translatable(EMUtilsTexts.UI_WAYPOINT_OTHER_DIMENSIONS);
		dimensionsWidth = UiText.width(font, dimensions, UiText.Size.LABEL) + 8 + UiWidgets.SWITCH_WIDTH;
		dimensionsX = right - dimensionsWidth;
		boolean showOther = EMUtilsClient.config().waypointShowOtherDimensions();
		boolean dimensionsHovered = contains(mouseX, mouseY, dimensionsX, toolbarY, dimensionsWidth, TOOLBAR_HEIGHT);
		UiText.drawCentered(context, font, dimensions, UiText.Size.LABEL, dimensionsX, center, showOther ? theme.text() : theme.textSecondary());
		float on = anim.transition("waypoint-other-dimensions", showOther, 0.18F);
		UiWidgets.toggle(context, theme, right - UiWidgets.SWITCH_WIDTH, center - UiWidgets.SWITCH_HEIGHT / 2, on, dimensionsHovered ? 1.0F : 0.0F);
		if (dimensionsHovered) {
			tooltip = Component.translatable(EMUtilsTexts.UI_WAYPOINT_OTHER_DIMENSIONS_DESC);
			tooltipX = mouseX;
			tooltipY = mouseY;
		}

		// The button is as wide as the longest sort name, so it doesn't change size as the sort does.
		int widest = 0;
		for (WaypointSort option : WaypointSort.values()) {
			widest = Math.max(widest, UiText.width(font, sortLabel(option), UiText.Size.LABEL));
		}
		sortWidth = widest + 20;
		sortX = dimensionsX - 10 - sortWidth;
		Component sort = sortLabel(EMUtilsClient.config().waypointSort());
		float sortHover = anim.towards("waypoint-sort", contains(mouseX, mouseY, sortX, toolbarY, sortWidth, TOOLBAR_HEIGHT), 16.0F);
		UiShapes.roundedRect(context, sortX, toolbarY, sortWidth, TOOLBAR_HEIGHT, 8, UiTheme.mix(theme.segmentBackground(), theme.hover(), sortHover));
		UiText.drawCentered(context, font, sort, UiText.Size.LABEL, sortX + (sortWidth - UiText.width(font, sort, UiText.Size.LABEL)) / 2, center, theme.textSecondary());

		searchWidth = Math.max(60, sortX - 8 - left);
		UiShapes.borderedRect(context, left, toolbarY, searchWidth, TOOLBAR_HEIGHT, 8, theme.surface(), search.focused() ? theme.accent() : theme.line());
		UiIcons.draw(context, HubIcons.SEARCH, left + 9, center - 5, 10, theme.textSecondary());
		boolean clearable = !search.text().isEmpty();
		search.draw(context, font, theme, left + 26, center, searchWidth - 26 - (clearable ? 26 : 10), Component.translatable(EMUtilsTexts.UI_WAYPOINT_SEARCH));
		if (clearable) {
			boolean xHovered = contains(mouseX, mouseY, left + searchWidth - 22, toolbarY + 2, 16, 16);
			UiIcons.draw(context, HubIcons.X, left + searchWidth - 19, center - 5, 10, xHovered ? theme.text() : theme.muted());
		}
	}

	private static Component sortLabel(WaypointSort sort) {
		return Component.translatable(EMUtilsTexts.UI_WAYPOINT_SORT, Component.translatable(sort.labelKey()));
	}

	/** What the list is made of: the waypoints, and above each set's waypoints the set's header when any set is in use. */
	private List<Item> buildItems(List<WaypointEntry> entries) {
		boolean searching = !search.text().isBlank();
		Map<String, List<WaypointEntry>> bySet = new LinkedHashMap<>();
		Map<String, String> titles = new LinkedHashMap<>();
		boolean anySet = false;
		for (WaypointEntry entry : entries) {
			String set = entry.waypoint().set();
			anySet |= !set.isEmpty();
			String key = set.toLowerCase(Locale.ROOT);
			bySet.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);
			titles.putIfAbsent(key, set);
		}
		List<Item> items = new ArrayList<>();
		if (!anySet) {
			for (WaypointEntry entry : entries) {
				items.add(new EntryItem(entry));
			}
			return items;
		}
		// Sets alphabetically, with the waypoints in none last.
		List<String> keys = new ArrayList<>(bySet.keySet());
		keys.sort((a, b) -> a.isEmpty() != b.isEmpty() ? (a.isEmpty() ? 1 : -1) : a.compareTo(b));
		for (String key : keys) {
			List<WaypointEntry> members = bySet.get(key);
			// A search has to show what it finds, so it opens the folded sets.
			boolean collapsed = COLLAPSED.contains(key) && !searching;
			String title = key.isEmpty() ? Component.translatable(EMUtilsTexts.UI_WAYPOINT_UNGROUPED).getString() : titles.get(key);
			items.add(new GroupItem(key, title, members.size(), collapsed));
			if (!collapsed) {
				for (WaypointEntry entry : members) {
					items.add(new EntryItem(entry));
				}
			}
		}
		return items;
	}

	private static int itemHeight(Item item) {
		return item instanceof GroupItem ? GROUP_HEIGHT : ROW_HEIGHT;
	}

	/** The space after an item: a little more before a set's header than between waypoints. */
	private static int gapAfter(Item item, @Nullable Item next) {
		if (item instanceof GroupItem) {
			return GROUP_GAP;
		}
		return next instanceof GroupItem ? ROW_GAP + 6 : ROW_GAP;
	}

	private void drawRows(GuiGraphicsExtractor context, UiTheme theme, List<WaypointEntry> entries, int mouseX, int mouseY) {
		List<Item> items = buildItems(entries);
		int[] tops = new int[items.size()];
		int total = 0;
		for (int i = 0; i < items.size(); i++) {
			tops[i] = total;
			total += itemHeight(items.get(i)) + gapAfter(items.get(i), i + 1 < items.size() ? items.get(i + 1) : null);
		}
		int contentHeight = items.isEmpty() ? 0 : total - gapAfter(items.getLast(), null) + FADE_HEIGHT;
		scroll.setContentHeight(contentHeight);
		scroll.animate(anim, "waypoints-scroll", mouseX, mouseY);
		rows.clear();
		groups.clear();
		boolean mouseInList = scroll.contains(mouseX, mouseY) && !scroll.dragging();
		int hoverX = mouseInList ? mouseX : Integer.MIN_VALUE / 2;
		scroll.begin(context);
		context.pose().pushMatrix();
		context.pose().translate(0.0F, scroll.offset() - scroll.exactOffset());
		int width = scroll.contentWidth();
		int baseY = scroll.y() + FADE_HEIGHT / 2 - scroll.offset();
		for (int i = 0; i < items.size(); i++) {
			Item item = items.get(i);
			int y = baseY + tops[i];
			int height = itemHeight(item);
			if (y + height < scroll.y() || y > scroll.y() + scroll.height()) {
				continue;
			}
			if (item instanceof GroupItem group) {
				drawGroup(context, theme, group, scroll.x(), y, width, hoverX, mouseY);
				groups.add(new GroupBox(group.key(), scroll.x(), y, width));
			} else if (item instanceof EntryItem entry) {
				rows.add(drawRow(context, theme, entry.entry(), scroll.x(), y, width, hoverX, mouseY));
			}
		}
		context.pose().popMatrix();
		if (!inWorld()) {
			drawNoWorld(context, theme, scroll.x() + width / 2, width);
		} else if (entries.isEmpty()) {
			int centerX = scroll.x() + width / 2;
			int iconSize = 22;
			int top = scroll.y() + Math.max(20, scroll.height() / 2 - 30);
			boolean searching = !search.text().isBlank();
			UiIcons.draw(context, searching ? HubIcons.SEARCH : HubIcons.MAP_PIN, centerX - iconSize / 2, top, iconSize, theme.muted());
			Component empty = searching
				? Component.translatable(EMUtilsTexts.UI_WAYPOINT_NO_MATCH, search.text().trim())
				: Component.translatable(EMUtilsTexts.WAYPOINT_NONE_WORLD);
			empty = UiText.ellipsize(font, empty, UiText.Size.BODY, width - 20);
			UiText.draw(context, font, empty, UiText.Size.BODY, centerX - UiText.width(font, empty, UiText.Size.BODY) / 2, top + iconSize + 10, theme.muted());
		}
		scroll.end(context, theme.panel(), FADE_HEIGHT, UiTheme.fade(theme.text(), 0.25F), UiTheme.fade(theme.text(), 0.45F));
	}

	/** A set's header: a chevron that shows whether it is open, its name and how many waypoints it holds. */
	private void drawGroup(GuiGraphicsExtractor context, UiTheme theme, GroupItem group, int x, int y, int width, int mouseX, int mouseY) {
		boolean hovered = contains(mouseX, mouseY, x, y, width, GROUP_HEIGHT);
		float hover = anim.towards("waypoint-group:" + group.key(), hovered, 16.0F);
		UiShapes.roundedRect(context, x, y, width, GROUP_HEIGHT, 8, UiTheme.fade(theme.hover(), hover));
		int center = y + GROUP_HEIGHT / 2;
		UiIcons.draw(context, group.collapsed() ? HubIcons.CHEVRON_RIGHT : HubIcons.CHEVRON_DOWN, x + 8, center - 5, 10, theme.textSecondary());
		Component title = UiText.ellipsize(font, Component.literal(group.title()), UiText.Size.BOLD, width - 24 - 40);
		UiText.drawCentered(context, font, title, UiText.Size.BOLD, x + 26, center, group.key().isEmpty() ? theme.textSecondary() : theme.text());
		Component count = Component.literal(String.valueOf(group.count()));
		UiText.drawCentered(context, font, count, UiText.Size.LABEL, x + 26 + UiText.width(font, title, UiText.Size.BOLD) + 8, center, theme.muted());
	}

	/** Outside a world there's nothing to list or add, so the whole body says so. */
	private void drawNoWorld(GuiGraphicsExtractor context, UiTheme theme, int centerX, int width) {
		int iconSize = 32;
		Component heading = Component.translatable(EMUtilsTexts.UI_WAYPOINT_NO_WORLD_TITLE);
		List<Component> lines = UiText.wrap(font, Component.translatable(EMUtilsTexts.UI_WAYPOINT_NO_WORLD), UiText.Size.BODY, Math.min(width - 40, 320));
		int headingHeight = UiText.lineHeight(font, UiText.Size.HEADING);
		int lineHeight = UiText.lineHeight(font, UiText.Size.BODY) + 3;
		int blockHeight = iconSize + 14 + headingHeight + 8 + lines.size() * lineHeight;
		int top = scroll.y() + Math.max(20, (scroll.height() - blockHeight) / 2);
		UiIcons.draw(context, HubIcons.MAP_PIN, centerX - iconSize / 2, top, iconSize, theme.muted());
		int headingTop = top + iconSize + 14;
		UiText.draw(context, font, heading, UiText.Size.HEADING, centerX - UiText.width(font, heading, UiText.Size.HEADING) / 2, headingTop, theme.text());
		int lineTop = headingTop + headingHeight + 8;
		for (int i = 0; i < lines.size(); i++) {
			Component line = lines.get(i);
			UiText.draw(context, font, line, UiText.Size.BODY, centerX - UiText.width(font, line, UiText.Size.BODY) / 2, lineTop + i * lineHeight, theme.textSecondary());
		}
	}

	private RowBox drawRow(GuiGraphicsExtractor context, UiTheme theme, WaypointEntry entry, int x, int y, int width, int mouseX, int mouseY) {
		Waypoint waypoint = entry.waypoint();
		boolean hovered = contains(mouseX, mouseY, x, y, width, ROW_HEIGHT);
		float hover = anim.towards("waypoint:" + waypoint.id(), hovered, 16.0F);
		UiShapes.borderedRect(context, x, y, width, ROW_HEIGHT, ROW_RADIUS, UiTheme.mix(theme.surface(), theme.surfaceHover(), hover), theme.border());
		// Hidden waypoints are dimmed, so the ones shown in the world stand out.
		float shown = waypoint.hidden() ? 0.45F : 1.0F;
		int left = x + ROW_PADDING;
		int nameCenter = y + ROW_PADDING + 4;
		UiShapes.circle(context, left, nameCenter - 4, 9, UiTheme.fade(0xFF000000 | waypoint.color(), shown));
		int nameX = left + 15;

		// Right side: the distance, then the actions.
		Identifier[] icons = {
			HubIcons.PENCIL,
			HubIcons.COPY,
			waypoint.hidden() ? HubIcons.EYE_OFF : HubIcons.EYE,
			HubIcons.BEAM,
			HubIcons.TRASH
		};
		String[] tips = {
			EMUtilsTexts.UI_WAYPOINT_EDIT,
			EMUtilsTexts.UI_COPY_COORDINATES,
			waypoint.hidden() ? EMUtilsTexts.WAYPOINT_ACTION_SHOW : EMUtilsTexts.WAYPOINT_ACTION_HIDE,
			waypoint.beaconEnabled() ? EMUtilsTexts.UI_BEACON_TURN_OFF : EMUtilsTexts.UI_BEACON_TURN_ON,
			EMUtilsTexts.UI_DELETE
		};
		int actionsWidth = icons.length * ACTION + (icons.length - 1) * ACTION_GAP;
		int actionsX = x + width - ROW_PADDING + 4 - actionsWidth;
		int actionY = y + (ROW_HEIGHT - ACTION) / 2;
		for (int i = 0; i < icons.length; i++) {
			int actionX = actionsX + i * (ACTION + ACTION_GAP);
			boolean actionHovered = contains(mouseX, mouseY, actionX, actionY, ACTION, ACTION);
			int color = switch (i) {
				case 3 -> waypoint.beaconEnabled() ? theme.accent() : theme.muted();
				case 4 -> actionHovered ? theme.warning() : theme.textSecondary();
				default -> theme.textSecondary();
			};
			UiWidgets.ghostIconButton(context, theme, actionX, actionY, ACTION, icons[i], color, actionHovered ? 1.0F : 0.0F);
			if (actionHovered) {
				tooltip = Component.translatable(tips[i]);
				tooltipX = mouseX;
				tooltipY = mouseY;
			}
		}
		// A waypoint from a dimension that doesn't line up with this one has no distance from here.
		Component distance = minecraft.player == null || !entry.placeable() ? Component.empty() : Component.translatable(EMUtilsTexts.UI_BLOCKS, EMUtilsClient.waypoint().distanceBlocks(minecraft, entry));
		int distanceWidth = UiText.width(font, distance, UiText.Size.LABEL);
		int distanceX = actionsX - 10 - distanceWidth;
		UiText.drawCentered(context, font, distance, UiText.Size.LABEL, distanceX, y + ROW_HEIGHT / 2, theme.muted());

		// Left side: name and badges, coordinates below.
		Component type = Component.translatable(waypoint.type().labelKey());
		int badgeWidth = UiText.width(font, type, UiText.Size.SMALL) + 8;
		Component origin = entry.sameDimension() ? null : originLabel(entry);
		int originWidth = origin == null ? 0 : UiText.width(font, origin, UiText.Size.SMALL) + 8 + 4;
		int textRight = distanceX - 10;
		Component name = UiText.ellipsize(font, Component.literal(waypoint.label()), UiText.Size.BOLD, textRight - nameX - badgeWidth - originWidth - 6);
		UiText.drawCentered(context, font, name, UiText.Size.BOLD, nameX, nameCenter, UiTheme.fade(theme.text(), shown));
		int badgeX = nameX + UiText.width(font, name, UiText.Size.BOLD) + 6;
		int badgeY = nameCenter - (UiText.lineHeight(font, UiText.Size.SMALL) + 5) / 2;
		boolean death = waypoint.type() == WaypointType.DEATH;
		UiWidgets.badge(context, font, badgeX, badgeY, type, death ? UiTheme.fade(theme.warning(), 0.16F) : theme.segmentBackground(), death ? theme.warning() : theme.textSecondary());
		if (origin != null) {
			UiWidgets.badge(context, font, badgeX + badgeWidth + 4, badgeY, origin, UiTheme.fade(theme.accent(), 0.16F), theme.accent());
		}
		Component coords = Component.literal("X " + entry.x() + "   Y " + entry.y() + "   Z " + entry.z());
		UiText.drawCentered(context, font, coords, UiText.Size.BODY, nameX, y + ROW_HEIGHT - ROW_PADDING - 4, UiTheme.fade(theme.muted(), shown));
		return new RowBox(entry, x, y, width, actionsX, actionY);
	}

	/** Where a waypoint from another dimension comes from: "From Nether" if it is converted, else the dimension's name. */
	private static Component originLabel(WaypointEntry entry) {
		Component dimension = dimensionName(entry.waypoint().dimension());
		return entry.placeable() ? Component.translatable(EMUtilsTexts.UI_WAYPOINT_FROM, dimension) : dimension;
	}

	@Override
	protected void drawOverlay(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		if (tooltip != null && sheet == null && dialog == null) {
			UiWidgets.tooltip(context, font, theme, tooltip, tooltipX, tooltipY, width, height);
		}
		if (sheet != null) {
			sheet.render(context, theme, mouseX, mouseY, width, height);
			if (sheet.isClosed()) {
				sheet = null;
				// Typing goes back to the search.
				if (!addOnly && inWorld()) {
					search.setFocused(true);
				}
			}
		}
		if (dialog != null) {
			dialog.render(context, theme, mouseX, mouseY, width, height);
			if (dialog.isClosed()) {
				dialog = null;
			}
		}
	}

	/** "Overworld", "Nether" or "End", or the dimension's id for other dimensions. */
	static Component dimensionName(String id) {
		String key = "emutils.dimension." + id.replace("minecraft:", "").replace(':', '.');
		return Language.getInstance().has(key) ? Component.translatable(key) : Component.literal(id);
	}

	// ---- input ----------------------------------------------------------------------------------

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		if (closing()) {
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
			if (left) {
				sheet.mouseClicked(mouseX, mouseY);
			}
			return true;
		}
		if (!left) {
			return super.mouseClicked(click, doubled);
		}
		if (contains(mouseX, mouseY, addX, headerButtonsY, addWidth, HEADER_BUTTON_HEIGHT)) {
			openAddSheet();
			return true;
		}
		int count = waypoints().size();
		if (count > 0 && contains(mouseX, mouseY, clearX, headerButtonsY, clearWidth, HEADER_BUTTON_HEIGHT)) {
			openClearDialog(() -> EMUtilsClient.waypoint().clearForCurrentWorld(minecraft));
			return true;
		}
		if (inWorld() && clickToolbar(mouseX, mouseY, click.hasShiftDown())) {
			return true;
		}
		if (scroll.mouseClicked(mouseX, mouseY)) {
			return true;
		}
		if (scroll.contains(mouseX, mouseY)) {
			for (GroupBox group : groups) {
				if (contains(mouseX, mouseY, group.x(), group.y(), group.width(), GROUP_HEIGHT)) {
					toggleGroup(group.key());
					return true;
				}
			}
			for (RowBox row : rows) {
				for (int i = 0; i < ACTION_COUNT; i++) {
					if (contains(mouseX, mouseY, row.actionsX() + i * (ACTION + ACTION_GAP), row.actionY(), ACTION, ACTION)) {
						runAction(row, i);
						return true;
					}
				}
				// Anywhere else on the row edits it.
				if (contains(mouseX, mouseY, row.x(), row.y(), row.width(), ROW_HEIGHT)) {
					openEditSheet(row.id());
					return true;
				}
			}
		}
		return super.mouseClicked(click, doubled);
	}

	/** Clicks on the search, the sort and the Other dimensions switch; true when one was used. */
	private boolean clickToolbar(double mouseX, double mouseY, boolean shift) {
		int left = panelX + PADDING;
		boolean onSearch = contains(mouseX, mouseY, left, toolbarY, searchWidth, TOOLBAR_HEIGHT);
		if (onSearch) {
			if (!search.text().isEmpty() && contains(mouseX, mouseY, left + searchWidth - 22, toolbarY + 2, 16, 16)) {
				search.setText("");
				search.select(0, 0);
				scroll.reset();
			} else {
				search.setFocused(true);
				search.click(font, mouseX, shift);
			}
			return true;
		}
		search.setFocused(false);
		if (contains(mouseX, mouseY, sortX, toolbarY, sortWidth, TOOLBAR_HEIGHT)) {
			EMUtilsClient.config().setWaypointSort(EMUtilsClient.config().waypointSort().next());
			scroll.reset();
			return true;
		}
		if (contains(mouseX, mouseY, dimensionsX, toolbarY, dimensionsWidth, TOOLBAR_HEIGHT)) {
			EMUtilsClient.config().setWaypointShowOtherDimensions(!EMUtilsClient.config().waypointShowOtherDimensions());
			scroll.reset();
			return true;
		}
		return false;
	}

	private static void toggleGroup(String key) {
		if (!COLLAPSED.remove(key)) {
			COLLAPSED.add(key);
		}
	}

	private void runAction(RowBox row, int action) {
		String id = row.id();
		switch (action) {
			case 0 -> openEditSheet(id);
			// A converted waypoint is shown at its converted coordinates, so those are the ones to copy.
			case 1 -> EMUtilsClient.waypoint().copyCoordinates(minecraft, row.entry().x(), row.entry().y(), row.entry().z());
			case 2 -> EMUtilsClient.waypoint().toggleHidden(id);
			case 3 -> EMUtilsClient.waypoint().toggleBeacon(id);
			default -> EMUtilsClient.waypoint().clear(minecraft, id);
		}
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
		if (sheet != null) {
			sheet.mouseDragged(click.x(), click.y());
			return true;
		}
		if (scroll.mouseDragged(click.y())) {
			return true;
		}
		return super.mouseDragged(click, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		if (sheet != null) {
			sheet.mouseReleased();
			return true;
		}
		if (scroll.mouseReleased()) {
			return true;
		}
		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (closing() || sheet != null || dialog != null) {
			return true;
		}
		if (scroll.scroll(mouseX, mouseY, verticalAmount)) {
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	/** Esc clears the search first, then closes; typing anywhere goes to the search. */
	@Override
	public boolean keyPressed(KeyEvent input) {
		if (closing()) {
			return true;
		}
		if (dialog != null) {
			dialog.keyPressed(input);
			return true;
		}
		if (sheet != null) {
			sheet.keyPressed(input);
			return true;
		}
		if (input.isEscape() && !search.text().isEmpty()) {
			search.setText("");
			search.select(0, 0);
			scroll.reset();
			return true;
		}
		if (!input.isEscape() && search.keyPressed(input, scroll::reset)) {
			return true;
		}
		return super.keyPressed(input);
	}

	@Override
	public boolean charTyped(CharacterEvent input) {
		if (closing()) {
			return true;
		}
		if (sheet != null) {
			sheet.charTyped(input);
			return true;
		}
		if (dialog == null && inWorld()) {
			if (!search.focused()) {
				search.setFocused(true);
			}
			if (search.charTyped(input, scroll::reset)) {
				return true;
			}
		}
		return super.charTyped(input);
	}

	@Override
	public void onClose() {
		if (sheet != null) {
			sheet.close();
		}
		search.setFocused(false);
		super.onClose();
	}

	/** Whether the add sheet is open; used by UI snapshots. */
	public boolean sheetOpenForSnapshot() {
		return sheet != null;
	}

	/** Opens the add sheet; used by UI snapshots. */
	public void openAddSheetForSnapshot() {
		openAddSheet();
	}

	/** Opens the edit sheet for the first waypoint in the list; used by UI snapshots. */
	public void openEditSheetForSnapshot() {
		List<Waypoint> waypoints = waypoints();
		if (!waypoints.isEmpty()) {
			openSheet(waypoints.getFirst());
		}
	}

	/** The name in the open sheet, or null when none is open; used by UI snapshots. */
	public @Nullable String sheetNameForSnapshot() {
		return sheet == null ? null : sheet.nameForSnapshot();
	}

	/** Types into the search, as the player would; used by UI snapshots. */
	public void searchForSnapshot(String text) {
		search.setText(text);
		search.select(text.length(), text.length());
		scroll.reset();
	}

	/** Folds or unfolds a set ("" is the waypoints in none); used by UI snapshots. */
	public static void setCollapsedForSnapshot(String set, boolean collapsed) {
		String key = set.toLowerCase(Locale.ROOT);
		if (collapsed) {
			COLLAPSED.add(key);
		} else {
			COLLAPSED.remove(key);
		}
	}

	/** What the list shows, top to bottom: a set's header as "# name", a waypoint as its name; used by UI snapshots. */
	public List<String> shownForSnapshot() {
		List<String> shown = new ArrayList<>();
		for (Item item : buildItems(shownEntries())) {
			if (item instanceof GroupItem group) {
				shown.add("# " + group.title());
			} else if (item instanceof EntryItem entry) {
				shown.add(entry.entry().waypoint().label());
			}
		}
		return shown;
	}

	/** Opens the clear-all confirmation without clearing on confirm; used by UI snapshots. */
	public void openClearDialogForSnapshot() {
		openClearDialog(() -> {
		});
	}

	/** Asks before clearing, since today's Clear Waypoints deleted every waypoint here in one click. */
	private void openClearDialog(Runnable clear) {
		int count = waypoints().size();
		dialog = new UiConfirmDialog(
			font,
			anim,
			Component.translatable(EMUtilsTexts.UI_CLEAR_WAYPOINTS_TITLE),
			Component.translatable(count == 1 ? EMUtilsTexts.UI_CLEAR_WAYPOINTS_MESSAGE_ONE : EMUtilsTexts.UI_CLEAR_WAYPOINTS_MESSAGE, count),
			Component.translatable(EMUtilsTexts.UI_CLEAR_ALL),
			clear
		);
	}

	/** One line of the list: a set's header or a waypoint. */
	private sealed interface Item permits GroupItem, EntryItem {
	}

	private record GroupItem(String key, String title, int count, boolean collapsed) implements Item {
	}

	private record EntryItem(WaypointEntry entry) implements Item {
	}

	private record GroupBox(String key, int x, int y, int width) {
	}

	private record RowBox(WaypointEntry entry, int x, int y, int width, int actionsX, int actionY) {
		String id() {
			return entry.waypoint().id();
		}
	}
}
