package net.emutils.client.emutils.compat;

import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.util.Util;

public final class ShaderPackLoadingOverlay extends Overlay {
	private static final int MIN_VISIBLE_TICKS = 5;
	private static final long MIN_VISIBLE_MS = 500L;

	private final LoadingOverlay splash;
	private final IrisShaderResourceReload reload;
	private final long startTime;
	private int ticks;
	private boolean reloadStarted;

	public ShaderPackLoadingOverlay(
		Minecraft client,
		IrisShaderResourceReload reload,
		Consumer<Optional<Throwable>> callback
	) {
		this.reload = reload;
		this.startTime = Util.getMillis();
		this.splash = new LoadingOverlay(client, reload, callback, false);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
		splash.extractRenderState(context, mouseX, mouseY, deltaTicks);
	}

	@Override
	public void tick() {
		splash.tick();
		if (reloadStarted) {
			return;
		}

		ticks++;
		long elapsed = Util.getMillis() - startTime;
		if (ticks >= MIN_VISIBLE_TICKS && elapsed >= MIN_VISIBLE_MS) {
			reloadStarted = true;
			reload.start();
		}
	}
}
