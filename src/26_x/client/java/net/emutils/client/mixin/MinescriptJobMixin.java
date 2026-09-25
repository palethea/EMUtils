package net.emutils.client.mixin;

import net.emutils.client.emutils.compat.MinescriptCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hands every line a Minescript script writes to stderr to EMUtils, so the Script Manager can show why
 * a run failed (#125) instead of leaving it in chat behind the screen. Minescript is optional and not
 * on the compile classpath, hence {@code @Pseudo}; {@link EMUtilsMixinPlugin} only applies this when
 * Minescript is loaded.
 */
@Pseudo
@Mixin(targets = "net.minescript.common.Job", remap = false)
public abstract class MinescriptJobMixin {
	@Inject(method = "processStderr(Ljava/lang/String;)V", at = @At("HEAD"), remap = false)
	private void emutils$captureStderr(String line, CallbackInfo callback) {
		MinescriptCompat.onJobStderr(this, line);
	}
}
