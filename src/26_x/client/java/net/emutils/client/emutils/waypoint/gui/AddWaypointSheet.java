package net.emutils.client.emutils.waypoint.gui;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import java.util.function.Consumer;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.compat.MinecraftClientCompat;
import net.emutils.client.emutils.gui.hub.HubIcons;
import net.emutils.client.emutils.gui.ui.UiAnim;
import net.emutils.client.emutils.gui.ui.UiColorPicker;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.gui.ui.UiSheetFrame;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.emutils.gui.ui.UiTextField;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.gui.ui.UiWidgets;
import net.emutils.client.emutils.text.EmUtilsChatPrefix;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * The Add Waypoint form as a sheet over the waypoints list (#103): a name, the coordinates (your
 * position to start with), one of the preset colors or any other from the color picker, and the beacon.
 * Enter adds, Esc cancels, Tab moves between the fields.
 */
final class AddWaypointSheet {
	/** The preset colors; the default custom color is picked if it's one of them. */
	private static final int[] PRESET_COLORS = {
		0xFFFF5555, 0xFF55FF55, 0xFF5555FF, 0xFFFFFF55, 0xFF55FFFF, 0xFFFF55FF, 0xFFFFAA55, 0xFFFFFFFF, 0xFF555555
	};
	private static final int WIDTH = 320;
	private static final int PADDING = 16;
	private static final int FIELD_HEIGHT = 20;
	private static final int BUTTON_HEIGHT = 20;
	private static final int SWATCH = 16;
	private static final int SWATCH_GAP = 6;
	private static final int ICON_BUTTON = 20;

	private final Font font;
	private final UiAnim anim;
	private final Consumer<Boolean> onClose;
	private final UiSheetFrame frame;
	private final UiTextField name = new UiTextField(this, 32);
	private final UiTextField[] coords = {
		new UiTextField(this, 9, AddWaypointSheet::coordinateChar),
		new UiTextField(this, 9, AddWaypointSheet::coordinateChar),
		new UiTextField(this, 9, AddWaypointSheet::coordinateChar)
	};
	private final List<UiTextField> fields = List.of(name, coords[0], coords[1], coords[2]);
	private int color;
	private boolean beacon;
	private @Nullable UiColorPicker colorPicker;
	private int x;
	private int y;
	private int height;
	private int nameY;
	private int coordsY;
	private int coordWidth;
	private int colorY;
	private int beaconY;
	private int footerY;
	private int addX;
	private int addWidth;
	private int cancelX;
	private int cancelWidth;
	private int locateX;

	/** {@code onClose} runs as soon as the sheet starts closing, with whether a waypoint was added. */
	AddWaypointSheet(Font font, UiAnim anim, Consumer<Boolean> onClose) {
		this.font = font;
		this.anim = anim;
		this.onClose = onClose;
		this.frame = new UiSheetFrame(anim, "add-waypoint:" + System.identityHashCode(this), 16);
		this.color = 0xFF000000 | EMUtilsClient.config().waypointDefaultCustomColor();
		useMyPosition();
		focus(name);
	}

	private static boolean coordinateChar(int codepoint) {
		return Character.isDigit(codepoint) || codepoint == '-';
	}

	private void useMyPosition() {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}
		coords[0].setText(String.valueOf(client.player.getBlockX()));
		coords[1].setText(String.valueOf(client.player.getBlockY()));
		coords[2].setText(String.valueOf(client.player.getBlockZ()));
		for (UiTextField field : coords) {
			field.setFocused(false);
		}
	}

	/** Focuses one field; the others lose focus first, because they share the text input request. */
	private void focus(@Nullable UiTextField target) {
		for (UiTextField field : fields) {
			if (field != target && field.focused()) {
				field.setFocused(false);
			}
		}
		if (target != null && !target.focused()) {
			target.setFocused(true);
		}
	}

	private @Nullable Integer coordinate(int axis) {
		try {
			return Integer.parseInt(coords[axis].text().trim());
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	private boolean valid() {
		return coordinate(0) != null && coordinate(1) != null && coordinate(2) != null;
	}

	boolean isClosed() {
		return frame.isClosed();
	}

	/** Closes without adding, for example when the screen closes. */
	void close() {
		finish(false);
	}

	private void finish(boolean added) {
		if (frame.closing()) {
			return;
		}
		focus(null);
		if (colorPicker != null) {
			colorPicker.release();
			colorPicker = null;
		}
		frame.close();
		onClose.accept(added);
	}

	private void add() {
		if (!valid()) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		String label = name.text().trim();
		if (label.isEmpty()) {
			label = "Waypoint";
		}
		EMUtilsClient.waypoint().addCustom(client, label, coordinate(0), coordinate(1), coordinate(2), color, beacon);
		if (client.gui != null) {
			MinecraftClientCompat.chat(client).addClientSystemMessage(EmUtilsChatPrefix.chat(
				Component.translatable(EMUtilsTexts.WAYPOINT_ADDED, label).withStyle(ChatFormatting.GREEN)
			));
		}
		finish(true);
	}

	// ---- drawing --------------------------------------------------------------------------------

	private void layout(int screenWidth, int screenHeight) {
		int labelBlock = UiText.lineHeight(font, UiText.Size.LABEL) + 6;
		int headerHeight = UiText.lineHeight(font, UiText.Size.HEADING);
		nameY = PADDING + headerHeight + 16 + labelBlock;
		coordsY = nameY + FIELD_HEIGHT + 12 + labelBlock;
		colorY = coordsY + FIELD_HEIGHT + 12 + labelBlock;
		beaconY = colorY + SWATCH + 16;
		footerY = beaconY + 34;
		height = footerY + BUTTON_HEIGHT + PADDING;
		x = (screenWidth - WIDTH) / 2;
		y = (screenHeight - height) / 2;
		coordWidth = (WIDTH - PADDING * 2 - ICON_BUTTON - 6 - 12) / 3;
	}

	void render(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY, int screenWidth, int screenHeight) {
		layout(screenWidth, screenHeight);
		if (frame.firstFrame()) {
			UiText.prepare(Component.translatable(EMUtilsTexts.SCREEN_ADD_WAYPOINT), UiText.Size.HEADING);
		}
		if (!frame.begin(context, theme, screenWidth, screenHeight, x, y, WIDTH, height)) {
			return;
		}
		boolean interactive = colorPicker == null && !frame.closing();
		int hoverX = interactive ? mouseX : Integer.MIN_VALUE / 2;
		int hoverY = interactive ? mouseY : Integer.MIN_VALUE / 2;
		int left = x + PADDING;
		int right = x + WIDTH - PADDING;
		UiText.draw(context, font, Component.translatable(EMUtilsTexts.SCREEN_ADD_WAYPOINT), UiText.Size.HEADING, left, y + PADDING, theme.text());

		label(context, theme, Component.translatable(EMUtilsTexts.UI_WAYPOINT_NAME), left, y + nameY);
		field(context, theme, name, left, y + nameY, right - left, Component.translatable(EMUtilsTexts.WAYPOINT_LABEL_PLACEHOLDER), false);

		label(context, theme, Component.translatable(EMUtilsTexts.UI_WAYPOINT_POSITION), left, y + coordsY);
		String[] axes = {"X", "Y", "Z"};
		for (int i = 0; i < 3; i++) {
			int fieldX = left + i * (coordWidth + 6);
			UiShapes.borderedRect(context, fieldX, y + coordsY, coordWidth, FIELD_HEIGHT, 6, theme.segmentBackground(), coords[i].focused() ? theme.accent() : coordinate(i) == null ? theme.warning() : theme.line());
			UiText.drawCentered(context, font, Component.literal(axes[i]), UiText.Size.LABEL, fieldX + 7, y + coordsY + FIELD_HEIGHT / 2, theme.muted());
			coords[i].draw(context, font, theme, fieldX + 17, y + coordsY + FIELD_HEIGHT / 2, coordWidth - 22, Component.empty());
		}
		locateX = right - ICON_BUTTON;
		float locateHover = contains(hoverX, hoverY, locateX, y + coordsY, ICON_BUTTON, ICON_BUTTON) ? 1.0F : 0.0F;
		UiWidgets.ghostIconButton(context, theme, locateX, y + coordsY, ICON_BUTTON, HubIcons.CROSSHAIR, theme.textSecondary(), locateHover);

		label(context, theme, Component.translatable(EMUtilsTexts.UI_WAYPOINT_COLOR), left, y + colorY);
		boolean preset = false;
		for (int i = 0; i < PRESET_COLORS.length; i++) {
			boolean selected = (PRESET_COLORS[i] & 0xFFFFFF) == (color & 0xFFFFFF);
			preset |= selected;
			swatch(context, theme, left + i * (SWATCH + SWATCH_GAP), y + colorY, PRESET_COLORS[i], selected);
		}
		// The last swatch shows a custom color and opens the color picker for any other one.
		int customX = left + PRESET_COLORS.length * (SWATCH + SWATCH_GAP);
		if (preset) {
			UiShapes.circle(context, customX, y + colorY, SWATCH, theme.segmentBackground());
			UiText.drawCentered(context, font, Component.literal("+"), UiText.Size.BOLD, customX + 5, y + colorY + SWATCH / 2, theme.textSecondary());
		} else {
			swatch(context, theme, customX, y + colorY, color, true);
		}

		UiText.drawCentered(context, font, Component.translatable(EMUtilsTexts.UI_WAYPOINT_BEACON), UiText.Size.BOLD, left, y + beaconY + 6, theme.text());
		UiText.drawCentered(context, font, Component.translatable(EMUtilsTexts.UI_WAYPOINT_BEACON_DESC), UiText.Size.BODY, left, y + beaconY + 20, theme.muted());
		float beaconOn = anim.transition("add-waypoint-beacon", beacon, 0.18F);
		UiWidgets.toggle(context, theme, right - UiWidgets.SWITCH_WIDTH, y + beaconY + 6 - UiWidgets.SWITCH_HEIGHT / 2, beaconOn, 0.0F);

		Component addLabel = Component.translatable(EMUtilsTexts.UI_ADD);
		addWidth = Math.max(60, UiWidgets.buttonWidth(font, addLabel) + 12);
		addX = right - addWidth;
		cancelWidth = UiWidgets.buttonWidth(font, CommonComponents.GUI_CANCEL) + 4;
		cancelX = addX - 6 - cancelWidth;
		UiWidgets.button(context, font, theme, cancelX, y + footerY, cancelWidth, BUTTON_HEIGHT, CommonComponents.GUI_CANCEL, UiWidgets.ButtonStyle.GHOST, contains(hoverX, hoverY, cancelX, y + footerY, cancelWidth, BUTTON_HEIGHT) ? 1.0F : 0.0F);
		UiWidgets.button(context, font, theme, addX, y + footerY, addWidth, BUTTON_HEIGHT, addLabel, valid() ? UiWidgets.ButtonStyle.PRIMARY : UiWidgets.ButtonStyle.SURFACE, valid() && contains(hoverX, hoverY, addX, y + footerY, addWidth, BUTTON_HEIGHT) ? 1.0F : 0.0F);
		frame.endBody(context);

		if (colorPicker != null) {
			colorPicker.render(context, font, theme);
		} else if (locateHover > 0.0F) {
			UiWidgets.tooltip(context, font, theme, Component.translatable(EMUtilsTexts.UI_USE_MY_POSITION), mouseX, mouseY, screenWidth, screenHeight);
		}
		frame.end();
	}

	private void label(GuiGraphicsExtractor context, UiTheme theme, Component text, int left, int fieldTop) {
		UiText.draw(context, font, text, UiText.Size.LABEL, left + 1, fieldTop - UiText.lineHeight(font, UiText.Size.LABEL) - 6, theme.muted());
	}

	private void field(GuiGraphicsExtractor context, UiTheme theme, UiTextField field, int fieldX, int fieldY, int width, Component placeholder, boolean invalid) {
		UiShapes.borderedRect(context, fieldX, fieldY, width, FIELD_HEIGHT, 6, theme.segmentBackground(), field.focused() ? theme.accent() : invalid ? theme.warning() : theme.line());
		field.draw(context, font, theme, fieldX + 8, fieldY + FIELD_HEIGHT / 2, width - 16, placeholder);
	}

	private static void swatch(GuiGraphicsExtractor context, UiTheme theme, int swatchX, int swatchY, int swatchColor, boolean selected) {
		if (selected) {
			UiShapes.circle(context, swatchX - 3, swatchY - 3, SWATCH + 6, theme.text());
			UiShapes.circle(context, swatchX - 1, swatchY - 1, SWATCH + 2, theme.surface());
		}
		UiShapes.circle(context, swatchX, swatchY, SWATCH, 0xFF000000 | swatchColor);
	}

	// ---- input ----------------------------------------------------------------------------------

	void mouseClicked(double mouseX, double mouseY) {
		if (frame.closing()) {
			return;
		}
		if (colorPicker != null) {
			if (colorPicker.contains(mouseX, mouseY)) {
				colorPicker.mouseClicked(mouseX, mouseY);
			} else {
				colorPicker.blur();
				colorPicker.release();
				colorPicker = null;
			}
			return;
		}
		if (!contains(mouseX, mouseY, x, y, WIDTH, height) || contains(mouseX, mouseY, cancelX, y + footerY, cancelWidth, BUTTON_HEIGHT)) {
			finish(false);
			return;
		}
		if (contains(mouseX, mouseY, addX, y + footerY, addWidth, BUTTON_HEIGHT)) {
			add();
			return;
		}
		int left = x + PADDING;
		int right = x + WIDTH - PADDING;
		if (contains(mouseX, mouseY, left, y + nameY, right - left, FIELD_HEIGHT)) {
			focus(name);
			name.click(font, mouseX, false);
			return;
		}
		for (int i = 0; i < 3; i++) {
			int fieldX = left + i * (coordWidth + 6);
			if (contains(mouseX, mouseY, fieldX, y + coordsY, coordWidth, FIELD_HEIGHT)) {
				focus(coords[i]);
				coords[i].click(font, mouseX, false);
				return;
			}
		}
		focus(null);
		if (contains(mouseX, mouseY, locateX, y + coordsY, ICON_BUTTON, ICON_BUTTON)) {
			useMyPosition();
			return;
		}
		for (int i = 0; i <= PRESET_COLORS.length; i++) {
			int swatchX = left + i * (SWATCH + SWATCH_GAP);
			if (contains(mouseX, mouseY, swatchX - 2, y + colorY - 2, SWATCH + 4, SWATCH + 4)) {
				if (i < PRESET_COLORS.length) {
					color = PRESET_COLORS[i];
				} else {
					colorPicker = new UiColorPicker(() -> color, value -> color = 0xFF000000 | value, swatchX + SWATCH + 8 + 164, y + colorY + SWATCH / 2, Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight());
				}
				return;
			}
		}
		if (contains(mouseX, mouseY, left, y + beaconY - 4, right - left, 30)) {
			beacon = !beacon;
		}
	}

	void mouseDragged(double mouseX, double mouseY) {
		if (colorPicker != null) {
			colorPicker.drag(mouseX, mouseY);
		}
	}

	void mouseReleased() {
		if (colorPicker != null) {
			colorPicker.release();
		}
	}

	/** Enter adds, Esc closes the color picker or cancels, Tab moves between fields; the sheet takes every key. */
	void keyPressed(KeyEvent input) {
		if (frame.closing()) {
			return;
		}
		if (colorPicker != null) {
			if (colorPicker.keyPressed(input)) {
				return;
			}
			if (input.isEscape()) {
				colorPicker.release();
				colorPicker = null;
			}
			return;
		}
		if (input.isEscape()) {
			finish(false);
			return;
		}
		if (input.isConfirmation()) {
			add();
			return;
		}
		if (input.key() == InputConstants.KEY_TAB) {
			int current = -1;
			for (int i = 0; i < fields.size(); i++) {
				if (fields.get(i).focused()) {
					current = i;
				}
			}
			int step = input.hasShiftDown() ? -1 : 1;
			int next = current < 0 ? 0 : Math.floorMod(current + step, fields.size());
			focus(fields.get(next));
			fields.get(next).setText(fields.get(next).text());
			return;
		}
		for (UiTextField field : fields) {
			if (field.keyPressed(input, () -> {
			})) {
				return;
			}
		}
	}

	void charTyped(CharacterEvent input) {
		if (colorPicker != null) {
			colorPicker.charTyped(input);
			return;
		}
		for (UiTextField field : fields) {
			if (field.charTyped(input, () -> {
			})) {
				return;
			}
		}
	}

	private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
}
