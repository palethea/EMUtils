package net.emutils.client.emutils.hud.layout;

import java.lang.reflect.Field;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.client.renderer.state.gui.TiledBlitRenderState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

public final class HudLayoutEditorVanillaDimStates {
	private static final Field TEXT_FONT = textField("font");
	private static final Field TEXT_TEXT = textField("text");
	private static final Field TEXT_X = textField("x");
	private static final Field TEXT_Y = textField("y");
	private static final Field TEXT_COLOR = textField("color");
	private static final Field TEXT_BACKGROUND_COLOR = textField("backgroundColor");
	private static final Field TEXT_DROP_SHADOW = textField("dropShadow");

	private HudLayoutEditorVanillaDimStates() {
	}

	public static GuiElementRenderState dimSimple(@Nullable GuiElementRenderState state) {
		if (state == null || HudLayoutEditorVanillaDim.ACTIVE.get() == null) {
			return state;
		}

		if (state instanceof ColoredRectangleRenderState quad) {
			return new ColoredRectangleRenderState(
				quad.pipeline(),
				quad.textureSetup(),
				new Matrix3x2f(quad.pose()),
				quad.x0(),
				quad.y0(),
				quad.x1(),
				quad.y1(),
				HudLayoutEditorVanillaDim.dimColor(quad.col1()),
				HudLayoutEditorVanillaDim.dimColor(quad.col2()),
				quad.scissorArea()
			);
		}

		if (state instanceof BlitRenderState quad) {
			return new BlitRenderState(
				quad.pipeline(),
				quad.textureSetup(),
				new Matrix3x2f(quad.pose()),
				quad.x0(),
				quad.y0(),
				quad.x1(),
				quad.y1(),
				quad.u0(),
				quad.u1(),
				quad.v0(),
				quad.v1(),
				HudLayoutEditorVanillaDim.dimGuiColor(quad.color()),
				quad.scissorArea()
			);
		}

		if (state instanceof TiledBlitRenderState quad) {
			return new TiledBlitRenderState(
				quad.pipeline(),
				quad.textureSetup(),
				new Matrix3x2f(quad.pose()),
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

	public static GuiTextRenderState dimText(@Nullable GuiTextRenderState state) {
		if (state == null || HudLayoutEditorVanillaDim.ACTIVE.get() == null) {
			return state;
		}

		try {
			return new GuiTextRenderState(
				(net.minecraft.client.gui.Font) TEXT_FONT.get(state),
				(net.minecraft.util.FormattedCharSequence) TEXT_TEXT.get(state),
				state.pose,
				TEXT_X.getInt(state),
				TEXT_Y.getInt(state),
				HudLayoutEditorVanillaDim.dimColor(TEXT_COLOR.getInt(state)),
				HudLayoutEditorVanillaDim.dimColor(TEXT_BACKGROUND_COLOR.getInt(state)),
				TEXT_DROP_SHADOW.getBoolean(state),
				false,
				state.scissor
			);
		} catch (IllegalAccessException exception) {
			return state;
		}
	}

	private static Field textField(String name) {
		try {
			Field field = GuiTextRenderState.class.getDeclaredField(name);
			field.setAccessible(true);
			return field;
		} catch (ReflectiveOperationException exception) {
			throw new ExceptionInInitializerError(exception);
		}
	}
}
