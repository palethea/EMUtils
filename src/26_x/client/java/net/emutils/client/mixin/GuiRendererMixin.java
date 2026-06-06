package net.emutils.client.mixin;

import net.emhelpers.client.hud.layout.HudLayoutEditorVanillaDim;
import net.emutils.client.emutils.inventory.InventoryPreviewItemOpacity;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
	private static final ThreadLocal<GuiItemRenderState> PREPARING_ITEM = new ThreadLocal<>();

	@Inject(method = "submitBlitFromItemAtlas", at = @At("HEAD"))
	private void emutils$capturePrepareItem(
		GuiItemRenderState state,
		net.minecraft.client.gui.render.GuiItemAtlas.SlotView slotView,
		CallbackInfo ci
	) {
		PREPARING_ITEM.set(state);
	}

	@Inject(method = "submitBlitFromItemAtlas", at = @At("RETURN"))
	private void emutils$clearPrepareItem(CallbackInfo ci) {
		PREPARING_ITEM.remove();
	}

	@ModifyConstant(method = "submitBlitFromItemAtlas", constant = @Constant(intValue = -1))
	private int emutils$applyInventoryPreviewItemOpacity(int color) {
		GuiItemRenderState state = PREPARING_ITEM.get();
		if (state != null) {
			Float previewOpacity = InventoryPreviewItemOpacity.remove(state);
			if (previewOpacity != null) {
				return ARGB.white(previewOpacity);
			}

			Float dimOpacity = HudLayoutEditorVanillaDim.itemOpacity(state);
			if (dimOpacity != null) {
				return ARGB.white(dimOpacity);
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
