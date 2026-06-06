package net.emutils.client.mixin;

import net.emutils.client.emutils.gui.hub.CustomHubScreen;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {
	private static final int EMUTILS_ROW = 5;

	@Shadow
	@Final
	private HeaderAndFooterLayout layout;

	protected OptionsScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void emutils$addSettingsButton(CallbackInfo ci) {
		GridLayout grid = emutils$findOptionsGrid(this.layout);
		if (grid == null) {
			return;
		}

		Button emutilsButton = Button.builder(Component.translatable(EMUtilsTexts.OPTIONS_BUTTON), button -> Minecraft.getInstance()
			.setScreen(new CustomHubScreen(this))).build();
		grid.addChild(emutilsButton, EMUTILS_ROW, 0, 1, 1, grid.defaultCellSetting());
		addRenderableWidget(emutilsButton);

		this.layout.arrangeElements();
	}

	private static GridLayout emutils$findOptionsGrid(HeaderAndFooterLayout layoutWidget) {
		GridLayout[] grid = new GridLayout[1];
		layoutWidget.visitChildren(widget -> {
			if (widget instanceof GridLayout found) {
				grid[0] = found;
			}
		});
		return grid[0];
	}
}
