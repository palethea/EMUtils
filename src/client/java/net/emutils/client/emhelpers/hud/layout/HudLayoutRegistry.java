package net.emutils.client.emhelpers.hud.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public final class HudLayoutRegistry {
	private static final Map<HudElementId, HudLayoutElement> BY_ID = new LinkedHashMap<>();
	private static final List<HudLayoutElement> ELEMENTS = new ArrayList<>();

	private HudLayoutRegistry() {
	}

	public static void register(HudLayoutElement element) {
		if (BY_ID.containsKey(element.id())) {
			throw new IllegalStateException("HUD layout element already registered: " + element.id());
		}

		BY_ID.put(element.id(), element);
		ELEMENTS.add(element);
		element.register();
	}

	public static List<HudLayoutElement> all() {
		return Collections.unmodifiableList(ELEMENTS);
	}

	public static @Nullable HudLayoutElement get(HudElementId id) {
		return BY_ID.get(id);
	}

	public static HudLayoutElement require(HudElementId id) {
		HudLayoutElement element = BY_ID.get(id);
		if (element == null) {
			throw new IllegalStateException("HUD layout element not registered: " + id);
		}

		return element;
	}
}
