package net.emutils.client.emutils.inventory.gui;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.gui.EMUtilsScreen;
import net.emhelpers.client.gui.widget.ConfigToggleButton;
import net.emutils.client.emutils.inventory.SlotLockColor;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.text.Text;

public final class InventoryToolsSettingsScreen extends EMUtilsScreen {
	public InventoryToolsSettingsScreen(Screen parent) {
		super(parent, Text.translatable(EMUtilsTexts.SCREEN_INVENTORY_TOOLS));
	}

	@Override
	protected void initBody() {
		GridWidget.Adder adder = initTwoColumnBody();
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_INVENTORY_TOOLS,
			() -> EMUtilsClient.config().inventoryToolsEnabled(),
			EMUtilsClient.config()::setInventoryToolsEnabled
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SLOT_LOCKING,
			() -> EMUtilsClient.config().slotLockingEnabled(),
			EMUtilsClient.config()::setSlotLockingEnabled
		));
		adder.add(slotLockColorButton());
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SLOT_BINDING,
			() -> EMUtilsClient.config().slotBindingEnabled(),
			EMUtilsClient.config()::setSlotBindingEnabled
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SLOT_BINDING_LOCK_BOUND_SLOTS,
			() -> EMUtilsClient.config().slotBindingLockBoundSlots(),
			EMUtilsClient.config()::setSlotBindingLockBoundSlots
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_INVENTORY_PREVIEW,
			() -> EMUtilsClient.config().inventoryPreviewEnabled(),
			EMUtilsClient.config()::setInventoryPreviewEnabled
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_PRESERVE_CONTAINER_CURSOR,
			() -> EMUtilsClient.config().preserveContainerCursor(),
			EMUtilsClient.config()::setPreserveContainerCursor
		));
		adder.add(fullWidthSettingsButton(Text.translatable(EMUtilsTexts.OPTION_RESET_DEFAULTS), button -> {
			EMUtilsClient.config().resetInventoryToolsDefaults();
			client.setScreen(new InventoryToolsSettingsScreen(parent));
		}), SETTINGS_COLUMNS);
	}

	private static ButtonWidget slotLockColorButton() {
		return ButtonWidget.builder(slotLockColorMessage(), button -> {
			SlotLockColor next = EMUtilsClient.config().slotLockColor().next();
			EMUtilsClient.config().setSlotLockColor(next);
			button.setMessage(slotLockColorMessage());
		}).width(SETTINGS_BUTTON_WIDTH).build();
	}

	private static Text slotLockColorMessage() {
		return Text.translatable(
			EMUtilsTexts.OPTION_VALUE,
			Text.translatable(EMUtilsTexts.OPTION_SLOT_LOCK_COLOR),
			Text.translatable(EMUtilsClient.config().slotLockColor().labelKey())
		);
	}
}
