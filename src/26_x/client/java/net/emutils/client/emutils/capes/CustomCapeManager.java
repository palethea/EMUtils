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
import net.minecraft.client.Minecraft;
import net.minecraft.core.ClientAsset;
import org.jspecify.annotations.Nullable;

public final class CustomCapeManager {
	private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(thread -> {
		Thread worker = new Thread(thread, "EMUtils-Capes");
		worker.setDaemon(true);
		return worker;
	});

	private static final Map<UUID, CapePlayerHandler> HANDLERS = new ConcurrentHashMap<>();
	/** How long a player without a cape waits before being looked up again. */
	private static final long RETRY_WITHOUT_CAPE_MILLIS = 5 * 60_000L;

	private CustomCapeManager() {
	}

	/**
	 * The cape to show for {@code profile}, given the official one from its skin ({@code official}, null
	 * without one): the first entry of the cape priority list (#52) that has a cape for them. Minecraft's
	 * place in the list decides whether the official cape wins, and switched off it's hidden. Returns
	 * {@code official} itself when that's the one to show, and null for no cape.
	 */
	public static ClientAsset.@Nullable Texture capeTextureFor(GameProfile profile, ClientAsset.@Nullable Texture official) {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || !config.customCapes()) {
			return official;
		}
		CapePlayerHandler handler = get(profile);
		ClientAsset.Texture above = handler == null ? null : handler.above;
		ClientAsset.Texture below = handler == null ? null : handler.below;
		ClientAsset.Texture shown;
		if (above != null) {
			shown = above;
		} else if (official != null && config.capeMinecraft()) {
			shown = official;
		} else {
			shown = below;
		}
		if (handler != null) {
			handler.showingCustom = shown != null && shown != official;
		}
		return shown;
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

	/** Whether {@code profile} shows a cape from a provider, rather than its official one or none. */
	public static boolean hasCustomCape(GameProfile profile) {
		CapePlayerHandler handler = get(profile);
		return handler != null && handler.showingCustom;
	}

	public static void reload() {
		// Every cape is looked up again, and animated ones start over, so no frames linger for a cape
		// that won't show any more (such as after switching its provider off).
		Minecraft.getInstance().execute(CapeAnimations::clear);
		for (CapePlayerHandler handler : HANDLERS.values()) {
			handler.reset();
			handler.requestLoad();
		}
	}

	public static void clear() {
		HANDLERS.clear();
		Minecraft.getInstance().execute(CapeAnimations::clear);
	}

	/**
	 * Resolves {@code profile}'s cape right away, on this thread, and returns the provider it came from, or
	 * null without one; used by UI snapshots.
	 */
	public static @Nullable String resolveForSnapshot(GameProfile profile) {
		CapePlayerHandler handler = new CapePlayerHandler(profile);
		handler.resolveCape();
		HANDLERS.put(profile.id(), handler);
		return handler.hasCape() ? handler.provider().displayName() : null;
	}

	private static boolean isValidProfile(GameProfile profile) {
		return profile.id() != null && profile.name() != null && !profile.name().isBlank();
	}

	static final class CapePlayerHandler {
		private final GameProfile profile;
		private final AtomicBoolean loading = new AtomicBoolean();
		/** Whether the capes have been looked up since the last reset, and when. */
		private volatile boolean resolved;
		private volatile long resolvedAtMillis;
		/** The first cape found among the sources above Minecraft in the priority list, and below it. */
		private volatile ClientAsset.@Nullable Texture above;
		private volatile ClientAsset.@Nullable Texture below;
		private volatile @Nullable CapeProvider provider;
		/** Whether the last skin lookup showed one of these capes, for the renderer. */
		private volatile boolean showingCustom;

		private CapePlayerHandler(GameProfile profile) {
			this.profile = profile;
		}

		/**
		 * Looks the capes up once; a player without one is looked up again after a while, in case a
		 * provider couldn't be reached, or they put one on since.
		 */
		void requestLoad() {
			boolean fresh = resolved && (hasCape() || System.currentTimeMillis() - resolvedAtMillis < RETRY_WITHOUT_CAPE_MILLIS);
			if (fresh || !loading.compareAndSet(false, true)) {
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
			resolved = false;
			above = null;
			below = null;
			provider = null;
			showingCustom = false;
		}

		boolean hasCape() {
			return above != null || below != null;
		}

		@Nullable CapeProvider provider() {
			return provider;
		}

		/**
		 * Looks up the enabled providers in priority order, until it has the first cape above Minecraft, or
		 * failing that the first below it: which of that and the official cape shows is only known once
		 * the skin is in, so the choice is made in {@link #capeTextureFor}. A provider without a cape for
		 * this player falls through to the next one, for every player, the local one too.
		 */
		private void resolveCape() {
			EMUtilsConfig config = EMUtilsClient.config();
			ClientAsset.Texture foundAbove = null;
			ClientAsset.Texture foundBelow = null;
			CapeProvider foundProvider = null;
			if (config != null && config.customCapes()) {
				boolean pastMinecraft = false;
				for (CapeSource source : config.capeOrder()) {
					CapeProvider candidate = source.provider();
					if (candidate == null) {
						pastMinecraft = true;
						continue;
					}
					if (!candidate.enabled(config)) {
						continue;
					}
					CapeTextureLoader.LoadedCape loadedCape = CapeTextureLoader.load(candidate, profile);
					if (loadedCape == null) {
						continue;
					}
					foundProvider = loadedCape.provider();
					EMUtilsClient.LOGGER.info("Loaded {} cape for {}", candidate.displayName(), profile.name());
					if (pastMinecraft) {
						foundBelow = loadedCape.texture();
					} else {
						foundAbove = loadedCape.texture();
					}
					break;
				}
			}
			above = foundAbove;
			below = foundBelow;
			provider = foundProvider;
			resolvedAtMillis = System.currentTimeMillis();
			resolved = true;
		}
	}
}
