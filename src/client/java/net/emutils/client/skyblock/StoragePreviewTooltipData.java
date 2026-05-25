package net.emutils.client.skyblock;

import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.util.collection.DefaultedList;

public record StoragePreviewTooltipData(int rows, DefaultedList<ItemStack> contents) implements TooltipData {
}
