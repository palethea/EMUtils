package net.emutils.client.emskyblock.features.inventory.auctionhouse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emskyblock.config.EMSkyblockConfig;
import net.emutils.client.emskyblock.features.inventory.common.InventoryFeatureUtils;
import net.emutils.client.emskyblock.features.inventory.estimateditemvalue.EstimatedItemValueCalculator;
import net.emutils.client.emskyblock.features.inventory.estimateditemvalue.EstimatedItemValueResult;
import net.emutils.client.emskyblock.features.inventory.estimateditemvalue.SkyblockItemAttributes;
import net.emutils.client.emskyblock.pricing.auction.AuctionProductPrice;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jspecify.annotations.Nullable;

public final class AuctionHouseFeatures {
	private static final Pattern AUCTION_PRICE = Pattern.compile("(?:Buy it now|Starting bid|Top bid):\\s*([0-9,]+) coins", Pattern.CASE_INSENSITIVE);
	private static final Pattern AUCTION_SEARCH = Pattern.compile("Auctions: \"?(.*?)\"?$");
	private static final Pattern OUTBID = Pattern.compile("\\[Auction].*outbid you by.*CLICK", Pattern.CASE_INSENSITIVE);

	private static final Map<Integer, Long> priceDiffBySlot = new HashMap<>();
	private static long bestDiff;
	private static long worstDiff;
	private static String lastComparisonTitle = "";
	private static int lastComparisonSlotCount = -1;
	private static String lastComparisonSignature = "";
	private static String lastAutoCopiedItem = "";

	private AuctionHouseFeatures() {
	}

	public static void onInventoryOpen(String title, ScreenHandler handler) {
		lastComparisonTitle = "";
		lastComparisonSlotCount = -1;
		lastComparisonSignature = "";
		priceDiffBySlot.clear();
		if (!InventoryFeatureUtils.titleMatches(title, "Create BIN Auction")) {
			lastAutoCopiedItem = "";
		}
	}

	public static void onInventoryClose() {
		priceDiffBySlot.clear();
		lastComparisonTitle = "";
		lastComparisonSlotCount = -1;
		lastComparisonSignature = "";
		lastAutoCopiedItem = "";
	}

	public static void onChat(Text message) {
		EMSkyblockConfig.AuctionHouse config = config();
		if (config == null || !config.auctionOutbid) {
			return;
		}

		String stripped = InventoryFeatureUtils.strip(message);
		if (!OUTBID.matcher(stripped).matches()) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.inGameHud == null) {
			return;
		}

		client.inGameHud.setTitle(Text.literal("You have been outbid!").formatted(Formatting.RED));
		client.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0F, 1.0F));
	}

	public static void renderScreenOverlay(DrawContext context, ScreenHandler handler, String title, int screenX, int screenY) {
		preparePriceComparison(title, handler);
		handleAutoCopyUnderbid(title, handler);
	}

	public static void drawSlotOverlay(DrawContext context, ScreenHandler handler, Slot slot, String title) {
		if (!InventoryFeatureUtils.topInventorySlot(slot)) {
			return;
		}

		EMSkyblockConfig.AuctionHouse config = config();
		if (config == null) {
			return;
		}

		if (config.openPriceWebsite && slot.id == 8 && auctionSearchTerm(title) != null) {
			InventoryFeatureUtils.drawPriceHistoryButton(context, slot);
			return;
		}

		if (!slot.hasStack()) {
			return;
		}

		if (config.priceComparison.enabled) {
			Long diff = priceDiffBySlot.get(slot.id);
			if (diff != null) {
				int color = comparisonColor(diff, config.priceComparison);
				InventoryFeatureUtils.highlightSlot(context, slot, color);
			}
		}

		if (InventoryFeatureUtils.titleMatches(title, "Manage Auctions")) {
			List<String> lore = InventoryFeatureUtils.strippedLore(slot.getStack());
			boolean sold = lore.contains("Status: Sold!");
			boolean expired = lore.contains("Status: Expired!");
			if (config.highlightAuctions) {
				if (sold) {
					InventoryFeatureUtils.highlightSlot(context, slot, InventoryFeatureUtils.color(config.soldColor, 0x9955FF55));
					return;
				}
				if (expired) {
					InventoryFeatureUtils.highlightSlot(context, slot, InventoryFeatureUtils.color(config.expiredColor, 0x99FF5555));
					return;
				}
			}

			if (!sold && !expired && config.highlightAuctionsUnderbid) {
				long listed = buyItNowPrice(slot.getStack());
				double lowestBin = lowestBin(slot.getStack());
				if (listed > 0L && lowestBin > 0.0D && listed > lowestBin) {
					InventoryFeatureUtils.highlightSlot(context, slot, InventoryFeatureUtils.color(config.underbidColor, 0x99FFAA00));
				}
			}
		}
	}

	public static List<Text> appendTooltip(String title, Slot slot, List<Text> tooltip) {
		EMSkyblockConfig.AuctionHouse config = config();
		if (config == null || slot == null) {
			return tooltip;
		}

		String search = auctionSearchTerm(title);
		if (config.openPriceWebsite && slot.id == 8 && search != null) {
			return InventoryFeatureUtils.priceHistoryTooltip("sky.coflnet.com", search);
		}

		if (!config.priceComparison.enabled || !isAuctionBrowser(title)) {
			return tooltip;
		}

		Long diff = priceDiffBySlot.get(slot.id);
		if (diff == null) {
			return tooltip;
		}

		ArrayList<Text> result = new ArrayList<>(tooltip);
		result.add(Text.literal(""));
		if (diff >= 0) {
			result.add(Text.literal("This item is ").formatted(Formatting.GREEN)
				.append(Text.literal(formatCoins(diff) + " coins").formatted(Formatting.GOLD))
				.append(Text.literal(" cheaper").formatted(Formatting.GREEN)));
			result.add(Text.literal("than the estimated item value.").formatted(Formatting.GREEN));
		} else {
			result.add(Text.literal("This item is ").formatted(Formatting.RED)
				.append(Text.literal(formatCoins(-diff) + " coins").formatted(Formatting.GOLD))
				.append(Text.literal(" more expensive").formatted(Formatting.RED)));
			result.add(Text.literal("than the estimated item value.").formatted(Formatting.RED));
		}
		return result;
	}

	public static boolean handleKeyPressed(ScreenHandler handler, @Nullable Slot focusedSlot, KeyInput input, String title) {
		EMSkyblockConfig.AuctionHouse config = config();
		if (config == null || config.copyUnderbidKeybind == org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN || input.key() != config.copyUnderbidKeybind) {
			return false;
		}
		if (focusedSlot == null || !focusedSlot.hasStack() || !allowedCopyInventory(title)) {
			return false;
		}

		long price = listedPrice(focusedSlot.getStack());
		if (price <= 1L) {
			return false;
		}

		copyUnderbid(price);
		return true;
	}

	public static boolean guardSlotClick(String title, Slot slot) {
		if (slot == null || !InventoryFeatureUtils.topInventorySlot(slot)) {
			return false;
		}
		EMSkyblockConfig.AuctionHouse config = config();
		if (config == null || !config.openPriceWebsite || slot.id != 8) {
			return false;
		}

		String search = auctionSearchTerm(title);
		if (search == null) {
			return false;
		}

		String encoded = URLEncoder.encode(search, StandardCharsets.UTF_8).replace("+", "%20");
		InventoryFeatureUtils.openUrl("https://sky.coflnet.com/api/mod/open/" + encoded);
		return true;
	}

	private static void preparePriceComparison(String title, ScreenHandler handler) {
		if (!isAuctionBrowser(title)) {
			priceDiffBySlot.clear();
			lastComparisonTitle = "";
			lastComparisonSlotCount = -1;
			lastComparisonSignature = "";
			return;
		}

		EMSkyblockConfig.AuctionHouse config = config();
		if (config == null || !config.priceComparison.enabled) {
			priceDiffBySlot.clear();
			lastComparisonSignature = "";
			return;
		}

		String signature = comparisonSignature(handler);
		if (title.equals(lastComparisonTitle)
			&& handler.slots.size() == lastComparisonSlotCount
			&& signature.equals(lastComparisonSignature)) {
			return;
		}

		lastComparisonTitle = title;
		lastComparisonSlotCount = handler.slots.size();
		lastComparisonSignature = signature;
		priceDiffBySlot.clear();
		bestDiff = 0L;
		worstDiff = 0L;
		for (Slot slot : handler.slots) {
			if (!InventoryFeatureUtils.topInventorySlot(slot) || !slot.hasStack()) {
				continue;
			}

			long listed = listedPrice(slot.getStack());
			if (listed <= 0L) {
				continue;
			}

			long estimated = Math.round(estimatedValue(slot.getStack()));
			if (estimated <= 0L) {
				continue;
			}

			long diff = estimated - listed;
			priceDiffBySlot.put(slot.id, diff);
			if (diff >= 0L) {
				bestDiff = Math.max(bestDiff, diff);
			} else {
				worstDiff = Math.min(worstDiff, diff);
			}
		}
	}

	private static void handleAutoCopyUnderbid(String title, ScreenHandler handler) {
		EMSkyblockConfig.AuctionHouse config = config();
		if (config == null || !config.autoCopyUnderbidPrice || !InventoryFeatureUtils.titleMatches(title, "Create BIN Auction")) {
			return;
		}

		Slot slot = slot(handler, 13);
		if (slot == null || !slot.hasStack()) {
			return;
		}

		String itemId = SkyblockItemAttributes.itemId(slot.getStack());
		if (itemId == null || itemId.isBlank()) {
			return;
		}

		if (itemId.equals(lastAutoCopiedItem)) {
			return;
		}
		lastAutoCopiedItem = itemId;

		double unitPrice = EMUtilsClient.skyblockPrices().price(itemId, slot.getStack()).amount();
		if (unitPrice <= 0.0D) {
			return;
		}
		long totalPrice = Math.round(unitPrice * Math.max(1, slot.getStack().getCount()));
		copyUnderbid(totalPrice);
	}

	private static String comparisonSignature(ScreenHandler handler) {
		StringBuilder builder = new StringBuilder(handler.slots.size() * 16);
		for (Slot slot : handler.slots) {
			if (!InventoryFeatureUtils.topInventorySlot(slot) || !slot.hasStack()) {
				continue;
			}

			ItemStack stack = slot.getStack();
			builder.append(slot.id).append(':')
				.append(stack.getCount()).append(':')
				.append(InventoryFeatureUtils.itemName(stack)).append(':')
				.append(InventoryFeatureUtils.strippedLore(stack).hashCode())
				.append('|');
		}
		return builder.toString();
	}

	private static boolean isAuctionBrowser(String title) {
		String clean = InventoryFeatureUtils.strip(title);
		return clean.startsWith("Auctions") || clean.startsWith("Cosmetics Browser");
	}

	private static boolean allowedCopyInventory(String title) {
		String clean = InventoryFeatureUtils.strip(title);
		return clean.equals("Auctions Browser") || clean.equals("Manage Auctions") || clean.startsWith("Auctions: ");
	}

	private static long listedPrice(ItemStack stack) {
		for (String line : InventoryFeatureUtils.strippedLore(stack)) {
			Matcher matcher = AUCTION_PRICE.matcher(line);
			if (matcher.find()) {
				return InventoryFeatureUtils.parseLong(matcher.group(1));
			}
		}
		return 0L;
	}

	private static long buyItNowPrice(ItemStack stack) {
		for (String line : InventoryFeatureUtils.strippedLore(stack)) {
			if (!line.toLowerCase(java.util.Locale.ROOT).startsWith("buy it now:")) {
				continue;
			}
			Matcher matcher = AUCTION_PRICE.matcher(line);
			if (matcher.find()) {
				return InventoryFeatureUtils.parseLong(matcher.group(1));
			}
		}
		return 0L;
	}

	private static double lowestBin(ItemStack stack) {
		AuctionProductPrice price = EMUtilsClient.skyblockPrices().auction()
			.price(stack)
			.orElse(null);
		return price == null ? 0.0D : price.lowestBin();
	}

	@Nullable
	private static String auctionSearchTerm(String title) {
		Matcher matcher = AUCTION_SEARCH.matcher(InventoryFeatureUtils.strip(title));
		if (!matcher.matches()) {
			return null;
		}
		String search = matcher.group(1).replace("\"", "").trim();
		return search.isEmpty() ? null : search;
	}

	private static double estimatedValue(ItemStack stack) {
		EstimatedItemValueResult result = EstimatedItemValueCalculator.calculate(stack, InventoryFeatureUtils.lore(stack));
		return result == null || result.isEmpty() ? 0.0D : result.totalValue();
	}

	private static int comparisonColor(long diff, EMSkyblockConfig.AuctionHousePriceComparison config) {
		if (diff >= 0L) {
			int good = InventoryFeatureUtils.color(config.good, 0x9955FF55);
			int veryGood = InventoryFeatureUtils.color(config.veryGood, 0xAA008B00);
			double percentage = bestDiff <= 0L ? 0.0D : diff / (double) bestDiff;
			return InventoryFeatureUtils.lerpColor(good, veryGood, percentage);
		}

		int bad = InventoryFeatureUtils.color(config.bad, 0x99FFFF55);
		int veryBad = InventoryFeatureUtils.color(config.veryBad, 0xAAE12B1E);
		double percentage = worstDiff >= 0L ? 0.0D : -diff / (double) -worstDiff;
		return InventoryFeatureUtils.lerpColor(bad, veryBad, percentage);
	}

	private static void copyUnderbid(long price) {
		long underbid = Math.max(1L, price - 1L);
		InventoryFeatureUtils.copyToClipboard(Long.toString(underbid));
		InventoryFeatureUtils.chat("Copied " + formatCoins(underbid) + " to clipboard. (Copy Underbid Price)");
	}

	@Nullable
	private static Slot slot(ScreenHandler handler, int slotId) {
		for (Slot slot : handler.slots) {
			if (slot.id == slotId) {
				return slot;
			}
		}
		return null;
	}

	private static String formatCoins(long coins) {
		return String.format("%,d", coins);
	}

	private static EMSkyblockConfig.AuctionHouse config() {
		EMSkyblockConfig config = InventoryFeatureUtils.config();
		return config == null ? null : config.inventory.auctionHouse;
	}
}
