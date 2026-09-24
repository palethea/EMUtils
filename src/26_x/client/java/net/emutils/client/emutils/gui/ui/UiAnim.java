package net.emutils.client.emutils.gui.ui;

import java.util.HashMap;
import java.util.Map;

/**
 * Smoothly animated values keyed by name, such as hover highlights and switch positions. Call
 * {@link #frame()} once per rendered frame, then {@link #towards} for each value.
 */
public final class UiAnim {
	private final Map<String, Float> values = new HashMap<>();
	private long lastFrameNanos;
	private float frameSeconds;

	public void frame() {
		long now = System.nanoTime();
		frameSeconds = lastFrameNanos == 0L ? 0.0F : Math.min(0.1F, (now - lastFrameNanos) / 1_000_000_000.0F);
		lastFrameNanos = now;
	}

	/**
	 * Moves the value for {@code key} towards {@code target} and returns it. {@code speed} is how fast it
	 * settles; around 14 feels snappy, 8 relaxed. A new key starts at its target.
	 */
	public float towards(String key, float target, float speed) {
		float current = values.getOrDefault(key, target);
		float next = current + (target - current) * (1.0F - (float) Math.exp(-speed * frameSeconds));
		if (Math.abs(target - next) < 0.001F) {
			next = target;
		}
		values.put(key, next);
		return next;
	}

	public float towards(String key, boolean on, float speed) {
		return towards(key, on ? 1.0F : 0.0F, speed);
	}

	/** Eases a 0..1 progress so movement starts fast and settles gently. */
	public static float easeOut(float t) {
		float inverse = 1.0F - Math.clamp(t, 0.0F, 1.0F);
		return 1.0F - inverse * inverse * inverse;
	}
}
