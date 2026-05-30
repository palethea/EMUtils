     
   
**EMUtils**  
   Tired of installing five different mods just to copy a chat message, zoom in, and reconnect after a kick?  
   
   **EMUtils** bundles the small stuff into one lightweight client-side mod for  **Minecraft 1.21.11** on Fabric — so you can stop juggling mods and get back to playing.  
   
**Status**  
EMUtils is in active development. One settings hub, a pile of quality-of-life features, and zero interest in getting in your way while you play.  
**Features**  
**Minecraft**  
- **Automatic reconnect** — reconnect to the last server after being kicked, with a countdown button on the disconnect screen, configurable retry delay, limited attempts, or always-retry mode.  
- **Screenshot helper** — replace the default screenshot message with quick actions, optional auto-copy feedback, an open-gallery keybind, and a configurable screenshot gallery for copying, opening, sorting, and deleting recent captures.  
- **Death waypoints** — save multiple deaths per world/server with in-world labels, distance text, configurable opacity, nearby removal prompts, and coordinate copying in plain, comma-separated, or teleport-command formats.  
- **Chat features** — copy full chat messages with Ctrl + left click, optionally prepend chat timestamps, collapse repeated messages, and show sound/toast alerts or configurable highlights when you are mentioned.  
- **HUD overlay** — show a configurable styled info panel with icons, selectable screen position, opacity, scale, coordinates, chunk/region, biome, ping, FPS, facing, memory, world time, and local time. Use anchor positioning or drag overlays into a custom HUD layout.  
- **Zoom** — hold a configurable keybind for OptiFine-style zoom with adjustable zoom amount, optional smooth fade in/out, cinematic camera smoothing, hide hand, and F1-style HUD hiding while zoomed.  
- **Tweaks** — toggle fullbright, clear weather, no fog, clear underwater/lava vision, no environment fog, no hurt cam, freelook, your own nametag, and shulker or bundle tooltip previews.  
- **Pack Manager** — browse installed and Modrinth resource packs and shader packs in-game, download into Minecraft's pack folders, delete installed packs, enable/disable resource packs, and apply or turn off Iris shader packs when Iris is installed.  
- **Script Manager** — when Minescript is installed, browse minecraft/minescript, create/edit Python scripts, run them locally through Minescript, and assign EMUtils-managed per-script keybinds.  
- **Custom capes** — show player capes from OptiFine, LabyMod, MinecraftCapes, Cosmetica, and Cloaks+, with per-provider toggles and a preferred provider selector when you have more than one.  
- **Spotify player** — show the current Spotify track in the pause menu and optional HUD overlay when Spotify is running (Linux, macOS, and Windows).  
- **Inventory Tools** — lock slots, bind hotbar-safe slot swaps, show a small in-game inventory preview above the hotbar, and keep the mouse cursor in place when switching between container screens instead of resetting to center. Slot locks and bindings persist per world/server.  
- **Settings UI** — open the modern EMUtils settings hub from  **Options → EMUtils...**, Mod Menu, or the settings keybind for normal Minecraft QoL. Open  **Options → EMSkyblock...** or the EMSkyblock button in the hub footer for Hypixel SkyBlock settings powered by MoulConfig. Mod Menu shows a chooser between both hubs when Mod Menu is installed.  
**Hypixel Skyblock**  
- **Storage previews** — remember what's in your backpacks and ender chest, and show a quick preview when you hover the menu item. Each Skyblock profile keeps its own saves.  
- **Skyblock detection** — track Hypixel Skyblock state from tab list, scoreboard, and /locraw data so Skyblock features stay profile-aware and server-aware.  
- **Price tooltips** — add Bazaar, Auction House, and NPC sell-price lines to item tooltips when Skyblock pricing data is available.  
- **Estimated Item Value** — estimate item value from base item prices, enchantments, reforges, recombobulators, potato books, gemstones, and scrolls, with tooltip lines and an optional HUD panel.  
- **Skyblock stats HUD** — render health, defense, mana, and soulflow from the Skyblock action bar in a configurable HUD panel, with options to hide vanilla hearts, food, armor, action bar text, and inventory potion effects.  
- **Fishing hook display** — show a large on-screen alert and countdown timer while holding a fishing rod and your bobber is active. Customize the pull-ready alert text and colors, hide Hypixel's small armor stand label, and position the overlay with the HUD Layout Editor.  
- **Fishing profit tracker** — track items and coins earned from fishing with a configurable HUD panel. Shows item names with color-coded rarities, total catches, session/all-time profit, profit-per-hour, and uptime. Supports session and all-time reset, and optionally stays visible while wearing fishing armor or on fishing islands.  
- **Sea creature tracker** — track sea creature catches with a configurable HUD panel. Shows creature names with rarity colors, catch counts, percentage breakdowns, total catches, double hook counting, session/all-time modes, and uptime. Same fishing armor and island visibility options as the profit tracker.  
- **HUD Layout Editor** — drag SkyBlock HUD elements (stats, EIV, fishing hook, profit tracker, sea creature tracker) into custom positions with per-element scale and opacity controls. Click the session/all-time headers on tracker panels to cycle modes, right-click to reset the current session.  
**Coming Soon**  
**Minecraft**  
- **Food HUD** — saturation and exhaustion overlays on the hunger bar, plus hunger and saturation values on food tooltips.  
- **Villager workstation protection** — prevent accidental villager workstation interactions when needed.  
- **Auto sort** — organize inventories and containers with a quick action.  
- **Quick GIF** — record short shareable gameplay clips from a keybind.  
- **Keybind wheel** — create profile-based wheel actions that run chat commands or send saved messages.  
**Requirements**  
- Minecraft 1.21.11  
- Java 21 or newer  
- Fabric Loader 0.19.2 or newer  
- Fabric API  
**Optional integrations**  
- **Mod Menu** — adds an EMUtils button on the mod list screen (the in-game hub works without it)  
- **Iris** — required for Pack Manager shader apply/disable actions  
- **Minescript** — required for Script Manager browsing, editing, running, and EMUtils script keybinds  
**Installation**  
1. Install [Fabric Loader for Minecraft 1.21.11.](https://fabricmc.net/use/ "https://fabricmc.net/use/")  
2. Download the latest EMUtils jar from releases.  
3. Place the jar in your mods folder.  
4. Launch the game.  
**Building**  
./gradlew build  
   
The built jar will be created in build/libs/.  
**License**  
EMUtils is licensed under the [Apache License 2.0.](LICENSE "LICENSE")  
Copyright 2026 Palethea. If you use, fork, modify, or redistribute EMUtils, keep the original license and attribution notices.  
**Screenshots**  
**Screenshot Gallery**  
**Death Waypoint**  
**Copy Chat**  
**Chat Features**  
