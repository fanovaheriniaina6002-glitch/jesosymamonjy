package com.example

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import android.content.res.Configuration
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.lifecycleScope
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemeManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.data.local.AppDatabase
import com.example.data.local.OnboardingPreferencesManager
import com.example.data.local.SongEntity
import com.example.data.local.TraktaEntity
import com.example.data.repository.SongRepository
import com.example.ui.fragments.FandaharampotoanaFragment
import com.example.ui.fragments.FandraisanaFragment
import com.example.ui.fragments.FifandraisanaFragment
import com.example.ui.fragments.HikarokaFragment
import com.example.ui.fragments.HiraTiakoFragment
import com.example.ui.fragments.HymnReaderFragment
import com.example.ui.fragments.KaontyFragment
import com.example.ui.fragments.TraktaFragment
import com.example.ui.fragments.TraktaReaderFragment
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.splash.SplashScreen
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    private enum class StartupFlowStage {
        SPLASH,
        ONBOARDING,
        MAIN_APP
    }

    private lateinit var repository: SongRepository
    private lateinit var onboardingPreferencesManager: OnboardingPreferencesManager

    private lateinit var fandraisanaFragment: FandraisanaFragment
    private lateinit var hikarokaFragment: HikarokaFragment
    private lateinit var hiraTiakoFragment: HiraTiakoFragment
    private lateinit var kaontyFragment: KaontyFragment
    private lateinit var hymnReaderFragment: HymnReaderFragment
    private lateinit var fandaharampotoanaFragment: FandaharampotoanaFragment
    private lateinit var traktaFragment: TraktaFragment
    private lateinit var traktaReaderFragment: TraktaReaderFragment
    private lateinit var fifandraisanaFragment: FifandraisanaFragment

    private var activeFragment: Fragment? = null
    private var currentTabIndex: Int = TAB_FANDRAISANA
    private var isReaderOpen: Boolean = false
    private var isFandaharampotoanaOpen: Boolean = false
    private var isTraktaOpen: Boolean = false
    private var isTraktaReaderOpen: Boolean = false
    private var isFifandraisanaOpen: Boolean = false
    private var startupStage by mutableStateOf(StartupFlowStage.SPLASH)

    private val isStartupOverlayVisible: Boolean
        get() = startupStage != StartupFlowStage.MAIN_APP

    // Startup (Splash + One-Time Onboarding) ComposeView Host
    private lateinit var onboardingComposeView: ComposeView

    // Custom Floating Opaque Pill Bottom Nav Views
    private lateinit var floatingBottomNavPill: MaterialCardView
    private lateinit var navTabFandraisana: MaterialCardView
    private lateinit var navTabHikaroka: MaterialCardView
    private lateinit var navTabHiraTiako: MaterialCardView
    private lateinit var navTabKaonty: MaterialCardView

    private lateinit var navInnerFandraisana: LinearLayout
    private lateinit var navInnerHikaroka: LinearLayout
    private lateinit var navInnerHiraTiako: LinearLayout
    private lateinit var navInnerKaonty: LinearLayout

    private lateinit var navIconFandraisana: ImageView
    private lateinit var navIconHikaroka: ImageView
    private lateinit var navIconHiraTiako: ImageView
    private lateinit var navIconKaonty: ImageView

    private lateinit var navLabelFandraisana: TextView
    private lateinit var navLabelHikaroka: TextView
    private lateinit var navLabelHiraTiako: TextView
    private lateinit var navLabelKaonty: TextView

    private val immediateThemeListener: (Boolean, String) -> Unit = { isDark, mode ->
        applyGlobalThemeInstant(isDark, mode)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val db = AppDatabase.getInstance(this)
        repository = SongRepository(db.songDao(), applicationContext)
        onboardingPreferencesManager = OnboardingPreferencesManager(this)

        ThemeManager.init(this, lifecycleScope)

        bindBottomNavViews()
        setupFragments(savedInstanceState)
        setupBottomNavigationClicks()
        setupStartupSplashAndOnboarding(db, savedInstanceState)
        setupBackNavigation()

        ThemeManager.registerImmediateListener(immediateThemeListener)
        applyGlobalThemeInstant(ThemeManager.isDarkMode.value, ThemeManager.themeMode.value)

        lifecycleScope.launch {
            ThemeManager.isDarkMode.collectLatest { isDark ->
                applyGlobalThemeInstant(isDark, ThemeManager.themeMode.value)
            }
        }
    }

    override fun onDestroy() {
        ThemeManager.unregisterImmediateListener(immediateThemeListener)
        super.onDestroy()
    }

    private fun applyGlobalThemeInstant(isDark: Boolean, mode: String) {
        val mainRoot = findViewById<View>(R.id.main_root_container)
        mainRoot?.background = ThemeManager.createScreenGradient(isDark)
        window.decorView.setBackgroundColor(if (isDark) 0xFF120A0B.toInt() else 0xFFFBF8F9.toInt())

        if (::fandraisanaFragment.isInitialized) fandraisanaFragment.applyThemeSync(isDark)
        if (::hikarokaFragment.isInitialized) hikarokaFragment.applyThemeSync(isDark)
        if (::hiraTiakoFragment.isInitialized) hiraTiakoFragment.applyThemeSync(isDark)
        if (::kaontyFragment.isInitialized) kaontyFragment.applyThemeSync(isDark, mode)
        if (::hymnReaderFragment.isInitialized) hymnReaderFragment.applyThemeSync(isDark)
        if (::fandaharampotoanaFragment.isInitialized) fandaharampotoanaFragment.applyThemeSync(isDark)
        if (::traktaFragment.isInitialized) traktaFragment.applyThemeSync(isDark)
        if (::traktaReaderFragment.isInitialized) traktaReaderFragment.applyThemeSync(isDark)
        if (::fifandraisanaFragment.isInitialized) fifandraisanaFragment.applyThemeSync(isDark)

        updateBottomNavPillVisuals(currentTabIndex)
    }

    private fun bindBottomNavViews() {
        onboardingComposeView = findViewById(R.id.onboarding_compose_view)
        floatingBottomNavPill = findViewById(R.id.floating_bottom_nav_pill)
        navTabFandraisana = findViewById(R.id.nav_tab_fandraisana)
        navTabHikaroka = findViewById(R.id.nav_tab_hikaroka)
        navTabHiraTiako = findViewById(R.id.nav_tab_hira_tiako)
        navTabKaonty = findViewById(R.id.nav_tab_kaonty)

        navInnerFandraisana = findViewById(R.id.nav_inner_fandraisana)
        navInnerHikaroka = findViewById(R.id.nav_inner_hikaroka)
        navInnerHiraTiako = findViewById(R.id.nav_inner_hira_tiako)
        navInnerKaonty = findViewById(R.id.nav_inner_kaonty)

        navIconFandraisana = findViewById(R.id.nav_icon_fandraisana)
        navIconHikaroka = findViewById(R.id.nav_icon_hikaroka)
        navIconHiraTiako = findViewById(R.id.nav_icon_hira_tiako)
        navIconKaonty = findViewById(R.id.nav_icon_kaonty)

        navLabelFandraisana = findViewById(R.id.nav_label_fandraisana)
        navLabelHikaroka = findViewById(R.id.nav_label_hikaroka)
        navLabelHiraTiako = findViewById(R.id.nav_label_hira_tiako)
        navLabelKaonty = findViewById(R.id.nav_label_kaonty)
    }

    private fun setupStartupSplashAndOnboarding(db: AppDatabase, savedInstanceState: Bundle?) {
        val alreadyOnboarded = onboardingPreferencesManager.isOnboardingCompletedSync()

        startupStage = when {
            savedInstanceState == null -> StartupFlowStage.SPLASH
            !alreadyOnboarded -> StartupFlowStage.ONBOARDING
            else -> StartupFlowStage.MAIN_APP
        }

        if (startupStage == StartupFlowStage.MAIN_APP) {
            onboardingComposeView.visibility = View.GONE
            return
        }

        floatingBottomNavPill.visibility = View.GONE
        onboardingComposeView.visibility = View.VISIBLE
        onboardingComposeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        onboardingComposeView.setContent {
            val isDark by ThemeManager.isDarkMode.collectAsState()
            val mode by ThemeManager.themeMode.collectAsState()
            MyApplicationTheme(darkTheme = isDark, themeMode = mode) {
                when (startupStage) {
                    StartupFlowStage.SPLASH -> {
                        SplashScreen(
                            onNavigateNext = {
                                completeStartupAndNavigateHome()
                            }
                        )
                    }
                    StartupFlowStage.ONBOARDING -> {
                        OnboardingScreen(
                            preferencesManager = onboardingPreferencesManager,
                            songDao = db.songDao(),
                            onOnboardingCompleted = {
                                completeStartupAndNavigateHome()
                            }
                        )
                    }
                    StartupFlowStage.MAIN_APP -> Unit
                }
            }
        }
    }

    private fun completeStartupAndNavigateHome() {
        startupStage = StartupFlowStage.MAIN_APP
        onboardingComposeView.visibility = View.GONE
        switchToTab(TAB_FANDRAISANA)
    }

    private fun setupFragments(savedInstanceState: Bundle?) {
        val fm = supportFragmentManager

        if (savedInstanceState == null) {
            fandraisanaFragment = FandraisanaFragment()
            hikarokaFragment = HikarokaFragment()
            hiraTiakoFragment = HiraTiakoFragment()
            kaontyFragment = KaontyFragment()
            hymnReaderFragment = HymnReaderFragment()
            fandaharampotoanaFragment = FandaharampotoanaFragment()
            traktaFragment = TraktaFragment()
            traktaReaderFragment = TraktaReaderFragment()
            fifandraisanaFragment = FifandraisanaFragment()

            fm.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.fragment_container, fifandraisanaFragment, TAG_FIFANDRAISANA)
                .hide(fifandraisanaFragment)
                .add(R.id.fragment_container, traktaReaderFragment, TAG_TRAKTA_READER)
                .hide(traktaReaderFragment)
                .add(R.id.fragment_container, traktaFragment, TAG_TRAKTA).hide(traktaFragment)
                .add(R.id.fragment_container, fandaharampotoanaFragment, TAG_FANDAHARAMPOTOANA)
                .hide(fandaharampotoanaFragment)
                .add(R.id.fragment_container, hymnReaderFragment, TAG_HYMN_READER).hide(hymnReaderFragment)
                .add(R.id.fragment_container, kaontyFragment, TAG_KAONTY).hide(kaontyFragment)
                .add(R.id.fragment_container, hiraTiakoFragment, TAG_HIRA_TIAKO).hide(hiraTiakoFragment)
                .add(R.id.fragment_container, hikarokaFragment, TAG_HIKAROKA).hide(hikarokaFragment)
                .add(R.id.fragment_container, fandraisanaFragment, TAG_FANDRAISANA)
                .commitNow()

            activeFragment = fandraisanaFragment
            currentTabIndex = TAB_FANDRAISANA
            isReaderOpen = false
            isFandaharampotoanaOpen = false
            isTraktaOpen = false
            isTraktaReaderOpen = false
            isFifandraisanaOpen = false
            updateBottomNavPillVisuals(TAB_FANDRAISANA)
        } else {
            fandraisanaFragment =
                (fm.findFragmentByTag(TAG_FANDRAISANA) as? FandraisanaFragment) ?: FandraisanaFragment()
            hikarokaFragment =
                (fm.findFragmentByTag(TAG_HIKAROKA) as? HikarokaFragment) ?: HikarokaFragment()
            hiraTiakoFragment =
                (fm.findFragmentByTag(TAG_HIRA_TIAKO) as? HiraTiakoFragment) ?: HiraTiakoFragment()
            kaontyFragment =
                (fm.findFragmentByTag(TAG_KAONTY) as? KaontyFragment) ?: KaontyFragment()
            hymnReaderFragment =
                (fm.findFragmentByTag(TAG_HYMN_READER) as? HymnReaderFragment) ?: HymnReaderFragment()
            fandaharampotoanaFragment =
                (fm.findFragmentByTag(TAG_FANDAHARAMPOTOANA) as? FandaharampotoanaFragment)
                    ?: FandaharampotoanaFragment()
            traktaFragment =
                (fm.findFragmentByTag(TAG_TRAKTA) as? TraktaFragment) ?: TraktaFragment()
            traktaReaderFragment =
                (fm.findFragmentByTag(TAG_TRAKTA_READER) as? TraktaReaderFragment)
                    ?: TraktaReaderFragment()
            fifandraisanaFragment =
                (fm.findFragmentByTag(TAG_FIFANDRAISANA) as? FifandraisanaFragment)
                    ?: FifandraisanaFragment()

            if (!hymnReaderFragment.isHidden) {
                isReaderOpen = true
                isFandaharampotoanaOpen = false
                isTraktaOpen = false
                isTraktaReaderOpen = false
                isFifandraisanaOpen = false
                floatingBottomNavPill.visibility = View.GONE
                activeFragment = fandraisanaFragment
            } else if (!traktaReaderFragment.isHidden) {
                isReaderOpen = false
                isFandaharampotoanaOpen = false
                isTraktaOpen = false
                isTraktaReaderOpen = true
                isFifandraisanaOpen = false
                floatingBottomNavPill.visibility = View.GONE
                activeFragment = fandraisanaFragment
            } else if (!fifandraisanaFragment.isHidden) {
                isReaderOpen = false
                isFandaharampotoanaOpen = false
                isTraktaOpen = false
                isTraktaReaderOpen = false
                isFifandraisanaOpen = true
                floatingBottomNavPill.visibility = View.GONE
                activeFragment = kaontyFragment
            } else if (!fandaharampotoanaFragment.isHidden) {
                isReaderOpen = false
                isFandaharampotoanaOpen = true
                isTraktaOpen = false
                isTraktaReaderOpen = false
                isFifandraisanaOpen = false
                floatingBottomNavPill.visibility = View.VISIBLE
                activeFragment = fandraisanaFragment
            } else if (!traktaFragment.isHidden) {
                isReaderOpen = false
                isFandaharampotoanaOpen = false
                isTraktaOpen = true
                isTraktaReaderOpen = false
                isFifandraisanaOpen = false
                floatingBottomNavPill.visibility = View.VISIBLE
                activeFragment = fandraisanaFragment
            } else {
                isReaderOpen = false
                isFandaharampotoanaOpen = false
                isTraktaOpen = false
                isTraktaReaderOpen = false
                isFifandraisanaOpen = false
                floatingBottomNavPill.visibility = View.VISIBLE
                activeFragment = listOf(
                    fandraisanaFragment,
                    hikarokaFragment,
                    hiraTiakoFragment,
                    kaontyFragment
                ).firstOrNull { !it.isHidden } ?: fandraisanaFragment
            }

            currentTabIndex = when (activeFragment) {
                hikarokaFragment -> TAB_HIKAROKA
                hiraTiakoFragment -> TAB_HIRA_TIAKO
                kaontyFragment -> TAB_KAONTY
                else -> TAB_FANDRAISANA
            }
            updateBottomNavPillVisuals(currentTabIndex)
        }
    }

    private fun setupBottomNavigationClicks() {
        navTabFandraisana.setOnClickListener { switchToTab(TAB_FANDRAISANA) }
        navTabHikaroka.setOnClickListener { switchToTab(TAB_HIKAROKA) }
        navTabHiraTiako.setOnClickListener { switchToTab(TAB_HIRA_TIAKO) }
        navTabKaonty.setOnClickListener { switchToTab(TAB_KAONTY) }
    }

    private fun updateBottomNavPillVisuals(selectedIndex: Int) {
        val isDark = ThemeManager.isDarkMode.value
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }

        val dp1 = (1f * resources.displayMetrics.density).toInt().coerceAtLeast(1)
        val pillBg = if (isDark) 0xFF1E1214.toInt() else 0xFF781C2E.toInt()
        val pillStroke = if (isDark) 0xFF382528.toInt() else 0xFF8F253B.toInt()
        floatingBottomNavPill.strokeColor = pillStroke
        floatingBottomNavPill.strokeWidth = dp1
        ThemeManager.stabilizeCardForApi23(floatingBottomNavPill, pillBg, pillStroke, dp1)

        val selectedTabBg = if (isDark) 0xFF28181B.toInt() else 0xFFFFFFFF.toInt()
        val inactiveTabBg = pillBg // 100% opaque parent capsule color (zero alpha)
        val selectedTint = if (isDark) 0xFFD4AF37.toInt() else 0xFF781C2E.toInt()
        val inactiveTint = if (isDark) 0xFFA69497.toInt() else 0xFFF5E6E8.toInt()
        val dp6 = (6 * resources.displayMetrics.density).toInt()

        data class TabHolder(
            val index: Int,
            val card: MaterialCardView,
            val inner: LinearLayout,
            val icon: ImageView,
            val label: TextView
        )

        val tabs = listOf(
            TabHolder(TAB_FANDRAISANA, navTabFandraisana, navInnerFandraisana, navIconFandraisana, navLabelFandraisana),
            TabHolder(TAB_HIKAROKA, navTabHikaroka, navInnerHikaroka, navIconHikaroka, navLabelHikaroka),
            TabHolder(TAB_HIRA_TIAKO, navTabHiraTiako, navInnerHiraTiako, navIconHiraTiako, navLabelHiraTiako),
            TabHolder(TAB_KAONTY, navTabKaonty, navInnerKaonty, navIconKaonty, navLabelKaonty)
        )

        tabs.forEach { tab ->
            val isSelected = tab.index == selectedIndex
            val lp = tab.card.layoutParams as LinearLayout.LayoutParams
            lp.weight = if (isSelected) 1.35f else 1.0f
            tab.card.layoutParams = lp

            val tabBg = if (isSelected) selectedTabBg else inactiveTabBg
            ThemeManager.stabilizeCardForApi23(tab.card, tabBg, pillStroke, 0)
            tab.inner.orientation = if (isSelected) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL

            val labelParams = tab.label.layoutParams as LinearLayout.LayoutParams
            labelParams.marginStart = if (isSelected) dp6 else 0
            tab.label.layoutParams = labelParams

            val targetTint = if (isSelected) selectedTint else inactiveTint
            ThemeManager.setSolidIconTint(tab.icon, targetTint)
            tab.label.setTextColor(targetTint)
            tab.label.setTextSize(TypedValue.COMPLEX_UNIT_SP, if (isSelected) 12f else 10f)
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isStartupOverlayVisible) {
                    return
                } else if (isReaderOpen) {
                    closeHymnReader()
                } else if (isTraktaReaderOpen) {
                    closeTraktaReaderScreen()
                } else if (isFifandraisanaOpen) {
                    closeFifandraisana()
                } else if (isFandaharampotoanaOpen) {
                    closeFandaharampotoana()
                } else if (isTraktaOpen) {
                    if (traktaFragment.isTraktaReaderVisible()) {
                        traktaFragment.closeTraktaReader()
                    } else {
                        closeTrakta()
                    }
                } else if (currentTabIndex == TAB_HIKAROKA && hikarokaFragment.isNumpadVisible()) {
                    hikarokaFragment.closeNumpad()
                } else if (currentTabIndex == TAB_KAONTY && kaontyFragment.isProfileModalVisible()) {
                    kaontyFragment.closeProfileModal()
                } else if (currentTabIndex != TAB_FANDRAISANA) {
                    switchToTab(TAB_FANDRAISANA)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    fun switchToTab(
        tabIndex: Int,
        initialQuery: String? = null,
        openNumpadSheet: Boolean = false
    ) {
        val targetFragment: Fragment = when (tabIndex) {
            TAB_FANDRAISANA -> fandraisanaFragment
            TAB_HIKAROKA -> hikarokaFragment
            TAB_HIRA_TIAKO -> hiraTiakoFragment
            TAB_KAONTY -> kaontyFragment
            else -> fandraisanaFragment
        }

        val transaction = supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)

        if (isReaderOpen) {
            transaction.hide(hymnReaderFragment)
            isReaderOpen = false
        }
        if (isTraktaReaderOpen) {
            transaction.hide(traktaReaderFragment)
            isTraktaReaderOpen = false
        }
        if (isFifandraisanaOpen) {
            transaction.hide(fifandraisanaFragment)
            isFifandraisanaOpen = false
        }
        if (isFandaharampotoanaOpen) {
            transaction.hide(fandaharampotoanaFragment)
            isFandaharampotoanaOpen = false
        }
        if (isTraktaOpen) {
            traktaFragment.closeTraktaReader()
            transaction.hide(traktaFragment)
            isTraktaOpen = false
        }
        if (!isStartupOverlayVisible) {
            floatingBottomNavPill.visibility = View.VISIBLE
        }

        if (activeFragment !== targetFragment) {
            activeFragment?.let { transaction.hide(it) }
            transaction.show(targetFragment)
            activeFragment = targetFragment
        } else {
            transaction.show(targetFragment)
        }
        transaction.commitNowAllowingStateLoss()

        currentTabIndex = tabIndex
        updateBottomNavPillVisuals(tabIndex)

        if (tabIndex == TAB_HIKAROKA) {
            if (initialQuery != null) {
                hikarokaFragment.applyExternalSearchQuery(initialQuery, openNumpadSheet)
            } else if (openNumpadSheet) {
                hikarokaFragment.openNumpad()
            }
        }
    }

    fun navigateToTab(tabIndex: Int) {
        switchToTab(tabIndex)
    }

    fun openHikarokaWithSearch(query: String, openNumpad: Boolean = false) {
        switchToTab(
            tabIndex = TAB_HIKAROKA,
            initialQuery = query,
            openNumpadSheet = openNumpad
        )
    }

    /**
     * Opens the full-screen HymnReaderFragment ("Famakiana Hira") for the selected song number
     * with 0-lag show/hide fragment transaction and intelligent query highlighting.
     */
    fun openHymnReader(songNumber: Int, searchQuery: String? = null) {
        hymnReaderFragment.loadHymn(songNumber, searchQuery)

        val transaction = supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
        if (isTraktaReaderOpen) {
            transaction.hide(traktaReaderFragment)
            isTraktaReaderOpen = false
        }
        if (isFifandraisanaOpen) {
            transaction.hide(fifandraisanaFragment)
            isFifandraisanaOpen = false
        }
        if (isFandaharampotoanaOpen) {
            transaction.hide(fandaharampotoanaFragment)
            isFandaharampotoanaOpen = false
        }
        if (isTraktaOpen) {
            traktaFragment.closeTraktaReader()
            transaction.hide(traktaFragment)
            isTraktaOpen = false
        }
        activeFragment?.let { transaction.hide(it) }
        transaction.show(hymnReaderFragment).commitNowAllowingStateLoss()

        isReaderOpen = true
        floatingBottomNavPill.visibility = View.GONE
    }

    fun showSongDetailDialog(song: SongEntity) {
        openHymnReader(song.number)
    }

    fun closeHymnReader() {
        if (!isReaderOpen) return
        val target = activeFragment ?: fandraisanaFragment
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .hide(hymnReaderFragment)
            .show(target)
            .commitNowAllowingStateLoss()

        isReaderOpen = false
        floatingBottomNavPill.visibility = View.VISIBLE
    }

    /**
     * Opens the FandaharampotoanaFragment ("Fotoana • Ora fiangonana") with 0-lag show/hide transaction.
     */
    fun openFandaharampotoana() {
        val transaction = supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
        if (isReaderOpen) {
            transaction.hide(hymnReaderFragment)
            isReaderOpen = false
        }
        if (isTraktaReaderOpen) {
            transaction.hide(traktaReaderFragment)
            isTraktaReaderOpen = false
        }
        if (isFifandraisanaOpen) {
            transaction.hide(fifandraisanaFragment)
            isFifandraisanaOpen = false
        }
        if (isTraktaOpen) {
            traktaFragment.closeTraktaReader()
            transaction.hide(traktaFragment)
            isTraktaOpen = false
        }
        activeFragment?.let { transaction.hide(it) }
        transaction.show(fandaharampotoanaFragment).commitNowAllowingStateLoss()

        isFandaharampotoanaOpen = true
        floatingBottomNavPill.visibility = View.VISIBLE
    }

    fun closeFandaharampotoana() {
        if (!isFandaharampotoanaOpen) return
        val target = activeFragment ?: fandraisanaFragment
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .hide(fandaharampotoanaFragment)
            .show(target)
            .commitNowAllowingStateLoss()

        isFandaharampotoanaOpen = false
        floatingBottomNavPill.visibility = View.VISIBLE
    }

    /**
     * Opens the TraktaFragment ("Trakta • 13 boky") with 0-lag show/hide transaction.
     */
    fun openTrakta() {
        val transaction = supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
        if (isReaderOpen) {
            transaction.hide(hymnReaderFragment)
            isReaderOpen = false
        }
        if (isTraktaReaderOpen) {
            transaction.hide(traktaReaderFragment)
            isTraktaReaderOpen = false
        }
        if (isFifandraisanaOpen) {
            transaction.hide(fifandraisanaFragment)
            isFifandraisanaOpen = false
        }
        if (isFandaharampotoanaOpen) {
            transaction.hide(fandaharampotoanaFragment)
            isFandaharampotoanaOpen = false
        }
        activeFragment?.let { transaction.hide(it) }
        transaction.show(traktaFragment).commitNowAllowingStateLoss()

        isTraktaOpen = true
        floatingBottomNavPill.visibility = View.VISIBLE
    }

    fun closeTrakta() {
        if (!isTraktaOpen) return
        traktaFragment.closeTraktaReader()
        val target = activeFragment ?: fandraisanaFragment
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .hide(traktaFragment)
            .show(target)
            .commitNowAllowingStateLoss()

        isTraktaOpen = false
        floatingBottomNavPill.visibility = View.VISIBLE
    }

    /**
     * Opens the dedicated TraktaReaderFragment ("TraktaReaderScreen") when the user clicks
     * the "Hamaky" button on any Trakta card, passing the selected TraktaEntity parameters.
     */
    fun openTraktaReaderScreen(trakta: TraktaEntity) {
        traktaReaderFragment.loadTrakta(trakta, resetScroll = true)

        val transaction = supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
        if (isTraktaOpen) {
            transaction.hide(traktaFragment)
        } else {
            activeFragment?.let { transaction.hide(it) }
        }
        transaction.show(traktaReaderFragment).commitNowAllowingStateLoss()

        isTraktaReaderOpen = true
        floatingBottomNavPill.visibility = View.GONE
    }

    fun closeTraktaReaderScreen() {
        if (!isTraktaReaderOpen) return
        val transaction = supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .hide(traktaReaderFragment)

        if (isTraktaOpen) {
            transaction.show(traktaFragment)
            floatingBottomNavPill.visibility = View.VISIBLE
        } else {
            val target = activeFragment ?: fandraisanaFragment
            transaction.show(target)
            floatingBottomNavPill.visibility = View.VISIBLE
        }
        transaction.commitNowAllowingStateLoss()

        isTraktaReaderOpen = false
    }

    /**
     * Opens the FifandraisanaFragment ("Fifandraisana") when the user clicks
     * "Hifandray aminay" in KaontyFragment.
     */
    fun openFifandraisana() {
        val transaction = supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
        if (isReaderOpen) {
            transaction.hide(hymnReaderFragment)
            isReaderOpen = false
        }
        if (isTraktaReaderOpen) {
            transaction.hide(traktaReaderFragment)
            isTraktaReaderOpen = false
        }
        if (isFandaharampotoanaOpen) {
            transaction.hide(fandaharampotoanaFragment)
            isFandaharampotoanaOpen = false
        }
        if (isTraktaOpen) {
            traktaFragment.closeTraktaReader()
            transaction.hide(traktaFragment)
            isTraktaOpen = false
        }
        activeFragment?.let { transaction.hide(it) }
        transaction.show(fifandraisanaFragment).commitNowAllowingStateLoss()

        isFifandraisanaOpen = true
        floatingBottomNavPill.visibility = View.GONE
    }

    fun closeFifandraisana() {
        if (!isFifandraisanaOpen) return
        val target = activeFragment ?: kaontyFragment
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .hide(fifandraisanaFragment)
            .show(target)
            .commitNowAllowingStateLoss()

        isFifandraisanaOpen = false
        floatingBottomNavPill.visibility = View.VISIBLE
    }

    fun setBottomNavVisible(visible: Boolean) {
        floatingBottomNavPill.visibility =
            if (visible && !isReaderOpen && !isTraktaReaderOpen && !isFifandraisanaOpen && !isStartupOverlayVisible) View.VISIBLE else View.GONE
    }

    fun showNumpadLookupDialog() {
        switchToTab(
            tabIndex = TAB_HIKAROKA,
            initialQuery = "",
            openNumpadSheet = true
        )
    }

    fun toggleThemeMode() {
        ThemeManager.toggleTheme(this)
    }

    fun setThemeMode(mode: String) {
        ThemeManager.setThemeMode(this, mode)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        ThemeManager.handleConfigurationChange(this)
    }

    companion object {
        const val TAB_FANDRAISANA = 0
        const val TAB_HIKAROKA = 1
        const val TAB_HIRA_TIAKO = 2
        const val TAB_FAVORIS = 2
        const val TAB_KAONTY = 3

        private const val TAG_FANDRAISANA = "TAG_FANDRAISANA"
        private const val TAG_HIKAROKA = "TAG_HIKAROKA"
        private const val TAG_HIRA_TIAKO = "TAG_HIRA_TIAKO"
        private const val TAG_KAONTY = "TAG_KAONTY"
        private const val TAG_HYMN_READER = "TAG_HYMN_READER"
        private const val TAG_FANDAHARAMPOTOANA = "TAG_FANDAHARAMPOTOANA"
        private const val TAG_TRAKTA = "TAG_TRAKTA"
        private const val TAG_TRAKTA_READER = "TAG_TRAKTA_READER"
        private const val TAG_FIFANDRAISANA = "TAG_FIFANDRAISANA"
    }
}
