package net.emutils.client.death;

import java.util.Optional;
import net.emutils.client.EMUtilsClient;
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

	public static Text nearWaypointPrompt() {
		return Text.empty()
			.append(Text.literal("EMUtils").formatted(Formatting.GREEN))
			.append(Text.literal(" You are near your last death location. ").formatted(Formatting.GRAY))
			.append(action("Remove", Formatting.RED, new ClickEvent.Custom(CLEAR_DEATH_WAYPOINT_ACTION, Optional.empty()), "Remove death waypoint"))
			.append(Text.literal(" "))
			.append(action("Keep", Formatting.AQUA, new ClickEvent.Custom(KEEP_DEATH_WAYPOINT_ACTION, Optional.empty()), "Keep death waypoint"));
	}

	public static Text cleared() {
		return Text.literal("EMUtils removed death waypoint.").formatted(Formatting.GREEN);
	}

	public static Text kept() {
		return Text.literal("EMUtils will keep the death waypoint.").formatted(Formatting.GREEN);
	}

	public static Text clearedForWorld() {
		return Text.literal("EMUtils cleared death waypoints for this world.").formatted(Formatting.GREEN);
	}

	public static Text noneForWorld() {
		return Text.literal("EMUtils has no death waypoint for this world.").formatted(Formatting.GRAY);
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
