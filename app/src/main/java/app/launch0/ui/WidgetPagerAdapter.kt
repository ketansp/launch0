package app.launch0.ui

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView

/**
 * Hosts the home-screen widget cards as pages inside a [androidx.viewpager2.widget.ViewPager2], so
 * the swipe follows the finger and flings smoothly. The cards are created and owned by the fragment
 * (they're stateful — the calendar keeps its events and listeners); this adapter just parents each
 * one in a full-bleed page.
 *
 * Each position gets its own view type so RecyclerView never reuses one card's holder for another
 * position, and each card is placed into its holder once at creation. Rebuild the pager by setting a
 * fresh adapter whenever the set of enabled widgets changes.
 */
class WidgetPagerAdapter(private val pages: List<View>) :
    RecyclerView.Adapter<WidgetPagerAdapter.PageHolder>() {

    class PageHolder(val container: FrameLayout) : RecyclerView.ViewHolder(container)

    // Distinct view type per position → each page is created once and never recycled onto another.
    override fun getItemViewType(position: Int): Int = position

    override fun getItemCount(): Int = pages.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
        val container = FrameLayout(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
        // viewType == position here, so this is the card for this page.
        val page = pages[viewType]
        (page.parent as? ViewGroup)?.removeView(page)
        container.addView(
            page,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        return PageHolder(container)
    }

    override fun onBindViewHolder(holder: PageHolder, position: Int) {
        // Nothing to bind — the card was placed into its holder at creation and stays there.
    }
}
