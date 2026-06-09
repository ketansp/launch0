package app.launch0.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.Spannable
import android.text.method.ArrowKeyMovementMethod
import android.text.style.ClickableSpan
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.util.LinkifyCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.launch0.data.NotesEntry
import app.launch0.databinding.AdapterNotesItemBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotesAdapter(
    private val onItemLongClick: (NotesEntry) -> Unit,
    private val onImageClick: (NotesEntry) -> Unit,
) : ListAdapter<NotesEntry, NotesAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AdapterNotesItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onItemLongClick, onImageClick)
    }

    class ViewHolder(private val binding: AdapterNotesItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            entry: NotesEntry,
            onItemLongClick: (NotesEntry) -> Unit,
            onImageClick: (NotesEntry) -> Unit,
        ) = with(binding) {
            notesTime.text = timeFormat.format(Date(entry.timestamp))

            if (entry.isImage && !entry.imagePath.isNullOrEmpty()) {
                notesText.isVisible = false
                notesImage.isVisible = true
                loadImage(entry.imagePath)
                notesImage.setOnClickListener { onImageClick(entry) }
            } else {
                notesImage.isVisible = false
                notesImage.setImageDrawable(null)
                notesImage.setOnClickListener(null)
                notesText.isVisible = true
                notesText.text = entry.text
                // Turn web/email URLs into tappable links while keeping the text selectable.
                LinkifyCompat.addLinks(notesText, Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES)
                notesText.movementMethod = LinkAndSelectMovementMethod
            }

            root.setOnLongClickListener {
                onItemLongClick(entry)
                true
            }
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
        private const val MAX_IMAGE_DIMEN = 1080
        private val timeFormat = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<NotesEntry>() {
            override fun areItemsTheSame(oldItem: NotesEntry, newItem: NotesEntry) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: NotesEntry, newItem: NotesEntry) =
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
