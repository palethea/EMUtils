package net.emutils.client.emutils.food;

public record FoodValues(int hunger, float saturation) {
	public float saturationIncrement() {
		return saturation;
	}
}
