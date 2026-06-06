package net.emutils.client.mixin;

import net.emhelpers.client.hud.layout.HudLayoutEditorVanillaDim;
import net.emhelpers.client.hud.layout.HudLayoutEditorVanillaDimStates;
import net.emutils.client.emutils.inventory.InventoryPreviewItemOpacity;
import net.emutils.client.emutils.inventory.InventoryPreviewRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderState.class)
public abstract class GuiRenderStateMixin {
	@ModifyVariable(method = "addGuiElement", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private GuiElementRenderState emutils$dimSimpleElement(GuiElementRenderState state) {
		return HudLayoutEditorVanillaDimStates.dimSimple(state);
	}

	@ModifyVariable(method = "addBlitToCurrentLayer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private BlitRenderState emutils$dimTexturedElement(BlitRenderState state) {
		return (BlitRenderState) HudLayoutEditorVanillaDimStates.dimSimple(state);
	}

	@ModifyVariable(method = "addText", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private GuiTextRenderState emutils$dimTextElement(GuiTextRenderState state) {
		return HudLayoutEditorVanillaDimStates.dimText(state);
	}

	@Inject(
		method = "addItem",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/state/gui/GuiRenderState$Node;addItem(Lnet/minecraft/client/renderer/state/gui/GuiItemRenderState;)V",
			shift = At.Shift.AFTER
		)
	)
	private void emutils$trackPreviewItemOpacity(GuiItemRenderState state, CallbackInfo ci) {
		InventoryPreviewItemOpacity.track(state, InventoryPreviewRenderer.itemRenderOpacity());
		HudLayoutEditorVanillaDim.trackItem(state);
	}
}
