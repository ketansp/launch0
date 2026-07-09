package app.launch0.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.ceil
import kotlin.math.min

/**
 * A thin vertical alphabet index shown beside the app drawer list for faster scrolling.
 * Tapping or dragging over a letter reports the selected section so the list can jump to the
 * first app under it. Apps whose name starts with a digit or symbol are grouped under "#".
 *
 * The list of sections is supplied by the adapter (already alphabetically ordered) via
 * [setSections]; this view only handles drawing and touch.
 */
class AlphabetIndexView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val baseTextSize = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, 13f, resources.displayMetrics
    )

    private var sections: List<String> = emptyList()
    private var selectedIndex = -1
    private var onSectionSelected: ((String) -> Unit)? = null

    fun setTextColor(color: Int) {
        paint.color = color
        invalidate()
    }

    fun setOnSectionSelectedListener(listener: (String) -> Unit) {
        onSectionSelected = listener
    }

    fun setSections(sections: List<String>) {
        if (this.sections == sections) return
        this.sections = sections
        selectedIndex = -1
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        paint.textSize = baseTextSize
        val contentWidth = ceil(paint.measureText("#")).toInt()
        val desiredWidth = paddingLeft + paddingRight + contentWidth
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(suggestedMinimumHeight, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (sections.isEmpty()) return
        val usableHeight = (height - paddingTop - paddingBottom).toFloat()
        if (usableHeight <= 0f) return
        val cellHeight = usableHeight / sections.size
        paint.textSize = min(baseTextSize, cellHeight * 0.72f)
        val centerX = paddingLeft + (width - paddingLeft - paddingRight) / 2f
        val textVerticalCenter = (paint.descent() + paint.ascent()) / 2f
        sections.forEachIndexed { i, section ->
            val y = paddingTop + cellHeight * i + cellHeight / 2f - textVerticalCenter
            paint.alpha = if (i == selectedIndex) 255 else 140
            canvas.drawText(section, centerX, y, paint)
        }
        paint.alpha = 255
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (sections.isEmpty()) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val usableHeight = (height - paddingTop - paddingBottom).toFloat()
                if (usableHeight <= 0f) return true
                val cellHeight = usableHeight / sections.size
                val index = ((event.y - paddingTop) / cellHeight).toInt()
                    .coerceIn(0, sections.size - 1)
                if (index != selectedIndex) {
                    selectedIndex = index
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onSectionSelected?.invoke(sections[index])
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                selectedIndex = -1
                invalidate()
            }
        }
        return true
    }
}
