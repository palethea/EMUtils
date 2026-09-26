package net.emutils.client.emutils.packs.gui;

import net.emutils.client.EMUtilsClient;
import net.minecraft.resources.Identifier;

public final class PackIcons {
	public static final Identifier RESOURCE_PACK = icon("resource_pack");
	public static final Identifier SHADER = icon("shader");

	private PackIcons() {
	}

	private static Identifier icon(String name) {
		return Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "textures/gui/packs/" + name + ".png");
	}
}
