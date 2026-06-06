package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPlayNetworkHandlerMixin {
	@Inject(method = "handleContainerClose", at = @At("HEAD"))
	private void emutils$saveContainerCursorOnClose(ClientboundContainerClosePacket packet, CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();
		if (client.screen instanceof AbstractContainerScreen<?>) {
			EMUtilsClient.inventoryTools().cursor().saveBeforeScreenChange(client);
		}
	}

	@Inject(method = "handleOpenScreen", at = @At("TAIL"))
	private void emutils$restoreContainerCursorOnOpen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
		EMUtilsClient.inventoryTools().cursor().tryRestoreAfterInit(Minecraft.getInstance());
	}
}
