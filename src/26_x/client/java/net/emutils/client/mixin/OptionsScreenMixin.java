package net.emutils.client.mixin;

import java.util.function.Supplier;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.settings.SettingsScreen;
import net.emutils.client.emutils.packs.gui.PacksScreen;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.emutils.client.versioned.VersionedInput;
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
import org.spongepowered.asm.mixin.injection.ModifyArg;
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

	@Shadow
	@Final
	private static Component RESOURCEPACK;

	/**
	 * With the Pack Manager's "Replace Resource Packs Button" on (#114), the Resource Packs button opens
	 * the Pack Manager instead. Shift-click still opens Minecraft's own screen, for reordering packs.
	 * Decided on click, so changing the setting applies without reopening Options.
	 */
	@ModifyArg(
		method = "init",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/options/OptionsScreen;openScreenButton(Lnet/minecraft/network/chat/Component;Ljava/util/function/Supplier;)Lnet/minecraft/client/gui/components/Button;"
		),
		index = 1
	)
	private Supplier<Screen> emutils$openPackManager(Component message, Supplier<Screen> screen) {
		if (message != RESOURCEPACK) {
			return screen;
		}
		return () -> {
			EMUtilsConfig config = EMUtilsClient.config();
			Minecraft client = Minecraft.getInstance();
			if (config != null && config.packManagerEnabled() && config.packManagerReplaceResourcePacksButton() && !VersionedInput.isShiftDown(client)) {
				return new PacksScreen(this);
			}
			return screen.get();
		};
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void emutils$addSettingsButton(CallbackInfo ci) {
		GridLayout grid = emutils$findOptionsGrid(this.layout);
		if (grid == null) {
			return;
		}

		Button emutilsButton = Button.builder(Component.translatable(EMUtilsTexts.OPTIONS_BUTTON), button -> Minecraft.getInstance()
			.gui.setScreen(new SettingsScreen(this))).build();
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
