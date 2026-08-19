# HardKeyPointer for Android

[Japanese version](README.md)

[Privacy Policy (Japanese)](PRIVACY_POLICY.md)

![Android 7.0 or later](https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

HardKeyPointer displays a pointer overlay on Android and lets you move, click, scroll, and zoom using physical keys. It is designed for feature-phone-style Android devices, hardware-keyboard devices, and situations where touch input is difficult to use.

## Features

- Move an on-screen pointer with directional or other hardware keys
- Continuous movement and acceleration while a movement key is held
- Short-press and long-press pointer clicks
- Four-direction scrolling by repeating short gestures while a key is held
- Two-finger pinch-in and pinch-out zoom gestures
- Direction correction after screen rotation
- Assign every operation to keys available on your device
- Choose immediate or long-press activation independently for each operation
- Switch between pixel and screen-ratio coordinate modes
- Store every numeric setting independently for each mode
- Show or hide the pointer with a dedicated key
- Automatic settings persistence and background operation
- Reset all settings to their defaults with one button and a confirmation dialog

## Default key bindings

| Action | Default key |
| --- | --- |
| Move the pointer up, down, left, or right | D-pad ↑ ↓ ← → |
| Tap / click | Enter |
| Show / hide pointer | Volume Down |
| Scroll up, left, down, or right | Number keys 2, 4, 5, 6 |
| Zoom in / zoom out | Number keys 9 / 7 |

The defaults are based on the key layout of the Mode 1 Retro II. All bindings can be changed in the settings screen. Long-press a key-binding button to clear that assignment.

## Requirements

- Android 7.0 (API 24) or later
- A device with physical keys or a hardware keyboard
- The ability to enable the HardKeyPointer accessibility service

The app uses Android's accessibility service to receive filtered key events and dispatch on-screen gestures. Root access and the Internet permission are not required. Depending on the device manufacturer or the target app, key filtering, scrolling, or zooming may be restricted.

## Installation and first-time setup

1. Install the APK on the device.
2. Open **Settings → Accessibility → HardKeyPointer** and enable the service.
3. Launch HardKeyPointer and customize the key bindings if needed.
4. With the default configuration, press Volume Down to show or hide the pointer.

When the service is disabled, the app's status card includes a shortcut to the accessibility settings.

## Usage

### Pointer and clicking

- Press the toggle key to show or hide the pointer.
- Hold a movement key to move the pointer continuously. Acceleration increases the movement amount over time when enabled.
- A short press of the tap key performs a click. The configured press duration is preserved for longer presses.
- Use the “Trigger mode” control below each key binding to choose whether that operation starts immediately or after Android's standard long-press timeout.

### Scrolling

Hold a scroll key to repeatedly send short swipes in that direction. Scroll distance and scroll speed can be adjusted in the settings screen. Some apps do not accept accessibility gestures, so scrolling may not work everywhere.

### Zooming

Press a zoom-in or zoom-out key to send a two-finger pinch centered on the pointer. While the key is held, the pinch repeats continuously using the configured gesture duration and stops when the key is released. Zoom amount and gesture duration are configurable. Apps that do not support pinch zoom will not respond.

## Coordinate modes

Use the mode selector at the top of the settings section to choose how distances are interpreted. Numeric values are stored separately for each mode, so switching modes never overwrites the other mode's configuration.

| Mode | Applies to | Reference |
| --- | --- | --- |
| **Pixels** | Movement speed, scroll distance, and zoom amount | Physical screen pixels. The apparent size changes with screen resolution. |
| **Screen ratio** | Movement speed, scroll distance, and zoom amount | A percentage of the screen. Horizontal movement and scrolling use the screen width, vertical movement and scrolling use the screen height, and zoom uses the shorter screen edge. |

The other numeric settings are also independent between modes:

- Movement speed: px/frame in pixel mode, %/second in screen-ratio mode
- Acceleration: 0–500%
- Scroll speed: level 1–10
- Zoom speed: gesture duration from 100 to 1000 ms

Use screen-ratio mode for a similar feel across devices with different resolutions or screen sizes. Use pixel mode when you need fixed, pixel-level control.

Use the “Reset all settings” button in the settings section to restore every setting, including key bindings, to its initial state. A confirmation dialog is shown before anything is changed.

## Building

### Prerequisites

- The latest stable Android Studio, or an Android SDK installation
- Android SDK Platform 35
- JDK 17

### Debug APK

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

### Release APK

Release builds enable R8 shrinking and resource shrinking. Pass the signing values as Gradle properties to produce a signed APK:

```bash
./gradlew assembleRelease \
  -PRELEASE_STORE_FILE=/path/to/release.jks \
  -PRELEASE_STORE_PASSWORD='***' \
  -PRELEASE_KEY_ALIAS='hardkeypointer' \
  -PRELEASE_KEY_PASSWORD='***'
```

Without signing properties, the unsigned APK is written to `app/build/outputs/apk/release/app-release-unsigned.apk`.

## Tests

```bash
./gradlew testDebugUnitTest lintDebug
./gradlew connectedDebugAndroidTest
```

`connectedDebugAndroidTest` requires an Android device connected through USB or ADB.

## Project structure

- `app/src/main/java/.../MainActivity.kt`: settings screen and key assignment
- `app/src/main/java/.../TapService.kt`: accessibility service and key-event handling
- `app/src/main/java/.../PointerMovementController.kt`: pointer movement and acceleration
- `app/src/main/java/.../GestureController.kt`: tap, scroll, and pinch gestures
- `app/src/main/java/.../SettingsRepository.kt`: persistent, mode-specific settings

## Demo

[Watch the demo video](https://github.com/user-attachments/assets/4d60c6fc-2446-4d48-9427-d80d26312bea)

## License

MIT License. See [LICENSE](LICENSE) for the full text.

Copyright (c) 2024-2026 nnnnnnn0090

## Contributing and support

Bug reports, feature requests, and pull requests are welcome through GitHub Issues and Pull Requests. Reproduction steps, device model, Android version, and relevant settings make issues easier to investigate.
