package net.emutils.client.emutils.waypoint;

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

public final class WaypointMessage {
	public static final Identifier CLEAR_WAYPOINT_ACTION = Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "clear_waypoint");
	public static final Identifier KEEP_WAYPOINT_ACTION = Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "keep_waypoint");

	private WaypointMessage() {
	}

	public static Component nearWaypointPrompt(long timestamp) {
		return Component.empty()
			.append(EMUtilsTexts.greenPrefix())
			.append(Component.translatable(EMUtilsTexts.WAYPOINT_PROMPT).withStyle(ChatFormatting.GRAY))
			.append(action(EMUtilsTexts.WAYPOINT_ACTION_REMOVE, ChatFormatting.RED, CLEAR_WAYPOINT_ACTION, timestamp, EMUtilsTexts.WAYPOINT_HOVER_REMOVE))
			.append(Component.literal(" "))
			.append(action(EMUtilsTexts.WAYPOINT_ACTION_KEEP, ChatFormatting.AQUA, KEEP_WAYPOINT_ACTION, timestamp, EMUtilsTexts.WAYPOINT_HOVER_KEEP));
	}

	public static Component cleared() {
		return Component.translatable(EMUtilsTexts.WAYPOINT_CLEARED).withStyle(ChatFormatting.GREEN);
	}

	public static Component kept() {
		return Component.translatable(EMUtilsTexts.WAYPOINT_KEPT).withStyle(ChatFormatting.GREEN);
	}

	public static Component clearedForWorld() {
		return Component.translatable(EMUtilsTexts.WAYPOINT_CLEARED_WORLD).withStyle(ChatFormatting.GREEN);
	}

	public static Component noneForWorld() {
		return Component.translatable(EMUtilsTexts.WAYPOINT_NONE_WORLD).withStyle(ChatFormatting.GRAY);
	}

	private static MutableComponent action(
		String labelKey,
		ChatFormatting color,
		Identifier actionId,
		long timestamp,
		String hoverKey
	) {
		return Component.empty()
			.append(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY))
			.append(Component.translatable(labelKey).withStyle(color).withStyle(style -> style
				.withClickEvent(new ClickEvent.Custom(actionId, Optional.of(StringTag.valueOf(Long.toString(timestamp)))))
				.withHoverEvent(new HoverEvent.ShowText(Component.translatable(hoverKey)))))
			.append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY));
	}
}
