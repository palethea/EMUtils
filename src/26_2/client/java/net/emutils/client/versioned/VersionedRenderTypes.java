package net.emutils.client.versioned;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/** Minecraft 26.2 render pipelines and render types used by EMUtils renderers. */
public final class VersionedRenderTypes {
	private VersionedRenderTypes() {
	}

	/** Translucent, unculled, textured quads for the light level overlay's digit sprites. */
	public static RenderType lightLevelNumbers(Identifier texture) {
		RenderPipeline pipeline = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
			.withLocation("pipeline/emutils_light_level_numbers")
			.withVertexShader("core/position_tex_color")
			.withFragmentShader("core/position_tex_color")
			.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
			.withCull(false)
			.withDepthStencilState(DepthStencilState.DEFAULT)
			.withBindGroupLayout(BindGroupLayouts.SAMPLER0)
			.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
			.withPrimitiveTopology(PrimitiveTopology.QUADS)
			.build());
		return RenderType.create(
			"emutils_light_level_numbers",
			RenderSetup.builder(pipeline)
				.withTexture("Sampler0", texture)
				.createRenderSetup()
		);
	}
}
