package app.launch0.helper

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import app.launch0.MainActivity
import app.launch0.R
import app.launch0.data.Constants
import app.launch0.data.Prefs

/**
 * Backs two optional, off-by-default features:
 *
 *  - **Double tap to lock** — a click on the home screen's lock layout triggers the global lock
 *    action (Android 9+).
 *  - **Distraction timer** — whenever an app the user flagged as distracting comes to the
 *    foreground, by any route (a notification, Recents, a deep link, an app switch, the launcher),
 *    this service raises Launch0's wait screen over it. Without this the timer only covered
 *    launches started from the launcher's own UI.
 *
 * The timer's pass is strictly per visit: once the user clears the wait they may stay in the app
 * as long as they like, but the moment they move to another app or back to the launcher the pass
 * is dropped, so coming back — from Recents or anywhere else — waits again. Transient windows
 * that appear *over* the current app (the keyboard, the notification shade, the Recents overview,
 * permission prompts, share sheets, the app's own dialogs) are not treated as leaving.
 *
 * Runs in the app's main process so it shares [Prefs] and [DistractionGuard] with the wait screen.
 * Everything here and in the wait screen runs on the main thread, so there is no race between
 * granting a pass and the app's foreground event being handled.
 */
class MyAccessibilityService : AccessibilityService() {

    private val prefs by lazy { Prefs(applicationContext) }

    // Window events are frequent; remember per package whether it is a real, launchable app.
    private val launchableCache = HashMap<String, Boolean>()

    // Home screens / launchers. Many devices have the OEM launcher host the Recents overview even
    // when it isn't the default, so one of these coming in front is not the user leaving for
    // another app.
    private val homePackages: Set<String> by lazy { queryHomePackages() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs.lockModeOn = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> handleLockClick(event)
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> handleForegroundChange(event)
        }
    }

    private fun handleLockClick(event: AccessibilityEvent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        try {
            val source: AccessibilityNodeInfo = event.source ?: return
            if ((source.className == "android.widget.FrameLayout") and
                (source.contentDescription == getString(R.string.lock_layout_description))
            )
                performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } catch (e: Exception) {
            return
        }
    }

    private fun handleForegroundChange(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return

        // Launch0 itself: the home screen or the wait screen. The user is on the launcher, so the
        // app they had cleared (if any) is left behind — next time it comes up, it waits again.
        if (pkg == packageName) {
            DistractionGuard.clear()
            DistractionGuard.resetPrompt()
            return
        }

        // Still inside the app the user cleared, its own dialogs included. Nothing to do.
        if (DistractionGuard.isAllowed(pkg)) return

        // Something that is not a destination app came in front: the keyboard, system UI (the
        // notification shade, Recents overview, volume, keyguard), a permission prompt, a share
        // sheet, another launcher hosting Recents. These sit *over* the current app without the
        // user leaving it, so they must neither drop the pass nor trigger a wait.
        if (!isDestinationApp(pkg)) return

        // A genuinely different app is in front, so whatever was cleared has been left.
        DistractionGuard.clear()

        if (!DistractionTimer.isDistractingApp(prefs, pkg)) return

        // An app fires a burst of window events while it opens; only the first should raise the
        // wait screen. The dedupe is time-bounded so a swallowed launch can never wedge the timer.
        if (DistractionGuard.recentlyPrompted(pkg)) return
        DistractionGuard.markPrompted(pkg)
        showWaitScreen(pkg)
    }

    /** A real app the user can be "in": it has a launcher entry and is not itself a home screen. */
    private fun isDestinationApp(pkg: String): Boolean {
        if (pkg in homePackages) return false
        return launchableCache.getOrPut(pkg) {
            try {
                packageManager.getLaunchIntentForPackage(pkg) != null
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun queryHomePackages(): Set<String> = try {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        packageManager.queryIntentActivities(home, 0).map { it.activityInfo.packageName }.toSet()
    } catch (e: Exception) {
        emptySet()
    }

    private fun showWaitScreen(pkg: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Constants.Key.SHOW_DISTRACTION_WAIT, true)
            putExtra(Constants.Key.APP_PACKAGE, pkg)
            putExtra(Constants.Key.APP_NAME, appLabel(pkg))
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            DistractionGuard.resetPrompt()
        }
    }

    private fun appLabel(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        pkg
    }

    override fun onInterrupt() {

    }
}
