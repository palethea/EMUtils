package net.emutils.client.emutils.spotify;

import java.util.Optional;

interface SpotifyClient {
	boolean supported();

	Optional<SpotifyTrackState> poll();

	void previous();

	void playPause();

	void next();
}
