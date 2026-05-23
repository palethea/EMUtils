package net.emutils.client.chat;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jspecify.annotations.Nullable;

public final class ChatCopyHandler {
	private static final Text COPIED_FEEDBACK = Text.literal("EMUtils copied chat message.").formatted(Formatting.GRAY);

	private ChatCopyHandler() {
	}

	public static boolean tryCopyClickedMessage(MinecraftClient client, Click click) {
		if (!EMUtilsClient.config().copyChat() || client.inGameHud == null) {
			return false;
		}

		ChatHud chatHud = client.inGameHud.getChatHud();
		if (!(chatHud instanceof ChatHudAccess access)) {
			return false;
		}

		String copiedText = access.emutils$getMessageAt(click.x(), click.y());
		if (copiedText == null || copiedText.isEmpty()) {
			return false;
		}

		client.keyboard.setClipboard(copiedText);
		chatHud.addMessage(COPIED_FEEDBACK, null, null);
		return true;
	}

	@Nullable
	public static String messageAt(MinecraftClient client, double mouseX, double mouseY) {
		if (client.inGameHud == null) {
			return null;
		}

		ChatHud chatHud = client.inGameHud.getChatHud();
		if (!(chatHud instanceof ChatHudAccess access)) {
			return null;
		}

		return access.emutils$getMessageAt(mouseX, mouseY);
	}
}
