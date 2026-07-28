package app.launch0.ui

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import app.launch0.R
import app.launch0.data.Prefs
import app.launch0.databinding.FragmentWelcomeBinding
import app.launch0.helper.dpToPx
import app.launch0.helper.getColorFromAttr

/**
 * The first-run walkthrough. A short, text-only carousel that teaches the core gestures (swipe up
 * for apps, swipe sideways, long-press to customise) and points the user at making Launch0 their
 * default launcher. It marks itself shown as soon as it appears, so it never auto-reopens; the user
 * can replay it any time from Settings. Finishing, skipping, or advancing past the last slide simply
 * returns to the home screen.
 */
class WelcomeFragment : Fragment() {

    private lateinit var prefs: Prefs

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    private val slides = listOf(
        WelcomeSlide(R.string.walkthrough_welcome_title, R.string.walkthrough_welcome_body),
        WelcomeSlide(
            R.string.walkthrough_apps_title,
            R.string.walkthrough_apps_body,
            R.drawable.ic_lucide_chevrons_up,
        ),
        WelcomeSlide(
            R.string.walkthrough_gestures_title,
            R.string.walkthrough_gestures_body,
            R.drawable.ic_lucide_chevrons_left,
        ),
        WelcomeSlide(R.string.walkthrough_customise_title, R.string.walkthrough_customise_body),
        WelcomeSlide(R.string.walkthrough_default_title, R.string.walkthrough_default_body),
    )

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            if (_binding != null) updateChrome(position)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        // Showing counts as done: never auto-reopen after this. Replay lives in Settings.
        prefs.walkthroughShown = true

        binding.welcomePager.adapter = WelcomeAdapter(slides)
        binding.welcomePager.registerOnPageChangeCallback(pageChangeCallback)
        buildDots(slides.size)
        updateChrome(0)

        binding.welcomeSkip.setOnClickListener { finish() }
        binding.welcomeBack.setOnClickListener {
            val current = binding.welcomePager.currentItem
            if (current > 0) binding.welcomePager.currentItem = current - 1
        }
        binding.welcomeNext.setOnClickListener {
            val current = binding.welcomePager.currentItem
            if (current < slides.lastIndex) binding.welcomePager.currentItem = current + 1
            else finish()
        }
    }

    /** Syncs the Skip/Back/Next chrome and dots to the current [position]. */
    private fun updateChrome(position: Int) {
        val isLast = position == slides.lastIndex
        binding.welcomeNext.setText(
            if (isLast) R.string.walkthrough_get_started else R.string.walkthrough_next
        )
        binding.welcomeBack.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
        binding.welcomeSkip.visibility = if (isLast) View.INVISIBLE else View.VISIBLE
        updateDots(position)
    }

    /** Builds the row of page dots, matching the home-screen widget carousel's dots. */
    private fun buildDots(count: Int) {
        val dots = binding.welcomeDots
        dots.removeAllViews()
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
        updateDots(0)
    }

    /** Brightens the dot for the current page and dims the rest. */
    private fun updateDots(index: Int) {
        val dots = binding.welcomeDots
        for (i in 0 until dots.childCount)
            dots.getChildAt(i).alpha = if (i == index) 0.9f else 0.3f
    }

    /** Leaves the walkthrough and returns to the home screen. */
    private fun finish() {
        try {
            findNavController().popBackStack(R.id.mainFragment, false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        binding.welcomePager.unregisterOnPageChangeCallback(pageChangeCallback)
        binding.welcomePager.adapter = null
        super.onDestroyView()
        _binding = null
    }
}
