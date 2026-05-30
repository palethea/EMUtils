package net.emutils.client.emutils.spotify;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.spotify.gui.SpotifyPlayerOverlay;
import net.emutils.client.emhelpers.hud.layout.HudElementId;
import net.emutils.client.emhelpers.hud.layout.HudLayoutManager;
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

		HudLayoutManager.ResolvedLayout layout = HudLayoutManager.resolveLayout(
			HudElementId.SPOTIFY,
			config,
			context.getScaledWindowWidth(),
			context.getScaledWindowHeight(),
			client
		);
		SpotifyPlayerOverlay.renderHud(
			context,
			layout.position().x(),
			layout.position().y(),
			EMUtilsClient.spotify().state(),
			layout.opacityPercent(),
			layout.scaleFactor()
		);
	}
}
