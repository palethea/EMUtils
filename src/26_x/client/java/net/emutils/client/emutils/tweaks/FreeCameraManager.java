package net.emutils.client.emutils.tweaks;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class FreeCameraManager {
	private static final float RAMP_STEP = 0.15F;
	private static final double MOVE_SPEED = 0.7D;
	private static final double SPRINT_MULTIPLIER = 3.0D;

	@Nullable
	private KeyMapping keyMapping;
	@Nullable
	private FreeCameraEntity camera;
	@Nullable
	private Entity originalCamera;
	private float forwardRamped;
	private float strafeRamped;
	private float verticalRamped;

	public void setKeyMapping(KeyMapping keyMapping) {
		this.keyMapping = keyMapping;
	}

	public void tick(Minecraft client) {
		while (keyMapping != null && keyMapping.consumeClick()) {
			EMUtilsClient.config().setTweakFreeCamera(!EMUtilsClient.config().tweakFreeCamera());
		}

		if (camera != null && (client.player == null || client.level == null || camera.level() != client.level)) {
			deactivate(client);
		}

		if (EMUtilsClient.config().tweakFreeCamera()) {
			if (camera == null) {
				activate(client);
			}
			moveCamera(client);
		} else if (camera != null) {
			deactivate(client);
		}
	}

	public boolean isActive() {
		return camera != null;
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
		client.setCameraEntity(camera);
		forwardRamped = 0.0F;
		strafeRamped = 0.0F;
		verticalRamped = 0.0F;
	}

	private void deactivate(Minecraft client) {
		if (camera != null && client.getCameraEntity() == camera) {
			client.setCameraEntity(originalCamera != null ? originalCamera : client.player);
		}
		camera = null;
		originalCamera = null;
		forwardRamped = 0.0F;
		strafeRamped = 0.0F;
		verticalRamped = 0.0F;
	}

	private void moveCamera(Minecraft client) {
		if (camera == null || client.gui.screen() != null) {
			return;
		}

		float forward = axis(client.options.keyUp.isDown(), client.options.keyDown.isDown());
		float strafe = axis(client.options.keyLeft.isDown(), client.options.keyRight.isDown());
		float vertical = axis(client.options.keyJump.isDown(), client.options.keyShift.isDown());
		forwardRamped = ramp(forwardRamped, forward);
		strafeRamped = ramp(strafeRamped, strafe);
		verticalRamped = ramp(verticalRamped, vertical);

		double diagonal = forwardRamped != 0.0F && strafeRamped != 0.0F ? Math.sqrt(0.5D) : 1.0D;
		double speed = MOVE_SPEED * (client.options.keySprint.isDown() ? SPRINT_MULTIPLIER : 1.0D);
		double yaw = Math.toRadians(camera.getYRot());
		double sin = Math.sin(yaw);
		double cos = Math.cos(yaw);
		double x = (strafeRamped * cos - forwardRamped * sin) * speed * diagonal;
		double z = (forwardRamped * cos + strafeRamped * sin) * speed * diagonal;

		camera.setOldPosAndRot();
		camera.setPos(camera.position().add(new Vec3(x, verticalRamped * speed, z)));
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
