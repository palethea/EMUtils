package net.emutils.client.skyblock.sacks;

import java.util.List;

public record SkyblockSackChangeBatch(
	List<SkyblockSackChange> changes,
	boolean otherItemsAdded,
	boolean otherItemsRemoved,
	long timestampMs
) {
	public boolean isEmpty() {
		return changes.isEmpty() && !otherItemsAdded && !otherItemsRemoved;
	}
}
