package net.emutils.client.emutils.minescript;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import net.emutils.client.emutils.compat.MinescriptCompat;

public final class MinescriptScriptRepository {
	private static final Set<String> IGNORED_DIRECTORIES = Set.of("system", "blockpacks", "undo");
	private static final String TEMPLATE = "import minescript\n\nminescript.echo(\"Hello from EMUtils\")\n";
	private final Path root;

	public MinescriptScriptRepository() {
		this(MinescriptCompat.scriptsDir());
	}

	public MinescriptScriptRepository(Path root) {
		this.root = root.normalize().toAbsolutePath();
	}

	public Path root() {
		return root;
	}

	public List<MinescriptScript> scan() throws IOException {
		Files.createDirectories(root);
		List<MinescriptScript> scripts = new ArrayList<>();
		scanDirectory(root, 0, scripts);
		return scripts;
	}

	public String read(MinescriptScript script) throws IOException {
		if (script.directory() || !script.editable()) {
			return "";
		}
		Path path = safePath(script.path());
		return Files.readString(path, StandardCharsets.UTF_8);
	}

	public void write(MinescriptScript script, String content) throws IOException {
		if (script.directory() || !script.editable()) {
			throw new IOException("Only .py Minescript files can be edited.");
		}
		Path path = safePath(script.path());
		Files.writeString(path, content == null ? "" : content, StandardCharsets.UTF_8);
	}

	public MinescriptScript createScript(String relativeName) throws IOException {
		String normalized = normalizeNewScriptName(relativeName);
		Path path = safePath(root.resolve(normalized));
		if (Files.exists(path)) {
			throw new IOException("Script already exists.");
		}
		Files.createDirectories(path.getParent());
		Files.writeString(path, TEMPLATE, StandardCharsets.UTF_8);
		return toScript(path, depth(path));
	}

	public boolean existsCommand(String command) {
		if (command == null || command.isBlank()) {
			return false;
		}
		try {
			return Files.isRegularFile(safePath(root.resolve(command + ".py"))) || Files.isRegularFile(safePath(root.resolve(command + ".pyj")));
		} catch (IOException ignored) {
			return false;
		}
	}

	public boolean isSafeCommand(String command) {
		return command != null && command.matches("[A-Za-z0-9_./-]+") && !command.contains("..") && !command.startsWith("/") && !command.endsWith("/");
	}

	/** Creates the folder {@code relativeName} (and any above it) inside the minescript folder; returns its path. */
	public String createFolder(String relativeName) throws IOException {
		String normalized = normalizeFolderName(relativeName);
		Path path = safePath(root.resolve(normalized));
		if (Files.exists(path)) {
			throw new IOException("A script or folder with that name already exists.");
		}
		Files.createDirectories(path);
		return normalized;
	}

	/**
	 * Renames or moves a script or folder to {@code newRelative}, creating folders on the way. A script
	 * keeps its extension when the new name has none. Returns the new path inside the minescript folder.
	 */
	public String move(MinescriptScript item, String newRelative) throws IOException {
		Path source = safePath(item.path());
		String normalized;
		if (item.directory()) {
			normalized = normalizeFolderName(newRelative);
			if ((normalized + "/").startsWith(item.relativePath() + "/")) {
				throw new IOException("A folder can't be moved into itself.");
			}
		} else {
			normalized = normalizeScriptName(newRelative, item.relativePath().toLowerCase(Locale.ROOT).endsWith(".pyj") ? ".pyj" : ".py");
		}
		if (normalized.equals(item.relativePath())) {
			return normalized;
		}
		Path target = safePath(root.resolve(normalized));
		// A case-only rename on Windows finds the source itself; that's still a rename.
		if (Files.exists(target) && !normalized.equalsIgnoreCase(item.relativePath())) {
			throw new IOException("A script or folder with that name already exists.");
		}
		Files.createDirectories(target.getParent());
		Files.move(source, target);
		return normalized;
	}

	/** How many scripts are inside {@code folder}, in any of its subfolders. */
	public int countScripts(MinescriptScript folder) {
		try (Stream<Path> paths = Files.walk(safePath(folder.path()))) {
			return (int) paths.filter(path -> Files.isRegularFile(path) && isVisibleScriptPath(path)).count();
		} catch (IOException | UncheckedIOException exception) {
			// Directory streams report read errors, such as a subfolder that can't be opened, unchecked.
			return 0;
		}
	}

	/** Deletes {@code folder} and everything in it. */
	public void deleteFolder(MinescriptScript folder) throws IOException {
		if (!folder.directory()) {
			throw new IOException("Not a folder.");
		}
		Path path = safePath(folder.path());
		if (path.equals(root)) {
			throw new IOException("The minescript folder itself can't be deleted.");
		}
		try (Stream<Path> paths = Files.walk(path)) {
			for (Path child : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.delete(safePath(child));
			}
		} catch (UncheckedIOException exception) {
			// Directory streams report read errors, such as a subfolder that can't be opened, unchecked.
			throw exception.getCause();
		}
	}

	public void delete(MinescriptScript script) throws IOException {
		if (script.directory() || !script.editable()) {
			throw new IOException("Only editable .py scripts can be deleted.");
		}
		Files.delete(safePath(script.path()));
	}

	private void scanDirectory(Path directory, int depth, List<MinescriptScript> scripts) throws IOException {
		List<Path> children;
		try (var stream = Files.list(directory)) {
			children = stream
				.filter(this::isVisibleScriptPath)
				.sorted(Comparator.comparing((Path path) -> !Files.isDirectory(path)).thenComparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
				.toList();
		} catch (UncheckedIOException exception) {
			// Directory streams report read errors unchecked; the screen handles the IOException.
			throw exception.getCause();
		}

		for (Path child : children) {
			MinescriptScript script = toScript(child, depth);
			scripts.add(script);
			if (script.directory()) {
				scanDirectory(child, depth + 1, scripts);
			}
		}
	}

	private boolean isVisibleScriptPath(Path path) {
		String filename = path.getFileName().toString();
		if (Files.isDirectory(path)) {
			return !IGNORED_DIRECTORIES.contains(filename.toLowerCase(Locale.ROOT));
		}
		String lower = filename.toLowerCase(Locale.ROOT);
		return lower.endsWith(".py") || lower.endsWith(".pyj");
	}

	private MinescriptScript toScript(Path path, int depth) throws IOException {
		Path safePath = safePath(path);
		boolean directory = Files.isDirectory(safePath);
		String relative = root.relativize(safePath).toString().replace('\\', '/');
		String display = safePath.getFileName().toString();
		String command = null;
		boolean editable = false;
		if (!directory) {
			String lower = relative.toLowerCase(Locale.ROOT);
			editable = lower.endsWith(".py");
			if (lower.endsWith(".py")) {
				command = relative.substring(0, relative.length() - 3);
			} else if (lower.endsWith(".pyj")) {
				command = relative.substring(0, relative.length() - 4);
			}
		}
		long modified = directory ? 0L : Files.getLastModifiedTime(safePath).toMillis();
		long size = directory ? 0L : Files.size(safePath);
		return new MinescriptScript(safePath, relative, command, display, directory, depth, modified, size, editable);
	}

	private Path safePath(Path path) throws IOException {
		Path normalized = path.normalize().toAbsolutePath();
		if (!normalized.startsWith(root)) {
			throw new IOException("Path is outside the Minescript folder.");
		}
		return normalized;
	}

	private int depth(Path path) {
		Path parent = path.getParent();
		if (parent == null || parent.equals(root)) {
			return 0;
		}
		return root.relativize(parent).getNameCount();
	}

	private String normalizeNewScriptName(String relativeName) throws IOException {
		return normalizeScriptName(relativeName, ".py");
	}

	private String normalizeScriptName(String relativeName, String extension) throws IOException {
		if (relativeName == null || relativeName.isBlank()) {
			throw new IOException("Script name cannot be empty.");
		}
		String normalized = relativeName.trim().replace('\\', '/');
		String lower = normalized.toLowerCase(Locale.ROOT);
		if (!lower.endsWith(".py") && !lower.endsWith(".pyj")) {
			normalized += extension;
		}
		if (normalized.startsWith("/") || normalized.contains("..") || normalized.contains("//") || normalized.matches(".*[\\p{Cntrl}:*?\"<>|].*")) {
			throw new IOException("Unsafe script name.");
		}
		String command = normalized.substring(0, normalized.lastIndexOf('.'));
		if (!isSafeCommand(command)) {
			throw new IOException("Script name cannot be converted to a safe Minescript command.");
		}
		return normalized;
	}

	/** Folder names follow the same rules as script commands, so the scripts inside can still run. */
	private String normalizeFolderName(String relativeName) throws IOException {
		if (relativeName == null || relativeName.isBlank()) {
			throw new IOException("Folder name cannot be empty.");
		}
		String normalized = relativeName.trim().replace('\\', '/');
		while (normalized.endsWith("/")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}
		if (!isSafeCommand(normalized)) {
			throw new IOException("Use only letters, numbers, _ - . and / in folder names.");
		}
		if (IGNORED_DIRECTORIES.contains(normalized.split("/")[0].toLowerCase(Locale.ROOT))) {
			throw new IOException("That folder name is reserved by Minescript.");
		}
		return normalized;
	}
}
