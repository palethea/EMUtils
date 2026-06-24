package net.emutils.client.emutils.tweaks;

import net.emutils.client.EMUtilsClient;
import net.minecraft.world.level.material.FogType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class TweaksManager {
	private final FreelookManager freelook = new FreelookManager();
	private final AutoToolManager autoTool = new AutoToolManager();
	private final AutoFlightGearManager autoFlightGear = new AutoFlightGearManager();
	private final PlaceBelowManager placeBelow = new PlaceBelowManager();
	private final LockedYPlacementManager lockedYPlacement = new LockedYPlacementManager();
	private final FreeCameraManager freeCamera = new FreeCameraManager();
	private boolean removeWorldFog;

	public void setKeyMappings(KeyMapping freelookKey, KeyMapping placeBelowKey, KeyMapping lockedYPlacementKey, KeyMapping freeCameraKey) {
		freelook.setKeyMapping(freelookKey);
		placeBelow.setKeyMapping(placeBelowKey);
		lockedYPlacement.setKeyMapping(lockedYPlacementKey);
		freeCamera.setKeyMapping(freeCameraKey);
	}

	public void tick(net.minecraft.client.Minecraft client) {
		freelook.tick(client);
		autoFlightGear.tick(client);
		lockedYPlacement.tick(client);
		freeCamera.tick(client);
	}

	public void resetSession() {
		autoFlightGear.reset();
		lockedYPlacement.reset();
		freeCamera.reset();
	}

	public void tickAutoTool(net.minecraft.client.Minecraft client) {
		autoTool.tick(client);
	}

	public FreelookManager freelook() {
		return freelook;
	}

	public PlaceBelowManager placeBelow() {
		return placeBelow;
	}

	public LockedYPlacementManager lockedYPlacement() {
		return lockedYPlacement;
	}

	public FreeCameraManager freeCamera() {
		return freeCamera;
	}

	public void updateFogState(Camera camera, Level world) {
		removeWorldFog = shouldRemoveFog(camera, world);
	}

	public boolean removeWorldFog() {
		return removeWorldFog;
	}

	public boolean shouldRemoveFog(Camera camera, Level world) {
		if (EMUtilsClient.config().tweakNoFog()) {
			return true;
		}

		FogType submersion = camera.getFluidInCamera();
		if (submersion == FogType.WATER && EMUtilsClient.config().tweakClearUnderwater()) {
			return true;
		}
		if (submersion == FogType.LAVA && EMUtilsClient.config().tweakClearLava()) {
			return true;
		}
		if (!EMUtilsClient.config().tweakNoEnvironmentFog()) {
			return false;
		}

		if (world.dimension() == Level.NETHER || world.dimension() == Level.END) {
			return true;
		}
		if (world.getRainLevel(1.0F) > 0.0F) {
			return true;
		}

		BlockPos pos = camera.blockPosition();
		return !world.canSeeSkyFromBelowWater(pos) && pos.getY() < world.getSeaLevel();
	}
}
