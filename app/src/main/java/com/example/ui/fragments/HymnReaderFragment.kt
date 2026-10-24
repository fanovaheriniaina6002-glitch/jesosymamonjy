package com.example.ui.fragments

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.NestedScrollView
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.HymnCatalog
import com.example.data.local.SongEntity
import com.example.data.repository.SongRepository
import android.content.res.ColorStateList
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.example.ui.theme.ThemeManager
import com.example.ui.util.HighlightHelper
import com.example.ui.util.KeepScreenOn
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Full-Screen Hymn Reader (60 FPS Solid Bento).
 * Loads fullLyrics on demand from HymnCatalog in 0ms and dynamically renders
 * all stanzas with 5-second intelligent search highlight and 300ms horizontal
 * slide + fade transition when switching hymns.
 */
class HymnReaderFragment : Fragment() {

    private lateinit var repository: SongRepository
    private var isReaderActiveState by mutableStateOf(false)
    private var currentSongNumber: Int = 1
    private var currentSong: SongEntity? = null
    private var currentFontSizeSp: Float = ThemeManager.DEFAULT_LYRICS_FONT_SIZE_SP

    private var currentSearchQuery: String? = null
    private var isHighlightVisible: Boolean = false
    private var highlightJob: Job? = null
    private var heartAnimJob: Job? = null

    private var tvHeaderTitle: TextView? = null
    private var tvHeaderTitleOutgoing: TextView? = null

    private var sectionVerse1: View? = null
    private var tvVerse1: TextView? = null
    private var sectionChorus: View? = null
    private var tvChorus: TextView? = null
    private var sectionVerse2: View? = null
    private var tvVerse2: TextView? = null
    private var sectionVerse3: View? = null
    private var tvVerse3: TextView? = null
    private var extraVersesContainer: LinearLayout? = null
    private var scrollView: NestedScrollView? = null
    private var ivLyricsTransitionSnapshot: ImageView? = null
    private var snapshotBitmap: Bitmap? = null
    private var cardReaderLyrics: MaterialCardView? = null
    private var ivDoubleTapHeart: ImageView? = null

    private var iconFav: ImageView? = null
    private var tvPrevHymn: TextView? = null
    private var tvNextHymn: TextView? = null

    private var fontJakartaBold: Typeface? = null
    private var fontInterRegular: Typeface? = null
    private var fontInterItalic: Typeface? = null

    private val dynamicVerseBadges = mutableListOf<TextView>()
    private val dynamicVerseTextViews = mutableListOf<TextView>()
    private val dynamicChorusBars = mutableListOf<View>()
    private val dynamicChorusIcons = mutableListOf<ImageView>()
    private val dynamicChorusLabels = mutableListOf<TextView>()
    private val dynamicChorusTextViews = mutableListOf<TextView>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_hymn_reader, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ctx = requireContext()
        HymnCatalog.ensureInitializedAsync(ctx)
        val dao = AppDatabase.getInstance(ctx).songDao()
        repository = SongRepository(dao, ctx.applicationContext)

        // Pre-cache fonts once at initialization for 60 FPS rendering
        fontJakartaBold = try {
            ResourcesCompat.getFont(ctx, R.font.plus_jakarta_sans)?.let {
                Typeface.create(it, Typeface.BOLD)
            }
        } catch (_: Exception) {
            Typeface.DEFAULT_BOLD
        }
        fontInterRegular = try {
            ResourcesCompat.getFont(ctx, R.font.inter)
        } catch (_: Exception) {
            Typeface.DEFAULT
        }
        fontInterItalic = fontInterRegular?.let {
            Typeface.create(it, Typeface.ITALIC)
        } ?: Typeface.defaultFromStyle(Typeface.ITALIC)

        val btnBack = view.findViewById<MaterialCardView>(R.id.btn_reader_back)
        val btnThemeToggle = view.findViewById<MaterialCardView>(R.id.btn_reader_theme_toggle)
        tvHeaderTitle = view.findViewById(R.id.tv_reader_header_title)
        tvHeaderTitleOutgoing = view.findViewById(R.id.tv_reader_header_title_outgoing)

        sectionVerse1 = view.findViewById(R.id.section_verse_1)
        tvVerse1 = view.findViewById(R.id.tv_verse_1)
        sectionChorus = view.findViewById(R.id.section_chorus)
        tvChorus = view.findViewById(R.id.tv_chorus)
        sectionVerse2 = view.findViewById(R.id.section_verse_2)
        tvVerse2 = view.findViewById(R.id.tv_verse_2)
        sectionVerse3 = view.findViewById(R.id.section_verse_3)
        tvVerse3 = view.findViewById(R.id.tv_verse_3)
        extraVersesContainer = view.findViewById(R.id.extra_verses_container)
        scrollView = view.findViewById(R.id.reader_scroll_view)
        ivLyricsTransitionSnapshot = view.findViewById(R.id.iv_lyrics_transition_snapshot)
        cardReaderLyrics = view.findViewById(R.id.card_reader_lyrics)
        ivDoubleTapHeart = view.findViewById(R.id.iv_double_tap_heart)

        val btnShare = view.findViewById<MaterialCardView>(R.id.btn_reader_share)
        val btnCopy = view.findViewById<MaterialCardView>(R.id.btn_reader_copy)
        val btnFav = view.findViewById<MaterialCardView>(R.id.btn_reader_fav)
        iconFav = view.findViewById(R.id.icon_reader_fav)

        val btnPrevHymn = view.findViewById<View>(R.id.btn_prev_hymn)
        tvPrevHymn = view.findViewById(R.id.tv_prev_hymn)
        val btnNextHymn = view.findViewById<View>(R.id.btn_next_hymn)
        tvNextHymn = view.findViewById(R.id.tv_next_hymn)

        val btnFontDecrease = view.findViewById<View>(R.id.btn_font_decrease)
        val btnFontIncrease = view.findViewById<View>(R.id.btn_font_increase)

        currentFontSizeSp = ThemeManager.lyricsFontSizeSp.value

        btnBack.setOnClickListener {
            (activity as? MainActivity)?.closeHymnReader()
        }

        btnThemeToggle.setOnClickListener {
            (activity as? MainActivity)?.toggleThemeMode()
        }

        applyThemeSync(ThemeManager.isDarkMode.value)

        viewLifecycleOwner.lifecycleScope.launch {
            ThemeManager.isDarkMode.collectLatest { isDark ->
                applyThemeSync(isDark)
            }
        }

        // Rule 1: Observe global lyricsFontSizeSp (shared between KaontyScreen and LyricsReader)
        viewLifecycleOwner.lifecycleScope.launch {
            ThemeManager.lyricsFontSizeSp.collectLatest { sizeSp ->
                currentFontSizeSp = sizeSp
                applyFontSize(sizeSp)
            }
        }

        // Rule 5: 0ms Favorite Toggle from Reader Pill
        btnFav.setOnClickListener {
            val song = currentSong ?: return@setOnClickListener
            val isNowFav = HymnCatalog.toggleFavorite(requireContext(), song.number)
            val updated = song.copy(isFavorite = isNowFav)
            currentSong = updated
            updateFavoriteIcon(isNowFav)
        }

        btnCopy.setOnClickListener {
            val song = currentSong ?: return@setOnClickListener
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val textToCopy = "Fihirana #${song.numberStr} - ${song.title}\n\n${song.fullLyrics.ifBlank { song.verse1 }}"
            clipboard.setPrimaryClip(ClipData.newPlainText("Fihirana #${song.numberStr}", textToCopy))
        }

        btnShare.setOnClickListener {
            val song = currentSong ?: return@setOnClickListener
            val shareText = buildString {
                append("FIHIRANA #${song.numberStr} - ${song.title}\n")
                append("(${song.category} - JESOSY MAMONJY M. A. DAOUD)\n\n")
                append(song.fullLyrics.ifBlank { song.verse1 })
            }
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Fihirana #${song.numberStr} - ${song.title}")
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(sendIntent, "Hizara ny tononkira"))
        }

        // Rule 1: A- | A+ modify global lyricsFontSizeSp (saved in preferences)
        btnFontDecrease.setOnClickListener {
            val nextSize = (ThemeManager.lyricsFontSizeSp.value - 2f).coerceIn(12f, 40f)
            ThemeManager.setLyricsFontSizeSp(requireContext(), nextSize)
        }

        btnFontIncrease.setOnClickListener {
            val nextSize = (ThemeManager.lyricsFontSizeSp.value + 2f).coerceIn(12f, 40f)
            ThemeManager.setLyricsFontSizeSp(requireContext(), nextSize)
        }

        btnPrevHymn.setOnClickListener {
            navigateToPreviousHymn()
        }

        btnNextHymn.setOnClickListener {
            navigateToNextHymn()
        }

        setupLyricsGestures()

        val composeKeepScreenOnHost = ComposeView(ctx).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                if (isReaderActiveState) {
                    HymnReaderScreen()
                }
            }
        }
        (view as? ViewGroup)?.addView(composeKeepScreenOnHost, ViewGroup.LayoutParams(0, 0))

        // Do NOT record in history during background initialization so history starts 100% empty!
        loadHymn(currentSongNumber, searchQuery = null, recordInHistory = false)
    }

    override fun onResume() {
        super.onResume()
        isReaderActiveState = !isHidden
        if (!isHidden) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        isReaderActiveState = !hidden
        if (!hidden) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onPause() {
        isReaderActiveState = false
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onPause()
    }

    override fun onDestroyView() {
        isReaderActiveState = false
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroyView()
    }

    private fun navigateToPreviousHymn() {
        val prevNumber = if (currentSongNumber > 1) currentSongNumber - 1 else 830
        loadHymn(prevNumber, searchQuery = null, recordInHistory = true, animateTransition = true)
    }

    private fun navigateToNextHymn() {
        val nextNumber = if (currentSongNumber < 830) currentSongNumber + 1 else 1
        loadHymn(nextNumber, searchQuery = null, recordInHistory = true, animateTransition = true)
    }

    /**
     * Compares targetState and initialState (song IDs) to determine slide direction:
     * - true  -> toward NEXT hymn (Swipe Left / Next button): slide in from +width, slide out to -width
     * - false -> toward PREVIOUS hymn (Swipe Right / Prev button): slide in from -width, slide out to +width
     */
    private fun isTransitionTowardNext(initialState: Int, targetState: Int): Boolean {
        if (initialState == 830 && targetState == 1) return true
        if (initialState == 1 && targetState == 830) return false
        return targetState > initialState
    }

    /**
     * Rules 6 & 7:
     * - Double-tap on lyrics area -> adds hymn to favorites (if not already) + 60 FPS 80dp Heart bounce animation.
     * - Horizontal swipe Left -> opens next hymn (Hira X+1) with 300ms slide + fade transition.
     * - Horizontal swipe Right -> opens previous hymn (Hira X-1) with 300ms slide + fade transition.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupLyricsGestures() {
        val ctx = context ?: return
        val density = resources.displayMetrics.density
        val minSwipeDistancePx = 64f * density
        val minFlingVelocityPx = 180f * density

        var touchDownRawX = 0f
        var touchDownRawY = 0f
        var maxVerticalMovePx = 0f
        var swipeHandledInGesture = false
        var isPinchScaling = false

        val scaleGestureDetector = ScaleGestureDetector(
            ctx,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                    isPinchScaling = true
                    swipeHandledInGesture = true
                    return true
                }

                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val zoom = detector.scaleFactor
                    applyPinchZoomFactor(zoom)
                    return true
                }

                override fun onScaleEnd(detector: ScaleGestureDetector) {
                    isPinchScaling = false
                }
            }
        )

        val gestureDetector = GestureDetector(
            ctx,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    if (isPinchScaling) return false
                    handleLyricsDoubleTap()
                    return true
                }

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (swipeHandledInGesture || isPinchScaling || e1 == null) return false
                    val dx = e2.rawX - e1.rawX
                    val dy = e2.rawY - e1.rawY
                    if (abs(dx) > minSwipeDistancePx &&
                        abs(dx) > abs(dy) * 1.5f &&
                        abs(velocityX) > minFlingVelocityPx
                    ) {
                        swipeHandledInGesture = true
                        if (dx < 0f) {
                            navigateToNextHymn()
                        } else {
                            navigateToPreviousHymn()
                        }
                        return true
                    }
                    return false
                }
            }
        )

        scrollView?.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            if (event.pointerCount > 1 || scaleGestureDetector.isInProgress) {
                isPinchScaling = true
                swipeHandledInGesture = true
                return@setOnTouchListener true
            }

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    touchDownRawX = event.rawX
                    touchDownRawY = event.rawY
                    maxVerticalMovePx = 0f
                    swipeHandledInGesture = false
                    isPinchScaling = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val currentDy = abs(event.rawY - touchDownRawY)
                    if (currentDy > maxVerticalMovePx) {
                        maxVerticalMovePx = currentDy
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (!swipeHandledInGesture && !isPinchScaling) {
                        val dx = event.rawX - touchDownRawX
                        val dy = event.rawY - touchDownRawY
                        if (abs(dx) > (80f * density) &&
                            abs(dx) > abs(dy) * 1.6f &&
                            abs(dx) > maxVerticalMovePx * 1.5f
                        ) {
                            swipeHandledInGesture = true
                            if (dx < 0f) {
                                navigateToNextHymn()
                            } else {
                                navigateToPreviousHymn()
                            }
                        }
                    }
                    isPinchScaling = false
                }
                MotionEvent.ACTION_CANCEL -> {
                    isPinchScaling = false
                }
            }
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    fun applyPinchZoomFactor(zoom: Float) {
        val ctx = context ?: return
        if (zoom != 1f && !zoom.isNaN() && zoom.isFinite()) {
            val nextSize = (ThemeManager.lyricsFontSizeSp.value * zoom).coerceIn(12f, 40f)
            ThemeManager.setLyricsFontSizeSp(ctx, nextSize)
        }
    }

    /**
     * Rule 6: Instagram-style Double-Tap on Lyrics (60 FPS, zero blur):
     * - Adds hymn to favorites if not already favorited.
     * - Animates 80dp Garnet/Red Heart at screen center: scale 0f -> 1.2f -> 1f in 250ms,
     *   then fades alpha -> 0f after 600ms.
     */
    private fun handleLyricsDoubleTap() {
        val song = currentSong ?: return
        val ctx = context ?: return

        if (!song.isFavorite) {
            HymnCatalog.addFavorite(ctx, song.number)
            currentSong = song.copy(isFavorite = true)
            updateFavoriteIcon(true)
        } else {
            updateFavoriteIcon(true)
        }

        val heart = ivDoubleTapHeart ?: return
        val isDark = ThemeManager.isDarkMode.value
        ThemeManager.setSolidIconTint(
            heart,
            if (isDark) 0xFFC82846.toInt() else 0xFF800020.toInt()
        )

        heartAnimJob?.cancel()
        heart.animate().cancel()

        heart.visibility = View.VISIBLE
        heart.alpha = 1f
        heart.scaleX = 0f
        heart.scaleY = 0f

        // Bounce 0f -> 1.2f (150ms) -> 1.0f (100ms) = 250ms total
        heart.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(150L)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                heart.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(100L)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
            .start()

        heartAnimJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(600L)
            heart.animate()
                .alpha(0f)
                .setDuration(180L)
                .withEndAction {
                    heart.visibility = View.GONE
                }
                .start()
        }
    }

    fun loadSongByNumber(songNumber: Int, resetScroll: Boolean = true) {
        loadHymn(songNumber, searchQuery = null, recordInHistory = true, animateTransition = false)
        if (resetScroll) {
            scrollView?.scrollTo(0, 0)
        }
    }

    fun loadHymn(
        songNumber: Int,
        searchQuery: String? = null,
        recordInHistory: Boolean = true,
        animateTransition: Boolean = false
    ) {
        val initialState = currentSongNumber
        val targetState = songNumber
        val shouldAnimate = animateTransition && initialState != targetState && currentSong != null

        currentSongNumber = songNumber
        currentSearchQuery = searchQuery?.trim()
        highlightJob?.cancel()

        if (!isAdded || view == null) return

        val headerTitleView = tvHeaderTitle
        val headerOutgoingView = tvHeaderTitleOutgoing
        val lyricsScroll = scrollView
        val snapshotView = ivLyricsTransitionSnapshot

        // Cancel any in-flight transitions and normalize incoming views before capturing outgoing state
        headerTitleView?.animate()?.cancel()
        headerOutgoingView?.animate()?.cancel()
        lyricsScroll?.animate()?.cancel()
        snapshotView?.animate()?.cancel()

        var snapshotReady = false
        if (shouldAnimate && lyricsScroll != null && snapshotView != null) {
            headerTitleView?.translationX = 0f
            headerTitleView?.alpha = 1f
            lyricsScroll.translationX = 0f
            lyricsScroll.alpha = 1f

            headerOutgoingView?.let { outgoing ->
                outgoing.text = headerTitleView?.text
                headerTitleView?.currentTextColor?.let { outgoing.setTextColor(it) }
            }

            val w = lyricsScroll.width
            val h = lyricsScroll.height
            if (w > 0 && h > 0) {
                var bmp = snapshotBitmap
                if (bmp == null || bmp.width != w || bmp.height != h || bmp.isRecycled) {
                    bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    snapshotBitmap = bmp
                } else {
                    bmp.eraseColor(0)
                }
                val canvas = Canvas(bmp)
                canvas.save()
                canvas.translate(-lyricsScroll.scrollX.toFloat(), -lyricsScroll.scrollY.toFloat())
                lyricsScroll.draw(canvas)
                canvas.restore()
                snapshotView.setImageBitmap(bmp)
                snapshotReady = true
            }
        } else {
            headerOutgoingView?.visibility = View.GONE
            snapshotView?.visibility = View.GONE
            headerTitleView?.translationX = 0f
            headerTitleView?.alpha = 1f
            lyricsScroll?.translationX = 0f
            lyricsScroll?.alpha = 1f
        }

        if (!currentSearchQuery.isNullOrBlank() && !currentSearchQuery!!.all { it.isDigit() }) {
            isHighlightVisible = true
            highlightJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(5000L) // Exactement 5 secondes
                isHighlightVisible = false
                currentSong?.let { renderStanzas(it) }
            }
        } else {
            isHighlightVisible = false
        }

        val ctx = requireContext()
        val song = HymnCatalog.getSongByNumber(ctx, songNumber)
            ?: HymnCatalog.getSongByNumber(ctx, 1)
            ?: return

        currentSong = song
        if (recordInHistory) {
            HymnCatalog.recordSongConsulted(ctx, song.number)
        }

        tvHeaderTitle?.text = "${song.numberStr}. ${song.title}"

        val prevNum = if (song.number > 1) song.number - 1 else 830
        val nextNum = if (song.number < 830) song.number + 1 else 1
        tvPrevHymn?.text = "Hira $prevNum"
        tvNextHymn?.text = "Hira $nextNum"

        updateFavoriteIcon(song.isFavorite)
        renderStanzas(song)
        scrollView?.scrollTo(0, 0)

        if (shouldAnimate && lyricsScroll != null) {
            val isNext = isTransitionTowardNext(initialState, targetState)
            val durationMs = 300L
            val interpolator = DecelerateInterpolator()

            // 1. Animate Header Title (slideInHorizontally + fadeIn / slideOutHorizontally + fadeOut)
            val headerDistance = (headerTitleView?.width?.takeIf { it > 0 }
                ?: (resources.displayMetrics.widthPixels / 2)).toFloat()
            val headerStartOffset = if (isNext) headerDistance else -headerDistance
            val headerExitOffset = if (isNext) -headerDistance else headerDistance

            headerOutgoingView?.let { outgoing ->
                outgoing.visibility = View.VISIBLE
                outgoing.translationX = 0f
                outgoing.alpha = 1f
                outgoing.animate()
                    .translationX(headerExitOffset)
                    .alpha(0f)
                    .setDuration(durationMs)
                    .setInterpolator(interpolator)
                    .withEndAction {
                        outgoing.visibility = View.GONE
                        outgoing.translationX = 0f
                    }
                    .start()
            }

            headerTitleView?.let { incoming ->
                incoming.translationX = headerStartOffset
                incoming.alpha = 0f
                incoming.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(durationMs)
                    .setInterpolator(interpolator)
                    .start()
            }

            // 2. Animate Lyrics Content (slideInHorizontally + fadeIn / slideOutHorizontally + fadeOut)
            val lyricsDistance = (lyricsScroll.width.takeIf { it > 0 }
                ?: resources.displayMetrics.widthPixels).toFloat()
            val lyricsStartOffset = if (isNext) lyricsDistance else -lyricsDistance
            val lyricsExitOffset = if (isNext) -lyricsDistance else lyricsDistance

            if (snapshotReady && snapshotView != null) {
                snapshotView.visibility = View.VISIBLE
                snapshotView.translationX = 0f
                snapshotView.alpha = 1f
                snapshotView.animate()
                    .translationX(lyricsExitOffset)
                    .alpha(0f)
                    .setDuration(durationMs)
                    .setInterpolator(interpolator)
                    .withLayer()
                    .withEndAction {
                        snapshotView.visibility = View.GONE
                        snapshotView.translationX = 0f
                    }
                    .start()
            }

            lyricsScroll.translationX = lyricsStartOffset
            lyricsScroll.alpha = 0f
            lyricsScroll.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(durationMs)
                .setInterpolator(interpolator)
                .withLayer()
                .start()
        }
    }

    /**
     * Dynamically renders ALL verses (ANDININY 1 .. ANDININY N) and ALL choruses
     * ("ISAN'ANDININY", "ISAN'ANDININY 4", "ISAN'ANDININY 1 SY 2", etc.) in their exact sequence.
     */
    private fun renderStanzas(song: SongEntity) {
        // Hide the static 3-verse template blocks and render dynamically in extraVersesContainer
        sectionVerse1?.visibility = View.GONE
        sectionChorus?.visibility = View.GONE
        sectionVerse2?.visibility = View.GONE
        sectionVerse3?.visibility = View.GONE

        val container = extraVersesContainer ?: return
        container.removeAllViews()
        container.visibility = View.VISIBLE

        dynamicVerseBadges.clear()
        dynamicVerseTextViews.clear()
        dynamicChorusBars.clear()
        dynamicChorusIcons.clear()
        dynamicChorusLabels.clear()
        dynamicChorusTextViews.clear()

        val rawSections = HymnCatalog.parseHymnSections(song.fullLyrics.ifBlank { song.verse1 })
        val sections = if (rawSections.isNotEmpty()) {
            rawSections
        } else {
            listOf(
                HymnCatalog.HymnSection(
                    isChorus = false,
                    title = "ANDININY 1",
                    content = song.verse1
                )
            )
        }

        val ctx = requireContext()
        val density = resources.displayMetrics.density
        val isDark = ThemeManager.isDarkMode.value

        val lyricsTextColor = if (isDark) ThemeManager.DARK_TEXT_PRIMARY else 0xFF1B1C1D.toInt()
        val accentColor = if (isDark) ThemeManager.DARK_PRIMARY_ACCENT else 0xFF800020.toInt()
        val chorusTextColor = if (isDark) ThemeManager.DARK_TEXT_PRIMARY else 0xFF800020.toInt()

        val lineHeightPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            currentFontSizeSp * 1.45f,
            resources.displayMetrics
        ).roundToInt()

        for (index in sections.indices) {
            val section = sections[index]
            val isLast = (index == sections.lastIndex)
            val bottomMarginPx = ((if (isLast) 8 else 28) * density).toInt()

            if (section.isChorus) {
                // Chorus Bento Callout ("ISAN'ANDININY", "ISAN'ANDININY 4", "ISAN'ANDININY 1 SY 2", etc.)
                val chorusRow = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = (4 * density).toInt()
                        bottomMargin = bottomMarginPx
                    }
                }

                // Left 3.5dp vertical accent bar
                val verticalBar = View(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        (3.5f * density).toInt().coerceAtLeast(2),
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(accentColor)
                }

                val rightCol = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginStart = (16 * density).toInt()
                    }
                    val vPad = (4 * density).toInt()
                    setPadding(0, vPad, 0, vPad)
                }

                val headerRow = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = (8 * density).toInt()
                    }
                }

                val musicIcon = ImageView(ctx).apply {
                    val iconSize = (16 * density).toInt()
                    layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
                    setImageResource(R.drawable.ic_library_music)
                }
                ThemeManager.setSolidIconTint(musicIcon, accentColor)

                val chorusLabel = TextView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginStart = (6 * density).toInt()
                    }
                    text = section.title
                    isAllCaps = true
                    letterSpacing = 0.06f
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    setTextColor(accentColor)
                    typeface = fontJakartaBold ?: Typeface.DEFAULT_BOLD
                }

                headerRow.addView(musicIcon)
                headerRow.addView(chorusLabel)

                val chorusBody = TextView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    text = HighlightHelper.highlightText(section.content, currentSearchQuery, isHighlightVisible)
                    setTextColor(chorusTextColor)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, currentFontSizeSp)
                    TextViewCompat.setLineHeight(this, lineHeightPx)
                    typeface = fontInterItalic
                }

                rightCol.addView(headerRow)
                rightCol.addView(chorusBody)

                chorusRow.addView(verticalBar)
                chorusRow.addView(rightCol)
                container.addView(chorusRow)

                dynamicChorusBars.add(verticalBar)
                dynamicChorusIcons.add(musicIcon)
                dynamicChorusLabels.add(chorusLabel)
                dynamicChorusTextViews.add(chorusBody)
            } else {
                // Verse Block ("ANDININY 1", "ANDININY 2", ..., "ANDININY 7", etc.)
                val verseBlock = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = bottomMarginPx
                    }
                }

                val badgeView = TextView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = (14 * density).toInt()
                    }
                    val hPad = (12 * density).toInt()
                    val vPad = (4 * density).toInt()
                    setPadding(hPad, vPad, hPad, vPad)
                    background = createVerseBadgeDrawable(density, isDark)
                    text = section.title
                    isAllCaps = true
                    letterSpacing = 0.06f
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    setTextColor(accentColor)
                    typeface = fontJakartaBold ?: Typeface.DEFAULT_BOLD
                }

                val verseBody = TextView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    text = HighlightHelper.highlightText(section.content, currentSearchQuery, isHighlightVisible)
                    setTextColor(lyricsTextColor)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, currentFontSizeSp)
                    TextViewCompat.setLineHeight(this, lineHeightPx)
                    fontInterRegular?.let { typeface = it }
                }

                verseBlock.addView(badgeView)
                verseBlock.addView(verseBody)
                container.addView(verseBlock)

                dynamicVerseBadges.add(badgeView)
                dynamicVerseTextViews.add(verseBody)
            }
        }
    }

    private fun createVerseBadgeDrawable(
        density: Float,
        isDark: Boolean
    ): android.graphics.drawable.GradientDrawable {
        val stroke1dp = (1f * density).toInt().coerceAtLeast(1)
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 16f * density
            // 100% opaque container color (zero alpha) + 1.dp solid border
            setColor(if (isDark) ThemeManager.DARK_SURFACE_INNER else 0xFFF9E8EA.toInt())
            setStroke(stroke1dp, if (isDark) ThemeManager.DARK_BORDER else 0xFFF0D5DA.toInt())
        }
    }

    fun applyThemeSync(isDark: Boolean) {
        val rootView = view ?: return
        ThemeManager.applyThemeToViewTree(rootView, isDark)
        val hymnReaderRoot = rootView.findViewById<View>(R.id.hymn_reader_root)
        hymnReaderRoot?.background = ThemeManager.createScreenGradient(isDark)

        val iconThemeToggle = rootView.findViewById<ImageView>(R.id.icon_reader_theme_toggle)
        iconThemeToggle?.setImageResource(
            if (isDark) R.drawable.ic_light_mode else R.drawable.ic_dark_mode
        )
        ThemeManager.setSolidIconTint(
            iconThemeToggle,
            if (isDark) ThemeManager.DARK_PRIMARY_ACCENT else 0xFF800020.toInt()
        )

        val density = resources.displayMetrics.density
        val lyricsTextColor = if (isDark) ThemeManager.DARK_TEXT_PRIMARY else 0xFF1B1C1D.toInt()
        val accentColor = if (isDark) ThemeManager.DARK_PRIMARY_ACCENT else 0xFF800020.toInt()
        val chorusTextColor = if (isDark) ThemeManager.DARK_TEXT_PRIMARY else 0xFF800020.toInt()

        tvHeaderTitle?.setTextColor(lyricsTextColor)
        tvHeaderTitleOutgoing?.setTextColor(lyricsTextColor)

        dynamicVerseBadges.forEach { badge ->
            badge.background = createVerseBadgeDrawable(density, isDark)
            badge.setTextColor(accentColor)
        }
        dynamicVerseTextViews.forEach { tv ->
            tv.setTextColor(lyricsTextColor)
        }
        dynamicChorusBars.forEach { bar ->
            bar.setBackgroundColor(accentColor)
        }
        dynamicChorusIcons.forEach { icon ->
            ThemeManager.setSolidIconTint(icon, accentColor)
        }
        dynamicChorusLabels.forEach { label ->
            label.setTextColor(accentColor)
        }
        dynamicChorusTextViews.forEach { tv ->
            tv.setTextColor(chorusTextColor)
        }
        updateFavoriteIcon(currentSong?.isFavorite == true)
    }

    private fun applyFontSize(sizeSp: Float) {
        if (!isAdded) return
        val lineHeightPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sizeSp * 1.45f,
            resources.displayMetrics
        ).roundToInt()

        dynamicVerseTextViews.forEach { tv ->
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
            TextViewCompat.setLineHeight(tv, lineHeightPx)
        }
        dynamicChorusTextViews.forEach { tv ->
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
            TextViewCompat.setLineHeight(tv, lineHeightPx)
        }
    }

    private fun updateFavoriteIcon(isFavorite: Boolean) {
        val icon = iconFav ?: return
        val isDark = ThemeManager.isDarkMode.value
        if (isFavorite) {
            icon.setImageResource(R.drawable.ic_heart)
        } else {
            icon.setImageResource(R.drawable.ic_favorite_border)
        }
        ThemeManager.setSolidIconTint(
            icon,
            if (isDark) ThemeManager.DARK_PRIMARY_ACCENT else 0xFF781C2E.toInt()
        )
    }
}

/**
 * Lecteur de paroles de cantiques (HymnReaderScreen).
 * Appelle KeepScreenOn() et supporte le geste Pinch-to-Zoom (12f..40f) synchronisé avec lyricsFontSizeSp.
 */
@Composable
fun HymnReaderScreen(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    KeepScreenOn()
    val context = LocalContext.current
    Box(
        modifier = modifier.pointerInput(Unit) {
            detectTransformGestures { _, _, zoom, _ ->
                if (zoom != 1f && !zoom.isNaN() && zoom.isFinite()) {
                    val nextSize = (ThemeManager.lyricsFontSizeSp.value * zoom).coerceIn(12f, 40f)
                    ThemeManager.setLyricsFontSizeSp(context, nextSize)
                }
            }
        }
    ) {
        content()
    }
}

