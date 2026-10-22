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
import net.minecraft.network.chat.contents.TranslatableContents;

public final class AutoReconnectManager {
	/**
	 * Only a session that stayed connected this long starts the tries over (#255); a server that kicks you
	 * right after joining would otherwise be rejoined forever, whatever Max Tries says.
	 */
	private static final long STABLE_MILLIS = 30_000L;
	/**
	 * Disconnects that retrying can't fix or that were meant (#255): bans, a kick by an operator, not being
	 * on the whitelist, a login of yours elsewhere (rejoining would kick that one, and it you), and a game
	 * version the server doesn't take. The Reconnect button still works for these; it just doesn't count down.
	 * Servers that word their own reasons can't be told apart, so this only knows vanilla's.
	 */
	private static final String[] NO_AUTO_RECONNECT = {
		"multiplayer.disconnect.banned",
		"multiplayer.disconnect.banned_ip",
		"multiplayer.disconnect.ip_banned",
		"multiplayer.disconnect.kicked",
		"multiplayer.disconnect.not_whitelisted",
		"multiplayer.disconnect.duplicate_login",
		"multiplayer.disconnect.outdated_client",
		"multiplayer.disconnect.incompatible"
	};

	private String lastServerName;
	private String lastServerAddress;
	private long nextReconnectAt;
	private int attemptsSinceDisconnect;
	/** When the last join happened, or 0 when not connected. */
	private long joinedAt;
	/** The disconnect screen shown says why in a way that shouldn't be retried on its own. */
	private boolean manualOnly;
	private Screen trackedDisconnectScreen;
	private Screen reconnectParentScreen;
	private Button reconnectButton;

	public void captureCurrentServer(Minecraft client) {
		// The tries aren't reset here: only staying connected for a while does that (#255).
		joinedAt = System.currentTimeMillis();
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
		if (joinedAt > 0L && System.currentTimeMillis() - joinedAt >= STABLE_MILLIS) {
			resetAttempts();
		}
		joinedAt = 0L;
		scheduleNextAttempt();
		trackedDisconnectScreen = null;
		reconnectParentScreen = null;
	}

	public void tick(Minecraft client) {
		if (!enabled() || !hasServer() || !(net.emutils.client.emutils.compat.MinecraftClientCompat.screen(client) instanceof DisconnectedScreen)) {
			reconnectButton = null;
			return;
		}

		track((DisconnectedScreen) net.emutils.client.emutils.compat.MinecraftClientCompat.screen(client));

		if (!manualOnly && System.currentTimeMillis() >= nextReconnectAt && hasAttemptsRemaining()) {
			reconnectNow(client, net.emutils.client.emutils.compat.MinecraftClientCompat.screen(client));
			return;
		}

		if (reconnectButton != null) {
			reconnectButton.setMessage(buttonText());
		}
	}

	/** Takes note of a disconnect screen as it opens: what it goes back to, and whether its reason is retried. */
	public void track(DisconnectedScreen screen) {
		if (trackedDisconnectScreen == screen) {
			return;
		}
		trackedDisconnectScreen = screen;
		manualOnly = noAutoReconnect(((DisconnectedScreenAccess) screen).emutils$getDetails().reason());
		if (reconnectParentScreen == null) {
			reconnectParentScreen = resolveReconnectParent(screen);
		}
		if (nextReconnectAt <= System.currentTimeMillis()) {
			scheduleNextAttempt();
		}
	}

	/** Whether a disconnect reason is one that isn't retried on its own (#255). */
	public static boolean noAutoReconnect(Component reason) {
		if (reason == null || !(reason.getContents() instanceof TranslatableContents translatable)) {
			return false;
		}
		String key = translatable.getKey();
		for (String prefix : NO_AUTO_RECONNECT) {
			if (key.equals(prefix) || key.startsWith(prefix + ".")) {
				return true;
			}
		}
		return false;
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

		if (manualOnly) {
			return Component.translatable(EMUtilsTexts.RECONNECT_MANUAL);
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

	/** For UI snapshot checks: the tries since the last stable session. */
	public int attemptsForSnapshot() {
		return attemptsSinceDisconnect;
	}

	/** For UI snapshot checks: counts a try without connecting anywhere. */
	public void countAttemptForSnapshot() {
		attemptsSinceDisconnect++;
	}

	/** For UI snapshot checks: when the last join happened. */
	public void joinedAtForSnapshot(long millis) {
		joinedAt = millis;
	}

	private static Screen resolveReconnectParent(DisconnectedScreen screen) {
		Screen parent = ((DisconnectedScreenAccess) screen).emutils$getParentScreen();
		while (parent instanceof DisconnectedScreen disconnectedParent) {
			parent = ((DisconnectedScreenAccess) disconnectedParent).emutils$getParentScreen();
		}

		return parent;
	}
}
