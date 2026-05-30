package net.emutils.client.emskyblock.context;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class SkyblockEvents {
	private final List<Consumer<SkyblockEvent>> listeners = new CopyOnWriteArrayList<>();

	public void addListener(Consumer<SkyblockEvent> listener) {
		listeners.add(listener);
	}

	public void removeListener(Consumer<SkyblockEvent> listener) {
		listeners.remove(listener);
	}

	void post(SkyblockEvent event) {
		for (Consumer<SkyblockEvent> listener : listeners) {
			try {
				listener.accept(event);
			} catch (RuntimeException exception) {
				// Keep other listeners alive if one feature misbehaves.
			}
		}
	}
}
