package app.launch0.helper

import app.launch0.data.Constants

/**
 * In-memory bridge between the distraction wait screen and [MyAccessibilityService], which share
 * the app's main process and its main thread.
 *
 * When the user clears the wait for an app, [allow] records a pass for it. The service lets that
 * app come to the foreground freely while the pass stands, and drops it ([clear]) the moment the
 * user moves to another app or back to the launcher — so returning later, by any route, waits
 * again. The prompt bookkeeping keeps the burst of window events an app fires while opening from
 * raising the wait screen more than once, bounded in time so a lost launch can never wedge it.
 *
 * State is deliberately process-memory only: if the process is recreated it resets to "no pass",
 * which fails safe (a wait is shown) rather than silently letting an app through.
 */
object DistractionGuard {

    /** The distracting app the user cleared and may keep using without waiting again, or null. */
    var allowedPackage: String? = null
        private set

    private var promptedPackage: String? = null
    private var promptedAt = 0L

    fun isAllowed(packageName: String): Boolean = packageName == allowedPackage

    /** Grants a pass to [packageName]; called right before the wait screen hands over to it. */
    fun allow(packageName: String) {
        allowedPackage = packageName
        promptedPackage = null
    }

    /** Drops any pass, so the next foreground entry of a distracting app waits again. */
    fun clear() {
        allowedPackage = null
    }

    /** True if the wait screen was raised for [packageName] within the last moment. */
    fun recentlyPrompted(packageName: String): Boolean =
        packageName == promptedPackage &&
            System.currentTimeMillis() - promptedAt < Constants.DistractionTimer.PROMPT_DEDUPE_MS

    fun markPrompted(packageName: String) {
        promptedPackage = packageName
        promptedAt = System.currentTimeMillis()
    }

    fun resetPrompt() {
        promptedPackage = null
    }
}
