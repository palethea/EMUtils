package net.emutils.client.emutils.tweaks;

import net.emutils.client.EMUtilsClient;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class TweaksManager {
	private final FreelookManager freelook = new FreelookManager();
	private boolean removeWorldFog;

	public void setKeyBindings(KeyBinding freelookKey) {
		freelook.setKeyBinding(freelookKey);
	}

	public void tick(net.minecraft.client.MinecraftClient client) {
		freelook.tick(client);
	}

	public FreelookManager freelook() {
		return freelook;
	}

	public void updateFogState(Camera camera, World world) {
		removeWorldFog = shouldRemoveFog(camera, world);
	}

	public boolean removeWorldFog() {
		return removeWorldFog;
	}

	public boolean shouldRemoveFog(Camera camera, World world) {
		if (EMUtilsClient.config().tweakNoFog()) {
			return true;
		}

		CameraSubmersionType submersion = camera.getSubmersionType();
		if (submersion == CameraSubmersionType.WATER && EMUtilsClient.config().tweakClearUnderwater()) {
			return true;
		}
		if (submersion == CameraSubmersionType.LAVA && EMUtilsClient.config().tweakClearLava()) {
			return true;
		}
		if (!EMUtilsClient.config().tweakNoEnvironmentFog()) {
			return false;
		}

		if (world.getRegistryKey() == World.NETHER || world.getRegistryKey() == World.END) {
			return true;
		}
		if (world.getRainGradient(1.0F) > 0.0F) {
			return true;
		}

		BlockPos pos = camera.getBlockPos();
		return !world.isSkyVisible(pos) && pos.getY() < world.getSeaLevel();
	}
}
