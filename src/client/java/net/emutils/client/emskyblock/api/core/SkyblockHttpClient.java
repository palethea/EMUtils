package net.emutils.client.emskyblock.api.core;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import net.emutils.client.EMUtilsClient;

public final class SkyblockHttpClient {
	private static final Gson GSON = new Gson();

	private final HttpClient httpClient = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(10))
		.followRedirects(HttpClient.Redirect.NORMAL)
		.build();

	public JsonObject getJson(String url, Duration timeout) throws IOException, InterruptedException, SkyblockApiException {
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
			.timeout(timeout)
			.header("Accept", "application/json")
			.header("User-Agent", "mwsk75996/EMUtils/" + EMUtilsClient.MOD_ID + " (Minecraft client mod)")
			.GET()
			.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		int status = response.statusCode();
		if (status < 200 || status >= 300) {
			throw new SkyblockApiException("HTTP " + status);
		}

		try {
			JsonObject root = GSON.fromJson(response.body(), JsonObject.class);
			if (root == null) {
				throw new SkyblockApiException("empty JSON response");
			}
			return root;
		} catch (JsonParseException exception) {
			throw new SkyblockApiException("malformed JSON response", exception);
		}
	}

	public static final class SkyblockApiException extends Exception {
		public SkyblockApiException(String message) {
			super(message);
		}

		public SkyblockApiException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
