package com.example.ui.fragments

import android.content.Context
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
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.HymnCatalog
import com.example.data.local.OnboardingPreferencesManager
import com.example.data.local.SongEntity
import com.example.data.local.UserProfileEntity
import com.example.data.repository.SongRepository
import com.example.ui.adapters.CategoryAdapter
import com.example.ui.adapters.SongAdapter
import com.example.ui.theme.ThemeManager
import com.google.android.material.card.MaterialCardView
import android.graphics.drawable.ColorDrawable
import android.graphics.BitmapFactory
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FandraisanaFragment : Fragment() {

    private lateinit var repository: SongRepository
    private lateinit var onboardingPrefs: OnboardingPreferencesManager
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var songAdapter: SongAdapter

    private var currentFeaturedSong: SongEntity? = null
    private var currentProfile: UserProfileEntity? = null
    private var historySongsCache: List<SongEntity> = emptyList()
    private var searchDebounceJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_fandraisana, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ctx = requireContext()
        HymnCatalog.ensureInitializedAsync(ctx)
        val dao = AppDatabase.getInstance(ctx).songDao()
        repository = SongRepository(dao, ctx.applicationContext)
        onboardingPrefs = OnboardingPreferencesManager(ctx)

        val tvWelcomeTitle = view.findViewById<TextView>(R.id.tv_welcome_title)
        val ivTopProfileAvatar = view.findViewById<ImageView>(R.id.iv_top_profile_avatar)
        val btnTopProfile = view.findViewById<MaterialCardView>(R.id.btn_top_profile)

        val tvHeroCollection = view.findViewById<TextView>(R.id.tv_hero_collection)
        val tvHeroTitle = view.findViewById<TextView>(R.id.tv_hero_song_title)
        val tvHeroExcerpt = view.findViewById<TextView>(R.id.tv_hero_song_excerpt)
        val btnHeroRead = view.findViewById<MaterialCardView>(R.id.btn_hero_read)
        val cardHeroSong = view.findViewById<MaterialCardView>(R.id.card_hero_song)

        val etSearchInput = view.findViewById<EditText>(R.id.et_search_input)
        val btnNumpadTrigger = view.findViewById<MaterialCardView>(R.id.btn_numpad)

        val cardBentoTrakta = view.findViewById<MaterialCardView>(R.id.card_bento_trakta)
        val cardBentoFotoana = view.findViewById<MaterialCardView>(R.id.card_bento_fotoana)

        val rvCategories = view.findViewById<RecyclerView>(R.id.rv_categories)
        val rvSongs = view.findViewById<RecyclerView>(R.id.rv_songs)
        val cardHistoryEmptyState = view.findViewById<MaterialCardView>(R.id.card_history_empty_state)
        val btnSeeAllSongs = view.findViewById<MaterialCardView>(R.id.btn_see_all_songs)

        val btnTopThemeToggle = view.findViewById<MaterialCardView>(R.id.btn_top_theme_toggle)
        val ivTopThemeIcon = view.findViewById<ImageView>(R.id.iv_top_theme_icon)
        val floatingTopBar = view.findViewById<MaterialCardView>(R.id.floating_top_bar)
        val fandraisanaScrollView = view.findViewById<androidx.core.widget.NestedScrollView>(R.id.fandraisana_scroll_view)
        val tvAppTitle = view.findViewById<TextView>(R.id.tv_app_title)
        val tvAppSubtitle = view.findViewById<TextView>(R.id.tv_app_subtitle)

        val cardSearchBar = view.findViewById<MaterialCardView>(R.id.card_search_bar)
        val tvBentoTraktaTitle = view.findViewById<TextView>(R.id.tv_bento_trakta_title)
        val tvBentoTraktaSub = view.findViewById<TextView>(R.id.tv_bento_trakta_sub)
        val tvBentoFotoanaTitle = view.findViewById<TextView>(R.id.tv_bento_fotoana_title)
        val tvBentoFotoanaSub = view.findViewById<TextView>(R.id.tv_bento_fotoana_sub)

        btnTopThemeToggle?.setOnClickListener {
            (activity as? MainActivity)?.toggleThemeMode()
        }

        cardHistoryEmptyState?.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(MainActivity.TAB_HIKAROKA)
        }

        // Setup Categories Grid (2 columns)
        categoryAdapter = CategoryAdapter { _ ->
            (activity as? MainActivity)?.openHikarokaWithSearch(
                query = "",
                openNumpad = false
            )
        }
        rvCategories.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = categoryAdapter
            isNestedScrollingEnabled = false
            itemAnimator = null
        }

        // Rule 4 & 5: Setup "Hira vao novakiana" (FIFO 10 consulted songs) + Left Swipe-to-Delete
        songAdapter = SongAdapter(
            onSongClick = { song ->
                HymnCatalog.recordSongConsulted(requireContext(), song.number)
                (activity as? MainActivity)?.openHymnReader(song.number)
            },
            onFavoriteToggle = { song ->
                HymnCatalog.toggleFavorite(requireContext(), song.number)
            }
        )
        rvSongs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = songAdapter
            isNestedScrollingEnabled = false
            itemAnimator = null
        }

        com.example.ui.util.SwipeToDeleteHelper.attachLeftSwipeToDelete(
            context = requireContext(),
            recyclerView = rvSongs,
            canSwipePosition = {
                etSearchInput.text?.toString()?.trim().isNullOrEmpty()
            },
            onSwipedAtPosition = { position ->
                val song = songAdapter.currentList.getOrNull(position) ?: return@attachLeftSwipeToDelete
                HymnCatalog.removeSongFromHistory(requireContext(), song.number)
            }
        )

        // Immediately bind the random "HIRA ANIO" song selected at launch (0ms)
        HymnCatalog.featuredSongFlow.value?.let { initialHero ->
            currentFeaturedSong = initialHero
            tvHeroCollection.text = initialHero.category
            tvHeroTitle.text = "${initialHero.numberStr}. ${initialHero.title}"
            tvHeroExcerpt.text = "“${initialHero.snippet}”"
        }

        // Top Profile Avatar -> Open Kaonty tab
        btnTopProfile.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(MainActivity.TAB_KAONTY)
        }

        // Hero Card Click & Read Button -> Record in History & Open HymnReader
        val openHeroSong = View.OnClickListener {
            val hero = currentFeaturedSong
                ?: HymnCatalog.featuredSongFlow.value
                ?: HymnCatalog.allSongsFlow.value.randomOrNull()
            if (hero != null) {
                HymnCatalog.recordSongConsulted(requireContext(), hero.number)
                (activity as? MainActivity)?.openHymnReader(hero.number)
            }
        }
        btnHeroRead.setOnClickListener(openHeroSong)
        cardHeroSong.setOnClickListener(openHeroSong)

        // Search Input Dual-Speed Filtering on Dashboard or Jump to Hikaroka on Enter
        etSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim().orEmpty()
                searchDebounceJob?.cancel()
                if (query.isEmpty()) {
                    val isEmpty = historySongsCache.isEmpty()
                    songAdapter.submitList(historySongsCache)
                    cardHistoryEmptyState?.visibility = if (isEmpty) View.VISIBLE else View.GONE
                    rvSongs.visibility = if (isEmpty) View.GONE else View.VISIBLE
                } else if (query.all { it.isDigit() }) {
                    // Rule 3: 0ms instant number search
                    val matches = HymnCatalog.searchSongs(
                        context = requireContext(),
                        rawQuery = query,
                        categoryFilter = "REHETRA"
                    ).take(10)
                    songAdapter.submitList(matches)
                    cardHistoryEmptyState?.visibility = View.GONE
                    rvSongs.visibility = View.VISIBLE
                } else {
                    // Rule 3: 120ms debounce for text search
                    searchDebounceJob = viewLifecycleOwner.lifecycleScope.launch {
                        delay(120L)
                        val matches = HymnCatalog.searchSongs(
                            context = requireContext(),
                            rawQuery = query,
                            categoryFilter = "REHETRA"
                        ).take(10)
                        songAdapter.submitList(matches)
                        cardHistoryEmptyState?.visibility = View.GONE
                        rvSongs.visibility = View.VISIBLE
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        etSearchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val q = etSearchInput.text?.toString()?.trim().orEmpty()
                (activity as? MainActivity)?.openHikarokaWithSearch(
                    query = q,
                    openNumpad = false
                )
                true
            } else {
                false
            }
        }

        // Numpad Trigger Button -> Open Hikaroka with Numpad Sheet
        btnNumpadTrigger.setOnClickListener {
            (activity as? MainActivity)?.openHikarokaWithSearch(
                query = "",
                openNumpad = true
            )
        }

        // Bento Hub Cards Navigation
        cardBentoTrakta.setOnClickListener {
            (activity as? MainActivity)?.openTrakta()
        }
        cardBentoFotoana.setOnClickListener {
            (activity as? MainActivity)?.openFandaharampotoana()
        }

        // "Hijery rehetra" Button -> Navigate to Hikaroka tab
        btnSeeAllSongs.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(MainActivity.TAB_HIKAROKA)
        }

        applyOnboardingWelcomeFromPrefs(tvWelcomeTitle, ivTopProfileAvatar)
        etSearchInput.clearFocus()

        // Observe Flows
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    onboardingPrefs.onboardingDataFlow.collectLatest { userData ->
                        val savedName = userData.name.ifBlank {
                            currentProfile?.displayName ?: "Frère Aina"
                        }
                        val savedGender = userData.gender.ifBlank {
                            currentProfile?.gender ?: "Lehilahy"
                        }
                        val savedPhotoUri = userData.imageUri ?: currentProfile?.photoUri.orEmpty()

                        val cleanName = savedName
                            .removePrefix("Rahalahy ")
                            .removePrefix("Ranabavy ")
                            .removePrefix("Anabavy ")
                            .removePrefix("Frère ")
                            .removePrefix("Sœur ")
                            .trim()

                        val prefix = if (savedGender.equals("Vehivavy", ignoreCase = true) ||
                            savedGender.equals("female", ignoreCase = true)
                        ) {
                            "Soeur"
                        } else {
                            "Frère"
                        }

                        tvWelcomeTitle.text = if (cleanName.isNotEmpty()) {
                            "Salama, $prefix $cleanName"
                        } else {
                            "Salama, Tongasoa"
                        }

                        bindTopAvatar(ivTopProfileAvatar, savedPhotoUri)
                    }
                }

                launch {
                    repository.userProfile.collectLatest { profile ->
                        currentProfile = profile
                    }
                }

                launch {
                    repository.featuredSong.collectLatest { hero ->
                        if (hero != null) {
                            currentFeaturedSong = hero
                            tvHeroCollection.text = hero.category
                            tvHeroTitle.text = "${hero.numberStr}. ${hero.title}"
                            tvHeroExcerpt.text = "“${hero.snippet}”"
                        }
                    }
                }

                launch {
                    repository.allCategories.collectLatest { categories ->
                        categoryAdapter.submitList(categories)
                    }
                }

                // Rule 5: Observe FIFO-10 consultation history without duplicates
                launch {
                    repository.historySongs.collectLatest { historySongs ->
                        historySongsCache = historySongs
                        val currentQuery = etSearchInput.text?.toString()?.trim().orEmpty()
                        if (currentQuery.isEmpty()) {
                            val isEmpty = historySongs.isEmpty()
                            songAdapter.submitList(historySongs)
                            cardHistoryEmptyState?.visibility = if (isEmpty) View.VISIBLE else View.GONE
                            rvSongs.visibility = if (isEmpty) View.GONE else View.VISIBLE
                        }
                    }
                }

                // 0ms Instant Dark Mode Observer (Single global RAM variable, zero delay)
                launch {
                    ThemeManager.isDarkMode.collectLatest { isDark ->
                        applyThemeSync(isDark)
                    }
                }
            }
        }
        applyThemeSync(ThemeManager.isDarkMode.value)
    }

    fun applyThemeSync(isDark: Boolean = ThemeManager.isDarkMode.value) {
        val root = view ?: return
        ThemeManager.applyThemeToViewTree(root, isDark)

        val ivTopThemeIcon = root.findViewById<ImageView>(R.id.iv_top_theme_icon)
        val ivTopProfileAvatar = root.findViewById<ImageView>(R.id.iv_top_profile_avatar)
        val fandraisanaScrollView = root.findViewById<View>(R.id.fandraisana_scroll_view)

        fandraisanaScrollView?.background = ThemeManager.createScreenGradient(isDark)

        ivTopThemeIcon?.setImageResource(
            if (isDark) R.drawable.ic_light_mode else R.drawable.ic_dark_mode
        )
        ThemeManager.setSolidIconTint(
            ivTopThemeIcon,
            if (isDark) 0xFFD4AF37.toInt() else 0xFF781C2E.toInt()
        )

        if (ivTopProfileAvatar != null) {
            ivTopProfileAvatar.imageTintList = null
            ivTopProfileAvatar.clearColorFilter()
        }

        val btnHeroRead = root.findViewById<MaterialCardView>(R.id.btn_hero_read)
        val tvBtnHeroRead = root.findViewById<TextView>(R.id.tv_btn_hero_read)
        val ivBtnHeroRead = root.findViewById<ImageView>(R.id.iv_btn_hero_read)

        val dp1 = (1f * resources.displayMetrics.density).toInt().coerceAtLeast(1)
        val heroBtnBg = if (isDark) 0xFF28181B.toInt() else 0xFFFFFFFF.toInt()
        val heroBtnStroke = if (isDark) 0xFF382528.toInt() else 0xFFEAE0E2.toInt()
        val heroBtnTextColor = if (isDark) 0xFFF5E6E8.toInt() else 0xFF1C1C1E.toInt()

        btnHeroRead?.let { card ->
            card.strokeColor = heroBtnStroke
            card.strokeWidth = dp1
            ThemeManager.stabilizeCardForApi23(card, heroBtnBg, heroBtnStroke, dp1)
        }
        tvBtnHeroRead?.setTextColor(heroBtnTextColor)
        ThemeManager.setSolidIconTint(ivBtnHeroRead, heroBtnTextColor)

        if (::songAdapter.isInitialized) {
            songAdapter.notifyDataSetChanged()
        }
        if (::categoryAdapter.isInitialized) {
            categoryAdapter.notifyDataSetChanged()
        }
    }

    private fun applyOnboardingWelcomeFromPrefs(
        tvWelcomeTitle: TextView,
        ivTopProfileAvatar: ImageView
    ) {
        val prefs = requireContext().getSharedPreferences(
            "jesosy_mamonjy_onboarding_prefs",
            Context.MODE_PRIVATE
        )
        val savedName = prefs.getString("user_name", "")?.trim().orEmpty()
        val savedGender = prefs.getString("user_gender", "Lehilahy").orEmpty()
        val savedPhotoUri = prefs.getString("profile_image_uri", "").orEmpty()

        if (savedName.isNotEmpty()) {
            val cleanName = savedName
                .removePrefix("Rahalahy ")
                .removePrefix("Ranabavy ")
                .removePrefix("Anabavy ")
                .removePrefix("Frère ")
                .removePrefix("Sœur ")
                .trim()
            val prefix = if (savedGender.equals("Vehivavy", ignoreCase = true) ||
                savedGender.equals("female", ignoreCase = true)
            ) {
                "Soeur"
            } else {
                "Frère"
            }
            tvWelcomeTitle.text = "Salama, $prefix $cleanName"
        }

        bindTopAvatar(ivTopProfileAvatar, savedPhotoUri)
    }

    private fun bindTopAvatar(imageView: ImageView, photoUriString: String) {
        if (photoUriString.isNotBlank()) {
            runCatching {
                val uri = Uri.parse(photoUriString)
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    val circularDrawable = RoundedBitmapDrawableFactory.create(resources, bitmap).apply {
                        isCircular = true
                        setAntiAlias(true)
                    }
                    imageView.setImageDrawable(circularDrawable)
                    imageView.imageTintList = null
                    imageView.clearColorFilter()
                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                    imageView.setPadding(0, 0, 0, 0)
                    return
                }
            }.onFailure {
                resetDefaultTopAvatar(imageView)
                return
            }
        }
        resetDefaultTopAvatar(imageView)
    }

    private fun resetDefaultTopAvatar(imageView: ImageView) {
        imageView.imageTintList = null
        imageView.clearColorFilter()
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        val pad = (2 * resources.displayMetrics.density).toInt()
        imageView.setPadding(pad, pad, pad, pad)
        com.example.ui.util.AssetImageLoader.loadInto(
            imageView = imageView,
            assetFileName = "logo_transparent.png",
            reqWidthDp = 64,
            reqHeightDp = 64,
            fallbackResId = R.drawable.ic_logo_jesosy_mamonjy
        )
    }
}
