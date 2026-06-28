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
     && convert "$file" -resize 320x -quality 55 "$thumb" 2>/dev/null; then
    {
      echo "### ${idx}. ${caption}"
      echo ""
      echo "<img alt=\"${caption}\" width=\"300\" src=\"data:image/jpeg;base64,$(base64 -w0 "$thumb")\" />"
      echo ""
    } >> "$GITHUB_STEP_SUMMARY"
  else
    # No ImageMagick available — list the step; the image is in the artifact.
    echo "### ${idx}. ${caption} — see the \`ui-screenshots\` artifact" >> "$GITHUB_STEP_SUMMARY"
    echo "" >> "$GITHUB_STEP_SUMMARY"
  fi
  rm -f "$thumb"
}

swipe_up()    { adb shell input swipe "$CX" "$(pct_y 80)" "$CX" "$(pct_y 20)" 350; }
swipe_down()  { adb shell input swipe "$CX" "$(pct_y 18)" "$CX" "$(pct_y 75)" 350; }
swipe_left()  { adb shell input swipe "$(pct_x 85)" "$(pct_y 50)" "$(pct_x 15)" "$(pct_y 50)" 350; }
long_press()  { adb shell input swipe "$CX" "$(pct_y 45)" "$CX" "$(pct_y 46)" 900; }
press_home()  { adb shell input keyevent KEYCODE_HOME; }
press_back()  { adb shell input keyevent KEYCODE_BACK; }

# ---- install -----------------------------------------------------------------
echo "Installing $APK_PATH ..."
adb install -r -t "$APK_PATH"

# Make Launch0 the default home so HOME/back resolve to it.
adb shell cmd package set-home-activity "${APP_ID}/${MAIN_ACTIVITY}" || true

if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
  echo "## Launch0 UI walkthrough screenshots" >> "$GITHUB_STEP_SUMMARY"
  echo "" >> "$GITHUB_STEP_SUMMARY"
  echo "Captured on an Android emulator from the debug APK built in this run." >> "$GITHUB_STEP_SUMMARY"
  echo "" >> "$GITHUB_STEP_SUMMARY"
fi

# ---- walkthrough -------------------------------------------------------------

# 1. Home screen
adb shell monkey -p "$APP_ID" -c android.intent.category.HOME 1 >/dev/null 2>&1 || \
  adb shell am start -n "${APP_ID}/${MAIN_ACTIVITY}" >/dev/null 2>&1 || true
press_home
settle 3
shot 1 home "Home screen"

# 2. App drawer (swipe up)
press_home; settle 1
swipe_up
settle 2
shot 2 app-drawer "App drawer (swipe up)"

# 3. Search within the app drawer (type a query)
adb shell input text "settings"
settle 2
shot 3 app-search "App drawer search for \"settings\""

# 4. Settings (long-press on home)
press_home; settle 2
long_press
settle 2
shot 4 settings "Settings (long-press home)"

# 5. Notes page (swipe left)
press_back; settle 1
press_home; settle 1
swipe_left
settle 2
shot 5 notes "Notes page (swipe left)"

# back to a clean home for good measure
press_home; settle 1

echo "Done. Screenshots in $OUT_DIR:"
ls -la "$OUT_DIR"
