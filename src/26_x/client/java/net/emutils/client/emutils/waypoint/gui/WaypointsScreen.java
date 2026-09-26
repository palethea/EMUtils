package net.emutils.client.emutils.waypoint.gui;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.gui.hub.HubIcons;
import net.emutils.client.emutils.gui.ui.UiConfirmDialog;
import net.emutils.client.emutils.gui.ui.UiIcons;
import net.emutils.client.emutils.gui.ui.UiPanelScreen;
import net.emutils.client.emutils.gui.ui.UiScrollArea;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.gui.ui.UiWidgets;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.emutils.client.emutils.waypoint.Waypoint;
import net.emutils.client.emutils.waypoint.WaypointType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * The waypoints of the current world and dimension (#103): each one with its color,
 * name, type, coordinates and distance, and buttons to copy the coordinates, hide it, toggle its
 * beacon or delete it. New waypoints are added in a sheet over the list.
 */
public final class WaypointsScreen extends UiPanelScreen {
	private static final int PADDING = 16;
	private static final int HEADER_BUTTON_HEIGHT = 20;
	private static final int ROW_HEIGHT = 44;
	private static final int ROW_GAP = 8;
	private static final int ROW_RADIUS = 9;
	private static final int ROW_PADDING = 12;
	private static final int ACTION = 20;
	private static final int ACTION_GAP = 2;
	private static final int FADE_HEIGHT = 12;

	private final UiScrollArea scroll = new UiScrollArea();
	private final List<RowBox> rows = new ArrayList<>();
	/** Opened by the Add Waypoint keybind: the screen closes together with the add sheet. */
	private final boolean addOnly;
	private @Nullable AddWaypointSheet sheet;
	private @Nullable UiConfirmDialog dialog;
	private int addX;
	private int addWidth;
	private int clearX;
	private int clearWidth;
	private int headerButtonsY;
	private @Nullable Component tooltip;
	private int tooltipX;
	private int tooltipY;

	public WaypointsScreen(@Nullable Screen parent) {
		this(parent, false);
	}

	private WaypointsScreen(@Nullable Screen parent, boolean addOnly) {
		super(Component.translatable(EMUtilsTexts.SCREEN_CURRENT_WAYPOINTS), parent);
		this.addOnly = addOnly;
	}

	/** Opens straight into the add sheet, for the Add Waypoint keybind; closing it closes the screen too. */
	public static WaypointsScreen addWaypoint(@Nullable Screen parent) {
		return new WaypointsScreen(parent, true);
	}

	@Override
	protected int maxPanelWidth() {
		return 560;
	}

	@Override
	protected void layout() {
		int titleHeight = UiText.lineHeight(font, UiText.Size.HEADING);
		int subtitleHeight = UiText.lineHeight(font, UiText.Size.BODY);
		int headerHeight = titleHeight + 6 + subtitleHeight;
		headerButtonsY = panelY + PADDING + (headerHeight - HEADER_BUTTON_HEIGHT) / 2;
		int bodyY = panelY + PADDING + headerHeight + 12;
		scroll.setBounds(panelX + PADDING, bodyY, panelWidth - PADDING * 2 + UiScrollArea.GUTTER, panelY + panelHeight - PADDING / 2 - bodyY);
		if (addOnly && sheet == null && !closing()) {
			openAddSheet();
		}
	}

	private List<Waypoint> waypoints() {
		return EMUtilsClient.waypoint().waypointsForCurrentWorld(minecraft);
	}

	private void openAddSheet() {
		sheet = new AddWaypointSheet(font, anim, added -> {
			if (addOnly) {
				onClose();
			}
		});
	}

	// ---- drawing --------------------------------------------------------------------------------

	@Override
	protected void drawPanel(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		tooltip = null;
		boolean interactive = sheet == null && dialog == null && !closing();
		int hoverX = interactive ? mouseX : Integer.MIN_VALUE / 2;
		int hoverY = interactive ? mouseY : Integer.MIN_VALUE / 2;
		List<Waypoint> waypoints = waypoints();
		drawHeader(context, theme, waypoints.size(), hoverX, hoverY);
		drawRows(context, theme, waypoints, hoverX, hoverY);
	}

	private void drawHeader(GuiGraphicsExtractor context, UiTheme theme, int count, int mouseX, int mouseY) {
		int left = panelX + PADDING;
		int top = panelY + PADDING;
		UiText.draw(context, font, title, UiText.Size.HEADING, left, top, theme.text());
		Component where = minecraft.level == null ? Component.empty() : dimensionName(minecraft.level.dimension().identifier().toString());
		Component countText = Component.translatable(count == 1 ? EMUtilsTexts.UI_WAYPOINT_COUNT_ONE : EMUtilsTexts.UI_WAYPOINT_COUNT, count);
		UiText.draw(context, font, where.copy().append(" · ").append(countText), UiText.Size.BODY, left, top + UiText.lineHeight(font, UiText.Size.HEADING) + 6, theme.muted());

		Component add = Component.translatable(EMUtilsTexts.UI_ADD_WAYPOINT);
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

	private void drawRows(GuiGraphicsExtractor context, UiTheme theme, List<Waypoint> waypoints, int mouseX, int mouseY) {
		scroll.setContentHeight(waypoints.isEmpty() ? 0 : waypoints.size() * (ROW_HEIGHT + ROW_GAP) - ROW_GAP + FADE_HEIGHT);
		scroll.animate(anim, "waypoints-scroll", mouseX, mouseY);
		rows.clear();
		boolean mouseInList = scroll.contains(mouseX, mouseY) && !scroll.dragging();
		scroll.begin(context);
		context.pose().pushMatrix();
		context.pose().translate(0.0F, scroll.offset() - scroll.exactOffset());
		int width = scroll.contentWidth();
		int y = scroll.y() + FADE_HEIGHT / 2 - scroll.offset();
		for (Waypoint waypoint : waypoints) {
			if (y + ROW_HEIGHT >= scroll.y() && y <= scroll.y() + scroll.height()) {
				rows.add(drawRow(context, theme, waypoint, scroll.x(), y, width, mouseInList ? mouseX : Integer.MIN_VALUE / 2, mouseY));
			}
			y += ROW_HEIGHT + ROW_GAP;
		}
		context.pose().popMatrix();
		if (waypoints.isEmpty()) {
			int centerX = scroll.x() + width / 2;
			int iconSize = 22;
			int top = scroll.y() + Math.max(20, scroll.height() / 2 - 30);
			UiIcons.draw(context, HubIcons.MAP_PIN, centerX - iconSize / 2, top, iconSize, theme.muted());
			Component empty = Component.translatable(EMUtilsTexts.WAYPOINT_NONE_WORLD);
			UiText.draw(context, font, empty, UiText.Size.BODY, centerX - UiText.width(font, empty, UiText.Size.BODY) / 2, top + iconSize + 10, theme.muted());
		}
		scroll.end(context, theme.panel(), FADE_HEIGHT, UiTheme.fade(theme.text(), 0.25F), UiTheme.fade(theme.text(), 0.45F));
	}

	private RowBox drawRow(GuiGraphicsExtractor context, UiTheme theme, Waypoint waypoint, int x, int y, int width, int mouseX, int mouseY) {
		boolean hovered = contains(mouseX, mouseY, x, y, width, ROW_HEIGHT);
		float hover = anim.towards("waypoint:" + waypoint.timestamp(), hovered, 16.0F);
		UiShapes.borderedRect(context, x, y, width, ROW_HEIGHT, ROW_RADIUS, UiTheme.mix(theme.surface(), theme.surfaceHover(), hover), theme.border());
		// Hidden waypoints are dimmed, so the ones shown in the world stand out.
		float shown = waypoint.hidden() ? 0.45F : 1.0F;
		int left = x + ROW_PADDING;
		int nameCenter = y + ROW_PADDING + 4;
		UiShapes.circle(context, left, nameCenter - 4, 9, UiTheme.fade(0xFF000000 | waypoint.color(), shown));
		int nameX = left + 15;

		// Right side: the distance, then the actions.
		Identifier[] icons = {
			HubIcons.COPY,
			waypoint.hidden() ? HubIcons.EYE_OFF : HubIcons.EYE,
			HubIcons.BEAM,
			HubIcons.TRASH
		};
		String[] tips = {
			EMUtilsTexts.UI_COPY_COORDINATES,
			waypoint.hidden() ? EMUtilsTexts.WAYPOINT_ACTION_SHOW : EMUtilsTexts.WAYPOINT_ACTION_HIDE,
			waypoint.beaconEnabled() ? EMUtilsTexts.UI_BEACON_TURN_OFF : EMUtilsTexts.UI_BEACON_TURN_ON,
			EMUtilsTexts.UI_DELETE
		};
		int actionsWidth = icons.length * ACTION + (icons.length - 1) * ACTION_GAP;
		int actionsX = x + width - ROW_PADDING + 4 - actionsWidth;
		int actionY = y + (ROW_HEIGHT - ACTION) / 2;
		for (int i = 0; i < icons.length; i++) {
			int actionX = actionsX + i * (ACTION + ACTION_GAP);
			boolean actionHovered = contains(mouseX, mouseY, actionX, actionY, ACTION, ACTION);
			int color = switch (i) {
				case 2 -> waypoint.beaconEnabled() ? theme.accent() : theme.muted();
				case 3 -> actionHovered ? theme.warning() : theme.textSecondary();
				default -> theme.textSecondary();
			};
			UiWidgets.ghostIconButton(context, theme, actionX, actionY, ACTION, icons[i], color, actionHovered ? 1.0F : 0.0F);
			if (actionHovered) {
				tooltip = Component.translatable(tips[i]);
				tooltipX = mouseX;
				tooltipY = mouseY;
			}
		}
		Component distance = minecraft.player == null ? Component.empty() : Component.translatable(EMUtilsTexts.UI_BLOCKS, EMUtilsClient.waypoint().distanceBlocks(minecraft, waypoint));
		int distanceWidth = UiText.width(font, distance, UiText.Size.LABEL);
		int distanceX = actionsX - 10 - distanceWidth;
		UiText.drawCentered(context, font, distance, UiText.Size.LABEL, distanceX, y + ROW_HEIGHT / 2, theme.muted());

		// Left side: name and type, coordinates below.
		Component type = Component.translatable(waypoint.type().labelKey());
		int badgeWidth = UiText.width(font, type, UiText.Size.SMALL) + 8;
		int textRight = distanceX - 10;
		Component name = UiText.ellipsize(font, Component.literal(waypoint.label()), UiText.Size.BOLD, textRight - nameX - badgeWidth - 6);
		UiText.drawCentered(context, font, name, UiText.Size.BOLD, nameX, nameCenter, UiTheme.fade(theme.text(), shown));
		int badgeX = nameX + UiText.width(font, name, UiText.Size.BOLD) + 6;
		int badgeY = nameCenter - (UiText.lineHeight(font, UiText.Size.SMALL) + 5) / 2;
		boolean death = waypoint.type() == WaypointType.DEATH;
		UiWidgets.badge(context, font, badgeX, badgeY, type, death ? UiTheme.fade(theme.warning(), 0.16F) : theme.segmentBackground(), death ? theme.warning() : theme.textSecondary());
		Component coords = Component.literal("X " + waypoint.x() + "   Y " + waypoint.y() + "   Z " + waypoint.z());
		UiText.drawCentered(context, font, coords, UiText.Size.BODY, nameX, y + ROW_HEIGHT - ROW_PADDING - 4, UiTheme.fade(theme.muted(), shown));
		return new RowBox(waypoint.timestamp(), actionsX, actionY);
	}

	@Override
	protected void drawOverlay(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		if (tooltip != null && sheet == null && dialog == null) {
			UiWidgets.tooltip(context, font, theme, tooltip, tooltipX, tooltipY, width, height);
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

	/** "Overworld", "Nether" or "End", or the dimension's id for other dimensions. */
	static Component dimensionName(String id) {
		String key = "emutils.dimension." + id.replace("minecraft:", "").replace(':', '.');
		return Language.getInstance().has(key) ? Component.translatable(key) : Component.literal(id);
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
		if (!left) {
			return super.mouseClicked(click, doubled);
		}
		if (contains(mouseX, mouseY, addX, headerButtonsY, addWidth, HEADER_BUTTON_HEIGHT)) {
			openAddSheet();
			return true;
		}
		int count = waypoints().size();
		if (count > 0 && contains(mouseX, mouseY, clearX, headerButtonsY, clearWidth, HEADER_BUTTON_HEIGHT)) {
			openClearDialog(() -> EMUtilsClient.waypoint().clearForCurrentWorld(minecraft));
			return true;
		}
		if (scroll.mouseClicked(mouseX, mouseY)) {
			return true;
		}
		if (scroll.contains(mouseX, mouseY)) {
			for (RowBox row : rows) {
				for (int i = 0; i < 4; i++) {
					if (contains(mouseX, mouseY, row.actionsX() + i * (ACTION + ACTION_GAP), row.actionY(), ACTION, ACTION)) {
						runAction(row.timestamp(), i);
						return true;
					}
				}
			}
		}
		return super.mouseClicked(click, doubled);
	}

	private void runAction(long timestamp, int action) {
		switch (action) {
			case 0 -> EMUtilsClient.waypoint().copyCoordinates(minecraft, timestamp);
			case 1 -> EMUtilsClient.waypoint().toggleHidden(timestamp);
			case 2 -> EMUtilsClient.waypoint().toggleBeacon(timestamp);
			default -> EMUtilsClient.waypoint().clear(minecraft, timestamp);
		}
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
		if (sheet != null) {
			sheet.mouseDragged(click.x(), click.y());
			return true;
		}
		if (scroll.mouseDragged(click.y())) {
			return true;
		}
		return super.mouseDragged(click, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		if (sheet != null) {
			sheet.mouseReleased();
			return true;
		}
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

	/** Opens the add sheet; used by UI snapshots. */
	public void openAddSheetForSnapshot() {
		openAddSheet();
	}

	/** Opens the clear-all confirmation without clearing on confirm; used by UI snapshots. */
	public void openClearDialogForSnapshot() {
		openClearDialog(() -> {
		});
	}

	/** Asks before clearing, since today's Clear Waypoints deleted every waypoint here in one click. */
	private void openClearDialog(Runnable clear) {
		int count = waypoints().size();
		dialog = new UiConfirmDialog(
			font,
			anim,
			Component.translatable(EMUtilsTexts.UI_CLEAR_WAYPOINTS_TITLE),
			Component.translatable(count == 1 ? EMUtilsTexts.UI_CLEAR_WAYPOINTS_MESSAGE_ONE : EMUtilsTexts.UI_CLEAR_WAYPOINTS_MESSAGE, count),
			Component.translatable(EMUtilsTexts.UI_CLEAR_ALL),
			clear
		);
	}

	private record RowBox(long timestamp, int actionsX, int actionY) {
	}
}
