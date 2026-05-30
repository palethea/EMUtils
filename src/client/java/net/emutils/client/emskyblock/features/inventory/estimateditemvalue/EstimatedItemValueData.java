package net.emutils.client.emskyblock.features.inventory.estimateditemvalue;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.emutils.client.EMUtilsClient;
import org.jspecify.annotations.Nullable;

public final class EstimatedItemValueData {
	private static final Gson GSON = new Gson();
	private static volatile LoadedData cache = LoadedData.empty();

	private EstimatedItemValueData() {
	}

	public static void load() {
		try (InputStream stream = EstimatedItemValueData.class.getResourceAsStream("/data/emutils/skyblock/estimated_item_value.json")) {
			if (stream == null) {
				EMUtilsClient.LOGGER.warn("Missing estimated item value metadata.");
				cache = LoadedData.empty();
				return;
			}

			MetadataFile file = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), MetadataFile.class);
			cache = LoadedData.from(file);
		} catch (IOException | JsonParseException exception) {
			EMUtilsClient.LOGGER.warn("Failed to load estimated item value metadata.", exception);
			cache = LoadedData.empty();
		}
	}

	public static LoadedData data() {
		return cache;
	}

	public record ReforgeEntry(String modifier, String name, String stoneId, Map<String, Long> costs) {
		@Nullable
		public Long costFor(SkyblockItemRarity rarity) {
			if (costs == null || rarity == null) {
				return null;
			}

			return costs.get(rarity.name());
		}
	}

	public record EndCapEntry(
		@SerializedName("required_level") int requiredLevel,
		@SerializedName("endcap_item") String endcapItem
	) {
	}

	public record AlwaysActiveEnchant(int level, List<String> items) {
	}

	public record LoadedData(
		Map<String, ReforgeEntry> reforgesByModifier,
		Map<String, AlwaysActiveEnchant> alwaysActiveEnchants,
		List<String> onlyTierOnePrices,
		List<String> onlyTierFivePrices,
		Map<String, String> renamedEnchants,
		Map<String, List<EndCapEntry>> endcapEnchants
	) {
		public static LoadedData empty() {
			return new LoadedData(
				Map.of(),
				Map.of(),
				List.of(),
				List.of(),
				Map.of(),
				Map.of()
			);
		}

		public static LoadedData from(@Nullable MetadataFile file) {
			if (file == null) {
				return empty();
			}

			Map<String, ReforgeEntry> reforges = new HashMap<>();
			if (file.reforges != null) {
				for (ReforgeEntry entry : file.reforges) {
					if (entry.modifier == null || entry.modifier.isBlank()) {
						continue;
					}

					reforges.put(entry.modifier.toLowerCase(Locale.ROOT), entry);
				}
			}

			Map<String, AlwaysActiveEnchant> alwaysActive = new HashMap<>();
			if (file.valueCalculation != null && file.valueCalculation.alwaysActiveEnchants != null) {
				for (Map.Entry<String, AlwaysActiveEnchant> entry : file.valueCalculation.alwaysActiveEnchants.entrySet()) {
					alwaysActive.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
				}
			}

			return new LoadedData(
				Map.copyOf(reforges),
				Map.copyOf(alwaysActive),
				file.valueCalculation == null || file.valueCalculation.onlyTierOnePrices == null
					? List.of()
					: List.copyOf(file.valueCalculation.onlyTierOnePrices),
				file.valueCalculation == null || file.valueCalculation.onlyTierFivePrices == null
					? List.of()
					: List.copyOf(file.valueCalculation.onlyTierFivePrices),
				file.valueCalculation == null || file.valueCalculation.renamedEnchants == null
					? Map.of()
					: Map.copyOf(file.valueCalculation.renamedEnchants),
				file.valueCalculation == null || file.valueCalculation.endcapEnchants == null
					? Map.of()
					: Map.copyOf(file.valueCalculation.endcapEnchants)
			);
		}

		@Nullable
		public ReforgeEntry reforge(@Nullable String modifier) {
			if (modifier == null || modifier.isBlank()) {
				return null;
			}

			return reforgesByModifier.get(modifier.toLowerCase(Locale.ROOT));
		}
	}

	private static final class MetadataFile {
		@Nullable
		List<ReforgeEntry> reforges;
		@Nullable
		@SerializedName("value_calculation")
		ValueCalculation valueCalculation;
	}

	private static final class ValueCalculation {
		@Nullable
		@SerializedName("always_active_enchants")
		Map<String, AlwaysActiveEnchant> alwaysActiveEnchants;
		@Nullable
		@SerializedName("only_tier_one_prices")
		List<String> onlyTierOnePrices;
		@Nullable
		@SerializedName("only_tier_five_prices")
		List<String> onlyTierFivePrices;
		@Nullable
		@SerializedName("renamed_enchants")
		Map<String, String> renamedEnchants;
		@Nullable
		@SerializedName("endcap_enchants")
		Map<String, List<EndCapEntry>> endcapEnchants;
	}
}
