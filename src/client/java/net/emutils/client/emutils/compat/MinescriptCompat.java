package net.emutils.client.emutils.compat;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.text.EmUtilsChatPrefix;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class MinescriptCompat {

    private static final String MOD_ID = "minescript";
    private static final String MINESCRIPT_CLASS =
        "net.minescript.common.Minescript";
    private static final int MAX_JOB_SCAN_ID = 128;
    private static final Set<String> RUNNING_FROM_EMUTILS =
        ConcurrentHashMap.newKeySet();
    private static final Map<String, Set<Integer>> TRACKED_JOB_IDS =
        new ConcurrentHashMap<>();

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
        int slash = Math.max(
            normalized.lastIndexOf('/'),
            normalized.lastIndexOf('\\')
        );
        if (slash >= 0 && slash + 1 < normalized.length()) {
            normalized = normalized.substring(slash + 1);
        }
        return normalized;
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
        ToggleResult started = sendChatCommand(normalized);
        if (started == ToggleResult.STARTED) {
            RUNNING_FROM_EMUTILS.add(normalized);
            rememberNewJobs(normalized, before);
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
        if (
            summary != null &&
            matchesInJobText(summary.toLowerCase(), lowerCommand)
        ) {
            return true;
        }

        Object boundCommand = job
            .getClass()
            .getMethod("boundCommand")
            .invoke(job);
        String[] parts = (String[]) boundCommand
            .getClass()
            .getMethod("command")
            .invoke(boundCommand);
        if (parts != null) {
            for (String part : parts) {
                if (normalizeScriptCommand(part).equalsIgnoreCase(command)) {
                    return true;
                }
            }
        }

        Path scriptPath = (Path) boundCommand
            .getClass()
            .getMethod("scriptPath")
            .invoke(boundCommand);
        if (scriptPath != null) {
            String fileName = scriptPath.getFileName().toString();
            if (fileName.endsWith(".py")) {
                fileName = fileName.substring(0, fileName.length() - 3);
            }
            if (fileName.equalsIgnoreCase(command)) {
                return true;
            }
            String pathString = scriptPath
                .toString()
                .replace('\\', '/')
                .toLowerCase();
            if (
                pathString.endsWith("/" + lowerCommand + ".py") ||
                pathString.endsWith(lowerCommand + ".py")
            ) {
                return true;
            }
        }

        return false;
    }

    private static boolean matchesInJobText(String text, String lowerCommand) {
        return (
            text.endsWith(": " + lowerCommand) ||
            text.endsWith(":" + lowerCommand) ||
            text.endsWith(" " + lowerCommand) ||
            text.contains("running: " + lowerCommand) ||
            text.contains("running:" + lowerCommand) ||
            text.contains("\\" + lowerCommand) ||
            text.contains("/" + lowerCommand + ".py")
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(
                EmUtilsChatPrefix.chat(
                    Text.literal(message).formatted(Formatting.RED)
                ),
                false
            );
        }
    }
}
