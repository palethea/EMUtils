package net.emutils.client.emutils.tweaks;

import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.emutils.client.emutils.util.UnfairFeatures;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class FreeCameraManager {
	private static final float RAMP_STEP = 0.15F;
	private static final double MOVE_SPEED = 0.7D;
	/** Two presses of the key within this many milliseconds are a double tap. */
	private static final long DOUBLE_TAP_MS = 300L;
	/** Half the size of the box around the camera's eye that Collision keeps out of blocks. */
	private static final double COLLISION_HALF_SIZE = 0.25D;

	@Nullable
	private KeyMapping keyMapping;
	@Nullable
	private FreeCameraEntity camera;
	@Nullable
	private Entity originalCamera;
	/** Where the camera was when its world went away, such as on a dimension change, to carry it over. */
	@Nullable
	private CarriedCamera carried;
	private long lastPressMs = Long.MIN_VALUE;
	private float forwardRamped;
	private float strafeRamped;
	private float verticalRamped;

	private record CarriedCamera(ResourceKey<Level> dimension, Vec3 position, float yaw, float pitch) {
	}

	public void setKeyMapping(KeyMapping keyMapping) {
		this.keyMapping = keyMapping;
	}

	public void tick(Minecraft client) {
		EMUtilsConfig config = EMUtilsClient.config();
		while (keyMapping != null && keyMapping.consumeClick()) {
			handleKeyPress(config);
		}

		if (camera != null && (client.player == null || client.level == null || camera.level() != client.level)) {
			// The world changed under the camera: a dimension change, or a respawn into a new world. The
			// camera comes back in the new world facing the same way, and in the same place if it's the
			// same dimension.
			carried = new CarriedCamera(camera.level().dimension(), camera.position(), camera.getYRot(), camera.getXRot());
			deactivate(client);
		}

		if (config.tweakFreeCamera() && !config.unfairFeatures()) {
			// Unfair Features turned off (#213): it ends, and doesn't start again when they're back on.
			config.setTweakFreeCamera(false);
		}
		if (config.tweakFreeCamera()) {
			if (camera == null) {
				activate(client);
			}
			moveCamera(client);
		} else {
			carried = null;
			if (camera != null) {
				deactivate(client);
			}
		}
	}

	/**
	 * The Free Camera key toggles it; with Double-Tap to Start, starting takes two presses in quick
	 * succession, while one press still ends it.
	 */
	private void handleKeyPress(EMUtilsConfig config) {
		if (!config.tweakFreeCamera() && !config.unfairFeatures()) {
			UnfairFeatures.tellOff(Minecraft.getInstance(), Component.translatable(EMUtilsTexts.OPTION_TWEAK_FREE_CAMERA));
			return;
		}
		if (config.tweakFreeCamera() || !config.freeCameraDoubleTap()) {
			lastPressMs = Long.MIN_VALUE;
			config.setTweakFreeCamera(!config.tweakFreeCamera());
			return;
		}
		long now = Util.getMillis();
		if (lastPressMs != Long.MIN_VALUE && now - lastPressMs <= DOUBLE_TAP_MS) {
			lastPressMs = Long.MIN_VALUE;
			config.setTweakFreeCamera(true);
		} else {
			lastPressMs = now;
		}
	}

	public boolean isActive() {
		return camera != null;
	}

	public boolean shouldUseSpectatorHud() {
		return isActive() && EMUtilsClient.config().freeCameraHudMode() == FreeCameraHudMode.SPECTATOR;
	}

	public boolean shouldUseRegularHud() {
		return isActive() && EMUtilsClient.config().freeCameraHudMode() == FreeCameraHudMode.REGULAR;
	}

	/** The field of view while the camera is detached with Camera FOV on, or {@code fov} otherwise. */
	public float fov(float fov) {
		EMUtilsConfig config = EMUtilsClient.config();
		return isActive() && config != null && config.freeCameraCustomFov() ? config.freeCameraFov() : fov;
	}

	@Nullable
	public BlockPos cameraBlockPosition() {
		return camera == null ? null : camera.blockPosition();
	}

	public boolean handleMouseTurn(double yawDelta, double pitchDelta) {
		if (camera == null) {
			return false;
		}
		camera.turn(yawDelta, pitchDelta);
		return true;
	}

	public void reset() {
		Minecraft client = Minecraft.getInstance();
		deactivate(client);
		carried = null;
		lastPressMs = Long.MIN_VALUE;
		if (EMUtilsClient.config() != null && EMUtilsClient.config().tweakFreeCamera()) {
			EMUtilsClient.config().setTweakFreeCamera(false);
		}
	}

	private void activate(Minecraft client) {
		if (client.player == null || client.level == null) {
			return;
		}
		originalCamera = client.getCameraEntity();
		camera = new FreeCameraEntity(client.player, client.level);
		if (carried != null) {
			camera.setYRot(carried.yaw());
			camera.setXRot(carried.pitch());
			if (carried.dimension() == client.level.dimension()) {
				camera.setPos(carried.position());
			}
			camera.setOldPosAndRot();
			carried = null;
		}
		client.setCameraEntity(camera);
		forwardRamped = 0.0F;
		strafeRamped = 0.0F;
		verticalRamped = 0.0F;
	}

	private void deactivate(Minecraft client) {
		if (camera != null && client.getCameraEntity() == camera) {
			// After a respawn or a dimension change the entity the camera was taken from is gone, so the
			// view goes back to the current player instead.
			boolean originalGone = originalCamera == null || originalCamera.isRemoved() || originalCamera.level() != client.level;
			client.setCameraEntity(originalGone ? client.player : originalCamera);
		}
		camera = null;
		originalCamera = null;
		forwardRamped = 0.0F;
		strafeRamped = 0.0F;
		verticalRamped = 0.0F;
	}

	private void moveCamera(Minecraft client) {
		if (camera == null || net.emutils.client.emutils.compat.MinecraftClientCompat.screen(client) != null) {
			return;
		}

		EMUtilsConfig config = EMUtilsClient.config();
		float forward = axis(client.options.keyUp.isDown(), client.options.keyDown.isDown());
		float strafe = axis(client.options.keyLeft.isDown(), client.options.keyRight.isDown());
		float vertical = axis(client.options.keyJump.isDown(), client.options.keyShift.isDown());
		forwardRamped = ramp(forwardRamped, forward);
		strafeRamped = ramp(strafeRamped, strafe);
		verticalRamped = ramp(verticalRamped, vertical);

		double diagonal = forwardRamped != 0.0F && strafeRamped != 0.0F ? Math.sqrt(0.5D) : 1.0D;
		double speed = MOVE_SPEED * (client.options.keySprint.isDown() ? config.freeCameraBoostMultiplier() : 1.0D);
		double horizontal = speed * config.freeCameraHorizontalSpeed() / 100.0D;
		double yaw = Math.toRadians(camera.getYRot());
		double sin = Math.sin(yaw);
		double cos = Math.cos(yaw);
		double x = (strafeRamped * cos - forwardRamped * sin) * horizontal * diagonal;
		double z = (forwardRamped * cos + strafeRamped * sin) * horizontal * diagonal;
		Vec3 movement = new Vec3(x, verticalRamped * speed * config.freeCameraVerticalSpeed() / 100.0D, z);
		if (config.freeCameraCollision()) {
			movement = collide(camera, movement);
		}

		camera.setOldPosAndRot();
		camera.setPos(camera.position().add(movement));
	}

	/** Shortens {@code movement} so a small box around the camera's eye doesn't move into blocks. */
	private static Vec3 collide(FreeCameraEntity camera, Vec3 movement) {
		Vec3 eye = camera.getEyePosition();
		AABB box = new AABB(eye, eye).inflate(COLLISION_HALF_SIZE);
		return Entity.collideBoundingBox(camera, movement, box, camera.level(), List.of());
	}

	/** Puts the free camera at a position and angle, without easing; used by UI snapshots. */
	public void placeForSnapshot(double x, double y, double z, float yaw, float pitch) {
		if (camera == null) {
			return;
		}
		camera.setPos(x, y, z);
		camera.setYRot(yaw);
		camera.setXRot(pitch);
		camera.setOldPosAndRot();
	}

	/** Acts as a press of the Free Camera key; used by UI snapshots. */
	public void pressKeyForSnapshot() {
		handleKeyPress(EMUtilsClient.config());
	}

	/** The free camera's position and angle, for UI snapshot checks. */
	@Nullable
	public Vec3 positionForSnapshot() {
		return camera == null ? null : camera.position();
	}

	public float yawForSnapshot() {
		return camera == null ? 0.0F : camera.getYRot();
	}

	@Nullable
	public ResourceKey<Level> dimensionForSnapshot() {
		return camera == null ? null : camera.level().dimension();
	}

	private static float axis(boolean positive, boolean negative) {
		return (positive ? 1.0F : 0.0F) - (negative ? 1.0F : 0.0F);
	}

	private static float ramp(float current, float target) {
		if (target == 0.0F) {
			return current * 0.5F;
		}
		if (Math.signum(current) != Math.signum(target)) {
			current = 0.0F;
		}
		return Math.max(-1.0F, Math.min(1.0F, current + Math.copySign(RAMP_STEP, target)));
	}
}
