package net.emutils.client.mixin;

import com.mojang.blaze3d.platform.FramerateLimitTracker;
import net.emutils.client.emutils.compat.MinecraftClientCompat;
import net.emutils.client.emutils.gui.ui.UiPanelScreen;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla caps the frame rate at 60 FPS while any screen is open outside a world, which made the EMUtils
 * menus' animations look choppy on the title screen next to how they run in a world (#162). EMUtils menus
 * use the player's Max Framerate there too; vanilla menus keep the cap, and the AFK and minimized limits
 * still apply, since those are other throttle reasons.
 */
@Mixin(FramerateLimitTracker.class)
public abstract class FramerateLimitTrackerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	private int framerateLimit;

	@Shadow
	public abstract FramerateLimitTracker.FramerateThrottleReason getThrottleReason();

	@Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
	private void emutils$fullRateInMenus(CallbackInfoReturnable<Integer> cir) {
		if (getThrottleReason() == FramerateLimitTracker.FramerateThrottleReason.OUT_OF_LEVEL_MENU
			&& MinecraftClientCompat.screen(minecraft) instanceof UiPanelScreen) {
			cir.setReturnValue(framerateLimit);
		}
	}
}
