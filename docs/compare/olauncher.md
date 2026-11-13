---
title: "Launch0 vs Olauncher: minimalist Android launchers compared"
titleTemplate: false
description: "Launch0 is an open-source fork of Olauncher. Both are text-only Android launchers; Launch0 adds notes, a distraction timer, held notifications and a calendar widget."
---

# Launch0 vs Olauncher

Launch0 is a fork of [Olauncher](https://github.com/tanujnotes/Olauncher), the minimal text-only Android launcher by [tanujnotes](https://github.com/tanujnotes). Both keep your home screen to a short list of app names, and both are free and open source under GPLv3. Launch0 builds an "OS for the busy" on that base: notes, focus tools and your day's schedule, one gesture from home, without the internet permission.

## At a glance

| | Launch0 | Olauncher |
|---|---|---|
| Text-only home screen | Yes, up to 8 apps | Yes |
| License | GPLv3 | GPLv3 |
| Internet permission | **Not declared** — the app can't connect to the network | Declared, "used only for downloading wallpapers" |
| Wallpapers | Generated on your device, a new one every hour | A new one downloaded daily |
| Notes & to-dos | [Built in](/features/notes): text, to-dos, voice memos, images | Not listed¹ |
| Delay before distracting apps | [Distraction timer](/features/distraction-timer), 10–60 s | Not listed¹ |
| Hold notifications from chosen apps | [Built in](/features/hold-notifications), 30 min – 3 h | Not listed¹ |
| Today's calendar on the home screen | [Calendar widget](/features/calendar-widget) | No — its manifest requests no calendar permission |
| Optional app icons in the drawer | Yes, with size and shape options | Not listed¹ |
| Double tap to lock, swipe up for apps, swipe down for notifications | Yes | Yes |
| Where to get it | [Google Play](https://play.google.com/store/apps/details?id=app.launch0), [GitHub APK](/download#apk) | F-Droid, Google Play, IzzyOnDroid, GitHub |
| Ads | None | None |

¹ Not mentioned in Olauncher's README or F-Droid listing as of 13 November 2026. Olauncher says some features are hidden and listed on its in-app About page, so check there before deciding.

## What's the same

Launch0 keeps the foundation it forked from Olauncher: a calm, text-only home screen, instant app search that auto-launches the top match, rename and hide apps, gestures, dark and light themes, and work-profile support. All credit for that foundation goes to the Olauncher team.

## What Launch0 adds

- **[Notes & to-dos](/features/notes)** — a private chat with yourself, one swipe left from home.
- **[Distraction timer](/features/distraction-timer)** — a countdown before the apps you flag, longer with every open that day.
- **[Hold notifications](/features/hold-notifications)** — park notifications from chosen apps and get them in one batch.
- **[Calendar widget](/features/calendar-widget)** — today's remaining events, with a countdown to the next one.
- **No internet permission** — wallpapers are generated on your device, so nothing needs the network. See the [privacy policy](/privacy).

## Which should you pick?

Pick **Launch0** if you want your launcher to also hold your notes, slow down your distracting apps and show what's next today, and you want a launcher that can't reach the internet at all.

Pick **Olauncher** if you want the original project, or you install apps only through F-Droid. Launch0 isn't on F-Droid yet.

## FAQ

### Can I move my settings from Olauncher to Launch0?

No. They're separate apps with separate package names, so Launch0 starts fresh. Setting up takes a minute: choose your home apps, gestures and theme in **Settings**.

### Is Launch0 made by the Olauncher developer?

No. Launch0 is an independent fork, maintained separately. It's licensed under GPLv3 like Olauncher, and its source is on [GitHub](https://github.com/ketansp/launch0).

### Can I run both?

Yes. Both can be installed at the same time; Android asks which one to use as your home app, and you can switch in your phone's default-apps settings.
