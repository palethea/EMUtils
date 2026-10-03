package net.emutils.client.versioned.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.emutils.client.emutils.tweaks.LowShield;
import net.emutils.client.emutils.tweaks.LowTotem;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/** Low Shield and Low Totem (#193): moves the hand that holds a raised shield, or a totem, down. Minecraft 26.3's first-person renderer. */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHeldItemMixin {
	@WrapMethod(method = "submitArmWithItem")
	private void emutils$lowerShield(
		PlayerRenderState playerState,
		FirstPersonHandsAndItemsRenderState state,
		float partialTicks,
		float xRot,
		InteractionHand hand,
		float attack,
		ItemStack itemStack,
		float inverseArmHeight,
		PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector,
		int lightCoords,
		Operation<Void> original
	) {
		float offset = LowShield.offset(hand, itemStack) + LowTotem.offset(itemStack);
		if (offset <= 0.0F) {
			original.call(playerState, state, partialTicks, xRot, hand, attack, itemStack, inverseArmHeight, poseStack, submitNodeCollector, lightCoords);
			return;
		}
		poseStack.pushPose();
		try {
			poseStack.translate(0.0F, -offset, 0.0F);
			original.call(playerState, state, partialTicks, xRot, hand, attack, itemStack, inverseArmHeight, poseStack, submitNodeCollector, lightCoords);
		} finally {
			poseStack.popPose();
		}
	}
}
