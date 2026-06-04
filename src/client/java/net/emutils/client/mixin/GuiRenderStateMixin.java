package net.emutils.client.mixin;

import net.emhelpers.client.hud.layout.HudLayoutEditorVanillaDim;
import net.emhelpers.client.hud.layout.HudLayoutEditorVanillaDimStates;
import net.emutils.client.emutils.inventory.InventoryPreviewItemOpacity;
import net.emutils.client.emutils.inventory.InventoryPreviewRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.ItemGuiElementRenderState;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.gui.render.state.TextGuiElementRenderState;
import net.minecraft.client.gui.render.state.TexturedQuadGuiElementRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderState.class)
public abstract class GuiRenderStateMixin {
	@ModifyVariable(method = "addSimpleElement", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private SimpleGuiElementRenderState emutils$dimSimpleElement(SimpleGuiElementRenderState state) {
		return HudLayoutEditorVanillaDimStates.dimSimple(state);
	}

	@ModifyVariable(method = "addSimpleElementToCurrentLayer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private TexturedQuadGuiElementRenderState emutils$dimTexturedElement(TexturedQuadGuiElementRenderState state) {
		return (TexturedQuadGuiElementRenderState) HudLayoutEditorVanillaDimStates.dimSimple(state);
	}

	@ModifyVariable(method = "addText", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private TextGuiElementRenderState emutils$dimTextElement(TextGuiElementRenderState state) {
		return HudLayoutEditorVanillaDimStates.dimText(state);
	}

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
		HudLayoutEditorVanillaDim.trackItem(state);
	}
}
