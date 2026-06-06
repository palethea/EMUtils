package net.emutils.client.emutils.inventory;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import org.jspecify.annotations.Nullable;

public final class InventoryPreviewItemOpacity {
	private static final Map<GuiItemRenderState, Float> OPACITIES = new IdentityHashMap<>();

	private InventoryPreviewItemOpacity() {
	}

	public static void track(GuiItemRenderState state, float opacity) {
		if (opacity < 1.0F) {
			OPACITIES.put(state, opacity);
		}
	}

	@Nullable
	public static Float remove(GuiItemRenderState state) {
		return OPACITIES.remove(state);
	}

	public static void clear() {
		OPACITIES.clear();
	}
}
