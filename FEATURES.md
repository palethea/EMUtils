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
- Hide Thunder Flash: stop the sky and world from flashing bright when lightning strikes; thunder still sounds.
- Hide Lightning Bolts: don't draw lightning bolts; they still strike, set fires, and deal damage.

### Visual Tweaks

Toggle small rendering changes without installing separate single-purpose mods.

- No Fog: reduce distance fog where possible.
- Clear Underwater: improve visibility underwater.
- Clear Lava: improve visibility while inside lava.
- No Environment Fog: remove biome and dimension fog.
- No Nether Particles: hide ambient Nether biome particles such as Basalt Deltas ash while preserving gameplay particles.
- No Falling Leaf Particles: hide the leaf particles that drift down from leaves, such as oak, cherry, pale oak and the 26.3 poplar leaves; breaking leaves still shows particles.
- No Fire Overlay: hide the first-person fire overlay while burning.
- Low Fire: keep flames visible but lower on screen.
- Low Shield: draw the shield lower on screen while you block with it, so it covers less of your view. Only the first-person drawing moves; blocking works the same. Off by default.
  - Lowered By: how far down the shield goes, from 10% to 100% (40% by default).
- Low Totem: draw a Totem of Undying you hold lower on screen, in either hand, so it covers less of your view. Only the first-person drawing moves; the totem works the same. Off by default.
  - Lowered By: how far down the totem goes, from 10% to 100% (40% by default).
- Small Totem: draw the Totem of Undying pop smaller, so it covers less of the screen. The totem, its sound and its particles work the same; only the size of the animation changes. Off by default.
  - Totem Size: how big the animation is compared to normal, from 20% to 80% (50% by default).
- No Nausea: hide nausea and portal distortion effects.
- No Spyglass Overlay: hide the spyglass scope overlay.
- No Pumpkin Overlay: hide the carved pumpkin blur while wearing one.
- Hide Effects: hide the vanilla status effect display. Effects still work; only the icons and list go away. Off by default.
  - On the HUD: hide the effect icons in the top-right corner of the screen.
  - Beside the Inventory: hide the effect list, and its tooltips, next to the survival and creative inventory.
- No Hurt Cam: disable hurt camera shake.
- Freelook: look around without turning movement.
  - Keep Perspective: look around in the perspective you're in, first person too, instead of switching to third person. Off by default.
- Beacon Radius Outline: draw a steady chunk-border-style grid cage around each loaded active beacon's true effect boundary, toggleable with a configurable keybind. Each cage takes its beacon's effect color (white before an effect is picked, or the beam's color if it's dyed), and where cages of the same color touch, every other one is drawn lighter.
  - Max Distance: only outline beacons within this many chunks (never past the render distance).
  - Grid Spacing: how many blocks apart the grid lines are (4 to 32, default 16).
  - Line Width: how thick the grid lines are (1 to 5 px); the cage's edges are drawn a bit thicker.
  - Only Active Beacons: only outline beacons that give an effect, skipping ones where no effect has been picked. A beacon you set up shows right away; the server doesn't tell clients when someone else picks an effect, so their newly set up beacons show once the chunk reloads.
  - Xaero Map Integration: show the same colored beacon boundary on Xaero's Minimap and World Map when those optional mods are installed, on by default. On the minimap each cage is a few lines that follow its zoom and rotation, so it costs next to nothing; turn it off to leave the maps alone. The switch only shows when a Xaero map mod is installed.
- Light Level Overlay: show fixed north-facing Minecraft-font block-light numbers on nearby spawnable floors, with yellow or red square markers only at block light level 0; the loaded-chunk scan and geometry are cached and the overlay is toggleable with a configurable keybind.
  - Range: how many blocks around you are scanned (8 to 32, default 24).
  - Only Spawnable Spots: only mark spots at block light 0, where hostile mobs can spawn.
- Own Nametag: show your own nametag in third person.
- Shulker Preview: preview shulker contents in item tooltips.
- Bundle Preview: preview bundle contents in item tooltips.

### Zoom

Hold a configurable keybind for OptiFine-style zoom.

- Zoom Amount: control the zoom multiplier.
- Smooth Transition: fade smoothly into zoom.
- Transition Speed: adjust how quickly zoom fades in.
- Zoom Out Speed: adjust how quickly zoom fades out.
- Scale Sensitivity: turn the mouse down the more you zoom in, so the view moves at the same speed on screen as when zoomed out. On by default.
- Cinematic Camera: enable cinematic camera smoothing while zoomed.
- Hide Hand: hide the held item while zoomed.
- Hide HUD: use an F1-style hidden HUD while zoomed.

### Custom Capes

Show third-party player capes from supported providers.

- Priority List: every provider and the official Minecraft cape in one list you order with the up and down arrows, each with its own switch. For each player the first source in the list that has a cape for them wins, so a provider without a cape for someone falls through to the next. By default the providers come first and Minecraft last, so a provider's cape replaces an official one.
  - Minecraft: the official cape. Move it above a provider to let official capes win over that provider's, or switch it off to hide official capes.
  - OptiFine: OptiFine capes.
  - LabyMod: LabyMod capes.
  - Cosmetica: the cape of the outfit a Cosmetica 2 user wears, animated when the cape is.
  - MinecraftCapes: MinecraftCapes capes.
  - Cloaks+: Cloaks+ capes.

## HUD

### HUD Overlay

Show a configurable info panel with icons and useful world or client stats, as a rounded card in the settings UI's dark or light theme with values lined up in a column. Its settings are split into General, World, Performance and Time tabs.

- HUD Layout Editor: drag HUD elements into a custom layout, resize them from their corner, and set each one's size and opacity from a card next to it. Hold Ctrl to snap to edges and other elements; arrow keys nudge the selected element. The editor's toolbar moves to the bottom of the screen when it would cover more of the elements at the top.
  - Picking Elements: a click picks the smallest element under the mouse, so a small one on top of or inside a big one (like the Tab List) can still be grabbed. Clicking the selected element again, without dragging, selects the next one under that spot.
  - Show Menu: the Show button in the toolbar lists every element with a switch and Show all / Hide all. A hidden element disappears from the editor, can't be hit and doesn't pull others when snapping, so what is under it can be reached. It only changes the editor: the HUD in the game and the saved layout stay as they are.
- Show Icons: show icons beside overlay values.
- Hide With F3: hide the overlay when the debug screen is open.
- Hide in Containers: hide the overlay while a chest, your inventory or another container is open. Off by default.
- Text Shadow: a soft shadow under the text and icons. Auto (the default) adds it below 50% background opacity, so it stays readable over the world; On and Off always or never show it.
- Coordinates: show current XYZ coordinates.
- Free Camera Coordinates: show the free camera XYZ as an extra line while Free Camera is active and Coordinates are enabled.
- Nether Coordinates: show equivalent Nether XYZ while in the Overworld using the 8:1 portal scale; hidden in other dimensions.
- Looking At: show the coordinates of the block you are looking at. Off by default.
- Dimension: show which dimension you are in. Off by default.
- Chunk / Region: show current chunk and region.
- Slime Chunk: show whether you stand in a slime chunk. It needs the world seed, so it works in singleplayer and says Unknown seed on servers. Off by default.
- Biome: show the current biome.
- Facing: show the direction you are facing.
- Speed: show movement speed in blocks per second, including climbing and falling, averaged over half a second; follows your vehicle while riding and works while flying with an elytra. Off by default.
- Ping: show server ping.
- Server TPS: show the server's ticks per second. In singleplayer it's exact and shows the milliseconds a tick takes; on servers it's estimated from the world time the server sends every second, marked with ~. Off by default.
  - Color TPS: green while the server keeps up, amber when it runs slow, red when it lags badly.
  - Compact TPS: show only the TPS, without the milliseconds.
- FPS: show current frame rate.
- Memory: show client memory use.
- Server Time: show world time.
- Day and Night: show the real time left until night (when monsters start spawning) or until day, or Time stopped when the daylight cycle is off. Off by default.
- Real Time: show local time.

### Look-At Info

Show a card about the block or entity in your crosshair, within your reach: its item, name and ID on top, then the lines below, in the HUD Overlay's look. It sits at the top middle of the screen by default and can be moved and resized in the HUD Layout Editor; its width follows the content and it lines up with the left edge, middle or right edge of its place, depending on which third of the screen that place is in. Off by default. Its settings are split into General, Block and Entity tabs.

- Show ID: show the ID under the name, such as minecraft:stone.
- Show Icons: show the item and the icons beside the lines.
- Text Shadow: a shadow under the text and icons, like the HUD Overlay's.
- Hide With F3: hide the card when the debug screen is open.
- Hide in Containers: hide the card while a chest, your inventory or another container is open.
- Blocks: show the card for blocks.
  - Position: the block's coordinates.
  - Hardness: how hard the block is to break, or Unbreakable.
  - Break Time: how long breaking it takes with what you hold, counting enchantments, effects, water and being in midair; Instant or Never when it is.
  - Tool: the tool that mines it fastest, and the lowest tier that gets drops when it needs one, such as Pickaxe · Iron+. Worked out from the vanilla tools themselves, so it follows the game's rules for every block.
  - Can Harvest: for blocks that need a certain tool to drop anything, such as stone, ores and obsidian, whether what you hold gets drops from it, in green or red. Other blocks always drop, so they don't show it.
- Entities: show the card for mobs, players and other entities; a dropped item shows its item and count.
  - Health: health, max health and absorption, green, amber or red by how hurt it is.
  - Armor: armor points, when it has any.
  - Effects: each active effect with its level and time left, green when it helps and red when it harms. Servers only share your own effects and those of what you ride, so other mobs' effects show in singleplayer only.

### Keystrokes

Show your movement keys on screen as keys in the settings UI's look, lighting up while held and fading out a moment after, so quick taps still show. Each key shows what it is bound to, so rebound controls show their own keys. It sits at the left edge of the screen by default and can be moved, resized and given a background opacity in the HUD Layout Editor. Off by default. Its settings are split into Keys and Style tabs.

- Mouse Buttons: show the attack (LMB) and use (RMB) buttons under the movement keys.
  - Clicks per Second: show how many times each was pressed in the last second. Every press counts, whatever the attack and use keys are bound to.
- Jump: show the jump key as a bar.
- Sneak: show the sneak key at the bottom. Off by default.
- Hide in Containers: hide the keys while a chest, your inventory or another container is open. They also hide with F1.
- Key Style: Rounded keys like the menus, or Square.
- Use Menu Accent: held keys light up in the menus' accent color.
  - Pressed Color: with Use Menu Accent off, pick the color held keys light up in.

### Minimap

A map of the area around you in the top-right corner, drawn from each block's real textures, so it follows your resource pack. Close up, every block shows its top texture with its biome's grass, foliage and water colors; farther out the blocks blend into their average color. Height changes are shaded, and where a block stands above the ground south of it a strip of its side shows, giving the map a slight 3D look. Water gets darker the deeper it is. Plants, glass, fences and torches are drawn over the ground under them. The map fills in from the chunks you have loaded and updates as blocks change; the work happens a little at a time and the tiles are drawn on a background thread, so it doesn't stall the game. Your arrow sits in the middle, an N marks north, and your coordinates show under the map. It can be moved, resized and given an opacity in the HUD Layout Editor. On by default, unless Xaero's Minimap is installed. Its settings are split into Map and Show tabs.

- Shape: a Square map, or a Round one.
- Rotate with You: turn the map so the way you face is always up. Turn it off to keep north up.
- Zoom: from 0.25× to 8×. The Minimap Zoom In (`=`) and Minimap Zoom Out (`-`) keys change it too.
- Coordinates: show your coordinates under the map.
- Waypoints: show your waypoints with the same markers as in the world.
  - Pin to Edge: keep waypoints past the map's edge on it, pinned to the edge in their direction. Turn it off to show only the ones in view.

### Armor Status

Show your armor and held items in a card, each with how much durability it has left and a durability bar in the item's own durability color. Held items without durability show how many you carry when they stack, such as blocks and arrows. It sits at the right edge of the screen by default and can be moved, resized and given a background opacity in the HUD Layout Editor; in the lower half of the screen the card sticks to the bottom of its place, so it grows upward. Empty slots are left out. Off by default. Its settings are split into Items and Display tabs.

- Helmet, Chestplate, Leggings, Boots, Main Hand, Off Hand: pick which items show.
- Fireworks with Elytra: while wearing an elytra, show how many firework rockets you carry, in the warning color when you have none.
- Hide in Containers: hide the card while a chest, your inventory or another container is open. It also hides with F1.
- Durability: write it as what's left, what's left out of the maximum, or a percentage.
- Durability Bar: show a bar under each item's durability.
- Low Durability: items with this percentage of durability left or less turn the warning color. 10% by default.
  - Flash When Low: low items fade in and out.
  - Sound When Low: play a sound when an item drops to Low Durability, or one that low is put on. Off by default. Held items are left to Anti-Durability Break's warning while that's on, so they don't warn twice.

### Scoreboard

Take over the server's sidebar scoreboard, so you can place, resize and restyle it like the other HUD elements. It shows exactly what vanilla shows: the objective's title, up to 15 lines sorted the way vanilla sorts them, team prefixes and suffixes, the number formats servers set (blank, fixed, styled), and the sidebar for your team's color when the server sets one. Nothing shows while there is no sidebar; the HUD Layout Editor shows a sample then. It sits at the right edge of the screen, vertically centered, by default, where vanilla draws it, and can be moved, resized and given a background opacity in the HUD Layout Editor. It lines up with the left, middle or right, and the top, middle or bottom, of its place, depending on which third of the screen that place is in, so it keeps its side as the server adds and removes lines. Text too long for the screen is cut with "...". Off by default; with it off, vanilla's own scoreboard shows as before. Its settings are split into Look and Content tabs.

- Style: a Card in the settings UI's look, like the HUD Overlay, or Vanilla's dark bars behind the sidebar. Server colors are made for a dark background, so under the light menu theme the Vanilla style reads better.
- Font: Minecraft's font keeps every server color and style exactly. The EMUtils font keeps colors (including the legacy § color codes servers such as Hypixel put in the text), bold, underline and strikethrough, but not italics or obfuscated text, and text a server sets in another font, such as icon glyphs, is still drawn in Minecraft's.
- Text Shadow: a shadow under the text, like the HUD Overlay's. Auto (the default) adds it below 50% background opacity.
- Show Numbers: show the scores on the right, the red numbers.
- Title: center the title or line it up with the left edge.
- Bold Title: write the title in bold.
- Hide With F3: hide the scoreboard when the debug screen is open.

### Tab List

Take over the player list you hold Tab for, so you can restyle it and place, resize and fade it in the HUD Layout Editor. It lists the same players as vanilla, at most 80, with their display names, team formatting and skin heads, spectators dimmed, the server's header and footer, and the list objective's score as numbers or hearts when the server sets one. Like vanilla, it adds columns as the list grows so no column is longer than 20 players, and it only shows in singleplayer when there is someone else to list or a list objective. It sits at the top middle of the screen by default, where vanilla draws it, and grows downward; with Always Centered off, it lines up with the left, middle or right of its slot, depending on which third of the screen the slot is in. Text too long for the screen is cut with "...". In the HUD Layout Editor it is previewed with sample players until the server lists more. Off by default; with it off, vanilla's own list shows as before. Its settings are split into Look, Players and Ping tabs. It fades and slides in and out at the menus' animation speed.

- Style: a Card in the settings UI's look, like the HUD Overlay, or Vanilla's dark bars. Server colors are made for a dark background, so under the light menu theme the Vanilla style reads better.
- Font: Minecraft's font keeps every server color and style exactly. The EMUtils font keeps colors (including the legacy § color codes servers such as Hypixel put in the text), bold, underline and strikethrough, but not italics or obfuscated text, and text a server sets in another font, such as icon glyphs, is still drawn in Minecraft's. It is drawn from cached glyphs, so a full list stays cheap.
- Text Shadow: a shadow under the text, like the HUD Overlay's. Auto (the default) adds it below 50% background opacity.
- Animation: fade and slide the list in when you press Tab and out when you let go. It follows the Animations setting in the menu settings, so Fast is twice as quick and Off is instant. On by default.
- Always Centered: keep the list in the middle of the screen however wide it gets, like vanilla, so one column is as centered as three. Only its height is placed in the HUD Layout Editor; turn it off to place it anywhere. On by default.
- Sort Players: Vanilla's order (the server's tab list order, spectators last, then team and name), by name, or by lowest ping with unknown pings last.
- Columns: the most columns the list may use. Auto (the default) adds columns like vanilla; a lower number makes longer columns.
- Row Height: how tall each player's row is, 9 to 18 pixels. Vanilla's is 9; the default is 10.
- Heads: show each player's head. Like vanilla, not on offline-mode servers.
- Highlight Yourself: mark your own row in the menu accent color.
- Ping: vanilla's connection bars, a number in milliseconds, or both.
  - Ping Colors: color the number green under 150 ms, amber under 300 ms and red above, so a bad connection stands out.

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

Show the current Spotify track, with its cover, artist and progress, in the pause menu and an optional in-game HUD card when Spotify is running on Linux, macOS, or Windows. Both follow the settings UI's dark or light theme, and the song fades in when it changes. On Windows it reads Spotify straight from the Windows media controls.

- Pause Menu Player: show the card with previous, play/pause and next buttons at the bottom of the pause menu.
  - Scroll Long Titles: scroll through titles and artists too long for the card, like Spotify does; off, they end in "...".
- In-Game HUD Overlay: show the same card while playing; its position, size and background opacity are set in the HUD Layout Editor.
  - Scroll Long Titles: the same, for the HUD card.

## Utility

### World Map

A full-screen map of everything you explored, drawn like the minimap, with block textures close up and colors far out. Open it with `M` (left unbound when Xaero's World Map is installed, which uses `M` too); with the minimap showing, the minimap grows into the world map and shrinks back when you close it. What the map sees is saved per world or server and per dimension in `.minecraft/emutils/maps`, so it remembers places you've been and the minimap shows them too. Drag to move, scroll to zoom around the cursor, and Space takes you back to where you are. The coordinates under the cursor show at the bottom. Your waypoints show on it, and clicking one edits it. Right-click opens a menu: add a waypoint there, or edit, share or delete the one under the cursor, teleport there (when the server lets you use `/tp`), or copy the coordinates. Adding and editing open the waypoint sheet over the map. The other dimensions you explored can be looked at from the panel at the top. On by default, unless Xaero's World Map is installed.

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

Save death and custom waypoints per world or server. In the world each one is a colored marker with the waypoint's initial (a cross for deaths). Markers shrink and fade with distance and fade out right next to you. The name and distance show when you aim at a marker or are within 8 blocks.

- Auto Copy Coords: copy coordinates when a waypoint is created.
- Coord Format: copy coordinates as plain, comma-separated, or teleport-command text.
- Copy Coordinates: a keybind that copies your current position in the Coord Format, or the camera's position while Free Camera is active.
- Copy Coords Feedback: show a chat message with the copied coordinates.
- Deaths to Keep: how many death waypoints a world keeps, from 1 to 10 (3 by default). A new death drops the oldest ones past that, never a custom waypoint, and never a death waypoint you chose to Keep, which isn't counted.
- When You Reach a Death Point: Ask in Chat (default) asks with Remove and Keep buttons when you get back to a death waypoint, Remove It removes it on its own once you are within 5 blocks, and Keep It leaves it. It counts only once you have been 20 blocks away from it, so respawning next to it doesn't trigger it, and hidden waypoints and ones you kept are left alone.
- Add Waypoint at Crosshair: a keybind that opens Add Waypoint at the spot you are aiming at, up to 512 blocks away, ready to name.
- Offer Shared Coordinates: when someone sends coordinates in chat, an offer under the message opens Add Waypoint with them filled in, named after the sender. On by default. Reads plain coordinates (`(23 44 1)`, `x: 123, y: 23, z: 54`, `x123 y23 z43`, `coords: 1 2 3`, a player's message ending in three numbers, a `/tp` command), Xaero's Minimap shares (`xaero-waypoint:...`) and JourneyMap's `[name:Home, x:1, y:2, z:3, dim:0]`. Xaero and JourneyMap shares keep their name, color and dimension. Your own messages and spots that already have a waypoint get no offer, and neither does command or server feedback that merely ends in numbers, like "Changed the block at 1, 2, 3".
- Auto-Add Shared Coordinates: skip the offer and add the waypoint at once, named after the sender (or the share's own name), with a Remove button in the message. Off by default.
- Share Format: how Share in chat writes a waypoint: plain text (`Base (x: 12, y: 64, z: -30)`), Xaero's Minimap, or JourneyMap, which players of those mods can add with their own button.
- Death Color: choose the default color for death waypoints.
- Custom Waypoint Color: choose the default color for custom waypoints.
- Waypoint Opacity: adjust how see-through markers and labels are.
- Waypoint Size: adjust marker size.
- Font: write waypoint names, distances and initials in Minecraft's font or the EMUtils font.
- Label Background: a dark panel behind the name and distance, off (0%) by default.
- Pin to Screen Edge: keep waypoints that are off-screen or behind you at the edge of the screen, with an arrow and their distance.
- Max Distance: hide waypoints beyond a distance (unlimited by default); they fade out over the last fifth.
- Show Other Dimensions: also show the world's waypoints from other dimensions, off by default. Overworld and Nether waypoints appear at the matching spot in the other one (Nether coordinates times eight, the same height), as markers and beacons. End and custom dimensions don't line up, so those stay in the list. The same switch sits in the Current Waypoints list.
- Show on Xaero's Maps: also show the waypoints on Xaero's Minimap and World Map when those optional mods are installed, on by default. They look like Xaero's own waypoints: on the minimap a rounded colored marker with the initial (a cross for deaths) that pins to the edge when out of range, following Waypoint Opacity, Waypoint Size and Max Distance; on the World Map Xaero's own flag with the name that fades in under the cursor, following Xaero's waypoint scale, background and zoom settings. Hidden waypoints stay hidden, and with Show Other Dimensions on, Overworld and Nether waypoints also show at their converted spot. The switch only shows when a Xaero map mod is installed.

## Management

### Settings Hub

Open the EMUtils settings from the EMUtils icon on the title screen and in the pause menu (the first of the small icon buttons), the vanilla Options screen, a keybind, Mod Menu, or the `/emutils` client command. Every feature is a card with a short description, grouped by category, with search. A card's switch turns the feature on or off, Open opens its screen, and the rest of the card opens a sheet with its settings and keybinds. All EMUtils screens share this look.

- Mod Menu Integration: open the EMUtils settings from Mod Menu's config button when Mod Menu is installed.
- Menu Settings: the palette button in the header, or the Menus card, sets how every EMUtils menu looks, in four tabs: Colors, Fonts, Layout and Effects. Menu settings stay the same for every profile.
  - Theme: dark or light.
  - Accent Color: one of eight presets or any color, for buttons, switches and highlights. Text on a light accent turns dark so it stays readable.
  - Use the Profile's Color: the accent follows the active profile's color, so you can see which profile you're on.
  - Font: Nunito, Inter, Rubik, Figtree, or Minecraft's own font.
  - Code Font: JetBrains Mono, Fira Code or Cascadia Code for scripts and shortcut commands, with a size from 80% to 150%.
  - Text Size: menu text from 90% to 125%, separate from Minecraft's GUI scale.
  - Animations: Normal, Fast, or Off for no motion at all.
  - Background Blur and Dimming: how strongly the world behind menus is blurred and darkened, down to none.
  - Panel Opacity: let a little of the world show through the panel.
  - Corner Roundness: from square corners to round ones.
  - Compact Cards: smaller cards without descriptions, four to a row.
  - Remember Last Menu Position: reopen the menu on the category, scroll position and settings sheet you left it on, instead of the hub. Off by default; search text is not kept.
  - Category Colors: turn the colored category dots off.
  - High Contrast: stronger secondary text, borders and dividers.
- `/emutils` Command: open settings, toggle features by name, reset a feature to its defaults, switch profiles, and export or import the active profile's settings together with the EMUtils keybinds through the clipboard.

### Screenshot Gallery

Browse, copy, open, sort, and delete recent screenshots from inside Minecraft.

- Gallery Sort: choose how screenshots are ordered.
- Confirm Deletes: ask before deleting screenshots.
- Max Screenshots: limit how many captures are shown.

### Current Waypoints

View and manage saved waypoints for the current world or server.

- Edit Waypoint: change a waypoint's name, set, position, color and beacon after creating it, with the pencil button or by clicking its row.
- Share in chat: the speech bubble button says the waypoint in chat in the chosen Share Format. Nothing is sent unless you press it.
- Tags: only the special waypoints are tagged, a death waypoint with "Death" and one from another dimension with where it comes from.
- Search: type to narrow the list to waypoints whose name or set matches; Esc clears it.
- Sort: order the list by distance (default), name or newest.
- Sets: give a waypoint a set in the add and edit sheet, typing a name or picking one already in use. A list with sets shows them as headers, alphabetically, with the waypoints in no set last; click a header to fold it up. A search opens folded sets it finds something in.
- Other Dimensions: a switch above the list (also in the Waypoints settings) that adds the waypoints made in the world's other dimensions. Overworld and Nether ones show at converted coordinates with a "From Nether" or "From Overworld" tag, copy those coordinates, and appear in the world; End and custom dimension ones show their own coordinates and dimension, without a distance. Editing such a waypoint shows the coordinates it was saved with. Clear all only deletes the current dimension's waypoints.
- Nearby Removal Prompts: with When You Reach a Death Point set to Ask in Chat, a death waypoint you get back to asks in chat whether to remove or keep it.
- Coordinate Copying: copy waypoint coordinates in the configured format.
- Waypoint Visibility: hide individual waypoints without deleting them.

### Pack Manager

Browse installed and online resource packs or shader packs in-game.

- Resource Pack Management: enable, disable, delete, and apply installed resource packs.
- Shader Pack Management: apply or turn off Iris shader packs when Iris is installed.
- Modrinth Support: search and download resource packs or shader packs into the right pack folders.
- Replace Resource Packs Button: make the Resource Packs button in Minecraft's Options open the Pack Manager. Shift-click it for Minecraft's own screen. Off by default.

### Script Manager

When Minescript is installed, browse and manage scripts from `minecraft/minescript`.

- Script Editor: create and edit Python scripts, with line numbers and syntax colors. Enter keeps the indentation (and indents after a colon), Tab inserts four spaces or indents the selected lines, brackets and quotes close themselves, Ctrl+/ toggles comments and Ctrl+D duplicates lines.
- Find: Ctrl+F in the editor highlights every match; Enter and Shift+Enter move between them.
- Run Scripts: launch scripts locally through Minescript.
- Run Errors: when a script fails, the footer shows the error and its line, and the editor marks that line.
- Files and Folders: rename or move scripts and folders (keybinds come along), create folders, and delete folders, from the buttons or by right-clicking the list.
- Python Check: warns when Minescript's Python doesn't work (such as the Microsoft Store shortcut it uses by default on Windows) and switches it to a working Python in one click, or links to python.org when none is installed.
- Script Keybinds: assign EMUtils-managed keybinds per script.

### Command Shortcuts

Create and manage saved quick commands.

- Saved Commands: keep reusable commands (starting with /) or chat messages, each sent by its own key combination while playing.
- Shortcut Management: add and edit shortcuts in a form with a key field that captures the combination (keys another shortcut uses are refused), run one right away, delete one, or clear them all.

### Profiles

Keep several sets of EMUtils settings, such as one for a PvP server and one for singleplayer, and switch between them. Each profile holds every EMUtils setting, including the HUD layout; keybinds, waypoints, shortcuts and other saved lists stay the same for every profile. Changes save to the active profile as you make them.

- Profile Switcher: pick a profile from the button next to the theme switch in the settings header, with the `/emutils profile <name>` command, or cycle through them with a keybind (unbound by default).
- Profile Icons: each profile is shown by an icon and color of your choice.
- Auto-Switch: link a profile to servers, typed or picked from your multiplayer server list (a domain also covers its subdomains, such as `hypixel.net` for `mc.hypixel.net`), or to singleplayer worlds, and it loads when you join. Joining anywhere else goes back to the profile you last picked yourself.
- Profile Management: create profiles from the current settings or the defaults, rename, duplicate, reorder, reset to defaults, and delete them. Names are unique, and the Default profile always stays.
- Import and Export: copy a profile to the clipboard to share it, and add one from the clipboard, including a config copied with `/emutils export`.

### Keybinds

Every EMUtils key on one page, opened from the Keybinds card or the keyboard button in the settings header. Keys are grouped by category like the settings, plus General for keys that don't belong to one feature, such as Open Settings. Changes here and in a feature's sheet are the same keys, so both always agree.

- Rebinding: click a key, here or in a feature's sheet, and press the new one. Esc cancels, Backspace clears it, and a right click puts back its default; a hint says so while the key waits. A bound key shows an × while hovered that unbinds it right away.
- Conflicts: a key shared with anything else, including vanilla keys and other mods, is highlighted, with what it clashes with. Conflicts Only lists just those.
- Search: find a key by its name, its feature or the key it's bound to.
- Feature Links: each key links to its feature's sheet, which opens over the page.
- Reset All: put every EMUtils key back to its default.
- Script Keybinds and Command Shortcuts: listed read-only, with a link to the Script Manager or Command Shortcuts where they're edited.

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
  - Global Hover Transfer: optionally allow hover transfer in normal inventory-style screens too, including the player inventory.
- Sort Buttons: show three sort buttons beside storage containers and the player inventory for sorting by name, category, or quantity.
- Sort Speed: choose Normal sorting or Legit sorting that spaces operations out over ticks.
- Quick Stack: use its container button or configurable keybind to move matching items from your inventory into a container that already holds the same item.
- Quick Stack Speed: choose Normal transfers or Legit transfers that add a short varied delay between matching stacks.
- Auto Refill: refill the active hotbar slot with a matching block stack when block placement empties it.
- Inventory Search: a search box in the EMUtils card look above chests, barrels, shulker boxes, hoppers, dispensers and your inventory. Items whose name, ID or lore contain every word you type are outlined in yellow. Ctrl+F jumps to the box, typing there never closes the screen or drops items, and Enter or a click elsewhere leaves it.
  - Dim Other Items: darken the items that don't match.
  - Search Inside Shulker Boxes: a shulker box, or any other item holding items, with a match inside is outlined too, with a filled corner, and its tooltip preview dims what doesn't match, so you see which box and where in it.
  - Keep Search Text: keep what you typed for the next container, so you can look through chest after chest.
  - Search in Creative: also show the box in the creative inventory, above its tabs. It only marks your own inventory, not the item tabs, which have their own search. Off by default.
- Inventory Preview: show a small inventory preview above the hotbar.
- Preserve Container Cursor: keep the mouse cursor in place when switching between container screens.
- Mass Drop: maintain a list of items to drop, found by searching every item by name or ID, then drop matching inventory stacks with a configurable keybind. The list shows how many of each item you carry, and the drop key and mode sit at the top.
  - Legit: drop one matching inventory slot per key press.
  - Unfair: drop every matching inventory slot per key press.

### Fast Place

Remove the block placement delay for faster building.

### Fast Use

Remove the delay between non-placement uses, including doors, levers, buttons, snowballs, eggs, and other item actions.

### Anti Durability Break

Prevent a held damageable item from attacking, breaking blocks, swinging, or performing item actions once it gets low on durability, keeping it safe until repaired or replaced.

- Count In: count the thresholds in durability points left or in percent of the item's maximum durability.
- Protect At: how low an item can get before it's protected (default 5 durability).
- Low Durability Warning: show a message above the hotbar and play a sound when a held item drops to Warn At, or when you switch to one that's already that low.
- Warn At: how low counts as low for the warning (default 20 durability).

### Auto Tool

Automatically select the fastest suitable tool using each item and block's public mining-speed and correct-tool data, including modded content.

- Legit: switch only between tools already in the hotbar.
- Unfair: search the whole player inventory and swap the best tool into the currently selected hotbar slot.
- Return to Previous Item: switch back to the item you were holding after Auto Tool stops using a tool.
- Enchantments: a priority list of the enchantments to prefer between tools that can mine the block and get its drops; the first one that tells two tools apart decides, and the fastest tool wins when none does. Each can be turned off and moved up or down. By default Fortune, then Silk Touch, then Efficiency.
  - Fortune: the highest Fortune, on ores, glowstone, sea lanterns, melons and amethyst clusters.
  - Silk Touch: a Silk Touch tool, on blocks it keeps whole, such as ores, glass, ice, bookshelves, ender chests, sculk blocks, bee nests and campfires.
  - Efficiency: the fastest tool, counting Efficiency.
- Hotbar Slots: the hotbar slots Auto Tool may pick tools from, so a slot kept for a weapon or blocks is left alone. Unfair still searches the rest of the inventory too.

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

### Locked Y Placement

Press a configurable keybind while targeting a block to lock block placements to that block's Y coordinate. The HUD overlay shows the active placement Y, and pressing the keybind while looking in the air or toggling the feature off clears the lock.

### Free Camera

Toggle a detached spectator-like camera while the visible real player continues to receive normal physics and knockback without accepting movement input. Move the camera with the normal movement keys, ascend with jump, descend with sneak, and hold sprint for a configurable speed boost. Free Camera defaults to a clean spectator-style HUD with held items and survival HUD elements hidden, and can switch to the regular HUD to show the real player's hand, hotbar, health, armor, food, and XP while the camera is detached. Left and right clicks act from the real player's position and block picking remains suppressed until the camera is restored. Through a dimension change or a respawn the camera stays detached and keeps facing the same way, and stays where it was when you end up in the same dimension.

- Free Camera HUD: a clean spectator-style view, or the regular HUD.
- Free Camera Boost: how much faster the camera moves while sprinting, both across and up and down.
- Horizontal Speed: how fast the camera moves forward, back and sideways, from 10% to 500%.
- Vertical Speed: how fast the camera moves up with jump and down with sneak, from 10% to 500%.
- Collision: stop the camera at blocks instead of passing through them. Off by default.
- Double-Tap to Start: start Free Camera with a double tap of its key, so a stray press doesn't detach the camera. One press still ends it. Off by default.
- Camera FOV: use a field of view of its own while the camera is detached. Off by default.
  - Field of View: the detached camera's field of view, from 30 to 110. Zoom still works on top of it.
