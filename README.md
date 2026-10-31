# Rocket Science

A modular Fabric utility client for **Minecraft 1.21.4** with a tactical **"Command Panel"** interface:
deep navy surfaces, electric blue accents, crimson highlights for combat and warnings.

> Many servers forbid ESP, aim assistance and similar tools. Only use these modules where the
> server rules allow it (your own worlds, anarchy servers, servers that explicitly permit clients).

---

## Features

| Category | Module | What it does |
|---|---|---|
| World | **Base Finder** | Scores loaded chunks by "player" blocks (storage clusters, beds, signs, crafting stations, configurable list), highlights high scoring chunks and alerts with coordinates. |
| World | **Player Finder** | Logs players entering/leaving render distance (name, XYZ, distance, time), toast/sound/chat alerts, last-known-position waypoints. Log shown in the **Players** panel. |
| World | **Storage ESP** | Boxes and/or tracers on chests, trapped chests, barrels, shulkers, ender chests, hoppers and furnaces through walls. Per-type toggle + colour, fill/outline, opacity, range. |
| Render | **Player ESP** | Box / 2D outline / glow modes, tracers, nametags (health + distance), colour by friend status or distance. |
| Render | **Tracers** | Lines from the crosshair to players (friends blue), hostile mobs, passive mobs and dropped items. Per-type toggle + colour, body/head/feet target, range, distance fade. |
| Render | **Free Cam** | Detached camera flight; your player stays still server-side. Optional interaction. Restores cleanly. |
| Combat | **Aim Assist** | Eased (never snapped) aim toward the best target in an FOV cone. FOV, range, speed, priority, friends, players-only. |
| Player | **Reach** | Extends entity (default 3.5) and block (default 5.0) reach. Servers verify distance, so large values get rejected or flagged. |
| Player | **Auto Sprint** | Tiny example module. |
| Misc | **HUD** | Gradient watermark, animated ArrayList, coords/FPS/facing, toasts. |
| Misc | **ClickGUI** | Opens the Command Panel (**Right Shift**, rebindable). |

Also: friends system, `.`-prefixed commands, JSON profiles, bundled Roboto UI font.

### The Command Panel UI

* **ClickGUI** (`Right Shift`): draggable category panels with a thin blue header bar and a red accent
  line, 5px rounded corners, translucency and 1px borders. Panels slide in with a stagger on open.
* Modules **glow blue** when enabled (eased), stay **grey** when off; **combat modules carry a red tick**.
* **Right-click** a module to expand its settings: toggles, sliders, dropdowns, HSV colour pickers,
  keybinds and list editors, all themed. Right-click a header to collapse a panel.
* **Search bar** at the top - just start typing.
* **Players** panel (Player Finder log, click a name to friend/unfriend) and **Friends** panel.
* Panels auto-wrap into rows to fit your screen and can't be dragged or scrolled off it.
  Scrolling over a panel moves that panel, scrolling over empty space moves all of them.
  **↺ reset layout** (bottom-right) or `.gui reset` restores the default arrangement.
  Positions are saved per profile.
* **HUD**: blue-to-red watermark (top-left), ArrayList sorted by width with slide animations
  (top-right), coordinates incl. nether/overworld conversion, FPS and facing (bottom-left),
  toasts (bottom-right) with a **blue border for "on"** and **red for "off"**.

---

## Building

Requirements: **JDK 21**. Gradle is provided by the wrapper.

```bash
./gradlew build
```

The mod jar is written to `build/libs/rocketscience-1.0.0.jar`. Copy it, together with
[Fabric API](https://modrinth.com/mod/fabric-api) for 1.21.4, into `.minecraft/mods/` of a
Fabric Loader 0.16.10+ installation.

Run a development client directly:

```bash
./gradlew runClient
```

IDE setup: open the folder in IntelliJ IDEA (Gradle import) and run `./gradlew genSources` for
readable Minecraft sources.

**Changing Minecraft version**: update `gradle.properties` with the values from
<https://fabricmc.net/develop> and review the version-sensitive spots listed at the end of this file.

---

## Usage

### Commands (prefix `.`, never sent to the server)

| Command | Description |
|---|---|
| `.help` | List commands |
| `.toggle <module>` | Toggle a module (`.t`) - names ignore case and spaces, e.g. `.t basefinder` |
| `.bind <module> <key\|none>` | Bind a key, e.g. `.bind playeresp g`, `.bind clickgui rshift` |
| `.set <module> <setting> <value>` | Change any setting, e.g. `.set aimassist fov 45`, `.set storageesp chestcolor #FFAA00` |
| `.friend add\|remove\|list [name]` | Manage friends |
| `.config save\|load\|delete\|list [name]` | Named profiles |
| `.players [clear]` | Show/clear the Player Finder log |
| `.prefix <symbol>` | Change the command prefix |
| `.gui reset` | Reset ClickGUI panel positions |

List settings accept `add <entry>` / `remove <entry>`:
`.set basefinder blocks add minecraft:obsidian=0.5`.

### Config files

```
.minecraft/rocketscience/
  client.json            active profile + prefix
  friends.json           global friend list
  profiles/<name>.json   module states, settings, keybinds and GUI layout
```

The active profile is saved when the ClickGUI closes and when the game exits. A config folder
from the client's old name (`.minecraft/sentinel/`) is moved over automatically on first launch.

### Base Finder scoring

Each entry is `id[=weight]` or `#tag[=weight]` (e.g. `minecraft:hopper=3`, `#minecraft:beds`).
Built-in weights cover the defaults (shulker boxes 5, beacons 6, hoppers 3, chests 1...); any other
block defaults to 2. Four or more chests/barrels in one chunk add a cluster bonus. Chunks at or above
**Threshold** are boxed and alerted once. Block-entity blocks come from the chunk's block-entity
map; other blocks are found by checking each 16x16x16 section's palette first, so only sections that
can contain a target are scanned. **Scan Rate** controls chunks processed per tick.

---

## Architecture

```
dev.rocketscience
├── RocketScience.java         entrypoint: creates managers, wires Fabric API events
├── module/                    Module base class, Category, ModuleManager
├── modules/<category>/        the modules themselves
├── setting/                   Setting<T> + Bool/Int/Double/Color/Enum/Keybind/StringList
├── command/                   Command base, CommandManager, commands/
├── config/ConfigManager       JSON profiles
├── friend/FriendManager       friends.json
├── gui/                       Theme, Animation, ClickGuiScreen, GuiState, component/...
├── hud/                       HudRenderer, Notifications
├── render/                    Render3D (batched world shapes), Projection (world->screen),
│                              Render2D (rounded rects, outlines, shadows), FontRenderer
├── util/                      ChatUtil, ColorUtil, KeyUtil, WorldUtil
└── mixin/                     6 small mixins (see below)
```

**Events**: ticks, world rendering, HUD rendering, chat, chunk load/unload, disconnect and shutdown
all come from Fabric API. The `ModuleManager` forwards them to enabled modules and wraps each call
so a crashing module is logged and disabled instead of taking the game down.

**Mixins** - only where Fabric API has no hook, each a few lines:

| Mixin | Purpose |
|---|---|
| `KeyboardMixin` | Raw key presses -> module keybinds (only with no screen open) |
| `KeyboardInputMixin` | Free Cam: zero the player's movement input |
| `CameraMixin` | Free Cam: override camera position/rotation, render own body |
| `EntityMixin` | Free Cam mouse look; Player ESP glow colour |
| `MinecraftClientMixin` | Player ESP glow mode: force the outline shader for players |
| `PlayerEntityMixin` | Reach: raise the local player's entity/block interaction range |

---

## Adding a module

1. Create a class in `dev.rocketscience.modules.<category>`:

```java
package dev.rocketscience.modules.player;

import dev.rocketscience.module.Category;
import dev.rocketscience.module.Module;
import dev.rocketscience.setting.BoolSetting;
import dev.rocketscience.setting.DoubleSetting;

/** Example: jump automatically while moving. */
public class AutoJump extends Module {
    // Settings declared as fields appear in the GUI and are saved automatically.
    private final BoolSetting onlyForward = bool("Only Forward", "Only while holding W", true);
    private final DoubleSetting chance = doubleSetting("Chance", "Probability per tick", 1.0, 0.0, 1.0, 0.05);

    public AutoJump() {
        super("Auto Jump", "Jumps for you", Category.PLAYER);   // optional 4th arg: default GLFW key
    }

    @Override
    public void onTick() {                     // also: onEnable, onDisable, onPreTick,
        if (mc.player == null) return;         //       onRender3D, onRender2D, onWorldLeave, getInfo
        if (onlyForward.get() && !mc.options.forwardKey.isPressed()) return;
        if (mc.player.isOnGround() && Math.random() < chance.get()) mc.player.jump();
    }
}
```

2. Register it in `ModuleManager#init()`:

```java
register(new AutoJump());
```

That's it: it shows up in its category panel, gets a keybind setting, is saved in profiles and is
reachable with `.toggle autojump` / `.set autojump chance 0.5`.

* World shapes: call `Render3D.box(...)`, `Render3D.line(...)` or `Render3D.tracer(...)` in
  `onRender3D`; everything is batched and drawn through walls once per frame.
* Screen-space overlays: use `Projection.toScreen(worldPos)` in `onRender2D`.
* Messages: `ChatUtil.info(...)`, `Notifications.push(title, text, Notifications.Type.INFO)`.
* New setting type: subclass `Setting<T>`, add a widget in `gui/component/setting/` and one line in
  `SettingComponent#create`.
* New command: subclass `Command` and register it in the `CommandManager` constructor.

---

## Suggested future modules

* **Waypoints** - manual waypoints with HUD compass and beams, shared via clipboard.
* **Search** - Storage-ESP style highlighting for any block (ores, spawners, portals).
* **Trajectories** - predicted arc for bows, tridents, pearls and potions.
* **Nametags+** - armour, enchantments and held item on player nametags.
* **Chunk Trails / New Chunks** - flag freshly generated vs. previously visited chunks.
* **Logout Spots** - ghost markers where players disconnected.
* **Minimap / Radar** - top-down entity and terrain radar panel.
* **Fullbright & X-Ray** - lighting and block visibility tweaks.
* **Auto Totem / Offhand** - keep a totem in the offhand.
* **Breadcrumbs** - trail of where you have been.
* **Macro** - bind chat messages or command sequences to keys.
* **Theme editor** - live-edit `Theme` colours from the GUI and save them in profiles.

---

## Version-sensitive spots

Mojang renames rendering and input internals often. When porting, check these first:

* `render/Render3D` - `ShaderProgramKeys`, `Tessellator#begin`, `BufferBuilder#endNullable`,
  `RenderSystem#getModelViewStack` (rendering was reworked in 1.21.5+).
* `mixin/PlayerEntityMixin` - `getEntityInteractionRange` / `getBlockInteractionRange`.
* `mixin/KeyboardInputMixin` - `Input#playerInput`, `movementForward/Sideways`
  (became `movementVector` in 1.21.5).
* `mixin/KeyboardMixin` - `Keyboard#onKey` signature (changed in 1.21.9).
* `RocketScience` - `HudRenderCallback` / `WorldRenderEvents` (replaced by newer Fabric APIs in 1.21.6+),
  `RenderTickCounter#getTickDelta` (renamed `getTickProgress` in 1.21.5).

## Credits

UI font: [Roboto](https://github.com/googlefonts/roboto) (Apache License 2.0, see
`src/main/resources/assets/rocketscience/font/ROBOTO_LICENSE.txt`). Code: MIT, see `LICENSE`.
