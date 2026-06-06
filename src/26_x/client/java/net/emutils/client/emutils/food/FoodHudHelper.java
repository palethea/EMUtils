package net.emutils.client.emutils.food;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.mixin.HungerManagerAccessor;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import org.lwjgl.glfw.GLFW;
import org.jspecify.annotations.Nullable;

public final class FoodHudHelper {
	private FoodHudHelper() {
	}

	public static boolean isFood(ItemStack stack) {
		return stack != null && !stack.isEmpty() && stack.get(DataComponents.FOOD) != null;
	}

	public static boolean canConsume(ItemStack stack, Player player) {
		if (!isFood(stack) || player == null) {
			return false;
		}

		FoodProperties food = stack.get(DataComponents.FOOD);
		return food != null && player.canEat(food.canAlwaysEat());
	}

	public static FoodValues values(ItemStack stack) {
		FoodProperties food = stack == null ? null : stack.get(DataComponents.FOOD);
		if (food == null) {
			return new FoodValues(0, 0.0F);
		}

		return new FoodValues(food.nutrition(), food.saturation());
	}

	public static boolean isRotten(ItemStack stack) {
		Consumable consumable = stack == null ? null : stack.get(DataComponents.CONSUMABLE);
		if (consumable == null) {
			return false;
		}

		for (ConsumeEffect effect : consumable.onConsumeEffects()) {
			if (effect instanceof ApplyStatusEffectsConsumeEffect applyEffects && hasHarmfulEffect(applyEffects)) {
				return true;
			}
		}
		return false;
	}

	public static float exhaustion(Player player) {
		return ((HungerManagerAccessor) player.getFoodData()).emutils$getExhaustion();
	}

	public static ItemStack heldFood(Player player) {
		EMUtilsConfig config = EMUtilsClient.config();
		ItemStack mainHand = player.getMainHandItem();
		if (canConsume(mainHand, player)) {
			return mainHand;
		}

		if (config.foodHudOffhandOverlay()) {
			ItemStack offHand = player.getOffhandItem();
			if (canConsume(offHand, player)) {
				return offHand;
			}
		}

		return ItemStack.EMPTY;
	}

	@Nullable
	public static FoodTooltipData tooltipData(ItemStack stack, @Nullable Player player) {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || !config.foodHud() || !config.foodHudTooltips()) {
			return null;
		}
		if (!config.foodHudTooltipAlways() && !shiftDown()) {
			return null;
		}
		if (!isFood(stack)) {
			return null;
		}

		FoodValues values = values(stack);
		return new FoodTooltipData(values, values, isRotten(stack));
	}

	private static boolean hasHarmfulEffect(ApplyStatusEffectsConsumeEffect applyEffects) {
		for (MobEffectInstance instance : applyEffects.effects()) {
			if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
				return true;
			}
		}
		return false;
	}

	private static boolean shiftDown() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.getWindow() == null) {
			return false;
		}
		return InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
			|| InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
	}
}
