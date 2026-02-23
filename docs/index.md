---
layout: home

hero:
  name: Launch0
  text: Minimal & Productive Launcher
  tagline: A minimal, open-source Android launcher built for productivity. Clean home screen, instant app access, powerful gestures. No ads, no data collection.
  actions:
    - theme: brand
      text: Get Started
      link: /guide/
    - theme: alt
      text: View on GitHub
      link: https://github.com/ketansp/Olauncher

features:
  - icon: ✨
    title: Minimal by Design
    details: A clean, text-based home screen with no icons or distractions. Just the apps you need, ready to launch.
    link: /guide/features#home-screen
    linkText: Learn more
  - icon: ⚡
    title: Built for Productivity
    details: Instant search, gesture shortcuts, and screen time tracking help you get things done faster.
    link: /guide/features#gestures
    linkText: Learn more
  - icon: 🔒
    title: Privacy-First
    details: No data collection, no ads, no tracking. Everything stays on your device.
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
3. **Swipe up** for all apps, **long press** anywhere for settings
4. **Start typing** an app name in the drawer to auto-launch it

</template>
<template v-slot:gestures>

| Gesture | Action |
|---------|--------|
| Swipe up | Open app drawer |
| Swipe left | Open left app |
| Swipe right | Open right app |
| Swipe down | Notifications or search |
| Double tap | Lock screen |
| Long press | Open settings |

</template>
<template v-slot:features-list>

* [Text-based home screen](/guide/features#home-screen)
* [Instant app search](/guide/features#app-drawer)
* [Gesture shortcuts](/guide/features#gestures)
* [Screen time tracking](/guide/features#home-screen)
* [Dark & light themes](/guide/features#appearance)
* [Daily wallpapers](/guide/features#appearance)
* [Rename apps](/guide/features#app-drawer)
* [Hide apps](/guide/features#app-drawer)
* [Work profile support](/guide/features#work-profile)
* [Double tap to lock](/guide/features#gestures)
* [Customizable text size](/guide/features#appearance)
* [Status bar toggle](/guide/features#appearance)
* [App alignment options](/guide/features#home-screen)
* [Swipe app shortcuts](/guide/features#gestures)
* [Auto-launch search](/guide/features#app-drawer)
* [Date & time display](/guide/features#home-screen)
* [20 languages](/guide/features#home-screen)

</template>
<template v-slot:screen-time>

Launch0 shows your daily screen time right on the home screen — no extra apps needed. Stay aware of your phone usage and make intentional choices about where your time goes.

[Learn more about features →](/guide/features#home-screen)

</template>
<template v-slot:wallpapers>

A beautiful new wallpaper every day, delivered automatically. Your minimal home screen stays fresh without any effort. Supports dark and light wallpaper themes.

[Learn more about features →](/guide/features#appearance)

</template>
<template v-slot:privacy>

Launch0 needs no account, sends no data, and works fully offline (except optional daily wallpapers). The entire codebase is open for inspection under the [GPLv3 license](https://www.gnu.org/licenses/gpl-3.0.en.html).

[Read the privacy policy →](/privacy)

</template>
</HomePage>
