package net.emutils.client.emutils.profile.gui;

import java.util.ArrayList;
import java.util.List;
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
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.profile.Profile;
import net.emutils.client.emutils.profile.ProfileManager;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Picks servers for a profile to load on (#89) from the multiplayer server list, plus the server
 * you're on when it isn't saved. Each click adds or removes one, with a check on the ones the profile
 * has, and the list stays open until you click elsewhere.
 */
final class ServerPicker {
	private static final int ROW_HEIGHT = 26;
	private static final int PADDING = 4;
	private static final int FADE_HEIGHT = 8;
	private static final int MAX_ROWS = 6;

	private final Font font;
	private final UiAnim anim;
	private final String key = "server-picker:" + System.identityHashCode(this);
	private final UiScrollArea scroll = new UiScrollArea();
	private final List<Entry> entries;
	private final Predicate<String> included;
	private final Consumer<String> toggle;
	private final int anchorX;
	private final int anchorBelow;
	private final int anchorAbove;
	private final int anchorWidth;
	private int x;
	private int y;
	private int width;
	private int height;
	private boolean closed;

	/**
	 * Opens below {@code anchorBelow} at {@code anchorX}, as wide as {@code anchorWidth}, or above
	 * {@code anchorAbove} when there isn't room below.
	 */
	ServerPicker(Font font, UiAnim anim, int anchorX, int anchorBelow, int anchorAbove, int anchorWidth, Predicate<String> included, Consumer<String> toggle) {
		this.font = font;
		this.anim = anim;
		this.anchorX = anchorX;
		this.anchorBelow = anchorBelow;
		this.anchorAbove = anchorAbove;
		this.anchorWidth = anchorWidth;
		this.included = included;
		this.toggle = toggle;
		this.entries = entries();
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
		closed = true;
	}

	void render(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY, int screenHeight) {
		if (closed) {
			return;
		}
		int rows = Math.max(1, entries.size());
		int listHeight = Math.min(rows, MAX_ROWS) * ROW_HEIGHT;
		width = Math.max(200, anchorWidth);
		height = listHeight + PADDING * 2;
		x = anchorX + anchorWidth - width;
		boolean below = anchorBelow + 4 + height <= screenHeight - 4 || anchorAbove - 4 - height < 4;
		y = below ? anchorBelow + 4 : anchorAbove - 4 - height;
		boolean scrolls = entries.size() > MAX_ROWS;
		scroll.setBounds(x + PADDING, y + PADDING, width - PADDING * 2 + (scrolls ? 2 : UiScrollArea.GUTTER), listHeight);
		scroll.setContentHeight(entries.size() * ROW_HEIGHT);
		scroll.animate(anim, key + ":scroll", mouseX, mouseY);

		float shown = anim.towards(key, 1.0F, 22.0F);
		float opacity = UiOpacity.get();
		UiOpacity.set(opacity * shown);
		context.pose().pushMatrix();
		context.pose().translate(0.0F, (1.0F - shown) * (below ? -3.0F : 3.0F));
		UiShapes.shadow(context, x, y, width, height, 8, 10, theme.shadow());
		UiShapes.borderedRect(context, x, y, width, height, 8, theme.surface(), theme.line());
		if (entries.isEmpty()) {
			UiText.drawCentered(context, font, Component.translatable(EMUtilsTexts.UI_PROFILE_NO_SERVERS), UiText.Size.BODY, x + 12, y + height / 2, theme.muted());
		} else {
			int rowWidth = scroll.contentWidth();
			boolean mouseInList = scroll.contains(mouseX, mouseY) && !scroll.dragging();
			scroll.begin(context);
			context.pose().pushMatrix();
			context.pose().translate(0.0F, scroll.offset() - scroll.exactOffset());
			int top = scroll.y() - scroll.offset();
			for (Entry entry : entries) {
				if (top + ROW_HEIGHT >= scroll.y() && top <= scroll.y() + scroll.height()) {
					boolean hovered = mouseInList && mouseY >= top && mouseY < top + ROW_HEIGHT;
					if (hovered) {
						UiShapes.roundedRect(context, scroll.x(), top, rowWidth, ROW_HEIGHT, 5, theme.hover());
					}
					boolean on = included.test(entry.address());
					int textRight = scroll.x() + rowWidth - 22;
					int nameHeight = UiText.lineHeight(font, UiText.Size.BOLD);
					int subHeight = UiText.lineHeight(font, UiText.Size.SMALL);
					int textTop = top + (ROW_HEIGHT - nameHeight - 2 - subHeight) / 2;
					UiText.draw(context, font, UiText.ellipsize(font, Component.literal(entry.name()), UiText.Size.BOLD, textRight - scroll.x() - 8), UiText.Size.BOLD, scroll.x() + 8, textTop, theme.text());
					UiText.draw(context, font, UiText.ellipsize(font, Component.literal(entry.address()), UiText.Size.SMALL, textRight - scroll.x() - 8), UiText.Size.SMALL, scroll.x() + 8, textTop + nameHeight + 2, theme.muted());
					if (on) {
						UiIcons.draw(context, HubIcons.CHECK, scroll.x() + rowWidth - 17, top + (ROW_HEIGHT - 11) / 2, 11, theme.accent());
					}
				}
				top += ROW_HEIGHT;
			}
			context.pose().popMatrix();
			scroll.end(context, theme.surface(), FADE_HEIGHT, UiTheme.fade(theme.text(), 0.25F), UiTheme.fade(theme.text(), 0.45F));
		}
		context.pose().popMatrix();
		UiOpacity.set(opacity);
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
			closed = true;
			return false;
		}
		if (scroll.mouseClicked(mouseX, mouseY)) {
			return true;
		}
		if (scroll.contains(mouseX, mouseY)) {
			int index = (int) Math.floor((mouseY - scroll.y() + scroll.exactOffset()) / ROW_HEIGHT);
			if (index >= 0 && index < entries.size()) {
				toggle.accept(entries.get(index).address());
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
		return contains(mouseX, mouseY, x, y, width, height) && scroll.scroll(mouseX, mouseY, amount);
	}

	/** The first server listed, or null; used by UI snapshots. */
	@Nullable String firstAddressForSnapshot() {
		return entries.isEmpty() ? null : entries.getFirst().address();
	}

	private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private record Entry(String name, String address) {
	}
}
