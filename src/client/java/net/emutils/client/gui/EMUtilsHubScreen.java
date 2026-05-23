package net.emutils.client.gui;

import net.emutils.client.gui.chat.ChatFeaturesSettingsScreen;
import net.emutils.client.gui.death.DeathWaypointSettingsScreen;
import net.emutils.client.gui.reconnect.AutoReconnectSettingsScreen;
import net.emutils.client.gui.screenshot.ScreenshotSettingsScreen;
import net.emutils.client.util.EMUtilsTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.text.Text;
import java.util.function.Function;

public final class EMUtilsHubScreen extends EMUtilsScreen {
	public EMUtilsHubScreen(Screen parent) {
		super(parent, Text.translatable(EMUtilsTexts.HUB_TITLE));
	}

	@Override
	protected void initBody() {
		GridWidget grid = new GridWidget();
		grid.getMainPositioner().marginX(4).marginBottom(4).alignHorizontalCenter();
		GridWidget.Adder adder = grid.createAdder(2);
		adder.add(navButton(EMUtilsTexts.HUB_DEATH_WAYPOINTS, DeathWaypointSettingsScreen::new));
		adder.add(navButton(EMUtilsTexts.HUB_AUTO_RECONNECT, AutoReconnectSettingsScreen::new));
		adder.add(navButton(EMUtilsTexts.HUB_SCREENSHOT_HELPER, ScreenshotSettingsScreen::new));
		adder.add(navButton(EMUtilsTexts.HUB_CHAT_FEATURES, ChatFeaturesSettingsScreen::new));
		layout.addBody(grid);
	}

	private ButtonWidget navButton(String labelKey, Function<Screen, Screen> screenFactory) {
		return ButtonWidget.builder(Text.translatable(labelKey), button -> client.setScreen(screenFactory.apply(this))).build();
	}
}
