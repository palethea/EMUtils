package net.emutils.client.versioned;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/** Minecraft 26.3 render pipelines and render types used by EMUtils renderers. */
public final class VersionedRenderTypes {
	private VersionedRenderTypes() {
	}

	/**
	 * Translucent, unculled, textured quads for the light level overlay's digit sprites.
	 *
	 * <p>With Improved Transparency on, 26.3 draws every blended custom geometry submit through its
	 * order-independent transparency passes and fails for render types without OIT pipelines. Only
	 * some core shaders support those passes, so this uses vanilla's world text shaders (the same
	 * setup as {@code RenderTypes.text}) instead of {@code position_tex_color}. Vertices need
	 * full-bright light so the lightmap leaves the digit colors unchanged.
	 */
	public static RenderType lightLevelNumbers(Identifier texture) {
		RenderPipeline pipeline = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.WORLD_TEXT_SNIPPET)
			.withLocation("pipeline/emutils_light_level_numbers")
			.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
			.withCull(false)
			.build());
		OitPipelineSet oitPipelines = RenderPipelines.register(OitPipelineSet.builder(
			"emutils_light_level_numbers",
			RenderPipeline.builder(RenderPipelines.WORLD_TEXT_SNIPPET).withCull(false)
		).build());
		return RenderType.create(
			"emutils_light_level_numbers",
			RenderSetup.builder(pipeline)
				.setOitPipelines(oitPipelines)
				.withTexture("Sampler0", texture)
				.useLightmap()
				.createRenderSetup()
		);
	}
}
