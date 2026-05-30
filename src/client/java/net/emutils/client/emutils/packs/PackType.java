package net.emutils.client.emutils.packs;

import net.emutils.client.emhelpers.util.EMUtilsTexts;

public enum PackType {
	RESOURCE("resourcepack", "resourcepacks", EMUtilsTexts.PACK_TAB_RESOURCE_PACKS),
	SHADER("shader", "shaderpacks", EMUtilsTexts.PACK_TAB_SHADER_PACKS);

	private final String modrinthProjectType;
	private final String folderName;
	private final String titleKey;

	PackType(String modrinthProjectType, String folderName, String titleKey) {
		this.modrinthProjectType = modrinthProjectType;
		this.folderName = folderName;
		this.titleKey = titleKey;
	}

	public String modrinthProjectType() {
		return modrinthProjectType;
	}

	public String folderName() {
		return folderName;
	}

	public String titleKey() {
		return titleKey;
	}
}
