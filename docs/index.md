---
layout: home

hero:
  name: Launch0
  text: OS for the busy
  tagline: A fast, text-only home screen that becomes the command layer for your phone — launch apps, capture notes, hold distractions, and track your time. No ads, no data collection.
  actions:
    - theme: brand
      text: Download now
      link: https://play.google.com/store/apps/details?id=app.launch0
    - theme: alt
      text: Explore features
      link: /features/
    - theme: alt
      text: View on GitHub
      link: https://github.com/ketansp/Olauncher
  image:
    src: /hero.svg
    alt: Launch0 Logo
    width: 320
    height: 320

features:
  - icon: ✨
    title: Minimal by Design
    details: A calm, text-first home screen with nothing to scroll and nothing to nag you. Just the apps you need, ready to launch.
    link: /features/home-screen
    linkText: Learn more
  - icon: ⚡
    title: Your Command Center
    details: Instant search, gesture shortcuts, notes, and screen time tracking put your whole day in one place.
    link: /features/search
    linkText: Learn more
  - icon: 📝
    title: Notes & To-Dos
    details: A private chat with yourself — text, to-dos, voice memos, and images, one swipe from home.
    link: /features/notes
    linkText: Learn more
  - icon: 🔕
    title: Do Not Disturb
    details: Hold notifications from distracting apps and release them when you're ready to focus.
    link: /features/do-not-disturb
    linkText: Learn more
  - icon: 🔒
    title: Privacy-First
    details: No data collection, no ads, no tracking. Launch0 has no internet permission at all, so it works fully offline.
    link: /privacy
    linkText: Learn more
  - icon: 🌐
    title: Open Source
    details: Free and open source under the GPLv3 license. Fork of the original Olauncher.
    link: /contributing
    linkText: Learn more
---

<script setup>
import HomePage from "./.vitepress/components/HomePage/HomePage.vue";
</script>

<HomePage>
<template v-slot:setup-steps>

1. **Install** Launch0 from [Google Play](https://play.google.com/store/apps/details?id=app.launch0) — or grab the [APK](/download#apk)
2. **Set as default** — tap "Set as default launcher" in settings
3. **Swipe up** for all apps, **swipe left** for Notes, **long press** for settings
4. **Start typing** an app name in the drawer to auto-launch it

</template>
<template v-slot:gestures>

| Gesture | Action |
|---------|--------|
| Swipe up | Open app drawer |
| Swipe left | Open Notes (or an app) |
| Swipe right | Open your quick-launch app |
| Swipe down | Open notifications |
| Double tap | Lock the screen (optional) |
| Long press | Open settings |

</template>
<template v-slot:features-list>

* [Text-first home screen](/features/home-screen)
* [Notes & to-dos](/features/notes)
* [Voice notes](/features/notes)
* [Instant app search](/features/search)
* [Auto-launch search](/features/search)
* [Alphabet fast-scroll](/features/app-drawer)
* [Distraction timer](/features/distraction-timer)
* [Hold notifications](/features/do-not-disturb)
* [Screen time tracking](/features/screen-time)
* [Days-left widget](/features/widgets)
* [Calendar widget](/features/widgets)
* [Gesture shortcuts](/features/gestures)
* [Double tap to lock](/features/gestures)
* [Dark & light themes](/features/appearance)
* [Hourly wallpapers](/features/appearance)
* [Customizable text size](/features/appearance)
* [Optional app icons](/features/app-drawer)
* [Rename apps](/features/app-drawer)
* [Hide apps](/features/app-drawer)
* [Pinned shortcuts](/features/app-drawer)
* [Work profile support](/features/work-profile)
* [App alignment options](/features/home-screen)
* [Date, time & battery](/features/home-screen)
* [Works fully offline](/privacy)

</template>
<template v-slot:notes>

Swipe left from home to open Notes — a private, chat-like space for text, to-dos, voice memos, and images. Mark items done or urgent, tap links, and search everything you've saved. It's stored in Launch0's private storage on your phone, and Launch0 has no internet access to send it anywhere.

[Learn more about Notes →](/features/notes)

</template>
<template v-slot:do-not-disturb>

Pick the apps that pull you away and Launch0 will hold their notifications for 30 minutes to 3 hours. A count appears on the home screen — release them with a tap, or let them arrive when your focus window ends.

[Learn more about Do Not Disturb →](/features/do-not-disturb)

</template>
<template v-slot:distraction-timer>

Mark the apps that pull you in, and Launch0 shows a short countdown before they open. By default the wait doubles with every open that day, from 10 seconds up to 60, and one tap on **Turn back** takes you home without counting the open. A small pause that turns a reflex into a choice.

[Learn more about the Distraction timer →](/features/distraction-timer)

</template>
<template v-slot:screen-time>

Launch0 shows your daily screen time right on the home screen — no extra apps needed. See how long you've spent in each app today and make intentional choices about where your time goes.

[Learn more about screen time →](/features/screen-time)

</template>
<template v-slot:wallpapers>

A fresh, abstract wallpaper every hour — generated right on your device, with no downloads and no internet. It matches your light or dark theme and keeps your minimal home screen feeling new.

[Learn more about wallpapers →](/features/appearance)

</template>
<template v-slot:privacy>

Launch0 needs no account, sends no data, and works **fully offline** — it doesn't even request internet permission. Launch0 can't send your notes, voice memos, or images anywhere. The entire codebase is open for inspection under the [GPLv3 license](https://www.gnu.org/licenses/gpl-3.0.en.html).

[Read the privacy policy →](/privacy)

</template>
</HomePage>
