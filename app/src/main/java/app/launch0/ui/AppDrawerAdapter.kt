package app.launch0.ui

import android.content.Context
import android.os.UserHandle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ImageSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.launch0.R
import app.launch0.data.AppModel
import app.launch0.data.Constants
import app.launch0.databinding.AdapterAppDrawerBinding
import app.launch0.helper.getNotificationCountDrawable
import app.launch0.helper.getShapedAppIcon
import app.launch0.helper.hideKeyboard
import app.launch0.helper.isSystemApp
import app.launch0.helper.pillTouchListener
import app.launch0.helper.showKeyboard
import java.text.Normalizer

class AppDrawerAdapter(
    private var flag: Int,
    private val appLabelGravity: Int,
    private val showAppIcons: Boolean,
    private val showAppNames: Boolean,
    private val iconSizePx: Int,
    private val iconShape: Int,
    private val appClickListener: (AppModel) -> Unit,
    private val appInfoListener: (AppModel) -> Unit,
    private val appDeleteListener: (AppModel) -> Unit,
    private val appHideListener: (AppModel, Int) -> Unit,
    private val appRenameListener: (AppModel, String) -> Unit,
    private val isDndApp: (String) -> Boolean = { false },
    private val parkedNotificationCount: (String) -> Int = { 0 },
    private val onReleaseNotifications: (AppModel) -> Unit = {},
) : ListAdapter<AppModel, AppDrawerAdapter.ViewHolder>(DIFF_CALLBACK), Filterable {

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AppModel>() {
            override fun areItemsTheSame(oldItem: AppModel, newItem: AppModel): Boolean = when {
                oldItem is AppModel.App && newItem is AppModel.App ->
                    oldItem.appPackage == newItem.appPackage && oldItem.user == newItem.user

                oldItem is AppModel.PinnedShortcut && newItem is AppModel.PinnedShortcut ->
                    oldItem.shortcutId == newItem.shortcutId && oldItem.user == newItem.user

                else -> false
            }

            override fun areContentsTheSame(oldItem: AppModel, newItem: AppModel): Boolean =
                oldItem == newItem
        }
    }

    private var autoLaunch = true
    private var isBangSearch = false
    private val appFilter = createAppFilter()
    private val myUserHandle = android.os.Process.myUserHandle()

    var appsList: MutableList<AppModel> = mutableListOf()
    var appFilteredList: MutableList<AppModel> = mutableListOf()

    /** Invoked after the displayed list changes so the alphabet index can be refreshed. */
    var onListUpdated: (() -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            AdapterAppDrawerBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try {
            if (appFilteredList.size == 0 || position == RecyclerView.NO_POSITION) return
            val appModel = appFilteredList[holder.bindingAdapterPosition]
            holder.bind(
                flag,
                appLabelGravity,
                showAppIcons,
                showAppNames,
                iconSizePx,
                iconShape,
                myUserHandle,
                appModel,
                appClickListener,
                appDeleteListener,
                appInfoListener,
                appHideListener,
                appRenameListener,
                isDndApp,
                parkedNotificationCount,
                onReleaseNotifications
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getFilter(): Filter = this.appFilter

    private fun createAppFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSearch: CharSequence?): FilterResults {
                isBangSearch = charSearch?.startsWith("!") ?: false
                autoLaunch = charSearch?.startsWith(" ")?.not() ?: true

                val appFilteredList = (if (charSearch.isNullOrBlank()) appsList
                else appsList.filter { app ->
                    appLabelMatches(app.appLabel, charSearch)
                }
                    // Rank apps whose name starts with the term above mid-string matches.
                    // sortedByDescending is stable, so the original (alphabetical) order is
                    // preserved within each group.
                    .sortedByDescending { app -> appLabelStartsWith(app.appLabel, charSearch) }
                    .toMutableList())

                val filterResults = FilterResults()
                filterResults.values = appFilteredList
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                results?.values?.let {
                    val items = it as MutableList<AppModel>
                    appFilteredList = items
                    submitList(appFilteredList) {
                        autoLaunch()
                        onListUpdated?.invoke()
                    }
                }
            }
        }
    }

    private fun autoLaunch() {
        try {
            if (itemCount == 1
                && autoLaunch
                && isBangSearch.not()
                && flag == Constants.FLAG_LAUNCH_APP
                && appFilteredList.size > 0
            ) appClickListener(appFilteredList[0])
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun appLabelMatches(appLabel: String, charSearch: CharSequence): Boolean {
        return (appLabel.contains(charSearch.trim(), true) or
                Normalizer.normalize(appLabel, Normalizer.Form.NFD)
                    .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
                    .replace(Regex("[-_+,. ]"), "")
                    .contains(charSearch, true))
    }

    /** True when the app name starts with the search term (raw or accent/separator-normalized). */
    private fun appLabelStartsWith(appLabel: String, charSearch: CharSequence): Boolean {
        val term = charSearch.trim()
        if (term.isEmpty()) return false
        return (appLabel.startsWith(term, true) ||
                Normalizer.normalize(appLabel, Normalizer.Form.NFD)
                    .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
                    .replace(Regex("[-_+,. ]"), "")
                    .startsWith(term.toString(), true))
    }

    fun setAppList(appsList: MutableList<AppModel>) {
        // Add empty app for bottom padding in recyclerview and assign to list
        appsList.add(
            AppModel.App(
                appLabel = "",
                key = null,
                appPackage = "",
                activityClassName = "",
                isNew = false,
                user = android.os.Process.myUserHandle()
            )
        )
        this.appsList = appsList
        this.appFilteredList = appsList
        submitList(appsList) { onListUpdated?.invoke() }
    }

    fun launchFirstInList() {
        if (appFilteredList.size > 0)
            appClickListener(appFilteredList[0])
    }

    /** Distinct section letters present in the current list, in their alphabetical order. */
    fun getSections(): List<String> =
        appFilteredList.mapNotNull { sectionFor(it.appLabel) }.distinct()

    /** Position of the first app belonging to [section], or -1 if none. */
    fun getPositionForSection(section: String): Int =
        appFilteredList.indexOfFirst { sectionFor(it.appLabel) == section }

    /**
     * Section a label belongs to: its first letter (accent-stripped, uppercased), or "#" when the
     * name starts with a digit or symbol. Blank labels (e.g. the trailing padding row) return null.
     */
    private fun sectionFor(appLabel: String): String? {
        val normalized = Normalizer.normalize(appLabel.trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        val first = normalized.firstOrNull() ?: return null
        val upper = first.uppercaseChar()
        return if (upper in 'A'..'Z') upper.toString() else "#"
    }

    class ViewHolder(private val binding: AdapterAppDrawerBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            flag: Int,
            appLabelGravity: Int,
            showAppIcons: Boolean,
            showAppNames: Boolean,
            iconSizePx: Int,
            iconShape: Int,
            myUserHandle: UserHandle,
            appModel: AppModel,
            clickListener: (AppModel) -> Unit,
            appDeleteListener: (AppModel) -> Unit,
            appInfoListener: (AppModel) -> Unit,
            appHideListener: (AppModel, Int) -> Unit,
            appRenameListener: (AppModel, String) -> Unit,
            isDndApp: (String) -> Boolean,
            parkedNotificationCount: (String) -> Int,
            onReleaseNotifications: (AppModel) -> Unit,
        ) = with(binding) {
            appHideLayout.visibility = View.GONE
            renameLayout.visibility = View.GONE
            appTitle.visibility = View.VISIBLE

            // Icons and names can't both be hidden, so only drop the name when icons are shown.
            val showNames = showAppNames || !showAppIcons
            // Show indicators in title based on app type and state
            appTitle.text = if (showNames) buildString {
                if (flag == Constants.FLAG_SET_DND_APPS
                    && appModel.appPackage.isNotEmpty()
                    && isDndApp(appModel.appPackage)
                ) append("✓ ")
                append(appModel.appLabel)
                if (appModel.isNew) append(" ✦")
            } else ""
            appTitle.gravity = appLabelGravity
            val notificationCount =
                if (appModel.appPackage.isNotEmpty()) parkedNotificationCount(appModel.appPackage) else 0
            setAppTitleDecorations(
                appTitle, appModel, showAppIcons, showNames, iconSizePx, iconShape,
                appLabelGravity, notificationCount,
            )
            otherProfileIndicator.isVisible = appModel.user != myUserHandle

            appTitle.setOnClickListener { clickListener(appModel) }
            appTitle.setOnTouchListener(
                pillTouchListener { onReleaseNotifications(appModel) }
            )

            appTitle.setOnLongClickListener {
                if (appModel.appPackage.isNotEmpty()) {
                    appDelete.alpha = when (
                        appModel is AppModel.PinnedShortcut || !root.context.isSystemApp(appModel.appPackage)
                    ) {
                        true -> 1.0f
                        false -> 0.5f
                    }
                    appHide.text = if (flag == Constants.FLAG_HIDDEN_APPS)
                        root.context.getString(R.string.adapter_show)
                    else
                        root.context.getString(R.string.adapter_hide)
                    appTitle.visibility = View.INVISIBLE
                    appHide.alpha = when (appModel is AppModel.PinnedShortcut) {
                        true -> 0.5f
                        false -> 1.0f
                    }
                    appHideLayout.visibility = View.VISIBLE
                    // Only allow renaming non hidden apps
                    appRename.isVisible = flag != Constants.FLAG_HIDDEN_APPS
                }
                true
            }

            // Configure rename behavior
            appRename.setOnClickListener {
                if (appModel.appPackage.isNotEmpty()) {
                    etAppRename.hint = getAppName(etAppRename.context, appModel.appPackage)
                    etAppRename.setText(appModel.appLabel)
                    etAppRename.setSelectAllOnFocus(true)
                    renameLayout.visibility = View.VISIBLE
                    appHideLayout.visibility = View.GONE
                    etAppRename.showKeyboard()
                    etAppRename.imeOptions = EditorInfo.IME_ACTION_DONE
                }
            }
            etAppRename.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                appTitle.visibility = if (hasFocus) View.INVISIBLE else View.VISIBLE
            }
            etAppRename.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    etAppRename.hint = getAppName(etAppRename.context, appModel.appPackage)
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    etAppRename.hint = ""
                }
            })
            etAppRename.setOnEditorActionListener { _, actionCode, _ ->
                if (actionCode == EditorInfo.IME_ACTION_DONE) {
                    val renameLabel = etAppRename.text.toString().trim()
                    if (renameLabel.isNotBlank() && appModel.appPackage.isNotBlank()) {
                        appRenameListener(appModel, renameLabel)
                        renameLayout.visibility = View.GONE
                    }
                    true
                }
                false
            }
            tvSaveRename.setOnClickListener {
                etAppRename.hideKeyboard()
                val renameLabel = etAppRename.text.toString().trim()
                if (renameLabel.isNotBlank() && appModel.appPackage.isNotBlank()) {
                    appRenameListener(appModel, renameLabel)
                    renameLayout.visibility = View.GONE
                } else {
                    val packageManager = etAppRename.context.packageManager
                    appRenameListener(
                        appModel,
                        packageManager.getApplicationLabel(
                            packageManager.getApplicationInfo(appModel.appPackage, 0)
                        ).toString()
                    )
                    renameLayout.visibility = View.GONE
                }
            }
            appInfo.setOnClickListener { appInfoListener(appModel) }
            appDelete.setOnClickListener { appDeleteListener(appModel) }
            appMenuClose.setOnClickListener {
                appHideLayout.visibility = View.GONE
                appTitle.visibility = View.VISIBLE
            }
            appRenameClose.setOnClickListener {
                renameLayout.visibility = View.GONE
                appTitle.visibility = View.VISIBLE
            }
            appHide.setOnClickListener { appHideListener(appModel, bindingAdapterPosition) }
        }

        /**
         * Shows the app icon when enabled and, when DND has parked notifications for the app, a
         * count pill on the side opposite the app name. With a visible label the icon sits next to
         * it (on the side matching its alignment); in icons-only mode ([showNames] false) the icon
         * is rendered inline so it follows the row alignment, with the pill alongside as a compound
         * drawable. The view is tagged so the pill can be hit-tested for taps (which release the
         * parked notifications).
         */
        private fun setAppTitleDecorations(
            textView: TextView,
            appModel: AppModel,
            showAppIcons: Boolean,
            showNames: Boolean,
            iconSizePx: Int,
            iconShape: Int,
            gravity: Int,
            notificationCount: Int,
        ) {
            val pill = if (notificationCount > 0)
                textView.context.getNotificationCountDrawable(notificationCount)
            else null
            val iconOnEnd = gravity == Gravity.END
            // The pill always sits opposite the app name: the start slot when the name (and icon)
            // are end-aligned, the end slot otherwise.
            val pillInStart = iconOnEnd

            val icon = if (showAppIcons && appModel.appPackage.isNotEmpty())
                textView.context.getShapedAppIcon(appModel.appPackage, appModel.user.toString(), iconSizePx, iconShape)
            else null
            icon?.setBounds(0, 0, iconSizePx, iconSizePx)

            if (icon != null && !showNames) {
                // Icons-only: render the icon inline so it honours the row alignment; place the pill
                // (if any) as a compound drawable on the opposite side.
                textView.text = SpannableString(" ").apply {
                    setSpan(ImageSpan(icon, ImageSpan.ALIGN_BOTTOM), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                textView.setCompoundDrawablesRelative(
                    if (pillInStart) pill else null, null, if (pillInStart) null else pill, null,
                )
            } else if (icon == null && pill == null) {
                textView.setCompoundDrawables(null, null, null, null)
            } else {
                // Icon on the alignment side, pill on the opposite side of the name.
                textView.setCompoundDrawablesRelative(
                    if (iconOnEnd) pill else icon, null, if (iconOnEnd) icon else pill, null,
                )
            }
            textView.compoundDrawablePadding = (textView.resources.displayMetrics.density * 12).toInt()
            textView.setTag(R.id.notif_pill_side, if (pill != null) pillInStart else null)
        }

        private fun getAppName(context: Context, appPackage: String): String {
            val packageManager = context.packageManager
            return packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(appPackage, 0)
            ).toString()
        }
    }
}
