package net.emutils.client.emutils.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.compat.XaeroMapIntegration;
import net.emutils.client.mixin.BeaconBlockEntityAccessor;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
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
	private static final int SCAN_INTERVAL_TICKS = 40;
	private static final int HORIZONTAL_GRID_STEP = 16;
	private static final int VERTICAL_GRID_STEP = 16;
	private static final double MAP_POINT_SPACING = 0.5D;
	private static final boolean XAERO_MINIMAP_LOADED = FabricLoader.getInstance().isModLoaded("xaerominimap");

	@Nullable
	private static KeyMapping keyMapping;
	private static List<Line> cachedLines = List.of();
	private static List<BeaconMapPoint> cachedMapPoints = List.of();
	@Nullable
	private static ClientLevel cachedLevel;
	private static int nextScanTick;

	private BeaconRadiusRenderer() {
	}

	public static void register() {
		LevelRenderEvents.COLLECT_SUBMITS.register(BeaconRadiusRenderer::render);
	}

	public static void setKeyMapping(KeyMapping binding) {
		keyMapping = binding;
	}

	public static void tick() {
		if (XAERO_MINIMAP_LOADED) {
			XaeroMapIntegration.tick();
		}

		while (keyMapping != null && keyMapping.consumeClick()) {
			EMUtilsClient.config().setBeaconRadiusOutline(!EMUtilsClient.config().beaconRadiusOutline());
			nextScanTick = 0;
		}

		Minecraft client = Minecraft.getInstance();
		if (!EMUtilsClient.config().beaconRadiusOutline() || client.level == null || client.player == null) {
			cachedLines = List.of();
			cachedMapPoints = List.of();
			cachedLevel = client.level;
			nextScanTick = 0;
			return;
		}
		if (cachedLevel != client.level || cachedLines.isEmpty() || client.player.tickCount >= nextScanTick) {
			refreshCache(client);
		}
	}

	private static void refreshCache(Minecraft client) {
		if (client.level == null) {
			return;
		}
		Camera camera = net.emutils.client.emutils.compat.MinecraftClientCompat.mainCamera(client);
		BlockPos cameraPos = camera.blockPosition();
		int cameraChunkX = cameraPos.getX() >> 4;
		int cameraChunkZ = cameraPos.getZ() >> 4;
		int chunkRadius = client.options.getEffectiveRenderDistance();
		List<Line> lines = new ArrayList<>();
		List<BeaconMapPoint> mapPoints = new ArrayList<>();
		for (int chunkX = cameraChunkX - chunkRadius; chunkX <= cameraChunkX + chunkRadius; chunkX++) {
			for (int chunkZ = cameraChunkZ - chunkRadius; chunkZ <= cameraChunkZ + chunkRadius; chunkZ++) {
				LevelChunk chunk = client.level.getChunkSource().getChunkNow(chunkX, chunkZ);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
					if (blockEntity instanceof BeaconBlockEntity beacon) {
						addBeaconGeometry(client.level, beacon, lines, mapPoints);
					}
				}
			}
		}
		cachedLines = List.copyOf(lines);
		cachedMapPoints = List.copyOf(mapPoints);
		cachedLevel = client.level;
		nextScanTick = (client.player == null ? 0 : client.player.tickCount) + SCAN_INTERVAL_TICKS;
	}

	public static List<BeaconMapPoint> mapPoints() {
		return cachedMapPoints;
	}

	private static void render(LevelRenderContext context) {
		if (!EMUtilsClient.config().beaconRadiusOutline() || cachedLines.isEmpty()) {
			return;
		}

		PoseStack matrices = context.poseStack();
		SubmitNodeCollector collector = context.submitNodeCollector();
		matrices.pushPose();
		try {
			matrices.translate(
				-context.levelState().cameraRenderState.pos.x,
				-context.levelState().cameraRenderState.pos.y,
				-context.levelState().cameraRenderState.pos.z
			);
			collector.submitCustomGeometry(matrices, RenderTypes.lines(), (pose, buffer) -> {
				for (Line line : cachedLines) {
					line.render(buffer, pose);
				}
			});
		} finally {
			matrices.popPose();
		}
	}

	private static void addBeaconGeometry(
		ClientLevel level,
		BeaconBlockEntity beacon,
		List<Line> lines,
		List<BeaconMapPoint> mapPoints
	) {
		int levels = ((BeaconBlockEntityAccessor) beacon).emutils$getLevels();
		List<BeaconBeamOwner.Section> sections = beacon.getBeamSections();
		if (levels <= 0 || sections.isEmpty() || beacon.isRemoved()) {
			return;
		}

		BlockPos pos = beacon.getBlockPos();
		double radius = levels * 10.0D + 10.0D;
		AABB bounds = new AABB(pos)
			.inflate(radius)
			.setMinY(Math.max(level.getMinY(), pos.getY() - radius))
			.setMaxY(level.getMaxY());
		int rgb = sections.getFirst().getColor() & 0x00FFFFFF;
		addGridOutline(lines, bounds, 0xB3000000 | rgb);
		addMapPoints(mapPoints, bounds.minX, bounds.maxX, bounds.minZ, bounds.maxZ, pos.getY(), 0xCC000000 | rgb);
	}

	private static void addMapPoints(
		List<BeaconMapPoint> mapPoints,
		double minX,
		double maxX,
		double minZ,
		double maxZ,
		double y,
		int color
	) {
		for (double x = minX; x <= maxX + 0.001D; x += MAP_POINT_SPACING) {
			mapPoints.add(new BeaconMapPoint(x, y, minZ, color));
			mapPoints.add(new BeaconMapPoint(x, y, maxZ, color));
		}
		for (double z = minZ + MAP_POINT_SPACING; z < maxZ - 0.001D; z += MAP_POINT_SPACING) {
			mapPoints.add(new BeaconMapPoint(minX, y, z, color));
			mapPoints.add(new BeaconMapPoint(maxX, y, z, color));
		}
	}

	private static void addGridOutline(
		List<Line> lines,
		AABB box,
		int color
	) {
		for (double y = box.minY + HORIZONTAL_GRID_STEP; y < box.maxY; y += HORIZONTAL_GRID_STEP) {
			addHorizontalOutline(lines, box, y, color, 1.0F);
		}
		addHorizontalOutline(lines, box, box.minY, color, 1.5F);
		addHorizontalOutline(lines, box, box.maxY, color, 1.5F);

		for (double x = box.minX + VERTICAL_GRID_STEP; x < box.maxX; x += VERTICAL_GRID_STEP) {
			addLine(lines, x, box.minY, box.minZ, x, box.maxY, box.minZ, color, 1.0F);
			addLine(lines, x, box.minY, box.maxZ, x, box.maxY, box.maxZ, color, 1.0F);
		}
		for (double z = box.minZ + VERTICAL_GRID_STEP; z < box.maxZ; z += VERTICAL_GRID_STEP) {
			addLine(lines, box.minX, box.minY, z, box.minX, box.maxY, z, color, 1.0F);
			addLine(lines, box.maxX, box.minY, z, box.maxX, box.maxY, z, color, 1.0F);
		}

		addLine(lines, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, color, 1.5F);
		addLine(lines, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, color, 1.5F);
		addLine(lines, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, color, 1.5F);
		addLine(lines, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, color, 1.5F);
	}

	private static void addHorizontalOutline(
		List<Line> lines,
		AABB box,
		double y,
		int color,
		float width
	) {
		addLine(lines, box.minX, y, box.minZ, box.maxX, y, box.minZ, color, width);
		addLine(lines, box.maxX, y, box.minZ, box.maxX, y, box.maxZ, color, width);
		addLine(lines, box.maxX, y, box.maxZ, box.minX, y, box.maxZ, color, width);
		addLine(lines, box.minX, y, box.maxZ, box.minX, y, box.minZ, color, width);
	}

	private static void addLine(
		List<Line> lines,
		double x1,
		double y1,
		double z1,
		double x2,
		double y2,
		double z2,
		int color,
		float width
	) {
		lines.add(new Line(x1, y1, z1, x2, y2, z2, color, width));
	}

	public record BeaconMapPoint(double x, double y, double z, int color) {
	}

	private record Line(
		double x1, double y1, double z1, double x2, double y2, double z2, int color, float width
	) {
		private void render(VertexConsumer buffer, PoseStack.Pose pose) {
			float dx = (float) (x2 - x1);
			float dy = (float) (y2 - y1);
			float dz = (float) (z2 - z1);
			float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
			float nx = length == 0.0F ? 0.0F : dx / length;
			float ny = length == 0.0F ? 1.0F : dy / length;
			float nz = length == 0.0F ? 0.0F : dz / length;
			buffer.addVertex(pose, (float) x1, (float) y1, (float) z1)
				.setColor(color).setNormal(pose, nx, ny, nz).setLineWidth(width);
			buffer.addVertex(pose, (float) x2, (float) y2, (float) z2)
				.setColor(color).setNormal(pose, nx, ny, nz).setLineWidth(width);
		}
	}
}
