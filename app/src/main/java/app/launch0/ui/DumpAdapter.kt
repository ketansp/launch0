package app.launch0.ui

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.launch0.data.DumpEntry
import app.launch0.databinding.AdapterDumpItemBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DumpAdapter(
    private val onItemLongClick: (DumpEntry) -> Unit,
) : ListAdapter<DumpEntry, DumpAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AdapterDumpItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onItemLongClick)
    }

    class ViewHolder(private val binding: AdapterDumpItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: DumpEntry, onItemLongClick: (DumpEntry) -> Unit) = with(binding) {
            dumpTime.text = timeFormat.format(Date(entry.timestamp))

            if (entry.isImage && !entry.imagePath.isNullOrEmpty()) {
                dumpText.isVisible = false
                dumpImage.isVisible = true
                loadImage(entry.imagePath)
            } else {
                dumpImage.isVisible = false
                dumpImage.setImageDrawable(null)
                dumpText.isVisible = true
                dumpText.text = entry.text
            }

            root.setOnLongClickListener {
                onItemLongClick(entry)
                true
            }
        }

        private fun loadImage(path: String) {
            // Tag guards against a recycled view showing the wrong image.
            binding.dumpImage.tag = path
            binding.dumpImage.setImageDrawable(null)
            val file = File(path)
            if (!file.exists()) return
            val bitmap = decodeSampledBitmap(path, MAX_IMAGE_DIMEN, MAX_IMAGE_DIMEN)
            if (bitmap != null && binding.dumpImage.tag == path) {
                binding.dumpImage.setImageBitmap(bitmap)
            }
        }

        private fun decodeSampledBitmap(path: String, reqWidth: Int, reqHeight: Int) =
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
    }

    companion object {
        private const val MAX_IMAGE_DIMEN = 1080
        private val timeFormat = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<DumpEntry>() {
            override fun areItemsTheSame(oldItem: DumpEntry, newItem: DumpEntry) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: DumpEntry, newItem: DumpEntry) =
                oldItem == newItem
        }
    }
}
