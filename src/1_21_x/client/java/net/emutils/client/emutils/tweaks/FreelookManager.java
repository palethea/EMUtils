package net.emutils.client.emutils.tweaks;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

public final class FreelookManager {
	private KeyBinding keyBinding;
	private boolean active;
	private Perspective previousPerspective;
	private float playerYaw;
	private float playerPitch;
	private float yawOffset;
	private float pitchOffset;

	public void setKeyBinding(KeyBinding keyBinding) {
		this.keyBinding = keyBinding;
	}

	public void tick(MinecraftClient client) {
		boolean shouldBeActive = keyBinding != null
			&& keyBinding.isPressed()
			&& EMUtilsClient.config().tweakFreelook()
			&& client.player != null
			&& client.world != null
			&& client.currentScreen == null;

		if (shouldBeActive && !active) {
			playerYaw = client.player.getYaw();
			playerPitch = client.player.getPitch();
			yawOffset = 0.0F;
			pitchOffset = 0.0F;
			previousPerspective = client.options.getPerspective();
			if (previousPerspective.isFirstPerson()) {
				client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
			}
		}

		if (active && client.player != null) {
			lockPlayerRotation(client.player);
		}

		if (!shouldBeActive && active && previousPerspective != null) {
			client.options.setPerspective(previousPerspective);
			previousPerspective = null;
		}

		active = shouldBeActive;
	}

	public boolean isActive() {
		return active;
	}

	public void changeLookDirection(PlayerEntity player, double cursorDeltaX, double cursorDeltaY) {
		if (!active) {
			player.changeLookDirection(cursorDeltaX, cursorDeltaY);
			return;
		}

		yawOffset = (float) (yawOffset + cursorDeltaX * 0.15D);
		pitchOffset = MathHelper.clamp((float) (pitchOffset + cursorDeltaY * 0.15D), -90.0F, 90.0F);
	}

	public float cameraYaw() {
		return playerYaw + yawOffset;
	}

	public float cameraPitch() {
		return MathHelper.clamp(playerPitch + pitchOffset, -90.0F, 90.0F);
	}

	private void lockPlayerRotation(ClientPlayerEntity player) {
		player.setYaw(playerYaw);
		player.setPitch(playerPitch);
		player.lastYaw = playerYaw;
		player.lastPitch = playerPitch;
		player.renderYaw = playerYaw;
		player.renderPitch = playerPitch;
		player.setHeadYaw(playerYaw);
		player.setBodyYaw(playerYaw);
		player.lastRenderYaw = playerYaw;
		player.lastRenderPitch = playerPitch;
	}
}
