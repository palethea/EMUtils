package net.emutils.client.emutils.screenshot.gui;

import net.emutils.client.EMUtilsClient;
import net.minecraft.resources.Identifier;

public final class GalleryIcons {
	public static final int SIZE = 16;
	public static final Identifier COPY = icon("copy");
	public static final Identifier OPEN = icon("open");
	public static final Identifier FOLDER = icon("folder");
	public static final Identifier DELETE = icon("delete");

	private GalleryIcons() {
	}

	private static Identifier icon(String name) {
		return Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "textures/gui/gallery/" + name + ".png");
	}
}
