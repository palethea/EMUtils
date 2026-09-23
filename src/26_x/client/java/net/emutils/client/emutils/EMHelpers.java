package net.emutils.client.emutils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.emutils.client.emutils.hud.layout.HudLayoutConfig;
import net.emutils.client.emutils.util.EMHelpersTexts;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public final class EMHelpers {
	private static String modId = "emhelpers";
	private static final Map<String, HudContext> HUD_CONTEXTS = new LinkedHashMap<>();

	private EMHelpers() {
	}

	public static void configure(String modId, Supplier<@Nullable HudLayoutConfig> hudConfigSupplier) {
		configure(modId, hudConfigSupplier, () -> false);
	}

	public static void configure(
		String modId,
		Supplier<@Nullable HudLayoutConfig> hudConfigSupplier,
		BooleanSupplier hideHudSupplier
	) {
		EMHelpers.modId = Objects.requireNonNull(modId, "modId");
		HUD_CONTEXTS.put(
			EMHelpers.modId,
			new HudContext(
				Objects.requireNonNull(hudConfigSupplier, "hudConfigSupplier"),
				Objects.requireNonNull(hideHudSupplier, "hideHudSupplier")
			)
		);
	}

	public static void registerTranslationPrefix(String prefix) {
		EMHelpersTexts.setPrefix(Objects.requireNonNull(prefix, "prefix"));
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(modId, path);
	}

	public static @Nullable HudLayoutConfig hudConfig() {
		return hudConfig(modId);
	}

	public static @Nullable HudLayoutConfig hudConfig(String modId) {
		HudContext context = HUD_CONTEXTS.get(modId);
		return context == null ? null : context.hudConfigSupplier().get();
	}

	public static boolean shouldHideHud() {
		return shouldHideHud(modId);
	}

	public static boolean shouldHideHud(String modId) {
		HudContext context = HUD_CONTEXTS.get(modId);
		return context != null && context.hideHudSupplier().getAsBoolean();
	}

	private record HudContext(
		Supplier<@Nullable HudLayoutConfig> hudConfigSupplier,
		BooleanSupplier hideHudSupplier
	) {}
}
