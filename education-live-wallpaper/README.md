# VISION EDU WALLS 5.0 • 150-Design Collection

This is an expanded, original offline Android wallpapers and widget application by SAURABH'S VISION™.

## Included in this APK
- **50 static wallpaper presets**: five educational groups × ten uniquely named, tinted and arranged poster-style compositions. Can be applied to Android Home Screen or exported as individual 720×1280 PNG images.
- **50 live wallpaper presets**: same curated 50 artworks plus five categories of moving scenes (orbit sparks, sine waves, stardust, geometric pulses and comets). Tap to trigger particles; double-tap and swipe cycle scenes on launchers that forward touch input.
- **50 widget styles**: ten clock faces, ten dates, ten Maths facts, ten Science facts, ten study motivation widgets. Android home screen widgets are real `AppWidgetProvider` instances with live `TextClock`, date updates and tap-to-cycle content. One widget provider exposes 50 selectable styles when adding/configuring widgets.
- **Preserves the 8 original v4 posters**, four original Canvas animation themes, and the "Choose HD Image from Gallery" custom photo mode under "Classic Wallpapers".
- **Offline** (no Internet permission, no accounts, no ads). Local device settings only.

## Important about the old HD image packs
Previously generated full-resolution HD artwork packs are available as downloadable ZIP files in the ChatGPT conversation. Those photographic/AI-generated original JPG assets are **not embedded into this APK**; v5 ships 50 self-contained procedural poster-style compositions instead. Users can import the separately supplied 50-HD-Wallpapers.zip archive **once from the Import HD ZIP button**, browse the HD image gallery inside the app, and apply any imported image as a static wallpaper or as an animated live wallpaper through the v4 custom-photo renderer. Image import and storage are strictly local. This distinction prevents claims that the 10 JPGs are included when they are not.

## Widgets usage
Open app > Widgets tab > select a style > Add Selected Widget, or long-press Android Home Screen > Widgets > Vision Edu Walls > pick one of 50 designs. Clock widgets update automatically via the system `TextClock`; the other types update per WidgetProvider and by tap. Some launchers do not support programmatic pinning.

## Installation caveat
Package: `com.saurabhsvision.eduwalls`. Android 6.0+. This is an automatically debug-signed **test** APK: the signature may not match prior v4 versions, preventing upgrade-in-place. Only uninstall the older Vision Edu Walls app if comfortable losing its app-specific wallpaper settings; the separate Saurabh Vision educational Quiz Club app is independent.

## Build & quality
Java 17, Android Gradle Plugin 8.7.3, Gradle 8.9 and SDK 35. GitHub Actions compiles, runs APK zip integrity and signing checks, then publishes the GitHub v5 test Release. Device-level installation, widget-pinning support, memory usage and battery consumption require physical-device testing.
