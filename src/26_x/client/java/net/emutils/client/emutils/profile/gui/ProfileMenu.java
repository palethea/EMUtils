package net.emutils.client.emutils.profile.gui;

import java.util.List;
import java.util.function.Consumer;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.gui.hub.HubIcons;
import net.emutils.client.emutils.gui.ui.UiAnim;
import net.emutils.client.emutils.gui.ui.UiIcons;
import net.emutils.client.emutils.gui.ui.UiOpacity;
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
 * a right-click menu, clicking anywhere closes it, and clicking a profile switches to it.
 */
public final class ProfileMenu {
	private static final int ROW_HEIGHT = 30;
	private static final int MANAGE_HEIGHT = 22;
	private static final int PADDING = 5;
	private static final int BADGE = 18;
	private static final int MIN_WIDTH = 190;
	private static final int MAX_WIDTH = 260;

	private final Font font;
	private final UiAnim anim;
	private final String key = "profile-menu:" + System.identityHashCode(this);
	private final int anchorRight;
	private final int anchorTop;
	private final Consumer<Profile> onPick;
	private final Runnable onManage;
	private List<Profile> profiles = List.of();
	private int x;
	private int y;
	private int width;
	private int height;
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

	public void render(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY, int screenWidth, int screenHeight) {
		if (closed) {
			return;
		}
		profiles = EMUtilsClient.profiles().profiles();
		Profile active = EMUtilsClient.profiles().active();
		width = MIN_WIDTH;
		for (Profile profile : profiles) {
			width = Math.max(width, PADDING * 2 + 8 + BADGE + 8 + UiText.width(font, profile.name(), UiText.Size.BOLD) + 24);
		}
		width = Math.min(width, Math.min(MAX_WIDTH, screenWidth - 8));
		height = PADDING * 2 + profiles.size() * ROW_HEIGHT + 5 + MANAGE_HEIGHT;
		x = Math.max(4, anchorRight - width);
		y = Math.min(anchorTop, Math.max(4, screenHeight - height - 4));

		float shown = anim.towards(key, 1.0F, 22.0F);
		UiOpacity.set(shown);
		context.pose().pushMatrix();
		context.pose().translate(0.0F, (1.0F - shown) * -3.0F);
		UiShapes.shadow(context, x, y, width, height, 10, 12, theme.shadow());
		UiShapes.borderedRect(context, x, y, width, height, 10, theme.surface(), theme.line());
		int top = y + PADDING;
		for (Profile profile : profiles) {
			boolean hovered = contains(mouseX, mouseY, x + PADDING, top, width - PADDING * 2, ROW_HEIGHT);
			if (hovered) {
				UiShapes.roundedRect(context, x + PADDING, top, width - PADDING * 2, ROW_HEIGHT, 7, theme.hover());
			}
			int left = x + PADDING + 6;
			ProfileBadge.draw(context, profile, left, top + (ROW_HEIGHT - BADGE) / 2, BADGE);
			int textX = left + BADGE + 8;
			boolean isActive = profile.id().equals(active.id());
			int textRight = x + width - PADDING - (isActive ? 20 : 8);
			int nameHeight = UiText.lineHeight(font, UiText.Size.BOLD);
			int subHeight = UiText.lineHeight(font, UiText.Size.SMALL);
			int textTop = top + (ROW_HEIGHT - nameHeight - 3 - subHeight) / 2;
			UiText.draw(context, font, UiText.ellipsize(font, profile.name(), UiText.Size.BOLD, textRight - textX), UiText.Size.BOLD, textX, textTop, theme.text());
			UiText.draw(context, font, UiText.ellipsize(font, ProfileBadge.describe(profile), UiText.Size.SMALL, textRight - textX), UiText.Size.SMALL, textX, textTop + nameHeight + 3, theme.muted());
			if (isActive) {
				UiIcons.draw(context, HubIcons.CHECK, x + width - PADDING - 17, top + (ROW_HEIGHT - 11) / 2, 11, theme.accent());
			}
			top += ROW_HEIGHT;
		}
		context.fill(x + PADDING + 2, top + 2, x + width - PADDING - 2, top + 3, UiOpacity.apply(theme.line()));
		top += 5;
		boolean manageHovered = contains(mouseX, mouseY, x + PADDING, top, width - PADDING * 2, MANAGE_HEIGHT);
		if (manageHovered) {
			UiShapes.roundedRect(context, x + PADDING, top, width - PADDING * 2, MANAGE_HEIGHT, 7, theme.hover());
		}
		UiIcons.draw(context, HubIcons.USERS, x + PADDING + 6 + (BADGE - 11) / 2, top + (MANAGE_HEIGHT - 11) / 2, 11, theme.textSecondary());
		UiText.drawCentered(context, font, Component.translatable(EMUtilsTexts.UI_PROFILE_MANAGE), UiText.Size.LABEL, x + PADDING + 6 + BADGE + 8, top + MANAGE_HEIGHT / 2, theme.textSecondary());
		context.pose().popMatrix();
		UiOpacity.reset();
	}

	/** Handles a click anywhere while the menu is open: picks or manages, and always closes. */
	public void mouseClicked(double mouseX, double mouseY) {
		if (closed) {
			return;
		}
		closed = true;
		int top = y + PADDING;
		for (Profile profile : profiles) {
			if (contains(mouseX, mouseY, x, top, width, ROW_HEIGHT)) {
				onPick.accept(profile);
				return;
			}
			top += ROW_HEIGHT;
		}
		top += 5;
		if (contains(mouseX, mouseY, x, top, width, MANAGE_HEIGHT)) {
			onManage.run();
		}
	}

	/** Whether the click lands on the menu, so the screen doesn't also act on it. */
	public boolean contains(double mouseX, double mouseY) {
		return contains(mouseX, mouseY, x, y, width, height);
	}

	private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
}
