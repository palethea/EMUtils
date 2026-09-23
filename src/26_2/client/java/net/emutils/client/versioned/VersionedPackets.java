package net.emutils.client.versioned;

import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;

/** Minecraft 26.2 access to packet fields whose accessors differ between versions. */
public final class VersionedPackets {
	private VersionedPackets() {
	}

	public static int chunkX(ClientboundLevelChunkWithLightPacket packet) {
		return packet.getX();
	}

	public static int chunkZ(ClientboundLevelChunkWithLightPacket packet) {
		return packet.getZ();
	}
}
