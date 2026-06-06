package net.emutils.client.emutils.gui;

import net.emutils.client.EMUtilsClient;
import net.emhelpers.client.gui.widget.ConfigToggleButton;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.emutils.client.emutils.commandshortcuts.gui.CommandShortcutListScreen;
import net.emutils.client.emutils.compat.MinescriptCompat;
import net.emutils.client.emutils.minescript.gui.ScriptManagerScreen;
import net.emutils.client.emutils.packs.gui.PackManagerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.network.chat.Component;

public final class ManagerFeaturesSettingsScreen extends EMUtilsScreen {
	public ManagerFeaturesSettingsScreen(Screen parent) {
		super(parent, Component.translatable(EMUtilsTexts.SCREEN_MANAGERS));
	}

	@Override
	protected void initBody() {
		GridLayout.RowHelper adder = initTwoColumnBody();
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_PACK_MANAGER,
			() -> EMUtilsClient.config().packManagerEnabled(),
			EMUtilsClient.config()::setPackManagerEnabled
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_COMMAND_SHORTCUTS,
			() -> EMUtilsClient.config().commandShortcutsEnabled(),
			EMUtilsClient.config()::setCommandShortcutsEnabled
		));
		adder.addChild(fullWidthSettingsButton(
			Component.translatable(EMUtilsTexts.HUB_OPEN_PACK_MANAGER),
			button -> client.setScreen(new PackManagerScreen(this))
		), SETTINGS_COLUMNS);
		adder.addChild(fullWidthSettingsButton(
			Component.translatable(EMUtilsTexts.HUB_OPEN_COMMAND_SHORTCUTS),
			button -> client.setScreen(new CommandShortcutListScreen(this))
		), SETTINGS_COLUMNS);
		adder.addChild(scriptManagerButton(), SETTINGS_COLUMNS);
		adder.addChild(fullWidthSettingsButton(Component.translatable(EMUtilsTexts.OPTION_RESET_DEFAULTS), button -> {
			EMUtilsClient.config().resetManagerDefaults();
			client.setScreen(new ManagerFeaturesSettingsScreen(parent));
		}), SETTINGS_COLUMNS);
	}

	private Button scriptManagerButton() {
		Button button = fullWidthSettingsButton(
			Component.translatable(EMUtilsTexts.HUB_OPEN_SCRIPT_MANAGER),
			ignored -> client.setScreen(new ScriptManagerScreen(this))
		);
		button.active = MinescriptCompat.isLoaded();
		if (!button.active) {
			button.setTooltip(Tooltip.create(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_REQUIRES_MINESCRIPT)));
		}
		return button;
	}
}
