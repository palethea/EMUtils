package net.emutils.client.emutils.capes.gui;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.capes.CapePreferredProvider;
import net.emutils.client.emutils.gui.EMUtilsScreen;
import net.emutils.client.emhelpers.gui.widget.ConfigToggleButton;
import net.emutils.client.emhelpers.util.EMUtilsTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.text.Text;

public final class CapesSettingsScreen extends EMUtilsScreen {
	public CapesSettingsScreen(Screen parent) {
		super(parent, Text.translatable(EMUtilsTexts.SCREEN_CAPES));
	}

	@Override
	protected void initBody() {
		GridWidget grid = new GridWidget();
		grid.setSpacing(4);
		grid.getMainPositioner().alignHorizontalCenter();
		GridWidget.Adder adder = grid.createAdder(SETTINGS_COLUMNS);
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_CUSTOM_CAPES,
			() -> EMUtilsClient.config().customCapes(),
			EMUtilsClient.config()::setCustomCapes
		), SETTINGS_COLUMNS);
		adder.add(preferredProviderButton(), SETTINGS_COLUMNS);
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_CAPE_OPTIFINE,
			() -> EMUtilsClient.config().capeOptifine(),
			EMUtilsClient.config()::setCapeOptifine
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_CAPE_LABYMOD,
			() -> EMUtilsClient.config().capeLabyMod(),
			EMUtilsClient.config()::setCapeLabyMod
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_CAPE_MINECRAFTCAPES,
			() -> EMUtilsClient.config().capeMinecraftCapes(),
			EMUtilsClient.config()::setCapeMinecraftCapes
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_CAPE_COSMETICA,
			() -> EMUtilsClient.config().capeCosmetica(),
			EMUtilsClient.config()::setCapeCosmetica
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_CAPE_CLOAKSPLUS,
			() -> EMUtilsClient.config().capeCloaksPlus(),
			EMUtilsClient.config()::setCapeCloaksPlus
		));
		adder.add(fullWidthSettingsButton(Text.translatable(EMUtilsTexts.OPTION_RESET_DEFAULTS), button -> {
			EMUtilsClient.config().resetCapesDefaults();
			client.setScreen(new CapesSettingsScreen(parent));
		}), SETTINGS_COLUMNS);
		layout.addBody(grid);
	}

	private ButtonWidget preferredProviderButton() {
		return fullWidthSettingsButton(preferredProviderMessage(), button -> {
			CapePreferredProvider next = EMUtilsClient.config().capePreferredProvider().next();
			EMUtilsClient.config().setCapePreferredProvider(next);
			button.setMessage(preferredProviderMessage());
		});
	}

	private static Text preferredProviderMessage() {
		CapePreferredProvider preferred = EMUtilsClient.config().capePreferredProvider();
		return Text.translatable(
			EMUtilsTexts.OPTION_VALUE,
			Text.translatable(EMUtilsTexts.OPTION_CAPE_PREFERRED_PROVIDER),
			Text.translatable(preferred.labelKey())
		);
	}
}
