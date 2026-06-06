package net.emutils.client.emutils.screenshot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.text.EmUtilsChatPrefix;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

public final class ScreenshotActions {

    private ScreenshotActions() {}

    public static boolean copyWithFeedback(
        MinecraftClient client,
        File screenshot
    ) {
        boolean copied = ScreenshotClipboard.copyImage(screenshot);
        postCopyFeedback(client, copied);
        return copied;
    }

    public static void openImage(File screenshot) {
        Util.getOperatingSystem().open(screenshot);
    }

    public static void openFolder(File screenshot) {
        File parent = screenshot.getParentFile();
        if (parent != null) {
            Util.getOperatingSystem().open(parent);
        }
    }

    public static boolean deleteWithFeedback(
        MinecraftClient client,
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
        MinecraftClient client,
        boolean deleted
    ) {
        if (client == null || client.inGameHud == null) {
            return;
        }

        String key = deleted
            ? EMUtilsTexts.CHAT_SCREENSHOT_DELETE_SUCCESS
            : EMUtilsTexts.CHAT_SCREENSHOT_DELETE_FAILURE;
        Formatting formatting = deleted ? Formatting.GREEN : Formatting.RED;
        client.inGameHud
            .getChatHud()
            .addMessage(
                EmUtilsChatPrefix.chat(
                    Text.translatable(key).formatted(formatting)
                ),
                null,
                null
            );
    }

    private static void postCopyFeedback(
        MinecraftClient client,
        boolean copied
    ) {
        if (client == null || client.inGameHud == null) {
            return;
        }

        String key = copied
            ? EMUtilsTexts.CHAT_SCREENSHOT_COPY_SUCCESS
            : EMUtilsTexts.CHAT_SCREENSHOT_COPY_FAILURE;
        Formatting formatting = copied ? Formatting.GREEN : Formatting.RED;
        client.inGameHud
            .getChatHud()
            .addMessage(
                EmUtilsChatPrefix.chat(
                    Text.translatable(key).formatted(formatting)
                ),
                null,
                null
            );
    }
}
