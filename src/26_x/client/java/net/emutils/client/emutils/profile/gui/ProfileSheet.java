package net.emutils.client.emutils.profile.gui;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.gui.hub.HubIcons;
import net.emutils.client.emutils.gui.ui.UiAnim;
import net.emutils.client.emutils.gui.ui.UiIcons;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.gui.ui.UiSheetFrame;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.emutils.gui.ui.UiTextField;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.gui.ui.UiWidgets;
import net.emutils.client.emutils.profile.Profile;
import net.emutils.client.emutils.profile.ProfileColor;
import net.emutils.client.emutils.profile.ProfileIcon;
import net.emutils.client.emutils.profile.ProfileManager;
import net.emutils.client.emutils.util.EMUtilsTexts;
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
 *
 * <p>In a window too short for one column, the look and the auto-switch settings sit side by side
 * under the name, so the whole sheet stays on screen.
 */
final class ProfileSheet {
	private static final int NARROW_WIDTH = 360;
	private static final int WIDE_WIDTH = 520;
	private static final int PADDING = 16;
	private static final int COLUMN_GAP = 20;
	private static final int FIELD_HEIGHT = 20;
	private static final int BUTTON_HEIGHT = 20;
	private static final int LINE_HEIGHT = 11;
	private static final int PREVIEW = 20;
	private static final int ICON_CELL = 18;
	private static final int ICON_ROW_GAP = 4;
	private static final int COLOR_DOT = 14;
	private static final int PICKER_BUTTON = 16;

	private final Font font;
	private final UiAnim anim;
	private final @Nullable Profile existing;
	private final UiSheetFrame frame;
	private final String key;
	private final UiTextField name = new UiTextField(this, Profile.MAX_NAME_LENGTH);
	/** Long enough for far more servers than anyone links; picks that wouldn't fit are refused, never cut. */
	private static final int SERVERS_MAX_LENGTH = 4096;
	private final UiTextField servers = new UiTextField(this, SERVERS_MAX_LENGTH);
	private @Nullable ServerPicker picker;
	private ProfileIcon icon;
	private ProfileColor color;
	private boolean singleplayer;
	/** For a new profile: start from the current settings rather than the defaults. */
	private boolean copyCurrent = true;

	// Layout, in screen coordinates; recomputed every frame.
	private int x;
	private int y;
	private int width;
	private int height;
	private boolean wide;
	private int nameY;
	private int lookX;
	private int lookWidth;
	private int iconsY;
	private int iconColumns;
	private int colorsY;
	private int startY;
	private int autoX;
	private int autoWidth;
	private int autoY;
	private int serversY;
	private int noteY;
	private int noteLines;
	private int footerY;
	private int saveX;
	private int saveWidth;
	private int cancelX;
	private int cancelWidth;
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
			closePicker();
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

	private boolean nameTaken() {
		String typed = name.text().isBlank() && existing != null && existing.isDefault()
			? Component.translatable(EMUtilsTexts.UI_PROFILE_DEFAULT_NAME).getString()
			: name.text();
		return EMUtilsClient.profiles().nameTaken(typed, existing);
	}

	private boolean valid() {
		return (!name.text().isBlank() || (existing != null && existing.isDefault())) && !nameTaken();
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

	/** Adds a server to the list, or takes it out when it's already there; for the server picker. */
	private void toggleServer(String address) {
		List<String> list = new ArrayList<>(ProfileManager.parseServers(servers.text()));
		if (!list.remove(address)) {
			list.add(address);
		}
		String text = String.join(", ", list);
		if (text.length() <= SERVERS_MAX_LENGTH) {
			servers.setText(text);
		}
	}

	private boolean hasServer(String address) {
		return ProfileManager.parseServers(servers.text()).contains(address);
	}

	private void openPicker() {
		focus(null);
		picker = new ServerPicker(font, anim, this::hasServer, this::toggleServer);
	}

	private void closePicker() {
		if (picker != null) {
			picker.close();
			picker = null;
		}
	}

	/** What's wrong with the form or what saving takes away from another profile, or null. */
	private @Nullable Component warning() {
		if (nameTaken()) {
			return Component.translatable(EMUtilsTexts.UI_PROFILE_NAME_TAKEN);
		}
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

	// ---- layout ---------------------------------------------------------------------------------

	private int labelBlock() {
		return UiText.lineHeight(font, UiText.Size.LABEL) + 6;
	}

	private int iconRows() {
		return (ProfileIcon.values().length + iconColumns - 1) / iconColumns;
	}

	/** Lays the look out in a column of {@code columnWidth} from {@code top}; returns where it ends. */
	private int layoutLook(int top, int columnWidth) {
		iconColumns = Math.clamp((columnWidth + 2) / (ICON_CELL + 2), 1, ProfileIcon.values().length);
		// Two even rows read better than one full row and a few stragglers.
		int rows = (ProfileIcon.values().length + iconColumns - 1) / iconColumns;
		iconColumns = (ProfileIcon.values().length + rows - 1) / rows;
		iconsY = top + labelBlock();
		colorsY = iconsY + iconRows() * ICON_CELL + (iconRows() - 1) * ICON_ROW_GAP + 8;
		int end = colorsY + COLOR_DOT;
		if (existing == null) {
			startY = end + 12 + labelBlock();
			end = startY + UiWidgets.SEGMENT_HEIGHT;
		}
		return end;
	}

	/** Lays the auto-switch settings out from {@code top}; returns where they end. */
	private int layoutAuto(int top) {
		autoY = top + labelBlock();
		serversY = autoY + UiWidgets.SWITCH_HEIGHT + 8;
		noteY = serversY + FIELD_HEIGHT + 6;
		return noteY + noteLines * LINE_HEIGHT;
	}

	private void layout(int screenWidth, int screenHeight) {
		int headerHeight = UiText.lineHeight(font, UiText.Size.HEADING);
		int nameTop = PADDING + headerHeight + 14 + labelBlock();
		int bodyTop = nameTop + FIELD_HEIGHT + 12;

		// One column first; two when that doesn't fit the window's height.
		wide = false;
		width = NARROW_WIDTH;
		noteLines = 1;
		int inner = width - PADDING * 2;
		int end = layoutAuto(layoutLook(bodyTop, inner) + 14);
		int narrowHeight = end + 14 + BUTTON_HEIGHT + PADDING;
		if (narrowHeight > screenHeight - 8 && screenWidth >= 380) {
			wide = true;
			width = Math.min(WIDE_WIDTH, screenWidth - 16);
			noteLines = 2;
			inner = width - PADDING * 2;
			int column = (inner - COLUMN_GAP) / 2;
			int lookEnd = layoutLook(bodyTop, column);
			int autoEnd = layoutAuto(bodyTop);
			end = Math.max(lookEnd, autoEnd);
			lookWidth = column;
			autoWidth = inner - column - COLUMN_GAP;
		} else {
			lookWidth = inner;
			autoWidth = inner;
		}
		height = end + 14 + BUTTON_HEIGHT + PADDING;
		x = (screenWidth - width) / 2;
		y = Math.max(4, (screenHeight - height) / 2);
		lookX = x + PADDING;
		autoX = wide ? lookX + lookWidth + COLUMN_GAP : lookX;

		// To screen coordinates.
		nameY = y + nameTop;
		iconsY += y;
		colorsY += y;
		startY += y;
		autoY += y;
		serversY += y;
		noteY += y;
		footerY = y + height - PADDING - BUTTON_HEIGHT;
	}

	private int iconX(int index) {
		int column = index % iconColumns;
		if (iconColumns == 1) {
			return lookX;
		}
		return lookX + Math.round(column * (lookWidth - ICON_CELL) / (float) (iconColumns - 1));
	}

	private int iconY(int index) {
		return iconsY + (index / iconColumns) * (ICON_CELL + ICON_ROW_GAP);
	}

	private int colorX(int index) {
		int count = ProfileColor.values().length;
		return lookX + 2 + Math.round(index * (Math.min(lookWidth, count * 22) - 4 - COLOR_DOT) / (float) (count - 1));
	}

	private int serversFieldWidth() {
		return autoWidth - PICKER_BUTTON - 6;
	}

	private int pickerButtonX() {
		return autoX + autoWidth - PICKER_BUTTON;
	}

	// ---- drawing --------------------------------------------------------------------------------

	void render(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY, int screenWidth, int screenHeight) {
		layout(screenWidth, screenHeight);
		Component title = Component.translatable(existing == null ? EMUtilsTexts.UI_PROFILE_NEW : EMUtilsTexts.UI_PROFILE_EDIT);
		if (frame.firstFrame()) {
			UiText.prepare(title, UiText.Size.HEADING);
		}
		if (!frame.begin(context, theme, screenWidth, screenHeight, x, y, width, height)) {
			return;
		}
		// While the server list is open, only it reacts to the mouse.
		boolean interactive = !frame.closing() && picker == null;
		int hoverX = interactive ? mouseX : Integer.MIN_VALUE / 2;
		int hoverY = interactive ? mouseY : Integer.MIN_VALUE / 2;
		int left = x + PADDING;
		int right = x + width - PADDING;
		UiText.draw(context, font, title, UiText.Size.HEADING, left, y + PADDING, theme.text());

		// Name, with the profile's look next to it as it will appear.
		label(context, theme, Component.translatable(EMUtilsTexts.UI_PROFILE_NAME), left, nameY);
		ProfileBadge.draw(context, icon, color, left, nameY, PREVIEW);
		int fieldX = left + PREVIEW + 8;
		Component placeholder = existing != null && existing.isDefault()
			? Component.translatable(EMUtilsTexts.UI_PROFILE_DEFAULT_NAME)
			: Component.translatable(EMUtilsTexts.UI_PROFILE_NAME_PLACEHOLDER);
		field(context, theme, name, fieldX, nameY, right - fieldX, placeholder, nameTaken());

		drawLook(context, theme, hoverX, hoverY);
		drawAuto(context, theme, hoverX, hoverY);

		Component saveLabel = Component.translatable(existing == null ? EMUtilsTexts.UI_ADD : EMUtilsTexts.COMMAND_SHORTCUT_ACTION_SAVE);
		saveWidth = Math.max(60, UiWidgets.buttonWidth(font, saveLabel) + 12);
		saveX = right - saveWidth;
		cancelWidth = UiWidgets.buttonWidth(font, CommonComponents.GUI_CANCEL) + 4;
		cancelX = saveX - 6 - cancelWidth;
		UiWidgets.button(context, font, theme, cancelX, footerY, cancelWidth, BUTTON_HEIGHT, CommonComponents.GUI_CANCEL, UiWidgets.ButtonStyle.GHOST, contains(hoverX, hoverY, cancelX, footerY, cancelWidth, BUTTON_HEIGHT) ? 1.0F : 0.0F);
		boolean valid = valid();
		UiWidgets.button(context, font, theme, saveX, footerY, saveWidth, BUTTON_HEIGHT, saveLabel, valid ? UiWidgets.ButtonStyle.PRIMARY : UiWidgets.ButtonStyle.SURFACE, valid && contains(hoverX, hoverY, saveX, footerY, saveWidth, BUTTON_HEIGHT) ? 1.0F : 0.0F);
		frame.endBody(context);
		if (picker != null) {
			picker.render(context, theme, mouseX, mouseY, autoX, serversY, serversY + FIELD_HEIGHT, autoWidth, screenHeight);
			if (picker.isClosed()) {
				picker = null;
			}
		}
		frame.end();
	}

	private void drawLook(GuiGraphicsExtractor context, UiTheme theme, int hoverX, int hoverY) {
		label(context, theme, Component.translatable(EMUtilsTexts.UI_PROFILE_LOOK), lookX, iconsY);
		ProfileIcon[] icons = ProfileIcon.values();
		for (int i = 0; i < icons.length; i++) {
			int cellX = iconX(i);
			int cellY = iconY(i);
			boolean selected = icons[i] == icon;
			boolean hovered = contains(hoverX, hoverY, cellX, cellY, ICON_CELL, ICON_CELL);
			if (selected) {
				UiShapes.roundedRect(context, cellX, cellY, ICON_CELL, ICON_CELL, 6, color.argb());
			} else if (hovered) {
				UiShapes.roundedRect(context, cellX, cellY, ICON_CELL, ICON_CELL, 6, theme.hover());
			}
			int iconSize = 11;
			int offset = (ICON_CELL - iconSize) / 2;
			UiIcons.draw(context, icons[i].texture(), cellX + offset, cellY + offset, iconSize, selected ? 0xFFFFFFFF : hovered ? theme.text() : theme.textSecondary());
		}
		ProfileColor[] colors = ProfileColor.values();
		for (int i = 0; i < colors.length; i++) {
			int dotX = colorX(i);
			boolean selected = colors[i] == color;
			boolean hovered = contains(hoverX, hoverY, dotX - 2, colorsY - 2, COLOR_DOT + 4, COLOR_DOT + 4);
			if (selected || hovered) {
				// A ring around the chosen color, in the sheet color between it and the dot.
				UiShapes.circle(context, dotX - 2, colorsY - 2, COLOR_DOT + 4, selected ? colors[i].argb() : theme.line());
				UiShapes.circle(context, dotX - 1, colorsY - 1, COLOR_DOT + 2, theme.surface());
			}
			UiShapes.circle(context, dotX, colorsY, COLOR_DOT, colors[i].argb());
		}

		if (existing == null) {
			label(context, theme, Component.translatable(EMUtilsTexts.UI_PROFILE_START_FROM), lookX, startY);
			List<Component> options = List.of(Component.translatable(EMUtilsTexts.UI_PROFILE_START_CURRENT), Component.translatable(EMUtilsTexts.UI_PROFILE_START_DEFAULTS));
			float selection = anim.transition(key + ":start", copyCurrent ? 0.0F : 1.0F, 0.18F);
			int hovered = -1;
			int[] edges = UiWidgets.segmentEdges(font, lookX, options);
			for (int i = 0; i < options.size(); i++) {
				if (contains(hoverX, hoverY, edges[i], startY, edges[i + 1] - edges[i], UiWidgets.SEGMENT_HEIGHT)) {
					hovered = i;
				}
			}
			startEdges = UiWidgets.segmented(context, font, theme, lookX, startY, options, selection, hovered);
		}
	}

	private void drawAuto(GuiGraphicsExtractor context, UiTheme theme, int hoverX, int hoverY) {
		label(context, theme, Component.translatable(EMUtilsTexts.UI_PROFILE_AUTO), autoX, autoY);
		float switchProgress = anim.transition(key + ":singleplayer", singleplayer, 0.18F);
		boolean switchHovered = contains(hoverX, hoverY, autoX, autoY, autoWidth, UiWidgets.SWITCH_HEIGHT);
		UiWidgets.toggle(context, theme, autoX, autoY, switchProgress, switchHovered ? 1.0F : 0.0F);
		UiText.drawCentered(context, font, Component.translatable(EMUtilsTexts.UI_PROFILE_SINGLEPLAYER), UiText.Size.BODY, autoX + UiWidgets.SWITCH_WIDTH + 8, autoY + UiWidgets.SWITCH_HEIGHT / 2, theme.text());

		field(context, theme, servers, autoX, serversY, serversFieldWidth(), Component.translatable(EMUtilsTexts.UI_PROFILE_SERVERS_PLACEHOLDER), false);
		// Picks servers from the multiplayer list, so they don't have to be typed.
		int buttonX = pickerButtonX();
		int buttonY = serversY + (FIELD_HEIGHT - PICKER_BUTTON) / 2;
		boolean buttonHovered = picker != null || contains(hoverX, hoverY, buttonX - 2, serversY, PICKER_BUTTON + 4, FIELD_HEIGHT);
		UiShapes.roundedRect(context, buttonX - 2, serversY, PICKER_BUTTON + 4, FIELD_HEIGHT, 6, buttonHovered ? theme.segmentSelected() : theme.segmentBackground());
		UiIcons.draw(context, HubIcons.CHEVRON_DOWN, buttonX + (PICKER_BUTTON - 10) / 2, buttonY + (PICKER_BUTTON - 10) / 2, 10, buttonHovered ? theme.text() : theme.textSecondary());

		Component warning = warning();
		Component note = warning != null ? warning : Component.translatable(EMUtilsTexts.UI_PROFILE_SERVERS_HINT);
		int noteColor = warning != null ? theme.warning() : theme.muted();
		List<Component> lines = UiText.wrap(font, note, UiText.Size.BODY, autoWidth - 2);
		for (int i = 0; i < Math.min(noteLines, lines.size()); i++) {
			Component line = lines.get(i);
			if (i == noteLines - 1 && lines.size() > noteLines) {
				line = UiText.ellipsize(font, Component.literal(line.getString() + " " + lines.get(i + 1).getString()), UiText.Size.BODY, autoWidth - 2);
			}
			UiText.draw(context, font, line, UiText.Size.BODY, autoX + 1, noteY + i * LINE_HEIGHT, noteColor);
		}
	}

	private void label(GuiGraphicsExtractor context, UiTheme theme, Component label, int left, int fieldTop) {
		UiText.draw(context, font, label, UiText.Size.LABEL, left + 1, fieldTop - UiText.lineHeight(font, UiText.Size.LABEL) - 6, theme.muted());
	}

	private void field(GuiGraphicsExtractor context, UiTheme theme, UiTextField field, int fieldX, int fieldY, int fieldWidth, Component placeholder, boolean problem) {
		int border = problem ? theme.warning() : field.focused() ? theme.accent() : theme.line();
		UiShapes.borderedRect(context, fieldX, fieldY, fieldWidth, FIELD_HEIGHT, 6, theme.segmentBackground(), border);
		field.draw(context, font, theme, fieldX + 8, fieldY + FIELD_HEIGHT / 2, fieldWidth - 16, placeholder);
	}

	// ---- input ----------------------------------------------------------------------------------

	void mouseClicked(double mouseX, double mouseY) {
		if (frame.closing()) {
			return;
		}
		if (picker != null) {
			// A click outside the list only closes it.
			picker.mouseClicked(mouseX, mouseY);
			if (picker.isClosed()) {
				picker = null;
			}
			return;
		}
		if (!contains(mouseX, mouseY, x, y, width, height) || contains(mouseX, mouseY, cancelX, footerY, cancelWidth, BUTTON_HEIGHT)) {
			close();
			return;
		}
		if (contains(mouseX, mouseY, saveX, footerY, saveWidth, BUTTON_HEIGHT)) {
			save();
			return;
		}
		int fieldX = x + PADDING + PREVIEW + 8;
		if (contains(mouseX, mouseY, fieldX, nameY, x + width - PADDING - fieldX, FIELD_HEIGHT)) {
			focus(name);
			name.click(font, mouseX, false);
			return;
		}
		if (contains(mouseX, mouseY, autoX, serversY, serversFieldWidth(), FIELD_HEIGHT)) {
			focus(servers);
			servers.click(font, mouseX, false);
			return;
		}
		focus(null);
		if (contains(mouseX, mouseY, pickerButtonX() - 2, serversY, PICKER_BUTTON + 4, FIELD_HEIGHT)) {
			openPicker();
			return;
		}
		ProfileIcon[] icons = ProfileIcon.values();
		for (int i = 0; i < icons.length; i++) {
			if (contains(mouseX, mouseY, iconX(i), iconY(i), ICON_CELL, ICON_CELL)) {
				icon = icons[i];
				return;
			}
		}
		ProfileColor[] colors = ProfileColor.values();
		for (int i = 0; i < colors.length; i++) {
			int dotX = colorX(i);
			if (contains(mouseX, mouseY, dotX - 3, colorsY - 3, COLOR_DOT + 6, COLOR_DOT + 6)) {
				color = colors[i];
				return;
			}
		}
		if (existing == null && startEdges.length == 3) {
			for (int i = 0; i < 2; i++) {
				if (contains(mouseX, mouseY, startEdges[i], startY, startEdges[i + 1] - startEdges[i], UiWidgets.SEGMENT_HEIGHT)) {
					copyCurrent = i == 0;
					return;
				}
			}
		}
		if (contains(mouseX, mouseY, autoX, autoY, autoWidth, UiWidgets.SWITCH_HEIGHT)) {
			singleplayer = !singleplayer;
		}
	}

	void mouseDragged(double mouseY) {
		if (picker != null) {
			picker.mouseDragged(mouseY);
		}
	}

	void mouseReleased() {
		if (picker != null) {
			picker.mouseReleased();
		}
	}

	void mouseScrolled(double mouseX, double mouseY, double amount) {
		if (picker != null && !picker.mouseScrolled(mouseX, mouseY, amount)) {
			closePicker();
		}
	}

	void keyPressed(KeyEvent input) {
		if (frame.closing()) {
			return;
		}
		if (picker != null) {
			// Typing filters the server list; Esc clears the filter, then closes the list.
			picker.keyPressed(input);
			if (picker.isClosed()) {
				picker = null;
			}
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
		if (picker != null) {
			picker.charTyped(input);
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

	/** Opens the server list and picks its first server; used by UI snapshots. Returns that server. */
	@Nullable String pickFirstServerForSnapshot() {
		openPicker();
		String first = picker == null ? null : picker.firstAddressForSnapshot();
		if (first != null) {
			toggleServer(first);
		}
		return first;
	}

	/** Whether the server list is open; used by UI snapshots. */
	@Nullable ServerPicker pickerForSnapshot() {
		return picker;
	}

	/** Saves as the Add or Save button would; used by UI snapshots. */
	void saveForSnapshot() {
		save();
	}

	/** Whether the name is refused as a duplicate; used by UI snapshots. */
	boolean nameTakenForSnapshot() {
		return nameTaken();
	}

	/** Whether the sheet uses two columns; used by UI snapshots. */
	boolean wideForSnapshot() {
		return wide;
	}

	/** How far the sheet reaches below the screen's top; used by UI snapshots. */
	int bottomForSnapshot() {
		return y + height;
	}

	private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
}
