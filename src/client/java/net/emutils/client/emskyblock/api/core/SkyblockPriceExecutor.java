package net.emutils.client.emskyblock.api.core;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public final class SkyblockPriceExecutor {
	public static final Executor EXECUTOR = Executors.newFixedThreadPool(4, factory("EMUtils-Prices"));

	private SkyblockPriceExecutor() {
	}

	private static ThreadFactory factory(String name) {
		return runnable -> {
			Thread thread = new Thread(runnable, name);
			thread.setDaemon(true);
			return thread;
		};
	}
}
