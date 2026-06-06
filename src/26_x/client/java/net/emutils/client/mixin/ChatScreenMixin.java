package net.emutils.client.mixin;

import net.emutils.client.emutils.chat.ChatCopyHandler;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void emutils$copyChatOnCtrlMouseButtonEvent(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
		if (click.button() != 0 || !click.hasControlDownWithQuirk()) {
			return;
		}

		net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
		if (ChatCopyHandler.tryCopyMouseButtonEventedMessage(client, click)) {
			cir.setReturnValue(true);
			cir.cancel();
		}
	}
}
