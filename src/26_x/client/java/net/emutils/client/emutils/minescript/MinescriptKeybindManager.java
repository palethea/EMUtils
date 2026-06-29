package net.emutils.client.emutils.minescript;

import java.util.HashSet;
import java.util.Set;
import net.emutils.client.emutils.text.EmUtilsChatPrefix;
import net.emutils.client.emutils.compat.MinescriptCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public final class MinescriptKeybindManager {

    private final MinescriptScriptRepository repository =
        new MinescriptScriptRepository();
    private MinescriptKeybindStore store = MinescriptKeybindStore.load();
    private final Set<String> pressed = new HashSet<>();
    private final Set<String> warnedMissing = new HashSet<>();

    public void reload() {
        store = MinescriptKeybindStore.load();
        pressed.clear();
    }

    public MinescriptKeybindStore store() {
        return store;
    }

    public void tick(Minecraft client) {
        if (
            !MinescriptCompat.isLoaded() ||
            client.player == null ||
            client.level == null ||
            net.emutils.client.emutils.compat.MinecraftClientCompat.screen(client) != null
        ) {
            pressed.clear();
            return;
        }

        long window = client.getWindow().handle();

        for (MinescriptKeyBinding binding : store.bindings()) {
            String id = binding.command();
            boolean down = binding.isDown(window);
            if (down && !pressed.contains(id)) {
                runBinding(client, binding);
            }
            if (down) {
                pressed.add(id);
            } else {
                pressed.remove(id);
            }
        }
    }

    private void runBinding(
        Minecraft client,
        MinescriptKeyBinding binding
    ) {
        if (!repository.existsCommand(binding.command())) {
            if (warnedMissing.add(binding.command()) && client.player != null) {
                client.player.sendSystemMessage(
                    EmUtilsChatPrefix.chat(
                        Component.literal(
                            "Minescript binding is stale: " + binding.command()
                        ).withStyle(ChatFormatting.YELLOW)
                    )
                );
            }
            return;
        }

        switch (MinescriptCompat.toggleCommand(binding.command())) {
            case STARTED -> {
                if (client.player != null) {
                    client.player.sendSystemMessage(
                        EmUtilsChatPrefix.chat(
                            Component.translatable(
                                "emutils.script_manager.running",
                                binding.command()
                            ).withStyle(ChatFormatting.GREEN)
                        )
                    );
                }
            }
            case STOPPED -> {
                if (client.player != null) {
                    client.player.sendSystemMessage(
                        EmUtilsChatPrefix.chat(
                            Component.translatable(
                                "emutils.script_manager.stopped",
                                binding.command()
                            ).withStyle(ChatFormatting.YELLOW)
                        )
                    );
                }
            }
            case FAILED -> {
            }
        }
    }
}
