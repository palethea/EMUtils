package net.emutils.client.mixin;

import net.emutils.client.emhelpers.hud.layout.HudLayoutEditorVanillaDim;
import net.emutils.client.emutils.inventory.InventoryPreviewItemOpacity;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.ItemGuiElementRenderState;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
	private static final ThreadLocal<ItemGuiElementRenderState> PREPARING_ITEM = new ThreadLocal<>();

	@Inject(method = "prepareItem", at = @At("HEAD"))
	private void emutils$capturePrepareItem(
		ItemGuiElementRenderState state,
		float u,
		float v,
		int pixelsPerItem,
		int itemAtlasSideLength,
		CallbackInfo ci
	) {
		PREPARING_ITEM.set(state);
	}

	@Inject(method = "prepareItem", at = @At("RETURN"))
	private void emutils$clearPrepareItem(CallbackInfo ci) {
		PREPARING_ITEM.remove();
	}

	@ModifyConstant(method = "prepareItem", constant = @Constant(intValue = -1))
	private int emutils$applyInventoryPreviewItemOpacity(int color) {
		ItemGuiElementRenderState state = PREPARING_ITEM.get();
		if (state != null) {
			Float previewOpacity = InventoryPreviewItemOpacity.remove(state);
			if (previewOpacity != null) {
				return ColorHelper.getWhite(previewOpacity);
			}

			Float dimOpacity = HudLayoutEditorVanillaDim.itemOpacity(state);
			if (dimOpacity != null) {
				return ColorHelper.getWhite(dimOpacity);
			}
		}

		return color;
	}

	@Inject(method = "prepareItemElements", at = @At("RETURN"))
	private void emutils$clearPreviewItemOpacities(CallbackInfo ci) {
		InventoryPreviewItemOpacity.clear();
		HudLayoutEditorVanillaDim.clearItems();
	}
}
