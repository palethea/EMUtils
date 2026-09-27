package net.emutils.client.emutils.hud;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.locale.Language;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public record HudOverlayData(
	String coordinates,
	String freeCameraCoordinates,
	String portalCoordinates,
	String chunkRegion,
	String biome,
	String ping,
	String fps,
	String memory,
	int memoryPercent,
	String facing,
	String speed,
	String serverTime,
	String realTime,
	String lockedYPlacement,
	String dimension,
	String dayNight,
	String slimeChunk,
	String targetBlock,
	HudTpsTracker.Reading tps
) {
	private static final DateTimeFormatter TWENTY_FOUR_HOUR_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH);
	private static final DateTimeFormatter TWELVE_HOUR_FORMAT = DateTimeFormatter.ofPattern("h:mm:ssa", Locale.ENGLISH);
	private static final long MEMORY_UPDATE_INTERVAL_MS = 2_000L;
	/** Slime chunks come from the world seed and this salt, as in {@code Slime.checkSlimeSpawnRules}. */
	private static final long SLIME_CHUNK_SALT = 987234911L;
	/** Clock ticks, with 0 at sunrise: monsters start spawning at dusk (13000) and night ends at 23000. */
	private static final long NIGHT_START = 13000L;
	private static final long DAY_START = 23000L;
	private static final HudOverlayData EMPTY = new HudOverlayData("-- -- --", "-- -- --", "-- -- --", "-- / --", "--", "-- ms", "--", "--/-- GB (--%)", 0, "--", "--", "--:--", "--:--", "--", "--", "--", "--", "--", HudTpsTracker.Reading.NONE);
	private static long lastDayTime = Long.MIN_VALUE;
	private static long dayTimeChangedMillis;
	private static MemoryUsage cachedMemoryUsage = new MemoryUsage("--/-- GB (--%)", 0);
	private static long lastMemoryUpdateMillis;

	public static HudOverlayData empty() {
		return EMPTY;
	}

	public static HudOverlayData collect(Minecraft client) {
		if (client == null || client.player == null || client.level == null) {
			return EMPTY;
		}

		BlockPos pos = client.player.blockPosition();
		ChunkPos chunkPos = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
		int regionX = chunkPos.x() >> 5;
		int regionZ = chunkPos.z() >> 5;
		MemoryUsage memoryUsage = memoryUsage();

		return new HudOverlayData(
			pos.getX() + " " + pos.getY() + " " + pos.getZ(),
			collectFreeCameraCoordinates(),
			portalCoordinates(client, pos),
			chunkPos.x() + " " + chunkPos.z() + " / " + regionX + " " + regionZ,
			biomeName(client, pos),
			ping(client),
			Integer.toString(client.getFps()),
			memoryUsage.display(),
			memoryUsage.percent(),
			prettify(client.player.getDirection().name().toLowerCase(Locale.ENGLISH)),
			HudSpeedTracker.collect(client),
			serverTime(client.level.getOverworldClockTime()),
			currentRealTime(),
			lockedYPlacementValue(),
			dimensionName(client),
			dayNight(client),
			slimeChunk(client, chunkPos),
			targetBlock(client),
			HudTpsTracker.read(client)
		);
	}

	private static String dimensionName(Minecraft client) {
		Identifier id = client.level.dimension().identifier();
		String key = "emutils.hud.dimension." + id.getNamespace() + "." + id.getPath().replace('/', '.');
		return Language.getInstance().has(key) ? Component.translatable(key).getString() : prettify(id.getPath());
	}

	/**
	 * How long until night (dusk, when monsters start spawning) or until day, in real minutes and seconds
	 * at the current tick rate; "Time stopped" while the clock doesn't move, such as with the daylight
	 * cycle turned off.
	 */
	private static String dayNight(Minecraft client) {
		long dayTime = Math.floorMod(client.level.getOverworldClockTime(), 24000L);
		long now = System.currentTimeMillis();
		// A paused singleplayer world doesn't count as the clock standing still.
		if (dayTime != lastDayTime || client.isPaused()) {
			lastDayTime = dayTime;
			dayTimeChangedMillis = now;
		} else if (now - dayTimeChangedMillis > 2_000L) {
			return Component.translatable(EMUtilsTexts.HUD_DAY_NIGHT_STOPPED).getString();
		}
		boolean night = dayTime >= NIGHT_START && dayTime < DAY_START;
		long ticksLeft = night ? DAY_START - dayTime : Math.floorMod(NIGHT_START - dayTime, 24000L);
		float tickRate = client.level.tickRateManager().tickrate();
		long seconds = (long) Math.ceil(ticksLeft / (double) (tickRate <= 0.0F ? 20.0F : tickRate));
		String time = String.format(Locale.ENGLISH, "%d:%02d", seconds / 60L, seconds % 60L);
		return Component.translatable(night ? EMUtilsTexts.HUD_DAY_IN : EMUtilsTexts.HUD_NIGHT_IN, time).getString();
	}

	/**
	 * Whether the player stands in a slime chunk. That depends on the world seed, which only a
	 * singleplayer world tells the client, and only the Overworld has slime chunks.
	 */
	private static String slimeChunk(Minecraft client, ChunkPos chunkPos) {
		if (client.level.dimension() != Level.OVERWORLD) {
			return "--";
		}
		IntegratedServer server = client.getSingleplayerServer();
		if (server == null) {
			return Component.translatable(EMUtilsTexts.HUD_SLIME_CHUNK_UNKNOWN).getString();
		}
		boolean slime = WorldgenRandom.seedSlimeChunk(chunkPos.x(), chunkPos.z(), server.overworld().getSeed(), SLIME_CHUNK_SALT).nextInt(10) == 0;
		return Component.translatable(slime ? EMUtilsTexts.HUD_YES : EMUtilsTexts.HUD_NO).getString();
	}

	private static String targetBlock(Minecraft client) {
		if (client.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
			BlockPos pos = hit.getBlockPos();
			return pos.getX() + " " + pos.getY() + " " + pos.getZ();
		}
		return "--";
	}

	private static String collectFreeCameraCoordinates() {
		if (EMUtilsClient.tweaks() == null || !EMUtilsClient.tweaks().freeCamera().isActive()) {
			return "-- -- --";
		}

		BlockPos pos = EMUtilsClient.tweaks().freeCamera().cameraBlockPosition();
		return pos == null ? "-- -- --" : pos.getX() + " " + pos.getY() + " " + pos.getZ();
	}

	private static String lockedYPlacementValue() {
		if (EMUtilsClient.tweaks() == null) {
			return "--";
		}

		Integer y = EMUtilsClient.tweaks().lockedYPlacement().lockedY();
		return y == null ? "--" : Integer.toString(y);
	}

	private static String portalCoordinates(Minecraft client, BlockPos pos) {
		if (client.level.dimension() == Level.OVERWORLD) {
			return Math.floorDiv(pos.getX(), 8) + " " + pos.getY() + " " + Math.floorDiv(pos.getZ(), 8);
		}
		return "-- -- --";
	}

	private static MemoryUsage memoryUsage() {
		long nowMillis = System.currentTimeMillis();
		if (nowMillis - lastMemoryUpdateMillis >= MEMORY_UPDATE_INTERVAL_MS) {
			cachedMemoryUsage = readMemoryUsage();
			lastMemoryUpdateMillis = nowMillis;
		}

		return cachedMemoryUsage;
	}

	private static MemoryUsage readMemoryUsage() {
		Runtime runtime = Runtime.getRuntime();
		long used = runtime.totalMemory() - runtime.freeMemory();
		long max = runtime.maxMemory();
		int percent = max <= 0L ? 0 : (int) Math.round(used * 100.0 / max);
		return new MemoryUsage(formatMemory(used) + "/" + formatMemory(max) + " (" + percent + "%)", percent);
	}

	private static String formatMemory(long bytes) {
		double gigabytes = bytes / (1024.0 * 1024.0 * 1024.0);
		if (gigabytes >= 1.0) {
			return String.format(Locale.ENGLISH, "%.1f GB", gigabytes);
		}

		double megabytes = bytes / (1024.0 * 1024.0);
		return String.format(Locale.ENGLISH, "%.0f MB", megabytes);
	}

	private record MemoryUsage(String display, int percent) {
	}

	private static String biomeName(Minecraft client, BlockPos pos) {
		return client.level.getBiome(pos)
			.unwrapKey()
			.map(ResourceKey::identifier)
			.map(id -> {
				String translationKey = "biome." + id.getNamespace() + "." + id.getPath().replace('/', '.');
				Language language = Language.getInstance();
				if (language.has(translationKey)) {
					return Component.translatable(translationKey).getString();
				}

				return prettify(id.getPath());
			})
			.orElse("--");
	}

	private static String ping(Minecraft client) {
		if (client.getConnection() == null || client.player == null) {
			return "-- ms";
		}

		PlayerInfo entry = client.getConnection().getPlayerInfo(client.player.getUUID());
		return entry == null ? "-- ms" : entry.getLatency() + " ms";
	}

	private static String serverTime(long timeOfDay) {
		long dayTime = Math.floorMod(timeOfDay + 6000L, 24000L);
		long hours = dayTime / 1000L;
		long minutes = (dayTime % 1000L) * 60L / 1000L;
		return String.format(Locale.ENGLISH, "%02d:%02d", hours, minutes);
	}

	private static String currentRealTime() {
		boolean twentyFourHour = EMUtilsClient.config() == null || EMUtilsClient.config().chatTimestamp24Hour();
		return LocalTime.now().format(twentyFourHour ? TWENTY_FOUR_HOUR_FORMAT : TWELVE_HOUR_FORMAT);
	}

	private static String prettify(String path) {
		String[] parts = path.replace('/', '_').split("_");
		StringBuilder builder = new StringBuilder();
		for (String part : parts) {
			if (part.isEmpty()) {
				continue;
			}

			if (!builder.isEmpty()) {
				builder.append(' ');
			}
			builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
		}

		return builder.isEmpty() ? path : builder.toString();
	}
}
