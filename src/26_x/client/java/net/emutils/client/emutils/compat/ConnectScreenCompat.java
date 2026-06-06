package net.emutils.client.emutils.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;

public final class ConnectScreenCompat {
	private ConnectScreenCompat() {
	}

	public static void connect(Screen parent, Minecraft client, ServerAddress address, ServerData serverInfo) {
		ConnectScreen.startConnecting(parent, client, address, serverInfo, false, null);
	}
}
