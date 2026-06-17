package net.emutils.client.emutils.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public abstract class EMUtilsScreen extends Screen {
	public static final int SETTINGS_COLUMNS = 2;
	public static final int SETTINGS_BUTTON_WIDTH = 200;

	protected final Screen parent;
	protected final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
	protected final Minecraft client = Minecraft.getInstance();
	protected Font textRenderer;

	protected EMUtilsScreen(Screen parent, Component title) {
		super(title);
		this.parent = parent;
	}

	@Override
	protected void init() {
		textRenderer = font;
		layout.addTitleHeader(title, font);
		initBody();
		layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(200).build());
		layout.visitWidgets(this::addRenderableWidget);
		repositionElements();
	}

	protected abstract void initBody();

	protected LinearLayout createVerticalBody() {
		LinearLayout body = LinearLayout.vertical().spacing(8);
		body.defaultCellSetting().alignHorizontallyCenter();
		return body;
	}

	protected GridLayout.RowHelper initTwoColumnBody() {
		GridLayout grid = new GridLayout();
		grid.spacing(4);
		grid.defaultCellSetting().alignHorizontallyCenter();
		layout.addToContents(grid);
		return grid.createRowHelper(SETTINGS_COLUMNS);
	}

	protected static Button fullWidthSettingsButton(Component message, Button.OnPress action) {
		return Button.builder(message, action).width(SETTINGS_BUTTON_WIDTH * SETTINGS_COLUMNS + 4).build();
	}

	@Override
	protected void repositionElements() {
		layout.arrangeElements();
	}

	@Override
	public void onClose() {
		minecraft.setScreenAndShow(parent);
	}
}
