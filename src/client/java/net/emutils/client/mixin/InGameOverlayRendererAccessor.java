package net.emutils.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(InGameOverlayRenderer.class)
public interface InGameOverlayRendererAccessor {
	@Invoker("renderUnderwaterOverlay")
	static void emutils$renderUnderwaterOverlay(
		MinecraftClient client,
		MatrixStack matrices,
		VertexConsumerProvider vertexConsumers
	) {
		throw new AssertionError();
	}

	@Invoker("renderFireOverlay")
	static void emutils$renderFireOverlay(
		MatrixStack matrices,
		VertexConsumerProvider vertexConsumers,
		Sprite sprite
	) {
		throw new AssertionError();
	}
}
