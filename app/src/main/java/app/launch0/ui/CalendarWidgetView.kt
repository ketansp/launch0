package app.launch0.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.format.DateFormat
import android.text.style.RelativeSizeSpan
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import app.launch0.R
import app.launch0.data.CalendarEvent
import app.launch0.helper.getColorFromAttr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

/**
 * The "boxed agenda" home-screen calendar widget: a quiet, translucent box under the clock that
 * lists today's remaining events. About two rows show at once; the list scrolls (with a soft fade
 * at the edges) for everything else today. Each row keeps a start time with its countdown tucked
 * underneath, and the title with its location — all in the launcher's monochrome, text-only
 * register so the home screen stays calm.
 *
 * Purely a presenter: it renders whatever [showEvents]/[showMessage] is handed and reports taps
 * back through the listeners. Reading the calendar and permission handling live in the fragment.
 */
class CalendarWidgetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val header = LinearLayout(context)
    private val todayLabel = TextView(context)
    private val countLabel = TextView(context)
    private val scroll = ScrollView(context)
    private val list = LinearLayout(context)

    private var fg = Color.WHITE
    private var shadowColor = Color.BLACK

    // The day's events currently rendered; kept so a periodic tick can re-render their countdowns.
    private var events: List<CalendarEvent> = emptyList()

    private var onEventClick: ((CalendarEvent) -> Unit)? = null
    private var onHeaderClick: (() -> Unit)? = null
    private var onMessageClick: (() -> Unit)? = null

    init {
        orientation = VERTICAL
        resolveColors()

        // Header: "TODAY" on the start edge, the event count on the end edge.
        header.orientation = HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        header.setPadding(dp(2), 0, dp(2), dp(9))
        header.setOnClickListener { onHeaderClick?.invoke() }

        todayLabel.applyLabel(11f, alpha(0.55f))
        todayLabel.isAllCaps = true
        todayLabel.letterSpacing = 0.14f
        todayLabel.text = context.getString(R.string.calendar_today)

        countLabel.applyLabel(11f, alpha(0.42f))
        countLabel.gravity = Gravity.END

        header.addView(todayLabel, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        header.addView(countLabel, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))

        // Scrolling list of rows, with a soft fade at the top/bottom while there's more to see.
        list.orientation = VERTICAL
        scroll.isVerticalFadingEdgeEnabled = true
        scroll.setFadingEdgeLength(dp(18))
        scroll.isVerticalScrollBarEnabled = false
        scroll.overScrollMode = View.OVER_SCROLL_NEVER
        scroll.addView(list, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        addView(header, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        // The list fills whatever height the card gives us and scrolls when the day overflows.
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
    }

    fun setOnEventClickListener(listener: (CalendarEvent) -> Unit) {
        onEventClick = listener
    }

    fun setOnHeaderClickListener(listener: () -> Unit) {
        onHeaderClick = listener
    }

    fun setOnMessageClickListener(listener: () -> Unit) {
        onMessageClick = listener
    }

    /** Re-reads theme colours (foreground flips with the light/dark theme). */
    fun refresh() {
        resolveColors()
    }

    /**
     * Renders [newEvents] as agenda rows. The list is already trimmed to ongoing and upcoming events
     * in start order; it fills the card and scrolls once the day overflows. [autoScrollToNow] starts
     * at the top — the most imminent event — while false (a periodic refresh) keeps the reader's
     * current scroll position.
     */
    fun showEvents(newEvents: List<CalendarEvent>, autoScrollToNow: Boolean = true) {
        events = newEvents
        val now = System.currentTimeMillis()
        countLabel.visibility = View.VISIBLE
        countLabel.text = resources.getQuantityString(R.plurals.calendar_events, events.size, events.size)
        val previousScrollY = scroll.scrollY
        list.removeAllViews()
        events.forEach { list.addView(buildRow(it, now)) }
        scroll.post { scroll.scrollTo(0, if (autoScrollToNow) 0 else previousScrollY) }
    }

    /** Shows a single centred line instead of the list — e.g. "grant access" or "nothing left". */
    fun showMessage(message: CharSequence) {
        events = emptyList()
        countLabel.visibility = View.GONE
        list.removeAllViews()
        val text = TextView(context).apply {
            applyLabel(13.5f, alpha(0.6f))
            isAllCaps = false
            letterSpacing = 0f
            setLineSpacing(dp(2).toFloat(), 1f)
            setPadding(dp(2), dp(6), dp(2), dp(4))
            this.text = message
            setOnClickListener { onMessageClick?.invoke() }
        }
        list.addView(text, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    private fun buildRow(event: CalendarEvent, now: Long): View {
        val whenNow = event.isNow(now)
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.TOP
            setPadding(dp(2), dp(8), dp(2), dp(8))
            isClickable = true
            isFocusable = true
            setOnClickListener { onEventClick?.invoke(event) }
        }

        val timeCol = LinearLayout(context).apply {
            orientation = VERTICAL
            minimumWidth = dp(52)
        }
        val time = TextView(context).apply {
            applyBody(15f, alpha(0.92f), light = false)
            maxLines = 1
            text = if (event.allDay) context.getString(R.string.calendar_all_day) else formatTime(event.begin)
        }
        timeCol.addView(time, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        // "now" while the event is live, otherwise a countdown until it starts.
        if (!event.allDay) {
            val whenLabel = TextView(context).apply {
                applyBody(11.5f, alpha(if (whenNow) 0.85f else 0.5f), light = false)
                maxLines = 1
                text = if (whenNow) context.getString(R.string.calendar_now) else formatCountdown(event.begin - now)
            }
            timeCol.addView(
                whenLabel,
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { topMargin = dp(2) }
            )
        }

        val detailCol = LinearLayout(context).apply { orientation = VERTICAL }
        val title = TextView(context).apply {
            applyBody(15.5f, alpha(0.95f), light = true)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            text = event.title.ifBlank { context.getString(R.string.calendar_untitled_event) }
        }
        detailCol.addView(title, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        if (!event.location.isNullOrBlank()) {
            val location = TextView(context).apply {
                applyBody(11.5f, alpha(0.5f), light = true)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                text = event.location
            }
            detailCol.addView(
                location,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = dp(2) }
            )
        }

        row.addView(timeCol, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        row.addView(detailCol, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(10) })
        return row
    }

    /** "10:00 am" (with a smaller am/pm) or "10:00" following the system's 12/24-hour setting. */
    private fun formatTime(millis: Long): CharSequence {
        val date = Date(millis)
        if (DateFormat.is24HourFormat(context)) {
            return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        }
        val time = SimpleDateFormat("h:mm", Locale.getDefault()).format(date)
        val amPm = SimpleDateFormat("a", Locale.getDefault()).format(date).lowercase(Locale.getDefault())
        val builder = SpannableStringBuilder(time).append(' ')
        val start = builder.length
        builder.append(amPm)
        builder.setSpan(RelativeSizeSpan(0.72f), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return builder
    }

    /** "in 45m", "in 2h", or "in 1h 15m" until an event starts. */
    private fun formatCountdown(untilMillis: Long): String {
        val totalMinutes = ceil(untilMillis / 60000.0).toInt().coerceAtLeast(0)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours <= 0 -> context.getString(R.string.calendar_in_minutes, minutes)
            minutes == 0 -> context.getString(R.string.calendar_in_hours, hours)
            else -> context.getString(R.string.calendar_in_hours_minutes, hours, minutes)
        }
    }

    private fun resolveColors() {
        fg = context.getColorFromAttr(R.attr.primaryColor)
        shadowColor = context.getColorFromAttr(R.attr.primaryTextShadowColor)
        todayLabel.setLabelColors(alpha(0.55f))
        countLabel.setLabelColors(alpha(0.42f))
    }

    private fun alpha(fraction: Float): Int =
        Color.argb((fraction * 255).toInt(), Color.red(fg), Color.green(fg), Color.blue(fg))

    /** Tiny, letter-spaced labels (header, messages). */
    private fun TextView.applyLabel(sizeSp: Float, color: Int) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setLabelColors(color)
        typeface = Typeface.SANS_SERIF
        includeFontPadding = false
    }

    private fun TextView.setLabelColors(color: Int) {
        setTextColor(color)
        setShadowLayer(4f, 0f, 2f, shadowColor)
    }

    /** Agenda body text; [light] picks the thin weight used for titles/times. */
    private fun TextView.applyBody(sizeSp: Float, color: Int, light: Boolean) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(color)
        setShadowLayer(4f, 0f, 2f, shadowColor)
        typeface = if (light) Typeface.create("sans-serif-light", Typeface.NORMAL) else Typeface.SANS_SERIF
        includeFontPadding = false
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
