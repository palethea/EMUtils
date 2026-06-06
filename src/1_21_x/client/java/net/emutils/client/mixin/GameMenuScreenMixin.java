package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.gui.hub.CustomHubScreen;
import net.emutils.client.emutils.spotify.gui.SpotifyPlayerOverlay;
import net.emutils.client.emutils.spotify.SpotifyTrackState;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
	private static final int BUTTON_WIDTH = 140;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_MARGIN = 8;

	@Unique
	private SpotifyPlayerOverlay emutils$spotifyOverlay;

	@Unique
	private ButtonWidget emutils$hubButton;

	protected GameMenuScreenMixin(Text title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void emutils$init(CallbackInfo ci) {
		emutils$hubButton = null;
		emutils$layoutGameMenuButtons();
		emutils$initSpotifyPlayer();
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void emutils$tick(CallbackInfo ci) {
		if (emutils$spotifyOverlay != null) {
			SpotifyTrackState state = EMUtilsClient.spotify().state();
			emutils$spotifyOverlay.setVisible(SpotifyPlayerOverlay.shouldDisplay(state));
			emutils$spotifyOverlay.syncPlaybackState(state);
		}

		if (emutils$hubButton == null) {
			emutils$layoutGameMenuButtons();
		}
	}

	@Inject(method = "render", at = @At("HEAD"))
	private void emutils$renderSpotifyBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (emutils$spotifyOverlay == null || !SpotifyPlayerOverlay.shouldDisplay(EMUtilsClient.spotify().state())) {
			return;
		}

		SpotifyPlayerOverlay.renderBackground(context, width, height);
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void emutils$renderSpotifyContent(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (emutils$spotifyOverlay == null || !SpotifyPlayerOverlay.shouldDisplay(EMUtilsClient.spotify().state())) {
			return;
		}

		SpotifyPlayerOverlay.renderContent(context, width, height, EMUtilsClient.spotify().state());
	}

	@Unique
	private void emutils$layoutGameMenuButtons() {
		if (emutils$hubButton != null) {
			return;
		}

		emutils$hubButton = addDrawableChild(emutils$createHubButton(BUTTON_MARGIN, BUTTON_MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT));
	}

	@Unique
	private ButtonWidget emutils$createHubButton(int x, int y, int width, int height) {
		return ButtonWidget.builder(
			Text.translatable(EMUtilsTexts.HUB_TITLE),
			open -> MinecraftClient.getInstance().setScreen(new CustomHubScreen(this))
		).dimensions(x, y, width, height).build();
	}

	@Unique
	private void emutils$initSpotifyPlayer() {
		emutils$spotifyOverlay = null;
		if (!EMUtilsClient.config().spotifyPlayerEnabled()) {
			return;
		}

		emutils$spotifyOverlay = SpotifyPlayerOverlay.create(width, height, this::addDrawableChild);
		emutils$spotifyOverlay.setVisible(SpotifyPlayerOverlay.shouldDisplay(EMUtilsClient.spotify().state()));
		EMUtilsClient.spotify().refreshSoon();
	}
}
