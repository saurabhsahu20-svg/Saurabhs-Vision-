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

# Rapid native screen taps must not lose letters while suggestions refresh.
# Use the same real key position tested above; do not fake text through adb.
start_count="$(grep -o 'Characters entered: [0-9]*' /tmp/sboard-tap-result.xml | head -1 | grep -o '[0-9]*' | tail -1)"
for _ in $(seq 1 14); do
  adb shell input tap 230 1335 >/dev/null
done
sleep 2
adb shell uiautomator dump /sdcard/sboard-fast-tap-result.xml >/dev/null 2>&1
adb pull /sdcard/sboard-fast-tap-result.xml /tmp/sboard-fast-tap-result.xml >/dev/null
end_count="$(grep -o 'Characters entered: [0-9]*' /tmp/sboard-fast-tap-result.xml | head -1 | grep -o '[0-9]*' | tail -1)"
echo "Fast tap count: baseline=$start_count, after=$end_count"
test "$((end_count-start_count))" -eq 14
echo "PASS: all 14 rapid screen taps appeared in the editor."

# Exercise microphone button with Android-granted permission.
# An emulator without an installed speech provider may report it unavailable;
# it must never crash or secretly start recording without user interaction.
adb shell pm grant "$APP" android.permission.RECORD_AUDIO
adb shell input tap 997 940 || true
sleep 3
adb shell settings get secure default_input_method | grep -F "$IME"
echo "PASS: microphone button handled on Android without losing the selected keyboard."

# The same rightmost ABC key must be a SEARCH action in search fields,
# NOT another '?123' toggle or a hard-coded newline.
adb shell input tap 245 413
sleep 3
adb shell input tap 1022 1695
sleep 2
adb shell uiautomator dump /sdcard/sboard-search-action.xml >/dev/null 2>&1
adb pull /sdcard/sboard-search-action.xml /tmp/sboard-search-action.xml >/dev/null
if ! grep -q 'Search actions: 1' /tmp/sboard-search-action.xml; then
  echo "FAIL: S Board right action did not trigger native IME_ACTION_SEARCH."
  grep -o 'Search actions: [0-9]*' /tmp/sboard-search-action.xml || true
  exit 1
fi
echo "PASS: right action key sends native Android Search in search fields."
adb shell input tap 245 267
sleep 1

# Switch to a small landscape window and verify the keyboard can still open.
# Force a wide logical display directly. Emulator user_rotation=1 can
# unexpectedly rotate that simulated display back into portrait mode.
adb shell wm size 1200x528
adb shell wm density 240
adb shell settings put system user_rotation 0 || true
# Never force-stop an app that is also the selected IME: Android can then
# switch to Gboard, invalidating the keyboard's landscape screenshot.
adb shell am start -W -n "$APP/.KeyboardVisibilityTestActivity"
sleep 8
# Tap INSIDE the EditText (the field begins at y≈140 on a 528px screen).
adb shell input tap 250 185
sleep 3
adb exec-out screencap -p > /tmp/sboard-landscape.png
test -s /tmp/sboard-landscape.png
python3 - <<'PY'
from pathlib import Path
import struct
b=Path('/tmp/sboard-landscape.png').read_bytes()
assert b.startswith(bytes((137,80,78,71,13,10,26,10))), 'Invalid screenshot PNG'
width,height=struct.unpack('>II',b[16:24])
print(f'Landscape image: {width}x{height}')
assert width>height, 'Screen did not rotate to landscape'
PY
adb shell settings get secure default_input_method | grep -F "$IME"
java vision-maths-science-keyboard/scripts/SBoardEmulatorScreenCheck.java /tmp/sboard-landscape.png
echo "PASS: S Board stays selected, and visible key panel rendered after rotation."

# Inspect only errors, never output sensitive input or clipboard content.
adb shell logcat -d -t 1800 -s AndroidRuntime:E > /tmp/sboard-androidruntime.log || true
if grep -F "Process: $APP" /tmp/sboard-androidruntime.log; then
  echo "FAIL: S Board crashed in Android runtime logs."
  exit 1
fi
echo "PASS: no S Board fatal AndroidRuntime crash in the test window."
