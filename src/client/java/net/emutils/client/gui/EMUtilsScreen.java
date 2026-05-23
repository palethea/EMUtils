package net.emutils.client.gui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public abstract class EMUtilsScreen extends Screen {
	public static final int SETTINGS_COLUMNS = 2;
	public static final int SETTINGS_BUTTON_WIDTH = 200;

	protected final Screen parent;
	protected final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);

	protected EMUtilsScreen(Screen parent, Text title) {
		super(title);
		this.parent = parent;
	}

	@Override
	protected void init() {
		layout.addHeader(title, textRenderer);
		initBody();
		layout.addFooter(ButtonWidget.builder(ScreenTexts.DONE, button -> close()).width(200).build());
		layout.forEachChild(child -> {
			ClickableWidget widget = addDrawableChild(child);
		});
		refreshWidgetPositions();
	}

	protected abstract void initBody();

	protected DirectionalLayoutWidget createVerticalBody() {
		DirectionalLayoutWidget body = DirectionalLayoutWidget.vertical().spacing(8);
		body.getMainPositioner().alignHorizontalCenter();
		return body;
	}

	protected GridWidget.Adder initTwoColumnBody() {
		GridWidget grid = new GridWidget();
		grid.setSpacing(4);
		grid.getMainPositioner().alignHorizontalCenter();
		layout.addBody(grid);
		return grid.createAdder(SETTINGS_COLUMNS);
	}

	protected static ButtonWidget fullWidthSettingsButton(Text message, ButtonWidget.PressAction action) {
		return ButtonWidget.builder(message, action).width(SETTINGS_BUTTON_WIDTH * SETTINGS_COLUMNS + 4).build();
	}

	@Override
	protected void refreshWidgetPositions() {
		layout.refreshPositions();
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}
}
