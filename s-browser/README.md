# S Browser — Android Browser + Offline Dictionary

**App name:** S Browser  
**Brand:** SAURABH'S VISION™  
**Version:** 1.0.0 Beta  
**Platform:** Android 8.0+ (optimized for Android 14)  
**Source:** Plain Android Java + system Chromium WebView; no additional runtime dependencies.

## Working version 1 features

- Clean dark navy/cyan native browser UI and user-approved S Browser launcher icon.
- HTTPS-first browsing using Android System WebView with Android Safe Browsing enabled.
- URL bar with Google, DuckDuckGo, and Bing search engines.
- Multiple tabs (up to 8), tab switch and close, forward page reloading and back navigation.
- Home with study shortcuts: Wikipedia, NCERT, Khan Academy, DIKSHA and Google Scholar.
- Local bookmarks, local history (up to 250 URLs), bookmarked page management.
- Find-on-page, share, file selection/upload, full-screen video view, mobile/desktop UA toggle.
- Download manager saving into the app's Downloads directory. Check Android notification for completion.
- **Inbuilt dictionary:** 260 curated common school English words with English definitions, Hindi meanings, 90ms responsive search and Android offline TTS pronunciation (voice support depends on device installation).
- Selected webpage text can be sent to dictionary via the bottom Dictionary action.
- One-tap clear of browsing cookies, cached website data and saved history (bookmarks kept).
- Android app launches HTTPS links from other apps when selected as a handler.

## Privacy defaults

- Enforces HTTPS for web content in this first beta; typed `http://` URLs are upgraded to `https://`.
- SSL errors are cancelled, never bypassed.
- Android WebView Safe Browsing enabled, third-party cookies disabled.
- JavaScript enabled for modern sites, but unrestricted file URL access and mixed content are disabled.
- Camera and microphone website permission requests denied in this first version.
- No app-added analytics, ads, or account upload; browsing traffic still goes to the visited websites and chosen search provider.
- Local bookmarks/history remain on the phone until user clears them or uninstalls the app.
- The dictionary meanings are bundled; dictionary searches do not need internet. TTS speech may require an installed voice package.

## Not implemented in the first beta

Do **not** claim full private/incognito isolation, built-in AI assistant, extensions, online translation, universal ad blocking, cross-device sync, a full English dictionary, real-time anti-malware or full parental controls. This is an initial functional browser with a compact offline **study glossary**, not a replacement for all features of Chrome or Firefox.

## Build

In this repository: 

1. Open GitHub Actions → **Build S Browser Android APK**.
2. Select latest **green ✅** workflow run.
3. Download **S-Browser-v1.0.0-beta-APK**, or open the published GitHub Release `s-browser-v1.0.0-beta` after success.
4. Extract the artifact ZIP if using Actions, then install `S_Browser_v1.0.0_BETA.apk` on Android.

To build locally: use JDK 17, Android SDK 35, Gradle 8.9, and run `gradle :app:assembleDebug` from the `s-browser` folder.

GitHub Actions must pass syntax validation, offline dictionary and URL unit tests, Android compilation, package validation, and APK signing before releasing an installable APK.

## Safety and compatibility

This is a **debug-signed beta**. A successful APK build is not proof of reliability on every Android device. Some services require HTTPS, special website permissions or browser capabilities this initial version doesn't support. Avoid entering sensitive credentials until you've tested the browser's behavior on your device. Internet browsing requires a network connection; local dictionary lookup works offline.
