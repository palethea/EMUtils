package net.emutils.client.spotify;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.Locale;
import java.util.Optional;

final class MacOsSpotifyClient implements SpotifyClient {
	private static final Gson GSON = new Gson();
	private static final long POLL_TIMEOUT_SECONDS = 3L;

	private static final String POLL_SCRIPT = """
		function run() {
		  const spotify = Application('Spotify');
		  if (!spotify.running()) {
		    return JSON.stringify({ kind: 'unavailable' });
		  }
		  try {
		    const state = spotify.playerState();
		    if (state === 'stopped') {
		      return JSON.stringify({ kind: 'no_track' });
		    }
		    const track = spotify.currentTrack;
		    return JSON.stringify({
		      kind: 'track',
		      title: track.name(),
		      artist: track.artist(),
		      playing: state === 'playing',
		      artUrl: String(track.artworkUrl()),
		      positionMs: Math.round(spotify.playerPosition() * 1000),
		      durationMs: track.duration()
		    });
		  } catch (error) {
		    return JSON.stringify({ kind: 'no_track' });
		  }
		}
		""";

	static boolean isSupported() {
		return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
	}

	@Override
	public boolean supported() {
		return isSupported();
	}

	@Override
	public Optional<SpotifyTrackState> poll() {
		CommandRunner.OptionalResult output = CommandRunner.run(
			POLL_TIMEOUT_SECONDS,
			"osascript",
			"-l",
			"JavaScript",
			"-e",
			POLL_SCRIPT
		);
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
		CommandRunner.runAsync("osascript", "-e", "tell application \"Spotify\" to previous track");
	}

	@Override
	public void playPause() {
		CommandRunner.runAsync("osascript", "-e", "tell application \"Spotify\" to playpause");
	}

	@Override
	public void next() {
		CommandRunner.runAsync("osascript", "-e", "tell application \"Spotify\" to next track");
	}

	private static SpotifyTrackState parseTrack(JsonObject root) {
		String title = root.has("title") ? root.get("title").getAsString() : "";
		String artist = root.has("artist") ? root.get("artist").getAsString() : "";
		boolean playing = root.has("playing") && root.get("playing").getAsBoolean();
		String artUrl = root.has("artUrl") ? root.get("artUrl").getAsString() : "";
		long positionMs = root.has("positionMs") ? root.get("positionMs").getAsLong() : 0L;
		long durationMs = root.has("durationMs") ? root.get("durationMs").getAsLong() : 0L;
		return SpotifyTrackState.track(title, artist, playing, artUrl, positionMs, durationMs);
	}
}
