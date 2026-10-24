package com.example.ui.fragments

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.BitmapFactory
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.OnboardingPreferencesManager
import com.example.data.local.UserProfileEntity
import com.example.data.repository.SongRepository
import com.example.ui.onboarding.ONBOARDING_REGIONS
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class KaontyFragment : Fragment() {

    private lateinit var repository: SongRepository
    private lateinit var onboardingPrefs: OnboardingPreferencesManager
    private var currentProfile = UserProfileEntity()
    private var selectedGender = "male"
    private var pendingPhotoUri: String = ""

    private var profileModalContainer: View? = null
    private var inputProfileName: EditText? = null
    private var selectProfileRegion: Spinner? = null
    private var regionSpinnerAdapter: ArrayAdapter<String>? = null

    private var ivKaontyTopAvatar: ImageView? = null
    private var ivKaontyTopThemeIcon: ImageView? = null
    private var ivKaontyProfileAvatar: ImageView? = null
    private var ivModalProfileAvatar: ImageView? = null

    private var btnGenderMale: MaterialCardView? = null
    private var btnGenderFemale: MaterialCardView? = null
    private var iconGenderMale: ImageView? = null
    private var iconGenderFemale: ImageView? = null
    private var tvGenderMale: TextView? = null
    private var tvGenderFemale: TextView? = null

    private var btnThemeJour: MaterialCardView? = null
    private var btnThemeNuit: MaterialCardView? = null
    private var btnThemeAuto: MaterialCardView? = null
    private var iconThemeJour: ImageView? = null
    private var iconThemeNuit: ImageView? = null
    private var iconThemeAuto: ImageView? = null
    private var tvThemeJour: TextView? = null
    private var tvThemeNuit: TextView? = null
    private var tvThemeAuto: TextView? = null

    private val regions = ONBOARDING_REGIONS

    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            pendingPhotoUri = uri.toString()
            bindAvatarImage(ivModalProfileAvatar, pendingPhotoUri, R.drawable.ic_menu_book, 12)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_kaonty, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getInstance(requireContext())
        repository = SongRepository(db.songDao())
        onboardingPrefs = OnboardingPreferencesManager(requireContext())

        // 1. Top Pill Views
        val btnTopPerson: View? = view.findViewById(R.id.btn_kaonty_top_person)
        val btnTopTheme: View? = view.findViewById(R.id.btn_kaonty_top_theme)
        ivKaontyTopAvatar = view.findViewById(R.id.iv_kaonty_top_avatar)
        ivKaontyTopThemeIcon = view.findViewById(R.id.iv_kaonty_top_theme_icon)

        // 3. Profile Card Views
        val cardUserProfile: View? = view.findViewById(R.id.card_user_profile)
        val btnEditProfile: View? = view.findViewById(R.id.btn_edit_profile)
        val tvDisplayName: TextView? = view.findViewById(R.id.profile_display_name)
        val tvDisplaySubtext: TextView? = view.findViewById(R.id.profile_display_subtext)
        ivKaontyProfileAvatar = view.findViewById(R.id.iv_kaonty_profile_avatar)

        // 4. Photo Category Cards (Trakta, Fotoana) — ONLY in KaontyScreen
        val cardTrakta: View? = view.findViewById(R.id.card_kaonty_trakta)
        val cardFotoana: View? = view.findViewById(R.id.card_kaonty_fotoana)

        view.findViewById<ImageView>(R.id.iv_kaonty_cover_trakta)?.let { iv ->
            com.example.ui.util.AssetImageLoader.loadInto(iv, "toriteny_sy_boky.webp", 250, 300)
        }
        view.findViewById<ImageView>(R.id.iv_kaonty_cover_fotoana)?.let { iv ->
            com.example.ui.util.AssetImageLoader.loadInto(iv, "fandaharam_potoana.jpg", 250, 300)
        }

        // 5. Settings ("Paramètre") Views
        btnThemeJour = view.findViewById(R.id.btn_theme_jour)
        btnThemeNuit = view.findViewById(R.id.btn_theme_nuit)
        btnThemeAuto = view.findViewById(R.id.btn_theme_auto)
        iconThemeJour = view.findViewById(R.id.icon_theme_jour)
        iconThemeNuit = view.findViewById(R.id.icon_theme_nuit)
        iconThemeAuto = view.findViewById(R.id.icon_theme_auto)
        tvThemeJour = view.findViewById(R.id.tv_theme_jour)
        tvThemeNuit = view.findViewById(R.id.tv_theme_nuit)
        tvThemeAuto = view.findViewById(R.id.tv_theme_auto)

        val tvFontSizeBadge: TextView? = view.findViewById(R.id.tv_font_size_badge)
        val btnFontMinus: View? = view.findViewById(R.id.btn_font_minus)
        val btnFontPlus: View? = view.findViewById(R.id.btn_font_plus)
        val seekBarFontSize: SeekBar? = view.findViewById(R.id.seekbar_font_size)
        val tvLyricsLivePreview: TextView? = view.findViewById(R.id.tv_lyrics_live_preview)

        // 6. Contact Support Card
        val cardContactSupport: View? = view.findViewById(R.id.card_contact_support)

        // 7. Edit Profile Modal / Bottom Sheet Views
        profileModalContainer = view.findViewById(R.id.profile_modal_container)
        val profileModalBackdrop: View? = view.findViewById(R.id.profile_modal_backdrop)
        val profileModalCloseBtn: View? = view.findViewById(R.id.profile_modal_close_btn)
        val btnChangePhoto: View? = view.findViewById(R.id.btn_change_photo)
        ivModalProfileAvatar = view.findViewById(R.id.iv_modal_profile_avatar)
        inputProfileName = view.findViewById(R.id.input_profile_name)
        selectProfileRegion = view.findViewById(R.id.select_profile_region)

        btnGenderMale = view.findViewById(R.id.btn_gender_male)
        btnGenderFemale = view.findViewById(R.id.btn_gender_female)
        iconGenderMale = view.findViewById(R.id.icon_gender_male)
        iconGenderFemale = view.findViewById(R.id.icon_gender_female)
        tvGenderMale = view.findViewById(R.id.tv_gender_male)
        tvGenderFemale = view.findViewById(R.id.tv_gender_female)

        val btnSaveProfile: View? = view.findViewById(R.id.btn_save_profile)
        val btnCancelProfile: View? = view.findViewById(R.id.btn_cancel_profile)

        // Setup Region Spinner (6 church regions) with dynamic onSurface text color
        regionSpinnerAdapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            regions
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                if (v is TextView) {
                    val isDark = com.example.ui.theme.ThemeManager.isDarkMode.value
                    v.setTextColor(if (isDark) 0xFFF5E6E8.toInt() else 0xFF1B1C1D.toInt())
                    v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    try {
                        val tf = ResourcesCompat.getFont(context, R.font.inter)
                        v.setTypeface(tf, Typeface.BOLD)
                    } catch (_: Exception) {}
                    val surfaceVariantBg = if (isDark) 0xFF28181B.toInt() else 0xFFF7F0F2.toInt()
                    v.setBackgroundColor(surfaceVariantBg)
                }
                return v
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getDropDownView(position, convertView, parent)
                val isDark = com.example.ui.theme.ThemeManager.isDarkMode.value
                val bgColor = if (isDark) 0xFF1E1214.toInt() else 0xFFFAF8F9.toInt()
                val textColor = if (isDark) 0xFFF5E6E8.toInt() else 0xFF1B1C1D.toInt()
                v.setBackgroundColor(bgColor)
                if (v is TextView) {
                    v.setTextColor(textColor)
                    v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    val padH = (16 * resources.displayMetrics.density).toInt()
                    val padV = (12 * resources.displayMetrics.density).toInt()
                    v.setPadding(padH, padV, padH, padV)
                    try {
                        val tf = ResourcesCompat.getFont(context, R.font.inter)
                        v.setTypeface(tf, Typeface.NORMAL)
                    } catch (_: Exception) {}
                }
                return v
            }
        }
        selectProfileRegion?.adapter = regionSpinnerAdapter
        selectProfileRegion?.setSelection(0)

        // Open Edit Profile Modal
        val openModalListener = View.OnClickListener { openProfileModal() }
        btnEditProfile?.setOnClickListener(openModalListener)
        cardUserProfile?.setOnClickListener(openModalListener)
        btnTopPerson?.setOnClickListener(openModalListener)

        // Close Edit Profile Modal
        val closeModalListener = View.OnClickListener { closeProfileModal() }
        profileModalBackdrop?.setOnClickListener(closeModalListener)
        profileModalCloseBtn?.setOnClickListener(closeModalListener)
        btnCancelProfile?.setOnClickListener(closeModalListener)

        btnChangePhoto?.setOnClickListener {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        // Gender Selection Buttons
        btnGenderMale?.setOnClickListener {
            selectedGender = "male"
            updateGenderVisuals("male")
        }
        btnGenderFemale?.setOnClickListener {
            selectedGender = "female"
            updateGenderVisuals("female")
        }

        // Save Profile Changes to Offline Room Database + DataStore/SharedPreferences
        btnSaveProfile?.setOnClickListener {
            val newName = inputProfileName?.text?.toString()?.trim()
                ?.takeIf { it.isNotEmpty() } ?: currentProfile.displayName
            val selectedRegionIndex = selectProfileRegion?.selectedItemPosition ?: 0
            val newRegion = regions.getOrElse(selectedRegionIndex) { regions.first() }

            onboardingPrefs.setRegionImmediate(newRegion)
            val updated = currentProfile.copy(
                displayName = newName,
                gender = selectedGender,
                region = newRegion,
                photoUri = pendingPhotoUri,
                isOnboardingCompleted = true
            )
            viewLifecycleOwner.lifecycleScope.launch {
                repository.saveUserProfile(updated)
                onboardingPrefs.saveOnboardingProfile(
                    name = newName,
                    gender = if (selectedGender == "female") "Vehivavy" else "Lehilahy",
                    region = newRegion,
                    imageUri = pendingPhotoUri,
                    songDao = AppDatabase.getInstance(requireContext()).songDao()
                )
                closeProfileModal()
            }
        }

        // 2 Photo Category Cards Click Listeners
        cardTrakta?.setOnClickListener {
            (activity as? com.example.MainActivity)?.openTrakta()
        }
        cardFotoana?.setOnClickListener {
            (activity as? com.example.MainActivity)?.openFandaharampotoana()
        }

        // Theme Segmented Buttons (Jour / Nuit / Auto) — 0ms instant RAM update, background save
        val applyThemeMode: (String) -> Unit = { mode ->
            currentProfile = currentProfile.copy(themeMode = mode)
            (activity as? com.example.MainActivity)?.setThemeMode(mode)
                ?: com.example.ui.theme.ThemeManager.setThemeMode(requireContext(), mode)
            applyThemeSync(com.example.ui.theme.ThemeManager.isDarkMode.value, mode)
        }
        btnThemeJour?.setOnClickListener { applyThemeMode("jour") }
        btnThemeNuit?.setOnClickListener { applyThemeMode("nuit") }
        btnThemeAuto?.setOnClickListener { applyThemeMode("auto") }
        btnTopTheme?.setOnClickListener {
            (activity as? com.example.MainActivity)?.toggleThemeMode()
                ?: com.example.ui.theme.ThemeManager.toggleTheme(requireContext())
            applyThemeSync(
                com.example.ui.theme.ThemeManager.isDarkMode.value,
                com.example.ui.theme.ThemeManager.themeMode.value
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    com.example.ui.theme.ThemeManager.isDarkMode.collect { isDark ->
                        applyThemeSync(isDark, com.example.ui.theme.ThemeManager.themeMode.value)
                    }
                }
                launch {
                    com.example.ui.theme.ThemeManager.themeMode.collect { mode ->
                        updateThemeVisuals(mode)
                    }
                }
                launch {
                    com.example.ui.theme.ThemeManager.lyricsFontSizeSp.collect { sizeSp ->
                        val clamped = sizeSp.toInt().coerceIn(12, 40)
                        val progress = (clamped - 12).coerceIn(0, seekBarFontSize?.max ?: 14)
                        if (seekBarFontSize?.progress != progress) {
                            seekBarFontSize?.progress = progress
                        }
                        tvFontSizeBadge?.text = "$clamped sp"
                        tvLyricsLivePreview?.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
                        currentProfile = currentProfile.copy(fontSizeSp = clamped)
                    }
                }
            }
        }

        // Font Size Slider & +/- Buttons synchronized globally with ThemeManager.lyricsFontSizeSp (default 18f)
        val applyFontSize: (Int) -> Unit = { sizeSp ->
            val clamped = sizeSp.coerceIn(12, 40)
            com.example.ui.theme.ThemeManager.setLyricsFontSizeSp(requireContext(), clamped.toFloat())
            val progress = (clamped - 12).coerceIn(0, seekBarFontSize?.max ?: 14)
            if (seekBarFontSize?.progress != progress) {
                seekBarFontSize?.progress = progress
            }
            tvFontSizeBadge?.text = "$clamped sp"
            tvLyricsLivePreview?.setTextSize(TypedValue.COMPLEX_UNIT_SP, clamped.toFloat())
            currentProfile = currentProfile.copy(fontSizeSp = clamped)
        }

        seekBarFontSize?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val sizeSp = (progress + 12).coerceIn(12, 40)
                    tvFontSizeBadge?.text = "$sizeSp sp"
                    tvLyricsLivePreview?.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp.toFloat())
                    currentProfile = currentProfile.copy(fontSizeSp = sizeSp)
                    com.example.ui.theme.ThemeManager.setLyricsFontSizeSp(requireContext(), sizeSp.toFloat())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        btnFontMinus?.setOnClickListener {
            val currentSp = com.example.ui.theme.ThemeManager.lyricsFontSizeSp.value.toInt()
            applyFontSize(currentSp - 1)
        }
        btnFontPlus?.setOnClickListener {
            val currentSp = com.example.ui.theme.ThemeManager.lyricsFontSizeSp.value.toInt()
            applyFontSize(currentSp + 1)
        }

        // Contact Support Bento Card ("Hifandray aminay" -> Fifandraisana Screen)
        cardContactSupport?.setOnClickListener {
            (activity as? com.example.MainActivity)?.openFifandraisana()
        }

        // Observe UserProfile from Offline Room Database
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.userProfile.collect { profile ->
                    val resolved = profile ?: UserProfileEntity()
                    val globalFontSp = com.example.ui.theme.ThemeManager.lyricsFontSizeSp.value.toInt().coerceIn(12, 40)
                    currentProfile = resolved.copy(
                        themeMode = com.example.ui.theme.ThemeManager.themeMode.value,
                        fontSizeSp = globalFontSp
                    )
                    selectedGender = resolved.gender
                    pendingPhotoUri = resolved.photoUri

                    val genderPrefix = if (resolved.gender == "female") "Ranabavy" else "Rahalahy"
                    tvDisplayName?.text = resolved.displayName
                    tvDisplaySubtext?.text = "$genderPrefix • Jesosy Mamonjy M.A Daoud\n${resolved.region}"

                    bindAvatarImage(ivKaontyTopAvatar, resolved.photoUri, R.drawable.ic_person, 6)
                    bindAvatarImage(ivKaontyProfileAvatar, resolved.photoUri, R.drawable.ic_menu_book, 12)
                    bindAvatarImage(ivModalProfileAvatar, resolved.photoUri, R.drawable.ic_menu_book, 12)

                    updateThemeVisuals(com.example.ui.theme.ThemeManager.themeMode.value)
                    seekBarFontSize?.progress = globalFontSp - 12
                    tvFontSizeBadge?.text = "$globalFontSp sp"
                    tvLyricsLivePreview?.setTextSize(
                        TypedValue.COMPLEX_UNIT_SP,
                        globalFontSp.toFloat()
                    )
                }
            }
        }

        applyThemeSync(
            com.example.ui.theme.ThemeManager.isDarkMode.value,
            com.example.ui.theme.ThemeManager.themeMode.value
        )
    }

    fun applyThemeSync(
        isDark: Boolean = com.example.ui.theme.ThemeManager.isDarkMode.value,
        activeMode: String = com.example.ui.theme.ThemeManager.themeMode.value
    ) {
        val root = view ?: return
        val ctx = context ?: return
        com.example.ui.theme.ThemeManager.applyThemeToViewTree(root, isDark)
        updateThemeVisuals(activeMode)
        updateGenderVisuals(selectedGender)

        // Dynamic theme colors matching MaterialTheme.colorScheme
        val onSurfaceColor = if (isDark) 0xFFF5E6E8.toInt() else 0xFF1B1C1D.toInt()
        val onSurfaceVariantColor = if (isDark) 0xFFA69497.toInt() else 0xFF564243.toInt()
        val surfaceVariantColor = if (isDark) 0xFF28181B.toInt() else 0xFFF7F0F2.toInt()
        val strokeColor = if (isDark) 0xFF382528.toInt() else 0xFFEAE0E2.toInt()
        val dp1 = (1 * resources.displayMetrics.density).toInt().coerceAtLeast(1)

        // 1. Box lyrics live preview in Section 3 ("Nahoana no mbola hiandry...")
        root.findViewById<View>(R.id.box_lyrics_live_preview)?.background =
            com.example.ui.theme.ThemeManager.createDashedPreviewDrawable(ctx, isDark)
        root.findViewById<TextView>(R.id.tv_lyrics_live_preview)?.setTextColor(onSurfaceColor)

        // 2. Profile Bottom Sheet Modal ("Fanovana ny mombamomba")
        root.findViewById<View>(R.id.profile_modal_sheet)?.background =
            com.example.ui.theme.ThemeManager.createNumpadSheetDrawable(ctx, isDark)
        root.findViewById<TextView>(R.id.tv_profile_modal_title)?.setTextColor(onSurfaceColor)
        root.findViewById<TextView>(R.id.tv_profile_modal_subtitle)?.setTextColor(onSurfaceVariantColor)
        root.findViewById<ImageView>(R.id.icon_profile_modal_close)?.let {
            ImageViewCompat.setImageTintList(it, ColorStateList.valueOf(onSurfaceVariantColor))
        }
        root.findViewById<View>(R.id.profile_modal_divider)?.setBackgroundColor(strokeColor)

        // Avatar container
        root.findViewById<MaterialCardView>(R.id.card_avatar_preview_container)?.apply {
            setCardBackgroundColor(surfaceVariantColor)
            this.strokeColor = strokeColor
            this.strokeWidth = dp1
        }
        root.findViewById<TextView>(R.id.tv_avatar_preview_title)?.setTextColor(onSurfaceColor)
        root.findViewById<TextView>(R.id.tv_avatar_preview_subtitle)?.setTextColor(onSurfaceVariantColor)

        // Input Name field & labels
        root.findViewById<TextView>(R.id.tv_label_profile_name)?.setTextColor(onSurfaceColor)
        root.findViewById<TextView>(R.id.tv_label_profile_name_sub)?.setTextColor(onSurfaceVariantColor)
        root.findViewById<MaterialCardView>(R.id.card_input_profile_name)?.apply {
            setCardBackgroundColor(surfaceVariantColor)
            this.strokeColor = strokeColor
            this.strokeWidth = dp1
        }
        root.findViewById<ImageView>(R.id.icon_profile_input_name)?.let {
            com.example.ui.theme.ThemeManager.setSolidIconTint(it, onSurfaceVariantColor)
        }
        inputProfileName?.setTextColor(onSurfaceColor)
        inputProfileName?.setHintTextColor(onSurfaceVariantColor)

        // Gender selector
        root.findViewById<TextView>(R.id.tv_label_profile_gender)?.setTextColor(onSurfaceColor)
        root.findViewById<MaterialCardView>(R.id.card_gender_selector)?.apply {
            this.strokeColor = strokeColor
            this.strokeWidth = dp1
            com.example.ui.theme.ThemeManager.stabilizeCardForApi23(this, surfaceVariantColor, strokeColor, dp1)
        }

        // Region dropdown selector
        root.findViewById<TextView>(R.id.tv_label_profile_region)?.setTextColor(onSurfaceColor)
        root.findViewById<TextView>(R.id.tv_label_profile_region_church)?.setTextColor(onSurfaceVariantColor)
        root.findViewById<MaterialCardView>(R.id.card_region_selector)?.apply {
            this.strokeColor = strokeColor
            this.strokeWidth = dp1
            com.example.ui.theme.ThemeManager.stabilizeCardForApi23(this, surfaceVariantColor, strokeColor, dp1)
        }
        root.findViewById<ImageView>(R.id.icon_profile_region_chevron)?.let {
            com.example.ui.theme.ThemeManager.setSolidIconTint(it, onSurfaceVariantColor)
        }
        selectProfileRegion?.setBackgroundColor(surfaceVariantColor)

        // Update region spinner adapter items to reflect theme colors
        regionSpinnerAdapter?.notifyDataSetChanged()
    }

    private fun bindAvatarImage(
        imageView: ImageView?,
        photoUri: String,
        fallbackResId: Int,
        fallbackPaddingDp: Int
    ) {
        val iv = imageView ?: return
        if (photoUri.isNotBlank()) {
            try {
                val uri = Uri.parse(photoUri)
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    val circularDrawable = RoundedBitmapDrawableFactory.create(resources, bitmap).apply {
                        isCircular = true
                        setAntiAlias(true)
                    }
                    iv.setImageDrawable(circularDrawable)
                    iv.setPadding(0, 0, 0, 0)
                    iv.scaleType = ImageView.ScaleType.CENTER_CROP
                    ImageViewCompat.setImageTintList(iv, null)
                    iv.clearColorFilter()
                    return
                }
            } catch (_: Exception) {
            }
        }
        val padPx = (2 * resources.displayMetrics.density).toInt()
        iv.setPadding(padPx, padPx, padPx, padPx)
        iv.scaleType = ImageView.ScaleType.FIT_CENTER
        ImageViewCompat.setImageTintList(iv, null)
        iv.clearColorFilter()
        com.example.ui.util.AssetImageLoader.loadInto(
            imageView = iv,
            assetFileName = "logo_transparent.png",
            reqWidthDp = 128,
            reqHeightDp = 128,
            fallbackResId = fallbackResId
        )
    }

    private fun updateThemeVisuals(activeMode: String) {
        if (context == null) return
        val isDark = com.example.ui.theme.ThemeManager.isDarkMode.value
        val activeCardBg = if (isDark) 0xFF1E1214.toInt() else 0xFFFFFFFF.toInt()
        val inactiveCardBg = if (isDark) 0xFF28181B.toInt() else 0xFFF7F0F2.toInt()
        val activeStroke = if (isDark) 0xFF382528.toInt() else 0xFFEAE0E2.toInt()
        val activeTintColor = if (isDark) 0xFFD4AF37.toInt() else 0xFF781C2E.toInt()
        val inactiveColor = if (isDark) 0xFFA69497.toInt() else 0xFF564243.toInt()
        val dp1 = (1 * resources.displayMetrics.density).toInt().coerceAtLeast(1)

        ivKaontyTopThemeIcon?.setImageResource(
            if (isDark) R.drawable.ic_light_mode else R.drawable.ic_dark_mode
        )
        com.example.ui.theme.ThemeManager.setSolidIconTint(ivKaontyTopThemeIcon, activeTintColor)

        data class ThemeTab(
            val mode: String,
            val card: MaterialCardView?,
            val icon: ImageView?,
            val label: TextView?
        )

        val tabs = listOf(
            ThemeTab("jour", btnThemeJour, iconThemeJour, tvThemeJour),
            ThemeTab("nuit", btnThemeNuit, iconThemeNuit, tvThemeNuit),
            ThemeTab("auto", btnThemeAuto, iconThemeAuto, tvThemeAuto)
        )

        tabs.forEach { tab ->
            val isSelected = tab.mode == activeMode
            tab.card?.let { card ->
                val bg = if (isSelected) activeCardBg else inactiveCardBg
                val sw = if (isSelected) dp1 else 0
                card.strokeColor = activeStroke
                card.strokeWidth = sw
                com.example.ui.theme.ThemeManager.stabilizeCardForApi23(card, bg, activeStroke, sw)
            }
            val tint = if (isSelected) activeTintColor else inactiveColor
            com.example.ui.theme.ThemeManager.setSolidIconTint(tab.icon, tint)
            tab.label?.setTextColor(tint)
        }
    }

    private fun updateGenderVisuals(gender: String) {
        if (context == null) return
        val isDark = com.example.ui.theme.ThemeManager.isDarkMode.value
        val grenat = 0xFF800020.toInt()
        val white = 0xFFFFFFFF.toInt()
        val inactiveBg = if (isDark) 0xFF28181B.toInt() else 0xFFFAF8F9.toInt()
        val inactiveText = if (isDark) 0xFFA69497.toInt() else 0xFF564243.toInt()

        val isMale = gender == "male"

        btnGenderMale?.let { card ->
            com.example.ui.theme.ThemeManager.stabilizeCardForApi23(
                card,
                if (isMale) grenat else inactiveBg,
                if (isMale) grenat else inactiveBg,
                0
            )
        }
        com.example.ui.theme.ThemeManager.setSolidIconTint(iconGenderMale, if (isMale) white else inactiveText)
        tvGenderMale?.setTextColor(if (isMale) white else inactiveText)

        btnGenderFemale?.let { card ->
            com.example.ui.theme.ThemeManager.stabilizeCardForApi23(
                card,
                if (!isMale) grenat else inactiveBg,
                if (!isMale) grenat else inactiveBg,
                0
            )
        }
        com.example.ui.theme.ThemeManager.setSolidIconTint(iconGenderFemale, if (!isMale) white else inactiveText)
        tvGenderFemale?.setTextColor(if (!isMale) white else inactiveText)
    }

    fun openProfileModal() {
        applyThemeSync()
        inputProfileName?.setText(currentProfile.displayName)
        inputProfileName?.setSelection(currentProfile.displayName.length)
        val regionIdx = regions.indexOfFirst { it.equals(currentProfile.region, ignoreCase = true) }
            .coerceAtLeast(0)
        selectProfileRegion?.setSelection(regionIdx)
        selectedGender = currentProfile.gender
        pendingPhotoUri = currentProfile.photoUri
        bindAvatarImage(ivModalProfileAvatar, pendingPhotoUri, R.drawable.ic_menu_book, 12)
        updateGenderVisuals(selectedGender)
        profileModalContainer?.visibility = View.VISIBLE
    }

    fun closeProfileModal() {
        profileModalContainer?.visibility = View.GONE
    }

    fun isProfileModalVisible(): Boolean = profileModalContainer?.visibility == View.VISIBLE
}
