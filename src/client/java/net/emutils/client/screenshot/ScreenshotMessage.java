package net.emutils.client.screenshot;

import java.io.File;
import java.util.Optional;
import net.emutils.client.EMUtilsClient;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;

public final class ScreenshotMessage {
	public static final Identifier COPY_SCREENSHOT_ACTION = Identifier.of(EMUtilsClient.MOD_ID, "copy_screenshot");

	private ScreenshotMessage() {
	}

	public static Text saved(File screenshot) {
		return Text.empty()
			.append(Text.literal("EMUtils").formatted(Formatting.GREEN))
			.append(Text.literal(" Saved screenshot ").formatted(Formatting.GRAY))
			.append(action("Copy", Formatting.YELLOW, new ClickEvent.Custom(COPY_SCREENSHOT_ACTION, Optional.of(NbtString.of(screenshot.getAbsolutePath()))), "Copy image to clipboard"))
			.append(Text.literal(" "))
			.append(action("Open", Formatting.AQUA, new ClickEvent.OpenFile(screenshot), "Open image"))
			.append(Text.literal(" "))
			.append(action("Folder", Formatting.WHITE, new ClickEvent.OpenFile(screenshot.getParentFile()), "Open screenshot folder"));
	}

	private static MutableText action(String label, Formatting color, ClickEvent clickEvent, String hoverText) {
		return Text.empty()
			.append(Text.literal("[").formatted(Formatting.DARK_GRAY))
			.append(Text.literal(label).formatted(color).styled(style -> style
				.withClickEvent(clickEvent)
				.withHoverEvent(new HoverEvent.ShowText(Text.literal(hoverText)))))
			.append(Text.literal("]").formatted(Formatting.DARK_GRAY));
	}
}
