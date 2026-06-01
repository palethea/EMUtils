package net.emutils.client.emskyblock.features.inventory.common;

import io.github.notenoughupdates.moulconfig.ChromaColour;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emhelpers.text.EmUtilsChatPrefix;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public final class InventoryFeatureUtils {
	private static final Pattern NUMBER = Pattern.compile("([0-9][0-9,]*(?:\\.[0-9]+)?)");
	private static final ItemStack PRICE_HISTORY_ICON = createPriceHistoryIcon();

	private InventoryFeatureUtils() {
	}

	public static boolean topInventorySlot(Slot slot) {
		return !(slot.inventory instanceof PlayerInventory);
	}

	public static List<Text> lore(ItemStack stack) {
		if (stack.isEmpty()) {
			return List.of();
		}

		LoreComponent lore = stack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT);
		return lore.lines();
	}

	public static List<String> strippedLore(ItemStack stack) {
		List<Text> lines = lore(stack);
		if (lines.isEmpty()) {
			return List.of();
		}

		List<String> result = new ArrayList<>(lines.size());
		for (Text line : lines) {
			result.add(strip(line));
		}
		return result;
	}

	public static String strip(Text text) {
		return strip(text == null ? "" : text.getString());
	}

	public static String strip(String text) {
		String stripped = Formatting.strip(text);
		return stripped == null ? "" : stripped.trim();
	}

	public static String itemName(ItemStack stack) {
		return stack.isEmpty() ? "" : strip(stack.getName());
	}

	public static boolean titleMatches(String title, String exact) {
		return strip(title).equalsIgnoreCase(exact);
	}

	public static boolean titleStarts(String title, String prefix) {
		return strip(title).toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT));
	}

	public static long parseLong(String value) {
		String normalized = value.replace(",", "").trim();
		if (normalized.isEmpty()) {
			return 0L;
		}

		try {
			return Math.round(Double.parseDouble(normalized));
		} catch (NumberFormatException ignored) {
			return 0L;
		}
	}

	public static double parseDouble(String value) {
		String normalized = value.replace(",", "").trim();
		if (normalized.isEmpty()) {
			return 0.0D;
		}

		try {
			return Double.parseDouble(normalized);
		} catch (NumberFormatException ignored) {
			return 0.0D;
		}
	}

	public static @Nullable String firstNumber(String line) {
		Matcher matcher = NUMBER.matcher(line);
		return matcher.find() ? matcher.group(1) : null;
	}

	public static void highlightSlot(DrawContext context, Slot slot, int color) {
		context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color);
	}

	public static void outlineSlot(DrawContext context, Slot slot, int color) {
		context.fill(slot.x, slot.y, slot.x + 16, slot.y + 1, color);
		context.fill(slot.x, slot.y + 15, slot.x + 16, slot.y + 16, color);
		context.fill(slot.x, slot.y, slot.x + 1, slot.y + 16, color);
		context.fill(slot.x + 15, slot.y, slot.x + 16, slot.y + 16, color);
	}

	public static int color(ChromaColour colour, int fallbackArgb) {
		if (colour == null) {
			return fallbackArgb;
		}

		int rgb = colour.getEffectiveColourRGB();
		int alpha = Math.max(90, Math.min(220, colour.getAlpha()));
		return (alpha << 24) | (rgb & 0x00FFFFFF);
	}

	public static int color(int red, int green, int blue, int alpha) {
		return ((alpha & 0xFF) << 24)
			| ((red & 0xFF) << 16)
			| ((green & 0xFF) << 8)
			| (blue & 0xFF);
	}

	public static int lerpColor(int from, int to, double percentage) {
		double p = Math.max(0.0D, Math.min(1.0D, percentage));
		int alpha = lerp((from >>> 24) & 0xFF, (to >>> 24) & 0xFF, p);
		int red = lerp((from >>> 16) & 0xFF, (to >>> 16) & 0xFF, p);
		int green = lerp((from >>> 8) & 0xFF, (to >>> 8) & 0xFF, p);
		int blue = lerp(from & 0xFF, to & 0xFF, p);
		return color(red, green, blue, alpha);
	}

	private static int lerp(int from, int to, double percentage) {
		return (int) Math.round(from + (to - from) * percentage);
	}

	public static void drawPanel(DrawContext context, TextRenderer textRenderer, int x, int y, List<String> lines) {
		if (lines.isEmpty()) {
			return;
		}

		List<Text> textLines = new ArrayList<>(lines.size());
		for (String line : lines) {
			textLines.add(legacyText(line));
		}
		drawTextPanel(context, textRenderer, x, y, textLines);
	}

	public static void drawPanelClamped(DrawContext context, TextRenderer textRenderer, int x, int y, List<String> lines) {
		if (lines.isEmpty()) {
			return;
		}

		List<Text> textLines = new ArrayList<>(lines.size());
		for (String line : lines) {
			textLines.add(legacyText(line));
		}
		int width = textPanelWidth(textRenderer, textLines);
		drawTextPanel(context, textRenderer, clampPanelX(context, x, width), clampPanelY(context, y, textPanelHeight(textLines.size())), textLines);
	}

	public static void drawTextPanel(DrawContext context, TextRenderer textRenderer, int x, int y, List<Text> lines) {
		if (lines.isEmpty()) {
			return;
		}

		int width = textPanelWidth(textRenderer, lines);
		int height = textPanelHeight(lines.size());
		context.fill(x + 2, y + 2, x + width + 2, y + height + 2, 0x77000000);
		context.fill(x, y, x + width, y + height, 0xDD101725);
		context.fill(x, y, x + width, y + 1, 0xFF60A8FF);
		for (int index = 0; index < lines.size(); index++) {
			context.drawTextWithShadow(textRenderer, lines.get(index), x + 6, y + 5 + index * 11, 0xFFFFFFFF);
		}
	}

	public static void drawLegacyLinesClamped(DrawContext context, TextRenderer textRenderer, int x, int y, List<String> lines) {
		if (lines.isEmpty()) {
			return;
		}

		List<Text> textLines = new ArrayList<>(lines.size());
		for (String line : lines) {
			textLines.add(legacyText("§f" + line));
		}
		int width = plainLinesWidth(textRenderer, textLines);
		int height = plainLinesHeight(textLines.size());
		int drawX = clampPanelX(context, x, width);
		int drawY = clampPanelY(context, y, height);
		for (int index = 0; index < textLines.size(); index++) {
			context.drawTextWithShadow(textRenderer, textLines.get(index), drawX + 1, drawY + 1 + index * 10, 0xFFFFFFFF);
		}
	}

	public static int textPanelWidth(TextRenderer textRenderer, List<Text> lines) {
		int width = 0;
		for (Text line : lines) {
			width = Math.max(width, textRenderer.getWidth(line));
		}
		return width + 12;
	}

	public static int textPanelHeight(int lineCount) {
		return lineCount * 11 + 8;
	}

	public static int plainLinesWidth(TextRenderer textRenderer, List<Text> lines) {
		int width = 0;
		for (Text line : lines) {
			width = Math.max(width, textRenderer.getWidth(line));
		}
		return Math.max(1, width + 2);
	}

	public static int plainLinesHeight(int lineCount) {
		return Math.max(1, lineCount * 10 + 2);
	}

	public static int clampPanelX(DrawContext context, int x, int width) {
		int maxX = Math.max(4, context.getScaledWindowWidth() - width - 4);
		return Math.max(4, Math.min(x, maxX));
	}

	public static int clampPanelY(DrawContext context, int y, int height) {
		int maxY = Math.max(4, context.getScaledWindowHeight() - height - 4);
		return Math.max(4, Math.min(y, maxY));
	}

	public static void drawPriceHistoryButton(DrawContext context, Slot slot) {
		context.drawItem(PRICE_HISTORY_ICON, slot.x, slot.y, slot.id);
	}

	public static List<Text> priceHistoryTooltip(String site, @Nullable String subject) {
		List<Text> lines = new ArrayList<>();
		lines.add(legacyText("§bPrice History"));
		lines.add(legacyText("§8(EMUtils)"));
		lines.add(Text.literal(""));
		lines.add(legacyText("§7Click here to open"));
		lines.add(legacyText("§7the price history"));
		if (subject != null && !subject.isBlank()) {
			lines.add(legacyText("§7of §e" + subject));
		}
		lines.add(legacyText("§7on §c" + site));
		return lines;
	}

	private static ItemStack createPriceHistoryIcon() {
		ItemStack stack = new ItemStack(Items.PAPER);
		stack.set(DataComponentTypes.CUSTOM_NAME, legacyText("§bPrice History"));
		stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
			legacyText("§7Open the current item's"),
			legacyText("§7price history page.")
		)));
		return stack;
	}

	public static Text legacyText(String legacy) {
		if (legacy == null || legacy.isEmpty()) {
			return Text.literal("");
		}

		MutableText result = Text.empty();
		List<Formatting> active = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		for (int index = 0; index < legacy.length(); index++) {
			char character = legacy.charAt(index);
			if (character == '\u00a7' && index + 1 < legacy.length()) {
				appendLegacyRun(result, current, active);
				Formatting formatting = Formatting.byCode(legacy.charAt(++index));
				if (formatting == null) {
					continue;
				}
				if (formatting == Formatting.RESET) {
					active.clear();
				} else if (formatting.isColor()) {
					active.clear();
					active.add(formatting);
				} else if (!active.contains(formatting)) {
					active.add(formatting);
				}
			} else {
				current.append(character);
			}
		}
		appendLegacyRun(result, current, active);
		return result;
	}

	private static void appendLegacyRun(MutableText result, StringBuilder current, List<Formatting> active) {
		if (current.isEmpty()) {
			return;
		}

		MutableText run = Text.literal(current.toString());
		if (!active.isEmpty()) {
			run.formatted(active.toArray(new Formatting[0]));
		}
		result.append(run);
		current.setLength(0);
	}

	public static void chat(String message) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null && client.inGameHud != null) {
			client.inGameHud.getChatHud().addMessage(EmUtilsChatPrefix.chat(message), null, null);
		}
	}

	public static void chat(Text message) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null && client.inGameHud != null) {
			client.inGameHud.getChatHud().addMessage(EmUtilsChatPrefix.chat(message), null, null);
		}
	}

	public static void copyToClipboard(String value) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null && client.keyboard != null) {
			client.keyboard.setClipboard(value);
		}
	}

	public static void sendCommand(String commandWithoutSlash) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null && client.player != null && client.player.networkHandler != null) {
			client.player.networkHandler.sendChatCommand(commandWithoutSlash);
		}
	}

	public static void openUrl(String url) {
		Util.getOperatingSystem().open(url);
	}

	public static int countInPlayerInventory(ScreenHandler handler, String itemId) {
		if (itemId == null || itemId.isBlank()) {
			return 0;
		}

		int count = 0;
		for (Slot slot : handler.slots) {
			if (!(slot.inventory instanceof PlayerInventory) || !slot.hasStack()) {
				continue;
			}
			String candidate = net.emutils.client.emskyblock.pricing.bazaar.SkyblockItemIds.resolveItemId(slot.getStack());
			if (itemId.equals(candidate)) {
				count += slot.getStack().getCount();
			}
		}
		return count;
	}

	public static boolean skyblockFeatureEnabled(MinecraftClient client) {
		return net.emutils.client.emskyblock.config.EMSkyblockSettings.skyblockEnabled()
			&& net.emutils.client.emskyblock.context.SkyblockFeatures.inSkyBlock(client);
	}

	public static net.emutils.client.emskyblock.config.EMSkyblockConfig config() {
		try {
			return EMUtilsClient.emSkyblockConfig();
		} catch (IllegalStateException ignored) {
			return null;
		}
	}
}
