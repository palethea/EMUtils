package net.emutils.client.emskyblock.api.core;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.emutils.client.emskyblock.api.core.SkyblockPriceExecutor;
import net.minecraft.client.MinecraftClient;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class SkyblockApiFetchTask<T> {
	private static final long STUCK_FETCH_MS = 60_000L;

	private final Logger logger;
	private final String name;
	private final long intervalMs;
	private final Predicate<@Nullable MinecraftClient> shouldRun;
	private final Supplier<T> fetcher;
	private final Function<T, Boolean> publisher;

	private final AtomicBoolean fetching = new AtomicBoolean();
	private final AtomicLong generation = new AtomicLong();
	private volatile long lastAttemptMs;
	private volatile long fetchStartedMs;
	private volatile long lastSuccessMs;
	private volatile int failureCount;
	@Nullable
	private volatile String lastError;

	public SkyblockApiFetchTask(
		Logger logger,
		String name,
		long intervalMs,
		Predicate<@Nullable MinecraftClient> shouldRun,
		Supplier<T> fetcher,
		Function<T, Boolean> publisher
	) {
		this.logger = logger;
		this.name = name;
		this.intervalMs = intervalMs;
		this.shouldRun = shouldRun;
		this.fetcher = fetcher;
		this.publisher = publisher;
	}

	public void tick(@Nullable MinecraftClient client) {
		recoverStuckFetch();
		if (!shouldRun.test(client)) {
			return;
		}

		long now = System.currentTimeMillis();
		if (fetching.get() || now - lastAttemptMs < intervalMs) {
			return;
		}

		startFetch();
	}

	public void fetchNow(@Nullable MinecraftClient client) {
		recoverStuckFetch();
		if (!shouldRun.test(client)) {
			return;
		}

		lastAttemptMs = 0L;
		startFetch();
	}

	public void requestImmediateFetch() {
		lastAttemptMs = 0L;
	}

	public void clear() {
		generation.incrementAndGet();
		lastAttemptMs = 0L;
		fetchStartedMs = 0L;
		lastSuccessMs = 0L;
		failureCount = 0;
		lastError = null;
		fetching.set(false);
	}

	public SkyblockApiStatus status() {
		return new SkyblockApiStatus(name, fetching.get(), lastAttemptMs, lastSuccessMs, failureCount, lastError);
	}

	private void recoverStuckFetch() {
		if (!fetching.get() || fetchStartedMs <= 0L) {
			return;
		}

		if (System.currentTimeMillis() - fetchStartedMs > STUCK_FETCH_MS) {
			logger.warn("{} API fetch timed out; allowing retry.", name);
			generation.incrementAndGet();
			fetchStartedMs = 0L;
			failureCount++;
			lastError = "timeout";
			fetching.set(false);
		}
	}

	private void startFetch() {
		if (!fetching.compareAndSet(false, true)) {
			return;
		}

		long runGeneration = generation.get();
		lastAttemptMs = System.currentTimeMillis();
		fetchStartedMs = lastAttemptMs;
		CompletableFuture.supplyAsync(fetcher, SkyblockPriceExecutor.EXECUTOR)
			.whenComplete((result, throwable) -> {
				if (generation.get() != runGeneration) {
					return;
				}

				fetching.set(false);
				fetchStartedMs = 0L;
				if (throwable != null) {
					failureCount++;
					lastError = throwable.getMessage();
					logger.warn("{} API fetch failed.", name, throwable);
					return;
				}

				if (publisher.apply(result)) {
					lastSuccessMs = System.currentTimeMillis();
					failureCount = 0;
					lastError = null;
				} else {
					failureCount++;
					lastError = "empty response";
					logger.warn("{} API returned no usable data.", name);
					lastAttemptMs = System.currentTimeMillis() - intervalMs + 5_000L;
				}
			});
	}
}
