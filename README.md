# Lumen

**A liquid-glass notebook for Android — write, draw, and shape your ideas on a freeform canvas.**

<p align="center">
  <!-- Add your screenshots here, e.g.:
  <img src="screenshots/home.png" width="270" />
  <img src="screenshots/editor.png" width="270" />
  -->
</p>

## ✨ Features

### Freeform canvas editor
- **Draw** with smooth quadratic ink — preset palette plus a custom picker (hue ring + lightness slider)
- **Write anywhere** — tap any spot to place text directly on the page
- **Lasso select** — circle anything to move, copy, cut, or delete it in one gesture
- **Eraser** — sweep over strokes and text to remove them with live preview
- **Pinch to zoom & pan**, double-tap to reset the view
- **Undo / redo** with atomic action groups

### Note management
- Auto-save with thumbnails — everything persists locally (Room database)
- **Folders & tags** to organize your notes
- **Trash** with restore and permanent delete
- Pin important notes to the top
- Search notes by title

### Liquid glass design
- Translucent glass UI with real refraction, blur, and vibrancy
- **Liquid pinch-off animation** when tool panels appear
- Per-note paper colors — light & dark palettes, plus a custom color wheel
- Adaptive ink — text and strokes stay readable on any paper color
- Dark mode, plus **pure white** and **pure black (OLED)** themes

### Extras
- Share any note as a PNG image
- Export all notes at once
- Editor paper color matches the home card automatically

## 🛠️ Built with
- **Kotlin** · **Jetpack Compose** · Material 3
- **Room** database · DataStore preferences
- Custom canvas engine — quadratic-smoothed strokes, viewport culling, incremental path caching, atomic saves
- Liquid glass rendering powered by [Kyant0's backdrop library](https://github.com/Kyant0/AndroidLiquidGlass)
- Hand-rolled metaball "pinch-off" animation (no animation libraries)

## 📲 Requirements
- Android 13 (API 33) or newer
- arm64-v8a devices

## 🚀 Building
1. Clone or download this repository
2. Open the project in **Android Studio** and let Gradle sync
3. Run the app on a device or emulator with API 33+

Or from the command line:

```bash
./gradlew assembleDebug
```

The APK is generated in `app/build/outputs/apk/debug/`.

> **Note:** `local.properties` (SDK path) is not included — Android Studio generates it automatically on sync.

---

Made with Opencode - Oxalpha 
