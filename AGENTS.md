# EMUtils Codex Rules

These instructions apply when working in `/home/matt/.code/java/client-mods/EMUtils`.

## Post-change workflow

After any functional code or asset change in this repo, unless the user explicitly says otherwise, finish the task with all of these steps before reporting done.

### 1. Bump version

- Edit `gradle.properties` -> `mod_version`.
- Patch bump on every functional change: `0.10.0` -> `0.10.1` -> `0.10.2`, etc.
- Only bump minor or major when the user explicitly asks.
- Version flows into `fabric.mod.json` via Gradle; do not edit the mod json directly.

### 2. Build

```bash
cd /home/matt/.code/java/client-mods/EMUtils && ./gradlew build -x test
```

### 3. Deploy jar

Copy the built jar to the EMUtils EMLauncher instance:

```bash
cp build/libs/EMUtils.jar \
  ~/.local/share/emlauncher/instances/f8950721-aacb-4291-83fd-a39eb000e2dd/minecraft/mods/EMUtils.jar
```

Also copy to the manual 2-player testing instance. Copy jar only; do not restart this instance.

```bash
cp build/libs/EMUtils.jar \
  ~/.local/share/emlauncher/instances/a1085ca9-cfc8-474f-895b-f8c24522dad0/minecraft/mods/EMUtils.jar
```

### 4. Restart instance

```bash
export PATH="$HOME/.local/bin:$PATH"
emlauncher ctl restart EMUtils --json
```

### 5. Crash check

Wait 5 seconds, then verify the instance is still running. If it crashed or exited immediately, read the log and fix the issue before reporting done.

```bash
export PATH="$HOME/.local/bin:$PATH"
sleep 5
emlauncher ctl list --json
```

Healthy means `running: true` and a live `pid`.

Unhealthy means `running: false`, missing or stale `pid`, or session state `exited` / `crashed`.

```bash
sqlite3 ~/.local/share/emlauncher/emlauncher.db \
  "SELECT instance_id, pid, state FROM running_sessions WHERE instance_id='f8950721-aacb-4291-83fd-a39eb000e2dd';"
tail -n 120 ~/.local/share/emlauncher/logs/f8950721-aacb-4291-83fd-a39eb000e2dd.log
```

On crash, diagnose from the log tail, fix the code, then repeat from step 1, including another version bump.

### Exemptions

Skip version bump, build, deploy, and restart only when:

- The user explicitly says not to, such as "don't restart", "no deploy", or "docs only".
- The work is purely read-only, such as answering questions or reviewing without edits.

When finishing implementation, briefly state the new version, jar deployment to both instances, restart result, and post-restart health.

## Hypixel SkyBlock API and data patterns

These notes apply when working under `src/client/java/net/emutils/client/skyblock/**`.

Reference implementation: SkyHanni, a Kotlin Fabric mod at `https://github.com/hannibal002/SkyHanni`. Keep EMUtils lightweight and prefer in-game signals over heavy external dependencies.

### Data layers, in order

1. In-game signals, preferred and no API key:
   - Scoreboard title and lines: SkyBlock mode, guest or co-op variants, purse or piggy, area, server id.
   - Tab list widgets: island name and profile detection, such as `Profile: Mango`.
   - Chat messages: events, sack changes, rare drops, party or guild messages, filtered with regex.
   - Inventory or GUI titles plus lore: menu detection, item stats, AH/BZ screens.
   - `/locraw` JSON: server, gametype, lobby, mode, map. It is sent as chat JSON and should be rate-limited around 15 seconds.

2. Hypixel Mod API, optional Fabric integration:
   - Subscribe to `ClientboundLocationPacket` for server name, server type, mode, and map.
   - Use `ClientboundHelloPacket` to distinguish production vs alpha.
   - Derive `inSkyblock` from `GameType.SKYBLOCK`, and island from `mode`.
   - Confirm guest islands via scoreboard title ending in `GUEST`.
   - Prefer this over `/locraw` when available, while keeping scoreboard fallback because Mod API can be disabled or unreliable.

3. Hypixel HTTP API, rate-limited and often API-key based:
   - Base URL: `https://api.hypixel.net/v2/...`
   - `/skyblock/bazaar`: instant buy/sell prices for all products. Poll around every 2 minutes and cache the map.
   - `/resources/skyblock/items`: NPC prices, motes, and base item stats. Load once at startup.
   - `/skyblock/election`: mayor and perks.
   - `/skyblock/profiles?uuid=&profile=`: profile inventories and collections. Requires an API key and has rate limits.

### EMUtils implementation guidance

- EMUtils already uses tab-list profile parsing in `SkyblockProfileDetector`; extend with scoreboard island and area checks before adding HTTP.
- Bazaar and Auction House tooltip features should start with the Bazaar endpoint plus an optional user API key in config.
- Never block the render thread for HTTP. Cache aggressively and expose manual refresh when useful.
- Map Hypixel item IDs to internal names using a lightweight local map, Hypixel `/resources/skyblock/items`, or an optional NEU API dependency if installed.
- Keep all SkyBlock state centralized through `SkyblockContext` / `SkyblockManager`; feature modules should subscribe to events such as profile join, island join, and area change.
- Keep SkyBlock storage profile-scoped. `StoragePreviewStore` already keys by profile; follow that pattern for all SkyBlock data.

### Scope boundaries

Good EMUtils-sized features:

- Bazaar price tags on tooltips.
- Chat filters for hub spam or sack messages.
- Current chat channel indicator.
- Not-clickable item gray-out in known GUIs.
- Compact item labels such as stars, pet level, and cake year.
- Sack display overlay.
- Item pickup log with coin value.
- Auction House price tags when supported by API data, NEU data, or manual AH scanning.
- Scoreboard/tab quality-of-life features if they fit the HUD.

Avoid for EMUtils scope:

- Dungeon copilot features.
- Garden macros.
- Full NEU repo fork.
- Slayer overlays that are too SkyHanni-specific or heavy.

Compliance:

- Respect Hypixel API rate limits.
- Document optional API key settings.
- Prefer reading client data over automating gameplay.
- `/locraw` and Mod API usage should stay read-only and only gate features or update context.
