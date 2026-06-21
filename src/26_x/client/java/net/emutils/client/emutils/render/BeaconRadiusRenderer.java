package net.emutils.client.emutils.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.mixin.BeaconBlockEntityAccessor;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BeaconBeamOwner;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public final class BeaconRadiusRenderer {
	private static final int LINE_ALPHA = 190;
	private static final int SCAN_INTERVAL_TICKS = 20;

	@Nullable
	private static KeyMapping keyMapping;
	private static List<BeaconBlockEntity> cachedBeacons = List.of();
	@Nullable
	private static ClientLevel cachedLevel;
	private static int nextScanTick;

	private BeaconRadiusRenderer() {
	}

	public static void register() {
		LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(BeaconRadiusRenderer::render);
	}

	public static void setKeyMapping(KeyMapping binding) {
		keyMapping = binding;
	}

	public static void tick() {
		while (keyMapping != null && keyMapping.consumeClick()) {
			EMUtilsClient.config().setBeaconRadiusOutline(!EMUtilsClient.config().beaconRadiusOutline());
		}

		Minecraft client = Minecraft.getInstance();
		if (!EMUtilsClient.config().beaconRadiusOutline() || client.level == null || client.player == null) {
			cachedBeacons = List.of();
			cachedLevel = client.level;
			return;
		}
		if (cachedLevel != client.level || client.player.tickCount >= nextScanTick) {
			refreshCache(client);
		}
	}

	private static void refreshCache(Minecraft client) {
		if (client.level == null) {
			return;
		}
		Camera camera = client.gameRenderer.mainCamera();
		BlockPos cameraPos = camera.blockPosition();
		int cameraChunkX = cameraPos.getX() >> 4;
		int cameraChunkZ = cameraPos.getZ() >> 4;
		int chunkRadius = client.options.getEffectiveRenderDistance();
		List<BeaconBlockEntity> beacons = new ArrayList<>();
		for (int chunkX = cameraChunkX - chunkRadius; chunkX <= cameraChunkX + chunkRadius; chunkX++) {
			for (int chunkZ = cameraChunkZ - chunkRadius; chunkZ <= cameraChunkZ + chunkRadius; chunkZ++) {
				LevelChunk chunk = client.level.getChunkSource().getChunkNow(chunkX, chunkZ);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
					if (blockEntity instanceof BeaconBlockEntity beacon) {
						beacons.add(beacon);
					}
				}
			}
		}
		cachedBeacons = List.copyOf(beacons);
		cachedLevel = client.level;
		nextScanTick = (client.player == null ? 0 : client.player.tickCount) + SCAN_INTERVAL_TICKS;
	}

	private static void render(LevelRenderContext context) {
		Minecraft client = Minecraft.getInstance();
		if (!EMUtilsClient.config().beaconRadiusOutline() || client.level == null) {
			return;
		}

		Camera camera = context.gameRenderer().mainCamera();
		PoseStack matrices = context.poseStack();
		SubmitNodeCollector collector = context.submitNodeCollector();
		matrices.pushPose();
		try {
			matrices.translate(-camera.position().x, -camera.position().y, -camera.position().z);
			collector.submitCustomGeometry(matrices, RenderTypes.linesTranslucent(), (pose, buffer) -> {
				for (BeaconBlockEntity beacon : cachedBeacons) {
					if (!beacon.isRemoved() && beacon.getLevel() == client.level) {
						renderBeaconRadius(client, beacon, pose, buffer);
					}
				}
			});
		} finally {
			matrices.popPose();
		}
	}

	private static void renderBeaconRadius(
		Minecraft client,
		BeaconBlockEntity beacon,
		PoseStack.Pose pose,
		VertexConsumer buffer
	) {
		int levels = ((BeaconBlockEntityAccessor) beacon).emutils$getLevels();
		List<BeaconBeamOwner.Section> sections = beacon.getBeamSections();
		if (levels <= 0 || sections.isEmpty() || client.level == null) {
			return;
		}

		BlockPos pos = beacon.getBlockPos();
		double radius = levels * 10.0D + 10.0D;
		AABB bounds = new AABB(pos)
			.inflate(radius)
			.setMinY(Math.max(client.level.getMinY(), pos.getY() - radius))
			.setMaxY(client.level.getMaxY());
		int color = (LINE_ALPHA << 24) | (sections.getFirst().getColor() & 0x00FFFFFF);
		addLineBox(buffer, pose, bounds, color);
	}

	private static void addLineBox(VertexConsumer buffer, PoseStack.Pose pose, AABB box, int color) {
		double minX = box.minX;
		double minY = box.minY;
		double minZ = box.minZ;
		double maxX = box.maxX;
		double maxY = box.maxY;
		double maxZ = box.maxZ;

		addLine(buffer, pose, minX, minY, minZ, maxX, minY, minZ, color);
		addLine(buffer, pose, maxX, minY, minZ, maxX, minY, maxZ, color);
		addLine(buffer, pose, maxX, minY, maxZ, minX, minY, maxZ, color);
		addLine(buffer, pose, minX, minY, maxZ, minX, minY, minZ, color);
		addLine(buffer, pose, minX, maxY, minZ, maxX, maxY, minZ, color);
		addLine(buffer, pose, maxX, maxY, minZ, maxX, maxY, maxZ, color);
		addLine(buffer, pose, maxX, maxY, maxZ, minX, maxY, maxZ, color);
		addLine(buffer, pose, minX, maxY, maxZ, minX, maxY, minZ, color);
		addLine(buffer, pose, minX, minY, minZ, minX, maxY, minZ, color);
		addLine(buffer, pose, maxX, minY, minZ, maxX, maxY, minZ, color);
		addLine(buffer, pose, maxX, minY, maxZ, maxX, maxY, maxZ, color);
		addLine(buffer, pose, minX, minY, maxZ, minX, maxY, maxZ, color);
	}

	private static void addLine(
		VertexConsumer buffer,
		PoseStack.Pose pose,
		double x1,
		double y1,
		double z1,
		double x2,
		double y2,
		double z2,
		int color
	) {
		float dx = (float) (x2 - x1);
		float dy = (float) (y2 - y1);
		float dz = (float) (z2 - z1);
		float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		float nx = length == 0.0F ? 0.0F : dx / length;
		float ny = length == 0.0F ? 1.0F : dy / length;
		float nz = length == 0.0F ? 0.0F : dz / length;
		buffer.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(color).setNormal(pose, nx, ny, nz);
		buffer.addVertex(pose, (float) x2, (float) y2, (float) z2).setColor(color).setNormal(pose, nx, ny, nz);
	}
}
