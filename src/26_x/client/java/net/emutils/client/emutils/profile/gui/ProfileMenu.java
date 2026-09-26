package net.emutils.client.emutils.profile.gui;

import java.util.List;
import java.util.function.Consumer;
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
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * The profile switcher's list (#89), opened from the profile button in the settings header: every
 * profile with its icon and when it loads, a check on the active one, and a way to manage them. Like
 * a right-click menu, clicking anywhere closes it, and clicking a profile switches to it. When the
 * profiles don't fit below the button, the list scrolls.
 */
public final class ProfileMenu {
	private static final int ROW_HEIGHT = 30;
	private static final int MANAGE_HEIGHT = 22;
	private static final int PADDING = 5;
	private static final int BADGE = 18;
	private static final int MIN_WIDTH = 190;
	private static final int MAX_WIDTH = 260;
	private static final int FADE_HEIGHT = 8;
	/** Always room for this many rows, even if the menu then has to move up. */
	private static final int MIN_VISIBLE_ROWS = 3;

	private final Font font;
	private final UiAnim anim;
	private final String key = "profile-menu:" + System.identityHashCode(this);
	private final UiScrollArea scroll = new UiScrollArea();
	private final int anchorRight;
	private final int anchorTop;
	private final Consumer<Profile> onPick;
	private final Runnable onManage;
	private List<Profile> profiles = List.of();
	private int x;
	private int y;
	private int width;
	private int height;
	private int listHeight;
	private boolean scrolls;
	private boolean closed;

	/** Opens below {@code anchorTop}, its right edge at {@code anchorRight}. */
	public ProfileMenu(Font font, UiAnim anim, int anchorRight, int anchorTop, Consumer<Profile> onPick, Runnable onManage) {
		this.font = font;
		this.anim = anim;
		this.anchorRight = anchorRight;
		this.anchorTop = anchorTop;
		this.onPick = onPick;
		this.onManage = onManage;
		anim.set(key, 0.0F);
	}

	public boolean isClosed() {
		return closed;
	}

	public void close() {
		closed = true;
	}

	private void layout(int screenWidth, int screenHeight) {
		profiles = EMUtilsClient.profiles().profiles();
		width = MIN_WIDTH;
		for (Profile profile : profiles) {
			width = Math.max(width, PADDING * 2 + 8 + BADGE + 8 + UiText.width(font, profile.name(), UiText.Size.BOLD) + 24);
		}
		width = Math.min(width, Math.min(MAX_WIDTH, screenWidth - 8));
		int chrome = PADDING * 2 + 5 + MANAGE_HEIGHT;
		int fullList = profiles.size() * ROW_HEIGHT;
		int room = Math.max(MIN_VISIBLE_ROWS * ROW_HEIGHT, screenHeight - anchorTop - 6 - chrome);
		listHeight = Math.min(fullList, Math.min(room, screenHeight - 12 - chrome));
		scrolls = listHeight < fullList;
		height = chrome + listHeight;
		x = Math.max(4, anchorRight - width);
		y = Math.max(4, Math.min(anchorTop, screenHeight - height - 6));
		int listWidth = width - PADDING * 2 + (scrolls ? UiScrollArea.GUTTER : 0);
		scroll.setBounds(x + PADDING, y + PADDING, listWidth - (scrolls ? UiScrollArea.GUTTER - 2 : 0), listHeight);
		scroll.setContentHeight(fullList);
	}

	public void render(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY, int screenWidth, int screenHeight) {
		if (closed) {
			return;
		}
		layout(screenWidth, screenHeight);
		Profile active = EMUtilsClient.profiles().active();
		scroll.animate(anim, key + ":scroll", mouseX, mouseY);

		float shown = anim.towards(key, 1.0F, 22.0F);
		UiOpacity.set(shown);
		context.pose().pushMatrix();
		context.pose().translate(0.0F, (1.0F - shown) * -3.0F);
		UiShapes.shadow(context, x, y, width, height, 10, 12, theme.shadow());
		UiShapes.borderedRect(context, x, y, width, height, 10, theme.surface(), theme.line());

		int rowWidth = scroll.contentWidth();
		boolean mouseInList = scroll.contains(mouseX, mouseY) && !scroll.dragging();
		scroll.begin(context);
		context.pose().pushMatrix();
		// Rows sit at whole pixels, and the scroll's fraction is a translation, so scrolling glides.
		context.pose().translate(0.0F, scroll.offset() - scroll.exactOffset());
		int top = scroll.y() - scroll.offset();
		for (Profile profile : profiles) {
			if (top + ROW_HEIGHT >= scroll.y() && top <= scroll.y() + scroll.height()) {
				drawRow(context, theme, profile, profile.id().equals(active.id()), top, rowWidth, mouseInList && contains(mouseX, mouseY, scroll.x(), top, rowWidth, ROW_HEIGHT));
			}
			top += ROW_HEIGHT;
		}
		context.pose().popMatrix();
		scroll.end(context, theme.surface(), FADE_HEIGHT, UiTheme.fade(theme.text(), 0.25F), UiTheme.fade(theme.text(), 0.45F));

		int manageTop = y + PADDING + listHeight + 5;
		context.fill(x + PADDING + 2, manageTop - 3, x + width - PADDING - 2, manageTop - 2, UiOpacity.apply(theme.line()));
		if (contains(mouseX, mouseY, x + PADDING, manageTop, width - PADDING * 2, MANAGE_HEIGHT)) {
			UiShapes.roundedRect(context, x + PADDING, manageTop, width - PADDING * 2, MANAGE_HEIGHT, 7, theme.hover());
		}
		UiIcons.draw(context, HubIcons.USERS, x + PADDING + 6 + (BADGE - 11) / 2, manageTop + (MANAGE_HEIGHT - 11) / 2, 11, theme.textSecondary());
		UiText.drawCentered(context, font, Component.translatable(EMUtilsTexts.UI_PROFILE_MANAGE), UiText.Size.LABEL, x + PADDING + 6 + BADGE + 8, manageTop + MANAGE_HEIGHT / 2, theme.textSecondary());
		context.pose().popMatrix();
		UiOpacity.reset();
	}

	private void drawRow(GuiGraphicsExtractor context, UiTheme theme, Profile profile, boolean isActive, int top, int rowWidth, boolean hovered) {
		int rowX = scroll.x();
		if (hovered) {
			UiShapes.roundedRect(context, rowX, top, rowWidth, ROW_HEIGHT, 7, theme.hover());
		}
		int left = rowX + 6;
		ProfileBadge.draw(context, profile, left, top + (ROW_HEIGHT - BADGE) / 2, BADGE);
		int textX = left + BADGE + 8;
		int textRight = rowX + rowWidth - (isActive ? 20 : 8);
		int nameHeight = UiText.lineHeight(font, UiText.Size.BOLD);
		int subHeight = UiText.lineHeight(font, UiText.Size.SMALL);
		int textTop = top + (ROW_HEIGHT - nameHeight - 3 - subHeight) / 2;
		UiText.draw(context, font, UiText.ellipsize(font, profile.name(), UiText.Size.BOLD, textRight - textX), UiText.Size.BOLD, textX, textTop, theme.text());
		UiText.draw(context, font, UiText.ellipsize(font, ProfileBadge.describe(profile), UiText.Size.SMALL, textRight - textX), UiText.Size.SMALL, textX, textTop + nameHeight + 3, theme.muted());
		if (isActive) {
			UiIcons.draw(context, HubIcons.CHECK, rowX + rowWidth - 17, top + (ROW_HEIGHT - 11) / 2, 11, theme.accent());
		}
	}

	/**
	 * Handles a click anywhere while the menu is open: picks a profile or opens the manager, and closes.
	 * Returns true when the menu stays open instead, because the click grabbed its scrollbar.
	 */
	public boolean mouseClicked(double mouseX, double mouseY) {
		if (closed) {
			return false;
		}
		if (scrolls && scroll.mouseClicked(mouseX, mouseY)) {
			return true;
		}
		closed = true;
		if (scroll.contains(mouseX, mouseY)) {
			int index = (int) Math.floor((mouseY - scroll.y() + scroll.exactOffset()) / ROW_HEIGHT);
			if (index >= 0 && index < profiles.size()) {
				onPick.accept(profiles.get(index));
			}
			return false;
		}
		if (contains(mouseX, mouseY, x, y + PADDING + listHeight + 5, width, MANAGE_HEIGHT)) {
			onManage.run();
		}
		return false;
	}

	public void mouseDragged(double mouseY) {
		scroll.mouseDragged(mouseY);
	}

	public void mouseReleased() {
		scroll.mouseReleased();
	}

	/** Scrolls the list when the mouse is over the menu; returns false when it isn't, so the menu can close. */
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if (!contains(mouseX, mouseY)) {
			return false;
		}
		scroll.scroll(mouseX, mouseY, amount);
		return true;
	}

	/** Scrolls to the last profile; used by UI snapshots. Returns whether the list scrolls at all. */
	public boolean scrollToEndForSnapshot() {
		scroll.scrollTo(scroll.maxScroll());
		return scrolls;
	}

	/** Whether the point is on the menu. */
	public boolean contains(double mouseX, double mouseY) {
		return contains(mouseX, mouseY, x, y, width, height);
	}

	private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
}
