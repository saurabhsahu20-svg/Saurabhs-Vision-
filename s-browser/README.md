# S Browser 1.8 Beta — Reliable News & Professional Design

**SAURABH'S VISION™** · Android 8.0+ · Source-first student browser

![APK build](https://github.com/saurabhsahu20-svg/Saurabhs-Vision-/actions/workflows/build-s-browser.yml/badge.svg?branch=main)

## Based on real Android screenshots

**Problem reported:** The Daily Assembly → General & Latest News page showed a Google News RSS **timeout** and a mostly empty screen.

**New architecture:** S Browser first requests a small publisher-attributed JSON news bulletin on the owner's GitHub Pages site. A [news refresh workflow](../.github/workflows/refresh-s-browser-news.yml) attempts updates approximately every three hours using RSS from approved publishers, and from PIB for India government announcements where available. If the hosted bulletin is missing, expired, or has no suitable items, the app can still try **live Google News RSS directly**.

This is not guaranteed live publishing: scheduled Actions runs can be delayed or suspended, GitHub Pages deployment can fail, and upstream news sources may be inaccessible. The app never invents headlines or silently relabels outdated stories as breaking news.

### General & Sports News page redesign
- Professional news desk header and gradient status panel with **source freshness** wording.
- Up to **6 concise, dated headline cards**, numbered, with publisher, actual date/time, **Read source** and **Cross-check** buttons.
- One-tap Hindi/English, Chhattisgarh/All India, and last 24h/3d filters.
- If both providers fail, the screen shows a useful recovery card with **Retry** and **Open latest news in browser**, plus News On AIR and PIB official shortcuts.
- A separate **offline Thought of the Day and short speech shortcut** is shown when headlines cannot load; these are clearly labelled offline content, not presented as news.
- Source allowlist includes large established media and local Chhattisgarh/Hindi news publishers. No independently fact-checked truth guarantee; read the full article before using it for a school assembly.
- No news account, no extra permissions, no personal information sent to our backend. Reading a news item contacts its publisher and the external services used to deliver results.

### Homepage design
Premium dark gradient, fresh daily banner, cleaner navigation and **TODAY'S NEWS DESK** with separately tappable **General News** and **Sports News** cards, in addition to the retained Daily Assembly & Fun, CG Jobs, City News, and Saurabh Vision Learning panels.

### Existing features preserved
304-entry offline English–Hindi dictionary, bookmarks, history, notes, downloads, smooth horizontal browsing tabs, Study Focus, Chhattisgarh Jobs/Exams, 33-district City News, zodiac reflections for fun, short Hindi–English speeches, thoughts and interactive puzzle games.

## Installation
Download the **debug-signed** Android APK from [S Browser v1.8 beta](https://github.com/saurabhsahu20-svg/Saurabhs-Vision-/releases/tag/s-browser-v1.8.0-beta) after successful Actions build. Open `s-browser/` in Android Studio with JDK 17, Gradle 8.9, Android SDK 35 to build.

**Limitations:** A passing GitHub build only verifies compilation/static tests and APK signing, not actual mobile network access or smooth performance on every phone. Some network/service failures can still happen. Read source links and published dates before announcing current news to children.
