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
import app.launch0.data.DumpEntry
import app.launch0.data.DumpStore
import app.launch0.databinding.FragmentDumpBinding
import app.launch0.helper.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The personal "dump" page — a chat-with-yourself screen reached by swiping left on the home
 * screen. It holds quick text notes and images (typed here or received via Android's share sheet,
 * see [app.launch0.MainActivity]).
 */
class DumpFragment : androidx.fragment.app.Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var dumpStore: DumpStore
    private lateinit var adapter: DumpAdapter

    private var _binding: FragmentDumpBinding? = null
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
        _binding = FragmentDumpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dumpStore = DumpStore(requireContext())

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
        ViewCompat.setOnApplyWindowInsetsListener(binding.dumpRoot) { v, insets ->
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
        adapter = DumpAdapter(onItemLongClick = { confirmDelete(it) })
        binding.recyclerView.layoutManager =
            LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        binding.recyclerView.adapter = adapter
    }

    private fun initInput() {
        binding.dumpSend.setOnClickListener { sendText() }
        binding.dumpAttach.setOnClickListener { pickImage.launch("image/*") }
    }

    private fun initObservers() {
        viewModel.dumpUpdated.observe(viewLifecycleOwner) {
            loadEntries(scrollToBottom = true)
        }
    }

    private fun sendText() {
        val text = binding.dumpInput.text?.toString().orEmpty()
        if (text.isBlank()) return
        binding.dumpInput.setText("")
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) { dumpStore.addText(text) }
            loadEntries(scrollToBottom = true)
        }
    }

    private fun addImage(uri: android.net.Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val entry = withContext(Dispatchers.IO) { dumpStore.addImageFromUri(uri) }
            if (entry == null) {
                requireContext().showToast(getString(R.string.couldnt_add_image))
            } else {
                loadEntries(scrollToBottom = true)
            }
        }
    }

    private fun loadEntries(scrollToBottom: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            val entries = withContext(Dispatchers.IO) { dumpStore.getEntries() }
            if (_binding == null) return@launch
            binding.dumpEmpty.isVisible = entries.isEmpty()
            adapter.submitList(entries) {
                if (scrollToBottom && entries.isNotEmpty())
                    binding.recyclerView.scrollToPosition(entries.size - 1)
            }
        }
    }

    private fun confirmDelete(entry: DumpEntry) {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.dump_delete_confirm)
            .setPositiveButton(R.string.dump_delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) { dumpStore.delete(entry) }
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
