# CLAUDE.md — Launch0

## Project Overview

Launch0 is a minimal, open-source Android launcher (home screen replacement) built for productivity, forked from [Olauncher](https://github.com/tanujnotes/Olauncher). It provides a clean, text-only home screen with instant app access, powerful gestures, no icons, no ads, and no data collection. Licensed under GPLv3.

- **Package**: `app.launch0`
- **Min SDK**: 24 (Android 7.0)
- **Target/Compile SDK**: 35
- **Language**: Kotlin
- **Build System**: Gradle (Groovy DSL) with version catalog (`gradle/libs.versions.toml`)

## Build & Run

```bash
# Debug build
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Release build (requires signing config)
./gradlew assembleRelease

# Clean
./gradlew clean
```

**Requirements**: JDK 17, Android SDK 35, Gradle 8.9.1, Kotlin 2.1.20.

The debug build uses application ID `app.launch0.debug` (via `applicationIdSuffix`), while release uses `app.launch0`.

## Project Structure

```
launch0/
├── app/
│   ├── build.gradle              # App-level build config
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/app/launch0/
│       │   ├── MainActivity.kt       # Single Activity entry point
│       │   ├── MainViewModel.kt      # Shared ViewModel (AndroidViewModel)
│       │   ├── data/
│       │   │   ├── AppModel.kt        # Sealed class: App | PinnedShortcut
│       │   │   ├── Constants.kt       # All constants, flags, URLs
│       │   │   ├── DrawerCharacterModel.kt
│       │   │   └── Prefs.kt           # SharedPreferences wrapper
│       │   ├── helper/
│       │   │   ├── AppFilterHelper.kt
│       │   │   ├── AppUsageStats.kt
│       │   │   ├── Extensions.kt      # Kotlin extension functions
│       │   │   ├── FakeHomeActivity.kt # Used for launcher reset trick
│       │   │   ├── MyAccessibilityService.kt
│       │   │   ├── PinItemActivity.kt
│       │   │   ├── SingleLiveEvent.kt  # One-shot LiveData events
│       │   │   ├── Utils.kt           # Utility functions (app list, wallpaper, etc.)
│       │   │   ├── WallpaperWorker.kt  # WorkManager periodic wallpaper job
│       │   │   └── usageStats/        # Screen time tracking
│       │   ├── listener/
│       │   │   ├── DeviceAdmin.kt
│       │   │   ├── OnSwipeTouchListener.kt
│       │   │   └── ViewSwipeTouchListener.kt
│       │   └── ui/
│       │       ├── HomeFragment.kt     # Main home screen
│       │       ├── AppDrawerFragment.kt # App list/search
│       │       ├── AppDrawerAdapter.kt  # RecyclerView adapter
│       │       └── SettingsFragment.kt  # Settings screen
│       └── res/
│           ├── layout/                # Portrait layouts
│           ├── layout-land/           # Landscape layouts
│           ├── navigation/nav_graph.xml
│           ├── values/                # Strings, colors, styles, dimens
│           └── values-{locale}/       # 20 language translations
├── build.gradle                  # Root build file
├── settings.gradle
├── gradle.properties
├── gradle/libs.versions.toml     # Version catalog
├── docs/                         # VitePress documentation site
├── fastlane/                     # Play Store metadata & screenshots
└── .github/
    ├── workflows/deploy-docs.yml # Docs deployment to GitHub Pages
    └── funding.yml
```

## Architecture & Patterns

### Single Activity + Navigation Component
The app uses a single `MainActivity` with three fragments navigated via Jetpack Navigation (`nav_graph.xml`):
- `HomeFragment` — Home screen with up to 8 configurable app shortcuts
- `AppDrawerFragment` — Searchable app list with RecyclerView
- `SettingsFragment` — All settings controls

### ViewModel
`MainViewModel` (extends `AndroidViewModel`) is shared across all fragments via `ViewModelProvider`. It handles:
- App list retrieval (via `LauncherApps` API)
- Home app selection and persistence
- Wallpaper worker scheduling (WorkManager)
- Screen time tracking
- Dialog/message orchestration via `SingleLiveEvent`

### Data Layer
- **No database** — all persistence uses `SharedPreferences` via the `Prefs` class
- `Prefs` stores up to 8 home screen apps, swipe apps, theme, wallpaper settings, and user state
- `AppModel` is a sealed class with two variants: `App` and `PinnedShortcut`
- App lists are fetched from the system `LauncherApps` service on a background thread (`Dispatchers.IO`)

### View Binding
All fragments and the activity use **View Binding** (not Data Binding). The pattern used:
```kotlin
private var _binding: FragmentHomeBinding? = null
private val binding get() = _binding!!
// Nullify in onDestroyView
```

### Extension Functions
`helper/Extensions.kt` and `helper/Utils.kt` contain Kotlin extension functions on `Context`, `View`, `Long`, etc. These are the preferred way to add utility behavior.

## Key Conventions

### Code Style
- **Kotlin official style** (`kotlin.code.style=official` in `gradle.properties`)
- Extension functions preferred over utility classes with static methods
- `View.OnClickListener` and `View.OnLongClickListener` interfaces implemented directly on fragments
- Constants organized as nested objects inside `Constants` (e.g., `Constants.Dialog.ABOUT`, `Constants.SwipeDownAction.SEARCH`)
- Flag-based navigation: fragments receive an integer `flag` via Bundle arguments to determine behavior

### Naming
- Activities: `*Activity.kt`
- Fragments: `*Fragment.kt`
- Layout files: `activity_*.xml`, `fragment_*.xml`, `adapter_*.xml`
- Preference keys: `UPPER_SNAKE_CASE` private vals in `Prefs`
- View IDs in XML: `camelCase`

### UI Approach
- Purely text-based UI — no app icons anywhere
- Supports dark/light/system themes via `AppCompatDelegate`
- Landscape layouts provided in `layout-land/`
- Responsive dimension values via density-specific `values-*dpi/dimens.xml`
- Custom touch/swipe listeners for gesture handling

### Localization
20 languages supported. String resources are in `res/values-{locale}/strings.xml`. Non-translatable strings are marked with `translatable="false"` in `res/values/strings.xml`.

Supported locales: en, ar, de, es-rES, es-rUS, fr, he, hr, hu, in, it, ja, nl, pl, pt-rBR, ru-rRU, sv, tr, uk, zh.

## Dependencies

All managed via version catalog in `gradle/libs.versions.toml`:

| Library | Purpose |
|---------|---------|
| `androidx.core:core-ktx` | Kotlin extensions for Android |
| `androidx.appcompat:appcompat` | Backward-compatible UI |
| `androidx.recyclerview:recyclerview` | App drawer list |
| `androidx.lifecycle:lifecycle-*` | ViewModel + LiveData |
| `androidx.navigation:navigation-fragment-ktx` | Fragment navigation |
| `androidx.work:work-runtime-ktx` | Periodic wallpaper worker |
| `com.google.android.material:material` | Material components |

The project intentionally keeps dependencies minimal. Do not add unnecessary dependencies.

## CI/CD

- **GitHub Actions**: `deploy-docs.yml` builds and deploys the VitePress documentation site to GitHub Pages on pushes to `master` that modify `docs/`.
- **No Android CI pipeline** currently — builds and tests are run locally.
- **Fastlane**: `fastlane/metadata/` contains Play Store listing metadata and screenshots.

## Testing

- The `AndroidJUnitRunner` is configured as the test instrumentation runner, but there are currently no test files in the repository.
- When adding tests, use `app/src/test/` for unit tests and `app/src/androidTest/` for instrumented tests.

## Important Notes for AI Assistants

1. **This is a launcher app** — it registers as a home screen replacement via `CATEGORY_HOME` intent filters. The `MainActivity` has `launchMode="singleTask"` and `excludeFromRecents="true"`.

2. **Privacy is paramount** — the app collects zero data. Never introduce analytics, tracking, or network calls beyond the existing wallpaper feature. The only internet usage is downloading daily wallpapers from Unsplash.

3. **Minimal design + productivity is the philosophy** — the app pairs a clean, distraction-free interface with productivity features (instant search, gestures, screen time tracking). The text-only aesthetic keeps things minimal; the features make you faster. Resist adding unnecessary UI elements or dependencies.

4. **ProGuard/R8** is enabled for release builds with minification. Check `proguard-rules.pro` if adding reflection-dependent code.

5. **Multi-user/work profile support** — the app handles `UserHandle` throughout for dual-app and work profile scenarios. Always consider this when modifying app list logic.

6. **The `Prefs` class is verbose by design** — each home app slot (1-8) has its own set of preference keys. This is the existing pattern; follow it for consistency.

7. **Branch workflow**: Create feature branches from `master`. Test on real devices when touching UI code. Keep changes focused and minimal.
