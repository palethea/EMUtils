package net.emutils.client.mixin;

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
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
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
			open -> Minecraft.getInstance().gui.setScreen(new SettingsScreen(this))
		).bounds(x, y, width, height).build();
	}

	@Unique
	private void emutils$initSpotifyPlayer() {
		emutils$spotifyOverlay = null;
		if (!EMUtilsClient.config().spotifyEnabled() || !EMUtilsClient.config().spotifyPlayerEnabled()) {
			return;
		}

		// The card stays below the menu's lowest button; our EMUtils button sits in the top corner.
		int menuBottom = 0;
		for (GuiEventListener child : children()) {
			if (child instanceof AbstractWidget widget && widget != emutils$hubButton) {
				menuBottom = Math.max(menuBottom, widget.getBottom());
			}
		}
		emutils$spotifyOverlay = SpotifyPlayerOverlay.create(width, height, menuBottom, this::addRenderableWidget);
		emutils$spotifyOverlay.setVisible(SpotifyPlayerOverlay.shouldDisplay(EMUtilsClient.spotify().state()));
		EMUtilsClient.spotify().refreshSoon();
	}
}
