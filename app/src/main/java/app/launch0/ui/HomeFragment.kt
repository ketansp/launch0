package app.launch0.ui

import android.app.admin.DevicePolicyManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.provider.CalendarContract
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import app.launch0.MainViewModel
import app.launch0.R
import app.launch0.data.AppModel
import app.launch0.data.CalendarEvent
import app.launch0.data.Constants
import app.launch0.data.Prefs
import app.launch0.databinding.FragmentHomeBinding
import app.launch0.helper.DistractionTimer
import app.launch0.helper.hasCalendarPermission
import app.launch0.helper.NotificationDndService
import app.launch0.helper.appUsagePermissionGranted
import app.launch0.helper.combineDrawablesHorizontally
import app.launch0.helper.dpToPx
import app.launch0.helper.getWaitCapsuleDrawable
import app.launch0.helper.expandNotificationDrawer
import app.launch0.helper.getChangedAppTheme
import app.launch0.helper.getColorFromAttr
import app.launch0.helper.getNotificationCountDrawable
import app.launch0.helper.getScreenTimeCapsuleDrawable
import app.launch0.helper.getShapedAppIcon
import app.launch0.helper.getUserHandleFromString
import app.launch0.helper.isEinkDisplay
import app.launch0.helper.isPackageInstalled
import app.launch0.helper.openAlarmApp
import app.launch0.helper.pillTouchListener
import app.launch0.helper.openCalendar
import app.launch0.helper.openCameraApp
import app.launch0.helper.openDialerApp
import app.launch0.helper.openSearch
import app.launch0.helper.setPlainWallpaperByTheme
import app.launch0.helper.showToast
import app.launch0.listener.OnSwipeTouchListener
import app.launch0.listener.ViewSwipeTouchListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment(), View.OnClickListener, View.OnLongClickListener {

    companion object {
        // Only show a home app's usage capsule once it has been used for more than this many minutes.
        private const val SCREEN_TIME_MIN_MINUTES = 5
    }

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Today's foreground time per package (millis); drives the per-app usage capsules on home apps.
    private var appScreenTimes: Map<String, Long> = emptyMap()

    // Home-screen widgets, each wrapped in a shared card and paged through the swipeable carousel.
    private var calendarView: CalendarWidgetView? = null
    private var yearView: YearProgressView? = null
    private var calendarCard: WidgetCard? = null
    private var yearCard: WidgetCard? = null

    // The cards currently in the pager, so the adapter is only rebuilt when the enabled set changes.
    private var widgetPages: List<View> = emptyList()

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            if (_binding == null) return
            updateWidgetDots(position, binding.widgetPager.adapter?.itemCount ?: 0)
        }
    }

    // Whether the next calendar-events render should jump to the current/next event. True for a fresh
    // show (resume/toggle), false for the minute ticker so it doesn't yank the user's scroll position.
    private var calendarAutoScroll = true

    // Ticks the calendar widget once a minute while the home screen is showing so countdowns stay
    // live and events roll from upcoming → now → past on their own.
    private val calendarTickHandler = Handler(Looper.getMainLooper())
    private val calendarTicker = object : Runnable {
        override fun run() {
            if (_binding == null || !prefs.showCalendarWidget) return
            refreshCalendarWidget(autoScroll = false)
            calendarTickHandler.postDelayed(this, Constants.ONE_MINUTE_IN_MILLIS)
        }
    }

    // Requests READ_CALENDAR when the user taps the widget's "grant access" prompt. On grant we keep
    // the widget on and repopulate; on denial the widget quietly falls back to the prompt.
    private val calendarPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (_binding == null) return@registerForActivityResult
            if (granted) setupWidgets()
            else requireContext().showToast(getString(R.string.calendar_permission_denied))
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        deviceManager = context?.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        initObservers()
        setHomeAlignment(prefs.homeAlignment)
        initSwipeTouchListener()
        initClickListeners()
        initWidgets()
        initSwipeUpNudge()
        initSwipeLeftNudge()
    }

    /**
     * Builds the home-screen widgets once — each in a shared [WidgetCard] — and wires the calendar's
     * taps (an event opens it in the calendar, the prompt requests access) and the pager's dots.
     * Which cards are actually shown, and in what order, is decided later by [setupWidgets].
     */
    private fun initWidgets() {
        val ctx = requireContext()
        calendarView = CalendarWidgetView(ctx).also { cv ->
            cv.setOnEventClickListener { openCalendarEvent(it) }
            cv.setOnHeaderClickListener { openCalendarApp() }
            cv.setOnMessageClickListener {
                if (requireContext().hasCalendarPermission()) openCalendarApp()
                else calendarPermissionLauncher.launch(android.Manifest.permission.READ_CALENDAR)
            }
        }
        val fill = FrameLayout.LayoutParams.MATCH_PARENT
        calendarCard = WidgetCard(ctx).apply {
            addView(calendarView, FrameLayout.LayoutParams(fill, fill))
        }
        yearView = YearProgressView(ctx)
        yearCard = WidgetCard(ctx).apply {
            addView(yearView, FrameLayout.LayoutParams(fill, fill))
        }
        binding.widgetPager.registerOnPageChangeCallback(pageChangeCallback)
        // A gap of wallpaper between cards as they slide, so adjacent widgets read as separate.
        binding.widgetPager.setPageTransformer(MarginPageTransformer(20.dpToPx()))
    }

    /**
     * Starts the gentle bobbing/pulse hint that swiping up opens the app list. The hint retires once
     * the user has opened the app drawer enough times. The icon is left static on e-ink panels,
     * where a perpetual animation would constantly redraw the screen.
     */
    private fun initSwipeUpNudge() {
        if (prefs.appDrawerOpenCount >= Constants.NUDGE_DISMISS_AFTER) {
            binding.swipeUpNudge.visibility = View.GONE
            return
        }
        if (requireContext().isEinkDisplay()) {
            binding.swipeUpNudge.alpha = 0.5f
            return
        }
        binding.swipeUpNudge.startAnimation(
            AnimationUtils.loadAnimation(requireContext(), R.anim.swipe_up_nudge)
        )
    }

    /**
     * Starts the gentle sideways drift/pulse hint that a right-to-left swipe opens notes. Only shown
     * while a left swipe is actually wired to notes, and retired once the user has opened notes
     * enough times. Left static on e-ink panels to avoid constant redraws.
     */
    private fun initSwipeLeftNudge() {
        val opensNotes = prefs.swipeLeftAction == Constants.SwipeLeftAction.NOTES
        if (!opensNotes || prefs.notesOpenCount >= Constants.NUDGE_DISMISS_AFTER) {
            binding.swipeLeftNudge.visibility = View.GONE
            return
        }
        binding.swipeLeftNudge.visibility = View.VISIBLE
        if (requireContext().isEinkDisplay()) {
            binding.swipeLeftNudge.alpha = 0.5f
            return
        }
        binding.swipeLeftNudge.startAnimation(
            AnimationUtils.loadAnimation(requireContext(), R.anim.swipe_left_nudge)
        )
    }

    override fun onResume() {
        super.onResume()
        populateHomeScreen(false)
        viewModel.isLaunch0Default()
        if (prefs.showStatusBar) showStatusBar()
        else hideStatusBar()
        startCalendarTicker()
    }

    override fun onPause() {
        super.onPause()
        stopCalendarTicker()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.lock -> {}
            R.id.clock -> openClockApp()
            R.id.date -> openCalendarApp()
            R.id.setDefaultLauncher -> viewModel.resetLauncherLiveData.call()
            R.id.tvScreenTime -> openScreenTimeDigitalWellbeing()

            else -> {
                try { // Launch app
                    val appLocation = view.tag.toString().toInt()
                    homeAppClicked(appLocation)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun openClockApp() {
        if (prefs.clockAppPackage.isBlank())
            openAlarmApp(requireContext())
        else
            launchApp(
                "Clock",
                prefs.clockAppPackage,
                prefs.clockAppClassName,
                prefs.clockAppUser
            )
    }

    private fun openCalendarApp() {
        if (prefs.calendarAppPackage.isBlank())
            openCalendar(requireContext())
        else
            launchApp(
                "Calendar",
                prefs.calendarAppPackage,
                prefs.calendarAppClassName,
                prefs.calendarAppUser
            )
    }

    override fun onLongClick(view: View): Boolean {
        when (view.id) {
            R.id.homeApp1 -> showAppList(Constants.FLAG_SET_HOME_APP_1, prefs.appName1.isNotEmpty(), true)
            R.id.homeApp2 -> showAppList(Constants.FLAG_SET_HOME_APP_2, prefs.appName2.isNotEmpty(), true)
            R.id.homeApp3 -> showAppList(Constants.FLAG_SET_HOME_APP_3, prefs.appName3.isNotEmpty(), true)
            R.id.homeApp4 -> showAppList(Constants.FLAG_SET_HOME_APP_4, prefs.appName4.isNotEmpty(), true)
            R.id.homeApp5 -> showAppList(Constants.FLAG_SET_HOME_APP_5, prefs.appName5.isNotEmpty(), true)
            R.id.homeApp6 -> showAppList(Constants.FLAG_SET_HOME_APP_6, prefs.appName6.isNotEmpty(), true)
            R.id.homeApp7 -> showAppList(Constants.FLAG_SET_HOME_APP_7, prefs.appName7.isNotEmpty(), true)
            R.id.homeApp8 -> showAppList(Constants.FLAG_SET_HOME_APP_8, prefs.appName8.isNotEmpty(), true)
            R.id.clock -> {
                showAppList(Constants.FLAG_SET_CLOCK_APP)
                prefs.clockAppPackage = ""
                prefs.clockAppClassName = ""
                prefs.clockAppUser = ""
            }

            R.id.date -> {
                showAppList(Constants.FLAG_SET_CALENDAR_APP)
                prefs.calendarAppPackage = ""
                prefs.calendarAppClassName = ""
                prefs.calendarAppUser = ""
            }

            R.id.setDefaultLauncher -> {
                prefs.hideSetDefaultLauncher = true
                binding.setDefaultLauncher.visibility = View.GONE
                if (viewModel.isLaunch0Default.value != true) {
                    requireContext().showToast(R.string.set_as_default_launcher)
                    findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
                }
            }
        }
        return true
    }

    private fun initObservers() {
        viewModel.refreshHome.observe(viewLifecycleOwner) {
            populateHomeScreen(it)
        }
        viewModel.isLaunch0Default.observe(viewLifecycleOwner, Observer {
            if (it != true) {
                if (prefs.hourlyWallpaper) {
                    prefs.hourlyWallpaper = false
                    viewModel.cancelWallpaperWorker()
                }
            }
            // Re-align whenever default-launcher status changes. While Launch0 isn't the
            // default launcher we render apps centered (so they don't collide with the
            // "Set as default" prompt at the bottom) without touching the saved preference.
            setHomeAlignment()
            binding.setDefaultLauncher.isVisible = it.not() && prefs.hideSetDefaultLauncher.not()
//            if (it) binding.setDefaultLauncher.visibility = View.GONE
//            else binding.setDefaultLauncher.visibility = View.VISIBLE
        })
        viewModel.homeAppAlignment.observe(viewLifecycleOwner) {
            setHomeAlignment(it)
        }
        viewModel.toggleDateTime.observe(viewLifecycleOwner) {
            populateDateTime()
        }
        viewModel.toggleWidget.observe(viewLifecycleOwner) {
            setupWidgets()
        }
        viewModel.toggleCalendarWidget.observe(viewLifecycleOwner) {
            setupWidgets()
        }
        viewModel.calendarEvents.observe(viewLifecycleOwner) {
            bindCalendarEvents(it)
        }
        viewModel.screenTimeValue.observe(viewLifecycleOwner) {
            it?.let { binding.tvScreenTime.text = it }
        }
        viewModel.appScreenTimes.observe(viewLifecycleOwner) {
            appScreenTimes = it ?: emptyMap()
            // Re-decorate home apps so each name picks up its refreshed usage capsule.
            populateHomeScreen(false)
        }
    }

    private fun initSwipeTouchListener() {
        val context = requireContext()
        binding.mainLayout.setOnTouchListener(getSwipeGestureListener(context))
        val homeApps = listOf(
            binding.homeApp1, binding.homeApp2, binding.homeApp3, binding.homeApp4,
            binding.homeApp5, binding.homeApp6, binding.homeApp7, binding.homeApp8,
        )
        homeApps.forEachIndexed { index, textView ->
            setHomeAppTouchListener(textView, index + 1)
        }
    }

    /**
     * Combines the per-app swipe gestures with the notification-count pill: a touch starting on the
     * pill releases the app's parked notifications, anything else falls through to the swipe handler.
     */
    private fun setHomeAppTouchListener(textView: TextView, location: Int) {
        val swipeListener = getViewSwipeTouchListener(requireContext(), textView)
        textView.setOnTouchListener(
            pillTouchListener(delegate = { v, event -> swipeListener.onTouch(v, event) }) {
                releaseNotificationsForHomeApp(location)
            }
        )
    }

    private fun initClickListeners() {
        binding.lock.setOnClickListener(this)
        binding.clock.setOnClickListener(this)
        binding.date.setOnClickListener(this)
        binding.clock.setOnLongClickListener(this)
        binding.date.setOnLongClickListener(this)
        binding.setDefaultLauncher.setOnClickListener(this)
        binding.setDefaultLauncher.setOnLongClickListener(this)
        binding.tvScreenTime.setOnClickListener(this)
    }

    private fun setHomeAlignment(horizontalGravity: Int = prefs.homeAlignment) {
        val bottomAligned = prefs.homeBottomAlignment && viewModel.isLaunch0Default.value == true
        val verticalGravity = if (bottomAligned) Gravity.BOTTOM else Gravity.CENTER_VERTICAL
        binding.homeAppsLayout.gravity = horizontalGravity or verticalGravity
        binding.dateTimeLayout.gravity = horizontalGravity
        // Keep the date's (now multi-line, with the battery line) text aligned with the home edge.
        binding.date.gravity = horizontalGravity

        // Left/right alignment stretches each row to the full width so the usage capsule and
        // notification pill (drawn as the compound drawable opposite the name) land on the far edge,
        // matching the app drawer. Centre keeps rows wrap-content so the icon stays beside the name.
        val rowWidth = if (horizontalGravity == Gravity.CENTER)
            ViewGroup.LayoutParams.WRAP_CONTENT else ViewGroup.LayoutParams.MATCH_PARENT
        listOf(
            binding.homeApp1, binding.homeApp2, binding.homeApp3, binding.homeApp4,
            binding.homeApp5, binding.homeApp6, binding.homeApp7, binding.homeApp8,
        ).forEach { homeApp ->
            homeApp.gravity = horizontalGravity
            if (homeApp.layoutParams.width != rowWidth) {
                homeApp.layoutParams = homeApp.layoutParams.apply { width = rowWidth }
            }
        }
    }

    private fun populateDateTime() {
        binding.dateTimeLayout.isVisible = prefs.dateTimeVisibility != Constants.DateTime.OFF
        binding.clock.isVisible = Constants.DateTime.isTimeVisible(prefs.dateTimeVisibility)
        binding.date.isVisible = Constants.DateTime.isDateVisible(prefs.dateTimeVisibility)

//        var dateText = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date())
        val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
        var dateText = dateFormat.format(Date())

        if (!prefs.showStatusBar) {
            val battery = (requireContext().getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
                .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            if (battery > 0)
                dateText = getString(R.string.day_battery, dateText, battery)
        }
        binding.date.text = dateText.replace(".,", ",")
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun populateScreenTime() {
        if (requireContext().appUsagePermissionGranted().not()) return

        viewModel.getTodaysScreenTime()
        binding.tvScreenTime.visibility = View.VISIBLE

        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val horizontalMargin = if (isLandscape) 64.dpToPx() else 10.dpToPx()
        val marginTop = if (isLandscape) {
            if (prefs.dateTimeVisibility == Constants.DateTime.DATE_ONLY) 36.dpToPx() else 56.dpToPx()
        } else {
            if (prefs.dateTimeVisibility == Constants.DateTime.DATE_ONLY) 45.dpToPx() else 72.dpToPx()
        }
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = marginTop
            marginStart = horizontalMargin
            marginEnd = horizontalMargin
            gravity = if (prefs.homeAlignment == Gravity.END) Gravity.START else Gravity.END
        }
        binding.tvScreenTime.layoutParams = params
        binding.tvScreenTime.setPadding(10.dpToPx())
    }

    /**
     * (Re)builds the widget carousel from the enabled widgets. Each enabled widget's card becomes a
     * page in a stable order (calendar, then days-left); the pager and its dots hide entirely when
     * none are on. Called on resume and whenever a widget is toggled. It only re-populates the pager
     * when the enabled set actually changed — so a plain resume doesn't flicker — then refreshes
     * contents.
     */
    private fun setupWidgets() {
        if (_binding == null) return
        val pager = binding.widgetPager
        val desired = mutableListOf<View>()
        if (prefs.showCalendarWidget) calendarCard?.let { desired += it }
        if (prefs.showYearWidget) yearCard?.let { desired += it }

        binding.widgetPagerLayout.isVisible = desired.isNotEmpty()
        if (desired.isEmpty()) {
            widgetPages = emptyList()
            pager.adapter = null
            buildWidgetDots(0)
            stopCalendarTicker()
            return
        }

        // Only swap the adapter when the enabled set actually changed, so a plain resume doesn't
        // reset the current page or flicker.
        if (desired != widgetPages) {
            val previousPage = pager.currentItem
            widgetPages = desired.toList()
            pager.isUserInputEnabled = desired.size > 1 // one widget: let home swipes pass through
            pager.offscreenPageLimit = (desired.size - 1).coerceAtLeast(1) // keep every page laid out
            pager.adapter = WidgetPagerAdapter(desired)
            pager.setCurrentItem(previousPage.coerceIn(0, desired.size - 1), false)
            buildWidgetDots(desired.size)
        }

        // Refresh theme + contents of whatever is enabled.
        yearCard?.refresh()
        yearView?.refresh()
        if (prefs.showCalendarWidget) {
            calendarCard?.refresh()
            calendarView?.refresh()
            if (requireContext().hasCalendarPermission()) {
                // A fresh show jumps to the current/next event; events arrive via bindCalendarEvents().
                calendarAutoScroll = true
                viewModel.loadCalendarEvents()
            } else {
                calendarView?.showMessage(getString(R.string.calendar_grant_access))
            }
        }
        positionWidgetPagerBelowHeader()
    }

    /** Re-reads events (used by the minute ticker); [autoScroll] false keeps the current scroll. */
    private fun refreshCalendarWidget(autoScroll: Boolean) {
        if (_binding == null) return
        if (!prefs.showCalendarWidget || !requireContext().hasCalendarPermission()) return
        calendarAutoScroll = autoScroll
        viewModel.loadCalendarEvents()
    }

    private fun bindCalendarEvents(events: List<CalendarEvent>) {
        if (_binding == null || !prefs.showCalendarWidget) return
        val cv = calendarView ?: return
        when {
            // Access revoked since the events were loaded — fall back to the prompt, not "no events".
            !requireContext().hasCalendarPermission() ->
                cv.showMessage(getString(R.string.calendar_grant_access))
            events.isEmpty() ->
                cv.showMessage(getString(R.string.calendar_no_events))
            else ->
                cv.showEvents(events, autoScrollToNow = calendarAutoScroll)
        }
    }

    /** Runs the minute ticker while the home screen is visible; only when the calendar widget is on. */
    private fun startCalendarTicker() {
        calendarTickHandler.removeCallbacks(calendarTicker)
        if (prefs.showCalendarWidget)
            calendarTickHandler.postDelayed(calendarTicker, Constants.ONE_MINUTE_IN_MILLIS)
    }

    private fun stopCalendarTicker() {
        calendarTickHandler.removeCallbacks(calendarTicker)
    }

    /**
     * Anchors the widget carousel just below whichever header is currently showing — the clock/date
     * block and/or the screen-time label — so it never overlaps them, regardless of font size,
     * density or which headers are enabled. Falls back to a fixed top margin when no header is
     * visible. Posted so the headers are measured first.
     */
    private fun positionWidgetPagerBelowHeader() {
        binding.widgetPagerLayout.post {
            if (_binding == null) return@post
            var top = 56.dpToPx()
            if (binding.dateTimeLayout.isVisible)
                top = maxOf(top, binding.dateTimeLayout.bottom)
            if (binding.tvScreenTime.isVisible)
                top = maxOf(top, binding.tvScreenTime.bottom)
            val params = binding.widgetPagerLayout.layoutParams as FrameLayout.LayoutParams
            val target = top + 16.dpToPx()
            if (params.topMargin != target) {
                params.topMargin = target
                binding.widgetPagerLayout.layoutParams = params
            }
        }
    }

    /** Rebuilds the row of page dots for [count] widgets (hidden when there are 0 or 1). */
    private fun buildWidgetDots(count: Int) {
        val dots = binding.widgetDots
        dots.removeAllViews()
        if (count <= 1) {
            dots.isVisible = false
            return
        }
        dots.isVisible = true
        val color = requireContext().getColorFromAttr(R.attr.primaryColor)
        val size = 6.dpToPx()
        repeat(count) {
            val dot = View(requireContext()).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                }
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginStart = 4.dpToPx()
                    marginEnd = 4.dpToPx()
                }
            }
            dots.addView(dot)
        }
        updateWidgetDots(binding.widgetPager.currentItem, count)
    }

    /** Brightens the dot for the current page and dims the rest. */
    private fun updateWidgetDots(index: Int, count: Int) {
        if (_binding == null) return
        val dots = binding.widgetDots
        if (count <= 1) {
            dots.isVisible = false
            return
        }
        for (i in 0 until dots.childCount)
            dots.getChildAt(i).alpha = if (i == index) 0.9f else 0.3f
    }

    /** Opens the tapped event in the calendar app, falling back to the calendar's day view. */
    private fun openCalendarEvent(event: CalendarEvent) {
        try {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id)
            startActivity(Intent(Intent.ACTION_VIEW).setData(uri))
        } catch (e: Exception) {
            openCalendarApp()
        }
    }

    private fun populateHomeScreen(appCountUpdated: Boolean) {
        if (appCountUpdated) hideHomeApps()
        populateDateTime()
        setupWidgets()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            populateScreenTime()

        val homeAppsNum = prefs.homeAppsNum
        if (homeAppsNum == 0) return

        binding.homeApp1.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp1, prefs.appName1, prefs.appPackage1, prefs.appUser1, prefs.isShortcut1, prefs.shortcutId1)) {
            prefs.appName1 = ""
            prefs.appPackage1 = ""
        }
        if (homeAppsNum == 1) return

        binding.homeApp2.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp2, prefs.appName2, prefs.appPackage2, prefs.appUser2, prefs.isShortcut2, prefs.shortcutId2)) {
            prefs.appName2 = ""
            prefs.appPackage2 = ""
        }
        if (homeAppsNum == 2) return

        binding.homeApp3.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp3, prefs.appName3, prefs.appPackage3, prefs.appUser3, prefs.isShortcut3, prefs.shortcutId3)) {
            prefs.appName3 = ""
            prefs.appPackage3 = ""
        }
        if (homeAppsNum == 3) return

        binding.homeApp4.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp4, prefs.appName4, prefs.appPackage4, prefs.appUser4, prefs.isShortcut4, prefs.shortcutId4)) {
            prefs.appName4 = ""
            prefs.appPackage4 = ""
        }
        if (homeAppsNum == 4) return

        binding.homeApp5.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp5, prefs.appName5, prefs.appPackage5, prefs.appUser5, prefs.isShortcut5, prefs.shortcutId5)) {
            prefs.appName5 = ""
            prefs.appPackage5 = ""
        }
        if (homeAppsNum == 5) return

        binding.homeApp6.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp6, prefs.appName6, prefs.appPackage6, prefs.appUser6, prefs.isShortcut6, prefs.shortcutId6)) {
            prefs.appName6 = ""
            prefs.appPackage6 = ""
        }
        if (homeAppsNum == 6) return

        binding.homeApp7.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp7, prefs.appName7, prefs.appPackage7, prefs.appUser7, prefs.isShortcut7, prefs.shortcutId7)) {
            prefs.appName7 = ""
            prefs.appPackage7 = ""
        }
        if (homeAppsNum == 7) return

        binding.homeApp8.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp8, prefs.appName8, prefs.appPackage8, prefs.appUser8, prefs.isShortcut8, prefs.shortcutId8)) {
            prefs.appName8 = ""
            prefs.appPackage8 = ""
        }
    }

    private fun setHomeAppText(textView: TextView, appName: String, packageName: String, userString: String, isShortcut: Boolean, shortcutId: String?): Boolean {
        // Get user handle for the app/shortcut
        val userHandle = getUserHandleFromString(requireContext(), userString)
        
        // If it's a shortcut, verify it still exists
        if (isShortcut) {
            val launcherApps = requireContext().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            
            // Query for the specific shortcut
            val query = LauncherApps.ShortcutQuery().apply {
                setPackage(packageName)
                setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
            }
            
            try {
                val shortcuts = launcherApps.getShortcuts(query, userHandle)
                // Check if our shortcut still exists
                if (shortcuts?.any { it.id == shortcutId } == true) {
                    textView.text = appName
                    setHomeAppDecorations(textView, packageName, userString)
                    return true
                }
                textView.text = ""
                setHomeAppDecorations(textView, "", userString)
                return false
            } catch (e: Exception) {
                e.printStackTrace()
                textView.text = ""
                setHomeAppDecorations(textView, "", userString)
                return false
            }
        }

        // Regular app check
        if (isPackageInstalled(requireContext(), packageName, userString)) {
            textView.text = appName
            setHomeAppDecorations(textView, packageName, userString)
            return true
        }
        textView.text = ""
        setHomeAppDecorations(textView, "", userString)
        return false
    }

    /**
     * Decorates [textView] with the app icon (when enabled) and, on the side opposite the app name,
     * any combination of a parked-notification count pill (when DND has held notifications back) and
     * a screen-time usage capsule (when the app has been used for more than [SCREEN_TIME_MIN_MINUTES]
     * today). The icon sits on the same side as the home layout alignment (right when right-aligned).
     * Tags the view so the notification pill alone can be hit-tested for taps (which release the
     * parked notifications), even when it shares the slot with the usage capsule.
     */
    private fun setHomeAppDecorations(textView: TextView, packageName: String, userString: String) {
        val sizePx = prefs.iconSize.dpToPx()
        val icon = if (prefs.showAppIcons && packageName.isNotEmpty())
            requireContext().getShapedAppIcon(packageName, userString, sizePx, prefs.iconShape)
        else null
        icon?.setBounds(0, 0, sizePx, sizePx)

        val count = NotificationDndService.parkedCount(prefs, packageName)
        val notifPill = if (count > 0) requireContext().getNotificationCountDrawable(count) else null

        val usageMinutes = screenTimeMinutes(packageName)
        val timeCapsule = if (usageMinutes > SCREEN_TIME_MIN_MINUTES)
            requireContext().getScreenTimeCapsuleDrawable(usageMinutes) else null

        // Distracting apps read recessed on home: name dimmed, next wait shown beside it.
        val isDistracting = DistractionTimer.isDistractingApp(prefs, packageName)
        val waitCapsule = if (isDistracting)
            requireContext().getWaitCapsuleDrawable(DistractionTimer.nextWaitSeconds(prefs, packageName))
        else null
        textView.alpha = if (isDistracting) 0.5f else 1f

        val iconOnEnd = prefs.homeAlignment == Gravity.END

        // All decorations share the slot opposite the name. The notification pill goes on the slot's
        // outer edge so its tap target keeps lining up with that edge; the capsules sit inboard.
        val inwards = listOfNotNull(notifPill, timeCapsule, waitCapsule)
        val opposite = when {
            inwards.size > 1 ->
                requireContext().combineDrawablesHorizontally(
                    if (iconOnEnd) inwards else inwards.reversed(),
                    6.dpToPx(),
                )
            else -> inwards.firstOrNull()
        }

        if (icon == null && opposite == null) {
            textView.setCompoundDrawables(null, null, null, null)
            textView.setTag(R.id.notif_pill_side, null)
            textView.setTag(R.id.notif_pill_width, null)
            return
        }

        // Icon on the alignment side, decorations on the opposite (start) side when right-aligned.
        val startDrawable = if (iconOnEnd) opposite else icon
        val endDrawable = if (iconOnEnd) icon else opposite
        textView.setCompoundDrawablesRelative(startDrawable, null, endDrawable, null)
        textView.compoundDrawablePadding = 12.dpToPx()
        textView.setTag(R.id.notif_pill_side, if (notifPill != null) iconOnEnd else null)
        textView.setTag(R.id.notif_pill_width, notifPill?.bounds?.width())
    }

    /** Rounded minutes of foreground time the app at [packageName] has had today, 0 if unknown. */
    private fun screenTimeMinutes(packageName: String): Int {
        if (packageName.isEmpty()) return 0
        val millis = appScreenTimes[packageName] ?: return 0
        return Math.round(millis / 60000.0).toInt()
    }

    private fun releaseNotificationsForHomeApp(location: Int) {
        val packageName = prefs.getAppPackage(location)
        if (packageName.isBlank()) return
        if (NotificationDndService.parkedCount(prefs, packageName) == 0) return
        NotificationDndService.releaseForPackage(prefs, packageName)
        populateHomeScreen(false)
        requireContext().showToast(getString(R.string.dnd_released))
    }

    private fun hideHomeApps() {
        binding.homeApp1.visibility = View.GONE
        binding.homeApp2.visibility = View.GONE
        binding.homeApp3.visibility = View.GONE
        binding.homeApp4.visibility = View.GONE
        binding.homeApp5.visibility = View.GONE
        binding.homeApp6.visibility = View.GONE
        binding.homeApp7.visibility = View.GONE
        binding.homeApp8.visibility = View.GONE
    }

    private fun launchAppOrShortcut(
        appName: String,
        packageName: String,
        activityClassName: String?,
        shortcutId: String?,
        isShortcut: Boolean,
        userString: String,
        fallback: (() -> Unit)? = null
    ) {
        if (appName.isEmpty()) {
            showLongPressToast()
            return
        }
        if (DistractionTimer.isDistractingApp(prefs, packageName)) {
            openDistractionWait(appName, packageName, activityClassName, shortcutId, isShortcut, userString)
            return
        }
        if (isShortcut && !shortcutId.isNullOrEmpty()) {
            launchShortcut(
                packageName = packageName,
                shortcutId = shortcutId,
                shortcutLabel = appName,
                userString = userString
            )
        } else if (packageName.isNotEmpty()) {
            launchApp(
                appName = appName,
                packageName = packageName,
                activityClassName = activityClassName,
                userString = userString
            )
        } else {
            fallback?.invoke()
        }
    }

    private fun launchShortcut(shortcutId: String, packageName: String, shortcutLabel: String, userString: String) {
        viewModel.selectedApp(
            AppModel.PinnedShortcut(
                shortcutId = shortcutId,
                appLabel = shortcutLabel,
                user = getUserHandleFromString(requireContext(), userString),
                key = null,
                appPackage = packageName,
                isNew = false,
            ),
            Constants.FLAG_LAUNCH_APP
        )
    }

    private fun launchApp(appName: String, packageName: String, activityClassName: String?, userString: String) {
        viewModel.selectedApp(
            AppModel.App(
                appLabel = appName,
                key = null,
                appPackage = packageName,
                activityClassName = activityClassName,
                isNew = false,
                user = getUserHandleFromString(requireContext(), userString)
            ),
            Constants.FLAG_LAUNCH_APP
        )
    }

    /** Routes a distracting app through the wait screen instead of opening it straight away. */
    private fun openDistractionWait(
        appName: String,
        packageName: String,
        activityClassName: String?,
        shortcutId: String?,
        isShortcut: Boolean,
        userString: String,
    ) {
        try {
            findNavController().navigate(
                R.id.action_mainFragment_to_distractionWaitFragment,
                bundleOf(
                    Constants.Key.APP_NAME to appName,
                    Constants.Key.APP_PACKAGE to packageName,
                    Constants.Key.APP_ACTIVITY_CLASS to activityClassName,
                    Constants.Key.SHORTCUT_ID to shortcutId,
                    Constants.Key.IS_SHORTCUT to isShortcut,
                    Constants.Key.APP_USER to userString,
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun homeAppClicked(location: Int) {
        launchAppOrShortcut(
            appName = prefs.getAppName(location),
            packageName = prefs.getAppPackage(location),
            activityClassName = prefs.getAppActivityClassName(location),
            shortcutId = prefs.getShortcutId(location),
            isShortcut = prefs.getIsShortcut(location),
            userString = prefs.getAppUser(location)
        )
    }

    private fun openSwipeRightApp() {
        if (!prefs.swipeRightEnabled) return
        launchAppOrShortcut(
            appName = prefs.appNameSwipeRight,
            packageName = prefs.appPackageSwipeRight,
            activityClassName = prefs.appActivityClassNameRight,
            shortcutId = prefs.shortcutIdSwipeRight,
            isShortcut = prefs.isShortcutSwipeRight,
            userString = prefs.appUserSwipeRight,
            fallback = { openDialerApp(requireContext()) }
        )
    }

    private fun swipeLeftAction() {
        when (prefs.swipeLeftAction) {
            Constants.SwipeLeftAction.APP -> openSwipeLeftApp()
            else -> openNotesPage()
        }
    }

    private fun openNotesPage() {
        if (prefs.notesOpenCount < Constants.NUDGE_DISMISS_AFTER)
            prefs.notesOpenCount++
        try {
            findNavController().navigate(R.id.action_mainFragment_to_notesFragment)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openSwipeLeftApp() {
        if (!prefs.swipeLeftEnabled) return
        launchAppOrShortcut(
            appName = prefs.appNameSwipeLeft,
            packageName = prefs.appPackageSwipeLeft,
            activityClassName = prefs.appActivityClassNameSwipeLeft,
            shortcutId = prefs.shortcutIdSwipeLeft,
            isShortcut = prefs.isShortcutSwipeLeft,
            userString = prefs.appUserSwipeLeft,
            fallback = { openCameraApp(requireContext()) }
        )
    }

    private fun showAppList(flag: Int, rename: Boolean = false, includeHiddenApps: Boolean = false) {
        // Count genuine app-drawer opens (launching an app) so the swipe-up hint can retire.
        if (flag == Constants.FLAG_LAUNCH_APP && prefs.appDrawerOpenCount < Constants.NUDGE_DISMISS_AFTER)
            prefs.appDrawerOpenCount++
        viewModel.getAppList(includeHiddenApps)
        try {
            findNavController().navigate(
                R.id.action_mainFragment_to_appListFragment,
                bundleOf(
                    Constants.Key.FLAG to flag,
                    Constants.Key.RENAME to rename
                )
            )
        } catch (e: Exception) {
            findNavController().navigate(
                R.id.appListFragment,
                bundleOf(
                    Constants.Key.FLAG to flag,
                    Constants.Key.RENAME to rename
                )
            )
            e.printStackTrace()
        }
    }

    private fun swipeDownAction() {
        when (prefs.swipeDownAction) {
            Constants.SwipeDownAction.SEARCH -> openSearch(requireContext())
            else -> expandNotificationDrawer(requireContext())
        }
    }

    private fun lockPhone() {
        requireActivity().runOnUiThread {
            try {
                deviceManager.lockNow()
            } catch (e: SecurityException) {
                requireContext().showToast(getString(R.string.please_turn_on_double_tap_to_unlock), Toast.LENGTH_LONG)
                findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
            } catch (e: Exception) {
                requireContext().showToast(getString(R.string.launcher_failed_to_lock_device), Toast.LENGTH_LONG)
                prefs.lockModeOn = false
            }
        }
    }

    private fun showStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            requireActivity().window.insetsController?.show(WindowInsets.Type.statusBars())
        else
            @Suppress("DEPRECATION", "InlinedApi")
            requireActivity().window.decorView.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
    }

    private fun hideStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            requireActivity().window.insetsController?.hide(WindowInsets.Type.statusBars())
        else {
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_FULLSCREEN
            }
        }
    }

    private fun changeAppTheme() {
        if (prefs.hourlyWallpaper.not()) return
        val changedAppTheme = getChangedAppTheme(requireContext(), prefs.appTheme)
        prefs.appTheme = changedAppTheme
        if (prefs.hourlyWallpaper) {
            setPlainWallpaperByTheme(requireContext(), changedAppTheme)
            viewModel.setWallpaperWorker()
        }
        requireActivity().recreate()
    }

    private fun openScreenTimeDigitalWellbeing() {
        val intent = Intent()
        try {
            intent.setClassName(
                Constants.DIGITAL_WELLBEING_PACKAGE_NAME,
                Constants.DIGITAL_WELLBEING_ACTIVITY
            )
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                intent.setClassName(
                    Constants.DIGITAL_WELLBEING_SAMSUNG_PACKAGE_NAME,
                    Constants.DIGITAL_WELLBEING_SAMSUNG_ACTIVITY
                )
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showLongPressToast() = requireContext().showToast(getString(R.string.long_press_to_select_app))

    private fun textOnClick(view: View) = onClick(view)

    private fun textOnLongClick(view: View) = onLongClick(view)

    private fun getSwipeGestureListener(context: Context): View.OnTouchListener {
        return object : OnSwipeTouchListener(context) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                swipeLeftAction()
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                openSwipeRightApp()
            }

            override fun onSwipeUp() {
                super.onSwipeUp()
                showAppList(Constants.FLAG_LAUNCH_APP)
            }

            override fun onSwipeDown() {
                super.onSwipeDown()
                swipeDownAction()
            }

            override fun onLongClick() {
                super.onLongClick()
                try {
                    findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
                    viewModel.firstOpen(false)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onDoubleClick() {
                super.onDoubleClick()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    binding.lock.performClick()
                else if (prefs.lockModeOn)
                    lockPhone()
            }

            override fun onClick() {
                super.onClick()
                viewModel.checkForMessages.call()
            }
        }
    }

    private fun getViewSwipeTouchListener(context: Context, view: View): View.OnTouchListener {
        return object : ViewSwipeTouchListener(context, view) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                swipeLeftAction()
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                openSwipeRightApp()
            }

            override fun onSwipeUp() {
                super.onSwipeUp()
                showAppList(Constants.FLAG_LAUNCH_APP)
            }

            override fun onSwipeDown() {
                super.onSwipeDown()
                swipeDownAction()
            }

            override fun onLongClick(view: View) {
                super.onLongClick(view)
                textOnLongClick(view)
            }

            override fun onClick(view: View) {
                super.onClick(view)
                textOnClick(view)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopCalendarTicker()
        binding.widgetPager.unregisterOnPageChangeCallback(pageChangeCallback)
        binding.widgetPager.adapter = null
        widgetPages = emptyList()
        calendarView = null
        yearView = null
        calendarCard = null
        yearCard = null
        _binding = null
    }
}