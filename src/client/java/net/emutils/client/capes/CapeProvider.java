package net.emutils.client.capes;

import com.mojang.authlib.GameProfile;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.config.EMUtilsConfig;
import org.jspecify.annotations.Nullable;

public enum CapeProvider {
	OPTIFINE("OptiFine"),
	LABYMOD("LabyMod"),
	COSMETICA("Cosmetica"),
	MINECRAFTCAPES("MinecraftCapes"),
	CLOAKSPLUS("Cloaks+");

	private final String displayName;

	CapeProvider(String displayName) {
		this.displayName = displayName;
	}

	public String displayName() {
		return displayName;
	}

	public boolean enabled(EMUtilsConfig config) {
		return switch (this) {
			case OPTIFINE -> config.capeOptifine();
			case LABYMOD -> config.capeLabyMod();
			case COSMETICA -> config.capeCosmetica();
			case MINECRAFTCAPES -> config.capeMinecraftCapes();
			case CLOAKSPLUS -> config.capeCloaksPlus();
		};
	}

	public @Nullable String requestUrl(GameProfile profile) {
		if (!enabled(EMUtilsClient.config())) {
			return null;
		}

		String name = profile.name();
		if (name == null || name.isBlank()) {
			return null;
		}

		return switch (this) {
			case OPTIFINE -> "http://s.optifine.net/capes/" + name + ".png";
			case LABYMOD -> "https://dl.labymod.net/capes/" + profile.id();
			case COSMETICA -> "https://api.cosmetica.cc/v2/get/info?uuid=" + profile.id();
			case MINECRAFTCAPES -> "https://api.minecraftcapes.net/profile/" + profile.id().toString().replace("-", "");
			case CLOAKSPLUS -> "http://161.35.130.99/capes/" + name + ".png";
		};
	}
}
