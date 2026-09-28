package net.emutils.client.emutils.inventory;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.hub.HubIcons;
import net.emutils.client.emutils.gui.ui.UiIcons;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.gui.ui.UiTextField;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.ItemLore;
import org.jspecify.annotations.Nullable;

/**
 * Inventory Search (#46): a search box above storage screens and the player inventory, in the look of
 * the HUD cards. Items whose name, ID or lore contain every word typed are outlined and the rest dimmed;
 * a shulker box also counts when something inside it matches, marked in its corner, and its tooltip
 * preview dims what doesn't. The text stays for the next container, so you can go from chest to chest.
 * In the creative inventory, if turned on, it only looks at your own inventory.
 */
public final class InventorySearch {
	private static final int BOX_HEIGHT = 16;
	private static final int BOX_GAP = 4;
	/** Room for the creative inventory's tabs, which sit above its panel. */
	private static final int CREATIVE_TABS_HEIGHT = 28;
	private static final int MAX_LENGTH = 50;
	private static final int SLOT_SIZE = 16;
	private static final int MATCH_COLOR = 0xFFFFD447;
	private static final int DIM_COLOR = 0xB0101010;
	private static final int INSIDE_MARK = 4;

	/** How an item relates to the search. */
	public enum Match {
		/** The search is empty or off. */
		NONE,
		/** The item itself matches. */
		ITEM,
		/** Something inside the item, such as a shulker box, matches. */
		INSIDE,
		/** It doesn't match. */
		MISS
	}

	private static String query = "";
	private static List<String> terms = List.of();
	private static final Map<ItemStack, Match> CACHE = new IdentityHashMap<>();
	private static @Nullable SearchBox box;
	private static @Nullable Screen boxScreen;

	private InventorySearch() {
	}

	public static boolean active() {
		EMUtilsConfig config = EMUtilsClient.config();
		return config != null && config.inventoryToolsEnabled() && config.inventorySearch();
	}

	/**
	 * Storage screens and the player inventory get a search box, and the creative inventory when turned
	 * on; others, like furnaces, don't.
	 */
	public static boolean supports(Screen screen) {
		if (screen instanceof CreativeModeInventoryScreen) {
			EMUtilsConfig config = EMUtilsClient.config();
			return config != null && config.inventorySearchCreative();
		}
		return screen instanceof ContainerScreen
			|| screen instanceof ShulkerBoxScreen
			|| screen instanceof HopperScreen
			|| screen instanceof DispenserScreen
			|| screen instanceof InventoryScreen;
	}

	/** Adds the search box to a container screen as it opens or resizes. */
	public static void attach(AbstractContainerScreen<?> screen, int leftPos, int topPos, int imageWidth) {
		EMUtilsConfig config = EMUtilsClient.config();
		if (!active() || !supports(screen)) {
			detach();
			return;
		}
		if (!config.inventorySearchRemember() && boxScreen != screen) {
			setQuery("");
		}
		boolean refocus = box != null && boxScreen == screen && box.field.focused();
		detach();
		SearchBox search = new SearchBox(leftPos, boxY(screen, topPos), imageWidth);
		search.field.setText(query);
		search.field.select(query.length(), query.length());
		Screens.getWidgets(screen).add(search);
		box = search;
		boxScreen = screen;
		if (refocus) {
			screen.setFocused(search);
		}
	}

	/** Lets go of the box when its screen closes, so the game stops taking text input for it. */
	public static void detach() {
		if (box != null) {
			box.field.setFocused(false);
		}
		box = null;
		boxScreen = null;
	}

	/** Keeps the box above the container when it moves, as with the recipe book in the inventory. */
	public static void follow(Screen screen, int leftPos, int topPos) {
		if (box != null && boxScreen == screen) {
			box.setX(leftPos);
			box.setY(boxY(screen, topPos));
		}
	}

	private static int boxY(Screen screen, int topPos) {
		int above = screen instanceof CreativeModeInventoryScreen ? CREATIVE_TABS_HEIGHT : 0;
		return Math.max(2, topPos - above - BOX_HEIGHT - BOX_GAP);
	}

	public static void clearCache() {
		CACHE.clear();
	}

	/**
	 * Typing goes to the box while it's focused, so letters don't close the screen or drop items;
	 * Enter leaves the box, and Escape clears it, then leaves it. Ctrl+F focuses it.
	 */
	public static boolean handleKeyPressed(Screen screen, KeyEvent input) {
		if (box == null || boxScreen != screen) {
			return false;
		}
		if (box.field.focused()) {
			if (input.key() == InputConstants.KEY_RETURN || input.key() == InputConstants.KEY_NUMPADENTER) {
				unfocus(screen);
				return true;
			}
			box.field.keyPressed(input, InventorySearch::textChanged);
			if (!box.field.focused()) {
				screen.setFocused(null);
			}
			return true;
		}
		if (input.key() == InputConstants.KEY_F && input.hasControlDown()) {
			screen.setFocused(box);
			return true;
		}
		return false;
	}

	/** Typed characters for the creative inventory, which would otherwise jump to its own search tab. */
	public static boolean handleCharTyped(Screen screen, CharacterEvent input) {
		return box != null && boxScreen == screen && box.field.focused() && box.charTyped(input);
	}

	/** Clicking anywhere but the box leaves it, so keys act on the inventory again. */
	public static void handleMouseClicked(Screen screen, MouseButtonEvent click) {
		if (box != null && boxScreen == screen && box.field.focused() && !box.isMouseOver(click.x(), click.y())) {
			unfocus(screen);
		}
	}

	private static void unfocus(Screen screen) {
		if (box != null) {
			box.setFocused(false);
		}
		screen.setFocused(null);
	}

	private static void textChanged() {
		if (box != null) {
			setQuery(box.field.text());
		}
	}

	private static void setQuery(String value) {
		query = value;
		List<String> words = new ArrayList<>();
		for (String word : value.toLowerCase(Locale.ROOT).trim().split("\\s+")) {
			if (!word.isEmpty()) {
				words.add(word);
			}
		}
		terms = List.copyOf(words);
		CACHE.clear();
	}

	/** Sets the search text as if typed, for the UI snapshot checks. */
	public static void setQueryForSnapshot(String value) {
		if (box != null) {
			box.field.setText(value);
		}
		setQuery(value);
	}

	/** The text in the box, for the UI snapshot checks. */
	public static String queryForSnapshot() {
		return query;
	}

	public static Match match(ItemStack stack) {
		// Empty slots stay as they are, so only items are dimmed.
		if (terms.isEmpty() || stack.isEmpty() || !active()) {
			return Match.NONE;
		}
		return CACHE.computeIfAbsent(stack, InventorySearch::compute);
	}

	private static Match compute(ItemStack stack) {
		if (matches(stack)) {
			return Match.ITEM;
		}
		EMUtilsConfig config = EMUtilsClient.config();
		ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
		if (config != null && config.inventorySearchShulkers() && contents != null
			&& contents.nonEmptyItemCopyStream().anyMatch(InventorySearch::matches)) {
			return Match.INSIDE;
		}
		return Match.MISS;
	}

	/** Every word must be in the item's name, its ID or a line of its lore. */
	private static boolean matches(ItemStack stack) {
		StringBuilder text = new StringBuilder(stack.getHoverName().getString());
		text.append('\n').append(BuiltInRegistries.ITEM.getKey(stack.getItem()));
		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore != null) {
			for (Component line : lore.lines()) {
				text.append('\n').append(line.getString());
			}
		}
		String haystack = text.toString().toLowerCase(Locale.ROOT);
		for (String term : terms) {
			if (!haystack.contains(term)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Marks a container slot: an outline when it matches, a dark layer when it doesn't. In the creative
	 * inventory only your own inventory's slots, not the item tabs.
	 */
	public static void drawSlot(Screen screen, GuiGraphicsExtractor context, Slot slot) {
		if (!slot.isActive() || boxScreen != screen) {
			return;
		}
		if (screen instanceof CreativeModeInventoryScreen && !(slot.container instanceof Inventory)) {
			return;
		}
		drawMark(context, slot.getItem(), slot.x, slot.y);
	}

	/** Marks an item drawn at ({@code x}, {@code y}), in a slot or a shulker box preview. */
	public static void drawMark(GuiGraphicsExtractor context, ItemStack stack, int x, int y) {
		Match match = match(stack);
		switch (match) {
			case NONE -> {
			}
			case MISS -> {
				EMUtilsConfig config = EMUtilsClient.config();
				if (config != null && config.inventorySearchDim()) {
					context.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, DIM_COLOR);
				}
			}
			case ITEM, INSIDE -> {
				outline(context, x, y);
				if (match == Match.INSIDE) {
					// A filled corner marks a box with a match inside.
					context.fill(x, y, x + INSIDE_MARK, y + INSIDE_MARK, MATCH_COLOR);
				}
			}
		}
	}

	private static void outline(GuiGraphicsExtractor context, int x, int y) {
		context.fill(x - 1, y - 1, x + SLOT_SIZE + 1, y, MATCH_COLOR);
		context.fill(x - 1, y + SLOT_SIZE, x + SLOT_SIZE + 1, y + SLOT_SIZE + 1, MATCH_COLOR);
		context.fill(x - 1, y, x, y + SLOT_SIZE, MATCH_COLOR);
		context.fill(x + SLOT_SIZE, y, x + SLOT_SIZE + 1, y + SLOT_SIZE, MATCH_COLOR);
	}

	/** The search box: a rounded card like the HUD's, with a search icon and the menus' text field. */
	private static final class SearchBox extends AbstractWidget {
		private static final int RADIUS = 6;
		private static final int ICON_SIZE = 9;
		private static final int PADDING = 6;

		private final UiTextField field = new UiTextField(this, MAX_LENGTH);

		SearchBox(int x, int y, int width) {
			super(x, y, width, BOX_HEIGHT, Component.translatable(EMUtilsTexts.INVENTORY_SEARCH_HINT));
		}

		@Override
		public void setFocused(boolean focused) {
			super.setFocused(focused);
			field.setFocused(focused);
		}

		@Override
		public void onClick(MouseButtonEvent click, boolean doubled) {
			field.click(Minecraft.getInstance().font, click.x(), click.hasShiftDown());
		}

		@Override
		public boolean charTyped(CharacterEvent input) {
			return field.charTyped(input, InventorySearch::textChanged);
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
			UiTheme theme = UiTheme.current();
			int x = getX();
			int y = getY();
			int border = field.focused() ? theme.accent() : isHovered() ? theme.muted() : theme.border();
			UiShapes.shadow(context, x, y, width, height, RADIUS, 6, theme.shadow());
			UiShapes.borderedRect(context, x, y, width, height, RADIUS, theme.panel(), border);
			int centerY = y + height / 2;
			UiIcons.draw(context, HubIcons.SEARCH, x + PADDING, centerY - ICON_SIZE / 2, ICON_SIZE, theme.muted());
			int textX = x + PADDING + ICON_SIZE + 5;
			field.draw(context, Minecraft.getInstance().font, theme, textX, centerY, x + width - PADDING - textX, getMessage());
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			defaultButtonNarrationText(output);
		}
	}
}
