package app.launch0

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.launch0.data.NotesEntry
import app.launch0.data.NotesStore
import app.launch0.data.Prefs
import app.launch0.databinding.ActivityNotesSearchBinding
import app.launch0.helper.hideKeyboard
import app.launch0.helper.isTablet
import app.launch0.helper.showKeyboard
import app.launch0.helper.showToast
import app.launch0.listener.OnSwipeTouchListener
import app.launch0.ui.NotesAdapter
import app.launch0.ui.buildNotesRows
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A standalone page for searching personal notes, reached by swiping left on the notes screen. It's
 * its own activity (rather than a fragment) so more search-specific features can grow here later.
 *
 * The borderless, bottom-aligned search box mirrors the app drawer: typing filters notes live after
 * a short debounce, and the screen starts empty until a term is entered. It slides in from the right
 * and back out to the right, matching the home → notes transition.
 */
class NotesSearchActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var notesStore: NotesStore
    private lateinit var adapter: NotesAdapter
    private lateinit var binding: ActivityNotesSearchBinding

    private var query: String = ""
    private var searchJob: Job? = null

    override fun attachBaseContext(context: Context) {
        val newConfig = Configuration(context.resources.configuration)
        newConfig.fontScale = Prefs(context).textSizeScale
        applyOverrideConfiguration(newConfig)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = Prefs(this)
        AppCompatDelegate.setDefaultNightMode(prefs.appTheme)
        super.onCreate(savedInstanceState)
        binding = ActivityNotesSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(FLAG_LAYOUT_NO_LIMITS)
        notesStore = NotesStore(this)

        setupOrientation()
        applyWindowInsets()
        initRecyclerView()
        initBackSwipe()
        initSearch()

        // Start blank: no results are shown until the user enters a term.
        resetState()
    }

    override fun onStart() {
        super.onStart()
        // Focus the field and open the keyboard as soon as the page appears.
        binding.search.showKeyboard()
    }

    override fun onStop() {
        binding.search.hideKeyboard()
        super.onStop()
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun setupOrientation() {
        if (isTablet(this) || Build.VERSION.SDK_INT == Build.VERSION_CODES.O) return
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    /**
     * Pushes the bottom-aligned search box (and the list above it) clear of the keyboard by padding
     * with the IME inset, falling back to the system bar inset when the keyboard is hidden.
     */
    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.notesSearchRoot) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.updatePadding(
                top = bars.top,
                bottom = maxOf(bars.bottom, ime.bottom),
            )
            insets
        }
    }

    private fun initRecyclerView() {
        adapter = NotesAdapter(
            onItemLongClick = { onItemLongClick(it) },
            onImageClick = {},
            onAudioClick = {},
            onToggleDone = { entry -> updateEntry(entry.copy(done = !entry.done)) },
            onToggleUrgent = { entry -> updateEntry(entry.copy(urgent = !entry.urgent)) },
        )
        // Anchor results to the bottom so they sit right above the search box and keyboard.
        binding.recyclerView.layoutManager =
            LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.recyclerView.adapter = adapter
    }

    /** Swiping right returns to the notes screen, the same way swipe-right works there for home. */
    private fun initBackSwipe() {
        val swipeListener = object : OnSwipeTouchListener(this) {
            override fun onSwipeRight() {
                super.onSwipeRight()
                closeScreen()
            }
        }
        binding.notesSearchRoot.setOnTouchListener(swipeListener)
        binding.recyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                swipeListener.onTouch(rv, e)
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
    }

    private fun initSearch() {
        // Align the typed text with the user's home-screen alignment, like the app drawer does.
        try {
            binding.search.findViewById<TextView>(R.id.search_src_text)?.gravity = prefs.homeAlignment
        } catch (e: Exception) {
            e.printStackTrace()
        }
        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                binding.search.hideKeyboard()
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                scheduleSearch(newText)
                return true
            }
        })
    }

    private fun closeScreen() {
        binding.search.hideKeyboard()
        finish()
    }

    private fun resetState() {
        searchJob?.cancel()
        query = ""
        binding.search.setQuery("", false)
        runSearch()
    }

    /** Debounces keystrokes so the notes are only filtered once typing pauses. */
    private fun scheduleSearch(newText: String) {
        query = newText.trim()
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            runSearch()
        }
    }

    private fun runSearch() {
        lifecycleScope.launch {
            val q = query
            // Nothing is shown until the user types something to match against.
            val matches = if (q.isEmpty()) emptyList() else withContext(Dispatchers.IO) {
                notesStore.getEntries().filter { it.text.contains(q, ignoreCase = true) }
            }
            val rows = buildNotesRows(this@NotesSearchActivity, matches)
            adapter.submitList(rows) {
                if (rows.isNotEmpty()) binding.recyclerView.scrollToPosition(rows.size - 1)
            }
        }
    }

    private fun updateEntry(entry: NotesEntry) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { notesStore.update(entry) }
            runSearch()
        }
    }

    private fun onItemLongClick(entry: NotesEntry) {
        if (!entry.isText) return
        val items = arrayOf(getString(R.string.notes_copy), getString(R.string.notes_share))
        AlertDialog.Builder(this)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> copyText(entry.text)
                    1 -> shareText(entry.text)
                }
            }
            .show()
    }

    private fun copyText(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("note", text))
        showToast(getString(R.string.notes_copied))
    }

    private fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.notes_share)))
    }

    @Suppress("DEPRECATION")
    override fun finish() {
        super.finish()
        // Slide back out to the right, mirroring the notes → home pop transition.
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun onDestroy() {
        super.onDestroy()
        searchJob?.cancel()
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 250L
    }
}
