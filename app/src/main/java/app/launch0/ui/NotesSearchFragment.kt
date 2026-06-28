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
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
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
import app.launch0.databinding.FragmentNotesSearchBinding
import app.launch0.helper.hideKeyboard
import app.launch0.helper.showToast
import app.launch0.helper.showKeyboard
import app.launch0.listener.OnSwipeTouchListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A dedicated search page for [NotesFragment], reached by swiping left on the notes screen. The user
 * types a term, taps the magnifier (or the keyboard's search action), and the matching notes are
 * listed below. Swiping right returns to the notes screen.
 */
class NotesSearchFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var notesStore: NotesStore
    private lateinit var adapter: NotesAdapter

    private var query: String = ""

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
        notesStore = NotesStore(requireContext())

        applyWindowInsets()
        initRecyclerView()
        initBackSwipe()
        initInput()
        initObservers()

        binding.notesSearchInput.showKeyboard()
    }

    override fun onResume() {
        super.onResume()
        // Reflect anything added or changed (e.g. via the share sheet) while we were away.
        if (query.isNotEmpty()) runSearch()
    }

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
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
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

    private fun initInput() {
        binding.notesSearchBack.setOnClickListener { goBack() }
        binding.notesSearchButton.setOnClickListener { onSearchTapped() }
        binding.notesSearchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                onSearchTapped()
                true
            } else {
                false
            }
        }
    }

    private fun initObservers() {
        viewModel.notesUpdated.observe(viewLifecycleOwner) {
            if (query.isNotEmpty()) runSearch()
        }
    }

    private fun goBack() {
        if (_binding == null) return
        binding.notesSearchInput.hideKeyboard()
        try {
            findNavController().popBackStack()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun onSearchTapped() {
        query = binding.notesSearchInput.text?.toString().orEmpty().trim()
        binding.notesSearchInput.hideKeyboard()
        runSearch()
    }

    private fun runSearch() {
        viewLifecycleOwner.lifecycleScope.launch {
            val q = query
            val matches = withContext(Dispatchers.IO) {
                if (q.isEmpty()) emptyList()
                else notesStore.getEntries().filter { it.text.contains(q, ignoreCase = true) }
            }
            if (_binding == null) return@launch
            val rows = buildNotesRows(requireContext(), matches)
            adapter.submitList(rows)
            binding.notesSearchEmpty.isVisible = matches.isEmpty()
            binding.notesSearchEmpty.text = getString(
                if (q.isEmpty()) R.string.notes_search_prompt else R.string.notes_no_results
            )
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
        _binding = null
    }
}
