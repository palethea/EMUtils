package net.emutils.client.mixin;

import net.emutils.client.death.DeathWaypointClickHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CustomClickActionC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonNetworkHandler.class)
public abstract class ClientCommonNetworkHandlerMixin {
	@Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
	private void emutils$interceptCustomClick(Packet<?> packet, CallbackInfo ci) {
		if (!(packet instanceof CustomClickActionC2SPacket clickPacket)) {
			return;
		}

		if (DeathWaypointClickHandler.tryHandle(clickPacket.id(), MinecraftClient.getInstance())) {
			ci.cancel();
		}
	}
}
