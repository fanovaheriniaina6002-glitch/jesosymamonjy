package com.example.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.data.local.TraktaEntity
import com.example.data.repository.SongRepository
import com.example.ui.util.KeepScreenOn
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemeManager

class TraktaReaderFragment : Fragment() {

    private lateinit var repository: SongRepository

    private var allTraktasState by mutableStateOf<List<TraktaEntity>>(emptyList())
    private var currentTraktaState by mutableStateOf<TraktaEntity?>(null)
    private var currentTraktaIdState by mutableIntStateOf(1)
    private var scrollResetTrigger by mutableIntStateOf(0)
    private var isReaderVisible by mutableStateOf(false)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        val db = AppDatabase.getInstance(context)
        repository = SongRepository(db.songDao(), context.applicationContext)

        isReaderVisible = !isHidden

        if (allTraktasState.isEmpty()) {
            allTraktasState = LocalContentCatalog.getCachedTraktasOrEmpty()
            LocalContentCatalog.ensureTraktasLoadedAsync(context)
        }
        if (currentTraktaState == null && allTraktasState.isNotEmpty()) {
            currentTraktaState = allTraktasState.firstOrNull { it.id == currentTraktaIdState }
                ?: allTraktasState.firstOrNull()
        }

        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val isDark by ThemeManager.isDarkMode.collectAsState()
                val mode by ThemeManager.themeMode.collectAsState()
                val dbTraktas by repository.allTraktaItems.collectAsState(initial = emptyList())
                val catalogTraktas by LocalContentCatalog.traktaEntitiesFlow.collectAsState()

                LaunchedEffect(dbTraktas, catalogTraktas) {
                    when {
                        dbTraktas.size >= 13 && dbTraktas.firstOrNull()?.content?.contains("\n\n") == true -> {
                            allTraktasState = dbTraktas
                        }
                        catalogTraktas.isNotEmpty() -> {
                            allTraktasState = catalogTraktas
                        }
                        allTraktasState.isEmpty() -> {
                            allTraktasState = LocalContentCatalog.loadTraktaEntitiesIo(context)
                        }
                    }
                    if (currentTraktaState == null && allTraktasState.isNotEmpty()) {
                        currentTraktaState = allTraktasState.firstOrNull { it.id == currentTraktaIdState }
                            ?: allTraktasState.first()
                    }
                }

                val activeTrakta = currentTraktaState
                    ?: allTraktasState.firstOrNull { it.id == currentTraktaIdState }
                    ?: allTraktasState.firstOrNull()

                MyApplicationTheme(darkTheme = isDark, themeMode = mode) {
                    if (isReaderVisible && activeTrakta != null) {
                        TraktaReaderScreen(
                            trakta = activeTrakta,
                            allTraktas = allTraktasState,
                            isDark = isDark,
                            scrollResetTrigger = scrollResetTrigger,
                            onBackClick = {
                                (activity as? MainActivity)?.closeTraktaReaderScreen()
                            },
                            onSelectTrakta = { target ->
                                loadTrakta(target, resetScroll = true)
                            },
                            onShareTrakta = { item ->
                                shareTrakta(item)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isReaderVisible = !isHidden
        if (!isHidden) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        isReaderVisible = !hidden
        if (!hidden) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onPause() {
        isReaderVisible = false
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onPause()
    }

    override fun onDestroyView() {
        isReaderVisible = false
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroyView()
    }

    fun applyThemeSync(@Suppress("UNUSED_PARAMETER") isDark: Boolean) {
        // Reactively observed via ThemeManager.isDarkMode inside ComposeView
    }

    fun loadTrakta(
        trakta: TraktaEntity,
        resetScroll: Boolean = true,
        @Suppress("UNUSED_PARAMETER") searchQuery: String? = null
    ) {
        val normalizedTrakta = if (trakta.content.contains("\n\n")) {
            trakta
        } else {
            val formatted = trakta.content
                .lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString("\n\n")
            trakta.copy(content = formatted)
        }
        currentTraktaIdState = normalizedTrakta.id
        currentTraktaState = normalizedTrakta
        if (resetScroll) {
            scrollResetTrigger++
        }
    }

    private fun shareTrakta(item: TraktaEntity) {
        val author = item.author.ifEmpty { com.example.data.local.getTraktaAuthor(item.id) }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "${item.numberLabel} - ${item.title}")
            putExtra(
                Intent.EXTRA_TEXT,
                if (author.isNotEmpty()) {
                    "${item.numberLabel} • ${item.title}\nMpanoratra: $author\n\n${item.content}"
                } else {
                    "${item.numberLabel} • ${item.title}\n\n${item.content}"
                }
            )
        }
        startActivity(Intent.createChooser(shareIntent, "Hizara ny Trakta"))
    }
}

@Composable
fun TraktaReaderScreen(
    trakta: TraktaEntity,
    allTraktas: List<TraktaEntity>,
    isDark: Boolean,
    scrollResetTrigger: Int,
    onBackClick: () -> Unit,
    onSelectTrakta: (TraktaEntity) -> Unit,
    onShareTrakta: (TraktaEntity) -> Unit
) {
    KeepScreenOn()

    val context = LocalContext.current
    val plusJakartaBold = remember {
        FontFamily(Font(R.font.plus_jakarta_sans, FontWeight.Bold))
    }
    val plusJakartaExtraBold = remember {
        FontFamily(Font(R.font.plus_jakarta_sans, FontWeight.ExtraBold))
    }
    val plusJakartaRegular = remember {
        FontFamily(Font(R.font.plus_jakarta_sans, FontWeight.Normal))
    }
    val interRegular = remember {
        FontFamily(Font(R.font.inter, FontWeight.Normal))
    }

    val bodyFontSizeSp by ThemeManager.lyricsFontSizeSp.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(trakta.id, scrollResetTrigger) {
        listState.scrollToItem(0)
    }

    val resourceId = remember(trakta.coverKey) {
        val resolved = resolveDrawableResourceId(context, trakta.coverKey)
        if (resolved != 0) resolved else R.drawable.ic_trakta_cover_1
    }

    // Rule 3: Parse text by double line breaks ("\n\n") to extract clean paragraphs
    val paragraphs = remember(trakta.id, trakta.content) {
        val normalized = if (trakta.content.contains("\n\n")) {
            trakta.content
        } else {
            trakta.content.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n\n")
        }
        normalized
            .split("\n\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    // Format header title strictly as "TRAKTA N° XY" (centered, bold)
    val formattedTopTitle = remember(trakta.id, trakta.numberLabel) {
        val label = trakta.numberLabel.trim()
        if (label.startsWith("TRAKTA", ignoreCase = true)) {
            label.uppercase()
        } else {
            "TRAKTA ${label.uppercase()}"
        }
    }

    val bgColor = MaterialTheme.colorScheme.background
    val cardSurface = MaterialTheme.colorScheme.surface
    val borderColor = MaterialTheme.colorScheme.outline
    val primaryThemeColor = MaterialTheme.colorScheme.primary
    val titleColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF781C2E)
    val secondaryColor = if (isDark) Color(0xFFA69497) else Color(0xFF564243)
    val pillBtnBg = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(horizontal = 16.dp)
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom != 1f && !zoom.isNaN() && zoom.isFinite()) {
                        val nextSize = (ThemeManager.lyricsFontSizeSp.value * zoom).coerceIn(12f, 40f)
                        ThemeManager.setLyricsFontSizeSp(context, nextSize)
                    }
                }
            }
            .testTag("trakta_reader_screen")
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // 1. Clean TopAppBar Capsule: ONLY "TRAKTA N° XY" centered and Bold (no "JESOSY MAMONJY" / "M. A. DAOUD")
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
                        .testTag("btn_trakta_reader_back"),
                    shape = CircleShape,
                    color = pillBtnBg,
                    border = BorderStroke(1.dp, borderColor),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back_ios),
                            contentDescription = "Miverina",
                            tint = titleColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = formattedTopTitle,
                    fontFamily = plusJakartaBold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 0.8.sp,
                    color = titleColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .testTag("tv_trakta_reader_top_title")
                )

                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable {
                            val nextSize = when {
                                bodyFontSizeSp < 17.5f -> 18.5f
                                bodyFontSizeSp < 20.5f -> 21.5f
                                bodyFontSizeSp < 25.5f -> 26f
                                else -> 16f
                            }.coerceIn(12f, 40f)
                            ThemeManager.setLyricsFontSizeSp(context, nextSize)
                        }
                        .testTag("btn_trakta_reader_font"),
                    shape = CircleShape,
                    color = pillBtnBg,
                    border = BorderStroke(1.dp, borderColor),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_format_size),
                            contentDescription = "Habe",
                            tint = titleColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Scrollable LazyColumn with generous paragraph spacing (18.dp) and 26.sp lineHeight
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("trakta_reader_lazy_column"),
            contentPadding = PaddingValues(top = 4.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header Bento Card (Cover + Metadata + Dynamic TRAKTA Badge + "Offline" badge)
            item(key = "reader_metadata_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardSurface),
                    border = BorderStroke(1.dp, borderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            modifier = Modifier
                                .width(84.dp)
                                .height(116.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF351019)),
                            border = BorderStroke(1.dp, borderColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(resourceId)
                                        .size(220, 300)
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

                        Column(modifier = Modifier.weight(1f)) {
                            // Rule 1: Dynamic Theme Badge ("TRAKTA")
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
                                fontSize = 18.sp,
                                lineHeight = 22.sp,
                                color = titleColor
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

                            Spacer(modifier = Modifier.height(8.dp))

                            // Rule 2: "Offline" badge below Mpanoratra: M. A. DAOUD
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check_circle),
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Offline",
                                    fontFamily = plusJakartaBold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }
            }

            // Rule 3: Paragraphs inside the Reader Surface Card (or direct spaced items inside the card)
            item(key = "reader_article_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardSurface),
                    border = BorderStroke(1.dp, borderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 22.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        paragraphs.forEachIndexed { index, paragraph ->
                            if (index == 0) {
                                // First paragraph (Title of the Trakta in .txt): ExtraBold, slightly larger, primary theme color
                                Text(
                                    text = paragraph,
                                    fontFamily = plusJakartaExtraBold,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = (bodyFontSizeSp + 3f).sp,
                                    lineHeight = maxOf(28f, (bodyFontSizeSp + 3f) * 1.4f).sp,
                                    color = primaryThemeColor,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                // Body paragraphs: spaced by 18.dp with airy lineHeight >= 26.sp
                                Text(
                                    text = paragraph,
                                    fontFamily = interRegular,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = bodyFontSizeSp.sp,
                                    lineHeight = maxOf(26f, bodyFontSizeSp * 1.55f).sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Bottom Divider & Previous / Share / Next Controls
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .height(1.dp)
                                .background(borderColor)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier
                                    .height(38.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        if (allTraktas.isNotEmpty()) {
                                            val idx = allTraktas.indexOfFirst { it.id == trakta.id }
                                            val prevIdx = if (idx > 0) idx - 1 else allTraktas.lastIndex
                                            onSelectTrakta(allTraktas[prevIdx])
                                        }
                                    }
                                    .testTag("btn_reader_prev_trakta"),
                                shape = CircleShape,
                                color = pillBtnBg,
                                border = BorderStroke(1.dp, borderColor),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_chevron_left),
                                        contentDescription = null,
                                        tint = titleColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Teo aloha",
                                        fontFamily = plusJakartaBold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = titleColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Surface(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .clickable { onShareTrakta(trakta) }
                                    .testTag("btn_reader_share_trakta"),
                                shape = CircleShape,
                                color = pillBtnBg,
                                border = BorderStroke(1.dp, borderColor),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_share),
                                        contentDescription = "Hizara",
                                        tint = titleColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                modifier = Modifier
                                    .height(38.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        if (allTraktas.isNotEmpty()) {
                                            val idx = allTraktas.indexOfFirst { it.id == trakta.id }
                                            val nextIdx = if (idx in 0 until allTraktas.lastIndex) idx + 1 else 0
                                            onSelectTrakta(allTraktas[nextIdx])
                                        }
                                    }
                                    .testTag("btn_reader_next_trakta"),
                                shape = CircleShape,
                                color = Color(0xFF781C2E),
                                border = BorderStroke(1.dp, borderColor),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Manaraka",
                                        fontFamily = plusJakartaBold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_chevron_right),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Footer Motto
            item(key = "reader_footer") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "« Ho voninahitr'Andriamanitra irery ihany »",
                        fontFamily = plusJakartaRegular,
                        fontStyle = FontStyle.Italic,
                        fontSize = 11.5.sp,
                        color = secondaryColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
