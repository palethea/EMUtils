package net.emutils.client.emutils.packs;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.emutils.client.emutils.compat.IrisCompat;
import net.minecraft.client.Minecraft;

public final class InstalledPackScanner {
	private InstalledPackScanner() {
	}

	public static List<InstalledPack> scan(Minecraft client, PackType type, InstalledPackIndex index) throws IOException {
		Path folder = ResourcePackController.folder(client, type);
		Files.createDirectories(folder);
		List<InstalledPack> packs = new ArrayList<>();
		List<String> existingFilenames = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
			for (Path path : stream) {
				if (!isPack(path)) {
					continue;
				}

				String filename = path.getFileName().toString();
				existingFilenames.add(filename);
				boolean enabled = type == PackType.RESOURCE
					? client.options.resourcePacks.contains(resourcePackId(filename))
					: IrisCompat.isActiveShaderPack(filename);
				packs.add(new InstalledPack(type, filename, path, Files.isDirectory(path), index.findByFilename(type, filename).orElse(null), enabled));
			}
		}
		index.removeMissing(type, existingFilenames);
		return packs;
	}

	public static String resourcePackId(String filename) {
		return "file/" + filename;
	}

	private static boolean isPack(Path path) {
		String filename = path.getFileName().toString().toLowerCase();
		if (Files.isRegularFile(path)) {
			return filename.endsWith(".zip");
		}
		if (Files.isDirectory(path)) {
			return Files.exists(path.resolve("pack.mcmeta")) || Files.exists(path.resolve("shaders"));
		}
		return false;
	}
}
