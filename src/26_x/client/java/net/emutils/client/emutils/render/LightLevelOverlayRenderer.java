package net.emutils.client.emutils.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * MiniHUD-style cached light-level overlay. The comparatively expensive world scan and all line
 * geometry generation happen only after the camera moves more than four blocks (or changes world
 * or horizontal facing). Normal frames only submit the immutable cached line list.
 */
public final class LightLevelOverlayRenderer {
	private static final Identifier NUMBER_TEXTURE =
		Identifier.fromNamespaceAndPath("emutils", "textures/misc/light_level_numbers.png");
	private static final RenderPipeline NUMBER_PIPELINE = RenderPipelines.register(
		RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
			.withLocation("pipeline/emutils_light_level_numbers")
			.withVertexShader("core/position_tex_color")
			.withFragmentShader("core/position_tex_color")
			.withBindGroupLayout(BindGroupLayouts.SAMPLER0)
			.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
			.withCull(false)
			.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
			.withPrimitiveTopology(PrimitiveTopology.QUADS)
			.withDepthStencilState(DepthStencilState.DEFAULT)
			.build()
	);
	private static final RenderType NUMBER_RENDER_TYPE = RenderType.create(
		"emutils_light_level_numbers",
		RenderSetup.builder(NUMBER_PIPELINE)
			.withTexture("Sampler0", NUMBER_TEXTURE)
			.createRenderSetup()
	);
	private static final int RANGE = 24;
	private static final int SPAWN_LIGHT_LEVEL = 0;
	private static final double MARKER_MIN = 0.08D;
	private static final double MARKER_MAX = 0.92D;
	private static final double SURFACE_OFFSET = 0.01D;
	private static final int NUMBER_LIT = 0xFF20FF40;
	private static final int NUMBER_DARK = 0xFFC03030;
	private static final int MARKER_SKY_LIT = 0xFFFFFF33;
	private static final int MARKER_DARK = 0xFFFF4848;

	@Nullable
	private static KeyMapping keyMapping;
	private static List<Line> cachedLines = List.of();
	private static List<NumberQuad> cachedNumbers = List.of();
	@Nullable
	private static ClientLevel cachedLevel;
	@Nullable
	private static BlockPos lastUpdatePos;

	private LightLevelOverlayRenderer() {
	}

	public static void register() {
		// Custom geometry must be collected during extraction. Submitting it from a
		// drawing event leaves it queued until a later frame, making the overlay lag
		// behind the camera and occasionally disappear.
		LevelRenderEvents.COLLECT_SUBMITS.register(LightLevelOverlayRenderer::render);
	}

	public static void setKeyMapping(KeyMapping binding) {
		keyMapping = binding;
	}

	public static void tick() {
		while (keyMapping != null && keyMapping.consumeClick()) {
			EMUtilsClient.config().setLightLevelOverlay(!EMUtilsClient.config().lightLevelOverlay());
			invalidate();
		}

		Minecraft client = Minecraft.getInstance();
		if (!EMUtilsClient.config().lightLevelOverlay() || client.level == null || client.player == null) {
			cachedLines = List.of();
			cachedNumbers = List.of();
			cachedLevel = client.level;
			lastUpdatePos = null;
			return;
		}

		BlockPos center = client.player.blockPosition();
		if (needsUpdate(client.level, center)) {
			refreshCache(client.level, center);
		}
	}

	public static void invalidate() {
		lastUpdatePos = null;
	}

	/** MiniHUD-style packet invalidation: only rebuild for chunk changes near the camera. */
	public static void onChunkChanged(int chunkX, int chunkZ) {
		Entity cameraEntity = Minecraft.getInstance().getCameraEntity();
		if (cameraEntity == null) {
			return;
		}

		Vec3 cameraPos = cameraEntity.position();
		if (Math.abs(cameraPos.x - (chunkX << 4) - 8) <= 48.0D
			|| Math.abs(cameraPos.z - (chunkZ << 4) - 8) <= 48.0D) {
			invalidate();
		}
	}

	private static boolean needsUpdate(ClientLevel level, BlockPos center) {
		return cachedLevel != level
			|| lastUpdatePos == null
			|| Math.abs(center.getX() - lastUpdatePos.getX()) > 4
			|| Math.abs(center.getY() - lastUpdatePos.getY()) > 4
			|| Math.abs(center.getZ() - lastUpdatePos.getZ()) > 4;
	}

	private static void refreshCache(ClientLevel level, BlockPos center) {
		List<Line> lines = new ArrayList<>();
		List<NumberQuad> numbers = new ArrayList<>();
		int minX = center.getX() - RANGE;
		int maxX = center.getX() + RANGE;
		int minZ = center.getZ() - RANGE;
		int maxZ = center.getZ() + RANGE;
		int minY = Math.max(level.getMinY() + 1, center.getY() - RANGE);
		int maxY = Math.min(level.getMaxY() - 2, center.getY() + RANGE);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();

		for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
			int startX = Math.max(minX, chunkX << 4);
			int endX = Math.min(maxX, (chunkX << 4) + 15);
			for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
				if (chunk == null) {
					continue;
				}
				int startZ = Math.max(minZ, chunkZ << 4);
				int endZ = Math.min(maxZ, (chunkZ << 4) + 15);
				for (int y = minY; y <= maxY; y++) {
					// MiniHUD skips entire empty sections below the tested spawn layer.
					if (y > minY && chunk.getSection(chunk.getSectionIndex(y - 1)).hasOnlyAir()) {
						continue;
					}
					for (int x = startX; x <= endX; x++) {
						for (int z = startZ; z <= endZ; z++) {
							pos.set(x, y, z);
							below.set(x, y - 1, z);
							above.set(x, y + 1, z);
							if (!isSpawnSurface(chunk, pos, below, above)) {
								continue;
							}

							int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
							int skyLight = level.getBrightness(LightLayer.SKY, pos);
							numbers.add(new NumberQuad(
								x + 0.26D,
								y + SURFACE_OFFSET,
								z + 0.32D,
								blockLight,
								blockLight > SPAWN_LIGHT_LEVEL ? NUMBER_LIT : NUMBER_DARK
							));
							if (blockLight == SPAWN_LIGHT_LEVEL) {
								addSquare(lines, x, y + SURFACE_OFFSET, z,
									skyLight > SPAWN_LIGHT_LEVEL ? MARKER_SKY_LIT : MARKER_DARK);
							}
						}
					}
				}
			}
		}

		cachedLines = List.copyOf(lines);
		cachedNumbers = List.copyOf(numbers);
		cachedLevel = level;
		lastUpdatePos = center.immutable();
	}

	private static boolean isSpawnSurface(LevelChunk chunk, BlockPos pos, BlockPos below, BlockPos above) {
		BlockState floor = chunk.getBlockState(below);
		if (floor.is(Blocks.BEDROCK) || floor.is(Blocks.BARRIER)
			|| !floor.isValidSpawn(chunk, below, EntityTypes.ZOMBIE)) {
			return false;
		}
		BlockState state = chunk.getBlockState(pos);
		BlockState stateAbove = chunk.getBlockState(above);
		return NaturalSpawner.isValidEmptySpawnBlock(chunk, pos, state, state.getFluidState(), EntityTypes.ZOMBIE)
			&& NaturalSpawner.isValidEmptySpawnBlock(chunk, above, stateAbove, stateAbove.getFluidState(), EntityTypes.ZOMBIE);
	}

	private static void addSquare(List<Line> lines, double x, double y, double z, int color) {
		addLine(lines, x + MARKER_MIN, y, z + MARKER_MIN, x + MARKER_MIN, y, z + MARKER_MAX, color, 1.5F);
		addLine(lines, x + MARKER_MIN, y, z + MARKER_MAX, x + MARKER_MAX, y, z + MARKER_MAX, color, 1.5F);
		addLine(lines, x + MARKER_MAX, y, z + MARKER_MAX, x + MARKER_MAX, y, z + MARKER_MIN, color, 1.5F);
		addLine(lines, x + MARKER_MAX, y, z + MARKER_MIN, x + MARKER_MIN, y, z + MARKER_MIN, color, 1.5F);
	}

	private static void addLine(
		List<Line> lines, double x1, double y1, double z1, double x2, double y2, double z2,
		int color, float width
	) {
		lines.add(new Line(x1, y1, z1, x2, y2, z2, color, width));
	}

	private static void render(LevelRenderContext context) {
		if (!EMUtilsClient.config().lightLevelOverlay() || (cachedLines.isEmpty() && cachedNumbers.isEmpty())) {
			return;
		}
		Vec3 cameraPosition = context.levelState().cameraRenderState.pos;
		PoseStack matrices = context.poseStack();
		SubmitNodeCollector collector = context.submitNodeCollector();
		matrices.pushPose();
		try {
			matrices.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
			collector.submitCustomGeometry(matrices, RenderTypes.lines(), (pose, buffer) -> {
				for (Line line : cachedLines) {
					line.render(buffer, pose);
				}
			});
			collector.submitCustomGeometry(matrices, NUMBER_RENDER_TYPE, (pose, buffer) -> {
				for (NumberQuad number : cachedNumbers) {
					number.render(buffer, pose);
				}
			});
		} finally {
			matrices.popPose();
		}
	}

	/** Fixed NORTH quad layout copied from MiniHUD's light-level renderer. */
	private record NumberQuad(double x, double y, double z, int lightLevel, int color) {
		private void render(VertexConsumer buffer, PoseStack.Pose pose) {
			float cell = 0.25F;
			float u = (lightLevel & 0x3) * cell;
			float v = (lightLevel >> 2) * cell;
			addVertex(buffer, pose, x, y, z, u, v, color);
			addVertex(buffer, pose, x, y, z + 1.0D, u, v + cell, color);
			addVertex(buffer, pose, x + 1.0D, y, z + 1.0D, u + cell, v + cell, color);
			addVertex(buffer, pose, x + 1.0D, y, z, u + cell, v, color);
		}

		private static void addVertex(
			VertexConsumer buffer, PoseStack.Pose pose, double x, double y, double z,
			float u, float v, int color
		) {
			buffer.addVertex(pose, (float) x, (float) y, (float) z)
				.setColor(color)
				.setUv(u, v);
		}
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
