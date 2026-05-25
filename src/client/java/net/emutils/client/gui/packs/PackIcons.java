package net.emutils.client.gui.packs;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.gui.screenshot.GalleryIcons;
import net.minecraft.util.Identifier;

public final class PackIcons {
	public static final int SIZE = GalleryIcons.SIZE;
	public static final Identifier DOWNLOAD = icon("download");
	public static final Identifier SEARCH = icon("search");
	public static final Identifier REFRESH = icon("refresh");
	public static final Identifier ENABLE = icon("enable");
	public static final Identifier DISABLE = icon("disable");
	public static final Identifier RESOURCE_PACK = icon("resource_pack");
	public static final Identifier SHADER = icon("shader");
	public static final Identifier DELETE = GalleryIcons.DELETE;

	private PackIcons() {
	}

	private static Identifier icon(String name) {
		return Identifier.of(EMUtilsClient.MOD_ID, "textures/gui/packs/" + name + ".png");
	}
}
