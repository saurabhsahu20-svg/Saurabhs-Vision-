# S Browser — Student-First Android Web Browser (0.1 Beta)

**by SAURABH'S VISION™** · Android 8.0+ · Optimized for Android 14

![S Browser build](https://github.com/saurabhsahu20-svg/Saurabhs-Vision-/actions/workflows/build-s-browser.yml/badge.svg?branch=main)

## Real working features

- **Browser:** Address/search bar, Google / DuckDuckGo / Bing, HTTPS-first typed domains, native WebView, Back/Forward, progress, up to 8 tabs, tab switcher and restore session.
- **Student dictionary:** 168 curated English–Hindi terms stored in a UTF-8 TSV asset. Search works **offline**, shows Hindi meanings and English explanations/examples, and provides Android text-to-speech for pronunciation where a voice is installed. Tap **Dictionary** in bottom navigation.
- **Learning:** NCERT, Khan Academy, Wikipedia and Google Scholar shortcuts; per-page local notes; readable text extraction from accessible pages; optional Study Focus that blocks a small preset of distracting top-level domains. These are actual app features, not placeholder buttons.
- **Personal browsing:** Local bookmarks/history, find in page, share links, desktop mode, file uploads and downloads through Android DownloadManager.
- **Security baseline:** SSL certificate errors cancelled (never bypassed), HTTPS used for address-bar domain input, unsafe address schemes blocked, file/content WebView access disabled, mixed content blocked, third-party cookies disabled, Safe Browsing enabled. No JavaScript bridge, analytics, advertising SDK or unnecessary permissions.

## APK
On a successful GitHub Actions run, download **S-Browser-0.1.0-APK** from the run's Artifacts, or get the debug APK from the release **s-browser-v0.1.0-beta**.

**Android Studio:** Open the `s-browser/` directory. JDK 17, Gradle 8.9, Android SDK 35, Android Gradle Plugin 8.7.3.

## Limitations

This is an early beta that needs device testing. Websites requiring specialized Chromium/Chrome services may not work in Android WebView. Study Focus is only a *top-level website blocklist*, not a comprehensive network filter. This initial release **does not** promise full private/incognito isolation, ad blocking, AI assistance, cloud sync, unrestricted PDF editing or a complete lexical dictionary. The dictionary has 168 locally curated entries and is expandable via `app/src/main/assets/dictionary.tsv`.

Downloads are handled by Android and might have different visibility on Android versions. Browser session restore reloads previous pages and needs internet. Basic local history and notes are stored in app preferences; uninstalling loses them. Test in a non-critical environment before relying on the browser daily. Do not mistake a green CI build for real-world website compatibility.

## Planned next steps

Expandable offline dictionary with subject packs, imported reading lists, better WebView lifecycle handling, battery/performance profiling, and independent accessibility and browser security audit.
