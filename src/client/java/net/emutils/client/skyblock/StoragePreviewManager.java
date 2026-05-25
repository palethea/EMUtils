package net.emutils.client.skyblock;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.config.EMUtilsConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.screen.ScreenHandler;
import org.jspecify.annotations.Nullable;

public final class StoragePreviewManager {
	private static final int PROFILE_CHECK_INTERVAL = 20;

	private final Map<String, StoragePreviewRecord> records = new LinkedHashMap<>();
	private final Map<String, String> aliasIndex = new LinkedHashMap<>();
	@Nullable
	private String activeScopeKey;
	private int profileCheckCooldown;

	public void onWorldJoin(MinecraftClient client) {
		onWorldLeave(client);
		if (SkyblockProfileDetector.isHypixel(client)) {
			refreshScopeIfNeeded(client);
			return;
		}

		switchToScope(StoragePreviewStore.scopeKey(client));
	}

	public void onWorldLeave(MinecraftClient client) {
		persist();
		records.clear();
		aliasIndex.clear();
		activeScopeKey = null;
		profileCheckCooldown = 0;
	}

	public void tick(MinecraftClient client) {
		if (!enabled() || !SkyblockProfileDetector.isHypixel(client)) {
			return;
		}

		if (profileCheckCooldown > 0) {
			profileCheckCooldown--;
			return;
		}

		profileCheckCooldown = PROFILE_CHECK_INTERVAL;
		refreshScopeIfNeeded(client);
	}

	public void onTabListUpdated(MinecraftClient client) {
		if (!enabled() || !SkyblockProfileDetector.isHypixel(client)) {
			return;
		}

		profileCheckCooldown = 0;
		refreshScopeIfNeeded(client);
	}

	public void captureFromScreen(HandledScreen<?> screen, ScreenHandler handler) {
		if (!enabled()) {
			return;
		}

		try {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client == null || client.player == null || !SkyblockProfileDetector.isHypixel(client)) {
				return;
			}

			if (SkyblockProfileDetector.detect(client) == null) {
				return;
			}

			refreshScopeIfNeeded(client);
			if (activeScopeKey == null) {
				return;
			}

			PlayerInventory inventory = client.player.getInventory();
			String title = StoragePreviewKeys.displayTitle(screen.getTitle().getString());
			StoragePreviewRecord captured = StoragePreviewCapture.capture(handler, inventory, title);
			if (captured == null) {
				return;
			}

			records.put(captured.id(), captured);
			rebuildAliasIndex();
			persist();
		} catch (RuntimeException exception) {
			EMUtilsClient.LOGGER.warn("Failed to capture Skyblock storage preview.", exception);
		}
	}

	public boolean shouldPreview(ItemStack stack) {
		return findRecord(stack) != null;
	}

	@Nullable
	public TooltipData createTooltipData(ItemStack stack) {
		StoragePreviewRecord record = findRecord(stack);
		return record == null ? null : new StoragePreviewTooltipData(record.rows(), record.resolveContents());
	}

	@Nullable
	private StoragePreviewRecord findRecord(ItemStack stack) {
		if (!enabled() || stack == null || stack.isEmpty()) {
			return null;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null && SkyblockProfileDetector.isHypixel(client)) {
			refreshScopeIfNeeded(client);
			if (activeScopeKey == null) {
				return null;
			}
		}

		for (String key : StoragePreviewMatcher.lookupKeys(stack, client)) {
			if (StoragePreviewFilters.isBlockedHover(stack, key)) {
				continue;
			}

			String id = aliasIndex.get(key);
			if (id != null) {
				StoragePreviewRecord record = records.get(id);
				if (record != null) {
					return record;
				}
			}
		}

		return null;
	}

	private void refreshScopeIfNeeded(MinecraftClient client) {
		String scopeKey = StoragePreviewStore.scopeKey(client);
		if (scopeKey == null || Objects.equals(scopeKey, activeScopeKey)) {
			return;
		}

		switchToScope(scopeKey);
	}

	private void switchToScope(@Nullable String scopeKey) {
		persist();
		records.clear();
		aliasIndex.clear();
		activeScopeKey = scopeKey;
		if (scopeKey == null) {
			return;
		}

		StoragePreviewStore.LoadedScope loaded = StoragePreviewStore.readScope(scopeKey);
		records.putAll(loaded.records());
		aliasIndex.putAll(loaded.aliasIndex());
	}

	private boolean enabled() {
		EMUtilsConfig config = EMUtilsClient.config();
		return config != null && config.skyblockEnabled() && config.storagePreviewEnabled();
	}

	private void persist() {
		if (activeScopeKey == null) {
			return;
		}

		StoragePreviewStore.writeScope(activeScopeKey, records);
	}

	private void rebuildAliasIndex() {
		aliasIndex.clear();
		for (StoragePreviewRecord record : records.values()) {
			StoragePreviewStore.indexAliases(aliasIndex, record);
		}
	}
}
