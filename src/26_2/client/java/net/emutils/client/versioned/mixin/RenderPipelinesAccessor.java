package net.emutils.client.versioned.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 26.2 keeps the world text snippet private; 26.3 made it public. */
@Mixin(RenderPipelines.class)
public interface RenderPipelinesAccessor {
	@Accessor("WORLD_TEXT_SNIPPET")
	static RenderPipeline.Snippet emutils$worldTextSnippet() {
		throw new AssertionError();
	}
}
