package net.emutils.client.emutils.packs;

import java.nio.file.Path;

public record PackInstallRecord(
	PackType type,
	String projectId,
	String versionId,
	String filename,
	String sha1,
	String sha512,
	long installedAtMillis,
	String title,
	String iconUrl
) {
	public boolean matches(PackType type, String projectId) {
		return this.type == type && this.projectId.equals(projectId);
	}

	public Path resolve(Path folder) {
		return folder.resolve(filename).normalize();
	}
}
