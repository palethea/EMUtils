package net.emutils.client.emutils.profile.gui;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.gui.ui.UiAnim;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.gui.ui.UiSheetFrame;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.emutils.gui.ui.UiTextField;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.gui.ui.UiWidgets;
import net.emutils.client.emutils.gui.ui.UiIcons;
import net.emutils.client.emutils.profile.Profile;
import net.emutils.client.emutils.profile.ProfileColor;
import net.emutils.client.emutils.profile.ProfileIcon;
import net.emutils.client.emutils.profile.ProfileManager;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Adds or edits a profile in a sheet over the list (#89): its name, icon and color, what a new one
 * starts from, and where it loads by itself. Enter saves, Esc cancels, Tab moves between the fields.
 */
final class ProfileSheet {
	private static final int WIDTH = 360;
	private static final int PADDING = 16;
	private static final int FIELD_HEIGHT = 20;
	private static final int BUTTON_HEIGHT = 20;
	private static final int LINE_HEIGHT = 11;
	private static final int PREVIEW = 20;
	private static final int ICON_CELL = 18;
	private static final int COLOR_DOT = 14;
	private static final int COLOR_GAP = 8;

	private final Font font;
	private final UiAnim anim;
	private final @Nullable Profile existing;
	private final UiSheetFrame frame;
	private final String key;
	private final UiTextField name = new UiTextField(this, Profile.MAX_NAME_LENGTH);
	private final UiTextField servers = new UiTextField(this, 256);
	private ProfileIcon icon;
	private ProfileColor color;
	private boolean singleplayer;
	/** For a new profile: start from the current settings rather than the defaults. */
	private boolean copyCurrent = true;
	private int x;
	private int y;
	private int height;
	private int nameY;
	private int iconsY;
	private int colorsY;
	private int startY;
	private int autoY;
	private int serversY;
	private int noteY;
	private int footerY;
	private int saveX;
	private int saveWidth;
	private int cancelX;
	private int cancelWidth;
	private int addServerX;
	private int addServerWidth;
	private int[] startEdges = new int[0];

	ProfileSheet(Font font, UiAnim anim, @Nullable Profile existing) {
		this.font = font;
		this.anim = anim;
		this.existing = existing;
		this.key = "profile-sheet:" + System.identityHashCode(this);
		this.frame = new UiSheetFrame(anim, key, 16);
		if (existing != null) {
			name.setText(existing.rawName());
			icon = existing.icon();
			color = existing.color();
			servers.setText(String.join(", ", existing.servers()));
			singleplayer = existing.singleplayer();
		} else {
			// A new profile gets the first look no other profile has yet, so they're easy to tell apart.
			List<Profile> profiles = EMUtilsClient.profiles().profiles();
			icon = ProfileIcon.values()[Math.min(profiles.size(), ProfileIcon.values().length - 1)];
			color = ProfileColor.values()[profiles.size() % ProfileColor.values().length];
		}
		name.setFocused(true);
	}

	boolean isClosed() {
		return frame.isClosed();
	}

	void close() {
		if (!frame.closing()) {
			focus(null);
			frame.close();
		}
	}

	private void focus(@Nullable UiTextField target) {
		for (UiTextField field : List.of(name, servers)) {
			if (field != target && field.focused()) {
				field.setFocused(false);
			}
		}
		if (target != null && !target.focused()) {
			target.setFocused(true);
		}
	}

	private boolean valid() {
		return !name.text().isBlank() || (existing != null && existing.isDefault());
	}

	private void save() {
		if (!valid()) {
			return;
		}
		ProfileManager profiles = EMUtilsClient.profiles();
		List<String> parsed = ProfileManager.parseServers(servers.text());
		if (existing == null) {
			profiles.create(name.text(), icon, color, parsed, singleplayer, copyCurrent);
		} else {
			profiles.update(existing, name.text(), icon, color, parsed, singleplayer);
		}
		close();
	}

	/** The server you're on, if it isn't in the list yet. */
	private @Nullable String serverToAdd() {
		String current = ProfileManager.currentServerAddress(Minecraft.getInstance());
		if (current == null || ProfileManager.parseServers(servers.text()).contains(current)) {
			return null;
		}
		return current;
	}

	/** What saving takes away from another profile, since each place loads one profile. */
	private @Nullable Component takeoverNote() {
		ProfileManager profiles = EMUtilsClient.profiles();
		List<String> taken = new ArrayList<>();
		Profile owner = null;
		for (String server : ProfileManager.parseServers(servers.text())) {
			Profile other = profiles.ownerOfServer(server, existing);
			if (other != null) {
				taken.add(server);
				owner = owner == null ? other : owner;
			}
		}
		Profile singleplayerOwner = singleplayer ? profiles.ownerOfSingleplayer(existing) : null;
		if (singleplayerOwner != null) {
			return Component.translatable(EMUtilsTexts.UI_PROFILE_TAKES_SINGLEPLAYER, singleplayerOwner.name());
		}
		if (owner != null) {
			return Component.translatable(EMUtilsTexts.UI_PROFILE_TAKES_SERVER, String.join(", ", taken), owner.name());
		}
		return null;
	}

	// ---- drawing --------------------------------------------------------------------------------

	private int labelBlock() {
		return UiText.lineHeight(font, UiText.Size.LABEL) + 6;
	}

	private void layout(int screenWidth, int screenHeight) {
		int headerHeight = UiText.lineHeight(font, UiText.Size.HEADING);
		nameY = PADDING + headerHeight + 14 + labelBlock();
		iconsY = nameY + FIELD_HEIGHT + 12 + labelBlock();
		colorsY = iconsY + ICON_CELL + 8;
		int afterLook = colorsY + COLOR_DOT;
		if (existing == null) {
			startY = afterLook + 12 + labelBlock();
			afterLook = startY + UiWidgets.SEGMENT_HEIGHT;
		}
		autoY = afterLook + 14 + labelBlock();
		serversY = autoY + UiWidgets.SWITCH_HEIGHT + 8;
		noteY = serversY + FIELD_HEIGHT + 6;
		footerY = noteY + LINE_HEIGHT + 14;
		height = footerY + BUTTON_HEIGHT + PADDING;
		x = (screenWidth - WIDTH) / 2;
		y = Math.max(4, (screenHeight - height) / 2);
	}

	void render(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY, int screenWidth, int screenHeight) {
		layout(screenWidth, screenHeight);
		Component title = Component.translatable(existing == null ? EMUtilsTexts.UI_PROFILE_NEW : EMUtilsTexts.UI_PROFILE_EDIT);
		if (frame.firstFrame()) {
			UiText.prepare(title, UiText.Size.HEADING);
		}
		if (!frame.begin(context, theme, screenWidth, screenHeight, x, y, WIDTH, height)) {
			return;
		}
		int hoverX = frame.closing() ? Integer.MIN_VALUE / 2 : mouseX;
		int hoverY = frame.closing() ? Integer.MIN_VALUE / 2 : mouseY;
		int left = x + PADDING;
		int right = x + WIDTH - PADDING;
		UiText.draw(context, font, title, UiText.Size.HEADING, left, y + PADDING, theme.text());

		// Name, with the profile's look next to it as it will appear.
		label(context, theme, Component.translatable(EMUtilsTexts.UI_PROFILE_NAME), left, y + nameY);
		ProfileBadge.draw(context, icon, color, left, y + nameY, PREVIEW);
		int fieldX = left + PREVIEW + 8;
		Component placeholder = existing != null && existing.isDefault()
			? Component.translatable(EMUtilsTexts.UI_PROFILE_DEFAULT_NAME)
			: Component.translatable(EMUtilsTexts.UI_PROFILE_NAME_PLACEHOLDER);
		field(context, theme, name, fieldX, y + nameY, right - fieldX, placeholder);

		// Icon and color.
		label(context, theme, Component.translatable(EMUtilsTexts.UI_PROFILE_LOOK), left, y + iconsY);
		ProfileIcon[] icons = ProfileIcon.values();
		for (int i = 0; i < icons.length; i++) {
			int cellX = iconX(i);
			boolean selected = icons[i] == icon;
			boolean hovered = contains(hoverX, hoverY, cellX, y + iconsY, ICON_CELL, ICON_CELL);
			if (selected) {
				UiShapes.roundedRect(context, cellX, y + iconsY, ICON_CELL, ICON_CELL, 6, color.argb());
			} else if (hovered) {
				UiShapes.roundedRect(context, cellX, y + iconsY, ICON_CELL, ICON_CELL, 6, theme.hover());
			}
			int iconSize = 11;
			int offset = (ICON_CELL - iconSize) / 2;
			UiIcons.draw(context, icons[i].texture(), cellX + offset, y + iconsY + offset, iconSize, selected ? 0xFFFFFFFF : hovered ? theme.text() : theme.textSecondary());
		}
		ProfileColor[] colors = ProfileColor.values();
		for (int i = 0; i < colors.length; i++) {
			int dotX = left + 2 + i * (COLOR_DOT + COLOR_GAP);
			boolean selected = colors[i] == color;
			boolean hovered = contains(hoverX, hoverY, dotX - 2, y + colorsY - 2, COLOR_DOT + 4, COLOR_DOT + 4);
			if (selected || hovered) {
				// A ring around the chosen color, in the panel color between it and the dot.
				UiShapes.circle(context, dotX - 2, y + colorsY - 2, COLOR_DOT + 4, selected ? colors[i].argb() : theme.line());
				UiShapes.circle(context, dotX - 1, y + colorsY - 1, COLOR_DOT + 2, theme.surface());
			}
			UiShapes.circle(context, dotX, y + colorsY, COLOR_DOT, colors[i].argb());
		}

		// What a new profile starts from.
		if (existing == null) {
			label(context, theme, Component.translatable(EMUtilsTexts.UI_PROFILE_START_FROM), left, y + startY);
			List<Component> options = List.of(Component.translatable(EMUtilsTexts.UI_PROFILE_START_CURRENT), Component.translatable(EMUtilsTexts.UI_PROFILE_START_DEFAULTS));
			float selection = anim.transition(key + ":start", copyCurrent ? 0.0F : 1.0F, 0.18F);
			int hovered = -1;
			int[] edges = UiWidgets.segmentEdges(font, left, options);
			for (int i = 0; i < options.size(); i++) {
				if (contains(hoverX, hoverY, edges[i], y + startY, edges[i + 1] - edges[i], UiWidgets.SEGMENT_HEIGHT)) {
					hovered = i;
				}
			}
			startEdges = UiWidgets.segmented(context, font, theme, left, y + startY, options, selection, hovered);
		}

		// Where it loads by itself.
		label(context, theme, Component.translatable(EMUtilsTexts.UI_PROFILE_AUTO), left, y + autoY);
		String serverToAdd = serverToAdd();
		if (serverToAdd != null) {
			Component add = Component.translatable(EMUtilsTexts.UI_PROFILE_ADD_SERVER, serverToAdd);
			addServerWidth = Math.min(UiText.width(font, add, UiText.Size.LABEL), right - left - 120);
			addServerX = right - addServerWidth;
			int labelTop = y + autoY - UiText.lineHeight(font, UiText.Size.LABEL) - 6;
			boolean hovered = contains(hoverX, hoverY, addServerX - 2, labelTop - 2, addServerWidth + 4, UiText.lineHeight(font, UiText.Size.LABEL) + 4);
			UiText.draw(context, font, UiText.ellipsize(font, add, UiText.Size.LABEL, addServerWidth), UiText.Size.LABEL, addServerX, labelTop, hovered ? theme.accentHover() : theme.accent());
		} else {
			addServerWidth = 0;
		}
		float switchProgress = anim.transition(key + ":singleplayer", singleplayer, 0.18F);
		boolean switchHovered = contains(hoverX, hoverY, left, y + autoY, right - left, UiWidgets.SWITCH_HEIGHT);
		UiWidgets.toggle(context, theme, left, y + autoY, switchProgress, switchHovered ? 1.0F : 0.0F);
		UiText.drawCentered(context, font, Component.translatable(EMUtilsTexts.UI_PROFILE_SINGLEPLAYER), UiText.Size.BODY, left + UiWidgets.SWITCH_WIDTH + 8, y + autoY + UiWidgets.SWITCH_HEIGHT / 2, theme.text());
		field(context, theme, servers, left, y + serversY, right - left, Component.translatable(EMUtilsTexts.UI_PROFILE_SERVERS_PLACEHOLDER));
		Component takeover = takeoverNote();
		Component note = takeover != null ? takeover : Component.translatable(EMUtilsTexts.UI_PROFILE_SERVERS_HINT);
		UiText.draw(context, font, UiText.ellipsize(font, note, UiText.Size.BODY, right - left), UiText.Size.BODY, left + 1, y + noteY, takeover != null ? theme.warning() : theme.muted());

		Component saveLabel = Component.translatable(existing == null ? EMUtilsTexts.UI_ADD : EMUtilsTexts.COMMAND_SHORTCUT_ACTION_SAVE);
		saveWidth = Math.max(60, UiWidgets.buttonWidth(font, saveLabel) + 12);
		saveX = right - saveWidth;
		cancelWidth = UiWidgets.buttonWidth(font, CommonComponents.GUI_CANCEL) + 4;
		cancelX = saveX - 6 - cancelWidth;
		UiWidgets.button(context, font, theme, cancelX, y + footerY, cancelWidth, BUTTON_HEIGHT, CommonComponents.GUI_CANCEL, UiWidgets.ButtonStyle.GHOST, contains(hoverX, hoverY, cancelX, y + footerY, cancelWidth, BUTTON_HEIGHT) ? 1.0F : 0.0F);
		boolean valid = valid();
		UiWidgets.button(context, font, theme, saveX, y + footerY, saveWidth, BUTTON_HEIGHT, saveLabel, valid ? UiWidgets.ButtonStyle.PRIMARY : UiWidgets.ButtonStyle.SURFACE, valid && contains(hoverX, hoverY, saveX, y + footerY, saveWidth, BUTTON_HEIGHT) ? 1.0F : 0.0F);
		frame.endBody(context);
		frame.end();
	}

	/** The icons are spread across the sheet's full width. */
	private int iconX(int index) {
		int count = ProfileIcon.values().length;
		return x + PADDING + Math.round(index * (WIDTH - PADDING * 2 - ICON_CELL) / (float) (count - 1));
	}

	private void label(GuiGraphicsExtractor context, UiTheme theme, Component label, int left, int fieldTop) {
		UiText.draw(context, font, label, UiText.Size.LABEL, left + 1, fieldTop - UiText.lineHeight(font, UiText.Size.LABEL) - 6, theme.muted());
	}

	private void field(GuiGraphicsExtractor context, UiTheme theme, UiTextField field, int fieldX, int fieldY, int width, Component placeholder) {
		UiShapes.borderedRect(context, fieldX, fieldY, width, FIELD_HEIGHT, 6, theme.segmentBackground(), field.focused() ? theme.accent() : theme.line());
		field.draw(context, font, theme, fieldX + 8, fieldY + FIELD_HEIGHT / 2, width - 16, placeholder);
	}

	// ---- input ----------------------------------------------------------------------------------

	void mouseClicked(double mouseX, double mouseY) {
		if (frame.closing()) {
			return;
		}
		if (!contains(mouseX, mouseY, x, y, WIDTH, height) || contains(mouseX, mouseY, cancelX, y + footerY, cancelWidth, BUTTON_HEIGHT)) {
			close();
			return;
		}
		if (contains(mouseX, mouseY, saveX, y + footerY, saveWidth, BUTTON_HEIGHT)) {
			save();
			return;
		}
		int left = x + PADDING;
		int right = x + WIDTH - PADDING;
		int fieldX = left + PREVIEW + 8;
		if (contains(mouseX, mouseY, fieldX, y + nameY, right - fieldX, FIELD_HEIGHT)) {
			focus(name);
			name.click(font, mouseX, false);
			return;
		}
		if (contains(mouseX, mouseY, left, y + serversY, right - left, FIELD_HEIGHT)) {
			focus(servers);
			servers.click(font, mouseX, false);
			return;
		}
		focus(null);
		ProfileIcon[] icons = ProfileIcon.values();
		for (int i = 0; i < icons.length; i++) {
			if (contains(mouseX, mouseY, iconX(i), y + iconsY, ICON_CELL, ICON_CELL)) {
				icon = icons[i];
				return;
			}
		}
		ProfileColor[] colors = ProfileColor.values();
		for (int i = 0; i < colors.length; i++) {
			int dotX = left + 2 + i * (COLOR_DOT + COLOR_GAP);
			if (contains(mouseX, mouseY, dotX - 3, y + colorsY - 3, COLOR_DOT + 6, COLOR_DOT + 6)) {
				color = colors[i];
				return;
			}
		}
		if (existing == null && startEdges.length == 3) {
			for (int i = 0; i < 2; i++) {
				if (contains(mouseX, mouseY, startEdges[i], y + startY, startEdges[i + 1] - startEdges[i], UiWidgets.SEGMENT_HEIGHT)) {
					copyCurrent = i == 0;
					return;
				}
			}
		}
		String serverToAdd = serverToAdd();
		int labelHeight = UiText.lineHeight(font, UiText.Size.LABEL);
		if (serverToAdd != null && addServerWidth > 0 && contains(mouseX, mouseY, addServerX - 2, y + autoY - labelHeight - 8, addServerWidth + 4, labelHeight + 4)) {
			String text = servers.text().strip();
			servers.setText(text.isEmpty() ? serverToAdd : text + ", " + serverToAdd);
			return;
		}
		if (contains(mouseX, mouseY, left, y + autoY, right - left, UiWidgets.SWITCH_HEIGHT)) {
			singleplayer = !singleplayer;
		}
	}

	void keyPressed(KeyEvent input) {
		if (frame.closing()) {
			return;
		}
		if (input.isEscape()) {
			close();
			return;
		}
		if (input.isConfirmation()) {
			save();
			return;
		}
		if (input.key() == InputConstants.KEY_TAB) {
			UiTextField next = name.focused() ? servers : name;
			focus(next);
			next.setText(next.text());
			return;
		}
		if (!name.keyPressed(input, () -> {
		})) {
			servers.keyPressed(input, () -> {
			});
		}
	}

	void charTyped(CharacterEvent input) {
		if (frame.closing()) {
			return;
		}
		if (!name.charTyped(input, () -> {
		})) {
			servers.charTyped(input, () -> {
			});
		}
	}

	/** Sets the fields as if typed; used by UI snapshots. */
	void fillForSnapshot(String profileName, String serverList, boolean loadInSingleplayer) {
		name.setText(profileName);
		servers.setText(serverList);
		singleplayer = loadInSingleplayer;
		focus(null);
	}

	/** Saves as the Add or Save button would; used by UI snapshots. */
	void saveForSnapshot() {
		save();
	}

	private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
}
