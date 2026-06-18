package net.emutils.client.emutils.chat;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.text.EmUtilsChatPrefix;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.jspecify.annotations.Nullable;

public final class ChatCopyHandler {

    private ChatCopyHandler() {}

    public static boolean tryCopyMouseButtonEventedMessage(
        Minecraft client,
        MouseButtonEvent click
    ) {
        if (!EMUtilsClient.config().copyChat() || client.gui == null) {
            return false;
        }

        ChatComponent chatHud = client.gui.hud.getChat();
        if (!(chatHud instanceof ChatHudAccess access)) {
            return false;
        }

        Component message = access.emutils$getMessageAt(click.x(), click.y());
        if (message == null) {
            return false;
        }

        String copiedText = formatForClipboard(message);
        if (copiedText.isEmpty()) {
            return false;
        }

        client.keyboardHandler.setClipboard(copiedText);
        if (EMUtilsClient.config().copyChatFeedback()) {
            chatHud.addClientSystemMessage(
                EmUtilsChatPrefix.chat(
                    Component.translatable(EMUtilsTexts.CHAT_COPY_SUCCESS).withStyle(
                        ChatFormatting.GREEN
                    )
                )
            );
        }
        return true;
    }

    @Nullable
    public static String messageAt(
        Minecraft client,
        double mouseX,
        double mouseY
    ) {
        if (client.gui == null) {
            return null;
        }

        ChatComponent chatHud = client.gui.hud.getChat();
        if (!(chatHud instanceof ChatHudAccess access)) {
            return null;
        }

        Component message = access.emutils$getMessageAt(mouseX, mouseY);
        return message == null ? null : formatForClipboard(message);
    }

    private static String formatForClipboard(Component message) {
        if (EMUtilsClient.config().copyChatFormatting()) {
            return ChatLegacyFormatting.toAmpersandString(message);
        }

        return message.getString();
    }
}
