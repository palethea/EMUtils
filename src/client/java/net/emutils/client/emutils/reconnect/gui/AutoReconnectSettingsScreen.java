package net.emutils.client.emutils.reconnect.gui;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.EMUtilsScreen;
import net.emutils.client.emhelpers.gui.widget.ConfigToggleButton;
import net.emutils.client.emhelpers.gui.widget.IntConfigSlider;
import net.emutils.client.emhelpers.util.EMUtilsTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.text.Text;

public final class AutoReconnectSettingsScreen extends EMUtilsScreen {
	public AutoReconnectSettingsScreen(Screen parent) {
		super(parent, Text.translatable(EMUtilsTexts.SCREEN_AUTO_RECONNECT));
	}

	@Override
	protected void initBody() {
		GridWidget.Adder adder = initTwoColumnBody();
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_AUTO_RECONNECT,
			() -> EMUtilsClient.config().autoReconnect(),
			EMUtilsClient.config()::setAutoReconnect
		));
		adder.add(new IntConfigSlider(
			0,
			0,
			SETTINGS_BUTTON_WIDTH,
			20,
			EMUtilsTexts.OPTION_RETRY_DELAY,
			EMUtilsTexts.SUFFIX_SECONDS,
			EMUtilsConfig.RECONNECT_DELAY_MIN,
			EMUtilsConfig.RECONNECT_DELAY_MAX,
			() -> EMUtilsClient.config().reconnectDelaySeconds(),
			EMUtilsClient.config()::setReconnectDelaySeconds
		));
		IntConfigSlider maxTriesSlider = new IntConfigSlider(
			0,
			0,
			SETTINGS_BUTTON_WIDTH,
			20,
			EMUtilsTexts.OPTION_AUTO_RECONNECT_MAX_TRIES,
			"",
			EMUtilsConfig.RECONNECT_MAX_TRIES_MIN,
			EMUtilsConfig.RECONNECT_MAX_TRIES_MAX,
			() -> EMUtilsClient.config().autoReconnectMaxTries(),
			EMUtilsClient.config()::setAutoReconnectMaxTries
		);
		maxTriesSlider.active = !EMUtilsClient.config().autoReconnectUnlimitedTries();
		adder.add(maxTriesSlider);
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_AUTO_RECONNECT_UNLIMITED,
			() -> EMUtilsClient.config().autoReconnectUnlimitedTries(),
			enabled -> {
				EMUtilsClient.config().setAutoReconnectUnlimitedTries(enabled);
				maxTriesSlider.active = !enabled;
			}
		));
		adder.add(fullWidthSettingsButton(Text.translatable(EMUtilsTexts.OPTION_RESET_DEFAULTS), button -> {
			EMUtilsClient.config().resetAutoReconnectDefaults();
			client.setScreen(new AutoReconnectSettingsScreen(parent));
		}), SETTINGS_COLUMNS);
	}
}
