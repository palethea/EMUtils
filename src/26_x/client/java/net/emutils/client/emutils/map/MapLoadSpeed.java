package net.emutils.client.emutils.map;

import net.emutils.client.emutils.util.EMUtilsTexts;
import org.jspecify.annotations.Nullable;

/**
 * How hard the map works to load (#239). Normal paces itself so the game stays smooth; Fast and Fastest spend
 * more of the CPU and GPU so a big explored or pre-generated world shows sooner when you zoom out: more threads
 * draw tiles and region overviews, more of them at once and more often, overviews are read from disk side by
 * side, more tiles go to the GPU each frame, and more of each tick goes to regions just read and to chunks
 * brought in from a singleplayer world's files.
 */
public enum MapLoadSpeed {
	NORMAL(EMUtilsTexts.MAP_LOAD_SPEED_NORMAL),
	FAST(EMUtilsTexts.MAP_LOAD_SPEED_FAST),
	FASTEST(EMUtilsTexts.MAP_LOAD_SPEED_FASTEST);

	private static final int CORES = Runtime.getRuntime().availableProcessors();

	private final String labelKey;

	MapLoadSpeed(String labelKey) {
		this.labelKey = labelKey;
	}

	public String labelKey() {
		return labelKey;
	}

	/** Threads drawing tiles and overviews: Normal's two, else half the cores, else all but two. */
	int bakerThreads() {
		return switch (this) {
			case NORMAL -> 2;
			case FAST -> Math.max(2, CORES / 2);
			case FASTEST -> Math.max(2, CORES - 2);
		};
	}

	/** Tiles of one map being drawn at once. */
	int tilesBaking() {
		return this == NORMAL ? 4 : bakerThreads() * 2;
	}

	/** Finished tiles handed to the GPU each frame. */
	int uploadsPerFrame() {
		return switch (this) {
			case NORMAL -> 3;
			case FAST -> 6;
			case FASTEST -> 12;
		};
	}

	/** Overviews drawn at once, while no tile on screen is waiting. */
	int overviewsBaking() {
		return this == NORMAL ? 2 : bakerThreads();
	}

	/** Overviews drawn at once while tiles on screen are waiting, so those don't wait behind overviews. */
	int overviewsBakingWhileBusy() {
		return this == NORMAL ? 1 : Math.max(1, bakerThreads() / 2);
	}

	/** How often, in ticks, the overviews waiting to be drawn are looked at. */
	int overviewEveryTicks() {
		return switch (this) {
			case NORMAL -> 4;
			case FAST -> 3;
			case FASTEST -> 1;
		};
	}

	/** Regions read in at a time only to draw their overviews again. */
	int redrawLoads() {
		return switch (this) {
			case NORMAL -> 4;
			case FAST -> 8;
			case FASTEST -> 16;
		};
	}

	/** Threads reading overviews from disk, for the far zoom levels. */
	int overviewReaders() {
		return switch (this) {
			case NORMAL -> 1;
			case FAST -> 2;
			case FASTEST -> 4;
		};
	}

	/** Threads unpacking and sampling chunks brought in from a singleplayer world's files. */
	int importWorkers() {
		return switch (this) {
			case NORMAL -> 1;
			case FAST -> Math.max(2, CORES / 3);
			case FASTEST -> Math.max(2, CORES - 2);
		};
	}

	/** Chunks being read from a singleplayer world's files at once. */
	int importReadsInFlight() {
		return switch (this) {
			case NORMAL -> 4;
			case FAST -> 16;
			case FASTEST -> 32;
		};
	}

	/** How soon a region's overview may be drawn again while it fills in. */
	long overviewMinMillis() {
		return switch (this) {
			case NORMAL -> 5_000L;
			case FAST -> 3_000L;
			case FASTEST -> 1_000L;
		};
	}

	/** Time per tick for the looks of blocks in regions just read from disk. */
	long prepareBudgetNanos() {
		return switch (this) {
			case NORMAL -> 2_000_000L;
			case FAST -> 4_000_000L;
			case FASTEST -> 8_000_000L;
		};
	}

	/** Time per tick for sampling chunks, loaded or brought in from a singleplayer world's files. */
	long sampleBudgetNanos() {
		return switch (this) {
			case NORMAL -> 3_000_000L;
			case FAST -> 5_000_000L;
			case FASTEST -> 8_000_000L;
		};
	}

	public static MapLoadSpeed fromName(@Nullable String name) {
		if (name != null) {
			for (MapLoadSpeed speed : values()) {
				if (speed.name().equalsIgnoreCase(name)) {
					return speed;
				}
			}
		}
		return NORMAL;
	}
}
