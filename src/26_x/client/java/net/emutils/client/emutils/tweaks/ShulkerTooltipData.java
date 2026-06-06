package net.emutils.client.emutils.tweaks;

import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record ShulkerTooltipData(ItemContainerContents contents) implements TooltipComponent {
}
