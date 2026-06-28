#!/usr/bin/env bash
#
# UI screenshot driver for Launch0 — a verification walkthrough of every screen.
#
# Runs *inside* a booted Android emulator (invoked by the
# reactivecircus/android-emulator-runner step). It:
#   1. installs the freshly built debug APK and makes Launch0 the default home;
#   2. pre-seeds realistic data straight into the app's SharedPreferences — a
#      curated set of home apps (picked from packages actually installed on the
#      emulator) and a rich notes history (text, a to-do, an urgent reminder, an
#      image and a voice memo) — plus flips first-run flags off so no onboarding
#      dialog hides the UI;
#   3. drives real gestures with `adb shell input` and taps located against a
#      live `uiautomator dump` (resolution-independent), screenshotting every
#      screen, sub-menu and a couple of theme/layout variations.
#
# Output:
#   $OUT_DIR/NN-slug.png   full-resolution screenshots
#   $OUT_DIR/manifest.tsv  "<filename>\t<section>\t<caption>" per line, consumed
#                          by publish_screenshots.sh to build the run's job
#                          summary gallery.
#
# Note: no `set -e` — the walkthrough is best-effort. A tap that can't find its
# target logs a warning and we still capture whatever is on screen, so one flaky
# step never aborts the whole gallery. Critical setup steps are checked inline.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

APP_ID="app.launch0.debug"
MAIN_ACTIVITY="app.launch0.MainActivity"
DATA_DIR="/data/data/${APP_ID}"
APK_PATH="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"
OUT_DIR="${OUT_DIR:-screenshots}"
MANIFEST="$OUT_DIR/manifest.tsv"

IMG_DEST="${DATA_DIR}/files/notes_images/img_seed.png"
AUDIO_DEST="${DATA_DIR}/files/notes_audio/audio_seed.m4a"

mkdir -p "$OUT_DIR"
: > "$MANIFEST"

# ---- screen geometry ---------------------------------------------------------
SIZE_LINE="$(adb shell wm size | tr -d '\r')"
W="$(echo "$SIZE_LINE" | grep -oE '[0-9]+x[0-9]+' | tail -1 | cut -dx -f1)"
H="$(echo "$SIZE_LINE" | grep -oE '[0-9]+x[0-9]+' | tail -1 | cut -dx -f2)"
W="${W:-1080}"; H="${H:-2340}"
CX=$(( W / 2 ))
echo "Emulator screen: ${W}x${H} (center x=${CX})"

pct_x() { echo $(( W * $1 / 100 )); }
pct_y() { echo $(( H * $1 / 100 )); }
settle() { sleep "${1:-2}"; }

current_focus() { adb shell dumpsys window 2>/dev/null | grep -m1 'mCurrentFocus' | tr -d '\r'; }

SECTION="App"
section() { SECTION="$1"; echo; echo "===== $1 ====="; }

# shot <index> <slug> <caption>
shot() {
  local idx="$1" slug="$2" caption="$3" file
  file="$(printf '%02d-%s.png' "$idx" "$slug")"
  adb exec-out screencap -p > "$OUT_DIR/$file"
  printf '%s\t%s\t%s\n' "$file" "$SECTION" "$caption" >> "$MANIFEST"
  echo "Captured: $file  [$SECTION] $caption  [$(current_focus)]"
}

# ---- gestures ----------------------------------------------------------------
# Fast swipes: a slow `input swipe` decelerates at the end so the terminal
# velocity drops under OnSwipeTouchListener's fling threshold and onSwipe* never
# fires. Keep the duration short so the gesture stays fast throughout.
swipe_up()    { adb shell input swipe "$CX" "$(pct_y 68)" "$CX" "$(pct_y 22)" 120; }
swipe_left()  { adb shell input swipe "$(pct_x 92)" "$(pct_y 45)" "$(pct_x 8)" "$(pct_y 45)" 120; }
long_press()  { adb shell input swipe "$CX" "$(pct_y 38)" "$CX" "$(pct_y 38)" 1800; }
# A controlled (non-fling) drag, for scrolling lists/the settings page.
scroll_down() { adb shell input swipe "$CX" "$(pct_y 72)" "$CX" "$(pct_y 30)" 400; settle 1; }

# Return to a clean home screen. Launch0 is the default home, so HOME resolves
# straight to it and (MainActivity being singleTask) onNewIntent() pops back to
# the home fragment from any sub-screen.
go_home() { adb shell input keyevent KEYCODE_HOME; settle 2; }
back()    { adb shell input keyevent KEYCODE_BACK; settle 1; }

# ---- uiautomator-located taps ------------------------------------------------
UIX=""
ui_dump() {
  local i
  for i in 1 2 3; do
    if adb shell uiautomator dump /sdcard/ui_dump.xml >/dev/null 2>&1; then
      UIX="$(adb exec-out cat /sdcard/ui_dump.xml 2>/dev/null)"
      [ -n "$UIX" ] && return 0
    fi
    sleep 1
  done
  return 1
}
locate() { printf '%s' "$UIX" | python3 "$SCRIPT_DIR/find_node.py" "$@"; }

# tap <find_node args...> — refresh UI, locate, tap. Returns non-zero (logs) on miss.
tap() {
  ui_dump || { echo "  [tap] ui dump failed for: $*"; return 1; }
  local xy; xy="$(locate "$@")" || { echo "  [tap] not found: $*"; return 1; }
  echo "  tap ($*) -> $xy"
  adb shell input tap $xy; return 0
}
# longpress <find_node args...>
longpress() {
  ui_dump || { echo "  [longpress] ui dump failed for: $*"; return 1; }
  local xy; xy="$(locate "$@")" || { echo "  [longpress] not found: $*"; return 1; }
  echo "  longpress ($*) -> $xy"
  set -- $xy
  adb shell input swipe "$1" "$2" "$1" "$2" 1800; return 0
}
present() { ui_dump || return 1; locate "$@" >/dev/null 2>&1; }

# Scroll the current (settings) page until <text> appears, then it's tap-able.
scroll_to_text() {
  local text="$1" n=0
  while [ $n -lt 8 ]; do
    present text "$text" --contains && return 0
    scroll_down
    n=$((n+1))
  done
  present text "$text" --contains
}
type_text() { adb shell input text "$(echo "$1" | sed 's/ /%s/g')"; }

# =============================================================================
# 1. Install + make default home
# =============================================================================
echo "Installing $APK_PATH ..."
adb install -r -t "$APK_PATH" || { echo "APK install failed"; exit 1; }
echo "set-home-activity: $(adb shell cmd package set-home-activity "${APP_ID}/${MAIN_ACTIVITY}" 2>&1 | tr -d '\r')"
# Usage-access powers the on-home screen-time label and the Settings "Screen
# time" row; granting it exercises that wiring (best-effort).
adb shell appops set "$APP_ID" GET_USAGE_STATS allow >/dev/null 2>&1 || true

# A tidy, deterministic status bar for the screens that show one.
adb shell settings put global sysui_demo_allowed 1 >/dev/null 2>&1 || true
demo() { adb shell am broadcast -a com.android.systemui.demo "$@" >/dev/null 2>&1 || true; }
demo -e command enter
demo -e command clock -e hhmm 1041
demo -e command battery -e level 100 -e plugged false
demo -e command network -e wifi show -e level 4
demo -e command notifications -e visible false

# =============================================================================
# 2. Seed realistic data into the app's private storage
# =============================================================================
echo "Seeding SharedPreferences ..."
adb shell run-as "$APP_ID" mkdir -p shared_prefs files/notes_images files/notes_audio 2>/dev/null || true

# Discover packages that expose a launcher activity, so seeded home apps resolve.
# `--brief` prints flattened pkg/cls components; fall back to the verbose
# `packageName=` form, then to a known-good AOSP set, so we always get something.
INSTALLED="$(mktemp)"
adb shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER 2>/dev/null \
  | tr ' ,' '\n\n' | grep -oE '[a-zA-Z][a-zA-Z0-9_.]+/[a-zA-Z0-9_.$]+' | cut -d/ -f1 | sort -u > "$INSTALLED"
if [ ! -s "$INSTALLED" ]; then
  adb shell cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.LAUNCHER 2>/dev/null \
    | grep -oE 'packageName=[a-zA-Z0-9_.]+' | cut -d= -f2 | sort -u > "$INSTALLED"
fi
if [ ! -s "$INSTALLED" ]; then
  printf '%s\n' com.android.settings com.android.deskclock com.android.calculator2 \
    com.android.contacts com.android.documentsui com.android.camera2 com.android.dialer > "$INSTALLED"
fi
echo "Launchable packages found: $(wc -l < "$INSTALLED")"
sort "$INSTALLED" | head -40 | tr '\n' ' '; echo

# A sample image for the image note (ImageMagick is present on GitHub runners;
# fall back to no image note if it isn't).
HOST_IMG=""
if command -v convert >/dev/null 2>&1; then
  HOST_IMG="$(mktemp --suffix=.png)"
  convert -size 1000x720 gradient:'#0ea5e9'-'#7c3aed' \
    -gravity center -pointsize 54 -fill white \
    -annotate +0-30 'Trailhead' -pointsize 30 -annotate +0+40 'Sat 6:41am · 12°C' \
    "$HOST_IMG" 2>/dev/null \
    || convert -size 1000x720 xc:'#334155' "$HOST_IMG" 2>/dev/null \
    || HOST_IMG=""
fi
SEED_IMG_PATH=""; [ -n "$HOST_IMG" ] && SEED_IMG_PATH="$IMG_DEST"

# Build the two prefs XML files on the host.
NOW_MS="$(date +%s)000"
MAIN_XML="$(mktemp)"; NOTES_XML="$(mktemp)"
python3 "$SCRIPT_DIR/seed_data.py" "$MAIN_XML" "$NOTES_XML" "$INSTALLED" "$NOW_MS" "$SEED_IMG_PATH" "$AUDIO_DEST"

# Write everything in the app's own security context (piped through run-as) so
# there are no SELinux / ownership surprises.
adb shell run-as "$APP_ID" sh -c 'cat > shared_prefs/app.launch0.xml'        < "$MAIN_XML"
adb shell run-as "$APP_ID" sh -c 'cat > shared_prefs/app.launch0.notes.xml'  < "$NOTES_XML"
adb shell run-as "$APP_ID" sh -c ': > files/notes_audio/audio_seed.m4a'      || true
if [ -n "$HOST_IMG" ]; then
  base64 -w0 "$HOST_IMG" | adb shell run-as "$APP_ID" sh -c 'base64 -d > files/notes_images/img_seed.png' || true
fi
echo "Seed files written. shared_prefs now:"
adb shell run-as "$APP_ID" ls -la shared_prefs files/notes_images 2>&1 | tr -d '\r' || true

# =============================================================================
# 3. Walkthrough
# =============================================================================
adb shell am start -n "${APP_ID}/${MAIN_ACTIVITY}" >/dev/null 2>&1
settle 4
go_home

# ---- Home -------------------------------------------------------------------
section "Home screen"
shot 1 home "Home — clock, date, year-progress widget and your apps (dark theme, right-aligned)"

# ---- App drawer -------------------------------------------------------------
section "App drawer"
swipe_up; settle 2
shot 2 app-drawer "App drawer — every installed app as plain text, with the A–Z fast-scroll index"

# Live search: type a query, watch the list filter.
tap id search; settle 1
type_text "ca"; settle 2
shot 3 app-search "Search filters the drawer live as you type (\"ca\")"

# Reopen a fresh, unfiltered drawer for the long-press demo.
go_home; swipe_up; settle 2

# Long-press an app row → the per-app action menu (rename / hide / info / uninstall).
longpress id appTitle --index 1; settle 2
shot 4 app-menu "Long-press any app for actions: uninstall, rename, hide, app info"
# Open the inline rename editor.
tap text "Rename"; settle 2
shot 5 app-rename "Rename an app inline, without leaving the drawer"
back; settle 1; back; settle 1

# ---- Settings ---------------------------------------------------------------
section "Settings"
go_home; long_press; settle 2
shot 6 settings-home "Settings — Home screen section (apps count, date/time, widgets, icons)"

tap text "Apps on home screen" --contains; settle 1
shot 7 settings-apps-num "Pick how many apps (0–8) appear on the home screen"

scroll_to_text "Show date time" >/dev/null 2>&1
tap text "Show date time" --contains; settle 1
shot 8 settings-datetime "Date & time display: On / Off / Date only"

scroll_to_text "Icon shape" >/dev/null 2>&1
tap text "Icon shape" --contains; settle 1
shot 9 settings-icon-shape "Icon shapes — default, circle, square, squircle, teardrop"

scroll_to_text "App alignment" >/dev/null 2>&1
tap text "App alignment" --contains; settle 1
shot 10 settings-alignment "Home layout alignment — left / center / right and bottom toggle"

# Appearance section.
scroll_to_text "Theme mode" >/dev/null 2>&1
shot 11 settings-appearance "Appearance — keyboard, hourly wallpaper, status bar, theme, text size"
tap text "Theme mode" --contains; settle 1
shot 12 settings-theme "Theme — Light / Dark / System"

# Do Not Disturb section.
scroll_to_text "Hold duration" >/dev/null 2>&1
shot 13 settings-dnd "Do Not Disturb — hold notifications and release them on your terms"
tap text "Hold duration" --contains; settle 1
shot 14 settings-dnd-duration "How long to hold notifications — 30 / 45 / 60 / 90 / 120 / 180 min"

# Gestures section (revealed by tapping its header).
scroll_to_text "Gestures" >/dev/null 2>&1
tap text "Gestures"; settle 1
scroll_to_text "Swipe left for" >/dev/null 2>&1
shot 15 settings-gestures "Gestures — swipe and double-tap actions"
tap text "Swipe left for" --contains; settle 1
shot 16 settings-swipe-left "Swipe-left action — open Notes or launch an app"

# ---- Notes ------------------------------------------------------------------
section "Notes"
go_home; swipe_left; settle 2
shot 17 notes "Notes — a private chat with yourself: text, to-dos, an image and a voice memo"

# Per-note actions menu on a text note.
longpress text "Grocery run" --contains; settle 2
shot 18 notes-menu "Note actions — copy, share, edit, delete"
# Edit the note: input bar pre-fills and an editing banner appears.
tap text "Edit"; settle 2
shot 19 notes-edit "Editing a note inline — the banner shows you're editing"
tap desc "Cancel"; settle 1            # clear the editing banner

# Full-screen image viewer.
tap id notesImage; settle 2
shot 20 notes-image "Tap an image note to view it full screen"
tap id notesFullImage 2>/dev/null || adb shell input tap "$CX" "$(pct_y 50)"; settle 1

# Notes search.
tap desc "Search"; settle 2
tap id search 2>/dev/null || true        # focus the search field if not already
type_text "launch"; settle 2
shot 21 notes-search "Search your notes (\"launch\")"
back; settle 1

# ---- Variations -------------------------------------------------------------
# Flip the theme to light and re-alignment via the real Settings UI, then show
# the home screen reacting — exercising those controls end to end.
section "Variations"
go_home; long_press; settle 2
scroll_to_text "Theme mode" >/dev/null 2>&1
tap text "Theme mode" --contains; settle 1
tap text "Light"; settle 2
go_home
shot 22 home-light "Home — light theme (changed live from Settings)"

go_home; long_press; settle 2
scroll_to_text "App alignment" >/dev/null 2>&1
tap text "App alignment" --contains; settle 1
tap text "Center"; settle 1
scroll_to_text "Apps on home screen" >/dev/null 2>&1
tap text "Apps on home screen" --contains; settle 1
tap text "4"; settle 1                    # 4 of the 6 seeded apps — no blank slots
go_home
shot 23 home-center "Home — center-aligned app names (light theme)"

# Notes empty-state for completeness: clear seeded notes and reopen.
section "Notes"
adb shell run-as "$APP_ID" sh -c ': > shared_prefs/app.launch0.notes.xml' 2>/dev/null || true
printf "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map></map>\n" \
  | adb shell run-as "$APP_ID" sh -c 'cat > shared_prefs/app.launch0.notes.xml' || true
adb shell am force-stop "$APP_ID" >/dev/null 2>&1 || true
adb shell am start -n "${APP_ID}/${MAIN_ACTIVITY}" >/dev/null 2>&1; settle 3
go_home; swipe_left; settle 2
shot 24 notes-empty "Notes — empty state inviting your first jot"

go_home
echo
echo "Done. Screenshots in $OUT_DIR:"
ls -la "$OUT_DIR"
