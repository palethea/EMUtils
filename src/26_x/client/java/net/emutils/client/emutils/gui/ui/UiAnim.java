package net.emutils.client.emutils.gui.ui;

import java.util.HashMap;
import java.util.Map;

/**
 * Smoothly animated values keyed by name, such as hover highlights and switch positions. Call
 * {@link #frame()} once per rendered frame, then {@link #towards} for each value. Everything follows
 * the Animations menu setting (#149): twice as fast, or no motion at all.
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
		UiMotion motion = UiStyle.motion();
		if (motion == UiMotion.OFF) {
			values.put(key, target);
			return target;
		}
		float current = values.getOrDefault(key, target);
		float scaledSpeed = motion == UiMotion.FAST ? speed * 2.0F : speed;
		float next = current + (target - current) * (1.0F - (float) Math.exp(-scaledSpeed * frameSeconds));
		if (Math.abs(target - next) < 0.001F) {
			next = target;
		}
		values.put(key, next);
		return next;
	}

	/** Jumps a {@link #towards} value to {@code value} without animating. */
	public void set(String key, float value) {
		values.put(key, value);
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
		return transition(key, target, seconds, false);
	}

	/**
	 * Like {@link #transition(String, float, float)}, with an ease-out curve when {@code easeOut} is set:
	 * it starts at full speed and settles gently, the usual curve for panels opening and closing.
	 */
	public float transition(String key, float target, float seconds, boolean easeOut) {
		UiMotion motion = UiStyle.motion();
		if (motion == UiMotion.OFF) {
			tweens.put(key, new Tween(target, target, 0L, 0.0F, easeOut));
			return target;
		}
		if (motion == UiMotion.FAST) {
			seconds *= 0.5F;
		}
		long now = System.nanoTime();
		Tween tween = tweens.get(key);
		if (tween == null) {
			tween = new Tween(target, target, now, seconds, easeOut);
			tweens.put(key, tween);
		} else if (tween.to() != target) {
			// Picked up from where the interrupted transition is at its own speed, so a transition that is
			// quicker one way than the other doesn't jump when it turns around.
			tween = new Tween(tween.value(now), target, now, seconds, easeOut);
			tweens.put(key, tween);
		}
		return tween.value(now);
	}

	/** Jumps a {@link #transition} value to {@code value}, so the next transition starts from there. */
	public void snap(String key, float value) {
		tweens.put(key, new Tween(value, value, System.nanoTime(), 0.0F, false));
	}

	public float transition(String key, boolean on, float seconds) {
		return transition(key, on ? 1.0F : 0.0F, seconds);
	}

	private record Tween(float from, float to, long startNanos, float seconds, boolean easeOut) {
		private float value(long now) {
			if (seconds <= 0.0F) {
				return to;
			}
			float t = Math.clamp((now - startNanos) / (seconds * 1_000_000_000.0F), 0.0F, 1.0F);
			float eased = easeOut
				? 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t)
				: t < 0.5F ? 4.0F * t * t * t : 1.0F - (float) Math.pow(-2.0F * t + 2.0F, 3.0) / 2.0F;
			return from + (to - from) * eased;
		}
	}
}
