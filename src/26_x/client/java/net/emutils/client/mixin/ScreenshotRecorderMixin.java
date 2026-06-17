package net.emutils.client.mixin;

import java.io.File;
import java.util.function.Consumer;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.screenshot.ScreenshotAutoCopy;
import net.emutils.client.emutils.screenshot.ScreenshotMetadataSaver;
import net.emutils.client.emutils.screenshot.ScreenshotMessage;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Screenshot.class)
public abstract class ScreenshotRecorderMixin {
	@Redirect(method = "lambda$grab$3", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V", ordinal = 0), require = 0)
	private static void emutils$replaceSuccessMessage(Consumer<Component> consumer, Object message, NativeImage image, File screenshot, Consumer<Component> originalConsumer) {
		consumer.accept(EMUtilsClient.config().screenshotHelper() ? ScreenshotMessage.saved(screenshot) : (Component) message);
		ScreenshotMetadataSaver.trySave(screenshot);
		ScreenshotAutoCopy.tryCopy(screenshot);
	}
}
