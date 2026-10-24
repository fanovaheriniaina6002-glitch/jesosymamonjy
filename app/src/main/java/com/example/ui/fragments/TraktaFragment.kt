package com.example.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.LocalContentCatalog
import com.example.data.local.OnboardingPreferencesManager
import com.example.data.local.TraktaEntity
import com.example.data.repository.SongRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemeManager
import java.util.Calendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Evaluates whether the current local time falls within the church worship hours
 * ("Fotoam-pivavahana") according to the user's selected region (Fiangonana).
 * Uses `java.util.Calendar` for native 0-overhead compatibility with Android 6.0+ (API 23+).
 */
object CulteTimeChecker {

    @Volatile
    var forcedCulteTimeForTesting: Boolean? = null

    fun isCulteTime(region: String, calendar: Calendar = Calendar.getInstance()): Boolean {
        forcedCulteTimeForTesting?.let { return it }

        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        return evaluateSchedule(region, dayOfWeek, currentMinutes)
    }

    private fun evaluateSchedule(region: String, dayOfWeek: Int, currentMinutes: Int): Boolean {
        return when (region.trim().lowercase()) {
            "ambohipo foibe" -> {
                when (dayOfWeek) {
                    Calendar.SUNDAY -> {
                        // 08H30-09H30, 10H-12H, 15H-17H
                        currentMinutes in (8 * 60 + 30)..(9 * 60 + 30) ||
                            currentMinutes in (10 * 60)..(12 * 60) ||
                            currentMinutes in (15 * 60)..(17 * 60)
                    }
                    Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY -> {
                        // Talata-Zoma: 05H-06H ; Talata sy Alakamisy: 17H-18H
                        val isMorning = currentMinutes in (5 * 60)..(6 * 60)
                        val isEvening = (dayOfWeek == Calendar.TUESDAY || dayOfWeek == Calendar.THURSDAY) &&
                            currentMinutes in (17 * 60)..(18 * 60)
                        isMorning || isEvening
                    }
                    else -> false
                }
            }

            "fianarantsoa" -> {
                when (dayOfWeek) {
                    Calendar.SUNDAY -> {
                        // 08H30-09H30, 09H30-11H, 14H-15H
                        currentMinutes in (8 * 60 + 30)..(11 * 60) ||
                            currentMinutes in (14 * 60)..(15 * 60)
                    }
                    Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY -> {
                        // Talata-Zoma: 05H30-06H ; Talata sy Alakamisy: 16H30-17H15
                        val isMorning = currentMinutes in (5 * 60 + 30)..(6 * 60)
                        val isEvening = (dayOfWeek == Calendar.TUESDAY || dayOfWeek == Calendar.THURSDAY) &&
                            currentMinutes in (16 * 60 + 30)..(17 * 60 + 15)
                        isMorning || isEvening
                    }
                    else -> false
                }
            }

            else -> {
                // Par Défaut (Toamasina, Mahajanga, Antsiranana, Antananarivo...)
                when (dayOfWeek) {
                    Calendar.SUNDAY -> {
                        // 08H30-09H30, 09H30-11H30, 15H-17H
                        currentMinutes in (8 * 60 + 30)..(11 * 60 + 30) ||
                            currentMinutes in (15 * 60)..(17 * 60)
                    }
                    Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY -> {
                        // Talata-Zoma: 05H-06H ; Talata sy Alakamisy: 17H-18H
                        val isMorning = currentMinutes in (5 * 60)..(6 * 60)
                        val isEvening = (dayOfWeek == Calendar.TUESDAY || dayOfWeek == Calendar.THURSDAY) &&
                            currentMinutes in (17 * 60)..(18 * 60)
                        isMorning || isEvening
                    }
                    else -> false
                }
            }
        }
    }
}

class TraktaFragment : Fragment() {

    private lateinit var repository: SongRepository
    private lateinit var onboardingPrefs: OnboardingPreferencesManager

    private var allTraktaListState by mutableStateOf<List<TraktaEntity>>(emptyList())
    private var inlineReaderTraktaState by mutableStateOf<TraktaEntity?>(null)
    private var userRegionState by mutableStateOf("Ambohipo foibe")
    private var isCulteTimeState by mutableStateOf(false)
    private var lockAnimationTrigger by mutableIntStateOf(0)

    private var immediateRegionListener: ((String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        val appContext = context.applicationContext
        val db = AppDatabase.getInstance(appContext)
        repository = SongRepository(db.songDao(), appContext)
        onboardingPrefs = OnboardingPreferencesManager(appContext)

        val initialRegion = onboardingPrefs.getRegionSync()
        userRegionState = initialRegion
        val initialCulte = CulteTimeChecker.isCulteTime(initialRegion)
        isCulteTimeState = initialCulte

        if (initialCulte) {
            // Do NOT generate or hold the 13 Trakta list in memory during worship hours
            allTraktaListState = emptyList()
        } else {
            allTraktaListState = LocalContentCatalog.getCachedTraktasOrEmpty()
            LocalContentCatalog.ensureTraktasLoadedAsync(appContext)
        }

        val regionListener: (String) -> Unit = { updatedRegion ->
            userRegionState = updatedRegion
            syncCulteStateAndMemory(appContext, updatedRegion)
        }
        immediateRegionListener = regionListener
        OnboardingPreferencesManager.registerRegionListener(regionListener)

        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val isDark by ThemeManager.isDarkMode.collectAsState()
                val mode by ThemeManager.themeMode.collectAsState()
                val activeRegion by OnboardingPreferencesManager.activeRegionFlow.collectAsState()
                val catalogTraktas by LocalContentCatalog.traktaEntitiesFlow.collectAsState()

                LaunchedEffect(activeRegion) {
                    if (activeRegion.isNotBlank() && activeRegion != userRegionState) {
                        userRegionState = activeRegion
                    }
                    syncCulteStateAndMemory(appContext, userRegionState)
                }

                // Re-check worship time periodically while screen is active
                LaunchedEffect(userRegionState) {
                    while (true) {
                        syncCulteStateAndMemory(appContext, userRegionState)
                        delay(15_000L)
                    }
                }

                // Only observe and load Trakta items on Dispatchers.IO when NOT in worship hours
                if (!isCulteTimeState) {
                    val dbTraktas by repository.allTraktaItems.collectAsState(initial = emptyList())
                    LaunchedEffect(dbTraktas, catalogTraktas, isCulteTimeState) {
                        if (!isCulteTimeState) {
                            when {
                                dbTraktas.size >= 13 && dbTraktas.firstOrNull()?.content?.contains("\n\n") == true -> {
                                    allTraktaListState = dbTraktas
                                }
                                catalogTraktas.isNotEmpty() -> {
                                    allTraktaListState = catalogTraktas
                                }
                                allTraktaListState.isEmpty() -> {
                                    allTraktaListState = LocalContentCatalog.loadTraktaEntitiesIo(appContext)
                                }
                            }
                        }
                    }
                }

                MyApplicationTheme(darkTheme = isDark, themeMode = mode) {
                    TraktaScreen(
                        isCulteTime = isCulteTimeState,
                        allTraktas = if (isCulteTimeState) emptyList() else allTraktaListState,
                        isDark = isDark,
                        lockAnimationTrigger = lockAnimationTrigger,
                        onBackClick = {
                            (activity as? MainActivity)?.closeTrakta()
                        },
                        onTraktaClick = { clickedTrakta ->
                            if (!isCulteTimeState) {
                                (activity as? MainActivity)?.openTraktaReaderScreen(clickedTrakta)
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        immediateRegionListener?.let { OnboardingPreferencesManager.unregisterRegionListener(it) }
        immediateRegionListener = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        if (::onboardingPrefs.isInitialized) {
            val currentRegion = onboardingPrefs.getRegionSync()
            userRegionState = currentRegion
            syncCulteStateAndMemory(requireContext().applicationContext, currentRegion)
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && ::onboardingPrefs.isInitialized) {
            val currentRegion = onboardingPrefs.getRegionSync()
            userRegionState = currentRegion
            syncCulteStateAndMemory(requireContext().applicationContext, currentRegion)
            if (isCulteTimeState) {
                lockAnimationTrigger++
            }
        }
    }

    fun refreshCulteLockState() {
        if (!isAdded) return
        val appContext = requireContext().applicationContext
        val currentRegion = if (::onboardingPrefs.isInitialized) {
            onboardingPrefs.getRegionSync()
        } else {
            userRegionState
        }
        userRegionState = currentRegion
        val culteNow = CulteTimeChecker.isCulteTime(currentRegion)
        isCulteTimeState = culteNow
        if (culteNow) {
            allTraktaListState = emptyList()
            lockAnimationTrigger++
        } else if (allTraktaListState.isEmpty()) {
            allTraktaListState = LocalContentCatalog.getCachedTraktasOrEmpty().ifEmpty {
                LocalContentCatalog.loadTraktaEntities(appContext)
            }
        }
    }

    fun isTraktaListLoadedInMemory(): Boolean = allTraktaListState.isNotEmpty()

    private fun syncCulteStateAndMemory(appContext: Context, region: String) {
        val culteNow = CulteTimeChecker.isCulteTime(region)
        isCulteTimeState = culteNow
        if (culteNow) {
            // Strictly release the 13 Trakta list from memory during worship hours
            if (allTraktaListState.isNotEmpty()) {
                allTraktaListState = emptyList()
            }
        } else {
            if (allTraktaListState.isEmpty()) {
                val cached = LocalContentCatalog.getCachedTraktasOrEmpty()
                if (cached.isNotEmpty()) {
                    allTraktaListState = cached
                } else {
                    LocalContentCatalog.ensureTraktasLoadedAsync(appContext)
                }
            }
        }
    }

    fun applyThemeSync(@Suppress("UNUSED_PARAMETER") isDark: Boolean) {
        // Reactively observed via ThemeManager.isDarkMode inside ComposeView
    }

    fun openTraktaReader(trakta: TraktaEntity) {
        if (isCulteTimeState) return
        inlineReaderTraktaState = trakta
        (activity as? MainActivity)?.openTraktaReaderScreen(trakta)
    }

    fun closeTraktaReader() {
        inlineReaderTraktaState = null
        (activity as? MainActivity)?.setBottomNavVisible(true)
    }

    fun isTraktaReaderVisible(): Boolean = inlineReaderTraktaState != null
}

@Composable
fun TraktaScreen(
    isCulteTime: Boolean = false,
    allTraktas: List<TraktaEntity>,
    isDark: Boolean,
    lockAnimationTrigger: Int = 0,
    onBackClick: () -> Unit,
    onTraktaClick: (TraktaEntity) -> Unit
) {
    val plusJakartaExtraBold = remember {
        FontFamily(Font(R.font.plus_jakarta_sans, FontWeight.ExtraBold))
    }
    val plusJakartaBold = remember {
        FontFamily(Font(R.font.plus_jakarta_sans, FontWeight.Bold))
    }
    val plusJakartaRegular = remember {
        FontFamily(Font(R.font.plus_jakarta_sans, FontWeight.Normal))
    }

    val bgColor = MaterialTheme.colorScheme.background
    val cardSurface = MaterialTheme.colorScheme.surface
    val borderColor = MaterialTheme.colorScheme.outline
    val titleAccent = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF781C2E)
    val subtitleColor = if (isDark) Color(0xFFA69497) else Color(0xFF564243)

    if (isCulteTime) {
        // Afficher l'écran de verrouillage central (Solid Bento + Animation 60 FPS)
        val bounceScale = remember(lockAnimationTrigger) { Animatable(0.76f) }
        val bounceAlpha = remember(lockAnimationTrigger) { Animatable(0f) }

        LaunchedEffect(lockAnimationTrigger) {
            launch {
                bounceScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            launch {
                bounceAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(horizontal = 24.dp)
                .testTag("trakta_culte_lock_box"),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = true,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = bounceScale.value
                            scaleY = bounceScale.value
                            alpha = bounceAlpha.value
                        }
                        .testTag("trakta_culte_lock_card"),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, borderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp, vertical = 38.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val lockIconTint = if (isDark) Color(0xFFD4AF37) else Color(0xFF781C2E)

                        Icon(
                            painter = painterResource(id = R.drawable.ic_lock_closed),
                            contentDescription = "FOTOAM-PIVAVAHANA",
                            tint = lockIconTint,
                            modifier = Modifier
                                .size(72.dp)
                                .testTag("trakta_culte_lock_icon")
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        Text(
                            text = "FOTOAM-PIVAVAHANA",
                            fontFamily = plusJakartaExtraBold,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            letterSpacing = 0.8.sp,
                            color = titleAccent,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("trakta_culte_lock_title")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Mihidy vonjimaika ny Trakta mba hifantohantsika amin'ny fivoriana.",
                            fontFamily = plusJakartaRegular,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            color = subtitleColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("trakta_culte_lock_subtitle")
                        )
                    }
                }
            }
        }
    } else {
        // Afficher la grille des 13 Trakta uniquement hors des heures de culte
        var isSearchVisible by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }

        val filteredTraktas = remember(allTraktas, searchQuery) {
            val q = searchQuery.trim()
            if (q.isEmpty()) {
                allTraktas
            } else {
                allTraktas.filter {
                    it.title.contains(q, ignoreCase = true) ||
                        it.numberLabel.contains(q, ignoreCase = true) ||
                        it.content.contains(q, ignoreCase = true)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(horizontal = 16.dp)
                .testTag("trakta_lazy_list"),
            contentPadding = PaddingValues(top = 14.dp, bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Top Pill Header
            item(key = "trakta_top_header") {
                Column {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(containerColor = cardSurface),
                        border = BorderStroke(1.dp, borderColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onBackClick)
                                    .testTag("btn_trakta_back"),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, borderColor),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_arrow_back_ios),
                                        contentDescription = "Miverina",
                                        tint = titleAccent,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "JESOSY MAMONJY",
                                    fontFamily = plusJakartaBold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.7.sp,
                                    color = titleAccent
                                )
                                Text(
                                    text = "M.A. DAOUD",
                                    fontFamily = plusJakartaBold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.5.sp,
                                    letterSpacing = 1.4.sp,
                                    color = subtitleColor
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        isSearchVisible = !isSearchVisible
                                        if (!isSearchVisible) searchQuery = ""
                                    }
                                    .testTag("btn_trakta_search"),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, borderColor),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_search),
                                        contentDescription = "Tadiavo",
                                        tint = titleAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (isSearchVisible) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(containerColor = cardSurface),
                            border = BorderStroke(1.dp, borderColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_search),
                                    contentDescription = null,
                                    tint = titleAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Hitady trakta (oh: Lanitra, Paska...)",
                                            fontFamily = plusJakartaRegular,
                                            fontSize = 13.sp,
                                            color = subtitleColor
                                        )
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            fontFamily = plusJakartaRegular,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        cursorBrush = SolidColor(titleAccent),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Trakta",
                        fontFamily = plusJakartaBold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = titleAccent
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Vakio ato ireo fampianarana nosoratan'ny Fr sy Sr Daoud",
                        fontFamily = plusJakartaRegular,
                        fontSize = 13.sp,
                        color = subtitleColor
                    )
                }
            }

            // 2. Trakta Bento Cards
            items(
                items = filteredTraktas,
                key = { it.id },
                contentType = { "trakta_card" }
            ) { trakta ->
                TraktaBentoCard(
                    trakta = trakta,
                    isDark = isDark,
                    plusJakartaBold = plusJakartaBold,
                    onClick = { onTraktaClick(trakta) }
                )
            }

            // 3. Footer Motto
            item(key = "trakta_footer") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "« Ho voninahitr'Andriamanitra irery ihany »",
                        fontFamily = plusJakartaRegular,
                        fontStyle = FontStyle.Italic,
                        fontSize = 11.5.sp,
                        color = subtitleColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun TraktaBentoCard(
    trakta: TraktaEntity,
    isDark: Boolean,
    plusJakartaBold: FontFamily,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val resourceId = remember(trakta.coverKey) {
        val resolved = resolveDrawableResourceId(context, trakta.coverKey)
        if (resolved != 0) resolved else R.drawable.ic_trakta_cover_1
    }

    val cardSurface = MaterialTheme.colorScheme.surface
    val borderColor = MaterialTheme.colorScheme.outline
    val titleColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF781C2E)
    val secondaryColor = if (isDark) Color(0xFFA69497) else Color(0xFF564243)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .testTag("trakta_card_${trakta.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardSurface),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Cover Image (100dp x 142dp) loaded with 60 FPS Coil AsyncImage
            Card(
                modifier = Modifier
                    .width(100.dp)
                    .height(142.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF351019)),
                border = BorderStroke(1.dp, borderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(resourceId)
                            .size(240, 340)
                            .precision(Precision.INEXACT)
                            .crossfade(true)
                            .build(),
                        contentDescription = trakta.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Right Content Block
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 142.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Rule 1: Dynamic Theme Badge ("TRAKTA") readable in both Day and Night Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, borderColor),
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {
                            Text(
                                text = "TRAKTA",
                                fontFamily = plusJakartaBold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }

                        Text(
                            text = trakta.numberLabel,
                            fontFamily = plusJakartaBold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = secondaryColor
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = trakta.title,
                        fontFamily = plusJakartaBold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.5.sp,
                        lineHeight = 21.sp,
                        color = titleColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    val author = trakta.author.ifEmpty { com.example.data.local.getTraktaAuthor(trakta.id) }
                    if (author.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Mpanoratra: $author",
                            fontFamily = plusJakartaBold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = secondaryColor
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(borderColor)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_check_circle),
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Offline",
                                fontFamily = plusJakartaBold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = secondaryColor
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(onClick = onClick)
                                .heightIn(min = 38.dp)
                                .testTag("btn_trakta_read_${trakta.id}"),
                            shape = CircleShape,
                            color = Color(0xFF781C2E),
                            border = BorderStroke(1.dp, borderColor),
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_menu_book),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Hamaky",
                                    fontFamily = plusJakartaBold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Resolves a drawable resource ID from a file name by stripping any extension first.
 * Never passes an extension to painterResource or getIdentifier.
 */
fun resolveDrawableResourceId(context: Context, rawFileName: String): Int {
    val fileNameWithoutExtension = rawFileName
        .trim()
        .substringAfterLast('/')
        .substringBeforeLast('.')
        .lowercase()
        .replace('-', '_')
        .replace(' ', '_')
    if (fileNameWithoutExtension.isEmpty()) return 0
    return try {
        context.resources.getIdentifier(fileNameWithoutExtension, "drawable", context.packageName)
    } catch (_: Exception) {
        0
    }
}
