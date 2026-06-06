package net.emutils.client.emutils.capes;

import com.mojang.authlib.GameProfile;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.minecraft.core.ClientAsset;
import org.jspecify.annotations.Nullable;

public final class CustomCapeManager {
	private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(thread -> {
		Thread worker = new Thread(thread, "EMUtils-Capes");
		worker.setDaemon(true);
		return worker;
	});

	private static final Map<UUID, CapePlayerHandler> HANDLERS = new ConcurrentHashMap<>();

	private CustomCapeManager() {
	}

	public static ClientAsset.Texture capeTextureFor(GameProfile profile) {
		CapePlayerHandler handler = get(profile);
		if (handler == null || !handler.hasCape()) {
			return null;
		}
		return handler.capeTexture();
	}

	public static void onLoadTexture(GameProfile profile) {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || !config.customCapes()) {
			return;
		}

		if (!isValidProfile(profile)) {
			return;
		}

		HANDLERS.computeIfAbsent(profile.id(), ignored -> new CapePlayerHandler(profile)).requestLoad();
	}

	public static @Nullable CapePlayerHandler get(GameProfile profile) {
		return profile == null ? null : HANDLERS.get(profile.id());
	}

	public static boolean hasCustomCape(GameProfile profile) {
		CapePlayerHandler handler = get(profile);
		return handler != null && handler.hasCape();
	}

	public static void reload() {
		for (CapePlayerHandler handler : HANDLERS.values()) {
			handler.reset();
			handler.requestLoad();
		}
	}

	public static void clear() {
		HANDLERS.clear();
	}

	private static boolean isValidProfile(GameProfile profile) {
		return profile.id() != null && profile.name() != null && !profile.name().isBlank();
	}

	static final class CapePlayerHandler {
		private final GameProfile profile;
		private final AtomicBoolean loading = new AtomicBoolean();
		private volatile boolean hasCape;
		private volatile ClientAsset.Texture capeTexture;
		private volatile CapeProvider provider;

		private CapePlayerHandler(GameProfile profile) {
			this.profile = profile;
		}

		void requestLoad() {
			if (hasCape() || !loading.compareAndSet(false, true)) {
				return;
			}

			EXECUTOR.submit(() -> {
				try {
					resolveCape();
				} finally {
					loading.set(false);
				}
			});
		}

		void reset() {
			hasCape = false;
			capeTexture = null;
			provider = null;
		}

		boolean hasCape() {
			return hasCape && capeTexture != null;
		}

		ClientAsset.Texture capeTexture() {
			return capeTexture;
		}

		CapeProvider provider() {
			return provider;
		}

		private void resolveCape() {
			EMUtilsConfig config = EMUtilsClient.config();
			if (config == null || !config.customCapes()) {
				hasCape = false;
				capeTexture = null;
				provider = null;
				return;
			}

			CapePreferredProvider preferred = config.capePreferredProvider();
			for (CapeProvider candidate : CapePreferredProvider.loadOrder(preferred, profile)) {
				if (!candidate.enabled(config)) {
					continue;
				}

				CapeTextureLoader.LoadedCape loadedCape = CapeTextureLoader.load(candidate, profile);
				if (loadedCape != null) {
					hasCape = true;
					capeTexture = loadedCape.texture();
					provider = loadedCape.provider();
					EMUtilsClient.LOGGER.info("Loaded {} cape for {}", candidate.displayName(), profile.name());
					return;
				}
			}

			hasCape = false;
			capeTexture = null;
			provider = null;
		}
	}
}
