package net.emutils.client.emskyblock.context;

import org.jspecify.annotations.Nullable;

public sealed interface SkyblockEvent permits
	SkyblockEvent.HypixelJoin,
	SkyblockEvent.HypixelLeave,
	SkyblockEvent.ProfileJoin,
	SkyblockEvent.IslandJoin,
	SkyblockEvent.IslandLeave,
	SkyblockEvent.AreaChange,
	SkyblockEvent.SnapshotUpdate {

	record HypixelJoin(boolean alpha) implements SkyblockEvent {
	}

	record HypixelLeave() implements SkyblockEvent {
	}

	record ProfileJoin(String profileName, @Nullable String previousProfile) implements SkyblockEvent {
	}

	record IslandJoin(SkyblockIsland island, SkyblockIsland previousIsland) implements SkyblockEvent {
	}

	record IslandLeave(SkyblockIsland island) implements SkyblockEvent {
	}

	record AreaChange(@Nullable String area, @Nullable String previousArea) implements SkyblockEvent {
	}

	record SnapshotUpdate(SkyblockSnapshot snapshot, SkyblockSnapshot previous) implements SkyblockEvent {
	}
}
