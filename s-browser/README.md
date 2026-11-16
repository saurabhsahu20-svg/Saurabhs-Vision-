# S Browser 1.4 Beta — Daily Assembly & Fun

**SAURABH'S VISION™** · Android 8+ · Student-centered browser

![S Browser build](https://github.com/saurabhsahu20-svg/Saurabhs-Vision-/actions/workflows/build-s-browser.yml/badge.svg?branch=main)

## New: Daily Assembly & Fun (7 point-wise activities)
Open the prominent **Daily Assembly & Fun** tile on the browser homepage, or the Browser Tools menu.

1. **Thought of the Day:** 21 original bilingual Hindi/English reflections. Day-based rotation; copy or share with class.
2. **School Assembly Speech:** 14 short, prepared Hindi/English school speeches. Copy, share and optional device Text-to-Speech listen button (voice availability depends on Android).
3. **Sports News:** On-demand sports publisher headlines (Chhattisgarh or India), last 24h or last 3 days, in Hindi/English with publisher and publication time. Requires internet.
4. **Breaking & Latest News:** Separately loaded current-affairs headlines with publisher/date and link. Headline appearance does not establish that the story is a verified breaking event. Do not read unverified news aloud without checking the article.
5. **Astrology & Reflection:** Choose any of 12 traditional zodiac signs to see a gentle daily positive suggestion. **Just for entertainment—not evidence-based predictions, life guidance or scientific facts.** Does not ask for birthdays, birth location or other private details.
6. **Mini Crossword:** 3 rotating playable 4×4 word-square crosswords (rows and columns form words), clue lists, answer check, one-letter hints, clear and local saved progress.
7. **Puzzle Games:** 14 rotating word scrambles plus short riddles, answer checks, hints and optional extra practice. Works offline.

**Bonus: Ready-to-read Assembly Script** brings a short greeting, today's thought, today's speech and a riddle into a point-wise Hindi/English script that can be copied or shared. Live news is deliberately not inserted as supposedly verified facts.

**Daily rotation:** Based on the phone's local calendar date. All prepared thought, speech and puzzle content is original and stored within the app. No sign-in, ads or additional personal-data permissions needed.

## News accuracy & availability
Sports and current-affairs news use third-party Google News RSS searches when the user opens each news section. Each headline is sourced from an identified publisher and carries its actual RSS date. Outdated items outside the selected window are omitted. S Browser **cannot independently fact-check headlines, verify breaking claims or guarantee fresh news for every topic**. Connectivity, source policies and feed availability may affect results. Always open the article and verify sensitive claims before presenting them in a school assembly.

## Previous Features
- CG City News: Chhattisgarh's 33 districts, other cities and custom localities, publisher/date filtering.
- CG Jobs & Exams: 20 government/education sources including CGPSC, CG Vyapam, RRB, IIT-JEE, JNVST and AISSEE; optional webpage-change notices.
- Native browser tabs, downloads, bookmarks, local history/notes, reading mode, Study Focus.
- 304-word offline English–Hindi study dictionary with pronunciation where available.
- App name **S Browser**, user-chosen neon S icon and original branding preserved.

## Build & Beta Testing
Open `s-browser/` in Android Studio, JDK 17, Gradle 8.9, Android SDK 35, or download the debug-signed APK from [v1.4 beta release](https://github.com/saurabhsahu20-svg/Saurabhs-Vision-/releases/tag/s-browser-v1.4.0-beta). Automated Java tests cover content rotation, valid puzzle answers, news query URL safety, city directory and offline dictionary data. CI also builds the Android APK and verifies its signature.

This is an experimental beta. A green build does not prove all websites, news links or speech voices work on real phones. Test with another Android device before relying on it for a class assembly. The live RSS feature may require review of third-party service conditions before commercial deployment.
