package net.emutils.client.emutils.gui;

import net.emutils.client.emutils.gui.hub.CustomHubScreen;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public final class SettingsChooserScreen extends Screen {
	private final @Nullable Screen parent;

	public SettingsChooserScreen(@Nullable Screen parent) {
		super(Component.translatable(EMUtilsTexts.SCREEN_SETTINGS_CHOOSER));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int buttonWidth = 260;
		int buttonHeight = 20;
		int centerX = width / 2;
		int centerY = height / 2;

		addRenderableWidget(Button.builder(Component.translatable(EMUtilsTexts.SETTINGS_CHOOSER_EMUTILS), button ->
			minecraft.setScreen(new CustomHubScreen(this))
		).bounds(centerX - buttonWidth / 2, centerY - 10, buttonWidth, buttonHeight).build());

		addRenderableWidget(Button.builder(Component.translatable("gui.back"), button ->
			minecraft.setScreen(parent)
		).bounds(centerX - 100, height - 52, 200, buttonHeight).build());
	}
}
