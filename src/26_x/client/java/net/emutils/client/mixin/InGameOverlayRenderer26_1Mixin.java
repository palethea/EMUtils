package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.client.renderer.ScreenEffectRenderer")
public abstract class InGameOverlayRenderer26_1Mixin {
	private static Method emutils$renderWaterMethod;
	private static Method emutils$renderFireMethod;

	@Redirect(
		method = "renderScreenEffect",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;renderWater(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"
		)
	)
	private static void emutils$skipUnderwaterOverlay(
		Minecraft client,
		PoseStack matrices,
		@Coerce Object vertexConsumers
	) {
		if (EMUtilsClient.config().tweakClearUnderwater()) {
			return;
		}

		emutils$invokeRenderWater(client, matrices, vertexConsumers);
	}

	@Redirect(
		method = "renderScreenEffect",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;renderFire(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V"
		)
	)
	private static void emutils$skipFireOverlay(
		PoseStack matrices,
		@Coerce Object vertexConsumers,
		TextureAtlasSprite sprite
	) {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config != null && config.tweakNoFireOverlay()) {
			return;
		}

		if (config != null && config.tweakLowFireOverlay()) {
			matrices.pushPose();
			matrices.translate(0.0F, -0.38F, 0.0F);
			matrices.scale(1.0F, 0.65F, 1.0F);
			emutils$invokeRenderFire(matrices, vertexConsumers, sprite);
			matrices.popPose();
			return;
		}

		emutils$invokeRenderFire(matrices, vertexConsumers, sprite);
	}

	private static void emutils$invokeRenderWater(Minecraft client, PoseStack matrices, Object vertexConsumers) {
		try {
			if (emutils$renderWaterMethod == null) {
				Class<?> multiBufferSourceClass = Class.forName("net.minecraft.client.renderer.MultiBufferSource");
				emutils$renderWaterMethod = ScreenEffectRenderer.class.getDeclaredMethod(
					"renderWater",
					Minecraft.class,
					PoseStack.class,
					multiBufferSourceClass
				);
				emutils$renderWaterMethod.setAccessible(true);
			}
			emutils$renderWaterMethod.invoke(null, client, matrices, vertexConsumers);
		} catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
			throw new IllegalStateException("Could not call Minecraft 26.1 water overlay renderer", e);
		} catch (InvocationTargetException e) {
			emutils$throwCause(e);
		}
	}

	private static void emutils$invokeRenderFire(PoseStack matrices, Object vertexConsumers, TextureAtlasSprite sprite) {
		try {
			if (emutils$renderFireMethod == null) {
				Class<?> multiBufferSourceClass = Class.forName("net.minecraft.client.renderer.MultiBufferSource");
				emutils$renderFireMethod = ScreenEffectRenderer.class.getDeclaredMethod(
					"renderFire",
					PoseStack.class,
					multiBufferSourceClass,
					TextureAtlasSprite.class
				);
				emutils$renderFireMethod.setAccessible(true);
			}
			emutils$renderFireMethod.invoke(null, matrices, vertexConsumers, sprite);
		} catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
			throw new IllegalStateException("Could not call Minecraft 26.1 fire overlay renderer", e);
		} catch (InvocationTargetException e) {
			emutils$throwCause(e);
		}
	}

	private static void emutils$throwCause(InvocationTargetException e) {
		Throwable cause = e.getCause();
		if (cause instanceof RuntimeException runtimeException) {
			throw runtimeException;
		}
		if (cause instanceof Error error) {
			throw error;
		}
		throw new IllegalStateException("Minecraft 26.1 overlay renderer failed", cause);
	}
}
