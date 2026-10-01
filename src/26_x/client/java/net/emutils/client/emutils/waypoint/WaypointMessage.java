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
	public static final Identifier ADD_SHARED_WAYPOINT_ACTION = Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "add_shared_waypoint");

	private WaypointMessage() {
	}

	public static Component nearWaypointPrompt(String id) {
		return Component.empty()
			.append(EMUtilsTexts.greenPrefix())
			.append(Component.translatable(EMUtilsTexts.WAYPOINT_PROMPT).withStyle(ChatFormatting.GRAY))
			.append(action(EMUtilsTexts.WAYPOINT_ACTION_REMOVE, ChatFormatting.RED, CLEAR_WAYPOINT_ACTION, id, EMUtilsTexts.WAYPOINT_HOVER_REMOVE))
			.append(Component.literal(" "))
			.append(action(EMUtilsTexts.WAYPOINT_ACTION_KEEP, ChatFormatting.AQUA, KEEP_WAYPOINT_ACTION, id, EMUtilsTexts.WAYPOINT_HOVER_KEEP));
	}

	/** The offer under someone's shared coordinates; its button opens Add Waypoint with them, by the key they are kept under. */
	public static Component sharedPrompt(String sender, String coordinates, String key) {
		return Component.empty()
			.append(Component.translatable(EMUtilsTexts.WAYPOINT_SHARED_PROMPT, sender, coordinates).withStyle(ChatFormatting.GRAY))
			.append(action(EMUtilsTexts.WAYPOINT_ACTION_ADD_SHARED, ChatFormatting.GREEN, ADD_SHARED_WAYPOINT_ACTION, key, EMUtilsTexts.WAYPOINT_HOVER_ADD_SHARED));
	}

	/** What Auto-Add says: the waypoint is there, with a button to take it back out. */
	public static Component sharedAdded(String name, String coordinates, String id) {
		return Component.empty()
			.append(Component.translatable(EMUtilsTexts.WAYPOINT_SHARED_ADDED, name, coordinates).withStyle(ChatFormatting.GRAY))
			.append(action(EMUtilsTexts.WAYPOINT_ACTION_REMOVE, ChatFormatting.RED, CLEAR_WAYPOINT_ACTION, id, EMUtilsTexts.WAYPOINT_HOVER_REMOVE));
	}

	public static Component sharedExpired() {
		return Component.translatable(EMUtilsTexts.WAYPOINT_SHARED_EXPIRED).withStyle(ChatFormatting.GRAY);
	}

	public static Component noTarget() {
		return Component.translatable(EMUtilsTexts.WAYPOINT_NO_TARGET).withStyle(ChatFormatting.GRAY);
	}

	public static Component reachedRemoved() {
		return Component.translatable(EMUtilsTexts.WAYPOINT_REACHED_REMOVED).withStyle(ChatFormatting.GREEN);
	}

	public static Component cleared() {
		return Component.translatable(EMUtilsTexts.WAYPOINT_CLEARED).withStyle(ChatFormatting.GREEN);
	}

	public static Component kept() {
		return Component.translatable(EMUtilsTexts.WAYPOINT_KEPT).withStyle(ChatFormatting.GREEN);
	}

	public static Component saveFailed() {
		return Component.translatable(EMUtilsTexts.WAYPOINT_SAVE_FAILED).withStyle(ChatFormatting.RED);
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
		String id,
		String hoverKey
	) {
		return Component.empty()
			.append(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY))
			.append(Component.translatable(labelKey).withStyle(color).withStyle(style -> style
				.withClickEvent(new ClickEvent.Custom(actionId, Optional.of(StringTag.valueOf(id))))
				.withHoverEvent(new HoverEvent.ShowText(Component.translatable(hoverKey)))))
			.append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY));
	}
}
