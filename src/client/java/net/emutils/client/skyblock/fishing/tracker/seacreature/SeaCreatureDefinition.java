package net.emutils.client.skyblock.fishing.tracker.seacreature;

public record SeaCreatureDefinition(
	String id,
	String displayName,
	String category,
	String chatColor,
	boolean rare
) {
}
