# S Browser 1.7 Beta — Smooth Tabs & Reliable News Recovery

**by SAURABH'S VISION™** · Android 8.0+ · Student-first browser

![S Browser APK](https://github.com/saurabhsahu20-svg/Saurabhs-Vision-/actions/workflows/build-s-browser.yml/badge.svg?branch=main)

## News update (from a real phone screenshot)

The screenshot showed **CG City News → Network/feed unavailable: timeout**. Instead of displaying a dead-end error, this version adds:

- One bounded retry on network timeout. RSS can still fail on certain Android networks, so the app **does not claim the problem can never recur**.
- **Retry feed**, **Open live news** (Google News web page in S Browser), and one-tap **Try English/Hindi headlines** actions.
- If headlines were downloaded recently, a **local backup** may display the last successful results. The backup is only used within **12 hours of fetching**, and every article must remain inside the chosen 24h / 3-day / 7-day publication filter. The news is explicitly marked **Saved, NOT live** with the last successful fetch time. No hidden old headlines.
- Clear status when no available articles meet the date and source filters. No fabricated local stories.
- Premium news header styling and improved retry controls.

**Important:** S Browser uses an external news RSS provider. Internet speed, network policy, website changes, and local coverage can still cause timeouts or empty results. The direct live-news fallback depends on that external website, too. Publishers may make factual mistakes; always check dates and original articles before sharing.

## New browser navigation & appearance

- Smooth, **horizontally scrollable tab strip** just under the search/address bar. Each website has its own tab title with an active tab highlight, a close × control, Home, and a ＋ New Tab button.
- Tapping a tab reattaches its existing WebView; it does not reload the page. Inactive WebViews are paused to reduce background work.
- Closing a different tab no longer throws the user back to Home.
- Premium navy/teal gradient homepage with improved headings and section dividers.
- Existing bottom browser navigation remains intact and familiar.

## All earlier features preserved

Daily Assembly & Fun (31 bilingual thoughts and speeches, General/Sports News, puzzles/crosswords), CG City News and official Jobs & Exams, **304-entry offline English–Hindi dictionary**, Study Focus, tabs, bookmarks, history, notes, downloads, and the one **Saurabh Vision Learning App** homepage shortcut linking to the owner's hosted learning app. S Browser name and original neon S icon remain.

## Build / APK

Open `s-browser/` in Android Studio (JDK 17, Gradle 8.9, Android SDK 35) or download the **debug-signed** APK from [S Browser v1.7 Beta Release](https://github.com/saurabhsahu20-svg/Saurabhs-Vision-/releases/tag/s-browser-v1.7.0-beta).

GitHub Actions runs Java unit tests, source-level UI and news fallback checks, APK compilation and APK signature verification. The only confirmation of smooth transitions across real websites, OEM phones, and unstable mobile connectivity comes from **device testing**, which remains to be performed.
