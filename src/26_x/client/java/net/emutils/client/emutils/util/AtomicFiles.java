package net.emutils.client.emutils.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Writes files so they're never left empty or half written: the content goes to a temporary file next
 * to the target first, which then replaces it in one step. Writing the target directly empties it
 * first, so a crash or a killed game mid-write would lose everything in it.
 */
public final class AtomicFiles {
	private AtomicFiles() {
	}

	public static void writeString(Path target, String content) throws IOException {
		Files.createDirectories(target.getParent());
		Path temp = target.resolveSibling(target.getFileName() + ".tmp");
		Files.writeString(temp, content, StandardCharsets.UTF_8);
		try {
			Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (AtomicMoveNotSupportedException exception) {
			Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException exception) {
			Files.deleteIfExists(temp);
			throw exception;
		}
	}
}
