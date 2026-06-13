package app.launch0.ui

import android.app.PendingIntent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.launch0.databinding.AdapterNotificationItemBinding

/**
 * A single active notification surfaced on the quick-actions panel. Built from a
 * [android.service.notification.StatusBarNotification] in [QuickActionsFragment].
 */
data class NotificationItem(
    val key: String,
    val appName: String,
    val title: String,
    val text: String,
    val contentIntent: PendingIntent?,
    val isClearable: Boolean,
)

class NotificationAdapter(
    private val onItemClick: (NotificationItem) -> Unit,
    private val onItemLongClick: (NotificationItem) -> Unit,
) : ListAdapter<NotificationItem, NotificationAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AdapterNotificationItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick, onItemLongClick)
    }

    class ViewHolder(private val binding: AdapterNotificationItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: NotificationItem,
            onItemClick: (NotificationItem) -> Unit,
            onItemLongClick: (NotificationItem) -> Unit,
        ) = with(binding) {
            notifAppName.text = item.appName
            notifTitle.isVisible = item.title.isNotEmpty()
            notifTitle.text = item.title
            notifText.isVisible = item.text.isNotEmpty()
            notifText.text = item.text

            root.setOnClickListener { onItemClick(item) }
            root.setOnLongClickListener {
                onItemLongClick(item)
                true
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<NotificationItem>() {
            override fun areItemsTheSame(oldItem: NotificationItem, newItem: NotificationItem) =
                oldItem.key == newItem.key

            override fun areContentsTheSame(oldItem: NotificationItem, newItem: NotificationItem) =
                oldItem == newItem
        }
    }
}
