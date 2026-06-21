package net.emutils.client.mixin;

import java.util.List;
import java.util.Optional;
import net.emutils.client.EMUtilsClient;
import net.emhelpers.client.hud.editor.HudLayoutEditorOverlay;
import net.emutils.client.emutils.food.FoodHudHelper;
import net.emutils.client.emutils.food.FoodTooltipData;
import net.emutils.client.emutils.tweaks.TooltipPreviewRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin<T extends AbstractContainerMenu> {
	@Shadow
	@Nullable
	protected Slot hoveredSlot;

	@Shadow
	protected T menu;

	@Shadow
	protected int leftPos;

	@Shadow
	protected int topPos;

	@ModifyArg(
		method = "extractTooltip",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/resources/Identifier;)V"
		),
		index = 1
	)
	private List<Component> emutils$stripShulkerContainerText(List<Component> tooltip) {
		if (this.hoveredSlot == null || !this.hoveredSlot.hasItem()) {
			return tooltip;
		}

		ItemStack stack = this.hoveredSlot.getItem();
		if (TooltipPreviewRenderer.shouldPreviewShulker(stack)) {
			return TooltipPreviewRenderer.stripContainerLines(tooltip);
		}

		return tooltip;
	}

	@ModifyArg(
		method = "extractTooltip",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/resources/Identifier;)V"
		),
		index = 2
	)
	private Optional<TooltipComponent> emutils$injectContainerPreview(Optional<TooltipComponent> tooltipData) {
		if (this.hoveredSlot == null || !this.hoveredSlot.hasItem()) {
			return tooltipData;
		}

		ItemStack stack = this.hoveredSlot.getItem();
		if (TooltipPreviewRenderer.shouldPreviewShulker(stack)) {
			return Optional.of(TooltipPreviewRenderer.createShulkerTooltipData(stack));
		}

		BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
		if (bundle != null && !bundle.isEmpty() && !EMUtilsClient.config().tweakBundleTooltipPreview()) {
			return Optional.empty();
		}

		if (tooltipData.isEmpty()) {
			FoodTooltipData foodData = FoodHudHelper.tooltipData(stack, Minecraft.getInstance().player);
			if (foodData != null) {
				return Optional.of(foodData);
			}
		}

		return tooltipData;
	}

	@Inject(method = "extractSlot", at = @At("TAIL"))
	private void emutils$drawInventoryToolSlotOverlay(GuiGraphicsExtractor context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
		Inventory inventory = emutils$playerInventory();
		if (inventory != null) {
			EMUtilsClient.inventoryTools().drawSlotOverlay(context, menu, slot, inventory);
		}
	}

	@Inject(method = "extractContents", at = @At("TAIL"))
	private void emutils$drawInventoryToolDragLine(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
		Inventory inventory = emutils$playerInventory();
		Minecraft client = Minecraft.getInstance();
		if (inventory != null && client != null) {
			EMUtilsClient.inventoryTools().finishDragIfBindKeyReleased(client, menu, hoveredSlot, inventory);
		}
		EMUtilsClient.inventoryTools().drawDragLine(context, hoveredSlot, leftPos, topPos, mouseX, mouseY);
		if (inventory != null && client != null) {
			EMUtilsClient.inventoryTools().drawSortButtons(client, context, menu, inventory, leftPos, topPos, mouseX - leftPos, mouseY - topPos);
		}
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void emutils$renderHudLayoutEditorOverlay(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
		HudLayoutEditorOverlay.render(context, mouseX, mouseY, deltaTicks);
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void emutils$handleInventoryToolKeyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
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

		Inventory inventory = emutils$playerInventory();
		if (inventory != null && EMUtilsClient.inventoryTools().handleKeyPressed(menu, hoveredSlot, input, inventory)) {
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
		Minecraft client = Minecraft.getInstance();
		if (client != null) {
			EMUtilsClient.inventoryTools().cursor().tryRestoreAfterInit(client);
		}
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void emutils$handleHudEditorMouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
		if (HudLayoutEditorOverlay.handleMouseClicked(click)) {
			cir.setReturnValue(true);
			return;
		}

		Inventory inventory = emutils$playerInventory();
		Minecraft client = Minecraft.getInstance();
		if (inventory == null || client == null) {
			return;
		}

		Slot slot = emutils$slotAt(click.x(), click.y());
		if (EMUtilsClient.inventoryTools().handleSortButtonMouseClicked(client, menu, click.button(), click.x() - leftPos, click.y() - topPos, inventory)) {
			cir.setReturnValue(true);
			return;
		}
		if (EMUtilsClient.inventoryTools().handleHoverTransferMouseClicked(client, menu, slot, click.button(), click.hasShiftDown(), inventory)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
	private void emutils$finishInventoryToolBindDrag(MouseButtonEvent click, CallbackInfoReturnable<Boolean> cir) {
		if (HudLayoutEditorOverlay.handleMouseReleased(click)) {
			cir.setReturnValue(true);
			return;
		}

		Inventory inventory = emutils$playerInventory();
		if (inventory == null) {
			return;
		}

		Slot slot = emutils$slotAt(click.x(), click.y());
		if (EMUtilsClient.inventoryTools().handleMouseReleased(menu, slot, inventory)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
	private void emutils$dragHudLayoutEditorOverlay(MouseButtonEvent click, double offsetX, double offsetY, CallbackInfoReturnable<Boolean> cir) {
		if (HudLayoutEditorOverlay.handleMouseDragged(click, offsetX, offsetY)) {
			cir.setReturnValue(true);
			return;
		}

		Inventory inventory = emutils$playerInventory();
		Minecraft client = Minecraft.getInstance();
		if (inventory == null || client == null) {
			return;
		}

		Slot slot = emutils$slotAt(click.x(), click.y());
		if (EMUtilsClient.inventoryTools().handleHoverTransferMouseDragged(client, menu, slot, click.button(), click.hasShiftDown(), inventory)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(
		method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void emutils$guardInventoryToolSlotMouseButtonEvent(
		@Nullable Slot slot,
		int slotId,
		int button,
		ContainerInput actionType,
		CallbackInfo ci
	) {
		if (HudLayoutEditorOverlay.isActive()) {
			ci.cancel();
			return;
		}

		Inventory inventory = emutils$playerInventory();
		Minecraft client = Minecraft.getInstance();
		if (inventory == null || client == null) {
			return;
		}

		if (EMUtilsClient.inventoryTools().guardSlotMouseButtonEvent(
			client,
			menu,
			slot,
			button,
			actionType,
			menu.getCarried(),
			inventory
		)) {
			ci.cancel();
		}
	}

	@Nullable
	private Inventory emutils$playerInventory() {
		Minecraft client = Minecraft.getInstance();
		return client == null || client.player == null ? null : client.player.getInventory();
	}

	@Nullable
	private Slot emutils$slotAt(double mouseX, double mouseY) {
		double pointX = mouseX - leftPos;
		double pointY = mouseY - topPos;
		for (Slot slot : menu.slots) {
			if (slot.isActive()
				&& pointX >= slot.x - 1
				&& pointX < slot.x + 17
				&& pointY >= slot.y - 1
				&& pointY < slot.y + 17) {
				return slot;
			}
		}

		return null;
	}
}
