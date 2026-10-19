package net.emutils.client.emutils.util;

import com.google.gson.Gson;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.text.EmUtilsChatPrefix;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Reads EMUtils' data files (#250). A file that's there but can't be read is kept as {@code <name>.broken}
 * before the defaults that replace it get saved over it, and the player is told in chat the next time they
 * join a world, so nothing is lost without a word.
 */
public final class DataFiles {
	/** Files kept as a broken copy this session that the player hasn't been told about yet. */
	private static final Set<Path> UNREPORTED = new LinkedHashSet<>();

	private DataFiles() {
	}

	/** What reading a file found: its contents, or null when there's no file or it couldn't be read. */
	public record Read<T>(@Nullable T value, boolean unreadable) {
	}

	/** Reads {@code file} as JSON of {@code type}; an unreadable one is kept as a broken copy. */
	public static <T> Read<T> readJson(Path file, Gson gson, Type type) {
		if (!Files.isRegularFile(file)) {
			return new Read<>(null, false);
		}
		try {
			T value = gson.fromJson(Files.readString(file), type);
			return new Read<>(value, false);
		} catch (IOException | RuntimeException exception) {
			EMUtilsClient.LOGGER.warn("Could not read the EMUtils file {}.", file, exception);
			keepBrokenCopy(file);
			return new Read<>(null, true);
		}
	}

	/** Like {@link #readJson}, for stores that start empty either way. */
	public static <T> @Nullable T readJsonOrNull(Path file, Gson gson, Type type) {
		return DataFiles.<T>readJson(file, gson, type).value();
	}

	/** Keeps an unreadable file as {@code <name>.broken} before it's replaced, and remembers to say so. */
	public static void keepBrokenCopy(Path file) {
		Path copy = brokenCopy(file);
		try {
			Files.copy(file, copy, StandardCopyOption.REPLACE_EXISTING);
			EMUtilsClient.LOGGER.warn("Kept the unreadable EMUtils file as {}.", copy);
			synchronized (UNREPORTED) {
				UNREPORTED.add(file);
			}
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("Could not keep a copy of the unreadable EMUtils file {}.", file, exception);
		}
	}

	public static Path brokenCopy(Path file) {
		return file.resolveSibling(file.getFileName() + ".broken");
	}

	/** Says in chat which files couldn't be read and where their copies are; call when joining a world. */
	public static void reportBroken(Minecraft client) {
		if (client.player == null) {
			return;
		}
		List<Path> files;
		synchronized (UNREPORTED) {
			files = new ArrayList<>(UNREPORTED);
			UNREPORTED.clear();
		}
		for (Path file : files) {
			Component message = Component.translatable(EMUtilsTexts.DATA_FILE_UNREADABLE, file.getFileName().toString(), brokenCopy(file).getFileName().toString());
			client.player.sendSystemMessage(EmUtilsChatPrefix.chat(message));
		}
	}

	/** For UI snapshot checks: the files still to be reported. */
	public static List<Path> unreportedForSnapshot() {
		synchronized (UNREPORTED) {
			return new ArrayList<>(UNREPORTED);
		}
	}
}
