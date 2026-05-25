package net.emutils.client.gui.skyblock;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.gui.EMUtilsScreen;
import net.emutils.client.gui.widget.ConfigToggleButton;
import net.emutils.client.util.EMUtilsTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.text.Text;

public final class SkyblockSettingsScreen extends EMUtilsScreen {
	public SkyblockSettingsScreen(Screen parent) {
		super(parent, Text.translatable(EMUtilsTexts.SCREEN_SKYBLOCK));
	}

	@Override
	protected void initBody() {
		GridWidget.Adder adder = initTwoColumnBody();
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SKYBLOCK,
			() -> EMUtilsClient.config().skyblockEnabled(),
			EMUtilsClient.config()::setSkyblockEnabled
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_STORAGE_PREVIEW,
			() -> EMUtilsClient.config().storagePreviewEnabled(),
			EMUtilsClient.config()::setStoragePreviewEnabled
		));
		adder.add(fullWidthSettingsButton(Text.translatable(EMUtilsTexts.OPTION_RESET_DEFAULTS), button -> {
			EMUtilsClient.config().resetSkyblockDefaults();
			client.setScreen(new SkyblockSettingsScreen(parent));
		}), SETTINGS_COLUMNS);
	}
}
