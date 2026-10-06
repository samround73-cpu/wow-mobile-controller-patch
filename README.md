# WoW Mobile — Controller Edition (fork)

A fork of [marcocastignoli/wow-mobile](https://github.com/marcocastignoli/wow-mobile) that adds
**full physical-controller support** (tested with a Bluetooth Xbox Wireless Controller on a
Samsung Galaxy S23 Ultra), fixes camera-related black flicker, and adds quality-of-life options
to the launcher. Everything from the original app still works the same way.

**Download:** [latest APK](https://github.com/samround73-cpu/wow-mobile-controller-patch/releases/tag/latest)
— built automatically from `main` by GitHub Actions.

> **Installing:** this build is signed with a different key from the original, so uninstall the
> original WoW Mobile first. Your WoW folder and server are not touched — just pick the game
> folder again. Updates of this fork install over each other normally.

## What's new in this fork

### Controller support
- **Left stick and D-pad are separate.** Controller stick movement now reaches the input
  bindings first and is always consumed, so Android no longer turns left-stick movement into
  fake D-pad presses (the stick used to fire D-pad bindings).
- **Right-stick camera.** New *Mouse → LOOK LEFT / RIGHT / UP / DOWN* bindings hold the right
  mouse button (WoW mouselook) while the stick is pushed and release it when it centres.
- **Camera spin fix.** The pointer is now clamped to the screen, so the camera no longer
  occasionally whips round when WoW re-centres the cursor.
- **Bindable triggers.** Bluetooth Xbox triggers (analog `BRAKE`/`GAS` / `LTRIGGER`/`RTRIGGER`
  axes) now register as **L2 / R2** and can be bound like any button.
- **Talk to NPCs with the controller.** New *Mouse → INTERACT CENTER* binding right-clicks just
  below the centre of the screen: face a quest giver / corpse / object and press it.
  ConsolePort then drives the quest menus with the D-pad.
- **Camera / cursor toggle.** New *Mouse → LOOK TOGGLE* binding switches the right stick between
  camera mode and plain cursor movement (for menus, map, bags).
- **WoW-friendly defaults** when capturing bindings: left stick → W/A/S/D, right stick → LOOK,
  D-pad → I/J/K/L (ConsolePort's cursor).

### Display
- **Much less black flicker when the mouse/camera moves.** The renderer used to redraw on every
  pointer move (and on every cursor change / cursor-position report), sampling the game's shared
  frame buffer mid-write. Cursor-only redraws are now skipped while the game is presenting frames.

### Launcher
- **Frame rate limit** (*WoW Settings → Frame rate limit*, default **30 fps**). Uncapped frame
  rates let the compositor grab frames before they are finished, which shows as black flicker
  (especially when opening shops or moving the ConsolePort cursor). Applied automatically on
  launch unless you pick another value.
- **Auto login** (*WoW Settings → Auto login*): account, password, wait time and an optional
  "enter the world with my last character" step. The account name is pre-filled in
  `Config.wtf`; once the login screen appears the password is typed and Enter pressed.
  Credentials are stored only in the app's private storage on the device.
- **On-screen controls choice is remembered** between launches.
- **ConsolePort calibration wizard no longer reopens every login.** Starting the wizard resets
  ConsolePort's stick type and button-skip flags, and a clean exit saved that broken state.
  The launcher now re-applies the required settings in `ConsolePort.lua` on every launch while
  keeping all other ConsolePort settings (a backup is kept as `ConsolePort.lua.bak`).

### Recommended Xbox mapping
| Controller | Binding | Set to |
|---|---|---|
| Left stick up / down / left / right | AXIS Y+ / Y- / X- / X+ | Keyboard W / S / A / D |
| Right stick up / down / left / right | AXIS RZ+ / RZ- / Z- / Z+ | Mouse LOOK UP / DOWN / LEFT / RIGHT |
| D-pad up / left / down / right | DPAD | Keyboard I / J / K / L |
| A / B / X / Y | BUTTON A / B / X / Y | Keyboard N / B / H / Y |
| LB / LT | BUTTON L1 / L2 | Keyboard L SHIFT / L CTRL |
| RB / RT | BUTTON R1 / R2 | Keyboard Q / E |
| View / Menu | BUTTON SELECT / START | Keyboard G / V |
| Left stick click | BUTTON THUMBL | Keyboard TAB |
| Right stick click | BUTTON THUMBR | Mouse INTERACT CENTER |

Tip: quit with **Esc → Exit Game** (or `/quit`) so WoW saves addon and key settings.

---

# WoW Mobile

Play **World of Warcraft 3.3.5a (Wrath of the Lich King)** on Android with a
touch UI designed for [ConsolePortLK](https://github.com/leoaviana/ConsolePortLK) —
no manual setup, no fiddling with Wine, bindings or addons.

WoW Mobile is a single-purpose fork of [Winlator](https://github.com/brunodev85/winlator):
it keeps the Wine + Box64 engine and wraps it in a three-screen app.

<img width="1280" height="576" alt="2026-08-13 16 01 33" src="https://github.com/user-attachments/assets/26b5064c-814d-469b-a28b-becea41696cf" />

60fps on Pixel 8

## What it does

1. **Play** — pick the folder that contains `Wow.exe` and press PLAY.
   On first launch the app automatically:
   - installs the bundled ConsolePortLK addons (if not already present)
   - makes ConsolePort keyboard bindings account-wide
   - detects your client locale (`enUS`, `enGB`, …) and sets the realmlist
     (default: `logon.therawow.com`, changeable in WoW Settings)
   - applies a tuned `Config.wtf` (windowed-maximized, 960x432, performance graphics)
   - creates a tuned Winlator container with the game mapped as drive `F:`
   - enables a touch-controls profile whose buttons emit the keystrokes
     ConsolePortLK expects (movement pad, UI-navigation pad, face buttons,
     modifiers, camera toggle, loot, jump, target)
2. **WoW Settings** — switch realm server, resolution and view distance
   directly from the app (edits `Config.wtf` / `realmlist.wtf` while the game is off).
3. **Container Settings** — the full Winlator container editor for the single
   game container, prefilled with sane defaults.

After the very first login the app asks you to log out once — that's when WoW
creates your account folder, and WoW Mobile finishes calibrating ConsolePort
for it automatically. From then on everything just works.

## Install

Grab the APK from [Releases](../../releases) and sideload it.

> **Note:** WoW Mobile uses its own application id (`it.wowmobile`), so it
> installs alongside the original Winlator without touching it. All asset
> archives (rootfs, drivers, box64) are re-patched at build time to match.

You need to provide your own World of Warcraft 3.3.5a (build 12340) client
folder on the phone's storage. No game files are distributed with this app.

## Controls layout

| Touch button | Key sent | ConsolePort action |
|---|---|---|
| Left pad | W/A/S/D | Movement |
| Small upper pad | I/J/K/L | UI cursor (CP_L_*) |
| Y / B / A / X | Y / B / N / H | CP_R_UP / RIGHT / DOWN / LEFT |
| L1 / L2 | Shift / Ctrl | Modifiers (more ability pages) |
| R1 / R2 | Q / E | CP_T1 / CP_T2 |
| BACK / MENU | G / V | CP_X_LEFT / CP_X_RIGHT |
| LOOK (toggle) | Right mouse | Camera |
| LCLICK / RCLICK | Left / right mouse | Cursor clicks |
| M | M | Map |
| JUMP / TGT | Space / Tab | Jump / target enemy |

## Building

```
./gradlew assembleRelease
```

Requires JDK 17, Android SDK (platform 34), NDK 24.0.8215888 and CMake 3.22.1.
Release signing reads `~/.wow-mobile-secrets/keystore.properties`
(`storeFile`/`storePassword`/`keyAlias`/`keyPassword`); without it the release
build is unsigned.

## Credits & license

- [Winlator](https://github.com/brunodev85/winlator) by brunodev85 — the entire
  Windows-on-Android engine this app is built on. LGPL-2.1, same as this fork.
- [ConsolePortLK](https://github.com/leoaviana/ConsolePortLK) by leoaviana —
  controller UI for WoW 3.3.5a (bundled unmodified).
- World of Warcraft is a trademark of Blizzard Entertainment. This project is
  not affiliated with or endorsed by Blizzard. Bring your own client files.
