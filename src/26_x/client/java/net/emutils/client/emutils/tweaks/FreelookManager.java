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
	private double previousMouseX;
	private double previousMouseY;

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
			previousMouseX = client.mouseHandler.xpos();
			previousMouseY = client.mouseHandler.ypos();
			previousCameraType = client.options.getCameraType();
			if (previousCameraType.isFirstPerson()) {
				client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
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

	public void changeLookDirection(Player player, double cursorDeltaX, double cursorDeltaY) {
		if (!active) {
			player.turn(cursorDeltaX, cursorDeltaY);
			return;
		}

		yawOffset = (float) (yawOffset + cursorDeltaX * 0.15D);
		pitchOffset = Mth.clamp((float) (pitchOffset + cursorDeltaY * 0.15D), -90.0F, 90.0F);
	}

	public void updateCamera(Minecraft client) {
		if (!active || client.player == null) {
			return;
		}

		double mouseX = client.mouseHandler.xpos();
		double mouseY = client.mouseHandler.ypos();
		double sensitivity = sensitivity(client);
		double deltaX = (previousMouseX - mouseX) * sensitivity * 0.15D;
		double deltaY = (previousMouseY - mouseY) * sensitivity * 0.15D;
		previousMouseX = mouseX;
		previousMouseY = mouseY;

		yawOffset -= (float) deltaX;
		if (client.options.invertMouseY().get()) {
			pitchOffset += (float) deltaY;
		} else {
			pitchOffset -= (float) deltaY;
		}
		pitchOffset = Mth.clamp(pitchOffset, -90.0F - playerPitch, 90.0F - playerPitch);

		lockPlayerRotation(client.player);
	}

	public float cameraYaw() {
		return playerYaw + yawOffset;
	}

	public float cameraPitch() {
		return Mth.clamp(playerPitch + pitchOffset, -90.0F, 90.0F);
	}

	private static double sensitivity(Minecraft client) {
		return Math.pow(client.options.sensitivity().get() * 0.6D + 0.2D, 3.0D) * 8.0D;
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
