package net.emutils.client.versioned;

import com.mojang.blaze3d.Blaze3D;
import java.io.File;
import java.net.URI;

/** Minecraft 26.3 access to desktop integration such as opening files. */
public final class VersionedPlatform {
	private VersionedPlatform() {
	}

	/** Opens a file or folder with the operating system's default handler. */
	public static void openFile(File file) {
		Blaze3D.openPath(file.toPath());
	}

	/** Opens a link in the default browser. */
	public static void openUri(URI uri) {
		Blaze3D.openUri(uri);
	}
}
