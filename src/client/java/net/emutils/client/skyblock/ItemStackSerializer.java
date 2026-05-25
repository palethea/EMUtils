package net.emutils.client.skyblock;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.emutils.client.EMUtilsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import org.jspecify.annotations.Nullable;

public final class ItemStackSerializer {
	private ItemStackSerializer() {
	}

	@Nullable
	public static JsonElement toJson(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return null;
		}

		RegistryWrapper.WrapperLookup lookup = registries();
		if (lookup == null) {
			return null;
		}

		try {
			return ItemStack.CODEC.encodeStart(RegistryOps.of(JsonOps.INSTANCE, lookup), stack.copy()).getOrThrow();
		} catch (RuntimeException exception) {
			EMUtilsClient.LOGGER.warn("Failed to serialize storage preview item stack.", exception);
			return null;
		}
	}

	public static ItemStack fromJson(@Nullable JsonElement json) {
		if (json == null || json.isJsonNull()) {
			return ItemStack.EMPTY;
		}

		RegistryWrapper.WrapperLookup lookup = registries();
		if (lookup == null) {
			return ItemStack.EMPTY;
		}

		try {
			return ItemStack.CODEC.parse(RegistryOps.of(JsonOps.INSTANCE, lookup), json).getOrThrow();
		} catch (RuntimeException exception) {
			EMUtilsClient.LOGGER.warn("Failed to deserialize storage preview item stack.", exception);
			return ItemStack.EMPTY;
		}
	}

	private static RegistryWrapper.WrapperLookup registries() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.getNetworkHandler() == null) {
			return null;
		}

		return client.getNetworkHandler().getRegistryManager();
	}
}
