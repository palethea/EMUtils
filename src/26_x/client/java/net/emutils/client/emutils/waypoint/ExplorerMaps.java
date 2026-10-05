package net.emutils.client.emutils.waypoint;

import java.util.List;
import java.util.Locale;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.map.MapManager;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/**
 * Explorer maps' destinations as waypoints (#241). A treasure, mansion, monument, village or other explorer map
 * (in 26.3 a separate item for each, such as the Abandoned Camp map) carries where it leads on the item itself,
 * as the map decoration the game keys {@code +}, so the client knows it on any server without asking.
 */
public final class ExplorerMaps {
	/** The key the game gives an explorer map's destination among the map's decorations. */
	private static final String TARGET = "+";
	/** Shown while held (the setting): the destination shown, and one reached, so it isn't shown again. */
	private static @Nullable Target shown;
	private static @Nullable Target reached;

	private ExplorerMaps() {
	}

	/**
	 * Where an explorer map leads: x and z, and what's there, by the decoration's id (red_x, mansion,
	 * monument, trial_chambers, ...), with the map's name for the waypoint.
	 */
	public record Target(int x, int z, String kind, String name) {
		/** The location for the add sheet: the ground there when known, else your own height. */
		SharedWaypoint shared(Minecraft client) {
			return new SharedWaypoint(name, x, groundY(client, x, z), z, null, color(kind));
		}
	}

	/** The destination of an explorer map, or null for any other item, or a map without one. */
	public static @Nullable Target target(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		MapDecorations decorations = stack.get(DataComponents.MAP_DECORATIONS);
		MapDecorations.Entry entry = decorations == null ? null : decorations.decorations().get(TARGET);
		if (entry == null) {
			return null;
		}
		String kind = entry.type().unwrapKey().map(key -> key.identifier().getPath()).orElse("target");
		return new Target((int) Math.floor(entry.x()), (int) Math.floor(entry.z()), kind, name(stack, kind));
	}

	/** The destination of the explorer map in your hand, main hand first, or null. */
	public static @Nullable Target held(@Nullable LocalPlayer player) {
		if (player == null) {
			return null;
		}
		Target main = target(player.getItemInHand(InteractionHand.MAIN_HAND));
		return main != null ? main : target(player.getItemInHand(InteractionHand.OFF_HAND));
	}

	/**
	 * For the Add Waypoint key: opens the add sheet filled in with the held map's destination, or says the spot
	 * has a waypoint already. Returns false when you hold no explorer map, so the key does what it always does.
	 */
	public static boolean addFromHeld(Minecraft client) {
		Target target = held(client.player);
		WaypointManager manager = EMUtilsClient.waypoint();
		if (target == null || manager == null || !manager.enabled()) {
			return false;
		}
		if (manager.hasWaypointAt(client, null, target.x(), null, target.z())) {
			client.gui.hud.setOverlayMessage(Component.translatable(EMUtilsTexts.WAYPOINT_EXPLORER_MAP_EXISTS, target.name()), false);
			return true;
		}
		client.gui.setScreen(net.emutils.client.emutils.waypoint.gui.WaypointsScreen.addShared(null, target.shared(client)));
		return true;
	}

	/** The line an explorer map's tooltip gets: where it leads. */
	public static void appendTooltip(ItemStack stack, List<Component> lines) {
		Target target = target(stack);
		if (target != null) {
			lines.add(Component.translatable(EMUtilsTexts.WAYPOINT_EXPLORER_MAP_TOOLTIP, target.x(), target.z()).withStyle(ChatFormatting.GRAY));
		}
	}

	/**
	 * With Show Explorer Map Destinations on, the destination of the map you hold is a temporary waypoint
	 * (#226) until you put the map away or get there. A temporary waypoint of your own is left alone. Once a tick.
	 */
	public static void tick(Minecraft client) {
		WaypointManager manager = EMUtilsClient.waypoint();
		if (manager == null || client.player == null || client.level == null) {
			shown = null;
			return;
		}
		Target target = EMUtilsClient.config().waypointExplorerMapPreview() ? held(client.player) : null;
		Waypoint temporary = manager.temporaryWaypoint();
		boolean ours = shown != null && temporary != null && temporary.x() == shown.x() && temporary.z() == shown.z();
		if (target == null) {
			if (ours) {
				manager.dropTemporary();
			}
			shown = null;
			reached = null;
			return;
		}
		if (shown != null && !ours && target.equals(shown)) {
			// Gone while still held: reached, or replaced by one of your own. Not shown again for this map.
			reached = target;
			shown = null;
		}
		if (target.equals(shown) || target.equals(reached) || temporary != null && !ours) {
			return;
		}
		Integer y = groundY(client, target.x(), target.z());
		manager.addTemporary(client, null, target.x(), y != null ? y : client.player.getBlockY(), target.z(), target.name(), color(target.kind()));
		shown = target;
	}

	/**
	 * The ground at a spot: from the EMUtils map when it has it, else from the loaded world's heights, which a
	 * chunk only has once the game filled them in; null when neither knows, so your own height is used.
	 */
	private static @Nullable Integer groundY(Minecraft client, int x, int z) {
		Integer mapped = MapManager.groundYIfKnown(x, z);
		if (mapped != null) {
			return mapped;
		}
		if (client.level != null && client.level.hasChunk(x >> 4, z >> 4)) {
			int top = client.level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
			if (top > client.level.getMinY()) {
				return top;
			}
		}
		return null;
	}

	/** The waypoint's name: the map's own, without "Map" at the end, as in "Buried Treasure". */
	private static String name(ItemStack stack, String kind) {
		String name = stack.getHoverName().getString().strip();
		if (name.endsWith(" Map") && name.length() > 4) {
			return name.substring(0, name.length() - 4);
		}
		if (name.isEmpty() || name.equalsIgnoreCase("map")) {
			// A map without a name of its own: after what's there.
			return switch (kind) {
				case "red_x", "target_x" -> "Treasure";
				case "mansion" -> "Woodland Mansion";
				case "monument" -> "Ocean Monument";
				default -> {
					String words = kind.replace('_', ' ');
					yield words.isEmpty() ? name : words.substring(0, 1).toUpperCase(Locale.ROOT) + words.substring(1);
				}
			};
		}
		return name;
	}

	/** A color for each kind of destination, close to how it looks; others get the default waypoint color. */
	static int color(String kind) {
		if (kind.endsWith("_village")) {
			return 0xFFF0A040;
		}
		return switch (kind) {
			case "red_x", "target_x" -> 0xFFFF5555;
			case "mansion", "woodland_mansion" -> 0xFF9A6B3F;
			case "monument", "ocean_monument" -> 0xFF3FB6C9;
			case "trial_chambers" -> 0xFFD8873A;
			case "jungle_temple" -> 0xFF5FAF3F;
			case "swamp_hut" -> 0xFF7A9A55;
			case "desert_pyramid" -> 0xFFE3C770;
			case "ancient_city" -> 0xFF2E7C8E;
			case "mineshaft" -> 0xFFA0794E;
			case "abandoned_camp" -> 0xFFC9A26B;
			case "ocean_ruin_warm", "ocean_ruin_cold" -> 0xFF4FD8E8;
			default -> EMUtilsClient.config().waypointDefaultCustomColor();
		};
	}

	/** For UI snapshot checks: the ground height a destination would get, or null for your own. */
	public static @Nullable Integer groundYForSnapshot(Minecraft client, int x, int z) {
		return groundY(client, x, z);
	}

	/** For UI snapshot checks: forgets what was shown while held. */
	public static void resetForSnapshot() {
		shown = null;
		reached = null;
	}
}
