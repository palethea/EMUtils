package net.emutils.client.emutils.hud;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayDeque;
import java.util.Deque;
import net.emutils.client.emutils.accessor.KeyBindingAccess;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Clicks per second for the Keystrokes overlay (#43): every press of the attack and use keys in the
 * last second. Presses come from {@link KeyMapping#click}, so each one counts once, even several in
 * one frame, and whatever button or key the attack and use keys are bound to.
 */
public final class ClickCounter {
	private static final long WINDOW_MILLIS = 1000L;
	private static final Deque<Long> LEFT = new ArrayDeque<>();
	private static final Deque<Long> RIGHT = new ArrayDeque<>();

	private ClickCounter() {
	}

	/** Called for every key or button press that reaches the key mappings. */
	public static void onPress(InputConstants.Key key) {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.options == null) {
			return;
		}
		long now = System.currentTimeMillis();
		if (key.equals(boundKey(client.options.keyAttack))) {
			record(LEFT, now);
		}
		if (key.equals(boundKey(client.options.keyUse))) {
			record(RIGHT, now);
		}
	}

	public static int leftCps() {
		return count(LEFT, System.currentTimeMillis());
	}

	public static int rightCps() {
		return count(RIGHT, System.currentTimeMillis());
	}

	/** Clicks of each button at the given times, for the UI snapshot checks. */
	public static int countForSnapshot(long[] clickMillis, long nowMillis) {
		Deque<Long> clicks = new ArrayDeque<>();
		for (long click : clickMillis) {
			record(clicks, click);
		}
		return count(clicks, nowMillis);
	}

	private static InputConstants.Key boundKey(KeyMapping mapping) {
		return ((KeyBindingAccess) mapping).emhelpers$getBoundKey();
	}

	private static void record(Deque<Long> clicks, long now) {
		clicks.addLast(now);
		prune(clicks, now);
	}

	private static int count(Deque<Long> clicks, long now) {
		prune(clicks, now);
		return clicks.size();
	}

	private static void prune(Deque<Long> clicks, long now) {
		while (!clicks.isEmpty() && now - clicks.peekFirst() >= WINDOW_MILLIS) {
			clicks.removeFirst();
		}
	}
}
