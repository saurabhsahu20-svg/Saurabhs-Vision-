# S VISION Launcher 1.0 — Maths & Science Android Home

**By SAURABH'S VISION™**

A real, minimal, privacy-first Android **home screen launcher**, with offline educational space/math/science themes and functional installed-app navigation. Built in native Java 17 and Android SDK 35. No Kotlin, external libraries, login, network code, or ads.

## What works in v1.0

- A real Android HOME application (manifest handles `ACTION_MAIN`, `CATEGORY_HOME` and `CATEGORY_DEFAULT`). Also has a normal icon so users can try it before setting it as their default.
- First-party **Science & Maths Canvas wallpaper** with 4 user-selectable offline styles: Cosmic Mathematics, Atomic Science, DNA Galaxy, Golden Equations.
- Dynamic clock/date and battery percentage (real system values, no invented weather).
- Discovers installed **launchable** apps via Android's scoped `<queries>` intent; `QUERY_ALL_PACKAGES` is deliberately not requested.
- Working 4-column app drawer and case-insensitive search by app name/package.
- Tap an app to open its actual launcher activity; long-press to add/remove Favorites. Eight launchable apps appear on Home even before any favorites are added.
- Real, user-triggered links to Android dialer, Messages, Browser, app drawer and theme picker; system handles these with installed apps.
- Two **Maths Lab** and **Science Lab** cards containing simple, accurate educational facts; tapping opens the content.
- Settings provide working theme switching, installed-app refresh, Android system settings, and default Home app selection via `RoleManager.ROLE_HOME` (Android 10+) or Android Home Settings fallback.
- Preferences (themes/favorites) stay on-device. **No Internet permission, no account or analytics, no battery-draining wallpaper animation.**

## What is NOT in the initial version

The promotional design concept is **not** an exact UI screenshot. Version 1.0 does **not** include system-wide live wallpapers, app hiding/locking, third-party widget hosting, custom icon packs, fingerprint authentication, weather, animation effects, matrix solvers or other advanced paid-launcher features. These would require separate code and appropriate Android integration.

## Build

Requires JDK 17 and Android SDK 35.

```bash
cd science-maths-launcher
gradle --no-daemon :app:assembleDebug
```

The debug-signed APK appears at `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions also produces a downloadable public **GitHub Release** tagged `s-vision-launcher-v1.0.0`, validates launcher HOME manifest filters and APK signature, and runs pure Java tests.

## Set as Default Launcher

1. Install the APK and open **S VISION Launcher**.
2. Tap ⚙ **Settings → Set as Default Home Launcher**.
3. Select **S VISION Launcher** in Android's system approval screen (or Settings → Default apps → Home app).
4. To go back to your previous launcher, select it from the same Android Home-app settings. **Never factory-reset your phone or disable security settings to switch launchers.**

## Important

This is an initial test APK, not a Google Play Store production-signed app. Real-device launcher behavior varies by Android/phone brand; test on your phone before making it the default. If Home role selection is unavailable, Android may restrict the choice. App drawer shows only apps visible through permitted launcher queries. Android may restrict direct access to certain system or work-profile apps.
