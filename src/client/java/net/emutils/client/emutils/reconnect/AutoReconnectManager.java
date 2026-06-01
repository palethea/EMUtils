package net.emutils.client.emutils.reconnect;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.compat.ConnectScreenCompat;
import net.emutils.client.mixin.DisconnectedScreenAccess;
import net.emutils.client.emutils.util.EMUtilsTexts;
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
	private int attemptsSinceDisconnect;
	private Screen trackedDisconnectScreen;
	private Screen reconnectParentScreen;
	private ButtonWidget reconnectButton;

	public void captureCurrentServer(MinecraftClient client) {
		resetAttempts();
		reconnectParentScreen = null;
		captureServer(client.getCurrentServerEntry());
	}

	public void captureServer(ServerInfo serverInfo) {
		if (serverInfo == null || serverInfo.address == null || serverInfo.address.isBlank()) {
			return;
		}

		if (lastServerAddress == null || !lastServerAddress.equals(serverInfo.address)) {
			resetAttempts();
			nextReconnectAt = 0L;
			reconnectParentScreen = null;
		}

		lastServerName = serverInfo.name;
		lastServerAddress = serverInfo.address;
	}

	public void onDisconnected() {
		resetAttempts();
		scheduleNextAttempt();
		trackedDisconnectScreen = null;
		reconnectParentScreen = null;
	}

	public void tick(MinecraftClient client) {
		if (!enabled() || !hasServer() || !(client.currentScreen instanceof DisconnectedScreen)) {
			reconnectButton = null;
			return;
		}

		if (trackedDisconnectScreen != client.currentScreen) {
			trackedDisconnectScreen = client.currentScreen;
			if (reconnectParentScreen == null) {
				reconnectParentScreen = resolveReconnectParent((DisconnectedScreen) client.currentScreen);
			}
			if (nextReconnectAt <= System.currentTimeMillis()) {
				scheduleNextAttempt();
			}
		}

		if (System.currentTimeMillis() >= nextReconnectAt && hasAttemptsRemaining()) {
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

		if (!hasAttemptsRemaining()) {
			return Text.translatable(EMUtilsTexts.RECONNECT_EXHAUSTED);
		}

		if (EMUtilsClient.config().autoReconnectUnlimitedTries()) {
			return Text.translatable(EMUtilsTexts.RECONNECT_COUNTDOWN, countdownText());
		}

		int maxTries = EMUtilsClient.config().autoReconnectMaxTries();
		int currentAttempt = Math.min(attemptsSinceDisconnect + 1, maxTries);
		return Text.translatable(EMUtilsTexts.RECONNECT_COUNTDOWN_ATTEMPTS, countdownText(), currentAttempt, maxTries);
	}

	public String countdownText() {
		long remaining = Math.max(0L, nextReconnectAt - System.currentTimeMillis());
		return String.format("%.1fs", remaining / 1000.0F);
	}

	public void reconnectNow(MinecraftClient client, Screen parent) {
		if (!hasServer()) {
			return;
		}

		if (!hasAttemptsRemaining()) {
			resetAttempts();
		}

		attemptsSinceDisconnect++;
		scheduleNextAttempt();
		Screen connectParent = reconnectParentScreen != null ? reconnectParentScreen : parent;
		try {
			ServerInfo reconnectServer = new ServerInfo(lastServerName, lastServerAddress, ServerInfo.ServerType.OTHER);
			ConnectScreenCompat.connect(connectParent, client, ServerAddress.parse(lastServerAddress), reconnectServer);
		} catch (RuntimeException exception) {
			EMUtilsClient.LOGGER.warn("Auto reconnect failed.", exception);
			scheduleNextAttempt();
			if (reconnectButton != null) {
				reconnectButton.setMessage(Text.translatable(EMUtilsTexts.RECONNECT_RETRYING));
			}
		}
	}

	private boolean hasAttemptsRemaining() {
		if (EMUtilsClient.config().autoReconnectUnlimitedTries()) {
			return true;
		}

		return attemptsSinceDisconnect < EMUtilsClient.config().autoReconnectMaxTries();
	}

	private void resetAttempts() {
		attemptsSinceDisconnect = 0;
	}

	private void scheduleNextAttempt() {
		nextReconnectAt = System.currentTimeMillis() + EMUtilsClient.config().reconnectDelaySeconds() * 1000L;
	}

	private static Screen resolveReconnectParent(DisconnectedScreen screen) {
		Screen parent = ((DisconnectedScreenAccess) screen).emutils$getParentScreen();
		while (parent instanceof DisconnectedScreen disconnectedParent) {
			parent = ((DisconnectedScreenAccess) disconnectedParent).emutils$getParentScreen();
		}

		return parent;
	}
}
