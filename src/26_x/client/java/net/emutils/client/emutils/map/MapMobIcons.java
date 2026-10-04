package net.emutils.client.emutils.map;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jspecify.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Mob faces for the entity radar (#224), like Xaero's radar icons, drawn from the mob's own texture, so its
 * variant (a cold pig, a red mooshroom), resource packs and modded mobs show, with nothing to keep a list of.
 *
 * <p>Most mobs show the front of their head: the model says where it is in the texture. A few whose head
 * that doesn't do justice get a view of their model instead ({@link #TUNED}): from the front where the face
 * is made of several boxes (a slime's eyes, a horse's muzzle), from the side for fish, and from above for
 * flat crawlers and the phantom. Such a view lays the faces of the model's boxes turned toward you over each
 * other, farthest first, like a flat drawing of the model. A mob the game doesn't draw with a model, or that
 * fails to give its texture, keeps its dot.
 */
final class MapMobIcons {
	/** UV coordinates are fractions of the texture; blitting them as parts of a texture this wide keeps them exact. */
	private static final int UV_SCALE = 1 << 14;
	/** Pieces are placed to a quarter of a model pixel. */
	private static final int SUBPIXELS = 4;
	/** A mob's texture is looked up again after this long, so a sheared sheep or a mob that changed variant shows it. */
	private static final long TEXTURE_MILLIS = 2_000L;
	private static final int MAX_CACHED = 512;
	private static final int BACKING = 0xFF1B1D21;

	/** Which way a tuned mob is looked at: toward its face, at its left side, or from above. */
	private enum View {
		FRONT,
		SIDE,
		TOP
	}

	/**
	 * A tuned mob's view: of the parts of its model with these names and what hangs on them, or all of it; and
	 * with {@code topSquare}, only a square from its top, for heads on a long neck that's one box with them.
	 */
	private record Tuning(View view, @Nullable Set<String> parts, boolean topSquare) {
		static Tuning of(View view) {
			return new Tuning(view, null, false);
		}

		static Tuning parts(View view, String... parts) {
			return new Tuning(view, Set.of(parts), false);
		}

		static Tuning top(View view, String... parts) {
			return new Tuning(view, Set.of(parts), true);
		}

		/** Whether a part, by its path in the model, is in the view. */
		boolean includes(String path) {
			if (parts == null) {
				return true;
			}
			for (String segment : path.split("/")) {
				if (parts.contains(segment)) {
					return true;
				}
			}
			return false;
		}
	}

	/** The mobs whose head alone doesn't look like them, by id, and how they're looked at instead. */
	private static final Map<String, Tuning> TUNED = Map.ofEntries(
		// Faces made of several boxes: eyes and a mouth inside a slime, a muzzle in front of the head.
		Map.entry("minecraft:slime", Tuning.of(View.FRONT)),
		Map.entry("minecraft:magma_cube", Tuning.of(View.FRONT)),
		Map.entry("minecraft:horse", Tuning.parts(View.FRONT, "head", "upper_mouth")),
		Map.entry("minecraft:donkey", Tuning.parts(View.FRONT, "head", "upper_mouth")),
		Map.entry("minecraft:mule", Tuning.parts(View.FRONT, "head", "upper_mouth")),
		Map.entry("minecraft:skeleton_horse", Tuning.parts(View.FRONT, "head", "upper_mouth")),
		Map.entry("minecraft:zombie_horse", Tuning.parts(View.FRONT, "head", "upper_mouth")),
		Map.entry("minecraft:hoglin", Tuning.parts(View.FRONT, "head")),
		Map.entry("minecraft:zoglin", Tuning.parts(View.FRONT, "head")),
		Map.entry("minecraft:sniffer", Tuning.parts(View.FRONT, "head")),
		Map.entry("minecraft:frog", Tuning.parts(View.FRONT, "head")),
		Map.entry("minecraft:copper_golem", Tuning.parts(View.FRONT, "head")),
		// Heads on a long neck that's one box with them.
		Map.entry("minecraft:llama", Tuning.top(View.FRONT, "head")),
		Map.entry("minecraft:trader_llama", Tuning.top(View.FRONT, "head")),
		Map.entry("minecraft:camel", Tuning.top(View.FRONT, "head")),
		Map.entry("minecraft:camel_husk", Tuning.top(View.FRONT, "head")),
		Map.entry("minecraft:creaking", Tuning.top(View.FRONT, "head")),
		// Long and flat: fish and the parrot from the side...
		Map.entry("minecraft:cod", Tuning.of(View.SIDE)),
		Map.entry("minecraft:salmon", Tuning.of(View.SIDE)),
		Map.entry("minecraft:tropical_fish", Tuning.of(View.SIDE)),
		Map.entry("minecraft:tadpole", Tuning.of(View.SIDE)),
		Map.entry("minecraft:parrot", Tuning.of(View.SIDE)),
		Map.entry("minecraft:dolphin", Tuning.of(View.SIDE)),
		Map.entry("minecraft:nautilus", Tuning.of(View.SIDE)),
		Map.entry("minecraft:zombie_nautilus", Tuning.of(View.SIDE)),
		// ...and crawlers, flyers and the armadillo's shell from above, as the map sees them.
		Map.entry("minecraft:armadillo", Tuning.of(View.TOP)),
		Map.entry("minecraft:bat", Tuning.of(View.TOP)),
		Map.entry("minecraft:silverfish", Tuning.of(View.TOP)),
		Map.entry("minecraft:endermite", Tuning.of(View.TOP)),
		Map.entry("minecraft:phantom", Tuning.of(View.TOP))
	);

	/** A piece of an icon: where it goes, in model pixels, where it comes from in the texture, and how far back it is. */
	private record Piece(float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1, float depth) {
	}

	/** An icon: its pieces, farthest first, and the box they fill. */
	private record Shape(List<Piece> pieces, float minX, float minY, float maxX, float maxY) {
		float width() {
			return maxX - minX;
		}

		float height() {
			return maxY - minY;
		}
	}

	/** A mob's icon as last looked up, until {@code until}. */
	private record Icon(@Nullable Identifier texture, @Nullable Shape shape, long until) {
	}

	/** By model, which every mob drawn with it shares; let go when resource packs make new ones. */
	private static final Map<Model<?>, Optional<Shape>> SHAPES = new WeakHashMap<>();
	/** By the entity's UUID, which, unlike its number, isn't used again for another entity in the next world. */
	private static final Map<java.util.UUID, Icon> ICONS = new HashMap<>();

	private MapMobIcons() {
	}

	/**
	 * Draws a mob's icon centered on the current origin, fitting a box {@code size} pixels wide, on a dark
	 * backing with a border of {@code border}'s color, and returns true; false when the mob has no icon to
	 * draw, for its dot instead.
	 */
	static boolean draw(GuiGraphicsExtractor context, Entity entity, int size, int border, int color) {
		Icon icon = icon(entity);
		Shape shape = icon.shape();
		if (icon.texture() == null || shape == null) {
			return false;
		}
		// Keeps the icon's shape, like a villager's long face, inside the box.
		float scale = size / Math.max(shape.width(), shape.height());
		int width = Math.max(2, Math.round(shape.width() * scale));
		int height = Math.max(2, Math.round(shape.height() * scale));
		int left = -width / 2;
		int top = -height / 2;
		context.fill(left - 1, top - 1, left + width + 1, top + height + 1, border);
		// Dark where the texture is clear, like the gaps in a skeleton horse's skull.
		context.fill(left, top, left + width, top + height, color & 0xFF000000 | BACKING & 0x00FFFFFF);
		context.pose().pushMatrix();
		context.pose().translate(left, top);
		context.pose().scale(width / (shape.width() * SUBPIXELS), height / (shape.height() * SUBPIXELS));
		for (Piece piece : shape.pieces()) {
			int x0 = Math.round((piece.x0() - shape.minX()) * SUBPIXELS);
			int y0 = Math.round((piece.y0() - shape.minY()) * SUBPIXELS);
			int x1 = Math.round((piece.x1() - shape.minX()) * SUBPIXELS);
			int y1 = Math.round((piece.y1() - shape.minY()) * SUBPIXELS);
			if (x1 <= x0 || y1 <= y0) {
				continue;
			}
			context.blit(
				RenderPipelines.GUI_TEXTURED, icon.texture(), x0, y0,
				piece.u0() * UV_SCALE, piece.v0() * UV_SCALE, x1 - x0, y1 - y0,
				Math.round((piece.u1() - piece.u0()) * UV_SCALE), Math.round((piece.v1() - piece.v0()) * UV_SCALE),
				UV_SCALE, UV_SCALE, color
			);
		}
		context.pose().popMatrix();
		return true;
	}

	/** For UI snapshot checks: the entities among {@code entities} that have no icon to show, by type. */
	static List<String> facelessForSnapshot(Iterable<Entity> entities) {
		List<String> faceless = new ArrayList<>();
		for (Entity entity : entities) {
			Icon icon = icon(entity);
			if (icon.texture() == null || icon.shape() == null) {
				faceless.add(entity.getType().toShortString());
			}
		}
		return faceless;
	}

	private static Icon icon(Entity entity) {
		long now = System.currentTimeMillis();
		Icon cached = ICONS.get(entity.getUUID());
		if (cached != null && now < cached.until()) {
			return cached;
		}
		if (ICONS.size() > MAX_CACHED) {
			ICONS.values().removeIf(icon -> now >= icon.until());
		}
		Icon icon = lookUp(entity, now + TEXTURE_MILLIS);
		ICONS.put(entity.getUUID(), icon);
		return icon;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static Icon lookUp(Entity entity, long until) {
		try {
			EntityRenderer<?, ?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
			if (!(renderer instanceof LivingEntityRenderer living)) {
				return new Icon(null, null, until);
			}
			Model<?> model = living.getModel();
			EntityType<?> type = entity.getType();
			Shape shape = SHAPES.computeIfAbsent(model, key -> shapeOf(key, type)).orElse(null);
			if (shape == null) {
				return new Icon(null, null, until);
			}
			LivingEntityRenderState state = (LivingEntityRenderState) living.createRenderState(entity, 1.0F);
			return new Icon(living.getTextureLocation(state), shape, until);
		} catch (RuntimeException exception) {
			// A modded renderer that can't do this away from its own drawing keeps the dot.
			EMUtilsClient.LOGGER.debug("EMUtils radar couldn't find the face of {}", entity, exception);
			return new Icon(null, null, until);
		}
	}

	private static Optional<Shape> shapeOf(Model<?> model, EntityType<?> type) {
		Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
		Tuning tuning = id == null ? null : TUNED.get(id.toString());
		if (tuning != null) {
			Optional<Shape> view = viewOf(model, tuning);
			if (view.isPresent()) {
				return view;
			}
		}
		return faceOf(model);
	}

	/**
	 * A tuned mob's view of its model ({@link #TUNED}), in its resting pose: of each box, the faces turned
	 * toward you, placed where they are seen, farthest first.
	 */
	private static Optional<Shape> viewOf(Model<?> model, Tuning tuning) {
		// The model's parts keep the pose of the last mob drawn with it; the view is of its resting pose, which
		// drawing the next mob starts from anyway.
		for (ModelPart part : model.root().getAllParts()) {
			part.resetPose();
		}
		List<Piece> pieces = new ArrayList<>();
		model.root().visit(new PoseStack(), (pose, path, index, cube) -> {
			if (!tuning.includes(path)) {
				return;
			}
			// The box's middle, how far back it is, to tell the faces turned toward you.
			float center = 0.0F;
			int count = 0;
			for (ModelPart.Polygon polygon : cube.polygons) {
				for (ModelPart.Vertex vertex : polygon.vertices()) {
					center += view(pose.pose().transformPosition(vertex.worldX(), vertex.worldY(), vertex.worldZ(), new Vector3f()), tuning.view())[2];
					count++;
				}
			}
			center /= Math.max(1, count);
			for (ModelPart.Polygon polygon : cube.polygons) {
				ModelPart.Vertex[] vertices = polygon.vertices();
				float[][] seen = new float[vertices.length][];
				float depth = 0.0F;
				for (int i = 0; i < vertices.length; i++) {
					ModelPart.Vertex vertex = vertices[i];
					seen[i] = view(pose.pose().transformPosition(vertex.worldX(), vertex.worldY(), vertex.worldZ(), new Vector3f()), tuning.view());
					depth += seen[i][2];
				}
				depth /= vertices.length;
				// Only the faces turned toward you: nearer than the box's middle.
				if (depth > center - 1.0E-3F) {
					continue;
				}
				Piece piece = piece(vertices, seen, depth);
				if (piece != null) {
					pieces.add(piece);
				}
			}
		});
		if (pieces.isEmpty()) {
			return Optional.empty();
		}
		pieces.sort(Comparator.comparingDouble(Piece::depth).reversed());
		Shape shape = bounds(pieces);
		return Optional.of(tuning.topSquare() && shape.height() > shape.width() ? topSquare(shape) : shape);
	}

	/** Only a square from the top of an icon; pieces across its bottom are cut, with their texture. */
	private static Shape topSquare(Shape shape) {
		float bottom = shape.minY() + shape.width();
		List<Piece> kept = new ArrayList<>();
		for (Piece piece : shape.pieces()) {
			if (piece.y0() >= bottom) {
				continue;
			}
			if (piece.y1() <= bottom) {
				kept.add(piece);
				continue;
			}
			float share = (bottom - piece.y0()) / (piece.y1() - piece.y0());
			kept.add(new Piece(piece.x0(), piece.y0(), piece.x1(), bottom, piece.u0(), piece.v0(), piece.u1(), piece.v0() + (piece.v1() - piece.v0()) * share, piece.depth()));
		}
		return new Shape(List.copyOf(kept), shape.minX(), shape.minY(), shape.maxX(), bottom);
	}

	/** Where a point of the model is seen in a view, in model pixels: across, down, and how far from you. */
	private static float[] view(Vector3f at, View view) {
		float x = at.x() * 16.0F;
		float y = at.y() * 16.0F;
		float z = at.z() * 16.0F;
		return switch (view) {
			// Models face north, toward -z, with y down.
			case FRONT -> new float[] {x, y, z};
			case SIDE -> new float[] {z, y, x};
			case TOP -> new float[] {x, z, y};
		};
	}

	/**
	 * A face as a piece of the icon: the rectangle it covers, with the texture laid the way it's seen, which
	 * may be mirrored. Null when it's seen edge on.
	 */
	private static @Nullable Piece piece(ModelPart.Vertex[] vertices, float[][] seen, float depth) {
		float x0 = Float.MAX_VALUE;
		float y0 = Float.MAX_VALUE;
		float x1 = -Float.MAX_VALUE;
		float y1 = -Float.MAX_VALUE;
		for (float[] point : seen) {
			x0 = Math.min(x0, point[0]);
			y0 = Math.min(y0, point[1]);
			x1 = Math.max(x1, point[0]);
			y1 = Math.max(y1, point[1]);
		}
		if (x1 - x0 < 1.0E-3F || y1 - y0 < 1.0E-3F) {
			return null;
		}
		// The texture at the corner seen top left, and at the one seen bottom right.
		int topLeft = 0;
		int bottomRight = 0;
		for (int i = 1; i < seen.length; i++) {
			if (seen[i][0] + seen[i][1] < seen[topLeft][0] + seen[topLeft][1]) {
				topLeft = i;
			}
			if (seen[i][0] + seen[i][1] > seen[bottomRight][0] + seen[bottomRight][1]) {
				bottomRight = i;
			}
		}
		return new Piece(x0, y0, x1, y1, vertices[topLeft].u(), vertices[topLeft].v(), vertices[bottomRight].u(), vertices[bottomRight].v(), depth);
	}

	private static Shape bounds(List<Piece> pieces) {
		float minX = Float.MAX_VALUE;
		float minY = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE;
		float maxY = -Float.MAX_VALUE;
		for (Piece piece : pieces) {
			minX = Math.min(minX, piece.x0());
			minY = Math.min(minY, piece.y0());
			maxX = Math.max(maxX, piece.x1());
			maxY = Math.max(maxY, piece.y1());
		}
		return new Shape(List.copyOf(pieces), minX, minY, maxX, maxY);
	}

	/**
	 * The front of a model's head: the face looking north of the biggest box in its head, a part named "head"
	 * and what hangs on it (some models keep the boxes in a child, like the wolf's "real_head"), with its hat's
	 * in front of it; or the front of the model's biggest box when it has no head.
	 */
	private static Optional<Shape> faceOf(Model<?> model) {
		Map<String, Piece> fronts = new HashMap<>();
		model.root().visit(new PoseStack(), (pose, path, index, cube) -> {
			for (ModelPart.Polygon polygon : cube.polygons) {
				if (polygon.normal().z() > -0.9F) {
					continue;
				}
				Piece face = front(polygon);
				Piece known = fronts.get(path);
				if (face != null && (known == null || area(face) > area(known))) {
					fronts.put(path, face);
				}
			}
		});
		// The head nearest the root: "/head", or "/body/head" in models that hang it elsewhere.
		String head = null;
		for (String path : fronts.keySet()) {
			String root = branchRoot(path, "head");
			if (root != null && (head == null || root.length() < head.length())) {
				head = root;
			}
		}
		Piece face = null;
		Piece hat = null;
		for (Map.Entry<String, Piece> entry : fronts.entrySet()) {
			String path = entry.getKey();
			boolean inHead = head == null || path.equals(head) || path.startsWith(head + "/");
			if (!inHead) {
				continue;
			}
			Piece front = entry.getValue();
			if (head != null && path.equals(head + "/hat")) {
				hat = hat == null || area(front) > area(hat) ? front : hat;
			} else if (!path.contains("/hat/") && !path.endsWith("/hat") && (face == null || area(front) > area(face))) {
				// Not what hangs on a hat either, like a villager's wide brim.
				face = front;
			}
		}
		if (face == null) {
			return Optional.empty();
		}
		// The hat covers the face exactly; its box is only a little bigger.
		List<Piece> pieces = new ArrayList<>();
		pieces.add(face);
		if (hat != null) {
			pieces.add(new Piece(face.x0(), face.y0(), face.x1(), face.y1(), hat.u0(), hat.v0(), hat.u1(), hat.v1(), face.depth() - 1.0F));
		}
		return Optional.of(new Shape(pieces, face.x0(), face.y0(), face.x1(), face.y1()));
	}

	private static float area(Piece piece) {
		return (piece.x1() - piece.x0()) * (piece.y1() - piece.y0());
	}

	/** The path of the part named {@code name} a part is in, or that it is, or null. */
	private static @Nullable String branchRoot(String path, String name) {
		String segment = "/" + name;
		int at = path.indexOf(segment);
		while (at >= 0) {
			int end = at + segment.length();
			if (end == path.length() || path.charAt(end) == '/') {
				return path.substring(0, end);
			}
			at = path.indexOf(segment, end);
		}
		return null;
	}

	/**
	 * A box's front as the simple face: its place in the texture, and its shape from the box itself, as a
	 * texture needn't be square (the creeper's is twice as wide as it's tall); null when it has no size.
	 */
	private static @Nullable Piece front(ModelPart.Polygon polygon) {
		float u0 = Float.MAX_VALUE;
		float v0 = Float.MAX_VALUE;
		float u1 = -Float.MAX_VALUE;
		float v1 = -Float.MAX_VALUE;
		float x0 = Float.MAX_VALUE;
		float y0 = Float.MAX_VALUE;
		float x1 = -Float.MAX_VALUE;
		float y1 = -Float.MAX_VALUE;
		for (ModelPart.Vertex vertex : polygon.vertices()) {
			u0 = Math.min(u0, vertex.u());
			v0 = Math.min(v0, vertex.v());
			u1 = Math.max(u1, vertex.u());
			v1 = Math.max(v1, vertex.v());
			x0 = Math.min(x0, vertex.x());
			y0 = Math.min(y0, vertex.y());
			x1 = Math.max(x1, vertex.x());
			y1 = Math.max(y1, vertex.y());
		}
		if (u1 <= u0 || v1 <= v0 || x1 <= x0 || y1 <= y0) {
			return null;
		}
		return new Piece(0.0F, 0.0F, x1 - x0, y1 - y0, u0, v0, u1, v1, 0.0F);
	}
}
