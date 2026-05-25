package net.emutils.client.packs.modrinth;

import com.google.gson.annotations.SerializedName;

public record ModrinthSearchResult(
	@SerializedName("project_id") String projectId,
	String slug,
	String title,
	String description,
	String author,
	int downloads,
	@SerializedName("icon_url") String iconUrl,
	@SerializedName("project_type") String projectType
) {
	public String displayTitle() {
		return title == null || title.isBlank() ? slug : title;
	}
}
