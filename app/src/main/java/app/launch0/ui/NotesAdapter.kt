package app.launch0.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.Spannable
import android.text.method.ArrowKeyMovementMethod
import android.text.style.ClickableSpan
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.util.LinkifyCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.launch0.R
import app.launch0.data.NotesEntry
import app.launch0.helper.getColorFromAttr
import app.launch0.databinding.AdapterNotesDateBinding
import app.launch0.databinding.AdapterNotesItemBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** One row in the notes list: either a [DateHeader] divider or a note [Item]. */
sealed class NotesRow {
    abstract val rowId: Long

    data class DateHeader(override val rowId: Long, val label: String) : NotesRow()
    data class Item(val entry: NotesEntry) : NotesRow() {
        override val rowId: Long get() = entry.id
    }
}

class NotesAdapter(
    private val onItemLongClick: (NotesEntry) -> Unit,
    private val onImageClick: (NotesEntry) -> Unit,
    private val onAudioClick: (NotesEntry) -> Unit,
    private val onToggleDone: (NotesEntry) -> Unit,
    private val onToggleUrgent: (NotesEntry) -> Unit,
) : ListAdapter<NotesRow, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    /** Id of the voice note currently loaded in the player, or null. Drives the play/pause glyph. */
    private var playingId: Long? = null

    /** Whether the loaded note is paused (vs. actively playing); a paused note shows the play glyph. */
    private var paused: Boolean = false

    fun setPlaybackState(id: Long?, paused: Boolean) {
        if (playingId == id && this.paused == paused) return
        val previous = playingId
        playingId = id
        this.paused = paused
        currentList.forEachIndexed { index, row ->
            if (row is NotesRow.Item && (row.entry.id == previous || row.entry.id == id)) {
                notifyItemChanged(index)
            }
        }
    }

    override fun getItemViewType(position: Int): Int =
        if (getItem(position) is NotesRow.DateHeader) TYPE_DATE else TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_DATE) {
            DateViewHolder(AdapterNotesDateBinding.inflate(inflater, parent, false))
        } else {
            ItemViewHolder(AdapterNotesItemBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is NotesRow.DateHeader -> (holder as DateViewHolder).bind(row)
            is NotesRow.Item -> (holder as ItemViewHolder).bind(
                row.entry, playingId, paused, onItemLongClick, onImageClick, onAudioClick,
                onToggleDone, onToggleUrgent,
            )
        }
    }

    class DateViewHolder(private val binding: AdapterNotesDateBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(header: NotesRow.DateHeader) {
            binding.notesDate.text = header.label
        }
    }

    class ItemViewHolder(private val binding: AdapterNotesItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            entry: NotesEntry,
            playingId: Long?,
            paused: Boolean,
            onItemLongClick: (NotesEntry) -> Unit,
            onImageClick: (NotesEntry) -> Unit,
            onAudioClick: (NotesEntry) -> Unit,
            onToggleDone: (NotesEntry) -> Unit,
            onToggleUrgent: (NotesEntry) -> Unit,
        ) = with(binding) {
            notesTime.text = timeFormat.format(Date(entry.timestamp))

            // The urgent/done toggles now live in the shared footer below every box, so they apply to
            // text, image and voice notes alike.
            bindTodoToggles(entry, onToggleDone, onToggleUrgent)

            // Pick the single visible box for this entry; the others stay gone.
            val activeBox: View = when {
                entry.isImage && !entry.mediaPath.isNullOrEmpty() -> {
                    notesTextBubble.isVisible = false
                    notesAudio.isVisible = false
                    notesImage.isVisible = true
                    loadImage(entry.mediaPath)
                    notesImage.setOnClickListener { onImageClick(entry) }
                    notesImage
                }
                entry.isAudio -> {
                    notesTextBubble.isVisible = false
                    notesImage.isVisible = false
                    notesImage.setImageDrawable(null)
                    notesAudio.isVisible = true
                    notesAudioDuration.text = formatDuration(entry.durationMs)
                    notesAudioIcon.text = root.context.getString(
                        if (entry.id == playingId && !paused) R.string.notes_pause_symbol
                        else R.string.notes_play_symbol
                    )
                    notesAudio.setOnClickListener { onAudioClick(entry) }
                    notesAudio
                }
                else -> {
                    notesImage.isVisible = false
                    notesImage.setImageDrawable(null)
                    notesImage.setOnClickListener(null)
                    notesAudio.isVisible = false
                    notesTextBubble.isVisible = true
                    notesText.text = entry.text
                    // Urgency reads from the box's red outline (set below), so the note text itself
                    // stays the plain foreground colour.
                    notesText.setTextColor(root.context.getColorFromAttr(R.attr.primaryColor))
                    // Turn web/email URLs into tappable links while keeping the text selectable.
                    LinkifyCompat.addLinks(notesText, Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES)
                    notesText.movementMethod = LinkAndSelectMovementMethod
                    notesTextBubble
                }
            }

            // A still-open urgent entry gets the accent-red outline on its box; a completed one relaxes
            // back to the neutral outline (done takes precedence) and the whole box dims to signal it.
            activeBox.isActivated = entry.urgent && !entry.done
            activeBox.alpha = if (entry.done) 0.5f else 1f

            root.setOnLongClickListener {
                onItemLongClick(entry)
                true
            }
        }

        private fun bindTodoToggles(
            entry: NotesEntry,
            onToggleDone: (NotesEntry) -> Unit,
            onToggleUrgent: (NotesEntry) -> Unit,
        ) = with(binding) {
            val ctx = root.context
            // Monochrome, in keeping with the launcher's no-third-colour rule: a completed to-do
            // turns the full foreground colour, an incomplete one stays dimmed.
            val dim = ctx.getColorFromAttr(R.attr.primaryColorTrans50)
            notesDone.setImageResource(
                if (entry.done) R.drawable.ic_lucide_square_check else R.drawable.ic_lucide_square_outline
            )
            notesDone.setColorFilter(
                if (entry.done) ctx.getColorFromAttr(R.attr.primaryColor) else dim
            )
            notesDone.setOnClickListener { onToggleDone(entry) }

            // The urgent flag is the only accent colour allowed inside Notes.
            notesUrgent.setImageResource(
                if (entry.urgent) R.drawable.ic_lucide_flag_filled else R.drawable.ic_lucide_flag
            )
            notesUrgent.setColorFilter(
                if (entry.urgent) ContextCompat.getColor(ctx, R.color.notesUrgent) else dim
            )
            notesUrgent.setOnClickListener { onToggleUrgent(entry) }
        }

        private fun loadImage(path: String) {
            // Tag guards against a recycled view showing the wrong image.
            binding.notesImage.tag = path
            binding.notesImage.setImageDrawable(null)
            val file = File(path)
            if (!file.exists()) return
            val bitmap = decodeSampledBitmap(path, MAX_IMAGE_DIMEN, MAX_IMAGE_DIMEN)
            if (bitmap != null && binding.notesImage.tag == path) {
                binding.notesImage.setImageBitmap(bitmap)
            }
        }
    }

    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_DATE = 1
        private const val MAX_IMAGE_DIMEN = 1080
        private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

        private fun formatDuration(ms: Long): String {
            val totalSeconds = (ms / 1000).coerceAtLeast(0)
            val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds)
            val seconds = totalSeconds - minutes * 60
            return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
        }

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<NotesRow>() {
            override fun areItemsTheSame(oldItem: NotesRow, newItem: NotesRow) =
                oldItem::class == newItem::class && oldItem.rowId == newItem.rowId

            override fun areContentsTheSame(oldItem: NotesRow, newItem: NotesRow) =
                oldItem == newItem
        }
    }
}

/**
 * A movement method that lets a [TextView] both react to taps on links and stay selectable.
 *
 * [android.text.method.LinkMovementMethod] handles link clicks but disables text selection, while
 * the default selectable behaviour ([ArrowKeyMovementMethod]) supports selection but ignores link
 * taps. This combines the two: a tap on a [ClickableSpan] follows the link, anything else falls
 * through to the standard selection handling.
 */
internal object LinkAndSelectMovementMethod : ArrowKeyMovementMethod() {
    override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
        val action = event.action
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
            val x = event.x.toInt() - widget.totalPaddingLeft + widget.scrollX
            val y = event.y.toInt() - widget.totalPaddingTop + widget.scrollY
            val layout = widget.layout
            val line = layout.getLineForVertical(y)
            val off = layout.getOffsetForHorizontal(line, x.toFloat())
            val links = buffer.getSpans(off, off, ClickableSpan::class.java)
            if (links.isNotEmpty()) {
                if (action == MotionEvent.ACTION_UP) links[0].onClick(widget)
                return true
            }
        }
        return super.onTouchEvent(widget, buffer, event)
    }
}

/** Groups chronological [entries] into rows, inserting a date divider when the day changes. */
internal fun buildNotesRows(context: Context, entries: List<NotesEntry>): List<NotesRow> {
    val rows = ArrayList<NotesRow>(entries.size + 4)
    var lastDay = Long.MIN_VALUE
    for (entry in entries) {
        val dayStart = startOfDay(entry.timestamp)
        if (dayStart != lastDay) {
            rows.add(NotesRow.DateHeader(dayStart, dateLabel(context, dayStart)))
            lastDay = dayStart
        }
        rows.add(NotesRow.Item(entry))
    }
    return rows
}

private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private val notesDateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

private fun dateLabel(context: Context, dayStart: Long): String {
    val today = startOfDay(System.currentTimeMillis())
    val dayMs = 24L * 60 * 60 * 1000
    return when (dayStart) {
        today -> context.getString(R.string.notes_today)
        today - dayMs -> context.getString(R.string.notes_yesterday)
        else -> notesDateFormat.format(Date(dayStart))
    }
}

/** Decodes a bitmap from [path], downsampled so it fits within [reqWidth] x [reqHeight]. */
internal fun decodeSampledBitmap(path: String, reqWidth: Int, reqHeight: Int): Bitmap? =
    try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds, reqWidth, reqHeight)
        }
        BitmapFactory.decodeFile(path, options)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int,
): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}
