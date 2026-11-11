# VISION EDU WALLS 9.0 — 100 PREMIUM 3D WALLPAPERS BUILT IN

SAURABH'S VISION™ • Learn Today, Lead Tomorrow!

## What's new
- **100 additional, individual 3D-style digital wallpaper JPG images are EMBEDDED in the Android APK**, not merely linked externally or imported after installation.
- 10 categories × 10 each: Maths, Physics, Chemistry, Biology, Botany, Space, Technology, Learning, 3D Abstract and Motivation.
- New **100 Built-in 3D Gallery** with categories, previews, working Static Wallpaper, Live Wallpaper (original image with animated overlay) and Digital Widget Background buttons. Save individual JPG to Documents.
- Every image uses a unique name, composition, graphical layout and artwork pairing from original AI artworks made in this conversation. Images are **not** 100 independently generated full-scene AI artworks. They are 100 different illustrated compositions (540×960 pixels), preserving 3D-neon creative style. Not marketed as 4K.
- Images are not copied from Pinterest screenshots.
- Older 66 poster-style static wallpapers, 66 separately drawn Live scenes, 60 functional Android widgets and Classic image ZIP importer are preserved.

## Android architecture
- Source ZIP archived into 8 binary Git blobs under `education-live-wallpaper/premium100-pack/part*.bin`.
- GitHub Actions reassembles binary chunks with SHA-256 verification, checks all 100 JPGs and catalog names, then extracts them into `app/src/main/assets/premium100` **before** Gradle compiles.
- The final **APK carries 100 real JPG asset files**. CI verifies their presence in the APK ZIP and checks its signing identity.
- `Premium100Activity` opens the preinstalled collection with no permission, no downloads and no manual ZIP import.
- Live option copies the selected asset into existing app-private custom art, enables wallpaper touch animation, and uses Android's live wallpaper picker.
- Widget option uses existing `PremiumWidgetArt.choose()`, which updates wallpaper backgrounds for real system TextClock/date/battery/optional Internet weather widgets.
- Wallpapers operate offline, but weather still requires explicitly selected city/optional approximate location and Internet.

## Limitations
3D-style photos are creative educational compositions, not anatomical textbook diagrams. This is a **debug-signed test APK**, not a Google Play release. Device testing of previews, OEM live-wallpaper support, widgets and performance is still required. Debug signature may differ from prior versions and an upgrade might require uninstalling only Vision Edu Walls, resetting its local custom wallpaper settings; the separate Saurabh Vision student app is unaffected.

Release 9.0.0 (versionCode 10). Requires Android 6.0+.
