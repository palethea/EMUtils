package net.emutils.client.versioned;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import org.jspecify.annotations.Nullable;

/**
 * Minecraft 26.3 vanilla screens whose constructors differ between versions. Every supported version
 * provides this class with the same method signatures.
 */
public final class VersionedScreens {
	private VersionedScreens() {
	}

	/** Minecraft's Options screen. */
	public static Screen options(@Nullable Screen parent, Minecraft client) {
		return new OptionsScreen(parent, client.options);
	}
}
