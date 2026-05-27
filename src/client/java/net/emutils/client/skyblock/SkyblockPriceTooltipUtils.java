package net.emutils.client.skyblock;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.emutils.client.skyblock.bazaar.BazaarCoinFormatter;
import net.emutils.client.skyblock.config.EMSkyblockSettings;
import net.emutils.client.skyblock.eiv.EivCoinFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class SkyblockPriceTooltipUtils {
	private static final Pattern SACK_STORED = Pattern.compile("Stored:\\s*([\\d,]+)", Pattern.CASE_INSENSITIVE);

	private SkyblockPriceTooltipUtils() {
	}

	public static List<Text> appendAddonBlock(List<Text> tooltip, List<Text> addonLines) {
		return appendAddonLines(tooltip, addonLines, true);
	}

	public static List<Text> appendAddonLines(List<Text> tooltip, List<Text> addonLines, boolean separatorBefore) {
		if (addonLines.isEmpty()) {
			return tooltip;
		}

		List<Text> combined = new ArrayList<>(tooltip.size() + addonLines.size() + (separatorBefore ? 1 : 0));
		combined.addAll(tooltip);
		if (separatorBefore && !tooltip.isEmpty()) {
			combined.add(Text.empty());
		}
		combined.addAll(addonLines);
		return combined;
	}

	public static double totalAmount(List<Text> tooltip, ItemStack stack, boolean stackTotal) {
		if (!stackTotal) {
			return 1.0D;
		}

		Integer sackStored = parseSackStoredAmount(tooltip);
		if (sackStored != null && sackStored > 0) {
			return sackStored;
		}

		return Math.max(1, stack.getCount());
	}

	public static boolean isShiftDown(MinecraftClient client) {
		if (client.getWindow() == null) {
			return false;
		}

		return InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_LEFT_SHIFT)
			|| InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_RIGHT_SHIFT);
	}

	public static Text priceLine(Text label, Formatting labelColor, double amount) {
		return Text.empty()
			.append(label.copy().formatted(labelColor))
			.append(Text.literal(formatPrice(amount) + " coins").formatted(Formatting.GOLD));
	}

	public static String formatPrice(double amount) {
		if (EMSkyblockSettings.priceTooltipCompactNumbers()) {
			return EivCoinFormat.compact(amount);
		}

		return BazaarCoinFormatter.format(amount);
	}

	public static boolean useStackTotal(MinecraftClient client) {
		return EMSkyblockSettings.priceTooltipShiftForStackTotal() && isShiftDown(client);
	}

	private static Integer parseSackStoredAmount(List<Text> tooltip) {
		for (Text line : tooltip) {
			String stripped = SkyblockTextUtils.strip(line);
			Matcher matcher = SACK_STORED.matcher(stripped);
			if (matcher.find()) {
				return parseIntGroup(matcher.group(1));
			}
		}

		return null;
	}

	private static Integer parseIntGroup(String value) {
		try {
			return Integer.parseInt(value.replace(",", ""));
		} catch (NumberFormatException ignored) {
			return null;
		}
	}
}
