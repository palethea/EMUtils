package net.emutils.client.emutils.tweaks;

import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.tooltip.TooltipData;

public record ShulkerTooltipData(ContainerComponent contents) implements TooltipData {
}
