# VISION EDU WALLS 2.0 — SAURABH'S VISION™

Premium educational **Android Live Wallpaper** app, created as a separate application from the Saurabh’s Vision learning/quiz app.

## Interactive V2 features
- **Maths Galaxy:** living open book, glowing formula particles, animated sine-wave graph.
- **Science Lab:** moving electrons, atom orbit rings and animated DNA-style double helix.
- **Cosmic Classroom:** planet rings, orbiting moon, aurora and a repeating comet.
- **Junior Learning:** counting bubbles, glowing book, alphabet and numbers.
- **Touch Magic:** touch-triggered star bursts and short educational fact labels.
- **Gestures:** double-tap or horizontally swipe in the app preview (and system wallpaper where device launcher forwards touch events) to switch the four themes.
- **Parallax:** depth movement with launcher page offsets.
- **Dynamic controls:** adjustable animation speed and on/off touch effects.
- **Power-aware:** lowers frame rate in Battery Saver and stops drawing when wallpaper is hidden.
- No ads, internet access, login or personal data collection.

## Get an APK
GitHub Release: https://github.com/saurabhsahu20-svg/Saurabhs-Vision-/releases/tag/vision-edu-walls-v2.0.0

### Compatibility and signing
Android 6.0+. v2 uses the **same application ID** `com.saurabhsvision.eduwalls` with `versionCode 2`, but GitHub CI auto-creates a **new debug signing key** per runner. If Android reports **App not installed / conflicting package**, the new APK cannot be installed over v1 due to a different signing certificate. You may uninstall the old Vision Edu Walls (this will reset local wallpaper theme/speed settings) and install v2. The original **Saurabh’s Vision Quiz Club app** is a different package and is not affected. Do NOT upload signing keys to the public repo. For future upgrade-safe releases, configure persistent private release signing using GitHub Actions encrypted secrets, and verify device-level update compatibility.

## Build yourself
- Java 17, Gradle 8.9, Android SDK Platform 35.
- `cd education-live-wallpaper && gradle :app:assembleDebug`.
- Test build signature and Android installation on actual devices before Play Store distribution.

## Legal
Not affiliated with CBSE, CGBSE or ICSE. Original educational artwork is generated procedurally using Android Canvas (no copyrighted media).