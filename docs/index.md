---
layout: home

hero:
  name: Launch0
  text: Operating System for the busy
  tagline: A fast, text-only home screen that becomes the command layer for your phone — launch apps, capture notes, hold distractions, and track your time. No ads, no data collection.
  actions:
    - theme: brand
      text: Get Started
      link: /guide/
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
    details: A clean, text-based home screen with no icons or distractions. Just the apps you need, ready to launch.
    link: /guide/features#home-screen
    linkText: Learn more
  - icon: ⚡
    title: Your Command Center
    details: Instant search, gesture shortcuts, notes, and screen time tracking put your whole day in one place.
    link: /guide/features#gestures
    linkText: Learn more
  - icon: 📝
    title: Notes & To-Dos
    details: A private chat with yourself — text, to-dos, voice memos, and images, one swipe from home.
    link: /guide/features#notes
    linkText: Learn more
  - icon: 🔕
    title: Do Not Disturb
    details: Hold notifications from distracting apps and release them when you're ready to focus.
    link: /guide/features#do-not-disturb
    linkText: Learn more
  - icon: 🔒
    title: Privacy-First
    details: No data collection, no ads, no tracking. Works fully offline — everything stays on your device.
    link: /privacy
    linkText: Learn more
  - icon: 🌐
    title: Open Source
    details: Free and open source under the GPLv3 license. Fork of the original Olauncher.
    link: /guide/contributing
    linkText: Learn more
---

<script setup>
import HomePage from "./.vitepress/components/HomePage/HomePage.vue";
</script>

<HomePage>
<template v-slot:setup-steps>

1. **Install** Launch0 from the Play Store or [build from source](/guide/build)
2. **Set as default** — tap "Set as default launcher" in settings
3. **Swipe up** for all apps, **swipe left** for Notes, **long press** for settings
4. **Start typing** an app name in the drawer to auto-launch it

</template>
<template v-slot:gestures>

| Gesture | Action |
|---------|--------|
| Swipe up | Open app drawer |
| Swipe left | Open Notes (or an app) |
| Swipe right | Open right app |
| Swipe down | Notifications or search |
| Double tap | Lock screen |
| Long press | Open settings |

</template>
<template v-slot:features-list>

* [Text-based home screen](/guide/features#home-screen)
* [Notes & to-dos](/guide/features#notes)
* [Voice notes](/guide/features#notes)
* [Instant app search](/guide/features#app-drawer)
* [Alphabet fast-scroll](/guide/features#app-drawer)
* [Gesture shortcuts](/guide/features#gestures)
* [Screen time tracking](/guide/features#home-screen)
* [Do Not Disturb](/guide/features#do-not-disturb)
* [Days-left widget](/guide/features#home-screen)
* [Dark & light themes](/guide/features#appearance)
* [Auto-generated wallpapers](/guide/features#appearance)
* [Optional app icons](/guide/features#app-drawer)
* [Rename apps](/guide/features#app-drawer)
* [Hide apps](/guide/features#app-drawer)
* [Work profile support](/guide/features#work-profile)
* [Double tap to lock](/guide/features#gestures)
* [Customizable text size](/guide/features#appearance)
* [Status bar toggle](/guide/features#appearance)
* [App alignment options](/guide/features#home-screen)
* [Configurable gestures](/guide/features#gestures)
* [Auto-launch search](/guide/features#app-drawer)
* [Date, time & battery](/guide/features#home-screen)
* [Works fully offline](/privacy)
* [20 languages](/guide/features#home-screen)

</template>
<template v-slot:notes>

Swipe left from home to open Notes — a private, chat-like space for text, to-dos, voice memos, and images. Mark items done or urgent, tap links, and search everything you've saved. It all stays on your device.

[Learn more about Notes →](/guide/features#notes)

</template>
<template v-slot:do-not-disturb>

Pick the apps that pull you away and Launch0 will hold their notifications for 30 minutes to 3 hours. A count appears on the home screen — release them with a tap, or let them arrive when your focus window ends.

[Learn more about Do Not Disturb →](/guide/features#do-not-disturb)

</template>
<template v-slot:screen-time>

Launch0 shows your daily screen time right on the home screen — no extra apps needed. See how long you've spent in each app today and make intentional choices about where your time goes.

[Learn more about features →](/guide/features#home-screen)

</template>
<template v-slot:wallpapers>

A fresh, abstract wallpaper every hour — generated right on your device, with no downloads and no internet. It matches your light or dark theme and keeps your minimal home screen feeling new.

[Learn more about features →](/guide/features#appearance)

</template>
<template v-slot:privacy>

Launch0 needs no account, sends no data, and works **fully offline** — it doesn't even request internet permission. Notes, voice memos, and images never leave your device. The entire codebase is open for inspection under the [GPLv3 license](https://www.gnu.org/licenses/gpl-3.0.en.html).

[Read the privacy policy →](/privacy)

</template>
</HomePage>
