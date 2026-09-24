package net.emutils.client.emutils.util;

import net.emutils.client.EMUtilsClient;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.Nullable;

/**
 * What this jar is: its version, and whether it is a dev build. Dev builds report
 * {@code <mod_version>+dev.<commit>} (plus {@code .dirty} for uncommitted changes); release builds report
 * the plain {@code mod_version}.
 */
public final class EMUtilsBuild {
	private static final String DEV_MARKER = "+dev";
	private static final String VERSION = FabricLoader.getInstance()
		.getModContainer(EMUtilsClient.MOD_ID)
		.map(container -> container.getMetadata().getVersion().getFriendlyString())
		.orElse("unknown");

	private EMUtilsBuild() {
	}

	/** The full version, including dev build metadata. */
	public static String version() {
		return VERSION;
	}

	/** The mod version without build metadata, for example {@code 0.14.1}. */
	public static String modVersion() {
		int plus = VERSION.indexOf('+');
		return plus < 0 ? VERSION : VERSION.substring(0, plus);
	}

	public static boolean isDev() {
		return VERSION.contains(DEV_MARKER);
	}

	/** The short commit a dev build was made from, with {@code *} when it had uncommitted changes. */
	public static @Nullable String devCommit() {
		int start = VERSION.indexOf(DEV_MARKER);
		if (start < 0) {
			return null;
		}

		String rest = VERSION.substring(start + DEV_MARKER.length());
		if (rest.startsWith(".")) {
			rest = rest.substring(1);
		}
		boolean dirty = rest.endsWith(".dirty");
		if (dirty) {
			rest = rest.substring(0, rest.length() - ".dirty".length());
		}
		return rest.isEmpty() ? null : dirty ? rest + "*" : rest;
	}
}
