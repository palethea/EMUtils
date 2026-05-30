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
import net.emutils.client.emutils.waypoint.gui.WaypointSettingsScreen;
import net.emutils.client.emhelpers.hud.editor.HudOverlaySettingsScreen;
import net.emutils.client.emutils.inventory.gui.InventoryToolsSettingsScreen;
import net.emutils.client.emutils.minescript.gui.ScriptManagerScreen;
import net.emutils.client.emutils.packs.gui.PackManagerScreen;
import net.emutils.client.emutils.reconnect.gui.AutoReconnectSettingsScreen;
import net.emutils.client.emutils.screenshot.gui.ScreenshotSettingsScreen;
import net.emutils.client.emutils.spotify.gui.SpotifyPlayerSettingsScreen;
import net.emutils.client.emutils.tweaks.gui.TweaksSettingsScreen;
import net.emutils.client.emutils.zoom.gui.ZoomSettingsScreen;
import net.emutils.client.emhelpers.util.EMUtilsTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class EMUtilsHubScreen extends EMUtilsScreen {
	private final List<StatusNavButton> navButtons = new ArrayList<>();

	public EMUtilsHubScreen(Screen parent) {
		super(parent, Text.translatable(EMUtilsTexts.HUB_TITLE));
	}

	@Override
	protected void initBody() {
		navButtons.clear();
		GridWidget grid = new GridWidget();
		grid.getMainPositioner().marginX(4).marginBottom(4).alignHorizontalCenter();
		GridWidget.Adder adder = grid.createAdder(2);
		adder.add(fullWidthSettingsButton(Text.translatable(EMUtilsTexts.HUB_MODERN_OPEN), button -> client.setScreen(new CustomHubScreen(this))), SETTINGS_COLUMNS);
		adder.add(navButton(EMUtilsTexts.HUB_WAYPOINTS, () -> EMUtilsClient.config().waypointEnabled(), WaypointSettingsScreen::new));
		adder.add(navButton(EMUtilsTexts.HUB_AUTO_RECONNECT, () -> EMUtilsClient.config().autoReconnect(), AutoReconnectSettingsScreen::new));
		adder.add(navButton(EMUtilsTexts.HUB_SCREENSHOT_HELPER, () -> EMUtilsClient.config().screenshotHelper(), ScreenshotSettingsScreen::new));
		adder.add(navButton(EMUtilsTexts.HUB_CHAT_FEATURES, this::chatFeaturesEnabled, ChatFeaturesSettingsScreen::new));
		adder.add(navButton(EMUtilsTexts.HUB_HUD_OVERLAY, () -> EMUtilsClient.config().hudOverlay(), HudOverlaySettingsScreen::new));
		adder.add(navButton(EMUtilsTexts.HUB_ZOOM, () -> EMUtilsClient.config().zoomEnabled(), ZoomSettingsScreen::new));
		adder.add(navButton(EMUtilsTexts.HUB_TWEAKS, () -> EMUtilsClient.config().tweaksEnabled(), TweaksSettingsScreen::new));
		adder.add(navButton(EMUtilsTexts.HUB_PACK_MANAGER, () -> EMUtilsClient.config().packManagerEnabled(), PackManagerScreen::new));
		adder.add(navButton(EMUtilsTexts.HUB_CAPES, () -> EMUtilsClient.config().capesEnabled(), CapesSettingsScreen::new));
		adder.add(navButton(EMUtilsTexts.HUB_INVENTORY_TOOLS, () -> EMUtilsClient.config().inventoryToolsEnabled(), InventoryToolsSettingsScreen::new));
		adder.add(optionalNavButton(
			EMUtilsTexts.HUB_SCRIPT_MANAGER,
			MinescriptCompat::isLoaded,
			MinescriptCompat::isLoaded,
			ScriptManagerScreen::new,
			EMUtilsTexts.SCRIPT_MANAGER_REQUIRES_MINESCRIPT
		));
		adder.add(navButton(
			EMUtilsTexts.HUB_SPOTIFY_PLAYER,
			() -> EMUtilsClient.config().spotifyPlayerEnabled() || EMUtilsClient.config().spotifyHudOverlay(),
			SpotifyPlayerSettingsScreen::new
		));
		layout.addBody(grid);
	}

	@Override
	public void tick() {
		super.tick();
		refreshNavButtons();
	}

	private ButtonWidget navButton(String labelKey, BooleanSupplier enabled, Function<Screen, Screen> screenFactory) {
		ButtonWidget button = ButtonWidget.builder(statusLabel(labelKey, enabled.getAsBoolean()), ignored -> client.setScreen(screenFactory.apply(this))).build();
		navButtons.add(new StatusNavButton(button, labelKey, enabled, () -> true));
		return button;
	}

	private ButtonWidget optionalNavButton(
		String labelKey,
		BooleanSupplier available,
		BooleanSupplier enabled,
		Function<Screen, Screen> screenFactory,
		String disabledTooltipKey
	) {
		ButtonWidget button = ButtonWidget.builder(statusLabel(labelKey, available.getAsBoolean() && enabled.getAsBoolean()), ignored -> {
			if (available.getAsBoolean()) {
				client.setScreen(screenFactory.apply(this));
			}
		}).build();
		button.active = available.getAsBoolean();
		button.setTooltip(Tooltip.of(Text.translatable(disabledTooltipKey)));
		navButtons.add(new StatusNavButton(button, labelKey, () -> available.getAsBoolean() && enabled.getAsBoolean(), available));
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

	private static Text statusLabel(String labelKey, boolean enabled) {
		return Text.empty()
			.append(Text.literal("● ").formatted(enabled ? Formatting.GREEN : Formatting.DARK_GRAY))
			.append(Text.translatable(labelKey));
	}

	private record StatusNavButton(ButtonWidget button, String labelKey, BooleanSupplier enabled, BooleanSupplier available) {
	}
}
