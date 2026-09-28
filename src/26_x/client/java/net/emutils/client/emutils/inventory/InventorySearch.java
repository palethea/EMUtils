package net.emutils.client.emutils.inventory;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.ItemLore;
import com.mojang.blaze3d.platform.InputConstants;
import org.jspecify.annotations.Nullable;

/**
 * Inventory Search (#46): a search box above storage screens and the player inventory. Items whose
 * name, ID or lore contain every word typed are outlined and the rest dimmed; a shulker box also
 * counts when something inside it matches, marked in its corner, and its tooltip preview dims what
 * doesn't. The text stays for the next container, so you can go from chest to chest.
 */
public final class InventorySearch {
	private static final int BOX_HEIGHT = 14;
	private static final int BOX_GAP = 3;
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
	private static @Nullable EditBox box;
	private static @Nullable Screen boxScreen;

	private InventorySearch() {
	}

	public static boolean active() {
		EMUtilsConfig config = EMUtilsClient.config();
		return config != null && config.inventoryToolsEnabled() && config.inventorySearch();
	}

	/** Storage screens and the player inventory get a search box; others, like furnaces, don't. */
	public static boolean supports(Screen screen) {
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
			box = null;
			boxScreen = null;
			return;
		}
		if (!config.inventorySearchRemember() && boxScreen != screen) {
			setQuery("");
		}
		Minecraft client = Minecraft.getInstance();
		EditBox search = new EditBox(client.font, leftPos, boxY(topPos), imageWidth, BOX_HEIGHT, Component.translatable(EMUtilsTexts.INVENTORY_SEARCH_HINT));
		search.setMaxLength(MAX_LENGTH);
		search.setHint(Component.translatable(EMUtilsTexts.INVENTORY_SEARCH_HINT));
		search.setValue(query);
		search.setResponder(InventorySearch::setQuery);
		Screens.getWidgets(screen).add(search);
		box = search;
		boxScreen = screen;
	}

	/** Keeps the box above the container when it moves, as with the recipe book in the inventory. */
	public static void follow(Screen screen, int leftPos, int topPos) {
		if (box != null && boxScreen == screen) {
			box.setX(leftPos);
			box.setY(boxY(topPos));
		}
	}

	private static int boxY(int topPos) {
		return Math.max(2, topPos - BOX_HEIGHT - BOX_GAP);
	}

	public static void clearCache() {
		CACHE.clear();
	}

	/**
	 * Typing goes to the box while it's focused, so letters don't close the screen or drop items;
	 * Enter leaves the box. Ctrl+F focuses it.
	 */
	public static boolean handleKeyPressed(Screen screen, KeyEvent input) {
		if (box == null || boxScreen != screen) {
			return false;
		}
		if (box.isFocused()) {
			if (input.key() == InputConstants.KEY_ESCAPE) {
				return false;
			}
			if (input.key() == InputConstants.KEY_RETURN || input.key() == InputConstants.KEY_NUMPADENTER) {
				unfocus(screen);
				return true;
			}
			box.keyPressed(input);
			return true;
		}
		if (input.key() == InputConstants.KEY_F && input.hasControlDown()) {
			screen.setFocused(box);
			box.setFocused(true);
			return true;
		}
		return false;
	}

	/** Clicking anywhere but the box leaves it, so keys act on the inventory again. */
	public static void handleMouseClicked(Screen screen, MouseButtonEvent click) {
		if (box != null && boxScreen == screen && box.isFocused() && !box.isMouseOver(click.x(), click.y())) {
			unfocus(screen);
		}
	}

	private static void unfocus(Screen screen) {
		if (box != null) {
			box.setFocused(false);
		}
		screen.setFocused(null);
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
			box.setValue(value);
		} else {
			setQuery(value);
		}
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

	/** Marks a container slot: an outline when it matches, a dark layer when it doesn't. */
	public static void drawSlot(GuiGraphicsExtractor context, Slot slot) {
		if (slot.isActive()) {
			drawMark(context, slot.getItem(), slot.x, slot.y);
		}
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
}
