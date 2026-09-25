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
	/** Right after a button press, Spotify is polled this often, so the change shows up quickly. */
	private static final long FAST_POLL_INTERVAL_MS = 150L;
	private static final long FAST_POLL_MS = 2_000L;
	/**
	 * How long a play/pause press wins over polls that still report the old state: Spotify updates
	 * what it reports a moment after it acts.
	 */
	private static final long PENDING_PLAYING_MS = 1_500L;
	private static final Identifier FALLBACK_ART = SpotifyIcons.FALLBACK_ART;

	private final SpotifyClient client = SpotifyClientFactory.get();
	private final ExecutorService pollExecutor = Executors.newSingleThreadExecutor(thread -> {
		Thread worker = new Thread(thread, "EMUtils-Spotify-Poll");
		worker.setDaemon(true);
		return worker;
	});

	private volatile SpotifyTrackState state = SpotifyTrackState.unavailable();
	private volatile boolean polling;
	private volatile boolean pendingPlaying;
	private volatile long pendingPlayingUntil;
	private volatile long fastPollUntil;
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
		long interval = now < fastPollUntil ? FAST_POLL_INTERVAL_MS : POLL_INTERVAL_MS;
		if (now - lastPollAt < interval) {
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

				state = withPendingPress(nextState);
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
				}
			} finally {
				polling = false;
			}
		});
	}

	/** Keeps a play/pause press on show until Spotify reports it too, or it has had time to. */
	private SpotifyTrackState withPendingPress(SpotifyTrackState polled) {
		if (pendingPlayingUntil == 0L) {
			return polled;
		}
		if (!polled.hasTrack() || polled.playing() == pendingPlaying || System.currentTimeMillis() > pendingPlayingUntil) {
			pendingPlayingUntil = 0L;
			return polled;
		}
		return polled.withPlaying(pendingPlaying);
	}

	public void refreshSoon() {
		lastPollAt = 0L;
	}

	public void previous() {
		if (!client.supported()) {
			return;
		}

		afterPress();
		client.previous();
	}

	public void playPause() {
		if (!client.supported()) {
			return;
		}

		SpotifyTrackState current = state;
		if (current.hasTrack()) {
			// Shows the press right away instead of after Spotify reports it.
			pendingPlaying = !current.playing();
			pendingPlayingUntil = System.currentTimeMillis() + PENDING_PLAYING_MS;
			state = current.withPlaying(pendingPlaying);
		}
		afterPress();
		client.playPause();
	}

	public void next() {
		if (!client.supported()) {
			return;
		}

		afterPress();
		client.next();
	}

	/** Drops polls already under way, which may predate the press, and polls quickly for a moment. */
	private void afterPress() {
		pollGeneration.incrementAndGet();
		fastPollUntil = System.currentTimeMillis() + FAST_POLL_MS;
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
