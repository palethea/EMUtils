package net.emutils.client.mixin;

import java.io.File;
import java.util.Optional;
import net.emutils.client.screenshot.ScreenshotClipboard;
import net.emutils.client.screenshot.ScreenshotMessage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
	@Inject(method = "handleClickEvent", at = @At("HEAD"), cancellable = true)
	private static void emutils$handleCopyScreenshot(ClickEvent event, MinecraftClient client, Screen screen, CallbackInfo ci) {
		if (!(event instanceof ClickEvent.Custom custom) || !ScreenshotMessage.COPY_SCREENSHOT_ACTION.equals(custom.id())) {
			return;
		}

		ci.cancel();
		custom.payload()
			.flatMap(element -> element instanceof NbtString string ? string.asString() : Optional.empty())
			.ifPresentOrElse(path -> {
				boolean copied = ScreenshotClipboard.copyImage(new File(path));
				Text message = copied
					? Text.literal("EMUtils copied screenshot to clipboard.").formatted(Formatting.GREEN)
					: Text.literal("EMUtils could not copy screenshot.").formatted(Formatting.RED);
				client.inGameHud.getChatHud().addMessage(message);
			}, () -> client.inGameHud.getChatHud().addMessage(Text.literal("EMUtils could not copy screenshot.").formatted(Formatting.RED)));
	}
}
