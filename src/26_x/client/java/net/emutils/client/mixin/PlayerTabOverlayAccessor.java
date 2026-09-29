package net.emutils.client.mixin;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The header and footer the server sets on the tab list, which vanilla only lets its own list read (#170). */
@Mixin(PlayerTabOverlay.class)
public interface PlayerTabOverlayAccessor {
	@Accessor("header")
	@Nullable Component emutils$getHeader();

	@Accessor("footer")
	@Nullable Component emutils$getFooter();
}
