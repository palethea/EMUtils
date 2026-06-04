package net.emutils.client.emutils.debug;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.text.EmUtilsChatPrefix;
import net.emutils.client.emutils.util.EMUtilsPaths;
import net.emutils.client.mixin.HandledScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.registry.Registries;
import net.minecraft.util.Formatting;
import org.jspecify.annotations.Nullable;

public final class DebugGuiDumper {

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    private DebugGuiDumper() {}

    public static void capture(MinecraftClient client) {
        if (client == null) {
            return;
        }

        try {
            JsonObject root = new JsonObject();
            root.addProperty("timestamp", Instant.now().toString());
            root.addProperty(
                "world",
                client.world == null
                    ? null
                    : client.world.getRegistryKey().getValue().toString()
            );
            root.add("screen", serializeScreen(client));

            String json = GSON.toJson(root);
            Path path = writeFile(json);
            String absolutePath = path.toAbsolutePath().toString();
            client.keyboard.setClipboard(absolutePath);
            if (client.player != null) {
                client.player.sendMessage(
                    EmUtilsChatPrefix.chat("Debug dump saved, path copied."),
                    true
                );
            }
            EMUtilsClient.LOGGER.info("Saved GUI debug dump to {}.", path);
        } catch (Exception exception) {
            EMUtilsClient.LOGGER.warn(
                "Failed to save GUI debug dump.",
                exception
            );
            if (client.player != null) {
                client.player.sendMessage(
                    EmUtilsChatPrefix.chat("Debug dump failed."),
                    true
                );
            }
        }
    }

    private static JsonObject serializeScreen(MinecraftClient client) {
        Screen screen = client.currentScreen;
        JsonObject object = new JsonObject();
        if (screen == null) {
            object.addProperty("type", "none");
            return object;
        }

        object.addProperty("class", screen.getClass().getName());
        object.addProperty("title", screen.getTitle().getString());
        object.addProperty(
            "titlePlain",
            strip(screen.getTitle())
        );

        if (screen instanceof HandledScreen<?> handledScreen) {
            ScreenHandler handler = handledScreen.getScreenHandler();
            object.addProperty("syncId", handler.syncId);
            object.add(
                "focusedSlot",
                serializeSlot(
                    client,
                    (
                        (HandledScreenAccessor) handledScreen
                    ).emutils$getFocusedSlot()
                )
            );
            object.add("slots", serializeSlots(client, handler));
        }

        return object;
    }

    private static JsonArray serializeSlots(
        MinecraftClient client,
        ScreenHandler handler
    ) {
        JsonArray slots = new JsonArray();
        for (int index = 0; index < handler.slots.size(); index++) {
            Slot slot = handler.slots.get(index);
            JsonObject entry = serializeSlot(client, slot);
            entry.addProperty("handlerIndex", index);
            slots.add(entry);
        }

        return slots;
    }

    private static JsonObject serializeSlot(
        MinecraftClient client,
        @Nullable Slot slot
    ) {
        JsonObject object = new JsonObject();
        if (slot == null) {
            object.addProperty("empty", true);
            return object;
        }

        object.addProperty("slotId", slot.id);
        object.addProperty("x", slot.x);
        object.addProperty("y", slot.y);
        object.addProperty("enabled", slot.isEnabled());
        object.addProperty(
            "inventoryType",
            slot.inventory.getClass().getName()
        );
        object.addProperty(
            "playerInventory",
            slot.inventory instanceof PlayerInventory
        );
        object.addProperty("stackEmpty", !slot.hasStack());

        if (!slot.hasStack()) {
            return object;
        }

        ItemStack stack = slot.getStack();
        object.addProperty("count", stack.getCount());
        object.addProperty("name", stack.getName().getString());
        object.addProperty(
            "namePlain",
            strip(stack.getName())
        );
        object.addProperty("itemId", Registries.ITEM.getId(stack.getItem()).toString());
        object.add("tooltip", tooltipJson(client, stack));
        return object;
    }

    private static JsonArray tooltipJson(
        MinecraftClient client,
        ItemStack stack
    ) {
        JsonArray lines = new JsonArray();
        if (client.player == null || client.world == null) {
            return lines;
        }

        try {
            List<Text> tooltip = stack.getTooltip(
                Item.TooltipContext.create(client.world),
                client.player,
                TooltipType.ADVANCED
            );
            for (Text line : tooltip) {
                JsonObject entry = new JsonObject();
                entry.addProperty("raw", line.getString());
                entry.addProperty("plain", strip(line));
                lines.add(entry);
            }
        } catch (RuntimeException exception) {
            JsonObject error = new JsonObject();
            error.addProperty("error", exception.getMessage());
            lines.add(error);
        }

        return lines;
    }

    private static String strip(Text text) {
        String stripped = Formatting.strip(text == null ? "" : text.getString());
        return stripped == null ? "" : stripped.trim();
    }

    private static Path writeFile(String json) throws IOException {
        Path directory = EMUtilsPaths.debugDir();
        Files.createDirectories(directory);
        Path path = directory.resolve(
            "gui-" + System.currentTimeMillis() + ".json"
        );
        try (Writer writer = Files.newBufferedWriter(path)) {
            writer.write(json);
        }

        return path;
    }
}
