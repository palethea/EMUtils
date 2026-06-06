package net.emutils.client.mixin;

import net.emutils.client.emutils.waypoint.WaypointClickHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonNetworkHandlerMixin {
	@Inject(method = "send", at = @At("HEAD"), cancellable = true)
	private void emutils$interceptCustomMouseButtonEvent(Packet<?> packet, CallbackInfo ci) {
		if (!(packet instanceof ServerboundCustomClickActionPacket clickPacket)) {
			return;
		}

		if (WaypointClickHandler.tryHandle(clickPacket.id(), clickPacket.payload(), Minecraft.getInstance())) {
			ci.cancel();
		}
	}
}
