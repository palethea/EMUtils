package net.emutils.client.hud;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import net.emutils.client.EMUtilsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Language;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public record HudOverlayData(
	String coordinates,
	String chunkRegion,
	String biome,
	String ping,
	String fps,
	String memory,
	int memoryPercent,
	String facing,
	String serverTime,
	String realTime
) {
	private static final DateTimeFormatter TWENTY_FOUR_HOUR_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH);
	private static final DateTimeFormatter TWELVE_HOUR_FORMAT = DateTimeFormatter.ofPattern("h:mm:ssa", Locale.ENGLISH);
	private static final long MEMORY_UPDATE_INTERVAL_MS = 2_000L;
	private static final HudOverlayData EMPTY = new HudOverlayData("-- -- --", "-- / --", "--", "-- ms", "--", "--/-- GB (--%)", 0, "--", "--:--", "--:--");
	private static MemoryUsage cachedMemoryUsage = new MemoryUsage("--/-- GB (--%)", 0);
	private static long lastMemoryUpdateMillis;

	public static HudOverlayData empty() {
		return EMPTY;
	}

	public static HudOverlayData collect(MinecraftClient client) {
		if (client == null || client.player == null || client.world == null) {
			return EMPTY;
		}

		BlockPos pos = client.player.getBlockPos();
		ChunkPos chunkPos = new ChunkPos(pos);
		int regionX = chunkPos.x >> 5;
		int regionZ = chunkPos.z >> 5;
		MemoryUsage memoryUsage = memoryUsage();

		return new HudOverlayData(
			pos.getX() + " " + pos.getY() + " " + pos.getZ(),
			chunkPos.x + " " + chunkPos.z + " / " + regionX + " " + regionZ,
			biomeName(client, pos),
			ping(client),
			Integer.toString(client.getCurrentFps()),
			memoryUsage.display(),
			memoryUsage.percent(),
			prettify(client.player.getHorizontalFacing().asString()),
			serverTime(client.world.getTimeOfDay()),
			currentRealTime()
		);
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

	private static String biomeName(MinecraftClient client, BlockPos pos) {
		return client.world.getBiome(pos)
			.getKey()
			.map(RegistryKey::getValue)
			.map(id -> {
				String translationKey = "biome." + id.getNamespace() + "." + id.getPath().replace('/', '.');
				Language language = Language.getInstance();
				if (language.hasTranslation(translationKey)) {
					return Text.translatable(translationKey).getString();
				}

				return prettify(id.getPath());
			})
			.orElse("--");
	}

	private static String ping(MinecraftClient client) {
		if (client.getNetworkHandler() == null || client.player == null) {
			return "-- ms";
		}

		PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
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
