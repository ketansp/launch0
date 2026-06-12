package app.launch0.ui

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.provider.Settings
import android.service.notification.StatusBarNotification
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import app.launch0.R
import app.launch0.databinding.FragmentQuickActionsBinding
import app.launch0.helper.NotificationDndService
import app.launch0.helper.isNotificationServiceEnabled
import app.launch0.helper.showToast

/**
 * The quick-actions + notification-centre page, reached by swiping right on the home screen
 * (mirrors the swipe-left [NotesFragment]). The top half is a grid of built-in toggles and
 * shortcuts (flashlight, Wi-Fi, Bluetooth, etc.); the bottom half lists active notifications
 * read from [NotificationDndService] once the user has granted notification access.
 */
class QuickActionsFragment : Fragment() {

    private var _binding: FragmentQuickActionsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: NotificationAdapter

    private val cameraManager by lazy {
        requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }
    private var flashlightCameraId: String? = null
    private var flashlightOn = false
    private var flashlightButton: TextView? = null

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == flashlightCameraId) {
                flashlightOn = enabled
                updateFlashlightLabel()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentQuickActionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyWindowInsets()
        flashlightCameraId = findFlashlightCamera()
        buildQuickActions()
        initRecyclerView()
        binding.qaClearAll.setOnClickListener { NotificationDndService.clearAll() }
        binding.qaGrantAccess.setOnClickListener { openNotificationAccessSettings() }
    }

    override fun onResume() {
        super.onResume()
        try {
            cameraManager.registerTorchCallback(torchCallback, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Refresh notifications when an open panel comes back to the foreground, and keep it
        // live-updated while it's visible.
        NotificationDndService.onNotificationsChanged = {
            view?.post { refreshNotifications() }
        }
        refreshNotifications()
    }

    override fun onPause() {
        super.onPause()
        NotificationDndService.onNotificationsChanged = null
        try {
            cameraManager.unregisterTorchCallback(torchCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.quickActionsRoot) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }
    }

    private fun initRecyclerView() {
        adapter = NotificationAdapter(
            onItemClick = { onNotificationClick(it) },
            onItemLongClick = { onNotificationDismiss(it) },
        )
        binding.qaNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.qaNotifications.adapter = adapter
    }

    // region Quick actions

    private data class QuickAction(
        val label: String,
        val isFlashlight: Boolean,
        val onClick: () -> Unit,
    )

    private fun buildQuickActions() {
        val actions = mutableListOf<QuickAction>()
        if (flashlightCameraId != null) {
            actions += QuickAction(getString(R.string.qa_flashlight), true) { toggleFlashlight() }
        }
        actions += QuickAction(getString(R.string.qa_wifi), false) {
            openSettings(Settings.ACTION_WIFI_SETTINGS)
        }
        actions += QuickAction(getString(R.string.qa_bluetooth), false) {
            openSettings(Settings.ACTION_BLUETOOTH_SETTINGS)
        }
        actions += QuickAction(getString(R.string.qa_mobile_data), false) {
            openSettings(Settings.ACTION_DATA_ROAMING_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS)
        }
        actions += QuickAction(getString(R.string.qa_airplane), false) {
            openSettings(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
        }
        actions += QuickAction(getString(R.string.qa_display), false) {
            openSettings(Settings.ACTION_DISPLAY_SETTINGS)
        }
        actions += QuickAction(getString(R.string.qa_settings), false) {
            openSettings(Settings.ACTION_SETTINGS)
        }

        val inflater = LayoutInflater.from(requireContext())
        val columns = 3
        var row: LinearLayout? = null
        actions.forEachIndexed { index, action ->
            if (index % columns == 0) {
                row = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
                }
                binding.quickActionsGrid.addView(row)
            }
            val button = inflater.inflate(
                R.layout.quick_action_button, row, false
            ) as TextView
            button.text = action.label
            button.layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply {
                val margin = (4 * resources.displayMetrics.density).toInt()
                setMargins(margin, margin, margin, margin)
            }
            button.setOnClickListener { action.onClick() }
            if (action.isFlashlight) flashlightButton = button
            row?.addView(button)
        }
        // Pad the final row so the last button isn't stretched full-width.
        val remainder = actions.size % columns
        if (remainder != 0) {
            repeat(columns - remainder) {
                val spacer = View(requireContext())
                spacer.layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
                row?.addView(spacer)
            }
        }
        updateFlashlightLabel()
    }

    private fun findFlashlightCamera(): String? {
        return try {
            cameraManager.cameraIdList.firstOrNull { id ->
                val hasFlash = cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val isBack = cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
                hasFlash && isBack
            } ?: cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun toggleFlashlight() {
        val id = flashlightCameraId ?: run {
            requireContext().showToast(getString(R.string.qa_flashlight_unavailable))
            return
        }
        try {
            cameraManager.setTorchMode(id, !flashlightOn)
        } catch (e: Exception) {
            e.printStackTrace()
            requireContext().showToast(getString(R.string.qa_flashlight_unavailable))
        }
    }

    private fun updateFlashlightLabel() {
        val base = getString(R.string.qa_flashlight)
        flashlightButton?.text =
            if (flashlightOn) "$base\n${getString(R.string.on)}" else base
    }

    private fun openSettings(action: String, fallback: String? = null) {
        try {
            startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            try {
                startActivity(Intent(fallback ?: Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }

    // endregion

    // region Notifications

    private fun refreshNotifications() {
        if (_binding == null) return
        val granted = isNotificationServiceEnabled(requireContext())
        binding.qaGrantAccess.isVisible = !granted
        if (!granted) {
            adapter.submitList(emptyList())
            binding.qaEmpty.isVisible = false
            binding.qaClearAll.isVisible = false
            return
        }
        val ownPackage = requireContext().packageName
        val items = NotificationDndService.activeNotifications()
            .asSequence()
            .filter { it.packageName != ownPackage }
            .mapNotNull { toItem(it) }
            .distinctBy { it.key }
            .toList()
        adapter.submitList(items)
        binding.qaEmpty.isVisible = items.isEmpty()
        binding.qaClearAll.isVisible = items.any { it.isClearable }
    }

    private fun toItem(sbn: StatusBarNotification): NotificationItem? {
        val notification = sbn.notification ?: return null
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT))?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return null
        return NotificationItem(
            key = sbn.key,
            appName = appLabel(sbn.packageName),
            title = title,
            text = text,
            contentIntent = notification.contentIntent,
            isClearable = sbn.isClearable,
        )
    }

    private fun appLabel(packageName: String): String {
        return try {
            val pm = requireContext().packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun onNotificationClick(item: NotificationItem) {
        try {
            item.contentIntent?.send()
            if (item.isClearable) NotificationDndService.dismiss(item.key)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun onNotificationDismiss(item: NotificationItem) {
        if (!item.isClearable) return
        NotificationDndService.dismiss(item.key)
        refreshNotifications()
    }

    private fun openNotificationAccessSettings() {
        try {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    // endregion

    override fun onDestroyView() {
        super.onDestroyView()
        flashlightButton = null
        _binding = null
    }
}
