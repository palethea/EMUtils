package net.emutils.client.emutils.spotify;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.spotify.gui.SpotifyPlayerOverlay;
import net.emhelpers.client.hud.layout.HudElementId;
import net.emhelpers.client.hud.layout.HudLayoutManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.resources.Identifier;

public final class SpotifyHudRenderer {
	private static final Identifier ID = Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "spotify_hud");

	private SpotifyHudRenderer() {
	}

	public static void register() {
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ID, (context, tickCounter) -> render(context));
	}

	private static void render(GuiGraphicsExtractor context) {
		EMUtilsConfig config = EMUtilsClient.config();
		Minecraft client = Minecraft.getInstance();
		if (config == null || client == null || client.player == null || client.level == null) {
			return;
		}
		if (!config.spotifyHudOverlay() && !HudLayoutManager.isEditing()) {
			return;
		}
		if (client.screen instanceof ChatScreen && config.spotifyHudAnchor().isBottom() && !HudLayoutManager.isEditing()) {
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
			net.emutils.client.EMUtilsHudElements.SPOTIFY,
			config,
			context.guiWidth(),
			context.guiHeight(),
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
