package net.emutils.client.emskyblock.sacks;

import java.util.List;

public record SkyblockSackChange(
	int delta,
	String itemId,
	String itemName,
	List<String> sackNames
) {
}
