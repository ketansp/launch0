package app.launch0.helper

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.SharedPreferences
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
 *  - **Distraction timer** — when an app the user flagged as distracting reaches the foreground by
 *    ANY route (a notification, Recents, a deep link, an app switch), this service raises Launch0's
 *    wait screen over it. Without this, the timer only covered launches started from the launcher UI.
 *
 * It runs in the app's main process so it shares [Prefs] and [DistractionGuard] with the wait screen.
 * The set of watched packages is narrowed at runtime to just the flagged apps (plus Launch0 itself,
 * for the lock gesture), so the service is told about nothing the user hasn't opted into.
 */
class MyAccessibilityService : AccessibilityService() {

    private val prefs by lazy { Prefs(applicationContext) }

    // Kept as a strong reference: SharedPreferences holds listeners weakly.
    private val prefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == Prefs.KEY_DISTRACTION_APPS) refreshWatchedPackages()
        }

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs.lockModeOn = true
        prefs.registerOnChange(prefsListener)
        refreshWatchedPackages()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> handleLockClick(event)
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> handleForegroundApp(event)
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

    private fun handleForegroundApp(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return

        // Our own windows (home screen, the wait screen itself). Landing on the home screen means the
        // user left whatever they had open, so any earlier "allowed" app must wait again next time.
        if (pkg == packageName) {
            DistractionGuard.promptedPackage = null
            DistractionGuard.clear()
            return
        }

        // Still inside the app the user just cleared — let it be.
        if (pkg == DistractionGuard.allowedPackage) return

        // Moved on to a different app than the one that was cleared; drop the pass.
        DistractionGuard.clear()

        if (!DistractionTimer.isDistractingApp(prefs, pkg)) return

        // A distracting app is in the foreground and hasn't been cleared. Swallow the burst of window
        // events it fires while opening, then raise the wait screen over it.
        if (DistractionGuard.promptedPackage == pkg) return
        DistractionGuard.promptedPackage = pkg
        showWaitScreen(pkg)
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
            DistractionGuard.promptedPackage = null
        }
    }

    private fun appLabel(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        pkg
    }

    /**
     * Narrows the service to the flagged apps (for the timer) plus Launch0 itself (for the lock
     * gesture), and listens for the window-state and click events those two features need.
     */
    private fun refreshWatchedPackages() {
        val info = serviceInfo ?: return
        info.eventTypes =
            AccessibilityEvent.TYPE_VIEW_CLICKED or AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.packageNames = (prefs.distractionApps + packageName).toTypedArray()
        try {
            serviceInfo = info
        } catch (e: Exception) {
            // Service not connected yet; onServiceConnected will retry.
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        prefs.unregisterOnChange(prefsListener)
        return super.onUnbind(intent)
    }

    override fun onInterrupt() {

    }
}
