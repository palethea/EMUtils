package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.gui.hub.CustomHubScreen;
import net.emutils.client.emutils.spotify.gui.SpotifyPlayerOverlay;
import net.emutils.client.emutils.spotify.SpotifyTrackState;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
	private static final int BUTTON_WIDTH = 140;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_MARGIN = 8;

	@Unique
	private SpotifyPlayerOverlay emutils$spotifyOverlay;

	@Unique
	private Button emutils$hubButton;

	protected GameMenuScreenMixin(Component title) {
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

	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void emutils$renderSpotifyBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (emutils$spotifyOverlay == null || !SpotifyPlayerOverlay.shouldDisplay(EMUtilsClient.spotify().state())) {
			return;
		}

		SpotifyPlayerOverlay.renderBackground(context, width, height);
	}

	@Inject(method = "extractRenderState", at = @At("RETURN"))
	private void emutils$renderSpotifyContent(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
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

		emutils$hubButton = addRenderableWidget(emutils$createHubButton(BUTTON_MARGIN, BUTTON_MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT));
	}

	@Unique
	private Button emutils$createHubButton(int x, int y, int width, int height) {
		return Button.builder(
			Component.translatable(EMUtilsTexts.HUB_TITLE),
			open -> Minecraft.getInstance().setScreen(new CustomHubScreen(this))
		).bounds(x, y, width, height).build();
	}

	@Unique
	private void emutils$initSpotifyPlayer() {
		emutils$spotifyOverlay = null;
		if (!EMUtilsClient.config().spotifyPlayerEnabled()) {
			return;
		}

		emutils$spotifyOverlay = SpotifyPlayerOverlay.create(width, height, this::addRenderableWidget);
		emutils$spotifyOverlay.setVisible(SpotifyPlayerOverlay.shouldDisplay(EMUtilsClient.spotify().state()));
		EMUtilsClient.spotify().refreshSoon();
	}
}
