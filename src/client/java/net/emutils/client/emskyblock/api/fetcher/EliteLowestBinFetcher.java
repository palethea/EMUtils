package net.emutils.client.emskyblock.api.fetcher;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import net.emutils.client.emskyblock.context.SkyblockContext;
import net.emutils.client.emskyblock.api.core.SkyblockApiFetchTask;
import net.emutils.client.emskyblock.api.core.SkyblockApiStatus;
import net.emutils.client.emskyblock.api.core.SkyblockHttpClient;
import net.minecraft.client.MinecraftClient;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EliteLowestBinFetcher {
	private static final Logger LOGGER = LoggerFactory.getLogger("emutils.elite_lbin");
	private static final String API_URL = "https://api.eliteskyblock.com/resources/auctions/neu";
	private static final long FETCH_INTERVAL_MS = 120_000L;
	private static final Duration TIMEOUT = Duration.ofSeconds(30);

	private final SkyblockHttpClient httpClient;
	private final SkyblockApiFetchTask<Map<String, Long>> fetchTask;
	private volatile Map<String, Long> lowestBins = Map.of();

	public EliteLowestBinFetcher(SkyblockHttpClient httpClient) {
		this.httpClient = httpClient;
		fetchTask = new SkyblockApiFetchTask<>(
			LOGGER,
			"EliteSkyBlock Lowest BIN",
			FETCH_INTERVAL_MS,
			EliteLowestBinFetcher::shouldRun,
			this::fetch,
			this::publish
		);
	}

	public int count() {
		return lowestBins.size();
	}

	public OptionalLong lowestBin(String internalName) {
		Long value = lowestBins.get(internalName);
		return value != null && value > 0L ? OptionalLong.of(value) : OptionalLong.empty();
	}

	public Map<String, Long> snapshot() {
		return lowestBins;
	}

	public SkyblockApiStatus status() {
		return fetchTask.status();
	}

	public void tick(@Nullable MinecraftClient client) {
		fetchTask.tick(client);
	}

	public void fetchNow(@Nullable MinecraftClient client) {
		fetchTask.fetchNow(client);
	}

	public void requestImmediateFetch() {
		fetchTask.requestImmediateFetch();
	}

	public void clear() {
		lowestBins = Map.of();
		fetchTask.clear();
	}

	private Map<String, Long> fetch() {
		try {
			return parse(httpClient.getJson(API_URL, TIMEOUT));
		} catch (Exception exception) {
			throw new IllegalStateException("EliteSkyBlock lowest BIN fetch failed: " + exception.getMessage(), exception);
		}
	}

	private boolean publish(Map<String, Long> parsed) {
		if (parsed.isEmpty()) {
			return false;
		}

		lowestBins = parsed;
		LOGGER.info("Loaded {} EliteSkyBlock lowest BIN entries.", parsed.size());
		return true;
	}

	private static Map<String, Long> parse(JsonObject root) {
		Map<String, Long> result = new HashMap<>();
		for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
			if (!entry.getValue().isJsonPrimitive()) {
				continue;
			}

			try {
				long value = entry.getValue().getAsLong();
				if (value > 0L) {
					result.put(entry.getKey(), value);
				}
			} catch (NumberFormatException | UnsupportedOperationException ignored) {
			}
		}

		return Map.copyOf(result);
	}

	private static boolean shouldRun(@Nullable MinecraftClient client) {
		return SkyblockContext.onHypixel(client);
	}
}
