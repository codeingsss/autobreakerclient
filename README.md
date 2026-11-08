# AutoBreaker

A Fabric client mod for Minecraft with a frosted-glass, rounded-corner click GUI and
one-click presets for squeezing out as much FPS as vanilla's own settings allow.

![license](https://img.shields.io/badge/license-GPL--3.0-blue)

## Features

- **Blurred, rounded click GUI** — opened with `Right Shift`. The background behind the
  panel is blurred using Minecraft's own `DrawContext#applyBlur()` post effect (the same
  mechanism vanilla uses for its pause/inventory screens), and every panel is drawn with
  rounded corners using plain solid fills (no shaders, no textures, no mixins).
- **Performance modules** — toggle vsync, particles, clouds, entity shadows, view
  bobbing, graphics preset and render distance independently, or flip all of them at once
  with **Extreme Performance**.
- **HUD modules** — watermark, FPS counter, coordinates.
- **Config persistence** — module states and slider values are saved to
  `config/autobreakerclient.json` and restored on the next launch.

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
open-source Minecraft client mods, including [LiquidBounce](https://github.com/CCBlueX/LiquidBounce)
(GPL-3.0). No LiquidBounce code is vendored here; the implementation in
`client/gui/RoundedRect.java` and `client/gui/ClickGuiScreen.java` is original. This
project is licensed GPL-3.0 to keep that door open for anyone who does want to pull in
GPL-3.0 code later — see [LICENSE](LICENSE).

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
2. Launch the game, press `Right Shift` in-world to open the AutoBreaker menu.
3. Click a module to toggle it; drag its slider (when present) to adjust a value;
   drag the panel's header to move it.

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

### HUD
| Module | Effect |
|---|---|
| Watermark | Shows an "AutoBreaker" tag, top-left |
| FPS Counter | Shows current FPS |
| Coordinates | Shows your X/Y/Z position |

## License

GPL-3.0-only — see [LICENSE](LICENSE).
