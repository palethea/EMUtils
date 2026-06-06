package net.emutils.client.emutils.food;

import java.util.Random;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;

public final class FoodHudRenderer {
	private static final int FOOD_BARS = 10;
	private static final int ICON_SIZE = 9;
	private static final int EXHAUSTION_WIDTH = 81;
	private static final float MAX_EXHAUSTION = 4.0F;
	private static final float FLASH_STEP = 0.125F;
	private static final float MAX_FLASH_ALPHA = 0.65F;
	private static final Random RANDOM = new Random();
	private static float unclampedFlashAlpha;
	private static float flashAlpha;
	private static int alphaDir = 1;

	private FoodHudRenderer() {
	}

	public static void tick(Minecraft client) {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || !config.foodHud()) {
			resetFlash();
			return;
		}

		unclampedFlashAlpha += alphaDir * FLASH_STEP;
		if (unclampedFlashAlpha >= 1.5F) {
			alphaDir = -1;
		} else if (unclampedFlashAlpha <= -0.5F) {
			alphaDir = 1;
		}
		flashAlpha = Mth.clamp(unclampedFlashAlpha, 0.0F, 1.0F) * MAX_FLASH_ALPHA;
	}

	public static void renderExhaustion(GuiGraphicsExtractor context, Player player, int top, int right) {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || !config.foodHud() || !config.foodHudExhaustionUnderlay()) {
			return;
		}

		int width = Mth.floor(Mth.clamp(FoodHudHelper.exhaustion(player) / MAX_EXHAUSTION, 0.0F, 1.0F) * EXHAUSTION_WIDTH);
		if (width <= 0) {
			return;
		}

		context.blit(
			RenderPipelines.GUI_TEXTURED,
			FoodHudTextures.ICONS,
			right - width,
			top,
			EXHAUSTION_WIDTH - width,
			18.0F,
			width,
			ICON_SIZE,
			width,
			ICON_SIZE,
			256,
			256,
			0xC0FFFFFF
		);
	}

	public static void renderOverlays(GuiGraphicsExtractor context, Player player, int top, int right, int ticks) {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || !config.foodHud()) {
			return;
		}

		Offset[] offsets = hungerOffsets(player, top, right, ticks, config.foodHudVanillaAnimations());
		FoodData hunger = player.getFoodData();
		if (config.foodHudSaturationOverlay()) {
			drawSaturationOverlay(context, offsets, hunger.getSaturationLevel(), 0.0F, 0xFFFFFFFF);
		}

		if (!config.foodHudHeldFoodOverlay()) {
			resetFlash();
			return;
		}

		ItemStack heldFood = FoodHudHelper.heldFood(player);
		if (heldFood.isEmpty()) {
			resetFlash();
			return;
		}

		FoodValues values = FoodHudHelper.values(heldFood);
		drawHungerOverlay(context, offsets, hunger.getFoodLevel(), values.hunger(), flashAlpha, FoodHudHelper.isRotten(heldFood));
		if (config.foodHudSaturationOverlay()) {
			int newFoodValue = hunger.getFoodLevel() + values.hunger();
			float newSaturationValue = hunger.getSaturationLevel() + values.saturationIncrement();
			float saturationGained = newSaturationValue > newFoodValue ? newFoodValue - hunger.getSaturationLevel() : values.saturationIncrement();
			drawSaturationOverlay(context, offsets, hunger.getSaturationLevel(), saturationGained, alphaColor(flashAlpha));
		}
	}

	private static void drawSaturationOverlay(GuiGraphicsExtractor context, Offset[] offsets, float saturationLevel, float saturationGained, int color) {
		if (saturationLevel + saturationGained < 0.0F) {
			return;
		}

		float modifiedSaturation = Mth.clamp(saturationLevel + saturationGained, 0.0F, 20.0F);
		int startSaturationBar = saturationGained == 0.0F ? 0 : Math.max((int) (saturationLevel / 2.0F), 0);
		int endSaturationBar = (int) Math.ceil(modifiedSaturation / 2.0F);
		for (int i = startSaturationBar; i < endSaturationBar && i < offsets.length; i++) {
			Offset offset = offsets[i];
			float effectiveSaturation = (modifiedSaturation / 2.0F) - i;
			int u = effectiveSaturation >= 1.0F ? 27 : effectiveSaturation > 0.5F ? 18 : effectiveSaturation > 0.25F ? 9 : 0;
			context.blit(RenderPipelines.GUI_TEXTURED, FoodHudTextures.ICONS, offset.x(), offset.y(), u, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, 256, 256, color);
		}
	}

	private static void drawHungerOverlay(GuiGraphicsExtractor context, Offset[] offsets, int foodLevel, int hungerRestored, float alpha, boolean rotten) {
		if (hungerRestored <= 0 || alpha <= 0.0F) {
			return;
		}

		int modifiedFood = Mth.clamp(foodLevel + hungerRestored, 0, 20);
		int startFoodBars = Math.max(0, foodLevel / 2);
		int endFoodBars = (int) Math.ceil(modifiedFood / 2.0F);
		for (int i = startFoodBars; i < endFoodBars && i < offsets.length; i++) {
			Offset offset = offsets[i];
			int x = offset.x();
			int y = offset.y();
			context.blitSprite(RenderPipelines.GUI_TEXTURED, FoodHudTextures.empty(rotten), x, y, ICON_SIZE, ICON_SIZE, alpha * 0.25F);
			boolean half = i * 2 + 1 == modifiedFood;
			context.blitSprite(RenderPipelines.GUI_TEXTURED, half ? FoodHudTextures.half(rotten) : FoodHudTextures.full(rotten), x, y, ICON_SIZE, ICON_SIZE, alpha);
		}
	}

	private static Offset[] hungerOffsets(Player player, int top, int right, int ticks, boolean vanillaAnimations) {
		Offset[] offsets = new Offset[FOOD_BARS];
		boolean animate = false;
		if (vanillaAnimations) {
			FoodData hunger = player.getFoodData();
			int foodLevel = hunger.getFoodLevel();
			animate = hunger.getSaturationLevel() <= 0.0F && ticks % (foodLevel * 3 + 1) == 0;
		}

		for (int i = 0; i < offsets.length; i++) {
			int y = top;
			if (animate) {
				y += RANDOM.nextInt(3) - 1;
			}
			offsets[i] = new Offset(right - i * 8 - ICON_SIZE, y);
		}
		return offsets;
	}

	private static int alphaColor(float alpha) {
		int alphaByte = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
		return (alphaByte << 24) | 0x00FFFFFF;
	}

	private static void resetFlash() {
		unclampedFlashAlpha = 0.0F;
		flashAlpha = 0.0F;
		alphaDir = 1;
	}

	private record Offset(int x, int y) {
	}
}
