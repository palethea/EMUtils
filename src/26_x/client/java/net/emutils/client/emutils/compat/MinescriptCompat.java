package net.emutils.client.emutils.compat;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.text.EmUtilsChatPrefix;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.jspecify.annotations.Nullable;

public final class MinescriptCompat {

    private static final String MOD_ID = "minescript";
    private static final String MINESCRIPT_CLASS =
        "net.minescript.common.Minescript";
    private static final int MAX_JOB_SCAN_ID = 128;
    private static final Set<String> RUNNING_FROM_EMUTILS =
        ConcurrentHashMap.newKeySet();
    private static final Map<String, Set<Integer>> TRACKED_JOB_IDS =
        new ConcurrentHashMap<>();
    /** What each script EMUtils started last wrote to stderr, by command; replaced when it runs again. */
    private static final Map<String, List<String>> STDERR =
        new ConcurrentHashMap<>();
    private static final int MAX_STDERR_LINES = 200;
    private static final Pattern TRACEBACK_FILE =
        Pattern.compile("^\\s*File \"(.+)\", line (\\d+)");
    private static final Pattern ERROR_NAME =
        Pattern.compile("^[A-Za-z_][\\w.]*(Error|Exception|Interrupt|Exit)\\b.*");

    /** Why a script's last run failed: the line in the script (1-based, 0 when unknown) and the error. */
    public record ScriptError(int line, String message) {}

    public enum ToggleResult {
        STARTED,
        STOPPED,
        FAILED,
    }

    private MinescriptCompat() {}

    public static void tickJobs() {
        for (Map.Entry<
            String,
            Set<Integer>
        > entry : TRACKED_JOB_IDS.entrySet()) {
            entry.getValue().removeIf(jobId -> !isActiveJobId(jobId));
            if (entry.getValue().isEmpty()) {
                TRACKED_JOB_IDS.remove(entry.getKey());
                RUNNING_FROM_EMUTILS.remove(entry.getKey());
            }
        }
    }

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    public static Path scriptsDir() {
        return FabricLoader.getInstance().getGameDir().resolve("minescript");
    }

    public static String normalizeScriptCommand(String command) {
        if (command == null) {
            return "";
        }
        String normalized = command.trim();
        if (normalized.startsWith("\\")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith(".py")) {
            normalized = normalized.substring(0, normalized.length() - 3);
        }
        int space = normalized.indexOf(' ');
        if (space >= 0) {
            normalized = normalized.substring(0, space);
        }
        // Keep the folder: Minescript runs minescript/test/foo.py as \test/foo, and \foo would look in
        // the minescript folder itself.
        return normalized.replace('\\', '/');
    }

    /**
     * The command that runs a script file: its path inside the minescript folder without the
     * extension, such as test/foo. Minescript's own system/exec scripts, and any others outside the
     * folder, go by their path from system/exec or their file name.
     */
    private static String commandForScript(Path scriptPath) {
        Path root = scriptsDir().toAbsolutePath().normalize();
        Path systemExec = root.resolve("system").resolve("exec");
        Path path = scriptPath.isAbsolute()
            ? scriptPath.normalize()
            : FabricLoader.getInstance().getGameDir().resolve(scriptPath).toAbsolutePath().normalize();
        String command = path.startsWith(systemExec)
            ? systemExec.relativize(path).toString()
            : path.startsWith(root)
                ? root.relativize(path).toString()
                : path.getFileName().toString();
        command = command.replace('\\', '/');
        int dot = command.lastIndexOf('.');
        return dot > command.lastIndexOf('/') ? command.substring(0, dot) : command;
    }

    /**
     * Has Minescript reread config.txt now. It also rereads a changed file before running a script,
     * so this only makes a change take effect right away.
     */
    public static void reloadConfig() {
        if (!isLoaded()) {
            return;
        }
        try {
            Object config = minescriptClass().getField("config").get(null);
            if (config != null) {
                config.getClass().getMethod("load").invoke(config);
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            EMUtilsClient.LOGGER.warn(
                "EMUtils could not reload Minescript's config: {}",
                exception.toString()
            );
        }
    }

    /**
     * Called from {@code MinescriptJobMixin}, on Minescript's threads, for each line a script writes to
     * stderr. Only runs EMUtils started are kept.
     */
    public static void onJobStderr(Object job, String line) {
        try {
            Object boundCommand = job.getClass().getMethod("boundCommand").invoke(job);
            Path scriptPath = (Path) boundCommand
                .getClass()
                .getMethod("scriptPath")
                .invoke(boundCommand);
            if (scriptPath == null || line == null) {
                return;
            }
            List<String> lines = STDERR.get(commandForScript(scriptPath));
            if (lines != null && lines.size() < MAX_STDERR_LINES) {
                lines.add(line);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Not a job we can read; its output still goes to chat.
        }
    }

    /**
     * Why {@code command}'s last run from EMUtils failed, read from its Python traceback, or null when it
     * is still running, succeeded, or wasn't started from EMUtils.
     */
    public static @Nullable ScriptError lastError(String command) {
        String normalized = normalizeScriptCommand(command);
        List<String> lines = STDERR.get(normalized);
        if (lines == null || lines.isEmpty() || !findActiveJobIdsForCommand(normalized).isEmpty()) {
            return null;
        }
        List<String> copy;
        synchronized (lines) {
            copy = List.copyOf(lines);
        }
        return parseError(normalized, copy);
    }

    /** Forgets the last run's error, once the script was changed. */
    public static void dismissError(String command) {
        STDERR.remove(normalizeScriptCommand(command));
    }

    /** Moves a remembered error along when a script is renamed or moved. */
    public static void renameCommand(String from, String to) {
        List<String> lines = STDERR.remove(normalizeScriptCommand(from));
        if (lines != null) {
            STDERR.put(normalizeScriptCommand(to), lines);
        }
    }

    /**
     * Reads a Python traceback: the error is its last unindented line (such as "NameError: name 'x' is
     * not defined"), and the line is the last "File ..., line N" frame in this script, since that's
     * where its own code went wrong even when the error was raised inside a library.
     */
    static @Nullable ScriptError parseError(String command, List<String> lines) {
        boolean traceback = false;
        String message = null;
        int line = 0;
        for (String text : lines) {
            if (text.startsWith("Traceback (most recent call last)")) {
                traceback = true;
            }
            Matcher file = TRACEBACK_FILE.matcher(text);
            if (file.find()) {
                try {
                    if (commandForScript(Path.of(file.group(1))).equalsIgnoreCase(command)) {
                        line = Integer.parseInt(file.group(2));
                    }
                } catch (RuntimeException ignored) {
                    // Not a path, such as "<string>".
                }
                continue;
            }
            if (!text.isBlank() && !Character.isWhitespace(text.charAt(0))) {
                message = text.strip();
            }
        }
        if (message == null || (!traceback && !ERROR_NAME.matcher(message).matches())) {
            return null;
        }
        return new ScriptError(line, message);
    }

    public static boolean runCommand(String command) {
        return sendChatCommand(command) == ToggleResult.STARTED;
    }

    public static ToggleResult toggleCommand(String command) {
        if (!isLoaded()) {
            showLocalError("Minescript is not installed.");
            return ToggleResult.FAILED;
        }
        if (command == null || command.isBlank()) {
            showLocalError("No Minescript command selected.");
            return ToggleResult.FAILED;
        }

        String normalized = normalizeScriptCommand(command);
        boolean markedRunning = RUNNING_FROM_EMUTILS.contains(normalized);
        List<Integer> activeJobIds = collectJobIdsForCommand(normalized);

        if (!activeJobIds.isEmpty() || markedRunning) {
            boolean killedAny = killJobsForCommand(normalized, activeJobIds);
            RUNNING_FROM_EMUTILS.remove(normalized);
            TRACKED_JOB_IDS.remove(normalized);
            return killedAny ? ToggleResult.STOPPED : ToggleResult.FAILED;
        }

        Set<Integer> before = snapshotActiveJobIds();
        // Collect its stderr from the start; the first lines can come in before this returns.
        STDERR.put(normalized, Collections.synchronizedList(new ArrayList<>()));
        ToggleResult started = sendChatCommand(normalized);
        if (started == ToggleResult.STARTED) {
            RUNNING_FROM_EMUTILS.add(normalized);
            rememberNewJobs(normalized, before);
        } else {
            STDERR.remove(normalized);
        }
        return started;
    }

    private static void rememberNewJobs(String command, Set<Integer> before) {
        Set<Integer> after = snapshotActiveJobIds();
        Set<Integer> created = new HashSet<>(after);
        created.removeAll(before);
        if (!created.isEmpty()) {
            TRACKED_JOB_IDS.computeIfAbsent(command, ignored ->
                ConcurrentHashMap.newKeySet()
            ).addAll(created);
            return;
        }

        List<Integer> matched = findActiveJobIdsForCommand(command);
        if (!matched.isEmpty()) {
            TRACKED_JOB_IDS.computeIfAbsent(command, ignored ->
                ConcurrentHashMap.newKeySet()
            ).addAll(matched);
        }
    }

    private static List<Integer> collectJobIdsForCommand(String command) {
        LinkedHashSet<Integer> jobIds = new LinkedHashSet<>();
        Set<Integer> tracked = TRACKED_JOB_IDS.get(command);
        if (tracked != null) {
            jobIds.addAll(tracked);
        }
        jobIds.addAll(findActiveJobIdsForCommand(command));
        return new ArrayList<>(jobIds);
    }

    private static Set<Integer> snapshotActiveJobIds() {
        try {
            return new HashSet<>(getActiveJobs().keySet());
        } catch (ReflectiveOperationException exception) {
            return Set.of();
        }
    }

    private static boolean killJobsForCommand(
        String command,
        List<Integer> knownJobIds
    ) {
        LinkedHashSet<Integer> targets = new LinkedHashSet<>(knownJobIds);
        targets.addAll(findActiveJobIdsForCommand(command));

        boolean killedAny = false;
        for (int jobId : targets) {
            if (killJob(jobId)) {
                killedAny = true;
            }
        }

        if (!killedAny && RUNNING_FROM_EMUTILS.contains(command)) {
            killedAny = killJobViaChat(-1);
        }

        return killedAny;
    }

    private static ToggleResult sendChatCommand(String command) {
        try {
            Class<?> minescriptClass = minescriptClass();
            Object result = minescriptClass
                .getMethod("onClientChat", String.class)
                .invoke(null, "\\" + command);
            return !(result instanceof Boolean booleanResult) || booleanResult
                ? ToggleResult.STARTED
                : ToggleResult.FAILED;
        } catch (
            ClassNotFoundException
            | IllegalAccessException
            | NoSuchMethodException
            | InvocationTargetException exception
        ) {
            showLocalError(
                "Could not run Minescript command \\" + command + "."
            );
            return ToggleResult.FAILED;
        }
    }

    public static boolean killJob(int jobId) {
        if (!isLoaded()) {
            return false;
        }

        try {
            Object job = getActiveJobs().get(jobId);
            if (job != null && isActiveJob(job)) {
                job.getClass().getMethod("requestKill").invoke(job);
                return true;
            }
        } catch (ReflectiveOperationException exception) {
            logReflectionFailure("killJob", exception);
        }

        return killJobViaChat(jobId);
    }

    private static boolean killJobViaChat(int jobId) {
        try {
            Class<?> minescriptClass = minescriptClass();
            minescriptClass
                .getMethod("onClientChat", String.class)
                .invoke(null, "\\killjob " + jobId);
            return true;
        } catch (
            ClassNotFoundException
            | IllegalAccessException
            | NoSuchMethodException
            | InvocationTargetException exception
        ) {
            return false;
        }
    }

    public static boolean isActiveJobId(int jobId) {
        if (!isLoaded()) {
            return false;
        }
        try {
            Object job = getActiveJobs().get(jobId);
            return job != null && isActiveJob(job);
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    public static List<Integer> findActiveJobIdsForCommand(String command) {
        List<Integer> jobIds = new ArrayList<>();
        if (!isLoaded()) {
            return jobIds;
        }

        String normalized = normalizeScriptCommand(command);
        if (normalized.isBlank()) {
            return jobIds;
        }

        try {
            for (Map.Entry<Integer, ?> entry : getActiveJobs().entrySet()) {
                Object job = entry.getValue();
                if (!isActiveJob(job) || !matchesCommand(job, normalized)) {
                    continue;
                }
                jobIds.add(entry.getKey());
            }
        } catch (ReflectiveOperationException exception) {
            logReflectionFailure("findActiveJobIdsForCommand", exception);
        }

        return jobIds;
    }

    private static Class<?> minescriptClass() throws ClassNotFoundException {
        ClassLoader loader = MinescriptCompat.class.getClassLoader();
        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
        }
        return Class.forName(MINESCRIPT_CLASS, true, loader);
    }

    @SuppressWarnings("unchecked")
    private static Map<Integer, ?> getActiveJobs()
        throws ReflectiveOperationException {
        Class<?> minescriptClass = minescriptClass();
        Field jobsField = minescriptClass.getDeclaredField("jobs");
        jobsField.setAccessible(true);
        Object jobManager = jobsField.get(null);
        if (jobManager == null) {
            throw new IllegalStateException("Minescript JobManager was null");
        }

        Field jobMapField = jobManager.getClass().getDeclaredField("jobMap");
        jobMapField.setAccessible(true);
        return (Map<Integer, ?>) jobMapField.get(jobManager);
    }

    private static boolean isActiveJob(Object job)
        throws ReflectiveOperationException {
        Object state = job.getClass().getMethod("state").invoke(job);
        if (state == null) {
            return false;
        }
        String status = (String) state
            .getClass()
            .getMethod("name")
            .invoke(state);
        return (
            "RUNNING".equals(status) ||
            "SUSPENDED".equals(status) ||
            "NOT_STARTED".equals(status)
        );
    }

    private static boolean matchesCommand(Object job, String command)
        throws ReflectiveOperationException {
        // The script's own path is exact: test/foo never matches foo, or the other way around.
        Object boundCommand = job
            .getClass()
            .getMethod("boundCommand")
            .invoke(job);
        Path scriptPath = (Path) boundCommand
            .getClass()
            .getMethod("scriptPath")
            .invoke(boundCommand);
        if (scriptPath != null) {
            return commandForScript(scriptPath).equalsIgnoreCase(command);
        }

        String lowerCommand = command.toLowerCase();
        String display = (String) job
            .getClass()
            .getMethod("toString")
            .invoke(job);
        if (
            display != null &&
            matchesInJobText(display.toLowerCase(), lowerCommand)
        ) {
            return true;
        }

        String summary = (String) job
            .getClass()
            .getMethod("jobSummary")
            .invoke(job);
        return (
            summary != null &&
            matchesInJobText(summary.toLowerCase(), lowerCommand)
        );
    }

    private static boolean matchesInJobText(String text, String lowerCommand) {
        return (
            text.endsWith(": " + lowerCommand) ||
            text.endsWith(":" + lowerCommand) ||
            text.endsWith(" " + lowerCommand) ||
            text.contains("\\" + lowerCommand + " ")
        );
    }

    private static void logReflectionFailure(
        String action,
        ReflectiveOperationException exception
    ) {
        EMUtilsClient.LOGGER.warn(
            "EMUtils Minescript {} failed: {}",
            action,
            exception.toString()
        );
    }

    private static void showLocalError(String message) {
		Minecraft client = Minecraft.getInstance();
		if (client.player != null) {
			client.player.sendSystemMessage(
				EmUtilsChatPrefix.chat(
					Component.literal(message).withStyle(ChatFormatting.RED)
				)
			);
		}
	}
}
