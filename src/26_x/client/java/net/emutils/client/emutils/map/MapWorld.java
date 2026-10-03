package net.emutils.client.emutils.map;

import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import org.jspecify.annotations.Nullable;

/**
 * The sampled chunks of the level you are in (#212). Lives as long as that level: a new dimension or world
 * starts a new one. Chunks are read by the tile baker from its own thread.
 */
public final class MapWorld {
	/** Chunks farther than this from the player are forgotten, to keep memory use small; the minimap never shows them. */
	static final int KEEP_RADIUS_CHUNKS = 40;

	private final ClientLevel level;
	private final Registry<Biome> biomes;
	private final ConcurrentHashMap<Long, MapChunk> chunks = new ConcurrentHashMap<>();

	MapWorld(ClientLevel level) {
		this.level = level;
		this.biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
	}

	public ClientLevel level() {
		return level;
	}

	public Registry<Biome> biomes() {
		return biomes;
	}

	public @Nullable MapChunk chunk(int chunkX, int chunkZ) {
		return chunks.get(ChunkPos.pack(chunkX, chunkZ));
	}

	void put(int chunkX, int chunkZ, MapChunk chunk) {
		chunks.put(ChunkPos.pack(chunkX, chunkZ), chunk);
	}

	int size() {
		return chunks.size();
	}

	/** Forgets the chunks far from {@code (chunkX, chunkZ)}. */
	void forgetFarFrom(int chunkX, int chunkZ) {
		chunks.keySet().removeIf(key -> Math.max(Math.abs(ChunkPos.getX(key) - chunkX), Math.abs(ChunkPos.getZ(key) - chunkZ)) > KEEP_RADIUS_CHUNKS);
	}
}
