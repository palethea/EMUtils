package net.emutils.client.versioned.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.emutils.client.emutils.tweaks.LowShield;
import net.emutils.client.emutils.tweaks.LowTotem;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/** Low Shield and Low Totem (#193): moves the hand that holds a raised shield, or a totem, down. Minecraft 26.2's first-person renderer. */
@Mixin(ItemInHandRenderer.class)
public abstract class FirstPersonShieldMixin {
	@WrapMethod(method = "submitArmWithItem")
	private void emutils$lowerShield(
		AbstractClientPlayer player,
		float frameInterp,
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
			original.call(player, frameInterp, xRot, hand, attack, itemStack, inverseArmHeight, poseStack, submitNodeCollector, lightCoords);
			return;
		}
		poseStack.pushPose();
		try {
			poseStack.translate(0.0F, -offset, 0.0F);
			original.call(player, frameInterp, xRot, hand, attack, itemStack, inverseArmHeight, poseStack, submitNodeCollector, lightCoords);
		} finally {
			poseStack.popPose();
		}
	}
}
