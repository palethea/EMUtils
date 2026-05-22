package net.emutils.client.mixin;

import java.io.File;
import java.util.function.Consumer;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.screenshot.ScreenshotMessage;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.client.util.ScreenshotRecorder")
public abstract class ScreenshotRecorderMixin {
	@Redirect(method = "method_22691", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V", ordinal = 0))
	private static void emutils$replaceSuccessMessage(Consumer<Text> consumer, Object message, NativeImage image, File screenshot, Consumer<Text> originalConsumer) {
		consumer.accept(EMUtilsClient.config().screenshotHelper() ? ScreenshotMessage.saved(screenshot) : (Text) message);
	}
}
