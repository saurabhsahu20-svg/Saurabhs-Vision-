# S BOARD 1.9 — Neon Blue Maths & Science Keyboard by SAURABH'S VISION™

**Type Smarter. Explore Further.**

A separate, **native functional Android Input Method Editor (IME)**; not a keyboard mockup, theme-only wallpaper, or browser page. Package: `com.saurabhsvision.keyboard`.

## S Board 1.9 — Screenshot-inspired native keyboard

This is a working Android InputMethodService keyboard (not a visual mockup). The first screen uses the reference's midnight-blue / cyan gradient look:

- Existing official **S Board** logo and two-line **S Board / by Saurabh's Vision™** header.
- Horizontally scrollable functional toolbar: **ABC, 123, ∑ Math, ⚛ Sci, 😊 Emoji, 🌐 Languages, 🎨 Theme, ⚙ Settings**. On narrow portrait phones, swipe the toolbar to access the tabs to the right.
- Bold glowing navy/cyan QWERTY keys, white labels, turquoise Enter, and an actual **wide Space key** with a thin glowing underline, not an image-only decorative spacebar.
- **Suggestions OFF by default** so the screen matches the supplied reference. Open Settings → Photo-Style Keyboard Layout → Toggle Smart Suggestions to enable existing offline English/Hinglish suggestions.
- Keyboard height automatically fits the real IME viewport in portrait and landscape. Compact mode is recommended for small screens. Other colours (Gold, Science Green, Purple, White) remain available.
- Language selection (English, Hinglish and 22 Indian scheduled languages via basic offline transliteration), Maths, Science and Emoji are preserved.
- No Internet permission, microphone permission, read-clipboard permission, typing telemetry, or stored keystroke history.

**Download:** Use the GitHub Releases entry **s-board-v1.9.0** once CI completes. The artifact is a signed-for-testing *debug* APK, not a Play Store release or a permanently signed production build. A previous debug APK with a different certificate may need uninstalling before installation; this resets that app's local settings. Never disable system security checks to install APKs.

**Verification:** GitHub Actions tests the phonetic core, layout algorithm, suggestion core, compiles Android APK and checks the debug APK signature. Physical installation and testing in WhatsApp, Notes, and password fields are still necessary.

## Features

- **ABC** QWERTY typing: Shift, double-tap Shift for Caps Lock, Delete with hold-to-repeat, space, punctuation, native IME actions and cursor navigation in supported modes.
- **123** digits and symbols, including ₹ and punctuation.
- **Maths ∑**: π, √, ∑, ∞, ≈, ≠, ≤, ≥, ±, ×, ÷, ∫, ∂, Δ, θ, Greek letters, powers, and function shortcuts.
- **Science ⚛**: H₂O, CO₂, O₂, NaCl, H⁺, OH⁻, e⁻, pH, DNA, RNA, reversible-reaction arrow, units and physics symbols.
- **5 keyboard themes**: Cosmic Blue, Golden Mathematics, Science Lab, Purple Quantum. Lightweight Canvas background and clickable ripple keys.
- Hindi + English walkthrough inside the app, with setup buttons and typing test box.
- Official public Instagram profile shortcut: `https://www.instagram.com/saurabhs_vision/`.

## How to enable on Android

1. Install the APK and open **Vision Maths & Science Keyboard**.
2. Tap **Enable Vision Keyboard** and enable it in Android Keyboard/Input Method Settings.
3. Return and tap **Choose Default Keyboard**, then select **Vision Maths & Science**.
4. Open Notes, WhatsApp or the built-in test field to type; use the tabs **ABC**, **123**, **∑ MATH**, **⚛ SCI**.

Android may show a standard warning when enabling any third-party keyboard. Review keyboard permissions before enabling.

## Privacy and limitations

This keyboard requests **no Internet permission** and does not log or upload keystrokes, passwords, clipboard contents, messages or typing history. It uses the Android InputMethodService API to send keys **only to the active input field** via InputConnection. It contains no autocorrect dictionary, text prediction, voice typing, GIF search or multilingual transliteration. Scientific shortcuts insert Unicode strings, not rendered equations or LaTeX. No cloud storage, analytics or ad SDK.

Built with Java 17, Android SDK 35, AGP 8.7.3, Gradle 8.9; minSdk 23 (Android 6.0). The automated GitHub build checks compilation, APK integrity, signing, package metadata and InputMethodService registration. **Real handset testing is still required**, especially on OEM keyboards, screen sizes and password-entry fields.

## Build

```bash
cd vision-maths-science-keyboard
gradle --no-daemon :app:assembleDebug
```

The GitHub Actions release distributes a **debug-signed test APK**. Do not treat it as a Play Store-ready release; use a stable private signing key for production, and test IME privacy and UX on a device.

**SAURABH'S VISION™ — Learn Today, Lead Tomorrow!**

## 1.1 Update — fixed full-screen background and added emoji panel

The first version showed a very tall empty neon background under the typing keys on some Android devices. This version eliminates the independent MATCH_PARENT wallpaper View. The full Maths/Science background is **painted by a KeyboardPanel containing only toolbar and key rows**, measured WRAP_CONTENT with a strict max height, and Android fullscreen extract mode is disabled. ChatGPT, WhatsApp and Notes should remain visible above the keyboard instead of being hidden behind unused blue space. A device test is still needed.

Choose the **😊** toolbar tab or bottom-row emoji key. The offline EmojiCatalog offers **923 emoji entries across 11 groups** (795 distinct strings), emphasizing facial emotions, love, feelings, hand gestures, people and more. Use the horizontal category bar and ◀ ▶ for 32 emoji per page. Emoji insertion uses the active InputConnection with Unicode, so combined emojis and skin tones depend on Android/font/app support. This is not the complete Unicode emoji standard and does not include a GIF or sticker browser.

Compatibility: Upgrading a previously installed debug-signed APK may conflict with a different debug signing certificate. If upgrading fails, uninstall only the old Vision Maths & Science Keyboard after first switching to the phone's original keyboard; then install this new APK. Settings and custom themes may reset. Do not uninstall the Android system keyboard.

## 1.2 Update — professional keycaps and Pure White

The keyboard includes 5 themes: Cosmic Blue, Golden Mathematics, Science Lab, Purple Quantum, and **Pure White**. Light mode uses a clean grey-white science grid, high-contrast dark text on bright keys, powder-blue function keys, a strong blue Enter key, and matching active tabs. Every theme has smoother rounded key shapes, soft borders/elevation, Android ripple response and subtle pressed-scale feedback, while keeping the compact-height design. The setup screen offers five theme cards and a matching key-style preview. Use the ☀ button in the keyboard toolbar to cycle themes, or select Pure White in app settings. All Maths, Science and emoji modes are retained. No Internet or typing-record permissions are added. Real device tests remain required.

## Version 1.3 — Spacebar row no longer cropped

Some Android/WhatsApp chat screens limited the height offered to this third-party keyboard. The previous KeyboardPanel clipped its children because it capped the parent measure without resizing button rows. V1.3 calculates the actual available IME height, measures each row and its contained keys, then fits all rows **before** measuring. It reserves a guaranteed final row for SPACE, Enter and punctuation, reducing the toolbar/other rows first when space is tight. Standard typing rows start at 48dp and Space row 51dp; both auto-shrink when the Android host offers less vertical space. The **SPACE** label is larger and accessible. Emoji pages also use the same adaptive panel. 

In the setup screen, **Compact Keyboard (Recommended)** is enabled by default; **Comfortable / Larger Keyboard** gives larger keys if the device has room, but still auto-fits. Close/reopen keyboard after changing. User device screenshots are the best way to verify actual manufacturer-specific behavior. Build tests check source layout contracts, Java compile, IME manifest and APK signing, but do not replace manual keyboard testing on WhatsApp.

## Version 1.4 — 22 Indian languages + Hinglish / English / Hindi input

Use **🌐** on the compact keyboard toolbar, select a language and return to ABC. Or open keyboard Settings and choose one from the **24 options**: English, Hinglish (Roman Hindi), plus India's **22 constitutionally scheduled languages** (Hindi, Bangla/Bengali, Marathi, Tamil, Telugu, Gujarati, Kannada, Malayalam, Punjabi, Odia, Assamese, Sanskrit, Nepali, Konkani, Maithili, Dogri, Bodo, Kashmiri, Sindhi, Manipuri/Meitei, Santali/Ol Chiki, Urdu).

**Hindi phonetic:** Type ordinary English letters, for example:
`namaste` → `नमस्ते`
`mera` → `मेरा`
`aap` → `आप`
`bharat` → `भारत`
`shiksha` → `शिक्षा`

Hindi mode uses Android InputConnection composition: typed Roman letters update the composing script word; Space/Enter commits the word; Backspace edits the composing Roman input and updates script. Hinglish mode types Roman letters exactly as written. English is ordinary QWERTY. Switch between Hindi and Hinglish via quick language toggle on the strip. Maths/Science/emoji are unchanged.

**Important limitations:** This is a **basic offline phonetic transliteration keyboard**, NOT an English↔Hindi sentence meaning translator or a full production language IME. A finite Hindi common-word dictionary helps typical Roman spellings; other Brahmic scripts use approximate syllabic character mappings and Indic common spellings are not always linguistically accurate. Kashmiri and Sindhi use their Devanagari script options, not their Perso-Arabic spellings; Meitei Mayek, Ol Chiki and Urdu use simplified roman-letter approximation, not full orthography rules. Unsupported characters can retain source script characters. A number of Indian dialects and unscheduled languages are not covered. Users should proofread critical writing.

No network needed or requested. Roman composing input is held **in memory only** while typing, without saving to disk, logging or transmission. Password fields intentionally fall back to ordinary Latin typing; the language picker is never a password collection service.

Native Java smoke tests run on the GitHub builder for all 24 options, letter-to-script checks for 11 scripts, plus actual Android build, manifest and APK-signature verification. Device typing with real launchers, cursor edits and Unicode emoji requires hands-on QA. 

## V1.5 — Saune Board brand, user's own original neon animated logo

**New app display name:** **Saune Board**, by Saurabh's Vision. Android application ID remains `com.saurabhsvision.keyboard` to keep the installed keyboard identity.

The original, newly generated blue-neon keyboard and gold book/graduation-cap artwork is included in the APK as `res/drawable-nodpi/saune_board_icon.webp`. This **last frame of the 6-frame user-approved animation** is the native launcher icon and the app's hero illustration. The launch screen features the SAME icon framed by bright animated cyan/gold orbital trails, smooth pulsing and subtle stars. Ordinary Android launcher icons do not animate: the startup screen does (approximately 2.4 seconds, tap to skip). No network/media permissions.

The app's name, setup call-to-action, keyboard display name and theme preview now use **Saune Board** branding. Android's InputMethodService stays separate from the splash, so the animation never delays typing in other apps. The setup screen also displays whether the keyboard is enabled and currently selected as default.

**Retained**: ABC, Maths, Science, numbers, Emoji categories, 5 themes including White, compact Space/Enter, 24 language options, basic offline phonetic transliteration, no Internet permission. Install as a debug-signed TEST APK and test on real Android hardware before production.

## Version 1.6 — S Board (Spacebar repaired)

The installed app name is now **S Board**, with an original blue/gold **S** lettermark app icon and the same logo in the animated launcher intro. The Android application ID and functionality remain unchanged so this is an update to the same keyboard.

**Important WhatsApp Spacebar bug:** Previous height-fix attempts left the bottom action row cropped because the IME container's inherited LinearLayout measuring step didn't guarantee all children fit their measured window. The new `KeyboardPanel` has explicit `onMeasure` **and** `onLayout` methods and a pure-Java `KeyboardLayoutPolicy`: every child is measured EXACTLY to the allocated width and height, and the bottom action row is reserved before allocating typing rows. Layout tests cover small landscape/portrait, Hindi, emoji and language-selection cases. `␣` in the top toolbar is a **backup Space button** if a device's system UI still clips the footer; the full SPACE key also remains at the bottom. Compact height defaults to a safe value. White and 4 neon themes, Maths, Science, emoji and Indic phonetic typing remain.

The new vector launcher icon is intentionally original, not copied from Google Gboard. Android system launchers generally show static app icons; the S Board activity has a matching animated splash. GitHub CI checks compile, layout regression tests and APK signing, while physical WhatsApp/Gboard-like keyboard behavior still needs on-device testing.

## S Board 1.7 — Your chosen neon 3D logo installed as Android app icon

The S BOARD visual artwork that the user selected is now included in the project at `app/src/main/res/drawable-nodpi/s_board_logo.webp`. This is an optimized **128 × 128** original-image conversion for small Android launcher icons; the original, full-size image is also available to the user separately in the conversation. AndroidManifest uses that resource for both `android:icon` and `android:roundIcon`. Both the launcher opening animation and main setup screen reference the same new resource. `s_board_icon.xml` remains in source as a legacy unused fallback asset. Branding remains **S Board**, not Saune Board; application ID is unchanged. No keyboard input behavior or permissions were changed. All existing IME, maths, science, emoji and languages features remain intact. GitHub Actions validates WebP resource headers, compiles and signs a test APK. Manual handset checks remain required to verify OEM launcher cropping and splash rendering.

## Version 1.8 — REAL native S Board neon typing UI, not a concept screenshot

The actual Android `InputMethodService` keyboard now matches the design concept: vivid navy/blue gradient keycaps with cyan outlines, large rounded Enter and Space keys, visible embedded **S Board** logo/brand in a horizontally scrollable top toolbar, and the fully interactive QWERTY layout (centered second row, Shift and Backspace). Below the toolbar is a **three-slot live suggestion strip**. On English and Roman Hinglish it presents the word currently being typed, plus simple completions from an **offline, fixed dictionary**. Tapping a suggestion replaces the unfinished word with the selected completion plus a space. It does **not** use cloud autocorrect, record keyboard history, or match the extensive predictive model in Google Gboard. In Hindi phonetic mode, the first candidate displays the converted-script form. Password fields do not offer suggestions.

The bottom row now intentionally resembles a regular mobile keyboard: **?123 / ☺ emoji / A↔अ language switch / Space / comma / period / Enter**. The Space key has the highest width weight and is laid out in the IME itself, with adaptive height reserved to avoid cropping. Long-press the Q–P top letters for digits 1–0. Tap the Math, Science, Emoji, Language, Theme or Settings tab to use those retained features. On narrow screens the toolbar can scroll sideways without reducing letter key size. White Theme is preserved. Actual Android app display name remains **S Board**; package ID is unchanged. CI tests offline word candidates, source contracts, language spelling smoke tests, footer row fit in different viewport budgets and Android compilation/APK signature. Confirm UI on a real phone before promising identical screenshots.
