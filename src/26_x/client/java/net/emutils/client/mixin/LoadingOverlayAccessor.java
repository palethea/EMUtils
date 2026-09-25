package net.emutils.client.mixin;

import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets the EMUtils loading card take over a resource reload Minecraft started with its own loading screen. */
@Mixin(LoadingOverlay.class)
public interface LoadingOverlayAccessor {
	@Accessor("reload")
	ReloadInstance emutils$reload();

	@Accessor("onFinish")
	Consumer<Optional<Throwable>> emutils$onFinish();
}
