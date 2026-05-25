package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.gui.spotify.SpotifyPlayerOverlay;
import net.emutils.client.spotify.SpotifyTrackState;
import net.emutils.client.util.EMUtilsTexts;
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
	private ButtonWidget emutils$clearWaypointsButton;

	@Unique
	private SpotifyPlayerOverlay emutils$spotifyOverlay;

	protected GameMenuScreenMixin(Text title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void emutils$init(CallbackInfo ci) {
		emutils$initClearWaypointsButton();
		emutils$initSpotifyPlayer();
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void emutils$tick(CallbackInfo ci) {
		if (emutils$clearWaypointsButton != null) {
			emutils$clearWaypointsButton.active = EMUtilsClient.deathWaypoint()
				.hasWaypointForCurrentWorld(MinecraftClient.getInstance());
		}

		if (emutils$spotifyOverlay != null) {
			SpotifyTrackState state = EMUtilsClient.spotify().state();
			emutils$spotifyOverlay.setVisible(SpotifyPlayerOverlay.shouldDisplay(state));
			emutils$spotifyOverlay.syncPlaybackState(state);
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
	private void emutils$initClearWaypointsButton() {
		if (!EMUtilsClient.config().deathWaypoint()) {
			emutils$clearWaypointsButton = null;
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		emutils$clearWaypointsButton = ButtonWidget.builder(Text.translatable(EMUtilsTexts.OPTION_CLEAR_WAYPOINTS), button -> {
			EMUtilsClient.deathWaypoint().clearForCurrentWorld(client);
			button.active = false;
		}).dimensions(BUTTON_MARGIN, BUTTON_MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT).build();
		emutils$clearWaypointsButton.active = EMUtilsClient.deathWaypoint().hasWaypointForCurrentWorld(client);
		addDrawableChild(emutils$clearWaypointsButton);
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
