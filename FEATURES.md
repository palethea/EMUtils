# Features

EMUtils features are organized to match the in-game settings hub: Render, HUD, Utility, Management, and QoL.

## Render

### Fullbright

Brighten dark areas without changing the vanilla gamma slider.

- Fullbright Strength: adjust how strongly dark areas are brightened.

### Clear Weather

Hide distracting weather effects while keeping the world playable.

- Hide Rain: remove visible rain.
- Hide Snow: remove visible snow.
- Hide Rain Particles and Sounds: remove rain particles and audio effects.

### Visual Tweaks

Toggle small rendering changes without installing separate single-purpose mods.

- No Fog: reduce distance fog where possible.
- Clear Underwater: improve visibility underwater.
- Clear Lava: improve visibility while inside lava.
- No Environment Fog: remove biome and dimension fog.
- No Nether Particles: hide ambient Nether biome particles such as Basalt Deltas ash while preserving gameplay particles.
- No Fire Overlay: hide the first-person fire overlay while burning.
- Low Fire: keep flames visible but lower on screen.
- No Nausea: hide nausea and portal distortion effects.
- No Spyglass Overlay: hide the spyglass scope overlay.
- No Hurt Cam: disable hurt camera shake.
- Freelook: look around without turning movement.
- Own Nametag: show your own nametag in third person.
- Shulker Preview: preview shulker contents in item tooltips.
- Bundle Preview: preview bundle contents in item tooltips.

### Zoom

Hold a configurable keybind for OptiFine-style zoom.

- Zoom Amount: control the zoom multiplier.
- Smooth Transition: fade smoothly into zoom.
- Transition Speed: adjust how quickly zoom fades in.
- Zoom Out Speed: adjust how quickly zoom fades out.
- Cinematic Camera: enable cinematic camera smoothing while zoomed.
- Hide Hand: hide the held item while zoomed.
- Hide HUD: use an F1-style hidden HUD while zoomed.

### Custom Capes

Show third-party player capes from supported providers.

- Preferred Provider: choose which provider to prefer when more than one cape is available.
- OptiFine: enable OptiFine capes.
- LabyMod: enable LabyMod capes.
- MinecraftCapes: enable MinecraftCapes capes.
- Cosmetica: enable Cosmetica capes.
- Cloaks+: enable Cloaks+ capes.

## HUD

### HUD Overlay

Show a configurable info panel with icons and useful world or client stats.

- HUD Layout Editor: drag HUD elements into a custom layout.
- Show Icons: show icons beside overlay values.
- Hide With F3: hide the overlay when the debug screen is open.
- Coordinates: show current XYZ coordinates.
- Nether Coordinates: show equivalent Nether XYZ while in the Overworld using the 8:1 portal scale; hidden in other dimensions.
- Chunk / Region: show current chunk and region.
- Biome: show the current biome.
- Facing: show the direction you are facing.
- Ping: show server ping.
- FPS: show current frame rate.
- Memory: show client memory use.
- Server Time: show world time.
- Real Time: show local time.

### Food HUD

Show food and saturation information on the vanilla hunger bar.

- Saturation Overlay: show saturation on the hunger bar.
- Held Food Preview: preview the food value of the held item.
- Offhand Food Preview: preview the food value of the offhand item.
- Exhaustion Underlay: show exhaustion progress below the hunger bar.
- Match Vanilla Shake: keep vanilla hunger bar animation behavior.
- Food Tooltips: add food values to item tooltips.
- Always Show Tooltips: show food tooltip values even outside normal comparison cases.

### Spotify Player

Show the current Spotify track in the pause menu and optional in-game HUD overlay when Spotify is running on Linux, macOS, or Windows.

- Pause Menu Player: show now-playing controls in the pause menu.
- In-Game HUD Overlay: show the same now-playing panel while playing.

## Utility

### Auto Reconnect

Reconnect to the last server after being kicked, with a countdown button on the disconnect screen.

- Retry Delay: set the delay before reconnecting.
- Always Retry: retry until manually stopped.
- Max Tries: limit automatic reconnect attempts.

### Screenshot Helper

Replace the default screenshot message with quick actions and optional metadata.

- Auto Copy Screenshot: copy screenshots after capture when supported.
- Screenshot Metadata: save location, server, dimension, and related context in `screenshots/metadata`.

### Waypoints

Save death and custom waypoints per world or server.

- Auto Copy Coords: copy coordinates when a waypoint is created.
- Coord Format: copy coordinates as plain, comma-separated, or teleport-command text.
- Death Color: choose the default color for death waypoints.
- Custom Waypoint Color: choose the default color for custom waypoints.
- Waypoint Opacity: adjust in-world label opacity.
- Waypoint Size: adjust in-world label size.

## Management

### Screenshot Gallery

Browse, copy, open, sort, and delete recent screenshots from inside Minecraft.

- Gallery Sort: choose how screenshots are ordered.
- Confirm Deletes: ask before deleting screenshots.
- Max Screenshots: limit how many captures are shown.

### Current Waypoints

View and manage saved waypoints for the current world or server.

- Nearby Removal Prompts: remove nearby waypoints quickly while playing.
- Coordinate Copying: copy waypoint coordinates in the configured format.
- Waypoint Visibility: hide individual waypoints without deleting them.

### Pack Manager

Browse installed and online resource packs or shader packs in-game.

- Resource Pack Management: enable, disable, delete, and apply installed resource packs.
- Shader Pack Management: apply or turn off Iris shader packs when Iris is installed.
- Modrinth Support: search and download resource packs or shader packs into the right pack folders.

### Script Manager

When Minescript is installed, browse and manage scripts from `minecraft/minescript`.

- Script Editor: create and edit Python scripts.
- Run Scripts: launch scripts locally through Minescript.
- Script Keybinds: assign EMUtils-managed keybinds per script.

### Command Shortcuts

Create and manage saved quick commands.

- Saved Commands: keep reusable chat commands or messages.
- Shortcut Management: add, edit, and clear shortcuts from the EMUtils settings hub.

## QoL

### Chat Features

Improve chat reading, copying, and notifications.

- Copy Chat: copy full chat messages with Ctrl + left click.
- Copy Formatting: include formatting when copying chat.
- Copy Feedback: show feedback after copying.
- Chat Timestamps: prepend timestamps to chat messages.
- 24-Hour Clock: use 24-hour timestamps.
- Smart Chat Filters: collapse repeated messages.
- Duplicate Time Window: group duplicate messages within a configurable time window.
- Mention Alerts: play sound or toast alerts when you are mentioned.
- Mention Highlight: highlight mentions with configurable color and style.

### Inventory Tools

Protect important slots and move items faster.

- Slot Locking: lock inventory slots per world or server.
- Lock Color: choose the locked-slot overlay color.
- Bound Slot Color: choose the bound-slot overlay color.
- Slot Binding: bind hotbar-safe slot swaps.
- Lock Bound Slots: protect bound slots from unsafe movement.
- Hover Transfer: hold Shift + left click in a storage container, then hover items to move them in or out quickly while ignoring locked or bound items.
- Sort Buttons: show three sort buttons beside storage containers and the player inventory for sorting by name, category, or quantity.
- Sort Speed: choose Normal sorting or Anti-Cheat sorting that spaces operations out over ticks.
- Quick Stack: use its container button or configurable keybind to move matching items from your inventory into a container that already holds the same item.
- Quick Stack Speed: choose Normal transfers or Anti-Cheat transfers that move one stack per tick.
- Inventory Preview: show a small inventory preview above the hotbar.
- Preserve Container Cursor: keep the mouse cursor in place when switching between container screens.
- Mass Drop: maintain a searchable list of items by icon, display name, and real item ID, then drop matching inventory stacks with a configurable keybind.
  - Legit: drop one matching inventory slot per key press.
  - Unfair: drop every matching inventory slot per key press.

### Fast Place

Remove the block placement delay for faster building.

### Anti Durability Break

Prevent a held damageable item from attacking, breaking blocks, swinging, or performing item actions once it reaches 5 durability, keeping it safe until repaired or replaced.

### Auto Tool

Automatically select the fastest suitable tool using each item and block's public mining-speed and correct-tool data, including modded content.

- Legit: switch only between tools already in the hotbar.
- Unfair: search the whole player inventory and swap the best tool into the currently selected hotbar slot.

### Auto Flight Gear

Temporarily prepare flight gear while falling, then restore the displaced items after landing.

- Auto Switch Elytra: equip an Elytra from the player inventory while falling and restore the previous chest equipment after landing.
- Auto Switch Rockets: move Flight Rockets into a configurable hotbar slot while falling and return both swapped stacks after landing.
- Rocket Hotbar Slot: choose slots 1-9 as the temporary Flight Rocket destination.
- Ignore Normal Jumps: wait until no selectable block is directly below within the player's block-interaction reach before switching gear.
- Start With Double Jump: replace automatic fall activation with a double-tap of the jump key that equips the Elytra and starts gliding immediately.

### Safe Walk

Prevent walking off ledges without slowing down or holding sneak. Jumping still lets you leave an edge normally.

### Place Below

Hold a configurable keybind while placing against a block to redirect the placement to the underside of the targeted block.

### Free Camera

Toggle a detached spectator-like camera while the real player remains frozen in place. Move with the normal movement keys, ascend with jump, descend with sneak, and hold sprint for faster travel. Player attacks, item use, and block picking are suppressed until the camera is restored.
