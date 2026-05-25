package net.emutils.client.skyblock;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

public final class StoragePreviewMatcher {
	private StoragePreviewMatcher() {
	}

	public static List<String> lookupKeys(ItemStack stack, @Nullable MinecraftClient client) {
		String displayName = StoragePreviewKeys.normalize(stack.getName().getString());
		List<String> tooltipLines = tooltipLines(stack, client);
		return StoragePreviewKeys.derivedLookupKeys(displayName, tooltipLines);
	}

	private static List<String> tooltipLines(ItemStack stack, @Nullable MinecraftClient client) {
		if (client == null || client.player == null) {
			return List.of();
		}

		try {
			List<Text> tooltip = stack.getTooltip(
				Item.TooltipContext.create(client.world),
				client.player,
				TooltipType.BASIC
			);
			List<String> lines = new ArrayList<>(tooltip.size());
			for (Text line : tooltip) {
				String normalized = StoragePreviewKeys.normalize(line.getString());
				if (!normalized.isBlank()) {
					lines.add(normalized);
				}
			}
			return lines;
		} catch (RuntimeException exception) {
			return List.of();
		}
	}
}
