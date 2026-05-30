package net.emutils.client.emskyblock.context;

public record SkyblockProfileModes(
	boolean guest,
	boolean coop,
	boolean ironman,
	boolean stranded,
	boolean bingo
) {
	public static final SkyblockProfileModes EMPTY = new SkyblockProfileModes(false, false, false, false, false);

	public boolean noTrade() {
		return ironman || stranded || bingo;
	}

	public SkyblockProfileModes withGuest(boolean value) {
		return value == guest ? this : new SkyblockProfileModes(value, coop, ironman, stranded, bingo);
	}

	public SkyblockProfileModes withCoop(boolean value) {
		return value == coop ? this : new SkyblockProfileModes(guest, value, ironman, stranded, bingo);
	}

	public SkyblockProfileModes withIronman(boolean value) {
		return value == ironman ? this : new SkyblockProfileModes(guest, coop, value, stranded, bingo);
	}

	public SkyblockProfileModes withStranded(boolean value) {
		return value == stranded ? this : new SkyblockProfileModes(guest, coop, ironman, value, bingo);
	}

	public SkyblockProfileModes withBingo(boolean value) {
		return value == bingo ? this : new SkyblockProfileModes(guest, coop, ironman, stranded, value);
	}
}
