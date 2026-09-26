package net.emutils.client.emutils.profile.gui;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.emutils.client.EMUtilsClient;
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
import net.emutils.client.emutils.profile.Profile;
import net.emutils.client.emutils.profile.ProfileManager;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Config profiles (#89): every profile with its icon, name and where it loads by itself, with buttons to
 * switch to it, edit, duplicate or delete it, and a right-click menu that also exports it and moves it.
 * Profiles are added and edited in a sheet over the list, and imported from the clipboard.
 */
public final class ProfilesScreen extends UiPanelScreen {
	private static final int PADDING = 16;
	private static final int HEADER_BUTTON_HEIGHT = 20;
	private static final int ROW_HEIGHT = 44;
	private static final int ROW_GAP = 8;
	private static final int ROW_RADIUS = 9;
	private static final int ROW_PADDING = 12;
	private static final int BADGE = 24;
	private static final int ACTION = 20;
	private static final int ACTION_GAP = 2;
	private static final int USE_HEIGHT = 16;
	private static final int FADE_HEIGHT = 12;
	private static final long STATUS_MILLIS = 3000L;
	private static final Identifier[] ACTIONS = {HubIcons.PENCIL, HubIcons.COPY, HubIcons.TRASH};
	private static final String[] ACTION_TIPS = {EMUtilsTexts.UI_PROFILE_EDIT_ACTION, EMUtilsTexts.UI_PROFILE_DUPLICATE, EMUtilsTexts.UI_DELETE};

	private final UiScrollArea scroll = new UiScrollArea();
	private final List<RowBox> rows = new ArrayList<>();
	private @Nullable ProfileSheet sheet;
	private @Nullable UiConfirmDialog dialog;
	private @Nullable UiContextMenu menu;
	private int addX;
	private int addWidth;
	private int importX;
	private int importWidth;
	private int headerButtonsY;
	private @Nullable Component tooltip;
	private int tooltipX;
	private int tooltipY;
	/** A short message in place of the subtitle, such as after exporting; it fades after a few seconds. */
	private @Nullable Component status;
	private boolean statusWarning;
	private long statusUntil;

	public ProfilesScreen(@Nullable Screen parent) {
		super(Component.translatable(EMUtilsTexts.SCREEN_PROFILES), parent);
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

	private static ProfileManager manager() {
		return EMUtilsClient.profiles();
	}

	private boolean overlayOpen() {
		return sheet != null || dialog != null || menu != null;
	}

	private void openSheet(@Nullable Profile existing) {
		menu = null;
		sheet = new ProfileSheet(font, anim, existing);
	}

	private void use(Profile profile) {
		if (!manager().pick(profile)) {
			showStatus(Component.translatable(EMUtilsTexts.PROFILE_SWITCH_FAILED), true);
		}
	}

	private void duplicate(Profile profile) {
		if (manager().duplicate(profile) == null) {
			showStatus(Component.translatable(EMUtilsTexts.UI_PROFILE_UNREADABLE, profile.name()), true);
		}
	}

	private void confirmDelete(Profile profile) {
		if (profile.isDefault()) {
			return;
		}
		menu = null;
		dialog = new UiConfirmDialog(
			font,
			anim,
			Component.translatable(EMUtilsTexts.UI_PROFILE_DELETE_TITLE, profile.name()),
			Component.translatable(profile == manager().active() ? EMUtilsTexts.UI_PROFILE_DELETE_ACTIVE_MESSAGE : EMUtilsTexts.UI_PROFILE_DELETE_MESSAGE),
			Component.translatable(EMUtilsTexts.UI_DELETE),
			() -> {
				if (!manager().delete(profile)) {
					showStatus(Component.translatable(EMUtilsTexts.PROFILE_SWITCH_FAILED), true);
				}
			}
		);
	}

	private void confirmReset(Profile profile) {
		menu = null;
		dialog = new UiConfirmDialog(
			font,
			anim,
			Component.translatable(EMUtilsTexts.UI_PROFILE_RESET_TITLE, profile.name()),
			Component.translatable(EMUtilsTexts.UI_PROFILE_RESET_MESSAGE),
			Component.translatable(EMUtilsTexts.UI_PROFILE_RESET),
			() -> {
				if (manager().resetToDefaults(profile)) {
					showStatus(Component.translatable(EMUtilsTexts.UI_PROFILE_RESET_DONE, profile.name()), false);
				} else {
					showStatus(Component.translatable(EMUtilsTexts.UI_PROFILE_RESET_FAILED), true);
				}
			}
		);
	}

	private void export(Profile profile) {
		String exported = manager().export(profile);
		if (exported == null) {
			showStatus(Component.translatable(EMUtilsTexts.UI_PROFILE_UNREADABLE, profile.name()), true);
			return;
		}
		minecraft.keyboardHandler.setClipboard(exported);
		showStatus(Component.translatable(EMUtilsTexts.UI_PROFILE_EXPORTED, profile.name()), false);
	}

	private void importFromClipboard() {
		Profile imported = manager().importProfile(minecraft.keyboardHandler.getClipboard());
		if (imported == null) {
			showStatus(Component.translatable(EMUtilsTexts.UI_PROFILE_IMPORT_FAILED), true);
		} else {
			showStatus(Component.translatable(EMUtilsTexts.UI_PROFILE_IMPORTED, imported.name()), false);
		}
	}

	private void showStatus(Component message, boolean warning) {
		status = message;
		statusWarning = warning;
		statusUntil = System.currentTimeMillis() + STATUS_MILLIS;
	}

	private void openMenu(Profile profile, int mouseX, int mouseY) {
		List<Profile> profiles = manager().profiles();
		int index = profiles.indexOf(profile);
		menu = new UiContextMenu(font, anim, mouseX, mouseY, List.of(
			new UiContextMenu.Item(Component.translatable(EMUtilsTexts.UI_PROFILE_USE), profile != manager().active(), false, () -> use(profile)),
			UiContextMenu.Item.of(Component.translatable(EMUtilsTexts.UI_PROFILE_EDIT_ACTION), () -> openSheet(profile)),
			UiContextMenu.Item.of(Component.translatable(EMUtilsTexts.UI_PROFILE_DUPLICATE), () -> duplicate(profile)),
			UiContextMenu.Item.of(Component.translatable(EMUtilsTexts.UI_PROFILE_EXPORT), () -> export(profile)),
			UiContextMenu.Item.of(Component.translatable(EMUtilsTexts.UI_PROFILE_RESET), () -> confirmReset(profile)),
			new UiContextMenu.Item(Component.translatable(EMUtilsTexts.UI_PROFILE_MOVE_UP), index > 1, false, () -> manager().move(profile, -1)),
			new UiContextMenu.Item(Component.translatable(EMUtilsTexts.UI_PROFILE_MOVE_DOWN), index > 0 && index < profiles.size() - 1, false, () -> manager().move(profile, 1)),
			new UiContextMenu.Item(Component.translatable(EMUtilsTexts.UI_DELETE), !profile.isDefault(), true, () -> confirmDelete(profile))
		));
	}

	// ---- drawing --------------------------------------------------------------------------------

	@Override
	protected void drawPanel(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		tooltip = null;
		boolean interactive = !overlayOpen() && !closing();
		int hoverX = interactive ? mouseX : Integer.MIN_VALUE / 2;
		int hoverY = interactive ? mouseY : Integer.MIN_VALUE / 2;
		List<Profile> profiles = manager().profiles();
		drawHeader(context, theme, profiles.size(), hoverX, hoverY);
		drawRows(context, theme, profiles, hoverX, hoverY);
	}

	private void drawHeader(GuiGraphicsExtractor context, UiTheme theme, int count, int mouseX, int mouseY) {
		int left = panelX + PADDING;
		int top = panelY + PADDING;
		UiText.draw(context, font, title, UiText.Size.HEADING, left, top, theme.text());
		int subtitleY = top + UiText.lineHeight(font, UiText.Size.HEADING) + 6;

		Component add = Component.translatable(EMUtilsTexts.UI_PROFILE_NEW);
		int iconSize = 9;
		addWidth = UiText.width(font, add, UiText.Size.LABEL) + iconSize + 4 + 20;
		addX = panelX + panelWidth - PADDING - addWidth;
		float addHover = contains(mouseX, mouseY, addX, headerButtonsY, addWidth, HEADER_BUTTON_HEIGHT) ? 1.0F : 0.0F;
		UiShapes.roundedRect(context, addX, headerButtonsY, addWidth, HEADER_BUTTON_HEIGHT, 8, UiTheme.mix(theme.accent(), theme.accentHover(), addHover));
		UiIcons.draw(context, HubIcons.PLUS, addX + 10, headerButtonsY + (HEADER_BUTTON_HEIGHT - iconSize) / 2, iconSize, 0xFFFFFFFF);
		UiText.drawCentered(context, font, add, UiText.Size.LABEL, addX + 10 + iconSize + 4, headerButtonsY + HEADER_BUTTON_HEIGHT / 2, 0xFFFFFFFF);

		Component importLabel = Component.translatable(EMUtilsTexts.UI_PROFILE_IMPORT);
		importWidth = UiText.width(font, importLabel, UiText.Size.LABEL) + iconSize + 4 + 20;
		importX = addX - 6 - importWidth;
		boolean importHovered = contains(mouseX, mouseY, importX, headerButtonsY, importWidth, HEADER_BUTTON_HEIGHT);
		UiShapes.roundedRect(context, importX, headerButtonsY, importWidth, HEADER_BUTTON_HEIGHT, 8, UiTheme.fade(theme.hover(), importHovered ? 1.0F : 0.0F));
		UiIcons.draw(context, HubIcons.CLIPBOARD_PASTE, importX + 10, headerButtonsY + (HEADER_BUTTON_HEIGHT - iconSize) / 2, iconSize, theme.textSecondary());
		UiText.drawCentered(context, font, importLabel, UiText.Size.LABEL, importX + 10 + iconSize + 4, headerButtonsY + HEADER_BUTTON_HEIGHT / 2, theme.textSecondary());
		if (importHovered) {
			showTooltip(Component.translatable(EMUtilsTexts.UI_PROFILE_IMPORT_TOOLTIP), mouseX, mouseY);
		}

		int subtitleWidth = importX - 12 - left;
		if (status != null && System.currentTimeMillis() < statusUntil) {
			UiText.draw(context, font, UiText.ellipsize(font, status, UiText.Size.BODY, subtitleWidth), UiText.Size.BODY, left, subtitleY, statusWarning ? theme.warning() : theme.accent());
		} else {
			status = null;
			Component subtitle = Component.translatable(count == 1 ? EMUtilsTexts.UI_PROFILE_COUNT_ONE : EMUtilsTexts.UI_PROFILE_COUNT, count, manager().active().name());
			UiText.draw(context, font, UiText.ellipsize(font, subtitle, UiText.Size.BODY, subtitleWidth), UiText.Size.BODY, left, subtitleY, theme.muted());
		}
	}

	private void drawRows(GuiGraphicsExtractor context, UiTheme theme, List<Profile> profiles, int mouseX, int mouseY) {
		scroll.setContentHeight(profiles.size() * (ROW_HEIGHT + ROW_GAP) - ROW_GAP + FADE_HEIGHT);
		scroll.animate(anim, "profiles-scroll", mouseX, mouseY);
		rows.clear();
		boolean mouseInList = scroll.contains(mouseX, mouseY) && !scroll.dragging();
		scroll.begin(context);
		context.pose().pushMatrix();
		context.pose().translate(0.0F, scroll.offset() - scroll.exactOffset());
		int width = scroll.contentWidth();
		int y = scroll.y() + FADE_HEIGHT / 2 - scroll.offset();
		Profile active = manager().active();
		for (Profile profile : profiles) {
			if (y + ROW_HEIGHT >= scroll.y() && y <= scroll.y() + scroll.height()) {
				rows.add(drawRow(context, theme, profile, profile == active, scroll.x(), y, width, mouseInList ? mouseX : Integer.MIN_VALUE / 2, mouseY));
			}
			y += ROW_HEIGHT + ROW_GAP;
		}
		context.pose().popMatrix();
		scroll.end(context, theme.panel(), FADE_HEIGHT, UiTheme.fade(theme.text(), 0.25F), UiTheme.fade(theme.text(), 0.45F));
	}

	private RowBox drawRow(GuiGraphicsExtractor context, UiTheme theme, Profile profile, boolean active, int x, int y, int width, int mouseX, int mouseY) {
		boolean hovered = contains(mouseX, mouseY, x, y, width, ROW_HEIGHT);
		float hover = anim.towards("profile:" + profile.id(), hovered, 16.0F);
		// The active profile's row is outlined in the accent color.
		UiShapes.borderedRect(context, x, y, width, ROW_HEIGHT, ROW_RADIUS, UiTheme.mix(theme.surface(), theme.surfaceHover(), hover), active ? theme.accent() : theme.border());
		int left = x + ROW_PADDING;
		ProfileBadge.draw(context, profile, left, y + (ROW_HEIGHT - BADGE) / 2, BADGE);
		int textX = left + BADGE + 10;

		// Right side: the actions, and Use for profiles that aren't active.
		int actionsWidth = ACTIONS.length * ACTION + (ACTIONS.length - 1) * ACTION_GAP;
		int actionsX = x + width - ROW_PADDING + 4 - actionsWidth;
		int actionY = y + (ROW_HEIGHT - ACTION) / 2;
		for (int i = 0; i < ACTIONS.length; i++) {
			int actionX = actionsX + i * (ACTION + ACTION_GAP);
			boolean actionHovered = contains(mouseX, mouseY, actionX, actionY, ACTION, ACTION);
			boolean off = i == 2 && profile.isDefault();
			int color = off ? UiTheme.fade(theme.muted(), 0.5F) : i == 2 && actionHovered ? theme.warning() : actionHovered ? theme.text() : theme.textSecondary();
			UiWidgets.ghostIconButton(context, theme, actionX, actionY, ACTION, ACTIONS[i], color, actionHovered && !off ? 1.0F : 0.0F);
			if (actionHovered) {
				showTooltip(Component.translatable(off ? EMUtilsTexts.UI_PROFILE_DEFAULT_CANT_DELETE : ACTION_TIPS[i]), mouseX, mouseY);
			}
		}
		int textRight = actionsX - 10;
		int useX = 0;
		int useWidth = 0;
		if (active) {
			Component badge = Component.translatable(EMUtilsTexts.UI_PROFILE_ACTIVE);
			int badgeWidth = UiText.width(font, badge, UiText.Size.SMALL) + 8;
			int badgeHeight = UiText.lineHeight(font, UiText.Size.SMALL) + 5;
			UiWidgets.badge(context, font, textRight - badgeWidth, y + (ROW_HEIGHT - badgeHeight) / 2, badge, UiTheme.fade(theme.accent(), 0.18F), theme.accent());
			textRight -= badgeWidth + 10;
		} else {
			Component use = Component.translatable(EMUtilsTexts.UI_PROFILE_USE);
			useWidth = UiWidgets.buttonWidth(font, use);
			useX = textRight - useWidth;
			boolean useHovered = contains(mouseX, mouseY, useX, y + (ROW_HEIGHT - USE_HEIGHT) / 2, useWidth, USE_HEIGHT);
			UiWidgets.button(context, font, theme, useX, y + (ROW_HEIGHT - USE_HEIGHT) / 2, useWidth, USE_HEIGHT, use, UiWidgets.ButtonStyle.TONAL, useHovered ? 1.0F : 0.0F);
			textRight = useX - 10;
		}

		// Left side: the name, and where it loads by itself below.
		int nameCenter = y + ROW_PADDING + 4;
		UiText.drawCentered(context, font, UiText.ellipsize(font, profile.name(), UiText.Size.BOLD, textRight - textX), UiText.Size.BOLD, textX, nameCenter, theme.text());
		Component rule = UiText.ellipsize(font, ProfileBadge.describe(profile), UiText.Size.BODY, textRight - textX);
		UiText.drawCentered(context, font, rule, UiText.Size.BODY, textX, y + ROW_HEIGHT - ROW_PADDING - 4, theme.muted());
		return new RowBox(profile, y, actionsX, actionY, useX, useWidth);
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
			openMenu(row.profile(), (int) mouseX, (int) mouseY);
			return true;
		}
		if (!left) {
			return super.mouseClicked(click, doubled);
		}
		if (contains(mouseX, mouseY, addX, headerButtonsY, addWidth, HEADER_BUTTON_HEIGHT)) {
			openSheet(null);
			return true;
		}
		if (contains(mouseX, mouseY, importX, headerButtonsY, importWidth, HEADER_BUTTON_HEIGHT)) {
			importFromClipboard();
			return true;
		}
		if (scroll.mouseClicked(mouseX, mouseY)) {
			return true;
		}
		if (row != null) {
			for (int i = 0; i < ACTIONS.length; i++) {
				if (contains(mouseX, mouseY, row.actionsX() + i * (ACTION + ACTION_GAP), row.actionY(), ACTION, ACTION)) {
					switch (i) {
						case 0 -> openSheet(row.profile());
						case 1 -> duplicate(row.profile());
						default -> confirmDelete(row.profile());
					}
					return true;
				}
			}
			if (row.useWidth() > 0 && contains(mouseX, mouseY, row.useX(), row.y() + (ROW_HEIGHT - USE_HEIGHT) / 2, row.useWidth(), USE_HEIGHT)) {
				use(row.profile());
				return true;
			}
			// Anywhere else on the row edits it.
			openSheet(row.profile());
			return true;
		}
		return super.mouseClicked(click, doubled);
	}

	private @Nullable RowBox rowAt(double mouseX, double mouseY) {
		if (!scroll.contains(mouseX, mouseY)) {
			return null;
		}
		for (RowBox row : rows) {
			if (mouseY >= row.y() && mouseY < row.y() + ROW_HEIGHT) {
				return row;
			}
		}
		return null;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
		if (sheet != null) {
			sheet.mouseDragged(click.y());
			return true;
		}
		if (!overlayOpen() && scroll.mouseDragged(click.y())) {
			return true;
		}
		return super.mouseDragged(click, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		if (sheet != null) {
			sheet.mouseReleased();
		}
		if (scroll.mouseReleased()) {
			return true;
		}
		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (sheet != null) {
			sheet.mouseScrolled(mouseX, mouseY, verticalAmount);
			return true;
		}
		if (closing() || dialog != null) {
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

	/**
	 * Opens the add sheet (null) or the edit sheet of the profile named {@code name}, filled in with
	 * these values unless {@code fillName} is null; used by UI snapshots.
	 */
	public void openSheetForSnapshot(@Nullable String name, @Nullable String fillName, String servers, boolean singleplayer) {
		Profile existing = name == null ? null : manager().byName(name);
		openSheet(existing);
		if (fillName != null && sheet != null) {
			sheet.fillForSnapshot(fillName, servers, singleplayer);
		}
	}

	/** Picks the first server in the open sheet's server list; used by UI snapshots. */
	public @Nullable String pickFirstServerForSnapshot() {
		return sheet == null ? null : sheet.pickFirstServerForSnapshot();
	}

	/** Whether the open sheet refuses its name, uses two columns, and where it ends; used by UI snapshots. */
	public boolean sheetNameTakenForSnapshot() {
		return sheet != null && sheet.nameTakenForSnapshot();
	}

	public boolean sheetWideForSnapshot() {
		return sheet != null && sheet.wideForSnapshot();
	}

	public int sheetBottomForSnapshot() {
		return sheet == null ? 0 : sheet.bottomForSnapshot();
	}

	/** How many servers the open server list shows; -1 when it isn't open. Used by UI snapshots. */
	public int pickerCountForSnapshot() {
		ServerPicker picker = sheet == null ? null : sheet.pickerForSnapshot();
		return picker == null ? -1 : picker.visibleCountForSnapshot();
	}

	public @Nullable String pickerFirstForSnapshot() {
		ServerPicker picker = sheet == null ? null : sheet.pickerForSnapshot();
		return picker == null ? null : picker.firstAddressForSnapshot();
	}

	public boolean pickerScrollToEndForSnapshot() {
		ServerPicker picker = sheet == null ? null : sheet.pickerForSnapshot();
		return picker != null && picker.scrollToEndForSnapshot();
	}

	public boolean pickerFitsForSnapshot() {
		ServerPicker picker = sheet == null ? null : sheet.pickerForSnapshot();
		return picker != null && picker.fitsForSnapshot(height);
	}

	/** Saves the open sheet; used by UI snapshots. */
	public void saveSheetForSnapshot() {
		if (sheet != null) {
			sheet.saveForSnapshot();
		}
	}

	/** Opens the right-click menu for the profile named {@code name}; used by UI snapshots. */
	public void openMenuForSnapshot(String name, int mouseX, int mouseY) {
		Profile profile = manager().byName(name);
		if (profile != null) {
			openMenu(profile, mouseX, mouseY);
		}
	}

	/** Asks to delete the profile named {@code name}; used by UI snapshots. */
	public void confirmDeleteForSnapshot(String name) {
		Profile profile = manager().byName(name);
		if (profile != null) {
			confirmDelete(profile);
		}
	}

	/** Whether a sheet, dialog or menu is open; used by UI snapshots. */
	public boolean overlayOpenForSnapshot() {
		return overlayOpen();
	}

	private record RowBox(Profile profile, int y, int actionsX, int actionY, int useX, int useWidth) {
	}
}
