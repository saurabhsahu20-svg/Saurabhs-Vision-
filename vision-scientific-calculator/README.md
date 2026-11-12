# VISION CALC 1.1 — Premium Scientific Calculator for Android

**By SAURABH'S VISION™ — Smart Maths, Simple Learning**

A real, native **offline Android scientific calculator**, not a static mockup. Built with Java 17 and Android Gradle Plugin 8.7.3; no third-party SDKs, Internet, login, ads, analytics, or personal-data permissions.

## Premium responsive interface (v1.1)

The screenshot-inspired **Pearl Light** interface is the new default. A ☾/☀ control at the top switches between Pearl Light and the original Navy Dark palette; theme preference stays on the device.

- **Extra-large 4 × 5 keypad:** AC, smart `( )` bracket toggle, %, ÷, 0–9, ×, −, +, backspace and =. Every button actually computes, unlike a decorative mockup.
- **Responsive layout:** four large rounded pills per row, with adaptive height on smaller phones and scrollable content in landscape. Android navigation bar insets keep bottom buttons accessible.
- **No unused blank panel:** a large live expression and result card, recent calculation, scientific function strip, memory row, and DEG/RAD switch make use of upper screen space.
- **Scientific quick row:** sin/cos/tan/√/π/x²/log/ln/nCr/comma/nPr/e; **Show All** expands additional functions without shrinking the basic numeric buttons. Tap the comma chip to separate nCr, nPr, root, min, max arguments.
- **Full-featured light and dark screens** for Calculator, Unit Converter and History; accessible dark text in light mode.
- This is a design inspired by the user-provided photo, not an exact reproduction of another app's artwork.

## Functions

- Standard: `+`, `−`, `×`, `÷`, brackets, `%` (divide by 100), modulo, scientific notation.
- Scientific: `sin`, `cos`, `tan`, `asin`, `acos`, `atan`, `sinh`, `cosh`, `tanh`, `log`, `ln`, `exp`, `sqrt`, `cbrt`, `root`, `floor`, `ceil`, `abs`, `round`, `1/x`, `^2` and arbitrary powers.
- Maths: `nCr` / `comb(n,r)`, `nPr` / `perm(n,r)`, `gcd(a,b)`, `lcm(a,b)`, `min(a,b)`, `max(a,b)`, factorial `n!` (0–170), implicit multiplication `2π`, `2(4+5)`.
- Constants: `π`, `e`, `τ`, golden ratio `φ`, `Ans` (previous answer) and `M` (stored memory).
- Degrees/radians toggle, preview, error handling, right-associated powers (`2^3^2 = 512`), and correct unary precedence (`-2^2 = -4`).
- Scientific results display 12 significant digits. Note: uses Java IEEE-754 double arithmetic, not arbitrary-precision symbolic mathematics; rounding/precision limitations apply.
- Unit converter: **Length, Mass, Temperature, Area, Volume, Time, Speed, Pressure, Energy, Angle, Data, Frequency**. No invented online currency rates.
- 60-entry on-device history, local memory `MC`, `MR`, `M+`, `M−`, `MS`, result copy, and responsive dark Navy/Cyan/Gold interface.

## Examples

```
sin(30)*200          = 100      (DEG)
sin(π/2)             = 1        (RAD)
(2+3)^2              = 25
comb(5,2)            = 10
perm(5,2)            = 20
10mod3               = 1
log(1000)            = 3
5!                   = 120
root(3,-8)           = -2
25%*200              = 50
```

**Percentage means divide by 100.** For 20% of a quantity, write `20%*quantity`. Calculations that are undefined (division by zero, log of nonpositive number, 171!, etc.) show errors, not misleading finite answers.

## Source and build

```
cd vision-scientific-calculator
gradle --no-daemon :app:assembleDebug
```

The Gradle build needs a configured Android SDK 35 and JDK 17. GitHub Actions installs dependencies, runs pure-Java tests (including 100,000 repeated expressions), builds the APK, validates the APK signature/manifest, and uploads the debug-signed package to GitHub Releases.

**Release:** `vision-calc-v1.1.0`. The downloadable APK is a **debug-signed test app**, not Play Store production signing. Android may require opting in to installing trusted downloads from the browser. Review download source and do not bypass system warnings. This is separate from S Board and does not modify the keyboard.

## Privacy

All calculations and saved history live **only in app-local SharedPreferences**. The app requests **no INTERNET permission**, has no account/login/payment and does not read contacts, microphone, location or files. Users can erase calculation history in-app, or remove local app data in Android settings. No backup is requested.

## Boundaries

The app handles the listed mathematics and conversions. It does **not** replace professional engineering/metrology tools, handle arbitrary symbolic calculus, real-time exchange rates, or guarantee arbitrary precision. Results are approximate where floating-point maths is involved.
