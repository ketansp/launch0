package app.launch0.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.launch0.MainActivity
import app.launch0.MainViewModel
import app.launch0.R
import app.launch0.data.NotesEntry
import app.launch0.data.NotesStore
import app.launch0.data.Prefs
import app.launch0.databinding.FragmentNotesSearchBinding
import app.launch0.helper.hideKeyboard
import app.launch0.helper.showKeyboard
import app.launch0.helper.showToast
import app.launch0.listener.OnSwipeTouchListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A dedicated search page for [NotesFragment], reached by swiping left on the notes screen. Its
 * borderless, bottom-aligned search box mirrors the app drawer: typing filters the notes live (after
 * a short debounce), and swiping right returns to the notes screen.
 */
class NotesSearchFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var prefs: Prefs
    private lateinit var notesStore: NotesStore
    private lateinit var adapter: NotesAdapter

    private var query: String = ""
    private var searchJob: Job? = null
    private var previousSoftInputMode: Int? = null

    private var _binding: FragmentNotesSearchBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNotesSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        notesStore = NotesStore(requireContext())

        applyWindowInsets()
        initRecyclerView()
        initBackSwipe()
        initSearch()
        initObservers()

        // Always arrive on a clean slate: empty query, full notes list.
        resetState()
    }

    override fun onStart() {
        super.onStart()
        // Resize the window for the keyboard so the IME inset is dispatched to applyWindowInsets(),
        // which lifts the list and search box above the keyboard. Restored in onStop.
        requireActivity().window.let { window ->
            previousSoftInputMode = window.attributes.softInputMode
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        // Focus the field and open the keyboard as soon as the page appears.
        binding.search.showKeyboard()
    }

    override fun onStop() {
        binding.search.hideKeyboard()
        previousSoftInputMode?.let { requireActivity().window.setSoftInputMode(it) }
        super.onStop()
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
            LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        binding.recyclerView.adapter = adapter
    }

    /** Swiping right returns to the notes screen, the same way swipe-right works there for home. */
    private fun initBackSwipe() {
        val swipeListener = object : OnSwipeTouchListener(requireContext()) {
            override fun onSwipeRight() {
                super.onSwipeRight()
                goBack()
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

    private fun initObservers() {
        viewModel.notesUpdated.observe(viewLifecycleOwner) { runSearch() }
    }

    private fun goBack() {
        if (_binding == null) return
        binding.search.hideKeyboard()
        try {
            findNavController().popBackStack()
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            runSearch()
        }
    }

    private fun runSearch() {
        viewLifecycleOwner.lifecycleScope.launch {
            val q = query
            val matches = withContext(Dispatchers.IO) {
                val all = notesStore.getEntries()
                if (q.isEmpty()) all else all.filter { it.text.contains(q, ignoreCase = true) }
            }
            if (_binding == null) return@launch
            val rows = buildNotesRows(requireContext(), matches)
            adapter.submitList(rows) {
                if (rows.isNotEmpty()) binding.recyclerView.scrollToPosition(rows.size - 1)
            }
        }
    }

    private fun updateEntry(entry: NotesEntry) {
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) { notesStore.update(entry) }
            runSearch()
        }
    }

    private fun onItemLongClick(entry: NotesEntry) {
        if (!entry.isText) return
        val items = arrayOf(getString(R.string.notes_copy), getString(R.string.notes_share))
        AlertDialog.Builder(requireContext())
            .setItems(items) { _, which ->
                when (which) {
                    0 -> copyText(entry.text)
                    1 -> shareText(entry.text)
                }
            }
            .show()
    }

    private fun copyText(text: String) {
        val clipboard =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("note", text))
        requireContext().showToast(getString(R.string.notes_copied))
    }

    private fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        (activity as? MainActivity)?.setAwaitingActivityResult()
        startActivity(Intent.createChooser(intent, getString(R.string.notes_share)))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 250L
    }
}
