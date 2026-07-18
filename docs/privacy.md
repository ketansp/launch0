# Privacy Policy

**Last updated: 18 July 2026**

Launch0 is a text-only home screen for Android. It is built so that privacy is a property of how the app *works*, not a promise you have to trust. There is no telemetry to turn off, because none was ever added. There is no account to create, no profile to build, and no server for your data to travel to — Launch0 doesn't even request permission to use the internet.

This policy explains, in detail, exactly what Launch0 does and does not do with your information. It applies to the Launch0 Android app (package `app.launch0`) distributed through Google Play and as a downloadable APK.

## The short version

- **No data collection.** Launch0 has no analytics, no crash reporting, no advertising SDKs, and no trackers of any kind.
- **No internet permission.** The app cannot send data off your device because it never requests network access.
- **Everything stays on your device.** Your settings, notes, voice memos, and images live only in Launch0's private storage on your phone.
- **No accounts, no sign-in, no cloud.** Launch0 never asks who you are.
- **Open source (GPLv3).** Every claim here is verifiable in the source code.

The rest of this document is the long version.

## What Launch0 does not do

Launch0 contains **none** of the following:

- No analytics or usage statistics reporting
- No crash or error reporting to any server
- No advertising, ad SDKs, or ad identifiers
- No user profiling, fingerprinting, or tracking
- No accounts, logins, or cloud sync
- No selling, sharing, or renting of data — there is no data to sell
- No background data collection of any kind

There is no "opt out," because there is nothing to opt out of.

## Where your data lives

All of your Launch0 data is stored **locally, in the app's private sandbox** on your device — the area of internal storage that Android reserves for Launch0 alone and that other apps cannot read. Nothing is written to shared or external storage, and nothing is uploaded.

| What | How it's stored | Where |
|------|-----------------|-------|
| Settings & preferences (theme, gestures, home apps, text size, etc.) | Android `SharedPreferences` | App-private storage |
| Home-screen app choices, hidden apps, custom app names | `SharedPreferences` | App-private storage |
| Notes text & to-dos (with their done/urgent flags and timestamps) | JSON in a private `SharedPreferences` file | App-private storage |
| Note images (attached or shared in) | Copied into a private folder | App-private internal storage |
| Voice memos | Recorded audio files (`.m4a`) | App-private internal storage |

Because this data lives inside Launch0's private storage, it is removed when you uninstall the app or clear its data from Android's app settings. Launch0 keeps no copy anywhere else.

## Network activity

**Launch0 does not declare the `INTERNET` permission.** On Android, an app that doesn't request that permission is not permitted to open network connections at all. This is the strongest guarantee the platform offers: Launch0 *cannot* transmit your data, even by accident or through a bug, because the operating system will not let it reach the network.

There are a few places where you can deliberately hand something off to **another** app you have chosen. In every case, Launch0 simply asks Android to open that other app — the launcher itself never touches the network:

- **Web search.** If you begin a search in the app drawer with `!`, Launch0 opens the query in your default browser using DuckDuckGo. The request is made by your browser, not by Launch0. Once you're in the browser, that browser's and DuckDuckGo's own privacy policies apply.
- **Links in Notes.** Tapping a URL or email address in a note opens it in your browser or mail app.
- **Sharing out.** Long-pressing a note to "share" hands its contents to whichever app you pick from Android's share sheet.
- **Opening apps.** Launching an app, or opening your clock, calendar, or alarm from the home screen, simply starts that app.

In all of these, what happens next is governed by the app you chose, not by Launch0.

## Permissions

Launch0 requests only the permissions its features need, and nearly all of them are optional. If you don't use a feature, you never need to grant its permission.

| Permission | Used for | Required? |
|-----------|----------|-----------|
| Default home app (launcher role) | Acting as your home screen | Yes — to work as a launcher |
| Query installed apps (`QUERY_ALL_PACKAGES`) | Listing and launching your apps in the drawer and home screen | Yes — core function |
| Expand status bar | The "swipe down for notifications" gesture | Only if you use it |
| Set wallpaper | Applying the auto-generated wallpaper | Only if you enable wallpapers |
| Set alarm | Opening your clock/alarm app | Only if you use it |
| Request delete packages | Uninstalling an app from the drawer | Only when you uninstall |
| Microphone (`RECORD_AUDIO`) | Recording voice notes in Notes | Only if you record a voice note |
| Usage access (`PACKAGE_USAGE_STATS`) | Showing screen time next to your apps | Optional |
| Accessibility service | Double-tap-to-lock gesture (Android 9+) | Optional, off by default |
| Notification access | Holding notifications for Do Not Disturb | Optional |
| Device admin | Double-tap-to-lock gesture (older Android) | Optional, off by default |

### The app list

To be a launcher, Launch0 needs to know which apps are installed so it can show and open them. It reads this list from the Android system on your device. The list is used only to display and launch your apps (including work-profile and dual-app entries) and to let you rename or hide them. It is never uploaded, and any customizations you make — hidden apps, renamed apps, home-screen picks — are stored only in Launch0's private storage.

### Accessibility service

Google requires apps to explain their use of accessibility services clearly, so to be explicit: Launch0's accessibility service exists for **one purpose only — to lock your screen when you double-tap the home screen.** It is:

- **Off by default** and entirely optional; Launch0 works fully without it.
- **Not used to read, collect, log, or transmit anything.** It does not inspect screen contents, capture what you type, or observe other apps. It only invokes Android's "lock screen" action.

On Android versions older than 9, the same double-tap-to-lock gesture uses the **device administrator** API instead. Launch0 requests only the lock-screen capability; it cannot wipe your device, change your password, or enforce any other policy.

### Notification access (Do Not Disturb)

If you turn on Do Not Disturb, Launch0 uses Android's notification access to **hold** notifications from the apps you choose and **release** them later — either when you tap the count beside the app, or automatically when the time window ends. Notification contents are handled on your device to display the held count and are never stored off-device or sent anywhere. Ongoing notifications such as calls and music are never held. This permission is optional and is used for nothing but this feature.

### Screen time (usage access)

If you grant usage access, Launch0 reads Android's own per-app usage figures so it can show how long you've spent in an app today, as a small label beside its name. This information is read from the system, displayed on your screen, and never leaves your device. The feature is optional; without it, screen time simply isn't shown.

## Notes

The built-in Notes page is a private space that works like a chat with yourself. Everything you put in it — text, to-dos, voice memos, and images — is stored **only in Launch0's private storage on your device**, as described in [Where your data lives](#where-your-data-lives).

- **Voice memos** are recorded with your microphone only while you are actively recording a note, and only after you grant the microphone permission. The resulting audio file is saved privately on your device. Launch0 does no speech recognition and no processing of the audio beyond letting you play it back.
- **Images** you attach, or share into Notes from another app, are copied into Launch0's private storage so they remain available to you. They are not sent anywhere.
- **Deleting** a note also deletes any image or audio file that belonged to it.

Nothing in Notes is uploaded, indexed by anyone else, or shared unless *you* explicitly choose to share an entry to another app.

## Wallpapers

Launch0 can generate a fresh, abstract wallpaper that regenerates periodically. These wallpapers are created **entirely on your device** using gradients and patterns — nothing is downloaded, no image service is contacted, and no data is sent. The feature honors your light/dark theme and uses only the "set wallpaper" permission.

## Backups

Launch0 leaves Android's standard app-backup behavior at its default. This means that **if you have device backup enabled in your own Android/Google settings**, your Launch0 data may be included in your personal device backup, stored under your own Google account. This backup is created and controlled by Android and Google, not by Launch0 — the app neither operates it nor receives your data through it. You can control or disable it in your device's backup settings, and it is governed by [Google's privacy policy](https://policies.google.com/privacy).

## Children's privacy

Launch0 does not knowingly collect any information from anyone, including children, because it does not collect information at all. It contains no ads, no in-app purchases, and no content directed at children.

## Third-party services

Launch0 bundles no third-party analytics, advertising, or tracking libraries. Its dependencies are limited to standard Android (AndroidX / Jetpack) and Google Material components used to build the interface; none of them are used to collect or transmit your data.

If you obtained Launch0 from the Google Play Store, your download and any updates are delivered by Google Play, whose data handling is governed by [Google's privacy policy](https://policies.google.com/privacy). You can also install Launch0 as an APK independently of any store.

## Verify it yourself

You don't have to take this policy on faith. Because Launch0 is fully open source, you can confirm every claim above:

1. **Read the source.** The complete codebase is public at [github.com/ketansp/launch0](https://github.com/ketansp/launch0).
2. **Check the permissions.** The full, current list is in the app's [`AndroidManifest.xml`](https://github.com/ketansp/launch0/blob/master/app/src/main/AndroidManifest.xml) — note the absence of the `INTERNET` permission.
3. **Build it yourself.** You can compile Launch0 from source and run your own build (see the [build guide](/guide/build)).
4. **Watch the network.** Because there is no internet permission, a network monitor will show Launch0 making no connections of its own.

## Legal compliance

Launch0 is designed to comply with data-protection laws structurally: because it collects, stores, and transmits no personal data off your device, there is no personal data to request, correct, delete, export, or refuse to sell.

- **GDPR** (EU/EEA/UK): Launch0 is not a data controller or processor of any personal data, as it collects none.
- **CCPA / CPRA** (California): Launch0 collects no personal information and sells or shares none.
- **COPPA** (children): Launch0 collects no information from anyone, including children under 13.

Whatever data you create inside Launch0 stays on your own device, under your control, and you can erase it at any time by deleting individual entries, clearing the app's data, or uninstalling the app.

## Changes to this policy

If this policy ever changes, the updated version will be published here with a new "Last updated" date. Because Launch0's privacy posture is built into the app itself, any change that affected what data the app handles would also be visible in the open-source code.

## Contact

If you have any questions about this privacy policy, please [open an issue](https://github.com/ketansp/launch0/issues) on GitHub.
