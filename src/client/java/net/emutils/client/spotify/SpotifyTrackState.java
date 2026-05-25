package net.emutils.client.spotify;

public record SpotifyTrackState(
	String title,
	String artist,
	boolean playing,
	Kind kind,
	String artUrl,
	long positionMs,
	long durationMs,
	long polledAtMs
) {
	public enum Kind {
		TRACK,
		NO_TRACK,
		UNAVAILABLE
	}

	public static SpotifyTrackState noTrack() {
		return new SpotifyTrackState("", "", false, Kind.NO_TRACK, "", 0L, 0L, System.currentTimeMillis());
	}

	public static SpotifyTrackState unavailable() {
		return new SpotifyTrackState("", "", false, Kind.UNAVAILABLE, "", 0L, 0L, System.currentTimeMillis());
	}

	public static SpotifyTrackState track(
		String title,
		String artist,
		boolean playing,
		String artUrl,
		long positionMs,
		long durationMs
	) {
		if (title == null || title.isBlank()) {
			return noTrack();
		}

		return new SpotifyTrackState(
			title.trim(),
			artist == null ? "" : artist.trim(),
			playing,
			Kind.TRACK,
			artUrl == null ? "" : artUrl.trim(),
			Math.max(0L, positionMs),
			Math.max(0L, durationMs),
			System.currentTimeMillis()
		);
	}

	public boolean hasTrack() {
		return kind == Kind.TRACK;
	}

	public boolean shouldDisplay() {
		return kind != Kind.UNAVAILABLE;
	}

	public long effectivePositionMs() {
		if (!playing || durationMs <= 0L) {
			return positionMs;
		}

		long elapsed = System.currentTimeMillis() - polledAtMs;
		return Math.min(durationMs, positionMs + elapsed);
	}

	public int progressPercent() {
		if (durationMs <= 0L) {
			return 0;
		}

		return (int) Math.min(100L, effectivePositionMs() * 100L / durationMs);
	}
}
