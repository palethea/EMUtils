package net.emutils.client.inventory;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.gui.render.state.ItemGuiElementRenderState;
import org.jspecify.annotations.Nullable;

public final class InventoryPreviewItemOpacity {
	private static final Map<ItemGuiElementRenderState, Float> OPACITIES = new IdentityHashMap<>();

	private InventoryPreviewItemOpacity() {
	}

	public static void track(ItemGuiElementRenderState state, float opacity) {
		if (opacity < 1.0F) {
			OPACITIES.put(state, opacity);
		}
	}

	@Nullable
	public static Float remove(ItemGuiElementRenderState state) {
		return OPACITIES.remove(state);
	}

	public static void clear() {
		OPACITIES.clear();
	}
}
