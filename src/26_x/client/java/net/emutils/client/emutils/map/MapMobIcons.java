package net.emutils.client.emutils.map;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * Mob faces for the entity radar (#224), like Xaero's radar icons. Each mob's model says where the front of
 * its head is in its texture, and the renderer says which texture the mob wears right now, so the face is
 * drawn straight from the mob's own texture: its variant (a cold pig, a red mooshroom), modded mobs whose model
 * has a head part, and resource packs, with nothing to keep a list of. A model without a head part shows the
 * front of its biggest box, which is the slime's or the ghast's face; a mob the game doesn't draw with a model,
 * or that fails to give its texture, keeps its dot.
 */
final class MapMobIcons {
	/** UV coordinates are fractions of the texture; blitting them as parts of a texture this wide keeps them exact. */
	private static final int UV_SCALE = 1 << 14;
	/** A mob's texture is looked up again after this long, so a sheared sheep or a mob that changed variant shows it. */
	private static final long TEXTURE_MILLIS = 2_000L;
	private static final int MAX_CACHED = 512;

	/**
	 * Where a face is in a texture, as fractions, its shape and size on the model, and the overlay in front of
	 * it (a hat), if its model has one.
	 */
	private record Face(float u0, float v0, float u1, float v1, float aspect, float area, @Nullable Face hat) {
		float width() {
			return u1 - u0;
		}

		float height() {
			return v1 - v0;
		}
	}

	/** A mob's face as last looked up, until {@code until}. */
	private record Icon(@Nullable Identifier texture, @Nullable Face face, long until) {
	}

	/** By model, which every mob drawn with it shares; let go when resource packs make new ones. */
	private static final Map<Model<?>, Optional<Face>> FACES = new WeakHashMap<>();
	private static final Map<Integer, Icon> ICONS = new HashMap<>();

	private MapMobIcons() {
	}

	/**
	 * Draws a mob's face centered on the current origin, fitting a box {@code size} pixels wide with a border
	 * of {@code border}'s color, and returns true; false when the mob has no face to draw, for its dot instead.
	 */
	static boolean draw(GuiGraphicsExtractor context, Entity entity, int size, int border, int color) {
		Icon icon = icon(entity);
		if (icon.texture() == null || icon.face() == null) {
			return false;
		}
		Face face = icon.face();
		// Keeps the face's shape, like a villager's long one, inside the box.
		float aspect = face.aspect();
		int width = aspect >= 1.0F ? size : Math.max(2, Math.round(size * aspect));
		int height = aspect >= 1.0F ? Math.max(2, Math.round(size / aspect)) : size;
		int left = -width / 2;
		int top = -height / 2;
		context.fill(left - 1, top - 1, left + width + 1, top + height + 1, border);
		blit(context, icon.texture(), face, left, top, width, height, color);
		if (face.hat() != null) {
			blit(context, icon.texture(), face.hat(), left, top, width, height, color);
		}
		return true;
	}

	/** For UI snapshot checks: the entities among {@code entities} that have no face to show, by type. */
	static java.util.List<String> facelessForSnapshot(Iterable<Entity> entities) {
		java.util.List<String> faceless = new java.util.ArrayList<>();
		for (Entity entity : entities) {
			Icon icon = icon(entity);
			if (icon.texture() == null || icon.face() == null) {
				faceless.add(entity.getType().toShortString());
			}
		}
		return faceless;
	}

	private static void blit(GuiGraphicsExtractor context, Identifier texture, Face face, int x, int y, int width, int height, int color) {
		context.blit(
			RenderPipelines.GUI_TEXTURED, texture, x, y,
			face.u0() * UV_SCALE, face.v0() * UV_SCALE, width, height,
			Math.round(face.width() * UV_SCALE), Math.round(face.height() * UV_SCALE),
			UV_SCALE, UV_SCALE, color
		);
	}

	private static Icon icon(Entity entity) {
		long now = System.currentTimeMillis();
		Icon cached = ICONS.get(entity.getId());
		if (cached != null && now < cached.until()) {
			return cached;
		}
		if (ICONS.size() > MAX_CACHED) {
			ICONS.values().removeIf(icon -> now >= icon.until());
		}
		Icon icon = lookUp(entity, now + TEXTURE_MILLIS);
		ICONS.put(entity.getId(), icon);
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
			Face face = FACES.computeIfAbsent(model, MapMobIcons::faceOf).orElse(null);
			if (face == null) {
				return new Icon(null, null, until);
			}
			LivingEntityRenderState state = (LivingEntityRenderState) living.createRenderState(entity, 1.0F);
			return new Icon(living.getTextureLocation(state), face, until);
		} catch (RuntimeException exception) {
			// A modded renderer that can't do this away from its own drawing keeps the dot.
			EMUtilsClient.LOGGER.debug("EMUtils radar couldn't find the face of {}", entity, exception);
			return new Icon(null, null, until);
		}
	}

	/**
	 * The front of a model's head: the face looking north of the biggest box in its head, a part named "head"
	 * and what hangs on it (some models keep the boxes in a child, like the wolf's "real_head"), with its hat's
	 * in front of it; or the front of the model's biggest box when it has no head.
	 */
	private static Optional<Face> faceOf(Model<?> model) {
		Map<String, Face> fronts = new HashMap<>();
		model.root().visit(new PoseStack(), (pose, path, index, cube) -> {
			for (ModelPart.Polygon polygon : cube.polygons) {
				if (polygon.normal().z() > -0.9F) {
					continue;
				}
				Face face = front(polygon);
				Face known = fronts.get(path);
				if (face != null && (known == null || face.area() > known.area())) {
					fronts.put(path, face);
				}
			}
		});
		// The head nearest the root: "/head", or "/body/head" or "/head_parts/head" in models that hang it elsewhere.
		String head = null;
		for (String path : fronts.keySet()) {
			String root = headRoot(path);
			if (root != null && (head == null || root.length() < head.length())) {
				head = root;
			}
		}
		Face face = null;
		Face hat = null;
		for (Map.Entry<String, Face> entry : fronts.entrySet()) {
			String path = entry.getKey();
			boolean inHead = head == null || path.equals(head) || path.startsWith(head + "/");
			if (!inHead) {
				continue;
			}
			Face front = entry.getValue();
			if (head != null && path.equals(head + "/hat")) {
				hat = hat == null || front.area() > hat.area() ? front : hat;
			} else if (!path.contains("/hat/") && !path.endsWith("/hat") && (face == null || front.area() > face.area())) {
				// Not what hangs on a hat either, like a villager's wide brim.
				face = front;
			}
		}
		if (face == null) {
			return Optional.empty();
		}
		return Optional.of(hat == null ? face : new Face(face.u0(), face.v0(), face.u1(), face.v1(), face.aspect(), face.area(), hat));
	}

	/** The path of the part named "head" a part is in, or that it is, or null. */
	private static @Nullable String headRoot(String path) {
		int at = path.indexOf("/head");
		while (at >= 0) {
			int end = at + "/head".length();
			if (end == path.length() || path.charAt(end) == '/') {
				return path.substring(0, end);
			}
			at = path.indexOf("/head", end);
		}
		return null;
	}

	/**
	 * Where a box's front is in the texture, and its shape from the box itself, as a texture needn't be square
	 * (the creeper's is twice as wide as it's tall); null when it has no size.
	 */
	private static @Nullable Face front(ModelPart.Polygon polygon) {
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
		float width = x1 - x0;
		float height = y1 - y0;
		if (u1 <= u0 || v1 <= v0 || width <= 0.0F || height <= 0.0F) {
			return null;
		}
		return new Face(u0, v0, u1, v1, width / height, width * height, null);
	}
}
