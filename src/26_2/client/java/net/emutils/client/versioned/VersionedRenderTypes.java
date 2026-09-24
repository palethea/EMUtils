package net.emutils.client.versioned;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.emutils.client.emutils.compat.IrisCompat;
import net.emutils.client.versioned.mixin.RenderPipelinesAccessor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/** Minecraft 26.2 render pipelines and render types used by EMUtils renderers. */
public final class VersionedRenderTypes {
	private VersionedRenderTypes() {
	}

	/**
	 * Translucent, unculled, textured quads for the light level overlay's digit sprites.
	 *
	 * <p>Built from vanilla's world text setup (the same as {@code RenderTypes.text}) so Iris can
	 * draw it with its text program when a shader pack is on. Vertices need full-bright light so
	 * the lightmap leaves the digit colors unchanged.
	 */
	public static RenderType lightLevelNumbers(Identifier texture) {
		RenderPipeline pipeline = RenderPipelines.register(RenderPipeline.builder(RenderPipelinesAccessor.emutils$worldTextSnippet())
			.withLocation("pipeline/emutils_light_level_numbers")
			.withVertexShader("core/text")
			.withFragmentShader("core/text")
			.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
			.withCull(false)
			.build());
		IrisCompat.shadeLikeVanilla(RenderPipelines.TEXT, pipeline);
		return RenderType.create(
			"emutils_light_level_numbers",
			RenderSetup.builder(pipeline)
				.withTexture("Sampler0", texture)
				.useLightmap()
				.createRenderSetup()
		);
	}
}
