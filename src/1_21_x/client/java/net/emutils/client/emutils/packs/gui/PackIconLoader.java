package net.emutils.client.emutils.packs.gui;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.emutils.client.EMUtilsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.glavo.webp.WebPException;
import org.glavo.webp.WebPFrame;
import org.glavo.webp.WebPImage;

public final class PackIconLoader implements AutoCloseable {
	public enum State {
		NONE,
		LOADING,
		LOADED,
		FAILED
	}

	public record IconResult(Identifier texture, State state) {
	}

	private static final int MAX_ICON_SIZE = 96;
	private static final AtomicInteger ICON_COUNTER = new AtomicInteger();

	private final MinecraftClient client;
	private final Runnable onLoaded;
	private final ExecutorService worker = Executors.newFixedThreadPool(3, thread -> {
		Thread workerThread = new Thread(thread, "EMUtils-Pack-Icons");
		workerThread.setDaemon(true);
		return workerThread;
	});
	private final HttpClient httpClient = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(10))
		.followRedirects(HttpClient.Redirect.NORMAL)
		.build();
	private final Map<String, Identifier> loaded = new ConcurrentHashMap<>();
	private final Map<String, CompletableFuture<Void>> inFlight = new ConcurrentHashMap<>();
	private final Set<String> failed = ConcurrentHashMap.newKeySet();
	private final AtomicBoolean closed = new AtomicBoolean();

	public PackIconLoader(MinecraftClient client, Runnable onLoaded) {
		this.client = client;
		this.onLoaded = onLoaded;
	}

	public IconResult resolve(String iconUrl, Identifier fallback) {
		if (iconUrl == null || iconUrl.isBlank()) {
			return new IconResult(fallback, State.NONE);
		}

		Identifier cached = loaded.get(iconUrl);
		if (cached != null) {
			return new IconResult(cached, State.LOADED);
		}
		if (failed.contains(iconUrl)) {
			return new IconResult(fallback, State.FAILED);
		}

		inFlight.computeIfAbsent(iconUrl, url -> CompletableFuture.runAsync(() -> download(url), worker)
			.whenComplete((ignored, error) -> client.execute(() -> complete(url, error))));
		return new IconResult(fallback, State.LOADING);
	}

	@Override
	public void close() {
		if (!closed.compareAndSet(false, true)) {
			return;
		}

		worker.shutdownNow();
		inFlight.clear();
		failed.clear();
		for (Identifier identifier : loaded.values()) {
			client.getTextureManager().destroyTexture(identifier);
		}
		loaded.clear();
	}

	private void download(String iconUrl) {
		IOException lastFailure = null;
		for (String candidate : iconUrlCandidates(iconUrl)) {
			try {
				byte[] bytes = downloadBytes(candidate);
				NativeImage image = decodeImage(bytes);
				client.execute(() -> register(iconUrl, image));
				return;
			} catch (IOException | InterruptedException exception) {
				if (exception instanceof InterruptedException) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(exception);
				}
				lastFailure = (IOException) exception;
			}
		}

		throw new RuntimeException(lastFailure == null ? new IOException("Icon download failed.") : lastFailure);
	}

	private static List<String> iconUrlCandidates(String iconUrl) {
		List<String> candidates = new ArrayList<>();
		candidates.add(iconUrl);
		if (iconUrl.endsWith("_96.webp")) {
			candidates.add(iconUrl.substring(0, iconUrl.length() - 8) + ".png");
		} else if (iconUrl.endsWith(".webp")) {
			candidates.add(iconUrl.substring(0, iconUrl.length() - 5) + ".png");
		}
		return candidates;
	}

	private byte[] downloadBytes(String iconUrl) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(URI.create(iconUrl))
			.timeout(Duration.ofSeconds(20))
			.header("User-Agent", "palethea/EMUtils/" + EMUtilsClient.MOD_ID + " (Minecraft client mod)")
			.header("Accept", "image/png,image/jpeg,image/webp,*/*")
			.GET()
			.build();
		HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw new IOException("Icon download failed with HTTP " + response.statusCode() + ".");
		}

		try (InputStream input = response.body()) {
			return input.readAllBytes();
		}
	}

	private static NativeImage decodeImage(byte[] bytes) throws IOException {
		if (isWebP(bytes)) {
			return decodeWebP(bytes);
		}

		try {
			return scaleIfNeeded(NativeImage.read(bytes));
		} catch (IOException firstFailure) {
			if (isWebP(bytes)) {
				return decodeWebP(bytes);
			}
			throw firstFailure;
		}
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
			NativeImage image = new NativeImage(width, height, false);
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					image.setColorArgb(x, y, pixels[y * width + x]);
				}
			}
			return scaleIfNeeded(image);
		} catch (WebPException exception) {
			throw new IOException("Failed to decode WebP icon.", exception);
		}
	}

	private static NativeImage scaleIfNeeded(NativeImage image) {
		int width = image.getWidth();
		int height = image.getHeight();
		if (width <= MAX_ICON_SIZE && height <= MAX_ICON_SIZE) {
			return image;
		}

		double scale = Math.min(MAX_ICON_SIZE / (double) width, MAX_ICON_SIZE / (double) height);
		int targetWidth = Math.max(1, (int) Math.round(width * scale));
		int targetHeight = Math.max(1, (int) Math.round(height * scale));
		NativeImage scaled = new NativeImage(targetWidth, targetHeight, false);
		image.resizeSubRectTo(0, 0, width, height, scaled);
		image.close();
		return scaled;
	}

	private void register(String iconUrl, NativeImage image) {
		if (closed.get()) {
			image.close();
			return;
		}

		try {
			Identifier identifier = Identifier.of(EMUtilsClient.MOD_ID, "pack_icon/" + ICON_COUNTER.incrementAndGet());
			NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "EMUtils pack icon", image);
			client.getTextureManager().registerTexture(identifier, texture);
			loaded.put(iconUrl, identifier);
			inFlight.remove(iconUrl);
			onLoaded.run();
		} catch (RuntimeException exception) {
			image.close();
			complete(iconUrl, exception);
		}
	}

	private void complete(String iconUrl, Throwable error) {
		inFlight.remove(iconUrl);
		if (closed.get()) {
			return;
		}
		if (error != null) {
			failed.add(iconUrl);
			onLoaded.run();
		}
	}
}
