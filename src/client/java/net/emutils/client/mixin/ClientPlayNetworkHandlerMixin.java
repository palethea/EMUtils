package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
	@Inject(method = "onCloseScreen", at = @At("HEAD"))
	private void emutils$saveContainerCursorOnClose(CloseScreenS2CPacket packet, CallbackInfo ci) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.currentScreen instanceof HandledScreen<?>) {
			EMUtilsClient.inventoryTools().cursor().saveBeforeScreenChange(client);
		}
	}

	@Inject(method = "onOpenScreen", at = @At("TAIL"))
	private void emutils$restoreContainerCursorOnOpen(OpenScreenS2CPacket packet, CallbackInfo ci) {
		EMUtilsClient.inventoryTools().cursor().tryRestoreAfterInit(MinecraftClient.getInstance());
	}

	@Inject(method = "onPlayerList", at = @At("TAIL"))
	private void emutils$refreshSkyblockProfileOnPlayerList(PlayerListS2CPacket packet, CallbackInfo ci) {
		EMUtilsClient.storagePreview().onTabListUpdated(MinecraftClient.getInstance());
	}

	@Inject(method = "onPlayerListHeader", at = @At("TAIL"))
	private void emutils$refreshSkyblockProfileOnPlayerListHeader(PlayerListHeaderS2CPacket packet, CallbackInfo ci) {
		EMUtilsClient.storagePreview().onTabListUpdated(MinecraftClient.getInstance());
	}
}
