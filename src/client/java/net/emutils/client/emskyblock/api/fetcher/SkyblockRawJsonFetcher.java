package net.emutils.client.emskyblock.api.fetcher;

import com.google.gson.JsonObject;
import java.time.Duration;
import net.emutils.client.emskyblock.context.SkyblockContext;
import net.emutils.client.emskyblock.api.core.SkyblockApiFetchTask;
import net.emutils.client.emskyblock.api.core.SkyblockApiStatus;
import net.emutils.client.emskyblock.api.core.SkyblockHttpClient;
import net.minecraft.client.MinecraftClient;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SkyblockRawJsonFetcher {
	private static final Logger LOGGER = LoggerFactory.getLogger("emutils.skyblock.api");
	private static final Duration TIMEOUT = Duration.ofSeconds(30);

	private final SkyblockHttpClient httpClient;
	private final String name;
	private final String url;
	private final SkyblockApiFetchTask<JsonObject> fetchTask;
	private volatile JsonObject latest = new JsonObject();

	public SkyblockRawJsonFetcher(SkyblockHttpClient httpClient, String name, String url, long intervalMs) {
		this.httpClient = httpClient;
		this.name = name;
		this.url = url;
		fetchTask = new SkyblockApiFetchTask<>(
			LOGGER,
			name,
			intervalMs,
			SkyblockRawJsonFetcher::shouldRun,
			this::fetch,
			this::publish
		);
	}

	public JsonObject latest() {
		return latest.deepCopy();
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
		latest = new JsonObject();
		fetchTask.clear();
	}

	private JsonObject fetch() {
		try {
			return httpClient.getJson(url, TIMEOUT);
		} catch (Exception exception) {
			throw new IllegalStateException(name + " fetch failed: " + exception.getMessage(), exception);
		}
	}

	private boolean publish(JsonObject json) {
		if (json.isEmpty()) {
			return false;
		}

		latest = json.deepCopy();
		LOGGER.info("Loaded {} API data.", name);
		return true;
	}

	private static boolean shouldRun(@Nullable MinecraftClient client) {
		return SkyblockContext.onHypixel(client);
	}
}
