# SV Caller Pro 1.0 — Full-Screen Caller Wallpaper Phone App

**By SAURABH'S VISION™** — a privacy-conscious, native Android dialer with immersive incoming and ongoing call wallpaper. This is a genuine experimental Android default Phone app, not a static mockup.

## Working components included

**Home / Dialpad**
- Responsive numeric phone keypad with digits, `*`, `#`, `+` (long-press zero), deletion, haptic feedback and manual number editing.
- A real **CALL NOW** action using Android `TelecomManager.placeCall()`, only after Android's default-dialer role and phone permission have been granted.
- Four functional tabs: Dialpad, Contacts, Recents and Favorites.

**Contacts, Call History & Favorites**
- Optional, runtime-permission-gated Contacts name/number list with local search. Tap a contact to put its number in the dialer, or the phone button to initiate a call.
- Optional local device Call History for the user-approved default Phone app, including call type, date, duration and redial.
- Locally pinned favorite contacts via long-press; no server.
- Long-press a contact to **choose a custom full-screen caller photo** from Android's system image picker. Also available for a typed phone number.
- Photo permissions use `ACTION_OPEN_DOCUMENT` and persisted URI read access. A contact's own photo is used when available, otherwise the selected screen-theme gradient.

**Full-screen incoming/ongoing call screen**
- Android `InCallService`, with a proper `ACTION_DIAL` activity and default `ROLE_DIALER` role request.
- Real full-screen caller **photo as the background wallpaper**, center-cropped, sampled off the UI thread to prevent large image memory spikes. Text and controls overlay a readable gradient.
- Caller name and number (if contact access granted), incoming/ringing/outgoing/in-call status and connected duration.
- Incoming: real **Answer / Decline** buttons.
- Ongoing: real **Mute, Speaker/Earpiece, DTMF Keypad, End**, plus **Hold/Resume when supported by Telecom's call capabilities**.
- Android high-importance **CALL notification** with full-screen intent for incoming calls; notification offers Answer/Decline as fallback. On Android 14+ the ability to show a full-screen incoming call UI is governed by the device's **Full-screen notifications** special permission and user/system policy; the app includes a shortcut to those settings.
- Four offline fallback call themes: Cosmic Navy/Cyan, Purple Galaxy, Rose Gold and Emerald Science.
- Restores the system preloaded dialer automatically for emergency calls according to Android Telecom behavior.

**Branding and privacy**
- SAURABH'S VISION™ branding and the original user-provided 3D red **S** app icon.
- No advertising, analytics, cloud sync, Internet permission or upload of contacts/photo/call history.
- Phone, Contacts and Call Log permissions are requested only for actual features. Contacts, call logs and custom-photo URI references stay on-device; backup disabled.

## Important limitations and safety

This is version 1.0 of a real Android dialer; its compatibility with every phone/SIM/Android version is **not guaranteed** without testing on a physical device. It is a **debug-signed test APK**, not a Play Store production release.

- Changing the default Phone app is a meaningful device change: **test the app before setting it as default** and know how to return to your device's original Phone app in Settings → Apps → Default Apps → Phone app.
- In-call full-screen photo control works only when the app is selected as Android's default dialer and system Full-Screen Intent access permits it. Even with that permission, a manufacturer may present a heads-up notification instead of immediately opening the full-screen UI.
- Current prototype handles the **newest active call**; multi-call/merging/conferences, call waiting, Bluetooth route selection, accessibility audits and advanced multi-SIM routing are **not fully supported**. Do not rely on it as your only phone dialer in urgent situations until tested.
- Promotional mockups might show HD Video Calling, Call Recording, Spam Blocking, Privacy Lock, Contact Announcements or cloud contact sync. **Those features are NOT implemented and are not claimed.**
- Not a replacement for the built-in emergency calling UI. Android Telecom routes emergency calls to the preloaded dialer by design.
- This build uses native Android cellular calls over your carrier network, not free internet telephony. Carrier voice/SMS charges may apply.

## How to install and test safely

1. Download the APK from the GitHub Release `sv-caller-pro-v1.0.0`, install, open **SV Caller Pro**.
2. Explore its dialpad and theme screens before changing your default Phone app.
3. Select **Set as Default Phone App**, and confirm via **Android's system chooser**. Request Phone, Contacts and Notification access only when the feature needs it.
4. Grant **Full-screen notifications** permission in the app's Settings if Android requires it.
5. Long-press a contact or use **Caller Photo** from the dialer; choose a local photo. The photo will appear wallpaper-style on an actual call with that contact.
6. For a device compatibility problem, **immediately return** to your original system Phone app using Android Settings → Default Apps → Phone app; do not disable system security or emergency services.

## Build

Java 17 / Android Gradle Plugin 8.7.3 / compile SDK 35; min SDK 26 / target SDK 35.

```bash
cd sv-caller-pro
gradle --no-daemon :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. CI compiles and signs the APK, checks manifest/Telecom integration and tests dialpad validation. A successful build **does not prove** full real-device incoming call behavior.

Official Android documentation:
- https://developer.android.com/develop/connectivity/telecom/dialer-app
- https://developer.android.com/reference/android/telecom/InCallService
- https://source.android.com/docs/core/permissions/fsi-limits
