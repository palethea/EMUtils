package net.emutils.client.emutils.profile.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.gui.hub.HubIcons;
import net.emutils.client.emutils.gui.ui.UiAnim;
import net.emutils.client.emutils.gui.ui.UiIcons;
import net.emutils.client.emutils.gui.ui.UiOpacity;
import net.emutils.client.emutils.gui.ui.UiScrollArea;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.emutils.gui.ui.UiTextField;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.profile.Profile;
import net.emutils.client.emutils.profile.ProfileManager;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Picks servers for a profile to load on (#89) from the multiplayer server list, plus the server
 * you're on when it isn't saved. Each click adds or removes one, with a check on the ones the profile
 * has, and the list stays open until you click elsewhere.
 *
 * <p>Typing filters the list by name or address, and Enter picks the first match, so a long server
 * list doesn't have to be scrolled through. The list opens below the field, or above it when there's
 * more room there, and shows fewer rows (still scrolling) when neither side fits six.
 */
final class ServerPicker {
	private static final int ROW_HEIGHT = 26;
	private static final int FILTER_HEIGHT = 20;
	private static final int PADDING = 4;
	private static final int FADE_HEIGHT = 8;
	private static final int MAX_ROWS = 6;
	private static final int MARGIN = 4;

	private final Font font;
	private final UiAnim anim;
	private final String key = "server-picker:" + System.identityHashCode(this);
	private final UiScrollArea scroll = new UiScrollArea();
	private final UiTextField filter = new UiTextField(this, 64);
	private final List<Entry> entries;
	private final Predicate<String> included;
	private final Consumer<String> toggle;
	private List<Entry> visible;
	private int x;
	private int y;
	private int width;
	private int height;
	private boolean closed;

	ServerPicker(Font font, UiAnim anim, Predicate<String> included, Consumer<String> toggle) {
		this.font = font;
		this.anim = anim;
		this.included = included;
		this.toggle = toggle;
		this.entries = entries();
		this.visible = entries;
		filter.setFocused(true);
		anim.set(key, 0.0F);
	}

	/** The saved servers, with the one you're on first when it isn't among them. */
	private static List<Entry> entries() {
		Minecraft client = Minecraft.getInstance();
		List<Entry> entries = new ArrayList<>();
		try {
			ServerList list = new ServerList(client);
			list.load();
			for (int i = 0; i < list.size(); i++) {
				ServerData server = list.get(i);
				String address = server.ip == null ? "" : Profile.normalizeAddress(server.ip);
				if (!address.isEmpty() && entries.stream().noneMatch(entry -> entry.address().equals(address))) {
					entries.add(new Entry(server.name == null || server.name.isBlank() ? address : server.name, address));
				}
			}
		} catch (RuntimeException exception) {
			EMUtilsClient.LOGGER.warn("Could not read the multiplayer server list for the profile server picker.", exception);
		}
		String current = ProfileManager.currentServerAddress(client);
		if (current != null && entries.stream().noneMatch(entry -> entry.address().equals(current))) {
			entries.addFirst(new Entry(Component.translatable(EMUtilsTexts.UI_PROFILE_THIS_SERVER).getString(), current));
		}
		return entries;
	}

	boolean isClosed() {
		return closed;
	}

	void close() {
		if (!closed) {
			closed = true;
			filter.setFocused(false);
		}
	}

	private void refilter() {
		String query = filter.text().strip().toLowerCase(Locale.ROOT);
		visible = query.isEmpty()
			? entries
			: entries.stream().filter(entry -> entry.name().toLowerCase(Locale.ROOT).contains(query) || entry.address().contains(query)).toList();
		scroll.reset();
	}

	/**
	 * Lays the list out against the servers field, which spans {@code fieldX} to {@code fieldX + fieldWidth}
	 * from {@code fieldTop} to {@code fieldBottom}; it's read every frame, so the list follows the sheet
	 * when the window resizes.
	 */
	private void layout(int fieldX, int fieldTop, int fieldBottom, int fieldWidth, int screenHeight) {
		int chrome = PADDING * 2 + FILTER_HEIGHT + 4;
		int roomBelow = screenHeight - MARGIN - (fieldBottom + 4);
		int roomAbove = fieldTop - 4 - MARGIN;
		int wantedRows = Math.clamp(visible.size(), 1, MAX_ROWS);
		int wanted = chrome + wantedRows * ROW_HEIGHT;
		boolean below = roomBelow >= wanted || roomBelow >= roomAbove;
		int room = below ? roomBelow : roomAbove;
		// Fewer rows when the window is short; the rest scroll.
		int rows = Math.clamp((room - chrome) / ROW_HEIGHT, 1, wantedRows);
		int listHeight = rows * ROW_HEIGHT;
		width = Math.max(200, fieldWidth);
		height = chrome + listHeight;
		x = fieldX + fieldWidth - width;
		y = below ? fieldBottom + 4 : fieldTop - 4 - height;
		y = Math.clamp(y, MARGIN, Math.max(MARGIN, screenHeight - MARGIN - height));
		boolean scrolls = visible.size() > rows;
		scroll.setBounds(x + PADDING, y + PADDING + FILTER_HEIGHT + 4, width - PADDING * 2 + (scrolls ? 2 : UiScrollArea.GUTTER), listHeight);
		scroll.setContentHeight(visible.size() * ROW_HEIGHT);
	}

	void render(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY, int fieldX, int fieldTop, int fieldBottom, int fieldWidth, int screenHeight) {
		if (closed) {
			return;
		}
		layout(fieldX, fieldTop, fieldBottom, fieldWidth, screenHeight);
		scroll.animate(anim, key + ":scroll", mouseX, mouseY);

		float shown = anim.towards(key, 1.0F, 22.0F);
		float opacity = UiOpacity.get();
		UiOpacity.set(opacity * shown);
		context.pose().pushMatrix();
		context.pose().translate(0.0F, (1.0F - shown) * -3.0F);
		UiShapes.shadow(context, x, y, width, height, 8, 10, theme.shadow());
		UiShapes.borderedRect(context, x, y, width, height, 8, theme.surface(), theme.line());

		// The filter, always ready for typing.
		int filterY = y + PADDING;
		UiShapes.roundedRect(context, x + PADDING, filterY, width - PADDING * 2, FILTER_HEIGHT, 6, theme.segmentBackground());
		UiIcons.draw(context, HubIcons.SEARCH, x + PADDING + 7, filterY + (FILTER_HEIGHT - 9) / 2, 9, theme.muted());
		filter.draw(context, font, theme, x + PADDING + 22, filterY + FILTER_HEIGHT / 2, width - PADDING * 2 - 30, Component.translatable(EMUtilsTexts.UI_PROFILE_FILTER_SERVERS));

		if (visible.isEmpty()) {
			Component empty = Component.translatable(entries.isEmpty() ? EMUtilsTexts.UI_PROFILE_NO_SERVERS : EMUtilsTexts.UI_PROFILE_NO_MATCHING_SERVERS);
			UiText.drawCentered(context, font, empty, UiText.Size.BODY, x + 12, scroll.y() + ROW_HEIGHT / 2, theme.muted());
		} else {
			drawRows(context, theme, mouseX, mouseY);
		}
		context.pose().popMatrix();
		UiOpacity.set(opacity);
	}

	private void drawRows(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		int rowWidth = scroll.contentWidth();
		boolean mouseInList = scroll.contains(mouseX, mouseY) && !scroll.dragging();
		scroll.begin(context);
		context.pose().pushMatrix();
		// Rows sit at whole pixels, and the scroll's fraction is a translation, so scrolling glides.
		context.pose().translate(0.0F, scroll.offset() - scroll.exactOffset());
		int top = scroll.y() - scroll.offset();
		for (int i = 0; i < visible.size(); i++) {
			Entry entry = visible.get(i);
			if (top + ROW_HEIGHT >= scroll.y() && top <= scroll.y() + scroll.height()) {
				boolean hovered = mouseInList && mouseY >= top && mouseY < top + ROW_HEIGHT;
				// With a filter typed, the first match is what Enter picks.
				boolean enterTarget = i == 0 && !filter.text().isBlank();
				if (hovered || enterTarget) {
					UiShapes.roundedRect(context, scroll.x(), top, rowWidth, ROW_HEIGHT, 5, theme.hover());
				}
				int textRight = scroll.x() + rowWidth - 22;
				int nameHeight = UiText.lineHeight(font, UiText.Size.BOLD);
				int subHeight = UiText.lineHeight(font, UiText.Size.SMALL);
				int textTop = top + (ROW_HEIGHT - nameHeight - 2 - subHeight) / 2;
				UiText.draw(context, font, UiText.ellipsize(font, Component.literal(entry.name()), UiText.Size.BOLD, textRight - scroll.x() - 8), UiText.Size.BOLD, scroll.x() + 8, textTop, theme.text());
				UiText.draw(context, font, UiText.ellipsize(font, Component.literal(entry.address()), UiText.Size.SMALL, textRight - scroll.x() - 8), UiText.Size.SMALL, scroll.x() + 8, textTop + nameHeight + 2, theme.muted());
				if (included.test(entry.address())) {
					UiIcons.draw(context, HubIcons.CHECK, scroll.x() + rowWidth - 17, top + (ROW_HEIGHT - 11) / 2, 11, theme.accent());
				}
			}
			top += ROW_HEIGHT;
		}
		context.pose().popMatrix();
		scroll.end(context, theme.surface(), FADE_HEIGHT, UiTheme.fade(theme.text(), 0.25F), UiTheme.fade(theme.text(), 0.45F));
	}

	/**
	 * Handles a click while the list is open. A click on a server adds or removes it and keeps the list
	 * open; a click elsewhere closes it. Returns true when the click was on the list.
	 */
	boolean mouseClicked(double mouseX, double mouseY) {
		if (closed) {
			return false;
		}
		if (!contains(mouseX, mouseY, x, y, width, height)) {
			close();
			return false;
		}
		if (contains(mouseX, mouseY, x + PADDING, y + PADDING, width - PADDING * 2, FILTER_HEIGHT)) {
			filter.click(font, mouseX, false);
			return true;
		}
		if (scroll.mouseClicked(mouseX, mouseY)) {
			return true;
		}
		if (scroll.contains(mouseX, mouseY)) {
			int index = (int) Math.floor((mouseY - scroll.y() + scroll.exactOffset()) / ROW_HEIGHT);
			if (index >= 0 && index < visible.size()) {
				toggle.accept(visible.get(index).address());
			}
		}
		return true;
	}

	void mouseDragged(double mouseY) {
		scroll.mouseDragged(mouseY);
	}

	void mouseReleased() {
		scroll.mouseReleased();
	}

	boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if (!contains(mouseX, mouseY, x, y, width, height)) {
			return false;
		}
		scroll.scroll(mouseX, mouseY, amount);
		return true;
	}

	/** Esc clears the filter, or closes the list when it's empty; Enter picks the first match. */
	void keyPressed(KeyEvent input) {
		if (input.isEscape()) {
			if (filter.text().isEmpty()) {
				close();
			} else {
				filter.setText("");
				refilter();
			}
			return;
		}
		if (input.isConfirmation()) {
			if (!filter.text().isBlank() && !visible.isEmpty()) {
				toggle.accept(visible.getFirst().address());
				filter.setText("");
				refilter();
			}
			return;
		}
		filter.keyPressed(input, this::refilter);
	}

	void charTyped(CharacterEvent input) {
		filter.charTyped(input, this::refilter);
	}

	/** The first server listed, or null; used by UI snapshots. */
	@Nullable String firstAddressForSnapshot() {
		return visible.isEmpty() ? null : visible.getFirst().address();
	}

	/** How many servers are listed with the current filter; used by UI snapshots. */
	int visibleCountForSnapshot() {
		return visible.size();
	}

	/** Scrolls to the last server; used by UI snapshots. Returns whether the list scrolls at all. */
	boolean scrollToEndForSnapshot() {
		scroll.scrollTo(scroll.maxScroll());
		return scroll.maxScroll() > 0;
	}

	/** Whether the whole list is on a screen this tall; used by UI snapshots. */
	boolean fitsForSnapshot(int screenHeight) {
		return y >= 0 && y + height <= screenHeight;
	}

	private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private record Entry(String name, String address) {
	}
}
