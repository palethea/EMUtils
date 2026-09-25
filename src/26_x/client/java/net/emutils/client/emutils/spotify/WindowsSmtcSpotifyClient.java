package net.emutils.client.emutils.spotify;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.emutils.client.EMUtilsClient;
import org.jspecify.annotations.Nullable;

/**
 * Reads and controls Spotify on Windows through the system media transport controls (SMTC), the
 * WinRT API behind the media overlay Windows shows for the volume keys. It talks to the local
 * Windows API only; the one network request is the iTunes cover lookup when Windows has no cover.
 */
final class WindowsSmtcSpotifyClient implements SpotifyClient {
	private static final Gson GSON = new Gson();
	private static final long TIMEOUT_MS = 5_000L;
	private static final int MAX_THUMBNAIL_BYTES = 2 * 1024 * 1024;
	private static final String NO_ART = "";
	private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(3))
		.followRedirects(HttpClient.Redirect.NORMAL)
		.build();
	private static final Map<String, String> ARTWORK_CACHE = new ConcurrentHashMap<>();

	// Interface IDs and runtime classes from the Windows SDK (windows.media.control.idl and
	// windows.storage.streams.idl). The slot numbers used below follow the method order there.
	private static final String SESSION_MANAGER = "Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager";
	private static final String DATA_READER = "Windows.Storage.Streams.DataReader";
	private static final Pointer IID_SESSION_MANAGER_STATICS = WinRt.iid("2050C4EE-11A0-57DE-AED7-C97C70338245");
	private static final Pointer IID_DATA_READER_FACTORY = WinRt.iid("D7527847-57DA-4E15-914C-06806699A098");
	private static final Pointer IID_RANDOM_ACCESS_STREAM = WinRt.iid("905A0FE1-BC53-11DF-8C49-001E4FC686DA");

	private static final int MANAGER_STATICS_REQUEST_ASYNC = 6;
	private static final int MANAGER_GET_CURRENT_SESSION = 6;
	private static final int MANAGER_GET_SESSIONS = 7;
	private static final int VECTOR_VIEW_GET_AT = 6;
	private static final int VECTOR_VIEW_SIZE = 7;
	private static final int SESSION_SOURCE_APP_ID = 6;
	private static final int SESSION_TRY_GET_MEDIA_PROPERTIES = 7;
	private static final int SESSION_GET_TIMELINE_PROPERTIES = 8;
	private static final int SESSION_GET_PLAYBACK_INFO = 9;
	private static final int SESSION_TRY_SKIP_NEXT = 16;
	private static final int SESSION_TRY_SKIP_PREVIOUS = 17;
	private static final int SESSION_TRY_TOGGLE_PLAY_PAUSE = 20;
	private static final int MEDIA_TITLE = 6;
	private static final int MEDIA_ARTIST = 9;
	private static final int MEDIA_THUMBNAIL = 15;
	private static final int TIMELINE_END_TIME = 7;
	private static final int TIMELINE_POSITION = 10;
	private static final int TIMELINE_LAST_UPDATED = 11;
	private static final int PLAYBACK_INFO_STATUS = 7;
	private static final int PLAYBACK_STATUS_PLAYING = 4;
	private static final int STREAM_REFERENCE_OPEN_READ = 6;
	private static final int STREAM_SIZE = 6;
	private static final int STREAM_GET_INPUT_STREAM_AT = 8;
	private static final int DATA_READER_FACTORY_CREATE = 6;
	private static final int DATA_READER_READ_BYTES = 14;
	private static final int DATA_READER_LOAD_ASYNC = 29;

	/** WinRT time spans and dates count 100 ns ticks; dates start in 1601. */
	private static final long TICKS_PER_MS = 10_000L;
	private static final long EPOCH_OFFSET_MS = 11_644_473_600_000L;

	/** Every WinRT call runs on this one thread, which joins the multithreaded apartment first. */
	private final ExecutorService thread = Executors.newSingleThreadExecutor(runnable -> {
		Thread worker = new Thread(() -> {
			WinRt.initThread();
			runnable.run();
		}, "EMUtils-Spotify-SMTC");
		worker.setDaemon(true);
		return worker;
	});
	/** Only touched on {@link #thread}. */
	private @Nullable Pointer manager;
	private String thumbnailKey = "";
	private String thumbnailUrl = NO_ART;

	static boolean isSupported() {
		return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows");
	}

	@Override
	public boolean supported() {
		return isSupported();
	}

	@Override
	public Optional<SpotifyTrackState> poll() {
		Future<SpotifyTrackState> result = thread.submit(this::readState);
		try {
			return Optional.of(result.get(TIMEOUT_MS * 2, TimeUnit.MILLISECONDS));
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			return Optional.of(SpotifyTrackState.unavailable());
		} catch (ExecutionException | TimeoutException exception) {
			result.cancel(true);
			EMUtilsClient.LOGGER.debug("Failed to read Spotify from the Windows media controls.", exception);
			return Optional.of(SpotifyTrackState.unavailable());
		}
	}

	@Override
	public void previous() {
		control(SESSION_TRY_SKIP_PREVIOUS);
	}

	@Override
	public void playPause() {
		control(SESSION_TRY_TOGGLE_PLAY_PAUSE);
	}

	@Override
	public void next() {
		control(SESSION_TRY_SKIP_NEXT);
	}

	private void control(int slot) {
		thread.execute(() -> {
			try {
				withSpotifySession(session -> {
					Pointer operation = WinRt.object(session, slot);
					if (operation != null) {
						WinRt.await(operation, TIMEOUT_MS, new Memory(4));
					}
					return null;
				});
			} catch (RuntimeException exception) {
				EMUtilsClient.LOGGER.debug("Failed to control Spotify through the Windows media controls.", exception);
			}
		});
	}

	private SpotifyTrackState readState() {
		SpotifyTrackState state = withSpotifySession(this::readSession);
		return state == null ? SpotifyTrackState.unavailable() : state;
	}

	private SpotifyTrackState readSession(Pointer session) {
		Pointer playback = WinRt.object(session, SESSION_GET_PLAYBACK_INFO);
		boolean playing;
		try {
			playing = playback != null && WinRt.int32(playback, PLAYBACK_INFO_STATUS) == PLAYBACK_STATUS_PLAYING;
		} finally {
			WinRt.release(playback);
		}

		long positionMs = 0L;
		long durationMs = 0L;
		Pointer timeline = WinRt.object(session, SESSION_GET_TIMELINE_PROPERTIES);
		if (timeline != null) {
			try {
				durationMs = WinRt.int64(timeline, TIMELINE_END_TIME) / TICKS_PER_MS;
				positionMs = WinRt.int64(timeline, TIMELINE_POSITION) / TICKS_PER_MS;
				if (playing) {
					// Spotify reports the position when it last changed; advance it to now.
					long updatedMs = WinRt.int64(timeline, TIMELINE_LAST_UPDATED) / TICKS_PER_MS - EPOCH_OFFSET_MS;
					positionMs += Math.max(0L, System.currentTimeMillis() - updatedMs);
					if (durationMs > 0L) {
						positionMs = Math.min(positionMs, durationMs);
					}
				}
			} finally {
				WinRt.release(timeline);
			}
		}

		Pointer operation = WinRt.object(session, SESSION_TRY_GET_MEDIA_PROPERTIES);
		Pointer properties = operation == null ? null : WinRt.awaitObject(operation, TIMEOUT_MS);
		if (properties == null) {
			return SpotifyTrackState.noTrack();
		}
		try {
			String title = WinRt.string(properties, MEDIA_TITLE);
			if (title.isBlank()) {
				// Spotify is open but not showing a track yet.
				return SpotifyTrackState.noTrack();
			}
			String artist = WinRt.string(properties, MEDIA_ARTIST);
			String artUrl = thumbnailUrl(properties, title + "\n" + artist);
			if (artUrl.isBlank()) {
				artUrl = findArtworkUrl(title, artist);
			}
			return SpotifyTrackState.track(title, artist, playing, artUrl, positionMs, durationMs);
		} finally {
			WinRt.release(properties);
		}
	}

	/**
	 * The track's cover from Windows as a data URL, read once per track and handed out as the same
	 * string afterwards, so the art loader's cache finds it without comparing the whole image.
	 * Windows sometimes adds the cover a moment after the title, so a missing one is tried again.
	 */
	private String thumbnailUrl(Pointer properties, String key) {
		if (key.equals(thumbnailKey) && !thumbnailUrl.isEmpty()) {
			return thumbnailUrl;
		}
		byte[] bytes = readThumbnail(properties);
		thumbnailKey = key;
		thumbnailUrl = bytes == null ? NO_ART : "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
		return thumbnailUrl;
	}

	private static byte @Nullable [] readThumbnail(Pointer properties) {
		try {
			Pointer reference = WinRt.object(properties, MEDIA_THUMBNAIL);
			if (reference == null) {
				return null;
			}
			Pointer opened;
			try {
				Pointer operation = WinRt.object(reference, STREAM_REFERENCE_OPEN_READ);
				opened = operation == null ? null : WinRt.awaitObject(operation, TIMEOUT_MS);
			} finally {
				WinRt.release(reference);
			}
			if (opened == null) {
				return null;
			}
			Pointer stream;
			try {
				stream = WinRt.queryInterface(opened, IID_RANDOM_ACCESS_STREAM);
			} finally {
				WinRt.release(opened);
			}
			try {
				long size = WinRt.int64(stream, STREAM_SIZE);
				if (size <= 0L || size > MAX_THUMBNAIL_BYTES) {
					return null;
				}
				return readAll(stream, (int) size);
			} finally {
				WinRt.release(stream);
			}
		} catch (RuntimeException exception) {
			// A missing cover shouldn't stop the track from showing.
			EMUtilsClient.LOGGER.debug("Failed to read the Spotify cover from Windows.", exception);
			return null;
		}
	}

	private static byte @Nullable [] readAll(Pointer stream, int size) {
		Pointer input = WinRt.object(stream, STREAM_GET_INPUT_STREAM_AT, 0L);
		if (input == null) {
			return null;
		}
		Pointer factory = null;
		Pointer reader = null;
		try {
			factory = WinRt.factory(DATA_READER, IID_DATA_READER_FACTORY);
			reader = WinRt.object(factory, DATA_READER_FACTORY_CREATE, input);
			if (reader == null) {
				return null;
			}
			Pointer load = WinRt.object(reader, DATA_READER_LOAD_ASYNC, size);
			if (load == null) {
				return null;
			}
			Memory loaded = new Memory(4);
			WinRt.await(load, TIMEOUT_MS, loaded);
			int count = loaded.getInt(0);
			if (count <= 0) {
				return null;
			}
			Memory bytes = new Memory(count);
			if (WinRt.invoke(reader, DATA_READER_READ_BYTES, count, bytes) < 0) {
				return null;
			}
			return bytes.getByteArray(0, count);
		} finally {
			WinRt.release(reader);
			WinRt.release(factory);
			WinRt.release(input);
		}
	}

	/**
	 * Runs {@code action} with Spotify's media session, or returns null when Spotify has none.
	 * Prefers the session that belongs to Spotify when several apps play media.
	 */
	private <T> @Nullable T withSpotifySession(SessionAction<T> action) {
		Pointer session;
		try {
			session = findSpotifySession(manager());
		} catch (RuntimeException exception) {
			// Ask Windows for a fresh session manager next time.
			WinRt.release(manager);
			manager = null;
			throw exception;
		}
		if (session == null) {
			return null;
		}
		try {
			return action.run(session);
		} finally {
			WinRt.release(session);
		}
	}

	private Pointer manager() {
		if (manager == null) {
			Pointer statics = WinRt.factory(SESSION_MANAGER, IID_SESSION_MANAGER_STATICS);
			try {
				Pointer operation = WinRt.object(statics, MANAGER_STATICS_REQUEST_ASYNC);
				manager = operation == null ? null : WinRt.awaitObject(operation, TIMEOUT_MS);
			} finally {
				WinRt.release(statics);
			}
			if (manager == null) {
				throw new WinRt.WinRtException("No media session manager", 0);
			}
		}
		return manager;
	}

	private static @Nullable Pointer findSpotifySession(Pointer manager) {
		Pointer sessions = WinRt.object(manager, MANAGER_GET_SESSIONS);
		if (sessions != null) {
			try {
				int count = WinRt.int32(sessions, VECTOR_VIEW_SIZE);
				for (int i = 0; i < count; i++) {
					Pointer session = WinRt.object(sessions, VECTOR_VIEW_GET_AT, i);
					if (isSpotify(session)) {
						return session;
					}
					WinRt.release(session);
				}
			} finally {
				WinRt.release(sessions);
			}
		}

		// Some Windows builds only list the active app as the current session.
		Pointer current = WinRt.object(manager, MANAGER_GET_CURRENT_SESSION);
		if (isSpotify(current)) {
			return current;
		}
		WinRt.release(current);
		return null;
	}

	private static boolean isSpotify(@Nullable Pointer session) {
		return session != null && WinRt.string(session, SESSION_SOURCE_APP_ID).toLowerCase(Locale.ROOT).contains("spotify");
	}

	private interface SessionAction<T> {
		@Nullable T run(Pointer session);
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
}
