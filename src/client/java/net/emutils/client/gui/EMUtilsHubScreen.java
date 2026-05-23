package net.emutils.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.gui.chat.ChatFeaturesSettingsScreen;
import net.emutils.client.gui.death.DeathWaypointSettingsScreen;
import net.emutils.client.gui.hud.HudOverlaySettingsScreen;
import net.emutils.client.gui.reconnect.AutoReconnectSettingsScreen;
import net.emutils.client.gui.screenshot.ScreenshotSettingsScreen;
import net.emutils.client.util.EMUtilsTexts;
import net.minecraft.client.gui.screen.Screen;
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
		adder.add(navButton(EMUtilsTexts.HUB_DEATH_WAYPOINTS, () -> EMUtilsClient.config().deathWaypoint(), DeathWaypointSettingsScreen::new));
		adder.add(navButton(EMUtilsTexts.HUB_AUTO_RECONNECT, () -> EMUtilsClient.config().autoReconnect(), AutoReconnectSettingsScreen::new));
		adder.add(navButton(EMUtilsTexts.HUB_SCREENSHOT_HELPER, () -> EMUtilsClient.config().screenshotHelper(), ScreenshotSettingsScreen::new));
		adder.add(navButton(EMUtilsTexts.HUB_CHAT_FEATURES, this::chatFeaturesEnabled, ChatFeaturesSettingsScreen::new));
		adder.add(navButton(EMUtilsTexts.HUB_HUD_OVERLAY, () -> EMUtilsClient.config().hudOverlay(), HudOverlaySettingsScreen::new));
		layout.addBody(grid);
	}

	@Override
	public void tick() {
		super.tick();
		refreshNavButtons();
	}

	private ButtonWidget navButton(String labelKey, BooleanSupplier enabled, Function<Screen, Screen> screenFactory) {
		ButtonWidget button = ButtonWidget.builder(statusLabel(labelKey, enabled.getAsBoolean()), ignored -> client.setScreen(screenFactory.apply(this))).build();
		navButtons.add(new StatusNavButton(button, labelKey, enabled));
		return button;
	}

	private void refreshNavButtons() {
		for (StatusNavButton navButton : navButtons) {
			navButton.button.setMessage(statusLabel(navButton.labelKey, navButton.enabled.getAsBoolean()));
		}
	}

	private boolean chatFeaturesEnabled() {
		return EMUtilsClient.config().copyChat()
			|| EMUtilsClient.config().chatTimestamps()
			|| EMUtilsClient.config().smartChatFilters()
			|| EMUtilsClient.config().chatMentionAlerts();
	}

	private static Text statusLabel(String labelKey, boolean enabled) {
		return Text.empty()
			.append(Text.literal("● ").formatted(enabled ? Formatting.GREEN : Formatting.DARK_GRAY))
			.append(Text.translatable(labelKey));
	}

	private record StatusNavButton(ButtonWidget button, String labelKey, BooleanSupplier enabled) {
	}
}
