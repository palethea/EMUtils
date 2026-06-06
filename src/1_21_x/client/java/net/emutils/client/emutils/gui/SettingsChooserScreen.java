package net.emutils.client.emutils.gui;

import net.emutils.client.emutils.gui.hub.CustomHubScreen;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

public final class SettingsChooserScreen extends Screen {
	private final @Nullable Screen parent;

	public SettingsChooserScreen(@Nullable Screen parent) {
		super(Text.translatable(EMUtilsTexts.SCREEN_SETTINGS_CHOOSER));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int buttonWidth = 260;
		int buttonHeight = 20;
		int centerX = width / 2;
		int centerY = height / 2;

		addDrawableChild(ButtonWidget.builder(Text.translatable(EMUtilsTexts.SETTINGS_CHOOSER_EMUTILS), button ->
			client.setScreen(new CustomHubScreen(this))
		).dimensions(centerX - buttonWidth / 2, centerY - 10, buttonWidth, buttonHeight).build());

		addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"), button ->
			client.setScreen(parent)
		).dimensions(centerX - 100, height - 52, 200, buttonHeight).build());
	}
}
