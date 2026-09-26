package net.emutils.client.emutils.minescript;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.compat.MinescriptCompat;
import org.jspecify.annotations.Nullable;

/**
 * Checks that the Python in Minescript's config.txt actually runs (#122). Minescript's default on
 * Windows is the Microsoft Store shortcut, which only prints "Python was not found" unless Python came
 * from the Store, so every script fails without saying why. When it doesn't work, this looks for a
 * Python that does, and {@link #use} writes it to config.txt; Minescript rereads the file before the
 * next script. Checks run on a background thread, since each one starts a process.
 */
public final class MinescriptPython {
	/** Minescript's runtime targets Python 3; older 3.x releases lack parts of the standard library it uses. */
	private static final int MIN_MINOR = 8;
	private static final Pattern PYTHON_LINE = Pattern.compile("^\\s*python\\s*=\\s*(.*?)\\s*$");
	private static final Pattern ENV_VAR = Pattern.compile("%([^%]+)%");
	private static final Pattern VERSION = Pattern.compile("(\\d+)\\.(\\d+)");
	private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");
	private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "EMUtils Minescript Python check");
		thread.setDaemon(true);
		return thread;
	});

	public enum Problem {
		/** Not checked yet, or still checking. */
		UNKNOWN,
		NONE,
		/** config.txt has no python line. */
		NOT_SET,
		/** The python in config.txt doesn't start, or isn't Python. */
		NOT_WORKING,
		/** It runs, but it's older than Python 3.8. */
		TOO_OLD
	}

	/** A Python that runs, and its version. */
	public record Python(String path, int major, int minor) {
		public String version() {
			return major + "." + minor;
		}
	}

	/**
	 * What the last check found: the problem with the configured Python (or none), the configured
	 * path, and when it's broken, a working Python to switch to; {@code searching} while that search runs.
	 */
	public record Result(Problem problem, @Nullable String configured, @Nullable Python configuredPython, @Nullable Python suggestion, boolean searching) {
		static final Result UNKNOWN = new Result(Problem.UNKNOWN, null, null, null, false);

		public boolean broken() {
			return problem == Problem.NOT_SET || problem == Problem.NOT_WORKING || problem == Problem.TOO_OLD;
		}
	}

	private static volatile Result result = Result.UNKNOWN;
	private static volatile long checkedConfigTime = Long.MIN_VALUE;
	private static volatile boolean checking;
	/** Bumped by {@link #use}, so a check that started before the change doesn't overwrite it. */
	private static volatile int generation;

	private MinescriptPython() {
	}

	public static Result result() {
		return result;
	}

	/** Checks again unless config.txt is unchanged since the last check. */
	public static void checkIfStale() {
		if (lastModified(configFile()) != checkedConfigTime) {
			check();
		}
	}

	/** Checks the configured Python in the background, and looks for a working one if it's broken. */
	public static void check() {
		if (!MinescriptCompat.isLoaded() || checking) {
			return;
		}
		checking = true;
		int started = generation;
		EXECUTOR.execute(() -> {
			try {
				Path config = configFile();
				checkedConfigTime = lastModified(config);
				String configured = readConfigured(config);
				Python python = configured == null ? null : probe(expand(configured)).orElse(null);
				Problem problem = configured == null
					? Problem.NOT_SET
					: python == null
						? Problem.NOT_WORKING
						: python.major() != 3 || python.minor() < MIN_MINOR ? Problem.TOO_OLD : Problem.NONE;
				if (problem == Problem.NONE) {
					publish(started, new Result(problem, configured, python, null, false));
					return;
				}
				publish(started, new Result(problem, configured, python, null, true));
				publish(started, new Result(problem, configured, python, findWorking().orElse(null), false));
			} catch (RuntimeException exception) {
				EMUtilsClient.LOGGER.warn("EMUtils could not check Minescript's Python.", exception);
				publish(started, Result.UNKNOWN);
			} finally {
				checking = false;
			}
		});
	}

	/**
	 * Sets {@code python} as Minescript's Python in config.txt, replacing any python line and keeping
	 * everything else, then has Minescript reread the file.
	 */
	public static void use(Python python) throws IOException {
		Path config = configFile();
		String text = Files.isRegularFile(config) ? Files.readString(config, StandardCharsets.UTF_8) : "";
		String newline = text.contains("\r\n") ? "\r\n" : "\n";
		String line = "python=\"" + python.path() + "\"";
		List<String> lines = new ArrayList<>();
		boolean replaced = false;
		for (String existing : text.split("\\R", -1)) {
			if (!existing.stripLeading().startsWith("#") && PYTHON_LINE.matcher(existing).matches()) {
				if (!replaced) {
					lines.add(line);
					replaced = true;
				}
				continue;
			}
			lines.add(existing);
		}
		if (!replaced) {
			if (!lines.isEmpty() && lines.getLast().isEmpty()) {
				lines.set(lines.size() - 1, line);
				lines.add("");
			} else {
				lines.add(line);
			}
		}
		Files.createDirectories(config.getParent());
		Files.writeString(config, String.join(newline, lines), StandardCharsets.UTF_8);
		MinescriptCompat.reloadConfig();
		generation++;
		result = new Result(Problem.NONE, python.path(), python, null, false);
		checkedConfigTime = lastModified(config);
	}

	private static void publish(int started, Result found) {
		if (started == generation) {
			result = found;
		}
	}

	private static Path configFile() {
		return MinescriptCompat.scriptsDir().resolve("config.txt");
	}

	private static long lastModified(Path file) {
		try {
			return Files.isRegularFile(file) ? Files.getLastModifiedTime(file).toMillis() : -1L;
		} catch (IOException exception) {
			return -1L;
		}
	}

	/** The python value in config.txt, without quotes; the last line wins, as Minescript applies them in order. */
	private static @Nullable String readConfigured(Path config) {
		if (!Files.isRegularFile(config)) {
			return null;
		}
		String value = null;
		try {
			for (String line : Files.readAllLines(config, StandardCharsets.UTF_8)) {
				if (line.stripLeading().startsWith("#")) {
					continue;
				}
				Matcher matcher = PYTHON_LINE.matcher(line);
				if (matcher.matches()) {
					value = matcher.group(1);
				}
			}
		} catch (IOException exception) {
			return null;
		}
		if (value != null && value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
			value = value.substring(1, value.length() - 1);
		}
		return value == null || value.isBlank() ? null : value;
	}

	/** Expands %userprofile% and other %VARIABLES%, like Minescript does. */
	private static String expand(String path) {
		Matcher matcher = ENV_VAR.matcher(path);
		StringBuilder expanded = new StringBuilder();
		while (matcher.find()) {
			String value = environment(matcher.group(1));
			matcher.appendReplacement(expanded, Matcher.quoteReplacement(value == null ? matcher.group() : value));
		}
		matcher.appendTail(expanded);
		return expanded.toString();
	}

	private static @Nullable String environment(String name) {
		for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
			if (entry.getKey().equalsIgnoreCase(name)) {
				return entry.getValue();
			}
		}
		return null;
	}

	/** Runs {@code command} and reads the Python version it prints, if it's a Python that works. */
	private static Optional<Python> probe(String command) {
		List<String> output = run(List.of(command, "-c", "import sys; print('%d.%d' % sys.version_info[:2])"));
		if (output.isEmpty()) {
			return Optional.empty();
		}
		Matcher matcher = VERSION.matcher(output.getLast().trim());
		if (!matcher.matches()) {
			return Optional.empty();
		}
		return Optional.of(new Python(command, Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))));
	}

	/** The process's output lines, or an empty list if it didn't start, timed out, or failed. */
	private static List<String> run(List<String> command) {
		Process process = null;
		try {
			process = new ProcessBuilder(command).redirectErrorStream(true).start();
			process.getOutputStream().close();
			if (!process.waitFor(8, TimeUnit.SECONDS)) {
				return List.of();
			}
			String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
			return process.exitValue() == 0 ? output.lines().filter(line -> !line.isBlank()).toList() : List.of();
		} catch (IOException exception) {
			return List.of();
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			return List.of();
		} finally {
			if (process != null) {
				process.destroy();
			}
		}
	}

	/** The first working Python 3.8+ among the usual places, skipping the Microsoft Store shortcut. */
	private static Optional<Python> findWorking() {
		for (String candidate : candidates()) {
			Optional<Python> python = probe(candidate).filter(found -> found.major() == 3 && found.minor() >= MIN_MINOR);
			if (python.isPresent()) {
				return python;
			}
		}
		return Optional.empty();
	}

	private static Set<String> candidates() {
		Set<String> candidates = new LinkedHashSet<>();
		if (WINDOWS) {
			// The py launcher from python.org knows which Python is installed; ask it for the real path.
			List<String> launcher = run(List.of("py", "-3", "-c", "import sys; print(sys.executable)"));
			if (!launcher.isEmpty()) {
				candidates.add(launcher.getLast().trim());
			}
			onPath(candidates, "python.exe", "python3.exe");
			String localAppData = environment("LOCALAPPDATA");
			if (localAppData != null) {
				newestFirst(candidates, Path.of(localAppData, "Programs", "Python"), "Python3", "python.exe");
			}
			String appData = environment("APPDATA");
			if (appData != null) {
				newestFirst(candidates, Path.of(appData, "uv", "python"), "cpython-3", "python.exe");
			}
			String programFiles = environment("ProgramFiles");
			if (programFiles != null) {
				newestFirst(candidates, Path.of(programFiles), "Python3", "python.exe");
			}
			newestFirst(candidates, Path.of("C:\\"), "Python3", "python.exe");
		} else {
			onPath(candidates, "python3");
			for (String path : List.of("/usr/bin/python3", "/usr/local/bin/python3", "/opt/homebrew/bin/python3")) {
				if (Files.isRegularFile(Path.of(path))) {
					candidates.add(path);
				}
			}
		}
		return candidates;
	}

	private static void onPath(Set<String> candidates, String... names) {
		String path = environment("PATH");
		if (path == null) {
			return;
		}
		for (String directory : path.split(java.io.File.pathSeparator)) {
			if (directory.isBlank() || directory.toLowerCase(Locale.ROOT).contains("windowsapps")) {
				continue;
			}
			for (String name : names) {
				try {
					Path file = Path.of(directory.trim(), name);
					if (Files.isRegularFile(file)) {
						candidates.add(file.toString());
					}
				} catch (RuntimeException ignored) {
					// A malformed PATH entry.
				}
			}
		}
	}

	/** Adds {@code folder/<prefix>*}/{@code executable}, newest version first by folder name. */
	private static void newestFirst(Set<String> candidates, Path folder, String prefix, String executable) {
		if (!Files.isDirectory(folder)) {
			return;
		}
		try (Stream<Path> children = Files.list(folder)) {
			children
				.filter(child -> child.getFileName().toString().toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT)))
				.map(child -> child.resolve(executable))
				.filter(Files::isRegularFile)
				.sorted(Comparator.comparing((Path file) -> versionKey(file.getParent().getFileName().toString())).reversed())
				.forEach(file -> candidates.add(file.toString()));
		} catch (IOException | UncheckedIOException ignored) {
			// Unreadable folder (directory streams report read errors unchecked): nothing to add from it.
		}
	}

	/** Sorts Python312 after Python39, and cpython-3.12.11 after cpython-3.9.2. */
	private static String versionKey(String name) {
		Matcher matcher = Pattern.compile("(\\d+)").matcher(name);
		StringBuilder key = new StringBuilder();
		while (matcher.find()) {
			key.append(String.format(Locale.ROOT, "%06d", Long.parseLong(matcher.group(1))));
		}
		return key.toString();
	}
}
