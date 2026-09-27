package net.emutils.client.emutils.capes;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
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

	/**
	 * A Cosmetica 2 player (#52): the cape of the outfit a Cosmetica user wears ({@code user.outfit.cloak}),
	 * which can be animated: its frames are stacked top to bottom, each shown for {@code ticksPerFrame}.
	 * Players who aren't Cosmetica users, and capes Cosmetica mirrors from other services
	 * ({@code externalCape}), are left to the other providers.
	 */
	private static @Nullable LoadedCape loadCosmetica(UUID profileId, String requestUrl) {
		try {
			JsonObject root = GSON.fromJson(downloadText(requestUrl), JsonObject.class);
			JsonObject cloak = object(object(object(root, "user"), "outfit"), "cloak");
			if (cloak == null || !cloak.has("texture") || cloak.get("texture").isJsonNull()) {
				return null;
			}
			String textureUrl = cloak.get("texture").getAsString();
			int frames = cloak.has("frames") ? Math.max(1, cloak.get("frames").getAsInt()) : 1;
			int ticksPerFrame = cloak.has("ticksPerFrame") ? Math.max(1, cloak.get("ticksPerFrame").getAsInt()) : 1;
			try (InputStream inputStream = openStream(textureUrl)) {
				return registerImage(profileId, CapeProvider.COSMETICA, textureUrl, NativeImage.read(inputStream), false, frames, ticksPerFrame);
			}
		} catch (IOException | RuntimeException exception) {
			EMUtilsClient.LOGGER.debug("Failed to load Cosmetica cape from {}", requestUrl, exception);
			return null;
		}
	}

	private static @Nullable JsonObject object(@Nullable JsonObject parent, String key) {
		if (parent == null || !parent.has(key) || !parent.get(key).isJsonObject()) {
			return null;
		}
		return parent.getAsJsonObject(key);
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
		return registerImage(profileId, provider, sourceUrl, image, labymod, 1, 1);
	}

	/** Registers the cape; with more than one frame, {@code image} holds them top to bottom and the cape animates. */
	private static @Nullable LoadedCape registerImage(
		UUID profileId,
		CapeProvider provider,
		String sourceUrl,
		NativeImage image,
		boolean labymod,
		int frameCount,
		int ticksPerFrame
	) throws IOException {
		if (labymod && isLabyModPlaceholder(image)) {
			image.close();
			return null;
		}

		List<NativeImage> frames = splitFrames(image, frameCount);
		NativeImage capeImage = new NativeImage(frames.getFirst().getWidth(), frames.getFirst().getHeight(), true);
		capeImage.copyFrom(frames.getFirst());
		if (frames.size() == 1) {
			frames.getFirst().close();
		}
		Identifier textureId = Identifier.fromNamespaceAndPath(
			EMUtilsClient.MOD_ID,
			"capes/" + provider.name().toLowerCase() + "/" + profileId.toString().replace("-", "")
		);
		CapeTextureAsset textureAsset = new CapeTextureAsset(textureId);
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			capeImage.close();
			return null;
		}

		CompletableFuture<LoadedCape> registration = new CompletableFuture<>();
		client.execute(() -> {
			try {
				DynamicTexture texture = new DynamicTexture(textureId::toString, capeImage);
				client.getTextureManager().register(textureId, texture);
				if (frames.size() > 1) {
					CapeAnimations.start(textureId, texture, frames, ticksPerFrame);
				} else {
					CapeAnimations.stop(textureId);
				}
				registration.complete(new LoadedCape(textureAsset, provider));
			} catch (RuntimeException exception) {
				capeImage.close();
				if (frames.size() > 1) {
					frames.forEach(NativeImage::close);
				}
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

	/**
	 * Splits an animated cape's frames, stacked top to bottom, into cape-sized images; one image when it
	 * isn't animated. Each is normalized like a single cape. Closes {@code image}.
	 */
	private static List<NativeImage> splitFrames(NativeImage image, int frameCount) {
		int frameHeight = image.getHeight() / Math.max(1, frameCount);
		if (frameCount <= 1 || frameHeight <= 0) {
			return List.of(normalizeCape(image));
		}
		List<NativeImage> frames = new ArrayList<>(frameCount);
		for (int frame = 0; frame < frameCount; frame++) {
			NativeImage single = new NativeImage(image.getWidth(), frameHeight, true);
			for (int x = 0; x < image.getWidth(); x++) {
				for (int y = 0; y < frameHeight; y++) {
					single.setPixel(x, y, image.getPixel(x, frame * frameHeight + y));
				}
			}
			frames.add(normalizeCape(single));
		}
		image.close();
		return frames;
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
				normalized.setPixel(x, y, image.getPixel(x, y));
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
			image.getPixel(0, 0),
			image.getPixel(image.getWidth() - 1, 0),
			image.getPixel(0, image.getHeight() - 1),
			image.getPixel(image.getWidth() - 1, image.getHeight() - 1)
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
		Minecraft client = Minecraft.getInstance();
		Proxy proxy = client == null ? Proxy.NO_PROXY : client.getProxy();
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

	private record CapeTextureAsset(Identifier id) implements ClientAsset.Texture {
		@Override
		public Identifier texturePath() {
			return id;
		}
	}

	record LoadedCape(ClientAsset.Texture texture, CapeProvider provider) {
	}
}
