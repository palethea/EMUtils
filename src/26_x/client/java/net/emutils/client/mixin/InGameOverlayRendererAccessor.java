package net.emutils.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ScreenEffectRenderer.class)
public interface InGameOverlayRendererAccessor {
	@Invoker("renderWater")
	static void emutils$renderWater(
		Minecraft client,
		PoseStack matrices,
		MultiBufferSource vertexConsumers
	) {
		throw new AssertionError();
	}

	@Invoker("renderFire")
	static void emutils$renderFire(
		PoseStack matrices,
		MultiBufferSource vertexConsumers,
		TextureAtlasSprite sprite
	) {
		throw new AssertionError();
	}
}
