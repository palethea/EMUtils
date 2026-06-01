package net.emutils.client.emutils.minescript;

import java.util.HashSet;
import java.util.Set;
import net.emutils.client.emhelpers.text.EmUtilsChatPrefix;
import net.emutils.client.emutils.compat.MinescriptCompat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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

    public void tick(MinecraftClient client) {
        if (
            !MinescriptCompat.isLoaded() ||
            client.player == null ||
            client.world == null ||
            client.currentScreen != null
        ) {
            pressed.clear();
            return;
        }

        long window = client.getWindow().getHandle();

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
        MinecraftClient client,
        MinescriptKeyBinding binding
    ) {
        if (!repository.existsCommand(binding.command())) {
            if (warnedMissing.add(binding.command()) && client.player != null) {
                client.player.sendMessage(
                    EmUtilsChatPrefix.chat(
                        Text.literal(
                            "Minescript binding is stale: " + binding.command()
                        ).formatted(Formatting.YELLOW)
                    ),
                    false
                );
            }
            return;
        }

        switch (MinescriptCompat.toggleCommand(binding.command())) {
            case STARTED -> {
                if (client.player != null) {
                    client.player.sendMessage(
                        EmUtilsChatPrefix.chat(
                            Text.translatable(
                                "emutils.script_manager.running",
                                binding.command()
                            ).formatted(Formatting.GREEN)
                        ),
                        false
                    );
                }
            }
            case STOPPED -> {
                if (client.player != null) {
                    client.player.sendMessage(
                        EmUtilsChatPrefix.chat(
                            Text.translatable(
                                "emutils.script_manager.stopped",
                                binding.command()
                            ).formatted(Formatting.YELLOW)
                        ),
                        false
                    );
                }
            }
            case FAILED -> {
            }
        }
    }
}
