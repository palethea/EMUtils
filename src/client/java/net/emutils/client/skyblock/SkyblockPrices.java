package net.emutils.client.skyblock;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.emutils.client.skyblock.auction.AuctionPriceFetcher;
import net.emutils.client.skyblock.auction.AuctionProductPrice;
import net.emutils.client.skyblock.bazaar.BazaarPriceFetcher;
import net.emutils.client.skyblock.bazaar.BazaarProductPrice;
import net.emutils.client.skyblock.bazaar.SkyblockItemIds;
import net.emutils.client.skyblock.eiv.SkyblockItemAttributes;
import net.emutils.client.skyblock.npc.NpcPriceFetcher;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class SkyblockPrices {
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

	private final BazaarPriceFetcher bazaar = new BazaarPriceFetcher();
	private final AuctionPriceFetcher auction = new AuctionPriceFetcher();
	private final NpcPriceFetcher npc = new NpcPriceFetcher();

	public void tick(@Nullable MinecraftClient client) {
		bazaar.tick(client);
		auction.tick(client);
		npc.tick(client);
	}

	public void fetchNow(@Nullable MinecraftClient client) {
		bazaar.fetchNow(client);
		auction.fetchNow(client);
		npc.fetchNow(client);
	}

	public void requestImmediateFetch() {
		bazaar.requestImmediateFetch();
		auction.requestImmediateFetch();
		npc.requestImmediateFetch();
	}

	public void clear() {
		bazaar.clear();
		auction.clear();
		npc.clear();
	}

	public BazaarPriceFetcher bazaar() {
		return bazaar;
	}

	public AuctionPriceFetcher auction() {
		return auction;
	}

	public NpcPriceFetcher npc() {
		return npc;
	}

	public PriceResult price(ItemStack stack) {
		return price(SkyblockItemAttributes.itemId(stack), stack);
	}

	public PriceResult price(String itemId) {
		return price(itemId, ItemStack.EMPTY);
	}

	public PriceResult price(@Nullable String itemId, ItemStack stack) {
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
			PriceResult bazaarPrice = bazaarPrice(candidate);
			if (bazaarPrice.known()) {
				return bazaarPrice;
			}
		}

		if (!stack.isEmpty()) {
			Optional<AuctionProductPrice> stackAuction = auction.price(stack);
			if (stackAuction.isPresent() && stackAuction.get().lowestBin() > 0.0D) {
				return PriceResult.of(stackAuction.get().lowestBin(), Source.AUCTION);
			}
		}

		for (String candidate : lookupIds(itemId)) {
			PriceResult auctionPrice = auctionPrice(candidate);
			if (auctionPrice.known()) {
				return auctionPrice;
			}
		}

		for (String candidate : lookupIds(itemId)) {
			PriceResult npcPrice = npcPrice(candidate);
			if (npcPrice.known()) {
				return npcPrice;
			}
		}

		return PriceResult.unknown();
	}

	public PriceResult baseItemAuctionPrice(String itemId) {
		if (itemId == null || itemId.isBlank()) {
			return PriceResult.unknown();
		}

		for (String candidate : lookupIds(itemId)) {
			PriceResult auctionPrice = auctionPrice(candidate);
			if (auctionPrice.known()) {
				return auctionPrice;
			}
		}

		for (String candidate : lookupIds(itemId)) {
			PriceResult npcPrice = npcPrice(candidate);
			if (npcPrice.known()) {
				return npcPrice;
			}
		}

		return PriceResult.unknown();
	}

	public boolean isBazaarItem(String itemId) {
		for (String candidate : lookupIds(itemId)) {
			if (bazaar.price(candidate).isPresent()) {
				return true;
			}
		}

		return false;
	}

	private PriceResult bazaarPrice(String itemId) {
		Optional<BazaarProductPrice> bazaarPrice = bazaar.price(itemId);
		if (bazaarPrice.isEmpty()) {
			return PriceResult.unknown();
		}

		double instantBuy = bazaarPrice.get().instantBuyPrice();
		if (instantBuy <= 0.0D) {
			instantBuy = bazaarPrice.get().buyPrice();
		}

		return instantBuy > 0.0D ? PriceResult.of(instantBuy, Source.BAZAAR) : PriceResult.unknown();
	}

	private PriceResult auctionPrice(String itemId) {
		Optional<AuctionProductPrice> auctionPrice = auction.price(itemId);
		if (auctionPrice.isEmpty() || auctionPrice.get().lowestBin() <= 0.0D) {
			return PriceResult.unknown();
		}

		return PriceResult.of(auctionPrice.get().lowestBin(), Source.AUCTION);
	}

	private PriceResult npcPrice(String itemId) {
		Optional<Double> npcPrice = npc.npcSellPrice(itemId);
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
