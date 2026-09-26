package net.emutils.client.emutils.profile.gui;

import java.util.List;
import net.emutils.client.emutils.gui.ui.UiIcons;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.profile.Profile;
import net.emutils.client.emutils.profile.ProfileColor;
import net.emutils.client.emutils.profile.ProfileIcon;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** How a profile is shown (#89): its icon in white on a circle of its color, plus a line about when it loads. */
public final class ProfileBadge {
	private ProfileBadge() {
	}

	public static void draw(GuiGraphicsExtractor context, Profile profile, int x, int y, int size) {
		draw(context, profile.icon(), profile.color(), x, y, size);
	}

	public static void draw(GuiGraphicsExtractor context, ProfileIcon icon, ProfileColor color, int x, int y, int size) {
		UiShapes.circle(context, x, y, size, color.argb());
		int iconSize = Math.round(size * 0.58F);
		int offset = (size - iconSize) / 2;
		UiIcons.draw(context, icon.texture(), x + offset, y + offset, iconSize, 0xFFFFFFFF);
	}

	/** When the profile loads by itself, such as "Loads in singleplayer and on mc.hypixel.net". */
	public static Component describe(Profile profile) {
		List<String> servers = profile.servers();
		if (!profile.singleplayer() && servers.isEmpty()) {
			return Component.translatable(profile.isDefault() ? EMUtilsTexts.UI_PROFILE_FALLBACK : EMUtilsTexts.UI_PROFILE_MANUAL);
		}
		String serverList = String.join(", ", servers);
		if (profile.singleplayer() && servers.isEmpty()) {
			return Component.translatable(EMUtilsTexts.UI_PROFILE_AUTO_SINGLEPLAYER);
		}
		if (profile.singleplayer()) {
			return Component.translatable(EMUtilsTexts.UI_PROFILE_AUTO_BOTH, serverList);
		}
		return Component.translatable(EMUtilsTexts.UI_PROFILE_AUTO_SERVERS, serverList);
	}
}
