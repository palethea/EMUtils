package net.emutils.client.emutils.map;

import java.nio.charset.StandardCharsets;
import net.emutils.client.emutils.compat.XaeroMapIntegration;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ServerboundPlayChannelEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * The world id a server tells map mods (#219), which is the surest way to tell its worlds apart, as Xaero's
 * maps do. Two kinds are listened for: Xaero's own ({@code xaerominimap:main} and {@code xaeroworldmap:main},
 * a zero byte and the id as an int, sent by the server as soon as the client listens), and the one
 * JourneyMap and VoxelMap share ({@code worldinfo:world_id}, asked for with a zero byte and 42, answered with
 * a zero byte, 42, a length and the id as text), which proxy plugins like MapModCompanion add. A channel is
 * left to its own mod when that's installed. Without an id, the map tells worlds apart itself.
 */
public final class MapServerWorlds {
	private static final byte MAGIC = 42;
	private static final CustomPacketPayload.Type<Raw> XAERO_MINIMAP = type("xaerominimap", "main");
	private static final CustomPacketPayload.Type<Raw> XAERO_WORLD_MAP = type("xaeroworldmap", "main");
	private static final CustomPacketPayload.Type<Raw> WORLD_ID = type("worldinfo", "world_id");
	/** The id the server last said the world you're in has, or null when it said none. */
	private static volatile @Nullable String current;

	/** A payload kept as its bytes, read by hand like the mods that defined it. */
	private record Raw(CustomPacketPayload.Type<Raw> type, byte[] data) implements CustomPacketPayload {
	}

	private MapServerWorlds() {
	}

	private static CustomPacketPayload.Type<Raw> type(String namespace, String path) {
		return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(namespace, path));
	}

	private static StreamCodec<RegistryFriendlyByteBuf, Raw> codec(CustomPacketPayload.Type<Raw> type) {
		return StreamCodec.of(
			(buf, payload) -> buf.writeBytes(payload.data()),
			buf -> {
				byte[] data = new byte[buf.readableBytes()];
				buf.readBytes(data);
				return new Raw(type, data);
			}
		);
	}

	public static void register() {
		if (!XaeroMapIntegration.minimapLoaded()) {
			listen(XAERO_MINIMAP, MapServerWorlds::readXaero);
		}
		if (!XaeroMapIntegration.worldMapLoaded()) {
			listen(XAERO_WORLD_MAP, MapServerWorlds::readXaero);
		}
		FabricLoader loader = FabricLoader.getInstance();
		boolean worldIdTaken = loader.isModLoaded("journeymap") || loader.isModLoaded("voxelmap");
		if (!worldIdTaken) {
			PayloadTypeRegistry.serverboundPlay().register(WORLD_ID, codec(WORLD_ID));
			listen(WORLD_ID, MapServerWorlds::readWorldId);
		}
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			current = null;
			ask(worldIdTaken);
		});
		// The server may say it takes the request only after you joined; it's asked as soon as it does.
		ServerboundPlayChannelEvents.REGISTER.register((handler, sender, client, channels) -> {
			if (channels.contains(WORLD_ID.id())) {
				ask(worldIdTaken);
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> current = null);
	}

	private static void listen(CustomPacketPayload.Type<Raw> type, java.util.function.Function<byte[], @Nullable String> reader) {
		PayloadTypeRegistry.clientboundPlay().register(type, codec(type));
		ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) -> {
			String id = reader.apply(payload.data());
			if (id != null) {
				current = id;
			}
		});
	}

	/** Asks for the world id where servers answer when asked, on joining and after each change of world. */
	static void ask() {
		FabricLoader loader = FabricLoader.getInstance();
		ask(loader.isModLoaded("journeymap") || loader.isModLoaded("voxelmap"));
	}

	private static void ask(boolean worldIdTaken) {
		if (!worldIdTaken && ClientPlayNetworking.canSend(WORLD_ID)) {
			ClientPlayNetworking.send(new Raw(WORLD_ID, new byte[] {0, MAGIC}));
		}
	}

	/** Xaero's world properties: a zero byte, then the id as an int. */
	private static @Nullable String readXaero(byte[] data) {
		if (data.length < 5 || data[0] != 0) {
			return null;
		}
		int id = (data[1] & 0xFF) << 24 | (data[2] & 0xFF) << 16 | (data[3] & 0xFF) << 8 | data[4] & 0xFF;
		return "xaero:" + id;
	}

	/** The shared world id: zero bytes, 42, the text's length, and the text. */
	private static @Nullable String readWorldId(byte[] data) {
		int at = 0;
		while (at < data.length && data[at] == 0) {
			at++;
		}
		if (at < data.length && data[at] == MAGIC) {
			at++;
		}
		if (at >= data.length) {
			return null;
		}
		int length = data[at++] & 0xFF;
		if (length == 0 || at + length > data.length) {
			return null;
		}
		String id = new String(data, at, length, StandardCharsets.UTF_8).strip();
		return id.isEmpty() ? null : "world_id:" + id;
	}

	/**
	 * For UI snapshot checks: reads both kinds of world id as servers send them, and says what came out wrong,
	 * or returns an empty text.
	 */
	public static String checkForSnapshot() {
		String xaero = readXaero(new byte[] {0, 0, 0, 1, 44});
		if (!"xaero:300".equals(xaero)) {
			return "Xaero's world id came out as " + xaero;
		}
		byte[] text = "-12345".getBytes(StandardCharsets.UTF_8);
		byte[] packet = new byte[3 + text.length];
		packet[1] = MAGIC;
		packet[2] = (byte) text.length;
		System.arraycopy(text, 0, packet, 3, text.length);
		String shared = readWorldId(packet);
		return "world_id:-12345".equals(shared) ? "" : "the shared world id came out as " + shared;
	}

	/**
	 * The id the server gave the world you're in, or null when it gives none. It's kept across changes of
	 * world, as the server sends the next one when you get there, maybe before the map notices the change.
	 */
	static @Nullable String current() {
		return current;
	}
}
