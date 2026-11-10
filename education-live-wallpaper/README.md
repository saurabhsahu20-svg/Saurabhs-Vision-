# SAURABH'S VISION™ — Vision Edu Walls

**Education-themed Android Live Wallpaper App v1.0.0 (test release)**

A fully native Android `WallpaperService` app built with Java + Android SDK. The live wallpaper draws all artwork with Android Canvas (no network, no ads, no third-party SDK). 

## Features
- Maths Galaxy: formulas, glowing open book, gold particles.
- Science Lab: electrons, animated atom, floating physics/chemistry symbols.
- Cosmic Classroom: spinning planet rings, moons, stars and educational notation.
- Junior Learning: beginner numbers, letters, colorful book and stars.
- Live preview in-app, apply wallpaper with Android's system preview.
- Adjustable slow/normal/fast motion, touch glows, immediate theme updates on existing wallpaper.
- Canvas animations pause when wallpaper isn't visible. Storage limited to anonymous local theme settings.

## Build
1. Install Java 17, Android SDK (API 35), and Gradle 8.9.
2. From this directory run `gradle :app:assembleDebug`.
3. Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
4. Or open GitHub Actions > **Build Education Live Wallpaper APK** for a downloadable artifact or test Release.

## Installation
Install APK on Android 6.0+, open **Vision Edu Walls**, select your wallpaper, then tap **Set as Live Wallpaper** and confirm from Android's native wallpaper picker. Some device manufacturers show their own wallpaper chooser.

## Important
The test APK is signed with a CI-generated debug signing identity and is for evaluation. An APK from a later CI run may use a different key and fail to update it in place. Before production publishing, configure a **private and consistent** release signing key in CI secrets, and conduct actual-device battery and performance testing. Never upload signing keys into this public repository.

This is an independent educational design tool, not an official CBSE/CGBSE/ICSE app. All visual elements are drawn from code.
