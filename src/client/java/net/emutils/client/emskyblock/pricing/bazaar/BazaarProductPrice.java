package net.emutils.client.emskyblock.pricing.bazaar;

public record BazaarProductPrice(
	double buyPrice,
	double sellPrice,
	double instantBuyPrice,
	double instantSellPrice,
	double average24h
) {
	public boolean hasAny() {
		return buyPrice > 0.0D
			|| sellPrice > 0.0D
			|| instantBuyPrice > 0.0D
			|| instantSellPrice > 0.0D
			|| average24h > 0.0D;
	}
}
