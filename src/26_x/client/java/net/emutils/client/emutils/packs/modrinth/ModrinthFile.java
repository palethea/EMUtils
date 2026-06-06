package net.emutils.client.emutils.packs.modrinth;

import java.util.Map;

public record ModrinthFile(
	String filename,
	String url,
	Boolean primary,
	long size,
	Map<String, String> hashes
) {
	public boolean isPrimary() {
		return primary != null && primary;
	}

	public String sha1() {
		return hashes == null ? null : hashes.get("sha1");
	}

	public String sha512() {
		return hashes == null ? null : hashes.get("sha512");
	}
}
