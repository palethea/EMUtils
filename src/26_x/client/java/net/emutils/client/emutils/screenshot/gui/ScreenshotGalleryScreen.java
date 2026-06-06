package net.emutils.client.emutils.screenshot.gui;

import net.emutils.client.emutils.screenshot.ScreenshotRepository;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class ScreenshotGalleryScreen extends Screen {
	private final Screen parent;
	private HeaderAndFooterLayout layout;
	private ScreenshotGalleryWidget gallery;

	public ScreenshotGalleryScreen(Screen parent) {
		super(Component.translatable(EMUtilsTexts.SCREEN_SCREENSHOT_GALLERY));
		this.parent = parent;
	}

	@Override
	protected void init() {
		if (gallery != null) {
			gallery.close();
		}

		layout = new HeaderAndFooterLayout(this);
		layout.addTitleHeader(title, font);
		layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(200).build());
		layout.visitWidgets(this::addRenderableWidget);

		gallery = addRenderableWidget(new ScreenshotGalleryWidget(minecraft, width, height));
		gallery.setOnScreenshotsChanged(this::refreshGallery);
		gallery.setScreenshots(ScreenshotRepository.list(minecraft));
		repositionElements();
	}

	private void refreshGallery() {
		if (gallery != null) {
			gallery.setScreenshots(ScreenshotRepository.list(minecraft));
		}
	}

	@Override
	protected void repositionElements() {
		if (layout == null) {
			return;
		}

		layout.arrangeElements();
		if (gallery != null) {
			gallery.setRectangle(width, layout.getContentHeight(), 0, layout.getHeaderHeight());
		}
	}

	@Override
	public void onClose() {
		if (gallery != null) {
			gallery.close();
		}
		minecraft.setScreen(parent);
	}

	@Override
	public void removed() {
	}
}
