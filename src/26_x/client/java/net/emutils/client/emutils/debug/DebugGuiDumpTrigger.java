package net.emutils.client.emutils.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.KeyMapping;
import org.jspecify.annotations.Nullable;

public final class DebugGuiDumpTrigger {
	private DebugGuiDumpTrigger() {
	}

	public static boolean tryFromBinding(@Nullable KeyMapping binding) {
		if (binding == null) {
			return false;
		}

		boolean triggered = false;
		while (binding.consumeClick()) {
			capture(Minecraft.getInstance());
			triggered = true;
		}

		return triggered;
	}

	public static boolean tryFromInput(@Nullable KeyMapping binding, KeyEvent input) {
		if (binding == null || !binding.matches(input)) {
			return false;
		}

		capture(Minecraft.getInstance());
		return true;
	}

	private static void capture(@Nullable Minecraft client) {
		if (client != null) {
			DebugGuiDumper.capture(client);
		}
	}
}
