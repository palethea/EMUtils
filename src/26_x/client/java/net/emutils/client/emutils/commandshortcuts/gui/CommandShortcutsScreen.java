package net.emutils.client.emutils.commandshortcuts.gui;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.commandshortcuts.CommandShortcut;
import net.emutils.client.emutils.gui.hub.HubIcons;
import net.emutils.client.emutils.gui.ui.UiConfirmDialog;
import net.emutils.client.emutils.gui.ui.UiContextMenu;
import net.emutils.client.emutils.gui.ui.UiIcons;
import net.emutils.client.emutils.gui.ui.UiPanelScreen;
import net.emutils.client.emutils.gui.ui.UiScrollArea;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.gui.ui.UiWidgets;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Command Shortcuts (#131): each shortcut with its name, whether it runs a command or
 * sends a message, the text itself and its keys, with buttons to run it now, edit or delete it, and a
 * right-click menu. Shortcuts are added and edited in a sheet over the list.
 */
public final class CommandShortcutsScreen extends UiPanelScreen {
	private static final int PADDING = 16;
	private static final int HEADER_BUTTON_HEIGHT = 20;
	private static final int ROW_HEIGHT = 44;
	private static final int ROW_GAP = 8;
	private static final int ROW_RADIUS = 9;
	private static final int ROW_PADDING = 12;
	private static final int ACTION = 20;
	private static final int ACTION_GAP = 2;
	private static final int FADE_HEIGHT = 12;
	private static final Identifier[] ACTIONS = {HubIcons.PLAY, HubIcons.PENCIL, HubIcons.TRASH};
	private static final String[] ACTION_TIPS = {EMUtilsTexts.UI_SHORTCUT_RUN, EMUtilsTexts.UI_SHORTCUT_EDIT_ACTION, EMUtilsTexts.UI_DELETE};

	private final UiScrollArea scroll = new UiScrollArea();
	private final List<RowBox> rows = new ArrayList<>();
	private @Nullable CommandShortcutSheet sheet;
	private @Nullable UiConfirmDialog dialog;
	private @Nullable UiContextMenu menu;
	private int addX;
	private int addWidth;
	private int clearX;
	private int clearWidth;
	private int headerButtonsY;
	private @Nullable Component tooltip;
	private int tooltipX;
	private int tooltipY;

	public CommandShortcutsScreen(@Nullable Screen parent) {
		super(Component.translatable(EMUtilsTexts.SCREEN_COMMAND_SHORTCUTS), parent);
	}

	@Override
	protected int maxPanelWidth() {
		return 560;
	}

	@Override
	protected void layout() {
		int headerHeight = UiText.lineHeight(font, UiText.Size.HEADING) + 6 + UiText.lineHeight(font, UiText.Size.BODY);
		headerButtonsY = panelY + PADDING + (headerHeight - HEADER_BUTTON_HEIGHT) / 2;
		int bodyY = panelY + PADDING + headerHeight + 12;
		scroll.setBounds(panelX + PADDING, bodyY, panelWidth - PADDING * 2 + UiScrollArea.GUTTER, panelY + panelHeight - PADDING / 2 - bodyY);
	}

	private static List<CommandShortcut> shortcuts() {
		return EMUtilsClient.commandShortcuts().store().shortcuts();
	}

	private boolean overlayOpen() {
		return sheet != null || dialog != null || menu != null;
	}

	private void openSheet(@Nullable CommandShortcut existing) {
		menu = null;
		sheet = new CommandShortcutSheet(font, anim, existing, () -> {
		});
	}

	/** Runs the shortcut as its keys would, closing every menu first so you're back in the game to see it. */
	private void run(CommandShortcut shortcut) {
		if (!canRun()) {
			return;
		}
		Minecraft client = minecraft;
		closeToGame();
		EMUtilsClient.commandShortcuts().runShortcut(client, shortcut);
	}

	/** Shortcuts need a world to send to; the screen can also be opened from the title screen. */
	private boolean canRun() {
		return minecraft.player != null && minecraft.getConnection() != null;
	}

	private void delete(CommandShortcut shortcut) {
		EMUtilsClient.commandShortcuts().store().remove(shortcut.id());
		EMUtilsClient.commandShortcuts().reload();
	}

	private void openClearDialog() {
		int count = shortcuts().size();
		dialog = new UiConfirmDialog(
			font,
			anim,
			Component.translatable(EMUtilsTexts.COMMAND_SHORTCUT_CLEAR_TITLE),
			Component.translatable(count == 1 ? EMUtilsTexts.UI_SHORTCUT_CLEAR_MESSAGE_ONE : EMUtilsTexts.UI_SHORTCUT_CLEAR_MESSAGE, count),
			Component.translatable(EMUtilsTexts.UI_CLEAR_ALL),
			() -> {
				EMUtilsClient.commandShortcuts().store().clear();
				EMUtilsClient.commandShortcuts().reload();
			}
		);
	}

	private void openMenu(CommandShortcut shortcut, int mouseX, int mouseY) {
		menu = new UiContextMenu(font, anim, mouseX, mouseY, List.of(
			new UiContextMenu.Item(Component.translatable(EMUtilsTexts.UI_SHORTCUT_RUN), canRun(), false, () -> run(shortcut)),
			UiContextMenu.Item.of(Component.translatable(EMUtilsTexts.UI_SHORTCUT_EDIT_ACTION), () -> openSheet(shortcut)),
			new UiContextMenu.Item(Component.translatable(EMUtilsTexts.UI_DELETE), true, true, () -> delete(shortcut))
		));
	}

	// ---- drawing --------------------------------------------------------------------------------

	@Override
	protected void drawPanel(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		tooltip = null;
		boolean interactive = !overlayOpen() && !closing();
		int hoverX = interactive ? mouseX : Integer.MIN_VALUE / 2;
		int hoverY = interactive ? mouseY : Integer.MIN_VALUE / 2;
		List<CommandShortcut> shortcuts = shortcuts();
		drawHeader(context, theme, shortcuts.size(), hoverX, hoverY);
		drawRows(context, theme, shortcuts, hoverX, hoverY);
	}

	private void drawHeader(GuiGraphicsExtractor context, UiTheme theme, int count, int mouseX, int mouseY) {
		int left = panelX + PADDING;
		int top = panelY + PADDING;
		UiText.draw(context, font, title, UiText.Size.HEADING, left, top, theme.text());
		int subtitleY = top + UiText.lineHeight(font, UiText.Size.HEADING) + 6;
		Component countText = Component.translatable(count == 1 ? EMUtilsTexts.UI_SHORTCUT_COUNT_ONE : EMUtilsTexts.UI_SHORTCUT_COUNT, count);
		UiText.draw(context, font, countText, UiText.Size.BODY, left, subtitleY, theme.muted());
		if (!EMUtilsClient.config().commandShortcutsEnabled()) {
			// The keys only work while the feature is on; say so where the list is.
			Component off = Component.literal(" · ").append(Component.translatable(EMUtilsTexts.UI_SHORTCUT_DISABLED));
			UiText.draw(context, font, off, UiText.Size.BODY, left + UiText.width(font, countText, UiText.Size.BODY), subtitleY, theme.warning());
		}

		Component add = Component.translatable(EMUtilsTexts.UI_SHORTCUT_NEW);
		int iconSize = 9;
		addWidth = UiText.width(font, add, UiText.Size.LABEL) + iconSize + 4 + 20;
		addX = panelX + panelWidth - PADDING - addWidth;
		float addHover = contains(mouseX, mouseY, addX, headerButtonsY, addWidth, HEADER_BUTTON_HEIGHT) ? 1.0F : 0.0F;
		UiShapes.roundedRect(context, addX, headerButtonsY, addWidth, HEADER_BUTTON_HEIGHT, 8, UiTheme.mix(theme.accent(), theme.accentHover(), addHover));
		UiIcons.draw(context, HubIcons.PLUS, addX + 10, headerButtonsY + (HEADER_BUTTON_HEIGHT - iconSize) / 2, iconSize, 0xFFFFFFFF);
		UiText.drawCentered(context, font, add, UiText.Size.LABEL, addX + 10 + iconSize + 4, headerButtonsY + HEADER_BUTTON_HEIGHT / 2, 0xFFFFFFFF);

		Component clear = Component.translatable(EMUtilsTexts.UI_CLEAR_ALL);
		clearWidth = UiWidgets.buttonWidth(font, clear) + 4;
		clearX = addX - 6 - clearWidth;
		boolean canClear = count > 0;
		float clearHover = canClear && contains(mouseX, mouseY, clearX, headerButtonsY, clearWidth, HEADER_BUTTON_HEIGHT) ? 1.0F : 0.0F;
		UiShapes.roundedRect(context, clearX, headerButtonsY, clearWidth, HEADER_BUTTON_HEIGHT, 8, UiTheme.fade(theme.hover(), clearHover));
		UiText.drawCentered(context, font, clear, UiText.Size.LABEL, clearX + (clearWidth - UiText.width(font, clear, UiText.Size.LABEL)) / 2, headerButtonsY + HEADER_BUTTON_HEIGHT / 2, canClear ? theme.textSecondary() : UiTheme.fade(theme.muted(), 0.5F));
	}

	private void drawRows(GuiGraphicsExtractor context, UiTheme theme, List<CommandShortcut> shortcuts, int mouseX, int mouseY) {
		scroll.setContentHeight(shortcuts.isEmpty() ? 0 : shortcuts.size() * (ROW_HEIGHT + ROW_GAP) - ROW_GAP + FADE_HEIGHT);
		scroll.animate(anim, "shortcuts-scroll", mouseX, mouseY);
		rows.clear();
		boolean mouseInList = scroll.contains(mouseX, mouseY) && !scroll.dragging();
		scroll.begin(context);
		context.pose().pushMatrix();
		context.pose().translate(0.0F, scroll.offset() - scroll.exactOffset());
		int width = scroll.contentWidth();
		int y = scroll.y() + FADE_HEIGHT / 2 - scroll.offset();
		for (CommandShortcut shortcut : shortcuts) {
			if (y + ROW_HEIGHT >= scroll.y() && y <= scroll.y() + scroll.height()) {
				rows.add(drawRow(context, theme, shortcut, scroll.x(), y, width, mouseInList ? mouseX : Integer.MIN_VALUE / 2, mouseY));
			}
			y += ROW_HEIGHT + ROW_GAP;
		}
		context.pose().popMatrix();
		if (shortcuts.isEmpty()) {
			int centerX = scroll.x() + width / 2;
			int iconSize = 22;
			int top = scroll.y() + Math.max(20, scroll.height() / 2 - 40);
			UiIcons.draw(context, HubIcons.KEYBOARD, centerX - iconSize / 2, top, iconSize, theme.muted());
			List<Component> lines = UiText.wrap(font, Component.translatable(EMUtilsTexts.UI_SHORTCUT_EMPTY), UiText.Size.BODY, Math.min(width - 40, 300));
			for (int i = 0; i < lines.size(); i++) {
				Component line = lines.get(i);
				UiText.draw(context, font, line, UiText.Size.BODY, centerX - UiText.width(font, line, UiText.Size.BODY) / 2, top + iconSize + 10 + i * 11, theme.muted());
			}
		}
		scroll.end(context, theme.panel(), FADE_HEIGHT, UiTheme.fade(theme.text(), 0.25F), UiTheme.fade(theme.text(), 0.45F));
	}

	private RowBox drawRow(GuiGraphicsExtractor context, UiTheme theme, CommandShortcut shortcut, int x, int y, int width, int mouseX, int mouseY) {
		boolean hovered = contains(mouseX, mouseY, x, y, width, ROW_HEIGHT);
		float hover = anim.towards("shortcut:" + shortcut.id(), hovered, 16.0F);
		UiShapes.borderedRect(context, x, y, width, ROW_HEIGHT, ROW_RADIUS, UiTheme.mix(theme.surface(), theme.surfaceHover(), hover), theme.border());
		int left = x + ROW_PADDING;
		// A shortcut without a name is shown by its text alone, on one line.
		boolean named = !shortcut.name().isBlank();
		int nameCenter = named ? y + ROW_PADDING + 4 : y + ROW_HEIGHT / 2;
		boolean command = shortcut.isCommand();
		UiIcons.draw(context, command ? HubIcons.TERMINAL : HubIcons.MESSAGE_SQUARE, left, nameCenter - 6, 12, theme.textSecondary());
		int nameX = left + 20;

		// Right side: the actions, then the keys.
		int actionsWidth = ACTIONS.length * ACTION + (ACTIONS.length - 1) * ACTION_GAP;
		int actionsX = x + width - ROW_PADDING + 4 - actionsWidth;
		int actionY = y + (ROW_HEIGHT - ACTION) / 2;
		for (int i = 0; i < ACTIONS.length; i++) {
			int actionX = actionsX + i * (ACTION + ACTION_GAP);
			boolean actionHovered = contains(mouseX, mouseY, actionX, actionY, ACTION, ACTION);
			boolean off = i == 0 && !canRun();
			int color = off ? UiTheme.fade(theme.muted(), 0.5F) : i == 2 && actionHovered ? theme.warning() : actionHovered ? theme.text() : theme.textSecondary();
			UiWidgets.ghostIconButton(context, theme, actionX, actionY, ACTION, ACTIONS[i], color, actionHovered && !off ? 1.0F : 0.0F);
			if (actionHovered) {
				showTooltip(Component.translatable(off ? EMUtilsTexts.UI_SHORTCUT_NEEDS_WORLD : ACTION_TIPS[i]), mouseX, mouseY);
			}
		}
		Component keys = Component.literal(shortcut.keyCombo().displayName());
		int keysWidth = UiWidgets.keycapWidth(font, keys);
		int keysX = actionsX - 10 - keysWidth;
		UiWidgets.keycap(context, font, theme, keysX, y + (ROW_HEIGHT - UiWidgets.KEYCAP_HEIGHT) / 2, keys, false, false, 0.0F);

		// Left side: the name and what kind of shortcut it is, the text below in the code font.
		Component type = Component.translatable(command ? EMUtilsTexts.UI_SHORTCUT_TYPE_COMMAND : EMUtilsTexts.UI_SHORTCUT_TYPE_MESSAGE);
		int badgeWidth = UiText.width(font, type, UiText.Size.SMALL) + 8;
		int textRight = keysX - 12;
		UiText.Size titleSize = named ? UiText.Size.BOLD : UiText.Size.CODE;
		Component name = UiText.ellipsize(font, Component.literal(shortcut.displayName()), titleSize, textRight - nameX - badgeWidth - 6);
		UiText.drawCentered(context, font, name, titleSize, nameX, nameCenter, theme.text());
		int badgeX = nameX + UiText.width(font, name, titleSize) + 6;
		UiWidgets.badge(context, font, badgeX, nameCenter - (UiText.lineHeight(font, UiText.Size.SMALL) + 5) / 2, type, theme.segmentBackground(), theme.textSecondary());
		if (named) {
			Component text = UiText.ellipsize(font, Component.literal(shortcut.displayText()), UiText.Size.CODE, textRight - left);
			UiText.drawCentered(context, font, text, UiText.Size.CODE, left, y + ROW_HEIGHT - ROW_PADDING - 4, theme.muted());
		}
		return new RowBox(shortcut, y, actionsX, actionY);
	}

	private void showTooltip(Component text, int mouseX, int mouseY) {
		tooltip = text;
		tooltipX = mouseX;
		tooltipY = mouseY;
	}

	@Override
	protected void drawOverlay(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		if (tooltip != null && !overlayOpen()) {
			UiWidgets.tooltip(context, font, theme, tooltip, tooltipX, tooltipY, width, height);
		}
		if (menu != null) {
			menu.render(context, theme, mouseX, mouseY, width, height);
			if (menu.isClosed()) {
				menu = null;
			}
		}
		if (sheet != null) {
			sheet.render(context, theme, mouseX, mouseY, width, height);
			if (sheet.isClosed()) {
				sheet = null;
			}
		}
		if (dialog != null) {
			dialog.render(context, theme, mouseX, mouseY, width, height);
			if (dialog.isClosed()) {
				dialog = null;
			}
		}
	}

	// ---- input ----------------------------------------------------------------------------------

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		if (closing()) {
			return true;
		}
		double mouseX = click.x();
		double mouseY = click.y();
		boolean left = click.button() == InputConstants.MOUSE_BUTTON_LEFT;
		if (menu != null) {
			menu.mouseClicked(mouseX, mouseY);
			menu = null;
			return true;
		}
		if (dialog != null) {
			if (left) {
				dialog.mouseClicked(mouseX, mouseY);
			}
			return true;
		}
		if (sheet != null) {
			if (left) {
				sheet.mouseClicked(mouseX, mouseY);
			}
			return true;
		}
		RowBox row = rowAt(mouseX, mouseY);
		if (click.button() == InputConstants.MOUSE_BUTTON_RIGHT && row != null) {
			openMenu(row.shortcut(), (int) mouseX, (int) mouseY);
			return true;
		}
		if (!left) {
			return super.mouseClicked(click, doubled);
		}
		if (contains(mouseX, mouseY, addX, headerButtonsY, addWidth, HEADER_BUTTON_HEIGHT)) {
			openSheet(null);
			return true;
		}
		if (!shortcuts().isEmpty() && contains(mouseX, mouseY, clearX, headerButtonsY, clearWidth, HEADER_BUTTON_HEIGHT)) {
			openClearDialog();
			return true;
		}
		if (scroll.mouseClicked(mouseX, mouseY)) {
			return true;
		}
		if (row != null) {
			for (int i = 0; i < ACTIONS.length; i++) {
				if (contains(mouseX, mouseY, row.actionsX() + i * (ACTION + ACTION_GAP), row.actionY(), ACTION, ACTION)) {
					switch (i) {
						case 0 -> run(row.shortcut());
						case 1 -> openSheet(row.shortcut());
						default -> delete(row.shortcut());
					}
					return true;
				}
			}
			// Anywhere else on the row edits it.
			openSheet(row.shortcut());
			return true;
		}
		return super.mouseClicked(click, doubled);
	}

	private @Nullable RowBox rowAt(double mouseX, double mouseY) {
		if (!scroll.contains(mouseX, mouseY)) {
			return null;
		}
		// Rows are drawn shifted by the scroll's sub-pixel offset; hit-testing by whole rows is close enough.
		for (RowBox row : rows) {
			if (mouseY >= row.y() && mouseY < row.y() + ROW_HEIGHT) {
				return row;
			}
		}
		return null;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
		if (!overlayOpen() && scroll.mouseDragged(click.y())) {
			return true;
		}
		return super.mouseDragged(click, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		if (scroll.mouseReleased()) {
			return true;
		}
		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (closing() || sheet != null || dialog != null) {
			return true;
		}
		menu = null;
		if (scroll.scroll(mouseX, mouseY, verticalAmount)) {
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean keyPressed(KeyEvent input) {
		if (closing()) {
			return true;
		}
		if (menu != null) {
			if (input.isEscape()) {
				menu = null;
			}
			return true;
		}
		if (dialog != null) {
			dialog.keyPressed(input);
			return true;
		}
		if (sheet != null) {
			sheet.keyPressed(input);
			return true;
		}
		return super.keyPressed(input);
	}

	@Override
	public boolean charTyped(CharacterEvent input) {
		if (closing()) {
			return true;
		}
		if (sheet != null) {
			sheet.charTyped(input);
			return true;
		}
		return super.charTyped(input);
	}

	@Override
	public void onClose() {
		if (sheet != null) {
			sheet.close();
		}
		super.onClose();
	}

	/** Opens the add sheet, or the edit sheet for the shortcut named {@code name}; used by UI snapshots. */
	public void openSheetForSnapshot(@Nullable String name) {
		openSheet(name == null ? null : shortcuts().stream().filter(shortcut -> shortcut.displayName().equals(name)).findFirst().orElse(null));
	}

	/** Presses Run now on the first shortcut; used by UI snapshots. */
	public void runFirstForSnapshot() {
		if (!shortcuts().isEmpty()) {
			run(shortcuts().getFirst());
		}
	}

	/** Opens the right-click menu for the first shortcut; used by UI snapshots. */
	public void openMenuForSnapshot(int mouseX, int mouseY) {
		if (!shortcuts().isEmpty()) {
			openMenu(shortcuts().getFirst(), mouseX, mouseY);
		}
	}

	private record RowBox(CommandShortcut shortcut, int y, int actionsX, int actionY) {
	}
}
