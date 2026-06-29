package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.render.LightLevelOverlayRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPlayNetworkHandlerMixin {
	@Inject(method = "handleContainerClose", at = @At("HEAD"))
	private void emutils$saveContainerCursorOnClose(ClientboundContainerClosePacket packet, CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();
		if (net.emutils.client.emutils.compat.MinecraftClientCompat.screen(client) instanceof AbstractContainerScreen<?>) {
			EMUtilsClient.inventoryTools().cursor().saveBeforeScreenChange(client);
		}
	}

	@Inject(method = "handleOpenScreen", at = @At("TAIL"))
	private void emutils$restoreContainerCursorOnOpen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
		EMUtilsClient.inventoryTools().cursor().tryRestoreAfterInit(Minecraft.getInstance());
	}

	@Inject(method = "handleBlockUpdate", at = @At("RETURN"))
	private void emutils$refreshLightOverlayAfterBlockUpdate(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
		LightLevelOverlayRenderer.onChunkChanged(packet.getPos().getX() >> 4, packet.getPos().getZ() >> 4);
	}

	@Inject(method = "handleChunkBlocksUpdate", at = @At("RETURN"))
	private void emutils$refreshLightOverlayAfterSectionUpdate(
		ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci
	) {
		packet.runUpdates((pos, state) ->
			LightLevelOverlayRenderer.onChunkChanged(pos.getX() >> 4, pos.getZ() >> 4));
	}

	@Inject(method = "handleLevelChunkWithLight", at = @At("RETURN"))
	private void emutils$refreshLightOverlayAfterChunkData(
		ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci
	) {
		LightLevelOverlayRenderer.onChunkChanged(packet.getX(), packet.getZ());
	}
}
