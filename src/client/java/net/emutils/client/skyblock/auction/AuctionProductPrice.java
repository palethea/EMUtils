package net.emutils.client.skyblock.auction;

public record AuctionProductPrice(
	double lowestBin,
	double average24h
) {
	public boolean hasAny() {
		return lowestBin > 0.0D || average24h > 0.0D;
	}
}
