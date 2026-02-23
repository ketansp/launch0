# Build from Source

Launch0 is open source. You can build it yourself from the source code.

## Prerequisites

- [Android Studio](https://developer.android.com/studio) (latest stable)
- JDK 17
- Android SDK 35

## Steps

1. Clone the repository:

```bash
git clone https://github.com/ketansp/Olauncher.git
cd Olauncher
```

2. Open the project in Android Studio.

3. Let Gradle sync and download dependencies.

4. Build and run on your device or emulator:

```bash
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Project Structure

```
app/src/main/java/app/launch0/
├── MainActivity.kt          # Main entry point
├── MainViewModel.kt         # App state management
├── data/
│   ├── AppModel.kt          # App data models
│   ├── Constants.kt         # App constants and URLs
│   └── Prefs.kt             # SharedPreferences wrapper
├── helper/
│   ├── Utils.kt             # Utility functions
│   ├── WallpaperWorker.kt   # Daily wallpaper background worker
│   └── ...
├── listener/
│   └── ...                  # Touch and gesture listeners
└── ui/
    ├── HomeFragment.kt      # Home screen
    ├── AppDrawerFragment.kt # App drawer
    └── SettingsFragment.kt  # Settings screen
```
