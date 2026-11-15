# S Browser 1.2 Beta — Chhattisgarh Jobs & Exams

**by SAURABH'S VISION™** · Android 8.0+ · official source-first search

![S Browser build](https://github.com/saurabhsahu20-svg/Saurabhs-Vision-/actions/workflows/build-s-browser.yml/badge.svg?branch=main)

## New: CG Jobs & Exams Hub
Open **Jobs** from S Browser's bottom navigation, home dashboard, or tools menu.

- **City filters:** Raipur, Bhilai (Durg district), Durg, Rajnandgaon, Bilaspur, Dhamtari, Mahasamund, Saraipali (Mahasamund district), Raigarh, plus all 33 Chhattisgarh districts through the district selector.
- **20 curated official portals:** District recruitment notices, CG Vyapam, CGPSC, CG Medical Education, RRB Bilaspur, UPSC, SSC, National Career Service, JEE Main / JEE Advanced, Navodaya Vidyalaya, Sainik School AISSEE, and NCERT NTSE information.
- **To-the-point source discovery:** Choose city and category, type a department/post keyword, open the publisher's official notices and PDFs, or run a host-restricted government website search. Search results are **web links**, not independently verified vacancies.
- **Opt-in change alerts:** Android may periodically check a rotating group of official pages. After an initial baseline, a changed page can trigger a local notification telling the user to check the official site. The alert NEVER claims a vacancy is open; page changes also include corrections, results and archived notices. Check timing depends on Android battery/network constraints.
- **No fake vacancy dates:** Eligibility, age, category rules, application fees, active status and last dates must be verified from the original official recruitment notice. NTSE is a link to NCERT for programme/status updates, not a claim that registration is currently open.

### Privacy and safeguards
Background checks are **optional**, use direct HTTPS official pages, store small local page fingerprints and do not send user data to our servers. The app requests notification permission only when enabling alerts on newer Android. Alerts are not a guaranteed daily feed: pages can be blocked, change without a new vacancy, or fail to update immediately. No account, location permission, or marketing analytics.

## Existing browser features
Native Android WebView browsing; Google/DuckDuckGo/Bing search; 8 tabs, bookmarks and history; local study notes, reading mode, and Study Focus. **304 offline English–Hindi dictionary entries**, with definitions and pronunciation where Android supports it. All previous browser features and the S Browser app name/icon remain.

## Build
Open `s-browser/` in Android Studio (Java 17, Gradle 8.9, Android SDK 35, AGP 8.7.3), or download a debug-signed APK from the [v1.2 beta release](https://github.com/saurabhsahu20-svg/Saurabhs-Vision-/releases/tag/s-browser-v1.2.0-beta). GitHub Actions compiles and checks signing plus Java tests for official source filtering and city mapping.

**Beta limitation:** A green CI build does not guarantee all government sites work in Android WebView, or verify their current application deadlines. Browser security and accessibility need real-device testing. Full private/incognito isolation, AI tutoring, scraping every official vacancy into structured cards, and instant verified push notifications are NOT yet implemented.
