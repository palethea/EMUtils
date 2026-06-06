package net.emutils.client.emutils.food;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.mixin.HungerManagerAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.ApplyEffectsConsumeEffect;
import net.minecraft.item.consume.ConsumeEffect;
import org.lwjgl.glfw.GLFW;
import org.jspecify.annotations.Nullable;

public final class FoodHudHelper {
	private FoodHudHelper() {
	}

	public static boolean isFood(ItemStack stack) {
		return stack != null && !stack.isEmpty() && stack.get(DataComponentTypes.FOOD) != null;
	}

	public static boolean canConsume(ItemStack stack, PlayerEntity player) {
		if (!isFood(stack) || player == null) {
			return false;
		}

		FoodComponent food = stack.get(DataComponentTypes.FOOD);
		return food != null && player.canConsume(food.canAlwaysEat());
	}

	public static FoodValues values(ItemStack stack) {
		FoodComponent food = stack == null ? null : stack.get(DataComponentTypes.FOOD);
		if (food == null) {
			return new FoodValues(0, 0.0F);
		}

		return new FoodValues(food.nutrition(), food.saturation());
	}

	public static boolean isRotten(ItemStack stack) {
		ConsumableComponent consumable = stack == null ? null : stack.get(DataComponentTypes.CONSUMABLE);
		if (consumable == null) {
			return false;
		}

		for (ConsumeEffect effect : consumable.onConsumeEffects()) {
			if (effect instanceof ApplyEffectsConsumeEffect applyEffects && hasHarmfulEffect(applyEffects)) {
				return true;
			}
		}
		return false;
	}

	public static float exhaustion(PlayerEntity player) {
		return ((HungerManagerAccessor) player.getHungerManager()).emutils$getExhaustion();
	}

	public static ItemStack heldFood(PlayerEntity player) {
		EMUtilsConfig config = EMUtilsClient.config();
		ItemStack mainHand = player.getMainHandStack();
		if (canConsume(mainHand, player)) {
			return mainHand;
		}

		if (config.foodHudOffhandOverlay()) {
			ItemStack offHand = player.getOffHandStack();
			if (canConsume(offHand, player)) {
				return offHand;
			}
		}

		return ItemStack.EMPTY;
	}

	@Nullable
	public static FoodTooltipData tooltipData(ItemStack stack, @Nullable PlayerEntity player) {
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

	private static boolean hasHarmfulEffect(ApplyEffectsConsumeEffect applyEffects) {
		for (StatusEffectInstance instance : applyEffects.effects()) {
			if (instance.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL) {
				return true;
			}
		}
		return false;
	}

	private static boolean shiftDown() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.getWindow() == null) {
			return false;
		}
		return InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
			|| InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
	}
}
