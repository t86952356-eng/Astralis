# Changelog

## 2.2.1 — Android optimization release

This build removes or safely disables parts that only work on Windows (desktop)
and were useless, heavy, or crash-prone for Android players.
No visual style and no working gameplay feature was changed.

### Fixed

- **Startup crash on Android.** The client forced the GLFW `WIN32` platform
  hint on every system, which breaks window initialization on non-Windows
  runtimes. It now selects the platform that matches the device (Windows,
  macOS, Wayland, X11, or automatic), so Android launchers such as FCL,
  PojavLauncher and Zalith can start the game normally.
- **Dear ImGui startup is now guarded.** On devices without the imgui native
  library (all Android devices) the client logs one warning and keeps running.
  The ClickGUI simply falls back to the built-in "Dropdown" style instead of
  crashing during the Mojang loading screen.

### Removed (desktop-only, no benefit on Android)

- **MCEF in-game Chromium browser** (0.4 MB of nested jar plus a Chromium
  runtime download attempt at startup). The "Browser" buttons were removed
  from the title screen and the pause menu, and the script editor's "Easy
  Mode" web panel now explains that it needs the desktop build.
- **Dear ImGui native jars for Windows, macOS and Linux x86** (~7.2 MB packed
  inside the mod jar). These libraries can never load on Android ARM devices,
  so they were pure dead weight.
- **Dead WMI hardware-fingerprint classes** left in the source by the original
  recovery (processor, BIOS, baseboard and PnP device serial property enums).

### Improved on Android

- The **MiniBlox** button (a local Node.js translation layer whose automatic
  installer is Windows-only) is now hidden outside Windows instead of always
  showing "Failed".
- The **music overlay** no longer tries to spawn `dbus-send` every 500 ms on
  systems without a desktop D-Bus session, which includes Android.
- The **window icon** code is fully fault-tolerant: GLFW builds that lack the
  newer platform-query API can no longer break startup.
- MixinExtras is now bundled as a standalone nested library (it previously
  only shipped inside the removed MCEF jar), keeping all client mixins and
  ViaFabricPlus mixins working.

### Kept intact

- All 112 modules, the Dropdown and Panel ClickGUI styles, TargetHUD, themes,
  fonts, shaders and every rendering effect.
- ViaFabricPlus (joining older and newer protocol servers), Hypixel Mod API,
  Microsoft and Minecraft account login, local configs, scripting and social
  screens.

### Size

- Mod jar: **62.8 MB → 55.4 MB** (7.4 MB / 11.9% smaller).
- Nested libraries inside the mod jar: 17 → 14 jars.
- Removed ~7.5 MB of desktop-only embedded libraries (7.2 MB imgui
  natives + 0.45 MB MCEF) and added back the 0.2 MB standalone MixinExtras.

### Requirements

- Minecraft 26.2, Fabric Loader >= 0.19.3, Fabric API, Java 25.
- Works on desktop too; the only desktop-only features that are gone are the
  in-game CEF browser, the "Imgui" ClickGUI style (falls back to Dropdown) and
  the SMTC music overlay on Windows remains available.
