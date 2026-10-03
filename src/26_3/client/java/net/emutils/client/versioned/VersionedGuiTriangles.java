package net.emutils.client.versioned;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

/**
 * Triangles in the GUI, for shapes a blit can't draw: map tiles clipped to a round or rotated minimap, and
 * the minimap's player arrow. The GUI pipelines draw quads, so each triangle is sent as a quad whose last
 * corner repeats its third.
 */
public final class VersionedGuiTriangles {
	private VersionedGuiTriangles() {
	}

	/**
	 * Textured triangles in the current pose: {@code xy} holds x, y for each corner and {@code uv} the
	 * texture coordinates for each, three corners per triangle. Only the first {@code corners} are drawn.
	 */
	public static void textured(GuiGraphicsExtractor context, AbstractTexture texture, float[] xy, float[] uv, int corners, int color) {
		if (corners < 3) {
			return;
		}
		TextureSetup setup = TextureSetup.singleTexture(texture.getTextureView(), texture.getSampler());
		submit(context, new State(RenderPipelines.GUI_TEXTURED, setup, new Matrix3x2f(context.pose()), xy.clone(), uv.clone(), corners, color, context.scissorStack.peek()));
	}

	/** Plain colored triangles in the current pose, three corners of x, y each. */
	public static void colored(GuiGraphicsExtractor context, float[] xy, int corners, int color) {
		if (corners < 3) {
			return;
		}
		submit(context, new State(RenderPipelines.GUI, TextureSetup.noTexture(), new Matrix3x2f(context.pose()), xy.clone(), null, corners, color, context.scissorStack.peek()));
	}

	private static void submit(GuiGraphicsExtractor context, State state) {
		if (state.bounds() != null) {
			context.guiRenderState.addGuiElement(state);
		}
	}

	private static final class State implements GuiElementRenderState {
		private final RenderPipeline pipeline;
		private final TextureSetup textureSetup;
		private final Matrix3x2f pose;
		private final float[] xy;
		private final float @Nullable [] uv;
		private final int corners;
		private final int color;
		private final @Nullable ScreenRectangle scissorArea;
		private final @Nullable ScreenRectangle bounds;

		private State(
			RenderPipeline pipeline,
			TextureSetup textureSetup,
			Matrix3x2f pose,
			float[] xy,
			float @Nullable [] uv,
			int corners,
			int color,
			@Nullable ScreenRectangle scissorArea
		) {
			this.pipeline = pipeline;
			this.textureSetup = textureSetup;
			this.pose = pose;
			this.xy = xy;
			this.uv = uv;
			this.corners = corners - corners % 3;
			this.color = color;
			this.scissorArea = scissorArea;
			float minX = Float.MAX_VALUE;
			float minY = Float.MAX_VALUE;
			float maxX = -Float.MAX_VALUE;
			float maxY = -Float.MAX_VALUE;
			for (int i = 0; i < this.corners; i++) {
				minX = Math.min(minX, xy[i * 2]);
				minY = Math.min(minY, xy[i * 2 + 1]);
				maxX = Math.max(maxX, xy[i * 2]);
				maxY = Math.max(maxY, xy[i * 2 + 1]);
			}
			int x0 = (int) Math.floor(minX);
			int y0 = (int) Math.floor(minY);
			ScreenRectangle area = new ScreenRectangle(x0, y0, (int) Math.ceil(maxX) - x0, (int) Math.ceil(maxY) - y0).transformMaxBounds(pose);
			this.bounds = scissorArea != null ? scissorArea.intersection(area) : area;
		}

		@Override
		public void buildVertices(VertexConsumer consumer) {
			for (int triangle = 0; triangle < corners; triangle += 3) {
				// The GUI pipelines cull back faces, so every triangle goes the way a blit's corners go.
				boolean flip = cross(triangle) > 0.0F;
				for (int corner = 0; corner < 4; corner++) {
					int pick = Math.min(corner, 2);
					if (flip && pick > 0) {
						pick = 3 - pick;
					}
					int i = triangle + pick;
					VertexConsumer vertex = consumer.addVertexWith2DPose(pose, xy[i * 2], xy[i * 2 + 1]);
					if (uv != null) {
						vertex.setUv(uv[i * 2], uv[i * 2 + 1]);
					}
					vertex.setColor(color);
				}
			}
		}

		private float cross(int triangle) {
			float ax = xy[triangle * 2];
			float ay = xy[triangle * 2 + 1];
			float bx = xy[triangle * 2 + 2];
			float by = xy[triangle * 2 + 3];
			float cx = xy[triangle * 2 + 4];
			float cy = xy[triangle * 2 + 5];
			return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
		}

		@Override
		public RenderPipeline pipeline() {
			return pipeline;
		}

		@Override
		public TextureSetup textureSetup() {
			return textureSetup;
		}

		@Override
		public @Nullable ScreenRectangle scissorArea() {
			return scissorArea;
		}

		@Override
		public @Nullable ScreenRectangle bounds() {
			return bounds;
		}
	}
}
