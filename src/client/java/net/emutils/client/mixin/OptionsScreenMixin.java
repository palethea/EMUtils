package net.emutils.client.mixin;

import net.emutils.client.gui.hub.CustomHubScreen;
import net.emutils.client.gui.skyblock.EMSkyblockConfigScreenFactory;
import net.emutils.client.util.EMUtilsTexts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.client.gui.widget.LayoutWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.text.Text;
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
	private ThreePartsLayoutWidget layout;

	protected OptionsScreenMixin(Text title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void emutils$addSettingsButton(CallbackInfo ci) {
		GridWidget grid = emutils$findOptionsGrid(this.layout);
		if (grid == null) {
			return;
		}

		ButtonWidget emutilsButton = ButtonWidget.builder(Text.translatable(EMUtilsTexts.OPTIONS_BUTTON), button -> MinecraftClient.getInstance()
			.setScreen(new CustomHubScreen(this))).build();
		grid.add(emutilsButton, EMUTILS_ROW, 0, 1, 1, grid.getMainPositioner());
		addDrawableChild(emutilsButton);

		ButtonWidget skyblockButton = ButtonWidget.builder(Text.translatable(EMUtilsTexts.OPTIONS_SKYBLOCK_BUTTON), button -> MinecraftClient.getInstance()
			.setScreen(EMSkyblockConfigScreenFactory.create(this))).build();
		grid.add(skyblockButton, EMUTILS_ROW, 1, 1, 1, grid.getMainPositioner());
		addDrawableChild(skyblockButton);
		this.layout.refreshPositions();
	}

	private static GridWidget emutils$findOptionsGrid(LayoutWidget layoutWidget) {
		GridWidget[] grid = new GridWidget[1];
		layoutWidget.forEachElement(widget -> {
			if (widget instanceof GridWidget found) {
				grid[0] = found;
			}
		});
		return grid[0];
	}
}
