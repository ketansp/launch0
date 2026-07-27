package app.launch0.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import app.launch0.R
import app.launch0.helper.getColorFromAttr
import app.launch0.helper.getNotificationCountDrawable

/**
 * The "on hold" home-screen widget: while Hold Notifications (DND) is on and at least one
 * notification is being held, this card lists each app that currently has parked notifications —
 * its name alongside the familiar bell+count pill — in the launcher's monochrome, text-only
 * register so it reads as one family with the calendar and days-left cards. Tapping a row releases
 * that app's held notifications so the system re-posts them now.
 *
 * Purely a presenter: it renders whatever [showHeldApps] is handed and reports row taps back through
 * the listener. Reading the held counts and doing the releasing live in the fragment.
 */
class HeldNotificationsWidgetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    /**
     * One app with parked notifications: its package, display label, how many are held, and an
     * optional pre-shaped launcher icon (null when the launcher's "show app icons" setting is off,
     * or the icon can't be resolved).
     */
    data class HeldApp(
        val packageName: String,
        val label: String,
        val count: Int,
        val icon: Drawable? = null,
    )

    private val header = LinearLayout(context)
    private val titleLabel = TextView(context)
    private val countLabel = TextView(context)
    private val scroll = ScrollView(context)
    private val list = LinearLayout(context)

    private var fg = Color.WHITE
    private var shadowColor = Color.BLACK

    private var onReleaseClick: ((String) -> Unit)? = null

    init {
        orientation = VERTICAL
        resolveColors()

        // Header: "ON HOLD" on the start edge, the total held count on the end edge.
        header.orientation = HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        header.setPadding(dp(2), 0, dp(2), dp(9))

        titleLabel.applyLabel(11f, alpha(0.55f))
        titleLabel.isAllCaps = true
        titleLabel.letterSpacing = 0.14f
        titleLabel.text = context.getString(R.string.held_notifications_title)

        countLabel.applyLabel(11f, alpha(0.42f))
        countLabel.gravity = Gravity.END

        header.addView(titleLabel, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        header.addView(countLabel, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))

        // Scrolling list of app rows, with a soft fade at the top/bottom while there's more to see.
        list.orientation = VERTICAL
        scroll.isVerticalFadingEdgeEnabled = true
        scroll.setFadingEdgeLength(dp(18))
        scroll.isVerticalScrollBarEnabled = false
        scroll.overScrollMode = View.OVER_SCROLL_NEVER
        scroll.addView(list, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        addView(header, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        // The list fills whatever height the card gives us and scrolls when there are many apps.
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
    }

    /** A row tap releases the tapped app's held notifications (the fragment does the releasing). */
    fun setOnReleaseClickListener(listener: (String) -> Unit) {
        onReleaseClick = listener
    }

    /** Re-reads theme colours (foreground flips with the light/dark theme). */
    fun refresh() {
        resolveColors()
    }

    /**
     * Renders one row per [apps] entry — the app name with its bell+count pill — in the order the
     * fragment hands them (most-held first). The header's end label sums the held notifications.
     */
    fun showHeldApps(apps: List<HeldApp>) {
        val total = apps.sumOf { it.count }
        countLabel.text = resources.getQuantityString(R.plurals.held_notifications_count, total, total)
        list.removeAllViews()
        apps.forEach { list.addView(buildRow(it)) }
    }

    private fun buildRow(app: HeldApp): View {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), dp(10), dp(2), dp(10))
            isClickable = true
            isFocusable = true
            setOnClickListener { onReleaseClick?.invoke(app.packageName) }
        }

        // The app icon leads the row (when enabled) so one-letter-named apps (e.g. "X") are
        // recognisable at a glance. The drawable is already sized and shaped by the fragment.
        app.icon?.let { icon ->
            val iconView = ImageView(context).apply { setImageDrawable(icon) }
            val size = icon.intrinsicWidth.takeIf { it > 0 } ?: dp(24)
            row.addView(
                iconView,
                LayoutParams(size, size).apply { marginEnd = dp(12) },
            )
        }

        val name = TextView(context).apply {
            applyBody(15.5f, alpha(0.95f), light = true)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            text = app.label
        }

        // The same bell+count pill used on home and drawer rows, so held counts read consistently.
        val pill = ImageView(context).apply {
            setImageDrawable(context.getNotificationCountDrawable(app.count))
        }

        row.addView(name, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        row.addView(
            pill,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { marginStart = dp(10) },
        )
        return row
    }

    private fun resolveColors() {
        fg = context.getColorFromAttr(R.attr.primaryColor)
        shadowColor = context.getColorFromAttr(R.attr.primaryTextShadowColor)
        titleLabel.setLabelColors(alpha(0.55f))
        countLabel.setLabelColors(alpha(0.42f))
    }

    private fun alpha(fraction: Float): Int =
        Color.argb((fraction * 255).toInt(), Color.red(fg), Color.green(fg), Color.blue(fg))

    /** Tiny, letter-spaced labels (header). */
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

    /** Row body text; [light] picks the thin weight used for app names. */
    private fun TextView.applyBody(sizeSp: Float, color: Int, light: Boolean) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(color)
        setShadowLayer(4f, 0f, 2f, shadowColor)
        typeface = if (light) Typeface.create("sans-serif-light", Typeface.NORMAL) else Typeface.SANS_SERIF
        includeFontPadding = false
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
