package net.emutils.client.emutils.map;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.Biome;
import org.jspecify.annotations.Nullable;

/**
 * Works out which of a server's saved worlds you're in (#219), from the first chunks the map sampled there.
 * A world whose saved terrain mostly matches them is the one; one whose terrain clearly differs, or whose
 * height range does, is not. With nothing saved where you are yet, the world with the same spawn point is
 * taken, and then one from before worlds were told apart, which has no spawn on record. Otherwise it's a new
 * world. Reads the saved regions, so it runs on the map's IO thread.
 */
final class MapWorldMatcher {
	/** At least this many columns have to be compared before terrain says yes or no. */
	private static final int MIN_COMPARED = 256;
	/** A world whose saved columns match at least this share of the ones sampled is the one. */
	private static final double SAME = 0.75D;
	/** At most this many regions are read per world, around where the samples are. */
	private static final int MAX_REGIONS = 6;

	/** A chunk the map sampled since you arrived. */
	record Sample(int chunkX, int chunkZ, MapChunk chunk) {
	}

	/** A known world, as the client thread saw it: its id, folder, spawn, height range and when it was last seen. */
	record Candidate(String id, Path folder, int @Nullable [] spawn, @Nullable Integer minY, @Nullable Integer height, long lastSeen) {
	}

	private MapWorldMatcher() {
	}

	/** The id of the world you're in, or null when it's a new one. */
	static @Nullable String match(List<Candidate> candidates, List<Sample> samples, int @Nullable [] spawn, int minY, int height, boolean ceiling, Registry<Biome> biomes) {
		Candidate best = null;
		double bestShare = 0.0D;
		Candidate sameSpawn = null;
		Candidate unmarked = null;
		for (Candidate candidate : candidates) {
			if (candidate.minY() != null && candidate.minY() != minY || candidate.height() != null && candidate.height() != height) {
				continue;
			}
			int[] counts = compare(candidate.folder(), samples, ceiling, biomes);
			if (counts[0] >= MIN_COMPARED) {
				double share = counts[1] / (double) counts[0];
				if (share >= SAME && share > bestShare) {
					best = candidate;
					bestShare = share;
				}
				// Terrain that's there but differs: not this world, whatever its spawn.
				continue;
			}
			if (spawn != null && candidate.spawn() != null && Arrays.equals(spawn, candidate.spawn())) {
				sameSpawn = later(sameSpawn, candidate);
			} else if (candidate.spawn() == null) {
				unmarked = later(unmarked, candidate);
			}
		}
		if (best != null) {
			return best.id();
		}
		if (sameSpawn != null) {
			return sameSpawn.id();
		}
		return unmarked == null ? null : unmarked.id();
	}

	private static Candidate later(@Nullable Candidate current, Candidate other) {
		return current == null || other.lastSeen() > current.lastSeen() ? other : current;
	}

	/** How many columns a world's saved map has where the samples are, and how many of them are the same. */
	private static int[] compare(Path folder, List<Sample> samples, boolean ceiling, Registry<Biome> biomes) {
		Map<Long, MapChunk[]> regions = new HashMap<>();
		int compared = 0;
		int same = 0;
		List<Sample> sorted = samples.stream().sorted(Comparator.comparingLong(sample -> MapRegion.key(sample.chunkX() >> MapRegion.SHIFT, sample.chunkZ() >> MapRegion.SHIFT))).toList();
		for (Sample sample : sorted) {
			int regionX = sample.chunkX() >> MapRegion.SHIFT;
			int regionZ = sample.chunkZ() >> MapRegion.SHIFT;
			long key = MapRegion.key(regionX, regionZ);
			MapChunk[] saved = regions.get(key);
			if (saved == null && !regions.containsKey(key)) {
				if (regions.size() >= MAX_REGIONS) {
					continue;
				}
				saved = read(MapRegionFile.path(folder, regionX, regionZ), ceiling, biomes);
				regions.put(key, saved);
			}
			MapChunk old = saved == null ? null : saved[MapRegion.index(sample.chunkX(), sample.chunkZ())];
			if (old == null) {
				continue;
			}
			for (int c = 0; c < MapChunk.AREA; c++) {
				if (old.top(c) == MapChunk.NONE || sample.chunk().top(c) == MapChunk.NONE) {
					continue;
				}
				compared++;
				if (old.top(c) == sample.chunk().top(c) && old.topY(c) == sample.chunk().topY(c)) {
					same++;
				}
			}
		}
		return new int[] {compared, same};
	}

	/** A saved region's chunks; in a dimension with a ceiling, not from files that show its roof (#221). */
	private static MapChunk @Nullable [] read(Path file, boolean ceiling, Registry<Biome> biomes) {
		try {
			MapRegionFile.Contents contents = MapRegionFile.readFor(file, biomes, ceiling);
			return contents == null ? null : contents.chunks();
		} catch (IOException | RuntimeException exception) {
			return null;
		}
	}
}
