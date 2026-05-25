package net.emutils.client.spotify;

import java.util.Locale;
import java.util.Optional;

final class LinuxSpotifyClient implements SpotifyClient {
	static boolean isSupported() {
		return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux");
	}

	@Override
	public boolean supported() {
		return isSupported();
	}

	private boolean isPlayerAvailable() {
		CommandRunner.OptionalResult result = CommandRunner.run("playerctl", "-l");
		return result.isPresent() && listsSpotify(result.value());
	}

	@Override
	public Optional<SpotifyTrackState> poll() {
		if (!isPlayerAvailable()) {
			return Optional.of(SpotifyTrackState.unavailable());
		}

		CommandRunner.OptionalResult metadata = CommandRunner.run(
			"playerctl",
			"-p",
			"spotify",
			"metadata",
			"--format",
			"{{artist}}|{{title}}|{{status}}|{{mpris:artUrl}}|{{mpris:length}}"
		);
		if (!metadata.isPresent()) {
			return Optional.of(SpotifyTrackState.noTrack());
		}

		String[] parts = metadata.value().split("\\|", 5);
		if (parts.length < 2) {
			return Optional.of(SpotifyTrackState.noTrack());
		}

		String artist = parts[0].trim();
		String title = parts[1].trim();
		boolean playing = parts.length >= 3 && "Playing".equalsIgnoreCase(parts[2].trim());
		String artUrl = parts.length >= 4 ? parts[3].trim() : "";
		long durationMs = parts.length >= 5 ? parseMicroseconds(parts[4]) : 0L;
		if (title.isEmpty()) {
			return Optional.of(SpotifyTrackState.noTrack());
		}

		long positionMs = 0L;
		CommandRunner.OptionalResult position = CommandRunner.run("playerctl", "-p", "spotify", "position");
		if (position.isPresent()) {
			positionMs = parsePositionSeconds(position.value());
		}

		return Optional.of(SpotifyTrackState.track(title, artist, playing, artUrl, positionMs, durationMs));
	}

	@Override
	public void previous() {
		CommandRunner.runAsync("playerctl", "-p", "spotify", "previous");
	}

	@Override
	public void playPause() {
		CommandRunner.runAsync("playerctl", "-p", "spotify", "play-pause");
	}

	@Override
	public void next() {
		CommandRunner.runAsync("playerctl", "-p", "spotify", "next");
	}

	private static boolean listsSpotify(String output) {
		for (String line : output.split("\n")) {
			if ("spotify".equalsIgnoreCase(line.trim())) {
				return true;
			}
		}

		return false;
	}

	private static long parseMicroseconds(String value) {
		try {
			return Math.max(0L, (long) Double.parseDouble(value.trim()) / 1_000L);
		} catch (NumberFormatException exception) {
			return 0L;
		}
	}

	private static long parsePositionSeconds(String value) {
		try {
			return Math.max(0L, (long) (Double.parseDouble(value.trim()) * 1_000D));
		} catch (NumberFormatException exception) {
			return 0L;
		}
	}
}
