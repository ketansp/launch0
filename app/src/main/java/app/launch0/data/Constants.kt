package app.launch0.data

object Constants {

    object Key {
        const val FLAG = "flag"
        const val RENAME = "rename"
        const val APP_NAME = "appName"
        const val APP_PACKAGE = "appPackage"
        const val APP_ACTIVITY_CLASS = "appActivityClass"
        const val APP_USER = "appUser"
        const val IS_SHORTCUT = "isShortcut"
        const val SHORTCUT_ID = "shortcutId"

        // Set by MyAccessibilityService when it starts MainActivity to raise the distraction wait
        // screen over an app the user opened outside the launcher (notification, app switch, etc.).
        const val SHOW_DISTRACTION_WAIT = "showDistractionWait"
    }

    object Dialog {
        const val ABOUT = "ABOUT"
        const val WALLPAPER = "WALLPAPER"
        const val REVIEW = "REVIEW"
        const val RATE = "RATE"
        const val HIDDEN = "HIDDEN"
        const val KEYBOARD = "KEYBOARD"
        const val DIGITAL_WELLBEING = "DIGITAL_WELLBEING"
    }

    object UserState {
        const val START = "START"
        const val WALLPAPER = "WALLPAPER"
        const val REVIEW = "REVIEW"
        const val RATE = "RATE"
        const val DONE = "DONE"
    }

    object DateTime {
        const val OFF = 0
        const val ON = 1
        const val DATE_ONLY = 2

        fun isTimeVisible(dateTimeVisibility: Int): Boolean {
            return dateTimeVisibility == ON
        }

        fun isDateVisible(dateTimeVisibility: Int): Boolean {
            return dateTimeVisibility == ON || dateTimeVisibility == DATE_ONLY
        }
    }

    object SwipeDownAction {
        const val SEARCH = 1
        const val NOTIFICATIONS = 2
    }

    object Dnd {
        const val DEFAULT_DURATION_MINUTES = 60
        val DURATION_OPTIONS = intArrayOf(30, 45, 60, 90, 120, 180)
    }

    object DistractionTimer {
        // Escalating mode: wait = BASE * 2^(opens today), capped at MAX. Fixed mode: FIXED.
        const val BASE_WAIT_SECONDS = 10
        const val MAX_WAIT_SECONDS = 60
        const val FIXED_WAIT_SECONDS = 30
    }

    object SwipeLeftAction {
        const val NOTES = 1
        const val APP = 2
    }

    object IconShape {
        const val DEFAULT = 0
        const val CIRCLE = 1
        const val SQUARE = 2
        const val SQUIRCLE = 3
        const val TEARDROP = 4
    }

    // Home/drawer app icon size in dp (used when "Show app icons" is on).
    const val ICON_SIZE_MIN = 16
    const val ICON_SIZE_MAX = 48
    const val ICON_SIZE_DEFAULT = 28

    // Times the app drawer / notes must be opened before its swipe-hint nudge is retired.
    const val NUDGE_DISMISS_AFTER = 10

    object CharacterIndicator {
        const val SHOW = 102
        const val HIDE = 101
    }

    val CLOCK_APP_PACKAGES = arrayOf(
        "com.google.android.deskclock", //Google Clock
        "com.sec.android.app.clockpackage", //Samsung Clock
        "com.oneplus.deskclock", //OnePlus Clock
        "com.miui.clock", //Xiaomi Clock
    )

    val WHATSAPP_PACKAGES = arrayOf(
        "com.whatsapp", //WhatsApp Messenger
        "com.whatsapp.w4b", //WhatsApp Business
    )


//    const val THEME_MODE_DARK = 0
//    const val THEME_MODE_LIGHT = 1
//    const val THEME_MODE_SYSTEM = 2

    const val FLAG_LAUNCH_APP = 100
    const val FLAG_HIDDEN_APPS = 101

    const val FLAG_SET_HOME_APP_1 = 1
    const val FLAG_SET_HOME_APP_2 = 2
    const val FLAG_SET_HOME_APP_3 = 3
    const val FLAG_SET_HOME_APP_4 = 4
    const val FLAG_SET_HOME_APP_5 = 5
    const val FLAG_SET_HOME_APP_6 = 6
    const val FLAG_SET_HOME_APP_7 = 7
    const val FLAG_SET_HOME_APP_8 = 8

    const val FLAG_SET_SWIPE_LEFT_APP = 11
    const val FLAG_SET_SWIPE_RIGHT_APP = 12
    const val FLAG_SET_CLOCK_APP = 13
    const val FLAG_SET_CALENDAR_APP = 14

    const val FLAG_SET_DND_APPS = 20
    const val FLAG_SET_DISTRACTION_APPS = 21

    const val REQUEST_CODE_ENABLE_ADMIN = 666
    const val REQUEST_CODE_LAUNCHER_SELECTOR = 678

    const val HINT_RATE_US = 15

    const val LONG_PRESS_DELAY_MS = 500L
    const val ONE_DAY_IN_MILLIS = 86400000L
    const val ONE_HOUR_IN_MILLIS = 3600000L
    const val ONE_MINUTE_IN_MILLIS = 60000L

    const val MIN_ANIM_REFRESH_RATE = 10f

    const val URL_ABOUT = "https://launch0.app"
    const val URL_PRIVACY = "https://launch0.app/privacy"
    const val URL_DOUBLE_TAP = "https://tanujnotes.notion.site/Double-tap-to-lock-Olauncher-0f7fb103ec1f47d7a90cdfdcd7fb86ef"
    const val URL_GITHUB = "https://www.github.com/ketansp/Olauncher"
    const val URL_PLAY_STORE = "https://play.google.com/store/apps/details?id=app.launch0"
    const val URL_PLAY_STORE_DEV = "https://play.google.com/store/apps/dev?id=7198807840081074933"
    const val URL_TWITTER = "https://x.com/tanujnotes"
    const val URL_NTS = "https://play.google.com/store/apps/details?id=com.makenotetoself"
    const val URL_DUCK_SEARCH = "https://duck.co/?q="
    const val URL_DIGITAL_WELLBEING_LEARN_MORE = "https://tanujnotes.substack.com/p/digital-wellbeing-app-on-android"

    const val DIGITAL_WELLBEING_PACKAGE_NAME = "com.google.android.apps.wellbeing"
    const val DIGITAL_WELLBEING_ACTIVITY = "com.google.android.apps.wellbeing.settings.TopLevelSettingsActivity"
    const val DIGITAL_WELLBEING_SAMSUNG_PACKAGE_NAME = "com.samsung.android.forest"
    const val DIGITAL_WELLBEING_SAMSUNG_ACTIVITY = "com.samsung.android.forest.launcher.LauncherActivity"
    const val WALLPAPER_WORKER_NAME = "WALLPAPER_WORKER_NAME"
}