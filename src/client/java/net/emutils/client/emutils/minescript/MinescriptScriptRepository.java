package net.emutils.client.emutils.minescript;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
			Path path = safePath(root.resolve(command + ".py"));
			return Files.isRegularFile(path);
		} catch (IOException ignored) {
			return false;
		}
	}

	public boolean isSafeCommand(String command) {
		return command != null && command.matches("[A-Za-z0-9_./-]+") && !command.contains("..") && !command.startsWith("/") && !command.endsWith("/");
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
		if (relativeName == null || relativeName.isBlank()) {
			throw new IOException("Script name cannot be empty.");
		}
		String normalized = relativeName.trim().replace('\\', '/');
		if (!normalized.toLowerCase(Locale.ROOT).endsWith(".py")) {
			normalized += ".py";
		}
		if (normalized.startsWith("/") || normalized.contains("..") || normalized.contains("//") || normalized.matches(".*[\\p{Cntrl}:*?\"<>|].*")) {
			throw new IOException("Unsafe script name.");
		}
		String command = normalized.substring(0, normalized.length() - 3);
		if (!isSafeCommand(command)) {
			throw new IOException("Script name cannot be converted to a safe Minescript command.");
		}
		return normalized;
	}
}
