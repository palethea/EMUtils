package net.emutils.client.emutils.packs.modrinth;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.packs.PackType;

public final class ModrinthClient {
	private static final String API = "https://api.modrinth.com/v2";
	private static final Gson GSON = new Gson();
	private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build();

	public List<ModrinthSearchResult> search(PackType type, String query, String minecraftVersion, int limit) throws IOException, InterruptedException {
		String index = query == null || query.isBlank() ? "downloads" : "relevance";
		return search(type, query, minecraftVersion, limit, index);
	}

	public List<ModrinthSearchResult> search(PackType type, String query, String minecraftVersion, int limit, String index) throws IOException, InterruptedException {
		String facets = "[[\"project_type:" + type.modrinthProjectType() + "\"],[\"versions:" + minecraftVersion + "\"]]";
		String url = API + "/search?limit=" + limit
			+ "&index=" + encode(index)
			+ "&query=" + encode(query == null ? "" : query)
			+ "&facets=" + encode(facets);
		ModrinthSearchResponse response = readJson(url, ModrinthSearchResponse.class);
		return response == null || response.hits == null ? List.of() : response.hits;
	}

	public ModrinthVersion newestCompatibleVersion(String projectId, String minecraftVersion) throws IOException, InterruptedException {
		ModrinthVersion[] versions = readJson(API + "/project/" + encodePath(projectId) + "/version", ModrinthVersion[].class);
		if (versions == null) {
			throw new IOException("Modrinth returned no versions.");
		}

		return Arrays.stream(versions)
			.filter(version -> version.supports(minecraftVersion))
			.sorted(Comparator.comparing(ModrinthVersion::datePublished, Comparator.nullsLast(String::compareTo)).reversed())
			.findFirst()
			.orElseThrow(() -> new IOException("No compatible version for Minecraft " + minecraftVersion + "."));
	}

	public void download(ModrinthFile file, Path target) throws IOException, InterruptedException {
		HttpRequest request = request(file.url()).build();
		HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(target));
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw new IOException("Modrinth download failed with HTTP " + response.statusCode() + ".");
		}
	}

	private <T> T readJson(String url, Class<T> type) throws IOException, InterruptedException {
		HttpRequest request = request(url).header("Accept", "application/json").build();
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw new IOException("Modrinth request failed with HTTP " + response.statusCode() + ".");
		}

		try {
			return GSON.fromJson(response.body(), type);
		} catch (JsonParseException exception) {
			throw new IOException("Modrinth returned malformed JSON.", exception);
		}
	}

	private static HttpRequest.Builder request(String url) {
		return HttpRequest.newBuilder(URI.create(url))
			.timeout(Duration.ofSeconds(30))
			.header("User-Agent", "palethea/EMUtils/" + EMUtilsClient.MOD_ID + " (Minecraft client mod)");
	}

	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private static String encodePath(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
	}

	private record ModrinthSearchResponse(List<ModrinthSearchResult> hits) {
	}
}
