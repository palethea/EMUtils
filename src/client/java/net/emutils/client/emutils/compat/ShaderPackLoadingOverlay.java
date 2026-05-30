package net.emutils.client.emutils.compat;

import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.util.Util;

public final class ShaderPackLoadingOverlay extends Overlay {
	private static final int MIN_VISIBLE_TICKS = 5;
	private static final long MIN_VISIBLE_MS = 500L;

	private final SplashOverlay splash;
	private final IrisShaderResourceReload reload;
	private final long startTime;
	private int ticks;
	private boolean reloadStarted;

	public ShaderPackLoadingOverlay(
		MinecraftClient client,
		IrisShaderResourceReload reload,
		Consumer<Optional<Throwable>> callback
	) {
		this.reload = reload;
		this.startTime = Util.getMeasuringTimeMs();
		this.splash = new SplashOverlay(client, reload, callback, false);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		splash.render(context, mouseX, mouseY, deltaTicks);
	}

	@Override
	public void tick() {
		splash.tick();
		if (reloadStarted) {
			return;
		}

		ticks++;
		long elapsed = Util.getMeasuringTimeMs() - startTime;
		if (ticks >= MIN_VISIBLE_TICKS && elapsed >= MIN_VISIBLE_MS) {
			reloadStarted = true;
			reload.start();
		}
	}
}
