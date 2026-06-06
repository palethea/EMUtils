package net.emutils.client.emutils.food;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;

public final class FoodTooltipComponent implements ClientTooltipComponent {
	private static final int HUNGER_ICON_SIZE = 9;
	private static final int SATURATION_ICON_SIZE = 7;
	private static final int ROW_GAP = 2;
	private final FoodValues defaultValues;
	private final FoodValues modifiedValues;
	private final boolean rotten;
	private final int hungerBars;
	private final int saturationBars;
	private final String hungerText;
	private final String saturationText;

	public FoodTooltipComponent(FoodTooltipData data) {
		this.defaultValues = data.defaultValues();
		this.modifiedValues = data.modifiedValues();
		this.rotten = data.rotten();
		int biggestHunger = Math.max(Math.abs(defaultValues.hunger()), Math.abs(modifiedValues.hunger()));
		int rawHungerBars = (int) Math.ceil(biggestHunger / 2.0F);
		if (rawHungerBars > 10) {
			hungerBars = 1;
			hungerText = "x" + rawHungerBars;
		} else {
			hungerBars = Math.max(1, rawHungerBars);
			hungerText = null;
		}

		float biggestSaturation = Math.max(Math.abs(defaultValues.saturationIncrement()), Math.abs(modifiedValues.saturationIncrement()));
		int rawSaturationBars = (int) Math.ceil(biggestSaturation / 2.0F);
		if (rawSaturationBars > 10 || rawSaturationBars == 0) {
			saturationBars = 1;
			saturationText = "x" + rawSaturationBars;
		} else {
			saturationBars = rawSaturationBars;
			saturationText = null;
		}
	}

	@Override
	public int getWidth(Font textRenderer) {
		int hungerWidth = hungerBars * HUNGER_ICON_SIZE + textWidth(textRenderer, hungerText);
		int saturationWidth = saturationBars * SATURATION_ICON_SIZE + textWidth(textRenderer, saturationText);
		return Math.max(hungerWidth, saturationWidth);
	}

	@Override
	public int getHeight(Font textRenderer) {
		return HUNGER_ICON_SIZE + ROW_GAP + SATURATION_ICON_SIZE;
	}

	@Override
	public void extractImage(Font textRenderer, int x, int y, int width, int height, GuiGraphicsExtractor context) {
		drawHungerRow(textRenderer, context, x, y);
		drawSaturationRow(textRenderer, context, x, y + HUNGER_ICON_SIZE + ROW_GAP);
	}

	private void drawHungerRow(Font textRenderer, GuiGraphicsExtractor context, int x, int y) {
		int defaultHunger = defaultValues.hunger();
		int modifiedHunger = modifiedValues.hunger();
		int drawX = x + (hungerBars - 1) * HUNGER_ICON_SIZE;
		for (int i = 0; i < hungerBars * 2; i += 2) {
			context.blitSprite(RenderPipelines.GUI_TEXTURED, FoodHudTextures.empty(rotten), drawX, y, HUNGER_ICON_SIZE, HUNGER_ICON_SIZE, 0.45F);
			if (modifiedHunger > i) {
				boolean half = modifiedHunger - 1 == i;
				context.blitSprite(RenderPipelines.GUI_TEXTURED, half ? FoodHudTextures.half(rotten) : FoodHudTextures.full(rotten), drawX, y, HUNGER_ICON_SIZE, HUNGER_ICON_SIZE);
			} else if (defaultHunger > i) {
				boolean half = defaultHunger - 1 == i;
				context.blitSprite(RenderPipelines.GUI_TEXTURED, half ? FoodHudTextures.half(rotten) : FoodHudTextures.full(rotten), drawX, y, HUNGER_ICON_SIZE, HUNGER_ICON_SIZE, 0.25F);
			}
			drawX -= HUNGER_ICON_SIZE;
		}
		if (hungerText != null) {
			context.text(textRenderer, Component.literal(hungerText), x + HUNGER_ICON_SIZE + 2, y + 1, 0xFFAAAAAA, false);
		}
	}

	private void drawSaturationRow(Font textRenderer, GuiGraphicsExtractor context, int x, int y) {
		float saturation = modifiedValues.saturationIncrement();
		float absoluteSaturation = Math.abs(saturation);
		int drawX = x + (saturationBars - 1) * SATURATION_ICON_SIZE;
		for (int i = 0; i < saturationBars * 2; i += 2) {
			float effectiveSaturation = (absoluteSaturation - i) / 2.0F;
			int u = effectiveSaturation >= 1.0F ? 21 : effectiveSaturation > 0.5F ? 14 : effectiveSaturation > 0.25F ? 7 : effectiveSaturation > 0.0F ? 0 : 28;
			int v = saturation >= 0.0F ? 27 : 34;
			int color = absoluteSaturation <= i ? 0x88FFFFFF : 0xFFFFFFFF;
			context.blit(RenderPipelines.GUI_TEXTURED, FoodHudTextures.ICONS, drawX, y, u, v, SATURATION_ICON_SIZE, SATURATION_ICON_SIZE, SATURATION_ICON_SIZE, SATURATION_ICON_SIZE, 256, 256, color);
			drawX -= SATURATION_ICON_SIZE;
		}
		if (saturationText != null) {
			context.text(textRenderer, Component.literal(saturationText), x + SATURATION_ICON_SIZE + 2, y, 0xFFAAAAAA, false);
		}
	}

	private static int textWidth(Font textRenderer, String text) {
		return text == null ? 0 : textRenderer.width(text) + 2;
	}
}
