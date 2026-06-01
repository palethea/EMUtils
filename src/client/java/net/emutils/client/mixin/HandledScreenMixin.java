package net.emutils.client.mixin;

import java.util.List;
import java.util.Optional;
import net.emutils.client.EMUtilsClient;
import net.emhelpers.client.hud.editor.HudLayoutEditorOverlay;
import net.emutils.client.emutils.tweaks.TooltipPreviewRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> {
	@Shadow
	@Nullable
	protected Slot focusedSlot;

	@Shadow
	protected T handler;

	@Shadow
	protected int x;

	@Shadow
	protected int y;

	@ModifyArg(
		method = "drawMouseoverTooltip",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/DrawContext;drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/util/Identifier;)V"
		),
		index = 1
	)
	private List<Text> emutils$stripShulkerContainerText(List<Text> tooltip) {
		if (this.focusedSlot == null || !this.focusedSlot.hasStack()) {
			return tooltip;
		}

		ItemStack stack = this.focusedSlot.getStack();
		List<Text> result = tooltip;
		if (TooltipPreviewRenderer.shouldPreviewShulker(stack)) {
			result = TooltipPreviewRenderer.stripContainerLines(tooltip);
		}

		return result;
	}

	@ModifyArg(
		method = "drawMouseoverTooltip",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/DrawContext;drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/util/Identifier;)V"
		),
		index = 2
	)
	private Optional<TooltipData> emutils$injectContainerPreview(Optional<TooltipData> tooltipData) {
		if (this.focusedSlot == null || !this.focusedSlot.hasStack()) {
			return tooltipData;
		}

		ItemStack stack = this.focusedSlot.getStack();
		if (TooltipPreviewRenderer.shouldPreviewShulker(stack)) {
			return Optional.of(TooltipPreviewRenderer.createShulkerTooltipData(stack));
		}

		BundleContentsComponent bundle = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
		if (bundle != null && !bundle.isEmpty() && !EMUtilsClient.config().tweakBundleTooltipPreview()) {
			return Optional.empty();
		}

		return tooltipData;
	}

	@Inject(method = "drawSlot", at = @At("TAIL"))
	private void emutils$drawInventoryToolSlotOverlay(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
		PlayerInventory inventory = emutils$playerInventory();
		if (inventory != null) {
			EMUtilsClient.inventoryTools().drawSlotOverlay(context, handler, slot, inventory);
		}
	}

	@Inject(
		method = "renderMain",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawSlotHighlightFront(Lnet/minecraft/client/gui/DrawContext;)V"
		)
	)
	private void emutils$drawInventoryToolDragLine(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
		PlayerInventory inventory = emutils$playerInventory();
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		if (inventory != null && minecraftClient != null) {
			EMUtilsClient.inventoryTools().finishDragIfBindKeyReleased(minecraftClient, handler, focusedSlot, inventory);
		}
		EMUtilsClient.inventoryTools().drawDragLine(context, focusedSlot, mouseX - x, mouseY - y);
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void emutils$renderHudLayoutEditorOverlay(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
		HudLayoutEditorOverlay.render(context, mouseX, mouseY, deltaTicks);
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void emutils$handleInventoryToolKeyPressed(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
		if (HudLayoutEditorOverlay.handleKeyPressed(input)) {
			cir.setReturnValue(true);
			return;
		}

		if (EMUtilsClient.tryOpenHudLayoutEditor(input)) {
			cir.setReturnValue(true);
			return;
		}

		if (EMUtilsClient.tryDebugGuiDump(input)) {
			cir.setReturnValue(true);
			return;
		}

		PlayerInventory inventory = emutils$playerInventory();
		if (inventory != null && EMUtilsClient.inventoryTools().handleKeyPressed(handler, focusedSlot, input, inventory)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "removed", at = @At("HEAD"))
	private void emutils$clearInventoryToolDrag(CallbackInfo ci) {
		HudLayoutEditorOverlay.cancelActive();
		EMUtilsClient.inventoryTools().clearDrag();
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void emutils$restoreContainerCursor(CallbackInfo ci) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null) {
			EMUtilsClient.inventoryTools().cursor().tryRestoreAfterInit(client);
		}
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void emutils$handleTrackerHudClickInInventory(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
		if (HudLayoutEditorOverlay.handleMouseClicked(click)) {
			cir.setReturnValue(true);
			return;
		}

		int button = click.button();
		if (button != 0 && button != 1) {
			return;
		}

	}

	@Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
	private void emutils$finishInventoryToolBindDrag(Click click, CallbackInfoReturnable<Boolean> cir) {
		if (HudLayoutEditorOverlay.handleMouseReleased(click)) {
			cir.setReturnValue(true);
			return;
		}

		PlayerInventory inventory = emutils$playerInventory();
		if (inventory == null) {
			return;
		}

		Slot slot = emutils$slotAt(click.x(), click.y());
		if (EMUtilsClient.inventoryTools().handleMouseReleased(handler, slot, inventory)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
	private void emutils$dragHudLayoutEditorOverlay(Click click, double offsetX, double offsetY, CallbackInfoReturnable<Boolean> cir) {
		if (HudLayoutEditorOverlay.handleMouseDragged(click, offsetX, offsetY)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"), cancellable = true)
	private void emutils$guardInventoryToolSlotClick(@Nullable Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
		if (HudLayoutEditorOverlay.isActive()) {
			ci.cancel();
			return;
		}

		PlayerInventory inventory = emutils$playerInventory();
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		if (inventory == null || minecraftClient == null) {
			return;
		}

		if (EMUtilsClient.inventoryTools().guardSlotClick(minecraftClient, handler, slot, button, actionType, handler.getCursorStack(), inventory)) {
			ci.cancel();
		}
	}

	@Nullable
	private PlayerInventory emutils$playerInventory() {
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		return minecraftClient == null || minecraftClient.player == null ? null : minecraftClient.player.getInventory();
	}

	@Nullable
	private Slot emutils$slotAt(double mouseX, double mouseY) {
		double pointX = mouseX - x;
		double pointY = mouseY - y;
		for (Slot slot : handler.slots) {
			if (slot.isEnabled()
				&& pointX >= slot.x - 1
				&& pointX < slot.x + 17
				&& pointY >= slot.y - 1
				&& pointY < slot.y + 17) {
				return slot;
			}
		}

		return null;
	}

	private String emutils$title() {
		String stripped = Formatting.strip(((HandledScreen<?>) (Object) this).getTitle().getString());
		return stripped == null ? "" : stripped.trim();
	}
}
