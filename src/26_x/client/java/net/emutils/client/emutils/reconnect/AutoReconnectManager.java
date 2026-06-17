package net.emutils.client.emutils.reconnect;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.compat.ConnectScreenCompat;
import net.emutils.client.mixin.DisconnectedScreenAccess;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

public final class AutoReconnectManager {
	private String lastServerName;
	private String lastServerAddress;
	private long nextReconnectAt;
	private int attemptsSinceDisconnect;
	private Screen trackedDisconnectScreen;
	private Screen reconnectParentScreen;
	private Button reconnectButton;

	public void captureCurrentServer(Minecraft client) {
		resetAttempts();
		reconnectParentScreen = null;
		captureServer(client.getCurrentServer());
	}

	public void captureServer(ServerData serverInfo) {
		if (serverInfo == null || serverInfo.ip == null || serverInfo.ip.isBlank()) {
			return;
		}

		if (lastServerAddress == null || !lastServerAddress.equals(serverInfo.ip)) {
			resetAttempts();
			nextReconnectAt = 0L;
			reconnectParentScreen = null;
		}

		lastServerName = serverInfo.name;
		lastServerAddress = serverInfo.ip;
	}

	public void onDisconnected() {
		resetAttempts();
		scheduleNextAttempt();
		trackedDisconnectScreen = null;
		reconnectParentScreen = null;
	}

	public void tick(Minecraft client) {
		if (!enabled() || !hasServer() || !(client.gui.screen() instanceof DisconnectedScreen)) {
			reconnectButton = null;
			return;
		}

		if (trackedDisconnectScreen != client.gui.screen()) {
			trackedDisconnectScreen = client.gui.screen();
			if (reconnectParentScreen == null) {
				reconnectParentScreen = resolveReconnectParent((DisconnectedScreen) client.gui.screen());
			}
			if (nextReconnectAt <= System.currentTimeMillis()) {
				scheduleNextAttempt();
			}
		}

		if (System.currentTimeMillis() >= nextReconnectAt && hasAttemptsRemaining()) {
			reconnectNow(client, client.gui.screen());
			return;
		}

		if (reconnectButton != null) {
			reconnectButton.setMessage(buttonText());
		}
	}

	public void setReconnectButton(Button reconnectButton) {
		this.reconnectButton = reconnectButton;
	}

	public boolean enabled() {
		return EMUtilsClient.config().autoReconnect();
	}

	public boolean hasServer() {
		return lastServerAddress != null && !lastServerAddress.isBlank();
	}

	public Component buttonText() {
		if (!hasServer()) {
			return Component.translatable(EMUtilsTexts.RECONNECT_UNAVAILABLE);
		}

		if (!hasAttemptsRemaining()) {
			return Component.translatable(EMUtilsTexts.RECONNECT_EXHAUSTED);
		}

		if (EMUtilsClient.config().autoReconnectUnlimitedTries()) {
			return Component.translatable(EMUtilsTexts.RECONNECT_COUNTDOWN, countdownText());
		}

		int maxTries = EMUtilsClient.config().autoReconnectMaxTries();
		int currentAttempt = Math.min(attemptsSinceDisconnect + 1, maxTries);
		return Component.translatable(EMUtilsTexts.RECONNECT_COUNTDOWN_ATTEMPTS, countdownText(), currentAttempt, maxTries);
	}

	public String countdownText() {
		long remaining = Math.max(0L, nextReconnectAt - System.currentTimeMillis());
		return String.format("%.1fs", remaining / 1000.0F);
	}

	public void reconnectNow(Minecraft client, Screen parent) {
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
			ServerData reconnectServer = new ServerData(lastServerName, lastServerAddress, ServerData.Type.OTHER);
			ConnectScreenCompat.connect(connectParent, client, ServerAddress.parseString(lastServerAddress), reconnectServer);
		} catch (RuntimeException exception) {
			EMUtilsClient.LOGGER.warn("Auto reconnect failed.", exception);
			scheduleNextAttempt();
			if (reconnectButton != null) {
				reconnectButton.setMessage(Component.translatable(EMUtilsTexts.RECONNECT_RETRYING));
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
