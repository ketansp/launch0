package app.launch0.ui

import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import app.launch0.MainViewModel
import app.launch0.R
import app.launch0.data.AppModel
import app.launch0.data.Constants
import app.launch0.data.Prefs
import app.launch0.databinding.FragmentDistractionWaitBinding
import app.launch0.helper.DistractionTimer
import app.launch0.helper.appUsagePermissionGranted
import app.launch0.helper.formattedTimeSpent
import app.launch0.helper.getUserHandleFromString
import app.launch0.helper.showToast

/**
 * The wait screen shown when a distracting app is opened. A countdown leads top-left in the home
 * screen's dialect; below it a ledger of today's receipts (opens, screen time, last open, next
 * wait). The app opens only after the wait; "Turn back", back, home, or switching away resets the
 * timer. The attempt is counted the moment this screen appears, so in escalating mode the next
 * open waits longer either way.
 */
class DistractionWaitFragment : Fragment(), View.OnClickListener {

    private lateinit var prefs: Prefs
    private val viewModel: MainViewModel by activityViewModels()

    private var _binding: FragmentDistractionWaitBinding? = null
    private val binding get() = _binding!!

    private var countDownTimer: CountDownTimer? = null

    private var appName = ""
    private var appPackage = ""
    private var appActivityClass: String? = null
    private var appUser = ""
    private var isShortcut = false
    private var shortcutId = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDistractionWaitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())

        arguments?.let {
            appName = it.getString(Constants.Key.APP_NAME, "")
            appPackage = it.getString(Constants.Key.APP_PACKAGE, "")
            appActivityClass = it.getString(Constants.Key.APP_ACTIVITY_CLASS)
            appUser = it.getString(Constants.Key.APP_USER, "")
            isShortcut = it.getBoolean(Constants.Key.IS_SHORTCUT, false)
            shortcutId = it.getString(Constants.Key.SHORTCUT_ID, "")
        }
        if (appPackage.isEmpty()) {
            findNavController().popBackStack()
            return
        }

        // Count this attempt once (not again on config changes); the next wait doubles from here.
        if (savedInstanceState == null) DistractionTimer.recordAttempt(prefs, appPackage)
        // Opens today now include this attempt; this wait escalates from the opens before it.
        val opensToday = DistractionTimer.opensToday(prefs, appPackage)
        val waitSeconds = DistractionTimer.waitSeconds(prefs, (opensToday - 1).coerceAtLeast(0))

        populateLedger(opensToday)
        binding.tvWaitCaption.text = getString(R.string.dt_seconds_until, appName)
        binding.tvOpen.text = getString(R.string.dt_open_app, appName)
        binding.tvWaitNote.text = if (prefs.distractionWaitEscalating)
            getString(R.string.dt_waits_double)
        else
            getString(R.string.dt_fixed_wait_note, Constants.DistractionTimer.FIXED_WAIT_SECONDS)

        binding.readyGroup.setOnClickListener(this)
        binding.turnBackGroup.setOnClickListener(this)

        startCountdown(waitSeconds)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.readyGroup -> openApp()
            R.id.turnBackGroup -> turnBack()
        }
    }

    private fun populateLedger(opensToday: Int) {
        binding.tvOpensToday.text = opensToday.toString()
        binding.tvNextWait.text =
            getString(R.string.dt_seconds_value, DistractionTimer.waitSeconds(prefs, opensToday))
        binding.tvLastOpen.text = lastOpenLabel()
        populateScreenTime()
    }

    private fun populateScreenTime() {
        binding.tvScreenTimeValue.text = getString(R.string.dt_none)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        if (requireContext().appUsagePermissionGranted().not()) return

        viewModel.appScreenTimes.observe(viewLifecycleOwner) {
            val millis = it?.get(appPackage) ?: return@observe
            if (millis > 0) binding.tvScreenTimeValue.text = requireContext().formattedTimeSpent(millis)
        }
        viewModel.getTodaysScreenTime()
    }

    private fun lastOpenLabel(): String {
        val lastOpen = DistractionTimer.lastOpenMillis(prefs, appPackage)
        if (lastOpen == 0L) return getString(R.string.dt_none)
        val minutes = (System.currentTimeMillis() - lastOpen) / Constants.ONE_MINUTE_IN_MILLIS
        return when {
            minutes < 60 -> getString(R.string.dt_minutes_ago, minutes.coerceAtLeast(1))
            minutes < 60 * 24 -> getString(R.string.dt_hours_ago, minutes / 60)
            else -> getString(R.string.dt_days_ago, minutes / (60 * 24))
        }
    }

    private fun startCountdown(waitSeconds: Int) {
        binding.tvCountdown.text = waitSeconds.toString()
        countDownTimer = object : CountDownTimer(waitSeconds * 1000L, 250L) {
            override fun onTick(millisUntilFinished: Long) {
                if (_binding == null) return
                binding.tvCountdown.text = ((millisUntilFinished + 999) / 1000).toString()
            }

            override fun onFinish() {
                if (_binding == null) return
                binding.waitingGroup.isVisible = false
                binding.readyGroup.isVisible = true
            }
        }.start()
    }

    private fun openApp() {
        DistractionTimer.recordLaunch(prefs, appPackage)
        val user = getUserHandleFromString(requireContext(), appUser)
        val appModel = if (isShortcut && shortcutId.isNotEmpty())
            AppModel.PinnedShortcut(
                shortcutId = shortcutId,
                appLabel = appName,
                user = user,
                key = null,
                appPackage = appPackage,
                isNew = false,
            )
        else
            AppModel.App(
                appLabel = appName,
                key = null,
                appPackage = appPackage,
                activityClassName = appActivityClass,
                isNew = false,
                user = user,
            )
        viewModel.selectedApp(appModel, Constants.FLAG_LAUNCH_APP)
        findNavController().popBackStack(R.id.mainFragment, false)
    }

    private fun turnBack() {
        requireContext().showToast(
            getString(
                if (prefs.distractionWaitEscalating) R.string.dt_timer_reset_toast
                else R.string.dt_timer_reset_toast_fixed
            )
        )
        findNavController().popBackStack(R.id.mainFragment, false)
    }

    override fun onDestroyView() {
        countDownTimer?.cancel()
        countDownTimer = null
        super.onDestroyView()
        _binding = null
    }
}
