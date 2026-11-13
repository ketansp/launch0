---
title: Features
description: Everything Launch0 does — a fast, text-first home screen with instant search, Notes, a Distraction timer, held notifications, screen time and more.
---

<script setup>
const start = [
  { icon: '🚀', title: 'Setup & first run', link: '/features/setup', text: 'What happens the first time you open Launch0, the short walkthrough, and making it your home screen.' },
]
const home = [
  { icon: '🏠', title: 'Home screen', link: '/features/home-screen', text: 'Up to eight apps as calm text, a clock and date that open what you choose, and nothing else.' },
  { icon: '▦', title: 'Home widgets', link: '/features/widgets', text: 'A days-left grid for the year, your agenda for today, and the notifications you are holding back.' },
  { icon: '👆', title: 'Gestures', link: '/features/gestures', text: 'Swipe up for apps, left for Notes, right for your quick-launch app, double tap to lock.' },
]
const apps = [
  { icon: '⌨️', title: 'Search & launch', link: '/features/search', text: 'Swipe up and type a letter or two. When only one app matches, it opens on its own.' },
  { icon: '🔤', title: 'App drawer', link: '/features/app-drawer', text: 'Your full app list with an A–Z rail, optional icons, and quick rename, hide and uninstall.' },
  { icon: '💼', title: 'Work profile', link: '/features/work-profile', text: 'Work and personal apps in one list, each opening in the right profile.' },
]
const capture = [
  { icon: '📝', title: 'Notes', link: '/features/notes', text: 'A private chat with yourself: text, to-dos, voice memos and images, one swipe from home.' },
]
const focus = [
  { icon: '⏳', title: 'Distraction timer', link: '/features/distraction-timer', text: 'A short, growing countdown before the apps that pull you in. Turn back costs nothing.' },
  { icon: '🔕', title: 'Do Not Disturb', link: '/features/do-not-disturb', text: 'Hold notifications from chosen apps and get them in one batch, or release them early.' },
  { icon: '📊', title: 'Screen time', link: '/features/screen-time', text: "Today's total in the corner and a quiet per-app counter beside the apps you use most." },
]
const yours = [
  { icon: '🎨', title: 'Appearance & wallpapers', link: '/features/appearance', text: 'Dark or light, text size, and a fresh abstract wallpaper every hour, made on your phone.' },
  { icon: '⚙️', title: 'Settings reference', link: '/features/settings', text: 'Every setting, its options and its default, card by card.' },
]
</script>

# Features

Launch0 is an OS for the busy: a fast, text-first home screen that becomes the command layer for your phone. Launch apps, capture notes, hold distractions and track your time, all from one calm surface. Everything here is built in and works offline. There are no accounts, no ads and no add-ons.

## Start here

<FeatureCards :items="start" />

## Your home screen

<FeatureCards :items="home" />

## Find and open apps

<FeatureCards :items="apps" />

## Capture

<FeatureCards :items="capture" />

## Focus

<FeatureCards :items="focus" />

## Make it yours

<FeatureCards :items="yours" />

## Private by design

Launch0 declares no internet permission, so it cannot send anything anywhere. There is no account, no analytics and no tracking. Read exactly what it stores and why in the [privacy policy](/privacy).
