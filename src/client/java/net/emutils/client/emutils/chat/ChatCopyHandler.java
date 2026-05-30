package net.emutils.client.emutils.chat;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emhelpers.text.EmUtilsChatPrefix;
import net.emutils.client.emhelpers.util.EMUtilsTexts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jspecify.annotations.Nullable;

public final class ChatCopyHandler {

    private ChatCopyHandler() {}

    public static boolean tryCopyClickedMessage(
        MinecraftClient client,
        Click click
    ) {
        if (!EMUtilsClient.config().copyChat() || client.inGameHud == null) {
            return false;
        }

        ChatHud chatHud = client.inGameHud.getChatHud();
        if (!(chatHud instanceof ChatHudAccess access)) {
            return false;
        }

        Text message = access.emutils$getMessageAt(click.x(), click.y());
        if (message == null) {
            return false;
        }

        String copiedText = formatForClipboard(message);
        if (copiedText.isEmpty()) {
            return false;
        }

        client.keyboard.setClipboard(copiedText);
        if (EMUtilsClient.config().copyChatFeedback()) {
            chatHud.addMessage(
                EmUtilsChatPrefix.chat(
                    Text.translatable(EMUtilsTexts.CHAT_COPY_SUCCESS).formatted(
                        Formatting.GREEN
                    )
                ),
                null,
                null
            );
        }
        return true;
    }

    @Nullable
    public static String messageAt(
        MinecraftClient client,
        double mouseX,
        double mouseY
    ) {
        if (client.inGameHud == null) {
            return null;
        }

        ChatHud chatHud = client.inGameHud.getChatHud();
        if (!(chatHud instanceof ChatHudAccess access)) {
            return null;
        }

        Text message = access.emutils$getMessageAt(mouseX, mouseY);
        return message == null ? null : formatForClipboard(message);
    }

    private static String formatForClipboard(Text message) {
        if (EMUtilsClient.config().copyChatFormatting()) {
            return ChatLegacyFormatting.toAmpersandString(message);
        }

        return message.getString();
    }
}
