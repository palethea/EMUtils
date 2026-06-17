package net.emutils.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ScreenEffectRenderer.class)
public interface InGameOverlayRendererAccessor {
	@Invoker("submitWater")
	static void emutils$submitWater(
		Minecraft client,
		PoseStack matrices,
		SubmitNodeCollector submitNodeCollector
	) {
		throw new AssertionError();
	}

	@Invoker("submitFire")
	static void emutils$submitFire(
		PoseStack matrices,
		SubmitNodeCollector submitNodeCollector,
		TextureAtlasSprite sprite
	) {
		throw new AssertionError();
	}
}
