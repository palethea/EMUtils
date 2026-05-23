package net.emutils.client.gui.reconnect;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.gui.EMUtilsScreen;
import net.emutils.client.gui.widget.ConfigToggleButton;
import net.emutils.client.gui.widget.IntConfigSlider;
import net.emutils.client.util.EMUtilsTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.text.Text;

public final class AutoReconnectSettingsScreen extends EMUtilsScreen {
	public AutoReconnectSettingsScreen(Screen parent) {
		super(parent, Text.translatable(EMUtilsTexts.SCREEN_AUTO_RECONNECT));
	}

	@Override
	protected void initBody() {
		DirectionalLayoutWidget body = createVerticalBody();
		body.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_AUTO_RECONNECT,
			() -> EMUtilsClient.config().autoReconnect(),
			EMUtilsClient.config()::setAutoReconnect
		));
		body.add(new IntConfigSlider(
			0,
			0,
			200,
			20,
			EMUtilsTexts.OPTION_RETRY_DELAY,
			EMUtilsTexts.SUFFIX_SECONDS,
			EMUtilsConfig.RECONNECT_DELAY_MIN,
			EMUtilsConfig.RECONNECT_DELAY_MAX,
			() -> EMUtilsClient.config().reconnectDelaySeconds(),
			EMUtilsClient.config()::setReconnectDelaySeconds
		));
		layout.addBody(body);
	}
}
