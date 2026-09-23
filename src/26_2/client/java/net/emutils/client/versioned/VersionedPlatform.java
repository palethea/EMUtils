package net.emutils.client.versioned;

import java.io.File;
import net.minecraft.util.Util;

/** Minecraft 26.2 access to desktop integration such as opening files. */
public final class VersionedPlatform {
	private VersionedPlatform() {
	}

	/** Opens a file or folder with the operating system's default handler. */
	public static void openFile(File file) {
		Util.getPlatform().openFile(file);
	}
}
