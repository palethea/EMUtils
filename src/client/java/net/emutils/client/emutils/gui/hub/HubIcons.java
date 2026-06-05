package net.emutils.client.emutils.gui.hub;

import net.emutils.client.EMUtilsClient;
import net.minecraft.util.Identifier;

public final class HubIcons {
	public static final Identifier APPLE = icon("apple");
	public static final Identifier BACKPACK = icon("backpack");
	public static final Identifier BOX = icon("box");
	public static final Identifier CHEVRON_DOWN = icon("chevron-down");
	public static final Identifier CHEVRON_UP = icon("chevron-up");
	public static final Identifier CLOUD_OFF = icon("cloud-off");
	public static final Identifier CLOUD_SUN = icon("cloud-sun");
	public static final Identifier DROPLETS = icon("droplets");
	public static final Identifier EYE = icon("eye");
	public static final Identifier FLAME = icon("flame");
	public static final Identifier FOLDER_COG = icon("folder-cog");
	public static final Identifier IMAGE = icon("image");
	public static final Identifier MAP_PIN = icon("map-pin");
	public static final Identifier MESSAGE_SQUARE = icon("message-square");
	public static final Identifier MONITOR = icon("monitor");
	public static final Identifier MUSIC = icon("music");
	public static final Identifier PACKAGE = icon("package");
	public static final Identifier PACKAGE_OPEN = icon("package-open");
	public static final Identifier REFRESH_CW = icon("refresh-cw");
	public static final Identifier SEARCH = icon("search");
	public static final Identifier SHIELD = icon("shield");
	public static final Identifier SHIRT = icon("shirt");
	public static final Identifier SPARKLES = icon("sparkles");
	public static final Identifier SUN = icon("sun");
	public static final Identifier TAG = icon("tag");
	public static final Identifier WRENCH = icon("wrench");
	public static final Identifier ZOOM_IN = icon("zoom-in");

	private HubIcons() {
	}

	private static Identifier icon(String name) {
		return Identifier.of(EMUtilsClient.MOD_ID, "textures/gui/hub/icons/" + name + ".png");
	}
}
