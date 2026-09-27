package net.emutils.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.gui.settings.SettingsScreen;
import net.emutils.client.emutils.spotify.gui.SpotifyPlayerOverlay;
import net.emutils.client.emutils.spotify.SpotifyTrackState;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
	@Unique
	private static final Identifier EMUTILS$ICON = Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "pause_menu/emutils");

	@Unique
	private SpotifyPlayerOverlay emutils$spotifyOverlay;

	protected GameMenuScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void emutils$init(CallbackInfo ci) {
		emutils$initSpotifyPlayer();
	}

	/**
	 * Opens the EMUtils settings from the row of small icon buttons, as its first icon, left of the bug
	 * report one (#160). The row is laid out and its buttons added to the screen by the menu itself.
	 */
	@Inject(
		method = "createPauseMenu",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;",
			ordinal = 0
		)
	)
	private void emutils$addSettingsIcon(CallbackInfo ci, @Local LinearLayout iconButtonRow) {
		iconButtonRow.addChild(SpriteIconButton.builder(
			Component.translatable(EMUtilsTexts.HUB_TITLE),
			button -> Minecraft.getInstance().gui.setScreen(new SettingsScreen(this)),
			true
		).width(20).sprite(EMUTILS$ICON, 15, 15).withTootip().build());
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void emutils$tick(CallbackInfo ci) {
		if (emutils$spotifyOverlay != null) {
			SpotifyTrackState state = EMUtilsClient.spotify().state();
			emutils$spotifyOverlay.setVisible(SpotifyPlayerOverlay.shouldDisplay(state));
			emutils$spotifyOverlay.syncPlaybackState(state);
		}
	}

	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void emutils$renderSpotifyBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (emutils$spotifyOverlay == null || !SpotifyPlayerOverlay.shouldDisplay(EMUtilsClient.spotify().state())) {
			return;
		}

		emutils$spotifyOverlay.renderBackground(context);
	}

	@Inject(method = "extractRenderState", at = @At("RETURN"))
	private void emutils$renderSpotifyContent(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (emutils$spotifyOverlay == null || !SpotifyPlayerOverlay.shouldDisplay(EMUtilsClient.spotify().state())) {
			return;
		}

		emutils$spotifyOverlay.renderContent(context, EMUtilsClient.spotify().state());
	}

	@Unique
	private void emutils$initSpotifyPlayer() {
		emutils$spotifyOverlay = null;
		if (!EMUtilsClient.config().spotifyEnabled() || !EMUtilsClient.config().spotifyPlayerEnabled()) {
			return;
		}

		// The card stays below the menu's lowest button.
		int menuBottom = 0;
		for (GuiEventListener child : children()) {
			if (child instanceof AbstractWidget widget) {
				menuBottom = Math.max(menuBottom, widget.getBottom());
			}
		}
		emutils$spotifyOverlay = SpotifyPlayerOverlay.create(width, height, menuBottom, this::addRenderableWidget);
		emutils$spotifyOverlay.setVisible(SpotifyPlayerOverlay.shouldDisplay(EMUtilsClient.spotify().state()));
		EMUtilsClient.spotify().refreshSoon();
	}
}
