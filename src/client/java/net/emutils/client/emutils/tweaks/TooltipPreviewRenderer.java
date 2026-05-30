package net.emutils.client.emutils.tweaks;

import java.util.ArrayList;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

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
		ContainerComponent container = stack.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT);
		return new ShulkerTooltipData(container);
	}

	public static List<Text> stripContainerLines(List<Text> tooltip) {
		List<Text> filtered = new ArrayList<>(tooltip.size());
		for (Text line : tooltip) {
			if (!isContainerLine(line)) {
				filtered.add(line);
			}
		}

		return filtered;
	}

	private static boolean isContainerLine(Text line) {
		if (line.getContent() instanceof TranslatableTextContent translatable) {
			String key = translatable.getKey();
			return CONTAINER_ITEM_COUNT_KEY.equals(key) || CONTAINER_MORE_ITEMS_KEY.equals(key);
		}

		return false;
	}

	private static boolean hasShulkerContents(ItemStack stack) {
		ContainerComponent component = stack.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT);
		for (ItemStack content : component.iterateNonEmpty()) {
			if (!content.isEmpty()) {
				return true;
			}
		}

		return false;
	}

	private static boolean isShulker(ItemStack stack) {
		return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
	}
}
