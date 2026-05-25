package net.emutils.client.skyblock;

import net.minecraft.client.MinecraftClient;
import org.jspecify.annotations.Nullable;

public final class SkyblockContext {
	private static SkyblockManager manager;

	private SkyblockContext() {
	}

	public static void bind(SkyblockManager boundManager) {
		manager = boundManager;
	}

	public static SkyblockSnapshot snapshot() {
		return manager == null ? SkyblockSnapshot.empty() : manager.snapshot();
	}

	public static SkyblockEvents events() {
		return manager == null ? new SkyblockEvents() : manager.events();
	}

	public static boolean onHypixel(@Nullable MinecraftClient client) {
		if (client != null && SkyblockManager.isHypixel(client)) {
			return true;
		}

		return snapshot().onHypixel();
	}

	public static boolean onAlpha() {
		return snapshot().onAlpha();
	}

	public static boolean inSkyBlock() {
		return snapshot().inSkyBlock();
	}

	public static boolean active() {
		return snapshot().active();
	}

	public static boolean inLobby() {
		return snapshot().inLobby();
	}

	public static boolean inLimbo() {
		return snapshot().inLimbo();
	}

	@Nullable
	public static String profileName() {
		return snapshot().profileName();
	}

	public static SkyblockProfileModes profileModes() {
		return snapshot().profileModes();
	}

	public static SkyblockIsland island() {
		return snapshot().island();
	}

	@Nullable
	public static String area() {
		return snapshot().area();
	}

	@Nullable
	public static String scoreboardTitle() {
		return snapshot().scoreboardTitle();
	}

	@Nullable
	public static String serverId() {
		return snapshot().serverId();
	}

	public static double purse() {
		return snapshot().purse();
	}

	public static double piggyBank() {
		return snapshot().piggyBank();
	}

	@Nullable
	public static String bankBalance() {
		return snapshot().bankBalance();
	}

	public static SkyblockLocrawData locraw() {
		return snapshot().locraw();
	}

	@Nullable
	public static String detectProfile(@Nullable MinecraftClient client) {
		String profile = profileName();
		if (profile != null && !profile.isBlank()) {
			return profile;
		}

		if (client == null) {
			return null;
		}

		return SkyblockTabListReader.read(client, null, null).profileName();
	}
}
