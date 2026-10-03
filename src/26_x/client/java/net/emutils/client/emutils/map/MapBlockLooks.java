package net.emutils.client.emutils.map;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceArray;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.mixin.SpriteContentsAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * The map's picture of every block state (#212), made from the block's model the first time the map meets
 * it. The top is drawn from the model's upward faces, so slabs, stairs, fences and torches show their real
 * shape from above; blocks without upward faces, like flowers and grass, show their texture instead.
 *
 * <p>Looks are made on the client thread, which owns the models, and read from the tile baker's thread.
 * A resource reload starts a new set, so the map follows the resource pack.
 */
public final class MapBlockLooks {
	/** Blocks whose top has fewer see-through pixels than this, like leaves, have the holes filled in. */
	private static final float FILL_HOLES_BELOW = 0.5F;
	/** How much darker the filled-in holes are than the rest of the top, so leaves keep some depth. */
	private static final float HOLE_SHADE = 0.72F;

	private static volatile @Nullable Generation current;
	/** States the baker met without a look, made on the client thread at the next tick. */
	private static final Set<Integer> REQUESTED = ConcurrentHashMap.newKeySet();

	private MapBlockLooks() {
	}

	private record Generation(BlockStateModelSet models, AtomicReferenceArray<MapBlockLook> looks, int fingerprint) {
	}

	/** Overviews saved by earlier builds could be unfinished without saying so; those are redrawn once. */
	private static final int OVERVIEW_REVISION = 2;

	/** Common blocks whose colors go into the fingerprint, so a pack changed under the same name is noticed too. */
	private static final Block[] FINGERPRINT_BLOCKS = {
		Blocks.GRASS_BLOCK, Blocks.STONE, Blocks.DIRT, Blocks.SAND, Blocks.WATER, Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES,
		Blocks.OAK_LOG, Blocks.SNOW_BLOCK, Blocks.GRAVEL, Blocks.DEEPSLATE, Blocks.NETHERRACK, Blocks.END_STONE, Blocks.SHORT_GRASS
	};

	/**
	 * Which resource packs the looks come from. Saved overviews remember it, so they are redrawn after a
	 * pack change. Safe from any thread.
	 */
	public static int fingerprint() {
		Generation generation = current;
		return generation == null ? 0 : generation.fingerprint();
	}

	/** Asks for a state's look to be made, for a state the map read from disk. Safe from any thread. */
	static void request(int stateId) {
		REQUESTED.add(stateId);
	}

	/** Makes the looks asked for since the last call; returns true when there were any. Client thread only. */
	static boolean makeRequested() {
		if (REQUESTED.isEmpty()) {
			return false;
		}
		Integer[] ids = REQUESTED.toArray(new Integer[0]);
		for (Integer id : ids) {
			REQUESTED.remove(id);
			ensure(Block.stateById(id), id);
		}
		return true;
	}

	/** The look of a block state id, or null when the map hasn't met that state yet. Safe from any thread. */
	public static @Nullable MapBlockLook get(int stateId) {
		Generation generation = current;
		if (generation == null || stateId < 0 || stateId >= generation.looks().length()) {
			return null;
		}
		return generation.looks().get(stateId);
	}

	/**
	 * Starts a new set of looks when the models were reloaded, which happens on every resource pack change.
	 * Returns true when it did, so the map redraws. Client thread only.
	 */
	public static boolean refreshIfReloaded(Minecraft client) {
		BlockStateModelSet models = client.getModelManager().getBlockStateModelSet();
		Generation generation = current;
		if (generation != null && generation.models() == models) {
			return false;
		}
		AtomicReferenceArray<MapBlockLook> looks = new AtomicReferenceArray<>(Block.BLOCK_STATE_REGISTRY.size());
		current = new Generation(models, looks, 0);
		int fingerprint = client.getResourcePackRepository().getSelectedIds().hashCode() * 31 + Block.BLOCK_STATE_REGISTRY.size();
		for (Block block : FINGERPRINT_BLOCKS) {
			BlockState state = block.defaultBlockState();
			fingerprint = fingerprint * 31 + ensure(state, Block.getId(state)).top().average();
		}
		// Raised when overviews are drawn differently, or were saved wrong, so the saved ones are redrawn.
		fingerprint = fingerprint * 31 + OVERVIEW_REVISION;
		current = new Generation(models, looks, fingerprint == 0 ? 1 : fingerprint);
		return generation != null;
	}

	/** The look of a block state, made now if the map hasn't met it yet. Client thread only. */
	public static MapBlockLook ensure(BlockState state, int stateId) {
		Generation generation = current;
		if (generation == null || stateId < 0 || stateId >= generation.looks().length()) {
			return MapBlockLook.INVISIBLE;
		}
		MapBlockLook look = generation.looks().get(stateId);
		if (look == null) {
			try {
				look = build(state);
			} catch (RuntimeException exception) {
				EMUtilsClient.LOGGER.warn("EMUtils map couldn't read the look of {}", state, exception);
				look = MapBlockLook.INVISIBLE;
			}
			generation.looks().set(stateId, look);
		}
		return look;
	}

	private static MapBlockLook build(BlockState state) {
		if (state.isAir()) {
			return MapBlockLook.INVISIBLE;
		}
		Minecraft client = Minecraft.getInstance();
		ModelManager models = client.getModelManager();
		if (state.getBlock() instanceof LiquidBlock && !state.getFluidState().isEmpty()) {
			FluidModel fluid = models.getFluidStateModelSet().get(state.getFluidState());
			BlockTintSource tint = fluid.tintSource();
			Raster raster = new Raster();
			raster.fill(fluid.stillMaterial().sprite(), tint != null);
			return new MapBlockLook(MapBlockLook.Kind.FLUID, raster.layer(), null, tint, true);
		}
		if (state.getRenderShape() == RenderShape.INVISIBLE) {
			return MapBlockLook.INVISIBLE;
		}

		BlockStateModel model = models.getBlockStateModelSet().get(state);
		List<BlockStateModelPart> parts = new ArrayList<>();
		model.collectParts(RandomSource.create(42L), parts);
		List<BakedQuad> quads = new ArrayList<>();
		for (BlockStateModelPart part : parts) {
			quads.addAll(part.getQuads(null));
			for (Direction direction : Direction.values()) {
				quads.addAll(part.getQuads(direction));
			}
		}

		Raster top = new Raster();
		Raster side = new Raster();
		int tintIndex = -1;
		boolean anyUp = false;
		boolean anySide = false;
		for (BakedQuad quad : quads) {
			if (quad.direction() == Direction.UP) {
				anyUp |= top.drawUp(quad);
				tintIndex = Math.max(tintIndex, quad.materialInfo().tintIndex());
			} else if (quad.direction() == Direction.SOUTH) {
				anySide |= side.drawSouth(quad);
			}
		}
		if (!anyUp) {
			// Flowers, grass and torches have no upward faces: their texture is the clearest way to show them.
			TextureAtlasSprite sprite = model.particleMaterial().sprite();
			if (!quads.isEmpty()) {
				sprite = quads.getFirst().materialInfo().sprite();
				tintIndex = quads.getFirst().materialInfo().tintIndex();
			}
			top.fill(sprite, tintIndex >= 0);
		}

		boolean fullBlock = Block.isShapeFullBlock(state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
		float open = top.openFraction();
		MapBlockLook.Kind kind;
		if (anyUp && open == 0.0F) {
			kind = MapBlockLook.Kind.OPAQUE;
		} else if (anyUp && fullBlock && open < FILL_HOLES_BELOW) {
			top.fillHoles();
			kind = MapBlockLook.Kind.OPAQUE;
		} else {
			kind = MapBlockLook.Kind.SEE_THROUGH;
		}
		BlockTintSource tint = tintIndex >= 0 ? client.getBlockColors().getTintSource(state, tintIndex) : null;
		boolean solid = fullBlock || (anyUp && open == 0.0F);
		MapBlockLook.Part part = state.is(BlockTags.LEAVES) ? MapBlockLook.Part.CANOPY
			: state.is(BlockTags.LOGS) ? MapBlockLook.Part.TRUNK
			: MapBlockLook.Part.BLOCK;
		return new MapBlockLook(kind, top.layer(), anySide ? side.layer() : null, tint, solid, part);
	}

	/** A 16 x 16 picture being drawn from a model's faces, keeping the highest face at each pixel. */
	private static final class Raster {
		private final int[] pixels = new int[MapChunk.AREA];
		private final boolean[] tinted = new boolean[MapChunk.AREA];
		private final float[] depth = new float[MapChunk.AREA];

		Raster() {
			Arrays.fill(depth, -Float.MAX_VALUE);
		}

		/** Draws an upward face where it lies seen from above: x across, z down. */
		boolean drawUp(BakedQuad quad) {
			return draw(quad, 0, 2, false);
		}

		/** Draws a south face as seen from the south: x across, y up. */
		boolean drawSouth(BakedQuad quad) {
			return draw(quad, 0, 1, true);
		}

		/**
		 * Draws a face that is a rectangle along two axes, {@code across} and {@code down}, mapping its texture
		 * from its four corners. With {@code flipDown} the axis grows upwards, as y does on a side face.
		 */
		private boolean draw(BakedQuad quad, int across, int down, boolean flipDown) {
			float minA = Float.MAX_VALUE;
			float maxA = -Float.MAX_VALUE;
			float minD = Float.MAX_VALUE;
			float maxD = -Float.MAX_VALUE;
			float height = -Float.MAX_VALUE;
			int depthAxis = 3 - across - down;
			for (int i = 0; i < BakedQuad.VERTEX_COUNT; i++) {
				Vector3fc position = quad.position(i);
				float a = position.get(across);
				float d = flipDown ? 1.0F - position.get(down) : position.get(down);
				minA = Math.min(minA, a);
				maxA = Math.max(maxA, a);
				minD = Math.min(minD, d);
				maxD = Math.max(maxD, d);
				height = Math.max(height, position.get(depthAxis));
			}
			if (maxA - minA < 1.0E-4F || maxD - minD < 1.0E-4F) {
				return false;
			}

			// The texture coordinates at the face's corners, by which end of each axis they sit at.
			float[] cornerU = new float[4];
			float[] cornerV = new float[4];
			for (int i = 0; i < BakedQuad.VERTEX_COUNT; i++) {
				Vector3fc position = quad.position(i);
				float a = position.get(across);
				float d = flipDown ? 1.0F - position.get(down) : position.get(down);
				int corner = (a - minA > (maxA - minA) / 2.0F ? 1 : 0) | (d - minD > (maxD - minD) / 2.0F ? 2 : 0);
				long uv = quad.packedUV(i);
				cornerU[corner] = UVPair.unpackU(uv);
				cornerV[corner] = UVPair.unpackV(uv);
			}

			TextureAtlasSprite sprite = quad.materialInfo().sprite();
			Frame frame = Frame.of(sprite);
			if (frame == null) {
				return false;
			}
			boolean tint = quad.materialInfo().tintIndex() >= 0;
			boolean drew = false;
			for (int y = 0; y < 16; y++) {
				float d = (y + 0.5F) / 16.0F;
				if (d < minD || d > maxD) {
					continue;
				}
				float fd = (d - minD) / (maxD - minD);
				for (int x = 0; x < 16; x++) {
					float a = (x + 0.5F) / 16.0F;
					if (a < minA || a > maxA) {
						continue;
					}
					int index = y * 16 + x;
					if (height < depth[index]) {
						continue;
					}
					float fa = (a - minA) / (maxA - minA);
					float u = lerp(lerp(cornerU[0], cornerU[1], fa), lerp(cornerU[2], cornerU[3], fa), fd);
					float v = lerp(lerp(cornerV[0], cornerV[1], fa), lerp(cornerV[2], cornerV[3], fa), fd);
					int pixel = frame.atUv(u, v);
					if ((pixel >>> 24) == 0) {
						continue;
					}
					pixels[index] = over(pixels[index], pixel);
					tinted[index] = tint;
					depth[index] = height;
					drew = true;
				}
			}
			return drew;
		}

		/** Fills the whole picture with a sprite's first frame. */
		void fill(TextureAtlasSprite sprite, boolean tint) {
			Frame frame = Frame.of(sprite);
			if (frame == null) {
				return;
			}
			for (int y = 0; y < 16; y++) {
				for (int x = 0; x < 16; x++) {
					int index = y * 16 + x;
					pixels[index] = frame.at(x, y);
					tinted[index] = tint && (pixels[index] >>> 24) != 0;
				}
			}
		}

		float openFraction() {
			int open = 0;
			for (int pixel : pixels) {
				if ((pixel >>> 24) < 128) {
					open++;
				}
			}
			return open / (float) MapChunk.AREA;
		}

		/** Closes the holes of a cut-out top, like leaves, with a darker average of its own pixels. */
		void fillHoles() {
			long r = 0;
			long g = 0;
			long b = 0;
			int count = 0;
			boolean tintedCovered = false;
			for (int i = 0; i < MapChunk.AREA; i++) {
				int pixel = pixels[i];
				if ((pixel >>> 24) >= 128) {
					r += (pixel >> 16) & 0xFF;
					g += (pixel >> 8) & 0xFF;
					b += pixel & 0xFF;
					count++;
					tintedCovered |= tinted[i];
				}
			}
			if (count == 0) {
				return;
			}
			int hole = 0xFF000000
				| Math.round(r / (float) count * HOLE_SHADE) << 16
				| Math.round(g / (float) count * HOLE_SHADE) << 8
				| Math.round(b / (float) count * HOLE_SHADE);
			for (int i = 0; i < MapChunk.AREA; i++) {
				if ((pixels[i] >>> 24) < 128) {
					pixels[i] = hole;
					tinted[i] = tintedCovered;
				} else {
					pixels[i] |= 0xFF000000;
				}
			}
		}

		MapBlockLook.Layer layer() {
			return new MapBlockLook.Layer(pixels, tinted);
		}

		private static float lerp(float from, float to, float t) {
			return from + (to - from) * t;
		}

		/** {@code top} drawn over {@code bottom}, both ARGB with straight alpha. */
		private static int over(int bottom, int top) {
			int topAlpha = top >>> 24;
			if (topAlpha == 255 || (bottom >>> 24) == 0) {
				return top;
			}
			int bottomAlpha = bottom >>> 24;
			int alpha = topAlpha + bottomAlpha * (255 - topAlpha) / 255;
			if (alpha == 0) {
				return 0;
			}
			int r = (((top >> 16) & 0xFF) * topAlpha + ((bottom >> 16) & 0xFF) * bottomAlpha * (255 - topAlpha) / 255) / alpha;
			int g = (((top >> 8) & 0xFF) * topAlpha + ((bottom >> 8) & 0xFF) * bottomAlpha * (255 - topAlpha) / 255) / alpha;
			int b = ((top & 0xFF) * topAlpha + (bottom & 0xFF) * bottomAlpha * (255 - topAlpha) / 255) / alpha;
			return alpha << 24 | r << 16 | g << 8 | b;
		}
	}

	/** The first animation frame of a sprite, read from the image it was loaded from. */
	private record Frame(TextureAtlasSprite sprite, NativeImage image, int width, int height) {
		static @Nullable Frame of(TextureAtlasSprite sprite) {
			SpriteContents contents = sprite.contents();
			NativeImage image = ((SpriteContentsAccessor) contents).emutils$originalImage();
			if (image == null || contents.width() <= 0 || contents.height() <= 0) {
				return null;
			}
			return new Frame(sprite, image, contents.width(), contents.height());
		}

		/** The pixel at a 16 x 16 grid position, whatever the texture's own resolution. */
		int at(int x, int y) {
			return image.getPixel(x * width / 16, y * height / 16);
		}

		/** The pixel at atlas texture coordinates inside this sprite. */
		int atUv(float u, float v) {
			float spanU = sprite.getU1() - sprite.getU0();
			float spanV = sprite.getV1() - sprite.getV0();
			int x = spanU == 0.0F ? 0 : (int) Math.floor((u - sprite.getU0()) / spanU * width);
			int y = spanV == 0.0F ? 0 : (int) Math.floor((v - sprite.getV0()) / spanV * height);
			return image.getPixel(Math.clamp(x, 0, width - 1), Math.clamp(y, 0, height - 1));
		}
	}
}
