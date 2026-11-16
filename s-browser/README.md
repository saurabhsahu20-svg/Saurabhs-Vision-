# S Browser 1.5 Beta — Accuracy First Daily Assembly

**SAURABH'S VISION™** · Android 8+ · News accuracy, fresh daily school content

![S Browser build](https://github.com/saurabhsahu20-svg/Saurabhs-Vision-/actions/workflows/build-s-browser.yml/badge.svg?branch=main)

## Improvements in v1.5

**Daily Thought of the Day** — 31 original short thoughts in Hindi and English. The selected thought changes with the phone's local date and is automatically refreshed when the app returns to the foreground after midnight. These are written as original positive educational material, **not quotes falsely attributed to public figures**.

**Daily Speech** — 31 distinct, concise Hindi–English school assembly speeches on timeless educational topics: reading, honesty, science, digital literacy, sportsmanship, fact-checking, kindness, nature and more. Speakers can copy, share or use Android text-to-speech. New content rotates daily; the prepared 31-item set repeats after 31 days. No AI-generated current-event claims or invented facts.

**General News & Sports News** — refreshed from the network each time their section is opened or Refresh is tapped. 24-hour / 3-day selection, Chhattisgarh/India and Hindi/English filters. Headlines are only shown if the item carries a real publication timestamp in the selected window and a source URL on an allowlist of established HTTPS publishers. Approved domains include `newsonair.gov.in`, `pib.gov.in`, `thehindu.com`, `indianexpress.com`, `reuters.com`, `bbc.com`, `espncricinfo.com`, `olympics.com`, and other reputable reporting outlets in the source.

**To-the-point format** — at most 6 dated, source-attributed headlines per news category. Each card includes: actual publisher, date/time supplied in its feed, link to read the story and link to compare other reports. Official shortcuts to PIB and News On AIR. Unavailable, outdated or unapproved stories are **not replaced with guesses**.

### Factual-accuracy limits

A reliable publisher name alone does **not** prove a headline is correct. News feeds may contain errors or editorial changes, and no third-party news app can guarantee that every article is true or cover every developing event instantly. The app does not claim independent fact-checking, and the compare-sources link opens another search for human review. Before speaking a news claim at assembly, check the full article, its date and (for important claims) corroboration or primary official records. If there is no qualifying recent article or no internet, the app explicitly displays that state.

Prepared speeches and thoughts are original age-appropriate writing; they are **not** presented as newly reported news. Android's device clock and network access affect refresh timing.

## Existing S Browser features

Daily Assembly & Fun, short speeches, zodiac entertainment-only reflections, offline puzzles/crosswords, 304-word English–Hindi dictionary, CG City News, jobs/exams official portal directory, WebView browsing, bookmarks, notes and downloads. App name, identity and the chosen S logo are preserved.

## Installation & testing

Android Studio: `s-browser/`, JDK 17, Gradle 8.9, Android SDK 35. Automated checks compile the Android APK, verify its debug signature and test 31-item daily rotation, puzzle validity, trusted publisher HTTPS host verification and bounded news query construction.

[Download v1.5 beta APK after successful CI release](https://github.com/saurabhsahu20-svg/Saurabhs-Vision-/releases/tag/s-browser-v1.5.0-beta).

This is a beta: compilation success does not prove that all news sources, links, Text-to-Speech voices or WebView pages work correctly on a physical Android phone. Provider terms and availability should be checked before commercial deployment.
