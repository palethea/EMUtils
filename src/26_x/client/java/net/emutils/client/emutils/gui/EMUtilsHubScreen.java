package net.emutils.client.emutils.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.compat.MinescriptCompat;
import net.emutils.client.emutils.gui.hub.CustomHubScreen;
import net.emutils.client.emutils.capes.gui.CapesSettingsScreen;
import net.emutils.client.emutils.chat.gui.ChatFeaturesSettingsScreen;
import net.emutils.client.emutils.food.gui.FoodHudSettingsScreen;
import net.emutils.client.emutils.waypoint.gui.WaypointSettingsScreen;
import net.emutils.client.emutils.hud.editor.HudOverlaySettingsScreen;
import net.emutils.client.emutils.inventory.gui.InventoryToolsSettingsScreen;
import net.emutils.client.emutils.reconnect.gui.AutoReconnectSettingsScreen;
import net.emutils.client.emutils.screenshot.gui.ScreenshotSettingsScreen;
import net.emutils.client.emutils.spotify.gui.SpotifyPlayerSettingsScreen;
import net.emutils.client.emutils.tweaks.gui.TweaksSettingsScreen;
import net.emutils.client.emutils.zoom.gui.ZoomSettingsScreen;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public final class EMUtilsHubScreen extends EMUtilsScreen {
	private final List<StatusNavButton> navButtons = new ArrayList<>();

	public EMUtilsHubScreen(Screen parent) {
		super(parent, Component.translatable(EMUtilsTexts.HUB_TITLE));
	}

	@Override
	protected void initBody() {
		navButtons.clear();
		GridLayout grid = new GridLayout();
		grid.defaultCellSetting().paddingHorizontal(4).paddingBottom(4).alignHorizontallyCenter();
		GridLayout.RowHelper adder = grid.createRowHelper(2);
		adder.addChild(fullWidthSettingsButton(Component.translatable(EMUtilsTexts.HUB_MODERN_OPEN), button -> client.setScreen(new CustomHubScreen(this))), SETTINGS_COLUMNS);
		adder.addChild(navButton(EMUtilsTexts.HUB_WAYPOINTS, () -> EMUtilsClient.config().waypointEnabled(), WaypointSettingsScreen::new));
		adder.addChild(navButton(EMUtilsTexts.HUB_AUTO_RECONNECT, () -> EMUtilsClient.config().autoReconnect(), AutoReconnectSettingsScreen::new));
		adder.addChild(navButton(EMUtilsTexts.HUB_SCREENSHOT_HELPER, () -> EMUtilsClient.config().screenshotHelper(), ScreenshotSettingsScreen::new));
		adder.addChild(navButton(EMUtilsTexts.HUB_CHAT_FEATURES, this::chatFeaturesEnabled, ChatFeaturesSettingsScreen::new));
		adder.addChild(navButton(EMUtilsTexts.HUB_MANAGERS, this::managerFeaturesEnabled, ManagerFeaturesSettingsScreen::new));
		adder.addChild(navButton(EMUtilsTexts.HUB_HUD_OVERLAY, () -> EMUtilsClient.config().hudOverlay(), HudOverlaySettingsScreen::new));
		adder.addChild(navButton(EMUtilsTexts.HUB_FOOD_HUD, () -> EMUtilsClient.config().foodHud(), FoodHudSettingsScreen::new));
		adder.addChild(navButton(EMUtilsTexts.HUB_ZOOM, () -> EMUtilsClient.config().zoomEnabled(), ZoomSettingsScreen::new));
		adder.addChild(navButton(EMUtilsTexts.HUB_TWEAKS, () -> EMUtilsClient.config().tweaksEnabled(), TweaksSettingsScreen::new));
		adder.addChild(navButton(EMUtilsTexts.HUB_CAPES, () -> EMUtilsClient.config().capesEnabled(), CapesSettingsScreen::new));
		adder.addChild(navButton(EMUtilsTexts.HUB_INVENTORY_TOOLS, () -> EMUtilsClient.config().inventoryToolsEnabled(), InventoryToolsSettingsScreen::new));
		adder.addChild(navButton(
			EMUtilsTexts.HUB_SPOTIFY_PLAYER,
			() -> EMUtilsClient.config().spotifyPlayerEnabled() || EMUtilsClient.config().spotifyHudOverlay(),
			SpotifyPlayerSettingsScreen::new
		));
		layout.addToContents(grid);
	}

	@Override
	public void tick() {
		super.tick();
		refreshNavButtons();
	}

	private Button navButton(String labelKey, BooleanSupplier enabled, Function<Screen, Screen> screenFactory) {
		Button button = Button.builder(statusLabel(labelKey, enabled.getAsBoolean()), ignored -> client.setScreen(screenFactory.apply(this))).build();
		navButtons.add(new StatusNavButton(button, labelKey, enabled, () -> true));
		return button;
	}

	private void refreshNavButtons() {
		for (StatusNavButton navButton : navButtons) {
			navButton.button.setMessage(statusLabel(navButton.labelKey, navButton.enabled.getAsBoolean()));
			navButton.button.active = navButton.available.getAsBoolean();
		}
	}

	private boolean chatFeaturesEnabled() {
		return EMUtilsClient.config().copyChat()
			|| EMUtilsClient.config().chatTimestamps()
			|| EMUtilsClient.config().smartChatFilters()
			|| EMUtilsClient.config().chatMentionAlerts()
			|| EMUtilsClient.config().chatMentionHighlight();
	}

	private boolean managerFeaturesEnabled() {
		return EMUtilsClient.config().packManagerEnabled()
			|| EMUtilsClient.config().commandShortcutsEnabled()
			|| MinescriptCompat.isLoaded();
	}

	private static Component statusLabel(String labelKey, boolean enabled) {
		return Component.empty()
			.append(Component.literal("● ").withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY))
			.append(Component.translatable(labelKey));
	}

	private record StatusNavButton(Button button, String labelKey, BooleanSupplier enabled, BooleanSupplier available) {
	}
}
