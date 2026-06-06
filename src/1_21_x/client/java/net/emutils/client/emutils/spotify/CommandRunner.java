package net.emutils.client.emutils.spotify;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import net.emutils.client.EMUtilsClient;
import org.jspecify.annotations.Nullable;

final class CommandRunner {
	private static final int MAX_OUTPUT_BYTES = 512_000;
	private static final long DEFAULT_TIMEOUT_SECONDS = 2L;

	private CommandRunner() {
	}

	static OptionalResult run(String @Nullable [] command, long timeoutSeconds) {
		if (command == null || command.length == 0) {
			return OptionalResult.empty();
		}

		try {
			Process process = new ProcessBuilder(command)
				.redirectErrorStream(true)
				.start();
			boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
			if (!finished) {
				process.destroyForcibly();
				return OptionalResult.empty();
			}

			if (process.exitValue() != 0) {
				return OptionalResult.empty();
			}

			byte[] raw = process.getInputStream().readNBytes(MAX_OUTPUT_BYTES + 1);
			if (raw.length > MAX_OUTPUT_BYTES) {
				EMUtilsClient.LOGGER.debug("Spotify command output truncated: {}", String.join(" ", command));
				raw = java.util.Arrays.copyOf(raw, MAX_OUTPUT_BYTES);
			}

			String output = new String(raw, StandardCharsets.UTF_8).trim();
			return output.isEmpty() ? OptionalResult.empty() : OptionalResult.of(output);
		} catch (IOException | InterruptedException exception) {
			if (exception instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}

			EMUtilsClient.LOGGER.debug("Spotify command failed: {}", String.join(" ", command), exception);
			return OptionalResult.empty();
		}
	}

	static OptionalResult run(String... command) {
		return run(command, DEFAULT_TIMEOUT_SECONDS);
	}

	static OptionalResult run(long timeoutSeconds, String... command) {
		return run(command, timeoutSeconds);
	}

	static void runAsync(String... command) {
		Thread.startVirtualThread(() -> run(command));
	}

	static void runAsync(long timeoutSeconds, String... command) {
		Thread.startVirtualThread(() -> run(command, timeoutSeconds));
	}

	record OptionalResult(@Nullable String value) {
		static OptionalResult empty() {
			return new OptionalResult(null);
		}

		static OptionalResult of(String value) {
			return new OptionalResult(value);
		}

		boolean isPresent() {
			return value != null && !value.isBlank();
		}
	}
}
