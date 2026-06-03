package app.launch0.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import app.launch0.MainViewModel
import app.launch0.R
import app.launch0.data.NotesEntry
import app.launch0.data.NotesStore
import app.launch0.databinding.FragmentNotesBinding
import app.launch0.helper.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The personal "notes" page — a chat-with-yourself screen reached by swiping left on the home
 * screen. It holds quick text notes and images (typed here or received via Android's share sheet,
 * see [app.launch0.MainActivity]).
 */
class NotesFragment : androidx.fragment.app.Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var notesStore: NotesStore
    private lateinit var adapter: NotesAdapter

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { addImage(it) }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        notesStore = NotesStore(requireContext())

        applyWindowInsets()
        initRecyclerView()
        initInput()
        initObservers()
        loadEntries(scrollToBottom = true)
    }

    override fun onResume() {
        super.onResume()
        // Reflect anything added via the share sheet while we were away.
        loadEntries(scrollToBottom = true)
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.notesRoot) { v, insets ->
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
        adapter = NotesAdapter(onItemLongClick = { confirmDelete(it) })
        binding.recyclerView.layoutManager =
            LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        binding.recyclerView.adapter = adapter
    }

    private fun initInput() {
        binding.notesSend.setOnClickListener { sendText() }
        binding.notesAttach.setOnClickListener { pickImage.launch("image/*") }
    }

    private fun initObservers() {
        viewModel.notesUpdated.observe(viewLifecycleOwner) {
            loadEntries(scrollToBottom = true)
        }
    }

    private fun sendText() {
        val text = binding.notesInput.text?.toString().orEmpty()
        if (text.isBlank()) return
        binding.notesInput.setText("")
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) { notesStore.addText(text) }
            loadEntries(scrollToBottom = true)
        }
    }

    private fun addImage(uri: android.net.Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val entry = withContext(Dispatchers.IO) { notesStore.addImageFromUri(uri) }
            if (entry == null) {
                requireContext().showToast(getString(R.string.couldnt_add_image))
            } else {
                loadEntries(scrollToBottom = true)
            }
        }
    }

    private fun loadEntries(scrollToBottom: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            val entries = withContext(Dispatchers.IO) { notesStore.getEntries() }
            if (_binding == null) return@launch
            binding.notesEmpty.isVisible = entries.isEmpty()
            adapter.submitList(entries) {
                if (scrollToBottom && entries.isNotEmpty())
                    binding.recyclerView.scrollToPosition(entries.size - 1)
            }
        }
    }

    private fun confirmDelete(entry: NotesEntry) {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.notes_delete_confirm)
            .setPositiveButton(R.string.notes_delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) { notesStore.delete(entry) }
                    loadEntries(scrollToBottom = false)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
