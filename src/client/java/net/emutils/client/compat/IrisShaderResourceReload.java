package net.emutils.client.compat;

import java.util.concurrent.CompletableFuture;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Util;

public final class IrisShaderResourceReload implements ResourceReload {
	private final CompletableFuture<Void> future = new CompletableFuture<>();
	private final long startTime = Util.getMeasuringTimeMs();
	private volatile float progress;

	public void start() {
		if (future.isDone()) {
			return;
		}

		try {
			progress = 0.25F;
			IrisCompat.reloadShaders();
			progress = 1.0F;
			future.complete(null);
		} catch (Exception exception) {
			future.completeExceptionally(exception);
		}
	}

	@Override
	public CompletableFuture<?> whenComplete() {
		return future;
	}

	@Override
	public float getProgress() {
		if (future.isDone()) {
			return 1.0F;
		}

		long elapsed = Util.getMeasuringTimeMs() - startTime;
		float estimated = Math.min(0.95F, elapsed / 12000.0F);
		return Math.max(progress, estimated);
	}
}
