package net.emutils.client.minescript;

import java.nio.file.Path;

public record MinescriptScript(
	Path path,
	String relativePath,
	String commandName,
	String displayName,
	boolean directory,
	int depth,
	long modifiedMillis,
	long sizeBytes,
	boolean editable
) {
	public boolean runnable() {
		return !directory && commandName != null && !commandName.isBlank();
	}
}
