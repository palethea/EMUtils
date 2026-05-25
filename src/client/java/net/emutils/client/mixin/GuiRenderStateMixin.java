package net.emutils.client.mixin;

import net.emutils.client.inventory.InventoryPreviewItemOpacity;
import net.emutils.client.inventory.InventoryPreviewRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.ItemGuiElementRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderState.class)
public abstract class GuiRenderStateMixin {
	@Inject(
		method = "addItem",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/render/state/GuiRenderState$Layer;addItem(Lnet/minecraft/client/gui/render/state/ItemGuiElementRenderState;)V",
			shift = At.Shift.AFTER
		)
	)
	private void emutils$trackPreviewItemOpacity(ItemGuiElementRenderState state, CallbackInfo ci) {
		InventoryPreviewItemOpacity.track(state, InventoryPreviewRenderer.itemRenderOpacity());
	}
}
