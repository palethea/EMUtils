package net.emutils.client.emutils.screenshot;

import java.io.File;
import java.util.Optional;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

public final class ScreenshotMessage {
	public static final Identifier COPY_SCREENSHOT_ACTION = Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "copy_screenshot");

	private ScreenshotMessage() {
	}

	public static Component saved(File screenshot) {
		return Component.empty()
			.append(EMUtilsTexts.greenPrefix())
			.append(Component.translatable(EMUtilsTexts.CHAT_SCREENSHOT_SAVED).withStyle(ChatFormatting.GRAY))
			.append(action(EMUtilsTexts.CHAT_ACTION_COPY, ChatFormatting.YELLOW, new ClickEvent.Custom(COPY_SCREENSHOT_ACTION, Optional.of(StringTag.valueOf(screenshot.getAbsolutePath()))), EMUtilsTexts.CHAT_HOVER_COPY_SCREENSHOT))
			.append(Component.literal(" "))
			.append(action(EMUtilsTexts.CHAT_ACTION_OPEN, ChatFormatting.AQUA, new ClickEvent.OpenFile(screenshot), EMUtilsTexts.CHAT_HOVER_OPEN_IMAGE))
			.append(Component.literal(" "))
			.append(action(EMUtilsTexts.CHAT_ACTION_FOLDER, ChatFormatting.WHITE, new ClickEvent.OpenFile(screenshot.getParentFile()), EMUtilsTexts.CHAT_HOVER_OPEN_FOLDER));
	}

	private static MutableComponent action(String labelKey, ChatFormatting color, ClickEvent clickEvent, String hoverKey) {
		return Component.empty()
			.append(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY))
			.append(Component.translatable(labelKey).withStyle(color).withStyle(style -> style
				.withClickEvent(clickEvent)
				.withHoverEvent(new HoverEvent.ShowText(Component.translatable(hoverKey)))))
			.append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY));
	}
}
