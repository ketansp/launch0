---
title: "Download for Android: Google Play or APK"
description: "Get Launch0 free on Google Play, or download the APK from GitHub. Android 7.0 or later. No account, no ads, no internet permission."
---

<script setup>
import { VPButton } from 'vitepress/theme'
</script>

# Download

Get Launch0 for your Android device.

## Google Play <Badge type="tip" text="Recommended" /> {#google-play}

The easiest way to install Launch0. You get automatic updates, and there's no need to allow installs from unknown sources.

<VPButton
  class="download-button"
  size="big"
  text="Download on Google Play"
  href="https://play.google.com/store/apps/details?id=app.launch0"
/>

Once installed, press the Home button and select **Launch0** as your launcher.

## APK from GitHub {#apk}

No Play Store on your phone, or prefer not to use it? Download the APK directly from GitHub:

**[Download Launch0 APK](https://github.com/ketansp/launch0/releases/latest/download/app-release.apk)**

You can also browse all releases on the [GitHub Releases page](https://github.com/ketansp/launch0/releases). The APK doesn't update itself — download each new release by hand.

### Installing the APK

Android blocks apps from outside the Play Store by default, so you'll need to allow installation from unknown sources:

1. Download the APK file using the link above.
2. Open the downloaded file on your Android device.
3. If prompted, tap **Settings** and enable **Allow from this source**.
4. Tap **Install**.
5. Once installed, press the Home button and select **Launch0** as your launcher.

::: tip
Android warns you about installing from an unknown source. This is normal for open-source apps distributed outside a store. You can verify the source code on [GitHub](https://github.com/ketansp/launch0).
:::

::: warning Pick one source and stick with it
The Google Play and GitHub builds are signed with different keys, so one can't update the other. To switch from the APK to Google Play (or back), you have to uninstall Launch0 first — and uninstalling deletes everything it stores on your device, including your notes, to-dos, voice memos, and settings.
:::

## Requirements

- Android 7.0 (API 24) or higher

## Build from Source

If you prefer to build the app yourself, follow the [Build from Source](/guide/build) guide.

<style scoped>
.download-button {
  margin: 8px 0 16px;
  text-decoration: none;
}
</style>
