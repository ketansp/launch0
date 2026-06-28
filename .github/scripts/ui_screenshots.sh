#!/usr/bin/env bash
#
# UI screenshot driver for Launch0.
#
# Runs *inside* a booted Android emulator (invoked by the
# reactivecircus/android-emulator-runner step). It installs the freshly built
# debug APK, makes Launch0 the default home launcher, then walks through a few
# screens — driving real gestures with `adb shell input` — and grabs a
# screenshot after each step.
#
# Every screenshot is:
#   1. saved to $OUT_DIR (uploaded as a workflow artifact), and
#   2. embedded inline (base64) into the GitHub Actions job summary so you can
#      eyeball the run straight from the run page.
#
# The plain text console log can't render images, so the job summary + the
# downloadable artifact are how the screenshots are "shown" for the run.
set -euo pipefail

APP_ID="app.launch0.debug"
MAIN_ACTIVITY="app.launch0.MainActivity"
APK_PATH="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"
OUT_DIR="${OUT_DIR:-screenshots}"

mkdir -p "$OUT_DIR"

# ---- screen geometry ---------------------------------------------------------
# Read the real device size so swipe coordinates are resolution-independent.
SIZE_LINE="$(adb shell wm size | tr -d '\r')"
W="$(echo "$SIZE_LINE" | grep -oE '[0-9]+x[0-9]+' | tail -1 | cut -dx -f1)"
H="$(echo "$SIZE_LINE" | grep -oE '[0-9]+x[0-9]+' | tail -1 | cut -dx -f2)"
W="${W:-1080}"
H="${H:-1920}"
CX=$(( W / 2 ))
echo "Emulator screen: ${W}x${H} (center x=${CX})"

# ---- helpers -----------------------------------------------------------------
pct_x() { echo $(( W * $1 / 100 )); }
pct_y() { echo $(( H * $1 / 100 )); }

settle() { sleep "${1:-2}"; }

# screenshot <index> <slug> <caption>
#
# Saves a full-resolution PNG (collected into the uploaded artifact) and, for
# the job summary, embeds a small downscaled JPEG thumbnail. The summary is
# capped at 1 MB total by GitHub, so full PNGs (a few MB each) can't be
# embedded inline — the thumbnail keeps every screenshot visible on the run
# page while the artifact carries the originals.
shot() {
  local idx="$1" slug="$2" caption="$3"
  local file thumb
  file="$(printf '%s/%02d-%s.png' "$OUT_DIR" "$idx" "$slug")"
  adb exec-out screencap -p > "$file"
  echo "Captured: $file  ($caption)"

  [[ -n "${GITHUB_STEP_SUMMARY:-}" ]] || return 0

  thumb="$(mktemp --suffix=.jpg)"
  if command -v convert >/dev/null 2>&1 \
     && convert "$file" -resize 480x -quality 60 "$thumb" 2>/dev/null; then
    {
      echo "### ${idx}. ${caption}"
      echo ""
      echo "<img alt=\"${caption}\" width=\"320\" src=\"data:image/jpeg;base64,$(base64 -w0 "$thumb")\" />"
      echo ""
    } >> "$GITHUB_STEP_SUMMARY"
  else
    # No ImageMagick available — list the step; the image is in the artifact.
    echo "### ${idx}. ${caption} — see the \`ui-screenshots\` artifact" >> "$GITHUB_STEP_SUMMARY"
    echo "" >> "$GITHUB_STEP_SUMMARY"
  fi
  rm -f "$thumb"
}

# Gestures. Swipes start/end in the empty middle band of the screen so they
# land on the home layout's gesture listener rather than on a home-app row, and
# clear the 100px / 100vel fling threshold in OnSwipeTouchListener.
swipe_up()    { adb shell input swipe "$CX" "$(pct_y 70)" "$CX" "$(pct_y 20)" 200; }
swipe_left()  { adb shell input swipe "$(pct_x 90)" "$(pct_y 45)" "$(pct_x 10)" "$(pct_y 45)" 200; }
long_press()  { adb shell input swipe "$CX" "$(pct_y 40)" "$CX" "$(pct_y 40)" 1000; }

# Return to a clean home screen. MainActivity is singleTask and its onNewIntent
# calls backToHomeScreen(), so re-launching it reliably pops back to the home
# fragment from anywhere — without triggering the system "Select a Home app"
# chooser that the HOME key would.
return_home() {
  adb shell am start -n "${APP_ID}/${MAIN_ACTIVITY}" >/dev/null 2>&1 || true
  settle 2
}

# ---- install -----------------------------------------------------------------
echo "Installing $APK_PATH ..."
adb install -r -t "$APK_PATH"

# Make Launch0 the default home too (best-effort; the walkthrough uses am start
# so it doesn't depend on this succeeding).
echo "set-home-activity: $(adb shell cmd package set-home-activity "${APP_ID}/${MAIN_ACTIVITY}" 2>&1 | tr -d '\r')"

if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
  {
    echo "## Launch0 UI walkthrough screenshots"
    echo ""
    echo "Captured on an Android emulator from the debug APK built in this run."
    echo "No download needed — the screenshots are embedded below."
    echo ""
  } >> "$GITHUB_STEP_SUMMARY"
fi

# ---- walkthrough -------------------------------------------------------------

# 1. Home screen
return_home
settle 1
echo "focus: $(adb shell dumpsys window 2>/dev/null | grep -m1 'mCurrentFocus' | tr -d '\r')"
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
return_home
long_press
settle 2
shot 4 settings "Settings (long-press home)"

# 5. Notes page (swipe left)
return_home
swipe_left
settle 2
shot 5 notes "Notes page (swipe left)"

# back to a clean home for good measure
return_home

echo "Done. Screenshots in $OUT_DIR:"
ls -la "$OUT_DIR"
