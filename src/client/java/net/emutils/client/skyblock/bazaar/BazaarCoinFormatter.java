package net.emutils.client.skyblock.bazaar;

import java.util.Locale;

public final class BazaarCoinFormatter {
	private BazaarCoinFormatter() {
	}

	public static String format(double amount) {
		if (!Double.isFinite(amount) || amount <= 0.0D) {
			return "0";
		}

		if (amount >= 10_000_000.0D) {
			return String.format(Locale.US, "%,.0f", amount);
		}

		if (amount >= 1_000.0D) {
			return String.format(Locale.US, "%,.1f", amount);
		}

		if (amount >= 10.0D) {
			return String.format(Locale.US, "%.1f", amount);
		}

		return String.format(Locale.US, "%.2f", amount);
	}
}
