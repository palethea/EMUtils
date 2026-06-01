package net.emutils.client.emskyblock.features.inventory.common;

import java.util.List;
import net.emutils.client.emskyblock.features.inventory.auctionhouse.AuctionHouseFeatures;
import net.emutils.client.emskyblock.features.inventory.bazaar.BazaarFeatures;
import net.emutils.client.emskyblock.features.inventory.experimentationtable.ExperimentationTableFeatures;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

public final class InventoryScreenFeatures {
	private InventoryScreenFeatures() {
	}

	public static void onInventoryOpen(String title, ScreenHandler handler) {
		if (!enabled()) {
			return;
		}
		AuctionHouseFeatures.onInventoryOpen(title, handler);
		BazaarFeatures.onInventoryOpen(title, handler);
		ExperimentationTableFeatures.onInventoryOpen(title, handler);
	}

	public static void onInventoryClose() {
		AuctionHouseFeatures.onInventoryClose();
		BazaarFeatures.onInventoryClose();
		ExperimentationTableFeatures.onInventoryClose();
	}

	public static void onChat(Text message) {
		if (!enabled()) {
			return;
		}
		AuctionHouseFeatures.onChat(message);
		BazaarFeatures.onChat(message);
		ExperimentationTableFeatures.onChat(message);
	}

	public static void onCommand(String command) {
		if (!enabled()) {
			return;
		}
		BazaarFeatures.onCommand(command);
	}

	public static void renderScreenOverlay(DrawContext context, ScreenHandler handler, String title, int screenX, int screenY, int mouseX, int mouseY) {
		if (!enabled()) {
			return;
		}
		AuctionHouseFeatures.renderScreenOverlay(context, handler, title, screenX, screenY);
		BazaarFeatures.renderScreenOverlay(context, handler, title, screenX, screenY, mouseX, mouseY);
		ExperimentationTableFeatures.renderScreenOverlay(context, handler, title, screenX, screenY);
	}

	public static void drawSlotOverlay(DrawContext context, ScreenHandler handler, Slot slot, String title) {
		if (!enabled()) {
			return;
		}
		AuctionHouseFeatures.drawSlotOverlay(context, handler, slot, title);
		BazaarFeatures.drawSlotOverlay(context, handler, slot, title);
		ExperimentationTableFeatures.drawSlotOverlay(context, handler, slot, title);
	}

	public static List<Text> appendTooltip(ScreenHandler handler, String title, @Nullable Slot slot, List<Text> tooltip) {
		if (!enabled() || slot == null) {
			return tooltip;
		}
		List<Text> result = AuctionHouseFeatures.appendTooltip(title, slot, tooltip);
		return BazaarFeatures.appendTooltip(handler, title, slot, result);
	}

	public static boolean handleKeyPressed(ScreenHandler handler, @Nullable Slot focusedSlot, KeyInput input, String title) {
		return enabled() && AuctionHouseFeatures.handleKeyPressed(handler, focusedSlot, input, title);
	}

	public static boolean handleMouseClicked(double mouseX, double mouseY, int button) {
		return enabled() && BazaarFeatures.handleMouseClicked(mouseX, mouseY, button);
	}

	public static boolean guardSlotClick(ScreenHandler handler, String title, @Nullable Slot slot) {
		if (!enabled() || slot == null) {
			return false;
		}
		return AuctionHouseFeatures.guardSlotClick(title, slot)
			|| BazaarFeatures.guardSlotClick(handler, title, slot)
			|| ExperimentationTableFeatures.guardSlotClick(title, slot);
	}

	private static boolean enabled() {
		return InventoryFeatureUtils.skyblockFeatureEnabled(MinecraftClient.getInstance());
	}
}
