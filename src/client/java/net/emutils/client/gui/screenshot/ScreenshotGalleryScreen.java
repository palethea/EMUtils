package net.emutils.client.gui.screenshot;

import net.emutils.client.screenshot.ScreenshotRepository;
import net.emutils.client.util.EMUtilsTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public final class ScreenshotGalleryScreen extends Screen {
	private final Screen parent;
	private ThreePartsLayoutWidget layout;
	private ScreenshotGalleryWidget gallery;

	public ScreenshotGalleryScreen(Screen parent) {
		super(Text.translatable(EMUtilsTexts.SCREEN_SCREENSHOT_GALLERY));
		this.parent = parent;
	}

	@Override
	protected void init() {
		if (gallery != null) {
			gallery.close();
		}

		layout = new ThreePartsLayoutWidget(this);
		layout.addHeader(title, textRenderer);
		layout.addFooter(ButtonWidget.builder(ScreenTexts.DONE, button -> close()).width(200).build());
		layout.forEachChild(this::addDrawableChild);

		gallery = addDrawableChild(new ScreenshotGalleryWidget(client, width, height));
		gallery.setOnScreenshotsChanged(this::refreshGallery);
		gallery.setScreenshots(ScreenshotRepository.list(client));
		refreshWidgetPositions();
	}

	private void refreshGallery() {
		if (gallery != null) {
			gallery.setScreenshots(ScreenshotRepository.list(client));
		}
	}

	@Override
	protected void refreshWidgetPositions() {
		if (layout == null) {
			return;
		}

		layout.refreshPositions();
		if (gallery != null) {
			gallery.position(width, layout.getContentHeight(), 0, layout.getHeaderHeight());
		}
	}

	@Override
	public void close() {
		if (gallery != null) {
			gallery.close();
		}
		client.setScreen(parent);
	}

	@Override
	public void removed() {
	}
}
