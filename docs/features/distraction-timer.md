---
title: "Distraction timer for Android: pause before you open an app"
description: "Launch0's distraction timer puts a 10–60 second countdown before the apps you flag as distracting, so every open is a choice. Free, open source, no internet permission."
---

# Distraction Timer

<FeatureIntro image="/screenshots/distraction-timer.webp" alt="Launch0 distraction timer: 37 seconds until YouTube opens, with today's opens, screen time, last open and next wait">

Launch0's distraction timer puts a short countdown between you and the apps that pull you in. You flag the apps; Launch0 makes you wait a few seconds before they open. Most of the time, those seconds are enough to notice the habit and turn back.

It's built into the launcher itself, so there's no separate blocker app to install, no account, and no internet permission.

</FeatureIntro>

## How it works

1. Long-press the home screen to open **Settings**, then find **Distraction timer**.
2. Tap the number beside **Distracting apps** and tap each app you want to slow down.
3. Flagged apps look dimmed on the home screen, with their next wait shown beside them.

Now, opening a flagged app from Launch0 — a home-screen slot, a swipe gesture, or the app drawer (including search auto-launch) — shows a countdown first. While it runs, you see today's numbers for that app: **opens today**, **screen time**, **last open**, and the **next wait**. When it reaches zero, tap to open the app.

## Escalating or fixed waits

Tap the **Wait** value in Settings to choose between two modes:

| Mode | Wait |
|------|------|
| **Escalating** *(default)* | 10 seconds for the first open of the day, then 20, 40, and 60 seconds for every open after that |
| **Fixed** | 30 seconds for every open |

Open counts are kept per app and reset at midnight.

## Turning back costs nothing

Tap **Turn back**, press back or home, or switch away during the wait, and the timer resets. That open isn't counted, so changing your mind never makes the next wait longer.

## FAQ

### Does it block apps opened from notifications?

No. The timer applies to opens that go through Launch0: the home screen, gestures and the app drawer. Opening an app from a notification, the recent-apps screen, or a link in another app skips it. It's a speed bump, not a lock.

### Does it need any permissions?

The timer itself needs none. The **screen time** line on the wait screen uses usage access (Android 10 and later), the same permission as screen time on the home screen. Without it, that line shows a dash.

### Is any of this data sent anywhere?

No. The apps you flag, today's open counts and last-open times are stored in Launch0's private storage on your phone. Launch0 declares no internet permission, so it can't send them anywhere. See the [privacy policy](/privacy).
