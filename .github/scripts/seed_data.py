#!/usr/bin/env python3
"""
Generate the two SharedPreferences XML files that pre-seed Launch0 with
realistic data for the CI screenshot walkthrough:

  * the main prefs file (app.launch0.xml)  — home apps, layout, theme, gestures,
    DND, and onboarding flags flipped off so no first-run dialogs/nudges hide the
    UI under test.
  * the notes prefs file (app.launch0.notes.xml) — a chat-like history of notes:
    plain text, a done to-do, an urgent reminder, an image and a voice memo.

Home apps are picked from the packages actually installed on the emulator (passed
in via a newline-delimited file) so every seeded slot resolves to a launchable
activity — otherwise HomeFragment would validate it away and blank the slot.

Usage:
    seed_data.py <out_main_xml> <out_notes_xml> <installed_pkgs_file> \
                 <now_ms> <image_path> <audio_path>

<image_path> may be empty to skip the image note (e.g. if ImageMagick is absent).
"""
import json
import sys
from xml.sax.saxutils import escape

# Preferred home-screen apps, best package first. Intersected with what's really
# installed so the home screen looks curated regardless of the system image.
PREFERRED = [
    ("Phone", ["com.google.android.dialer", "com.android.dialer"]),
    ("Messages", ["com.google.android.apps.messaging", "com.android.messaging"]),
    ("Chrome", ["com.android.chrome"]),
    ("Camera", ["com.android.camera2", "com.google.android.GoogleCamera"]),
    ("Photos", ["com.google.android.apps.photos", "com.android.gallery3d"]),
    ("Calendar", ["com.google.android.calendar", "com.android.calendar"]),
    ("Clock", ["com.google.android.deskclock", "com.android.deskclock"]),
    ("Calculator", ["com.android.calculator2", "com.google.android.calculator"]),
    ("Contacts", ["com.google.android.contacts", "com.android.contacts"]),
    ("Files", ["com.android.documentsui"]),
    ("Settings", ["com.android.settings"]),
    ("Maps", ["com.google.android.apps.maps"]),
    ("Gmail", ["com.google.android.gm"]),
    ("YouTube", ["com.google.android.youtube"]),
]


def select_home_apps(installed, want=6):
    iset = set(installed)
    chosen, used = [], set()
    for label, pkgs in PREFERRED:
        for p in pkgs:
            if p in iset:
                chosen.append([label, p])
                used.add(p)
                break
        if len(chosen) >= want:
            break
    # Fall back to other launchable packages if the image is unusually bare.
    if len(chosen) < want:
        for p in sorted(installed):
            if len(chosen) >= want:
                break
            if p in used or p.startswith("app.launch0"):
                continue
            label = p.rstrip(".").split(".")[-1].replace("_", " ").title()
            chosen.append([label, p])
            used.add(p)
    return chosen


def s(name, val):
    return f'    <string name="{escape(name)}">{escape(str(val))}</string>'


def i(name, val):
    return f'    <int name="{escape(name)}" value="{int(val)}" />'


def b(name, val):
    return f'    <boolean name="{escape(name)}" value="{"true" if val else "false"}" />'


def build_main(home_apps, theme=2, alignment=8388613, apps_num=None):
    if apps_num is None:
        apps_num = len(home_apps)
    lines = ["<?xml version='1.0' encoding='utf-8' standalone='yes' ?>", "<map>"]
    # Onboarding / first-run state: suppress dialogs and the "set default" prompt.
    lines += [
        b("FIRST_OPEN", False), b("FIRST_SETTINGS_OPEN", False), b("FIRST_HIDE", False),
        s("USER_STATE", "DONE"),
        b("KEYBOARD_MESSAGE", True), b("WALLPAPER_MSG_SHOWN", True),
        b("PRO_MESSAGE_SHOWN", True), b("HIDE_SET_DEFAULT_LAUNCHER", True),
    ]
    # Layout & appearance.
    lines += [
        i("HOME_APPS_NUM", apps_num),
        i("DATE_TIME_VISIBILITY", 1),       # On (clock + date)
        b("SHOW_YEAR_WIDGET", True),
        b("SHOW_APP_ICONS", True), b("SHOW_APP_NAMES", True),
        i("ICON_SIZE", 28), i("ICON_SHAPE", 0),
        i("HOME_ALIGNMENT", alignment),       # Gravity.END=8388613, CENTER=17, START=8388611
        b("HOME_BOTTOM_ALIGNMENT", True),
        b("AUTO_SHOW_KEYBOARD", False),       # keep the drawer list unobscured
        i("APP_THEME", theme),                # MODE_NIGHT_YES=2 (dark), MODE_NIGHT_NO=1 (light)
    ]
    # Gestures.
    lines += [
        i("SWIPE_LEFT_ACTION", 1),            # Notes
        i("SWIPE_DOWN_ACTION", 2),            # Notifications
        b("SWIPE_LEFT_ENABLED", True), b("SWIPE_RIGHT_ENABLED", True),
    ]
    # Do Not Disturb (so the Settings DND section reads as configured).
    lines += [b("DND_ENABLED", True), i("DND_DURATION_MINUTES", 60)]
    # Home app slots.
    for idx, (name, pkg) in enumerate(home_apps, start=1):
        lines += [s(f"APP_NAME_{idx}", name), s(f"APP_PACKAGE_{idx}", pkg), s(f"APP_USER_{idx}", "")]
    if home_apps:
        lines.append('    <set name="DND_APPS">')
        lines.append(f"        <string>{escape(home_apps[0][1])}</string>")
        lines.append("    </set>")
    lines.append("</map>")
    return "\n".join(lines) + "\n"


def build_notes(now_ms, image_path, audio_path):
    minute = 60_000

    def note(off_min, type_, text="", done=False, urgent=False, media="", dur=0):
        ts = now_ms - off_min * minute
        return {
            "id": ts, "type": type_, "text": text, "imagePath": media,
            "timestamp": ts, "done": done, "urgent": urgent, "duration": dur,
        }

    entries = [
        note(95, "text", "Welcome to Launch0 — your private, text-first home screen 👋"),
        note(80, "text", "Finish the Q3 launch checklist", done=True),
        note(64, "text", "Ideas: weekly review, read 20 pages a day, meditation streak"),
        note(47, "text", "Call the dentist back — appointment Friday 3pm", urgent=True),
        note(33, "text", "Grocery run: milk, eggs, spinach, coffee, oats"),
    ]
    if image_path:
        entries.append(note(22, "image", media=image_path))
    entries += [
        note(14, "text", "Standup: shipped notes search, voice playback next"),
        note(8, "audio", media=audio_path, dur=8000),
        note(2, "text", "Launch0 docs are live → launch0.app/docs"),
    ]
    blob = json.dumps(entries, ensure_ascii=False)
    return (
        "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n"
        f'    <string name="NOTES_ENTRIES">{escape(blob)}</string>\n</map>\n'
    )


def main():
    out_main, out_notes, installed_file, now_ms, image_path, audio_path = sys.argv[1:7]
    # Optional appearance overrides (used to re-seed for the variation screenshots).
    theme = int(sys.argv[7]) if len(sys.argv) > 7 else 2
    alignment = int(sys.argv[8]) if len(sys.argv) > 8 else 8388613
    apps_num = int(sys.argv[9]) if len(sys.argv) > 9 and sys.argv[9] else None
    with open(installed_file) as fh:
        installed = [ln.strip() for ln in fh if ln.strip()]
    home_apps = select_home_apps(installed)
    with open(out_main, "w") as fh:
        fh.write(build_main(home_apps, theme, alignment, apps_num))
    with open(out_notes, "w") as fh:
        fh.write(build_notes(int(now_ms), image_path, audio_path))
    # Echo the chosen home apps so the CI log records what was seeded.
    sys.stderr.write("Seeded home apps: " + ", ".join(f"{n} ({p})" for n, p in home_apps) + "\n")


if __name__ == "__main__":
    main()
