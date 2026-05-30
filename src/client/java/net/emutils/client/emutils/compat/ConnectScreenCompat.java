package net.emutils.client.emutils.compat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

public final class ConnectScreenCompat {
	private ConnectScreenCompat() {
	}

	public static void connect(Screen parent, MinecraftClient client, ServerAddress address, ServerInfo serverInfo) {
		ConnectScreen.connect(parent, client, address, serverInfo, false, null);
	}
}
