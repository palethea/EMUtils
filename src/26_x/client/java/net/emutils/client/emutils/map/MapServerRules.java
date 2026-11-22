package net.emutils.client.emutils.map;

import java.util.ArrayList;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.compat.MinecraftClientCompat;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * What a server turned off of the maps (#269), with the codes servers already send to Xaero's Minimap and World Map
 * and to JourneyMap (VoxelMap's codes), so a server that restricts those restricts EMUtils too. Like Xaero's, the codes
 * are read from system messages only, never from player chat, and hold until you leave the server.
 * <ul>
 * <li>Xaero's no-minimap code hides the minimap.</li>
 * <li>Xaero's fair-play code turns off the entity radar and cave view; its Nether code then allows cave view in the
 * Nether. Xaero's reset code takes all of Xaero's back.</li>
 * <li>JourneyMap's codes turn off the entity radar, or cave view everywhere.</li>
 * </ul>
 */
public final class MapServerRules {
	// Xaero's Minimap and World Map, as xaero.common.events.ClientEvents and xaero.map.events.ClientEvents read them.
	static final String XAERO_RESET = codes("resetxaero");
	static final String XAERO_NO_MINIMAP = codes("nominimap");
	static final String XAERO_FAIR_PLAY = codes("fairxaero");
	static final String XAERO_MINIMAP_NETHER_FAIR = codes("xaeromm") + codes("netherisfair");
	static final String XAERO_WORLD_MAP_NETHER_FAIR = codes("xaerowm") + codes("netherisfair");
	// JourneyMap before 5.5.5 and VoxelMap: the same prefix, then the feature.
	static final String JOURNEYMAP_NO_RADAR = "§3 §6 §3 §6 §3 §6 §e";
	static final String JOURNEYMAP_NO_CAVES = "§3 §6 §3 §6 §3 §6 §d";

	private static boolean noMinimap;
	private static boolean fairPlay;
	private static boolean netherFair;
	private static boolean noRadar;
	private static boolean noCaves;

	private MapServerRules() {
	}

	public static void register() {
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> read(Minecraft.getInstance(), message.getString()));
		// From the start of each connection, before the server can send any codes, and again after leaving.
		ClientPlayConnectionEvents.INIT.register((handler, client) -> reset());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
	}

	/** Reads a system message's text for the codes, and says in chat what it turned off that you had on. */
	static void read(Minecraft client, String text) {
		if (text.indexOf('§') < 0) {
			return;
		}
		boolean minimapBefore = minimapAllowed();
		boolean radarBefore = radarAllowed();
		boolean cavesBefore = cavesAllowedIn(client.level);
		apply(text);
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || client.player == null) {
			return;
		}
		List<Component> off = new ArrayList<>();
		if (minimapBefore && !minimapAllowed() && config.minimap()) {
			off.add(Component.translatable(EMUtilsTexts.OPTION_MINIMAP));
		}
		if (radarBefore && !radarAllowed() && config.mapRadar() && config.unfairFeatures()) {
			off.add(Component.translatable(EMUtilsTexts.OPTION_MAP_RADAR));
		}
		if (cavesBefore && !cavesAllowedIn(client.level) && (config.minimap() || config.worldMap()) && config.unfairFeatures()) {
			off.add(Component.translatable(EMUtilsTexts.OPTION_MAP_CAVES));
		}
		if (!off.isEmpty()) {
			MinecraftClientCompat.chat(client).addClientSystemMessage(Component.empty().append(EMUtilsTexts.greenPrefix())
				.append(Component.translatable(EMUtilsTexts.CHAT_MAP_SERVER_OFF, join(off)).withStyle(ChatFormatting.GRAY)));
		}
	}

	/** Takes in the codes in a message, in the order Xaero's mods check them: a reset first, then the rest. */
	static void apply(String text) {
		if (text.contains(XAERO_RESET)) {
			noMinimap = false;
			fairPlay = false;
			netherFair = false;
		}
		if (text.contains(XAERO_NO_MINIMAP)) {
			noMinimap = true;
		}
		if (text.contains(XAERO_MINIMAP_NETHER_FAIR) || text.contains(XAERO_WORLD_MAP_NETHER_FAIR)) {
			netherFair = true;
		}
		if (text.contains(XAERO_FAIR_PLAY)) {
			fairPlay = true;
		}
		if (text.contains(JOURNEYMAP_NO_RADAR)) {
			noRadar = true;
		}
		if (text.contains(JOURNEYMAP_NO_CAVES)) {
			noCaves = true;
		}
	}

	/** Clears every code, as leaving the server does. */
	public static void reset() {
		noMinimap = false;
		fairPlay = false;
		netherFair = false;
		noRadar = false;
		noCaves = false;
	}

	/** Whether the server lets the minimap show. */
	public static boolean minimapAllowed() {
		return !noMinimap;
	}

	/** Whether the server lets the entity radar show, on the minimap and the world map. */
	public static boolean radarAllowed() {
		return !fairPlay && !noRadar;
	}

	/** Whether the server lets cave view show where you are: never with JourneyMap's code, only in the Nether with Xaero's fair play and its Nether code. */
	public static boolean cavesAllowedIn(@Nullable Level level) {
		if (noCaves) {
			return false;
		}
		return !fairPlay || netherFair && level != null && level.dimension() == Level.NETHER;
	}

	/** Each letter after a section sign, the way Xaero writes its codes. */
	private static String codes(String letters) {
		StringBuilder text = new StringBuilder(letters.length() * 2);
		for (char letter : letters.toCharArray()) {
			text.append('§').append(letter);
		}
		return text.toString();
	}

	private static Component join(List<Component> parts) {
		net.minecraft.network.chat.MutableComponent joined = Component.empty();
		for (int i = 0; i < parts.size(); i++) {
			if (i > 0) {
				joined.append(i == parts.size() - 1 ? Component.translatable(EMUtilsTexts.CHAT_LIST_AND) : Component.literal(", "));
			}
			joined.append(parts.get(i));
		}
		return joined;
	}
}
