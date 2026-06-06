package net.emutils.client.emutils.food;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record FoodTooltipData(FoodValues defaultValues, FoodValues modifiedValues, boolean rotten) implements TooltipComponent {
}
