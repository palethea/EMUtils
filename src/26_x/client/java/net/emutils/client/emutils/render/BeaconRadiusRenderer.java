package net.emutils.client.emutils.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.compat.XaeroMapIntegration;
import net.emutils.client.mixin.BeaconBlockEntityAccessor;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.ARGB;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BeaconBeamOwner;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public final class BeaconRadiusRenderer {
	private static final int SCAN_INTERVAL_TICKS = 40;
	/** The cage's edges are drawn this much wider than its grid lines. */
	private static final float EDGE_WIDTH_SCALE = 1.5F;
	private static final double MAP_POINT_SPACING = 0.5D;
	private static final boolean XAERO_MINIMAP_LOADED = FabricLoader.getInstance().isModLoaded("xaerominimap");

	@Nullable
	private static KeyMapping keyMapping;
	/** How much of the way to white a cage is drawn when it touches another cage of the same color. */
	private static final float NEIGHBOR_LIGHTEN = 0.45F;
	/** A white (or nearly white) cage touching another of its color is drawn this light gray instead. */
	private static final int WHITE_NEIGHBOR = 0xFFB4B4B4;

	private static final WorldLines.Batch LINES = new WorldLines.Batch();

	private static List<WorldLines.Line> cachedLines = List.of();
	private static List<BeaconMapPoint> cachedMapPoints = List.of();
	@Nullable
	private static ClientLevel cachedLevel;
	private static int nextScanTick;
	private static int beaconCount;
	private static List<Integer> outlineColors = List.of();
	/** The settings the cache was built with; changing any of them rescans right away. */
	private static int cachedRange = -1;
	private static boolean cachedActiveOnly;
	private static int cachedGridSpacing;
	private static int cachedLineWidth;
	/** The beacon last right-clicked, so an effect picked in its screen can be applied to the client's copy. */
	@Nullable
	private static BlockPos openBeacon;

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
			beaconCount = 0;
			cachedLevel = client.level;
			nextScanTick = 0;
			return;
		}
		if (cachedRange != EMUtilsClient.config().beaconRadiusRange()
			|| cachedActiveOnly != EMUtilsClient.config().beaconRadiusActiveOnly()
			|| cachedGridSpacing != EMUtilsClient.config().beaconRadiusGridSpacing()
			|| cachedLineWidth != EMUtilsClient.config().beaconRadiusLineWidth()) {
			nextScanTick = 0;
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
		int range = EMUtilsClient.config().beaconRadiusRange();
		boolean activeOnly = EMUtilsClient.config().beaconRadiusActiveOnly();
		int gridSpacing = EMUtilsClient.config().beaconRadiusGridSpacing();
		int lineWidth = EMUtilsClient.config().beaconRadiusLineWidth();
		int chunkRadius = Math.min(client.options.getEffectiveRenderDistance(), range);
		List<Outline> outlines = new ArrayList<>();
		for (int chunkX = cameraChunkX - chunkRadius; chunkX <= cameraChunkX + chunkRadius; chunkX++) {
			for (int chunkZ = cameraChunkZ - chunkRadius; chunkZ <= cameraChunkZ + chunkRadius; chunkZ++) {
				LevelChunk chunk = client.level.getChunkSource().getChunkNow(chunkX, chunkZ);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
					if (blockEntity instanceof BeaconBlockEntity beacon
						&& (!activeOnly || ((BeaconBlockEntityAccessor) beacon).emutils$getPrimaryPower() != null)) {
						Outline outline = outline(client.level, beacon);
						if (outline != null) {
							outlines.add(outline);
						}
					}
				}
			}
		}
		List<WorldLines.Line> lines = new ArrayList<>();
		List<BeaconMapPoint> mapPoints = new ArrayList<>();
		int[] colors = colors(outlines);
		for (int i = 0; i < outlines.size(); i++) {
			AABB bounds = outlines.get(i).bounds();
			addGridOutline(lines, bounds, colors[i], gridSpacing, lineWidth);
			addMapPoints(mapPoints, bounds.minX, bounds.maxX, bounds.minZ, bounds.maxZ, outlines.get(i).pos().getY(), 0xCC000000 | (colors[i] & 0x00FFFFFF));
		}
		cachedLines = List.copyOf(lines);
		cachedMapPoints = List.copyOf(mapPoints);
		cachedLevel = client.level;
		cachedRange = range;
		beaconCount = outlines.size();
		outlineColors = Arrays.stream(colors).boxed().toList();
		cachedActiveOnly = activeOnly;
		cachedGridSpacing = gridSpacing;
		cachedLineWidth = lineWidth;
		nextScanTick = (client.player == null ? 0 : client.player.tickCount) + SCAN_INTERVAL_TICKS;
	}

	/** Remembers a right-clicked beacon: the beacon screen that opens next belongs to it, but doesn't know its position. */
	public static void onBlockUsed(BlockPos pos) {
		Minecraft client = Minecraft.getInstance();
		if (client.level != null && client.level.getBlockEntity(pos) instanceof BeaconBlockEntity) {
			openBeacon = pos.immutable();
		}
	}

	/**
	 * Called when the beacon screen confirms an effect. The server saves it without telling clients,
	 * which only learn a beacon's effect when its chunk loads, so without this Only Active Beacons
	 * would skip a beacon you just set up until you relog.
	 */
	public static void onBeaconEffectPicked(Optional<Holder<MobEffect>> primary) {
		Minecraft client = Minecraft.getInstance();
		if (openBeacon == null || primary.isEmpty() || client.level == null
			|| !(net.emutils.client.emutils.compat.MinecraftClientCompat.screen(client) instanceof BeaconScreen)) {
			return;
		}
		if (client.level.getBlockEntity(openBeacon) instanceof BeaconBlockEntity beacon) {
			((BeaconBlockEntityAccessor) beacon).emutils$setPrimaryPower(primary.get());
			nextScanTick = 0;
		}
	}

	public static List<BeaconMapPoint> mapPoints() {
		return cachedMapPoints;
	}

	/** How many lines the outline currently draws, for UI snapshot checks. */
	public static int lineCountForSnapshot() {
		return cachedLines.size();
	}

	/** The colors the cages are drawn in, for UI snapshot checks. */
	public static List<Integer> outlineColorsForSnapshot() {
		return outlineColors;
	}

	/** How many beacons the outline currently draws, for UI snapshot checks. */
	public static int outlinedBeaconsForSnapshot() {
		return beaconCount;
	}

	private static void render(LevelRenderContext context) {
		if (!EMUtilsClient.config().beaconRadiusOutline() || cachedLines.isEmpty()) {
			return;
		}

		WorldLines.Batch batch = LINES.prepare(cachedLines, context.levelState().cameraRenderState);
		if (batch.isEmpty()) {
			return;
		}
		PoseStack matrices = context.poseStack();
		SubmitNodeCollector collector = context.submitNodeCollector();
		collector.submitCustomGeometry(matrices, RenderTypes.lines(), batch::render);
	}

	/** A beacon's effect area, if it has a pyramid and an unblocked beam. */
	@Nullable
	private static Outline outline(ClientLevel level, BeaconBlockEntity beacon) {
		int levels = ((BeaconBlockEntityAccessor) beacon).emutils$getLevels();
		List<BeaconBeamOwner.Section> sections = beacon.getBeamSections();
		if (levels <= 0 || sections.isEmpty() || beacon.isRemoved()) {
			return null;
		}

		BlockPos pos = beacon.getBlockPos();
		double radius = levels * 10.0D + 10.0D;
		AABB bounds = new AABB(pos)
			.inflate(radius)
			.setMinY(Math.max(level.getMinY(), pos.getY() - radius))
			.setMaxY(level.getMaxY());
		Holder<MobEffect> effect = ((BeaconBlockEntityAccessor) beacon).emutils$getPrimaryPower();
		int beam = sections.getFirst().getColor() & 0x00FFFFFF;
		// An undyed beam is white glass's off-white; its cage is drawn plain white.
		int rgb = effect != null ? effect.value().getColor() : beam == (DyeColor.WHITE.getTextureDiffuseColor() & 0x00FFFFFF) ? 0xFFFFFF : beam;
		return new Outline(pos.immutable(), bounds, rgb & 0x00FFFFFF);
	}

	/**
	 * Each cage takes its beacon's effect color, or its beam color before an effect is picked (white
	 * unless the beam is dyed). Where cages of the same color touch, every other one is drawn lighter,
	 * so neighbors stay apart.
	 */
	private static int[] colors(List<Outline> outlines) {
		List<Integer> order = new ArrayList<>();
		for (int i = 0; i < outlines.size(); i++) {
			order.add(i);
		}
		order.sort(Comparator.comparingLong(i -> outlines.get(i).pos().asLong()));
		boolean[] lighter = new boolean[outlines.size()];
		for (int i = 0; i < order.size(); i++) {
			Outline outline = outlines.get(order.get(i));
			AABB touching = outline.bounds().inflate(0.5D);
			boolean nextToDefault = false;
			boolean nextToLighter = false;
			for (int j = 0; j < i; j++) {
				Outline earlier = outlines.get(order.get(j));
				if (earlier.rgb() == outline.rgb() && earlier.bounds().intersects(touching)) {
					if (lighter[order.get(j)]) {
						nextToLighter = true;
					} else {
						nextToDefault = true;
					}
				}
			}
			lighter[order.get(i)] = nextToDefault && !nextToLighter;
		}
		int[] colors = new int[outlines.size()];
		for (int i = 0; i < outlines.size(); i++) {
			int rgb = outlines.get(i).rgb();
			if (!lighter[i]) {
				colors[i] = 0xFF000000 | rgb;
			} else if (ARGB.red(rgb) > 0xE0 && ARGB.green(rgb) > 0xE0 && ARGB.blue(rgb) > 0xE0) {
				// Too light to get lighter.
				colors[i] = WHITE_NEIGHBOR;
			} else {
				colors[i] = ARGB.srgbLerp(NEIGHBOR_LIGHTEN, 0xFF000000 | rgb, 0xFFFFFFFF);
			}
		}
		return colors;
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
		List<WorldLines.Line> lines,
		AABB box,
		int color,
		int spacing,
		float width
	) {
		float edge = width * EDGE_WIDTH_SCALE;
		for (double y = box.minY + spacing; y < box.maxY; y += spacing) {
			addHorizontalOutline(lines, box, y, color, width);
		}
		addHorizontalOutline(lines, box, box.minY, color, edge);
		addHorizontalOutline(lines, box, box.maxY, color, edge);

		for (double x = box.minX + spacing; x < box.maxX; x += spacing) {
			addLine(lines, x, box.minY, box.minZ, x, box.maxY, box.minZ, color, width);
			addLine(lines, x, box.minY, box.maxZ, x, box.maxY, box.maxZ, color, width);
		}
		for (double z = box.minZ + spacing; z < box.maxZ; z += spacing) {
			addLine(lines, box.minX, box.minY, z, box.minX, box.maxY, z, color, width);
			addLine(lines, box.maxX, box.minY, z, box.maxX, box.maxY, z, color, width);
		}

		addLine(lines, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, color, edge);
		addLine(lines, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, color, edge);
		addLine(lines, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, color, edge);
		addLine(lines, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, color, edge);
	}

	private static void addHorizontalOutline(
		List<WorldLines.Line> lines,
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
		List<WorldLines.Line> lines,
		double x1,
		double y1,
		double z1,
		double x2,
		double y2,
		double z2,
		int color,
		float width
	) {
		lines.add(new WorldLines.Line(x1, y1, z1, x2, y2, z2, color, width));
	}

	public record BeaconMapPoint(double x, double y, double z, int color) {
	}

	private record Outline(BlockPos pos, AABB bounds, int rgb) {
	}
}
