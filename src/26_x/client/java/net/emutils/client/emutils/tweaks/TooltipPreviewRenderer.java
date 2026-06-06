package net.emutils.client.emutils.tweaks;

import java.util.ArrayList;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

public final class TooltipPreviewRenderer {
	private static final String CONTAINER_ITEM_COUNT_KEY = "item.container.item_count";
	private static final String CONTAINER_MORE_ITEMS_KEY = "item.container.more_items";

	private TooltipPreviewRenderer() {
	}

	public static boolean shouldPreviewShulker(ItemStack stack) {
		if (stack == null || stack.isEmpty() || !isShulker(stack)) {
			return false;
		}

		return EMUtilsClient.config().tweakShulkerTooltipPreview() && hasShulkerContents(stack);
	}

	public static ShulkerTooltipData createShulkerTooltipData(ItemStack stack) {
		ItemContainerContents container = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
		return new ShulkerTooltipData(container);
	}

	public static List<Component> stripContainerLines(List<Component> tooltip) {
		List<Component> filtered = new ArrayList<>(tooltip.size());
		for (Component line : tooltip) {
			if (!isContainerLine(line)) {
				filtered.add(line);
			}
		}

		return filtered;
	}

	private static boolean isContainerLine(Component line) {
		if (line.getContents() instanceof TranslatableContents translatable) {
			String key = translatable.getKey();
			return CONTAINER_ITEM_COUNT_KEY.equals(key) || CONTAINER_MORE_ITEMS_KEY.equals(key);
		}

		return false;
	}

	private static boolean hasShulkerContents(ItemStack stack) {
		ItemContainerContents component = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
		return component.nonEmptyItemCopyStream().anyMatch(content -> !content.isEmpty());
	}

	private static boolean isShulker(ItemStack stack) {
		return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
	}
}
