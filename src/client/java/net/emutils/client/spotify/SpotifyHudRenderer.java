package net.emutils.client.spotify;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.gui.spotify.SpotifyPlayerOverlay;
import net.emutils.client.hud.HudOverlayPlacement;
import net.emutils.client.hud.layout.HudElementId;
import net.emutils.client.hud.layout.HudLayoutManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.Identifier;

public final class SpotifyHudRenderer {
	private static final Identifier ID = Identifier.of(EMUtilsClient.MOD_ID, "spotify_hud");

	private SpotifyHudRenderer() {
	}

	public static void register() {
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ID, (context, tickCounter) -> render(context));
	}

	private static void render(DrawContext context) {
		EMUtilsConfig config = EMUtilsClient.config();
		MinecraftClient client = MinecraftClient.getInstance();
		if (config == null || client == null || client.player == null || client.world == null) {
			return;
		}
		if (!config.spotifyHudOverlay() && !HudLayoutManager.isEditing()) {
			return;
		}
		if (client.currentScreen instanceof ChatScreen && config.spotifyHudAnchor().isBottom() && !HudLayoutManager.isEditing()) {
			return;
		}
		if (!EMUtilsClient.spotify().state().shouldDisplay() && !HudLayoutManager.isEditing()) {
			return;
		}
		if (EMUtilsClient.zoom() != null && EMUtilsClient.zoom().shouldHideHud()) {
			return;
		}
		if (HudLayoutManager.isEditing()) {
			return;
		}

		float scale = config.spotifyHudScale() / 100.0F;
		HudOverlayPlacement.PanelDimensions dimensions = HudOverlayPlacement.spotifyHudDimensions(config);
		HudOverlayPlacement.Position position = HudLayoutManager.resolve(
			HudElementId.SPOTIFY,
			config,
			context.getScaledWindowWidth(),
			context.getScaledWindowHeight(),
			dimensions,
			client
		);
		SpotifyPlayerOverlay.renderHud(
			context,
			position.x(),
			position.y(),
			EMUtilsClient.spotify().state(),
			config.spotifyHudBackgroundOpacity(),
			scale
		);
	}
}
