---
title: "Features: notes, distraction timer, focus tools"
description: "Everything in Launch0: a text-only home screen, notes and to-dos, a distraction timer, held notifications, a calendar widget, screen time and wallpapers."
---

# Features

Launch0 is an OS for the busy: a free, open-source minimalist launcher for Android with built-in notes, to-dos, a distraction timer, held notifications and screen time. It replaces your home screen with a fast, text-only command layer. Everything below is built in — no accounts, no add-ons, and no internet connection required.

<FeatureCards :items="[
  { icon: '⏳', title: 'Distraction Timer', text: 'A short countdown before the apps that pull you in.', link: '/features/distraction-timer' },
  { icon: '📝', title: 'Notes & To-Dos', text: 'A private chat with yourself, one swipe from home.', link: '/features/notes' },
  { icon: '🔕', title: 'Hold Notifications', text: 'Park notifications from chosen apps and get them in one batch.', link: '/features/hold-notifications' },
  { icon: '📅', title: 'Calendar Widget', text: 'The rest of today on your home screen, with a countdown.', link: '/features/calendar-widget' },
]" />

## Home Screen

- **Text-based app shortcuts** — pin 0 to 8 apps as plain text for instant, icon-free access. Long-press a slot to assign or change its app.
- **Date, time & battery** — a `time` line and a `date` line (which also shows your current battery level). Tap the time to open your clock app and the date to open your calendar; long-press either to choose which app it launches. You can show the time and date, show the **date only**, or hide both.
- **Days-left widget** — an optional grid that visualizes how much of the current year remains.
- **Calendar widget** — today's remaining events under the clock, with a countdown to the next one. See [Calendar Widget](#calendar-widget).
- **Widget cards** — when more than one widget is on (calendar, days left, and the "On hold" card while notifications are held), they share one card under the clock that you swipe between.
- **Screen time** — see how long you've spent in each home app today, shown as a small capsule beside the app's name. It appears once an app has been used for at least 5 minutes and needs usage-access permission (Android 10+).
- **Held-notification pill** — when Do Not Disturb is holding notifications for an app, a count appears next to it; tap it to release them immediately.
- **Alignment** — align home text to the left, center, or right, with an optional **bottom** position (available once Launch0 is your default launcher).

## Notes

More detail: [Notes & to-dos](/features/notes).

Launch0 includes a built-in **Notes** page — like a private chat with yourself. Swipe left on the home screen to open it (the default swipe-left action), and swipe left again inside Notes to search. Everything is stored locally on your device.

- **Text notes** — type and send a thought, link, or reminder, messaging-app style. A running count of your entries is shown at the top.
- **To-dos** — mark any note as **done** (with a checkbox) or flag it as **urgent** to make it stand out.
- **Voice notes** — record an audio memo, then play, pause, and resume it inline. Each shows its duration. Requires microphone permission.
- **Images** — attach an image from the picker, or **share an image into Notes** from any other app via Android's share sheet. Tap an image to view it full-screen.
- **Clickable links** — URLs and email addresses in your notes become tappable automatically.
- **Message actions** — long-press any note to **copy**, **share**, **edit**, or **delete** it.
- **Date dividers** — notes are grouped by day with Today / Yesterday labels and full dates for older entries.
- **Search** — a dedicated search page filters your notes as you type, with results anchored next to the keyboard for quick reach.

## App Drawer

- **Instant search** — swipe up and start typing to filter your apps. The top match launches automatically; begin your query with a **space** if you'd rather it didn't auto-launch.
- **Smart ranking** — apps whose names *start* with your query rank above mid-word matches, so the app you mean is usually first.
- **Alphabet fast-scroll** — an A–Z index down the edge of the list lets you jump straight to a letter.
- **App icons** — optionally show app icons in the drawer, with an adjustable **size** (16–48 dp) and **shape** (default, circle, square, squircle, or teardrop).
- **Icons-only or names-only** — hide app names for a compact icon grid, or hide icons for the classic text list. (You can't hide both.)
- **Screen time** — see today's per-app usage right in the drawer, alongside any held-notification count.
- **Rename apps** — give any app a custom label that makes sense to you.
- **Hide apps** — remove apps from the drawer without uninstalling them; unhide them later from **Settings → hidden apps**.
- **App info & uninstall** — long-press an app for quick access to its system info or to uninstall it (system apps can be hidden but not removed).
- **Pinned shortcuts** — app shortcuts appear alongside apps and can be launched or removed.
- **Web search** — start your query with `!` to search the web instead of your apps.
- **Work profile aware** — work-profile and dual-app entries are listed and launched correctly.

## Gestures

| Gesture | Action |
|---------|--------|
| Swipe up | Open the app drawer |
| Swipe left | Open Notes *(default)* or a favorite app |
| Swipe right | Open a favorite app |
| Swipe down | Open notifications *(default)* or search |
| Double tap | Lock the screen |
| Long press | Open settings |

- **Configurable** — the swipe-left action (Notes or an app) and the swipe-down action (notifications or search) are set in **Settings → Gestures**. You can assign apps to swipe-left and swipe-right, and long-press those rows to enable or disable them.
- **Double tap to lock** — locks the screen instantly. On Android 9+ this uses an accessibility service; on older versions it uses the device-admin API. Either way it's optional and off by default.

## Do Not Disturb

Hold back notifications from distracting apps so you can focus, then get them all at once when you're ready.

- **Hold notifications** — choose which apps' notifications get parked instead of interrupting you.
- **Choose a duration** — hold for 30, 45, 60, 90, 120, or 180 minutes.
- **Release on your terms** — a count pill appears beside the app on the home screen and in the drawer; tap it to release the held notifications, or let them arrive automatically when the window ends.
- **One batch, not a drip** — the first held notification starts the window; everything that arrives from your chosen apps during it is released together when it ends.
- **Requires notification access**, which you grant from Settings, and Android 8.0 or later. Ongoing notifications such as calls and music are never held.

More detail: [Hold notifications](/features/hold-notifications).

## Distraction Timer

Flag the apps that pull you in, and Launch0 puts a short wait between you and them.

- **Pick your distracting apps** in **Settings → Distraction timer** (tap the number beside **Distracting apps**). They look dimmed on the home screen, with their next wait time beside them.
- **A pause before the pull** — opening a flagged app from Launch0 — the home screen, a swipe gesture or the app drawer — shows a countdown along with today's opens, screen time, last open and the next wait.
- **Escalating or fixed** — waits start at 10 seconds and double with each open that day, up to 60 seconds (10, 20, 40, 60), or stay at a fixed 30 seconds — tap the **Wait** value to switch. Counts reset at midnight.
- **Turn back anytime** — tapping **Turn back**, or leaving mid-wait, resets the timer, and that open isn't counted.

More detail: [Distraction timer](/features/distraction-timer).

## Calendar Widget

See the rest of today without opening another app. Turn it on with **Calendar widget** in **Settings**.

- **Today's events on your home screen**, each with its start time, location and a countdown ("in 15m", "in 1h 5m"), or **now** while it's happening.
- **Only what's left** — events drop off once they end. Cancelled and declined events never show, and all-day entries such as holidays and working locations are left out.
- **Tap through** — tap an event to open it in your calendar app, or tap **Today** to open the calendar.
- **Work calendars too** — if your work profile shares its calendar, those events appear alongside your personal ones.
- **Stays on your device** — Launch0 reads events with calendar permission and can't send them anywhere, because it has no internet permission.

More detail: [Calendar widget](/features/calendar-widget).

## Appearance

- **Themes** — dark, light, or follow the system setting.
- **Text size** — scale text across the app from 0.5× to 1.5× (up to 2.0× on tablets) in small steps.
- **App icons** — toggle icons in the drawer and set their size and shape (see [App Drawer](#app-drawer)).
- **Status bar** — show or hide the system status bar.
- **Auto-show keyboard** — automatically raise the keyboard when you open the app drawer, so you can start typing right away.
- **Auto-generated wallpapers** — turn on a fresh, abstract gradient wallpaper that regenerates every hour, created entirely on your device (no download, no internet). It matches your light/dark theme and works while Launch0 is your default launcher.

## Localization

Launch0 is translated into 20 languages, including English, Arabic, German, Spanish, French, Hebrew, Croatian, Hungarian, Indonesian, Italian, Japanese, Dutch, Polish, Portuguese (Brazil), Russian, Swedish, Turkish, Ukrainian, and Chinese.

## Privacy

- No ads
- No data collection
- No tracking
- **Works fully offline** — Launch0 doesn't even request internet permission
- Notes, voice memos, and images stay on your device
- Open source under GPLv3

## Work Profile

Launch0 supports Android work profiles and dual apps, so your work and personal apps are listed, labeled, and launched correctly throughout the launcher.
