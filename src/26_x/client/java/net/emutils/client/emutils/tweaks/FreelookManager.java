package net.emutils.client.emutils.tweaks;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.CameraType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;

public final class FreelookManager {
	private KeyMapping keyBinding;
	private boolean active;
	private CameraType previousCameraType;
	private float playerYaw;
	private float playerPitch;
	private float yawOffset;
	private float pitchOffset;

	public void setKeyMapping(KeyMapping keyBinding) {
		this.keyBinding = keyBinding;
	}

	public void tick(Minecraft client) {
		boolean shouldBeActive = keyBinding != null
			&& keyBinding.isDown()
			&& EMUtilsClient.config().tweakFreelook()
			&& client.player != null
			&& client.level != null
			&& net.emutils.client.emutils.compat.MinecraftClientCompat.screen(client) == null;

		if (shouldBeActive && !active) {
			playerYaw = client.player.getYRot();
			playerPitch = client.player.getXRot();
			yawOffset = 0.0F;
			pitchOffset = 0.0F;
			// Keep Perspective (#171) looks around in whatever perspective the player is in, first person too.
			if (!EMUtilsClient.config().freelookKeepPerspective()) {
				previousCameraType = client.options.getCameraType();
				if (previousCameraType.isFirstPerson()) {
					client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
				}
			}
		}

		if (!shouldBeActive && active && previousCameraType != null) {
			client.options.setCameraType(previousCameraType);
			previousCameraType = null;
		}

		active = shouldBeActive;
	}

	public boolean isActive() {
		return active;
	}

	/**
	 * Turns the camera instead of the player while Freelook is held, by the mouse movement vanilla would
	 * turn the player with ({@code LocalPlayer.turn} in {@code MouseHandler.turnPlayer}), so it follows
	 * sensitivity, inverted axes and the cinematic camera. Turning by the cursor position instead stopped
	 * at the window edge on 26.3, whose SDL input reports that position clamped to the window (#167).
	 * Returns whether it took the movement.
	 */
	public boolean handleMouseTurn(double yawDelta, double pitchDelta) {
		if (!active) {
			return false;
		}
		// The same scale as Entity.turn.
		yawOffset += (float) (yawDelta * 0.15D);
		pitchOffset = Mth.clamp(pitchOffset + (float) (pitchDelta * 0.15D), -90.0F - playerPitch, 90.0F - playerPitch);
		return true;
	}

	/** Keeps the player facing where they were while the camera looks around; called every frame. */
	public void updateCamera(Minecraft client) {
		if (!active || client.player == null) {
			return;
		}
		lockPlayerRotation(client.player);
	}

	public float cameraYaw() {
		return playerYaw + yawOffset;
	}

	public float cameraPitch() {
		return Mth.clamp(playerPitch + pitchOffset, -90.0F, 90.0F);
	}

	private void lockPlayerRotation(Player player) {
		player.setYRot(playerYaw);
		player.setXRot(playerPitch);
		player.yRotO = playerYaw;
		player.xRotO = playerPitch;
		player.setYHeadRot(playerYaw);
		player.yHeadRotO = playerYaw;
	}
}
