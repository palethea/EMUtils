package net.emutils.client.emutils.tweaks;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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
			&& client.screen == null;

		if (shouldBeActive && !active) {
			playerYaw = client.player.getYRot();
			playerPitch = client.player.getXRot();
			yawOffset = 0.0F;
			pitchOffset = 0.0F;
			previousCameraType = client.options.getCameraType();
			if (previousCameraType.isFirstPerson()) {
				client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			}
		}

		if (active && client.player != null) {
			lockPlayerRotation(client.player);
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

	public void changeLookDirection(Player player, double cursorDeltaX, double cursorDeltaY) {
		if (!active) {
			player.turn(cursorDeltaX, cursorDeltaY);
			return;
		}

		yawOffset = (float) (yawOffset + cursorDeltaX * 0.15D);
		pitchOffset = Mth.clamp((float) (pitchOffset + cursorDeltaY * 0.15D), -90.0F, 90.0F);
	}

	public float cameraYaw() {
		return playerYaw + yawOffset;
	}

	public float cameraPitch() {
		return Mth.clamp(playerPitch + pitchOffset, -90.0F, 90.0F);
	}

	private void lockPlayerRotation(LocalPlayer player) {
		player.setYRot(playerYaw);
		player.setXRot(playerPitch);
		player.yRotO = playerYaw;
		player.xRotO = playerPitch;
		player.setYHeadRot(playerYaw);
		player.yHeadRotO = playerYaw;
		player.setYBodyRot(playerYaw);
		player.yBodyRotO = playerYaw;
	}
}
