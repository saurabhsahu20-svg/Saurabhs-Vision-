#!/usr/bin/env bash
set -euo pipefail

# On a true Android 15 emulator check *rendered* key bounds, rather than
# assuming source-code arithmetic proves the Spacebar is visible.
APK="vision-maths-science-keyboard/app/build/outputs/apk/debug/app-debug.apk"
test -s "$APK"
adb wait-for-device
adb shell wm size 1200x528
adb shell wm density 240
adb shell settings put system accelerometer_rotation 0 || true
adb shell settings put system user_rotation 1 || true
adb install -r "$APK"
adb shell ime enable com.saurabhsvision.keyboard/.VisionKeyboardService
adb shell ime set com.saurabhsvision.keyboard/.VisionKeyboardService
adb shell am start -n com.saurabhsvision.keyboard/.KeyboardVisibilityTestActivity

# Android 15 hides third-party IME keys from UiAutomator's accessibility
# hierarchy even when the keyboard is visible. Validate the ACTUAL pixels.
sleep 10
adb exec-out screencap -p > /tmp/sboard-visual.png
test -s /tmp/sboard-visual.png
java vision-maths-science-keyboard/scripts/SBoardScreenshotCheck.java /tmp/sboard-visual.png
