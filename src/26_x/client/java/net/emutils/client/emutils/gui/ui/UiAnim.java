package net.emutils.client.emutils.gui.ui;

import java.util.HashMap;
import java.util.Map;

/**
 * Smoothly animated values keyed by name, such as hover highlights and switch positions. Call
 * {@link #frame()} once per rendered frame, then {@link #towards} for each value.
 */
public final class UiAnim {
	private final Map<String, Float> values = new HashMap<>();
	private final Map<String, Tween> tweens = new HashMap<>();
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

	/**
	 * A fixed-length transition like a CSS {@code transition: ... ease-in-out}: when {@code target}
	 * changes, the value travels from where it is now to the target in {@code seconds}. Unlike
	 * {@link #towards}, it has no long tail, so short movements look crisp. A new key starts at its target.
	 */
	public float transition(String key, float target, float seconds) {
		long now = System.nanoTime();
		Tween tween = tweens.get(key);
		if (tween == null) {
			tween = new Tween(target, target, now);
			tweens.put(key, tween);
		} else if (tween.to() != target) {
			tween = new Tween(tween.value(now, seconds), target, now);
			tweens.put(key, tween);
		}
		return tween.value(now, seconds);
	}

	public float transition(String key, boolean on, float seconds) {
		return transition(key, on ? 1.0F : 0.0F, seconds);
	}

	private record Tween(float from, float to, long startNanos) {
		private float value(long now, float seconds) {
			float t = Math.clamp((now - startNanos) / (seconds * 1_000_000_000.0F), 0.0F, 1.0F);
			float eased = t < 0.5F ? 4.0F * t * t * t : 1.0F - (float) Math.pow(-2.0F * t + 2.0F, 3.0) / 2.0F;
			return from + (to - from) * eased;
		}
	}
}
