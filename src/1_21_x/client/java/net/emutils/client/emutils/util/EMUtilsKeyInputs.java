package net.emutils.client.emutils.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class EMUtilsKeyInputs {
	private EMUtilsKeyInputs() {
	}

	public static boolean hasCtrlOrCmd() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.getWindow() == null) {
			return false;
		}
		return InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
			|| InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL)
			|| InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_LEFT_SUPER)
			|| InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_SUPER);
	}
}
