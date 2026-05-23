package net.emutils.client.modmenu;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public final class EMUtilsSettingsScreen extends Screen {
	private static final int MIN_DELAY_SECONDS = 5;
	private static final int MAX_DELAY_SECONDS = 15;
	private static final int MIN_WAYPOINT_OPACITY = 25;
	private static final int MAX_WAYPOINT_OPACITY = 100;
	private static final int MIN_WAYPOINT_SIZE = 25;
	private static final int MAX_WAYPOINT_SIZE = 100;

	private final Screen parent;

	public EMUtilsSettingsScreen(Screen parent) {
		super(Text.literal("EMUtils Settings"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int centerX = width / 2;
		int y = height / 4 + 24;

		addDrawableChild(ButtonWidget.builder(autoReconnectText(), button -> {
			EMUtilsClient.config().setAutoReconnect(!EMUtilsClient.config().autoReconnect());
			button.setMessage(autoReconnectText());
		}).dimensions(centerX - 100, y, 200, 20).build());

		addDrawableChild(new ReconnectDelaySlider(centerX - 100, y + 28, 200, 20));

		addDrawableChild(ButtonWidget.builder(screenshotHelperText(), button -> {
			EMUtilsClient.config().setScreenshotHelper(!EMUtilsClient.config().screenshotHelper());
			button.setMessage(screenshotHelperText());
		}).dimensions(centerX - 100, y + 56, 200, 20).build());

		addDrawableChild(ButtonWidget.builder(copyChatText(), button -> {
			EMUtilsClient.config().setCopyChat(!EMUtilsClient.config().copyChat());
			button.setMessage(copyChatText());
		}).dimensions(centerX - 100, y + 84, 200, 20).build());

		addDrawableChild(ButtonWidget.builder(deathWaypointText(), button -> {
			EMUtilsClient.config().setDeathWaypoint(!EMUtilsClient.config().deathWaypoint());
			button.setMessage(deathWaypointText());
		}).dimensions(centerX - 100, y + 112, 200, 20).build());

		addDrawableChild(new DeathWaypointOpacitySlider(centerX - 100, y + 140, 200, 20));

		addDrawableChild(new DeathWaypointSizeSlider(centerX - 100, y + 168, 200, 20));

		addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> close())
			.dimensions(centerX - 100, height - 32, 200, 20)
			.build());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 4 - 8, 0xFFFFFF);
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}

	private static Text autoReconnectText() {
		String state = EMUtilsClient.config().autoReconnect() ? "On" : "Off";
		return Text.literal("Auto Reconnect: " + state);
	}

	private static Text screenshotHelperText() {
		String state = EMUtilsClient.config().screenshotHelper() ? "On" : "Off";
		return Text.literal("Screenshot Helper: " + state);
	}

	private static Text copyChatText() {
		String state = EMUtilsClient.config().copyChat() ? "On" : "Off";
		return Text.literal("Copy Chat: " + state);
	}

	private static Text deathWaypointText() {
		String state = EMUtilsClient.config().deathWaypoint() ? "On" : "Off";
		return Text.literal("Death Waypoint: " + state);
	}

	private static final class ReconnectDelaySlider extends SliderWidget {
		private ReconnectDelaySlider(int x, int y, int width, int height) {
			super(x, y, width, height, Text.empty(), valueFromDelay());
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(Text.literal("Retry Delay: " + delaySeconds() + "s"));
		}

		@Override
		protected void applyValue() {
			EMUtilsClient.config().setReconnectDelaySeconds(delaySeconds());
		}

		private int delaySeconds() {
			return MIN_DELAY_SECONDS + (int) Math.round(value * (MAX_DELAY_SECONDS - MIN_DELAY_SECONDS));
		}

		private static double valueFromDelay() {
			int delay = EMUtilsClient.config().reconnectDelaySeconds();
			return (delay - MIN_DELAY_SECONDS) / (double) (MAX_DELAY_SECONDS - MIN_DELAY_SECONDS);
		}
	}

	private static final class DeathWaypointOpacitySlider extends SliderWidget {
		private DeathWaypointOpacitySlider(int x, int y, int width, int height) {
			super(x, y, width, height, Text.empty(), valueFromOpacity());
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(Text.literal("Waypoint Opacity: " + opacityPercent() + "%"));
		}

		@Override
		protected void applyValue() {
			EMUtilsClient.config().setDeathWaypointOpacity(opacityPercent());
		}

		private int opacityPercent() {
			return MIN_WAYPOINT_OPACITY + (int) Math.round(value * (MAX_WAYPOINT_OPACITY - MIN_WAYPOINT_OPACITY));
		}

		private static double valueFromOpacity() {
			int opacity = EMUtilsClient.config().deathWaypointOpacity();
			return (opacity - MIN_WAYPOINT_OPACITY) / (double) (MAX_WAYPOINT_OPACITY - MIN_WAYPOINT_OPACITY);
		}
	}

	private static final class DeathWaypointSizeSlider extends SliderWidget {
		private DeathWaypointSizeSlider(int x, int y, int width, int height) {
			super(x, y, width, height, Text.empty(), valueFromSize());
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(Text.literal("Waypoint Size: " + sizePercent() + "%"));
		}

		@Override
		protected void applyValue() {
			EMUtilsClient.config().setDeathWaypointSize(sizePercent());
		}

		private int sizePercent() {
			return MIN_WAYPOINT_SIZE + (int) Math.round(value * (MAX_WAYPOINT_SIZE - MIN_WAYPOINT_SIZE));
		}

		private static double valueFromSize() {
			int size = EMUtilsClient.config().deathWaypointSize();
			return (size - MIN_WAYPOINT_SIZE) / (double) (MAX_WAYPOINT_SIZE - MIN_WAYPOINT_SIZE);
		}
	}
}
