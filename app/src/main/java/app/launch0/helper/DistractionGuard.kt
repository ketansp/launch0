package app.launch0.helper

/**
 * In-memory bridge between the distraction wait screen and [MyAccessibilityService], which now share
 * the app's main process.
 *
 * The wait screen only ever intercepts launches that go through Launch0's own UI. To also catch a
 * flagged app opened from a notification, Recents, a deep link or an app switch, the accessibility
 * service watches for that app reaching the foreground and raises the wait screen over it. But once
 * the user clears the wait and the app actually opens, the app returns to the foreground and would
 * be intercepted again in a loop. [allow] records the just-cleared app so the service lets it be
 * until the user moves on to a different watched app or back to the launcher.
 *
 * State is intentionally process-memory only: if the process is recreated it resets to "prompt",
 * which fails safe (a wait is shown) rather than silently skipping the timer.
 */
object DistractionGuard {

    /** The distracting app the user just cleared and may keep using without waiting again. */
    @Volatile
    var allowedPackage: String? = null
        private set

    /**
     * The app the wait screen is currently being raised for. Used to swallow the burst of window
     * events an app fires while opening so the wait screen is only started once.
     */
    @Volatile
    var promptedPackage: String? = null

    /** Marks [packageName] as cleared to open; called right before the wait screen launches it. */
    fun allow(packageName: String) {
        allowedPackage = packageName
        promptedPackage = null
    }

    /** Forgets any allowed app, so the next foreground entry of a distracting app waits again. */
    fun clear() {
        allowedPackage = null
    }
}
