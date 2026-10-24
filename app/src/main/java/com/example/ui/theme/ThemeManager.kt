package com.example.ui.theme

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.widget.ImageViewCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.onboardingDataStore
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * 0ms Instant In-Memory Theme Manager.
 * - Single global variable in RAM (`isDarkMode`).
 * - Zero animation, zero delay (0 ms), zero Activity recreation, zero hymns.json reload.
 * - Background persistence to SharedPreferences, DataStore, and Room.
 */
object ThemeManager {

    private const val PREFS_NAME = "jesosy_mamonjy_instant_theme_prefs"
    private const val PREF_KEY_MODE = "instant_theme_mode"
    private const val PREF_KEY_FONT_SIZE_SP = "lyrics_font_size_sp"
    const val DEFAULT_LYRICS_FONT_SIZE_SP = 18f
    val KEY_THEME_MODE = stringPreferencesKey("theme_mode_preference")

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _themeMode = MutableStateFlow("jour") // "jour", "nuit", "auto"
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _lyricsFontSizeSp = MutableStateFlow(DEFAULT_LYRICS_FONT_SIZE_SP)
    val lyricsFontSizeSp: StateFlow<Float> = _lyricsFontSizeSp.asStateFlow()

    private var isInitialized = false
    private var fontSaveJob: kotlinx.coroutines.Job? = null
    private val immediateListeners = mutableListOf<(Boolean, String) -> Unit>()

    private data class ViewThemeBaseline(
        val dayCardBg: Int? = null,
        val dayStrokeColor: Int? = null,
        val dayViewBgColor: Int? = null,
        val dayTextColor: Int? = null,
        val dayHintColor: Int? = null,
        val dayIconTint: Int? = null
    )

    fun registerImmediateListener(listener: (isDark: Boolean, mode: String) -> Unit) {
        if (!immediateListeners.contains(listener)) {
            immediateListeners.add(listener)
        }
    }

    fun unregisterImmediateListener(listener: (isDark: Boolean, mode: String) -> Unit) {
        immediateListeners.remove(listener)
    }

    fun init(context: Context, @Suppress("UNUSED_PARAMETER") scope: CoroutineScope) {
        if (isInitialized) return
        isInitialized = true

        val appContext = context.applicationContext
        val fastPrefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedMode = fastPrefs.getString(PREF_KEY_MODE, null)
        val cachedFontSize = fastPrefs.getFloat(PREF_KEY_FONT_SIZE_SP, DEFAULT_LYRICS_FONT_SIZE_SP)
        _lyricsFontSizeSp.value = cachedFontSize.coerceIn(12f, 40f)

        if (cachedMode != null) {
            applyModeInMemory(appContext, cachedMode, notifySync = false)
        } else {
            applyModeInMemory(appContext, "jour", notifySync = false)
        }

        // Verify/load from DataStore or Room in background only if not cached yet
        backgroundScope.launch {
            val savedMode = cachedMode ?: appContext.onboardingDataStore.data.map { prefs ->
                prefs[KEY_THEME_MODE]
            }.firstOrNull() ?: try {
                AppDatabase.getInstance(appContext).songDao().getUserProfileOnce()?.themeMode ?: "jour"
            } catch (_: Exception) {
                "jour"
            }

            fastPrefs.edit().putString(PREF_KEY_MODE, savedMode).apply()
            if (savedMode != _themeMode.value) {
                kotlinx.coroutines.withContext(Dispatchers.Main.immediate) {
                    applyModeInMemory(appContext, savedMode, notifySync = true)
                }
            }
        }
    }

    /**
     * Global 0ms synchronous update for lyrics font size (`lyricsFontSizeSp`, default 18f)
     * shared between KaontyFragment, HymnReaderFragment, and TraktaReaderFragment.
     */
    fun setLyricsFontSizeSp(context: Context, sizeSp: Float) {
        val clamped = sizeSp.coerceIn(12f, 40f)
        if (_lyricsFontSizeSp.value == clamped) return
        _lyricsFontSizeSp.value = clamped

        val appContext = context.applicationContext
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(PREF_KEY_FONT_SIZE_SP, clamped)
            .apply()

        fontSaveJob?.cancel()
        fontSaveJob = backgroundScope.launch {
            kotlinx.coroutines.delay(150L)
            try {
                val dao = AppDatabase.getInstance(appContext).songDao()
                val existing = dao.getUserProfileOnce()
                if (existing != null && existing.fontSizeSp != clamped.toInt()) {
                    dao.saveUserProfile(existing.copy(fontSizeSp = clamped.toInt()))
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun resolveIsDark(context: Context, mode: String): Boolean {
        return when (mode) {
            "nuit" -> true
            "jour" -> false
            else -> {
                val currentNightMask = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                currentNightMask == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    private fun applyModeInMemory(context: Context, mode: String, notifySync: Boolean) {
        val isDark = resolveIsDark(context, mode)
        _themeMode.value = mode
        _isDarkMode.value = isDark
        if (notifySync) {
            val snapshot = immediateListeners.toList()
            for (listener in snapshot) {
                listener(isDark, mode)
            }
        }
    }

    /**
     * 0ms synchronous theme mode switch on UI thread + background persistence.
     */
    fun setThemeMode(context: Context, mode: String) {
        val appContext = context.applicationContext
        applyModeInMemory(appContext, mode, notifySync = true)

        // Save in background without ever delaying the UI
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_KEY_MODE, mode)
            .apply()

        backgroundScope.launch {
            try {
                appContext.onboardingDataStore.edit { prefs ->
                    prefs[KEY_THEME_MODE] = mode
                }
            } catch (_: Exception) {
            }
            try {
                val dao = AppDatabase.getInstance(appContext).songDao()
                val existing = dao.getUserProfileOnce()
                if (existing != null && existing.themeMode != mode) {
                    dao.saveUserProfile(existing.copy(themeMode = mode))
                }
            } catch (_: Exception) {
            }
        }
    }

    /**
     * 0ms synchronous toggle between Jour and Nuit + background persistence.
     */
    fun toggleTheme(context: Context): Boolean {
        val next = if (_isDarkMode.value) "jour" else "nuit"
        setThemeMode(context, next)
        return _isDarkMode.value
    }

    fun handleConfigurationChange(context: Context) {
        if (_themeMode.value == "auto") {
            applyModeInMemory(context.applicationContext, "auto", notifySync = true)
        }
    }

    // Exact Premium Dark Bento Palette Constants
    const val DARK_BG = 0xFF120A0B.toInt()             // #120A0B Gris-Grenat extrêmement profond
    const val DARK_SURFACE = 0xFF1E1214.toInt()        // #1E1214 Gris-Rouge chaud et élégant (Cartes, TopBar, NavBar)
    const val DARK_SURFACE_INNER = 0xFF28181B.toInt()  // #28181B Surface secondaire interne aux cartes
    const val DARK_TEXT_PRIMARY = 0xFFF5E6E8.toInt()   // #F5E6E8 Blanc Cassé très doux (Textes principaux)
    const val DARK_TEXT_SECONDARY = 0xFFA69497.toInt() // #A69497 Gris rosé (Sous-titres, descriptions, compteurs)
    const val DARK_PRIMARY_ACCENT = 0xFFD4AF37.toInt() // #D4AF37 Or mat (Onglet actif & icônes importantes)
    const val DARK_BORDER = 0xFF382528.toInt()         // #382528 Bordures fines 1.dp

    fun createScreenGradient(isDark: Boolean): GradientDrawable {
        val colors = if (isDark) {
            intArrayOf(DARK_BG, DARK_BG, DARK_BG)
        } else {
            intArrayOf(0xFFFAF8F9.toInt(), 0xFFF6E9EC.toInt(), 0xFFF9F1E8.toInt())
        }
        return GradientDrawable(GradientDrawable.Orientation.TL_BR, colors)
    }

    fun createNumpadSheetDrawable(context: Context, isDark: Boolean): GradientDrawable {
        val density = context.resources.displayMetrics.density
        val r = 28f * density
        val stroke1dp = (1f * density).toInt().coerceAtLeast(1)
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            setColor(if (isDark) DARK_SURFACE else 0xFFFFFFFF.toInt())
            setStroke(stroke1dp, if (isDark) DARK_BORDER else 0xFFEAE0E2.toInt())
        }
    }

    fun createDashedPreviewDrawable(context: Context, isDark: Boolean): GradientDrawable {
        val density = context.resources.displayMetrics.density
        val stroke1dp = (1f * density).toInt().coerceAtLeast(1)
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16f * density
            // MaterialTheme.colorScheme.surface: blanc cassé #FAF8F9 en Jour, gris sombre #1E1214 en Nuit
            setColor(if (isDark) DARK_SURFACE else 0xFFFAF8F9.toInt())
            setStroke(
                stroke1dp,
                if (isDark) DARK_BORDER else 0xFFEAE0E2.toInt(),
                4f * density,
                3f * density
            )
        }
    }

    /**
     * Forces an explicit, 100% opaque icon tint on an ImageView for Android 6.0 (API 23) compatibility.
     * Applies both ImageViewCompat.setImageTintList and PorterDuff.Mode.SRC_IN colorFilter.
     */
    fun setSolidIconTint(imageView: ImageView?, color: Int) {
        val view = imageView ?: return
        val opaqueColor = color or 0xFF000000.toInt()
        ImageViewCompat.setImageTintList(view, ColorStateList.valueOf(opaqueColor))
        view.setColorFilter(opaqueColor, android.graphics.PorterDuff.Mode.SRC_IN)
    }

    /**
     * Stabilizes a MaterialCardView for Android 6.0 (API 23) Canvas/hwui rendering:
     * 1. Zero elevation / zero shadow (`cardElevation = 0f`, `elevation = 0f`)
     * 2. Disables hardware outline clipping (`clipToOutline = false`) and clamps excessive `999dp` corner radii
     * 3. Removes MaterialCardView's foreground ripple mask (`foreground = null`) which renders as a solid white overlay on API 23
     * 4. Applies a crisp, 100% opaque native `GradientDrawable` background with a `1.dp` border when `strokeWidth > 0`.
     */
    fun stabilizeCardForApi23(
        card: MaterialCardView,
        explicitBgColor: Int? = null,
        explicitStrokeColor: Int? = null,
        explicitStrokeWidthPx: Int? = null
    ) {
        val density = card.resources.displayMetrics.density
        val dp1 = (1f * density).toInt().coerceAtLeast(1)
        val maxSafeRadius = 26f * density
        val currentRadius = card.radius
        val clampedRadius = if (currentRadius > maxSafeRadius) maxSafeRadius else currentRadius

        card.cardElevation = 0f
        card.maxCardElevation = 0f
        card.elevation = 0f
        card.clipToOutline = false
        card.preventCornerOverlap = false
        card.useCompatPadding = false
        if (currentRadius > maxSafeRadius) {
            card.radius = maxSafeRadius
        }

        val rawBg = explicitBgColor ?: card.cardBackgroundColor.defaultColor
        val opaqueBg = if ((rawBg ushr 24) == 0) {
            // Never allow transparent card containers on API 23
            if (_isDarkMode.value) DARK_SURFACE_INNER else 0xFFF7F0F2.toInt()
        } else {
            rawBg or 0xFF000000.toInt()
        }

        val strokeW = explicitStrokeWidthPx ?: if (card.strokeWidth > 0) dp1 else 0
        val rawStroke = explicitStrokeColor ?: card.strokeColor
        val opaqueStroke = if (rawStroke != 0) {
            rawStroke or 0xFF000000.toInt()
        } else {
            if (_isDarkMode.value) DARK_BORDER else 0xFFEAE0E2.toInt()
        }

        card.setCardBackgroundColor(opaqueBg)
        card.setCardForegroundColor(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT))
        card.foreground = null
        card.outlineProvider = null

        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = clampedRadius
            setColor(opaqueBg)
            if (strokeW > 0) {
                setStroke(strokeW, opaqueStroke)
            }
        }
        card.background = bgDrawable
    }

    /**
      * Synchronously applies the current `isDark` state to an entire View tree in 0ms.
      * Ensures background gradients, cards, borders (strictly 1.dp), texts, hints, and icons update in the same frame.
      */
    fun applyThemeToViewTree(root: View?, isDark: Boolean) {
        val view = root ?: return
        val id = view.id
        val dp1 = (1f * view.resources.displayMetrics.density).toInt().coerceAtLeast(1)

        // Special handling for btn_hero_read ("Hamaky >" in Hira Anio)
        if (id == R.id.btn_hero_read && view is MaterialCardView) {
            val heroBtnBg = if (isDark) 0xFF28181B.toInt() else 0xFFFFFFFF.toInt()
            val heroBtnStroke = if (isDark) DARK_BORDER else 0xFFEAE0E2.toInt()
            view.strokeWidth = dp1
            view.strokeColor = heroBtnStroke
            stabilizeCardForApi23(view, heroBtnBg, heroBtnStroke, dp1)
        }

        // Skip self-contained dark/hero/photo components or explicitly managed bottom nav
        if (id == R.id.floating_bottom_nav_pill ||
            (id == R.id.card_hero_song && view.id != R.id.btn_hero_read) ||
            id == R.id.card_fifandraisana_hero ||
            id == R.id.card_trakta_cover ||
            id == R.id.card_reader_cover ||
            id == R.id.card_kaonty_trakta ||
            id == R.id.card_kaonty_fotoana ||
            id == R.id.onboarding_compose_view
        ) {
            if (view is MaterialCardView) {
                view.cardElevation = 0f
                view.maxCardElevation = 0f
                view.elevation = 0f
                view.setCardForegroundColor(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT))
                view.foreground = null
                if (id == R.id.card_hero_song || id == R.id.card_fifandraisana_hero) {
                    val heroBg = if (isDark) 0xFF800020.toInt() else 0xFF781C2E.toInt()
                    val heroStroke = if (isDark) DARK_BORDER else 0xFF8F253B.toInt()
                    stabilizeCardForApi23(view, heroBg, heroStroke, dp1)
                } else if (view.strokeWidth > 0) {
                    view.strokeWidth = dp1
                    if (isDark) {
                        view.strokeColor = DARK_BORDER
                    }
                }
            }
            return
        }

        // 1. Screen root / scroll gradient backgrounds
        if (id == R.id.main_root_container ||
            id == R.id.fandraisana_scroll_view ||
            id == R.id.hymn_reader_root ||
            id == R.id.fandaharampotoana_root ||
            id == R.id.trakta_root ||
            id == R.id.trakta_reader_modal_container ||
            id == R.id.fifandraisana_root
        ) {
            view.background = createScreenGradient(isDark)
        } else if (id == R.id.numpad_sheet || id == R.id.profile_modal_sheet) {
            view.background = createNumpadSheetDrawable(view.context, isDark)
        } else if (id == R.id.box_lyrics_live_preview) {
            view.background = createDashedPreviewDrawable(view.context, isDark)
        }

        val existingBaseline = view.getTag(R.id.main_root_container) as? ViewThemeBaseline
        val baseline = existingBaseline ?: captureBaseline(view).also {
            view.setTag(R.id.main_root_container, it)
        }

        // 2. MaterialCardView surface & stroke (strictly 1.dp on bordered cards, 0dp elevation)
        if (view is MaterialCardView) {
            val resolvedBg = baseline.dayCardBg?.let { dayBg ->
                mapCardBgColor(dayBg, isDark) ?: dayBg
            } ?: (if (isDark) DARK_SURFACE else 0xFFFFFFFF.toInt())

            val resolvedStroke = baseline.dayStrokeColor?.let { dayStroke ->
                mapStrokeColor(dayStroke, isDark) ?: dayStroke
            } ?: (if (isDark) DARK_BORDER else 0xFFEAE0E2.toInt())

            val resolvedStrokeWidth = if (view.strokeWidth > 0) dp1 else 0
            view.strokeColor = resolvedStroke
            if (resolvedStrokeWidth > 0) {
                view.strokeWidth = resolvedStrokeWidth
            }
            stabilizeCardForApi23(view, resolvedBg, resolvedStroke, resolvedStrokeWidth)
        } else if (baseline.dayViewBgColor != null &&
            id != R.id.main_root_container &&
            id != R.id.fandraisana_scroll_view &&
            id != R.id.hymn_reader_root &&
            id != R.id.fandaharampotoana_root &&
            id != R.id.trakta_root &&
            id != R.id.trakta_reader_modal_container &&
            id != R.id.fifandraisana_root &&
            id != R.id.numpad_sheet &&
            id != R.id.profile_modal_sheet &&
            id != R.id.box_lyrics_live_preview &&
            id != R.id.tv_lyrics_live_preview
        ) {
            val targetBg = mapViewBgColor(baseline.dayViewBgColor, isDark)
            if (targetBg != null) {
                view.setBackgroundColor(targetBg or 0xFF000000.toInt())
            }
        } else if (view.background is GradientDrawable &&
            view.parent?.let { it is View && (it as View).id == R.id.fragment_container } == true
        ) {
            // Top-level Fragment root with bg_screen_gradient
            view.background = createScreenGradient(isDark)
        }

        // 3. TextView & EditText colors & backgrounds (Solid 100% opaque, zero black rectangles on API 23)
        if (view is TextView) {
            if (id == R.id.tv_lyrics_live_preview) {
                view.setTextColor(if (isDark) DARK_TEXT_PRIMARY else 0xFF1B1C1D.toInt())
                view.background = null
            } else if (id == R.id.tv_btn_hero_read) {
                view.setTextColor(if (isDark) DARK_TEXT_PRIMARY else 0xFF1C1C1E.toInt())
            } else if (id == R.id.input_profile_name) {
                view.setTextColor(if (isDark) DARK_TEXT_PRIMARY else 0xFF1B1C1D.toInt())
                view.setHintTextColor(if (isDark) DARK_TEXT_SECONDARY else 0xFF897173.toInt())
            } else {
                baseline.dayTextColor?.let { dayText ->
                    val targetText = mapTextColor(dayText, isDark)
                    if (targetText != null) {
                        view.setTextColor(targetText or 0xFF000000.toInt())
                    }
                }
                if (view is EditText || baseline.dayHintColor != null) {
                    view.setHintTextColor(if (isDark) DARK_TEXT_SECONDARY else 0xFF897173.toInt())
                }
            }

            if (view is EditText) {
                val inputBg = if (id == R.id.input_profile_name) {
                    if (isDark) DARK_SURFACE_INNER else 0xFFF7F0F2.toInt()
                } else {
                    if (isDark) DARK_SURFACE else 0xFFFFFFFF.toInt()
                }
                view.setBackgroundColor(inputBg or 0xFF000000.toInt())
            }
        }

        // 4. ImageView & ImageButton explicit tints
        if (view is ImageView) {
            if (id == R.id.iv_btn_hero_read) {
                setSolidIconTint(view, if (isDark) DARK_TEXT_PRIMARY else 0xFF1C1C1E.toInt())
            } else {
                // Skip custom user photo avatars that cleared their tint
                val currentTint = ImageViewCompat.getImageTintList(view)
                if (currentTint != null && baseline.dayIconTint != null) {
                    val targetTint = mapIconTint(baseline.dayIconTint, isDark) ?: baseline.dayIconTint
                    setSolidIconTint(view, targetTint)
                }
            }
        }

        // 5. SeekBar tints
        if (view is SeekBar) {
            val trackBg = if (isDark) DARK_BORDER else 0xFFEAE0E2.toInt()
            val accent = if (isDark) DARK_PRIMARY_ACCENT else 0xFF800020.toInt()
            view.progressBackgroundTintList = ColorStateList.valueOf(trackBg)
            view.progressTintList = ColorStateList.valueOf(accent)
            view.thumbTintList = ColorStateList.valueOf(accent)
        }

        // 6. Recurse into children synchronously
        if (view is ViewGroup) {
            val count = view.childCount
            for (i in 0 until count) {
                applyThemeToViewTree(view.getChildAt(i), isDark)
            }
        }
    }

    private fun captureBaseline(view: View): ViewThemeBaseline {
        val dayCardBg = if (view is MaterialCardView) {
            normalizeCardBgToDay(view.cardBackgroundColor.defaultColor)
        } else null

        val dayStroke = if (view is MaterialCardView) {
            normalizeStrokeToDay(view.strokeColor)
        } else null

        val dayViewBg = (view.background as? ColorDrawable)?.color?.let {
            normalizeViewBgToDay(it)
        }

        val dayText = if (view is TextView) {
            normalizeTextToDay(view.currentTextColor)
        } else null

        val dayHint = if (view is EditText) {
            view.currentHintTextColor
        } else null

        val dayTint = if (view is ImageView) {
            ImageViewCompat.getImageTintList(view)?.defaultColor?.let {
                normalizeIconTintToDay(it)
            }
        } else null

        return ViewThemeBaseline(
            dayCardBg = dayCardBg,
            dayStrokeColor = dayStroke,
            dayViewBgColor = dayViewBg,
            dayTextColor = dayText,
            dayHintColor = dayHint,
            dayIconTint = dayTint
        )
    }

    private fun normalizeCardBgToDay(color: Int): Int {
        return when (color) {
            DARK_SURFACE, 0xFF1C1C1E.toInt() -> 0xFFFFFFFF.toInt()
            DARK_SURFACE_INNER, 0xFF2C2C2E.toInt() -> 0xFFF7F0F2.toInt()
            0xFF2C1A1E.toInt(),
            0x14781C2E,
            0x1A781C2E,
            0x14800020,
            0x1A800020,
            0x26781C2E,
            0x33781C2E -> 0xFFF9E8EA.toInt()
            0xFF2C2418.toInt() -> 0xFFFDF4E7.toInt()
            0xFF262022.toInt() -> 0xFFF2ECEE.toInt()
            DARK_BG, 0xFF0A0204.toInt(), 0xFF000000.toInt() -> 0xFFFAF8F9.toInt()
            0x00000000 -> 0xFFF7F0F2.toInt()
            else -> color or 0xFF000000.toInt()
        }
    }

    private fun mapCardBgColor(dayColor: Int, isDark: Boolean): Int? {
        if (!isDark) return dayColor or 0xFF000000.toInt()
        return when (dayColor) {
            0xFFFFFFFF.toInt(),
            0xFFF9E8EA.toInt(),
            0xFFFDF4E7.toInt(),
            0xFFF2ECEE.toInt() -> DARK_SURFACE
            0xFFFAF8F9.toInt(),
            0xFFFDF9FA.toInt(),
            0xFFFBF9FA.toInt(),
            0xFFF7F0F2.toInt(),
            0xFFFAF0F2.toInt(),
            0xFFF9F1F2.toInt(),
            0xFFF5F3F4.toInt(),
            0x14781C2E,
            0x1A781C2E,
            0x14800020,
            0x1A800020 -> DARK_SURFACE_INNER
            else -> dayColor or 0xFF000000.toInt()
        }
    }

    private fun normalizeStrokeToDay(color: Int): Int {
        return when (color) {
            DARK_BORDER,
            0xFF38383A.toInt(),
            0xFF2C2C2E.toInt(),
            0x4DDCC0C1,
            0x26781C2E,
            0x1A781C2E -> 0xFFEAE0E2.toInt()
            else -> if (color != 0) color or 0xFF000000.toInt() else 0xFFEAE0E2.toInt()
        }
    }

    private fun mapStrokeColor(dayColor: Int, isDark: Boolean): Int? {
        if (!isDark) return dayColor or 0xFF000000.toInt()
        return when (dayColor) {
            0xFFEAE0E2.toInt(),
            0xFFF0D5DA.toInt(),
            0xFF8F253B.toInt(),
            0x4DDCC0C1,
            0x26781C2E,
            0x1A781C2E -> DARK_BORDER
            else -> DARK_BORDER
        }
    }

    private fun normalizeViewBgToDay(color: Int): Int {
        return when (color) {
            DARK_BG, 0xFF0A0204.toInt(), 0xFF000000.toInt() -> 0xFFFBF9FA.toInt()
            DARK_SURFACE, 0xFF1C1C1E.toInt() -> 0xFFFFFFFF.toInt()
            DARK_SURFACE_INNER, 0xFF2C2C2E.toInt() -> 0xFFFAF8F9.toInt()
            DARK_BORDER, 0xFF38383A.toInt(), 0x4DDCC0C1 -> 0xFFEAE0E2.toInt()
            else -> color or 0xFF000000.toInt()
        }
    }

    private fun mapViewBgColor(dayColor: Int, isDark: Boolean): Int? {
        if (!isDark) return dayColor or 0xFF000000.toInt()
        return when (dayColor) {
            0xFFFFFFFF.toInt() -> DARK_SURFACE
            0xFFFBF9FA.toInt(), 0xFFFAF8F9.toInt(), 0xFFFDF9FA.toInt() -> DARK_BG
            0xFFF7F0F2.toInt(), 0xFFFAF0F2.toInt(), 0xFFF2ECEE.toInt(), 0xFFF5F3F4.toInt() -> DARK_SURFACE_INNER
            0xFFEAE0E2.toInt(), 0xFFF0D5DA.toInt() -> DARK_BORDER
            else -> null
        }
    }

    private fun normalizeTextToDay(color: Int): Int {
        return when (color) {
            DARK_TEXT_PRIMARY -> 0xFF1B1C1D.toInt()
            DARK_TEXT_SECONDARY -> 0xFF564243.toInt()
            DARK_PRIMARY_ACCENT -> 0xFF781C2E.toInt()
            else -> color or 0xFF000000.toInt()
        }
    }

    /**
      * Rule 3:
      * - Primary texts (song titles, menu titles, headers, body text): Blanc Cassé #F5E6E8
      * - Secondary texts (subtitles, descriptions, item counts, snippets): Gris Rosé #A69497
      * - Zero bright yellow on basic text!
      */
    private fun mapTextColor(dayColor: Int, isDark: Boolean): Int? {
        if (dayColor == 0xFFFFFFFF.toInt()) return 0xFFFFFFFF.toInt()
        if (!isDark) return dayColor or 0xFF000000.toInt()
        return when (dayColor) {
            // Primary texts, song titles, section headers, menu titles -> #F5E6E8
            0xFF1B1C1D.toInt(),
            0xFF1C1C1E.toInt(),
            0xFF2A2426.toInt(),
            0xFF000000.toInt(),
            0xFF781C2E.toInt(),
            0xFF800020.toInt(),
            0xFF59021A.toInt(),
            0xFF5A1827.toInt() -> DARK_TEXT_PRIMARY

            // Secondary texts, subtitles, descriptions, item counts -> #A69497
            0xFF564243.toInt(),
            0xFF625E55.toInt(),
            0xFF534345.toInt(),
            0xFF897173.toInt(),
            0xFF8E8E93.toInt(),
            0xFFA69497.toInt() -> DARK_TEXT_SECONDARY

            else -> null
        }
    }

    private fun normalizeIconTintToDay(color: Int): Int {
        return when (color) {
            DARK_TEXT_PRIMARY -> 0xFF1B1C1D.toInt()
            DARK_TEXT_SECONDARY -> 0xFF564243.toInt()
            DARK_PRIMARY_ACCENT -> 0xFF781C2E.toInt()
            0xB3800020.toInt() -> 0xFF781C2E.toInt()
            else -> color or 0xFF000000.toInt()
        }
    }

    /**
      * Rule 4:
      * - Important/active icons (primary_container, grenat, gold): Or mat #D4AF37
      * - Secondary/muted icons: Gris rosé #A69497
      */
    private fun mapIconTint(dayColor: Int, isDark: Boolean): Int? {
        if (dayColor == 0xFFFFFFFF.toInt()) return 0xFFFFFFFF.toInt()
        if (!isDark) return dayColor or 0xFF000000.toInt()
        return when (dayColor) {
            0xFF1B1C1D.toInt(),
            0xFF1C1C1E.toInt(),
            0xFF2A2426.toInt() -> DARK_TEXT_PRIMARY
            0xFF564243.toInt(),
            0xFF625E55.toInt(),
            0xFF897173.toInt(),
            0xFF8E8E93.toInt() -> DARK_TEXT_SECONDARY
            0xFFB8860B.toInt(),
            0xFF781C2E.toInt(),
            0xFF800020.toInt(),
            0xFF59021A.toInt(),
            0xB3800020.toInt() -> DARK_PRIMARY_ACCENT
            else -> dayColor or 0xFF000000.toInt()
        }
    }
}
