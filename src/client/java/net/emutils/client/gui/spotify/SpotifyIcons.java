package net.emutils.client.gui.spotify;

import net.emutils.client.EMUtilsClient;
import net.minecraft.util.Identifier;

public final class SpotifyIcons {
	public static final int SIZE = 16;
	public static final Identifier PREVIOUS = icon("previous");
	public static final Identifier PLAY = icon("play");
	public static final Identifier PAUSE = icon("pause");
	public static final Identifier NEXT = icon("next");
	public static final Identifier FALLBACK_ART = icon("fallback_art");

	private SpotifyIcons() {
	}

	private static Identifier icon(String name) {
		return Identifier.of(EMUtilsClient.MOD_ID, "textures/gui/spotify/" + name + ".png");
	}
}
