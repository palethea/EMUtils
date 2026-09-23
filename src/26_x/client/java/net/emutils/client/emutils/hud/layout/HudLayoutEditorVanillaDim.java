package net.emutils.client.emutils.hud.layout;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.util.ARGB;

public final class HudLayoutEditorVanillaDim {
	public static final float OPACITY = 0.18F;

	static final ThreadLocal<Float> ACTIVE = new ThreadLocal<>();
	private static final Map<GuiItemRenderState, Float> ITEMS = new IdentityHashMap<>();

	private HudLayoutEditorVanillaDim() {
	}

	public static void begin() {
		ACTIVE.set(OPACITY);
	}

	public static void end() {
		ACTIVE.remove();
		ITEMS.clear();
	}

	public static int dimColor(int color) {
		Float opacity = ACTIVE.get();
		if (opacity == null) {
			return color;
		}

		return ARGB.multiplyAlpha(color, opacity);
	}

	public static float dimAlpha(float alpha) {
		Float opacity = ACTIVE.get();
		if (opacity == null) {
			return alpha;
		}

		if (alpha < 0.0F) {
			return opacity;
		}

		return alpha * opacity;
	}

	public static int dimGuiColor(int color) {
		Float opacity = ACTIVE.get();
		if (opacity == null) {
			return color;
		}

		if (color == -1) {
			return ARGB.white(opacity);
		}

		return dimColor(color);
	}

	public static void trackItem(GuiItemRenderState state) {
		Float opacity = ACTIVE.get();
		if (opacity != null) {
			ITEMS.put(state, opacity);
		}
	}

	public static Float itemOpacity(GuiItemRenderState state) {
		return ITEMS.remove(state);
	}

	public static void clearItems() {
		ITEMS.clear();
	}
}
