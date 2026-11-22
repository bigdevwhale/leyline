#!/usr/bin/env bash
# Screenshots from the demo build (app.leyline.demo, central Moscow) into docs/screenshots/<lang>/.
#   ANDROID_SERIAL=<phone> tools/capture.sh ru|en
set -e
LANG_=${1:-ru}
cd "$(dirname "$0")/.."
export JAVA_HOME="${JAVA_HOME:-/c/Program Files/Android/Android Studio/jbr}"
./gradlew -q :app:assembleDebug -Pdemo -Pshots -Pdemolang="$LANG_"
PKG=app.leyline.demo
adb install -r app/build/outputs/apk/debug/app-debug.apk >/dev/null
adb shell svc power stayon usb
OUT=docs/screenshots/$LANG_; mkdir -p "$OUT"
shot() { # name tab [open]
  adb shell pm clear $PKG >/dev/null
  if [ -n "$3" ]; then adb shell am start -n $PKG/app.leyline.MainActivity --ei tab "$2" --es open "$3" >/dev/null
  else adb shell am start -n $PKG/app.leyline.MainActivity --ei tab "$2" >/dev/null; fi
  sleep 7
  # the demo switches its language once on first start: start again so the tab and sheet apply
  if [ -n "$3" ]; then adb shell am start -S -n $PKG/app.leyline.MainActivity --ei tab "$2" --es open "$3" >/dev/null
  else adb shell am start -S -n $PKG/app.leyline.MainActivity --ei tab "$2" >/dev/null; fi
  sleep ${WAIT:-8}
  adb exec-out screencap -p > "$OUT/$1.png"
  echo "$OUT/$1.png"
}
if [ -n "$ONLY" ]; then eval "$ONLY"; else
shot 1-tower 0
shot 2-map 1
shot 3-cauldron 2
shot 4-stable 3
shot 5-trip 0 trip
shot 6-spirit 0 spirit:water
shot 7-recipes 2 recipes
shot 8-cozy 0 cozy
fi
adb shell svc power stayon false
