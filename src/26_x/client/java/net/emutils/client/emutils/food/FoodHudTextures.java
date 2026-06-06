package net.emutils.client.emutils.food;

import net.emutils.client.EMUtilsClient;
import net.minecraft.resources.Identifier;

final class FoodHudTextures {
	static final Identifier ICONS = Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "textures/gui/food/icons.png");
	static final Identifier FOOD_EMPTY = Identifier.withDefaultNamespace("hud/food_empty");
	static final Identifier FOOD_HALF = Identifier.withDefaultNamespace("hud/food_half");
	static final Identifier FOOD_FULL = Identifier.withDefaultNamespace("hud/food_full");
	static final Identifier FOOD_EMPTY_HUNGER = Identifier.withDefaultNamespace("hud/food_empty_hunger");
	static final Identifier FOOD_HALF_HUNGER = Identifier.withDefaultNamespace("hud/food_half_hunger");
	static final Identifier FOOD_FULL_HUNGER = Identifier.withDefaultNamespace("hud/food_full_hunger");

	private FoodHudTextures() {
	}

	static Identifier empty(boolean hunger) {
		return hunger ? FOOD_EMPTY_HUNGER : FOOD_EMPTY;
	}

	static Identifier half(boolean hunger) {
		return hunger ? FOOD_HALF_HUNGER : FOOD_HALF;
	}

	static Identifier full(boolean hunger) {
		return hunger ? FOOD_FULL_HUNGER : FOOD_FULL;
	}
}
