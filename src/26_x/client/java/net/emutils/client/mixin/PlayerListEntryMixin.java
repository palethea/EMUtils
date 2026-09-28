package net.emutils.client.mixin;

import com.mojang.authlib.GameProfile;
import net.emutils.client.emutils.capes.CustomCapeManager;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInfo.class)
public abstract class PlayerListEntryMixin {
	@Shadow
	@Final
	private GameProfile profile;

	@Inject(method = "getSkin", at = @At("HEAD"))
	private void emutils$loadCustomCape(CallbackInfoReturnable<PlayerSkin> cir) {
		CustomCapeManager.onLoadTexture(profile);
	}

	/** Shows the cape the priority list picks (#52): a provider's, the official one, or none. */
	@Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
	private void emutils$applyCustomCape(CallbackInfoReturnable<PlayerSkin> cir) {
		PlayerSkin original = cir.getReturnValue();
		ClientAsset.Texture capeTexture = CustomCapeManager.capeTextureFor(profile, original.cape());
		if (capeTexture == original.cape()) {
			return;
		}

		cir.setReturnValue(new PlayerSkin(
			original.body(),
			capeTexture,
			original.elytra(),
			original.model(),
			original.secure()
		));
	}
}
