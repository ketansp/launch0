package app.launch0.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.widget.FrameLayout
import app.launch0.R
import app.launch0.helper.getColorFromAttr
import kotlin.math.max

/**
 * The shared "card" every home-screen widget sits in: a quiet, translucent rounded box in the
 * launcher's monochrome register. Centralises the look (background + padding) so the calendar,
 * days-left, and any future widget read as one family. The foreground tint flips with the theme.
 */
class WidgetCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        setPadding(dp(14), dp(12), dp(14), dp(12))
        applyBox()
    }

    /** Re-applies the box tint after a theme change (foreground flips light/dark). */
    fun refresh() = applyBox()

    private fun applyBox() {
        val fg = context.getColorFromAttr(R.attr.primaryColor)
        fun alpha(fraction: Float) =
            Color.argb((fraction * 255).toInt(), Color.red(fg), Color.green(fg), Color.blue(fg))
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(18).toFloat()
            setColor(alpha(0.10f))
            setStroke(max(1, dp(1) / 2), alpha(0.10f))
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
