package net.emutils.client.death;

import java.util.Optional;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.util.EMUtilsTexts;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public final class DeathWaypointMessage {
	public static final Identifier CLEAR_DEATH_WAYPOINT_ACTION = Identifier.of(EMUtilsClient.MOD_ID, "clear_death_waypoint");
	public static final Identifier KEEP_DEATH_WAYPOINT_ACTION = Identifier.of(EMUtilsClient.MOD_ID, "keep_death_waypoint");

	private DeathWaypointMessage() {
	}

	public static Text nearWaypointPrompt(long deathTimestamp) {
		return Text.empty()
			.append(EMUtilsTexts.greenPrefix())
			.append(Text.translatable(EMUtilsTexts.DEATH_PROMPT).formatted(Formatting.GRAY))
			.append(action(EMUtilsTexts.DEATH_ACTION_REMOVE, Formatting.RED, CLEAR_DEATH_WAYPOINT_ACTION, deathTimestamp, EMUtilsTexts.DEATH_HOVER_REMOVE))
			.append(Text.literal(" "))
			.append(action(EMUtilsTexts.DEATH_ACTION_KEEP, Formatting.AQUA, KEEP_DEATH_WAYPOINT_ACTION, deathTimestamp, EMUtilsTexts.DEATH_HOVER_KEEP));
	}

	public static Text cleared() {
		return Text.translatable(EMUtilsTexts.DEATH_CLEARED).formatted(Formatting.GREEN);
	}

	public static Text kept() {
		return Text.translatable(EMUtilsTexts.DEATH_KEPT).formatted(Formatting.GREEN);
	}

	public static Text clearedForWorld() {
		return Text.translatable(EMUtilsTexts.DEATH_CLEARED_WORLD).formatted(Formatting.GREEN);
	}

	public static Text noneForWorld() {
		return Text.translatable(EMUtilsTexts.DEATH_NONE_WORLD).formatted(Formatting.GRAY);
	}

	private static MutableText action(
		String labelKey,
		Formatting color,
		Identifier actionId,
		long deathTimestamp,
		String hoverKey
	) {
		return Text.empty()
			.append(Text.literal("[").formatted(Formatting.DARK_GRAY))
			.append(Text.translatable(labelKey).formatted(color).styled(style -> style
				.withClickEvent(new ClickEvent.Custom(actionId, Optional.of(NbtString.of(Long.toString(deathTimestamp)))))
				.withHoverEvent(new HoverEvent.ShowText(Text.translatable(hoverKey)))))
			.append(Text.literal("]").formatted(Formatting.DARK_GRAY));
	}
}
