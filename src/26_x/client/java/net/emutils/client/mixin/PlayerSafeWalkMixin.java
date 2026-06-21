package net.emutils.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerSafeWalkMixin {
	@ModifyExpressionValue(
		method = "maybeBackOffFromEdge",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Player;isStayingOnGroundSurface()Z"
		)
	)
	private boolean emutils$enableSafeWalk(boolean stayingOnGroundSurface) {
		EMUtilsConfig config = EMUtilsClient.config();
		return stayingOnGroundSurface
			|| (Object) this instanceof LocalPlayer
			&& config != null
			&& config.tweakSafeWalk();
	}
}
