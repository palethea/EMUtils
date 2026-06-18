package net.emutils.client.emutils.screenshot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.text.EmUtilsChatPrefix;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;

public final class ScreenshotActions {

    private ScreenshotActions() {}

    public static boolean copyWithFeedback(
        Minecraft client,
        File screenshot
    ) {
        if (screenshot == null || !screenshot.isFile()) {
            postCopyFeedback(client, false);
            return false;
        }

        Thread worker = new Thread(() -> {
            boolean copied = ScreenshotClipboard.copyImage(screenshot);
            if (client != null) {
                client.execute(() -> postCopyFeedback(client, copied));
            }
        }, "EMUtils-Screenshot-Clipboard");
        worker.setDaemon(true);
        worker.start();
        return true;
    }

    public static void openImage(File screenshot) {
        Util.getPlatform().openFile(screenshot);
    }

    public static void openFolder(File screenshot) {
        File parent = screenshot.getParentFile();
        if (parent != null) {
            Util.getPlatform().openFile(parent);
        }
    }

    public static boolean deleteWithFeedback(
        Minecraft client,
        File screenshot
    ) {
        boolean deleted = delete(screenshot);
        postDeleteFeedback(client, deleted);
        return deleted;
    }

    private static boolean delete(File screenshot) {
        if (screenshot == null || !screenshot.isFile()) {
            return false;
        }

        try {
            return Files.deleteIfExists(screenshot.toPath());
        } catch (IOException exception) {
            EMUtilsClient.LOGGER.warn(
                "Failed to delete screenshot {}.",
                screenshot,
                exception
            );
            return false;
        }
    }

    private static void postDeleteFeedback(
        Minecraft client,
        boolean deleted
    ) {
        if (client == null || client.gui == null) {
            return;
        }

        String key = deleted
            ? EMUtilsTexts.CHAT_SCREENSHOT_DELETE_SUCCESS
            : EMUtilsTexts.CHAT_SCREENSHOT_DELETE_FAILURE;
        ChatFormatting formatting = deleted ? ChatFormatting.GREEN : ChatFormatting.RED;
        client.gui.hud.getChat()
            .addClientSystemMessage(
                EmUtilsChatPrefix.chat(
                    Component.translatable(key).withStyle(formatting)
                )
            );
    }

    private static void postCopyFeedback(
        Minecraft client,
        boolean copied
    ) {
        if (client == null || client.gui == null) {
            return;
        }

        String key = copied
            ? EMUtilsTexts.CHAT_SCREENSHOT_COPY_SUCCESS
            : EMUtilsTexts.CHAT_SCREENSHOT_COPY_FAILURE;
        ChatFormatting formatting = copied ? ChatFormatting.GREEN : ChatFormatting.RED;
        client.gui.hud.getChat()
            .addClientSystemMessage(
                EmUtilsChatPrefix.chat(
                    Component.translatable(key).withStyle(formatting)
                )
            );
    }
}
