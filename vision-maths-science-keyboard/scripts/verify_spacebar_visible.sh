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

file=/tmp/sboard-window.xml
seen=false
for attempt in $(seq 1 12); do
  sleep 3
  adb shell uiautomator dump /sdcard/sboard-window.xml >/dev/null 2>&1 || true
  adb pull /sdcard/sboard-window.xml "$file" >/dev/null 2>&1 || true
  if [[ -s "$file" ]] && grep -q 'text="Space"' "$file"; then seen=true;break;fi
  echo "S Board not yet visible in accessibility hierarchy; retry ${attempt}/12"
done

adb exec-out screencap -p > /tmp/sboard-visual.png || true
if [[ "$seen" != true ]]; then
  echo "FAIL: Keyboard Spacebar was never observed by Android UiAutomator."
  exit 1
fi

python3 - "$file" <<'PY'
import re,struct,sys,xml.etree.ElementTree as ET
from pathlib import Path
raw=Path('/tmp/sboard-visual.png').read_bytes()
if raw[:8]!=b'\x89PNG\r\n\x1a\n':
    raise SystemExit('FAIL: emulator did not produce a PNG screenshot')
width,height=struct.unpack('>II',raw[16:24])
root=ET.parse(sys.argv[1]).getroot()
keynodes=[]
for node in root.iter('node'):
    label=node.attrib.get('text','').strip()
    desc=node.attrib.get('content-desc','').strip()
    bounds=node.attrib.get('bounds','')
    if (label in ('Space','q','a','z') or 'Spacebar - insert space' in desc) and bounds:
        pairs=re.findall(r'\[(\d+),(\d+)\]',bounds)
        if len(pairs)==2:
            x1,y1=map(int,pairs[0]);x2,y2=map(int,pairs[1])
            keynodes.append((label,desc,(x1,y1,x2,y2)))
print('EMU_SCREEN:',width,height)
for item in keynodes: print('KEY_BOUNDS:',item)
space=[x for x in keynodes if x[0]=='Space' or 'Spacebar - insert space' in x[1]]
if not space:raise SystemExit('FAIL: Space key not found in UiAutomator hierarchy')
if not all(any(x[0]==label for x in keynodes) for label in ('q','a','z')):
    raise SystemExit('FAIL: Q/A/Z letter rows missing or covered by accessibility')
bottom=max(x[2][3] for x in space)
# Android navigation buttons live at the bottom of the physical screen.
# On the user's 1201x528 landscape capture they begin at y~468.
# Make sure the WHOLE Space key—not just its top pixels—is above that zone.
safe_y=int(height*.90)
if bottom>safe_y:
    raise SystemExit(f'FAIL: Spacebar bottom {bottom}px overlaps last 10% navigation region (safe={safe_y}px)')
print('PASS: real Android emulator shows Q/A/Z letters and the COMPLETE Space key ABOVE the navigation area')
PY
