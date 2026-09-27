package net.emutils.client.emutils.capes;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Animated capes (#52), such as Cosmetica's: every frame is kept, and the cape's texture is swapped to
 * the current frame as the game ticks, so the renderer keeps using one texture per player. Only touched
 * on the client thread.
 */
public final class CapeAnimations {
	private static final Map<Identifier, Animation> ANIMATIONS = new HashMap<>();
	private static long ticks;

	private CapeAnimations() {
	}

	/** Starts animating {@code texture} through {@code frames}, replacing an earlier animation of the same texture. */
	static void start(Identifier textureId, DynamicTexture texture, List<NativeImage> frames, int ticksPerFrame) {
		stop(textureId);
		ANIMATIONS.put(textureId, new Animation(texture, frames, Math.max(1, ticksPerFrame)));
	}

	/** Stops animating {@code textureId} and frees its frames. */
	static void stop(Identifier textureId) {
		Animation old = ANIMATIONS.remove(textureId);
		if (old != null) {
			old.frames.forEach(NativeImage::close);
		}
	}

	/** Stops every animation and frees the frames, such as when the cape cache is cleared. */
	static void clear() {
		ANIMATIONS.values().forEach(animation -> animation.frames.forEach(NativeImage::close));
		ANIMATIONS.clear();
	}

	/** Steps every animated cape; call once per client tick. */
	public static void tick() {
		ticks++;
		for (Animation animation : ANIMATIONS.values()) {
			int frame = (int) ((ticks / animation.ticksPerFrame) % animation.frames.size());
			NativeImage pixels = animation.texture.getPixels();
			if (frame != animation.shown && pixels != null) {
				animation.shown = frame;
				pixels.copyFrom(animation.frames.get(frame));
				animation.texture.upload();
			}
		}
	}

	/** How many capes are animating; used by UI snapshots. */
	public static int count() {
		return ANIMATIONS.size();
	}

	private static final class Animation {
		private final DynamicTexture texture;
		private final List<NativeImage> frames;
		private final int ticksPerFrame;
		/** The frame the texture holds now; it starts with the first. */
		private int shown;

		private Animation(DynamicTexture texture, List<NativeImage> frames, int ticksPerFrame) {
			this.texture = texture;
			this.frames = frames;
			this.ticksPerFrame = ticksPerFrame;
		}
	}
}
