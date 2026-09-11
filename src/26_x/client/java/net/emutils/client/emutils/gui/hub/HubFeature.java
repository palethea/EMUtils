package net.emutils.client.emutils.gui.hub;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public final class HubFeature {
	public enum Group {
		RENDER(EMUtilsTexts.HUB_GROUP_RENDER, HubIcons.SUN),
		HUD(EMUtilsTexts.HUB_GROUP_HUD, HubIcons.MONITOR),
		UTILITY(EMUtilsTexts.HUB_GROUP_UTILITY, HubIcons.WRENCH),
		MANAGEMENT(EMUtilsTexts.HUB_GROUP_MANAGEMENT, HubIcons.FOLDER_COG),
		QOL(EMUtilsTexts.HUB_GROUP_QOL, HubIcons.SPARKLES);

		private final String labelKey;
		private final Identifier icon;

		Group(String labelKey, Identifier icon) {
			this.labelKey = labelKey;
			this.icon = icon;
		}

		public String labelKey() {
			return labelKey;
		}

		public Identifier icon() {
			return icon;
		}
	}

	public enum Icon {
		CHAT(HubIcons.MESSAGE_SQUARE),
		PIN(HubIcons.MAP_PIN),
		RECONNECT(HubIcons.REFRESH_CW),
		IMAGE(HubIcons.IMAGE),
		TOOL(HubIcons.FOLDER_COG),
		HUD(HubIcons.MONITOR),
		MOUSE_CLICK(HubIcons.MOUSE_POINTER_CLICK),
		APPLE(HubIcons.APPLE),
		ZOOM(HubIcons.ZOOM_IN),
		CAPE(HubIcons.SHIRT),
		BAG(HubIcons.BACKPACK),
		MUSIC(HubIcons.MUSIC),
		SUN(HubIcons.SUN),
		CLOUD_SUN(HubIcons.CLOUD_SUN),
		CLOUD_OFF(HubIcons.CLOUD_OFF),
		DROPLETS(HubIcons.DROPLETS),
		FLAME(HubIcons.FLAME),
		SHIELD(HubIcons.SHIELD),
		EYE(HubIcons.EYE),
		TAG(HubIcons.TAG),
		BOX(HubIcons.BOX),
		PACKAGE(HubIcons.PACKAGE),
		PACKAGE_OPEN(HubIcons.PACKAGE_OPEN),
		SPARKLES(HubIcons.SPARKLES),
		SCRIPT(HubIcons.PACKAGE_OPEN);

		private final Identifier texture;

		Icon(Identifier texture) {
			this.texture = texture;
		}

		public Identifier texture() {
			return texture;
		}
	}

	public record Toggle(BooleanSupplier getter, Consumer<Boolean> setter) {
	}

	private final String id;
	private final @Nullable HubCategory category;
	private final Group group;
	private final String titleKey;
	private final String descriptionKey;
	private final Icon icon;
	private final @Nullable Toggle toggle;
	private final @Nullable List<HubSettingRow> rows;
	private final @Nullable Runnable primaryAction;
	private final boolean primaryActionEnabled;
	private final @Nullable Runnable resetAction;

	public HubFeature(
		String id,
		@Nullable HubCategory category,
		Group group,
		String titleKey,
		String descriptionKey,
		Icon icon,
		@Nullable Toggle toggle,
		@Nullable List<HubSettingRow> rows,
		@Nullable Runnable primaryAction,
		boolean primaryActionEnabled,
		@Nullable Runnable resetAction
	) {
		this.id = id;
		this.category = category;
		this.group = group;
		this.titleKey = titleKey;
		this.descriptionKey = descriptionKey;
		this.icon = icon;
		this.toggle = toggle;
		this.rows = rows;
		this.primaryAction = primaryAction;
		this.primaryActionEnabled = primaryActionEnabled;
		this.resetAction = resetAction;
	}

	public String id() {
		return id;
	}

	public @Nullable HubCategory category() {
		return category;
	}

	public Group group() {
		return group;
	}

	public String titleKey() {
		return titleKey;
	}

	public String descriptionKey() {
		return descriptionKey;
	}

	public Icon icon() {
		return icon;
	}

	public @Nullable Toggle toggle() {
		return toggle;
	}

	public @Nullable List<HubSettingRow> rows() {
		return rows;
	}

	public @Nullable Runnable primaryAction() {
		return primaryAction;
	}

	public boolean primaryActionEnabled() {
		return primaryActionEnabled;
	}

	public @Nullable Runnable resetAction() {
		return resetAction;
	}

	public Component title() {
		return Component.translatable(titleKey);
	}

	public boolean matches(String query) {
		return HubFeatureCatalog.normalize(Component.translatable(titleKey).getString()).contains(query)
			|| HubFeatureCatalog.normalize(Component.translatable(descriptionKey).getString()).contains(query);
	}

	public static String normalize(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
	}
}
