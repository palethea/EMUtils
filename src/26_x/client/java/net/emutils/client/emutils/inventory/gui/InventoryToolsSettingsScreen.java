package net.emutils.client.emutils.inventory.gui;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.gui.EMUtilsScreen;
import net.emhelpers.client.gui.widget.ConfigToggleButton;
import net.emutils.client.emutils.inventory.InventorySortSpeed;
import net.emutils.client.emutils.inventory.SlotLockColor;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.network.chat.Component;

public final class InventoryToolsSettingsScreen extends EMUtilsScreen {
	public InventoryToolsSettingsScreen(Screen parent) {
		super(parent, Component.translatable(EMUtilsTexts.SCREEN_INVENTORY_TOOLS));
	}

	@Override
	protected void initBody() {
		GridLayout.RowHelper adder = initTwoColumnBody();
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_INVENTORY_TOOLS,
			() -> EMUtilsClient.config().inventoryToolsEnabled(),
			EMUtilsClient.config()::setInventoryToolsEnabled
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SLOT_LOCKING,
			() -> EMUtilsClient.config().slotLockingEnabled(),
			EMUtilsClient.config()::setSlotLockingEnabled
		));
		adder.addChild(slotLockColorButton());
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SLOT_BINDING,
			() -> EMUtilsClient.config().slotBindingEnabled(),
			EMUtilsClient.config()::setSlotBindingEnabled
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SLOT_BINDING_LOCK_BOUND_SLOTS,
			() -> EMUtilsClient.config().slotBindingLockBoundSlots(),
			EMUtilsClient.config()::setSlotBindingLockBoundSlots
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_HOVER_TRANSFER,
			() -> EMUtilsClient.config().hoverTransferEnabled(),
			EMUtilsClient.config()::setHoverTransferEnabled
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_HOVER_TRANSFER_GLOBAL,
			() -> EMUtilsClient.config().hoverTransferGlobal(),
			EMUtilsClient.config()::setHoverTransferGlobal
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SORT_BUTTONS,
			() -> EMUtilsClient.config().sortButtonsEnabled(),
			EMUtilsClient.config()::setSortButtonsEnabled
		));
		adder.addChild(sortSpeedButton());
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_QUICK_STACK,
			() -> EMUtilsClient.config().quickStackEnabled(),
			EMUtilsClient.config()::setQuickStackEnabled
		));
		adder.addChild(quickStackSpeedButton());
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_INVENTORY_PREVIEW,
			() -> EMUtilsClient.config().inventoryPreviewEnabled(),
			EMUtilsClient.config()::setInventoryPreviewEnabled
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_PRESERVE_CONTAINER_CURSOR,
			() -> EMUtilsClient.config().preserveContainerCursor(),
			EMUtilsClient.config()::setPreserveContainerCursor
		));
		adder.addChild(fullWidthSettingsButton(Component.translatable("emutils.mass_drop.manage"), button ->
			client.setScreenAndShow(new MassDropScreen(this))
		), SETTINGS_COLUMNS);
		adder.addChild(fullWidthSettingsButton(Component.translatable(EMUtilsTexts.OPTION_RESET_DEFAULTS), button -> {
			EMUtilsClient.config().resetInventoryToolsDefaults();
			client.setScreenAndShow(new InventoryToolsSettingsScreen(parent));
		}), SETTINGS_COLUMNS);
	}

	private static Button slotLockColorButton() {
		return Button.builder(slotLockColorMessage(), button -> {
			SlotLockColor next = EMUtilsClient.config().slotLockColor().next();
			EMUtilsClient.config().setSlotLockColor(next);
			button.setMessage(slotLockColorMessage());
		}).width(SETTINGS_BUTTON_WIDTH).build();
	}

	private static Component slotLockColorMessage() {
		return Component.translatable(
			EMUtilsTexts.OPTION_VALUE,
			Component.translatable(EMUtilsTexts.OPTION_SLOT_LOCK_COLOR),
			Component.translatable(EMUtilsClient.config().slotLockColor().labelKey())
		);
	}

	private static Button sortSpeedButton() {
		return Button.builder(sortSpeedMessage(), button -> {
			InventorySortSpeed next = EMUtilsClient.config().sortSpeed().next();
			EMUtilsClient.config().setSortSpeed(next);
			button.setMessage(sortSpeedMessage());
		}).width(SETTINGS_BUTTON_WIDTH).build();
	}

	private static Component sortSpeedMessage() {
		return Component.translatable(
			EMUtilsTexts.OPTION_VALUE,
			Component.translatable(EMUtilsTexts.OPTION_SORT_SPEED),
			Component.translatable(EMUtilsClient.config().sortSpeed().labelKey())
		);
	}

	private static Button quickStackSpeedButton() {
		return Button.builder(quickStackSpeedMessage(), button -> {
			InventorySortSpeed next = EMUtilsClient.config().quickStackSpeed().next();
			EMUtilsClient.config().setQuickStackSpeed(next);
			button.setMessage(quickStackSpeedMessage());
		}).width(SETTINGS_BUTTON_WIDTH).build();
	}

	private static Component quickStackSpeedMessage() {
		return Component.translatable(
			EMUtilsTexts.OPTION_VALUE,
			Component.translatable(EMUtilsTexts.OPTION_QUICK_STACK_SPEED),
			Component.translatable(EMUtilsClient.config().quickStackSpeed().labelKey())
		);
	}
}
