package com.example.ui.fragments

import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.HymnCatalog
import com.example.data.local.SongEntity
import com.example.data.repository.SongRepository
import com.example.ui.adapters.HikarokaSongAdapter
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * High-Performance Hikaroka (Search & Browse) Screen.
 * Enforces the 5 Bible-App 60 FPS engineering rules:
 * 1. Pre-indexed in-memory 830 hymns (zero JSON/Room lag on scroll).
 * 2. Strict 25-per-page pagination + smooth +25 Infinite Scroll.
 * 3. Dual-speed search engine: 0ms instant for numbers, 120ms debounce for text.
 * 4. Lightweight 6-property ViewHolders with stable IDs and no itemAnimator.
 * 5. Real-time favorites (double-tap or heart icon) & FIFO-10 consultation history.
 */
class HikarokaFragment : Fragment() {

    private lateinit var repository: SongRepository
    private lateinit var songAdapter: HikarokaSongAdapter

    private var allSongsCache: List<SongEntity> = emptyList()
    private var currentMatchedSongs: List<SongEntity> = emptyList()

    private var currentCategoryFilter: String = "REHETRA"
    private var currentSearchQuery: String = ""
    private var currentDisplayLimit: Int = HymnCatalog.PAGE_SIZE

    private var textSearchDebounceJob: Job? = null
    private var pendingExternalQuery: String? = null
    private var pendingOpenNumpad: Boolean = false

    private var etSearch: EditText? = null
    private var btnClearSearch: ImageButton? = null
    private var tvResultsCount: TextView? = null
    private var tvEmptyState: TextView? = null
    private var layoutEmptyState: View? = null
    private var btnImportHymnsJson: View? = null
    private var rvResults: RecyclerView? = null

    // Numpad Overlay Views
    private var numpadOverlay: FrameLayout? = null
    private var currentNumpadValue: String = ""

    private val jsonFilePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val appCtx = requireContext().applicationContext
            viewLifecycleOwner.lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val content = appCtx.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader().readText()
                    }.orEmpty()
                    if (content.isNotEmpty()) {
                        val success = HymnCatalog.importHymnsFromJsonString(appCtx, content)
                        if (success) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                resetLimitAndExecuteSearch()
                            }
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_hikaroka, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ctx = requireContext()
        HymnCatalog.ensureInitializedAsync(ctx)
        val dao = AppDatabase.getInstance(ctx).songDao()
        repository = SongRepository(dao, ctx.applicationContext)

        val btnBack = view.findViewById<MaterialCardView>(R.id.btn_hikaroka_back)
        val btnTopSearch = view.findViewById<MaterialCardView>(R.id.btn_hikaroka_top_search)
        etSearch = view.findViewById(R.id.et_hikaroka_query)
        btnClearSearch = view.findViewById(R.id.btn_search_clear)
        val btnOpenNumpad = view.findViewById<MaterialCardView>(R.id.btn_numpad_toggle)

        val chipAll = view.findViewById<MaterialCardView>(R.id.chip_filter_all)
        val chipLehibe = view.findViewById<MaterialCardView>(R.id.chip_filter_lehibe)
        val chipFanampiny = view.findViewById<MaterialCardView>(R.id.chip_filter_fanampiny)

        val iconChipAll = view.findViewById<ImageView>(R.id.icon_chip_all)
        val tvChipAll = view.findViewById<TextView>(R.id.tv_chip_all)
        val iconChipLehibe = view.findViewById<ImageView>(R.id.icon_chip_lehibe)
        val tvChipLehibe = view.findViewById<TextView>(R.id.tv_chip_lehibe)
        val iconChipFanampiny = view.findViewById<ImageView>(R.id.icon_chip_fanampiny)
        val tvChipFanampiny = view.findViewById<TextView>(R.id.tv_chip_fanampiny)

        tvResultsCount = view.findViewById(R.id.tv_hikaroka_results_count)
        tvEmptyState = view.findViewById(R.id.tv_hikaroka_empty)
        layoutEmptyState = view.findViewById(R.id.layout_hikaroka_empty)
        btnImportHymnsJson = view.findViewById(R.id.btn_import_hymns_json)
        rvResults = view.findViewById(R.id.rv_hikaroka_results)

        btnImportHymnsJson?.setOnClickListener {
            jsonFilePickerLauncher.launch("*/*")
        }

        numpadOverlay = view.findViewById(R.id.numpad_overlay)
        val numpadBackdrop = view.findViewById<View>(R.id.numpad_backdrop)
        val btnCloseNumpad = view.findViewById<MaterialCardView>(R.id.numpad_close_btn)
        val btnClearAllNumpad = view.findViewById<MaterialCardView>(R.id.numpad_clear_all)

        // Setup high-performance 60 FPS RecyclerView
        songAdapter = HikarokaSongAdapter(
            onSongClick = { song ->
                HymnCatalog.recordSongConsulted(requireContext(), song.number)
                (activity as? MainActivity)?.openHymnReader(song.number, currentSearchQuery)
            },
            onFavoriteDoubleTap = { song ->
                HymnCatalog.toggleFavorite(requireContext(), song.number)
            }
        )

        val layoutManager = LinearLayoutManager(requireContext())
        rvResults?.apply {
            this.layoutManager = layoutManager
            adapter = songAdapter
            setHasFixedSize(true)
            itemAnimator = null
            setItemViewCacheSize(25)

            // Rule 2: Strict 25-per-page pagination with smooth +25 Infinite Scroll
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy <= 0) return
                    val totalDisplayed = songAdapter.itemCount
                    if (totalDisplayed >= currentMatchedSongs.size) return

                    val lastVisible = layoutManager.findLastVisibleItemPosition()
                    if (lastVisible >= totalDisplayed - 6) {
                        currentDisplayLimit = (currentDisplayLimit + HymnCatalog.PAGE_SIZE)
                            .coerceAtMost(currentMatchedSongs.size)
                        renderPaginatedSlice()
                    }
                }
            })
        }

        // Top Bar Actions
        btnBack.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(MainActivity.TAB_FANDRAISANA)
        }
        btnTopSearch.setOnClickListener {
            etSearch?.requestFocus()
        }

        // Clear search button
        btnClearSearch?.setOnClickListener {
            etSearch?.setText("")
            currentNumpadValue = ""
        }

        // Category Filter Chips (Rehetra / Fihirana Taloha / Fihirana Vaovao)
        fun updateFilterChipsUI() {
            val isDark = com.example.ui.theme.ThemeManager.isDarkMode.value
            val activeBg = if (isDark) 0xFF800020.toInt() else 0xFF5A1827.toInt()
            val inactiveBg = if (isDark) 0xFF1E1214.toInt() else 0xFFFFFFFF.toInt()
            val white = 0xFFFFFFFF.toInt()
            val inactiveColor = if (isDark) 0xFFA69497.toInt() else 0xFF564243.toInt()

            fun styleChip(
                card: MaterialCardView,
                icon: ImageView,
                tv: TextView,
                isActive: Boolean
            ) {
                com.example.ui.theme.ThemeManager.stabilizeCardForApi23(
                    card,
                    if (isActive) activeBg else inactiveBg,
                    0,
                    0
                )
                com.example.ui.theme.ThemeManager.setSolidIconTint(icon, if (isActive) white else inactiveColor)
                tv.setTextColor(if (isActive) white else inactiveColor)
            }

            styleChip(chipAll, iconChipAll, tvChipAll, currentCategoryFilter == "REHETRA")
            styleChip(chipLehibe, iconChipLehibe, tvChipLehibe, currentCategoryFilter == "LEHIBE")
            styleChip(chipFanampiny, iconChipFanampiny, tvChipFanampiny, currentCategoryFilter == "FANAMPINY")
        }
        updateFilterChipsUI()

        chipAll.setOnClickListener {
            if (currentCategoryFilter != "REHETRA") {
                currentCategoryFilter = "REHETRA"
                updateFilterChipsUI()
                resetLimitAndExecuteSearch()
                rvResults?.scrollToPosition(0)
            }
        }
        chipLehibe.setOnClickListener {
            if (currentCategoryFilter != "LEHIBE") {
                currentCategoryFilter = "LEHIBE"
                updateFilterChipsUI()
                resetLimitAndExecuteSearch()
                rvResults?.scrollToPosition(0)
            }
        }
        chipFanampiny.setOnClickListener {
            if (currentCategoryFilter != "FANAMPINY") {
                currentCategoryFilter = "FANAMPINY"
                updateFilterChipsUI()
                resetLimitAndExecuteSearch()
                rvResults?.scrollToPosition(0)
            }
        }

        // Rule 3: Dual-Speed Search Engine (0ms for Number, 120ms debounce for Text)
        etSearch?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString().orEmpty().trim()
                currentSearchQuery = query
                btnClearSearch?.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
                textSearchDebounceJob?.cancel()

                // 1. Numeric query or empty query -> 0ms instant execution
                if (query.isEmpty() || query.all { it.isDigit() }) {
                    currentDisplayLimit = HymnCatalog.PAGE_SIZE
                    executeSearchNow()
                } else {
                    // 2. Keyword/phrase query -> 120ms debounce + async contextual snippet & highlight pre-computation
                    textSearchDebounceJob = viewLifecycleOwner.lifecycleScope.launch {
                        delay(120L)
                        currentDisplayLimit = HymnCatalog.KEYWORD_SEARCH_INITIAL_CAP
                        val songsToSearch = allSongsCache.ifEmpty { HymnCatalog.allSongsFlow.value }
                        val querySnapshot = currentSearchQuery
                        val categorySnapshot = currentCategoryFilter
                        val matched = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                            val results = HymnCatalog.searchSongs(
                                songs = songsToSearch,
                                rawQuery = querySnapshot,
                                categoryFilter = categorySnapshot
                            )
                            val warmCount = results.size.coerceAtMost(HymnCatalog.KEYWORD_SEARCH_INITIAL_CAP)
                            for (i in 0 until warmCount) {
                                val item = results[i]
                                com.example.ui.util.HighlightHelper.highlightText(item.title, querySnapshot)
                                com.example.ui.util.HighlightHelper.highlightText("“${item.snippet}”", querySnapshot)
                            }
                            results
                        }
                        if (querySnapshot == currentSearchQuery && categorySnapshot == currentCategoryFilter) {
                            currentMatchedSongs = matched
                            renderPaginatedSlice()
                        }
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        etSearch?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                textSearchDebounceJob?.cancel()
                executeSearchNow()
                true
            } else {
                false
            }
        }

        // Numpad Overlay Controls
        btnOpenNumpad.setOnClickListener {
            openNumpad()
        }
        btnCloseNumpad.setOnClickListener {
            closeNumpad()
        }
        numpadBackdrop.setOnClickListener {
            closeNumpad()
        }
        btnClearAllNumpad.setOnClickListener {
            currentNumpadValue = ""
            etSearch?.setText("")
        }

        setupNumpadKeys(view)

        // Apply any pending external query/numpad trigger
        pendingExternalQuery?.let { q ->
            etSearch?.setText(q)
            etSearch?.setSelection(q.length)
            pendingExternalQuery = null
        }
        if (pendingOpenNumpad) {
            openNumpad()
            pendingOpenNumpad = false
        } else {
            etSearch?.clearFocus()
        }

        // Observe pre-indexed in-memory 830 hymns
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    repository.allSongs.collectLatest { songs ->
                        allSongsCache = songs
                        executeSearchNow()
                    }
                }
                launch {
                    com.example.ui.theme.ThemeManager.isDarkMode.collectLatest { isDark ->
                        applyThemeSync(isDark)
                    }
                }
            }
        }
        applyThemeSync(com.example.ui.theme.ThemeManager.isDarkMode.value)
    }

    fun applyThemeSync(isDark: Boolean = com.example.ui.theme.ThemeManager.isDarkMode.value) {
        val root = view ?: return
        com.example.ui.theme.ThemeManager.applyThemeToViewTree(root, isDark)
        val activeBg = if (isDark) 0xFF800020.toInt() else 0xFF5A1827.toInt()
        val inactiveBg = if (isDark) 0xFF1E1214.toInt() else 0xFFFFFFFF.toInt()
        val white = 0xFFFFFFFF.toInt()
        val inactiveColor = if (isDark) 0xFFA69497.toInt() else 0xFF564243.toInt()

        fun styleChip(cardId: Int, iconId: Int, tvId: Int, isActive: Boolean) {
            root.findViewById<MaterialCardView>(cardId)?.let { card ->
                com.example.ui.theme.ThemeManager.stabilizeCardForApi23(
                    card,
                    if (isActive) activeBg else inactiveBg,
                    0,
                    0
                )
            }
            com.example.ui.theme.ThemeManager.setSolidIconTint(
                root.findViewById(iconId),
                if (isActive) white else inactiveColor
            )
            root.findViewById<TextView>(tvId)?.setTextColor(if (isActive) white else inactiveColor)
        }
        styleChip(R.id.chip_filter_all, R.id.icon_chip_all, R.id.tv_chip_all, currentCategoryFilter == "REHETRA")
        styleChip(R.id.chip_filter_lehibe, R.id.icon_chip_lehibe, R.id.tv_chip_lehibe, currentCategoryFilter == "LEHIBE")
        styleChip(R.id.chip_filter_fanampiny, R.id.icon_chip_fanampiny, R.id.tv_chip_fanampiny, currentCategoryFilter == "FANAMPINY")

        if (::songAdapter.isInitialized) {
            songAdapter.notifyDataSetChanged()
        }
    }

    private fun setupNumpadKeys(view: View) {
        val digitPairs: List<Pair<Int, String>> = listOf(
            Pair(R.id.numpad_key_0, "0"),
            Pair(R.id.numpad_key_1, "1"),
            Pair(R.id.numpad_key_2, "2"),
            Pair(R.id.numpad_key_3, "3"),
            Pair(R.id.numpad_key_4, "4"),
            Pair(R.id.numpad_key_5, "5"),
            Pair(R.id.numpad_key_6, "6"),
            Pair(R.id.numpad_key_7, "7"),
            Pair(R.id.numpad_key_8, "8"),
            Pair(R.id.numpad_key_9, "9")
        )

        for (pair in digitPairs) {
            val viewId = pair.first
            val digit = pair.second
            view.findViewById<View>(viewId)?.setOnClickListener {
                if (currentNumpadValue.length < 3) {
                    currentNumpadValue = if (currentNumpadValue == "0") digit else currentNumpadValue + digit
                    etSearch?.setText(currentNumpadValue)
                    etSearch?.setSelection(currentNumpadValue.length)
                }
            }
        }

        view.findViewById<View>(R.id.numpad_backspace)?.setOnClickListener {
            if (currentNumpadValue.isNotEmpty()) {
                currentNumpadValue = currentNumpadValue.dropLast(1)
                etSearch?.setText(currentNumpadValue)
                etSearch?.setSelection(currentNumpadValue.length)
            } else {
                val existing = etSearch?.text?.toString().orEmpty()
                if (existing.isNotEmpty()) {
                    val shortened = existing.dropLast(1)
                    etSearch?.setText(shortened)
                    etSearch?.setSelection(shortened.length)
                }
            }
        }

        view.findViewById<View>(R.id.numpad_submit)?.setOnClickListener {
            val numberQuery = currentNumpadValue.trim().ifEmpty {
                etSearch?.text?.toString()?.trim().orEmpty()
            }
            closeNumpad()
            if (numberQuery.isNotEmpty()) {
                val numberInt = numberQuery.toIntOrNull()
                if (numberInt != null && HymnCatalog.getSongByNumber(requireContext(), numberInt) != null) {
                    HymnCatalog.recordSongConsulted(requireContext(), numberInt)
                    (activity as? MainActivity)?.openHymnReader(numberInt)
                } else {
                    etSearch?.setText(numberQuery)
                    etSearch?.setSelection(numberQuery.length)
                }
            }
        }
    }

    fun openNumpad() {
        currentNumpadValue = etSearch?.text?.toString()?.filter { it.isDigit() }?.take(3).orEmpty()
        numpadOverlay?.visibility = View.VISIBLE
    }

    fun closeNumpad() {
        numpadOverlay?.visibility = View.GONE
    }

    fun closeNumpadOverlay() {
        closeNumpad()
    }

    fun isNumpadVisible(): Boolean {
        return numpadOverlay?.visibility == View.VISIBLE
    }

    fun applyExternalSearchQuery(query: String, openNumpadSheet: Boolean = false) {
        if (!isAdded || etSearch == null) {
            pendingExternalQuery = query
            pendingOpenNumpad = openNumpadSheet
            return
        }
        etSearch?.setText(query)
        etSearch?.setSelection(query.length)
        textSearchDebounceJob?.cancel()
        resetLimitAndExecuteSearch()
        if (openNumpadSheet) {
            openNumpad()
        } else {
            closeNumpad()
        }
    }

    fun applyExternalQuery(query: String, openNumpadSheet: Boolean = false) {
        applyExternalSearchQuery(query, openNumpadSheet)
    }

    private fun resetLimitAndExecuteSearch() {
        currentDisplayLimit = if (currentSearchQuery.isNotEmpty() && !currentSearchQuery.all { it.isDigit() }) {
            HymnCatalog.KEYWORD_SEARCH_INITIAL_CAP
        } else {
            HymnCatalog.PAGE_SIZE
        }
        executeSearchNow()
    }

    private fun executeSearchNow() {
        if (!isAdded) return
        val matched = HymnCatalog.searchSongs(
            songs = allSongsCache.ifEmpty { HymnCatalog.allSongsFlow.value },
            rawQuery = currentSearchQuery,
            categoryFilter = currentCategoryFilter
        )
        currentMatchedSongs = matched
        renderPaginatedSlice()
    }

    private fun renderPaginatedSlice() {
        val totalMatches = currentMatchedSongs.size
        val safeLimit = currentDisplayLimit.coerceAtMost(totalMatches)
        val visibleSlice = if (safeLimit > 0) {
            currentMatchedSongs.subList(0, safeLimit).toList()
        } else {
            emptyList()
        }

        songAdapter.currentQuery = currentSearchQuery
        songAdapter.submitList(visibleSlice)

        // Update result counter & empty state
        tvResultsCount?.text = "$totalMatches hira hita"
        val isEmpty = totalMatches == 0
        layoutEmptyState?.visibility = if (isEmpty) View.VISIBLE else View.GONE
        tvEmptyState?.visibility = if (isEmpty) View.VISIBLE else View.GONE
        rvResults?.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}
