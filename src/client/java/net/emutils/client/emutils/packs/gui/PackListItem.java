package net.emutils.client.emutils.packs.gui;

import net.emutils.client.emutils.packs.InstalledPack;
import net.emutils.client.emutils.packs.PackType;
import net.emutils.client.emutils.packs.modrinth.ModrinthSearchResult;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

public record PackListItem(PackType type, @Nullable ModrinthSearchResult result, @Nullable InstalledPack installed) {
	public static PackListItem result(PackType type, ModrinthSearchResult result, @Nullable InstalledPack installed) {
		return new PackListItem(type, result, installed);
	}

	public static PackListItem installed(InstalledPack installed) {
		return new PackListItem(installed.type(), null, installed);
	}

	public String title() {
		if (result != null) {
			return result.displayTitle();
		}
		if (installed != null && installed.record() != null && installed.record().title() != null && !installed.record().title().isBlank()) {
			return installed.record().title();
		}
		return installed == null ? "" : installed.filename();
	}

	public String description() {
		if (result != null && result.description() != null) {
			return result.description();
		}
		if (installed != null && installed.record() != null) {
			return "Installed by EMUtils";
		}
		return "Local pack";
	}

	public String author() {
		return result == null || result.author() == null ? "" : result.author();
	}

	public @Nullable String iconUrl() {
		if (result != null) {
			return result.iconUrl();
		}
		if (installed != null && installed.record() != null) {
			return installed.record().iconUrl();
		}
		return null;
	}

	public Identifier fallbackIcon() {
		return type == PackType.RESOURCE ? PackIcons.RESOURCE_PACK : PackIcons.SHADER;
	}
}
