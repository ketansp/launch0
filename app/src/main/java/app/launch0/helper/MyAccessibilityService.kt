package app.launch0.helper

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import app.launch0.data.Prefs

/**
 * Exists for one feature only: double-tap-to-lock on Android 9+.
 *
 * GLOBAL_ACTION_LOCK_SCREEN is the only public API that locks the screen the way the
 * power button does. The narrower DevicePolicyManager.lockNow() (still used on
 * Android 7-8) forces a PIN/password on the next unlock and disables biometrics.
 *
 * The service is deliberately unable to observe anything: accessibility_service_config
 * subscribes to no accessibility events and sets canRetrieveWindowContent="false", so
 * Android never hands it any screen content. The home screen asks for a lock with a
 * package-private broadcast ([requestLockScreen]); the receiver's only job is to
 * perform the global lock action.
 *
 * This is NOT an accessibility tool. Never set android:isAccessibilityTool="true" —
 * Google Play rejected the app for it.
 */
class MyAccessibilityService : AccessibilityService() {

    private var lockReceiverRegistered = false

    private val lockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Prefs(applicationContext).lockModeOn = true
        if (!lockReceiverRegistered) {
            ContextCompat.registerReceiver(
                this,
                lockReceiver,
                IntentFilter(ACTION_LOCK_SCREEN),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            lockReceiverRegistered = true
        }
    }

    override fun onDestroy() {
        if (lockReceiverRegistered) {
            unregisterReceiver(lockReceiver)
            lockReceiverRegistered = false
        }
        super.onDestroy()
    }

    // The service subscribes to no event types, so the system never calls this.
    override fun onAccessibilityEvent(event: AccessibilityEvent) = Unit

    override fun onInterrupt() = Unit

    companion object {
        private const val ACTION_LOCK_SCREEN = "app.launch0.action.LOCK_SCREEN"

        /**
         * Asks the service to lock the screen. A no-op if the user hasn't enabled it.
         * The service runs in its own process, so this goes through a broadcast scoped
         * to this package; RECEIVER_NOT_EXPORTED stops other apps from triggering it.
         */
        fun requestLockScreen(context: Context) {
            context.sendBroadcast(Intent(ACTION_LOCK_SCREEN).setPackage(context.packageName))
        }
    }
}
