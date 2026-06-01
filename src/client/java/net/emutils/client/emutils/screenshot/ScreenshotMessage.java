package net.emutils.client.emutils.screenshot;

import java.io.File;
import java.util.Optional;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public final class ScreenshotMessage {
	public static final Identifier COPY_SCREENSHOT_ACTION = Identifier.of(EMUtilsClient.MOD_ID, "copy_screenshot");

	private ScreenshotMessage() {
	}

	public static Text saved(File screenshot) {
		return Text.empty()
			.append(EMUtilsTexts.greenPrefix())
			.append(Text.translatable(EMUtilsTexts.CHAT_SCREENSHOT_SAVED).formatted(Formatting.GRAY))
			.append(action(EMUtilsTexts.CHAT_ACTION_COPY, Formatting.YELLOW, new ClickEvent.Custom(COPY_SCREENSHOT_ACTION, Optional.of(NbtString.of(screenshot.getAbsolutePath()))), EMUtilsTexts.CHAT_HOVER_COPY_SCREENSHOT))
			.append(Text.literal(" "))
			.append(action(EMUtilsTexts.CHAT_ACTION_OPEN, Formatting.AQUA, new ClickEvent.OpenFile(screenshot), EMUtilsTexts.CHAT_HOVER_OPEN_IMAGE))
			.append(Text.literal(" "))
			.append(action(EMUtilsTexts.CHAT_ACTION_FOLDER, Formatting.WHITE, new ClickEvent.OpenFile(screenshot.getParentFile()), EMUtilsTexts.CHAT_HOVER_OPEN_FOLDER));
	}

	private static MutableText action(String labelKey, Formatting color, ClickEvent clickEvent, String hoverKey) {
		return Text.empty()
			.append(Text.literal("[").formatted(Formatting.DARK_GRAY))
			.append(Text.translatable(labelKey).formatted(color).styled(style -> style
				.withClickEvent(clickEvent)
				.withHoverEvent(new HoverEvent.ShowText(Text.translatable(hoverKey)))))
			.append(Text.literal("]").formatted(Formatting.DARK_GRAY));
	}
}
