package net.emutils.client.emutils.spotify;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.emutils.client.EMUtilsClient;

final class WindowsSmtcSpotifyClient implements SpotifyClient {
	private static final Gson GSON = new Gson();
	private static final long SCRIPT_TIMEOUT_SECONDS = 5L;
	// Java SE has no built-in Windows Runtime projection. This transparent
	// PowerShell bridge only talks to local Windows SMTC APIs for Spotify state
	// and media controls; it does not install, download, or upload anything.
	private static final String SCRIPT_RESOURCE = "/assets/emutils/scripts/spotify_smtc.ps1";
	private static final String NO_ART = "";
	private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(3))
		.followRedirects(HttpClient.Redirect.NORMAL)
		.build();
	private static final Map<String, String> ARTWORK_CACHE = new ConcurrentHashMap<>();

	static boolean isSupported() {
		return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows");
	}

	@Override
	public boolean supported() {
		return isSupported();
	}

	@Override
	public Optional<SpotifyTrackState> poll() {
		CommandRunner.OptionalResult output = runScript("poll");
		if (!output.isPresent()) {
			return Optional.of(SpotifyTrackState.unavailable());
		}

		try {
			JsonObject root = GSON.fromJson(output.value(), JsonObject.class);
			if (root == null) {
				return Optional.of(SpotifyTrackState.unavailable());
			}

			String kind = root.has("kind") ? root.get("kind").getAsString() : "unavailable";
			return switch (kind) {
				case "track" -> Optional.of(parseTrack(root));
				case "no_track" -> Optional.of(SpotifyTrackState.noTrack());
				default -> Optional.of(SpotifyTrackState.unavailable());
			};
		} catch (RuntimeException exception) {
			return Optional.of(SpotifyTrackState.unavailable());
		}
	}

	@Override
	public void previous() {
		CommandRunner.runAsync(SCRIPT_TIMEOUT_SECONDS, scriptCommand("previous"));
	}

	@Override
	public void playPause() {
		CommandRunner.runAsync(SCRIPT_TIMEOUT_SECONDS, scriptCommand("playpause"));
	}

	@Override
	public void next() {
		CommandRunner.runAsync(SCRIPT_TIMEOUT_SECONDS, scriptCommand("next"));
	}

	private static SpotifyTrackState parseTrack(JsonObject root) {
		String title = root.has("title") ? root.get("title").getAsString() : "";
		String artist = root.has("artist") ? root.get("artist").getAsString() : "";
		boolean playing = root.has("playing") && root.get("playing").getAsBoolean();
		long positionMs = root.has("positionMs") ? root.get("positionMs").getAsLong() : 0L;
		long durationMs = root.has("durationMs") ? root.get("durationMs").getAsLong() : 0L;
		String artUrl = "";
		if (root.has("artBase64")) {
			String base64 = root.get("artBase64").getAsString();
			if (base64 != null && !base64.isBlank()) {
				artUrl = "data:image/png;base64," + base64.trim();
			}
		}
		if (artUrl.isBlank()) {
			artUrl = findArtworkUrl(title, artist);
		}

		return SpotifyTrackState.track(title, artist, playing, artUrl, positionMs, durationMs);
	}

	private static String findArtworkUrl(String title, String artist) {
		if (title == null || title.isBlank()) {
			return "";
		}

		String key = (title + "\n" + artist).toLowerCase(Locale.ROOT);
		String cached = ARTWORK_CACHE.get(key);
		if (cached != null) {
			return cached;
		}

		String resolved = lookupItunesArtwork(title, artist).orElse(NO_ART);
		ARTWORK_CACHE.put(key, resolved);
		return resolved;
	}

	private static Optional<String> lookupItunesArtwork(String title, String artist) {
		try {
			String term = URLEncoder.encode((title + " " + (artist == null ? "" : artist)).trim(), StandardCharsets.UTF_8);
			URI uri = URI.create("https://itunes.apple.com/search?media=music&entity=song&limit=1&term=" + term);
			HttpRequest request = HttpRequest.newBuilder(uri)
				.timeout(Duration.ofSeconds(4))
				.header("Accept", "application/json")
				.header("User-Agent", "palethea/EMUtils/" + EMUtilsClient.MOD_ID + " (Minecraft client mod)")
				.GET()
				.build();
			HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				return Optional.empty();
			}

			JsonObject root = GSON.fromJson(response.body(), JsonObject.class);
			if (root == null || !root.has("results")) {
				return Optional.empty();
			}

			JsonArray results = root.getAsJsonArray("results");
			if (results == null || results.isEmpty()) {
				return Optional.empty();
			}

			JsonElement first = results.get(0);
			if (!first.isJsonObject()) {
				return Optional.empty();
			}

			JsonObject result = first.getAsJsonObject();
			if (!result.has("artworkUrl100")) {
				return Optional.empty();
			}

			String url = result.get("artworkUrl100").getAsString();
			return url == null || url.isBlank()
				? Optional.empty()
				: Optional.of(url.replace("100x100bb", "300x300bb"));
		} catch (IOException | InterruptedException | RuntimeException exception) {
			if (exception instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}

			EMUtilsClient.LOGGER.debug("Failed to resolve Spotify artwork fallback for '{} - {}'.", artist, title, exception);
			return Optional.empty();
		}
	}

	private CommandRunner.OptionalResult runScript(String action) {
		return CommandRunner.run(SCRIPT_TIMEOUT_SECONDS, scriptCommand(action));
	}

	private static String[] scriptCommand(String action) {
		return new String[] {
			resolvePowerShellExecutable(),
			"-NoProfile",
			"-NonInteractive",
			"-ExecutionPolicy",
			"Bypass",
			"-File",
			scriptPath().toString(),
			"-Action",
			action
		};
	}

	private static String resolvePowerShellExecutable() {
		String systemRoot = System.getenv("SystemRoot");
		if (systemRoot != null) {
			Path candidate = Path.of(systemRoot, "System32", "WindowsPowerShell", "v1.0", "powershell.exe");
			if (Files.isRegularFile(candidate)) {
				return candidate.toString();
			}
		}

		return "powershell.exe";
	}

	private static Path scriptPath() {
		try {
			Path cached = ScriptCache.PATH;
			if (cached != null && Files.isRegularFile(cached)) {
				return cached;
			}

			try (InputStream input = WindowsSmtcSpotifyClient.class.getResourceAsStream(SCRIPT_RESOURCE)) {
				if (input == null) {
					throw new IOException("Missing Spotify SMTC script resource.");
				}

				Path tempDir = Files.createTempDirectory("emutils-spotify-");
				Path script = tempDir.resolve("spotify_smtc.ps1");
				Files.write(script, input.readAllBytes());
				script.toFile().deleteOnExit();
				tempDir.toFile().deleteOnExit();
				ScriptCache.PATH = script;
				return script;
			}
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.debug("Failed to extract Spotify SMTC script.", exception);
			throw new IllegalStateException("Spotify SMTC script unavailable.", exception);
		}
	}

	private static final class ScriptCache {
		private static volatile Path PATH;
	}
}
