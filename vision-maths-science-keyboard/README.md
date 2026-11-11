# SAURABH'S VISION™ — Maths & Science Keyboard 1.3

**Type Smarter. Explore Further.**

A separate, **native functional Android Input Method Editor (IME)**; not a keyboard mockup, theme-only wallpaper, or browser page. Package: `com.saurabhsvision.keyboard`.

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
