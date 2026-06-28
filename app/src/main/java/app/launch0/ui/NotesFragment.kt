package app.launch0.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
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
import app.launch0.databinding.FragmentNotesBinding
import app.launch0.helper.getColorFromAttr
import app.launch0.helper.showToast
import app.launch0.listener.OnSwipeTouchListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * The personal "notes" page — a chat-with-yourself screen reached by swiping left on the home
 * screen. It holds quick text notes (which double as to-dos), images and voice memos, either typed
 * here or received via Android's share sheet (see [app.launch0.MainActivity]).
 */
class NotesFragment : androidx.fragment.app.Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var notesStore: NotesStore
    private lateinit var adapter: NotesAdapter
    private var imageViewerBackCallback: OnBackPressedCallback? = null

    private var editingId: Long? = null

    // Recording state
    private var recorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var recordStartTime = 0L
    private var recordingTimerJob: Job? = null
    private val isRecording get() = recorder != null

    // Playback state
    private var player: MediaPlayer? = null

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { addImage(it) }
        }

    private val requestMicPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startRecording()
            else requireContext().showToast(getString(R.string.notes_mic_permission))
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
        initBackSwipe()
        initInput()
        initImageViewer()
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
        adapter = NotesAdapter(
            onItemLongClick = { onItemLongClick(it) },
            onImageClick = { showFullImage(it) },
            onAudioClick = { togglePlayback(it) },
            onToggleDone = { entry -> updateEntry(entry.copy(done = !entry.done)) },
            onToggleUrgent = { entry -> updateEntry(entry.copy(urgent = !entry.urgent)) },
        )
        binding.recyclerView.layoutManager =
            LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        binding.recyclerView.adapter = adapter
    }

    /**
     * Notes slides in from the right (swipe-left on home), so swiping back to the right returns
     * home. Swiping further left opens the dedicated notes search page. The same gesture listener is
     * fed the list's touches via an item-touch listener so the swipes also work while the finger is
     * over the (otherwise touch-consuming) RecyclerView.
     */
    private fun initBackSwipe() {
        val swipeListener = object : OnSwipeTouchListener(requireContext()) {
            override fun onSwipeRight() {
                super.onSwipeRight()
                goHome()
            }

            override fun onSwipeLeft() {
                super.onSwipeLeft()
                openSearch()
            }
        }
        binding.notesRoot.setOnTouchListener(swipeListener)
        binding.recyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                swipeListener.onTouch(rv, e)
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
    }

    private fun goHome() {
        if (_binding == null) return
        try {
            findNavController().popBackStack()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openSearch() {
        if (_binding == null) return
        try {
            findNavController().navigate(R.id.action_notesFragment_to_notesSearchFragment)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initImageViewer() {
        binding.notesImageViewer.setOnClickListener { hideFullImage() }
        // Let the back gesture/button close the viewer before leaving the screen.
        imageViewerBackCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() = hideFullImage()
        }.also {
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, it)
        }
    }

    private fun showFullImage(entry: NotesEntry) {
        val path = entry.mediaPath ?: return
        binding.notesImageViewer.visibility = View.VISIBLE
        imageViewerBackCallback?.isEnabled = true
        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                decodeSampledBitmap(path, FULL_IMAGE_DIMEN, FULL_IMAGE_DIMEN)
            }
            if (_binding == null) return@launch
            if (bitmap != null) {
                binding.notesFullImage.setImageBitmap(bitmap)
            } else {
                hideFullImage()
                requireContext().showToast(getString(R.string.couldnt_add_image))
            }
        }
    }

    private fun hideFullImage() {
        if (_binding == null) return
        binding.notesImageViewer.visibility = View.GONE
        binding.notesFullImage.setImageDrawable(null)
        imageViewerBackCallback?.isEnabled = false
    }

    private fun initInput() {
        binding.notesBack.setOnClickListener { goHome() }
        binding.notesSearch.setOnClickListener { openSearch() }
        binding.notesSend.setOnClickListener { onSend() }
        binding.notesRecord.setOnClickListener { onRecordTapped() }
        binding.notesBannerClear.setOnClickListener { clearBanner() }
        binding.notesAttach.setOnClickListener {
            // Tell the launcher we're leaving for a result, so it doesn't snap back to the home
            // screen (which would destroy this fragment and drop the picked image).
            (activity as? MainActivity)?.setAwaitingActivityResult()
            pickImage.launch("image/*")
        }
    }

    private fun initObservers() {
        viewModel.notesUpdated.observe(viewLifecycleOwner) {
            loadEntries(scrollToBottom = true)
        }
    }

    // region Send / edit

    private fun onSend() {
        val text = binding.notesInput.text?.toString().orEmpty().trim()
        if (text.isEmpty()) return
        val editId = editingId
        binding.notesInput.setText("")
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (editId != null) {
                    notesStore.getEntries().firstOrNull { it.id == editId }
                        ?.let { notesStore.update(it.copy(text = text)) }
                } else {
                    notesStore.addText(text)
                }
            }
            editingId = null
            updateBanner()
            loadEntries(scrollToBottom = true)
        }
    }

    private fun startEditing(entry: NotesEntry) {
        editingId = entry.id
        binding.notesInput.setText(entry.text)
        binding.notesInput.setSelection(entry.text.length)
        binding.notesInput.requestFocus()
        updateBanner()
    }

    private fun updateEntry(entry: NotesEntry) {
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) { notesStore.update(entry) }
            loadEntries(scrollToBottom = false)
        }
    }

    // endregion

    // region Editing banner

    private fun clearBanner() {
        editingId = null
        binding.notesInput.setText("")
        updateBanner()
    }

    private fun updateBanner() {
        if (_binding == null) return
        val editing = editingId != null
        binding.notesBanner.isVisible = editing
        if (editing) binding.notesBannerText.text = getString(R.string.notes_editing)
        binding.notesSend.setImageResource(
            if (editing) R.drawable.ic_lucide_check else R.drawable.ic_lucide_send
        )
        binding.notesSend.contentDescription =
            getString(if (editing) R.string.notes_save else R.string.notes_send)
    }

    // endregion

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
            val all = withContext(Dispatchers.IO) { notesStore.getEntries() }
            if (_binding == null) return@launch
            binding.notesCount.text =
                resources.getQuantityString(R.plurals.notes_entry_count, all.size, all.size)
            val rows = buildNotesRows(requireContext(), all)
            binding.notesEmpty.isVisible = all.isEmpty()
            binding.notesEmpty.text = getString(R.string.notes_empty_hint)
            adapter.submitList(rows) {
                if (scrollToBottom && rows.isNotEmpty())
                    binding.recyclerView.scrollToPosition(rows.size - 1)
            }
        }
    }

    // region Long-press actions

    private fun onItemLongClick(entry: NotesEntry) {
        if (entry.isText) showTextActions(entry) else confirmDelete(entry)
    }

    private fun showTextActions(entry: NotesEntry) {
        val items = arrayOf(
            getString(R.string.notes_copy),
            getString(R.string.notes_share),
            getString(R.string.notes_edit),
            getString(R.string.notes_delete),
        )
        AlertDialog.Builder(requireContext())
            .setItems(items) { _, which ->
                when (which) {
                    0 -> copyText(entry.text)
                    1 -> shareText(entry.text)
                    2 -> startEditing(entry)
                    3 -> confirmDelete(entry)
                }
            }
            .show()
    }

    private fun copyText(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
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

    // endregion

    // region Voice recording

    private fun onRecordTapped() {
        if (isRecording) {
            stopRecording(save = true)
        } else if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startRecording()
        } else {
            requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startRecording() {
        if (isRecording) return
        val file = notesStore.newAudioFile()
        val newRecorder = createRecorder()
        try {
            newRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            newRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            newRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            newRecorder.setAudioEncodingBitRate(128000)
            newRecorder.setAudioSamplingRate(44100)
            newRecorder.setOutputFile(file.absolutePath)
            newRecorder.prepare()
            newRecorder.start()
        } catch (e: Exception) {
            e.printStackTrace()
            runCatching { newRecorder.release() }
            if (file.exists()) file.delete()
            requireContext().showToast(getString(R.string.notes_recording_failed))
            return
        }
        recorder = newRecorder
        recordingFile = file
        recordStartTime = SystemClock.elapsedRealtime()
        updateRecordingUi(recording = true)
        startRecordingTimer()
    }

    private fun stopRecording(save: Boolean) {
        val activeRecorder = recorder ?: return
        val file = recordingFile
        val duration = SystemClock.elapsedRealtime() - recordStartTime
        recorder = null
        recordingFile = null
        recordingTimerJob?.cancel()
        recordingTimerJob = null

        var stopped = false
        try {
            activeRecorder.stop()
            stopped = true
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            runCatching { activeRecorder.release() }
        }
        if (_binding != null) updateRecordingUi(recording = false)

        // A too-short or failed capture isn't worth keeping.
        val keep = save && stopped && file != null && duration >= MIN_RECORDING_MS && file.exists()
        if (keep) {
            viewLifecycleOwner.lifecycleScope.launch {
                val entry = withContext(Dispatchers.IO) { notesStore.addAudio(file!!, duration) }
                if (entry == null) {
                    requireContext().showToast(getString(R.string.notes_recording_failed))
                } else {
                    loadEntries(scrollToBottom = true)
                }
            }
        } else if (file?.exists() == true) {
            file.delete()
            if (save && _binding != null) requireContext().showToast(getString(R.string.notes_recording_failed))
        }
    }

    @Suppress("DEPRECATION")
    private fun createRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(requireContext())
        else MediaRecorder()

    private fun startRecordingTimer() {
        recordingTimerJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isRecording) {
                val elapsed = SystemClock.elapsedRealtime() - recordStartTime
                if (_binding != null) {
                    val seconds = (elapsed / 1000)
                    binding.notesRecording.text = getString(
                        R.string.notes_recording
                    ) + "  " + String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60)
                }
                delay(250)
            }
        }
    }

    private fun updateRecordingUi(recording: Boolean) {
        binding.notesRecording.isVisible = recording
        binding.notesInput.isVisible = !recording
        binding.notesAttach.isVisible = !recording
        binding.notesSearch.isVisible = !recording
        binding.notesSend.isVisible = !recording
        binding.notesRecord.setImageResource(
            if (recording) R.drawable.ic_lucide_square else R.drawable.ic_lucide_mic
        )
        // Red while recording; otherwise the theme's primary colour.
        binding.notesRecord.setColorFilter(
            if (recording) ContextCompat.getColor(requireContext(), R.color.notesUrgent)
            else requireContext().getColorFromAttr(R.attr.primaryColor)
        )
    }

    // endregion

    // region Voice playback

    private fun togglePlayback(entry: NotesEntry) {
        val path = entry.mediaPath ?: return
        val activePlayer = player
        // Tapping the note that's already loaded pauses or resumes it rather than restarting.
        if (activePlayer != null && currentPlayingId == entry.id) {
            try {
                if (activePlayer.isPlaying) {
                    activePlayer.pause()
                    adapter.setPlaybackState(entry.id, paused = true)
                } else {
                    activePlayer.start()
                    adapter.setPlaybackState(entry.id, paused = false)
                }
            } catch (e: IllegalStateException) {
                e.printStackTrace()
                stopPlayback()
            }
            return
        }
        stopPlayback()
        val newPlayer = MediaPlayer()
        try {
            newPlayer.setDataSource(path)
            newPlayer.setOnCompletionListener { stopPlayback() }
            newPlayer.prepare()
            newPlayer.start()
        } catch (e: Exception) {
            e.printStackTrace()
            runCatching { newPlayer.release() }
            requireContext().showToast(getString(R.string.notes_recording_failed))
            return
        }
        player = newPlayer
        currentPlayingId = entry.id
        adapter.setPlaybackState(entry.id, paused = false)
    }

    private fun stopPlayback() {
        player?.let { runCatching { it.release() } }
        player = null
        currentPlayingId = null
        if (_binding != null) adapter.setPlaybackState(null, paused = false)
    }

    private var currentPlayingId: Long? = null

    // endregion

    private fun confirmDelete(entry: NotesEntry) {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.notes_delete_confirm)
            .setPositiveButton(R.string.notes_delete) { _, _ ->
                if (currentPlayingId == entry.id) stopPlayback()
                if (editingId == entry.id) {
                    editingId = null
                    binding.notesInput.setText("")
                    updateBanner()
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) { notesStore.delete(entry) }
                    loadEntries(scrollToBottom = false)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onPause() {
        super.onPause()
        if (isRecording) stopRecording(save = true)
        stopPlayback()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (isRecording) stopRecording(save = false)
        stopPlayback()
        imageViewerBackCallback = null
        _binding = null
    }

    companion object {
        private const val FULL_IMAGE_DIMEN = 2048
        private const val MIN_RECORDING_MS = 500L
    }
}
