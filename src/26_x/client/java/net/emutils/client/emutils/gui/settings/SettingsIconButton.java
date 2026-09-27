package net.emutils.client.emutils.gui.settings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * The EMUtils icon among the small icon buttons of the pause menu and the title screen (#160): a 20×20
 * button with a 15×15 pixel-art sprite in the vanilla style, which opens the settings.
 */
public final class SettingsIconButton {
	public static final int SIZE = 20;
	private static final int ICON = 15;
	/** The gap vanilla leaves between its small icon buttons. */
	private static final int GAP = 4;
	private static final Identifier SPRITE = Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "pause_menu/emutils");

	private SettingsIconButton() {
	}

	public static SpriteIconButton create(Screen parent) {
		return SpriteIconButton.builder(
			Component.translatable(EMUtilsTexts.HUB_TITLE),
			button -> Minecraft.getInstance().gui.setScreen(new SettingsScreen(parent)),
			true
		).width(SIZE).sprite(SPRITE, ICON, ICON).withTootip().build();
	}

	/** Whether this is the EMUtils icon; used by UI snapshots. */
	public static boolean is(GuiEventListener child) {
		return child instanceof SpriteIconButton icon && icon.getMessage().getString().equals(Component.translatable(EMUtilsTexts.HUB_TITLE).getString());
	}

	/**
	 * Centers the row of small icon buttons at {@code rowY} on the screen again, with {@code first} at its
	 * start: the title screen places its icons by hand for exactly its own three, and other mods add theirs
	 * after it. Every 20×20 widget on that line counts as part of the row, in the order they stand.
	 */
	public static void recenterRow(Screen screen, List<? extends GuiEventListener> children, AbstractWidget first, int rowY) {
		List<AbstractWidget> row = new ArrayList<>();
		for (GuiEventListener child : children) {
			if (child != first && child instanceof AbstractWidget widget && widget.getY() == rowY && widget.getWidth() == SIZE && widget.getHeight() == SIZE) {
				row.add(widget);
			}
		}
		row.sort(Comparator.comparingInt(AbstractWidget::getX));
		row.addFirst(first);
		int total = row.size() * SIZE + (row.size() - 1) * GAP;
		int x = screen.width / 2 - total / 2;
		for (AbstractWidget widget : row) {
			widget.setPosition(x, rowY);
			x += SIZE + GAP;
		}
	}
}
