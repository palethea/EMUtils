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
	private static final int SCAN_INTERVAL_TICKS = 20;
	private static final int HORIZONTAL_GRID_STEP = 4;
	private static final int VERTICAL_GRID_STEP = 5;

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
			collector.submitCustomGeometry(matrices, RenderTypes.lines(), (pose, buffer) -> {
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
		int rgb = sections.getFirst().getColor() & 0x00FFFFFF;
		int color = 0xFF000000 | rgb;
		addGridOutline(buffer, pose, bounds, color);
	}

	private static void addGridOutline(
		VertexConsumer buffer,
		PoseStack.Pose pose,
		AABB box,
		int color
	) {
		for (double y = box.minY + HORIZONTAL_GRID_STEP; y < box.maxY; y += HORIZONTAL_GRID_STEP) {
			addHorizontalOutline(buffer, pose, box, y, color, 1.0F);
		}
		addHorizontalOutline(buffer, pose, box, box.minY, color, 2.0F);
		addHorizontalOutline(buffer, pose, box, box.maxY, color, 2.0F);

		for (double x = box.minX + VERTICAL_GRID_STEP; x < box.maxX; x += VERTICAL_GRID_STEP) {
			addLine(buffer, pose, x, box.minY, box.minZ, x, box.maxY, box.minZ, color, 1.0F);
			addLine(buffer, pose, x, box.minY, box.maxZ, x, box.maxY, box.maxZ, color, 1.0F);
		}
		for (double z = box.minZ + VERTICAL_GRID_STEP; z < box.maxZ; z += VERTICAL_GRID_STEP) {
			addLine(buffer, pose, box.minX, box.minY, z, box.minX, box.maxY, z, color, 1.0F);
			addLine(buffer, pose, box.maxX, box.minY, z, box.maxX, box.maxY, z, color, 1.0F);
		}

		addLine(buffer, pose, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, color, 2.0F);
		addLine(buffer, pose, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, color, 2.0F);
		addLine(buffer, pose, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, color, 2.0F);
		addLine(buffer, pose, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, color, 2.0F);
	}

	private static void addHorizontalOutline(
		VertexConsumer buffer,
		PoseStack.Pose pose,
		AABB box,
		double y,
		int color,
		float width
	) {
		addLine(buffer, pose, box.minX, y, box.minZ, box.maxX, y, box.minZ, color, width);
		addLine(buffer, pose, box.maxX, y, box.minZ, box.maxX, y, box.maxZ, color, width);
		addLine(buffer, pose, box.maxX, y, box.maxZ, box.minX, y, box.maxZ, color, width);
		addLine(buffer, pose, box.minX, y, box.maxZ, box.minX, y, box.minZ, color, width);
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
		int color,
		float width
	) {
		float dx = (float) (x2 - x1);
		float dy = (float) (y2 - y1);
		float dz = (float) (z2 - z1);
		float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		float nx = length == 0.0F ? 0.0F : dx / length;
		float ny = length == 0.0F ? 1.0F : dy / length;
		float nz = length == 0.0F ? 0.0F : dz / length;
		buffer.addVertex(pose, (float) x1, (float) y1, (float) z1)
			.setColor(color)
			.setNormal(pose, nx, ny, nz)
			.setLineWidth(width);
		buffer.addVertex(pose, (float) x2, (float) y2, (float) z2)
			.setColor(color)
			.setNormal(pose, nx, ny, nz)
			.setLineWidth(width);
	}
}
