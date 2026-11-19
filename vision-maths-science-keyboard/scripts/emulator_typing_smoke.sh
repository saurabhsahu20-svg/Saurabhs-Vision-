#!/usr/bin/env bash
set -euo pipefail
APP="com.saurabhsvision.keyboard"
IME="$APP/.VisionKeyboardService"
APK="vision-maths-science-keyboard/app/build/outputs/apk/debug/app-debug.apk"
test -s "$APK"

echo "Android API:"; adb shell getprop ro.build.version.sdk
adb wait-for-device
adb shell wm size 1080x1920
adb shell wm density 420
adb shell settings put system accelerometer_rotation 0 || true
adb shell settings put system user_rotation 0 || true
adb install -r "$APK"
adb shell am start -W -n "$APP/.KeyboardVisibilityTestActivity" || true
# The first boot may complete before Android's InputMethodManager refreshes the
# installed IME service cache. Poll for registration instead of racing it.
found=0
for attempt in $(seq 1 30); do
  if adb shell ime list -a | grep -F "$APP" >/dev/null; then found=1;break;fi
  echo "Waiting for S Board to register as Android IME ($attempt/30)"
  sleep 2
done
if [[ "$found" != 1 ]]; then
  echo "Android IME not registered. IME list:"
  adb shell ime list -a || true
  adb shell dumpsys package "$APP" | head -70 || true
  exit 1
fi
adb shell ime enable "$IME"
adb shell ime set "$IME"
adb shell am start -W -n "$APP/.KeyboardVisibilityTestActivity"
sleep 12

# Take a screenshot of the actual Android IME in portrait.
adb exec-out screencap -p > /tmp/sboard-portrait.png
test -s /tmp/sboard-portrait.png
echo "Portrait screenshot captured."

# Tap physical screen positions in ABC rows. Verify text reaches the host
# EditText through the active IME, not via 'adb input text' or a fake test API.
adb shell input tap 230 1335
adb shell input tap 420 1475
adb shell input tap 590 1610
sleep 2
adb shell uiautomator dump /sdcard/sboard-tap-result.xml >/dev/null 2>&1 || true
adb pull /sdcard/sboard-tap-result.xml /tmp/sboard-tap-result.xml >/dev/null
if grep -Eq 'Characters entered: [1-9][0-9]*' /tmp/sboard-tap-result.xml; then
  echo "PASS: keyboard screen taps produced real Android editor characters."
else
  echo "Typed characters were not confirmed; inspecting screenshot/UI XML."
  grep -o 'Characters entered: [0-9]*' /tmp/sboard-tap-result.xml || true
  exit 1
fi

# Check the OS still reports S Board as the active IME.
adb shell settings get secure default_input_method | grep -F "$IME"
echo "PASS: S Board is the selected input method."

# Switch to a small landscape window and verify the keyboard can still open.
adb shell wm size 1200x528
adb shell wm density 240
adb shell settings put system user_rotation 1 || true
adb shell am force-stop "$APP" || true
adb shell am start -W -n "$APP/.KeyboardVisibilityTestActivity"
sleep 8
adb exec-out screencap -p > /tmp/sboard-landscape.png
test -s /tmp/sboard-landscape.png
adb shell settings get secure default_input_method | grep -F "$IME"
echo "PASS: landscape keyboard launches and screenshot captured."

# Inspect only errors, never output sensitive input or clipboard content.
adb shell logcat -d -t 1800 -s AndroidRuntime:E > /tmp/sboard-androidruntime.log || true
if grep -F "Process: $APP" /tmp/sboard-androidruntime.log; then
  echo "FAIL: S Board crashed in Android runtime logs."
  exit 1
fi
echo "PASS: no S Board fatal AndroidRuntime crash in the test window."
