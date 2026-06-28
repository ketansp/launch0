#!/usr/bin/env bash
#
# UI screenshot driver for Launch0.
#
# Runs *inside* a booted Android emulator (invoked by the
# reactivecircus/android-emulator-runner step). It installs the freshly built
# debug APK, makes Launch0 the default home, then walks through a few screens —
# driving real gestures with `adb shell input` — and grabs a screenshot after
# each step.
#
# Output:
#   $OUT_DIR/NN-slug.png   full-resolution screenshots
#   $OUT_DIR/manifest.tsv  "<filename>\t<caption>" per line, for the publish
#                          step that renders them into the run's job summary.
set -euo pipefail

APP_ID="app.launch0.debug"
MAIN_ACTIVITY="app.launch0.MainActivity"
APK_PATH="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"
OUT_DIR="${OUT_DIR:-screenshots}"
MANIFEST="$OUT_DIR/manifest.tsv"

mkdir -p "$OUT_DIR"
: > "$MANIFEST"

# ---- screen geometry ---------------------------------------------------------
# Read the real device size so swipe coordinates are resolution-independent.
SIZE_LINE="$(adb shell wm size | tr -d '\r')"
W="$(echo "$SIZE_LINE" | grep -oE '[0-9]+x[0-9]+' | tail -1 | cut -dx -f1)"
H="$(echo "$SIZE_LINE" | grep -oE '[0-9]+x[0-9]+' | tail -1 | cut -dx -f2)"
W="${W:-1080}"
H="${H:-2340}"
CX=$(( W / 2 ))
echo "Emulator screen: ${W}x${H} (center x=${CX})"

# ---- helpers -----------------------------------------------------------------
pct_x() { echo $(( W * $1 / 100 )); }
pct_y() { echo $(( H * $1 / 100 )); }
settle() { sleep "${1:-2}"; }

current_focus() {
  adb shell dumpsys window 2>/dev/null | grep -m1 'mCurrentFocus' | tr -d '\r'
}

# shot <index> <slug> <caption>
shot() {
  local idx="$1" slug="$2" caption="$3" file
  file="$(printf '%02d-%s.png' "$idx" "$slug")"
  adb exec-out screencap -p > "$OUT_DIR/$file"
  printf '%s\t%s\n' "$file" "$caption" >> "$MANIFEST"
  echo "Captured: $file  ($caption)  [$(current_focus)]"
}

# Fast swipes: a slow `input swipe` decelerates at the end, so the terminal
# velocity falls under OnSwipeTouchListener's fling threshold and onSwipe* never
# fires. Keep the duration short so the gesture stays fast throughout.
swipe_up()   { adb shell input swipe "$CX" "$(pct_y 68)" "$CX" "$(pct_y 22)" 120; }
swipe_left() { adb shell input swipe "$(pct_x 92)" "$(pct_y 45)" "$(pct_x 8)" "$(pct_y 45)" 120; }
# Long-press: onLongPress fires at ~500ms, then a further 500ms delay before
# onLongClick(), so hold well past 1s.
long_press() { adb shell input swipe "$CX" "$(pct_y 38)" "$CX" "$(pct_y 38)" 1800; }

# Return to a clean home screen. Launch0 is the default home, so HOME resolves
# straight to it (no chooser) and, because MainActivity is singleTask, delivers
# a new MAIN intent whose onNewIntent() calls backToHomeScreen() — popping back
# to the home fragment from any sub-screen.
go_home() { adb shell input keyevent KEYCODE_HOME; settle 2; }

# ---- install -----------------------------------------------------------------
echo "Installing $APK_PATH ..."
adb install -r -t "$APK_PATH"

# Set Launch0 as the default home and give the system a moment to register it,
# so the very first HOME press doesn't pop the "Select a Home app" chooser.
echo "set-home-activity: $(adb shell cmd package set-home-activity "${APP_ID}/${MAIN_ACTIVITY}" 2>&1 | tr -d '\r')"
settle 3

# ---- walkthrough -------------------------------------------------------------

# 1. Home screen
go_home
shot 1 home "Home screen"

# 2. App drawer (swipe up)
swipe_up
settle 2
shot 2 app-drawer "App drawer (swipe up)"

# 3. Search within the app drawer (type a query)
adb shell input text "settings"
settle 2
shot 3 app-search "App drawer search for \"settings\""

# 4. Settings (long-press on empty area of home)
go_home
long_press
settle 2
shot 4 settings "Settings (long-press home)"

# 5. Notes page (swipe left)
go_home
swipe_left
settle 2
shot 5 notes "Notes page (swipe left)"

go_home

echo "Done. Screenshots in $OUT_DIR:"
ls -la "$OUT_DIR"
