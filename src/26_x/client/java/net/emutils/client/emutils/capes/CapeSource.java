package net.emutils.client.emutils.capes;

import java.util.List;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.util.EMUtilsTexts;
import org.jspecify.annotations.Nullable;

/**
 * An entry in the cape priority list (#52): the official Minecraft cape, or one of the cape providers.
 * For each player the first entry in the list that has a cape for them wins.
 */
public enum CapeSource {
	MINECRAFT(EMUtilsTexts.OPTION_CAPE_MINECRAFT, null),
	OPTIFINE(EMUtilsTexts.OPTION_CAPE_OPTIFINE, CapeProvider.OPTIFINE),
	LABYMOD(EMUtilsTexts.OPTION_CAPE_LABYMOD, CapeProvider.LABYMOD),
	COSMETICA(EMUtilsTexts.OPTION_CAPE_COSMETICA, CapeProvider.COSMETICA),
	MINECRAFTCAPES(EMUtilsTexts.OPTION_CAPE_MINECRAFTCAPES, CapeProvider.MINECRAFTCAPES),
	CLOAKSPLUS(EMUtilsTexts.OPTION_CAPE_CLOAKSPLUS, CapeProvider.CLOAKSPLUS);

	/**
	 * The providers in their old fixed order, then Minecraft: a third-party cape replaces an official
	 * one, as before the priority list existed.
	 */
	public static final List<CapeSource> DEFAULT_ORDER = List.of(OPTIFINE, LABYMOD, COSMETICA, MINECRAFTCAPES, CLOAKSPLUS, MINECRAFT);

	private final String labelKey;
	private final @Nullable CapeProvider provider;

	CapeSource(String labelKey, @Nullable CapeProvider provider) {
		this.labelKey = labelKey;
		this.provider = provider;
	}

	public String labelKey() {
		return labelKey;
	}

	/** The provider that loads this source's capes, or null for the official Minecraft cape. */
	public @Nullable CapeProvider provider() {
		return provider;
	}

	public boolean enabled(EMUtilsConfig config) {
		return provider == null ? config.capeMinecraft() : provider.enabled(config);
	}

	public void setEnabled(EMUtilsConfig config, boolean enabled) {
		switch (this) {
			case MINECRAFT -> config.setCapeMinecraft(enabled);
			case OPTIFINE -> config.setCapeOptifine(enabled);
			case LABYMOD -> config.setCapeLabyMod(enabled);
			case COSMETICA -> config.setCapeCosmetica(enabled);
			case MINECRAFTCAPES -> config.setCapeMinecraftCapes(enabled);
			case CLOAKSPLUS -> config.setCapeCloaksPlus(enabled);
		}
	}

	public static @Nullable CapeSource fromName(@Nullable String name) {
		if (name != null) {
			for (CapeSource source : values()) {
				if (source.name().equals(name)) {
					return source;
				}
			}
		}
		return null;
	}
}
