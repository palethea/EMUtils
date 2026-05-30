package net.emutils.client.emutils.screenshot;

import net.emutils.client.emhelpers.util.EMUtilsTexts;

public enum ScreenshotGallerySort {
	NEWEST_FIRST(EMUtilsTexts.GALLERY_SORT_NEWEST_FIRST),
	OLDEST_FIRST(EMUtilsTexts.GALLERY_SORT_OLDEST_FIRST);

	private final String labelKey;

	ScreenshotGallerySort(String labelKey) {
		this.labelKey = labelKey;
	}

	public String labelKey() {
		return labelKey;
	}

	public ScreenshotGallerySort next() {
		return this == NEWEST_FIRST ? OLDEST_FIRST : NEWEST_FIRST;
	}

	public static ScreenshotGallerySort fromName(String name) {
		if (name != null) {
			for (ScreenshotGallerySort sort : values()) {
				if (sort.name().equalsIgnoreCase(name)) {
					return sort;
				}
			}
		}

		return NEWEST_FIRST;
	}
}
