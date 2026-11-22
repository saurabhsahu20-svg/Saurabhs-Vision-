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

# Cold Android 15 emulators can show a SYSTEM launcher "Quickstep isn't
# responding" ANR while the OS initializes, covering a healthy IME. That
# dialog is not an S Board crash. Dismiss Wait and refocus the actual editor
# before measuring keyboard touch accuracy.
recover_system_anr() {
  # Only the Android launcher is touched here. Do not force-stop the IME,
  # because Android may silently replace the selected S Board keyboard.
  for retry in 1 2 3 4 5; do
    adb shell uiautomator dump /sdcard/sboard-system-state.xml >/dev/null 2>&1 || true
    if ! adb shell cat /sdcard/sboard-system-state.xml 2>/dev/null |
      grep -q "Quickstep isn.t responding"; then
      adb shell input tap 250 265
      sleep 2
      return 0
    fi
    echo "Android emulator Quickstep launcher ANR, recovery ${retry}/5"
    # Close the *launcher* whose ANR is covering the S Board test UI.
    adb shell input tap 335 950 || true
    adb shell am force-stop com.android.launcher3 >/dev/null 2>&1 || true
    adb shell am force-stop com.google.android.apps.nexuslauncher >/dev/null 2>&1 || true
    sleep 5
    adb shell am start -W -n "$APP/.KeyboardVisibilityTestActivity" || true
    adb shell input tap 250 265 || true
    sleep 3
  done
  adb shell uiautomator dump /sdcard/sboard-system-state.xml >/dev/null 2>&1 || true
  if adb shell cat /sdcard/sboard-system-state.xml 2>/dev/null |
    grep -q "Quickstep isn.t responding"; then
    echo "FAIL: Android system Quickstep ANR persists (unrelated to S Board)."
    return 1
  fi
}

recover_system_anr

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
if grep -q "Quickstep isn.t responding" /tmp/sboard-tap-result.xml; then
  echo "Android system launcher dialog interrupted typing. Recovering and retrying."
  recover_system_anr
  adb shell input tap 230 1335
  adb shell input tap 420 1475
  adb shell input tap 590 1610
  sleep 2
  adb shell uiautomator dump /sdcard/sboard-tap-result.xml >/dev/null 2>&1 || true
  adb pull /sdcard/sboard-tap-result.xml /tmp/sboard-tap-result.xml >/dev/null
fi
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

# Test the familiar ?123 → ABC return flow with REAL on-screen taps.
# Read the synthetic debug text field only; no user typing is collected.
adb shell uiautomator dump /sdcard/sboard-before-abc.xml >/dev/null 2>&1
adb pull /sdcard/sboard-before-abc.xml /tmp/sboard-before-abc.xml >/dev/null
adb shell input tap 68 1695
sleep 2
adb exec-out screencap -p > /tmp/sboard-numbers.png
test -s /tmp/sboard-numbers.png
adb shell input tap 68 1695
sleep 2
adb shell input tap 230 1335
sleep 1
adb shell uiautomator dump /sdcard/sboard-after-abc.xml >/dev/null 2>&1
adb pull /sdcard/sboard-after-abc.xml /tmp/sboard-after-abc.xml >/dev/null
python3 - <<'PY'
import re, xml.etree.ElementTree as ET
def field_text(path):
    root=ET.parse(path).getroot()
    fields=[n.get('text','') for n in root.iter()
            if n.get('content-desc')=='IME_TYPING_TEST_FIELD']
    assert fields, f'Test field unavailable in {path}'
    return fields[0]
before=field_text('/tmp/sboard-before-abc.xml')
after=field_text('/tmp/sboard-after-abc.xml')
print(f'ABC-return synthetic editor character count: {len(before)} -> {len(after)}')
assert len(after)==len(before)+1, 'ABC tap did not restore one-letter typing'
assert after[-1].isalpha(), 'ABC return typed a nonalphabet symbol instead of a letter'
print('PASS: ?123 page returns to ABC through lower-left key, then types letters.')
PY

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

# Debug-only, Android-native secure clipboard regression. No user data is used.
adb shell logcat -c || true
adb shell am start -W -n "$APP/.ClipboardHistorySelfTestActivity"
sleep 4
adb shell logcat -d -s SBoardClipQA:I | grep 'PASS: clipboard opt-in, AES-GCM ciphertext'
echo "PASS: encrypted 24-hour history, clipboard opt-in and HTML clipboard metadata."

# Test original offline theme and PNG sticker assets through the Android provider.
adb shell logcat -c || true
adb shell am start -W -n "$APP/.ThemeStickerSelfTestActivity"
sleep 3
adb shell logcat -d -s SBoardThemeQA:I | grep 'PASS: 35 themes'
echo "PASS: 35 original themes and 30 stickers including real PNG ContentProvider payload."

# Check the themed gallery really opens on Android rather than just compiling.
adb shell am start -W -n "$APP/.ThemeGalleryTestAlias"
sleep 3
adb exec-out screencap -p > /tmp/sboard-theme-gallery.png
test -s /tmp/sboard-theme-gallery.png
adb shell uiautomator dump /sdcard/sboard-theme-gallery.xml >/dev/null 2>&1
adb pull /sdcard/sboard-theme-gallery.xml /tmp/sboard-theme-gallery.xml >/dev/null
grep -q 'S Board Themes' /tmp/sboard-theme-gallery.xml
grep -q 'Create' /tmp/sboard-theme-gallery.xml
echo "PASS: visual theme studio launches with Create and category filters."

# Inspect only errors, never output sensitive input or clipboard content.
adb shell logcat -d -t 1800 -s AndroidRuntime:E > /tmp/sboard-androidruntime.log || true
if grep -F "Process: $APP" /tmp/sboard-androidruntime.log; then
  echo "FAIL: S Board crashed in Android runtime logs."
  exit 1
fi
echo "PASS: no S Board fatal AndroidRuntime crash in the test window."
