package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.access.MinecraftFastPlaceTracker;
import net.emutils.client.emutils.tweaks.LockedYPlacementManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeFastPlaceMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Redirect(
		method = "performUseItemOn",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;")
	)
	private InteractionResult emutils$trackBlockPlacementAttempt(ItemStack stack, UseOnContext context) {
		if (stack.getItem() instanceof BlockItem) {
			if (EMUtilsClient.config() != null && EMUtilsClient.config().tweakFastPlace()) {
				((MinecraftFastPlaceTracker) minecraft).emutils$markFastPlaceAttempt();
			}
			if (EMUtilsClient.tweaks() != null
				&& EMUtilsClient.tweaks().lockedYPlacement().shouldBlockPlacement(stack, context.getClickedPos(), context.getClickedFace())) {
				EMUtilsClient.tweaks().lockedYPlacement().markBlockedPlacementPacket();
				return LockedYPlacementManager.blockedResult();
			}
		}
		return stack.useOn(context);
	}

	@Redirect(
		method = "startPrediction",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V")
	)
	private void emutils$suppressBlockedLockedYPlacementPacket(ClientPacketListener connection, Packet<?> packet) {
		if (packet instanceof ServerboundUseItemOnPacket
			&& EMUtilsClient.tweaks() != null
			&& EMUtilsClient.tweaks().lockedYPlacement().consumeBlockedPlacementPacket()) {
			return;
		}

		connection.send(packet);
	}
}
