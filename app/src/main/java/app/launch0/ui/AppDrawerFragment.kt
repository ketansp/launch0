package app.launch0.ui

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import app.launch0.MainViewModel
import app.launch0.R
import app.launch0.data.AppModel
import app.launch0.data.Constants
import app.launch0.data.Prefs
import app.launch0.databinding.FragmentAppDrawerBinding
import app.launch0.helper.deletePinnedShortcut
import app.launch0.helper.dpToPx
import app.launch0.helper.hideKeyboard
import app.launch0.helper.isEinkDisplay
import app.launch0.helper.isSystemApp
import app.launch0.helper.openAppInfo
import app.launch0.helper.openSearch
import app.launch0.helper.openUrl
import app.launch0.helper.showKeyboard
import app.launch0.helper.showToast
import app.launch0.helper.uninstall

class AppDrawerFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var adapter: AppDrawerAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager

    private var flag = Constants.FLAG_LAUNCH_APP
    private var canRename = false
    private var previousSoftInputMode: Int? = null

    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: FragmentAppDrawerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAppDrawerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        arguments?.let {
            flag = it.getInt(Constants.Key.FLAG, Constants.FLAG_LAUNCH_APP)
            canRename = it.getBoolean(Constants.Key.RENAME, false)
        }

        applyWindowInsets()
        initViews()
        initSearch()
        initAdapter()
        initAlphabetIndex()
        initObservers()
        initClickListeners()
    }

    /**
     * Pushes the bottom-aligned search bar (and the list above it) clear of the keyboard by
     * padding the drawer with the IME inset, falling back to the system bar inset when the
     * keyboard is hidden. This is more reliable than panning the window, which left the list
     * and search bar tucked behind the keyboard.
     */
    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.updatePadding(
                top = bars.top,
                bottom = maxOf(bars.bottom, ime.bottom),
            )
            insets
        }
    }

    private fun initViews() {
        if (flag == Constants.FLAG_HIDDEN_APPS)
            binding.search.queryHint = getString(R.string.hidden_apps)
        else if (flag == Constants.FLAG_SET_DND_APPS)
            binding.search.queryHint = getString(R.string.dnd_select_apps_hint)
        else if (flag in Constants.FLAG_SET_HOME_APP_1..Constants.FLAG_SET_CALENDAR_APP)
            binding.search.queryHint = "Please select an app"
        try {
            val searchTextView = binding.search.findViewById<TextView>(R.id.search_src_text)
            if (searchTextView != null) searchTextView.gravity = prefs.appLabelAlignment
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initSearch() {
        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query?.startsWith("!") == true)
                    requireContext().openUrl(Constants.URL_DUCK_SEARCH + query.replace(" ", "%20"))
                else if (adapter.itemCount == 0)
                    requireContext().openSearch(query?.trim())
                else
                    adapter.launchFirstInList()
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                try {
                    // While searching, flip the list so the most relevant result (item 0 after
                    // the adapter's ranking sort) renders at the bottom, closest to the search
                    // box and keyboard. Cleared search reverts to the normal top-anchored list.
                    val searching = newText.isNotBlank()
                    if (linearLayoutManager.reverseLayout != searching)
                        linearLayoutManager.reverseLayout = searching
                    adapter.filter.filter(newText)
                    binding.appRename.visibility =
                        if (canRename && newText.isNotBlank()) View.VISIBLE else View.GONE
                    return true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return false
            }
        })
    }

    private fun initAdapter() {
        adapter = AppDrawerAdapter(
            flag,
            prefs.appLabelAlignment,
            prefs.showAppIcons,
            prefs.showAppNames,
            prefs.iconSize.dpToPx(),
            prefs.iconShape,
            appClickListener = { appModel ->
                if (flag == Constants.FLAG_SET_DND_APPS) {
                    toggleDndApp(appModel)
                } else {
                    viewModel.selectedApp(appModel, flag)
                    if (flag == Constants.FLAG_LAUNCH_APP || flag == Constants.FLAG_HIDDEN_APPS)
                        findNavController().popBackStack(R.id.mainFragment, false)
                    else
                        findNavController().popBackStack()
                }
            },
            appInfoListener = {
                openAppInfo(
                    requireContext(),
                    it.user,
                    it.appPackage
                )
                findNavController().popBackStack(R.id.mainFragment, false)
            },
            appDeleteListener = { appModel ->
                when (appModel) {
                    is AppModel.PinnedShortcut ->
                        requireContext().deletePinnedShortcut(
                            packageName = appModel.appPackage,
                            shortcutIdToDelete = appModel.shortcutId,
                            user = appModel.user,
                        )

                    is AppModel.App -> {
                        requireContext().apply {
                            if (isSystemApp(appModel.appPackage))
                                showToast(getString(R.string.system_app_cannot_delete))
                            else
                                uninstall(appModel.appPackage)
                        }
                    }
                }
                viewModel.getAppList()
            },
            appHideListener = { appModel, position ->
                if (appModel is AppModel.PinnedShortcut) {
                    requireContext().showToast("Hiding pinned shortcuts is not supported")
                    return@AppDrawerAdapter
                }
                adapter.appFilteredList.removeAt(position)
                adapter.notifyItemRemoved(position)
                adapter.appsList.remove(appModel)

                val newSet = mutableSetOf<String>()
                newSet.addAll(prefs.hiddenApps)
                if (flag == Constants.FLAG_HIDDEN_APPS) {
                    newSet.remove(appModel.appPackage) // for backward compatibility
                    newSet.remove(appModel.appPackage + "|" + appModel.user.toString())
                } else
                    newSet.add(appModel.appPackage + "|" + appModel.user.toString())

                prefs.hiddenApps = newSet
                if (newSet.isEmpty())
                    findNavController().popBackStack()
                if (prefs.firstHide) {
                    binding.search.hideKeyboard()
                    prefs.firstHide = false
                    findNavController().navigate(R.id.action_appListFragment_to_settingsFragment2)
                }
                viewModel.getAppList()
                viewModel.getHiddenApps()
            },
            appRenameListener = { appModel, renameLabel ->
                val identifier = when (appModel) {
                    is AppModel.PinnedShortcut -> appModel.shortcutId
                    is AppModel.App -> appModel.appPackage
                }
                prefs.setAppRenameLabel(identifier, renameLabel)
                viewModel.getAppList()
            },
            isDndApp = { appPackage -> prefs.dndApps.contains(appPackage) }
        )

        linearLayoutManager = object : LinearLayoutManager(requireContext()) {
            override fun scrollVerticallyBy(
                dx: Int,
                recycler: Recycler,
                state: RecyclerView.State,
            ): Int {
                val scrollRange = super.scrollVerticallyBy(dx, recycler, state)
                val overScroll = dx - scrollRange
                if (overScroll < -10 && binding.recyclerView.scrollState == RecyclerView.SCROLL_STATE_DRAGGING)
                    checkMessageAndExit()
                return scrollRange
            }
        }

        binding.recyclerView.layoutManager = linearLayoutManager
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addOnScrollListener(getRecyclerViewOnScrollListener())
        binding.recyclerView.itemAnimator = null
        if (requireContext().isEinkDisplay().not())
            binding.recyclerView.layoutAnimation =
                AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_anim_from_bottom)
    }

    private fun initAlphabetIndex() {
        // The list (and the index beside it) span exactly half of the screen height.
        binding.listContainer.layoutParams = binding.listContainer.layoutParams.apply {
            height = resources.displayMetrics.heightPixels / 2
        }

        // Place the index opposite the app name alignment: names on the right (END) put the
        // index on the left, otherwise it sits on the right.
        val onEndSide = prefs.appLabelAlignment != Gravity.END
        (binding.alphabetIndex.layoutParams as FrameLayout.LayoutParams).gravity =
            (if (onEndSide) Gravity.END else Gravity.START) or Gravity.CENTER_VERTICAL

        binding.alphabetIndex.setTextColor(themeColor(R.attr.primaryColor))
        binding.alphabetIndex.setOnSectionSelectedListener { section ->
            val position = adapter.getPositionForSection(section)
            if (position >= 0) linearLayoutManager.scrollToPositionWithOffset(position, 0)
        }
        adapter.onListUpdated = { refreshAlphabetIndex() }

        // Reserve space on the index side so long app names don't render under the letters.
        val unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        binding.alphabetIndex.measure(unspecified, unspecified)
        val reserve = binding.alphabetIndex.measuredWidth
        if (onEndSide)
            binding.recyclerView.setPaddingRelative(0, 0, reserve, 0)
        else
            binding.recyclerView.setPaddingRelative(reserve, 0, 0, 0)
    }

    /** Rebuilds the alphabet index from the current list, hiding it while a search is active. */
    private fun refreshAlphabetIndex() {
        if (_binding == null) return
        val searching = binding.search.query?.isNotBlank() == true
        val sections = if (searching) emptyList() else adapter.getSections()
        binding.alphabetIndex.setSections(sections)
        binding.alphabetIndex.visibility = if (sections.size > 1) View.VISIBLE else View.GONE
    }

    private fun themeColor(attr: Int): Int {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(attr, typedValue, true)
        return if (typedValue.resourceId != 0)
            ContextCompat.getColor(requireContext(), typedValue.resourceId)
        else typedValue.data
    }

    private fun initObservers() {
        if (flag == Constants.FLAG_HIDDEN_APPS) {
            viewModel.hiddenApps.observe(viewLifecycleOwner) {
                it?.let {
                    adapter.setAppList(it.toMutableList())
                }
            }
        } else {
            viewModel.appList.observe(viewLifecycleOwner) {
                it?.let { appModels ->
                    adapter.setAppList(appModels.toMutableList())
                    adapter.filter.filter(binding.search.query)
                }
            }
        }
    }

    private fun initClickListeners() {
        binding.appRename.setOnClickListener {
            val name = binding.search.query.toString().trim()
            if (name.isEmpty()) {
                requireContext().showToast(getString(R.string.type_a_new_app_name_first))
                binding.search.showKeyboard()
                return@setOnClickListener
            }

            when (flag) {
                Constants.FLAG_SET_HOME_APP_1 -> prefs.appName1 = name
                Constants.FLAG_SET_HOME_APP_2 -> prefs.appName2 = name
                Constants.FLAG_SET_HOME_APP_3 -> prefs.appName3 = name
                Constants.FLAG_SET_HOME_APP_4 -> prefs.appName4 = name
                Constants.FLAG_SET_HOME_APP_5 -> prefs.appName5 = name
                Constants.FLAG_SET_HOME_APP_6 -> prefs.appName6 = name
                Constants.FLAG_SET_HOME_APP_7 -> prefs.appName7 = name
                Constants.FLAG_SET_HOME_APP_8 -> prefs.appName8 = name
            }
            findNavController().popBackStack()
        }
    }

    private fun getRecyclerViewOnScrollListener(): RecyclerView.OnScrollListener {
        return object : RecyclerView.OnScrollListener() {

            var onTop = false

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {

                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        onTop = !recyclerView.canScrollVertically(-1)
                        if (onTop)
                            binding.search.hideKeyboard()
                    }

                    RecyclerView.SCROLL_STATE_IDLE -> {
                        if (!recyclerView.canScrollVertically(1))
                            binding.search.hideKeyboard()
                        else if (!recyclerView.canScrollVertically(-1))
                            if (!onTop && isRemoving.not())
                                binding.search.showKeyboard(prefs.autoShowKeyboard)
                    }
                }
            }
        }
    }

    private fun toggleDndApp(appModel: AppModel) {
        if (appModel.appPackage.isBlank()) return
        val dndApps = prefs.dndApps
        if (!dndApps.add(appModel.appPackage)) dndApps.remove(appModel.appPackage)
        prefs.dndApps = dndApps
        val position = adapter.appFilteredList.indexOf(appModel)
        if (position >= 0) adapter.notifyItemChanged(position)
    }

    private fun checkMessageAndExit() {
        findNavController().popBackStack()
        if (flag == Constants.FLAG_LAUNCH_APP)
            viewModel.checkForMessages.call()
    }

    override fun onStart() {
        super.onStart()
        // Resize the window for the keyboard so the IME inset is dispatched to
        // applyWindowInsets(), which lifts the list and search bar above the keyboard.
        // The previous mode is restored in onStop.
        requireActivity().window.let { window ->
            previousSoftInputMode = window.attributes.softInputMode
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        binding.search.showKeyboard(prefs.autoShowKeyboard)
    }

    override fun onStop() {
        binding.search.hideKeyboard()
        previousSoftInputMode?.let { requireActivity().window.setSoftInputMode(it) }
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}