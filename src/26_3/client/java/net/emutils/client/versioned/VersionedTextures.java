package net.emutils.client.versioned;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import java.util.function.Supplier;
import net.minecraft.client.renderer.texture.DynamicTexture;

/** Textures that need a non-default sampler. */
public final class VersionedTextures {
	private VersionedTextures() {
	}

	/**
	 * A dynamic texture sampled with linear filtering and clamped edges, for UI shapes that are baked at
	 * the physical pixel density and then drawn at GUI coordinates. The default sampler uses nearest
	 * filtering when minifying, which turns their anti-aliased edges jagged.
	 */
	public static DynamicTexture smoothTexture(Supplier<String> label, NativeImage image) {
		return new SmoothTexture(label, image);
	}

	/**
	 * A dynamic texture for map tiles: linear filtering while shrinking, so a tile drawn smaller than its
	 * pixels doesn't shimmer as the map moves, and nearest while enlarging, so block textures stay crisp.
	 */
	public static DynamicTexture mapTexture(Supplier<String> label, NativeImage image) {
		return new MapTexture(label, image);
	}

	private static final class MapTexture extends DynamicTexture {
		private MapTexture(Supplier<String> label, NativeImage image) {
			super(label, image);
			sampler = RenderSystem.getSamplerCache().getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.NEAREST, false);
		}
	}

	private static final class SmoothTexture extends DynamicTexture {
		private SmoothTexture(Supplier<String> label, NativeImage image) {
			super(label, image);
			sampler = RenderSystem.getSamplerCache().getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, false);
		}
	}
}
