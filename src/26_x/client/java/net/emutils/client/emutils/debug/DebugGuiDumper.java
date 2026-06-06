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
import net.emutils.client.emutils.text.EmUtilsChatPrefix;
import net.emutils.client.emutils.util.EMUtilsPaths;
import net.emutils.client.mixin.HandledScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.ChatFormatting;
import org.jspecify.annotations.Nullable;

public final class DebugGuiDumper {

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    private DebugGuiDumper() {}

    public static void capture(Minecraft client) {
        if (client == null) {
            return;
        }

        try {
            JsonObject root = new JsonObject();
            root.addProperty("timestamp", Instant.now().toString());
            String world = client.level == null
                ? null
                : client.level.dimension().identifier().toString();
            root.addProperty("world", world);
            root.add("screen", serializeScreen(client));

            String json = GSON.toJson(root);
            Path path = writeFile(json);
            String absolutePath = path.toAbsolutePath().toString();
            client.keyboardHandler.setClipboard(absolutePath);
            if (client.player != null) {
                client.player.sendSystemMessage(EmUtilsChatPrefix.chat("Debug dump saved, path copied."));
            }
        } catch (Exception exception) {
            if (client.player != null) {
                client.player.sendSystemMessage(EmUtilsChatPrefix.chat("Debug dump failed."));
            }
        }
    }

    private static JsonObject serializeScreen(Minecraft client) {
        Screen screen = client.screen;
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

        if (screen instanceof AbstractContainerScreen<?> handledScreen) {
            AbstractContainerMenu handler = handledScreen.getMenu();
            object.addProperty("syncId", handler.containerId);
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
        Minecraft client,
        AbstractContainerMenu handler
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
        Minecraft client,
        @Nullable Slot slot
    ) {
        JsonObject object = new JsonObject();
        if (slot == null) {
            object.addProperty("empty", true);
            return object;
        }

        object.addProperty("slotId", slot.getContainerSlot());
        object.addProperty("x", slot.x);
        object.addProperty("y", slot.y);
        object.addProperty("enabled", slot.isActive());
        object.addProperty(
            "inventoryType",
            slot.container.getClass().getName()
        );
        object.addProperty(
            "playerInventory",
            slot.container instanceof Inventory
        );
        object.addProperty("stackEmpty", !slot.hasItem());

        if (!slot.hasItem()) {
            return object;
        }

        ItemStack stack = slot.getItem();
        object.addProperty("count", stack.getCount());
        object.addProperty("name", stack.getHoverName().getString());
        object.addProperty(
            "namePlain",
            strip(stack.getHoverName())
        );
        object.addProperty("itemId", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        object.add("tooltip", tooltipJson(client, stack));
        return object;
    }

    private static JsonArray tooltipJson(
        Minecraft client,
        ItemStack stack
    ) {
        JsonArray lines = new JsonArray();
        if (client.player == null || client.level == null) {
            return lines;
        }

        try {
            List<Component> tooltip = stack.getTooltipLines(
                Item.TooltipContext.of(client.level),
                client.player,
                TooltipFlag.ADVANCED
            );
            for (Component line : tooltip) {
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

    private static String strip(Component text) {
        String stripped = ChatFormatting.stripFormatting(text == null ? "" : text.getString());
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
