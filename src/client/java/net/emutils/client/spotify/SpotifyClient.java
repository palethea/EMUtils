package net.emutils.client.spotify;

import java.util.Optional;

interface SpotifyClient {
	boolean supported();

	Optional<SpotifyTrackState> poll();

	void previous();

	void playPause();

	void next();
}
