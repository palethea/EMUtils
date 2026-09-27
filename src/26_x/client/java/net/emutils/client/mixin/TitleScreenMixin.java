package net.emutils.client.mixin;

import net.emutils.client.emutils.gui.settings.SettingsIconButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.FriendsButton;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The EMUtils icon on the title screen (#160), first in the row of small icon buttons, before friends,
 * language and accessibility, as in the pause menu.
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
	@Shadow
	private @Nullable FriendsButton friends;

	@Unique
	private @Nullable SpriteIconButton emutils$settingsIcon;

	@Unique
	private boolean emutils$rowPlaced;

	protected TitleScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void emutils$init(CallbackInfo ci) {
		emutils$settingsIcon = null;
		emutils$rowPlaced = false;
		if (friends == null) {
			return;
		}
		emutils$settingsIcon = addRenderableWidget(SettingsIconButton.create(this));
		emutils$settingsIcon.setPosition(friends.getX(), friends.getY());
	}

	/**
	 * Centers the row with the EMUtils icon in it on the first frame, rather than at the end of init, so
	 * icons other mods add to the row after the title screen's own init are placed along with it.
	 */
	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void emutils$placeRow(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (!emutils$rowPlaced && emutils$settingsIcon != null && friends != null) {
			emutils$rowPlaced = true;
			SettingsIconButton.recenterRow(this, children(), emutils$settingsIcon, friends.getY());
		}
	}
}
