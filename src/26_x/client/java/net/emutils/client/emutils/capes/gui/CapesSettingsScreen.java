package net.emutils.client.emutils.capes.gui;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.capes.CapePreferredProvider;
import net.emutils.client.emutils.gui.EMUtilsScreen;
import net.emhelpers.client.gui.widget.ConfigToggleButton;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.network.chat.Component;

public final class CapesSettingsScreen extends EMUtilsScreen {
	public CapesSettingsScreen(Screen parent) {
		super(parent, Component.translatable(EMUtilsTexts.SCREEN_CAPES));
	}

	@Override
	protected void initBody() {
		GridLayout grid = new GridLayout();
		grid.spacing(4);
		grid.defaultCellSetting().alignHorizontallyCenter();
		GridLayout.RowHelper adder = grid.createRowHelper(SETTINGS_COLUMNS);
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_CUSTOM_CAPES,
			() -> EMUtilsClient.config().customCapes(),
			EMUtilsClient.config()::setCustomCapes
		), SETTINGS_COLUMNS);
		adder.addChild(preferredProviderButton(), SETTINGS_COLUMNS);
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_CAPE_OPTIFINE,
			() -> EMUtilsClient.config().capeOptifine(),
			EMUtilsClient.config()::setCapeOptifine
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_CAPE_LABYMOD,
			() -> EMUtilsClient.config().capeLabyMod(),
			EMUtilsClient.config()::setCapeLabyMod
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_CAPE_MINECRAFTCAPES,
			() -> EMUtilsClient.config().capeMinecraftCapes(),
			EMUtilsClient.config()::setCapeMinecraftCapes
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_CAPE_COSMETICA,
			() -> EMUtilsClient.config().capeCosmetica(),
			EMUtilsClient.config()::setCapeCosmetica
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_CAPE_CLOAKSPLUS,
			() -> EMUtilsClient.config().capeCloaksPlus(),
			EMUtilsClient.config()::setCapeCloaksPlus
		));
		adder.addChild(fullWidthSettingsButton(Component.translatable(EMUtilsTexts.OPTION_RESET_DEFAULTS), button -> {
			EMUtilsClient.config().resetCapesDefaults();
			client.setScreenAndShow(new CapesSettingsScreen(parent));
		}), SETTINGS_COLUMNS);
		layout.addToContents(grid);
	}

	private Button preferredProviderButton() {
		return fullWidthSettingsButton(preferredProviderMessage(), button -> {
			CapePreferredProvider next = EMUtilsClient.config().capePreferredProvider().next();
			EMUtilsClient.config().setCapePreferredProvider(next);
			button.setMessage(preferredProviderMessage());
		});
	}

	private static Component preferredProviderMessage() {
		CapePreferredProvider preferred = EMUtilsClient.config().capePreferredProvider();
		return Component.translatable(
			EMUtilsTexts.OPTION_VALUE,
			Component.translatable(EMUtilsTexts.OPTION_CAPE_PREFERRED_PROVIDER),
			Component.translatable(preferred.labelKey())
		);
	}
}
