package net.emutils.client.spotify;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import net.emutils.client.EMUtilsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.glavo.webp.WebPException;
import org.glavo.webp.WebPFrame;
import org.glavo.webp.WebPImage;

public final class SpotifyArtLoader implements AutoCloseable {
	public enum State {
		NONE,
		LOADING,
		LOADED,
		FAILED
	}

	public record ArtResult(Identifier texture, State state, int width, int height) {
		public static ArtResult fallback(Identifier texture, State state, int size) {
			return new ArtResult(texture, state, size, size);
		}
	}

	public static final int TEXTURE_SIZE = 128;
	public static final int DISPLAY_SIZE = 32;

	private static final AtomicInteger ART_COUNTER = new AtomicInteger();

	private final MinecraftClient client;
	private final ExecutorService worker = Executors.newSingleThreadExecutor(thread -> {
		Thread workerThread = new Thread(thread, "EMUtils-Spotify-Art");
		workerThread.setDaemon(true);
		return workerThread;
	});
	private final HttpClient httpClient = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(10))
		.followRedirects(HttpClient.Redirect.NORMAL)
		.build();
	private final Map<String, LoadedArt> loaded = new ConcurrentHashMap<>();
	private final Map<String, CompletableFuture<Void>> inFlight = new ConcurrentHashMap<>();
	private final AtomicBoolean closed = new AtomicBoolean();

	public SpotifyArtLoader(MinecraftClient client) {
		this.client = client;
	}

	public ArtResult resolve(String artUrl, Identifier fallback) {
		if (artUrl == null || artUrl.isBlank()) {
			return ArtResult.fallback(fallback, State.NONE, DISPLAY_SIZE);
		}

		LoadedArt cached = loaded.get(artUrl);
		if (cached != null) {
			return new ArtResult(cached.texture(), State.LOADED, cached.width(), cached.height());
		}

		inFlight.computeIfAbsent(artUrl, url -> CompletableFuture.runAsync(() -> download(url), worker)
			.whenComplete((ignored, error) -> {
				inFlight.remove(url);
				if (error != null) {
					EMUtilsClient.LOGGER.debug("Failed to load Spotify album art from {}", url, error);
				}
			}));
		return ArtResult.fallback(fallback, State.LOADING, DISPLAY_SIZE);
	}

	@Override
	public void close() {
		if (!closed.compareAndSet(false, true)) {
			return;
		}

		worker.shutdownNow();
		inFlight.clear();
		for (LoadedArt art : loaded.values()) {
			client.getTextureManager().destroyTexture(art.texture());
		}
		loaded.clear();
	}

	private void download(String artUrl) {
		try {
			byte[] bytes = fetchBytes(artUrl);
			NativeImage image = scaleToTextureSize(decodeToPngNativeImage(bytes));
			client.execute(() -> register(artUrl, image));
		} catch (IOException | InterruptedException exception) {
			if (exception instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}

			throw new RuntimeException(exception);
		}
	}

	private byte[] fetchBytes(String artUrl) throws IOException, InterruptedException {
		if (artUrl.regionMatches(true, 0, "data:", 0, 5)) {
			return decodeDataUri(artUrl);
		}

		if (artUrl.regionMatches(true, 0, "file:", 0, 5)) {
			return Files.readAllBytes(Path.of(URI.create(artUrl)));
		}

		return downloadHttpBytes(artUrl);
	}

	private static byte[] decodeDataUri(String artUrl) throws IOException {
		int comma = artUrl.indexOf(',');
		if (comma < 0) {
			throw new IOException("Invalid data URI.");
		}

		String encoded = artUrl.substring(comma + 1).trim();
		if (encoded.isEmpty()) {
			throw new IOException("Empty data URI payload.");
		}

		try {
			return Base64.getDecoder().decode(encoded);
		} catch (IllegalArgumentException exception) {
			throw new IOException("Invalid base64 album art.", exception);
		}
	}

	private byte[] downloadHttpBytes(String artUrl) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(URI.create(artUrl))
			.timeout(Duration.ofSeconds(15))
			.header("User-Agent", "mwsk75996/EMUtils/" + EMUtilsClient.MOD_ID + " (Minecraft client mod)")
			.header("Accept", "image/png,image/jpeg,image/webp,*/*")
			.GET()
			.build();
		HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw new IOException("Album art download failed with HTTP " + response.statusCode() + ".");
		}

		try (InputStream input = response.body()) {
			return input.readAllBytes();
		}
	}

	private static NativeImage decodeToPngNativeImage(byte[] bytes) throws IOException {
		if (isPng(bytes)) {
			return NativeImage.read(bytes);
		}

		BufferedImage buffered;
		if (isWebP(bytes)) {
			try (NativeImage webp = decodeWebP(bytes)) {
				buffered = toBufferedImage(webp);
			}
		} else {
			try (ByteArrayInputStream input = new ByteArrayInputStream(bytes)) {
				buffered = ImageIO.read(input);
			}
			if (buffered == null) {
				throw new IOException("Unsupported album art format.");
			}
		}

		return encodePngNativeImage(buffered);
	}

	private static NativeImage encodePngNativeImage(BufferedImage source) throws IOException {
		BufferedImage rgba = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = rgba.createGraphics();
		try {
			graphics.drawImage(source, 0, 0, null);
		} finally {
			graphics.dispose();
		}

		ByteArrayOutputStream pngBytes = new ByteArrayOutputStream();
		if (!ImageIO.write(rgba, "png", pngBytes)) {
			throw new IOException("Failed to encode album art as PNG.");
		}

		return NativeImage.read(pngBytes.toByteArray());
	}

	private static BufferedImage toBufferedImage(NativeImage image) {
		BufferedImage buffered = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				buffered.setRGB(x, y, image.getColorArgb(x, y));
			}
		}
		return buffered;
	}

	private static boolean isPng(byte[] bytes) {
		return bytes.length >= 8
			&& bytes[0] == (byte) 0x89
			&& bytes[1] == 0x50
			&& bytes[2] == 0x4E
			&& bytes[3] == 0x47;
	}

	private static boolean isWebP(byte[] bytes) {
		return bytes.length >= 12
			&& bytes[0] == 'R'
			&& bytes[1] == 'I'
			&& bytes[2] == 'F'
			&& bytes[3] == 'F'
			&& bytes[8] == 'W'
			&& bytes[9] == 'E'
			&& bytes[10] == 'B'
			&& bytes[11] == 'P';
	}

	private static NativeImage decodeWebP(byte[] bytes) throws IOException {
		try {
			WebPImage webp = WebPImage.read(new ByteArrayInputStream(bytes));
			WebPFrame frame = webp.getFirstFrame();
			int width = frame.getWidth();
			int height = frame.getHeight();
			int[] pixels = frame.getArgbArray();
			NativeImage image = new NativeImage(NativeImage.Format.RGBA, width, height, false);
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					image.setColorArgb(x, y, pixels[y * width + x]);
				}
			}
			return image;
		} catch (WebPException exception) {
			throw new IOException("Failed to decode WebP album art.", exception);
		}
	}

	private static NativeImage scaleToTextureSize(NativeImage image) throws IOException {
		if (image.getWidth() == TEXTURE_SIZE && image.getHeight() == TEXTURE_SIZE) {
			return image;
		}

		BufferedImage source = toBufferedImage(image);
		image.close();
		BufferedImage scaled = new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = scaled.createGraphics();
		try {
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.drawImage(source, 0, 0, TEXTURE_SIZE, TEXTURE_SIZE, null);
		} finally {
			graphics.dispose();
		}

		return encodePngNativeImage(scaled);
	}

	private void register(String artUrl, NativeImage image) {
		if (closed.get()) {
			image.close();
			return;
		}

		try {
			Identifier identifier = Identifier.of(EMUtilsClient.MOD_ID, "spotify_art/" + ART_COUNTER.incrementAndGet());
			NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "EMUtils Spotify art", image);
			client.getTextureManager().registerTexture(identifier, texture);
			texture.upload();
			loaded.put(artUrl, new LoadedArt(identifier, image.getWidth(), image.getHeight()));
		} catch (RuntimeException exception) {
			image.close();
			throw exception;
		}
	}

	private record LoadedArt(Identifier texture, int width, int height) {
	}
}
