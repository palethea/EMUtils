package net.emutils.client.skyblock.fishing;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public final class FishingHookTextFormat {
	private FishingHookTextFormat() {
	}

	public static Text fromLegacyCodes(String raw) {
		if (raw == null || raw.isBlank()) {
			return Text.empty();
		}

		return Text.literal(raw.replace('&', '§'));
	}

	public static MutableText copyStyled(Text source) {
		return source.copy();
	}
}
