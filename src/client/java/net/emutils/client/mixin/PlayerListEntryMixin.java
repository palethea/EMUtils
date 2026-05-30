package net.emutils.client.mixin;

import com.mojang.authlib.GameProfile;
import net.emutils.client.emutils.capes.CustomCapeManager;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntryMixin {
	@Shadow
	@Final
	private GameProfile profile;

	@Inject(method = "getSkinTextures", at = @At("HEAD"))
	private void emutils$loadCustomCape(CallbackInfoReturnable<SkinTextures> cir) {
		CustomCapeManager.onLoadTexture(profile);
	}

	@Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
	private void emutils$applyCustomCape(CallbackInfoReturnable<SkinTextures> cir) {
		AssetInfo.TextureAsset capeTexture = CustomCapeManager.capeTextureFor(profile);
		if (capeTexture == null) {
			return;
		}

		SkinTextures original = cir.getReturnValue();
		cir.setReturnValue(new SkinTextures(
			original.body(),
			capeTexture,
			original.elytra(),
			original.model(),
			original.secure()
		));
	}
}
