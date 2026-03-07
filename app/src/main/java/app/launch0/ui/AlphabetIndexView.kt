package app.launch0.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import app.launch0.R

class AlphabetIndexView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val letters = listOf("#") + ('A'..'Z').map { it.toString() }

    private val bubbleRadius = dp(28f)
    private val bubbleOffsetX = dp(48f)

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        textSize = sp(10f)
    }

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        textSize = sp(10f)
    }

    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val bubbleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        textSize = sp(24f)
    }

    private val connectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var activeLetter: String? = null
    private var activeLetterY = 0f
    private var letterAnimProgress = 0f
    private var bubbleAnimProgress = 0f
    private var letterAnimator: ValueAnimator? = null
    private var bubbleAnimator: ValueAnimator? = null
    private var isTouching = false

    var onLetterSelected: ((String, Float) -> Unit)? = null
    var onLetterDeselected: (() -> Unit)? = null
    var isRightAligned: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    private var normalColor: Int = 0
    private var dimColor: Int = 0
    private var highlightColor: Int = 0
    private var bubbleColor: Int = 0
    private var bubbleTextColor: Int = 0

    init {
        resolveColors()
    }

    private fun resolveColors() {
        val typedValue = TypedValue()

        context.theme.resolveAttribute(R.attr.primaryColor, typedValue, true)
        normalColor = typedValue.data
        highlightColor = normalColor
        bubbleColor = normalColor

        context.theme.resolveAttribute(R.attr.primaryColorTrans50, typedValue, true)
        dimColor = typedValue.data

        context.theme.resolveAttribute(R.attr.primaryInverseColor, typedValue, true)
        bubbleTextColor = typedValue.data

        textPaint.color = dimColor
        highlightPaint.color = highlightColor
        bubblePaint.color = bubbleColor
        bubbleTextPaint.color = bubbleTextColor
        connectorPaint.color = bubbleColor
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (letters.isEmpty()) return

        val itemHeight = height.toFloat() / letters.size
        val stripCenterX = width / 2f

        // Draw letter strip
        for (i in letters.indices) {
            val letter = letters[i]
            val y = itemHeight * i + itemHeight / 2f + textPaint.textSize / 3f

            if (letter == activeLetter && isTouching) {
                val scale = 1f + 0.3f * letterAnimProgress
                canvas.save()
                canvas.scale(scale, scale, stripCenterX, y - textPaint.textSize / 3f)
                highlightPaint.alpha = 255
                canvas.drawText(letter, stripCenterX, y, highlightPaint)
                canvas.restore()
            } else {
                textPaint.color = if (isTouching) dimColor else normalColor
                textPaint.alpha = if (isTouching) 100 else 180
                canvas.drawText(letter, stripCenterX, y, textPaint)
            }
        }

        // Draw bubble outside view bounds when touching
        if (isTouching && activeLetter != null && bubbleAnimProgress > 0f) {
            val bubbleCenterX = if (isRightAligned) {
                width / 2f + bubbleOffsetX
            } else {
                width / 2f - bubbleOffsetX
            }
            val bubbleCenterY = activeLetterY

            val scale = bubbleAnimProgress
            canvas.save()
            canvas.scale(scale, scale, bubbleCenterX, bubbleCenterY)

            // Draw connector from strip to bubble
            val connectorPath = Path()
            val connectorStartX = if (isRightAligned) {
                width / 2f + dp(4f)
            } else {
                width / 2f - dp(4f)
            }
            val connectorEndX = if (isRightAligned) {
                bubbleCenterX - bubbleRadius + dp(4f)
            } else {
                bubbleCenterX + bubbleRadius - dp(4f)
            }
            connectorPaint.alpha = (200 * bubbleAnimProgress).toInt()
            val connectorWidth = dp(5f)
            connectorPath.moveTo(connectorStartX, bubbleCenterY - connectorWidth)
            connectorPath.lineTo(connectorEndX, bubbleCenterY - connectorWidth * 1.5f)
            connectorPath.lineTo(connectorEndX, bubbleCenterY + connectorWidth * 1.5f)
            connectorPath.lineTo(connectorStartX, bubbleCenterY + connectorWidth)
            connectorPath.close()
            canvas.drawPath(connectorPath, connectorPaint)

            // Draw bubble circle
            bubblePaint.alpha = (230 * bubbleAnimProgress).toInt()
            canvas.drawCircle(bubbleCenterX, bubbleCenterY, bubbleRadius, bubblePaint)

            // Draw letter in bubble
            bubbleTextPaint.alpha = (255 * bubbleAnimProgress).toInt()
            val textY = bubbleCenterY + bubbleTextPaint.textSize / 3f
            canvas.drawText(activeLetter!!, bubbleCenterX, textY, bubbleTextPaint)

            canvas.restore()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isTouching = true
                handleLetterTouch(event.y)
                parent?.requestDisallowInterceptTouchEvent(true)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                handleLetterTouch(event.y)
                parent?.requestDisallowInterceptTouchEvent(true)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                animateBubbleOut()
                parent?.requestDisallowInterceptTouchEvent(false)
                onLetterDeselected?.invoke()
                return true
            }
        }
        return false
    }

    private fun handleLetterTouch(y: Float) {
        val index = getLetterIndex(y)
        if (index in letters.indices) {
            val letter = letters[index]
            val itemHeight = height.toFloat() / letters.size
            activeLetterY = (itemHeight * index + itemHeight / 2f)
                .coerceIn(bubbleRadius, height - bubbleRadius)
            if (letter != activeLetter) {
                activeLetter = letter
                animateLetterPop()
                animateBubbleIn()
                onLetterSelected?.invoke(letter, activeLetterY)
            }
        }
    }

    private fun getLetterIndex(y: Float): Int {
        val itemHeight = height.toFloat() / letters.size
        return (y / itemHeight).toInt().coerceIn(0, letters.size - 1)
    }

    private fun animateLetterPop() {
        letterAnimator?.cancel()
        letterAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 150
            interpolator = OvershootInterpolator(2f)
            addUpdateListener {
                letterAnimProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun animateBubbleIn() {
        if (bubbleAnimProgress >= 1f) return
        bubbleAnimator?.cancel()
        bubbleAnimator = ValueAnimator.ofFloat(bubbleAnimProgress, 1f).apply {
            duration = 200
            interpolator = OvershootInterpolator(1.5f)
            addUpdateListener {
                bubbleAnimProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun animateBubbleOut() {
        bubbleAnimator?.cancel()
        bubbleAnimator = ValueAnimator.ofFloat(bubbleAnimProgress, 0f).apply {
            duration = 150
            addUpdateListener {
                bubbleAnimProgress = it.animatedValue as Float
                if (bubbleAnimProgress <= 0f) {
                    isTouching = false
                    letterAnimator?.cancel()
                    letterAnimProgress = 0f
                    activeLetter = null
                }
                invalidate()
            }
            start()
        }
    }

    private fun dp(value: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            resources.displayMetrics
        )
    }

    private fun sp(value: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            value,
            resources.displayMetrics
        )
    }
}
