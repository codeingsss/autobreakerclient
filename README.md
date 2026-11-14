# AutoBreaker

A Fabric client mod for Minecraft with a frosted-glass, rounded-corner click GUI and
one-click presets for squeezing out as much FPS as vanilla's own settings allow.

![license](https://img.shields.io/badge/license-GPL--3.0-blue)

## Features

- **Fixed, centered click GUI** — opened with `Right Shift`: a rectangular window with a
  left-hand category sidebar (tabs, not a single scrolling list) instead of a floating
  draggable box. The background is blurred using Minecraft's own `DrawContext#applyBlur()`
  post effect (the same mechanism vanilla uses for its pause/inventory screens), every
  panel is drawn with rounded corners using plain solid fills (no shaders, no textures, no
  mixins), and the signature accent is orange throughout.
- **Performance Governor** — a live FPS-tracking loop, not a one-shot preset: nudges
  render distance (and, if that's not enough, particles) up or down roughly once a
  second to hold a target FPS, so it keeps adapting as the scene changes instead of
  staying fixed at whatever the preset picked at login.
- **Learned per-server tuning** — leave Performance Governor on for a session and
  AutoBreaker remembers the render distance it settled on for that server address; next
  time you join, that value is applied immediately and the Governor picks up refining it
  from there. A packed lobby and a quiet SMP end up with different numbers, automatically.
- **Performance modules** — toggle vsync, particles, clouds, entity shadows, view
  bobbing, graphics preset and render distance independently, or flip all of them at once
  with **Extreme Performance**.
- **HUD modules** — watermark, FPS counter, coordinates, ping, CPS, clock, armor HUD,
  server address, and a live ping-stability graph — each independently draggable via the
  in-game **HUD editor** (`HUD` tab → `Edit HUD Layout...`).
- **One-string config sharing** — `/autobreaker config export` turns your whole setup
  (every module's state, settings and HUD layout) into a single compact code; anyone can
  `/autobreaker config import <code>` it back. No account, no backend, just a string.
- **Config persistence** — module states, slider values and HUD element positions are
  saved to `config/autobreakerclient.json` and restored on the next launch.

## What this is *not*

AutoBreaker only flips switches that already exist in vanilla Minecraft
(`GameOptions`/`SimpleOption`), exposed through a nicer GUI. It does not reimplement
Sodium/Lithium-style chunk rendering, and it does not contain code taken from any
closed-source client — doing so would be copyright infringement. For real engine-level
rendering gains, install this alongside:

- [Sodium](https://modrinth.com/mod/sodium) — rendering engine rewrite
- [Lithium](https://modrinth.com/mod/lithium) — general game-logic optimizations
- [Iris](https://modrinth.com/mod/iris) — shader support that stays compatible with Sodium

How high your FPS actually goes depends on your hardware and the scene (render
distance, entity count, shaders) — "2000 FPS" is realistic in a simple scene on a
capable GPU with vsync off and render distance turned down, not a number this mod can
force on its own.

`client/gui/RoundedRect.java` and `client/gui/ClickGuiScreen.java` are an original,
zero-extra-dependency GUI: rounded corners are plain circle-equation fills (no shader, no
texture, no native rendering library), and the background blur comes straight from
`DrawContext#applyBlur()` — see [LICENSE](LICENSE).

## Requirements

| | |
|---|---|
| Minecraft | 1.21.11 |
| Fabric Loader | ≥ 0.20.1 |
| Fabric API | 0.141.6+1.21.11 |
| [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin) | required — the OneConfig config page is written in Kotlin |
| [OneConfig](https://modrinth.com/mod/oneconfig) | required — every module is exposed on a real OneConfig settings page instead of (or in addition to) the in-game click GUI |
| [Compose Multiplatform](https://modrinth.com/mod/compose-multiplatform) | required — OneConfig's own rendering dependency |
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

1. Drop the built jar into your `mods` folder alongside Fabric API, Fabric Language
   Kotlin, Compose Multiplatform and OneConfig (see [Requirements](#requirements)).
2. Launch the game, press `Right Shift` in-world to open the AutoBreaker menu — a fixed,
   centered window with category tabs down the left side (Performance / Quality of Life
   / Visual / HUD). This is the primary, fully-themed way to use AutoBreaker; every
   module is also exposed on its own OneConfig page for convenience, but OneConfig's own
   UI chrome is Polyfrost's, not ours, so it won't pick up the orange theme the same way.
3. Click a module to toggle it; drag its slider (when present) to adjust a value;
   scroll if a tab's list doesn't fit.
4. On the HUD tab, click `Edit HUD Layout...` to drag each HUD element (enabled or not)
   to wherever you want it on screen. Press Escape to save and close the editor.
5. Run `/autobreaker profile set <name>` while connected to a server to have
   AutoBreaker switch to that OneConfig profile automatically every time you join that
   address again (`/autobreaker profile clear` / `list` manage the mapping).
6. Run `/autobreaker config export` to get a share code for your whole setup, and
   `/autobreaker config import <code>` to apply someone else's.
7. Enable **Performance Governor** and just keep playing — `/autobreaker tune list`
   shows what it's learned per server so far, `/autobreaker tune forget` resets one.

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
| Performance Governor | Live-adjusts render distance/particles once a second to hold a target FPS (settings: target FPS, min/max render distance) |

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

Motion Blur bakes one fixed blend amount into a post-effect pipeline
(`assets/autobreakerclient/post_effect/motion_blur.json`), needing only
`MinecraftClient#getShaderLoader()` and `PostEffectProcessor#render`, both public.
`MotionBlurModule` owns a `SimpleFramebuffer` history buffer and imports it into the
post-effect's frame graph as an external target (`autobreakerclient:previous`) alongside
vanilla's own `minecraft:main`, the same way `WorldRenderer` imports its own long-lived
entity-outline framebuffer for its outline post-effect, and explicitly re-captures it
every frame rather than trusting the processor's own internal "accum" target to persist
pixel contents between frames. The one mixin this project has injects at the tail of
`GameRenderer#render` — there's no public "frame fully rendered" event — and everything
it touches (the class, the `render` method, the shadowed `pool` field) resolved cleanly
against this project's Yarn mappings during the build.

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
| Network Doctor | Ping as a color-coded bar graph plus an average/jitter verdict ("Good, Stable" vs. a steady-but-high ping vs. one that's swinging wildly), not just a single rolling number |

CPS avoids mixins (see [the "What this is not" section](#what-this-is-not)) and instead
chains onto `GLFW.glfwSetMouseButtonCallback` directly — it captures whatever callback
Minecraft's own `Mouse` class already installed and always re-invokes it after recording
a click, so vanilla input isn't affected. An earlier version polled the button state once
per client tick (20 Hz), which silently missed fast clicks (drag-clicking,
butterfly-clicking) whose entire press-release cycle can be shorter than one tick.

## License

GPL-3.0-only — see [LICENSE](LICENSE).
