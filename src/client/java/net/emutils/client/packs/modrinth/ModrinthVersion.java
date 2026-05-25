package net.emutils.client.packs.modrinth;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record ModrinthVersion(
	String id,
	String name,
	@SerializedName("version_number") String versionNumber,
	@SerializedName("version_type") String versionType,
	@SerializedName("date_published") String datePublished,
	@SerializedName("game_versions") List<String> gameVersions,
	List<String> loaders,
	List<ModrinthFile> files
) {
	public boolean supports(String minecraftVersion) {
		return gameVersions != null && gameVersions.contains(minecraftVersion) && files != null && !files.isEmpty();
	}

	@Nullable
	public ModrinthFile primaryFile() {
		if (files == null || files.isEmpty()) {
			return null;
		}

		return files.stream().filter(ModrinthFile::isPrimary).findFirst().orElse(files.getFirst());
	}
}
