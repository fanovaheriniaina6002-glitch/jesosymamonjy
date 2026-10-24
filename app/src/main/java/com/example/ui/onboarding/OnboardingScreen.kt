package com.example.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.OnboardingPreferencesManager
import com.example.data.local.SongDao
import kotlinx.coroutines.launch

// Exact Pixel-by-Pixel Color Tokens from HTML (100% Opaque for Android 6.0 API 23)
private val ColorGrenat = Color(0xFF781C2E)
private val ColorGrenat75 = Color(0xFF97444F)
private val ColorGrenat80 = Color(0xFF781C2E)
private val ColorSubtitle = Color(0xFF625E55)
private val ColorSubtitle80 = Color(0xFF756E66)
private val ColorSubtitle60 = Color(0xFF897173)
private val ColorSubtitle75 = Color(0xFF756E66)
private val ColorTextDark = Color(0xFF1C1C1E)
private val ColorHomeIndicator = Color(0xFFD6D3D1)
private val ColorAvatarBg = Color(0xFFF2ECEE)
private val ColorBorder = Color(0xFFEAE0E2)
private val ColorDivider = Color(0xFFEAE0E2)
private val ColorInputBg = Color(0xFFFDF9FA)
private val ColorDropdownSelectedBg = Color(0xFFFAF1F3)
private val ColorScreenBg = Color(0xFFFAF8F9)
private val ColorScreenBg95 = Color(0xFFFAF8F9)

val ONBOARDING_REGIONS = listOf(
    "Ambohipo foibe",
    "Fianarantsoa",
    "Mahajanga",
    "Antananarivo",
    "Toamasina",
    "Antsiranana"
)

@Composable
fun OnboardingScreen(
    preferencesManager: OnboardingPreferencesManager,
    songDao: SongDao? = null,
    onOnboardingCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val plusJakartaSans = remember {
        FontFamily(
            Font(R.font.plus_jakarta_sans, FontWeight.Normal),
            Font(R.font.plus_jakarta_sans, FontWeight.Medium),
            Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
            Font(R.font.plus_jakarta_sans, FontWeight.Bold),
            Font(R.font.plus_jakarta_sans, FontWeight.ExtraBold)
        )
    }
    val interFont = remember {
        FontFamily(
            Font(R.font.inter, FontWeight.Normal),
            Font(R.font.inter, FontWeight.Medium),
            Font(R.font.inter, FontWeight.SemiBold),
            Font(R.font.inter, FontWeight.Bold)
        )
    }

    // 1. State Management
    var userName by rememberSaveable { mutableStateOf("") }
    var selectedGender by rememberSaveable { mutableStateOf("Lehilahy") }
    var selectedRegion by rememberSaveable { mutableStateOf("Ambohipo foibe") }
    var selectedImageUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var isDropdownOpen by rememberSaveable { mutableStateOf(true) }
    var isNameFocused by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Android Standard PhotoPicker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            selectedImageUriString = uri.toString()
        }
    }

    val bottomFadeBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                ColorScreenBg95,
                ColorScreenBg
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ColorScreenBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Scrollable Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 132.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 448.dp)
            ) {
                // <!-- 1. EN-TÊTE (Header) -->
                // <div class="mb-6">
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Text(
                        text = "Tongasoa !",
                        fontFamily = plusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp,
                        lineHeight = 38.sp,
                        letterSpacing = (-0.5).sp,
                        color = ColorGrenat
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Manasa anao hameno ireo fafana etsy ambany.",
                        fontFamily = interFont,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = ColorSubtitle
                    )
                }

                // <!-- 2. UPLOAD DE PHOTO (Optionnel / Facultatif) -->
                // <div class="flex flex-col items-center justify-center my-4">
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(112.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("onboarding_photo_picker_card"),
                        contentAlignment = Alignment.Center
                    ) {
                        // <!-- Outer Solid Circle Frame -->
                        // <div class="w-28 h-28 rounded-full bg-[#F2ECEE] border-2 border-[#EAE0E2] flex items-center justify-center shadow-sm relative overflow-hidden">
                        Box(
                            modifier = Modifier
                                .size(112.dp)
                                .clip(CircleShape)
                                .background(ColorAvatarBg)
                                .border(2.dp, ColorBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!selectedImageUriString.isNullOrBlank()) {
                                AsyncImage(
                                    model = Uri.parse(selectedImageUriString),
                                    contentDescription = "Sary",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            } else {
                                val defaultLogoBitmap = remember(context) {
                                    com.example.ui.util.AssetImageLoader.loadBitmapSync(
                                        context,
                                        "logo_transparent.png",
                                        256,
                                        256
                                    )
                                }
                                if (defaultLogoBitmap != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = defaultLogoBitmap.asImageBitmap(),
                                        contentDescription = "Logo Jesosy Mamonjy",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(14.dp)
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_person),
                                        contentDescription = "person",
                                        tint = ColorGrenat75,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }
                        }

                        // <!-- Floating '+' Badge Button -->
                        // <div class="absolute bottom-1 right-1 w-8 h-8 rounded-full bg-[#781C2E] border-2 border-white flex items-center justify-center shadow-md text-white">
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = (-4).dp, y = (-4).dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(ColorGrenat)
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_photo_camera),
                                contentDescription = "add",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // <span class="text-[13px] font-bold text-[#625E55] mt-3 tracking-wide">Sary <span class="font-normal text-[#625E55]/80">(Tsy voatery)</span></span>
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    fontFamily = plusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = ColorSubtitle
                                )
                            ) {
                                append("Sary ")
                            }
                            withStyle(
                                SpanStyle(
                                    fontFamily = interFont,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = ColorSubtitle80
                                )
                            ) {
                                append("(Tsy voatery)")
                            }
                        },
                        letterSpacing = 0.3.sp
                    )
                }

                // <!-- 3. CARTE DE SAISIE BENTO (Formulaire 100% Solid, 0% Blur) -->
                // <div class="bg-white rounded-[24px] border border-[#EAE0E2] p-5 shadow-sm mt-4 space-y-5">
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .border(1.dp, ColorBorder, RoundedCornerShape(24.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // <!-- Champ 1 : Anarana (Nom) -->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "NY ANARANAO",
                            fontFamily = plusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 0.8.sp,
                            color = ColorGrenat
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val inputBg = if (isNameFocused) Color.White else ColorInputBg
                        val inputBorder = if (isNameFocused) ColorGrenat else ColorBorder

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .background(inputBg)
                                .border(1.dp, inputBorder, CircleShape)
                                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_person),
                                contentDescription = "badge",
                                tint = ColorGrenat,
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (userName.isEmpty()) {
                                    Text(
                                        text = "Soraty eto ny anaranao...",
                                        fontFamily = interFont,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = ColorSubtitle60
                                    )
                                }
                                BasicTextField(
                                    value = userName,
                                    onValueChange = { userName = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontFamily = plusJakartaSans,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = ColorTextDark
                                    ),
                                    cursorBrush = SolidColor(ColorGrenat),
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Words,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { focusManager.clearFocus() }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { isNameFocused = it.isFocused }
                                        .testTag("onboarding_name_input")
                                )
                            }
                        }
                    }

                    // <!-- Champ 2 : Lehilahy sa Vehivavy (Genre) -->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "LEHILAHY SA VEHIVAVY",
                            fontFamily = plusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 0.8.sp,
                            color = ColorGrenat
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // <!-- iOS Segmented Control (Solid) -->
                        // <div class="mt-2 rounded-full bg-[#F2ECEE] border border-[#EAE0E2] p-1 flex items-center space-x-1">
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .background(ColorAvatarBg)
                                .border(1.dp, ColorBorder, CircleShape)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Option 1: Lehilahy
                            val isMale = selectedGender == "Lehilahy"
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(CircleShape)
                                    .background(if (isMale) Color.White else ColorAvatarBg)
                                    .clickable {
                                        focusManager.clearFocus()
                                        selectedGender = "Lehilahy"
                                    }
                                    .padding(vertical = 10.dp)
                                    .testTag("onboarding_gender_lehilahy"),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_man),
                                    contentDescription = "face",
                                    tint = if (isMale) ColorGrenat else ColorSubtitle75,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Lehilahy",
                                    fontFamily = plusJakartaSans,
                                    fontWeight = if (isMale) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = if (isMale) ColorGrenat else ColorSubtitle
                                )
                                if (isMale) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(ColorGrenat)
                                    )
                                }
                            }

                            // Option 2: Vehivavy
                            val isFemale = selectedGender == "Vehivavy"
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(CircleShape)
                                    .background(if (isFemale) Color.White else ColorAvatarBg)
                                    .clickable {
                                        focusManager.clearFocus()
                                        selectedGender = "Vehivavy"
                                    }
                                    .padding(vertical = 10.dp)
                                    .testTag("onboarding_gender_vehivavy"),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_woman),
                                    contentDescription = "face_3",
                                    tint = if (isFemale) ColorGrenat else ColorSubtitle75,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Vehivavy",
                                    fontFamily = plusJakartaSans,
                                    fontWeight = if (isFemale) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = if (isFemale) ColorGrenat else ColorSubtitle
                                )
                                if (isFemale) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(ColorGrenat)
                                    )
                                }
                            }
                        }
                    }

                    // <!-- Champ 3 : Fiangonana (Sélection de la Région/Église) -->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "FIANGONANA MISY ANAO",
                            fontFamily = plusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 0.8.sp,
                            color = ColorGrenat
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // <!-- Bouton Dropdown (Sélecteur Capsule) -->
                        // <button type="button" class="w-full rounded-full bg-[#FDF9FA] border border-[#EAE0E2] py-3.5 px-4 mt-2 flex justify-between items-center text-left">
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .background(ColorInputBg)
                                .border(1.dp, ColorBorder, CircleShape)
                                .clickable {
                                    focusManager.clearFocus()
                                    isDropdownOpen = !isDropdownOpen
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                .testTag("onboarding_region_selector"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_church),
                                    contentDescription = "church",
                                    tint = ColorGrenat,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = selectedRegion,
                                    fontFamily = plusJakartaSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = ColorTextDark
                                )
                            }

                            Icon(
                                painter = painterResource(id = R.drawable.ic_expand_more),
                                contentDescription = "expand_more",
                                tint = ColorGrenat,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // <!-- Dropdown Menu Ouvert (Solid Bento Preview) -->
                        // <div class="mt-2.5 rounded-[18px] bg-white border border-[#EAE0E2] shadow-sm overflow-hidden divide-y divide-[#EAE0E2]/60">
                        AnimatedVisibility(
                            visible = isDropdownOpen,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.White)
                                    .border(1.dp, ColorBorder, RoundedCornerShape(18.dp))
                            ) {
                                ONBOARDING_REGIONS.forEachIndexed { index, regionItem ->
                                    val isSelected = selectedRegion == regionItem
                                    val isAmbohipo = regionItem.equals("Ambohipo foibe", ignoreCase = true)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isSelected) ColorDropdownSelectedBg else Color.White)
                                            .clickable {
                                                focusManager.clearFocus()
                                                selectedRegion = regionItem
                                            }
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) ColorGrenat else Color.White)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))

                                            if (isAmbohipo) {
                                                Column {
                                                    Text(
                                                        text = "Ambohipo ",
                                                        fontFamily = plusJakartaSans,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                        fontSize = 14.sp,
                                                        color = if (isSelected) ColorGrenat else ColorTextDark
                                                    )
                                                    Text(
                                                        text = "Foibe",
                                                        fontFamily = interFont,
                                                        fontWeight = FontWeight.Normal,
                                                        fontSize = 11.sp,
                                                        color = ColorSubtitle
                                                    )
                                                }
                                            } else {
                                                Text(
                                                    text = regionItem,
                                                    fontFamily = plusJakartaSans,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                    fontSize = 14.sp,
                                                    color = if (isSelected) ColorGrenat else ColorTextDark
                                                )
                                            }
                                        }

                                        if (isSelected) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_check_circle),
                                                contentDescription = "check_circle",
                                                tint = ColorGrenat,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    if (index < ONBOARDING_REGIONS.lastIndex) {
                                        HorizontalDivider(
                                            thickness = 1.dp,
                                            color = ColorDivider
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // <!-- Devise Liturgique de Clôture -->
                // <div class="text-center mt-6">
                //   <p class="text-[11px] italic text-[#781C2E]/80 tracking-wide">
                //     « Ho voninahitr'Andriamanitra irery ihany »
                //   </p>
                // </div>
                Text(
                    text = "« Ho voninahitr'Andriamanitra irery ihany »",
                    fontFamily = interFont,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    letterSpacing = 0.4.sp,
                    color = ColorGrenat80,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                )
            }
        }

        // <!-- 4. BOUTON D'ACTION FIXE (Sticky Bottom Island) -->
        // <div class="fixed bottom-0 left-0 right-0 max-w-md mx-auto p-5 bg-gradient-to-t from-[#FAF8F9] via-[#FAF8F9]/95 to-transparent pointer-events-auto z-50">
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(ColorScreenBg)
                .border(1.dp, ColorBorder)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 448.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // <button type="button" class="w-full rounded-full bg-[#781C2E] py-4 px-6 text-white text-[16px] font-bold shadow-lg shadow-[#781C2E]/25 flex items-center justify-center space-x-2">
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(ColorGrenat)
                        .border(1.dp, ColorGrenat, CircleShape)
                        .clickable(enabled = !isSaving) {
                            if (isSaving) return@clickable
                            isSaving = true
                            focusManager.clearFocus()
                            coroutineScope.launch {
                                preferencesManager.saveOnboardingProfile(
                                    name = userName,
                                    gender = selectedGender,
                                    region = selectedRegion,
                                    imageUri = selectedImageUriString,
                                    songDao = songDao
                                )
                                onOnboardingCompleted()
                            }
                        }
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .testTag("onboarding_submit_button"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hanomboka",
                        fontFamily = plusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_forward),
                        contentDescription = "arrow_forward",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // <!-- iOS Home Indicator bar -->
                // <div class="w-32 h-1 bg-[#1C1C1E]/20 rounded-full mx-auto mt-3"></div>
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .width(128.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(ColorHomeIndicator)
                )
            }
        }
    }
}
