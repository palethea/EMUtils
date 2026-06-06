package net.emutils.client.emutils.waypoint;

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

public final class WaypointMessage {
	public static final Identifier CLEAR_WAYPOINT_ACTION = Identifier.of(EMUtilsClient.MOD_ID, "clear_waypoint");
	public static final Identifier KEEP_WAYPOINT_ACTION = Identifier.of(EMUtilsClient.MOD_ID, "keep_waypoint");

	private WaypointMessage() {
	}

	public static Text nearWaypointPrompt(long timestamp) {
		return Text.empty()
			.append(EMUtilsTexts.greenPrefix())
			.append(Text.translatable(EMUtilsTexts.WAYPOINT_PROMPT).formatted(Formatting.GRAY))
			.append(action(EMUtilsTexts.WAYPOINT_ACTION_REMOVE, Formatting.RED, CLEAR_WAYPOINT_ACTION, timestamp, EMUtilsTexts.WAYPOINT_HOVER_REMOVE))
			.append(Text.literal(" "))
			.append(action(EMUtilsTexts.WAYPOINT_ACTION_KEEP, Formatting.AQUA, KEEP_WAYPOINT_ACTION, timestamp, EMUtilsTexts.WAYPOINT_HOVER_KEEP));
	}

	public static Text cleared() {
		return Text.translatable(EMUtilsTexts.WAYPOINT_CLEARED).formatted(Formatting.GREEN);
	}

	public static Text kept() {
		return Text.translatable(EMUtilsTexts.WAYPOINT_KEPT).formatted(Formatting.GREEN);
	}

	public static Text clearedForWorld() {
		return Text.translatable(EMUtilsTexts.WAYPOINT_CLEARED_WORLD).formatted(Formatting.GREEN);
	}

	public static Text noneForWorld() {
		return Text.translatable(EMUtilsTexts.WAYPOINT_NONE_WORLD).formatted(Formatting.GRAY);
	}

	private static MutableText action(
		String labelKey,
		Formatting color,
		Identifier actionId,
		long timestamp,
		String hoverKey
	) {
		return Text.empty()
			.append(Text.literal("[").formatted(Formatting.DARK_GRAY))
			.append(Text.translatable(labelKey).formatted(color).styled(style -> style
				.withClickEvent(new ClickEvent.Custom(actionId, Optional.of(NbtString.of(Long.toString(timestamp)))))
				.withHoverEvent(new HoverEvent.ShowText(Text.translatable(hoverKey)))))
			.append(Text.literal("]").formatted(Formatting.DARK_GRAY));
	}
}
