package net.emutils.client.capes;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.emutils.client.EMUtilsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

final class CapeTextureLoader {
	private static final Gson GSON = new Gson();
	private static final int CONNECT_TIMEOUT_MS = 8_000;
	private static final int READ_TIMEOUT_MS = 8_000;
	private static final int REGISTER_TIMEOUT_MS = 10_000;

	private CapeTextureLoader() {
	}

	static @Nullable LoadedCape load(CapeProvider provider, GameProfile profile) {
		String requestUrl = provider.requestUrl(profile);
		if (requestUrl == null) {
			return null;
		}

		return switch (provider) {
			case OPTIFINE, LABYMOD, CLOAKSPLUS -> loadImage(provider, profile.id(), requestUrl, provider == CapeProvider.LABYMOD);
			case COSMETICA -> loadCosmetica(profile.id(), requestUrl);
			case MINECRAFTCAPES -> loadMinecraftCapes(profile.id(), requestUrl);
		};
	}

	private static @Nullable LoadedCape loadCosmetica(UUID profileId, String requestUrl) {
		try {
			String body = downloadText(requestUrl);
			JsonObject root = GSON.fromJson(body, JsonObject.class);
			if (root == null || !root.has("cape") || root.get("cape").isJsonNull()) {
				return null;
			}

			JsonObject cape = root.getAsJsonObject("cape");
			if (!cape.has("origin") || !"Cosmetica".equalsIgnoreCase(cape.get("origin").getAsString())) {
				return null;
			}

			String image = cape.get("image").getAsString();
			if (image.startsWith("data:image")) {
				int comma = image.indexOf(',');
				if (comma >= 0) {
					image = image.substring(comma + 1);
				}
			}

			byte[] bytes = Base64.getDecoder().decode(image);
			try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
				return registerImage(profileId, CapeProvider.COSMETICA, requestUrl, NativeImage.read(inputStream), false);
			}
		} catch (IOException | RuntimeException exception) {
			EMUtilsClient.LOGGER.debug("Failed to load Cosmetica cape from {}", requestUrl, exception);
			return null;
		}
	}

	private static @Nullable LoadedCape loadMinecraftCapes(UUID profileId, String requestUrl) {
		try {
			String body = downloadText(requestUrl);
			JsonObject root = GSON.fromJson(body, JsonObject.class);
			if (root == null || !root.has("cape_url")) {
				return null;
			}

			String capeUrl = root.get("cape_url").getAsString();
			if (capeUrl == null || capeUrl.isBlank()) {
				return null;
			}

			return loadImage(CapeProvider.MINECRAFTCAPES, profileId, capeUrl, false);
		} catch (IOException | RuntimeException exception) {
			EMUtilsClient.LOGGER.debug("Failed to load MinecraftCapes profile from {}", requestUrl, exception);
			return null;
		}
	}

	private static @Nullable LoadedCape loadImage(CapeProvider provider, UUID profileId, String imageUrl, boolean labymod) {
		try (InputStream inputStream = openStream(imageUrl)) {
			return registerImage(profileId, provider, imageUrl, NativeImage.read(inputStream), labymod);
		} catch (IOException | RuntimeException exception) {
			EMUtilsClient.LOGGER.debug("Failed to load {} cape from {}", provider.displayName(), imageUrl, exception);
			return null;
		}
	}

	private static @Nullable LoadedCape registerImage(
		UUID profileId,
		CapeProvider provider,
		String sourceUrl,
		NativeImage image,
		boolean labymod
	) throws IOException {
		if (labymod && isLabyModPlaceholder(image)) {
			image.close();
			return null;
		}

		NativeImage capeImage = normalizeCape(image);
		Identifier textureId = Identifier.of(
			EMUtilsClient.MOD_ID,
			"capes/" + provider.name().toLowerCase() + "/" + profileId.toString().replace("-", "")
		);
		AssetInfo.SkinAssetInfo textureAsset = new AssetInfo.SkinAssetInfo(textureId, sourceUrl);
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) {
			capeImage.close();
			return null;
		}

		CompletableFuture<LoadedCape> registration = new CompletableFuture<>();
		client.execute(() -> {
			try {
				client.getTextureManager().registerTexture(
					textureId,
					new NativeImageBackedTexture(textureId::toString, capeImage)
				);
				registration.complete(new LoadedCape(textureAsset, provider));
			} catch (RuntimeException exception) {
				capeImage.close();
				registration.completeExceptionally(exception);
			}
		});

		try {
			return registration.get(REGISTER_TIMEOUT_MS, TimeUnit.MILLISECONDS);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			EMUtilsClient.LOGGER.debug("Interrupted while registering {} cape texture", provider.displayName(), exception);
			return null;
		} catch (ExecutionException | TimeoutException exception) {
			EMUtilsClient.LOGGER.debug("Failed to register {} cape texture on render thread", provider.displayName(), exception);
			return null;
		}
	}

	private static NativeImage normalizeCape(NativeImage image) {
		int targetWidth = 64;
		int targetHeight = 32;
		int sourceWidth = image.getWidth();
		int sourceHeight = image.getHeight();
		while (targetWidth < sourceWidth || targetHeight < sourceHeight) {
			targetWidth *= 2;
			targetHeight *= 2;
		}

		NativeImage normalized = new NativeImage(targetWidth, targetHeight, true);
		for (int x = 0; x < sourceWidth; x++) {
			for (int y = 0; y < sourceHeight; y++) {
				normalized.setColorArgb(x, y, image.getColorArgb(x, y));
			}
		}
		image.close();
		return normalized;
	}

	private static boolean isLabyModPlaceholder(NativeImage image) {
		if (image.getWidth() <= 0 || image.getHeight() <= 0) {
			return true;
		}

		int[] sample = new int[] {
			image.getColorArgb(0, 0),
			image.getColorArgb(image.getWidth() - 1, 0),
			image.getColorArgb(0, image.getHeight() - 1),
			image.getColorArgb(image.getWidth() - 1, image.getHeight() - 1)
		};
		int expected = sample[0];
		for (int color : sample) {
			if (color != expected) {
				return false;
			}
		}

		return true;
	}

	private static String downloadText(String url) throws IOException {
		try (InputStream inputStream = openStream(url)) {
			return new String(inputStream.readAllBytes());
		}
	}

	private static InputStream openStream(String url) throws IOException {
		MinecraftClient client = MinecraftClient.getInstance();
		Proxy proxy = client == null ? Proxy.NO_PROXY : client.getNetworkProxy();
		HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection(proxy);
		connection.setRequestProperty("User-Agent", "Mozilla/5.0");
		connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
		connection.setReadTimeout(READ_TIMEOUT_MS);
		connection.setDoInput(true);
		connection.connect();
		int responseCode = connection.getResponseCode();
		if (responseCode / 100 != 2) {
			connection.disconnect();
			throw new IOException("HTTP " + responseCode + " for " + url);
		}
		return connection.getInputStream();
	}

	record LoadedCape(AssetInfo.SkinAssetInfo texture, CapeProvider provider) {
	}
}
