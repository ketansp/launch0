package app.launch0.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import app.launch0.databinding.ItemWelcomeSlideBinding

/** One walkthrough slide: a title, a body, and an optional gesture glyph (0 = no glyph). */
data class WelcomeSlide(
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
    @DrawableRes val glyphRes: Int = 0,
)

/**
 * Backs the walkthrough [androidx.viewpager2.widget.ViewPager2] with a fixed list of text-only
 * slides. Each page fills the pager (the item root is match_parent) and simply binds its strings.
 */
class WelcomeAdapter(private val slides: List<WelcomeSlide>) :
    RecyclerView.Adapter<WelcomeAdapter.SlideHolder>() {

    class SlideHolder(val binding: ItemWelcomeSlideBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlideHolder {
        val binding = ItemWelcomeSlideBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SlideHolder(binding)
    }

    override fun getItemCount(): Int = slides.size

    override fun onBindViewHolder(holder: SlideHolder, position: Int) {
        val slide = slides[position]
        holder.binding.slideTitle.setText(slide.titleRes)
        holder.binding.slideBody.setText(slide.bodyRes)
        if (slide.glyphRes != 0) {
            holder.binding.slideGlyph.setImageResource(slide.glyphRes)
            holder.binding.slideGlyph.isVisible = true
        } else {
            holder.binding.slideGlyph.isVisible = false
        }
    }
}
