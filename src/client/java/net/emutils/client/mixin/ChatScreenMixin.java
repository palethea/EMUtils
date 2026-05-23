package net.emutils.client.mixin;

import net.emutils.client.chat.ChatCopyHandler;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void emutils$copyChatOnCtrlClick(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
		if (click.button() != 0 || !click.hasCtrl()) {
			return;
		}

		net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
		if (ChatCopyHandler.tryCopyClickedMessage(client, click)) {
			cir.setReturnValue(true);
			cir.cancel();
		}
	}
}
