package net.emutils.client.capes;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import net.emutils.client.util.EMUtilsTexts;
import net.minecraft.client.MinecraftClient;
import org.jspecify.annotations.Nullable;

public enum CapePreferredProvider {
	AUTO(EMUtilsTexts.OPTION_CAPE_PREFERRED_AUTO, null),
	OPTIFINE(EMUtilsTexts.OPTION_CAPE_OPTIFINE, CapeProvider.OPTIFINE),
	LABYMOD(EMUtilsTexts.OPTION_CAPE_LABYMOD, CapeProvider.LABYMOD),
	COSMETICA(EMUtilsTexts.OPTION_CAPE_COSMETICA, CapeProvider.COSMETICA),
	MINECRAFTCAPES(EMUtilsTexts.OPTION_CAPE_MINECRAFTCAPES, CapeProvider.MINECRAFTCAPES),
	CLOAKSPLUS(EMUtilsTexts.OPTION_CAPE_CLOAKSPLUS, CapeProvider.CLOAKSPLUS);

	private final String labelKey;
	private final @Nullable CapeProvider provider;

	CapePreferredProvider(String labelKey, @Nullable CapeProvider provider) {
		this.labelKey = labelKey;
		this.provider = provider;
	}

	public String labelKey() {
		return labelKey;
	}

	public @Nullable CapeProvider provider() {
		return provider;
	}

	public CapePreferredProvider next() {
		CapePreferredProvider[] values = values();
		return values[(ordinal() + 1) % values.length];
	}

	public static CapePreferredProvider fromName(@Nullable String name) {
		if (name != null) {
			for (CapePreferredProvider preferred : values()) {
				if (preferred.name().equals(name)) {
					return preferred;
				}
			}
		}

		return AUTO;
	}

	public static List<CapeProvider> loadOrder(CapePreferredProvider preferred, GameProfile profile) {
		if (preferred == AUTO || preferred.provider == null) {
			return List.of(CapeProvider.values());
		}

		if (isLocalPlayer(profile)) {
			return List.of(preferred.provider);
		}

		List<CapeProvider> order = new ArrayList<>(CapeProvider.values().length);
		order.add(preferred.provider);
		for (CapeProvider candidate : CapeProvider.values()) {
			if (candidate != preferred.provider) {
				order.add(candidate);
			}
		}
		return order;
	}

	private static boolean isLocalPlayer(GameProfile profile) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) {
			return false;
		}

		if (client.player != null && profile.id().equals(client.player.getUuid())) {
			return true;
		}

		return client.getSession().getUuidOrNull() != null && profile.id().equals(client.getSession().getUuidOrNull());
	}
}
