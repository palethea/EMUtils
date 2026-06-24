<p align="center">
  <img src="assets/branding/emutils-logo.svg" alt="EMUtils logo" width="160">
</p>

<h1 align="center">EMUtils</h1>

<p align="center">
  Tired of installing five different mods just to copy a chat message, zoom in, and reconnect after a kick?<br>
  <strong>EMUtils</strong> bundles the small stuff into one lightweight client-side mod for <strong>Minecraft 1.21.9-1.21.11</strong> and <strong>Minecraft 26.x</strong> on Fabric — so you can stop juggling mods and get back to playing.
</p>

## Status

EMUtils is in active development. One settings hub, a pile of quality-of-life features, and zero interest in getting in your way while you play.

### Version support

- **Minecraft 26.x** is the active development version and receives new features and fixes.
- **Minecraft 1.21.9-1.21.11** enters maintenance mode after EMUtils 0.13.0. It will continue receiving bug fixes and compatibility fixes, but new features will target Minecraft 26.x.

## Features

EMUtils features are documented in [FEATURES.md](FEATURES.md), grouped the same way as the in-game settings hub:

- **Render** — zoom, fullbright, clear weather, visibility tweaks, beacon radius outlines, light level overlay, custom capes, and visual overlays.
- **HUD** — info overlay, free camera coordinates, food HUD, layout editor, and Spotify now-playing panels.
- **Utility** — auto reconnect, screenshot helper, screenshot metadata, and waypoints.
- **Management** — screenshot gallery, waypoint management, Pack Manager, Script Manager, and command shortcuts.
- **QoL** — chat tools, inventory tools, Sort Buttons, Hover Transfer, Mass Drop, Auto Tool, Auto Flight Gear, Safe Walk, Free Camera, placement helpers, Fast Place, and Fast Use.

## Feature Ideas

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

- Minecraft 1.21.9-1.21.11 or Minecraft 26.x
- Java 21 or newer for the 1.21.x jar
- Java 25 or newer for the 26.x jar
- Fabric Loader 0.19.2 or newer for 1.21.x, or 0.19.3 or newer for 26.x
- Fabric API

### Optional integrations

- **Iris** — required for Pack Manager shader apply/disable actions
- **Minescript** — required for Script Manager browsing, editing, running, and EMUtils script keybinds
- **Xaero's Minimap / Xaero's World Map** — optional support for showing Beacon Radius Outline boundaries on Xaero maps

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
