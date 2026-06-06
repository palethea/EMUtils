package net.emutils.client.emutils.minescript.gui;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.packs.gui.PackIcons;
import net.emutils.client.emutils.screenshot.gui.GalleryIcons;
import net.minecraft.resources.Identifier;

public final class ScriptIcons {
	public static final int SIZE = GalleryIcons.SIZE;
	public static final Identifier OPEN_FOLDER = GalleryIcons.FOLDER;
	public static final Identifier NEW_SCRIPT = icon("new_script");
	public static final Identifier REFRESH = icon("refresh");
	public static final Identifier RUN = PackIcons.ENABLE;
	public static final Identifier SAVE = GalleryIcons.COPY;
	public static final Identifier KEYBIND = icon("keybind");
	public static final Identifier CLEAR_KEYBIND = icon("clear_keybind");
	public static final Identifier DELETE = GalleryIcons.DELETE;

	private ScriptIcons() {
	}

	private static Identifier icon(String name) {
		return Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "textures/gui/scripts/" + name + ".png");
	}
}
