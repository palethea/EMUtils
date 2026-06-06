package net.emutils.client.mixin;

import java.io.File;
import java.util.Optional;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.waypoint.WaypointClickHandler;
import net.emutils.client.emutils.waypoint.WaypointMessage;
import net.emutils.client.emutils.screenshot.ScreenshotActions;
import net.emutils.client.emutils.screenshot.ScreenshotMessage;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class ScreenMixin {
	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void emutils$openHudLayoutEditorFromScreenKey(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
		if (EMUtilsClient.tryOpenHudLayoutEditor(input)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "handleClickEvent", at = @At("HEAD"), cancellable = true)
	private static void emutils$handleCustomClickEvents(ClickEvent event, MinecraftClient client, Screen screen, CallbackInfo ci) {
		if (!(event instanceof ClickEvent.Custom custom)) {
			return;
		}

		if (WaypointClickHandler.tryHandle(custom.id(), custom, client)) {
			ci.cancel();
			return;
		}

		if (!ScreenshotMessage.COPY_SCREENSHOT_ACTION.equals(custom.id())) {
			return;
		}

		ci.cancel();
		custom.payload()
			.flatMap(element -> element instanceof NbtString string ? string.asString() : Optional.empty())
			.ifPresentOrElse(
				path -> ScreenshotActions.copyWithFeedback(client, new File(path)),
				() -> client.inGameHud.getChatHud().addMessage(Text.translatable(EMUtilsTexts.CHAT_SCREENSHOT_COPY_FAILURE).formatted(Formatting.RED), null, null)
			);
	}
}
