package net.emutils.client.spotify;

import java.util.Optional;

final class NoopSpotifyClient implements SpotifyClient {
	@Override
	public boolean supported() {
		return false;
	}

	@Override
	public Optional<SpotifyTrackState> poll() {
		return Optional.of(SpotifyTrackState.unavailable());
	}

	@Override
	public void previous() {
	}

	@Override
	public void playPause() {
	}

	@Override
	public void next() {
	}
}
