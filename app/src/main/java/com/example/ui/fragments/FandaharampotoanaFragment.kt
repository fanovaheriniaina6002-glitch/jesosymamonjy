package com.example.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.OnboardingPreferencesManager
import com.example.data.repository.SongRepository
import com.example.ui.theme.ThemeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class RegionalScheduleItem(
    val id: Int,
    val section: String,
    val title: String,
    val subtitle: String,
    val timeRange: String
)

class FandaharampotoanaViewModel(
    private val onboardingPrefs: OnboardingPreferencesManager,
    private val repository: SongRepository
) : ViewModel() {

    private val _userRegion = MutableStateFlow(onboardingPrefs.getRegionSync())
    val userRegion: StateFlow<String> = _userRegion.asStateFlow()

    private val _scheduleItems = MutableStateFlow(resolveScheduleForRegion(_userRegion.value))
    val scheduleItems: StateFlow<List<RegionalScheduleItem>> = _scheduleItems.asStateFlow()

    init {
        // 1. Observe 0ms in-memory region StateFlow from OnboardingPreferencesManager
        viewModelScope.launch {
            OnboardingPreferencesManager.activeRegionFlow.collect { region ->
                updateRegionIfChanged(region)
            }
        }
        // 2. Observe Jetpack DataStore onboarding preferences
        viewModelScope.launch {
            onboardingPrefs.onboardingDataFlow.collect { data ->
                if (data.region.isNotBlank()) {
                    updateRegionIfChanged(data.region)
                }
            }
        }
        // 3. Observe Room UserProfileEntity changes
        viewModelScope.launch {
            repository.userProfile.collect { profile ->
                val profileRegion = profile?.region?.trim().orEmpty()
                if (profileRegion.isNotEmpty() && profile?.isOnboardingCompleted == true) {
                    updateRegionIfChanged(profileRegion)
                }
            }
        }
    }

    fun refreshRegionSync() {
        val current = onboardingPrefs.getRegionSync()
        updateRegionIfChanged(current)
    }

    private fun updateRegionIfChanged(rawRegion: String) {
        val clean = rawRegion.trim().ifEmpty { "Ambohipo foibe" }
        if (_userRegion.value != clean || _scheduleItems.value.isEmpty()) {
            _userRegion.value = clean
            _scheduleItems.value = resolveScheduleForRegion(clean)
        }
    }

    companion object {
        fun resolveScheduleForRegion(region: String): List<RegionalScheduleItem> {
            return when (region.trim().lowercase()) {
                "ambohipo foibe" -> listOf(
                    RegionalScheduleItem(
                        id = 1,
                        section = "ALAHADY",
                        title = "Sekoly Alahady sy Tanora",
                        subtitle = "Fianarana Soratra Masina",
                        timeRange = "08H30-09H30"
                    ),
                    RegionalScheduleItem(
                        id = 2,
                        section = "ALAHADY",
                        title = "Maraina",
                        subtitle = "Hira, Vavaka sy Toriteny",
                        timeRange = "10H-12H"
                    ),
                    RegionalScheduleItem(
                        id = 3,
                        section = "ALAHADY",
                        title = "Hariva",
                        subtitle = "Hira, Vavaka sy Toriteny",
                        timeRange = "15H-17H"
                    ),
                    RegionalScheduleItem(
                        id = 4,
                        section = "VAVAKA ANDAVANANDRO",
                        title = "Talata-Zoma",
                        subtitle = "Vavaka maraina",
                        timeRange = "05H-06H"
                    ),
                    RegionalScheduleItem(
                        id = 5,
                        section = "VAVAKA ANDAVANANDRO",
                        title = "Talata sy Alakamisy",
                        subtitle = "Fotoam-bavaka hariva",
                        timeRange = "17H-18H"
                    )
                )

                "fianarantsoa" -> listOf(
                    RegionalScheduleItem(
                        id = 1,
                        section = "ALAHADY",
                        title = "Sekoly Alahady sy Tanora",
                        subtitle = "Fianarana Soratra Masina",
                        timeRange = "08H30-09H30"
                    ),
                    RegionalScheduleItem(
                        id = 2,
                        section = "ALAHADY",
                        title = "Maraina",
                        subtitle = "Hira, Vavaka sy Toriteny",
                        timeRange = "09H30-11H"
                    ),
                    RegionalScheduleItem(
                        id = 3,
                        section = "ALAHADY",
                        title = "Hariva",
                        subtitle = "Hira, Vavaka sy Toriteny",
                        timeRange = "14H-15H"
                    ),
                    RegionalScheduleItem(
                        id = 4,
                        section = "VAVAKA ANDAVANANDRO",
                        title = "Talata-Zoma",
                        subtitle = "Vavaka maraina",
                        timeRange = "05H30-06H"
                    ),
                    RegionalScheduleItem(
                        id = 5,
                        section = "VAVAKA ANDAVANANDRO",
                        title = "Talata sy Alakamisy",
                        subtitle = "Fotoam-bavaka hariva",
                        timeRange = "16H30-17H15"
                    )
                )

                else -> listOf(
                    RegionalScheduleItem(
                        id = 1,
                        section = "ALAHADY",
                        title = "Sekoly Alahady sy Tanora",
                        subtitle = "Fianarana Soratra Masina",
                        timeRange = "08H30-09H30"
                    ),
                    RegionalScheduleItem(
                        id = 2,
                        section = "ALAHADY",
                        title = "Maraina",
                        subtitle = "Hira, Vavaka sy Toriteny",
                        timeRange = "09H30-11H30"
                    ),
                    RegionalScheduleItem(
                        id = 3,
                        section = "ALAHADY",
                        title = "Hariva",
                        subtitle = "Hira, Vavaka sy Toriteny",
                        timeRange = "15H-17H"
                    ),
                    RegionalScheduleItem(
                        id = 4,
                        section = "VAVAKA ANDAVANANDRO",
                        title = "Talata-Zoma",
                        subtitle = "Vavaka maraina",
                        timeRange = "05H-06H"
                    ),
                    RegionalScheduleItem(
                        id = 5,
                        section = "VAVAKA ANDAVANANDRO",
                        title = "Talata sy Alakamisy",
                        subtitle = "Fotoam-bavaka hariva",
                        timeRange = "17H-18H"
                    )
                )
            }
        }
    }
}

class FandaharampotoanaFragment : Fragment() {

    private lateinit var viewModel: FandaharampotoanaViewModel
    private var immediateRegionListener: ((String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_fandaharampotoana, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appContext = requireContext().applicationContext
        val db = AppDatabase.getInstance(appContext)
        val repository = SongRepository(db.songDao())
        val onboardingPrefs = OnboardingPreferencesManager(appContext)

        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return FandaharampotoanaViewModel(onboardingPrefs, repository) as T
                }
            }
        )[FandaharampotoanaViewModel::class.java]

        val btnBack: View? = view.findViewById(R.id.btn_fotoana_back)
        val tvRegionSubtitle: TextView? = view.findViewById(R.id.tv_fotoana_region_subtitle)

        val tvTitle1: TextView? = view.findViewById(R.id.tv_schedule_title_1)
        val tvSub1: TextView? = view.findViewById(R.id.tv_schedule_sub_1)
        val tvTime1: TextView? = view.findViewById(R.id.tv_schedule_time_1)

        val tvTitle2: TextView? = view.findViewById(R.id.tv_schedule_title_2)
        val tvSub2: TextView? = view.findViewById(R.id.tv_schedule_sub_2)
        val tvTime2: TextView? = view.findViewById(R.id.tv_schedule_time_2)

        val tvTitle3: TextView? = view.findViewById(R.id.tv_schedule_title_3)
        val tvSub3: TextView? = view.findViewById(R.id.tv_schedule_sub_3)
        val tvTime3: TextView? = view.findViewById(R.id.tv_schedule_time_3)

        val tvTitle4: TextView? = view.findViewById(R.id.tv_schedule_title_4)
        val tvSub4: TextView? = view.findViewById(R.id.tv_schedule_sub_4)
        val tvTime4: TextView? = view.findViewById(R.id.tv_schedule_time_4)

        val tvTitle5: TextView? = view.findViewById(R.id.tv_schedule_title_5)
        val tvSub5: TextView? = view.findViewById(R.id.tv_schedule_sub_5)
        val tvTime5: TextView? = view.findViewById(R.id.tv_schedule_time_5)

        // Ensure all Bento cards are strictly non-clickable (0 dialog, 100% static consultation)
        listOf(
            R.id.card_fotoana_verse,
            R.id.card_alahady_1,
            R.id.card_alahady_2,
            R.id.card_alahady_3,
            R.id.card_andavanandro_1,
            R.id.card_andavanandro_2
        ).forEach { cardId ->
            view.findViewById<View>(cardId)?.apply {
                setOnClickListener(null)
                isClickable = false
                isFocusable = false
            }
        }

        btnBack?.setOnClickListener {
            (activity as? MainActivity)?.closeFandaharampotoana()
        }

        fun bindSchedule(region: String, items: List<RegionalScheduleItem>) {
            tvRegionSubtitle?.text = "Ao amin'ny Fiangonana Jesosy Mamonjy M. A DAOUD • $region"
            items.forEach { item ->
                when (item.id) {
                    1 -> {
                        tvTitle1?.text = item.title
                        tvSub1?.text = item.subtitle
                        tvTime1?.text = item.timeRange
                    }
                    2 -> {
                        tvTitle2?.text = item.title
                        tvSub2?.text = item.subtitle
                        tvTime2?.text = item.timeRange
                    }
                    3 -> {
                        tvTitle3?.text = item.title
                        tvSub3?.text = item.subtitle
                        tvTime3?.text = item.timeRange
                    }
                    4 -> {
                        tvTitle4?.text = item.title
                        tvSub4?.text = item.subtitle
                        tvTime4?.text = item.timeRange
                    }
                    5 -> {
                        tvTitle5?.text = item.title
                        tvSub5?.text = item.subtitle
                        tvTime5?.text = item.timeRange
                    }
                }
            }
        }

        val listener: (String) -> Unit = { region ->
            viewModel.refreshRegionSync()
            bindSchedule(region, FandaharampotoanaViewModel.resolveScheduleForRegion(region))
        }
        immediateRegionListener = listener
        OnboardingPreferencesManager.registerRegionListener(listener)

        // 0ms immediate bind on view creation
        viewModel.refreshRegionSync()
        bindSchedule(viewModel.userRegion.value, viewModel.scheduleItems.value)

        applyThemeSync(ThemeManager.isDarkMode.value)

        viewLifecycleOwner.lifecycleScope.launch {
            ThemeManager.isDarkMode.collectLatest { isDark ->
                applyThemeSync(isDark)
            }
        }

        // Reactive 0-latency observation of userRegion & scheduleItems
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.userRegion.collect { region ->
                        bindSchedule(region, FandaharampotoanaViewModel.resolveScheduleForRegion(region))
                    }
                }
                launch {
                    viewModel.scheduleItems.collect { items ->
                        bindSchedule(viewModel.userRegion.value, items)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        immediateRegionListener?.let { OnboardingPreferencesManager.unregisterRegionListener(it) }
        immediateRegionListener = null
        super.onDestroyView()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && ::viewModel.isInitialized) {
            viewModel.refreshRegionSync()
        }
    }

    fun applyThemeSync(isDark: Boolean) {
        val rootView = view ?: return
        ThemeManager.applyThemeToViewTree(rootView, isDark)
    }
}
