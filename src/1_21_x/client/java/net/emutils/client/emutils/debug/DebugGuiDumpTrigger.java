package net.emutils.client.emutils.debug;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import org.jspecify.annotations.Nullable;

public final class DebugGuiDumpTrigger {
	private DebugGuiDumpTrigger() {
	}

	public static boolean tryFromBinding(@Nullable KeyBinding binding) {
		if (binding == null) {
			return false;
		}

		boolean triggered = false;
		while (binding.wasPressed()) {
			capture(MinecraftClient.getInstance());
			triggered = true;
		}

		return triggered;
	}

	public static boolean tryFromInput(@Nullable KeyBinding binding, KeyInput input) {
		if (binding == null || !binding.matchesKey(input)) {
			return false;
		}

		capture(MinecraftClient.getInstance());
		return true;
	}

	private static void capture(@Nullable MinecraftClient client) {
		if (client != null) {
			DebugGuiDumper.capture(client);
		}
	}
}
