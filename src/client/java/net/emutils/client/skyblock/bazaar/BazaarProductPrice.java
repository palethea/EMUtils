package net.emutils.client.skyblock.bazaar;

public record BazaarProductPrice(
	double buyPrice,
	double sellPrice,
	double instantBuyPrice,
	double instantSellPrice
) {
}
