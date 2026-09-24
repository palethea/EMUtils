<p align="center">
  <img src="assets/branding/emutils-logo.svg" alt="EMUtils logo" width="140">
</p>

<h1 align="center">EMUtils</h1>

<p align="center">
  <strong>One mod. Every little job.</strong><br>
  A lightweight, client-side utility mod for Minecraft on Fabric.
</p>

<p align="center">
  <a href="https://modrinth.com/mod/emutils">Modrinth</a> ·
  <a href="FEATURES.md">All features</a> ·
  <a href="https://modrinth.com/mod/emutils/gallery">Screenshots</a> ·
  <a href="https://github.com/palethea/EMUtils/issues/55">Ideas backlog</a>
</p>

---

Most utility mods do one job. One zooms, one copies chat, one sorts your chests, one reconnects you after a kick. That works fine until your mods folder has thirty of them, each with its own config screen, its own keybinds and its own update schedule.

**EMUtils does all of those jobs.** One jar, one settings hub, and one update when Minecraft moves on.

## What's inside

| Category | Highlights |
|---|---|
| **Render** | Zoom, Freelook, Fullbright, Clear Weather, Light Level Overlay, Beacon Radius Outline, custom capes, and a stack of visual tweaks |
| **HUD** | Info overlay with a drag-and-drop layout editor, Food HUD, Spotify now playing |
| **Utility** | Auto Reconnect, Screenshot Helper, death and custom Waypoints |
| **Management** | Settings hub, Screenshot Gallery, Pack Manager with Modrinth search, Script Manager |
| **QoL** | Chat copy and filters, Sort Buttons, Quick Stack, Slot Locking, Auto Tool, Auto Flight Gear, Free Camera, Safe Walk, Fast Place |

Every feature has its own on/off switch. The full list with every setting is in **[FEATURES.md](FEATURES.md)**.

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/) and [Fabric API](https://modrinth.com/mod/fabric-api).
2. Download the jar for your Minecraft version from [Modrinth](https://modrinth.com/mod/emutils) or [Releases](https://github.com/palethea/EMUtils/releases), for example `EMUtils-26.3.jar`.
3. Drop it in your `mods` folder and launch.

Open the settings with the pause menu button, the `/emutils` command, or Mod Menu.

**Supports:** Minecraft 26.2 and 26.3 · Java 25 · Fabric Loader 0.19.3+

EMUtils follows the two latest Minecraft releases. Older versions live on the [`legacy/26.1.x`](https://github.com/palethea/EMUtils/tree/legacy/26.1.x) and [`legacy/1.21.x`](https://github.com/palethea/EMUtils/tree/legacy/1.21.x) branches.

<details>
<summary><strong>Optional integrations</strong></summary>

- **Mod Menu:** opens the EMUtils settings hub from its config button.
- **Iris:** lets Pack Manager apply and turn off shader packs.
- **Minescript:** powers Script Manager (browse, edit, run, and bind scripts).
- **Xaero's Minimap / World Map:** shows Beacon Radius Outline boundaries on the map.

</details>

<details>
<summary><strong>Building from source</strong></summary>

```bash
./gradlew -PmcFamily=26.x -PmcVersion=26.3 build
```

The jar lands in `build/libs/EMUtils-26.3.jar`. Leave out `-PmcVersion` to build the latest supported version. Shared code lives in `src/26_x`, and anything version-specific lives in that version's folder, such as `src/26_3`.

</details>

## License

[Apache License 2.0](LICENSE). Copyright 2026 Palethea. If you use, fork, modify or redistribute EMUtils, keep the original license and attribution notices.
