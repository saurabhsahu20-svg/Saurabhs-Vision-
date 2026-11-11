# SAURABH'S VISION™ — Maths & Science Keyboard 1.1

**Type Smarter. Explore Further.**

A separate, **native functional Android Input Method Editor (IME)**; not a keyboard mockup, theme-only wallpaper, or browser page. Package: `com.saurabhsvision.keyboard`.

## Features

- **ABC** QWERTY typing: Shift, double-tap Shift for Caps Lock, Delete with hold-to-repeat, space, punctuation, native IME actions and cursor navigation in supported modes.
- **123** digits and symbols, including ₹ and punctuation.
- **Maths ∑**: π, √, ∑, ∞, ≈, ≠, ≤, ≥, ±, ×, ÷, ∫, ∂, Δ, θ, Greek letters, powers, and function shortcuts.
- **Science ⚛**: H₂O, CO₂, O₂, NaCl, H⁺, OH⁻, e⁻, pH, DNA, RNA, reversible-reaction arrow, units and physics symbols.
- **4 original neon designs**: Cosmic Blue, Golden Mathematics, Science Lab, Purple Quantum. Lightweight Canvas background and clickable ripple keys.
- Hindi + English walkthrough inside the app, with setup buttons and typing test box.
- Official public Instagram profile shortcut: `https://www.instagram.com/saurabhs_vision/`.

## How to enable on Android

1. Install the APK and open **Vision Maths & Science Keyboard**.
2. Tap **Enable Vision Keyboard** and enable it in Android Keyboard/Input Method Settings.
3. Return and tap **Choose Default Keyboard**, then select **Vision Maths & Science**.
4. Open Notes, WhatsApp or the built-in test field to type; use the tabs **ABC**, **123**, **∑ MATH**, **⚛ SCI**.

Android may show a standard warning when enabling any third-party keyboard. Review keyboard permissions before enabling.

## Privacy and limitations

This keyboard requests **no Internet permission** and does not log or upload keystrokes, passwords, clipboard contents, messages or typing history. It uses the Android InputMethodService API to send keys **only to the active input field** via InputConnection. It contains no autocorrect dictionary, text prediction, voice typing, GIF search, multilingual transliteration or emoji picker yet. Scientific shortcuts insert Unicode strings, not rendered equations or LaTeX. No cloud storage, analytics or ad SDK.

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
