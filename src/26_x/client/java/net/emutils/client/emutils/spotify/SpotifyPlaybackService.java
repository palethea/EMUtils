package net.emutils.client.emutils.spotify;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.spotify.gui.SpotifyIcons;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class SpotifyPlaybackService {
	private static final long POLL_INTERVAL_MS = 1_000L;
	private static final Identifier FALLBACK_ART = SpotifyIcons.FALLBACK_ART;

	private final SpotifyClient client = SpotifyClientFactory.get();
	private final ExecutorService pollExecutor = Executors.newSingleThreadExecutor(thread -> {
		Thread worker = new Thread(thread, "EMUtils-Spotify-Poll");
		worker.setDaemon(true);
		return worker;
	});

	private volatile SpotifyTrackState state = SpotifyTrackState.unavailable();
	private volatile boolean polling;
	private volatile boolean awaitingArtUpdate;
	private final AtomicLong pollGeneration = new AtomicLong();
	private long lastPollAt;
	private SpotifyArtLoader artLoader;

	public SpotifyTrackState state() {
		return state;
	}

	public SpotifyArtLoader.ArtResult art(SpotifyTrackState trackState) {
		SpotifyArtLoader loader = artLoader();
		if (loader == null || !trackState.hasTrack()) {
			return SpotifyArtLoader.ArtResult.fallback(FALLBACK_ART, SpotifyArtLoader.State.NONE, SpotifyArtLoader.DISPLAY_SIZE);
		}

		if (awaitingArtUpdate) {
			loader.resolve(trackState.artUrl(), FALLBACK_ART);
			return SpotifyArtLoader.ArtResult.fallback(FALLBACK_ART, SpotifyArtLoader.State.LOADING, SpotifyArtLoader.DISPLAY_SIZE);
		}

		return loader.resolve(trackState.artUrl(), FALLBACK_ART);
	}

	public void tick(boolean active) {
		if (!client.supported()) {
			state = SpotifyTrackState.unavailable();
			return;
		}

		if (!active || polling) {
			return;
		}

		long now = System.currentTimeMillis();
		if (now - lastPollAt < POLL_INTERVAL_MS) {
			return;
		}

		lastPollAt = now;
		long generation = pollGeneration.get();
		polling = true;
		pollExecutor.submit(() -> {
			try {
				SpotifyTrackState nextState = client.poll().orElse(SpotifyTrackState.unavailable());
				if (generation != pollGeneration.get()) {
					return;
				}

				state = nextState;
				awaitingArtUpdate = false;
				if (nextState.hasTrack() && !nextState.artUrl().isBlank()) {
					SpotifyArtLoader loader = artLoader();
					if (loader != null) {
						loader.resolve(nextState.artUrl(), FALLBACK_ART);
					}
				}
			} catch (RuntimeException exception) {
				if (generation == pollGeneration.get()) {
					EMUtilsClient.LOGGER.debug("Failed to poll Spotify playback", exception);
					state = SpotifyTrackState.unavailable();
					awaitingArtUpdate = false;
				}
			} finally {
				polling = false;
			}
		});
	}

	public void refreshSoon() {
		lastPollAt = 0L;
	}

	public void previous() {
		if (!client.supported()) {
			return;
		}

		pollGeneration.incrementAndGet();
		awaitingArtUpdate = true;
		client.previous();
		refreshSoon();
	}

	public void playPause() {
		if (!client.supported()) {
			return;
		}

		pollGeneration.incrementAndGet();
		client.playPause();
		refreshSoon();
	}

	public void next() {
		if (!client.supported()) {
			return;
		}

		pollGeneration.incrementAndGet();
		awaitingArtUpdate = true;
		client.next();
		refreshSoon();
	}

	private SpotifyArtLoader artLoader() {
		Minecraft minecraftClient = Minecraft.getInstance();
		if (minecraftClient == null) {
			return null;
		}

		if (artLoader == null) {
			artLoader = new SpotifyArtLoader(minecraftClient);
		}

		return artLoader;
	}
}
