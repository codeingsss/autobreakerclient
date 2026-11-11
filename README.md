# AutoBreaker

A Fabric client mod for Minecraft with a frosted-glass, rounded-corner click GUI and
one-click presets for squeezing out as much FPS as vanilla's own settings allow.

![license](https://img.shields.io/badge/license-GPL--3.0-blue)

## Features

- **Fixed, centered click GUI** — opened with `Right Shift`, styled after Lunar Client's
  mod menu: a rectangular window with a left-hand category sidebar (tabs, not a single
  scrolling list) instead of a floating draggable box. The background is blurred using
  Minecraft's own `DrawContext#applyBlur()` post effect (the same mechanism vanilla uses
  for its pause/inventory screens), and every panel is drawn with rounded corners using
  plain solid fills (no shaders, no textures, no mixins).
- **Performance modules** — toggle vsync, particles, clouds, entity shadows, view
  bobbing, graphics preset and render distance independently, or flip all of them at once
  with **Extreme Performance**.
- **HUD modules** — watermark, FPS counter, coordinates, ping, CPS, clock, armor HUD,
  server address — each independently draggable via the in-game **HUD editor**
  (`HUD` tab → `Edit HUD Layout...`).
- **Config persistence** — module states, slider values and HUD element positions are
  saved to `config/autobreakerclient.json` and restored on the next launch.

## What this is *not*

AutoBreaker only flips switches that already exist in vanilla Minecraft
(`GameOptions`/`SimpleOption`), exposed through a nicer GUI. It does not reimplement
Sodium/Lithium-style chunk rendering, and it does not contain code taken from
closed-source clients (Lunar Client, Badlion, etc.) — doing so would be copyright
infringement. For real engine-level rendering gains, install this alongside:

- [Sodium](https://modrinth.com/mod/sodium) — rendering engine rewrite
- [Lithium](https://modrinth.com/mod/lithium) — general game-logic optimizations
- [Iris](https://modrinth.com/mod/iris) — shader support that stays compatible with Sodium

How high your FPS actually goes depends on your hardware and the scene (render
distance, entity count, shaders) — "2000 FPS" is realistic in a simple scene on a
capable GPU with vsync off and render distance turned down, not a number this mod can
force on its own.

The GUI's visual language (dark, blurred, rounded panels) is a common style across
open-source Minecraft client mods. We looked at pulling actual code from two of them
before writing our own:

- **[LiquidBounce](https://github.com/CCBlueX/LiquidBounce)** (GPL-3.0) — the `legacy`
  branch's ClickGUI is immediate-mode OpenGL 1.x from the 1.8.x era and doesn't target
  a rendering pipeline this game version still has; the `nextgen` branch's ClickGUI is
  a Svelte web page rendered through an embedded browser, which is the opposite of what
  an FPS-focused client wants open on top of the game.
- **[Meteor Client](https://github.com/MeteorDevelopment/meteor-client)** (GPL-3.0) —
  draws its rounded panels with NanoVG, which gives properly anti-aliased corners but
  pulls in a native LWJGL binding as a dependency.

Neither was a drop-in fit for a zero-extra-dependency, blur-via-vanilla-API GUI, so
`client/gui/RoundedRect.java` and `client/gui/ClickGuiScreen.java` here are original,
built directly on `DrawContext#applyBlur()` and plain solid fills. This project stays
GPL-3.0 regardless, so that door is open if someone wants to pull in GPL-3.0 code (e.g.
a NanoVG-based theme) later — see [LICENSE](LICENSE).

## Requirements

| | |
|---|---|
| Minecraft | 1.21.11 |
| Fabric Loader | ≥ 0.20.1 |
| Fabric API | 0.141.6+1.21.11 |
| JDK (build-time) | 25+ (Fabric Loom's current snapshot targeting 1.21.11 requires it; the compiled mod itself targets Java 21 bytecode) |

## Building

```sh
./gradlew build
```

The mod jar is written to `build/libs/autobreakerclient-<version>.jar`.

If the build fails with a message about Gradle/JVM version requirements, point
`JAVA_HOME` at a JDK 25+ install before running the wrapper — Fabric Loom is tracking
Minecraft 1.21.11 through a snapshot build, and snapshot versions occasionally bump
their own JVM requirement.

## Usage

1. Drop the built jar into your `mods` folder alongside Fabric API.
2. Launch the game, press `Right Shift` in-world to open the AutoBreaker menu — it's a
   fixed, centered window with category tabs down the left side (Performance / Quality
   of Life / HUD).
3. Click a module to toggle it; drag its slider (when present) to adjust a value;
   scroll if a tab's list doesn't fit.
4. On the HUD tab, click `Edit HUD Layout...` to drag each HUD element (enabled or not)
   to wherever you want it on screen. Press Escape to save and close the editor.

## Module list

### Performance
| Module | Effect |
|---|---|
| Extreme Performance | Bundles every toggle below plus a render-distance slider (2–32) into one switch |
| Fast Graphics | Forces the vanilla "Fast" graphics preset |
| No Vsync | Disables vertical sync |
| No Clouds | Turns off cloud rendering |
| Minimal Particles | Forces the lowest vanilla particle setting |
| No Entity Shadows | Disables the blob shadow under entities |
| No View Bobbing | Disables the camera bob while walking |

### Quality of Life
| Module | Effect |
|---|---|
| Zoom | Hold `C` to zoom in (lowers FOV and sensitivity) |
| Full Bright | Maxes out the brightness setting |
| Auto GG | Sends "gg" in chat when the server announces a round has ended |

### Visual
| Module | Effect |
|---|---|
| Motion Blur | Blends recent frames together for a trailing blur on fast camera movement |

Motion Blur started as a port of the open-source
[motionblur-fabric-1.21.11](https://github.com/7avyy/motionblur-fabric-1.21.11) (GPL-3.0,
originally by wavy/Noryea), reimplemented through public API only: the original reads
its blend amount through three `@Accessor` mixins and a reflective `createBuffer` call
so the strength can be a live slider; this version bakes one fixed blend amount into the
post-effect pipeline (`assets/autobreakerclient/post_effect/motion_blur.json`) instead,
needing only `MinecraftClient#getShaderLoader()` and `PostEffectProcessor#render`, both
public. The first version of that pipeline trusted the processor's own internal "accum"
target to keep its pixel contents between separate `render()` calls across frames — the
one assumption in this project that couldn't be checked against the Yarn-mapped classes,
since it's runtime behaviour, not a method signature. Comparing against
[Polyfrost/PolyBlur](https://github.com/Polyfrost/PolyBlur) (GPL-3.0) — a much more
mature motion blur mod for the same ecosystem — showed that assumption doesn't hold:
PolyBlur never relies on it either, and keeps its own history framebuffer that it
explicitly re-captures every frame. This does the same now: `MotionBlurModule` owns a
`SimpleFramebuffer` and imports it into the post-effect's frame graph as an external
target (`autobreakerclient:previous`) alongside vanilla's own `minecraft:main`, the same
way `WorldRenderer` imports its own long-lived entity-outline framebuffer for its outline
post-effect. The one mixin this project has injects at the tail of `GameRenderer#render`
— there's no public "frame fully rendered" event — and everything it touches (the class,
the `render` method, the shadowed `pool` field) resolved cleanly against this project's
Yarn mappings during the build.

### HUD
| Module | Effect |
|---|---|
| Watermark | Shows an "AutoBreaker" tag, top-left |
| FPS Counter | Shows current FPS |
| Coordinates | Shows your X/Y/Z position |
| Ping | Shows your connection latency |
| CPS | Shows left/right clicks-per-second |
| Clock | Shows the current system time |
| Armor HUD | Shows equipped armor with durability |
| Server Address | Shows the server you're connected to |

The Zoom, Full Bright, Auto GG, Ping, CPS, Clock, Armor HUD and Server Address modules are adapted
from [CoralMod](https://github.com/jirmjahu/coralmod) (CC0-1.0 — public domain),
remapped from Mojang's official mappings onto this project's Yarn mappings (e.g.
`Options#fov()` → `GameOptions#getFov()`). CoralMod counts clicks through a custom
mixin-based event; this project avoids mixins (see
[the "What this is not" section](#what-this-is-not)) and instead chains onto
`GLFW.glfwSetMouseButtonCallback` directly — it captures whatever callback Minecraft's
own `Mouse` class already installed and always re-invokes it after recording a click, so
vanilla input isn't affected. An earlier version polled the button state once per client
tick (20 Hz), which silently missed fast clicks (drag-clicking, butterfly-clicking)
whose entire press-release cycle can be shorter than one tick.

## License

GPL-3.0-only — see [LICENSE](LICENSE).
