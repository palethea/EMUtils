package net.emutils.client.skyblock.eiv;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.skyblock.auction.AuctionProductPrice;
import net.emutils.client.skyblock.bazaar.BazaarProductPrice;
import net.minecraft.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class EstimatedItemValuePriceService {
	public enum Source {
		BAZAAR,
		AUCTION,
		NPC
	}

	public record PriceResult(double amount, Source source, boolean known) {
		public static PriceResult unknown() {
			return new PriceResult(0.0D, Source.NPC, false);
		}

		public static PriceResult of(double amount, Source source) {
			return new PriceResult(amount, source, amount > 0.0D);
		}
	}

	private EstimatedItemValuePriceService() {
	}

	public static PriceResult price(ItemStack stack) {
		return price(SkyblockItemAttributes.itemId(stack), stack);
	}

	public static PriceResult price(@Nullable String itemId, ItemStack stack) {
		if (itemId == null || itemId.isBlank()) {
			if (stack.isEmpty()) {
				return PriceResult.unknown();
			}

			itemId = SkyblockItemAttributes.itemId(stack);
		}

		if (itemId == null || itemId.isBlank()) {
			return PriceResult.unknown();
		}

		for (String candidate : lookupIds(itemId)) {
			PriceResult bazaar = bazaarPrice(candidate);
			if (bazaar.known()) {
				return bazaar;
			}
		}

		if (!stack.isEmpty()) {
			Optional<AuctionProductPrice> stackAuction = EMUtilsClient.auctionPrices().price(stack);
			if (stackAuction.isPresent()) {
				double lowestBin = stackAuction.get().lowestBin();
				if (lowestBin > 0.0D) {
					return PriceResult.of(lowestBin, Source.AUCTION);
				}
			}
		}

		for (String candidate : lookupIds(itemId)) {
			PriceResult auction = auctionPrice(candidate);
			if (auction.known()) {
				return auction;
			}
		}

		for (String candidate : lookupIds(itemId)) {
			PriceResult npc = npcPrice(candidate);
			if (npc.known()) {
				return npc;
			}
		}

		return PriceResult.unknown();
	}

	public static PriceResult price(String itemId) {
		return price(itemId, ItemStack.EMPTY);
	}

	public static boolean isBazaarItem(String itemId) {
		for (String candidate : lookupIds(itemId)) {
			if (EMUtilsClient.bazaarPrices().price(candidate).isPresent()) {
				return true;
			}
		}

		return false;
	}

	private static PriceResult bazaarPrice(String itemId) {
		Optional<BazaarProductPrice> bazaarPrice = EMUtilsClient.bazaarPrices().price(itemId);
		if (bazaarPrice.isEmpty()) {
			return PriceResult.unknown();
		}

		double instantBuy = bazaarPrice.get().instantBuyPrice();
		if (instantBuy <= 0.0D) {
			instantBuy = bazaarPrice.get().buyPrice();
		}

		if (instantBuy > 0.0D) {
			return PriceResult.of(instantBuy, Source.BAZAAR);
		}

		return PriceResult.unknown();
	}

	private static PriceResult auctionPrice(String itemId) {
		Optional<AuctionProductPrice> auctionPrice = EMUtilsClient.auctionPrices().price(itemId);
		if (auctionPrice.isEmpty()) {
			return PriceResult.unknown();
		}

		double lowestBin = auctionPrice.get().lowestBin();
		if (lowestBin > 0.0D) {
			return PriceResult.of(lowestBin, Source.AUCTION);
		}

		return PriceResult.unknown();
	}

	private static PriceResult npcPrice(String itemId) {
		Optional<Double> npcPrice = EMUtilsClient.npcPrices().npcSellPrice(itemId);
		if (npcPrice.isPresent() && npcPrice.get() > 0.0D) {
			return PriceResult.of(npcPrice.get(), Source.NPC);
		}

		return PriceResult.unknown();
	}

	private static List<String> lookupIds(String itemId) {
		Set<String> candidates = new LinkedHashSet<>();
		candidates.add(itemId);

		if (itemId.startsWith("ENCHANTMENT_")) {
			String body = itemId.substring("ENCHANTMENT_".length());
			int lastUnderscore = body.lastIndexOf('_');
			if (lastUnderscore > 0) {
				String enchant = body.substring(0, lastUnderscore).replace('_', '-');
				String level = body.substring(lastUnderscore + 1);
				candidates.add("ENCHANTED_BOOK-" + enchant + "-" + level);
			}
		}

		return List.copyOf(candidates);
	}
}
