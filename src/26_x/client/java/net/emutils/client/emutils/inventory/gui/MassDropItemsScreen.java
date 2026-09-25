package net.emutils.client.emutils.inventory.gui;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.gui.hub.HubIcons;
import net.emutils.client.emutils.gui.ui.UiIcons;
import net.emutils.client.emutils.gui.ui.UiOpacity;
import net.emutils.client.emutils.gui.ui.UiPanelScreen;
import net.emutils.client.emutils.gui.ui.UiScrollArea;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.emutils.gui.ui.UiTextField;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.gui.ui.UiWidgets;
import net.emutils.client.emutils.inventory.MassDropMode;
import net.emutils.client.emutils.inventory.MassDropStore;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * The Mass Drop list in the new UI (#134): the items the drop key throws out, each with how many you
 * carry, and a search over every item to add or remove more. The Legit/Unfair mode and the drop key sit
 * in the header.
 */
public final class MassDropItemsScreen extends UiPanelScreen {
	private static final int PADDING = 16;
	private static final int FIELD_HEIGHT = 24;
	private static final int ROW_HEIGHT = 34;
	private static final int ROW_GAP = 4;
	private static final int ACTION = 20;
	private static final int FADE_HEIGHT = 10;
	private static final int PLAYER_INVENTORY_SIZE = 36;
	private static final List<MassDropMode> MODES = List.of(MassDropMode.LEGIT, MassDropMode.UNFAIR);

	private final UiTextField search = new UiTextField(this, 64);
	private final UiScrollArea scroll = new UiScrollArea();
	private final List<RowBox> rows = new ArrayList<>();
	/** Every item with its ID and name, gathered on the first search. */
	private @Nullable List<ItemInfo> allItems;
	private List<ItemInfo> shown = List.of();
	private String shownQuery = "";
	private int shownVersion = -1;
	private int listVersion;
	private int headerButtonsY;
	private int modeX;
	private int[] modeEdges = new int[0];
	private int searchY;
	private int infoY;
	private @Nullable Component tooltip;
	private int tooltipX;
	private int tooltipY;

	public MassDropItemsScreen(@Nullable Screen parent) {
		super(Component.translatable("emutils.mass_drop.title"), parent);
		search.setFocused(true);
	}

	@Override
	protected int maxPanelWidth() {
		return 560;
	}

	@Override
	protected void layout() {
		int headerHeight = UiText.lineHeight(font, UiText.Size.HEADING) + 6 + UiText.lineHeight(font, UiText.Size.BODY);
		headerButtonsY = panelY + PADDING + (headerHeight - UiWidgets.SEGMENT_HEIGHT) / 2;
		searchY = panelY + PADDING + headerHeight + 12;
		infoY = searchY + FIELD_HEIGHT + 10;
		int listTop = infoY + UiText.lineHeight(font, UiText.Size.BODY) + 10;
		scroll.setBounds(panelX + PADDING, listTop, panelWidth - PADDING * 2 + UiScrollArea.GUTTER, panelY + panelHeight - PADDING / 2 - listTop);
		search.restoreFocus();
	}

	private static MassDropStore store() {
		return EMUtilsClient.massDrop().store();
	}

	private List<ItemInfo> allItems() {
		if (allItems == null) {
			List<ItemInfo> items = new ArrayList<>();
			for (Item item : BuiltInRegistries.ITEM) {
				if (item != Items.AIR) {
					ItemStack stack = item.getDefaultInstance();
					items.add(new ItemInfo(item, BuiltInRegistries.ITEM.getKey(item).toString(), stack.getHoverName().getString(), stack));
				}
			}
			items.sort(Comparator.comparing(ItemInfo::name, String.CASE_INSENSITIVE_ORDER));
			allItems = items;
		}
		return allItems;
	}

	/**
	 * The rows to show: the dropped items while the search is empty, otherwise every item whose name or
	 * ID matches, names starting with the query first.
	 */
	private List<ItemInfo> shownItems() {
		String query = search.text().trim().toLowerCase(Locale.ROOT);
		if (!query.equals(shownQuery) || shownVersion != listVersion) {
			shownQuery = query;
			shownVersion = listVersion;
			List<ItemInfo> items = new ArrayList<>();
			for (ItemInfo info : allItems()) {
				if (query.isEmpty() ? store().contains(info.id()) : info.matches(query)) {
					items.add(info);
				}
			}
			if (!query.isEmpty()) {
				items.sort(Comparator.comparing((ItemInfo info) -> !info.name().toLowerCase(Locale.ROOT).startsWith(query)));
			}
			shown = items;
		}
		return shown;
	}

	private void toggle(ItemInfo info) {
		store().toggle(info.id());
		listVersion++;
	}

	/** How many of {@code item} are in the player's inventory right now. */
	private int carried(Item item) {
		if (minecraft.player == null) {
			return 0;
		}
		Inventory inventory = minecraft.player.getInventory();
		int count = 0;
		for (int slot = 0; slot < PLAYER_INVENTORY_SIZE; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private @Nullable KeyMapping dropKey() {
		for (KeyMapping mapping : minecraft.options.keyMappings) {
			if (mapping.getName().equals("key.emutils.mass_drop")) {
				return mapping;
			}
		}
		return null;
	}

	// ---- drawing --------------------------------------------------------------------------------

	@Override
	protected void drawPanel(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		tooltip = null;
		int hoverX = closing() ? Integer.MIN_VALUE / 2 : mouseX;
		int hoverY = closing() ? Integer.MIN_VALUE / 2 : mouseY;
		drawHeader(context, theme, hoverX, hoverY);
		drawSearch(context, theme);
		drawRows(context, theme, shownItems(), hoverX, hoverY);
	}

	private void drawHeader(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		int left = panelX + PADDING;
		int top = panelY + PADDING;
		UiText.draw(context, font, title, UiText.Size.HEADING, left, top, theme.text());
		int count = store().itemIds().size();
		Component countText = Component.translatable(count == 1 ? EMUtilsTexts.UI_MASS_DROP_COUNT_ONE : EMUtilsTexts.UI_MASS_DROP_COUNT, count);
		UiText.draw(context, font, countText, UiText.Size.BODY, left, top + UiText.lineHeight(font, UiText.Size.HEADING) + 6, theme.muted());

		List<Component> labels = MODES.stream().map(mode -> (Component) Component.translatable(mode.labelKey())).toList();
		modeX = panelX + panelWidth - PADDING - UiWidgets.segmentedWidth(font, labels);
		float selection = anim.transition("mass-drop-mode", MODES.indexOf(store().mode()), 0.18F);
		int[] edges = UiWidgets.segmentEdges(font, modeX, labels);
		int hovered = -1;
		for (int i = 0; i < labels.size(); i++) {
			if (contains(mouseX, mouseY, edges[i], headerButtonsY, edges[i + 1] - edges[i], UiWidgets.SEGMENT_HEIGHT)) {
				hovered = i;
			}
		}
		modeEdges = UiWidgets.segmented(context, font, theme, modeX, headerButtonsY, labels, selection, hovered);
	}

	private void drawSearch(GuiGraphicsExtractor context, UiTheme theme) {
		int left = panelX + PADDING;
		int width = panelWidth - PADDING * 2;
		UiShapes.borderedRect(context, left, searchY, width, FIELD_HEIGHT, 8, theme.surface(), search.focused() ? theme.accent() : theme.line());
		int center = searchY + FIELD_HEIGHT / 2;
		UiIcons.draw(context, HubIcons.SEARCH, left + 9, center - 5, 10, theme.textSecondary());
		search.draw(context, font, theme, left + 26, center, width - 36, Component.translatable(EMUtilsTexts.UI_MASS_DROP_SEARCH));

		// What the drop key does in this mode, and which key it is.
		int x = left + 1;
		int lineCenter = infoY + UiText.lineHeight(font, UiText.Size.BODY) / 2;
		Component keyLabel = Component.translatable(EMUtilsTexts.UI_MASS_DROP_KEY);
		UiText.drawCentered(context, font, keyLabel, UiText.Size.BODY, x, lineCenter, theme.muted());
		x += UiText.width(font, keyLabel, UiText.Size.BODY) + 6;
		KeyMapping key = dropKey();
		Component keyName = key == null || key.isUnbound() ? Component.translatable(EMUtilsTexts.UI_NOT_BOUND) : key.getTranslatedKeyMessage();
		UiWidgets.keycap(context, font, theme, x, lineCenter - UiWidgets.KEYCAP_HEIGHT / 2, keyName, false, key == null || key.isUnbound(), 0.0F);
		x += UiWidgets.keycapWidth(font, keyName) + 8;
		Component description = Component.translatable(store().mode() == MassDropMode.LEGIT ? EMUtilsTexts.UI_MASS_DROP_LEGIT_DESC : EMUtilsTexts.UI_MASS_DROP_UNFAIR_DESC);
		UiText.drawCentered(context, font, UiText.ellipsize(font, description, UiText.Size.BODY, left + width - x), UiText.Size.BODY, x, lineCenter, theme.muted());
	}

	private void drawRows(GuiGraphicsExtractor context, UiTheme theme, List<ItemInfo> items, int mouseX, int mouseY) {
		scroll.setContentHeight(items.isEmpty() ? 0 : items.size() * (ROW_HEIGHT + ROW_GAP) - ROW_GAP + FADE_HEIGHT);
		scroll.animate(anim, "mass-drop-scroll", mouseX, mouseY);
		rows.clear();
		boolean searching = !search.text().isBlank();
		boolean mouseInList = scroll.contains(mouseX, mouseY) && !scroll.dragging();
		scroll.begin(context);
		context.pose().pushMatrix();
		context.pose().translate(0.0F, scroll.offset() - scroll.exactOffset());
		int width = scroll.contentWidth();
		int y = scroll.y() + FADE_HEIGHT / 2 - scroll.offset();
		for (ItemInfo info : items) {
			if (y + ROW_HEIGHT >= scroll.y() && y <= scroll.y() + scroll.height()) {
				drawRow(context, theme, info, searching, scroll.x(), y, width, mouseInList ? mouseX : Integer.MIN_VALUE / 2, mouseY);
				rows.add(new RowBox(info, y));
			}
			y += ROW_HEIGHT + ROW_GAP;
		}
		context.pose().popMatrix();
		if (items.isEmpty()) {
			Component text = searching
				? Component.translatable(EMUtilsTexts.UI_MASS_DROP_NO_MATCH, search.text().trim())
				: Component.translatable(EMUtilsTexts.UI_MASS_DROP_EMPTY);
			int centerX = scroll.x() + width / 2;
			int top = scroll.y() + Math.max(16, scroll.height() / 2 - 40);
			UiIcons.draw(context, HubIcons.BACKPACK, centerX - 11, top, 22, theme.muted());
			List<Component> lines = UiText.wrap(font, text, UiText.Size.BODY, Math.min(width - 40, 300));
			for (int i = 0; i < lines.size(); i++) {
				Component line = lines.get(i);
				UiText.draw(context, font, line, UiText.Size.BODY, centerX - UiText.width(font, line, UiText.Size.BODY) / 2, top + 32 + i * 11, theme.muted());
			}
		}
		scroll.end(context, theme.panel(), FADE_HEIGHT, UiTheme.fade(theme.text(), 0.25F), UiTheme.fade(theme.text(), 0.45F));
	}

	private void drawRow(GuiGraphicsExtractor context, UiTheme theme, ItemInfo info, boolean searching, int x, int y, int width, int mouseX, int mouseY) {
		boolean added = store().contains(info.id());
		boolean hovered = contains(mouseX, mouseY, x, y, width, ROW_HEIGHT);
		float hover = anim.towards("mass-drop:" + info.id(), hovered && searching, 16.0F);
		UiShapes.roundedRect(context, x, y, width, ROW_HEIGHT, 8, UiTheme.mix(theme.surface(), theme.surfaceHover(), hover));
		int center = y + ROW_HEIGHT / 2;
		// Minecraft draws item icons without the panel's fade, so they wait until it has faded in.
		if (UiOpacity.get() >= 0.999F) {
			context.item(info.stack(), x + 9, center - 8, 0);
		}
		int textX = x + 34;
		int right = x + width - 8;

		if (searching) {
			// Added items show a check; the rest an Add hint, and clicking the row switches it.
			Component label = Component.translatable(added ? EMUtilsTexts.UI_MASS_DROP_ADDED : EMUtilsTexts.UI_ADD);
			int labelWidth = UiText.width(font, label, UiText.Size.LABEL);
			int labelX = right - 4 - labelWidth;
			UiText.drawCentered(context, font, label, UiText.Size.LABEL, labelX, center, added ? theme.accent() : hovered ? theme.text() : theme.textSecondary());
			UiIcons.draw(context, added ? HubIcons.CHECK : HubIcons.PLUS, labelX - 14, center - 5, 10, added ? theme.accent() : hovered ? theme.text() : theme.textSecondary());
			right = labelX - 22;
		} else {
			int actionX = right - ACTION;
			int actionY = center - ACTION / 2;
			boolean removeHovered = contains(mouseX, mouseY, actionX, actionY, ACTION, ACTION);
			UiWidgets.ghostIconButton(context, theme, actionX, actionY, ACTION, HubIcons.X, removeHovered ? theme.warning() : theme.textSecondary(), removeHovered ? 1.0F : 0.0F);
			if (removeHovered) {
				tooltip = Component.translatable(EMUtilsTexts.UI_MASS_DROP_REMOVE);
				tooltipX = mouseX;
				tooltipY = mouseY;
			}
			int count = carried(info.item());
			right = actionX - 10;
			if (count > 0) {
				Component carried = Component.translatable(EMUtilsTexts.UI_MASS_DROP_CARRIED, count);
				int carriedWidth = UiText.width(font, carried, UiText.Size.BODY);
				UiText.drawCentered(context, font, carried, UiText.Size.BODY, right - carriedWidth, center, theme.muted());
				right -= carriedWidth + 10;
			}
		}
		int textWidth = right - textX;
		UiText.draw(context, font, UiText.ellipsize(font, Component.literal(info.name()), UiText.Size.BODY, textWidth), UiText.Size.BODY, textX, y + 6, theme.text());
		UiText.draw(context, font, UiText.ellipsize(font, Component.literal(info.id()), UiText.Size.SMALL, textWidth), UiText.Size.SMALL, textX, y + 19, theme.muted());
	}

	@Override
	protected void drawOverlay(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		if (tooltip != null) {
			UiWidgets.tooltip(context, font, theme, tooltip, tooltipX, tooltipY, width, height);
		}
	}

	// ---- input ----------------------------------------------------------------------------------

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		if (closing()) {
			return true;
		}
		double mouseX = click.x();
		double mouseY = click.y();
		if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) {
			return super.mouseClicked(click, doubled);
		}
		for (int i = 0; i + 1 < modeEdges.length; i++) {
			if (contains(mouseX, mouseY, modeEdges[i], headerButtonsY, modeEdges[i + 1] - modeEdges[i], UiWidgets.SEGMENT_HEIGHT)) {
				store().setMode(MODES.get(i));
				return true;
			}
		}
		boolean onSearch = contains(mouseX, mouseY, panelX + PADDING, searchY, panelWidth - PADDING * 2, FIELD_HEIGHT);
		search.setFocused(onSearch);
		if (onSearch) {
			search.click(font, mouseX, click.hasShiftDown());
			return true;
		}
		if (scroll.mouseClicked(mouseX, mouseY)) {
			return true;
		}
		if (scroll.contains(mouseX, mouseY)) {
			boolean searching = !search.text().isBlank();
			int right = scroll.x() + scroll.contentWidth() - 8;
			for (RowBox row : rows) {
				if (mouseY < row.y() || mouseY >= row.y() + ROW_HEIGHT) {
					continue;
				}
				// Searching, the whole row switches the item; in the list, only its remove button does.
				if (searching || contains(mouseX, mouseY, right - ACTION, row.y() + (ROW_HEIGHT - ACTION) / 2, ACTION, ACTION)) {
					toggle(row.info());
				}
				return true;
			}
		}
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
		if (scroll.mouseDragged(click.y())) {
			return true;
		}
		return super.mouseDragged(click, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
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
		if (input.isEscape() && !search.text().isEmpty()) {
			search.setText("");
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
		if (!search.focused()) {
			search.setFocused(true);
		}
		if (search.charTyped(input, scroll::reset)) {
			return true;
		}
		return super.charTyped(input);
	}

	@Override
	public void onClose() {
		search.setFocused(false);
		super.onClose();
	}

	/** Types into the search, as the player would; used by UI snapshots. */
	public void searchForSnapshot(String text) {
		search.setText(text);
		search.select(text.length(), text.length());
	}

	/** Adds or removes the first item the search shows; used by UI snapshots. */
	public void toggleFirstForSnapshot() {
		List<ItemInfo> items = shownItems();
		if (!items.isEmpty()) {
			toggle(items.getFirst());
		}
	}

	/** The IDs of the rows shown right now; used by UI snapshots. */
	public List<String> shownForSnapshot() {
		return shownItems().stream().map(ItemInfo::id).toList();
	}

	private record ItemInfo(Item item, String id, String name, ItemStack stack) {
		boolean matches(String query) {
			return name.toLowerCase(Locale.ROOT).contains(query) || id.contains(query);
		}
	}

	private record RowBox(ItemInfo info, int y) {
	}
}
