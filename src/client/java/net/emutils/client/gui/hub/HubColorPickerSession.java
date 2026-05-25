package net.emutils.client.gui.hub;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.client.MinecraftClient;

public final class HubColorPickerSession {
	private static final int POPUP_SIZE = 132;
	private static final int HUE_RING_DISTANCE = (HubColorPickerGraphics.WHEEL_INNER_RADIUS + HubColorPickerGraphics.WHEEL_OUTER_RADIUS) / 2;

	private final IntSupplier getter;
	private final IntConsumer setter;
	private final int anchorX;
	private final int anchorY;
	private final HubColorPickerGraphics.SaturationValueField saturationValueField;
	private float hue;
	private float saturation;
	private float brightness;
	private boolean draggingWheel;
	private boolean draggingInner;
	private boolean dirty;

	public HubColorPickerSession(IntSupplier getter, IntConsumer setter, int anchorX, int anchorY) {
		this.getter = getter;
		this.setter = setter;
		this.anchorX = anchorX;
		this.anchorY = anchorY;
		MinecraftClient client = MinecraftClient.getInstance();
		this.saturationValueField = new HubColorPickerGraphics.SaturationValueField(client);
		int color = getter.getAsInt();
		float[] hsv = HubColorMath.rgbToHsv((color >>> 16) & 0xFF, (color >>> 8) & 0xFF, color & 0xFF);
		this.hue = hsv[0];
		this.saturation = hsv[1];
		this.brightness = hsv[2];
	}

	public void close() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null) {
			saturationValueField.close(client);
		}
	}

	public int popupX(int screenWidth) {
		int x = anchorX - POPUP_SIZE;
		return Math.max(8, Math.min(x, screenWidth - POPUP_SIZE - 8));
	}

	public int popupY(int screenHeight) {
		int y = anchorY - POPUP_SIZE / 2;
		return Math.max(8, Math.min(y, screenHeight - POPUP_SIZE - 8));
	}

	public int previewColor() {
		int alpha = (getter.getAsInt() >>> 24) & 0xFF;
		if (alpha == 0) {
			alpha = 0xAA;
		}

		int rgb = HubColorMath.hsvToRgb(hue, saturation, brightness);
		return (alpha << 24) | (rgb & 0x00FFFFFF);
	}

	public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY) {
		MinecraftClient client = MinecraftClient.getInstance();
		HubColorPickerGraphics.ensureHueWheel(client);

		int left = popupX(context.getScaledWindowWidth());
		int top = popupY(context.getScaledWindowHeight());
		int right = left + POPUP_SIZE;
		int bottom = top + POPUP_SIZE;
		int centerX = left + POPUP_SIZE / 2;
		int centerY = top + POPUP_SIZE / 2;

		HubRoundedGraphics.drawRoundedRect(context, left - 2, top - 2, right + 2, bottom + 2, HubPanelTheme.BORDER, HubRoundedGraphics.RADIUS_MD);
		HubRoundedGraphics.drawRoundedRect(context, left, top, right, bottom, HubPanelTheme.SURFACE, HubRoundedGraphics.RADIUS_MD);

		HubColorPickerGraphics.drawHueWheel(context, centerX, centerY);
		saturationValueField.ensure(hue, client);
		saturationValueField.draw(context, centerX, centerY);
		drawPickerHandle(context, centerX, centerY);

		int preview = previewColor();
		HubRoundedGraphics.drawRoundedRect(context, right - 22, top + 6, right - 6, top + 22, HubPanelTheme.BORDER, HubRoundedGraphics.RADIUS_SM);
		HubRoundedGraphics.drawRoundedRect(context, right - 21, top + 7, right - 7, top + 21, preview, HubRoundedGraphics.RADIUS_SM);
	}

	private void drawPickerHandle(net.minecraft.client.gui.DrawContext context, int centerX, int centerY) {
		int[] huePoint = pointFromHue(hue, HUE_RING_DISTANCE, centerX, centerY);
		drawHandle(context, huePoint[0], huePoint[1]);

		double[] innerPoint = HubColorMath.saturationBrightnessToDisc(
			saturation,
			brightness,
			HubColorPickerGraphics.INNER_RADIUS
		);
		drawHandle(context, centerX + (int) Math.round(innerPoint[0]), centerY + (int) Math.round(innerPoint[1]));
	}

	private static void drawHandle(net.minecraft.client.gui.DrawContext context, int x, int y) {
		HubRoundedGraphics.drawCircle(context, x, y, 9, HubPanelTheme.BORDER);
		HubRoundedGraphics.drawCircle(context, x, y, 7, HubPanelTheme.TEXT_PRIMARY);
	}

	public boolean contains(double mouseX, double mouseY, int screenWidth, int screenHeight) {
		int left = popupX(screenWidth);
		int top = popupY(screenHeight);
		return mouseX >= left && mouseX < left + POPUP_SIZE && mouseY >= top && mouseY < top + POPUP_SIZE;
	}

	public boolean handleClick(double mouseX, double mouseY, int screenWidth, int screenHeight) {
		int left = popupX(screenWidth);
		int top = popupY(screenHeight);
		int centerX = left + POPUP_SIZE / 2;
		int centerY = top + POPUP_SIZE / 2;
		double localX = mouseX - centerX;
		double localY = mouseY - centerY;
		double distance = Math.sqrt(localX * localX + localY * localY);

		if (distance >= HubColorPickerGraphics.WHEEL_INNER_RADIUS && distance <= HubColorPickerGraphics.WHEEL_OUTER_RADIUS) {
			draggingWheel = true;
			draggingInner = false;
			applyWheel(localX, localY);
			return true;
		}

		if (distance <= HubColorPickerGraphics.INNER_RADIUS) {
			draggingInner = true;
			draggingWheel = false;
			applyInner(localX, localY);
			return true;
		}

		return false;
	}

	public boolean handleDrag(double mouseX, double mouseY, int screenWidth, int screenHeight) {
		if (!draggingWheel && !draggingInner) {
			return false;
		}

		int left = popupX(screenWidth);
		int top = popupY(screenHeight);
		int centerX = left + POPUP_SIZE / 2;
		int centerY = top + POPUP_SIZE / 2;
		double localX = mouseX - centerX;
		double localY = mouseY - centerY;

		if (draggingWheel) {
			applyWheel(localX, localY);
		} else {
			applyInner(localX, localY);
		}

		return true;
	}

	public void release() {
		if (dirty) {
			commit();
		}

		draggingWheel = false;
		draggingInner = false;
	}

	private void applyWheel(double localX, double localY) {
		hue = HubColorMath.hueFromPoint(localX, localY);
		markDirty();
	}

	private void applyInner(double localX, double localY) {
		float[] values = HubColorMath.discToSaturationBrightness(
			localX,
			localY,
			HubColorPickerGraphics.INNER_RADIUS
		);
		saturation = values[0];
		brightness = values[1];
		markDirty();
	}

	private void markDirty() {
		dirty = true;
	}

	private void commit() {
		setter.accept(previewColor());
		dirty = false;
	}

	private static int[] pointFromHue(float hueValue, int radius, int centerX, int centerY) {
		double angle = hueValue * Math.PI * 2.0;
		int x = centerX + (int) Math.round(Math.cos(angle) * radius);
		int y = centerY - (int) Math.round(Math.sin(angle) * radius);
		return new int[] { x, y };
	}
}
