package net.emutils.client.emutils.gui;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emhelpers.gui.widget.ConfigToggleButton;
import net.emutils.client.emhelpers.util.EMUtilsTexts;
import net.emutils.client.emutils.commandshortcuts.gui.CommandShortcutListScreen;
import net.emutils.client.emutils.compat.MinescriptCompat;
import net.emutils.client.emutils.minescript.gui.ScriptManagerScreen;
import net.emutils.client.emutils.packs.gui.PackManagerScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.text.Text;

public final class ManagerFeaturesSettingsScreen extends EMUtilsScreen {
	public ManagerFeaturesSettingsScreen(Screen parent) {
		super(parent, Text.translatable(EMUtilsTexts.SCREEN_MANAGERS));
	}

	@Override
	protected void initBody() {
		GridWidget.Adder adder = initTwoColumnBody();
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_PACK_MANAGER,
			() -> EMUtilsClient.config().packManagerEnabled(),
			EMUtilsClient.config()::setPackManagerEnabled
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_COMMAND_SHORTCUTS,
			() -> EMUtilsClient.config().commandShortcutsEnabled(),
			EMUtilsClient.config()::setCommandShortcutsEnabled
		));
		adder.add(fullWidthSettingsButton(
			Text.translatable(EMUtilsTexts.HUB_OPEN_PACK_MANAGER),
			button -> client.setScreen(new PackManagerScreen(this))
		), SETTINGS_COLUMNS);
		adder.add(fullWidthSettingsButton(
			Text.translatable(EMUtilsTexts.HUB_OPEN_COMMAND_SHORTCUTS),
			button -> client.setScreen(new CommandShortcutListScreen(this))
		), SETTINGS_COLUMNS);
		adder.add(scriptManagerButton(), SETTINGS_COLUMNS);
		adder.add(fullWidthSettingsButton(Text.translatable(EMUtilsTexts.OPTION_RESET_DEFAULTS), button -> {
			EMUtilsClient.config().resetManagerDefaults();
			client.setScreen(new ManagerFeaturesSettingsScreen(parent));
		}), SETTINGS_COLUMNS);
	}

	private ButtonWidget scriptManagerButton() {
		ButtonWidget button = fullWidthSettingsButton(
			Text.translatable(EMUtilsTexts.HUB_OPEN_SCRIPT_MANAGER),
			ignored -> client.setScreen(new ScriptManagerScreen(this))
		);
		button.active = MinescriptCompat.isLoaded();
		if (!button.active) {
			button.setTooltip(Tooltip.of(Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_REQUIRES_MINESCRIPT)));
		}
		return button;
	}
}
