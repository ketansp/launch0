package app.launch0.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import app.launch0.R
import app.launch0.helper.getColorFromAttr
import java.util.Calendar
import kotlin.math.ceil

/**
 * "Days left in the year" widget — a GitHub-contribution-style grid rendered in the
 * launcher's monochrome register. One box per day of the current year, filled
 * sequentially from the first box (Jan 1), top-to-bottom down each column:
 *
 *  - elapsed days  → bright foreground (alpha 0.80)
 *  - today         → solid foreground with an inset ring (a donut)
 *  - remaining     → faint foreground (alpha 0.10)
 *
 * The column count (and therefore row count) is derived from the available width, so
 * the grid stays dense and fills the screen on any device. There is no weekday/month
 * alignment — the fill is purely sequential.
 */
class YearProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    // A box is roughly this wide; the actual size is recomputed to fill the width exactly.
    private val targetBoxPx = dp(8.5f)
    private val gapPx = dp(3f)
    private val cornerPx = dp(2f)

    private var emptyColor = 0
    private var elapsedColor = 0
    private var todayColor = 0

    // Grid geometry, recomputed on measure.
    private var cols = 0
    private var rows = 0
    private var boxPx = 0f
    private val drawRect = RectF()
    private val ringRect = RectF()

    private var totalDays = 0
    private var todayIndex = 0 // 0-based day-of-year for today

    init {
        computeDates()
        resolveColors()
        hairPaint.strokeWidth = dp(0.6f)
        ringPaint.strokeWidth = dp(1.4f)
    }

    private fun computeDates() {
        val cal = Calendar.getInstance()
        todayIndex = cal.get(Calendar.DAY_OF_YEAR) - 1
        totalDays = cal.getActualMaximum(Calendar.DAY_OF_YEAR) // 365 or 366
    }

    /** Recompute date and colors — call when the screen is (re)shown. */
    fun refresh() {
        computeDates()
        resolveColors()
        requestLayout()
        invalidate()
    }

    private fun resolveColors() {
        val fg = context.getColorFromAttr(R.attr.primaryColor)
        val r = Color.red(fg)
        val g = Color.green(fg)
        val b = Color.blue(fg)
        emptyColor = Color.argb(26, r, g, b)    // 0.10 — remaining day
        elapsedColor = Color.argb(204, r, g, b) // 0.80 — completed day
        todayColor = Color.argb(255, r, g, b)   // 1.00 — today
        hairPaint.color = Color.argb(36, r, g, b) // 0.14 — hairline edge

        // The ring on "today" uses the inverse (background-ish) color to read as a donut.
        val luminance = 0.299 * r + 0.587 * g + 0.114 * b
        ringPaint.color = if (luminance > 127)
            Color.argb(140, 0, 0, 0)        // light foreground → dark ring (~0.55)
        else
            Color.argb(178, 255, 255, 255)  // dark foreground → light ring (~0.70)
    }

    private fun computeGrid(width: Int) {
        val avail = width - paddingLeft - paddingRight
        if (avail <= 0) {
            cols = 0; rows = 0; boxPx = 0f
            return
        }
        cols = ((avail + gapPx) / (targetBoxPx + gapPx)).toInt().coerceAtLeast(7)
        boxPx = (avail - (cols - 1) * gapPx) / cols
        rows = ceil(totalDays / cols.toDouble()).toInt()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        computeGrid(width)
        val height = if (rows > 0)
            ceil(rows * boxPx + (rows - 1) * gapPx).toInt() + paddingTop + paddingBottom
        else
            paddingTop + paddingBottom
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        if (cols <= 0 || boxPx <= 0f) return
        val ringInset = dp(0.7f)
        val step = boxPx + gapPx
        for (i in 0 until totalDays) {
            val left = paddingLeft + (i / rows) * step
            val top = paddingTop + (i % rows) * step
            drawRect.set(left, top, left + boxPx, top + boxPx)
            when {
                i < todayIndex -> {
                    boxPaint.color = elapsedColor
                    canvas.drawRoundRect(drawRect, cornerPx, cornerPx, boxPaint)
                    canvas.drawRoundRect(drawRect, cornerPx, cornerPx, hairPaint)
                }
                i == todayIndex -> {
                    boxPaint.color = todayColor
                    canvas.drawRoundRect(drawRect, cornerPx, cornerPx, boxPaint)
                    ringRect.set(left + ringInset, top + ringInset, left + boxPx - ringInset, top + boxPx - ringInset)
                    canvas.drawRoundRect(ringRect, cornerPx, cornerPx, ringPaint)
                }
                else -> {
                    boxPaint.color = emptyColor
                    canvas.drawRoundRect(drawRect, cornerPx, cornerPx, boxPaint)
                    canvas.drawRoundRect(drawRect, cornerPx, cornerPx, hairPaint)
                }
            }
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
