package net.emutils.client.emutils.zoom;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;

public final class ZoomManager {
	private static final float ACTIVE_THRESHOLD = 1.001F;
	private static final float MAX_FRAME_DELTA_SECONDS = 0.1F;
	private static final float SCROLL_ZOOM_FACTOR = 1.28F;
	private static final float SCROLL_RESPONSE_BLEND = 0.45F;

	private KeyMapping keyBinding;
	private float currentDivisor = 1.0F;
	private float targetDivisor = 1.0F;
	private float scrollDivisorOffset;
	private long lastUpdateNanos;

	public void setKeyMapping(KeyMapping keyBinding) {
		this.keyBinding = keyBinding;
	}

	public void tick(Minecraft client) {
		updateAnimation(client);
	}

	public boolean handleScroll(double vertical) {
		Minecraft client = Minecraft.getInstance();
		if (!wantsZoom(client) || vertical == 0.0D) {
			return false;
		}

		float baseDivisor = EMUtilsClient.config().zoomAmount();
		float currentTarget = clampDivisor(baseDivisor + scrollDivisorOffset);
		float nextTarget = clampDivisor(currentTarget * (float) Math.pow(SCROLL_ZOOM_FACTOR, vertical));
		scrollDivisorOffset = nextTarget - baseDivisor;
		targetDivisor = nextTarget;

		if (EMUtilsClient.config().zoomSmoothTransition()) {
			currentDivisor += (nextTarget - currentDivisor) * SCROLL_RESPONSE_BLEND;
		} else {
			currentDivisor = nextTarget;
		}

		lastUpdateNanos = System.nanoTime();
		updateAnimation(client);
		return true;
	}

	public boolean wantsZoom(Minecraft client) {
		return keyBinding != null
			&& keyBinding.isDown()
			&& client != null
			&& net.emutils.client.emutils.compat.MinecraftClientCompat.screen(client) == null
			&& client.player != null
			&& client.level != null;
	}

	public boolean isZoomEffectActive() {
		updateAnimation(Minecraft.getInstance());
		return EMUtilsClient.config() != null
			&& EMUtilsClient.config().zoomEnabled()
			&& currentDivisor > ACTIVE_THRESHOLD;
	}

	public float zoomDivisor() {
		updateAnimation(Minecraft.getInstance());
		return currentDivisor;
	}

	public boolean shouldUseCinematicCamera() {
		if (EMUtilsClient.config() == null || !EMUtilsClient.config().zoomCinematicCamera()) {
			return false;
		}

		return wantsZoom(Minecraft.getInstance());
	}

	public boolean shouldHideHand() {
		return EMUtilsClient.config() != null
			&& EMUtilsClient.config().zoomHideHand()
			&& shouldHideHandOrHud();
	}

	public boolean shouldHideHud() {
		return EMUtilsClient.config() != null
			&& EMUtilsClient.config().zoomHideHud()
			&& shouldHideHandOrHud();
	}

	public boolean shouldHideHudWhileZooming(boolean vanillaHudHidden) {
		return vanillaHudHidden || shouldHideHud();
	}

	public boolean shouldHideHandWhileZooming(boolean vanillaHudHidden) {
		return vanillaHudHidden || shouldHideHud() || shouldHideHand();
	}

	private boolean shouldHideHandOrHud() {
		if (EMUtilsClient.config() == null || !EMUtilsClient.config().zoomEnabled()) {
			return false;
		}

		Minecraft client = Minecraft.getInstance();
		updateAnimation(client);
		return wantsZoom(client);
	}

	private void updateAnimation(Minecraft client) {
		if (EMUtilsClient.config() == null || !EMUtilsClient.config().zoomEnabled()) {
			targetDivisor = 1.0F;
			currentDivisor = 1.0F;
			lastUpdateNanos = 0L;
			return;
		}

		boolean wantsZoom = wantsZoom(client);
		if (!wantsZoom) {
			scrollDivisorOffset = 0.0F;
		}

		targetDivisor = wantsZoom
			? clampDivisor(EMUtilsClient.config().zoomAmount() + scrollDivisorOffset)
			: 1.0F;
		if (!EMUtilsClient.config().zoomSmoothTransition()) {
			currentDivisor = targetDivisor;
			lastUpdateNanos = 0L;
			return;
		}

		long nowNanos = System.nanoTime();
		if (lastUpdateNanos == 0L) {
			lastUpdateNanos = nowNanos;
			return;
		}

		float delta = targetDivisor - currentDivisor;
		if (Math.abs(delta) <= 0.001F) {
			currentDivisor = targetDivisor;
			lastUpdateNanos = nowNanos;
			return;
		}

		float deltaSeconds = Math.min(MAX_FRAME_DELTA_SECONDS, Math.max(0.0F, (nowNanos - lastUpdateNanos) / 1_000_000_000.0F));
		lastUpdateNanos = nowNanos;
		float speed = EMUtilsClient.config().zoomTransitionSpeed();
		if (targetDivisor < currentDivisor) {
			speed *= EMUtilsClient.config().zoomOutSpeedMultiplier();
		}

		float blend = 1.0F - (float) Math.exp(-speed * deltaSeconds);
		currentDivisor += delta * blend;
	}

	private static float clampDivisor(float divisor) {
		return Math.max(1.0F, Math.min(EMUtilsConfig.ZOOM_SCROLL_AMOUNT_MAX, divisor));
	}
}
