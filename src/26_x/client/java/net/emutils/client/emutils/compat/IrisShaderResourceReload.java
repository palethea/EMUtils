package net.emutils.client.emutils.compat;

import java.util.concurrent.CompletableFuture;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Util;

public final class IrisShaderResourceReload implements ReloadInstance {
	private final CompletableFuture<Void> future = new CompletableFuture<>();
	private final long startTime = Util.getMillis();
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
	public CompletableFuture<?> done() {
		return future;
	}

	@Override
	public float getActualProgress() {
		if (future.isDone()) {
			return 1.0F;
		}

		long elapsed = Util.getMillis() - startTime;
		float estimated = Math.min(0.95F, elapsed / 12000.0F);
		return Math.max(progress, estimated);
	}
}
