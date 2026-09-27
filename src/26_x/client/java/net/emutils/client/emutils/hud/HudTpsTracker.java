package net.emutils.client.emutils.hud;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.server.IntegratedServer;

/**
 * The server's ticks per second for the HUD Overlay's TPS line (#47). In singleplayer it's read from the
 * integrated server, with the milliseconds a tick takes. On a server it's estimated from the world time
 * the server sends every 20 ticks: how much game time passed between those updates against how much real
 * time did, over the last several seconds. Estimates start with "~".
 */
public final class HudTpsTracker {
	/** How far back samples count; longer smooths out network jitter, shorter reacts faster to lag. */
	private static final long WINDOW_NANOS = 10_000_000_000L;
	/** An estimate needs samples at least this far apart, about three time updates. */
	private static final long MIN_SPAN_NANOS = 2_500_000_000L;

	private static final Deque<Sample> SAMPLES = new ArrayDeque<>();
	private static ClientLevel sampledLevel;

	private HudTpsTracker() {
	}

	/** How healthy the tick rate is, for coloring the value. */
	public enum Health {
		UNKNOWN,
		GOOD,
		SLOW,
		BAD
	}

	public record Reading(String value, String compactValue, Health health) {
		static final Reading NONE = new Reading("--", "--", Health.UNKNOWN);
	}

	/** Called with the game time of every world time update from the server, on the client thread. */
	public static void onTimeSync(long gameTime) {
		Minecraft client = Minecraft.getInstance();
		if (client.level != sampledLevel) {
			SAMPLES.clear();
			sampledLevel = client.level;
		}
		long now = System.nanoTime();
		Sample last = SAMPLES.peekLast();
		// Game time going backwards means another world or server; start over.
		if (last != null && gameTime < last.gameTime()) {
			SAMPLES.clear();
		}
		SAMPLES.addLast(new Sample(gameTime, now));
		while (SAMPLES.size() > 2 && now - SAMPLES.peekFirst().nanos() > WINDOW_NANOS) {
			SAMPLES.removeFirst();
		}
	}

	static Reading read(Minecraft client) {
		if (client.level == null) {
			return Reading.NONE;
		}
		float target = client.level.tickRateManager().tickrate();
		IntegratedServer server = client.getSingleplayerServer();
		if (server != null) {
			double mspt = server.getAverageTickTimeNanos() / 1_000_000.0;
			double tps = mspt <= 0.0 ? target : Math.min(target, 1000.0 / mspt);
			String tpsText = String.format(Locale.ENGLISH, "%.1f", tps);
			return new Reading(tpsText + String.format(Locale.ENGLISH, " · %.1f ms", mspt), tpsText, health(tps, target));
		}
		if (client.level != sampledLevel) {
			return Reading.NONE;
		}
		return estimate(SAMPLES, System.nanoTime(), target);
	}

	/**
	 * The tick rate from world time updates: game time passed between the first and last sample against
	 * the real time between them. While no update arrives for over two seconds, the real time runs on to
	 * {@code now}, so a server that stopped ticking drifts down instead of showing its last rate.
	 */
	static Reading estimate(Deque<Sample> samples, long now, float target) {
		if (samples.size() < 2) {
			return Reading.NONE;
		}
		Sample first = samples.peekFirst();
		Sample last = samples.peekLast();
		long span = last.nanos() - first.nanos();
		long sinceLast = now - last.nanos();
		if (span < MIN_SPAN_NANOS && sinceLast < MIN_SPAN_NANOS) {
			return Reading.NONE;
		}
		long realNanos = sinceLast > 2_000_000_000L ? now - first.nanos() : span;
		double tps = Math.min(target, (last.gameTime() - first.gameTime()) * 1_000_000_000.0 / realNanos);
		String text = String.format(Locale.ENGLISH, "~%.1f", tps);
		return new Reading(text, text, health(tps, target));
	}

	/** Estimates from made-up updates, {@code {gameTime, millis}} pairs, as of {@code nowMillis}; used by UI snapshots. */
	public static Reading estimateForSnapshot(long[][] updates, long nowMillis) {
		Deque<Sample> samples = new ArrayDeque<>();
		for (long[] update : updates) {
			samples.addLast(new Sample(update[0], update[1] * 1_000_000L));
		}
		return estimate(samples, nowMillis * 1_000_000L, 20.0F);
	}

	private static Health health(double tps, float target) {
		double ratio = target <= 0.0F ? 1.0 : tps / target;
		if (ratio >= 0.9) {
			return Health.GOOD;
		}
		return ratio >= 0.75 ? Health.SLOW : Health.BAD;
	}

	private record Sample(long gameTime, long nanos) {
	}
}
