<p align="center">
  <img src="assets/branding/emutils-logo.svg" alt="EMUtils logo" width="160">
</p>

<h1 align="center">EMUtils</h1>

<p align="center">
  Tired of installing five different mods just to copy a chat message, zoom in, and reconnect after a kick?<br>
  <strong>EMUtils</strong> bundles the small stuff into one lightweight client-side mod for <strong>Minecraft 1.21.9-1.21.11</strong> and <strong>Minecraft 26.1-26.1.2</strong> on Fabric — so you can stop juggling mods and get back to playing.
</p>

## Status

EMUtils is in active development. One settings hub, a pile of quality-of-life features, and zero interest in getting in your way while you play.

## Features

- **Automatic reconnect** — reconnect to the last server after being kicked, with a countdown button on the disconnect screen, configurable retry delay, limited attempts, or always-retry mode.
- **Screenshot helper** — replace the default screenshot message with quick actions, optional auto-copy feedback, location/server/dimension metadata saved in a screenshots metadata folder, an open-gallery keybind, and a configurable screenshot gallery for copying, opening, sorting, and deleting recent captures.
- **Death waypoints** — save multiple deaths per world/server with in-world labels, distance text, configurable opacity, nearby removal prompts, and coordinate copying in plain, comma-separated, or teleport-command formats.
- **Chat features** — copy full chat messages with Ctrl + left click, optionally prepend chat timestamps, collapse repeated messages, and show sound/toast alerts or configurable highlights when you are mentioned.
- **HUD overlay** — show a configurable styled info panel with icons, selectable screen position, opacity, scale, coordinates, chunk/region, biome, ping, FPS, facing, memory, world time, and local time. Use anchor positioning or drag overlays into a custom HUD layout.
- **Food HUD** — show saturation, held-food, and exhaustion overlays on the vanilla hunger bar, plus food values in item tooltips.
- **Zoom** — hold a configurable keybind for OptiFine-style zoom with adjustable zoom amount, optional smooth fade in/out, cinematic camera smoothing, hide hand, and F1-style HUD hiding while zoomed.
- **Tweaks** — toggle fullbright with strength control, clear weather with separate rain, snow, and rain-effect controls, no fog, clear underwater/lava vision, no fire overlay, low fire, no nausea, no spyglass overlay, fast block placement, no environment fog, no hurt cam, freelook, your own nametag, and shulker or bundle tooltip previews.
- **Pack Manager** — browse installed and Modrinth resource packs and shader packs in-game, download into pack folders, delete installed packs, enable/disable resource packs, and apply or turn off Iris shader packs when Iris is installed.
- **Script Manager** — when Minescript is installed, browse `minecraft/minescript`, create/edit Python scripts, run them locally through Minescript, and assign EMUtils-managed per-script keybinds.
- **Custom capes** — show player capes from OptiFine, LabyMod, MinecraftCapes, Cosmetica, and Cloaks+, with per-provider toggles and a preferred provider selector when you have more than one.
- **Spotify player** — show the current Spotify track in the pause menu and optional HUD overlay when Spotify is running (Linux, macOS, and Windows).
- **Inventory Tools** — lock slots, bind hotbar-safe slot swaps, use Hover Transfer to sweep items in or out of storage containers with Shift + left click, show a small in-game inventory preview above the hotbar, and keep the mouse cursor in place when switching between container screens instead of resetting to center. Slot locks and bindings persist per world/server.
- **Settings UI** — open the modern EMUtils settings hub from **Options → EMUtils...** or the settings keybind.

## Feature Ideas

- **Auto sort** — organize inventories and containers with a quick action.
- **Quick GIF** — record short shareable gameplay clips from a keybind.
- **Keybind wheel** — create profile-based wheel actions that run chat commands or send saved messages.
- **Last server** — add a main-menu button for instantly joining the last server you played on.
- **No Pumpkin Overlay** — hide the pumpkin overlay while wearing a carved pumpkin.
- **Armor & Hand Equipment Status HUD** — show equipped armor durability and held item status.
- **Keypress / Keystrokes Overlay** — display keystrokes and CPS on-screen.
- **Target Entity Info Overlay** — show targeted mob/player health and active effects.
- **TPS & Server Lag Monitor** — monitor server-side performance (TPS/MSPT) from the HUD.
- **Inventory Search & Highlight Filter** — filter and highlight container contents in real-time.

## Requirements

- Minecraft 1.21.9-1.21.11 or Minecraft 26.1-26.1.2
- Java 21 or newer for the 1.21.x jar
- Java 25 or newer for the 26.x jar
- Fabric Loader 0.19.2 or newer for 1.21.x, or 0.19.3 or newer for 26.x
- Fabric API

### Optional integrations

- **Iris** — required for Pack Manager shader apply/disable actions
- **Minescript** — required for Script Manager browsing, editing, running, and EMUtils script keybinds

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for your Minecraft version.
2. Download the matching EMUtils jar from releases: `EMUtils-1.21.x.jar` or `EMUtils-26.x.jar`.
3. Place the jar in your `mods` folder.
4. Launch the game.

## Building

```bash
./gradlew -PmcFamily=1.21.x build
./gradlew -PmcFamily=26.x build
```

The built jars will be created in `build/libs/` as `EMUtils-1.21.x.jar` and `EMUtils-26.x.jar`.

## License

EMUtils is licensed under the [Apache License 2.0](LICENSE).

Copyright 2026 Palethea. If you use, fork, modify, or redistribute EMUtils, keep the original license and attribution notices.

## Screenshots

See the [Modrinth gallery](https://modrinth.com/mod/emutils/gallery) for screenshots and feature previews.
