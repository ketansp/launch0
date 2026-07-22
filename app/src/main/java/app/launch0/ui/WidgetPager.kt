package app.launch0.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.TranslateAnimation
import android.widget.ViewFlipper
import app.launch0.helper.isEinkDisplay
import kotlin.math.abs

/**
 * A minimal swipeable carousel for the home-screen widgets. Shows one widget card at a time and
 * pages left/right on a horizontal swipe, while letting vertical drags and taps fall through to the
 * widget inside — so the calendar can still scroll and its rows stay tappable, and a vertical swipe
 * over the area still reaches the home screen's own gestures.
 *
 * Built on [ViewFlipper] to avoid pulling in a pager dependency; paging is discrete with a slide
 * animation (static on e-ink). Reports the current page so the caller can update the dots.
 */
class WidgetPager @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ViewFlipper(context, attrs) {

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f

    /** Called with (currentIndex, pageCount) whenever the visible page changes. */
    var onPageChanged: ((Int, Int) -> Unit)? = null

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
            }

            MotionEvent.ACTION_MOVE -> if (childCount > 1) {
                val dx = abs(ev.x - downX)
                val dy = abs(ev.y - downY)
                // Steal a clearly-horizontal drag away from a child (e.g. a tappable calendar row).
                if (dx > touchSlop && dx > dy * 1.4f) return true
            }
        }
        return false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        // With one (or no) widget there's nothing to page, so let the gesture bubble up to the home
        // screen's own swipe handler. With more than one we own the gesture here, which is how a
        // horizontal swipe that starts on a non-scrolling widget (the days-left grid) still pages —
        // vertical drags on a scrollable widget are handled by that widget before they reach us.
        if (childCount <= 1) return false
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
            }

            MotionEvent.ACTION_UP -> {
                val dx = ev.x - downX
                val dy = ev.y - downY
                if (abs(dx) > touchSlop * 2 && abs(dx) > abs(dy)) {
                    if (dx < 0) showNextPage() else showPreviousPage()
                }
            }
        }
        return true
    }

    private fun showNextPage() {
        if (displayedChild >= childCount - 1) return
        setSlideAnimations(forward = true)
        showNext()
        onPageChanged?.invoke(displayedChild, childCount)
    }

    private fun showPreviousPage() {
        if (displayedChild <= 0) return
        setSlideAnimations(forward = false)
        showPrevious()
        onPageChanged?.invoke(displayedChild, childCount)
    }

    private fun setSlideAnimations(forward: Boolean) {
        if (context.isEinkDisplay()) {
            inAnimation = null
            outAnimation = null
            return
        }
        inAnimation = slide(if (forward) 1f else -1f, 0f)
        outAnimation = slide(0f, if (forward) -1f else 1f)
    }

    private fun slide(fromX: Float, toX: Float): TranslateAnimation = TranslateAnimation(
        Animation.RELATIVE_TO_SELF, fromX, Animation.RELATIVE_TO_SELF, toX,
        Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f,
    ).apply {
        duration = 220
        interpolator = DecelerateInterpolator()
    }
}
