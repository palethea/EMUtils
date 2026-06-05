package net.emutils.client.emutils.food;

import net.minecraft.item.tooltip.TooltipData;

public record FoodTooltipData(FoodValues defaultValues, FoodValues modifiedValues, boolean rotten) implements TooltipData {
}
