package net.emutils.client.packs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.emutils.client.packs.modrinth.ModrinthClient;
import net.emutils.client.packs.modrinth.ModrinthFile;
import net.emutils.client.packs.modrinth.ModrinthSearchResult;
import net.emutils.client.packs.modrinth.ModrinthVersion;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;

public final class ResourcePackController {
	private ResourcePackController() {
	}

	public static Path folder(MinecraftClient client, PackType type) {
		if (type == PackType.RESOURCE) {
			return client.getResourcePackDir();
		}
		return FabricLoader.getInstance().getGameDir().resolve(type.folderName());
	}

	public static PackInstallRecord install(ModrinthClient modrinth, MinecraftClient client, PackType type, ModrinthSearchResult project, InstalledPackIndex index)
		throws IOException, InterruptedException {
		String minecraftVersion = SharedConstants.getGameVersion().name();
		ModrinthVersion version = modrinth.newestCompatibleVersion(project.projectId(), minecraftVersion);
		ModrinthFile file = version.primaryFile();
		if (file == null) {
			throw new IOException("No downloadable file found.");
		}

		String filename = sanitizeFilename(file.filename());
		Path folder = folder(client, type).normalize();
		Files.createDirectories(folder);
		Path temp = folder.resolve(".emutils-" + UUID.randomUUID() + ".tmp").normalize();
		Path target = folder.resolve(filename).normalize();
		if (!temp.startsWith(folder) || !target.startsWith(folder)) {
			throw new IOException("Unsafe Modrinth filename.");
		}

		modrinth.download(file, temp);
		verifyHash(temp, file);
		Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);

		PackInstallRecord record = new PackInstallRecord(
			type,
			project.projectId(),
			version.id(),
			filename,
			file.sha1(),
			file.sha512(),
			System.currentTimeMillis(),
			project.displayTitle(),
			project.iconUrl()
		);
		index.put(record);
		return record;
	}

	public static PackOperationResult setResourcePackEnabled(MinecraftClient client, String filename, boolean enabled) {
		ResourcePackManager manager = client.getResourcePackManager();
		manager.scanPacks();
		String id = InstalledPackScanner.resourcePackId(filename);
		List<String> enabledIds = new ArrayList<>(manager.getEnabledProfiles().stream().map(profile -> profile.getId()).toList());
		boolean changed;
		if (enabled) {
			if (!manager.hasProfile(id)) {
				return PackOperationResult.error("Pack was installed but Minecraft could not load it.");
			}
			changed = !enabledIds.contains(id);
			if (changed) {
				enabledIds.add(id);
			}
		} else {
			changed = enabledIds.remove(id);
		}

		if (!changed) {
			return PackOperationResult.ok(enabled ? "Already enabled." : "Already disabled.");
		}

		manager.setEnabledProfiles(enabledIds);
		client.options.refreshResourcePacks(manager);
		return PackOperationResult.ok(enabled ? "Enabled resource pack." : "Disabled resource pack.");
	}

	public static PackOperationResult delete(MinecraftClient client, InstalledPackIndex index, InstalledPack pack) {
		Path folder = folder(client, pack.type()).normalize();
		Path path = pack.path().normalize();
		if (!path.startsWith(folder)) {
			return PackOperationResult.error("Refused to delete a pack outside the Minecraft pack folder.");
		}

		if (pack.type() == PackType.RESOURCE && pack.enabled()) {
			PackOperationResult disabled = setResourcePackEnabled(client, pack.filename(), false);
			if (!disabled.success()) {
				return disabled;
			}
		}

		try {
			deletePath(path);
		} catch (IOException exception) {
			return PackOperationResult.error("Could not delete pack: " + exception.getMessage());
		}

		if (pack.record() != null) {
			index.remove(pack.type(), pack.record().projectId());
		}
		return PackOperationResult.ok("Deleted pack.");
	}

	public static PackOperationResult deleteInstalledFile(MinecraftClient client, InstalledPackIndex index, InstalledPack pack) {
		Path folder = folder(client, pack.type()).normalize();
		Path path = pack.path().normalize();
		if (!path.startsWith(folder)) {
			return PackOperationResult.error("Refused to delete a pack outside the Minecraft pack folder.");
		}

		try {
			deletePath(path);
		} catch (IOException exception) {
			return PackOperationResult.error("Could not delete pack: " + exception.getMessage());
		}

		if (pack.record() != null) {
			index.remove(pack.type(), pack.record().projectId());
		}
		return PackOperationResult.ok("Deleted pack.");
	}

	private static void deletePath(Path path) throws IOException {
		if (Files.isDirectory(path)) {
			try (var walk = Files.walk(path)) {
				for (Path nested : walk.sorted((a, b) -> b.getNameCount() - a.getNameCount()).toList()) {
					Files.deleteIfExists(nested);
				}
			}
			return;
		}
		Files.deleteIfExists(path);
	}

	private static void verifyHash(Path path, ModrinthFile file) throws IOException {
		String sha512 = file.sha512();
		if (sha512 != null && !sha512.isBlank()) {
			verifyHash(path, "SHA-512", sha512);
			return;
		}
		String sha1 = file.sha1();
		if (sha1 != null && !sha1.isBlank()) {
			verifyHash(path, "SHA-1", sha1);
		}
	}

	private static void verifyHash(Path path, String algorithm, String expected) throws IOException {
		try {
			MessageDigest digest = MessageDigest.getInstance(algorithm);
			try (InputStream input = new DigestInputStream(Files.newInputStream(path), digest)) {
				input.transferTo(java.io.OutputStream.nullOutputStream());
			}
			String actual = HexFormat.of().formatHex(digest.digest());
			if (!actual.equalsIgnoreCase(expected)) {
				throw new IOException("Downloaded pack failed " + algorithm + " verification.");
			}
		} catch (NoSuchAlgorithmException exception) {
			throw new IOException("Hash algorithm unavailable: " + algorithm, exception);
		}
	}

	private static String sanitizeFilename(String filename) throws IOException {
		if (filename == null || filename.isBlank()) {
			throw new IOException("Modrinth did not provide a filename.");
		}

		String trimmed = filename.trim();
		String lower = trimmed.toLowerCase(Locale.ROOT);
		if (trimmed.contains("/") || trimmed.contains("\\") || trimmed.contains("..") || Path.of(trimmed).isAbsolute() || !lower.endsWith(".zip")) {
			throw new IOException("Unsafe Modrinth filename: " + filename);
		}
		for (int i = 0; i < trimmed.length(); i++) {
			if (Character.isISOControl(trimmed.charAt(i))) {
				throw new IOException("Unsafe Modrinth filename: " + filename);
			}
		}
		return trimmed;
	}

}
