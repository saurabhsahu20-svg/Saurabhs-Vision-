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

# Android does not expose this third-party IME in UiAutomator XML. Instead
# verify the screenshot pixels. A headless emulator may briefly display a
# Launcher ANR dialog during the first boot, so safely reopen our probe if needed.
for attempt in 1 2 3 4; do
  sleep 8
  adb exec-out screencap -p > /tmp/sboard-visual.png
  test -s /tmp/sboard-visual.png
  if java vision-maths-science-keyboard/scripts/SBoardScreenshotCheck.java /tmp/sboard-visual.png; then
    echo "PASS: full S Board Spacebar is visible above system navigation"
    exit 0
  fi
  echo "Android display not yet ready (attempt $attempt). Checking for system dialogs..."
  adb shell uiautomator dump /sdcard/sboard-window.xml >/dev/null 2>&1 || true
  adb pull /sdcard/sboard-window.xml /tmp/sboard-window.xml >/dev/null 2>&1 || true
  if [[ -f /tmp/sboard-window.xml ]] && grep -qiE "isn.t responding|not responding|Close app" /tmp/sboard-window.xml; then
    echo "System launcher ANR detected, dismissing dialog"
    adb shell input tap 420 268 || true
  fi
  adb shell am force-stop com.google.android.apps.nexuslauncher 2>/dev/null || true
  adb shell am start -n com.saurabhsvision.keyboard/.KeyboardVisibilityTestActivity || true
  adb shell input tap 320 130 || true
  adb shell ime set com.saurabhsvision.keyboard/.VisionKeyboardService || true
done
echo "FAIL: Android Emulator did not show an unobstructed Spacebar after recovery attempts."
exit 1
