package net.emutils.client.emskyblock.features.inventory.estimateditemvalue;

import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public final class EivCoinFormat {
	private static final NavigableMap<Long, String> SUFFIXES = new TreeMap<>();

	static {
		SUFFIXES.put(1_000L, "k");
		SUFFIXES.put(1_000_000L, "M");
		SUFFIXES.put(1_000_000_000L, "B");
		SUFFIXES.put(1_000_000_000_000L, "T");
	}

	private EivCoinFormat() {
	}

	public static String compact(double amount) {
		if (!Double.isFinite(amount) || amount <= 0.0D) {
			return "0";
		}

		long value = (long) amount;
		if (value < 1_000L) {
			return Long.toString(value);
		}

		Map.Entry<Long, String> suffixEntry = SUFFIXES.floorEntry(value);
		if (suffixEntry == null) {
			return Long.toString(value);
		}

		long divideBy = suffixEntry.getKey();
		String suffix = suffixEntry.getValue();
		long truncated = value / (divideBy / 10L);
		long truncatedAt = switch (suffix) {
			case "M" -> 1_000L;
			case "B" -> 1_000_000L;
			default -> 100L;
		};
		boolean hasDecimal = truncated < truncatedAt && truncated % 10L != 0L;
		if (hasDecimal) {
			return String.format(Locale.US, "%.1f%s", truncated / 10.0D, suffix);
		}

		return (truncated / 10L) + suffix;
	}

	public static String exact(double amount) {
		if (!Double.isFinite(amount) || amount <= 0.0D) {
			return "0";
		}

		return String.format(Locale.US, "%,.0f", amount);
	}

	public static String coin(double amount, boolean exact) {
		return "§6" + (exact ? exact(amount) : compact(amount));
	}

	public static String coinBracket(double amount, boolean exact) {
		return "§7(§6" + (exact ? exact(amount) : compact(amount)) + "§7)";
	}

	public static String sectionTotal(double amount, boolean exact) {
		return coin(amount, exact);
	}

	public static String hudCoinBracket(double amount) {
		return coinBracket(amount, false);
	}

	public static String hudSectionTotal(double amount) {
		return coin(amount, false);
	}
}
