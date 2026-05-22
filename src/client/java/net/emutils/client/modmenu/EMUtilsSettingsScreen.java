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
}
