package net.emutils.client.packs;

import java.nio.file.Path;
import org.jspecify.annotations.Nullable;

public record InstalledPack(
	PackType type,
	String filename,
	Path path,
	boolean directory,
	@Nullable PackInstallRecord record,
	boolean enabled
) {
	public boolean emutilsInstalled() {
		return record != null;
	}
}
