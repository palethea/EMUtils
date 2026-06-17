package net.emutils.client.mixin;

import java.io.File;
import java.util.Optional;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.waypoint.WaypointClickHandler;
import net.emutils.client.emutils.waypoint.WaypointMessage;
import net.emutils.client.emutils.screenshot.ScreenshotActions;
import net.emutils.client.emutils.screenshot.ScreenshotMessage;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class ScreenMixin {
	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void emutils$openHudLayoutEditorFromScreenKey(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
		if (EMUtilsClient.tryOpenHudLayoutEditor(input)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "defaultHandleClickEvent", at = @At("HEAD"), cancellable = true)
	private static void emutils$handleCustomClickEvents(ClickEvent event, Minecraft client, Screen screen, CallbackInfo ci) {
		if (emutils$tryHandleCustomClickEvent(event, client)) {
			ci.cancel();
		}
	}

	@Inject(method = "defaultHandleGameClickEvent", at = @At("HEAD"), cancellable = true)
	private static void emutils$handleCustomGameClickEvents(ClickEvent event, Minecraft client, Screen screen, CallbackInfo ci) {
		if (emutils$tryHandleCustomClickEvent(event, client)) {
			ci.cancel();
		}
	}

	private static boolean emutils$tryHandleCustomClickEvent(ClickEvent event, Minecraft client) {
		if (!(event instanceof ClickEvent.Custom custom)) {
			return false;
		}

		if (WaypointClickHandler.tryHandle(custom.id(), custom, client)) {
			return true;
		}

		if (!ScreenshotMessage.COPY_SCREENSHOT_ACTION.equals(custom.id())) {
			return false;
		}

		custom.payload()
			.flatMap(element -> element instanceof StringTag string ? string.asString() : Optional.empty())
			.ifPresentOrElse(
				path -> ScreenshotActions.copyWithFeedback(client, new File(path)),
				() -> client.gui.hud.getChat().addClientSystemMessage(Component.translatable(EMUtilsTexts.CHAT_SCREENSHOT_COPY_FAILURE).withStyle(ChatFormatting.RED))
			);
		return true;
	}
}
