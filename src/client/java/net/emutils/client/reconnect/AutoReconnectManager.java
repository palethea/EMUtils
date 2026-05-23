package net.emutils.client.reconnect;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.compat.ConnectScreenCompat;
import net.emutils.client.util.EMUtilsTexts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

public final class AutoReconnectManager {
	private String lastServerName;
	private String lastServerAddress;
	private long nextReconnectAt;
	private Screen trackedDisconnectScreen;
	private ButtonWidget reconnectButton;

	public void captureCurrentServer(MinecraftClient client) {
		captureServer(client.getCurrentServerEntry());
	}

	public void captureServer(ServerInfo serverInfo) {
		if (serverInfo == null || serverInfo.address == null || serverInfo.address.isBlank()) {
			return;
		}

		lastServerName = serverInfo.name;
		lastServerAddress = serverInfo.address;
	}

	public void onDisconnected() {
		scheduleNextAttempt();
		trackedDisconnectScreen = null;
	}

	public void tick(MinecraftClient client) {
		if (!enabled() || !hasServer() || !(client.currentScreen instanceof DisconnectedScreen)) {
			reconnectButton = null;
			return;
		}

		if (trackedDisconnectScreen != client.currentScreen) {
			trackedDisconnectScreen = client.currentScreen;
			scheduleNextAttempt();
		}

		if (System.currentTimeMillis() >= nextReconnectAt) {
			reconnectNow(client, client.currentScreen);
			return;
		}

		if (reconnectButton != null) {
			reconnectButton.setMessage(buttonText());
		}
	}

	public void setReconnectButton(ButtonWidget reconnectButton) {
		this.reconnectButton = reconnectButton;
	}

	public boolean enabled() {
		return EMUtilsClient.config().autoReconnect();
	}

	public boolean hasServer() {
		return lastServerAddress != null && !lastServerAddress.isBlank();
	}

	public Text buttonText() {
		if (!hasServer()) {
			return Text.translatable(EMUtilsTexts.RECONNECT_UNAVAILABLE);
		}

		return Text.translatable(EMUtilsTexts.RECONNECT_COUNTDOWN, countdownText());
	}

	public String countdownText() {
		long remaining = Math.max(0L, nextReconnectAt - System.currentTimeMillis());
		return String.format("%.1fs", remaining / 1000.0F);
	}

	public void reconnectNow(MinecraftClient client, Screen parent) {
		if (!hasServer()) {
			return;
		}

		scheduleNextAttempt();
		try {
			ServerInfo reconnectServer = new ServerInfo(lastServerName, lastServerAddress, ServerInfo.ServerType.OTHER);
			ConnectScreenCompat.connect(parent, client, ServerAddress.parse(lastServerAddress), reconnectServer);
		} catch (RuntimeException exception) {
			EMUtilsClient.LOGGER.warn("Auto reconnect failed.", exception);
			scheduleNextAttempt();
			if (reconnectButton != null) {
				reconnectButton.setMessage(Text.translatable(EMUtilsTexts.RECONNECT_RETRYING));
			}
		}
	}

	private void scheduleNextAttempt() {
		nextReconnectAt = System.currentTimeMillis() + EMUtilsClient.config().reconnectDelaySeconds() * 1000L;
	}
}
