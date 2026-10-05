package net.emutils.client.emutils.gui.settings;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.gui.hub.HubFeature;
import net.emutils.client.emutils.gui.hub.HubFeatureCatalog;
import net.emutils.client.emutils.gui.hub.HubIcons;
import net.emutils.client.emutils.gui.ui.UiIcons;
import net.emutils.client.emutils.gui.ui.UiPanelScreen;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.gui.ui.UiWidgets;
import net.emutils.client.emutils.map.WorldMapScreen;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Every setting of the map in one place (#243): the minimap, the world map, the entity radar, caves and the
 * Nether, and how hard the map works to load, each an area in the sidebar with its settings, switch, tabs and
 * keybinds beside it, as its settings sheet shows them. Waypoints keep a card of their own, since they work
 * without the map.
 */
public final class MapSettingsScreen extends UiPanelScreen {
	/** The areas, by the id of the feature whose settings each shows. */
	public static final String MINIMAP = "minimap";
	public static final String WORLD_MAP = "world_map";
	public static final String ENTITY_RADAR = "entity_radar";
	public static final String CAVES = "map_caves";
	public static final String LOADING = "map_loading";
	private static final String[] AREAS = {MINIMAP, WORLD_MAP, ENTITY_RADAR, CAVES, LOADING};
	private static final int PADDING = 16;
	private static final int SIDEBAR_WIDTH = 150;
	private static final int ITEM_HEIGHT = 26;
	private static final int ITEM_GAP = 3;
	private static final int ITEM_ICON = 12;
	private static final int HEADER_BUTTON = 20;
	private static final int DOT = 6;

	private final KeybindCapture capture = new KeybindCapture();
	private final List<HubFeature> areas = new ArrayList<>();
	private int area;
	private @Nullable SettingsSheet sheet;
	private int headerBottom;
	private int sidebarX;
	private int sidebarY;
	private int paneX;
	private int paneWidth;
	private int openMapX;
	private int openMapWidth;
	private int headerButtonY;

	public MapSettingsScreen(@Nullable Screen parent) {
		this(parent, MINIMAP);
	}

	/** Opens on an area, by its feature id, such as {@link #WORLD_MAP}. */
	public MapSettingsScreen(@Nullable Screen parent, String openArea) {
		super(Component.translatable(EMUtilsTexts.HUB_MAP), parent);
		for (String id : AREAS) {
			for (HubFeature feature : HubFeatureCatalog.all()) {
				if (feature.id().equals(id)) {
					areas.add(feature);
				}
			}
		}
		for (int i = 0; i < areas.size(); i++) {
			if (areas.get(i).id().equals(openArea)) {
				area = i;
			}
		}
	}

	@Override
	protected int maxPanelWidth() {
		return 760;
	}

	@Override
	protected int maxPanelHeight() {
		return 470;
	}

	@Override
	protected void layout() {
		int headerHeight = UiText.lineHeight(font, UiText.Size.HEADING) + 6 + UiText.lineHeight(font, UiText.Size.BODY);
		headerButtonY = panelY + PADDING + (headerHeight - HEADER_BUTTON) / 2;
		headerBottom = panelY + PADDING + headerHeight + 10;
		sidebarX = panelX + PADDING;
		sidebarY = headerBottom + 4;
		paneX = sidebarX + SIDEBAR_WIDTH + 4;
		paneWidth = panelX + panelWidth - paneX;
	}

	@Override
	protected void beforeFrame() {
		capture.frame();
		if (sheet == null && !areas.isEmpty()) {
			sheet = new SettingsSheet(font, anim, areas.get(area), capture, true);
		}
	}

	@Override
	protected void drawPanel(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		boolean interactive = !closing();
		int hoverX = interactive ? mouseX : Integer.MIN_VALUE / 2;
		int hoverY = interactive ? mouseY : Integer.MIN_VALUE / 2;
		drawHeader(context, theme, hoverX, hoverY);
		drawSidebar(context, theme, hoverX, hoverY);
		// A line between the sidebar and the area, from under the header to the bottom.
		UiShapes.roundedRect(context, paneX - 2, headerBottom + 4, 1, panelY + panelHeight - PADDING - headerBottom - 4, 0, theme.line());
		if (sheet != null) {
			sheet.render(context, theme, hoverX, hoverY, paneX, headerBottom - PADDING + 2, paneWidth, panelY + panelHeight - headerBottom + PADDING - 2, width, height);
		}
	}

	private void drawHeader(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		int left = panelX + PADDING;
		int top = panelY + PADDING;
		UiText.draw(context, font, title, UiText.Size.HEADING, left, top, theme.text());
		int subtitleY = top + UiText.lineHeight(font, UiText.Size.HEADING) + 6;
		UiText.draw(context, font, Component.translatable(EMUtilsTexts.UI_MAP_SETTINGS_SUBTITLE), UiText.Size.BODY, left, subtitleY, theme.muted());

		openMapWidth = 0;
		if (canOpenWorldMap()) {
			Component open = Component.translatable(EMUtilsTexts.UI_MAP_SETTINGS_OPEN_WORLD_MAP);
			int iconSize = 10;
			openMapWidth = UiText.width(font, open, UiText.Size.LABEL) + iconSize + 4 + 20;
			openMapX = panelX + panelWidth - PADDING - openMapWidth;
			boolean hovered = contains(mouseX, mouseY, openMapX, headerButtonY, openMapWidth, HEADER_BUTTON);
			UiShapes.roundedRect(context, openMapX, headerButtonY, openMapWidth, HEADER_BUTTON, 8, UiTheme.mix(theme.surfaceAlt(), theme.segmentSelected(), hovered ? 1.0F : 0.0F));
			UiIcons.draw(context, HubIcons.GLOBE, openMapX + 10, headerButtonY + (HEADER_BUTTON - iconSize) / 2, iconSize, theme.text());
			UiText.drawCentered(context, font, open, UiText.Size.LABEL, openMapX + 10 + iconSize + 4, headerButtonY + HEADER_BUTTON / 2, theme.text());
		}
	}

	/** The world map can be opened from here: you're in a world and it's turned on. */
	private boolean canOpenWorldMap() {
		return minecraft.level != null && EMUtilsClient.config().worldMap();
	}

	private void drawSidebar(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		for (int i = 0; i < areas.size(); i++) {
			HubFeature feature = areas.get(i);
			int y = sidebarY + i * (ITEM_HEIGHT + ITEM_GAP);
			boolean hovered = contains(mouseX, mouseY, sidebarX, y, SIDEBAR_WIDTH - 8, ITEM_HEIGHT);
			float selected = anim.transition("map-area:" + i, i == area, 0.15F);
			float hover = anim.towards("map-area-hover:" + i, hovered, 16.0F);
			int background = UiTheme.mix(UiTheme.fade(theme.hover(), hover), theme.segmentSelected(), selected);
			UiShapes.roundedRect(context, sidebarX, y, SIDEBAR_WIDTH - 8, ITEM_HEIGHT, 8, background);
			int center = y + ITEM_HEIGHT / 2;
			int textColor = UiTheme.mix(theme.textSecondary(), theme.text(), Math.max(selected, hover));
			UiIcons.draw(context, feature.icon().texture(), sidebarX + 9, center - ITEM_ICON / 2, ITEM_ICON, textColor);
			int labelRight = sidebarX + SIDEBAR_WIDTH - 8 - 10 - DOT - 6;
			Component label = UiText.ellipsize(font, SettingsScreen.title(feature), UiText.Size.LABEL, labelRight - (sidebarX + 9 + ITEM_ICON + 7));
			UiText.drawCentered(context, font, label, UiText.Size.LABEL, sidebarX + 9 + ITEM_ICON + 7, center, textColor);
			// Whether the area is on: a dot in the accent color, or hollow when off or held back by Unfair Features.
			if (feature.toggle() != null) {
				boolean on = feature.toggle().getter().getAsBoolean() && !(feature.unfair() && !EMUtilsClient.config().unfairFeatures());
				int dotX = sidebarX + SIDEBAR_WIDTH - 8 - 10 - DOT;
				UiShapes.circle(context, dotX + DOT / 2, center, DOT / 2, on ? theme.accent() : UiTheme.fade(theme.muted(), 0.5F));
			}
		}
		// Unfair Features off (#213): said once, under the areas it holds back.
		if (!EMUtilsClient.config().unfairFeatures()) {
			int y = sidebarY + areas.size() * (ITEM_HEIGHT + ITEM_GAP) + 6;
			Component badge = Component.translatable(EMUtilsTexts.UI_UNFAIR_OFF);
			int badgeHeight = UiText.lineHeight(font, UiText.Size.SMALL) + 5;
			UiWidgets.badge(context, font, sidebarX + 2, y, badge, theme.surfaceAlt(), theme.textSecondary());
			y += badgeHeight + 4;
			for (Component line : UiText.wrap(font, Component.translatable(EMUtilsTexts.UI_MAP_SETTINGS_UNFAIR_NOTE), UiText.Size.SMALL, SIDEBAR_WIDTH - 12)) {
				UiText.draw(context, font, line, UiText.Size.SMALL, sidebarX + 2, y, theme.muted());
				y += UiText.lineHeight(font, UiText.Size.SMALL) + 2;
			}
		}
	}

	/** Shows an area, by its index in the sidebar. */
	private void select(int index) {
		if (index == area || index < 0 || index >= areas.size()) {
			return;
		}
		capture.cancel();
		area = index;
		sheet = new SettingsSheet(font, anim, areas.get(area), capture, true);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		if (closing()) {
			return true;
		}
		if (capture.mouseClicked(click.button())) {
			return true;
		}
		double mouseX = click.x();
		double mouseY = click.y();
		if (click.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			if (openMapWidth > 0 && contains(mouseX, mouseY, openMapX, headerButtonY, openMapWidth, HEADER_BUTTON)) {
				minecraft.gui.setScreen(null);
				WorldMapScreen.open(minecraft, null);
				return true;
			}
			for (int i = 0; i < areas.size(); i++) {
				if (contains(mouseX, mouseY, sidebarX, sidebarY + i * (ITEM_HEIGHT + ITEM_GAP), SIDEBAR_WIDTH - 8, ITEM_HEIGHT)) {
					select(i);
					return true;
				}
			}
		}
		if (sheet != null && sheet.mouseClicked(mouseX, mouseY, click.button())) {
			return true;
		}
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
		if (sheet != null && sheet.mouseDragged(click.x(), click.y())) {
			return true;
		}
		return super.mouseDragged(click, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		if (sheet != null) {
			sheet.mouseReleased();
		}
		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (sheet != null && mouseX >= paneX) {
			return sheet.mouseScrolled(mouseX, mouseY, verticalAmount);
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean keyPressed(KeyEvent input) {
		if (closing()) {
			return true;
		}
		if (capture.keyPressed(input)) {
			return true;
		}
		if (sheet != null && sheet.keyPressed(input)) {
			return true;
		}
		return super.keyPressed(input);
	}

	@Override
	public boolean charTyped(CharacterEvent input) {
		if (closing() || capture.swallowChar()) {
			return true;
		}
		if (sheet != null) {
			return sheet.charTyped(input);
		}
		return super.charTyped(input);
	}

	@Override
	public void onClose() {
		capture.cancel();
		super.onClose();
	}

	// ---- UI snapshots ---------------------------------------------------------------------------

	/** Shows an area, by its feature id; used by UI snapshots. */
	public void selectAreaForSnapshot(String id) {
		for (int i = 0; i < areas.size(); i++) {
			if (areas.get(i).id().equals(id)) {
				select(i);
			}
		}
	}

	/** Opens tab {@code index} of the area shown; used by UI snapshots. */
	public void selectSectionForSnapshot(int index) {
		if (sheet != null) {
			sheet.selectSection(index);
		}
	}

	/** Scrolls the area's rows to the middle, so both edges fade; used by UI snapshots. */
	public void scrollToMiddleForSnapshot() {
		if (sheet != null) {
			sheet.scrollToMiddleForSnapshot();
		}
	}

	/** The ids of the areas in the sidebar, in order; used by UI snapshots. */
	public List<String> areasForSnapshot() {
		List<String> ids = new ArrayList<>();
		for (HubFeature feature : areas) {
			ids.add(feature.id());
		}
		return ids;
	}
}
