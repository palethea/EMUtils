package net.emutils.client.emutils.inventory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.util.EMUtilsPaths;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

public final class InventoryToolsStore {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final int OFF_HAND_SLOT = 40;

	private InventoryToolsStore() {
	}

	public static InventoryToolsScopeData readScope(String scopeKey) {
		InventoryToolsSaveData saveData = loadAll();
		InventoryToolsScopeData scope = saveData.scopes().get(scopeKey);
		return scope == null ? new InventoryToolsScopeData() : scope;
	}

	public static void writeScope(String scopeKey, Set<InventorySlotRef> lockedSlots, Map<InventorySlotRef, InventorySlotRef> boundSlots) {
		if (scopeKey == null || scopeKey.isBlank()) {
			return;
		}

		InventoryToolsSaveData saveData = loadAll();
		InventoryToolsScopeData scope = new InventoryToolsScopeData();
		scope.setLockedSlots(serializeLocks(lockedSlots));
		scope.setBindings(serializeBindings(boundSlots));
		if (scope.lockedSlots().isEmpty() && scope.bindings().isEmpty()) {
			saveData.scopes().remove(scopeKey);
		} else {
			saveData.scopes().put(scopeKey, scope);
		}
		saveAll(saveData);
	}

	public static void applyScope(
		InventoryToolsScopeData scope,
		Set<InventorySlotRef> lockedSlots,
		Map<InventorySlotRef, InventorySlotRef> boundSlots
	) {
		lockedSlots.clear();
		boundSlots.clear();
		if (scope == null) {
			return;
		}

		for (Integer index : scope.lockedSlots()) {
			if (isValidPlayerIndex(index)) {
				lockedSlots.add(InventorySlotRef.forPlayerIndex(index));
			}
		}

		Set<String> seen = new HashSet<>();
		for (int[] binding : scope.bindings()) {
			if (binding == null || binding.length != 2) {
				continue;
			}

			int first = binding[0];
			int second = binding[1];
			if (!isValidBinding(first, second)) {
				continue;
			}

			String key = first + ":" + second;
			if (!seen.add(key)) {
				continue;
			}

			InventorySlotRef hotbar = InventorySlotRef.forPlayerIndex(first);
			InventorySlotRef other = InventorySlotRef.forPlayerIndex(second);
			boundSlots.put(hotbar, other);
			boundSlots.put(other, hotbar);
		}
	}

	@Nullable
	public static String scopeKey(Minecraft client) {
		ServerData serverInfo = client.getCurrentServer();
		if (serverInfo != null && serverInfo.ip != null && !serverInfo.ip.isBlank()) {
			return "multiplayer:" + serverInfo.ip;
		}

		if (client.hasSingleplayerServer()) {
			var server = client.getSingleplayerServer();
			if (server != null) {
				return "singleplayer:" + server.getWorldData().getLevelName();
			}
		}

		return null;
	}

	private static InventoryToolsSaveData loadAll() {
		if (!Files.exists(EMUtilsPaths.inventoryToolsFile())) {
			return new InventoryToolsSaveData();
		}

		try {
			String json = Files.readString(EMUtilsPaths.inventoryToolsFile());
			InventoryToolsSaveData saveData = GSON.fromJson(json, InventoryToolsSaveData.class);
			return saveData == null ? new InventoryToolsSaveData() : saveData;
		} catch (IOException | JsonParseException | IllegalStateException exception) {
			EMUtilsClient.LOGGER.warn("Failed to load inventory tools state.", exception);
			return new InventoryToolsSaveData();
		}
	}

	private static void saveAll(InventoryToolsSaveData saveData) {
		try {
			Files.createDirectories(EMUtilsPaths.configDir());
			if (saveData.scopes().isEmpty()) {
				Files.deleteIfExists(EMUtilsPaths.inventoryToolsFile());
				return;
			}

			try (Writer writer = Files.newBufferedWriter(EMUtilsPaths.inventoryToolsFile())) {
				GSON.toJson(saveData, writer);
			}
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("Failed to save inventory tools state.", exception);
		}
	}

	private static List<Integer> serializeLocks(Set<InventorySlotRef> lockedSlots) {
		List<Integer> indices = new ArrayList<>();
		for (InventorySlotRef ref : lockedSlots) {
			if (ref.kind() == InventorySlotRef.Kind.PLAYER && isValidPlayerIndex(ref.inventoryIndex())) {
				indices.add(ref.inventoryIndex());
			}
		}
		indices.sort(Integer::compareTo);
		return indices;
	}

	private static List<int[]> serializeBindings(Map<InventorySlotRef, InventorySlotRef> boundSlots) {
		List<int[]> bindings = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		for (Map.Entry<InventorySlotRef, InventorySlotRef> entry : boundSlots.entrySet()) {
			InventorySlotRef first = entry.getKey();
			InventorySlotRef second = entry.getValue();
			if (first.kind() != InventorySlotRef.Kind.PLAYER || second.kind() != InventorySlotRef.Kind.PLAYER) {
				continue;
			}

			InventorySlotRef hotbar = first.isHotbar() ? first : second.isHotbar() ? second : null;
			InventorySlotRef other = first.isHotbar() ? second : second.isHotbar() ? first : first;
			if (hotbar == null || other.isHotbar() || !isValidBinding(hotbar.inventoryIndex(), other.inventoryIndex())) {
				continue;
			}

			String key = hotbar.inventoryIndex() + ":" + other.inventoryIndex();
			if (seen.add(key)) {
				bindings.add(new int[] { hotbar.inventoryIndex(), other.inventoryIndex() });
			}
		}
		return bindings;
	}

	private static boolean isValidPlayerIndex(int index) {
		return index >= 0 && index <= OFF_HAND_SLOT;
	}

	private static boolean isValidBinding(int hotbarIndex, int otherIndex) {
		return hotbarIndex >= 0
			&& hotbarIndex < Inventory.SELECTION_SIZE
			&& otherIndex >= Inventory.SELECTION_SIZE
			&& isValidPlayerIndex(otherIndex);
	}
}
