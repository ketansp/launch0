package app.launch0.helper

import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import app.launch0.data.Prefs

/**
 * Intercepts notifications from a user-selected list of apps and holds them for a
 * configurable period (the "DND" / Do Not Disturb mode). All notifications that arrive
 * during a hold window are snoozed until the window ends, at which point the system
 * re-posts (releases) them together.
 *
 * Holding is implemented via [snoozeNotification], which is only available on API 26+.
 * On older versions notifications are left untouched.
 *
 * The user must grant notification access (Settings > Notification access) for this
 * service to receive notifications.
 */
class NotificationDndService : NotificationListenerService() {

    private val prefs by lazy { Prefs(applicationContext) }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        onNotificationsChanged?.invoke()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        onNotificationsChanged?.invoke()
        sbn ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (!prefs.dndEnabled) return
        if (sbn.packageName == applicationContext.packageName) return
        if (sbn.packageName !in prefs.dndApps) return

        val key = sbn.key ?: return
        val now = System.currentTimeMillis()

        val heldKeys = prefs.dndHeldKeys
        if (heldKeys.contains(key)) {
            // This is the re-post of a notification we held earlier (the window ended,
            // or the app updated it). Let it through and stop tracking it.
            heldKeys.remove(key)
            prefs.dndHeldKeys = heldKeys
            return
        }

        // Don't hold ongoing notifications (calls, music, downloads, foreground services).
        if (sbn.isOngoing) return

        var windowEnd = prefs.dndWindowEnd
        if (windowEnd <= now) {
            // Start a fresh hold window.
            windowEnd = now + prefs.dndDurationMinutes * 60_000L
            prefs.dndWindowEnd = windowEnd
        }

        val holdDuration = windowEnd - now
        if (holdDuration <= 0L) return

        try {
            snoozeNotification(key, holdDuration)
            heldKeys.add(key)
            prefs.dndHeldKeys = heldKeys
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification?,
        rankingMap: RankingMap?,
        reason: Int,
    ) {
        super.onNotificationRemoved(sbn, rankingMap, reason)
        onNotificationsChanged?.invoke()
        // Ignore removals caused by our own snooze, otherwise we'd forget the key and
        // re-hold the notification forever when it is re-posted.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && reason == REASON_SNOOZED) return
        val key = sbn?.key ?: return
        val heldKeys = prefs.dndHeldKeys
        if (heldKeys.remove(key)) prefs.dndHeldKeys = heldKeys
    }

    companion object {
        /**
         * Live reference to the bound service, used by the quick-actions panel to read the
         * currently active notifications. Null when the listener isn't connected (e.g. the user
         * hasn't granted notification access).
         */
        @Volatile
        private var instance: NotificationDndService? = null

        /** Invoked whenever notifications are posted/removed so an open panel can refresh. */
        @Volatile
        var onNotificationsChanged: (() -> Unit)? = null

        fun isConnected(): Boolean = instance != null

        /** Active notifications, or an empty list if the listener isn't connected. */
        fun activeNotifications(): List<StatusBarNotification> = try {
            instance?.activeNotifications?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        fun dismiss(key: String) {
            try {
                instance?.cancelNotification(key)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun clearAll() {
            try {
                instance?.cancelAllNotifications()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
