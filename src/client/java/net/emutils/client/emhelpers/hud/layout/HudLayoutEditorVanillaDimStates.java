package net.emutils.client.emhelpers.hud.layout;

import net.minecraft.client.gui.render.state.ColoredQuadGuiElementRenderState;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.gui.render.state.TextGuiElementRenderState;
import net.minecraft.client.gui.render.state.TexturedQuadGuiElementRenderState;
import net.minecraft.client.gui.render.state.TiledTexturedQuadGuiElementRenderState;
import org.jspecify.annotations.Nullable;

public final class HudLayoutEditorVanillaDimStates {
	private HudLayoutEditorVanillaDimStates() {
	}

	public static SimpleGuiElementRenderState dimSimple(@Nullable SimpleGuiElementRenderState state) {
		if (state == null || HudLayoutEditorVanillaDim.ACTIVE.get() == null) {
			return state;
		}

		if (state instanceof ColoredQuadGuiElementRenderState quad) {
			return new ColoredQuadGuiElementRenderState(
				quad.pipeline(),
				quad.textureSetup(),
				quad.pose(),
				quad.x0(),
				quad.y0(),
				quad.x1(),
				quad.y1(),
				HudLayoutEditorVanillaDim.dimColor(quad.col1()),
				HudLayoutEditorVanillaDim.dimColor(quad.col2()),
				quad.scissorArea()
			);
		}

		if (state instanceof TexturedQuadGuiElementRenderState quad) {
			return new TexturedQuadGuiElementRenderState(
				quad.pipeline(),
				quad.textureSetup(),
				quad.pose(),
				quad.x1(),
				quad.y1(),
				quad.x2(),
				quad.y2(),
				quad.u1(),
				quad.u2(),
				quad.v1(),
				quad.v2(),
				HudLayoutEditorVanillaDim.dimGuiColor(quad.color()),
				quad.scissorArea()
			);
		}

		if (state instanceof TiledTexturedQuadGuiElementRenderState quad) {
			return new TiledTexturedQuadGuiElementRenderState(
				quad.pipeline(),
				quad.textureSetup(),
				quad.pose(),
				quad.tileWidth(),
				quad.tileHeight(),
				quad.x0(),
				quad.y0(),
				quad.x1(),
				quad.y1(),
				quad.u0(),
				quad.u1(),
				quad.v0(),
				quad.v1(),
				HudLayoutEditorVanillaDim.dimGuiColor(quad.color()),
				quad.scissorArea(),
				quad.bounds()
			);
		}

		return state;
	}

	public static TextGuiElementRenderState dimText(@Nullable TextGuiElementRenderState state) {
		if (state == null || HudLayoutEditorVanillaDim.ACTIVE.get() == null) {
			return state;
		}

		return new TextGuiElementRenderState(
			state.textRenderer,
			state.orderedText,
			state.matrix,
			state.x,
			state.y,
			HudLayoutEditorVanillaDim.dimColor(state.color),
			HudLayoutEditorVanillaDim.dimColor(state.backgroundColor),
			state.shadow,
			false,
			state.clipBounds
		);
	}
}
