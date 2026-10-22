package net.emutils.client.emutils.packs.modrinth;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.packs.PackType;

public final class ModrinthClient {
	private static final String API = "https://api.modrinth.com/v2";
	private static final Gson GSON = new Gson();
	/**
	 * A download with no new bytes for this long is given up (#257). The request timeout only covers waiting
	 * for the response to start, so a body that stalls partway would otherwise never end.
	 */
	private static final Duration STALL_TIMEOUT = Duration.ofSeconds(30);
	private static final ScheduledExecutorService WATCHDOG = Executors.newSingleThreadScheduledExecutor(runnable -> {
		Thread thread = new Thread(runnable, "EMUtils Pack Download Watchdog");
		thread.setDaemon(true);
		return thread;
	});
	private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build();
	private final Duration stallTimeout;

	public ModrinthClient() {
		this(STALL_TIMEOUT);
	}

	/** A client whose downloads give up after {@code stallTimeout} without new bytes; UI snapshots use a short one. */
	public ModrinthClient(Duration stallTimeout) {
		this.stallTimeout = stallTimeout;
	}

	public List<ModrinthSearchResult> search(PackType type, String query, String minecraftVersion, int limit) throws IOException, InterruptedException {
		String index = query == null || query.isBlank() ? "downloads" : "relevance";
		return search(type, query, minecraftVersion, limit, index);
	}

	public List<ModrinthSearchResult> search(PackType type, String query, String minecraftVersion, int limit, String index) throws IOException, InterruptedException {
		return search(type, query, minecraftVersion, limit, index, 0);
	}

	/** One page of search results, starting {@code offset} results in. An empty query lists the most downloaded. */
	public List<ModrinthSearchResult> search(PackType type, String query, String minecraftVersion, int limit, int offset) throws IOException, InterruptedException {
		String index = query == null || query.isBlank() ? "downloads" : "relevance";
		return search(type, query, minecraftVersion, limit, index, offset);
	}

	private List<ModrinthSearchResult> search(PackType type, String query, String minecraftVersion, int limit, String index, int offset) throws IOException, InterruptedException {
		String facets = "[[\"project_type:" + type.modrinthProjectType() + "\"],[\"versions:" + minecraftVersion + "\"]]";
		String url = API + "/search?limit=" + limit
			+ "&offset=" + offset
			+ "&index=" + encode(index)
			+ "&query=" + encode(query == null ? "" : query)
			+ "&facets=" + encode(facets);
		ModrinthSearchResponse response = readJson(url, ModrinthSearchResponse.class);
		return response == null || response.hits == null ? List.of() : response.hits;
	}

	public ModrinthVersion newestCompatibleVersion(String projectId, String minecraftVersion) throws IOException, InterruptedException {
		ModrinthVersion[] versions = readJson(API + "/project/" + encodePath(projectId) + "/version", ModrinthVersion[].class);
		if (versions == null) {
			throw new IOException("Modrinth returned no versions.");
		}

		return Arrays.stream(versions)
			.filter(version -> version.supports(minecraftVersion))
			.sorted(Comparator.comparing(ModrinthVersion::datePublished, Comparator.nullsLast(String::compareTo)).reversed())
			.findFirst()
			.orElseThrow(() -> new IOException("No compatible version for Minecraft " + minecraftVersion + "."));
	}

	/** Downloads {@code file} to {@code target}, giving up when no bytes arrive for a while. */
	public void download(ModrinthFile file, Path target) throws IOException, InterruptedException {
		HttpRequest request = request(file.url()).build();
		HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
		try (InputStream body = response.body()) {
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				throw new IOException("Modrinth download failed with HTTP " + response.statusCode() + ".");
			}
			copyWatched(body, target, stallTimeout);
		}
	}

	/**
	 * Copies the body to the file. A watchdog checks every second whether bytes still come, and stops a
	 * stalled read by interrupting it and closing the stream.
	 */
	private static void copyWatched(InputStream body, Path target, Duration stallTimeout) throws IOException {
		Thread reader = Thread.currentThread();
		AtomicLong lastProgress = new AtomicLong(System.nanoTime());
		AtomicBoolean stalled = new AtomicBoolean();
		long stallNanos = stallTimeout.toNanos();
		ScheduledFuture<?> watchdog = WATCHDOG.scheduleAtFixedRate(() -> {
			if (!stalled.get() && System.nanoTime() - lastProgress.get() > stallNanos) {
				stalled.set(true);
				reader.interrupt();
				try {
					body.close();
				} catch (IOException ignored) {
					// Closing is only to unblock the read.
				}
			}
		}, 1L, 1L, TimeUnit.SECONDS);
		try (OutputStream out = Files.newOutputStream(target)) {
			byte[] buffer = new byte[64 * 1024];
			int read;
			while ((read = body.read(buffer)) >= 0) {
				out.write(buffer, 0, read);
				lastProgress.set(System.nanoTime());
			}
		} catch (IOException exception) {
			if (stalled.get()) {
				throw new IOException("Modrinth download stalled.", exception);
			}
			throw exception;
		} finally {
			watchdog.cancel(false);
			if (stalled.get()) {
				// The interrupt was only for the read; the worker thread goes on with other tasks.
				Thread.interrupted();
			}
		}
		if (stalled.get()) {
			throw new IOException("Modrinth download stalled.");
		}
	}

	private <T> T readJson(String url, Class<T> type) throws IOException, InterruptedException {
		HttpRequest request = request(url).header("Accept", "application/json").build();
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw new IOException("Modrinth request failed with HTTP " + response.statusCode() + ".");
		}

		try {
			return GSON.fromJson(response.body(), type);
		} catch (JsonParseException exception) {
			throw new IOException("Modrinth returned malformed JSON.", exception);
		}
	}

	private static HttpRequest.Builder request(String url) {
		return HttpRequest.newBuilder(URI.create(url))
			.timeout(Duration.ofSeconds(30))
			.header("User-Agent", "palethea/EMUtils/" + EMUtilsClient.MOD_ID + " (Minecraft client mod)");
	}

	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private static String encodePath(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
	}

	private record ModrinthSearchResponse(List<ModrinthSearchResult> hits) {
	}
}
